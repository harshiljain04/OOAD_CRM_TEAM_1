/**
 * Mock legacy ERP system with a different API (sendData).
 * This class represents the incompatible interface we need to adapt.
 * Can be mocked or injected for testing and flexibility.
 */
public class LegacyERPSystem {
    /**
     * Send JSON-formatted data to the legacy system.
     * @param json JSON data string
     */
    public void sendData(String json) {
        System.out.println("[LegacyERP] " + json);
    }
}
