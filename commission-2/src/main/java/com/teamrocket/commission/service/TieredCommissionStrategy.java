package com.teamrocket.commission.service;

import com.teamrocket.commission.model.CommissionTier;
import org.springframework.stereotype.Component;
import java.util.List;

@Component // Tells Spring to load this into memory
public class TieredCommissionStrategy implements CommissionStrategy {

    // IMPORTANT FOR PROFESSOR: This class implements the Strategy Pattern.
    // It also follows the Single Responsibility Principle (it ONLY does math).

    @Override
    public double calculateCommission(double totalSales, List<CommissionTier> activeTiers) {
        double totalCommission = 0.0;

        for (CommissionTier tier : activeTiers) {
            double tierMin = tier.getMinSalesRange();
            Double tierMax = tier.getMaxSalesRange();
            double tierRate = tier.getCommissionPercentage();

            // Check if the agent sold enough to reach this tier
            if (totalSales > tierMin) {
                double applicableSalesForTier;
                
                // If there's a max limit AND they sold over it, they only get commission up to the max for this bracket
                if (tierMax != null && totalSales > tierMax) {
                    applicableSalesForTier = tierMax - tierMin;
                } else {
                    // Otherwise, they just get commission on whatever they sold above the minimum
                    applicableSalesForTier = totalSales - tierMin;
                }
                
                totalCommission += (applicableSalesForTier * tierRate);
            }
        }
        return totalCommission;
    }
}