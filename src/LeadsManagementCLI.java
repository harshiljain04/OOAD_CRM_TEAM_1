import java.util.Scanner;

/**
 * Simple terminal UI for Sales Lead and Opportunity management.
 * Launch via: java -cp src CRMInfrastructure cli
 */
public class LeadsManagementCLI {

    private final ILeadServices leadServices;
    private final Scanner scanner;

    public LeadsManagementCLI(ILeadServices leadServices, Scanner scanner) {
        this.leadServices = leadServices;
        this.scanner = scanner;
    }

    public void run() {
        System.out.println("=== Leads Management Screen ===");
        boolean running = true;

        while (running) {
            printMenu();
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    createLead();
                    break;
                case "2":
                    advanceLeadStatus();
                    break;
                case "3":
                    viewLeadStatus();
                    break;
                case "0":
                    running = false;
                    System.out.println("Exiting lead screen.");
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void printMenu() {
        System.out.println();
        System.out.println("1) Create Lead");
        System.out.println("2) Update Lead Status");
        System.out.println("3) View Lead Status");
        System.out.println("0) Exit");
        System.out.print("Choose: ");
    }

    private void createLead() {
        System.out.print("Lead name/company: ");
        String name = scanner.nextLine().trim();

        System.out.print("Contact email: ");
        String email = scanner.nextLine().trim();

        int id = leadServices.createLead(name, email);
        System.out.println("Lead created with id=" + id + " and status=LEAD");
    }

    private void advanceLeadStatus() {
        Integer id = promptLeadId();
        if (id == null) return;

        try {
            leadServices.advanceLeadStatus(id);
            System.out.println("Lead #" + id + " updated to status=" + leadServices.getLeadStatus(id));
        } catch (LeadNotFoundException e) {
            System.out.println(e.getMessage());
        } catch (RuntimeException e) {
            // CustomerState throws when trying to move beyond CUSTOMER.
            System.out.println("Cannot advance status: " + e.getMessage());
        }
    }

    private void viewLeadStatus() {
        Integer id = promptLeadId();
        if (id == null) return;

        try {
            System.out.println("Lead #" + id + " status=" + leadServices.getLeadStatus(id));
        } catch (LeadNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private Integer promptLeadId() {
        System.out.print("Lead id: ");
        String raw = scanner.nextLine().trim();
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            System.out.println("Lead id must be a number.");
            return null;
        }
    }
}
