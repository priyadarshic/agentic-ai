package kite;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Standalone Backtesting Demo
 * Demonstrates backtesting with simulated data (no API required)
 */
public class BacktestDemo {
    
    public static void main(String[] args) {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🎯 BACKTESTING DEMO - NO API REQUIRED");
            System.out.println("=".repeat(80));
            System.out.println("\nThis demo uses simulated market data to demonstrate backtesting.");
            System.out.println("For live data, use the full BacktestingApp with Kite Connect API.\n");
            
            // ============================================================
            // GENERATE SAMPLE DATA
            // ============================================================
            
            System.out.println("📊 Generating simulated market data...");
            List<Candle> sampleData = generateSampleData(252); // 1 year of daily data
            System.out.println("✅ Generated " + sampleData.size() + " candles");
            
            // ============================================================
            // DEFINE STRATEGIES
            // ============================================================
            
            System.out.println("\n🎯 Configuring trading strategies...");
            
            List<TradingStrategy> strategies = new ArrayList<>();
            
            // Strategy 1: Moving Average Crossover (10, 20)
            TradingStrategy maCrossover = new MovingAverageCrossoverStrategy();
            Map<String, Object> maParams = new HashMap<>();
            maParams.put("fastPeriod", 10);
            maParams.put("slowPeriod", 20);
            maCrossover.initialize(maParams);
            strategies.add(maCrossover);
            
            // Strategy 2: RSI (14, 30/70)
            TradingStrategy rsi = new RSIStrategy();
            Map<String, Object> rsiParams = new HashMap<>();
            rsiParams.put("period", 14);
            rsiParams.put("oversold", 30.0);
            rsiParams.put("overbought", 70.0);
            rsi.initialize(rsiParams);
            strategies.add(rsi);
            
            // Strategy 3: Bollinger Bands (20, 2)
            TradingStrategy bb = new BollingerBandsStrategy();
            Map<String, Object> bbParams = new HashMap<>();
            bbParams.put("period", 20);
            bbParams.put("stdDev", 2.0);
            bb.initialize(bbParams);
            strategies.add(bb);
            
            // Strategy 4: MACD (12, 26, 9)
            TradingStrategy macd = new MACDStrategy();
            Map<String, Object> macdParams = new HashMap<>();
            macdParams.put("fast", 12);
            macdParams.put("slow", 26);
            macdParams.put("signal", 9);
            macd.initialize(macdParams);
            strategies.add(macd);
            
            // Strategy 5: Breakout (20)
            TradingStrategy breakout = new BreakoutStrategy();
            Map<String, Object> breakoutParams = new HashMap<>();
            breakoutParams.put("lookback", 20);
            breakout.initialize(breakoutParams);
            strategies.add(breakout);
            
            System.out.println("✅ " + strategies.size() + " strategies ready");
            
            // ============================================================
            // RUN BACKTESTS
            // ============================================================
            
            double initialCapital = 100000; // ₹1,00,000
            int quantity = 50; // 50 shares per trade
            
            List<BacktestResult> results = new ArrayList<>();
            
            for (TradingStrategy strategy : strategies) {
                BacktestEngine engine = new BacktestEngine(strategy, initialCapital, quantity);
                engine.loadHistoricalData(sampleData);
                
                BacktestResult result = engine.runBacktest();
                engine.printReport(result);
                
                results.add(result);
            }
            
            // ============================================================
            // COMPARE STRATEGIES
            // ============================================================
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🏆 STRATEGY COMPARISON");
            System.out.println("=".repeat(80));
            
            // Sort by total return
            results.sort((a, b) -> Double.compare(b.totalReturn, a.totalReturn));
            
            System.out.println("\n📊 PERFORMANCE RANKING:");
            System.out.println(String.format("   %-25s | %10s | %8s | %12s | %10s | %8s",
                "Strategy", "Return %", "Trades", "Win Rate %", "Profit ₹", "DD %"));
            System.out.println("   " + "-".repeat(88));
            
            for (int i = 0; i < results.size(); i++) {
                BacktestResult r = results.get(i);
                System.out.println(String.format("%d. %-25s | %9.2f%% | %8d | %11.2f%% | %11.2f | %7.2f%%",
                    i + 1,
                    r.strategyName,
                    r.totalReturn,
                    r.totalTrades,
                    r.winRate,
                    r.finalCapital - r.initialCapital,
                    r.maxDrawdown));
            }
            
            // Best performers
            BacktestResult bestReturn = results.get(0);
            BacktestResult bestWinRate = results.stream()
                .max(Comparator.comparingDouble(r -> r.winRate))
                .orElse(null);
            BacktestResult lowestDrawdown = results.stream()
                .min(Comparator.comparingDouble(r -> r.maxDrawdown))
                .orElse(null);
            
            System.out.println("\n🥇 CHAMPIONS:");
            System.out.println("   🏆 Best Return:       " + bestReturn.strategyName + 
                " (" + String.format("%.2f%%", bestReturn.totalReturn) + ")");
            if (bestWinRate != null) {
                System.out.println("   🎯 Best Win Rate:     " + bestWinRate.strategyName + 
                    " (" + String.format("%.2f%%", bestWinRate.winRate) + ")");
            }
            if (lowestDrawdown != null) {
                System.out.println("   🛡️  Lowest Drawdown:  " + lowestDrawdown.strategyName + 
                    " (" + String.format("%.2f%%", lowestDrawdown.maxDrawdown) + ")");
            }
            
            // ============================================================
            // VISUALIZATION
            // ============================================================
            
            System.out.println("\n📈 EQUITY CURVE COMPARISON:");
            visualizeEquityCurves(results, initialCapital);
            
            // ============================================================
            // RECOMMENDATIONS
            // ============================================================
            
            System.out.println("\n💡 INSIGHTS & RECOMMENDATIONS:");
            
            if (bestReturn.totalReturn > 10) {
                System.out.println("   ✅ Strong performance detected!");
                System.out.println("      Best strategy: " + bestReturn.strategyName);
                System.out.println("      Consider forward testing with paper trading.");
            } else if (bestReturn.totalReturn > 0) {
                System.out.println("   ⚠️  Moderate performance.");
                System.out.println("      Consider optimizing parameters or combining strategies.");
            } else {
                System.out.println("   ❌ All strategies showed losses.");
                System.out.println("      Recommendations:");
                System.out.println("      1. Adjust strategy parameters");
                System.out.println("      2. Test on different market conditions");
                System.out.println("      3. Consider market regime filters");
            }
            
            System.out.println("\n📝 NEXT STEPS:");
            System.out.println("   1. Test winning strategy on different time periods");
            System.out.println("   2. Optimize strategy parameters");
            System.out.println("   3. Add risk management (stop loss, position sizing)");
            System.out.println("   4. Forward test with paper trading");
            System.out.println("   5. Connect to live market with Kite Connect API");
            
            System.out.println("\n" + "=".repeat(80));
            System.out.println("✅ BACKTESTING DEMO COMPLETE");
            System.out.println("=".repeat(80));
            
        } catch (Exception e) {
            System.err.println("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Generate sample market data
     * Simulates trending and ranging market conditions
     */
    private static List<Candle> generateSampleData(int numCandles) {
        List<Candle> candles = new ArrayList<>();
        Random random = new Random(12345); // Fixed seed for reproducibility
        
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, -numCandles);
        
        double price = 1000.0; // Starting price
        double trend = 0.001; // Upward trend
        double volatility = 0.02; // 2% volatility
        
        for (int i = 0; i < numCandles; i++) {
            // Add some market cycles
            double cycleFactor = Math.sin(i * 0.1) * 0.5;
            double trendFactor = trend + (cycleFactor * 0.001);
            
            // Random walk with trend
            double change = (random.nextGaussian() * volatility) + trendFactor;
            price = price * (1 + change);
            
            // Generate OHLC
            double open = price;
            double high = price * (1 + Math.abs(random.nextGaussian() * volatility * 0.5));
            double low = price * (1 - Math.abs(random.nextGaussian() * volatility * 0.5));
            double close = low + (random.nextDouble() * (high - low));
            
            long volume = (long) (1000000 + random.nextInt(5000000));
            
            Date timestamp = cal.getTime();
            Candle candle = new Candle(timestamp, open, high, low, close, volume);
            candles.add(candle);
            
            cal.add(Calendar.DAY_OF_MONTH, 1);
            price = close; // Next day starts from previous close
        }
        
        return candles;
    }
    
    /**
     * Simple text-based equity curve visualization
     */
    private static void visualizeEquityCurves(List<BacktestResult> results, double initialCapital) {
        System.out.println("\n   Strategy Performance Over Time:");
        
        for (BacktestResult result : results) {
            double returnPct = result.totalReturn;
            int bars = (int) Math.round(returnPct / 2); // Scale for display
            
            StringBuilder chart = new StringBuilder();
            chart.append(String.format("   %-25s |", result.strategyName));
            
            if (bars > 0) {
                chart.append(" ");
                for (int i = 0; i < Math.min(bars, 50); i++) {
                    chart.append("█");
                }
                chart.append(String.format(" +%.2f%%", returnPct));
            } else if (bars < 0) {
                for (int i = 0; i > Math.max(bars, -50); i--) {
                    chart.append("▓");
                }
                chart.append(String.format(" %.2f%%", returnPct));
            } else {
                chart.append(" ▪ 0.00%");
            }
            
            System.out.println(chart.toString());
        }
    }
}
