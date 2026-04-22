/**
 * Service layer for customer operations. Uses a CustomerDAO to access data and an
 * IERPConnector to sync to the ERP system.
 */
import java.util.List;

public class CustomerService {

    private final CustomerDAO dao;
    private final IERPConnector erpConnector;

    /**
     * Create a CustomerService with given DAO and ERP connector.
     * @param dao data access implementation
     * @param erpConnector ERP connector/adapter
     */
    public CustomerService(CustomerDAO dao, IERPConnector erpConnector) {
        this.dao = dao;
        this.erpConnector = erpConnector;
    }

    /**
     * Fetch a customer by id.
     * @param id customer id
     * @return Customer
     * @throws CustomerNotFoundException if not found
     */
    public Customer getCustomer(int id) throws CustomerNotFoundException {
        Customer c = dao.findById(id);
        if (c == null) throw new CustomerNotFoundException("Customer " + id + " not found");
        return c;
    }

    /**
     * Perform an ERP synchronization for a customer identified by id.
     * @param customerId id
     * @throws CustomerNotFoundException if customer is missing
     * @throws ERPSyncException if ERP sync fails
     */
    public void performERPSync(int customerId) throws CustomerNotFoundException, ERPSyncException {
        Customer c = getCustomer(customerId);
        erpConnector.syncCustomer(c);
    }

    // Additional convenience methods delegating to DAO
    public int createCustomer(Customer c) { return dao.create(c); }
    public int updateCustomer(Customer c) { return dao.update(c); }
    public int deleteCustomer(int id) { return dao.delete(id); }
    public List<Customer> listCustomers() { return dao.findAll(); }
}
