package com.carehub.carehub.repository;
import com.carehub.carehub.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findByStatus(String status);
    List<PurchaseOrder> findByMedicine_MedicineIdAndStatus(Long medicineId, String status);
}
