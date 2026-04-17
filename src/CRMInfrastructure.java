import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// ==========================================================
//  FINAL CRM INFRASTRUCTURE (WITH PATTERNS)
// ==========================================================


// ─────────────────────────────────────────────
// EXCEPTIONS
// ─────────────────────────────────────────────

class DatabaseSyncFailureException extends RuntimeException {
    public DatabaseSyncFailureException(String msg) { super(msg); }
    public DatabaseSyncFailureException(String msg, Throwable cause) { super(msg, cause); }
}

class LeadNotFoundException extends Exception {
    public LeadNotFoundException(String msg) { super(msg); }
}


// ─────────────────────────────────────────────
// MODEL
// ─────────────────────────────────────────────

class Interaction {
    private static final AtomicInteger counter = new AtomicInteger(1);

    private final int id;
    private final Integer leadId;
    private final Integer customerId; // ✅ FIXED
    private final String type;
    private final String notes;
    private final LocalDateTime timestamp;

    public Interaction(Integer leadId, Integer customerId, String type, String notes) {
        this.id = counter.getAndIncrement();
        this.leadId = leadId;
        this.customerId = customerId;
        this.type = type;
        this.notes = notes;
        this.timestamp = LocalDateTime.now();
    }

    public int getId() { return id; }
    public Integer getLeadId() { return leadId; }
    public Integer getCustomerId() { return customerId; }
    public String getType() { return type; }
    public String getNotes() { return notes; }
    public LocalDateTime getTimestamp() { return timestamp; }

    public String toString() {
        return String.format("Interaction #%d | lead=%s | customer=%s | %s",
                id, leadId, customerId, type);
    }
}


// ─────────────────────────────────────────────
// STATE PATTERN (Lead lifecycle)
// ─────────────────────────────────────────────

interface LeadState {
    void next(Lead lead);
    String getName();
}

class NewState implements LeadState {
    public void next(Lead lead) { lead.setState(new QualifiedState()); }
    public String getName() { return "NEW"; }
}

class QualifiedState implements LeadState {
    public void next(Lead lead) { lead.setState(new CustomerState()); }
    public String getName() { return "QUALIFIED"; }
}

class CustomerState implements LeadState {
    public void next(Lead lead) {
        throw new RuntimeException("Already a customer");
    }
    public String getName() { return "CUSTOMER"; }
}

class Lead {
    private LeadState state = new NewState();

    public void nextState() { state.next(this); }
    public void setState(LeadState s) { this.state = s; }
    public String getStatus() { return state.getName(); }
}


// ─────────────────────────────────────────────
// SINGLETON
// ─────────────────────────────────────────────

class ConnectionPoolManager {

    private static volatile ConnectionPoolManager instance;
    private final List<String> pool = new ArrayList<>();

    private ConnectionPoolManager() {
        for (int i = 1; i <= 5; i++) pool.add("Conn-" + i);
        System.out.println("[Singleton] Pool initialized");
    }

    public static ConnectionPoolManager getInstance() {
        if (instance == null) {
            synchronized (ConnectionPoolManager.class) {
                if (instance == null) {
                    instance = new ConnectionPoolManager();
                }
            }
        }
        return instance;
    }

    public synchronized String getConnection() {
        if (pool.isEmpty()) throw new RuntimeException("No connections");
        return pool.remove(pool.size() - 1);
    }

    public synchronized void releaseConnection(String c) { pool.add(c); }
}


// ─────────────────────────────────────────────
// CENTRAL DB (mock)
// ─────────────────────────────────────────────

class CentralDatabase {
    static List<Map<String,Object>> interactions = new ArrayList<>();

    static Map<String,Object> row(Object... kv) {
        Map<String,Object> m = new HashMap<>();
        for(int i=0;i<kv.length;i+=2) m.put((String)kv[i], kv[i+1]);
        return m;
    }
}


// ─────────────────────────────────────────────
// DATA ACCESS
// ─────────────────────────────────────────────

interface IDataAccess {
    int executeUpdate(String sql, Object... params);
    List<Map<String,Object>> executeQuery(String sql, Object... params);
}

class InMemoryDataAccess implements IDataAccess {

    private final ConnectionPoolManager pool = ConnectionPoolManager.getInstance();
    private static List<Map<String,Object>> customers = new ArrayList<>();
    private static AtomicInteger customerIdCounter = new AtomicInteger(1);

    public int executeUpdate(String sql, Object... params) {
        String conn = pool.getConnection();
        try {
            if ("INSERT".equals(sql) || "INSERT_INTERACTION".equals(sql)) {
                Map<String,Object> row = CentralDatabase.row(
                    "lead_id", params[0],
                    "customer_id", params[1],
                    "type", params[2]
                );
                CentralDatabase.interactions.add(row);
                return 1;
            } else if ("INSERT_CUSTOMER".equals(sql)) {
                int id = customerIdCounter.getAndIncrement();
                Map<String,Object> row = CentralDatabase.row(
                    "id", id,
                    "name", params[0],
                    "email", params[1]
                );
                customers.add(row);
                return id;
            } else if ("UPDATE_CUSTOMER".equals(sql)) {
                Integer id = (Integer) params[0];
                for (Map<String,Object> c : customers) {
                    if (id.equals(c.get("id"))) {
                        c.put("name", params[1]);
                        c.put("email", params[2]);
                        return 1;
                    }
                }
                return 0;
            } else if ("DELETE_CUSTOMER".equals(sql)) {
                Integer id = (Integer) params[0];
                return customers.removeIf(c -> id.equals(c.get("id"))) ? 1 : 0;
            }
            return 0;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public List<Map<String,Object>> executeQuery(String sql, Object... params) {
        if ("SELECT".equals(sql) || "SELECT_INTERACTIONS".equals(sql)) {
            return new ArrayList<>(CentralDatabase.interactions);
        } else if ("SELECT_CUSTOMERS".equals(sql)) {
            return new ArrayList<>(customers);
        } else if ("FIND_CUSTOMER_BY_ID".equals(sql)) {
            Integer id = (Integer) params[0];
            return customers.stream()
                .filter(c -> id.equals(c.get("id")))
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}


// ─────────────────────────────────────────────
// FACTORY PATTERN
// ─────────────────────────────────────────────

class DAOFactory {
    public static IDataAccess create() {
        return new InMemoryDataAccess();
    }
}


// ─────────────────────────────────────────────
// SERVICE
// ─────────────────────────────────────────────

interface IInteractionServices {
    void logInteraction(Interaction i) throws InvalidDataException;
    List<Interaction> getAll();
}

/**
 * InteractionManager manages interaction logging and ERP sync.
 * Follows SOLID Dependency Inversion: depends on IDataAccess and IERPConnector abstractions,
 * injected via constructor (not created internally).
 */
class InteractionManager implements IInteractionServices {

    private final IDataAccess dao;
    private final IERPConnector erpConnector;

    /**
     * Create an InteractionManager with injected dependencies.
     * @param dao data access abstraction
     * @param erpConnector ERP connector abstraction (the adapter)
     */
    public InteractionManager(IDataAccess dao, IERPConnector erpConnector) {
        this.dao = dao;
        this.erpConnector = erpConnector;
    }

    @Override
    public void logInteraction(Interaction i) throws InvalidDataException {
        if (i.getLeadId() == null && i.getCustomerId() == null)
            throw new InvalidDataException("Invalid interaction: both lead_id and customer_id are null");

        dao.executeUpdate("INSERT",
                i.getLeadId(),
                i.getCustomerId(),
                i.getType()
        );

        // Attempt ERP sync; log or handle ERPSyncException if needed
        try {
            if (i.getCustomerId() != null) {
                // Only sync interactions that reference customers
                // Note: In a real system, we'd fetch the Customer object here
                erpConnector.syncCustomer(new Customer(i.getCustomerId(), "", ""));
            }
        } catch (ERPSyncException e) {
            System.err.println("ERP sync warning (non-fatal): " + e.getMessage());
            // In production, log or retry asynchronously
        }
    }

    @Override
    public List<Interaction> getAll() {
        return dao.executeQuery("SELECT").stream()
            .map(r -> new Interaction(
                (Integer)r.get("lead_id"),
                (Integer)r.get("customer_id"),
                (String)r.get("type"),
                ""
            ))
            .collect(Collectors.toList());
    }
}


// ─────────────────────────────────────────────
// MAIN
// ─────────────────────────────────────────────

public class CRMInfrastructure {
    public static void main(String[] args) {

        IDataAccess dao = DAOFactory.create();
        
        // Create ERP connector (adapter) with injected legacy system (SOLID DIP)
        IERPConnector erpConnector = new ERPAdapter(new LegacyERPSystem());
        
        // Create service with injected DAO and ERP connector
        IInteractionServices service = new InteractionManager(dao, erpConnector);

        // Logging
        try {
            service.logInteraction(new Interaction(1, null, "call", "notes"));
            service.logInteraction(new Interaction(1, 1, "meeting", "notes"));
        } catch (InvalidDataException e) {
            System.err.println("Invalid interaction: " + e.getMessage());
        }

        // Fetch
        service.getAll().forEach(System.out::println);

        // State Pattern demo
        Lead lead = new Lead();
        System.out.println("State: " + lead.getStatus());
        lead.nextState();
        System.out.println("State: " + lead.getStatus());
        lead.nextState();
        System.out.println("State: " + lead.getStatus());
        
        // Customer Management + ERP sync demo
        System.out.println("\n--- Customer Management Demo ---");
        try {
            CustomerDAO custDao = new CustomerDAOInMemory();
            CustomerService custService = new CustomerService(custDao, erpConnector);
            
            int newId = custService.createCustomer(new Customer(0, "Alice", "alice@example.com"));
            Customer alice = custService.getCustomer(newId);
            System.out.println("Created: " + alice);
            custService.performERPSync(alice.getId());
        } catch (CustomerNotFoundException | ERPSyncException e) {
            System.err.println("Customer operation failed: " + e.getMessage());
        }
    }
}