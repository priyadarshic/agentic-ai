# Kite Connect Java Trading App - Setup Guide

## Prerequisites

1. **Zerodha Trading Account**
   - An active Zerodha trading account
   - 2FA TOTP enabled (mandatory for Kite Connect)
   - Setup guide: https://support.zerodha.com/category/trading-and-markets/general-kite/login-credentials-of-trading-platforms/articles/time-based-otp-setup

2. **Java Development Environment**
   - Java JDK 8 or higher
   - JSON library (org.json) - Add this dependency to your project

3. **Developer Account**
   - Kite Connect developer account

---

## Setting Up Redirect URL - CRITICAL STEP

### Step 1: Create a Developer Account

1. Visit the Kite Connect Developer Portal: https://developers.kite.trade/login
2. Sign in with your Zerodha credentials
3. Complete the registration process

### Step 2: Create a New App

1. After logging in, click on "Create new app"
2. Fill in the application details:
   - **App name**: Your application name (e.g., "My Trading App")
   - **Type**: Select "Connect" for trading applications
   - **Description**: Brief description of your app

### Step 3: Configure Redirect URL

The **Redirect URL** is where users will be sent after they complete the login flow. This is a CRITICAL setting.

#### Option A: For Development/Testing (Local Machine)

```
http://localhost:8080/callback
```

Or use a specific port:
```
http://127.0.0.1:3000/callback
```

**Important Notes:**
- Kite Connect allows localhost URLs for development
- You'll need to run a local server to capture the request_token
- The path can be anything you choose (e.g., /callback, /redirect, /auth)

#### Option B: For Production (Remote Server)

```
https://yourdomain.com/kite/callback
```

**Requirements:**
- MUST use HTTPS (SSL certificate required)
- Domain must be publicly accessible
- Can use any valid path

#### Option C: For Mobile/Desktop Apps

For mobile and desktop applications:
1. You MUST have a remote backend server with a redirect URL
2. The mobile app redirects users to Kite login
3. After login, Kite redirects to your backend server
4. Your backend captures the request_token and exchanges it for access_token
5. Your backend sends the access_token to your mobile app

**Example flow:**
```
Mobile App → Kite Login → Backend Server (https://api.yourapp.com/callback) 
→ Backend gets request_token → Backend exchanges for access_token 
→ Backend sends access_token to Mobile App
```

**SECURITY WARNING:** Never embed your `api_secret` in mobile or desktop applications!

### Step 4: Save and Get API Credentials

1. After setting the redirect URL, click "Create"
2. You'll receive:
   - **API Key** (api_key): Public identifier for your app
   - **API Secret** (api_secret): Secret key - KEEP THIS SECURE!

3. **Important Security Notes:**
   - Never commit api_secret to version control
   - Never expose api_secret in client-side code
   - Never share api_secret publicly
   - Store it in environment variables or secure configuration

---

## Authentication Flow Explanation

### Visual Flow:

```
1. User clicks "Login with Zerodha"
   ↓
2. App redirects to: https://kite.zerodha.com/connect/login?v=3&api_key=YOUR_API_KEY
   ↓
3. User logs in with Zerodha credentials + TOTP
   ↓
4. Kite redirects to: YOUR_REDIRECT_URL?request_token=XXXXXX&action=login&status=success
   ↓
5. Your app captures request_token from URL
   ↓
6. Your app generates checksum: SHA256(api_key + request_token + api_secret)
   ↓
7. Your app POSTs to https://api.kite.trade/session/token with:
   - api_key
   - request_token
   - checksum
   ↓
8. Kite API returns access_token
   ↓
9. Use access_token for all subsequent API calls
```

### Example Redirect URL Response:

After successful login, Kite will redirect to:
```
http://localhost:8080/callback?request_token=AbCdEf123456&action=login&status=success
```

Your app needs to extract the `request_token` parameter.

---

## Setting Up a Local Callback Server (For Development)

If you're using localhost as your redirect URL, you need to run a simple server to capture the request_token.

### Simple Java HTTP Server Example:

```java
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class CallbackServer {
    
    private static String capturedRequestToken = null;
    
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        
        server.createContext("/callback", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                String query = exchange.getRequestURI().getQuery();
                
                // Parse request_token from query string
                if (query != null && query.contains("request_token=")) {
                    String[] params = query.split("&");
                    for (String param : params) {
                        if (param.startsWith("request_token=")) {
                            capturedRequestToken = param.split("=")[1];
                            break;
                        }
                    }
                }
                
                // Send response to browser
                String response = "Authentication successful! Request token captured. You can close this window.";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                
                System.out.println("Request token captured: " + capturedRequestToken);
            }
        });
        
        server.setExecutor(null);
        server.start();
        System.out.println("Server started on port 8080");
        System.out.println("Waiting for callback...");
    }
    
    public static String getRequestToken() {
        return capturedRequestToken;
    }
}
```

---

## Project Dependencies

### Using Maven (pom.xml):

```xml
<dependencies>
    <!-- JSON library -->
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
</dependencies>
```

### Using Gradle (build.gradle):

```gradle
dependencies {
    implementation 'org.json:json:20231013'
}
```

### Manual JAR Download:

Download org.json JAR from:
https://mvnrepository.com/artifact/org.json/json

---

## Compilation and Running

### Compile the application:

```bash
# If using Maven
mvn compile

# If compiling manually with JSON jar
javac -cp ".:json-20231013.jar" KiteConnectApp.java

# On Windows
javac -cp ".;json-20231013.jar" KiteConnectApp.java
```

### Run the application:

```bash
# If using Maven
mvn exec:java -Dexec.mainClass="KiteConnectApp"

# If running manually
java -cp ".:json-20231013.jar" KiteConnectApp

# On Windows
java -cp ".;json-20231013.jar" KiteConnectApp
```

---

## Complete Usage Example

### 1. Update your API credentials in the code:

```java
String API_KEY = "your_actual_api_key";
String API_SECRET = "your_actual_api_secret";
```

### 2. Run the application:

```bash
java KiteConnectApp
```

### 3. Follow the prompts:

```
Please login at: https://kite.zerodha.com/connect/login?v=3&api_key=xxxxx

After logging in, you'll be redirected to your redirect URL.
Copy the 'request_token' parameter from the URL and paste it here:
```

### 4. Login Flow:

1. Open the displayed URL in your browser
2. Login with your Zerodha credentials
3. Enter your TOTP code
4. You'll be redirected to your redirect URL
5. Copy the `request_token` from the URL
6. Paste it into the terminal

### 5. The app will then:

- Exchange request_token for access_token
- Display your user profile
- Show your margin details
- List positions and holdings
- Log out

---

## Important Security Considerations

### DO:
✅ Store api_secret in environment variables
✅ Use HTTPS for production redirect URLs
✅ Keep access_token secure and never expose it
✅ Implement proper error handling
✅ Use a backend server for mobile/desktop apps
✅ Validate and sanitize all inputs
✅ Log out users when done
✅ Handle token expiration (expires at 6 AM next day)

### DON'T:
❌ Never commit api_secret to version control
❌ Never embed api_secret in mobile apps
❌ Never expose api_secret in client-side code
❌ Never share your access_token publicly
❌ Never use HTTP for production redirect URLs
❌ Don't hardcode credentials in your source code

---

## Access Token Lifecycle

- **Valid until**: 6 AM (IST) next day (regulatory requirement)
- **How to extend**: Re-authenticate daily
- **On logout**: Token is immediately invalidated
- **On master logout from Kite web**: All tokens invalidated

---

## Testing Your Setup

### Test Checklist:

1. ✅ Verify redirect URL is correctly set in developer console
2. ✅ Verify local server (if using localhost) is running on correct port
3. ✅ Verify you can access the login URL
4. ✅ Verify redirect happens after login
5. ✅ Verify request_token is captured
6. ✅ Verify checksum generation is working
7. ✅ Verify token exchange succeeds
8. ✅ Verify API calls work with access_token

---

## Common Issues and Solutions

### Issue 1: "Invalid API credentials"
- **Solution**: Double-check your api_key and api_secret
- Ensure you copied them correctly from developer console

### Issue 2: "Redirect URI mismatch"
- **Solution**: The redirect URL in your request MUST exactly match what's configured in the developer console
- Check for trailing slashes, http vs https, ports, etc.

### Issue 3: "Invalid checksum"
- **Solution**: Ensure checksum is SHA-256 of (api_key + request_token + api_secret) in that exact order
- No spaces or special characters between concatenation

### Issue 4: "Request token expired"
- **Solution**: Request tokens expire quickly (few minutes)
- Complete the token exchange immediately after getting request_token

### Issue 5: "Connection refused" (localhost)
- **Solution**: Ensure your callback server is running before initiating login
- Check the port number matches your redirect URL

---

## Additional Resources

- **Official Documentation**: https://kite.trade/docs/connect/v3/
- **API Reference**: https://kite.trade/docs/connect/v3/
- **Developer Forum**: https://kite.trade/forum/
- **Support**: https://support.zerodha.com/

---

## Example Production Setup

For a production application:

```java
// Use environment variables for credentials
String API_KEY = System.getenv("KITE_API_KEY");
String API_SECRET = System.getenv("KITE_API_SECRET");
String REDIRECT_URL = System.getenv("KITE_REDIRECT_URL");

// Set environment variables before running:
// export KITE_API_KEY=your_key
// export KITE_API_SECRET=your_secret
// export KITE_REDIRECT_URL=https://yourdomain.com/callback
```

---

## Next Steps

1. Set up your developer account and app
2. Configure your redirect URL
3. Test the authentication flow
4. Explore other API endpoints (orders, positions, holdings, etc.)
5. Implement WebSocket streaming for live market data
6. Add error handling and logging
7. Implement proper session management
8. Deploy to production with HTTPS

Good luck with your trading application! 🚀
