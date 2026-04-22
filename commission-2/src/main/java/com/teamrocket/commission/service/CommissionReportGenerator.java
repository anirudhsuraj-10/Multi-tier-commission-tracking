package com.teamrocket.commission.service;

import com.jackfruit.scm.database.adapter.CommissionAdapter;
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.model.CommissionModels;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// EXCEPTION HANDLER IMPORTS
import com.scm.subsystems.MultiTierCommissionSubsystem;

@Service
public class CommissionReportGenerator {

    private final MultiTierCommissionSubsystem exceptions = MultiTierCommissionSubsystem.INSTANCE;

    public List<CommissionModels.CommissionHistory> generateReportForAgent(String agentId) {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            return adapter.listCommissionHistory().stream()
                    .filter(h -> h.agentId().equals(agentId))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            exceptions.onReportSyncFailure(agentId);
            return Collections.emptyList();
        }
    }

    public List<CommissionModels.CommissionHistory> generateGlobalPayrollReport() {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            return adapter.listCommissionHistory();
        } catch (Exception e) {
            exceptions.onReportSyncFailure("GLOBAL_PAYROLL");
            return Collections.emptyList();
        }
    }

    public Map<String, Object> generateSystemAnalytics() {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            List<CommissionModels.CommissionHistory> allHistory = adapter.listCommissionHistory();

            Map<String, Object> analytics = new HashMap<>();
            long totalTransactions = allHistory.size();
            double totalCompanySales = allHistory.stream().mapToDouble(h -> h.totalSales().doubleValue()).sum();
            double totalCommissionsPaid = allHistory.stream().mapToDouble(h -> h.totalCommission().doubleValue()).sum();

            double payoutRatio = totalCompanySales > 0 ? (totalCommissionsPaid / totalCompanySales) * 100 : 0.0;

            analytics.put("totalTransactionsProcessed", totalTransactions);
            analytics.put("totalCompanySalesVolume", totalCompanySales);
            analytics.put("totalCommissionsPaidOut", totalCommissionsPaid);
            analytics.put("averageCommissionPayoutRatio", String.format("%.2f%%", payoutRatio));

            return analytics;
        } catch (Exception e) {
            exceptions.onReportSyncFailure("SYSTEM_ANALYTICS");
            return Collections.emptyMap();
        }
    }
}