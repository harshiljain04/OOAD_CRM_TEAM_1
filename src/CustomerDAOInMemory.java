import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * In-memory implementation of CustomerDAO that adapts the existing IDataAccess
 * (Harshita's DB layer) using DAOFactory. This is a thin adapter that speaks
 * the SQL-like tokens used by the in-memory DAO implementation.
 */
public class CustomerDAOInMemory implements CustomerDAO {

    private final IDataAccess dao = DAOFactory.create();

    @Override
    public Customer findById(int id) {
        List<Map<String,Object>> res = dao.executeQuery("FIND_CUSTOMER_BY_ID", id);
        if (res == null || res.isEmpty()) return null;
        Map<String,Object> r = res.get(0);
        return new Customer((Integer) r.get("id"), (String) r.get("name"), (String) r.get("email"));
    }

    @Override
    public List<Customer> findAll() {
        return dao.executeQuery("SELECT_CUSTOMERS").stream()
            .map(r -> new Customer((Integer) r.get("id"), (String) r.get("name"), (String) r.get("email")))
            .collect(Collectors.toList());
    }

    @Override
    public int create(Customer c) {
        // returns generated id in our in-memory DAO
        return dao.executeUpdate("INSERT_CUSTOMER", c.getName(), c.getEmail());
    }

    @Override
    public int update(Customer c) {
        return dao.executeUpdate("UPDATE_CUSTOMER", c.getId(), c.getName(), c.getEmail());
    }

    @Override
    public int delete(int id) {
        return dao.executeUpdate("DELETE_CUSTOMER", id);
    }
}
