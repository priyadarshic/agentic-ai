# Kite Connect Java Trading Application

A complete Java implementation of the Kite Connect API for trading on Zerodha platform.

## 📦 Package Contents

1. **KiteConnectApp.java** - Basic implementation with manual request_token entry
2. **KiteConnectAppWithServer.java** - Advanced implementation with automatic callback handling
3. **SETUP_GUIDE.md** - Comprehensive setup and configuration guide

## 🚀 Quick Start

### Prerequisites

1. **Zerodha Account**
   - Active Zerodha trading account
   - 2FA TOTP enabled

2. **Developer Credentials**
   - Kite Connect API key and secret
   - Get them from: https://developers.kite.trade/

3. **Java Environment**
   - Java JDK 8+
   - org.json library

### Step 1: Set Up Redirect URL

**CRITICAL STEP**: Before running the application, you MUST configure your redirect URL in the Kite Connect developer console.

#### For Testing (Recommended):

1. Go to https://developers.kite.trade/
2. Login and select your app
3. Set Redirect URL to: `http://localhost:8080/callback`
4. Save changes

#### For Production:

Use an HTTPS URL: `https://yourdomain.com/callback`

### Step 2: Add Dependencies

#### Using Maven (pom.xml):

```xml
<dependencies>
    <dependency>
        <groupId>org.json</groupId>
        <artifactId>json</artifactId>
        <version>20231013</version>
    </dependency>
</dependencies>
```

#### Using Gradle (build.gradle):

```gradle
dependencies {
    implementation 'org.json:json:20231013'
}
```

#### Manual Download:

Download from: https://mvnrepository.com/artifact/org.json/json/20231013

### Step 3: Configure Credentials

#### Option A: Environment Variables (Recommended)

```bash
export KITE_API_KEY="your_api_key"
export KITE_API_SECRET="your_api_secret"
```

On Windows:
```cmd
set KITE_API_KEY=your_api_key
set KITE_API_SECRET=your_api_secret
```

#### Option B: Hardcode (Testing Only)

Edit the source file and replace:
```java
String API_KEY = "your_api_key_here";
String API_SECRET = "your_api_secret_here";
```

### Step 4: Compile and Run

#### Using Basic Version (Manual Token Entry):

```bash
# Compile
javac -cp ".:json-20231013.jar" KiteConnectApp.java

# Run
java -cp ".:json-20231013.jar" KiteConnectApp
```

On Windows:
```cmd
javac -cp ".;json-20231013.jar" KiteConnectApp.java
java -cp ".;json-20231013.jar" KiteConnectApp
```

#### Using Advanced Version (Auto Callback):

```bash
# Compile
javac -cp ".:json-20231013.jar" KiteConnectAppWithServer.java

# Run
java -cp ".:json-20231013.jar" KiteConnectAppWithServer
```

## 🎯 Usage Flow

### Basic Version (KiteConnectApp.java)

1. Run the application
2. Copy the displayed login URL
3. Open in browser and login
4. Copy request_token from redirect URL
5. Paste into terminal
6. Access your trading account

### Advanced Version (KiteConnectAppWithServer.java)

1. Run the application
2. Browser opens automatically (or open URL manually)
3. Login with Zerodha credentials
4. Callback is captured automatically
5. Access your trading account
6. Use interactive menu

## 📋 Features

### Authentication
- ✅ Complete OAuth2 login flow
- ✅ Automatic token exchange
- ✅ SHA-256 checksum generation
- ✅ Secure session management

### Account Information
- ✅ User profile retrieval
- ✅ Funds and margins
- ✅ Equity and commodity segments

### Trading Operations
- ✅ Place orders (Market, Limit, SL, SL-M)
- ✅ Modify orders
- ✅ Cancel orders
- ✅ Order history
- ✅ Order status

### Portfolio Management
- ✅ View positions
- ✅ View holdings
- ✅ Position conversion
- ✅ P&L tracking

### Advanced Features
- ✅ Built-in HTTP callback server
- ✅ Automatic browser launch
- ✅ Interactive menu system
- ✅ Pretty JSON printing
- ✅ Error handling

## 🔐 Security Best Practices

### DO:
✅ Use environment variables for credentials  
✅ Use HTTPS for production redirect URLs  
✅ Keep api_secret secure and never expose it  
✅ Implement proper error handling  
✅ Use backend server for mobile/desktop apps  
✅ Validate all inputs  
✅ Log out when done  

### DON'T:
❌ Never commit api_secret to version control  
❌ Never embed api_secret in mobile apps  
❌ Never expose access_token publicly  
❌ Never use HTTP for production  
❌ Don't hardcode credentials in source  

## 📊 Example Usage

### Get Margins

```java
KiteConnectApp kite = new KiteConnectApp(API_KEY, API_SECRET);
// ... perform login ...

JSONObject margins = kite.getMargins();
JSONObject equity = margins.getJSONObject("equity");
double available = equity.getJSONObject("available").getDouble("cash");
System.out.println("Available: ₹" + available);
```

### Place Order

```java
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

JSONObject response = kite.placeOrder(orderParams);
String orderId = response.getString("order_id");
```

### View Positions

```java
JSONObject positions = kite.getPositions();
JSONArray netPositions = positions.getJSONArray("net");

for (int i = 0; i < netPositions.length(); i++) {
    JSONObject pos = netPositions.getJSONObject(i);
    System.out.println(pos.getString("tradingsymbol") + 
                       " | Qty: " + pos.getInt("quantity") +
                       " | P&L: ₹" + pos.getDouble("pnl"));
}
```

## 🔧 Troubleshooting

### Issue: "Redirect URI mismatch"
**Solution**: Ensure redirect URL in code matches developer console exactly

### Issue: "Invalid API credentials"
**Solution**: Verify api_key and api_secret are correct

### Issue: "Request token expired"
**Solution**: Complete token exchange immediately after login

### Issue: "Connection refused"
**Solution**: Ensure callback server is running on correct port

### Issue: "Invalid checksum"
**Solution**: Verify checksum is SHA-256 of (api_key + request_token + api_secret)

## 📚 API Documentation

For complete API reference, visit:
- **Official Docs**: https://kite.trade/docs/connect/v3/
- **Developer Forum**: https://kite.trade/forum/
- **Support**: https://support.zerodha.com/

## ⚠️ Important Notes

1. **Access Token Validity**: Expires at 6 AM IST next day (regulatory requirement)
2. **Rate Limits**: Be mindful of API rate limits
3. **Testing**: Always test with small quantities first
4. **Market Hours**: Orders can only be placed during market hours
5. **Regulatory**: Follow all SEBI regulations and guidelines

## 📄 License

This is a sample implementation for educational purposes. Use at your own risk.

## 🤝 Contributing

Feel free to submit issues and enhancement requests!

## 📞 Support

For Kite Connect API support:
- Forum: https://kite.trade/forum/
- Email: [email protected]

---

**Disclaimer**: This application is for educational purposes. Trading in securities involves risk. Please ensure you understand the risks before trading with real money.

Happy Trading! 📈
