package kite;

import org.json.JSONObject;
import org.jspecify.annotations.NonNull;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Complete Intraday Trading System
 * 1. Backtest multiple strategies on historical 5-min data
 * 2. Select best strategy
 * 3. Execute live trades with best strategy
 */
public class IntradayTradingApp {
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🎯 INTRADAY TRADING SYSTEM");
            System.out.println("=".repeat(80));
            System.out.println("\nChoose mode:");
            System.out.println("1. Backtest strategies on historical data");
            System.out.println("2. Live trading with best strategy");
            System.out.println("3. Full workflow (Backtest → Select → Live Trade)");
            System.out.print("\nEnter choice (1-3): ");

            int choice = Integer.parseInt(scanner.nextLine().trim());

            if (choice == 1) {
                runBacktest(scanner);
            }
//            else if (choice == 2) {
//                runLiveTrading(scanner);
//            } else if (choice == 3) {
//                runFullWorkflow(scanner);
//            }

        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Mode 1: Backtest strategies
     */
    private static void runBacktest(Scanner scanner) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 INTRADAY STRATEGY BACKTESTING");
        System.out.println("=".repeat(80));
        
        // Get credentials
        String API_KEY = System.getenv("KITE_API_KEY");
        String API_SECRET = System.getenv("KITE_API_SECRET");
        
        if (API_KEY == null || API_SECRET == null) {
            System.out.print("\nEnter API Key: ");
            API_KEY = scanner.nextLine().trim();
            System.out.print("Enter API Secret: ");
            API_SECRET = scanner.nextLine().trim();
        }
        
        // Authenticate
        KiteConnectAppWithServer kite = new KiteConnectAppWithServer(API_KEY, API_SECRET);
        JSONObject session = kite.performLogin();
        String accessToken = session.getString("access_token");
        
        // Get trading symbol
        System.out.print("\n📊 Enter symbol for backtesting (e.g., INFY, RELIANCE): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        
        // Fetch 5-minute intraday data for last 30 days
        System.out.println("\n📥 Fetching 5-minute intraday data...");
        
        HistoricalDataFetcher fetcher = new HistoricalDataFetcher(API_KEY, accessToken);
        long instrumentToken = fetcher.getInstrumentToken("NSE", symbol);
        
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -30);
        Date fromDate = cal.getTime();
        
        List<Candle> intradayData = fetcher.fetchHistoricalData(
            instrumentToken,
            HistoricalDataFetcher.Interval.FIVE_MINUTE,
            fromDate,
            toDate,
            false
        );
        
        System.out.println("✅ Fetched " + intradayData.size() + " 5-minute candles");
        
        // Define strategies
        List<IntradayStrategy> strategies = getIntradayStrategies();

        // Run backtests
        double initialCapital = 50000; // ₹50,000
        int quantity = 20;
        
        List<IntradayBacktestResult> results = new ArrayList<>();
        
        for (IntradayStrategy strategy : strategies) {
            IntradayBacktestEngine engine = new IntradayBacktestEngine(strategy, initialCapital, quantity);
            engine.loadIntradayData(intradayData);
            
            IntradayBacktestResult result = engine.runBacktest();
            engine.printReport(result);
            
            results.add(result);
        }
        
        // Compare and select best
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 STRATEGY COMPARISON");
        System.out.println("=".repeat(80));
        
        results.sort((a, b) -> Double.compare(b.totalReturn, a.totalReturn));
        
        System.out.println(String.format("\n%-20s | %10s | %8s | %10s | %8s | %8s",
            "Strategy", "Return %", "Trades", "Win Rate%", "Auto-SQ", "Costs ₹"));
        System.out.println("-".repeat(90));
        
        for (IntradayBacktestResult r : results) {
            System.out.println(String.format("%-20s | %9.2f%% | %8d | %9.1f%% | %8d | %8.0f",
                r.strategyName,
                r.totalReturn,
                r.totalTrades,
                r.winRate,
                r.autoSquareOffs,
                r.totalCosts));
        }
        
        IntradayBacktestResult best = results.get(0);
        System.out.println("\n🥇 BEST STRATEGY: " + best.strategyName);
        System.out.println("   Return: " + String.format("%.2f%%", best.totalReturn));
        System.out.println("   Win Rate: " + String.format("%.1f%%", best.winRate));
        System.out.println("   Profit Factor: " + String.format("%.2f", best.profitFactor));
        System.out.println("   Auto Square-offs: " + best.autoSquareOffs + 
            (best.autoSquareOffs == 0 ? " ✅ Perfect!" : " ⚠️  Optimize timing"));
        
        // Cleanup
        kite.logout();
        kite.stopCallbackServer();
    }

    private static @NonNull List<IntradayStrategy> getIntradayStrategies() {
        List<IntradayStrategy> strategies = new ArrayList<>();

        // 1. Opening Range Breakout
        IntradayStrategy orb = new OpeningRangeBreakout();
        Map<String, Object> orbParams = new HashMap<>();
        orbParams.put("rangePeriod", 15);
        orbParams.put("stopLoss", 0.5);
        orbParams.put("target", 1.0);
        orb.initialize(orbParams);
        strategies.add(orb);

        // 2. VWAP
        IntradayStrategy vwap = new VWAPStrategy();
        Map<String, Object> vwapParams = new HashMap<>();
        vwapParams.put("stopLoss", 0.4);
        vwapParams.put("target", 0.8);
        vwap.initialize(vwapParams);
        strategies.add(vwap);

        // 3. Supertrend
        IntradayStrategy supertrend = new SupertrendStrategy();
        Map<String, Object> stParams = new HashMap<>();
        stParams.put("period", 10);
        stParams.put("multiplier", 3.0);
        stParams.put("stopLoss", 0.5);
        stParams.put("target", 1.5);
        supertrend.initialize(stParams);
        strategies.add(supertrend);

        // 4. Scalping
        IntradayStrategy scalping = new ScalpingStrategy();
        Map<String, Object> scalpParams = new HashMap<>();
        scalpParams.put("rsiPeriod", 14);
        scalpParams.put("stopLoss", 0.3);
        scalpParams.put("target", 0.6);
        scalping.initialize(scalpParams);
        strategies.add(scalping);
        return strategies;
    }

    /**
     * Mode 2: Live trading
     */
    private static void runLiveTrading(Scanner scanner) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🚀 LIVE INTRADAY TRADING");
        System.out.println("=".repeat(80));
        
        // Get credentials
        String API_KEY = System.getenv("KITE_API_KEY");
        String API_SECRET = System.getenv("KITE_API_SECRET");
        
        if (API_KEY == null) {
            System.out.print("\nEnter API Key: ");
            API_KEY = scanner.nextLine().trim();
            System.out.print("Enter API Secret: ");
            API_SECRET = scanner.nextLine().trim();
        }
        
        // Authenticate
        KiteConnectAppWithServer kite = new KiteConnectAppWithServer(API_KEY, API_SECRET);
        kite.performLogin();
        
        // Strategy selection
        System.out.println("\n🎯 Select strategy:");
        System.out.println("1. Opening Range Breakout");
        System.out.println("2. VWAP");
        System.out.println("3. Supertrend");
        System.out.println("4. Scalping");
        System.out.print("\nChoice (1-4): ");
        
        int strategyChoice = Integer.parseInt(scanner.nextLine().trim());
        IntradayStrategy strategy = getStrategyByChoice(strategyChoice);
        
        // Get trading parameters
        System.out.print("\n📊 Enter trading symbol (e.g., INFY): ");
        String symbol = scanner.nextLine().trim().toUpperCase();
        
        System.out.print("Enter quantity per trade: ");
        int quantity = Integer.parseInt(scanner.nextLine().trim());
        
        System.out.print("Enter capital allocated: ₹");
        double capital = Double.parseDouble(scanner.nextLine().trim());
        
        // Risk parameters
        System.out.println("\n⚠️  Risk Management:");
        System.out.print("Max loss per day (₹): ");
        double maxLoss = Double.parseDouble(scanner.nextLine().trim());
        
        System.out.print("Target profit per day (₹): ");
        double maxProfit = Double.parseDouble(scanner.nextLine().trim());
        
        System.out.print("Max trades per day: ");
        int maxTrades = Integer.parseInt(scanner.nextLine().trim());
        
        // Confirm
        System.out.println("\n" + "=".repeat(80));
        System.out.println("⚠️  LIVE TRADING CONFIRMATION");
        System.out.println("=".repeat(80));
        System.out.println("Strategy: " + strategy.getName());
        System.out.println("Symbol: " + symbol);
        System.out.println("Quantity: " + quantity);
        System.out.println("Capital: ₹" + capital);
        System.out.println("Max Loss: ₹" + maxLoss);
        System.out.println("Target: ₹" + maxProfit);
        System.out.println("Max Trades: " + maxTrades);
        System.out.print("\n⚠️  Start live trading? (yes/no): ");
        
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes")) {
            System.out.println("\n❌ Live trading cancelled.");
            return;
        }
        
        // Start live trading
        LiveIntradayTrader trader = new LiveIntradayTrader(
            kite, strategy, symbol, quantity, capital
        );
        trader.setRiskParameters(maxLoss, maxProfit, maxTrades);
        trader.startLiveTrading();
        
        // Cleanup
        kite.logout();
        kite.stopCallbackServer();
    }
    
    /**
     * Mode 3: Full workflow
     */
    private static void runFullWorkflow(Scanner scanner) throws Exception {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🎯 FULL INTRADAY TRADING WORKFLOW");
        System.out.println("=".repeat(80));
        System.out.println("\nThis will:");
        System.out.println("1. Backtest all strategies");
        System.out.println("2. Automatically select the best one");
        System.out.println("3. Execute live trades");
        System.out.println("\n⚠️  This is for advanced users only!");
        System.out.print("Continue? (yes/no): ");
        
        String confirm = scanner.nextLine().trim().toLowerCase();
        if (!confirm.equals("yes")) {
            System.out.println("\n❌ Workflow cancelled.");
            return;
        }
        
        // Run backtest first
        System.out.println("\n📊 Step 1: Running backtests...");
        runBacktest(scanner);
        
        System.out.println("\n✅ Backtest complete!");
        System.out.print("\nProceed to live trading with best strategy? (yes/no): ");
        
        confirm = scanner.nextLine().trim().toLowerCase();
        if (confirm.equals("yes")) {
            runLiveTrading(scanner);
        }
    }
    
    /**
     * Get strategy by choice
     */
    private static IntradayStrategy getStrategyByChoice(int choice) {
        IntradayStrategy strategy;
        Map<String, Object> params;
        
        switch (choice) {
            case 1:
                strategy = new OpeningRangeBreakout();
                params = new HashMap<>();
                params.put("rangePeriod", 15);
                params.put("stopLoss", 0.5);
                params.put("target", 1.0);
                strategy.initialize(params);
                break;
                
            case 2:
                strategy = new VWAPStrategy();
                params = new HashMap<>();
                params.put("stopLoss", 0.4);
                params.put("target", 0.8);
                strategy.initialize(params);
                break;
                
            case 3:
                strategy = new SupertrendStrategy();
                params = new HashMap<>();
                params.put("period", 10);
                params.put("multiplier", 3.0);
                params.put("stopLoss", 0.5);
                params.put("target", 1.5);
                strategy.initialize(params);
                break;
                
            case 4:
            default:
                strategy = new ScalpingStrategy();
                params = new HashMap<>();
                params.put("rsiPeriod", 14);
                params.put("stopLoss", 0.3);
                params.put("target", 0.6);
                strategy.initialize(params);
                break;
        }
        
        return strategy;
    }
}
