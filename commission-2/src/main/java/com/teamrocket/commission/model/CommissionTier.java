package com.teamrocket.commission.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "commission_tiers")
public class CommissionTier {

    @Id
    private int tierLevel; // 1, 2, or 3
    private double minSalesRange;
    private Double maxSalesRange; // Use capital Double so it can be null for the top tier
    private double commissionPercentage; // e.g., 0.02, 0.05, 0.08

    public CommissionTier() {}

    public CommissionTier(int tierLevel, double minSalesRange, Double maxSalesRange, double commissionPercentage) {
        this.tierLevel = tierLevel;
        this.minSalesRange = minSalesRange;
        this.maxSalesRange = maxSalesRange;
        this.commissionPercentage = commissionPercentage;
    }

    // Getters and Setters
    public int getTierLevel() { return tierLevel; }
    public void setTierLevel(int tierLevel) { this.tierLevel = tierLevel; }

    public double getMinSalesRange() { return minSalesRange; }
    public void setMinSalesRange(double minSalesRange) { this.minSalesRange = minSalesRange; }

    public Double getMaxSalesRange() { return maxSalesRange; }
    public void setMaxSalesRange(Double maxSalesRange) { this.maxSalesRange = maxSalesRange; }

    public double getCommissionPercentage() { return commissionPercentage; }
    public void setCommissionPercentage(double commissionPercentage) { this.commissionPercentage = commissionPercentage; }
}