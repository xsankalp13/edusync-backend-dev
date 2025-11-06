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
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.finance.repository.PaymentRepository;
import com.project.edusync.finance.service.PaymentService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Value("${app.razorpay.key-id}")
    private String razorpayKeyId;
    @Value("${app.razorpay.key-secret}")
    private String razorpayKeySecret;

    @Override
    @Transactional
    public PaymentResponseDTO recordOfflinePayment(RecordOfflinePaymentDTO createDTO) {

        // 1. Find the related entities
        Invoice invoice = invoiceRepository.findById(createDTO.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with invoice ID: " + createDTO.getInvoiceId()));

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

        // 6. Return the DTO for the new payment
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

        // Update parent Invoice
        Invoice invoice = payment.getInvoice();
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

        return paymentMapper.toDto(savedPayment);
    }

}