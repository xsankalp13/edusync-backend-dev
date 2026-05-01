package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.InvalidPaymentOperationException;
import com.project.edusync.common.exception.finance.InvoiceNotFoundException;
import com.project.edusync.common.exception.finance.PaymentNotFoundException;
import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.payment.*;
import com.project.edusync.finance.mapper.PaymentMapper;
import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.entity.Payment;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.finance.model.enums.PaymentMethod;
import com.project.edusync.finance.model.enums.PaymentStatus;
import com.project.edusync.common.settings.service.AppSettingService;
import com.project.edusync.finance.model.enums.JournalReferenceType;
import java.time.Month;
import java.time.LocalDate;
import com.project.edusync.finance.repository.AccountRepository;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.finance.repository.PaymentRepository;
import com.project.edusync.finance.service.GeneralLedgerService;
import com.project.edusync.finance.service.PaymentService;
import com.project.edusync.dashboard.model.DashboardEvent;
import com.project.edusync.dashboard.service.DashboardEventService;
import com.project.edusync.finance.service.PdfGenerationService;
import com.project.edusync.finance.utils.NumberToWordsConverter;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.model.entity.UserProfile;
import com.project.edusync.uis.repository.StudentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final PaymentMapper paymentMapper;
    private final ModelMapper modelMapper; // We keep it for other future methods
    private final RazorpayClient razorpayClient;
    private final DashboardEventService dashboardEventService;
    private final GeneralLedgerService generalLedgerService;
    private final AccountRepository accountRepository;
    private final PdfGenerationService pdfGenerationService;
    private final NumberToWordsConverter numberToWordsConverter;
    private final AppSettingService appSettingService;

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;
    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    /**
     * GL account codes used for payment auto-posting.
     * These must exist in the COA (seeded via AccountController /seed endpoint).
     * 1120 = Bank — Main Account (debit on payment received)
     * 1130 = Online Gateway Float (debit on online payment)
     * 4110 = Tuition Fee Revenue (credit — generic fee income)
     */
    private static final String BANK_ACCOUNT_CODE      = "1120";
    private static final String GATEWAY_ACCOUNT_CODE   = "1130";
    private static final String FEE_REVENUE_CODE       = "4110";
    private static final Long   DEFAULT_SCHOOL_ID      = 1L;

    @Override
    @Transactional
    @CacheEvict(value = {"dashboard", "dashboardOverview"}, allEntries = true)
    public PaymentResponseDTO recordOfflinePayment(RecordOfflinePaymentDTO createDTO) {

        // 1. Find the related entities
        // ── BUG FIX 2: Use pessimistic write lock to prevent race condition. ──────
        // Two concurrent payments for the same invoice could both read the same
        // paidAmount, add their payment, and overwrite each other. The lock ensures
        // only one transaction can modify the invoice row at a time.
        Invoice invoice = invoiceRepository.findByIdWithLock(createDTO.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with invoice ID: " + createDTO.getInvoiceId()));
        // ────────────────────────────────────────────────────────────────────────

        Student student = studentRepository.findById(createDTO.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Student not found with student ID: " + createDTO.getStudentId()));

        // 2. Perform Validation
        if (!invoice.getStudent().getId().equals(student.getId())) {
            throw new InvalidPaymentOperationException("Invoice studentId does not match provided studentId.");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Invoice is already " + invoice.getStatus());
        }

        // --- FIX: Reverted to manual mapping for entity creation ---
        // 3. Create the new Payment entity manually
        Payment payment = new Payment();
        payment.setStudent(student);
        payment.setInvoice(invoice);
        payment.setAmountPaid(createDTO.getAmountPaid());
        payment.setPaymentMethod(createDTO.getPaymentMethod());
        payment.setTransactionId(createDTO.getTransactionId());
        payment.setNotes(createDTO.getNotes());
        payment.setStatus(PaymentStatus.SUCCESS); // Offline is considered an immediate success

        // Use provided payment date or set to now
        if (createDTO.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        } else {
            payment.setPaymentDate(createDTO.getPaymentDate());
        }
        // --- END OF FIX ---

        // 4. Update the Invoice
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(createDTO.getAmountPaid());
        invoice.setPaidAmount(newPaidAmount);

        // Check if this payment makes the invoice fully paid
        // We use compareTo: 0 (equal), 1 (newPaidAmount is greater)
        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            // If it's a partial payment, just mark it as pending (or a new "PARTIAL" status)
            invoice.setStatus(InvoiceStatus.PENDING);
        }

        // 5. Save both entities in the transaction
        invoiceRepository.save(invoice);
        Payment savedPayment = paymentRepository.save(payment);

        // Emit dashboard event
        DashboardEvent event = DashboardEvent.builder()
                .type("finance")
                .severity("info")
                .title("Offline Payment Collected")
                .message("₹" + createDTO.getAmountPaid() + " collected from " + student.getUserProfile().getFirstName() + " for invoice #" + invoice.getId())
                .metadata(java.util.Map.of("invoiceId", invoice.getId(), "studentId", student.getId(), "amount", createDTO.getAmountPaid(), "status", "SUCCESS"))
                .build();
        dashboardEventService.pushEvent(event);

        // 6. Auto-post GL double-entry: Debit Bank, Credit Fee Revenue
        tryPostGLEntry(savedPayment, payment.getPaymentMethod() == PaymentMethod.ONLINE
                ? GATEWAY_ACCOUNT_CODE : BANK_ACCOUNT_CODE, FEE_REVENUE_CODE);

        // 7. Return the DTO for the new payment
        return paymentMapper.toDto(savedPayment);
    }


    @Override
    @Transactional(readOnly = true)
    public Page<PaymentResponseDTO> getAllPayments(Pageable pageable) {
        Page<Payment> paymentPage = paymentRepository.findAll(pageable);
        return paymentPage.map(paymentMapper::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponseDTO getPaymentById(Long paymentId) {
        Payment payment = findPaymentById(paymentId);
        return paymentMapper.toDto(payment);
    }

    @Override
    @Transactional
    public PaymentResponseDTO updatePayment(Long paymentId, PaymentUpdateDTO updateDTO) {
        // 1. Find the existing payment
        Payment existingPayment = findPaymentById(paymentId);

        // 2. Use ModelMapper to apply the non-null updates from the DTO
        modelMapper.map(updateDTO, existingPayment);
        // Note: We might need to configure ModelMapper to skip nulls if
        // we want this to be a PATCH, but for a PUT this is fine.

        // 3. Save the updated entity
        Payment updatedPayment = paymentRepository.save(existingPayment);

        // 4. Return the response DTO
        return paymentMapper.toDto(updatedPayment);
    }

    private Payment findPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with Payment ID: " + paymentId));
    }

    @Override
    @Transactional
    public InitiatePaymentResponseDTO initiateOnlinePayment(InitiatePaymentRequestDTO requestDTO) throws Exception {
        log.info("Initiating payment for invoiceId: {}", requestDTO.getInvoiceId());

        Invoice invoice = invoiceRepository.findById(requestDTO.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with ID: " + requestDTO.getInvoiceId()));

        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new InvalidPaymentOperationException("Invoice is already paid.");
        }

        BigDecimal amountDue = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        if (requestDTO.getAmount().compareTo(amountDue) > 0) {
            throw new InvalidPaymentOperationException("Payment amount (" + requestDTO.getAmount() +
                    ") cannot be greater than the outstanding amount due (" + amountDue + ").");
        }
        if (requestDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPaymentOperationException("Payment amount must be positive.");
        }

        log.info("Creating PENDING payment record for amount: {}", requestDTO.getAmount());
        Payment payment = new Payment();
        payment.setStudent(invoice.getStudent());
        payment.setInvoice(invoice);
        payment.setAmountPaid(requestDTO.getAmount());
        payment.setPaymentMethod(PaymentMethod.ONLINE);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setPaymentDate(LocalDateTime.now());

        Payment pendingPayment = paymentRepository.save(payment);

        long amountInPaise = requestDTO.getAmount().multiply(new BigDecimal(100)).longValue();
        log.info("Creating Razorpay Order for amount: {} paise, receiptId: {}", amountInPaise, pendingPayment.getPaymentId());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put("receipt", pendingPayment.getPaymentId().toString());

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");
        log.info("Razorpay Order created: {}", razorpayOrderId);

        pendingPayment.setTransactionId(razorpayOrderId); // Store Order ID temporarily
        paymentRepository.save(pendingPayment);

        // We return the orderId and the public keyId. The 'clientSecret' field is repurposed.
        return new InitiatePaymentResponseDTO(razorpayKeyId, null, razorpayOrderId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"dashboard", "dashboardOverview"}, allEntries = true)
    public PaymentResponseDTO verifyOnlinePayment(VerifyPaymentRequestDTO verifyDTO) throws Exception {
        log.info("Verifying payment for Razorpay Order ID: {}", verifyDTO.getOrderId());

        Payment payment = paymentRepository.findByTransactionId(verifyDTO.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found" + 0L)); // Using 0L as a placeholder

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentOperationException("Payment is not in a pending state. Current status: " + payment.getStatus());
        }

        JSONObject options = new JSONObject();
        options.put("razorpay_order_id", verifyDTO.getOrderId());
        options.put("razorpay_payment_id", verifyDTO.getGatewayTransactionId());
        options.put("razorpay_signature", verifyDTO.getSignature());

        boolean isSignatureValid = Utils.verifyPaymentSignature(options, this.razorpayKeySecret);

        if (!isSignatureValid) {
            log.warn("Payment verification FAILED for Order ID: {}", verifyDTO.getOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new SecurityException("Invalid payment signature. Payment verification failed.");
        }

        log.info("Payment verification SUCCESS for Order ID: {}", verifyDTO.getOrderId());

        // Update Payment record
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId(verifyDTO.getGatewayTransactionId()); // Store the final 'pay_...' ID
        payment.setPaymentDate(LocalDateTime.now()); // Mark the actual time of verification

        // ── BUG FIX 2: Reload Invoice with pessimistic lock before updating balance ──
        // The invoice reference inside `payment` was loaded earlier without a lock.
        // Re-fetch it now with the write lock to prevent any concurrent payment
        // from overwriting our balance update.
        Invoice invoice = invoiceRepository.findByIdWithLock(payment.getInvoice().getId())
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found during payment verification."));
        // ────────────────────────────────────────────────────────────────────────
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(payment.getAmountPaid());
        invoice.setPaidAmount(newPaidAmount);

        // Check for partial or full payment
        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
            log.info("Invoice {} is now fully PAID.", invoice.getId());
        } else {
            log.info("Invoice {} is now partially paid. Total paid: {}", invoice.getId(), newPaidAmount);
        }

        invoiceRepository.save(invoice);
        Payment savedPayment = paymentRepository.save(payment);

        DashboardEvent event = DashboardEvent.builder()
                .type("finance")
                .severity("info")
                .title("Online Payment Verified")
                .message("₹" + payment.getAmountPaid() + " verified online for invoice #" + invoice.getId())
                .metadata(java.util.Map.of("invoiceId", invoice.getId(), "transactionId", verifyDTO.getGatewayTransactionId(), "amount", payment.getAmountPaid(), "status", "SUCCESS"))
                .build();
        dashboardEventService.pushEvent(event);

        // Auto-post GL double-entry: Debit Online Gateway Float, Credit Fee Revenue
        tryPostGLEntry(savedPayment, GATEWAY_ACCOUNT_CODE, FEE_REVENUE_CODE);

        return paymentMapper.toDto(savedPayment);
    }

    /**
     * Posts a GL journal entry for a successful payment.
     * Fails silently with a warning log to avoid rolling back the payment transaction.
     * GL can be reconciled later if needed.
     */
    private void tryPostGLEntry(Payment payment, String debitAccountCode, String creditAccountCode) {
        try {
            var debitOpt  = accountRepository.findByCodeAndSchoolId(debitAccountCode,  DEFAULT_SCHOOL_ID);
            var creditOpt = accountRepository.findByCodeAndSchoolId(creditAccountCode, DEFAULT_SCHOOL_ID);
            if (debitOpt.isEmpty() || creditOpt.isEmpty()) {
                log.warn("GL auto-post skipped for Payment #{}: COA not yet seeded.", payment.getPaymentId());
                return;
            }
            generalLedgerService.autoPostEntry(
                    payment.getPaymentDate() != null ? payment.getPaymentDate().toLocalDate() : java.time.LocalDate.now(),
                    "Fee Payment — Invoice #" + payment.getInvoice().getId() + " | Student STU-" + payment.getStudent().getId(),
                    JournalReferenceType.PAYMENT,
                    payment.getPaymentId().longValue(),
                    debitOpt.get().getId(),
                    creditOpt.get().getId(),
                    payment.getAmountPaid(),
                    DEFAULT_SCHOOL_ID
            );
            log.info("GL entry posted for Payment #{}", payment.getPaymentId());
        } catch (Exception ex) {
            log.error("GL auto-post FAILED for Payment #{}: {}", payment.getPaymentId(), ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsForStudent(Long studentId) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new StudentNotFoundException("Student not found with student ID: " + studentId));

        List<Payment> payments = paymentRepository.findByStudent(student);
        return payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.SUCCESS)
                .map(paymentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponseDTO> getPaymentsByInvoiceId(Long invoiceId) {
        List<Payment> payments = paymentRepository.findByInvoice_Id(invoiceId);
        return payments.stream()
                .map(paymentMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] getPaymentReceipt(Long paymentId) {
        log.info("Generating receipt for paymentId: {}", paymentId);

        // 1. Find the Payment
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found with ID: " + paymentId));

        // 2. Find the Invoice
        Invoice invoice = payment.getInvoice();
        Student student = payment.getStudent();
        UserProfile profile = student.getUserProfile();

        // 3. Build data map
        Map<String, Object> data = new HashMap<>();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        // School branding from AppSettings
        populateSchoolBranding(data);

        data.put("receiptNo", payment.getPaymentId().toString());
        data.put("paymentDate", payment.getPaymentDate().format(dtf));
        data.put("payMode", payment.getPaymentMethod().toString());
        data.put("bankName", appSettingService.getValue("school.bank_name", "HDFC BANK"));
        data.put("paymentNumber", payment.getTransactionId());
        data.put("counterNo", "DPS-RECEIPT");
        data.put("note", "Partial Payment for Invoice #" + invoice.getInvoiceNumber());

        data.put("studentName", profile.getFirstName() + " " + profile.getLastName());
        data.put("admissionNumber", student.getEnrollmentNumber());
        data.put("session", computeAcademicYear());
        data.put("className", student.getSection().getAcademicClass().getName() + " - " + student.getSection().getSectionName());

        // For a payment receipt, we show the amount paid in this transaction
        data.put("lineItems", List.of(Map.of(
            "description", "Partial Fee Payment",
            "due", payment.getAmountPaid(),
            "con", BigDecimal.ZERO,
            "paid", payment.getAmountPaid()
        )));
        data.put("totalAmount", payment.getAmountPaid());
        
        long totalRupees = payment.getAmountPaid().setScale(0, RoundingMode.HALF_UP).longValue();
        data.put("totalInWords", numberToWordsConverter.convertToWords(totalRupees));

        return pdfGenerationService.generatePdfFromHtml("receipt", data);
    }

    private void populateSchoolBranding(Map<String, Object> data) {
        data.put("schoolName", appSettingService.getValue("school.name", "My School"));
        data.put("schoolAddress", appSettingService.getValue("school.address", ""));
        data.put("schoolPhone", appSettingService.getValue("school.phone", ""));
        data.put("schoolEmail", appSettingService.getValue("school.email", ""));
        data.put("schoolWebsite", appSettingService.getValue("school.website", ""));

        // Branding
        data.put("schoolName", appSettingService.getValue("school.name", "Shiksha Intelligence"));
        data.put("schoolAddress", appSettingService.getValue("school.address", "Site No.1, Sector-45, Urban Estate, Gurgaon, Haryana"));

        // Logo
        String logoUrl = appSettingService.getValue("school.logo_url", "");
        if (logoUrl != null && !logoUrl.isBlank()) {
            data.put("schoolLogoBase64", pdfGenerationService.fetchRemoteImageAsBase64(logoUrl));
        } else {
            data.put("schoolLogoBase64", pdfGenerationService.loadSchoolLogoBase64());
        }

        // Signature
        String signatureUrl = appSettingService.getValue("school.signature_url", "");
        if (signatureUrl != null && !signatureUrl.isBlank()) {
            data.put("signatureBase64", pdfGenerationService.fetchRemoteImageAsBase64(signatureUrl));
        } else {
            data.put("signatureBase64", "");
        }
    }

    private String computeAcademicYear() {
        String startMonthStr = appSettingService.getValue("school.academic_year_start", "APRIL");
        Month startMonth;
        try {
            startMonth = Month.valueOf(startMonthStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            startMonth = Month.APRIL;
        }

        LocalDate now = LocalDate.now();
        int startYear = now.getMonthValue() >= startMonth.getValue() ? now.getYear() : now.getYear() - 1;
        return startYear + "-" + (startYear + 1);
    }
}

