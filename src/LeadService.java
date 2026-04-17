import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-memory implementation of ILeadServices.
 * Uses the existing Lead state model to enforce valid status transitions.
 */
public class LeadService implements ILeadServices {

    private static class LeadRecord {
        private final int id;
        private final String name;
        private final String email;
        private final Lead lead;

        LeadRecord(int id, String name, String email) {
            this.id = id;
            this.name = name;
            this.email = email;
            this.lead = new Lead();
        }
    }

    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final Map<Integer, LeadRecord> leads = new HashMap<>();

    @Override
    public int createLead(String name, String email) {
        int id = idCounter.getAndIncrement();
        leads.put(id, new LeadRecord(id, name, email));
        return id;
    }

    @Override
    public void advanceLeadStatus(int leadId) throws LeadNotFoundException {
        LeadRecord record = getLeadRecordOrThrow(leadId);
        record.lead.nextState();
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
