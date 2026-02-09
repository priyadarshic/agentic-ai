package kite;

import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import org.json.*;
import com.sun.net.httpserver.*;

/**
 * Kite Connect Trading Application with Built-in Callback Server
 * This version includes an embedded HTTP server to automatically capture the request_token
 */
public class KiteConnectAppWithServer {
    
    private static final String API_BASE_URL = "https://api.kite.trade";
    private static final String LOGIN_URL = "https://kite.zerodha.com/connect/login";
    private static final String API_VERSION = "3";
    private static final int CALLBACK_PORT = 8080;
    
    private String apiKey;
    private String apiSecret;
    protected String accessToken;
    private String userId;
    private HttpServer callbackServer;
    private String capturedRequestToken;
    private CountDownLatch tokenLatch;
    
    /**
     * Constructor
     */
    public KiteConnectAppWithServer(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.tokenLatch = new CountDownLatch(1);
    }
    
    /**
     * Start the callback server to capture request_token
     */
    public void startCallbackServer() throws IOException {
        callbackServer = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);
        
        callbackServer.createContext("/callback", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String query = exchange.getRequestURI().getQuery();
                
                System.out.println("\n✅ Callback received!");
                
                // Parse request_token from query string
                if (query != null && query.contains("request_token=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("request_token=")) {
                            capturedRequestToken = param.split("=")[1];
                            System.out.println("📝 Request token captured: " + capturedRequestToken);
                            break;
                        }
                    }
                }
                
                // Send HTML response to browser
                String response = "<!DOCTYPE html><html><head><title>Authentication Success</title>" +
                    "<style>body{font-family:Arial,sans-serif;text-align:center;padding:50px;background:#f0f0f0;}" +
                    "h1{color:#4CAF50;}p{font-size:18px;}</style></head><body>" +
                    "<h1>✅ Authentication Successful!</h1>" +
                    "<p>Request token has been captured.</p>" +
                    "<p>You can close this window and return to the application.</p>" +
                    "</body></html>";
                
                exchange.getResponseHeaders().set("Content-Type", "text/html");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                
                // Signal that token has been captured
                tokenLatch.countDown();
            }
        });
        
        callbackServer.setExecutor(null);
        callbackServer.start();
        System.out.println("🌐 Callback server started on http://localhost:" + CALLBACK_PORT);
    }
    
    /**
     * Stop the callback server
     */
    public void stopCallbackServer() {
        if (callbackServer != null) {
            callbackServer.stop(0);
            System.out.println("🛑 Callback server stopped");
        }
    }
    
    /**
     * Get login URL with redirect to localhost callback
     */
    public String getLoginURL() {
        return LOGIN_URL + "?v=" + API_VERSION + "&api_key=" + apiKey;
    }
    
    /**
     * Wait for request token to be captured
     */
    public String waitForRequestToken(int timeoutSeconds) throws InterruptedException {
        System.out.println("\n⏳ Waiting for authentication callback (timeout: " + timeoutSeconds + "s)...");
        
        boolean received = tokenLatch.await(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        
        if (!received) {
            throw new RuntimeException("Timeout waiting for request token");
        }
        
        return capturedRequestToken;
    }
    
    /**
     * Generate checksum for token exchange
     */
    private String generateChecksum(String requestToken) {
        try {
            String data = apiKey + requestToken + apiSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error generating checksum", e);
        }
    }
    
    /**
     * Exchange request token for access token
     */
    public JSONObject generateSession(String requestToken) throws Exception {
        String checksum = generateChecksum(requestToken);
        
        System.out.println("\n🔄 Exchanging request token for access token...");
        
        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);
        params.put("request_token", requestToken);
        params.put("checksum", checksum);
        
        JSONObject response = makePostRequest("/session/token", params, false);
        
        if (response.getString("status").equals("success")) {
            JSONObject data = response.getJSONObject("data");
            this.accessToken = data.getString("access_token");
            this.userId = data.getString("user_id");
            System.out.println("✅ Authentication successful!");
            System.out.println("👤 User ID: " + userId);
            return data;
        } else {
            throw new Exception("Authentication failed");
        }
    }
    
    /**
     * Automated login flow
     */
    public JSONObject performLogin() throws Exception {
        // Start callback server
        startCallbackServer();
        
        // Generate and display login URL
        String loginURL = getLoginURL();
        System.out.println("\n" + "=".repeat(70));
        System.out.println("🔐 KITE CONNECT AUTHENTICATION");
        System.out.println("=".repeat(70));
        System.out.println("\n📌 Please open this URL in your browser to login:");
        System.out.println("\n   " + loginURL);
        System.out.println("\n💡 Make sure your redirect URL is set to:");
        System.out.println("   http://localhost:" + CALLBACK_PORT + "/callback");
        System.out.println();
        
        // Try to open browser automatically
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(new URI(loginURL));
                System.out.println("🌐 Browser opened automatically");
            }
        } catch (Exception e) {
            System.out.println("⚠️  Could not open browser automatically. Please open the URL manually.");
        }
        
        // Wait for callback
        String requestToken = waitForRequestToken(300); // 5 minute timeout
        
        // Exchange for access token
        JSONObject session = generateSession(requestToken);
        
        // Stop callback server
        stopCallbackServer();
        
        return session;
    }
    
    /**
     * Get user profile
     */
    public JSONObject getUserProfile() throws Exception {
        JSONObject response = makeGetRequest("/user/profile");
        return response.getJSONObject("data");
    }
    
    /**
     * Get margins
     */
    public JSONObject getMargins() throws Exception {
        JSONObject response = makeGetRequest("/user/margins");
        return response.getJSONObject("data");
    }
    
    /**
     * Get margins for specific segment
     */
    public JSONObject getMargins(String segment) throws Exception {
        JSONObject response = makeGetRequest("/user/margins/" + segment);
        return response.getJSONObject("data");
    }
    
    /**
     * Place an order
     */
    public JSONObject placeOrder(Map<String, String> params) throws Exception {
        JSONObject response = makePostRequest("/orders/" + params.get("variety"), params, true);
        return response.getJSONObject("data");
    }
    
    /**
     * Get all orders
     */
    public JSONArray getOrders() throws Exception {
        JSONObject response = makeGetRequest("/orders");
        return response.getJSONArray("data");
    }
    
    /**
     * Get order history
     */
    public JSONArray getOrderHistory(String orderId) throws Exception {
        JSONObject response = makeGetRequest("/orders/" + orderId);
        return response.getJSONArray("data");
    }
    
    /**
     * Modify an order
     */
    public JSONObject modifyOrder(String orderId, String variety, Map<String, String> params) throws Exception {
        JSONObject response = makePutRequest("/orders/" + variety + "/" + orderId, params);
        return response.getJSONObject("data");
    }
    
    /**
     * Cancel an order
     */
    public JSONObject cancelOrder(String orderId, String variety) throws Exception {
        JSONObject response = makeDeleteRequest("/orders/" + variety + "/" + orderId);
        return response.getJSONObject("data");
    }
    
    /**
     * Get positions
     */
    public JSONObject getPositions() throws Exception {
        JSONObject response = makeGetRequest("/portfolio/positions");
        return response.getJSONObject("data");
    }
    
    /**
     * Get holdings
     */
    public JSONArray getHoldings() throws Exception {
        JSONObject response = makeGetRequest("/portfolio/holdings");
        return response.getJSONArray("data");
    }
    
    /**
     * Convert position
     */
    public boolean convertPosition(Map<String, String> params) throws Exception {
        JSONObject response = makePutRequest("/portfolio/positions", params);
        return response.getString("status").equals("success");
    }
    
    /**
     * Logout
     */
    public boolean logout() throws Exception {
        makeDeleteRequest("/session/token?api_key=" + apiKey + "&access_token=" + accessToken);
        this.accessToken = null;
        this.userId = null;
        System.out.println("\n👋 Logged out successfully");
        return true;
    }
    
    // HTTP Request Methods
    
    private JSONObject makeGetRequest(String endpoint) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        return getResponse(conn);
    }
    
    private JSONObject makePostRequest(String endpoint, Map<String, String> params, boolean requiresAuth) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        
        if (requiresAuth) {
            conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        }
        
        conn.setDoOutput(true);
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        
        byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postDataBytes);
        }
        
        return getResponse(conn);
    }
    
    private JSONObject makePutRequest(String endpoint, Map<String, String> params) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        conn.setDoOutput(true);
        
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : params.entrySet()) {
            if (postData.length() != 0) postData.append('&');
            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
            postData.append('=');
            postData.append(URLEncoder.encode(param.getValue(), "UTF-8"));
        }
        
        byte[] postDataBytes = postData.toString().getBytes(StandardCharsets.UTF_8);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        
        try (OutputStream os = conn.getOutputStream()) {
            os.write(postDataBytes);
        }
        
        return getResponse(conn);
    }
    
    private JSONObject makeDeleteRequest(String endpoint) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        return getResponse(conn);
    }
    
    private JSONObject getResponse(HttpURLConnection conn) throws Exception {
        int responseCode = conn.getResponseCode();
        
        InputStream is;
        if (responseCode >= 200 && responseCode < 300) {
            is = conn.getInputStream();
        } else {
            is = conn.getErrorStream();
        }
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        
        JSONObject jsonResponse = new JSONObject(response.toString());
        
        if (responseCode >= 400) {
            throw new Exception("API Error: " + jsonResponse.toString());
        }
        
        return jsonResponse;
    }
    
    /**
     * Pretty print JSON
     */
    private void printJSON(String title, Object data) {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("📊 " + title);
        System.out.println("=".repeat(70));
        if (data instanceof JSONObject) {
            System.out.println(((JSONObject) data).toString(2));
        } else if (data instanceof JSONArray) {
            System.out.println(((JSONArray) data).toString(2));
        }
    }
    
    /**
     * Main method with comprehensive example
     */
    public static void main(String[] args) {
        try {
            // Load credentials from environment variables (recommended)
            String API_KEY = System.getenv("KITE_API_KEY");
            String API_SECRET = System.getenv("KITE_API_SECRET");
            
            // Fallback to hardcoded values if env vars not set (for testing only!)
            if (API_KEY == null || API_SECRET == null) {
                System.out.println("⚠️  Environment variables not set. Using hardcoded values.");
                System.out.println("💡 For production, set KITE_API_KEY and KITE_API_SECRET environment variables.");
                API_KEY = "your_api_key_here";
                API_SECRET = "your_api_secret_here";
            }
            
            // Initialize app
            KiteConnectAppWithServer kite = new KiteConnectAppWithServer(API_KEY, API_SECRET);
            
            // Perform automated login
            JSONObject session = kite.performLogin();
            
            // Display user profile
            kite.printJSON("User Profile", session);
            
            // Get and display margins
            JSONObject margins = kite.getMargins();
            kite.printJSON("Account Margins", margins);
            
            // Display equity balance
            JSONObject equity = margins.getJSONObject("equity");
            System.out.println("\n💰 EQUITY ACCOUNT");
            System.out.println("   Available Cash: ₹" + String.format("%.2f", equity.getJSONObject("available").getDouble("cash")));
            System.out.println("   Net Available: ₹" + String.format("%.2f", equity.getDouble("net")));
            System.out.println("   Used Margin: ₹" + String.format("%.2f", equity.getJSONObject("utilised").getDouble("debits")));
            
            // Get positions
            JSONObject positions = kite.getPositions();
            JSONArray netPositions = positions.getJSONArray("net");
            System.out.println("\n📈 POSITIONS");
            System.out.println("   Open Positions: " + netPositions.length());
            
            if (netPositions.length() > 0) {
                for (int i = 0; i < netPositions.length(); i++) {
                    JSONObject pos = netPositions.getJSONObject(i);
                    System.out.println("   - " + pos.getString("tradingsymbol") + 
                                     " | Qty: " + pos.getInt("quantity") + 
                                     " | P&L: ₹" + pos.getDouble("pnl"));
                }
            }
            
            // Get holdings
            JSONArray holdings = kite.getHoldings();
            System.out.println("\n💼 HOLDINGS");
            System.out.println("   Total Holdings: " + holdings.length());
            
            if (holdings.length() > 0) {
                for (int i = 0; i < Math.min(5, holdings.length()); i++) {
                    JSONObject holding = holdings.getJSONObject(i);
                    System.out.println("   - " + holding.getString("tradingsymbol") + 
                                     " | Qty: " + holding.getInt("quantity") + 
                                     " | Avg: ₹" + holding.getDouble("average_price"));
                }
                if (holdings.length() > 5) {
                    System.out.println("   ... and " + (holdings.length() - 5) + " more");
                }
            }
            
            // Get recent orders
            JSONArray orders = kite.getOrders();
            System.out.println("\n📋 RECENT ORDERS");
            System.out.println("   Total Orders: " + orders.length());
            
            // Example: Place order (commented out for safety)
            /*
            System.out.println("\n⚠️  PLACE ORDER EXAMPLE (Uncomment to test)");
            Map<String, String> orderParams = new HashMap<>();
            orderParams.put("variety", "regular");
            orderParams.put("exchange", "NSE");
            orderParams.put("tradingsymbol", "INFY");
            orderParams.put("transaction_type", "BUY");
            orderParams.put("order_type", "LIMIT");
            orderParams.put("price", "1500.00");
            orderParams.put("quantity", "1");
            orderParams.put("product", "CNC");
            orderParams.put("validity", "DAY");
            
            JSONObject orderResponse = kite.placeOrder(orderParams);
            System.out.println("✅ Order placed successfully!");
            System.out.println("   Order ID: " + orderResponse.getString("order_id"));
            */
            
            // Interactive menu
            Scanner scanner = new Scanner(System.in);
            boolean continueLoop = true;
            
            while (continueLoop) {
                System.out.println("\n" + "=".repeat(70));
                System.out.println("📱 MENU OPTIONS");
                System.out.println("=".repeat(70));
                System.out.println("1. Refresh Margins");
                System.out.println("2. View Positions");
                System.out.println("3. View Holdings");
                System.out.println("4. View Orders");
                System.out.println("5. Logout and Exit");
                System.out.print("\nSelect option (1-5): ");
                
                String choice = scanner.nextLine().trim();
                
                switch (choice) {
                    case "1":
                        margins = kite.getMargins();
                        kite.printJSON("Refreshed Margins", margins);
                        break;
                    case "2":
                        positions = kite.getPositions();
                        kite.printJSON("Current Positions", positions);
                        break;
                    case "3":
                        holdings = kite.getHoldings();
                        kite.printJSON("Holdings", holdings);
                        break;
                    case "4":
                        orders = kite.getOrders();
                        kite.printJSON("All Orders", orders);
                        break;
                    case "5":
                        kite.logout();
                        continueLoop = false;
                        break;
                    default:
                        System.out.println("❌ Invalid option");
                }
            }
            
            scanner.close();
            System.out.println("\n✅ Application terminated successfully");
            
        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
