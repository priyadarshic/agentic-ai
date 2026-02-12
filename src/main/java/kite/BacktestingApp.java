package kite;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Complete Backtesting Application
 * Integrates Kite Connect API, Trading Strategies, and Backtesting Engine
 */
public class BacktestingApp {
    
    public static void main(String[] args) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🚀 ALGORITHMIC TRADING BACKTESTING SYSTEM");
            System.out.println("=".repeat(80));

            // ============================================================
            // STEP 1: AUTHENTICATION
            // ============================================================

            String API_KEY = System.getenv("KITE_API_KEY");
            String API_SECRET = System.getenv("KITE_API_SECRET");

            if (API_KEY == null || API_SECRET == null) {
                System.out.println("⚠️  Environment variables not set. Using hardcoded values.");
                API_KEY = "your_api_key_here";
                API_SECRET = "your_api_secret_here";
            }

            KiteConnectAppWithServer kite = new KiteConnectAppWithServer(API_KEY, API_SECRET);

            // Perform login (comment out if you already have access token)
            System.out.println("\n📋 Step 1: Authentication");
            JSONObject userProfile = kite.performLogin();
            String accessToken = userProfile.getString("access_token");

            // ============================================================
            // STEP 2: FETCH HISTORICAL DATA
            // ============================================================

            System.out.println("\n📋 Step 2: Fetch Historical Data");

            HistoricalDataFetcher dataFetcher = new HistoricalDataFetcher(
                    API_KEY,
                    accessToken
            );

            Scanner scanner;
            while (true) {



            System.out.print("\n📊 Enter Trading Symbol (e.g., INFY, RELIANCE, TCS): ");
            scanner = new Scanner(System.in);
            String tradingSymbol = scanner.nextLine().trim().toUpperCase();
            if (tradingSymbol.equals("EXIT")) {
                break;
            }

            String exchange = "NSE";

            System.out.println("\n🔍 Searching for instrument: " + tradingSymbol);
            long instrumentToken = dataFetcher.getInstrumentToken(exchange, tradingSymbol);
            System.out.println("✅ Found instrument token: " + instrumentToken);

            // Define date range for backtesting
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date fromDate = sdf.parse("2024-01-01");
            Date toDate = sdf.parse("2024-12-31");

            System.out.println("\n📥 Fetching historical data...");
            System.out.println("   Symbol: " + tradingSymbol);
            System.out.println("   From: " + sdf.format(fromDate));
            System.out.println("   To: " + sdf.format(toDate));
            System.out.println("   Interval: Day");

            List<Candle> historicalData = dataFetcher.fetchHistoricalData(
                    instrumentToken,
                    HistoricalDataFetcher.Interval.DAY,
                    fromDate,
                    toDate,
                    false // includeOI
            );

            System.out.println("✅ Fetched " + historicalData.size() + " candles");

            // Save data to CSV for future use
            String csvFilename = "historical_data_" + tradingSymbol.replace(" ", "_") + ".csv";
            dataFetcher.saveToCSV(historicalData, csvFilename);

            // ============================================================
            // STEP 3: DEFINE STRATEGIES TO TEST
            // ============================================================

            System.out.println("\n📋 Step 3: Define Trading Strategies");

            List<TradingStrategy> strategies = new ArrayList<>();

            // Strategy 1: Moving Average Crossover
            TradingStrategy maCrossover = new MovingAverageCrossoverStrategy();
            Map<String, Object> maParams = new HashMap<>();
            maParams.put("fastPeriod", 10);
            maParams.put("slowPeriod", 20);
            maCrossover.initialize(maParams);
            strategies.add(maCrossover);

            // Strategy 2: RSI
            TradingStrategy rsi = new RSIStrategy();
            Map<String, Object> rsiParams = new HashMap<>();
            rsiParams.put("period", 14);
            rsiParams.put("oversold", 30.0);
            rsiParams.put("overbought", 70.0);
            rsi.initialize(rsiParams);
            strategies.add(rsi);

            // Strategy 3: Bollinger Bands
            TradingStrategy bb = new BollingerBandsStrategy();
            Map<String, Object> bbParams = new HashMap<>();
            bbParams.put("period", 20);
            bbParams.put("stdDev", 2.0);
            bb.initialize(bbParams);
            strategies.add(bb);

            // Strategy 4: MACD
            TradingStrategy macd = new MACDStrategy();
            Map<String, Object> macdParams = new HashMap<>();
            macdParams.put("fast", 12);
            macdParams.put("slow", 26);
            macdParams.put("signal", 9);
            macd.initialize(macdParams);
            strategies.add(macd);

            // Strategy 5: Breakout
            TradingStrategy breakout = new BreakoutStrategy();
            Map<String, Object> breakoutParams = new HashMap<>();
            breakoutParams.put("lookback", 20);
            breakout.initialize(breakoutParams);
            strategies.add(breakout);

            System.out.println("✅ " + strategies.size() + " strategies configured");

            // ============================================================
            // STEP 4: RUN BACKTESTS
            // ============================================================

            System.out.println("\n📋 Step 4: Run Backtests");

            double initialCapital = 100000; // ₹1,00,000
            int quantity = 1; // Trade quantity

            List<BacktestResult> results = new ArrayList<>();

            for (TradingStrategy strategy : strategies) {
                BacktestEngine engine = new BacktestEngine(strategy, initialCapital, quantity);
                engine.loadHistoricalData(historicalData);

                BacktestResult result = engine.runBacktest();
                engine.printReport(result);

                results.add(result);
            }

            // ============================================================
            // STEP 5: COMPARE STRATEGIES
            // ============================================================

            System.out.println("\n📋 Step 5: Strategy Comparison");
            compareStrategies(results);

            // ============================================================
            // STEP 6: EXPORT RESULTS
            // ============================================================

            System.out.println("\n📋 Step 6: Export Results");

            for (BacktestResult result : results) {
                String filename = "backtest_" +
                        result.strategyName.replace(" ", "_") + ".csv";

                java.io.PrintWriter writer = new java.io.PrintWriter(filename);
                writer.print(result.toCSV());
                writer.close();

                System.out.println("✅ Exported: " + filename);
            }
        }
            // Clean up
            scanner.close();
            
            // Logout
            kite.logout();
            
            // Stop callback server
            kite.stopCallbackServer();
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ BACKTESTING COMPLETE");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Compare strategies and rank them
     */
    private static void compareStrategies(List<BacktestResult> results) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 STRATEGY RANKING");
        System.out.println("=".repeat(80));
        
        // Sort by total return
        results.sort((a, b) -> Double.compare(b.totalReturn, a.totalReturn));
        
        System.out.println("\n📊 BY TOTAL RETURN:");
        System.out.println(String.format("   %-30s | %10s | %10s | %10s | %10s",
            "Strategy", "Return %", "Win Rate%", "Profit ₹", "Drawdown%"));
        System.out.println("   " + "-".repeat(78));
        
        for (int i = 0; i < results.size(); i++) {
            BacktestResult r = results.get(i);
            System.out.println(String.format("%d. %-30s | %9.2f%% | %9.2f%% | %9.2f | %9.2f%%",
                i + 1,
                r.strategyName,
                r.totalReturn,
                r.winRate,
                r.finalCapital - r.initialCapital,
                r.maxDrawdown));
        }
        
        // Find best by different metrics
        BacktestResult bestReturn = results.stream()
            .max(Comparator.comparingDouble(r -> r.totalReturn))
            .orElse(null);
        
        BacktestResult bestWinRate = results.stream()
            .max(Comparator.comparingDouble(r -> r.winRate))
            .orElse(null);
        
        BacktestResult bestProfitFactor = results.stream()
            .max(Comparator.comparingDouble(r -> r.profitFactor))
            .orElse(null);
        
        BacktestResult lowestDrawdown = results.stream()
            .min(Comparator.comparingDouble(r -> r.maxDrawdown))
            .orElse(null);
        
        System.out.println("\n🥇 BEST PERFORMERS:");
        if (bestReturn != null) {
            System.out.println("   Highest Return:     " + bestReturn.strategyName + 
                " (" + String.format("%.2f%%", bestReturn.totalReturn) + ")");
        }
        if (bestWinRate != null) {
            System.out.println("   Best Win Rate:      " + bestWinRate.strategyName + 
                " (" + String.format("%.2f%%", bestWinRate.winRate) + ")");
        }
        if (bestProfitFactor != null) {
            System.out.println("   Best Profit Factor: " + bestProfitFactor.strategyName + 
                " (" + String.format("%.2f", bestProfitFactor.profitFactor) + ")");
        }
        if (lowestDrawdown != null) {
            System.out.println("   Lowest Drawdown:    " + lowestDrawdown.strategyName + 
                " (" + String.format("%.2f%%", lowestDrawdown.maxDrawdown) + ")");
        }
        
        System.out.println("\n💡 RECOMMENDATIONS:");
        
        if (bestReturn != null && bestReturn.totalReturn > 0) {
            System.out.println("   ✅ Best overall strategy: " + bestReturn.strategyName);
            System.out.println("      Return: " + String.format("%.2f%%", bestReturn.totalReturn) + 
                ", Win Rate: " + String.format("%.2f%%", bestReturn.winRate) + 
                ", Max Drawdown: " + String.format("%.2f%%", bestReturn.maxDrawdown));
        } else {
            System.out.println("   ⚠️  All strategies showed negative returns. Consider:");
            System.out.println("      - Adjusting strategy parameters");
            System.out.println("      - Testing different time periods");
            System.out.println("      - Trying different instruments");
        }
    }
}
