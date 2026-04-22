package com.teamrocket.commission.service;

import com.teamrocket.commission.model.CommissionTier;
import java.util.List;

public interface CommissionStrategy {
    double calculateCommission(double totalSales, List<CommissionTier> activeTiers);
}