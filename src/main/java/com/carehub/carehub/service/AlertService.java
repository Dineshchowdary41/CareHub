package com.carehub.carehub.service;

import com.carehub.carehub.entity.Inventory;
import com.carehub.carehub.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AlertService {

    @Autowired
    private InventoryRepository inventoryRepository;

    /** Available_Stock < Minimum_Stock, across all batches. */
    public List<Inventory> lowStock() {
        return inventoryRepository.findLowStock();
    }

    /** Batches expiring within the given horizon (30 / 60 / 90 days by default). */
    public List<Inventory> expiringWithin(int days) {
        LocalDate today = LocalDate.now();
        return inventoryRepository.findExpiringBetween(today, today.plusDays(days));
    }

    public List<Inventory> alreadyExpired() {
        return inventoryRepository.findAlreadyExpired(LocalDate.now());
    }

    public List<Inventory> expiringIn30() { return expiringWithin(30); }

    public List<Inventory> expiringIn60() {
        java.util.Set<Long> already = expiringIn30().stream().map(Inventory::getInventoryId).collect(Collectors.toSet());
        return expiringWithin(60).stream().filter(i -> !already.contains(i.getInventoryId())).collect(Collectors.toList());
    }

    public List<Inventory> expiringIn90() {
        java.util.Set<Long> already = expiringWithin(60).stream().map(Inventory::getInventoryId).collect(Collectors.toSet());
        return expiringWithin(90).stream().filter(i -> !already.contains(i.getInventoryId())).collect(Collectors.toList());
    }
}
