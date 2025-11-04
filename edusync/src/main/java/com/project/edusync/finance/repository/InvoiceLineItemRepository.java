package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.InvoiceLineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for the {@link InvoiceLineItem} entity.
 * Operations are typically cascaded from the InvoiceRepository.
 */
@Repository
public interface InvoiceLineItemRepository extends JpaRepository<InvoiceLineItem, Long> {

    // Custom queries are rare here, but you could add:
    // List<InvoiceLineItem> findByInvoice_InvoiceId(Integer invoiceId);
}
