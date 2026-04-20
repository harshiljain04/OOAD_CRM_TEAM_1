import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 * Simple smoke check to verify CRM can reach the shared ERP SDK + RDS.
 */
public class CRMRdsSmokeCheck {

    public static void main(String[] args) throws Exception {
        Path configPath = resolveConfigPath();
        String username = resolveUsername();

        Class<?> databaseConfigClass = Class.forName("com.erp.sdk.config.DatabaseConfig");
        Method fromProperties = databaseConfigClass.getMethod("fromProperties", Path.class);
        Object config = fromProperties.invoke(null, configPath);

        Class<?> subsystemNameClass = Class.forName("com.erp.sdk.subsystem.SubsystemName");
        Method enumValueOf = subsystemNameClass.getMethod("valueOf", String.class);
        Object crmEnum = enumValueOf.invoke(null, "CRM");

        Class<?> subsystemFactoryClass = Class.forName("com.erp.sdk.factory.SubsystemFactory");
        Method create = subsystemFactoryClass.getMethod("create", subsystemNameClass, databaseConfigClass);
        Object crm = create.invoke(null, crmEnum, config);

        try {
            Method readAll = crm.getClass().getMethod("readAll", String.class, Map.class, String.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> customers = (List<Map<String, Object>>) readAll.invoke(
                crm,
                "customers",
                Map.of(),
                username
            );

            System.out.println("CRM RDS connectivity OK");
            System.out.println("Config: " + configPath);
            System.out.println("User: " + username);
            System.out.println("Customers visible: " + customers.size());
        } finally {
            Method close = crm.getClass().getMethod("close");
            close.invoke(crm);
        }
    }

    private static Path resolveConfigPath() {
        String fromSystemProperty = System.getProperty("crm.rds.config");
        if (fromSystemProperty != null && !fromSystemProperty.isBlank()) {
            Path candidate = Path.of(fromSystemProperty.trim());
            if (Files.exists(candidate)) {
                return candidate;
            }
            throw new IllegalStateException("Config path from -Dcrm.rds.config not found: " + candidate);
        }

        String fromEnv = System.getenv("CRM_RDS_CONFIG");
        if (fromEnv != null && !fromEnv.isBlank()) {
            Path candidate = Path.of(fromEnv.trim());
            if (Files.exists(candidate)) {
                return candidate;
            }
            throw new IllegalStateException("Config path from CRM_RDS_CONFIG not found: " + candidate);
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

        throw new IllegalStateException("No RDS properties file found");
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

        return "integration_lead";
    }
}
