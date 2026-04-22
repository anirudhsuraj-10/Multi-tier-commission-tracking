package com.teamrocket.commission.service;

import com.teamrocket.commission.model.CommissionTier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// TEAM 5 DATABASE IMPORTS
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.adapter.CommissionAdapter;
import com.jackfruit.scm.database.model.CommissionModels;

// EXCEPTION HANDLER IMPORTS
import com.scm.subsystems.MultiTierCommissionSubsystem;

@Service
public class CommissionService {

    private final CommissionRuleEngine ruleEngine;
    private static final double MANAGER_OVERRIDE_RATE = 0.01;

    // Exception handler instance — singleton per the integration spec
    private final MultiTierCommissionSubsystem exceptions = MultiTierCommissionSubsystem.INSTANCE;

    public CommissionService(CommissionRuleEngine ruleEngine) {
        this.ruleEngine = ruleEngine;
    }

    public double calculateCommission(String agentId, double totalSales, String coAgentId, double splitRatio) {
        // 1. VALIDATE AGENT ID BEFORE OPENING DB
        if (agentId == null || agentId.trim().isEmpty()) {
            exceptions.onInvalidAgentId("UNKNOWN");
            return 0.0;
        }

        // 2. OPEN CONNECTION TO TEAM 5 DATABASE FACADE
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);

            // 3. FETCH TIERS FROM TEAM 5 AND TRANSLATE TO OUR ENGINE
            List<CommissionModels.CommissionTier> dbTiers;
            try {
                dbTiers = adapter.listTiers();
            } catch (Exception e) {
                exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
                return 0.0;
            }

            if (dbTiers.isEmpty()) {
                exceptions.onInvalidTierConfiguration("ALL");
                return 0.0;
            }

            List<CommissionTier> activeTiers = dbTiers.stream()
                .map(t -> new CommissionTier(
                    t.tierLevel(),
                    t.minSales().doubleValue(),
                    t.maxSales() != null ? t.maxSales().doubleValue() : null,
                    t.commissionPct().doubleValue()))
                .collect(Collectors.toList());

            // 4. RUN CORE MATH ENGINE
            double totalPool = ruleEngine.runEngine(totalSales, activeTiers);

            // 5. ACCELERATOR CHECK
            double historicalSales;
            try {
                historicalSales = adapter.listCommissionHistory().stream()
                    .filter(h -> h.agentId().equals(agentId))
                    .mapToDouble(h -> h.totalSales().doubleValue())
                    .sum();
            } catch (Exception e) {
                exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
                return 0.0;
            }

            if ((historicalSales + totalSales) >= 500000.0) {
                totalPool *= 1.5;
                System.out.println("ACCELERATOR UNLOCKED!");
            }

            // 6. MANAGER OVERRIDE
            try {
                CommissionModels.Agent currentAgent = adapter.listAgents().stream()
                    .filter(a -> a.agentId().equals(agentId))
                    .findFirst().orElse(null);

                if (currentAgent != null && currentAgent.parentAgentId() != null) {
                    double managerOverride = totalSales * MANAGER_OVERRIDE_RATE;
                    saveHistoryRecord(adapter, currentAgent.parentAgentId(), totalSales, managerOverride, "Manager Override (1%)");
                }
            } catch (Exception e) {
                exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
                return 0.0;
            }

            // 7. SPLIT AND SAVE
            double mainAgentCut = totalPool * splitRatio;
            double coAgentCut = totalPool * (1.0 - splitRatio);

            saveHistoryRecord(adapter, agentId, totalSales, mainAgentCut, "Main Agent Payout");

            if (coAgentId != null && coAgentCut > 0) {
                saveHistoryRecord(adapter, coAgentId, totalSales, coAgentCut, "Co-Agent Split");
                System.out.println("SPLIT COMMISSION: Co-Agent " + coAgentId + " received " + coAgentCut);
            }

            return mainAgentCut;

        } catch (Exception e) {
            // No registered exception for this case — using closest match as fallback
            exceptions.onExternalSubsystemTimeout("CommissionService: " + e.getMessage(), 0);
            return 0.0;
        }
    }

    private void saveHistoryRecord(CommissionAdapter adapter, String agentId, double sales, double commission, String note) {
        CommissionModels.CommissionHistory record = new CommissionModels.CommissionHistory(
            UUID.randomUUID().toString(),
            agentId,
            LocalDate.now(),
            LocalDate.now(),
            BigDecimal.valueOf(sales),
            note,
            BigDecimal.valueOf(commission),
            LocalDateTime.now()
        );
        try {
            adapter.createCommissionHistory(record);
            System.out.println("INTEGRATION SUCCESS: Saved record for " + agentId + " to Canonical DB.");
        } catch (Exception e) {
            exceptions.onDuplicateCommissionEntry(record.commissionId(), agentId);
        }
    }

    public double processClawback(String originalCommissionId) {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);

            CommissionModels.CommissionHistory originalRecord;
            try {
                originalRecord = adapter.listCommissionHistory().stream()
                    .filter(h -> h.commissionId().equals(originalCommissionId))
                    .findFirst().orElseThrow(() -> new RuntimeException("Record not found: " + originalCommissionId));
            } catch (Exception e) {
                exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
                return 0.0;
            }

            double clawbackSales = -originalRecord.totalSales().doubleValue();
            double clawbackCommission = -originalRecord.totalCommission().doubleValue();

            saveHistoryRecord(adapter, originalRecord.agentId(), clawbackSales, clawbackCommission, "Clawback Executed");

            return clawbackCommission;

        } catch (Exception e) {
            exceptions.onExternalSubsystemTimeout("processClawback: " + e.getMessage(), 0);
            return 0.0;
        }
    }
}