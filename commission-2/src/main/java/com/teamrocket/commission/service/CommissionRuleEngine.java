package com.teamrocket.commission.service;

import com.teamrocket.commission.model.CommissionTier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class CommissionRuleEngine {

    private final CommissionStrategy commissionStrategy;

    // IMPORTANT FOR PROFESSOR: Dependency Injection (SOLID: Dependency Inversion Principle)
    // We pass the interface here, not the hardcoded class.
    @Autowired
    public CommissionRuleEngine(CommissionStrategy commissionStrategy) {
        this.commissionStrategy = commissionStrategy;
    }

    public double runEngine(double totalSales, List<CommissionTier> tiers) {
        // Delegates the math to whichever strategy is currently active
        return commissionStrategy.calculateCommission(totalSales, tiers);
    }
}