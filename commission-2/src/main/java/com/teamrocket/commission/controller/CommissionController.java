package com.teamrocket.commission.controller;

import com.jackfruit.scm.database.model.CommissionModels;
import com.teamrocket.commission.service.CommissionReportGenerator;
import com.teamrocket.commission.service.CommissionService;
import com.teamrocket.commission.service.CurrencyConverterService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// EXCEPTION HANDLER IMPORTS
import com.scm.subsystems.MultiTierCommissionSubsystem;

@RestController
@RequestMapping("/api/commission")
public class CommissionController {

    private final CommissionService commissionService;
    private final CommissionReportGenerator reportGenerator;
    private final CurrencyConverterService currencyConverter;

    private final MultiTierCommissionSubsystem exceptions = MultiTierCommissionSubsystem.INSTANCE;

    public CommissionController(CommissionService commissionService,
                                CommissionReportGenerator reportGenerator,
                                CurrencyConverterService currencyConverter) {
        this.commissionService = commissionService;
        this.reportGenerator = reportGenerator;
        this.currencyConverter = currencyConverter;
    }

    @GetMapping("/calculate")
    public Map<String, Object> calculate(
            @RequestParam String agentId,
            @RequestParam double sales,
            @RequestParam(required = false) String coAgentId,
            @RequestParam(defaultValue = "1.0") double splitRatio,
            @RequestParam(defaultValue = "INR") String currency
    ) {
        if (agentId == null || agentId.trim().isEmpty()) {
            exceptions.onInvalidAgentId("UNKNOWN");
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "FAILED");
            errorResponse.put("message", "Validation Failed: Agent ID is missing.");
            errorResponse.put("calculatedCommission", 0.0);
            return errorResponse;
        }

        double commissionInINR = commissionService.calculateCommission(agentId, sales, coAgentId, splitRatio);

        double convertedSales = currencyConverter.convertFromINR(sales, currency);
        double convertedCommission = currencyConverter.convertFromINR(commissionInINR, currency);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("agentId", agentId);
        if (coAgentId != null) response.put("coAgentId", coAgentId);
        response.put("currency", currency.toUpperCase());
        response.put("totalSales", convertedSales);
        response.put("calculatedCommission", convertedCommission);
        response.put("message", "Calculation saved via Team 5 DB Facade. Converted to " + currency.toUpperCase());

        return response;
    }

    @GetMapping("/report")
    public List<CommissionModels.CommissionHistory> getReport(@RequestParam String agentId) {
        return reportGenerator.generateReportForAgent(agentId);
    }

    @PostMapping("/clawback")
    public Map<String, Object> executeClawback(@RequestParam String originalCommissionId) {
        double deductedAmount = commissionService.processClawback(originalCommissionId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "CLAWBACK_SUCCESS");
        response.put("originalRecordId", originalCommissionId);
        response.put("deductedAmount", deductedAmount);
        response.put("message", "Commission reversed in Canonical DB.");

        return response;
    }

    @PostMapping("/dispute")
    public Map<String, Object> fileDispute(
            @RequestParam String commissionId,
            @RequestParam String agentId,
            @RequestParam String reason
    ) {
        System.out.println("DISPUTE RECEIVED (Local Only): " + reason + " for ID: " + commissionId);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "DISPUTE_FILED");
        response.put("disputeId", System.currentTimeMillis());
        response.put("message", "Dispute submitted. (Note: Not saved to canonical DB per schema limits).");

        return response;
    }

    @GetMapping("/payroll-export")
    public Map<String, Object> exportForPayroll() {
        List<CommissionModels.CommissionHistory> allRecords = reportGenerator.generateGlobalPayrollReport();

        Map<String, Object> payload = new HashMap<>();
        payload.put("system", "MULTI_TIER_COMMISSION_ENGINE");
        payload.put("exportType", "GLOBAL_PAYROLL_LEDGER");
        payload.put("totalRecords", allRecords.size());
        payload.put("data", allRecords);

        return payload;
    }

    @GetMapping("/analytics")
    public Map<String, Object> getSystemAnalytics() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "SUCCESS");
        response.put("metrics", reportGenerator.generateSystemAnalytics());
        return response;
    }
}