/**
 * Adapter that implements the IERPConnector contract and wraps the legacy ERP.
 * Converts Customer objects to JSON strings before sending to the legacy API.
 * Follows the Adapter Pattern: adapts the incompatible LegacyERPSystem interface
 * to the IERPConnector abstraction, allowing clients to depend only on IERPConnector.
 */
public class ERPAdapter implements IERPConnector {
    private final LegacyERPSystem legacy;

    /**
     * Create an ERPAdapter with a given LegacyERPSystem instance.
     * This constructor injection allows mocking/testing and follows SOLID Dependency Inversion.
     * @param legacy the legacy ERP system to adapt
     */
    public ERPAdapter(LegacyERPSystem legacy) {
        this.legacy = legacy;
    }

    /**
     * Sync a customer by converting it to JSON and sending via the legacy API.
     * @param c customer to sync
     * @throws ERPSyncException if sync fails
     */
    @Override
    public void syncCustomer(Customer c) throws ERPSyncException {
        if (c == null) throw new ERPSyncException("Customer is null");
        try {
            String json = toJson(c);
            legacy.sendData(json);
        } catch (Exception e) {
            throw new ERPSyncException("Failed to sync customer", e);
        }
    }

    /**
     * Backwards-compatible method for interaction flows.
     * Converts an Interaction to JSON-like format and sends via the legacy API.
     * @param i interaction to send
     */
    public void sendInteraction(Interaction i) {
        String payload = "{id:" + i.getId() + ", type:" + i.getType() + "}";
        legacy.sendData(payload);
    }

    /**
     * Convert a Customer to JSON format (without external libraries).
     * @param c customer
     * @return JSON string representation
     */
    private String toJson(Customer c) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":").append(c.getId()).append(',');
        sb.append("\"name\":\"").append(escape(c.getName())).append("\",");
        sb.append("\"email\":\"").append(escape(c.getEmail())).append("\"");
        sb.append('}');
        return sb.toString();
    }

    /**
     * Escape special characters in strings for JSON safety.
     * @param s input string
     * @return escaped string safe for JSON
     */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
