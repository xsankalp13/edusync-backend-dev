package com.project.edusync.finance.mapper;

import com.project.edusync.finance.dto.invoice.InvoiceLineItemResponseDTO;
import com.project.edusync.finance.dto.invoice.InvoiceResponseDTO;
import com.project.edusync.finance.model.entity.Invoice;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InvoiceMapper {

    private final ModelMapper modelMapper;

    /**
     * Converts an Invoice entity to an InvoiceResponseDTO.
     * Manually maps the studentId and the list of line items.
     */
    public InvoiceResponseDTO toDto(Invoice invoice) {
        // 1. Use ModelMapper for all flat fields
        InvoiceResponseDTO dto = modelMapper.map(invoice, InvoiceResponseDTO.class);

        // 2. Manually map student ID
        if (invoice.getStudent() != null) {
            dto.setStudentId(invoice.getStudent().getId());
            if (invoice.getStudent().getUserProfile() != null) {
                dto.setStudentName(invoice.getStudent().getUserProfile().getFirstName() + " " + 
                                  invoice.getStudent().getUserProfile().getLastName());
            }
        }

        // 3. Manually map the list of line items
        if (invoice.getLineItems() != null) {
            dto.setLineItems(
                    invoice.getLineItems().stream()
                            .map(lineItem -> modelMapper.map(lineItem, InvoiceLineItemResponseDTO.class))
                            .collect(Collectors.toList())
            );
        }

        return dto;
    }
}
