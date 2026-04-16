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

    public int executeUpdate(String sql, Object... params) {
        String conn = pool.getConnection();
        try {
            Map<String,Object> row = CentralDatabase.row(
                "lead_id", params[0],
                "customer_id", params[1],
                "type", params[2]
            );
            CentralDatabase.interactions.add(row);
            return 1;
        } finally {
            pool.releaseConnection(conn);
        }
    }

    public List<Map<String,Object>> executeQuery(String sql, Object... params) {
        return new ArrayList<>(CentralDatabase.interactions);
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
// ADAPTER PATTERN
// ─────────────────────────────────────────────

class ExternalERP {
    public void send(String data) {
        System.out.println("[ERP] " + data);
    }
}

class ERPAdapter {
    private final ExternalERP erp = new ExternalERP();

    public void sendInteraction(Interaction i) {
        String payload = "{id:" + i.getId() + ", type:" + i.getType() + "}";
        erp.send(payload);
    }
}


// ─────────────────────────────────────────────
// SERVICE
// ─────────────────────────────────────────────

interface IInteractionServices {
    void logInteraction(Interaction i);
    List<Interaction> getAll();
}

class InteractionManager implements IInteractionServices {

    private final IDataAccess dao;
    private final ERPAdapter adapter = new ERPAdapter();

    public InteractionManager(IDataAccess dao) {
        this.dao = dao;
    }

    public void logInteraction(Interaction i) {
        if (i.getLeadId() == null && i.getCustomerId() == null)
            throw new RuntimeException("Invalid interaction");

        dao.executeUpdate("INSERT",
                i.getLeadId(),
                i.getCustomerId(),
                i.getType()
        );

        adapter.sendInteraction(i); // Adapter call
    }

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
        IInteractionServices service = new InteractionManager(dao);

        // Logging
        service.logInteraction(new Interaction(1, null, "call", "notes"));
        service.logInteraction(new Interaction(1, 1, "meeting", "notes"));

        // Fetch
        service.getAll().forEach(System.out::println);

        // State Pattern demo
        Lead lead = new Lead();
        System.out.println("State: " + lead.getStatus());
        lead.nextState();
        System.out.println("State: " + lead.getStatus());
        lead.nextState();
        System.out.println("State: " + lead.getStatus());
    }
}