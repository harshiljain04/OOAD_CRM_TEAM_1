import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Customer holds personal data and its purchases.
 *
 * Follows GRASP Information Expert: Customer is the natural owner of purchase data
 * and therefore is responsible for calculating its lifetime value.
 */
public class Customer {
    private final int id;
    private String name;
    private String email;
    private final List<Purchase> purchases = new ArrayList<>();

    /**
     * Create a customer with id, name and email.
     * @param id unique identifier
     * @param name customer's name
     * @param email customer's email
     */
    public Customer(int id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }

    /**
     * Return an unmodifiable view of purchases.
     * @return list of purchases
     */
    public List<Purchase> getPurchases() {
        return Collections.unmodifiableList(purchases);
    }

    /**
     * Add a purchase to the customer record.
     * @param p purchase to add (ignored if null)
     */
    public void addPurchase(Purchase p) {
        if (p != null) purchases.add(p);
    }

    /**
     * Calculate the lifetime value (LTV) as the sum of all purchase amounts.
     * Uses BigDecimal for monetary accuracy.
     * @return total amount of all purchases (zero if none)
     */
    public BigDecimal calculateLifetimeValue() {
        return purchases.stream()
            .map(Purchase::getAmount)
            .filter(a -> a != null)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return String.format("Customer #%d: %s <%s> (purchases=%d)", id, name, email, purchases.size());
    }
}
