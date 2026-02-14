package kite;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Diagnostic Tool - Check Why No Trades Generated
 */
public class BacktestDiagnostic {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🔍 BACKTEST DIAGNOSTIC TOOL");
            System.out.println("=".repeat(80));

            // Authenticate
            String apiKey = System.getenv("KITE_API_KEY");
            String apiSecret = System.getenv("KITE_API_SECRET");


            KiteConnectAppWithServer kite = new KiteConnectAppWithServer(apiKey, apiSecret);
            JSONObject session = kite.performLogin();
            String accessToken = session.getString("access_token");

            HistoricalDataFetcher fetcher = new HistoricalDataFetcher(apiKey, accessToken);

            while(true){
                // Get symbol
                System.out.print("\nEnter symbol to diagnose: ");
                String symbol = scanner.nextLine().trim().toUpperCase();
                if(symbol.equals("EXIT")){
                    break;
                }

                // Get instrument token
                System.out.println("\n🔍 Looking up " + symbol + "...");
                long instrumentToken = fetcher.getInstrumentToken("NSE", symbol);
                if(instrumentToken == -1)
                {
                    continue;
                }
                System.out.println("✅ Instrument token: " + instrumentToken);

                // Fetch data
                System.out.print("\nEnter days to fetch (e.g., 30): ");
                int days = Integer.parseInt(scanner.nextLine().trim());

                Calendar cal = Calendar.getInstance();
                Date toDate = cal.getTime();
                cal.add(Calendar.DAY_OF_MONTH, -days);
                Date fromDate = cal.getTime();

                System.out.println("\n📥 Fetching 5-minute data...");
                List<Candle> candles = fetcher.fetchHistoricalData(
                        instrumentToken,
                        HistoricalDataFetcher.Interval.FIVE_MINUTE,
                        fromDate,
                        toDate,
                        false
                );

                System.out.println("✅ Fetched " + candles.size() + " candles");

                if (candles.size() == 0) {
                    System.out.println("\n❌ PROBLEM: No data fetched!");
                    System.out.println("Possible reasons:");
                    System.out.println("1. Symbol doesn't exist or is incorrect");
                    System.out.println("2. No trading in the selected period (holidays)");
                    System.out.println("3. API rate limit reached");
                    return;
                }

                // Run diagnostics
                runDiagnostics(candles);

            }

            // Cleanup
            kite.logout();
            kite.stopCallbackServer();
            scanner.close();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void runDiagnostics(List<Candle> candles) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 DATA QUALITY CHECK");
        System.out.println("=".repeat(80));
        
        // 1. Basic stats
        System.out.println("\n1️⃣ BASIC STATISTICS:");
        System.out.println("   Total candles: " + candles.size());
        System.out.println("   First candle: " + sdf.format(candles.get(0).timestamp));
        System.out.println("   Last candle: " + sdf.format(candles.get(candles.size()-1).timestamp));
        
        // 2. Check time range
        System.out.println("\n2️⃣ TIME RANGE CHECK:");
        
        int marketHourCandles = 0;
        int beforeMarketOpen = 0;
        int afterMarketClose = 0;
        
        for (Candle c : candles) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(c.timestamp);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            
            if (hour < 9 || (hour == 9 && minute < 15)) {
                beforeMarketOpen++;
            } else if (hour > 15 || (hour == 15 && minute >= 30)) {
                afterMarketClose++;
            } else {
                marketHourCandles++;
            }
        }
        
        System.out.println("   Market hours (9:15-15:30): " + marketHourCandles + " candles");
        System.out.println("   Before 9:15 AM: " + beforeMarketOpen + " candles");
        System.out.println("   After 3:30 PM: " + afterMarketClose + " candles");
        
        if (marketHourCandles < 50) {
            System.out.println("   ⚠️  WARNING: Very few market-hour candles!");
        }
        
        // 3. Price range check
        System.out.println("\n3️⃣ PRICE RANGE CHECK:");
        
        double minPrice = Double.MAX_VALUE;
        double maxPrice = Double.MIN_VALUE;
        double avgVolume = 0;
        
        for (Candle c : candles) {
            minPrice = Math.min(minPrice, c.low);
            maxPrice = Math.max(maxPrice, c.high);
            avgVolume += c.volume;
        }
        
        avgVolume /= candles.size();
        double priceRange = maxPrice - minPrice;
        double priceRangePercent = (priceRange / minPrice) * 100;
        
        System.out.println("   Lowest price: ₹" + String.format("%.2f", minPrice));
        System.out.println("   Highest price: ₹" + String.format("%.2f", maxPrice));
        System.out.println("   Price range: ₹" + String.format("%.2f", priceRange) + 
            " (" + String.format("%.2f%%", priceRangePercent) + ")");
        System.out.println("   Average volume: " + String.format("%,.0f", avgVolume));
        
        if (priceRangePercent < 1) {
            System.out.println("   ⚠️  WARNING: Very low volatility - may not generate signals!");
        }
        
        // 4. Check for valid trading days
        System.out.println("\n4️⃣ TRADING DAYS:");
        
        Set<String> tradingDays = new HashSet<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        
        for (Candle c : candles) {
            tradingDays.add(dayFormat.format(c.timestamp));
        }
        
        System.out.println("   Unique trading days: " + tradingDays.size());
        
        if (tradingDays.size() < 5) {
            System.out.println("   ⚠️  WARNING: Very few trading days!");
        }
        
        // 5. Test each strategy for signals
        System.out.println("\n5️⃣ STRATEGY SIGNAL ANALYSIS:");
        
        testStrategySignals(candles);
        
        // 6. Show sample candles
        System.out.println("\n6️⃣ SAMPLE DATA (First 10 market-hour candles):");
        System.out.println(String.format("   %-20s | %8s | %8s | %8s | %8s | %10s",
            "Time", "Open", "High", "Low", "Close", "Volume"));
        System.out.println("   " + "-".repeat(75));
        
        int count = 0;
        for (Candle c : candles) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(c.timestamp);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            int minute = cal.get(Calendar.MINUTE);
            
            if (hour >= 9 && hour <= 15) {
                System.out.println(String.format("   %-20s | %8.2f | %8.2f | %8.2f | %8.2f | %10d",
                    sdf.format(c.timestamp),
                    c.open, c.high, c.low, c.close, c.volume));
                count++;
                if (count >= 10) break;
            }
        }
    }
    
    private static void testStrategySignals(List<Candle> candles) {
        // Test Opening Range Breakout
        System.out.println("\n   🔹 Opening Range Breakout:");
        
        IntradayStrategy orb = new OpeningRangeBreakout();
        Map<String, Object> orbParams = new HashMap<>();
        orbParams.put("rangePeriod", 15);
        orbParams.put("stopLoss", 0.5);
        orbParams.put("target", 1.0);
        orb.initialize(orbParams);
        
        int orbSignals = countSignals(candles, orb);
        System.out.println("      Signals generated: " + orbSignals);
        
        if (orbSignals == 0) {
            System.out.println("      ⚠️  No signals! Possible reasons:");
            System.out.println("         - Price didn't break opening range");
            System.out.println("         - Not enough opening candles");
            System.out.println("         - Low volatility");
        }
        
        // Test VWAP
        System.out.println("\n   🔹 VWAP:");
        
        IntradayStrategy vwap = new VWAPStrategy();
        Map<String, Object> vwapParams = new HashMap<>();
        vwapParams.put("stopLoss", 0.4);
        vwapParams.put("target", 0.8);
        vwap.initialize(vwapParams);
        
        int vwapSignals = countSignals(candles, vwap);
        System.out.println("      Signals generated: " + vwapSignals);
        
        if (vwapSignals == 0) {
            System.out.println("      ⚠️  No signals! Possible reasons:");
            System.out.println("         - Price didn't cross VWAP");
            System.out.println("         - Not enough volume data");
        }
        
        // Test Supertrend
        System.out.println("\n   🔹 Supertrend:");
        
        IntradayStrategy st = new SupertrendStrategy();
        Map<String, Object> stParams = new HashMap<>();
        stParams.put("period", 10);
        stParams.put("multiplier", 3.0);
        stParams.put("stopLoss", 0.5);
        stParams.put("target", 1.5);
        st.initialize(stParams);
        
        int stSignals = countSignals(candles, st);
        System.out.println("      Signals generated: " + stSignals);
        
        if (stSignals == 0) {
            System.out.println("      ⚠️  No signals! Possible reasons:");
            System.out.println("         - No trend changes detected");
            System.out.println("         - Need more candles (min 10)");
        }
        
        // Test Scalping
        System.out.println("\n   🔹 Scalping (RSI):");
        
        IntradayStrategy scalp = new ScalpingStrategy();
        Map<String, Object> scalpParams = new HashMap<>();
        scalpParams.put("rsiPeriod", 14);
        scalpParams.put("stopLoss", 0.3);
        scalpParams.put("target", 0.6);
        scalp.initialize(scalpParams);
        
        int scalpSignals = countSignals(candles, scalp);
        System.out.println("      Signals generated: " + scalpSignals);
        
        if (scalpSignals == 0) {
            System.out.println("      ⚠️  No signals! Possible reasons:");
            System.out.println("         - RSI didn't cross 30/70 levels");
            System.out.println("         - Need at least 15 candles");
        }
    }
    
    private static int countSignals(List<Candle> candles, IntradayStrategy strategy) {
        int signalCount = 0;
        
        for (int i = 20; i < candles.size(); i++) {
            int signal = strategy.generateSignal(candles, i, candles.get(i).timestamp);
            if (signal != 0) {
                signalCount++;
            }
        }
        
        return signalCount;
    }
}
