[README.md](https://github.com/user-attachments/files/26980554/README.md)
# Multi-tier-commission-tracking# Subsystem 11 — Multi-Tier Commission Tracking

> **Team Rocket** | Object-Oriented Analysis and Design | April 2026  
> Ankur · Anshool · Suraj · Amrith

---

## Overview

A Spring Boot REST API that calculates, records, and manages sales commissions for agents in a hierarchical structure. Integrates with the shared SCM canonical database (Team 5) and the SCM Exception Handler for real-time error reporting.

---

## Prerequisites

- Java 17
- Maven 3.8+
- MySQL running on `localhost:3306`
- Windows (for exception popups and Event Viewer integration)

---

## Setup

### 1. MySQL — Grant privileges
```sql
GRANT ALL PRIVILEGES ON *.* TO 'teamrocket'@'localhost' WITH GRANT OPTION;
FLUSH PRIVILEGES;
SET GLOBAL log_bin_trust_function_creators = 1;
```

### 2. Register Windows Event Log source
Run Command Prompt as **Administrator**:
```cmd
reg add "HKLM\SYSTEM\CurrentControlSet\Services\EventLog\Application\SCM-Multi-TierCommissionTracking" /v EventMessageFile /t REG_SZ /d "%SystemRoot%\System32\EventCreate.exe" /f
```

### 3. Configure database credentials
`src/main/resources/database.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/OOAD
db.username=teamrocket
db.password=admin123
db.pool.size=5
```

### 4. Build
```cmd
mvn clean package -DskipTests
```

### 5. Run
```cmd
java -jar target\commission-0.0.1-SNAPSHOT.jar
```

App starts on `http://localhost:8080`. On first run, tiers and agents are automatically seeded into MySQL.

---

## JAR Files Required (place in `lib/`)

| JAR | Purpose |
|---|---|
| `database-module-1.0.0-SNAPSHOT-standalone.jar` | Team 5 canonical DB adapter |
| `scm-exception-handler-v3.jar` | SCM exception handler |
| `scm-exception-viewer-gui.jar` | Exception viewer GUI |
| `jna-5.18.1.jar` | Windows Event Viewer integration |
| `jna-platform-5.18.1.jar` | Windows Event Viewer integration |

---

## API Endpoints

### Commission
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/commission/calculate` | Calculate commission for an agent |
| GET | `/api/commission/report` | Commission history for an agent |
| POST | `/api/commission/clawback` | Reverse a previously paid commission |
| POST | `/api/commission/dispute` | File a dispute on a commission record |
| GET | `/api/commission/payroll-export` | Global payroll ledger |
| GET | `/api/commission/analytics` | KPI analytics |

### Tiers
| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/tiers` | List all tiers |
| POST | `/api/tiers` | Create a tier |
| DELETE | `/api/tiers/{tierId}` | Delete a tier |

---

## Quick Test

**Calculate commission:**
```
GET http://localhost:8080/api/commission/calculate?agentId=A123&sales=60000
```

**Trigger exception (blank agentId → ID 16 fires):**
```
GET http://localhost:8080/api/commission/calculate?agentId=&sales=1000
```

**Seed a tier via Postman:**
```
POST http://localhost:8080/api/tiers
Content-Type: application/json

{"tierId":"T1","tierLevel":1,"minSales":0,"maxSales":50000,"commissionPct":0.02}
```

---

## Commission Tiers (default)

| Tier | Sales Range (INR) | Rate |
|---|---|---|
| T1 | 0 – 50,000 | 2% |
| T2 | 50,000 – 1,00,000 | 5% |
| T3 | 1,00,000+ | 8% |

Commission is calculated progressively (like tax brackets), not as a flat rate on the total.

---

## Exception IDs

| ID | Name | Fired When |
|---|---|---|
| 16 | INVALID_AGENT_ID | Blank agentId in request |
| 17 | INVALID_TIER_CONFIGURATION | No tiers in canonical DB |
| 56 | EXTERNAL_SUBSYSTEM_TIMEOUT | DB facade connection fails |
| 62 | REPORT_SYNC_FAILURE | Report query fails |
| 108 | DUPLICATE_COMMISSION_ENTRY | Commission write fails |

---

## Project Structure

```
commission/
├── lib/                          # External JARs
├── src/main/java/com/teamrocket/commission/
│   ├── CommissionApplication.java        # Entry point, DB seeding
│   ├── controller/
│   │   ├── CommissionController.java     # Commission API endpoints
│   │   └── TierController.java          # Tier CRUD endpoints
│   ├── service/
│   │   ├── CommissionService.java        # Core calculation logic
│   │   ├── CommissionReportGenerator.java
│   │   ├── CommissionRuleEngine.java     # Strategy context
│   │   ├── CommissionStrategy.java      # Strategy interface
│   │   ├── TieredCommissionStrategy.java # Tier math implementation
│   │   └── CurrencyConverterService.java
│   └── model/
│       └── CommissionTier.java           # Local tier model
└── src/main/resources/
    ├── application.properties
    └── database.properties
```

---

## Design

**Architecture:** MVC (Controller → Service → External DB Adapter)

**Design Patterns:**
- **Strategy** — `CommissionStrategy` / `TieredCommissionStrategy` / `CommissionRuleEngine`
- **Singleton** — `MultiTierCommissionSubsystem.INSTANCE`

**SOLID:** All five principles applied — see report for details.

---

## Team

| Name | Contribution |
|---|---|
| Ankur | Commission Calculation Engine — Strategy Pattern, tier math, CommissionRuleEngine, CommissionTier model |
| Anshool | CommissionService — full calculation flow, accelerator, manager override, clawback, DB integration, startup seeding |
| Suraj | API Layer — all commission endpoints, tier CRUD, currency conversion |
| Amrith | Exception integration (IDs 16,17,56,62,108), reporting & analytics, build configuration |
