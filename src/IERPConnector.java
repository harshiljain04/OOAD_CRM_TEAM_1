/**
 * Abstraction for ERP connectors. Implements the Adapter pattern contract.
 */
public interface IERPConnector {
    /**
     * Sync a customer to the ERP system.
     * @param c customer to sync
     * @throws ERPSyncException when the sync fails
     */
    void syncCustomer(Customer c) throws ERPSyncException;
}
