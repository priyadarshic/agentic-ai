package kite;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Standalone Intraday Backtesting Program
 * Tests multiple symbols in a loop with all strategies
 */
public class IntradayBacktester {
    
    private static KiteConnectAppWithServer kiteConnect;
    private static String apiKey;
    private static String accessToken;
    private static HistoricalDataFetcher dataFetcher;
    
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("📊 INTRADAY STRATEGY BACKTESTING SYSTEM");
            System.out.println("=".repeat(80));
            
            // Authenticate once
            authenticateUser(scanner);
            
            // Backtesting parameters
            System.out.println("\n⚙️  BACKTESTING CONFIGURATION");
            System.out.println("=".repeat(80));
            
            System.out.print("Enter initial capital (₹): ");
            double initialCapital = Double.parseDouble(scanner.nextLine().trim());
            
            System.out.print("Enter quantity per trade: ");
            int quantity = Integer.parseInt(scanner.nextLine().trim());
            
            System.out.print("Enter number of days to backtest (e.g., 30): ");
            int daysToBacktest = Integer.parseInt(scanner.nextLine().trim());
            
            // Results storage
            Map<String, List<IntradayBacktestResult>> allResults = new LinkedHashMap<>();
            
            // Symbol loop
            boolean continueBacktest = true;
            
            while (continueBacktest) {
                System.out.println("\n" + "=".repeat(80));
                System.out.print("📊 Enter trading symbol (e.g., INFY, RELIANCE) or 'EXIT' to finish: ");
                String symbol = scanner.nextLine().trim().toUpperCase();
                
                if (symbol.equals("EXIT") || symbol.isEmpty()) {
                    continueBacktest = false;
                    break;
                }
                
                try {
                    // Fetch and backtest this symbol
                    List<IntradayBacktestResult> symbolResults = backtestSymbol(
                        symbol, 
                        initialCapital, 
                        quantity, 
                        daysToBacktest
                    );
                    
                    allResults.put(symbol, symbolResults);
                    
                    // Ask if user wants to continue
                    System.out.print("\n✅ Backtest complete! Add another symbol? (yes/no): ");
                    String response = scanner.nextLine().trim().toLowerCase();
                    
                    if (!response.equals("yes") && !response.equals("y")) {
                        continueBacktest = false;
                    }
                    
                } catch (Exception e) {
                    System.err.println("❌ Error backtesting " + symbol + ": " + e.getMessage());
                    System.out.print("Continue with another symbol? (yes/no): ");
                    String response = scanner.nextLine().trim().toLowerCase();
                    
                    if (!response.equals("yes") && !response.equals("y")) {
                        continueBacktest = false;
                    }
                }
            }
            
            // Generate comprehensive comparison report
            if (!allResults.isEmpty()) {
                generateComparisonReport(allResults, initialCapital);
                
                // Export results
                System.out.print("\n💾 Export results to CSV? (yes/no): ");
                String export = scanner.nextLine().trim().toLowerCase();
                
                if (export.equals("yes") || export.equals("y")) {
                    exportResultsToCSV(allResults);
                }
            }
            
            // Cleanup
            kiteConnect.logout();
            kiteConnect.stopCallbackServer();
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ BACKTESTING SESSION COMPLETE");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("\n❌ Fatal error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            scanner.close();
        }
    }
    
    /**
     * Authenticate user once
     */
    private static void authenticateUser(Scanner scanner) throws Exception {
        System.out.println("\n🔐 AUTHENTICATION");
        System.out.println("=".repeat(80));
        
        apiKey = System.getenv("KITE_API_KEY");
        String apiSecret = System.getenv("KITE_API_SECRET");
        
        if (apiKey == null || apiSecret == null) {
            System.out.print("Enter API Key: ");
            apiKey = scanner.nextLine().trim();
            System.out.print("Enter API Secret: ");
            apiSecret = scanner.nextLine().trim();
        }
        
        kiteConnect = new KiteConnectAppWithServer(apiKey, apiSecret);
        JSONObject session = kiteConnect.performLogin();
        accessToken = session.getString("access_token");
        
        dataFetcher = new HistoricalDataFetcher(apiKey, accessToken);
        
        System.out.println("✅ Authentication successful!");
    }
    
    /**
     * Backtest a single symbol with all strategies
     */
    private static List<IntradayBacktestResult> backtestSymbol(
            String symbol, 
            double initialCapital, 
            int quantity, 
            int daysToBacktest) throws Exception {
        
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 BACKTESTING: " + symbol);
        System.out.println("=".repeat(80));
        
        // Get instrument token
        System.out.println("🔍 Looking up instrument...");
        long instrumentToken = dataFetcher.getInstrumentToken("NSE", symbol);
        System.out.println("✅ Instrument token: " + instrumentToken);
        
        // Fetch historical 5-minute data
        System.out.println("📥 Fetching " + daysToBacktest + " days of 5-minute data...");
        
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -daysToBacktest);
        Date fromDate = cal.getTime();
        
        List<Candle> intradayData = dataFetcher.fetchHistoricalData(
            instrumentToken,
            HistoricalDataFetcher.Interval.FIVE_MINUTE,
            fromDate,
            toDate,
            false
        );
        
        System.out.println("✅ Fetched " + intradayData.size() + " candles");
        
        if (intradayData.size() < 100) {
            throw new Exception("Insufficient data for backtesting (need at least 100 candles)");
        }
        
        // Define all strategies
        List<IntradayStrategy> strategies = getAllStrategies();
        
        // Run backtests
        List<IntradayBacktestResult> results = new ArrayList<>();
        
        System.out.println("\n🔬 Running backtests on " + strategies.size() + " strategies...\n");
        
        for (IntradayStrategy strategy : strategies) {
            System.out.println("Testing: " + strategy.getName() + "...");
            
            IntradayBacktestEngine engine = new IntradayBacktestEngine(
                strategy, 
                initialCapital, 
                quantity
            );
            engine.loadIntradayData(intradayData);
            
            IntradayBacktestResult result = engine.runBacktest();
            results.add(result);
            
            // Print quick summary
            System.out.println("   Return: " + String.format("%.2f%%", result.totalReturn) + 
                             " | Trades: " + result.totalTrades + 
                             " | Win Rate: " + String.format("%.1f%%", result.winRate));
        }
        
        // Print detailed results for this symbol
        printSymbolResults(symbol, results, initialCapital);
        
        return results;
    }
    
    /**
     * Get all strategies with default parameters
     */
    private static List<IntradayStrategy> getAllStrategies() {
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
     * Print detailed results for a symbol
     */
    private static void printSymbolResults(String symbol, List<IntradayBacktestResult> results, 
                                          double initialCapital) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 DETAILED RESULTS: " + symbol);
        System.out.println("=".repeat(80));
        
        // Sort by return
        results.sort((a, b) -> Double.compare(b.totalReturn, a.totalReturn));
        
        System.out.println(String.format("\n%-20s | %10s | %8s | %10s | %10s | %8s | %8s",
            "Strategy", "Return %", "Trades", "Win Rate%", "Profit ₹", "Costs ₹", "Auto-SQ"));
        System.out.println("-".repeat(100));
        
        for (IntradayBacktestResult r : results) {
            System.out.println(String.format("%-20s | %9.2f%% | %8d | %9.1f%% | %9.0f | %8.0f | %8d%s",
                r.strategyName,
                r.totalReturn,
                r.totalTrades,
                r.winRate,
                r.finalCapital - r.initialCapital,
                r.totalCosts,
                r.autoSquareOffs,
                r.autoSquareOffs == 0 ? " ✅" : " ⚠️"));
        }
        
        // Best strategy summary
        IntradayBacktestResult best = results.get(0);
        System.out.println("\n🥇 BEST STRATEGY FOR " + symbol + ": " + best.strategyName);
        System.out.println("   Return: " + String.format("%.2f%%", best.totalReturn));
        System.out.println("   Total Profit: ₹" + String.format("%.2f", best.finalCapital - best.initialCapital));
        System.out.println("   Win Rate: " + String.format("%.1f%%", best.winRate));
        System.out.println("   Profit Factor: " + String.format("%.2f", best.profitFactor));
        System.out.println("   Max Drawdown: " + String.format("%.2f%%", best.maxDrawdown));
        System.out.println("   Avg Trade Duration: " + String.format("%.0f mins", best.avgTradeDurationMinutes));
        System.out.println("   Total Costs: ₹" + String.format("%.2f", best.totalCosts));
        System.out.println("   Cost Impact: " + String.format("%.1f%%", 
            (best.totalCosts / (best.finalCapital - best.initialCapital + best.totalCosts)) * 100));
        
        // Trade breakdown
        System.out.println("\n📈 TRADE BREAKDOWN:");
        System.out.println("   Total Trades: " + best.totalTrades);
        System.out.println("   Winning: " + best.winningTrades + 
            " (Avg: ₹" + String.format("%.2f", best.averageWin) + ")");
        System.out.println("   Losing: " + best.losingTrades + 
            " (Avg: ₹" + String.format("%.2f", best.averageLoss) + ")");
        System.out.println("   Target Hits: " + best.targetHits);
        System.out.println("   Stop Loss Hits: " + best.stopLossHits);
        System.out.println("   Auto Square-offs: " + best.autoSquareOffs + 
            (best.autoSquareOffs == 0 ? " ✅ Perfect!" : " ⚠️  Needs optimization"));
        
        // Daily performance
        System.out.println("\n📅 DAILY PERFORMANCE:");
        System.out.println("   Trading Days: " + best.totalDays);
        System.out.println("   Profitable Days: " + best.profitableDays + 
            " (" + String.format("%.1f%%", (double)best.profitableDays/best.totalDays*100) + ")");
        System.out.println("   Avg Profit/Day: ₹" + 
            String.format("%.2f", (best.finalCapital - best.initialCapital) / best.totalDays));
    }
    
    /**
     * Generate comprehensive comparison report across all symbols
     */
    private static void generateComparisonReport(Map<String, List<IntradayBacktestResult>> allResults, 
                                                double initialCapital) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🏆 CROSS-SYMBOL COMPARISON REPORT");
        System.out.println("=".repeat(80));
        
        // Find best strategy for each symbol
        System.out.println("\n📊 BEST STRATEGY PER SYMBOL:");
        System.out.println(String.format("%-15s | %-20s | %10s | %10s | %8s | %10s",
            "Symbol", "Best Strategy", "Return %", "Profit ₹", "Trades", "Win Rate%"));
        System.out.println("-".repeat(90));
        
        Map<String, IntradayBacktestResult> bestPerSymbol = new LinkedHashMap<>();
        
        for (Map.Entry<String, List<IntradayBacktestResult>> entry : allResults.entrySet()) {
            String symbol = entry.getKey();
            List<IntradayBacktestResult> results = entry.getValue();
            
            // Sort by return
            results.sort((a, b) -> Double.compare(b.totalReturn, a.totalReturn));
            IntradayBacktestResult best = results.get(0);
            bestPerSymbol.put(symbol, best);
            
            System.out.println(String.format("%-15s | %-20s | %9.2f%% | %9.0f | %8d | %9.1f%%",
                symbol,
                best.strategyName,
                best.totalReturn,
                best.finalCapital - initialCapital,
                best.totalTrades,
                best.winRate));
        }
        
        // Overall best
        System.out.println("\n🥇 OVERALL WINNERS:");
        
        // Best by return
        Map.Entry<String, IntradayBacktestResult> bestReturn = bestPerSymbol.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().totalReturn))
            .orElse(null);
        
        if (bestReturn != null) {
            System.out.println("\n🎯 HIGHEST RETURN:");
            System.out.println("   Symbol: " + bestReturn.getKey());
            System.out.println("   Strategy: " + bestReturn.getValue().strategyName);
            System.out.println("   Return: " + String.format("%.2f%%", bestReturn.getValue().totalReturn));
            System.out.println("   Profit: ₹" + String.format("%.2f", 
                bestReturn.getValue().finalCapital - initialCapital));
        }
        
        // Best win rate
        Map.Entry<String, IntradayBacktestResult> bestWinRate = bestPerSymbol.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().winRate))
            .orElse(null);
        
        if (bestWinRate != null) {
            System.out.println("\n🎲 HIGHEST WIN RATE:");
            System.out.println("   Symbol: " + bestWinRate.getKey());
            System.out.println("   Strategy: " + bestWinRate.getValue().strategyName);
            System.out.println("   Win Rate: " + String.format("%.1f%%", bestWinRate.getValue().winRate));
        }
        
        // Most consistent (lowest drawdown)
        Map.Entry<String, IntradayBacktestResult> lowestDD = bestPerSymbol.entrySet().stream()
            .min(Comparator.comparingDouble(e -> e.getValue().maxDrawdown))
            .orElse(null);
        
        if (lowestDD != null) {
            System.out.println("\n🛡️  LOWEST RISK (Min Drawdown):");
            System.out.println("   Symbol: " + lowestDD.getKey());
            System.out.println("   Strategy: " + lowestDD.getValue().strategyName);
            System.out.println("   Max Drawdown: " + String.format("%.2f%%", lowestDD.getValue().maxDrawdown));
        }
        
        // Strategy performance across all symbols
        System.out.println("\n📈 STRATEGY PERFORMANCE ACROSS ALL SYMBOLS:");
        
        Map<String, StrategyStats> strategyStats = new LinkedHashMap<>();
        
        for (List<IntradayBacktestResult> results : allResults.values()) {
            for (IntradayBacktestResult result : results) {
                String strategyName = result.strategyName;
                
                if (!strategyStats.containsKey(strategyName)) {
                    strategyStats.put(strategyName, new StrategyStats(strategyName));
                }
                
                StrategyStats stats = strategyStats.get(strategyName);
                stats.addResult(result);
            }
        }
        
        System.out.println(String.format("\n%-20s | %10s | %10s | %10s | %10s",
            "Strategy", "Avg Return", "Symbols", "Best %", "Worst %"));
        System.out.println("-".repeat(70));
        
        for (StrategyStats stats : strategyStats.values()) {
            System.out.println(String.format("%-20s | %9.2f%% | %10d | %9.2f%% | %9.2f%%",
                stats.name,
                stats.avgReturn,
                stats.symbolCount,
                stats.bestReturn,
                stats.worstReturn));
        }
        
        // Recommendations
        System.out.println("\n💡 RECOMMENDATIONS:");
        
        if (bestReturn != null) {
            System.out.println("\n1. BEST TRADING OPPORTUNITY:");
            System.out.println("   Trade " + bestReturn.getKey() + " using " + 
                bestReturn.getValue().strategyName);
            System.out.println("   Expected return: " + 
                String.format("%.2f%%", bestReturn.getValue().totalReturn));
            
            // Calculate required capital for ₹500/day
            double avgDailyProfit = (bestReturn.getValue().finalCapital - initialCapital) / 
                bestReturn.getValue().totalDays;
            double capitalFor500 = initialCapital * (500 / avgDailyProfit);
            
            System.out.println("   Average profit/day: ₹" + String.format("%.2f", avgDailyProfit));
            System.out.println("   Capital needed for ₹500/day: ₹" + String.format("%.0f", capitalFor500));
        }
        
        // Find most consistent strategy
        Map.Entry<String, StrategyStats> mostConsistent = strategyStats.entrySet().stream()
            .max(Comparator.comparingDouble(e -> e.getValue().avgReturn))
            .orElse(null);
        
        if (mostConsistent != null) {
            System.out.println("\n2. MOST CONSISTENT STRATEGY:");
            System.out.println("   " + mostConsistent.getKey());
            System.out.println("   Works well on " + mostConsistent.getValue().symbolCount + " symbols");
            System.out.println("   Average return: " + 
                String.format("%.2f%%", mostConsistent.getValue().avgReturn));
        }
        
        // Portfolio suggestion
        System.out.println("\n3. DIVERSIFIED PORTFOLIO:");
        System.out.println("   Trade multiple symbols to reduce risk");
        
        int topSymbolsCount = Math.min(3, bestPerSymbol.size());
        List<Map.Entry<String, IntradayBacktestResult>> topSymbols = bestPerSymbol.entrySet().stream()
            .sorted((a, b) -> Double.compare(b.getValue().totalReturn, a.getValue().totalReturn))
            .limit(topSymbolsCount)
            .collect(java.util.stream.Collectors.toList());
        
        System.out.println("   Top " + topSymbolsCount + " symbols:");
        for (int i = 0; i < topSymbols.size(); i++) {
            Map.Entry<String, IntradayBacktestResult> entry = topSymbols.get(i);
            System.out.println("   " + (i+1) + ". " + entry.getKey() + " - " + 
                entry.getValue().strategyName + " (" + 
                String.format("%.2f%%", entry.getValue().totalReturn) + ")");
        }
    }
    
    /**
     * Export results to CSV
     */
    private static void exportResultsToCSV(Map<String, List<IntradayBacktestResult>> allResults) {
        try {
            String filename = "intraday_backtest_" + 
                new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date()) + ".csv";
            
            java.io.PrintWriter writer = new java.io.PrintWriter(filename);
            
            // Header
            writer.println("Symbol,Strategy,Return %,Total Trades,Winning Trades,Losing Trades," +
                "Win Rate %,Total Profit,Total Loss,Profit Factor,Avg Win,Avg Loss," +
                "Total Costs,Max Drawdown %,Target Hits,Stop Loss Hits,Auto Square-offs," +
                "Profitable Days,Total Days,Avg Trade Duration (mins)");
            
            // Data
            for (Map.Entry<String, List<IntradayBacktestResult>> entry : allResults.entrySet()) {
                String symbol = entry.getKey();
                
                for (IntradayBacktestResult r : entry.getValue()) {
                    writer.printf("%s,%s,%.2f,%d,%d,%d,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d,%d,%d,%d,%.0f\n",
                        symbol,
                        r.strategyName,
                        r.totalReturn,
                        r.totalTrades,
                        r.winningTrades,
                        r.losingTrades,
                        r.winRate,
                        r.totalProfit,
                        r.totalLoss,
                        r.profitFactor,
                        r.averageWin,
                        r.averageLoss,
                        r.totalCosts,
                        r.maxDrawdown,
                        r.targetHits,
                        r.stopLossHits,
                        r.autoSquareOffs,
                        r.profitableDays,
                        r.totalDays,
                        r.avgTradeDurationMinutes);
                }
            }
            
            writer.close();
            System.out.println("\n✅ Results exported to: " + filename);
            
        } catch (Exception e) {
            System.err.println("❌ Error exporting to CSV: " + e.getMessage());
        }
    }
}

/**
 * Strategy statistics aggregator
 */
class StrategyStats {
    public String name;
    public int symbolCount = 0;
    public double totalReturn = 0;
    public double avgReturn = 0;
    public double bestReturn = Double.MIN_VALUE;
    public double worstReturn = Double.MAX_VALUE;
    
    public StrategyStats(String name) {
        this.name = name;
    }
    
    public void addResult(IntradayBacktestResult result) {
        symbolCount++;
        totalReturn += result.totalReturn;
        avgReturn = totalReturn / symbolCount;
        
        if (result.totalReturn > bestReturn) {
            bestReturn = result.totalReturn;
        }
        if (result.totalReturn < worstReturn) {
            worstReturn = result.totalReturn;
        }
    }
}
