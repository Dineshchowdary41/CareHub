package com.carehub.carehub.service;

import com.carehub.carehub.config.ResourceNotFoundException;
import com.carehub.carehub.entity.PurchaseOrder;
import com.carehub.carehub.repository.PurchaseOrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository repository;

    public List<PurchaseOrder> getAll() {
        return repository.findAll();
    }

    public PurchaseOrder getOne(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found with id " + id));
    }

    public PurchaseOrder create(PurchaseOrder order) {
        order.setStatus("Pending Approval");
        return repository.save(order);
    }

    public PurchaseOrder approve(Long id) {
        PurchaseOrder order = getOne(id);
        order.setStatus("Approved");
        order.setApprovedAt(LocalDateTime.now());
        return repository.save(order);
    }

    public PurchaseOrder reject(Long id) {
        PurchaseOrder order = getOne(id);
        order.setStatus("Rejected");
        return repository.save(order);
    }

    public PurchaseOrder markReceived(Long id) {
        PurchaseOrder order = getOne(id);
        order.setStatus("Received");
        return repository.save(order);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Purchase order not found with id " + id);
        }
        repository.deleteById(id);
    }
}
