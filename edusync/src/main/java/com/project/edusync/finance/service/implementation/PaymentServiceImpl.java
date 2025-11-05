package com.project.edusync.finance.service.implementation;

import com.project.edusync.common.exception.finance.InvalidPaymentOperationException;
import com.project.edusync.common.exception.finance.InvoiceNotFoundException;
import com.project.edusync.common.exception.finance.StudentNotFoundException;
import com.project.edusync.finance.dto.payment.PaymentResponseDTO;
import com.project.edusync.finance.dto.payment.RecordOfflinePaymentDTO;
import com.project.edusync.finance.mapper.PaymentMapper;
import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.entity.Payment;
import com.project.edusync.finance.model.enums.InvoiceStatus;
import com.project.edusync.finance.model.enums.PaymentStatus;
import com.project.edusync.finance.repository.InvoiceRepository;
import com.project.edusync.finance.repository.PaymentRepository;
import com.project.edusync.finance.service.PaymentService;
import com.project.edusync.uis.model.entity.Student;
import com.project.edusync.uis.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final StudentRepository studentRepository;
    private final PaymentMapper paymentMapper;
    private final ModelMapper modelMapper;

    @Override
    @Transactional
    public PaymentResponseDTO recordOfflinePayment(RecordOfflinePaymentDTO createDTO) {

        // 1. Find the related entities
        Invoice invoice = invoiceRepository.findById(createDTO.getInvoiceId())
                .orElseThrow(() -> new InvoiceNotFoundException("Invoice not found with Id: " + createDTO.getInvoiceId()));

        Student student = studentRepository.findById(createDTO.getStudentId())
                .orElseThrow(() -> new StudentNotFoundException("Invoice not found with Id: " + createDTO.getStudentId()));

        // 2. Perform Validation
        if (!invoice.getStudent().getId().equals(student.getId())) {
            throw new InvalidPaymentOperationException("Invoice studentId does not match provided studentId.");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new InvalidPaymentOperationException("Invoice is already " + invoice.getStatus());
        }

        // 3. Create the new Payment entity
        Payment payment = modelMapper.map(createDTO, Payment.class);
        payment.setStudent(student);
        payment.setInvoice(invoice);
        payment.setStatus(PaymentStatus.SUCCESS); // Offline is considered an immediate success

        // Use provided payment date or set to now
        if (createDTO.getPaymentDate() == null) {
            payment.setPaymentDate(LocalDateTime.now());
        }

        // 4. Update the Invoice
        BigDecimal newPaidAmount = invoice.getPaidAmount().add(createDTO.getAmountPaid());
        invoice.setPaidAmount(newPaidAmount);

        // Check if this payment makes the invoice fully paid
        if (newPaidAmount.compareTo(invoice.getTotalAmount()) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            // If it's a partial payment, just mark it as pending (or a new "PARTIAL" status)
            // For now, we keep it PENDING until fully paid.
            invoice.setStatus(InvoiceStatus.PENDING); // Or OVERDUE if dueDate is past
        }

        // 5. Save both entities in the transaction
        invoiceRepository.save(invoice);
        Payment savedPayment = paymentRepository.save(payment);

        // 6. Return the DTO for the new payment
        return paymentMapper.toDto(savedPayment);
    }
}
