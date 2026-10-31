package com.carehub.carehub.repository;
import com.carehub.carehub.entity.BillItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDateTime;
import java.util.List;

public interface BillItemRepository extends JpaRepository<BillItem, Long> {
    List<BillItem> findByBill_BillId(Long billId);

    @Query("SELECT bi FROM BillItem bi WHERE bi.medicine.medicineId = :medicineId AND bi.bill.billDate >= :since")
    List<BillItem> findByMedicineSince(Long medicineId, LocalDateTime since);

    @Query("SELECT bi.medicine.medicineId, bi.medicine.medicineName, COALESCE(SUM(bi.quantity), 0) " +
           "FROM BillItem bi WHERE bi.bill.billDate >= :since GROUP BY bi.medicine.medicineId, bi.medicine.medicineName " +
           "ORDER BY SUM(bi.quantity) DESC")
    List<Object[]> findUsageSince(LocalDateTime since);

    @Query("SELECT COALESCE(SUM(bi.totalPrice), 0) FROM BillItem bi WHERE bi.bill.billDate >= :since")
    java.math.BigDecimal revenueSince(LocalDateTime since);
}
