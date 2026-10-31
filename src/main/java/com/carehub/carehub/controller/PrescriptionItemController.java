package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.PrescriptionItem;
import com.carehub.carehub.repository.PrescriptionItemRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescription-items")
public class PrescriptionItemController {

    @Autowired
    private PrescriptionItemRepository repository;

    @GetMapping
    public List<PrescriptionItem> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public PrescriptionItem getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription item not found with id " + id));
    }

    @GetMapping("/by-prescription/{prescriptionId}")
    public List<PrescriptionItem> getByPrescription(@PathVariable Long prescriptionId) {
        return repository.findByPrescription_PrescriptionId(prescriptionId);
    }

    @PostMapping
    public PrescriptionItem create(@Valid @RequestBody PrescriptionItem item) {
        return repository.save(item);
    }

    @PutMapping("/{id}")
    public PrescriptionItem update(@PathVariable Long id, @Valid @RequestBody PrescriptionItem updated) {
        PrescriptionItem existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription item not found with id " + id));
        existing.setPrescription(updated.getPrescription());
        existing.setMedicine(updated.getMedicine());
        existing.setDosage(updated.getDosage());
        existing.setFrequency(updated.getFrequency());
        existing.setDuration(updated.getDuration());
        existing.setQuantity(updated.getQuantity());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription item not found with id " + id);
        }
        repository.deleteById(id);
    }
}
