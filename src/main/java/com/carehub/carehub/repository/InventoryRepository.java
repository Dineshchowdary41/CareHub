package com.carehub.carehub.repository;
import com.carehub.carehub.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    List<Inventory> findByMedicine_MedicineId(Long medicineId);

    @Query("SELECT i FROM Inventory i WHERE i.availableStock <= i.minimumStock")
    List<Inventory> findLowStock();

    @Query("SELECT i FROM Inventory i WHERE i.expiryDate BETWEEN :start AND :end ORDER BY i.expiryDate ASC")
    List<Inventory> findExpiringBetween(LocalDate start, LocalDate end);

    @Query("SELECT i FROM Inventory i WHERE i.expiryDate < :today")
    List<Inventory> findAlreadyExpired(LocalDate today);

    @Query("SELECT COALESCE(SUM(i.availableStock * i.unitPrice), 0) FROM Inventory i")
    java.math.BigDecimal totalInventoryValue();
}
