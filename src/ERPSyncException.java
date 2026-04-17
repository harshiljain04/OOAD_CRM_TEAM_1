/**
 * Exception thrown when syncing with the ERP system fails.
 */
public class ERPSyncException extends Exception {
    public ERPSyncException(String message) { super(message); }
    public ERPSyncException(String message, Throwable cause) { super(message, cause); }
}
