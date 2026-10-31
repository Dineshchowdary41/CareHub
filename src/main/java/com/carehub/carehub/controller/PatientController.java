package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Patient;
import com.carehub.carehub.repository.PatientRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private com.carehub.carehub.service.AuditLogService auditLogService;

    @GetMapping
    public List<Patient> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Patient getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + id));
    }

    @PostMapping
    public Patient create(@Valid @RequestBody Patient patient) {
        Patient saved = repository.save(patient);
        auditLogService.log("CREATE", "Patient", "Patient registered: " + saved.getName() + " (" + saved.getGender() + ", Blood: " + saved.getBloodGroup() + ")", String.valueOf(saved.getPatientId()));
        return saved;
    }

    @PutMapping("/{id}")
    public Patient update(@PathVariable Long id, @Valid @RequestBody Patient updated) {
        Patient existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + id));
        existing.setName(updated.getName());
        existing.setDateOfBirth(updated.getDateOfBirth());
        existing.setGender(updated.getGender());
        existing.setPhone(updated.getPhone());
        existing.setAddress(updated.getAddress());
        existing.setBloodGroup(updated.getBloodGroup());
        Patient saved = repository.save(existing);
        auditLogService.log("UPDATE", "Patient", "Patient updated: " + saved.getName(), String.valueOf(saved.getPatientId()));
        return saved;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        Patient p = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id " + id));
        repository.deleteById(id);
        auditLogService.log("DELETE", "Patient", "Patient deleted: " + p.getName(), String.valueOf(id));
    }
}
