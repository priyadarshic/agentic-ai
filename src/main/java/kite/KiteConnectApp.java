package kite;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/**
 * Kite Connect Trading Application
 * This application demonstrates the complete authentication flow and basic trading operations
 */
public class KiteConnectApp {
    
    private static final String API_BASE_URL = "https://api.kite.trade";
    private static final String LOGIN_URL = "https://kite.zerodha.com/connect/login";
    private static final String API_VERSION = "3";
    
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private String userId;
    
    /**
     * Constructor
     * @param apiKey Your API key from Kite Connect developer console
     * @param apiSecret Your API secret (keep this secure!)
     */
    public KiteConnectApp(String apiKey, String apiSecret) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    /**
     * Step 1: Generate the login URL
     * User should be redirected to this URL to complete the login flow
     * @return Login URL
     */
    public String getLoginURL() {
        return LOGIN_URL + "?v=" + API_VERSION + "&api_key=" + apiKey;
    }
    
    /**
     * Step 2: Generate checksum for token exchange
     * SHA-256 hash of (api_key + request_token + api_secret)
     * @param requestToken The request token received after login
     * @return Checksum string
     */
    private String generateChecksum(String requestToken) {
        try {
            String data = apiKey + requestToken + apiSecret;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            
            // Convert byte array to hex string
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
     * Step 3: Exchange request token for access token
     * @param requestToken The request token received from redirect URL after login
     * @return JSONObject containing user profile and access token
     */
    public JSONObject generateSession(String requestToken) throws Exception {
        String checksum = generateChecksum(requestToken);
        
        Map<String, String> params = new HashMap<>();
        params.put("api_key", apiKey);
        params.put("request_token", requestToken);
        params.put("checksum", checksum);
        
        JSONObject response = makePostRequest("/session/token", params, false);
        
        if (response.getString("status").equals("success")) {
            JSONObject data = response.getJSONObject("data");
            this.accessToken = data.getString("access_token");
            this.userId = data.getString("user_id");
            System.out.println("Authentication successful for user: " + userId);
            return data;
        } else {
            throw new Exception("Authentication failed");
        }
    }
    
    /**
     * Get user profile
     * @return JSONObject containing user profile information
     */
    public JSONObject getUserProfile() throws Exception {
        JSONObject response = makeGetRequest("/user/profile");
        return response.getJSONObject("data");
    }
    
    /**
     * Get funds and margins for all segments
     * @return JSONObject containing margin information
     */
    public JSONObject getMargins() throws Exception {
        JSONObject response = makeGetRequest("/user/margins");
        return response.getJSONObject("data");
    }
    
    /**
     * Get margins for specific segment
     * @param segment "equity" or "commodity"
     * @return JSONObject containing margin information for the segment
     */
    public JSONObject getMargins(String segment) throws Exception {
        JSONObject response = makeGetRequest("/user/margins/" + segment);
        return response.getJSONObject("data");
    }
    
    /**
     * Place an order
     * @param params Order parameters
     * @return JSONObject containing order ID
     */
    public JSONObject placeOrder(Map<String, String> params) throws Exception {
        JSONObject response = makePostRequest("/orders/" + params.get("variety"), params, true);
        return response.getJSONObject("data");
    }
    
    /**
     * Get all orders
     * @return JSONArray containing all orders
     */
    public JSONArray getOrders() throws Exception {
        JSONObject response = makeGetRequest("/orders");
        return response.getJSONArray("data");
    }
    
    /**
     * Get order history for a specific order
     * @param orderId The order ID
     * @return JSONArray containing order history
     */
    public JSONArray getOrderHistory(String orderId) throws Exception {
        JSONObject response = makeGetRequest("/orders/" + orderId);
        return response.getJSONArray("data");
    }
    
    /**
     * Cancel an order
     * @param orderId The order ID to cancel
     * @param variety Order variety (regular, amo, co, iceberg)
     * @return JSONObject containing cancellation response
     */
    public JSONObject cancelOrder(String orderId, String variety) throws Exception {
        JSONObject response = makeDeleteRequest("/orders/" + variety + "/" + orderId);
        return response.getJSONObject("data");
    }
    
    /**
     * Get positions
     * @return JSONObject containing positions
     */
    public JSONObject getPositions() throws Exception {
        JSONObject response = makeGetRequest("/portfolio/positions");
        return response.getJSONObject("data");
    }
    
    /**
     * Get holdings
     * @return JSONArray containing holdings
     */
    public JSONArray getHoldings() throws Exception {
        JSONObject response = makeGetRequest("/portfolio/holdings");
        return response.getJSONArray("data");
    }
    
    /**
     * Logout and invalidate access token
     * @return boolean indicating success
     */
    public boolean logout() throws Exception {
        makeDeleteRequest("/session/token?api_key=" + apiKey + "&access_token=" + accessToken);
        this.accessToken = null;
        this.userId = null;
        System.out.println("Logged out successfully");
        return true;
    }
    
    /**
     * Make GET request to Kite API
     */
    private JSONObject makeGetRequest(String endpoint) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        
        return getResponse(conn);
    }
    
    /**
     * Make POST request to Kite API
     */
    private JSONObject makePostRequest(String endpoint, Map<String, String> params, boolean requiresAuth) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        
        if (requiresAuth) {
            conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        }
        
        conn.setDoOutput(true);
        
        // Build form data
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
    
    /**
     * Make DELETE request to Kite API
     */
    private JSONObject makeDeleteRequest(String endpoint) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        
        return getResponse(conn);
    }
    
    /**
     * Read and parse response from HttpURLConnection
     */
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
     * Main method with example usage
     */
    public static void main(String[] args) {
        try {
            // Initialize with your API credentials
            String API_KEY = "your_api_key_here";
            String API_SECRET = "your_api_secret_here";
            
            KiteConnectApp kite = new KiteConnectApp(API_KEY, API_SECRET);
            
            // Step 1: Get login URL and display to user
            String loginURL = kite.getLoginURL();
            System.out.println("Please login at: " + loginURL);
            System.out.println("\nAfter logging in, you'll be redirected to your redirect URL.");
            System.out.println("Copy the 'request_token' parameter from the URL and paste it here:");
            
            // Step 2: Read request token from user input
            Scanner scanner = new Scanner(System.in);
            String requestToken = scanner.nextLine().trim();
            
            // Step 3: Generate session and get access token
            JSONObject userProfile = kite.generateSession(requestToken);
            System.out.println("\n=== User Profile ===");
            System.out.println("User ID: " + userProfile.getString("user_id"));
            System.out.println("Name: " + userProfile.getString("user_name"));
            System.out.println("Email: " + userProfile.getString("email"));
            System.out.println("Broker: " + userProfile.getString("broker"));
            
            // Get margins
            System.out.println("\n=== Margins ===");
            JSONObject margins = kite.getMargins();
            JSONObject equityMargin = margins.getJSONObject("equity");
            System.out.println("Equity Available: ₹" + equityMargin.getJSONObject("available").getDouble("cash"));
            System.out.println("Equity Net: ₹" + equityMargin.getDouble("net"));
            
            // Example: Place a market order (commented out for safety)
            /*
            Map<String, String> orderParams = new HashMap<>();
            orderParams.put("variety", "regular");
            orderParams.put("exchange", "NSE");
            orderParams.put("tradingsymbol", "INFY");
            orderParams.put("transaction_type", "BUY");
            orderParams.put("order_type", "MARKET");
            orderParams.put("quantity", "1");
            orderParams.put("product", "CNC");
            orderParams.put("validity", "DAY");
            
            JSONObject orderResponse = kite.placeOrder(orderParams);
            System.out.println("Order placed: " + orderResponse.getString("order_id"));
            */
            
            // Get current positions
            System.out.println("\n=== Positions ===");
            JSONObject positions = kite.getPositions();
            JSONArray netPositions = positions.getJSONArray("net");
            System.out.println("Total positions: " + netPositions.length());
            
            // Get holdings
            System.out.println("\n=== Holdings ===");
            JSONArray holdings = kite.getHoldings();
            System.out.println("Total holdings: " + holdings.length());
            
            // Logout
            kite.logout();
            
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
