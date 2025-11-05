package com.project.edusync.finance.mapper;

import com.project.edusync.finance.dto.payment.PaymentResponseDTO;
import com.project.edusync.finance.model.entity.Payment;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMapper {

    private final ModelMapper modelMapper;

    public PaymentResponseDTO toDto(Payment payment) {
        // 1. Use ModelMapper for flat fields
        PaymentResponseDTO dto = modelMapper.map(payment, PaymentResponseDTO.class);

        // 2. Manually map relationship IDs
        if (payment.getInvoice() != null) {
            dto.setInvoiceId(payment.getInvoice().getId());
        }
        if (payment.getStudent() != null) {
            dto.setStudentId(payment.getStudent().getId());
        }

        return dto;
    }
}