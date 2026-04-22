import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * Swing desktop frontend for CRM Team 1 modules.
 */
public class CRMSwingFrontend extends JFrame {

    private final ILeadServices leadService;
    private final CustomerService customerService;
    private final IInteractionServices interactionService;
    private final IMarketingServices marketingService;
    private final IAnalyticsReports analyticsService;

    private final JTextArea leadsOutput = outputArea();
    private final JTextArea customersOutput = outputArea();
    private final JTextArea interactionsOutput = outputArea();
    private final JTextArea marketingOutput = outputArea();

    public CRMSwingFrontend() {
        super("OOAD CRM Team 1 - Java Swing Frontend");

        IDataAccess dao = DAOFactory.create();
        IERPConnector erpConnector = new ERPAdapter(new LegacyERPSystem());

        this.leadService = new LeadService();
        this.customerService = new CustomerService(new CustomerDAOInMemory(), erpConnector);
        this.interactionService = new InteractionManager(dao, erpConnector);

        CampaignManager campaignManager = new CampaignManager();
        this.marketingService = campaignManager;
        this.analyticsService = new AnalyticsManager(campaignManager);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(1050, 760));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Leads", buildLeadsTab());
        tabs.addTab("Customers", buildCustomersTab());
        tabs.addTab("Interactions", buildInteractionsTab());
        tabs.addTab("Marketing & Analytics", buildMarketingTab());

        add(tabs, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    public static void launch() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // Fallback to default look and feel if system LAF is unavailable.
        }

        SwingUtilities.invokeLater(() -> new CRMSwingFrontend().setVisible(true));
    }

    private JPanel buildLeadsTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new GridLayout(3, 1, 10, 10));

        JPanel createPanel = titledPanel("Create Lead", new GridLayout(3, 2, 8, 8));
        JTextField leadNameField = new JTextField();
        JTextField leadEmailField = new JTextField();
        JButton createBtn = new JButton("Create");
        createPanel.add(new JLabel("Name/Company"));
        createPanel.add(leadNameField);
        createPanel.add(new JLabel("Email"));
        createPanel.add(leadEmailField);
        createPanel.add(new JLabel());
        createPanel.add(createBtn);

        createBtn.addActionListener(e -> {
            String name = leadNameField.getText().trim();
            String email = leadEmailField.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                showError("Name and email are required.");
                return;
            }
            int id = leadService.createLead(name, email);
            leadsOutput.setText("Lead created\nID: " + id + "\nStatus: LEAD");
        });

        JPanel statusPanel = titledPanel("View Lead Status", new GridLayout(2, 2, 8, 8));
        JTextField statusLeadIdField = new JTextField();
        JButton statusBtn = new JButton("View Status");
        statusPanel.add(new JLabel("Lead ID"));
        statusPanel.add(statusLeadIdField);
        statusPanel.add(new JLabel());
        statusPanel.add(statusBtn);

        statusBtn.addActionListener(e -> {
            Integer id = parseInt(statusLeadIdField.getText(), "Lead ID");
            if (id == null) return;
            try {
                String status = leadService.getLeadStatus(id);
                leadsOutput.setText("Lead #" + id + " status: " + status);
            } catch (LeadNotFoundException ex) {
                showError(ex.getMessage());
            }
        });

        JPanel advancePanel = titledPanel("Advance Lead Status", new GridLayout(2, 2, 8, 8));
        JTextField advanceLeadIdField = new JTextField();
        JButton advanceBtn = new JButton("Advance");
        advancePanel.add(new JLabel("Lead ID"));
        advancePanel.add(advanceLeadIdField);
        advancePanel.add(new JLabel());
        advancePanel.add(advanceBtn);

        advanceBtn.addActionListener(e -> {
            Integer id = parseInt(advanceLeadIdField.getText(), "Lead ID");
            if (id == null) return;
            try {
                leadService.advanceLeadStatus(id);
                String status = leadService.getLeadStatus(id);
                leadsOutput.setText("Lead #" + id + " updated to: " + status);
            } catch (LeadNotFoundException ex) {
                showError(ex.getMessage());
            } catch (RuntimeException ex) {
                showError("Cannot advance lead: " + ex.getMessage());
            }
        });

        actions.add(createPanel);
        actions.add(statusPanel);
        actions.add(advancePanel);

        root.add(actions, BorderLayout.CENTER);
        root.add(new JScrollPane(leadsOutput), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildCustomersTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new GridLayout(2, 2, 10, 10));

        JPanel createPanel = titledPanel("Create Customer", new GridLayout(3, 2, 8, 8));
        JTextField createName = new JTextField();
        JTextField createEmail = new JTextField();
        JButton createBtn = new JButton("Create");
        createPanel.add(new JLabel("Name"));
        createPanel.add(createName);
        createPanel.add(new JLabel("Email"));
        createPanel.add(createEmail);
        createPanel.add(new JLabel());
        createPanel.add(createBtn);

        createBtn.addActionListener(e -> {
            String name = createName.getText().trim();
            String email = createEmail.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                showError("Name and email are required.");
                return;
            }
            int id = customerService.createCustomer(new Customer(0, name, email));
            customersOutput.setText("Customer created with ID: " + id);
        });

        JPanel updatePanel = titledPanel("Update Customer", new GridLayout(4, 2, 8, 8));
        JTextField updateId = new JTextField();
        JTextField updateName = new JTextField();
        JTextField updateEmail = new JTextField();
        JButton updateBtn = new JButton("Update");
        updatePanel.add(new JLabel("Customer ID"));
        updatePanel.add(updateId);
        updatePanel.add(new JLabel("Name"));
        updatePanel.add(updateName);
        updatePanel.add(new JLabel("Email"));
        updatePanel.add(updateEmail);
        updatePanel.add(new JLabel());
        updatePanel.add(updateBtn);

        updateBtn.addActionListener(e -> {
            Integer id = parseInt(updateId.getText(), "Customer ID");
            if (id == null) return;
            String name = updateName.getText().trim();
            String email = updateEmail.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                showError("Name and email are required.");
                return;
            }
            int updated = customerService.updateCustomer(new Customer(id, name, email));
            customersOutput.setText("Updated rows: " + updated);
        });

        JPanel syncDeletePanel = titledPanel("Sync / Delete", new GridLayout(3, 2, 8, 8));
        JTextField syncDeleteId = new JTextField();
        JButton syncBtn = new JButton("Sync to ERP");
        JButton deleteBtn = new JButton("Delete");
        syncDeletePanel.add(new JLabel("Customer ID"));
        syncDeletePanel.add(syncDeleteId);
        syncDeletePanel.add(syncBtn);
        syncDeletePanel.add(deleteBtn);
        syncDeletePanel.add(new JLabel());
        syncDeletePanel.add(new JLabel());

        syncBtn.addActionListener(e -> {
            Integer id = parseInt(syncDeleteId.getText(), "Customer ID");
            if (id == null) return;
            try {
                customerService.performERPSync(id);
                customersOutput.setText("ERP sync successful for customer #" + id);
            } catch (CustomerNotFoundException | ERPSyncException ex) {
                showError(ex.getMessage());
            }
        });

        deleteBtn.addActionListener(e -> {
            Integer id = parseInt(syncDeleteId.getText(), "Customer ID");
            if (id == null) return;
            int deleted = customerService.deleteCustomer(id);
            customersOutput.setText("Deleted rows: " + deleted);
        });

        JPanel listPanel = titledPanel("List Customers", new FlowLayout(FlowLayout.LEFT));
        JButton listBtn = new JButton("Refresh List");
        listPanel.add(listBtn);

        listBtn.addActionListener(e -> {
            List<Customer> customers = customerService.listCustomers();
            if (customers.isEmpty()) {
                customersOutput.setText("No customers found.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Customer c : customers) {
                sb.append("ID=").append(c.getId())
                    .append(" | ").append(c.getName())
                    .append(" <").append(c.getEmail()).append(">")
                    .append(" | LTV=").append(c.calculateLifetimeValue().toPlainString())
                    .append("\n");
            }
            customersOutput.setText(sb.toString());
        });

        actions.add(createPanel);
        actions.add(updatePanel);
        actions.add(syncDeletePanel);
        actions.add(listPanel);

        root.add(actions, BorderLayout.CENTER);
        root.add(new JScrollPane(customersOutput), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildInteractionsTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new GridLayout(2, 1, 10, 10));

        JPanel logPanel = titledPanel("Log Interaction", new GridLayout(5, 2, 8, 8));
        JTextField leadIdField = new JTextField();
        JTextField customerIdField = new JTextField();
        JTextField typeField = new JTextField("call");
        JTextField notesField = new JTextField();
        JButton logBtn = new JButton("Log");
        logPanel.add(new JLabel("Lead ID (optional)"));
        logPanel.add(leadIdField);
        logPanel.add(new JLabel("Customer ID (optional)"));
        logPanel.add(customerIdField);
        logPanel.add(new JLabel("Type (call/email/meeting)"));
        logPanel.add(typeField);
        logPanel.add(new JLabel("Notes"));
        logPanel.add(notesField);
        logPanel.add(new JLabel());
        logPanel.add(logBtn);

        logBtn.addActionListener(e -> {
            Integer leadId = parseOptionalInt(leadIdField.getText(), "Lead ID");
            Integer customerId = parseOptionalInt(customerIdField.getText(), "Customer ID");
            if (leadId == Integer.MIN_VALUE || customerId == Integer.MIN_VALUE) {
                return;
            }

            String type = typeField.getText().trim();
            String notes = notesField.getText().trim();
            if (type.isEmpty()) {
                showError("Type is required.");
                return;
            }

            try {
                interactionService.logInteraction(new Interaction(leadId, customerId, type, notes));
                interactionsOutput.setText("Interaction logged successfully.");
            } catch (InvalidDataException ex) {
                showError(ex.getMessage());
            }
        });

        JPanel listPanel = titledPanel("List Interactions", new FlowLayout(FlowLayout.LEFT));
        JButton listBtn = new JButton("Refresh List");
        listPanel.add(listBtn);

        listBtn.addActionListener(e -> {
            List<Interaction> interactions = interactionService.getAll();
            if (interactions.isEmpty()) {
                interactionsOutput.setText("No interactions found.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Interaction i : interactions) {
                sb.append("ID=").append(i.getId())
                    .append(" | lead=").append(i.getLeadId())
                    .append(" | customer=").append(i.getCustomerId())
                    .append(" | type=").append(i.getType())
                    .append(" | notes=").append(i.getNotes())
                    .append(" | ts=").append(i.getTimestamp())
                    .append("\n");
            }
            interactionsOutput.setText(sb.toString());
        });

        actions.add(logPanel);
        actions.add(listPanel);

        root.add(actions, BorderLayout.CENTER);
        root.add(new JScrollPane(interactionsOutput), BorderLayout.SOUTH);
        return root;
    }

    private JPanel buildMarketingTab() {
        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel actions = new JPanel(new GridLayout(3, 1, 10, 10));

        JPanel createPanel = titledPanel("Create Campaign", new GridLayout(6, 2, 8, 8));
        JTextField nameField = new JTextField();
        JTextField audienceField = new JTextField();
        JTextField budgetField = new JTextField();
        JTextField startDateField = new JTextField("2026-04-22");
        JTextField endDateField = new JTextField("2026-05-22");
        JButton createBtn = new JButton("Create Campaign");
        createPanel.add(new JLabel("Name"));
        createPanel.add(nameField);
        createPanel.add(new JLabel("Audience"));
        createPanel.add(audienceField);
        createPanel.add(new JLabel("Budget"));
        createPanel.add(budgetField);
        createPanel.add(new JLabel("Start Date (YYYY-MM-DD)"));
        createPanel.add(startDateField);
        createPanel.add(new JLabel("End Date (YYYY-MM-DD)"));
        createPanel.add(endDateField);
        createPanel.add(new JLabel());
        createPanel.add(createBtn);

        createBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            String audience = audienceField.getText().trim();
            Double budget = parseDouble(budgetField.getText(), "Budget");
            if (budget == null) return;

            try {
                Campaign c = marketingService.createCampaign(
                        name,
                        audience,
                        budget,
                        LocalDate.parse(startDateField.getText().trim()),
                        LocalDate.parse(endDateField.getText().trim())
                );
                marketingOutput.setText("Campaign created: " + campaignLine(c));
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        JPanel updatePanel = titledPanel("Revenue / Status", new GridLayout(3, 4, 8, 8));
        JTextField campaignIdField = new JTextField();
        JTextField revenueField = new JTextField();
        JComboBox<CampaignStatus> statusBox = new JComboBox<>(CampaignStatus.values());
        JButton revenueBtn = new JButton("Record Revenue");
        JButton statusBtn = new JButton("Update Status");
        updatePanel.add(new JLabel("Campaign ID"));
        updatePanel.add(campaignIdField);
        updatePanel.add(new JLabel("Revenue"));
        updatePanel.add(revenueField);
        updatePanel.add(new JLabel("Status"));
        updatePanel.add(statusBox);
        updatePanel.add(revenueBtn);
        updatePanel.add(statusBtn);
        updatePanel.add(new JLabel());
        updatePanel.add(new JLabel());
        updatePanel.add(new JLabel());
        updatePanel.add(new JLabel());

        revenueBtn.addActionListener(e -> {
            String id = campaignIdField.getText().trim();
            Double revenue = parseDouble(revenueField.getText(), "Revenue");
            if (revenue == null) return;
            try {
                marketingService.recordRevenue(id, revenue);
                Campaign c = marketingService.getCampaignById(id);
                marketingOutput.setText("Revenue updated: " + campaignLine(c));
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        statusBtn.addActionListener(e -> {
            String id = campaignIdField.getText().trim();
            CampaignStatus status = (CampaignStatus) statusBox.getSelectedItem();
            if (status == null) {
                showError("Status is required.");
                return;
            }
            try {
                marketingService.updateStatus(id, status);
                Campaign c = marketingService.getCampaignById(id);
                marketingOutput.setText("Status updated: " + campaignLine(c));
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        JPanel listPanel = titledPanel("Campaigns & Analytics", new FlowLayout(FlowLayout.LEFT));
        JButton listBtn = new JButton("List Campaigns");
        JButton analyticsBtn = new JButton("Show Analytics Summary");
        listPanel.add(listBtn);
        listPanel.add(analyticsBtn);

        listBtn.addActionListener(e -> {
            List<Campaign> campaigns = marketingService.getAllCampaigns();
            if (campaigns.isEmpty()) {
                marketingOutput.setText("No campaigns found.");
                return;
            }
            StringBuilder sb = new StringBuilder();
            for (Campaign c : campaigns) {
                sb.append(campaignLine(c)).append("\n");
            }
            marketingOutput.setText(sb.toString());
        });

        analyticsBtn.addActionListener(e -> {
            AnalyticsReport r = analyticsService.generateReport();
            String summary = "Campaigns=" + r.getTotalCampaigns()
                    + "\nTotal Budget=" + r.getTotalBudgetSpent()
                    + "\nTotal Revenue=" + r.getTotalRevenue()
                    + "\nAverage ROI=" + String.format("%.2f%%", r.getAverageROI())
                    + "\nBest Performer=" + r.getBestPerformerName()
                    + " (" + String.format("%.2f%%", r.getBestPerformerROI()) + ")";
            marketingOutput.setText(summary);
        });

        actions.add(createPanel);
        actions.add(updatePanel);
        actions.add(listPanel);

        root.add(actions, BorderLayout.CENTER);
        root.add(new JScrollPane(marketingOutput), BorderLayout.SOUTH);
        return root;
    }

    private JPanel titledPanel(String title, java.awt.LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setBorder(BorderFactory.createTitledBorder(title));
        return panel;
    }

    private JTextArea outputArea() {
        JTextArea area = new JTextArea(9, 40);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return area;
    }

    private Integer parseInt(String raw, String label) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception e) {
            showError(label + " must be a valid integer.");
            return null;
        }
    }

    private Integer parseOptionalInt(String raw, String label) {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return Integer.parseInt(trimmed);
        } catch (NumberFormatException e) {
            showError(label + " must be a valid integer.");
            return Integer.MIN_VALUE;
        }
    }

    private Double parseDouble(String raw, String label) {
        try {
            return Double.parseDouble(raw.trim());
        } catch (Exception e) {
            showError(label + " must be a valid number.");
            return null;
        }
    }

    private String campaignLine(Campaign c) {
        return "ID=" + c.getId()
                + " | " + c.getName()
                + " | audience=" + c.getTargetAudience()
                + " | status=" + c.getStatus()
                + " | budget=" + c.getBudget()
                + " | revenue=" + c.getRevenue()
                + " | ROI=" + c.getFormattedROI();
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}
