package com.carehub.carehub.repository;
import com.carehub.carehub.entity.Bill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface BillRepository extends JpaRepository<Bill, Long> {
    List<Bill> findByPatient_PatientId(Long patientId);

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b")
    java.math.BigDecimal totalRevenue();

    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b WHERE b.paymentStatus <> 'Paid'")
    java.math.BigDecimal totalPending();

    @Query("SELECT COUNT(b) FROM Bill b WHERE b.paymentStatus <> 'Paid'")
    long countPendingBills();

    @Query("SELECT FUNCTION('DATE_FORMAT', b.billDate, '%Y-%m') AS ym, COALESCE(SUM(b.totalAmount), 0) " +
           "FROM Bill b WHERE b.billDate >= :since GROUP BY ym ORDER BY ym")
    List<Object[]> monthlyRevenueSince(LocalDateTime since);
}
