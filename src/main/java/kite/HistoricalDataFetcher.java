package kite;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

/**
 * Historical Data Fetcher for Kite Connect API
 * Fetches and manages historical candle data
 */
public class HistoricalDataFetcher {
    
    private static final String API_BASE_URL = "https://api.kite.trade";
    private static final String API_VERSION = "3";
    
    // Rate limit: 3 requests per second for historical data
    private static final int RATE_LIMIT_DELAY_MS = 350;
    
    private String apiKey;
    private String accessToken;
    
    /**
     * Historical data intervals
     */
    public enum Interval {
        MINUTE("minute"),
        THREE_MINUTE("3minute"),
        FIVE_MINUTE("5minute"),
        TEN_MINUTE("10minute"),
        FIFTEEN_MINUTE("15minute"),
        THIRTY_MINUTE("30minute"),
        SIXTY_MINUTE("60minute"),
        DAY("day");
        
        private String value;
        
        Interval(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        // Maximum days that can be fetched in single request
        public int getMaxDays() {
            switch (this) {
                case MINUTE: return 60;
                case THREE_MINUTE: return 100;
                case FIVE_MINUTE: return 100;
                case TEN_MINUTE: return 100;
                case FIFTEEN_MINUTE: return 200;
                case THIRTY_MINUTE: return 200;
                case SIXTY_MINUTE: return 400;
                case DAY: return 2000;
                default: return 60;
            }
        }
    }
    
    public HistoricalDataFetcher(String apiKey, String accessToken) {
        this.apiKey = apiKey;
        this.accessToken = accessToken;
    }
    
    /**
     * Fetch historical data for an instrument
     * @param instrumentToken Instrument token (get from instruments list)
     * @param interval Data interval
     * @param fromDate Start date
     * @param toDate End date
     * @param includeOI Include Open Interest (for F&O)
     * @return List of candles
     */
    public List<Candle> fetchHistoricalData(
            long instrumentToken,
            Interval interval,
            Date fromDate,
            Date toDate,
            boolean includeOI) throws Exception {
        
        List<Candle> allCandles = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        // Calculate days difference
        long diffInMillis = toDate.getTime() - fromDate.getTime();
        int daysDiff = (int) (diffInMillis / (1000 * 60 * 60 * 24));
        int maxDays = interval.getMaxDays();
        
        if (daysDiff <= maxDays) {
            // Single request
            return fetchSingleRequest(instrumentToken, interval, fromDate, toDate, includeOI);
        } else {
            // Multiple requests needed
            System.out.println("📊 Fetching " + daysDiff + " days of data in chunks of " + maxDays + " days...");
            
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(fromDate);
            
            while (calendar.getTime().before(toDate)) {
                Date chunkStart = calendar.getTime();
                calendar.add(Calendar.DAY_OF_MONTH, maxDays);
                
                Date chunkEnd = calendar.getTime();
                if (chunkEnd.after(toDate)) {
                    chunkEnd = toDate;
                }
                
                System.out.println("   Fetching: " + sdf.format(chunkStart) + " to " + sdf.format(chunkEnd));
                
                List<Candle> chunk = fetchSingleRequest(instrumentToken, interval, chunkStart, chunkEnd, includeOI);
                allCandles.addAll(chunk);
                
                // Rate limiting
                Thread.sleep(RATE_LIMIT_DELAY_MS);
            }
            
            System.out.println("✅ Total candles fetched: " + allCandles.size());
        }
        
        return allCandles;
    }
    
    /**
     * Fetch data in single request
     */
    private List<Candle> fetchSingleRequest(
            long instrumentToken,
            Interval interval,
            Date fromDate,
            Date toDate,
            boolean includeOI) throws Exception {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        String endpoint = String.format("/instruments/historical/%d/%s?from=%s&to=%s",
            instrumentToken,
            interval.getValue(),
            URLEncoder.encode(sdf.format(fromDate), "UTF-8"),
            URLEncoder.encode(sdf.format(toDate), "UTF-8"));
        
        if (includeOI) {
            endpoint += "&oi=1";
        }
        
        JSONObject response = makeGetRequest(endpoint);
        
        List<Candle> candles = new ArrayList<>();
        if (response.getString("status").equals("success")) {
            JSONObject data = response.getJSONObject("data");
            JSONArray candleArray = data.getJSONArray("candles");
            
            for (int i = 0; i < candleArray.length(); i++) {
                JSONArray candleData = candleArray.getJSONArray(i);
                candles.add(new Candle(candleData));
            }
        }
        
        return candles;
    }
    
    /**
     * Get instrument token by trading symbol
     */
    public long getInstrumentToken(String exchange, String tradingSymbol) throws Exception {
        // Fetch instruments list
        String endpoint = "/instruments/" + exchange;
        
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        
        // CSV format: instrument_token, exchange_token, tradingsymbol, ...
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue; // Skip header
            }
            
            String[] parts = line.split(",");
            if (parts.length > 2 && parts[2].trim().equalsIgnoreCase(tradingSymbol)) {
                reader.close();
                return Long.parseLong(parts[0].trim());
            }
        }
        
        reader.close();
//        throw new Exception("Instrument not found: " + tradingSymbol);
        System.out.println("Instrument not found: " + tradingSymbol);
        return -1;
    }
    
    /**
     * Fetch instruments list
     */
    public List<Instrument> getInstruments(String exchange) throws Exception {
        List<Instrument> instruments = new ArrayList<>();
        String endpoint = "/instruments/" + exchange;
        
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String line;
        
        boolean firstLine = true;
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            
            String[] parts = line.split(",");
            if (parts.length >= 12) {
                Instrument inst = new Instrument();
                inst.instrumentToken = Long.parseLong(parts[0].trim());
                inst.exchangeToken = parts[1].trim();
                inst.tradingSymbol = parts[2].trim();
                inst.name = parts[3].trim();
                inst.lastPrice = parts[4].trim().isEmpty() ? 0 : Double.parseDouble(parts[4].trim());
                inst.expiry = parts[5].trim();
                inst.strike = parts[6].trim().isEmpty() ? 0 : Double.parseDouble(parts[6].trim());
                inst.tickSize = parts[7].trim().isEmpty() ? 0 : Double.parseDouble(parts[7].trim());
                inst.lotSize = parts[8].trim().isEmpty() ? 0 : Integer.parseInt(parts[8].trim());
                inst.instrumentType = parts[9].trim();
                inst.segment = parts[10].trim();
                inst.exchange = parts[11].trim();
                
                instruments.add(inst);
            }
        }
        
        reader.close();
        return instruments;
    }
    
    /**
     * Save candles to CSV file
     */
    public void saveToCSV(List<Candle> candles, String filename) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(filename));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        writer.println("timestamp,open,high,low,close,volume,oi");
        
        for (Candle candle : candles) {
            writer.printf("%s,%.2f,%.2f,%.2f,%.2f,%d,%d\n",
                sdf.format(candle.timestamp),
                candle.open,
                candle.high,
                candle.low,
                candle.close,
                candle.volume,
                candle.openInterest);
        }
        
        writer.close();
        System.out.println("✅ Data saved to: " + filename);
    }
    
    /**
     * Load candles from CSV file
     */
    public List<Candle> loadFromCSV(String filename) throws IOException, ParseException {
        List<Candle> candles = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        boolean firstLine = true;
        
        while ((line = reader.readLine()) != null) {
            if (firstLine) {
                firstLine = false;
                continue;
            }
            
            String[] parts = line.split(",");
            if (parts.length >= 6) {
                Candle candle = new Candle(
                    sdf.parse(parts[0]),
                    Double.parseDouble(parts[1]),
                    Double.parseDouble(parts[2]),
                    Double.parseDouble(parts[3]),
                    Double.parseDouble(parts[4]),
                    Long.parseLong(parts[5])
                );
                if (parts.length > 6) {
                    candle.openInterest = Long.parseLong(parts[6]);
                }
                candles.add(candle);
            }
        }
        
        reader.close();
        System.out.println("✅ Loaded " + candles.size() + " candles from: " + filename);
        return candles;
    }
    
    /**
     * Make GET request
     */
    private JSONObject makeGetRequest(String endpoint) throws Exception {
        URL url = new URL(API_BASE_URL + endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("X-Kite-Version", API_VERSION);
        conn.setRequestProperty("Authorization", "token " + apiKey + ":" + accessToken);
        
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
}

/**
 * Instrument data structure
 */
class Instrument {
    public long instrumentToken;
    public String exchangeToken;
    public String tradingSymbol;
    public String name;
    public double lastPrice;
    public String expiry;
    public double strike;
    public double tickSize;
    public int lotSize;
    public String instrumentType;
    public String segment;
    public String exchange;
    
    @Override
    public String toString() {
        return String.format("%s (%s) - Token: %d", tradingSymbol, name, instrumentToken);
    }
}
