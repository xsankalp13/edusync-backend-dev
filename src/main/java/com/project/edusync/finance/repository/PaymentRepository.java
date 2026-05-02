package com.project.edusync.finance.repository;


import com.project.edusync.finance.model.entity.Invoice;
import com.project.edusync.finance.model.entity.Payment;
import com.project.edusync.uis.model.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for the {@link Payment} entity.
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    /**
     * Finds all payments associated with a specific invoice.
     *
     * @param invoice The Invoice entity.
     * @return A list of payments for that invoice.
     */
    List<Payment> findByInvoice(Invoice invoice);
    List<Payment> findByInvoice_Id(Long invoiceId);

    /**
     * Finds all payments made by a specific student.
     *
     * @param student The Student entity.
     * @return A list of all payments from that student.
     */
    List<Payment> findByStudent(Student student);

    /**
     * Finds a payment by its unique payment gateway transaction ID.
     *
     * @param transactionId The ID from the payment provider.
     * @return An Optional containing the found Payment, or empty if not found.
     */
    Optional<Payment> findByTransactionId(String transactionId);

    /**
     * Calculates the total collected amount from all SUCCESSFUL payments.
     */
    @Query("SELECT COALESCE(SUM(p.amountPaid), 0) " +
            "FROM Payment p " +
            "WHERE p.status = 'SUCCESS'")
    BigDecimal findTotalCollected();

    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND YEAR(p.paymentDate) = :year
              AND MONTH(p.paymentDate) = :month
            """)
    BigDecimal sumCollectedByYearMonth(@Param("year") int year, @Param("month") int month);

    /**
     * Projection for monthly collected payment totals.
     */
    interface MonthlyPaymentSumProjection {
        Integer getPaymentYear();
        Integer getPaymentMonth();
        java.math.BigDecimal getCollectedTotal();
    }

    /**
     * Returns (paymentYear, paymentMonth, collectedTotal) for each month in range.
     * Replaces 6 individual sumCollectedByYearMonth calls in MasterDashboardAnalyticsServiceImpl.
     * 1 query instead of 6.
     */
    @Query("""
            SELECT YEAR(p.paymentDate) as paymentYear,
                   MONTH(p.paymentDate) as paymentMonth,
                   COALESCE(SUM(p.amountPaid), 0) as collectedTotal
            FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND p.paymentDate >= :startDate
              AND p.paymentDate <= :endDate
            GROUP BY YEAR(p.paymentDate), MONTH(p.paymentDate)
            ORDER BY YEAR(p.paymentDate), MONTH(p.paymentDate)
            """)
    java.util.List<MonthlyPaymentSumProjection> sumCollectedGroupedByMonth(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );

    /**
     * Returns total collected payments in an arbitrary date-time range (for MTD comparisons).
     */
    @Query("""
            SELECT COALESCE(SUM(p.amountPaid), 0)
            FROM Payment p
            WHERE p.status = 'SUCCESS'
              AND p.paymentDate >= :startDate
              AND p.paymentDate <= :endDate
            """)
    java.math.BigDecimal sumCollectedByDateRange(
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate
    );
}