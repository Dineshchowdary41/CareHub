package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Prescription;
import com.carehub.carehub.repository.PrescriptionRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    @Autowired
    private PrescriptionRepository repository;

    @GetMapping
    public List<Prescription> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Prescription getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id " + id));
    }

    @GetMapping("/by-patient/{patientId}")
    public List<Prescription> getByPatient(@PathVariable Long patientId) {
        return repository.findByPatient_PatientId(patientId);
    }

    @GetMapping("/by-doctor/{doctorId}")
    public List<Prescription> getByDoctor(@PathVariable Long doctorId) {
        return repository.findByDoctor_DoctorId(doctorId);
    }

    @PostMapping
    public Prescription create(@Valid @RequestBody Prescription prescription) {
        return repository.save(prescription);
    }

    @PutMapping("/{id}")
    public Prescription update(@PathVariable Long id, @Valid @RequestBody Prescription updated) {
        Prescription existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found with id " + id));
        existing.setPatient(updated.getPatient());
        existing.setDoctor(updated.getDoctor());
        existing.setPrescriptionDate(updated.getPrescriptionDate());
        existing.setNotes(updated.getNotes());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Prescription not found with id " + id);
        }
        repository.deleteById(id);
    }
}
