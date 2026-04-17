/**
 * Contract for Sales Lead and Opportunity management.
 * Supports creating leads and progressing them through the sales pipeline.
 */
public interface ILeadServices {
    /**
     * Create a new lead in LEAD state.
     * @param name lead/company name
     * @param email contact email
     * @return generated lead id
     */
    int createLead(String name, String email);

    /**
     * Advance a lead to the next pipeline state.
     * LEAD -> OPPORTUNITY -> CUSTOMER.
     * @param leadId lead identifier
     * @throws LeadNotFoundException when lead does not exist
     */
    void advanceLeadStatus(int leadId) throws LeadNotFoundException;

    /**
     * Get current pipeline status of a lead.
     * @param leadId lead identifier
     * @return status name
     * @throws LeadNotFoundException when lead does not exist
     */
    String getLeadStatus(int leadId) throws LeadNotFoundException;
}
