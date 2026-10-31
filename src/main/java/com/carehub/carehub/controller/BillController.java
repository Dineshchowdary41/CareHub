package com.carehub.carehub.controller;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.Bill;
import com.carehub.carehub.repository.BillRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bills")
public class BillController {

    @Autowired
    private BillRepository repository;

    @GetMapping
    public List<Bill> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public Bill getOne(@PathVariable Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id " + id));
    }

    @GetMapping("/by-patient/{patientId}")
    public List<Bill> getByPatient(@PathVariable Long patientId) {
        return repository.findByPatient_PatientId(patientId);
    }

    @PostMapping
    public Bill create(@Valid @RequestBody Bill bill) {
        return repository.save(bill);
    }

    @PutMapping("/{id}")
    public Bill update(@PathVariable Long id, @Valid @RequestBody Bill updated) {
        Bill existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Bill not found with id " + id));
        existing.setPatient(updated.getPatient());
        existing.setTotalAmount(updated.getTotalAmount());
        existing.setPaymentStatus(updated.getPaymentStatus());
        return repository.save(existing);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Bill not found with id " + id);
        }
        repository.deleteById(id);
    }
}
