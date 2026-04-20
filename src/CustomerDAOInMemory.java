import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Backward-compatible CustomerDAO implementation name, now backed by the
 * Integration team's ERP SDK facade for CRM.
 */
public class CustomerDAOInMemory implements CustomerDAO {

    private static final String TABLE_NAME = "customers";
    private static final String ID_COLUMN = "customer_id";
    private static final String DEFAULT_USERNAME = "integration_lead";

    private static volatile Object crmFacade;
    private static volatile String erpUsername;

    private static Object crm() {
        if (crmFacade == null) {
            synchronized (CustomerDAOInMemory.class) {
                if (crmFacade == null) {
                    initializeFacade();
                }
            }
        }
        return crmFacade;
    }

    private static String username() {
        if (erpUsername == null || erpUsername.isBlank()) {
            erpUsername = resolveUsername();
        }
        return erpUsername;
    }

    private static void initializeFacade() {
        try {
            Path configPath = resolveConfigPath();
            Class<?> databaseConfigClass = Class.forName("com.erp.sdk.config.DatabaseConfig");
            Method fromProperties = databaseConfigClass.getMethod("fromProperties", Path.class);
            Object config = fromProperties.invoke(null, configPath);

            Class<?> subsystemNameClass = Class.forName("com.erp.sdk.subsystem.SubsystemName");
            Method enumValueOf = subsystemNameClass.getMethod("valueOf", String.class);
            Object crmEnum = enumValueOf.invoke(null, "CRM");

            Class<?> subsystemFactoryClass = Class.forName("com.erp.sdk.factory.SubsystemFactory");
            Method create = subsystemFactoryClass.getMethod("create", subsystemNameClass, databaseConfigClass);
            crmFacade = create.invoke(null, crmEnum, config);

            erpUsername = resolveUsername();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    if (crmFacade != null) {
                        Method close = crmFacade.getClass().getMethod("close");
                        close.invoke(crmFacade);
                    }
                } catch (Exception ignored) {
                    // Best effort close.
                }
            }));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize CRM RDS integration: " + e.getMessage(), e);
        }
    }

    private static Path resolveConfigPath() {
        String fromSystemProperty = System.getProperty("crm.rds.config");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            Path candidate = Path.of(fromSystemProperty.trim());
            if (Files.exists(candidate)) {
                return candidate;
            }
            throw new IllegalStateException("CRM RDS config path from -Dcrm.rds.config not found: " + candidate);
        }

        String fromEnv = System.getenv("CRM_RDS_CONFIG");
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path candidate = Path.of(fromEnv.trim());
            if (Files.exists(candidate)) {
                return candidate;
            }
            throw new IllegalStateException("CRM RDS config path from CRM_RDS_CONFIG not found: " + candidate);
        }

        Path[] defaults = new Path[] {
            Path.of("DB_Integration", "application-rds.properties"),
            Path.of("application-rds.properties"),
            Path.of("DB_Integration", "application-rds-template.properties")
        };

        for (Path path : defaults) {
            if (Files.exists(path)) {
                return path;
            }
        }

        throw new IllegalStateException(
            "CRM RDS config file not found. Set -Dcrm.rds.config=... or CRM_RDS_CONFIG, " +
            "or add DB_Integration/application-rds-template.properties"
        );
    }

    private static String resolveUsername() {
        String fromSystemProperty = System.getProperty("crm.erp.username");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            return fromSystemProperty.trim();
        }

        String fromEnv = System.getenv("CRM_ERP_USERNAME");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }

        return DEFAULT_USERNAME;
    }

    private Customer mapRowToCustomer(Map<String, Object> row) {
        Object idRaw = row.get(ID_COLUMN);
        if (idRaw == null) {
            idRaw = row.get("id");
        }
        if (!(idRaw instanceof Number)) {
            throw new IllegalStateException("Unexpected customer id type: " + idRaw);
        }

        int id = ((Number) idRaw).intValue();
        String name = row.get("name") == null ? "" : String.valueOf(row.get("name"));
        String email = row.get("email") == null ? "" : String.valueOf(row.get("email"));
        return new Customer(id, name, email);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readById(int id) {
        try {
            Method method = crm().getClass().getMethod(
                "readById", String.class, String.class, Object.class, String.class
            );
            return (Map<String, Object>) method.invoke(crm(), TABLE_NAME, ID_COLUMN, id, username());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read customer by id via ERP SDK: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readAll() {
        try {
            Method method = crm().getClass().getMethod(
                "readAll", String.class, Map.class, String.class
            );
            return (List<Map<String, Object>>) method.invoke(crm(), TABLE_NAME, Map.of(), username());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read all customers via ERP SDK: " + e.getMessage(), e);
        }
    }

    private long createRow(Map<String, Object> payload) {
        try {
            Method method = crm().getClass().getMethod(
                "create", String.class, Map.class, String.class
            );
            Object createdId = method.invoke(crm(), TABLE_NAME, payload, username());
            if (!(createdId instanceof Number)) {
                throw new IllegalStateException("Unexpected create result type: " + createdId);
            }
            return ((Number) createdId).longValue();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create customer via ERP SDK: " + e.getMessage(), e);
        }
    }

    private int updateRow(int id, Map<String, Object> payload) {
        try {
            Method method = crm().getClass().getMethod(
                "update", String.class, String.class, Object.class, Map.class, String.class
            );
            Object updated = method.invoke(crm(), TABLE_NAME, ID_COLUMN, id, payload, username());
            if (!(updated instanceof Number)) {
                throw new IllegalStateException("Unexpected update result type: " + updated);
            }
            return ((Number) updated).intValue();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to update customer via ERP SDK: " + e.getMessage(), e);
        }
    }

    private int deleteRow(int id) {
        try {
            Method method = crm().getClass().getMethod(
                "delete", String.class, String.class, Object.class, String.class
            );
            Object deleted = method.invoke(crm(), TABLE_NAME, ID_COLUMN, id, username());
            if (!(deleted instanceof Number)) {
                throw new IllegalStateException("Unexpected delete result type: " + deleted);
            }
            return ((Number) deleted).intValue();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete customer via ERP SDK: " + e.getMessage(), e);
        }
    }

    @Override
    public Customer findById(int id) {
        Map<String, Object> row = readById(id);
        if (row == null || row.isEmpty()) {
            return null;
        }
        return mapRowToCustomer(row);
    }

    @Override
    public List<Customer> findAll() {
        return readAll().stream()
            .map(this::mapRowToCustomer)
            .collect(Collectors.toList());
    }

    @Override
    public int create(Customer c) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", c.getName());
        payload.put("email", c.getEmail());

        long id = createRow(payload);
        return Math.toIntExact(id);
    }

    @Override
    public int update(Customer c) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", c.getName());
        payload.put("email", c.getEmail());
        return updateRow(c.getId(), payload);
    }

    @Override
    public int delete(int id) {
        return deleteRow(id);
    }
}
