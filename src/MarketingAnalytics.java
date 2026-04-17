import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

// ============================================================
//  MARKETING & ANALYTICS MODULE
//  Owner : Aryan M
//  Team  : OOAD CRM Team 1 — PES University
//
//  Design Patterns demonstrated:
//    Creational  → ReportFactory (Factory Method)
//    Structural  → (Adapter used by Harshil for ERP; here we
//                   keep a clean dependency on interfaces)
//    Behavioural → CampaignObserver (Observer)
//
//  SOLID principles:
//    SRP  → Each class has exactly one responsibility
//    OCP  → New report types added via ReportFactory — no
//            existing code changes needed
//    DIP  → AnalyticsManager depends on IMarketingServices
//            (abstraction), never on CampaignManager directly
//
//  GRASP principles:
//    Information Expert → Campaign.calculateROI() — Campaign
//                         owns budget & revenue so it computes
//                         its own ROI
//    Controller         → MarketingAnalyticsCLI delegates all
//                         business logic to service classes;
//                         it never manipulates data directly
// ============================================================


// ────────────────────────────────────────────────────────────
//  ENUM: CampaignStatus
//  Tracks the lifecycle state of a campaign.
//  Supports OCP — new states can be added without touching
//  existing service logic.
// ────────────────────────────────────────────────────────────

enum CampaignStatus {
    /** Campaign created but not launched yet. */
    DRAFT,
    /** Campaign is live and accepting activity. */
    ACTIVE,
    /** Campaign has finished; results available. */
    COMPLETED,
    /** Campaign stopped before completion. */
    CANCELLED
}


// ────────────────────────────────────────────────────────────
//  EXCEPTIONS
//  Specific exceptions (not generic Exception) as required
//  by the professor's rubric.
// ────────────────────────────────────────────────────────────

/**
 * Thrown when a campaign ID cannot be found in the data store.
 * SRP: One class, one error condition.
 */
class CampaignNotFoundException extends Exception {
    private final String campaignId;

    public CampaignNotFoundException(String campaignId) {
        super("Campaign not found with ID: " + campaignId);
        this.campaignId = campaignId;
    }

    public String getCampaignId() { return campaignId; }
}

/**
 * Thrown when campaign creation/update data fails validation
 * (e.g. null name, negative budget, end date before start date).
 */
class InvalidCampaignDataException extends Exception {
    public InvalidCampaignDataException(String message) {
        super(message);
    }
}


// ────────────────────────────────────────────────────────────
//  MODEL: Campaign
//
//  GRASP — Information Expert:
//    Campaign holds budget & revenue, so calculateROI() lives
//    here. No external class needs to reach inside to compute
//    this value.
// ────────────────────────────────────────────────────────────

class Campaign {

    // Unique identifier generated at creation
    private final String id;

    private String        name;
    private String        targetAudience;
    private double        budget;   // allocated spend in INR
    private double        revenue;  // attributed revenue in INR
    private LocalDate     startDate;
    private LocalDate     endDate;
    private CampaignStatus status;

    /**
     * Creates a new Campaign in DRAFT status with zero revenue.
     *
     * @param name           campaign name (must not be blank)
     * @param targetAudience description of the audience
     * @param budget         allocated budget in INR (must be > 0)
     * @param startDate      planned start date
     * @param endDate        planned end date (must be >= startDate)
     */
    Campaign(String name, String targetAudience, double budget,
             LocalDate startDate, LocalDate endDate) {
        // Generate a short unique ID using timestamp + hash
        this.id             = "C" + Math.abs(Long.hashCode(System.nanoTime()));
        this.name           = name;
        this.targetAudience = targetAudience;
        this.budget         = budget;
        this.revenue        = 0.0;
        this.startDate      = startDate;
        this.endDate        = endDate;
        this.status         = CampaignStatus.DRAFT;
    }

    // ── Business Logic (GRASP: Information Expert) ───────────

    /**
     * Calculates Return on Investment (ROI) for this campaign.
     *
     * Formula: ((revenue - budget) / budget) * 100
     *
     * Returns 0.0 if budget is zero (guards against division by zero).
     *
     * @return ROI as a percentage (e.g. 140.0 means 140%)
     */
    double calculateROI() {
        if (budget == 0) return 0.0;
        return ((revenue - budget) / budget) * 100.0;
    }

    /** @return ROI formatted as a percentage string e.g. "140.00%" */
    String getFormattedROI() {
        return String.format("%.2f%%", calculateROI());
    }

    // ── Getters & Setters ────────────────────────────────────

    String          getId()             { return id; }
    String          getName()           { return name; }
    void            setName(String n)   { this.name = n; }
    String          getTargetAudience() { return targetAudience; }
    void            setTargetAudience(String a) { this.targetAudience = a; }
    double          getBudget()         { return budget; }
    void            setBudget(double b) { this.budget = b; }
    double          getRevenue()        { return revenue; }
    void            setRevenue(double r){ this.revenue = r; }
    LocalDate       getStartDate()      { return startDate; }
    void            setStartDate(LocalDate d) { this.startDate = d; }
    LocalDate       getEndDate()        { return endDate; }
    void            setEndDate(LocalDate d)   { this.endDate = d; }
    CampaignStatus  getStatus()         { return status; }
    void            setStatus(CampaignStatus s) { this.status = s; }

    @Override
    public String toString() {
        return String.format("Campaign{id='%s', name='%s', status=%s, ROI=%s}",
                id, name, status, getFormattedROI());
    }
}


// ────────────────────────────────────────────────────────────
//  MODEL: AnalyticsReport  (Data Transfer Object)
//
//  SRP: Only carries aggregated report data; computation
//       lives in AnalyticsManager.
// ────────────────────────────────────────────────────────────

class AnalyticsReport {

    private final int    totalCampaigns;
    private final double totalBudgetSpent;
    private final double totalRevenue;
    private final double averageROI;        // mean ROI across all campaigns (%)
    private final String bestPerformerName;
    private final double bestPerformerROI;

    AnalyticsReport(int totalCampaigns, double totalBudgetSpent,
                    double totalRevenue, double averageROI,
                    String bestPerformerName, double bestPerformerROI) {
        this.totalCampaigns    = totalCampaigns;
        this.totalBudgetSpent  = totalBudgetSpent;
        this.totalRevenue      = totalRevenue;
        this.averageROI        = averageROI;
        this.bestPerformerName = bestPerformerName;
        this.bestPerformerROI  = bestPerformerROI;
    }

    // Getters — DTO is read-only after construction
    int    getTotalCampaigns()    { return totalCampaigns; }
    double getTotalBudgetSpent()  { return totalBudgetSpent; }
    double getTotalRevenue()      { return totalRevenue; }
    double getAverageROI()        { return averageROI; }
    String getBestPerformerName() { return bestPerformerName; }
    double getBestPerformerROI()  { return bestPerformerROI; }

    @Override
    public String toString() {
        return String.format(
            "AnalyticsReport{campaigns=%d, spend=%.2f, revenue=%.2f, avgROI=%.2f%%, best='%s'}",
            totalCampaigns, totalBudgetSpent, totalRevenue, averageROI, bestPerformerName);
    }
}


// ────────────────────────────────────────────────────────────
//  OBSERVER PATTERN — Interface (Behavioural Pattern)
//
//  Any component that wants to be notified when campaign data
//  changes implements this interface and registers with
//  CampaignManager. Eliminates the need for manual refresh.
// ────────────────────────────────────────────────────────────

/**
 * Observer interface for campaign data change notifications.
 *
 * BEHAVIOURAL PATTERN — Observer:
 *   Subject  = CampaignManager (calls notifyObservers on change)
 *   Observer = Any class implementing this (e.g. DashboardView)
 */
interface CampaignObserver {
    /**
     * Called by the subject (CampaignManager) whenever campaign
     * data is created, updated, or deleted.
     */
    void onCampaignDataChanged();
}


// ────────────────────────────────────────────────────────────
//  SERVICE INTERFACES
//
//  SOLID — DIP: High-level classes (CLI, analytics) depend
//  on these abstractions, never on concrete implementations.
//
//  SOLID — ISP: Marketing operations and analytics operations
//  are split into two interfaces so consumers only depend on
//  what they actually need.
// ────────────────────────────────────────────────────────────

/**
 * Contract for all marketing campaign CRUD operations.
 */
interface IMarketingServices {

    /** Create and store a new campaign. */
    Campaign createCampaign(String name, String targetAudience,
                             double budget, LocalDate startDate,
                             LocalDate endDate)
            throws InvalidCampaignDataException;

    /** Retrieve a campaign by ID. */
    Campaign getCampaignById(String id) throws CampaignNotFoundException;

    /** Return an unmodifiable view of all campaigns. */
    List<Campaign> getAllCampaigns();

    /** Update an existing campaign's fields. */
    Campaign updateCampaign(Campaign campaign)
            throws CampaignNotFoundException, InvalidCampaignDataException;

    /** Record revenue achieved by a campaign. */
    void recordRevenue(String id, double revenue)
            throws CampaignNotFoundException, InvalidCampaignDataException;

    /** Change the lifecycle status of a campaign. */
    void updateStatus(String id, CampaignStatus status)
            throws CampaignNotFoundException;

    /** Permanently remove a campaign. */
    void deleteCampaign(String id) throws CampaignNotFoundException;
}

/**
 * Contract for reporting and analytics operations.
 * Separate interface from IMarketingServices (ISP).
 */
interface IAnalyticsReports {

    /** Generate a full analytics summary across all campaigns. */
    AnalyticsReport generateReport();

    /** Sum of revenue across all campaigns. */
    double getTotalRevenue();

    /** Sum of budgets across all campaigns. */
    double getTotalBudgetSpent();

    /** Campaign with the highest ROI (null if none exist). */
    Campaign getBestPerformingCampaign();

    /** Average ROI across all campaigns; 0.0 if none exist. */
    double getAverageROI();
}


// ────────────────────────────────────────────────────────────
//  SERVICE: CampaignManager  (implements IMarketingServices)
//  Also acts as SUBJECT in the Observer pattern.
//
//  Uses an in-memory List as the data store.
//  When Harshita's IDataAccess layer is integrated, only
//  this class needs to change — all callers keep depending
//  on the IMarketingServices interface (DIP).
// ────────────────────────────────────────────────────────────

class CampaignManager implements IMarketingServices {

    // In-memory store — replace with DAO calls when integrating Harshita's layer
    private final List<Campaign>       campaigns = new ArrayList<>();

    // Observer list — notified after every mutating operation
    private final List<CampaignObserver> observers = new ArrayList<>();

    // ── Observer registration ─────────────────────────────

    /** Register an observer to receive data-change notifications. */
    void addObserver(CampaignObserver o)    { observers.add(o); }

    /** Deregister an observer. */
    void removeObserver(CampaignObserver o) { observers.remove(o); }

    /**
     * Notify all registered observers that campaign data has changed.
     * Called after every create / update / delete operation.
     */
    private void notifyObservers() {
        for (CampaignObserver o : observers) o.onCampaignDataChanged();
    }

    // ── IMarketingServices implementation ────────────────

    @Override
    public Campaign createCampaign(String name, String targetAudience,
                                   double budget, LocalDate startDate,
                                   LocalDate endDate)
            throws InvalidCampaignDataException {

        // Validate inputs before creating — meaningful checks, not generic
        if (name == null || name.isBlank())
            throw new InvalidCampaignDataException("Campaign name must not be empty.");
        if (budget <= 0)
            throw new InvalidCampaignDataException(
                    "Budget must be > 0. Provided: " + budget);
        if (startDate == null || endDate == null)
            throw new InvalidCampaignDataException("Start and end dates are required.");
        if (endDate.isBefore(startDate))
            throw new InvalidCampaignDataException("End date cannot be before start date.");

        Campaign c = new Campaign(name, targetAudience, budget, startDate, endDate);
        campaigns.add(c);
        notifyObservers();  // Observer pattern: notify Dashboard console
        return c;
    }

    @Override
    public Campaign getCampaignById(String id) throws CampaignNotFoundException {
        // Search with stream, throw specific exception if not found
        return campaigns.stream()
                .filter(c -> c.getId().equals(id))
                .findFirst()
                .orElseThrow(() -> new CampaignNotFoundException(id));
    }

    @Override
    public List<Campaign> getAllCampaigns() {
        // Return unmodifiable view — prevents external mutation of internal list
        return Collections.unmodifiableList(campaigns);
    }

    @Override
    public Campaign updateCampaign(Campaign updated)
            throws CampaignNotFoundException, InvalidCampaignDataException {
        Campaign existing = getCampaignById(updated.getId());
        if (updated.getName() == null || updated.getName().isBlank())
            throw new InvalidCampaignDataException("Campaign name must not be empty.");
        if (updated.getBudget() <= 0)
            throw new InvalidCampaignDataException("Budget must be > 0.");
        // Apply updates to the existing managed object
        existing.setName(updated.getName());
        existing.setTargetAudience(updated.getTargetAudience());
        existing.setBudget(updated.getBudget());
        existing.setRevenue(updated.getRevenue());
        existing.setStartDate(updated.getStartDate());
        existing.setEndDate(updated.getEndDate());
        existing.setStatus(updated.getStatus());
        notifyObservers();
        return existing;
    }

    @Override
    public void recordRevenue(String id, double revenue)
            throws CampaignNotFoundException, InvalidCampaignDataException {
        if (revenue < 0)
            throw new InvalidCampaignDataException("Revenue cannot be negative.");
        getCampaignById(id).setRevenue(revenue);
        notifyObservers();
    }

    @Override
    public void updateStatus(String id, CampaignStatus status)
            throws CampaignNotFoundException {
        getCampaignById(id).setStatus(status);
        notifyObservers();
    }

    @Override
    public void deleteCampaign(String id) throws CampaignNotFoundException {
        Campaign c = getCampaignById(id);
        campaigns.remove(c);
        notifyObservers();
    }
}


// ────────────────────────────────────────────────────────────
//  SERVICE: AnalyticsManager  (implements IAnalyticsReports)
//
//  SOLID — DIP: Depends on IMarketingServices (interface),
//  not CampaignManager (concrete class). If the data source
//  changes, this class doesn't change.
//
//  GRASP — Controller: Aggregation live here, not in the CLI.
// ────────────────────────────────────────────────────────────

class AnalyticsManager implements IAnalyticsReports {

    // Depend on the interface abstraction (DIP)
    private final IMarketingServices marketingServices;

    AnalyticsManager(IMarketingServices marketingServices) {
        this.marketingServices = marketingServices;
    }

    @Override
    public AnalyticsReport generateReport() {
        int    total   = getAverageROI() == 0 && marketingServices.getAllCampaigns().isEmpty() ? 0
                         : marketingServices.getAllCampaigns().size();
        double budget  = getTotalBudgetSpent();
        double revenue = getTotalRevenue();
        double avgROI  = getAverageROI();
        Campaign best  = getBestPerformingCampaign();
        String bestName = (best != null) ? best.getName() : "N/A";
        double bestROI  = (best != null) ? best.calculateROI() : 0.0;
        return new AnalyticsReport(
                marketingServices.getAllCampaigns().size(),
                budget, revenue, avgROI, bestName, bestROI);
    }

    @Override
    public double getTotalRevenue() {
        // Sum revenue across all campaigns
        return marketingServices.getAllCampaigns().stream()
                .mapToDouble(Campaign::getRevenue).sum();
    }

    @Override
    public double getTotalBudgetSpent() {
        // Sum budget across all campaigns
        return marketingServices.getAllCampaigns().stream()
                .mapToDouble(Campaign::getBudget).sum();
    }

    @Override
    public Campaign getBestPerformingCampaign() {
        // Find campaign with highest ROI; null if no campaigns exist
        return marketingServices.getAllCampaigns().stream()
                .max(Comparator.comparingDouble(Campaign::calculateROI))
                .orElse(null);
    }

    @Override
    public double getAverageROI() {
        List<Campaign> all = marketingServices.getAllCampaigns();
        if (all.isEmpty()) return 0.0; // Guard against divide-by-zero
        return all.stream().mapToDouble(Campaign::calculateROI).average().orElse(0.0);
    }
}


// ────────────────────────────────────────────────────────────
//  FACTORY PATTERN: ReportFactory  (Creational Pattern)
//
//  Decouples report construction from the code that requests
//  it. Callers never instantiate AnalyticsManager directly.
//
//  SOLID — OCP: New report types can be added as new factory
//  methods without modifying existing code.
// ────────────────────────────────────────────────────────────

class ReportFactory {

    // Utility class — no instantiation
    private ReportFactory() {}

    /**
     * Creates a summary analytics report across all campaigns.
     *
     * CREATIONAL PATTERN — Factory Method:
     *   Caller asks the factory for a report; it doesn't need
     *   to know how AnalyticsManager is constructed or wired.
     *
     * @param marketingServices data source for the report
     * @return a fully populated AnalyticsReport
     */
    static AnalyticsReport createSummaryReport(IMarketingServices marketingServices) {
        AnalyticsManager manager = new AnalyticsManager(marketingServices);
        return manager.generateReport();
    }

    /**
     * Creates a report focused on top-performing campaigns.
     * (Extensible later without touching createSummaryReport — OCP)
     */
    static AnalyticsReport createTopPerformersReport(IMarketingServices marketingServices) {
        // Future: filter to top N by ROI before generating
        AnalyticsManager manager = new AnalyticsManager(marketingServices);
        return manager.generateReport();
    }
}


// ────────────────────────────────────────────────────────────
//  CONSOLE VIEW: DashboardView
//  Implements CampaignObserver to auto-refresh on data change.
//
//  GRASP — Controller: Receives user commands and delegates
//  to service classes. Does NOT contain any business logic.
// ────────────────────────────────────────────────────────────

class DashboardView implements CampaignObserver {

    private final IMarketingServices marketingServices;

    DashboardView(IMarketingServices marketingServices) {
        this.marketingServices = marketingServices;
    }

    /**
     * Observer callback — called by CampaignManager whenever data changes.
     * Prints an updated dashboard automatically (no manual refresh needed).
     */
    @Override
    public void onCampaignDataChanged() {
        System.out.println("\n[Observer] Dashboard auto-refreshing...");
        printDashboard();
    }

    /** Prints the analytics dashboard to the console using ReportFactory. */
    void printDashboard() {
        // Factory pattern builds the report — DashboardView doesn't care how
        AnalyticsReport report = ReportFactory.createSummaryReport(marketingServices);
        System.out.println("\n╔══════════════════════════════════════════════╗");
        System.out.println("║       CRM — Marketing Analytics Dashboard    ║");
        System.out.println("╠══════════════════════════════════════════════╣");
        System.out.printf( "║  Total Campaigns   : %-24d║%n", report.getTotalCampaigns());
        System.out.printf( "║  Total Spend       : Rs %-21.2f║%n", report.getTotalBudgetSpent());
        System.out.printf( "║  Total Revenue     : Rs %-21.2f║%n", report.getTotalRevenue());
        System.out.printf( "║  Average ROI       : %-23.2f%%║%n", report.getAverageROI());
        System.out.printf( "║  Best Performer    : %-24s║%n", report.getBestPerformerName());
        System.out.printf( "║  Best Campaign ROI : %-23.2f%%║%n", report.getBestPerformerROI());
        System.out.println("╚══════════════════════════════════════════════╝");
    }
}


// ────────────────────────────────────────────────────────────
//  MAIN CLI: MarketingAnalyticsCLI
//  Entry point — wires services, registers observer, runs menu.
//
//  GRASP — Controller: Routes user input to the appropriate
//  service method. No business logic here.
// ────────────────────────────────────────────────────────────

public class MarketingAnalytics {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Wire up the service layer
        CampaignManager manager = new CampaignManager();

        // Wire up the dashboard observer — it auto-refreshes on every change
        DashboardView dashboard = new DashboardView(manager);
        manager.addObserver(dashboard);  // Observer registered at startup

        System.out.println("==========================================");
        System.out.println("  CRM — Marketing & Analytics Module");
        System.out.println("  Owner: Aryan M | OOAD CRM Team 1");
        System.out.println("==========================================");

        boolean running = true;
        while (running) {
            printMenu();
            System.out.print("Enter choice: ");
            String choice = scanner.nextLine().trim();

            try {
                switch (choice) {
                    case "1" -> handleCreateCampaign(scanner, manager);
                    case "2" -> listAllCampaigns(manager);
                    case "3" -> handleRecordRevenue(scanner, manager);
                    case "4" -> handleUpdateStatus(scanner, manager);
                    case "5" -> handleDeleteCampaign(scanner, manager);
                    case "6" -> dashboard.printDashboard();
                    case "7" -> running = false;
                    default  -> System.out.println("[!] Invalid option, please try again.");
                }
            } catch (InvalidCampaignDataException | CampaignNotFoundException e) {
                // Specific exceptions — meaningful error messages
                System.out.println("[Error] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("[Unexpected Error] " + e.getMessage());
            }
        }
        System.out.println("Exiting Marketing & Analytics Module. Goodbye!");
        scanner.close();
    }

    // ── Menu helpers ─────────────────────────────────────────

    private static void printMenu() {
        System.out.println("\n--- Marketing & Analytics Menu ---");
        System.out.println("1. Create Campaign");
        System.out.println("2. List All Campaigns");
        System.out.println("3. Record Revenue");
        System.out.println("4. Update Campaign Status");
        System.out.println("5. Delete Campaign");
        System.out.println("6. View Analytics Dashboard");
        System.out.println("7. Exit");
    }

    /**
     * Reads campaign details from the user and calls the service.
     * GRASP Controller: delegates immediately to CampaignManager.
     */
    private static void handleCreateCampaign(Scanner sc, IMarketingServices svc)
            throws InvalidCampaignDataException {
        System.out.print("Campaign name: ");
        String name = sc.nextLine().trim();
        System.out.print("Target audience: ");
        String audience = sc.nextLine().trim();
        System.out.print("Budget (INR): ");
        double budget = Double.parseDouble(sc.nextLine().trim());
        System.out.print("Start date (YYYY-MM-DD): ");
        LocalDate start = LocalDate.parse(sc.nextLine().trim());
        System.out.print("End date   (YYYY-MM-DD): ");
        LocalDate end   = LocalDate.parse(sc.nextLine().trim());

        Campaign c = svc.createCampaign(name, audience, budget, start, end);
        System.out.println("[OK] Campaign created: " + c);
        // Dashboard auto-refreshes via Observer — no explicit refresh call needed
    }

    /** Lists all campaigns with their IDs, status, and ROI. */
    private static void listAllCampaigns(IMarketingServices svc) {
        List<Campaign> all = svc.getAllCampaigns();
        if (all.isEmpty()) {
            System.out.println("[Info] No campaigns found.");
            return;
        }
        System.out.println("\n--- All Campaigns ---");
        all.forEach(c -> System.out.printf(
            "  ID=%-12s  %-20s  Status=%-10s  ROI=%s%n",
            c.getId(), c.getName(), c.getStatus(), c.getFormattedROI()));
    }

    /** Reads a campaign ID and revenue, then delegates to the service. */
    private static void handleRecordRevenue(Scanner sc, IMarketingServices svc)
            throws CampaignNotFoundException, InvalidCampaignDataException {
        System.out.print("Campaign ID: ");
        String id = sc.nextLine().trim();
        System.out.print("Revenue (INR): ");
        double revenue = Double.parseDouble(sc.nextLine().trim());
        svc.recordRevenue(id, revenue);
        System.out.println("[OK] Revenue recorded. ROI is now: "
                + svc.getCampaignById(id).getFormattedROI());
    }

    /** Reads a campaign ID and new status, then delegates to the service. */
    private static void handleUpdateStatus(Scanner sc, IMarketingServices svc)
            throws CampaignNotFoundException {
        System.out.print("Campaign ID: ");
        String id = sc.nextLine().trim();
        System.out.println("Select status: 1=DRAFT  2=ACTIVE  3=COMPLETED  4=CANCELLED");
        String choice = sc.nextLine().trim();
        CampaignStatus status = switch (choice) {
            case "1" -> CampaignStatus.DRAFT;
            case "2" -> CampaignStatus.ACTIVE;
            case "3" -> CampaignStatus.COMPLETED;
            case "4" -> CampaignStatus.CANCELLED;
            default  -> throw new IllegalArgumentException("Invalid status choice: " + choice);
        };
        svc.updateStatus(id, status);
        System.out.println("[OK] Status updated to " + status);
    }

    /** Reads a campaign ID and deletes it after confirmation. */
    private static void handleDeleteCampaign(Scanner sc, IMarketingServices svc)
            throws CampaignNotFoundException {
        System.out.print("Campaign ID to delete: ");
        String id = sc.nextLine().trim();
        System.out.print("Confirm delete? (yes/no): ");
        if ("yes".equalsIgnoreCase(sc.nextLine().trim())) {
            svc.deleteCampaign(id);
            System.out.println("[OK] Campaign deleted.");
        } else {
            System.out.println("[Cancelled] No changes made.");
        }
    }
}
