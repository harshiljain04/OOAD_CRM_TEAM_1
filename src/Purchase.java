import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Represents a purchase made by a customer.
 */
public class Purchase {
    private final int id;
    private final BigDecimal amount;
    private final LocalDateTime timestamp;
    private final String description;

    /**
     * Create a purchase record.
     * @param id unique identifier
     * @param amount monetary amount (use BigDecimal for money)
     * @param timestamp time of purchase
     * @param description optional description
     */
    public Purchase(int id, BigDecimal amount, LocalDateTime timestamp, String description) {
        this.id = id;
        this.amount = amount == null ? BigDecimal.ZERO : amount;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.description = description;
    }

    public int getId() { return id; }
    public BigDecimal getAmount() { return amount; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("Purchase #%d: %s at %s", id, amount.toPlainString(), timestamp.toString());
    }
}
