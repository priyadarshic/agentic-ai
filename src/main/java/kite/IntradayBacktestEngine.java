package kite;

import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Intraday Backtest Engine
 * Specialized for day trading with realistic constraints
 */
public class IntradayBacktestEngine {
    
    private IntradayStrategy strategy;
    private List<Candle> intradayData;
    private List<IntradayTrade> trades;
    private double initialCapital;
    private double currentCapital;
    private int quantity;
    private IntradayTrade currentTrade;
    
    // Cost settings
    private boolean includeCosts = true;
    private static final double AUTO_SQUARE_OFF_PENALTY = 50; // ₹50 per order
    private static final double BROKERAGE_PER_ORDER = 20; // ₹20 or 0.03%
    
    // Performance metrics
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private int targetHits;
    private int stopLossHits;
    private int autoSquareOffs;
    private double totalProfit;
    private double totalLoss;
    private double totalCosts;
    private double maxDrawdown;
    private double maxCapital;
    
    // Daily tracking
    private Map<String, DayStats> dailyStats;
    
    public IntradayBacktestEngine(IntradayStrategy strategy, double initialCapital, int quantity) {
        this.strategy = strategy;
        this.initialCapital = initialCapital;
        this.currentCapital = initialCapital;
        this.quantity = quantity;
        this.trades = new ArrayList<>();
        this.dailyStats = new LinkedHashMap<>();
        
        resetMetrics();
    }
    
    private void resetMetrics() {
        this.totalTrades = 0;
        this.winningTrades = 0;
        this.losingTrades = 0;
        this.targetHits = 0;
        this.stopLossHits = 0;
        this.autoSquareOffs = 0;
        this.totalProfit = 0;
        this.totalLoss = 0;
        this.totalCosts = 0;
        this.maxDrawdown = 0;
        this.maxCapital = initialCapital;
    }
    
    /**
     * Load intraday candle data (5-min or 15-min)
     */
    public void loadIntradayData(List<Candle> data) {
        this.intradayData = data;
    }
    
    /**
     * Run backtest on intraday data
     */
    public IntradayBacktestResult runBacktest() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 INTRADAY BACKTESTING: " + strategy.getName());
        System.out.println("=".repeat(80));
        System.out.println("Description: " + strategy.getDescription());
        System.out.println("Initial Capital: ₹" + String.format("%.2f", initialCapital));
        System.out.println("Quantity per trade: " + quantity);
        System.out.println("Candles: " + intradayData.size());
        System.out.println();
        
        resetMetrics();
        trades.clear();
        dailyStats.clear();
        currentTrade = null;
        currentCapital = initialCapital;
        
        String currentDay = null;
        DayStats dayStats = null;
        
        for (int i = 0; i < intradayData.size(); i++) {
            Candle candle = intradayData.get(i);
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd");
            String day = dayFormat.format(candle.timestamp);
            
            // Track daily stats
            if (!day.equals(currentDay)) {
                currentDay = day;
                dayStats = new DayStats(day);
                dailyStats.put(day, dayStats);
                
                // Close any open position at day end (shouldn't happen with auto square-off)
                if (currentTrade != null) {
                    closePosition(candle, "EOD Close", i);
                }
            }
            
            // Check if we should exit current position
            if (currentTrade != null) {
                if (strategy.shouldExit(intradayData, i, currentTrade, candle.timestamp)) {
                    String exitReason = getExitReason(candle, currentTrade);
                    closePosition(candle, exitReason, i);
                    if (dayStats != null) dayStats.trades++;
                }
            }
            
            // Generate signal for new position (only one trade at a time)
            if (currentTrade == null) {
                int signal = strategy.generateSignal(intradayData, i, candle.timestamp);
                
                if (signal == 1) { // BUY
                    openPosition(IntradayTrade.Type.LONG, candle, "Buy signal", i);
                } else if (signal == -1) { // SELL/SHORT
                    openPosition(IntradayTrade.Type.SHORT, candle, "Sell signal", i);
                }
            }
        }
        
        // Close any remaining open position
        if (currentTrade != null) {
            closePosition(intradayData.get(intradayData.size() - 1), "End of backtest", intradayData.size() - 1);
        }
        
        return generateReport();
    }
    
    /**
     * Open a new intraday position
     */
    private void openPosition(IntradayTrade.Type type, Candle candle, String signal, int index) {
        double entryPrice = candle.close;
        
        // Check if we have enough capital
        double requiredCapital = entryPrice * quantity;
        if (requiredCapital > currentCapital) {
            return; // Not enough capital
        }
        
        // Calculate stop loss and target
        double stopLoss, target;
        if (type == IntradayTrade.Type.LONG) {
            stopLoss = entryPrice * 0.995; // 0.5% SL
            target = entryPrice * 1.01; // 1% target
        } else {
            stopLoss = entryPrice * 1.005; // 0.5% SL
            target = entryPrice * 0.99; // 1% target
        }
        
        currentTrade = new IntradayTrade(type, candle.timestamp, entryPrice, 
                                         quantity, signal, stopLoss, target);
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("📈 OPEN  " + currentTrade.type + " | " + 
            String.format("%.2f @ %s | SL: %.2f | TGT: %.2f", 
            entryPrice, sdf.format(candle.timestamp), stopLoss, target));
    }
    
    /**
     * Close current position
     */
    private void closePosition(Candle candle, String reason, int index) {
        double exitPrice = candle.close;
        currentTrade.close(candle.timestamp, exitPrice, reason);
        
        // Calculate transaction costs
        double costs = calculateTransactionCosts(currentTrade);
        currentTrade.pnl -= costs;
        totalCosts += costs;
        
        trades.add(currentTrade);
        
        // Update capital
        currentCapital += currentTrade.pnl;
        
        // Update metrics
        totalTrades++;
        if (currentTrade.pnl > 0) {
            winningTrades++;
            totalProfit += currentTrade.pnl;
        } else if (currentTrade.pnl < 0) {
            losingTrades++;
            totalLoss += Math.abs(currentTrade.pnl);
        }
        
        if (currentTrade.hitTarget) targetHits++;
        if (currentTrade.hitStopLoss) stopLossHits++;
        if (currentTrade.autoSquareOff) autoSquareOffs++;
        
        // Track max capital and drawdown
        if (currentCapital > maxCapital) {
            maxCapital = currentCapital;
        }
        double drawdown = (maxCapital - currentCapital) / maxCapital * 100;
        if (drawdown > maxDrawdown) {
            maxDrawdown = drawdown;
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("📉 CLOSE " + currentTrade.type + " | " + 
            String.format("%.2f @ %s | P&L: %.2f | Costs: %.2f | %s", 
            exitPrice, sdf.format(candle.timestamp), currentTrade.pnl, costs, reason));
        
        currentTrade = null;
    }
    
    /**
     * Determine exit reason
     */
    private String getExitReason(Candle candle, IntradayTrade trade) {
        double currentPrice = candle.close;
        
        if (trade.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= trade.target) return "Target Hit";
            if (currentPrice <= trade.stopLoss) return "Stop Loss";
        } else {
            if (currentPrice <= trade.target) return "Target Hit";
            if (currentPrice >= trade.stopLoss) return "Stop Loss";
        }
        
        if (strategy.mustSquareOff(candle.timestamp)) {
            return "Auto Square-off";
        }
        
        return "Exit Signal";
    }
    
    /**
     * Calculate realistic transaction costs
     */
    private double calculateTransactionCosts(IntradayTrade trade) {
        double tradeValue = trade.entryPrice * trade.quantity;
        
        // Brokerage: ₹20 or 0.03%, whichever is lower (both buy and sell)
        double brokerage = Math.min(20, tradeValue * 0.0003) * 2;
        
        // STT: 0.025% on sell side only
        double stt = tradeValue * 0.00025;
        
        // Transaction charges: 0.00297%
        double transactionCharges = tradeValue * 0.0000297 * 2;
        
        // GST: 18% on brokerage + transaction charges
        double gst = (brokerage + transactionCharges) * 0.18;
        
        // SEBI charges: ₹10 per crore (negligible)
        double sebi = tradeValue * 0.0000001;
        
        // Stamp duty: 0.003% on buy side
        double stampDuty = tradeValue * 0.00003;
        
        // Auto square-off penalty (if applicable)
        double penalty = trade.autoSquareOff ? AUTO_SQUARE_OFF_PENALTY : 0;
        
        return brokerage + stt + transactionCharges + gst + sebi + stampDuty + penalty;
    }
    
    /**
     * Generate backtest report
     */
    private IntradayBacktestResult generateReport() {
        IntradayBacktestResult result = new IntradayBacktestResult();
        
        result.strategyName = strategy.getName();
        result.strategyDescription = strategy.getDescription();
        result.initialCapital = initialCapital;
        result.finalCapital = currentCapital;
        result.totalReturn = ((currentCapital - initialCapital) / initialCapital) * 100;
        result.totalTrades = totalTrades;
        result.winningTrades = winningTrades;
        result.losingTrades = losingTrades;
        result.winRate = totalTrades > 0 ? (double) winningTrades / totalTrades * 100 : 0;
        result.totalProfit = totalProfit;
        result.totalLoss = totalLoss;
        result.profitFactor = totalLoss > 0 ? totalProfit / totalLoss : 0;
        result.maxDrawdown = maxDrawdown;
        result.totalCosts = totalCosts;
        result.targetHits = targetHits;
        result.stopLossHits = stopLossHits;
        result.autoSquareOffs = autoSquareOffs;
        result.trades = new ArrayList<>(trades);
        result.dailyStats = new LinkedHashMap<>(dailyStats);
        
        // Calculate additional metrics
        if (trades.size() > 0) {
            result.averageWin = winningTrades > 0 ? totalProfit / winningTrades : 0;
            result.averageLoss = losingTrades > 0 ? totalLoss / losingTrades : 0;
            result.largestWin = trades.stream()
                .filter(t -> t.pnl > 0)
                .mapToDouble(t -> t.pnl)
                .max()
                .orElse(0);
            result.largestLoss = trades.stream()
                .filter(t -> t.pnl < 0)
                .mapToDouble(t -> Math.abs(t.pnl))
                .max()
                .orElse(0);
            
            // Average trade duration
            long totalDuration = 0;
            for (IntradayTrade t : trades) {
                if (t.exitTime != null) {
                    totalDuration += (t.exitTime.getTime() - t.entryTime.getTime());
                }
            }
            result.avgTradeDurationMinutes = (totalDuration / trades.size()) / (1000 * 60);
        }
        
        // Calculate daily metrics
        int profitableDays = 0;
        for (DayStats ds : dailyStats.values()) {
            if (ds.pnl > 0) profitableDays++;
        }
        result.profitableDays = profitableDays;
        result.totalDays = dailyStats.size();
        
        return result;
    }
    
    /**
     * Print detailed report
     */
    public void printReport(IntradayBacktestResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 INTRADAY BACKTEST RESULTS");
        System.out.println("=".repeat(80));
        
        System.out.println("\n💼 CAPITAL & RETURNS");
        System.out.println("   Initial Capital:    ₹" + String.format("%,.2f", result.initialCapital));
        System.out.println("   Final Capital:      ₹" + String.format("%,.2f", result.finalCapital));
        System.out.println("   Net P&L:            ₹" + String.format("%,.2f", result.finalCapital - result.initialCapital));
        System.out.println("   Total Return:       " + String.format("%.2f%%", result.totalReturn));
        
        System.out.println("\n📈 TRADE STATISTICS");
        System.out.println("   Total Trades:       " + result.totalTrades);
        System.out.println("   Winning Trades:     " + result.winningTrades);
        System.out.println("   Losing Trades:      " + result.losingTrades);
        System.out.println("   Win Rate:           " + String.format("%.2f%%", result.winRate));
        System.out.println("   Target Hits:        " + result.targetHits);
        System.out.println("   Stop Loss Hits:     " + result.stopLossHits);
        System.out.println("   Auto Square-offs:   " + result.autoSquareOffs + 
            (result.autoSquareOffs > 0 ? " ⚠️" : " ✅"));
        
        System.out.println("\n💰 PROFIT & LOSS");
        System.out.println("   Total Profit:       ₹" + String.format("%,.2f", result.totalProfit));
        System.out.println("   Total Loss:         ₹" + String.format("%,.2f", result.totalLoss));
        System.out.println("   Profit Factor:      " + String.format("%.2f", result.profitFactor));
        System.out.println("   Average Win:        ₹" + String.format("%,.2f", result.averageWin));
        System.out.println("   Average Loss:       ₹" + String.format("%,.2f", result.averageLoss));
        System.out.println("   Largest Win:        ₹" + String.format("%,.2f", result.largestWin));
        System.out.println("   Largest Loss:       ₹" + String.format("%,.2f", result.largestLoss));
        
        System.out.println("\n💸 COST ANALYSIS");
        System.out.println("   Total Costs:        ₹" + String.format("%,.2f", result.totalCosts));
        System.out.println("   Avg Cost/Trade:     ₹" + String.format("%.2f", result.totalCosts / result.totalTrades));
        System.out.println("   Costs as % of P&L:  " + 
            String.format("%.2f%%", (result.totalCosts / Math.abs(result.finalCapital - result.initialCapital)) * 100));
        
        System.out.println("\n📉 RISK METRICS");
        System.out.println("   Max Drawdown:       " + String.format("%.2f%%", result.maxDrawdown));
        System.out.println("   Avg Trade Duration: " + String.format("%.0f minutes", result.avgTradeDurationMinutes));
        
        System.out.println("\n📅 DAILY PERFORMANCE");
        System.out.println("   Total Trading Days: " + result.totalDays);
        System.out.println("   Profitable Days:    " + result.profitableDays + 
            " (" + String.format("%.1f%%", (double)result.profitableDays/result.totalDays*100) + ")");
        
        System.out.println("\n" + "=".repeat(80));
    }
}

/**
 * Daily statistics
 */
class DayStats {
    public String date;
    public int trades = 0;
    public double pnl = 0;
    
    public DayStats(String date) {
        this.date = date;
    }
}

/**
 * Intraday backtest result
 */
class IntradayBacktestResult {
    public String strategyName;
    public String strategyDescription;
    public double initialCapital;
    public double finalCapital;
    public double totalReturn;
    public int totalTrades;
    public int winningTrades;
    public int losingTrades;
    public double winRate;
    public double totalProfit;
    public double totalLoss;
    public double profitFactor;
    public double averageWin;
    public double averageLoss;
    public double largestWin;
    public double largestLoss;
    public double maxDrawdown;
    public double totalCosts;
    public int targetHits;
    public int stopLossHits;
    public int autoSquareOffs;
    public double avgTradeDurationMinutes;
    public int profitableDays;
    public int totalDays;
    public List<IntradayTrade> trades;
    public Map<String, DayStats> dailyStats;
}
