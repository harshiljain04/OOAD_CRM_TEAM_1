import java.util.List;

/**
 * Data access object abstraction for Customer records.
 * Expected to be implemented by the project's DB layer.
 */
public interface CustomerDAO {
    /**
     * Find a customer by id.
     * @param id customer id
     * @return Customer or null if not found
     */
    Customer findById(int id);

    /**
     * Return all customers.
     * @return list of customers
     */
    List<Customer> findAll();

    /**
     * Create a customer. Returns the generated id (or >0) on success.
     * @param c customer (id may be ignored)
     * @return generated id or 0 on failure
     */
    int create(Customer c);

    /**
     * Update a customer.
     * @param c customer with id
     * @return number of rows updated (0 if not found)
     */
    int update(Customer c);

    /**
     * Delete customer by id.
     * @param id id
     * @return number of rows deleted (0 if not found)
     */
    int delete(int id);
}
