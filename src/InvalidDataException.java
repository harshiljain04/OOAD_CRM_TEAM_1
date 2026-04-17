/**
 * Exception thrown when provided data is invalid or fails validation rules.
 */
public class InvalidDataException extends Exception {
    /**
     * Create an InvalidDataException with a custom message.
     * @param message explanation of the validation failure
     */
    public InvalidDataException(String message) { super(message); }
}
