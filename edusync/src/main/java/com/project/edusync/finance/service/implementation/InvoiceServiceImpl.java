package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.InvalidPaymentOperationException;
import com.project.edusync.common.exception.finance.InvoiceNotFoundException;
import com.project.edusync.common.exception.finance.StudentFeeMapNotFoundException;
import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import com.project.edusync.finance.mapper.InvoiceMapper;
import com.project.edusync.finance.model.entity.*;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final PaymentRepository paymentRepository;
    private final StudentFeeMapRepository studentFeeMapRepository;
    private final FeeParticularRepository feeParticularRepository;
    private final InvoiceMapper invoiceMapper;

    private final PdfGenerationService pdfGenerationService;
    private final NumberToWordsConverter numberToWordsConverter;
    // We don't need InvoiceLineItemRepository, as it will be saved by cascade.

    @Override
    @Transactional
    public InvoiceResponseDTO generateSingleInvoice(Long studentId) {
        // 1. Find the Student
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with Student ID: " + studentId));

        // 2. Find the Student's Fee Map
        StudentFeeMap feeMap = studentFeeMapRepository.findByStudent_Id(studentId)
                .orElseThrow(() -> new StudentFeeMapNotFoundException("Student not Found with Student ID: " + studentId));

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

        // 7. Save the invoice (and its line items via CascadeType.ALL)
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
        data.put("totalInWords", numberToWordsConverter.convertToWords(invoice.getTotalAmount().longValue()));

        // 7. Call the PDF service
        return pdfGenerationService.generatePdfFromHtml("receipt", data);
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
