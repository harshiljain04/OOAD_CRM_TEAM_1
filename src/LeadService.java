import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of ILeadServices.
 * Uses the existing Lead state model to enforce valid status transitions.
 */
public class LeadService implements ILeadServices {

    private static class LeadRecord {
        private final String name;
        private final String email;
        private final Lead lead;

        LeadRecord(int id, String name, String email) {
            this.name = name;
            this.email = email;
            this.lead = new Lead();
        }
    }

    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final Map<Integer, LeadRecord> leads = new HashMap<>();
    private final CustomerDAO customerDAO;

    public LeadService() {
        this(new CustomerDAOInMemory());
    }

    public LeadService(CustomerDAO customerDAO) {
        this.customerDAO = customerDAO;
    }

    @Override
    public int createLead(String name, String email) {
        int id = idCounter.getAndIncrement();
        leads.put(id, new LeadRecord(id, name, email));
        return id;
    }

    @Override
    public void advanceLeadStatus(int leadId) throws LeadNotFoundException {
        LeadRecord record = getLeadRecordOrThrow(leadId);
        String before = record.lead.getStatus();
        record.lead.nextState();
        String after = record.lead.getStatus();

        // Keep lead pipeline and customer data in sync for frontend demos.
        if (!"CUSTOMER".equals(before) && "CUSTOMER".equals(after)) {
            customerDAO.create(new Customer(0, record.name, record.email));
        }
    }

    @Override
    public String getLeadStatus(int leadId) throws LeadNotFoundException {
        LeadRecord record = getLeadRecordOrThrow(leadId);
        return record.lead.getStatus();
    }

    private LeadRecord getLeadRecordOrThrow(int leadId) throws LeadNotFoundException {
        LeadRecord record = leads.get(leadId);
        if (record == null) {
            throw new LeadNotFoundException("Lead " + leadId + " not found");
        }
        return record;
    }
}
