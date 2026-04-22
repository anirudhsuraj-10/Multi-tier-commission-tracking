package com.teamrocket.commission.controller;

import com.jackfruit.scm.database.adapter.CommissionAdapter;
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.model.CommissionModels;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// EXCEPTION HANDLER IMPORTS
import com.scm.subsystems.MultiTierCommissionSubsystem;

@RestController
@RequestMapping("/api/tiers")
public class TierController {

    private final MultiTierCommissionSubsystem exceptions = MultiTierCommissionSubsystem.INSTANCE;

    @GetMapping
    public List<CommissionModels.CommissionTier> getAllTiers() {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            List<CommissionModels.CommissionTier> tiers = adapter.listTiers();
            if (tiers.isEmpty()) {
                exceptions.onInvalidTierConfiguration("ALL");
            }
            return tiers;
        } catch (Exception e) {
            exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
            return null;
        }
    }

    @PostMapping
    public String saveTier(@RequestBody CommissionModels.CommissionTier tier) {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            adapter.createCommissionTier(tier);
            return "Tier saved successfully!";
        } catch (Exception e) {
            exceptions.onInvalidTierConfiguration(String.valueOf(tier.tierLevel()));
            return "Failed to save tier: " + e.getMessage();
        }
    }

    @DeleteMapping("/{tierId}")
    public String deleteTier(@PathVariable String tierId) {
        try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
            CommissionAdapter adapter = new CommissionAdapter(db);
            adapter.deleteCommissionTier(tierId);
            return "Tier deleted successfully!";
        } catch (Exception e) {
            exceptions.onExternalSubsystemTimeout("SupplyChainDatabaseFacade", 0);
            return "Failed to delete tier: " + e.getMessage();
        }
    }
}