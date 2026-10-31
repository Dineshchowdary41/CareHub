package com.carehub.carehub.repository;
import com.carehub.carehub.entity.PrescriptionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {
    List<PrescriptionItem> findByPrescription_PrescriptionId(Long prescriptionId);
}
