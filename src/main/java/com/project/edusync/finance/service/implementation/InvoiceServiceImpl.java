package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.InvalidPaymentOperationException;
import com.project.edusync.common.exception.finance.InvoiceNotFoundException;
import com.project.edusync.common.exception.finance.StudentFeeMapNotFoundException;
import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import com.project.edusync.finance.mapper.InvoiceMapper;
import com.project.edusync.finance.model.entity.*;
import com.project.edusync.finance.model.enums.FineType;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.finance.model.enums.PaymentStatus;
import com.project.edusync.finance.repository.*;
import com.project.edusync.finance.service.InvoiceService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.finance.utils.NumberToWordsConverter;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final StudentFeeMapRepository studentFeeMapRepository;
    private final FeeParticularRepository feeParticularRepository;
    private final LateFeeRuleRepository lateFeeRuleRepository;
    private final ScholarshipAssignmentRepository scholarshipAssignmentRepository;
    private final ScholarshipTypeRepository scholarshipTypeRepository;
    private final InvoiceMapper invoiceMapper;

    private final PdfGenerationService pdfGenerationService;
    private final NumberToWordsConverter numberToWordsConverter;
    // InvoiceLineItemRepository is not needed — saved by CascadeType.ALL.

    @Override
    @Transactional
    public InvoiceResponseDTO generateSingleInvoice(Long studentId) {
        log.info("Generating invoice for studentId: {}", studentId);

        // 1. Find the Student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with Student ID: " + studentId));

        // ── BUG FIX 1: Duplicate Billing Guard ──────────────────────────────────
        // Before generating a new invoice, verify the student does NOT already have
        // an open (non-cancelled, non-paid) invoice. CANCELLED invoices are excluded
        // so that a replacement can be issued after a cancellation.
        List<InvoiceStatus> openStatuses = Arrays.asList(InvoiceStatus.CANCELLED, InvoiceStatus.PAID);
        if (invoiceRepository.existsByStudentAndStatusNotIn(student, openStatuses)) {
            throw new InvalidPaymentOperationException(
                    "Student " + studentId + " already has an active invoice. " +
                    "Cancel the existing invoice before generating a new one.");
        }
        // ────────────────────────────────────────────────────────────────────────

        // 2. Find the Student's Fee Map
        StudentFeeMap feeMap = studentFeeMapRepository.findByStudent_Id(studentId)
                .orElseThrow(() -> new StudentFeeMapNotFoundException("Student Fee map not Found with Student ID: " + studentId));

        // 3. Get the assigned Fee Structure
        FeeStructure feeStructure = feeMap.getFeeStructure();

        // 4. Get all particulars (line items) for that structure
        List<FeeParticular> particulars = feeParticularRepository.findByFeeStructure_Id(feeStructure.getId());

        // 5. Create the new Invoice
        Invoice invoice = new Invoice();
        invoice.setStudent(student);
        invoice.setIssueDate(LocalDate.now());
        invoice.setDueDate(LocalDate.now().plusDays(30)); // Default due date
        invoice.setStatus(InvoiceStatus.PENDING);
        invoice.setInvoiceNumber(generateInvoiceNumber());

        BigDecimal totalAmount = BigDecimal.ZERO;

        // 6. Create InvoiceLineItems from the particulars
        for (FeeParticular particular : particulars) {
            // TODO: Add logic here to check if this particular is due
            // (e.g., is it ONE_TIME and not yet invoiced? Is it MONTHLY?)
            // For now, we will add all particulars.

            InvoiceLineItem lineItem = new InvoiceLineItem();
            lineItem.setDescription(particular.getName());
            lineItem.setAmount(particular.getAmount());

            // Add the line item to the invoice (this sets the bidirectional link)
            invoice.addLineItem(lineItem);

            totalAmount = totalAmount.add(particular.getAmount());
        }

        invoice.setTotalAmount(totalAmount);

        // 7. Check for Active Scholarships
        List<ScholarshipAssignment> scholarships = scholarshipAssignmentRepository.findByStudentId(studentId);
        log.info("Found {} scholarship assignments for student {}", scholarships.size(), studentId);
        
        ScholarshipAssignment activeScholarship = scholarships.stream()
                .peek(s -> log.info("Checking scholarship: ID={}, Status={}, From={}, To={}", 
                        s.getId(), s.getStatus(), s.getEffectiveFrom(), s.getEffectiveTo()))
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .filter(s -> !LocalDate.now().isBefore(s.getEffectiveFrom()))
                .filter(s -> s.getEffectiveTo() == null || !LocalDate.now().isAfter(s.getEffectiveTo()))
                .findFirst()
                .orElse(null);

        if (activeScholarship != null) {
            log.info("Active scholarship found: {}", activeScholarship.getScholarshipType().getName());
            BigDecimal discountAmount = BigDecimal.ZERO;
            if ("PERCENTAGE".equalsIgnoreCase(activeScholarship.getDiscountType())) {
                discountAmount = totalAmount.multiply(activeScholarship.getDiscountValue())
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if ("FIXED".equalsIgnoreCase(activeScholarship.getDiscountType())) {
                discountAmount = activeScholarship.getDiscountValue();
            }

            if (discountAmount.compareTo(BigDecimal.ZERO) > 0) {
                InvoiceLineItem discountItem = new InvoiceLineItem();
                discountItem.setDescription("Scholarship Discount (" + activeScholarship.getScholarshipType().getName() + ")");
                discountItem.setAmount(discountAmount.negate());
                invoice.addLineItem(discountItem);

                totalAmount = totalAmount.subtract(discountAmount);
                invoice.setTotalAmount(totalAmount);

                // Atomically increment total discount in DB — bypasses proxy/cache issues
                scholarshipTypeRepository.incrementTotalDiscountIssued(
                        activeScholarship.getScholarshipType().getId(), discountAmount);
                
                log.info("Incremented totalDiscountIssued for scholarshipType id={} by {}",
                        activeScholarship.getScholarshipType().getId(), discountAmount);
                log.info("Applied scholarship discount: {} to invoice {}", discountAmount, invoice.getInvoiceNumber());
            }
        }

        // 8. Save the invoice (and its line items via CascadeType.ALL)
        Invoice savedInvoice = invoiceRepository.save(invoice);

        // 8. Map to DTO and return
        return invoiceMapper.toDto(savedInvoice);
    }

    @Override
    @Transactional
    public Page<InvoiceResponseDTO> getAllInvoices(Pageable pageable) {
        Page<Invoice> invoicePage = invoiceRepository.findAll(pageable);
        return invoicePage.map(invoiceMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with invoice Id:" + invoiceId));
        return invoiceMapper.toDto(invoice);
    }


    @Override
    @Transactional(readOnly = true)
    public byte[] getInvoiceReceipt(Long invoiceId) {
        log.info("Generating receipt for invoiceId: {}", invoiceId);

        // 1. Find the Invoice
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with Invoice ID: " + invoiceId));

        // 2. Validate Status
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new InvalidPaymentOperationException("Cannot generate receipt for unpaid invoice: " + invoiceId);
        }

        // 3. Find the associated Payment record(s)
        List<Payment> payments = paymentRepository.findByInvoice(invoice);
        if (payments.isEmpty()) {
            // This should not happen if status is PAID, but a good safe-guard.
            throw new InvalidPaymentOperationException("No payment records found for paid invoice: " + invoiceId);
        }
        // We'll use the *first* successful payment for the receipt details
        Payment payment = payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentOperationException("No successful payment found for invoice: " + invoiceId));

        // 4. Get Student and Profile data
        Student student = invoice.getStudent();
        UserProfile profile = student.getUserProfile();

        // 5. TODO: Implement Security Check
        // Check if the authenticated user (from SecurityContextHolder)
        // is a guardian of this 'student'.
        // if (!isUserGuardianOf(student)) {
        //    throw new AccessDeniedException("You are not authorized to view this receipt.");
        // }

        // 6. Build the data map for Thymeleaf
        Map<String, Object> data = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // Receipt/Payment Info
        data.put("receiptNo", payment.getPaymentId().toString());
        data.put("paymentDate", payment.getPaymentDate().format(dtf));
        data.put("payMode", payment.getPaymentMethod().toString());
        data.put("bankName", "HDFC BANK"); // Hardcoded as per image
        data.put("paymentNumber", payment.getTransactionId()); // e.g., Cheque number
        data.put("counterNo", "DPS-RECEIPT"); // Hardcoded as per image
        data.put("note", "356"); // Hardcoded as per image

        // Student/School Info
        data.put("studentName", profile.getFirstName() + " " + profile.getLastName());
        data.put("admissionNumber", student.getEnrollmentNumber());
        // TODO: Change it to real values;
        data.put("session", "2024-2025");
        // data.put("session", invoice.getFeeStructure().getAcademicYear()); // Assumes relation
        data.put("className", student.getSection().getSectionName()); // Assumes relation
        data.put("installmentName", "JULY-SEP"); // Hardcoded as per image

        // Financials
        data.put("lineItems", invoice.getLineItems());
        data.put("totalAmount", invoice.getTotalAmount());
        // ── BUG FIX 5: Use HALF_UP rounding to avoid truncating paise. ──────────
        // Without rounding, ₹1,000.75 would become "One Thousand Rupees Only".
        long totalRupees = invoice.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue();
        data.put("totalInWords", numberToWordsConverter.convertToWords(totalRupees));
        // ────────────────────────────────────────────────────────────────────────

        // 7. Call the PDF service
        return pdfGenerationService.generatePdfFromHtml("receipt", data);
    }


    @Override
    @Transactional(readOnly = true)
    public List<InvoiceResponseDTO> getInvoicesForStudent(Long studentId) {
        // 1. Find the student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with ID: " + studentId));

        // 2. Find all invoices for that one student
        return invoiceRepository.findByStudent(student).stream()
                .map(invoiceMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponseDTO getInvoiceByIdForParent(Long invoiceId) {
        // 1. Find the invoice by its ID
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with ID: " + invoiceId));

        // 2. SECURITY WARNING: We are skipping the check for parent ownership
        // Todo:  add this later.

        // 3. Map and return
        return invoiceMapper.toDto(invoice);
    }


    @Override
    @Transactional
    public InvoiceResponseDTO applyLateFee(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with Invoice ID:  " + invoiceId));

        // 1. Validation Checks
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Cannot apply late fee to a " + invoice.getStatus() + " invoice.");
        }
        if (invoice.getDueDate().isAfter(LocalDate.now())) {
            throw new InvalidPaymentOperationException("Cannot apply late fee to an invoice that is not yet overdue.");
        }
        // Check if a late fee has already been applied
        if (invoice.getLateFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new InvalidPaymentOperationException("A late fee has already been applied to this invoice.");
        }

        // ── BUG FIX 6: Replace hardcoded ₹250 with DB-driven LateFeeRule ────────
        // Fetch the first active rule. Throw a meaningful error if none is configured.
        LateFeeRule rule = lateFeeRuleRepository.findByIsActive(true)
                .stream()
                .findFirst()
                .orElseThrow(() -> new InvalidPaymentOperationException(
                        "No active late fee rule is configured. Please create one under Late Fee Policies."));

        BigDecimal lateFee;
        if (rule.getFineType() == FineType.FIXED) {
            lateFee = rule.getFineValue();
        } else {
            // PERCENTAGE: calculate against the original invoice amount (before late fee)
            lateFee = invoice.getTotalAmount()
                    .multiply(rule.getFineValue())
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        log.info("Applying late fee of {} (rule: {}, type: {}) to invoice {}",
                lateFee, rule.getRuleName(), rule.getFineType(), invoiceId);
        // ────────────────────────────────────────────────────────────────────────

        // 3. Create a new line item for the fee
        InvoiceLineItem feeLineItem = new InvoiceLineItem();
        feeLineItem.setDescription("Late Payment Fee (" + rule.getRuleName() + ")");
        feeLineItem.setAmount(lateFee);

        // 4. Add line item and update totals
        invoice.addLineItem(feeLineItem);
        invoice.setLateFeeAmount(lateFee);
        invoice.setTotalAmount(invoice.getTotalAmount().add(lateFee));
        invoice.setStatus(InvoiceStatus.OVERDUE); // Ensure it's marked as overdue

        // 5. Save and return DTO
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return invoiceMapper.toDto(updatedInvoice);
    }


    @Override
    @Transactional
    public InvoiceResponseDTO cancelInvoice(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with Invoice ID:  " + invoiceId));

        // 1. Validation Checks
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidPaymentOperationException("Cannot cancel an invoice that has already been paid.");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Invoice is already cancelled.");
        }

        // 2. Set status to CANCELLED
        invoice.setStatus(InvoiceStatus.CANCELLED);

        // 3. Save and return DTO
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return invoiceMapper.toDto(updatedInvoice);
    }

    // --- Private Helper Methods ---

    /**
     * Generates a unique invoice number.
     * TODO: This should be a robust, sequence-based generator.
     */
    private String generateInvoiceNumber() {
        // Simple, non-production-ready generator
        return "INV-" + System.currentTimeMillis();
    }
}
