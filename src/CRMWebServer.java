import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * Lightweight HTTP server that exposes CRM module functionality to a browser frontend.
 * Run with: java -cp src CRMWebServer
 */
public class CRMWebServer {

    private final ILeadServices leadService;
    private final CustomerService customerService;
    private final IInteractionServices interactionService;
    private final IMarketingServices marketingService;
    private final IAnalyticsReports analyticsService;

    private CRMWebServer() {
        CustomerDAO customerDAO = new CustomerDAOInMemory();
        IDataAccess dao = DAOFactory.create();
        IERPConnector erpConnector = new ERPAdapter(new LegacyERPSystem());

        this.leadService = new LeadService(customerDAO);
        this.customerService = new CustomerService(customerDAO, erpConnector);
        this.interactionService = new InteractionManager(dao, erpConnector);

        CampaignManager campaignManager = new CampaignManager();
        this.marketingService = campaignManager;
        this.analyticsService = new AnalyticsManager(campaignManager);
    }

    public static void main(String[] args) throws IOException {
        CRMWebServer app = new CRMWebServer();
        app.start(8080);
    }

    private void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRequest);
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();

        System.out.println("CRM Web Server started at http://localhost:" + port);
        System.out.println("Open the URL above in your browser to demo all modules.");
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();

            if (path.startsWith("/api/")) {
                handleApi(exchange, path);
                return;
            }

            if ("/".equals(path)) {
                serveFile(exchange, "frontend/index.html", "text/html; charset=utf-8");
                return;
            }
            if ("/styles.css".equals(path)) {
                serveFile(exchange, "frontend/styles.css", "text/css; charset=utf-8");
                return;
            }
            if ("/app.js".equals(path)) {
                serveFile(exchange, "frontend/app.js", "application/javascript; charset=utf-8");
                return;
            }

            sendJson(exchange, 404, jsonError("Route not found"));
        } catch (Exception e) {
            sendJson(exchange, 500, jsonError("Server error: " + e.getMessage()));
        }
    }

    private void handleApi(HttpExchange exchange, String path) throws IOException {
        String method = exchange.getRequestMethod();

        if ("/api/health".equals(path) && "GET".equalsIgnoreCase(method)) {
            sendJson(exchange, 200, "{\"ok\":true,\"message\":\"CRM API is running\"}");
            return;
        }

        Map<String, String> params = readParams(exchange);

        try {
            switch (path) {
                case "/api/leads/create":
                    requireMethod(method, "POST");
                    int createdLeadId = leadService.createLead(value(params, "name"), value(params, "email"));
                    sendJson(exchange, 200, "{\"ok\":true,\"leadId\":" + createdLeadId + "}");
                    return;

                case "/api/leads/advance":
                    requireMethod(method, "POST");
                    int advanceLeadId = intParam(params, "id");
                    leadService.advanceLeadStatus(advanceLeadId);
                    sendJson(exchange, 200, "{\"ok\":true,\"status\":\"" + escapeJson(leadService.getLeadStatus(advanceLeadId)) + "\"}");
                    return;

                case "/api/leads/status":
                    requireMethod(method, "GET");
                    int leadId = intParam(params, "id");
                    sendJson(exchange, 200, "{\"ok\":true,\"status\":\"" + escapeJson(leadService.getLeadStatus(leadId)) + "\"}");
                    return;

                case "/api/customers/create":
                    requireMethod(method, "POST");
                    int customerId = customerService.createCustomer(new Customer(0, value(params, "name"), value(params, "email")));
                    sendJson(exchange, 200, "{\"ok\":true,\"customerId\":" + customerId + "}");
                    return;

                case "/api/customers/get":
                    requireMethod(method, "GET");
                    Customer customer = customerService.getCustomer(intParam(params, "id"));
                    sendJson(exchange, 200, "{\"ok\":true,\"customer\":" + customerToJson(customer) + "}");
                    return;

                case "/api/customers/list":
                    requireMethod(method, "GET");
                    List<Customer> customers = customerService.listCustomers();
                    List<String> customerJson = new ArrayList<>();
                    for (Customer c : customers) {
                        customerJson.add(customerToJson(c));
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"customers\":[" + String.join(",", customerJson) + "]}");
                    return;

                case "/api/customers/update":
                    requireMethod(method, "POST");
                    int updateId = intParam(params, "id");
                    int updated = customerService.updateCustomer(new Customer(updateId, value(params, "name"), value(params, "email")));
                    sendJson(exchange, 200, "{\"ok\":true,\"updated\":" + updated + "}");
                    return;

                case "/api/customers/delete":
                    requireMethod(method, "POST");
                    int deleted = customerService.deleteCustomer(intParam(params, "id"));
                    sendJson(exchange, 200, "{\"ok\":true,\"deleted\":" + deleted + "}");
                    return;

                case "/api/customers/sync":
                    requireMethod(method, "POST");
                    int syncId = intParam(params, "id");
                    customerService.performERPSync(syncId);
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"ERP sync successful\"}");
                    return;

                case "/api/interactions/log":
                    requireMethod(method, "POST");
                    Integer interactionLeadId = optionalIntParam(params, "leadId");
                    Integer interactionCustomerId = optionalIntParam(params, "customerId");
                    String type = value(params, "type");
                    String notes = params.getOrDefault("notes", "");
                    interactionService.logInteraction(new Interaction(interactionLeadId, interactionCustomerId, type, notes));
                    sendJson(exchange, 200, "{\"ok\":true,\"message\":\"Interaction logged\"}");
                    return;

                case "/api/interactions/list":
                    requireMethod(method, "GET");
                    List<Interaction> interactions = interactionService.getAll();
                    List<String> interactionJson = new ArrayList<>();
                    for (Interaction i : interactions) {
                        interactionJson.add(interactionToJson(i));
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"interactions\":[" + String.join(",", interactionJson) + "]}");
                    return;

                case "/api/campaigns/create":
                    requireMethod(method, "POST");
                    Campaign campaign = marketingService.createCampaign(
                            value(params, "name"),
                            value(params, "audience"),
                            doubleParam(params, "budget"),
                            LocalDate.parse(value(params, "startDate")),
                            LocalDate.parse(value(params, "endDate"))
                    );
                    sendJson(exchange, 200, "{\"ok\":true,\"campaign\":" + campaignToJson(campaign) + "}");
                    return;

                case "/api/campaigns/list":
                    requireMethod(method, "GET");
                    List<String> campaignJson = new ArrayList<>();
                    for (Campaign c : marketingService.getAllCampaigns()) {
                        campaignJson.add(campaignToJson(c));
                    }
                    sendJson(exchange, 200, "{\"ok\":true,\"campaigns\":[" + String.join(",", campaignJson) + "]}");
                    return;

                case "/api/campaigns/revenue":
                    requireMethod(method, "POST");
                    String campaignIdForRevenue = value(params, "id");
                    marketingService.recordRevenue(campaignIdForRevenue, doubleParam(params, "revenue"));
                    sendJson(exchange, 200, "{\"ok\":true,\"campaign\":" + campaignToJson(marketingService.getCampaignById(campaignIdForRevenue)) + "}");
                    return;

                case "/api/campaigns/status":
                    requireMethod(method, "POST");
                    String campaignIdForStatus = value(params, "id");
                    CampaignStatus status;
                    try {
                        status = CampaignStatus.valueOf(value(params, "status"));
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException("Invalid status. Use DRAFT, ACTIVE, COMPLETED, or CANCELLED");
                    }
                    marketingService.updateStatus(campaignIdForStatus, status);
                    sendJson(exchange, 200, "{\"ok\":true,\"campaign\":" + campaignToJson(marketingService.getCampaignById(campaignIdForStatus)) + "}");
                    return;

                case "/api/analytics/summary":
                    requireMethod(method, "GET");
                    AnalyticsReport report = analyticsService.generateReport();
                    sendJson(exchange, 200, "{\"ok\":true,\"report\":" + reportToJson(report) + "}");
                    return;

                default:
                    sendJson(exchange, 404, jsonError("API route not found"));
            }
        } catch (Exception e) {
            sendJson(exchange, 400, jsonError(e.getMessage()));
        }
    }

    private Map<String, String> readParams(HttpExchange exchange) throws IOException {
        Map<String, String> out = new LinkedHashMap<>();

        String query = exchange.getRequestURI().getRawQuery();
        if (query != null && !query.isBlank()) {
            mergeFormIntoMap(query, out);
        }

        if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            String body = readBody(exchange);
            if (!body.isBlank()) {
                mergeFormIntoMap(body, out);
            }
        }
        return out;
    }

    private void mergeFormIntoMap(String formEncoded, Map<String, String> out) {
        String[] pairs = formEncoded.split("&");
        for (String pair : pairs) {
            if (pair.isBlank()) {
                continue;
            }
            String[] parts = pair.split("=", 2);
            String key = urlDecode(parts[0]);
            String value = parts.length > 1 ? urlDecode(parts[1]) : "";
            out.put(key, value);
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        try (InputStream in = exchange.getRequestBody()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private void requireMethod(String actual, String expected) {
        if (!expected.equalsIgnoreCase(actual)) {
            throw new IllegalArgumentException("Expected " + expected + " request");
        }
    }

    private String value(Map<String, String> params, String key) {
        String v = params.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing parameter: " + key);
        }
        return v.trim();
    }

    private int intParam(Map<String, String> params, String key) {
        try {
            return Integer.parseInt(value(params, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be a number");
        }
    }

    private Integer optionalIntParam(Map<String, String> params, String key) {
        String raw = params.get(key);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be a number");
        }
    }

    private double doubleParam(Map<String, String> params, String key) {
        try {
            return Double.parseDouble(value(params, key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Parameter " + key + " must be numeric");
        }
    }

    private void serveFile(HttpExchange exchange, String relativePath, String contentType) throws IOException {
        Path fullPath = Path.of(relativePath);
        if (!Files.exists(fullPath)) {
            sendJson(exchange, 404, jsonError("Static file not found"));
            return;
        }
        byte[] bytes = Files.readAllBytes(fullPath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] payload = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, payload.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(payload);
        }
    }

    private String jsonError(String message) {
        return "{\"ok\":false,\"error\":\"" + escapeJson(message == null ? "unknown error" : message) + "\"}";
    }

    private String customerToJson(Customer c) {
        return "{"
                + "\"id\":" + c.getId() + ","
                + "\"name\":\"" + escapeJson(c.getName()) + "\","
                + "\"email\":\"" + escapeJson(c.getEmail()) + "\","
                + "\"ltv\":\"" + escapeJson(c.calculateLifetimeValue().toPlainString()) + "\""
                + "}";
    }

    private String interactionToJson(Interaction i) {
        String leadPart = i.getLeadId() == null ? "null" : i.getLeadId().toString();
        String customerPart = i.getCustomerId() == null ? "null" : i.getCustomerId().toString();

        return "{"
                + "\"id\":" + i.getId() + ","
                + "\"leadId\":" + leadPart + ","
                + "\"customerId\":" + customerPart + ","
                + "\"type\":\"" + escapeJson(i.getType()) + "\","
                + "\"notes\":\"" + escapeJson(i.getNotes()) + "\","
                + "\"timestamp\":\"" + escapeJson(i.getTimestamp().toString()) + "\""
                + "}";
    }

    private String campaignToJson(Campaign c) {
        return "{"
                + "\"id\":\"" + escapeJson(c.getId()) + "\","
                + "\"name\":\"" + escapeJson(c.getName()) + "\","
                + "\"audience\":\"" + escapeJson(c.getTargetAudience()) + "\","
                + "\"budget\":" + c.getBudget() + ","
                + "\"revenue\":" + c.getRevenue() + ","
                + "\"status\":\"" + escapeJson(c.getStatus().name()) + "\","
                + "\"roi\":\"" + escapeJson(c.getFormattedROI()) + "\","
                + "\"startDate\":\"" + escapeJson(c.getStartDate().toString()) + "\","
                + "\"endDate\":\"" + escapeJson(c.getEndDate().toString()) + "\""
                + "}";
    }

    private String reportToJson(AnalyticsReport r) {
        return "{"
                + "\"totalCampaigns\":" + r.getTotalCampaigns() + ","
                + "\"totalBudgetSpent\":" + r.getTotalBudgetSpent() + ","
                + "\"totalRevenue\":" + r.getTotalRevenue() + ","
                + "\"averageROI\":" + r.getAverageROI() + ","
                + "\"bestPerformerName\":\"" + escapeJson(r.getBestPerformerName()) + "\","
                + "\"bestPerformerROI\":" + r.getBestPerformerROI()
                + "}";
    }

    private String escapeJson(String raw) {
        if (raw == null) {
            return "";
        }
        return raw
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
