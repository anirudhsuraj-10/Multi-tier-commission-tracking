package com.teamrocket.commission;

import com.jackfruit.scm.database.adapter.CommissionAdapter;
import com.jackfruit.scm.database.facade.SupplyChainDatabaseFacade;
import com.jackfruit.scm.database.model.CommissionModels;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class CommissionApplication {

    public static void main(String[] args) {
        // 🔥 THE FIX: Force-feed Team 5's JAR the database credentials via System Properties!
        System.setProperty("db.url", "jdbc:mysql://localhost:3306/OOAD");
        System.setProperty("db.username", "teamrocket");
        System.setProperty("db.password", "admin123");
        
        SpringApplication.run(CommissionApplication.class, args);
    }

    @Bean
    public CommandLineRunner loadData() {
        return (args) -> {
            // WE NOW SEED THE DATABASE USING TEAM 5'S FACADE
            try (SupplyChainDatabaseFacade db = new SupplyChainDatabaseFacade()) {
                CommissionAdapter adapter = new CommissionAdapter(db);

                // 1. Seed Commission Tiers 
                if (adapter.listTiers().isEmpty()) {
                    adapter.createCommissionTier(new CommissionModels.CommissionTier("T1", 1, BigDecimal.valueOf(0.0), BigDecimal.valueOf(50000.0), BigDecimal.valueOf(0.02)));
                    adapter.createCommissionTier(new CommissionModels.CommissionTier("T2", 2, BigDecimal.valueOf(50000.0), BigDecimal.valueOf(100000.0), BigDecimal.valueOf(0.05)));
                    adapter.createCommissionTier(new CommissionModels.CommissionTier("T3", 3, BigDecimal.valueOf(100000.0), BigDecimal.valueOf(999999999.0), BigDecimal.valueOf(0.08)));
                    System.out.println("Database Seeded with Commission Tiers via Canonical DB!");
                }

                // 2. Seed Agents 
                if (adapter.listAgents().isEmpty()) {
                    adapter.createAgent(new CommissionModels.Agent("M999", "Giovanni Boss", 2, null, "A123", "ACTIVE"));
                    adapter.createAgent(new CommissionModels.Agent("A123", "Jessie Rep", 1, "M999", null, "ACTIVE"));
                    System.out.println("Database Seeded with Agents via Canonical DB!");
                }

            } catch (Exception e) {
                System.err.println("WARNING: Failed to seed database. " + e.getMessage());
                e.printStackTrace(); // This will reveal exactly what Team 5's JAR is complaining about!
            }
        };
    }
}