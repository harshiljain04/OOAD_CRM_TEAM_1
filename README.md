# OOAD_CRM_TEAM_1

Complete OOAD CRM showcase project for Team 1 with:

- Browser frontend (single-page dashboard)
- Java HTTP backend API
- In-memory data layer (DAO + factory + singleton pool)
- CLI demos for lead workflow and marketing analytics

This README explains exactly how the project works, which files implement each feature, how each team member can present their work, and where GRASP/SOLID and OOAD patterns are used.

## 1) High-Level System Overview

The CRM is organized into four major business modules:

1. Sales Lead & Opportunity Management
2. Customer Data & ERP Integration
3. Marketing Campaigns & Analytics
4. Core Infrastructure & Interaction Tracking

Runtime flow in web mode:

1. `CRMWebServer` starts an HTTP server on port 8080.
2. Static frontend files are served from `frontend/`.
3. Frontend forms call `/api/...` endpoints.
4. Endpoints invoke service-layer classes.
5. Services use interfaces + adapters + DAO abstractions.
6. Data is stored in in-memory collections managed by infrastructure classes.

## 2) How to Run

### 2.1 Compile

```bash
javac src/*.java
```

### 2.2 Run Web Frontend Demo

```bash
java -cp src CRMWebServer
```

Open:

```text
http://localhost:8080
```

### 2.3 Other Demo Modes

Full infrastructure demo:

```bash
java -cp src CRMInfrastructure
```

Lead CLI demo:

```bash
java -cp src CRMInfrastructure cli
```

Marketing CLI demo:

```bash
java -cp src MarketingAnalytics
```

### 2.4 Frontend Regression Test Script

```bash
chmod +x regression_frontend.sh
./regression_frontend.sh
```

## 3) Project Structure and What Each File Does

### 3.1 Backend Core and Integration

- `src/CRMWebServer.java`
	- Main web server, API routing, request parsing, response JSON building.
	- Wires all service modules together.

- `src/CRMInfrastructure.java`
	- Shared infrastructure and domain internals:
		- `Interaction`, `Lead`, lead state classes
		- `ConnectionPoolManager` singleton
		- `CentralDatabase`, `IDataAccess`, `InMemoryDataAccess`, `DAOFactory`
		- `IInteractionServices`, `InteractionManager`
	- Also includes standalone demo `main`.

- `src/LeadsManagementCLI.java`
	- CLI screen for lead operations (create, advance state, status).

### 3.2 Lead Module

- `src/ILeadServices.java`
	- Lead management contract.

- `src/LeadService.java`
	- In-memory lead service.
	- Handles state transition LEAD -> OPPORTUNITY -> CUSTOMER.
	- Auto-creates a customer record when lead reaches CUSTOMER.

### 3.3 Customer and ERP Module

- `src/Customer.java`
	- Customer entity, purchase list, lifetime value calculation.

- `src/Purchase.java`
	- Purchase entity used by `Customer`.

- `src/CustomerDAO.java`
	- DAO abstraction for customer persistence.

- `src/CustomerDAOInMemory.java`
	- DAO implementation using infrastructure `IDataAccess` tokens.

- `src/CustomerService.java`
	- Customer use cases (create/update/delete/get/sync).

- `src/IERPConnector.java`
	- ERP interface.

- `src/ERPAdapter.java`
	- Adapter from CRM model to legacy ERP `sendData(...)` API.

- `src/LegacyERPSystem.java`
	- Mock legacy ERP implementation.

- `src/CustomerNotFoundException.java`
- `src/ERPSyncException.java`

### 3.4 Marketing and Analytics Module

- `src/MarketingAnalytics.java`
	- Contains:
		- `CampaignStatus` enum
		- `Campaign`, `AnalyticsReport`
		- `CampaignObserver`
		- Interfaces: `IMarketingServices`, `IAnalyticsReports`
		- Services: `CampaignManager`, `AnalyticsManager`
		- Factory: `ReportFactory`
		- CLI view/controller: `DashboardView`, `MarketingAnalytics` main

### 3.5 Validation/Utility

- `src/InvalidDataException.java`

### 3.6 Frontend Files

- `frontend/index.html`
	- Feature cards and input forms for all modules.

- `frontend/app.js`
	- Form submission, API calls, GET/POST handling, output rendering, toast notifications.

- `frontend/styles.css`
	- Responsive visual design and animations.

### 3.7 Database Schema Reference

- `db/schema.sql`
	- SQL-oriented schema draft for a persistent DB version.
	- Current runtime implementation is in-memory.

### 3.8 Testing Support

- `regression_frontend.sh`
	- Automated API-level regression matrix for frontend behavior.

## 4) Feature Ownership: What to Say in Demo

## 4.1 Ashmith Reddy: Sales Lead & Opportunity Management

### What it does

- Creates leads.
- Tracks status through a strict pipeline:
	- LEAD
	- OPPORTUNITY
	- CUSTOMER
- Prevents invalid transition past CUSTOMER.
- On transition to CUSTOMER, auto-converts lead into customer record.

### Main files

- `src/ILeadServices.java`
- `src/LeadService.java`
- `src/LeadsManagementCLI.java`
- Lead state model in `src/CRMInfrastructure.java`

### How to explain build approach

- Contract-first service design via `ILeadServices`.
- `LeadService` stores leads in-memory with generated IDs.
- State behavior delegated to state classes (`NewState`, `QualifiedState`, `CustomerState`).
- Integrated with customer module via auto-create on conversion.

### Patterns and principles used

- Behavioral (OOAD): State Pattern for lead lifecycle.
- SOLID:
	- OCP: new states can be added with minimal changes.
	- SRP: lead service focuses on lead operations.
- GRASP:
	- Controller: `LeadService` controls lead use cases.

## 4.2 Harshil Jain: Customer Data & ERP Integration

### What it does

- Customer CRUD through service + DAO.
- Customer fetch by ID with explicit exception.
- ERP synchronization of customer data.

### Main files

- `src/Customer.java`
- `src/CustomerDAO.java`
- `src/CustomerDAOInMemory.java`
- `src/CustomerService.java`
- `src/IERPConnector.java`
- `src/ERPAdapter.java`
- `src/LegacyERPSystem.java`
- `src/CustomerNotFoundException.java`
- `src/ERPSyncException.java`

### How to explain build approach

- Built clean boundaries:
	- Service layer for business actions
	- DAO abstraction for persistence
	- ERP abstraction for external integration
- Adapter converts `Customer` object into JSON payload accepted by legacy system.

### Patterns and principles used

- Structural (OOAD): Adapter Pattern (`ERPAdapter` wraps `LegacyERPSystem`).
- SOLID:
	- DIP: `CustomerService` depends on `CustomerDAO` and `IERPConnector` abstractions.
	- SRP: separate classes for service, adapter, and data access.
- GRASP:
	- Information Expert: `Customer` calculates its own lifetime value.

## 4.3 Aryan M: Marketing & Analytics

### What it does

- Campaign creation, status updates, revenue tracking, deletion.
- ROI calculation and analytics summary.
- Dashboard-style reporting and observer-based auto-refresh in CLI mode.

### Main file

- `src/MarketingAnalytics.java` (contains full module)

### How to explain build approach

- Defined two interfaces:
	- `IMarketingServices` for campaign operations
	- `IAnalyticsReports` for analytics calculations
- Built `CampaignManager` for campaign workflow.
- Built `AnalyticsManager` to aggregate metrics (spend/revenue/avg ROI/best performer).
- Added `ReportFactory` to centralize report creation.

### Patterns and principles used

- Creational (OOAD): Factory Method via `ReportFactory`.
- Behavioral (OOAD): Observer via `CampaignObserver` + `DashboardView`.
- SOLID:
	- ISP: separate marketing vs analytics interfaces.
	- DIP: analytics depends on `IMarketingServices`.
	- OCP: report generation extensions can be added via factory.
- GRASP:
	- Information Expert: `Campaign.calculateROI()`.
	- Controller: module CLI delegates to services.

## 4.4 Harshita Chhaparia: Core Infrastructure & Interaction Tracking

### What it does

- Central in-memory CRM data storage.
- Interaction tracking and retrieval.
- DAO abstraction for infrastructure-level data operations.
- Connection pool management for conceptual DB access.

### Main files

- `src/CRMInfrastructure.java`
	- `ConnectionPoolManager` (singleton)
	- `CentralDatabase`
	- `IDataAccess`, `InMemoryDataAccess`, `DAOFactory`
	- `Interaction`, `IInteractionServices`, `InteractionManager`
- `src/InvalidDataException.java`
- `db/schema.sql` (persistent DB blueprint)

### How to explain build approach

- Created abstraction `IDataAccess` so service layer is decoupled from storage details.
- Implemented in-memory storage with SQL-like command keys.
- Implemented interaction logging with validation:
	- rejects events with both lead and customer null.
- Preserves interaction id/notes/timestamp and returns full records.

### Patterns and principles used

- Creational (OOAD): Factory Pattern via `DAOFactory`.
- Creational (OOAD): Singleton Pattern via `ConnectionPoolManager`.
- SOLID:
	- DIP: `InteractionManager` depends on `IDataAccess` + `IERPConnector` abstractions.
	- SRP: separate responsibility among DAO, service, and model.
- GRASP:
	- Controller: `InteractionManager` orchestrates interaction use cases.
	- Low Coupling: interfaces reduce direct dependency on implementations.

## 5) Frontend: How It Connects to Features

Frontend screen sections match team ownership cards:

1. Lead Pipeline (Ashmith)
2. Customer and ERP (Harshil)
3. Interaction Tracking (Harshita)
4. Marketing and Analytics (Aryan)

The frontend submits form data using `application/x-www-form-urlencoded` and renders API JSON responses directly into output panels.

## 6) API Endpoints (Web Demo)

Lead:

- `POST /api/leads/create`
- `POST /api/leads/advance`
- `GET /api/leads/status?id=...`

Customer:

- `POST /api/customers/create`
- `POST /api/customers/update`
- `POST /api/customers/delete`
- `GET /api/customers/list`
- `GET /api/customers/get?id=...`
- `POST /api/customers/sync`

Interaction:

- `POST /api/interactions/log`
- `GET /api/interactions/list`

Marketing and analytics:

- `POST /api/campaigns/create`
- `POST /api/campaigns/revenue`
- `POST /api/campaigns/status`
- `GET /api/campaigns/list`
- `GET /api/analytics/summary`

Health:

- `GET /api/health`

## 7) End-to-End Demo Script (Suggested Viva Flow)

1. Start server and open frontend.
2. Create a lead and advance it to CUSTOMER.
3. Click list customers and show auto-converted lead appears.
4. Create another customer and trigger ERP sync.
5. Log interaction with notes and list interactions.
6. Create campaign, record revenue, update status.
7. Open analytics dashboard and explain ROI + best performer.

## 8) Pattern Summary Matrix

Creational patterns:

- Factory: `DAOFactory` in `src/CRMInfrastructure.java`
- Factory Method: `ReportFactory` in `src/MarketingAnalytics.java`
- Singleton: `ConnectionPoolManager` in `src/CRMInfrastructure.java`

Structural patterns:

- Adapter: `ERPAdapter` adapting `LegacyERPSystem` through `IERPConnector`

Behavioral patterns:

- State: lead lifecycle (`LeadState`, `NewState`, `QualifiedState`, `CustomerState`)
- Observer: campaign updates (`CampaignObserver`, `DashboardView`)

## 9) SOLID Summary

- SRP: distinct classes for service, DAO, adapter, model, and UI/controller concerns.
- OCP: extend campaign reports/states and lead states with minimal modification.
- LSP: interface-based service/DAO/ERP implementations are substitutable.
- ISP: split interfaces (`IMarketingServices`, `IAnalyticsReports`, `ILeadServices`, `IERPConnector`, `CustomerDAO`, `IDataAccess`).
- DIP: business layers depend on abstractions, not concretions.

## 10) GRASP Summary

- Information Expert:
	- `Campaign.calculateROI()`
	- `Customer.calculateLifetimeValue()`
- Controller:
	- `LeadService`, `CustomerService`, `InteractionManager`, `CampaignManager`
	- `CRMWebServer` routes requests to controllers/services
- Low Coupling / High Cohesion:
	- Interfaces and adapters isolate concerns
	- Modules remain focused by responsibility

## 11) Notes

- Current runtime storage is in-memory and resets when process restarts.
- `db/schema.sql` documents the intended relational schema for a persistent variant.
- Files `CUSTOMER` and `OPPORTUNITY` currently exist as empty placeholders and are not used at runtime.