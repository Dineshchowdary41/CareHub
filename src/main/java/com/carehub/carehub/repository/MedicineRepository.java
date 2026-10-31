package com.carehub.carehub.repository;
import com.carehub.carehub.entity.Medicine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MedicineRepository extends JpaRepository<Medicine, Long> {
    List<Medicine> findByMedicineNameContainingIgnoreCase(String name);
    List<Medicine> findByCategoryIgnoreCase(String category);

    @Query("SELECT DISTINCT m.category FROM Medicine m WHERE m.category IS NOT NULL AND m.category <> '' ORDER BY m.category")
    List<String> findDistinctCategories();

    @Query("SELECT m FROM Medicine m WHERE " +
           "(:query IS NULL OR LOWER(m.medicineName) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "(:category IS NULL OR LOWER(m.category) = LOWER(:category))")
    List<Medicine> searchMedicines(@Param("query") String query, @Param("category") String category);
}
