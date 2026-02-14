package kite;

import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Backtesting Engine
 * Runs trading strategies on historical data and generates performance metrics
 */
public class BacktestEngine {
    
    private TradingStrategy strategy;
    private List<Candle> historicalData;
    private List<Trade> trades;
    private double initialCapital;
    private double currentCapital;
    private int quantity;
    private Trade currentTrade;
    private boolean includeCosts;
    private TransactionCostCalculator.TradeType tradeType;
    
    // Performance metrics
    private double totalCosts;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private double totalProfit;
    private double totalLoss;
    private double maxDrawdown;
    private double maxCapital;
    
    public BacktestEngine(TradingStrategy strategy, double initialCapital, int quantity) {
        this(strategy, initialCapital, quantity, false, TransactionCostCalculator.TradeType.EQUITY_DELIVERY);
    }
    
    public BacktestEngine(TradingStrategy strategy, double initialCapital, int quantity, 
                         boolean includeCosts, TransactionCostCalculator.TradeType tradeType) {
        this.strategy = strategy;
        this.initialCapital = initialCapital;
        this.currentCapital = initialCapital;
        this.quantity = quantity;
        this.trades = new ArrayList<>();
        this.currentTrade = null;
        this.includeCosts = includeCosts;
        this.tradeType = tradeType;
        
        resetMetrics();
    }
    
    private void resetMetrics() {
        this.totalTrades = 0;
        this.winningTrades = 0;
        this.losingTrades = 0;
        this.totalProfit = 0;
        this.totalLoss = 0;
        this.maxDrawdown = 0;
        this.maxCapital = initialCapital;
        this.totalCosts = 0;
    }
    
    /**
     * Load historical data
     */
    public void loadHistoricalData(List<Candle> data) {
        this.historicalData = data;
    }
    
    /**
     * Run backtest
     */
    public BacktestResult runBacktest() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("🔬 BACKTESTING: " + strategy.getName());
        System.out.println("=".repeat(80));
        System.out.println("Description: " + strategy.getDescription());
        System.out.println("Initial Capital: ₹" + String.format("%.2f", initialCapital));
        System.out.println("Quantity per trade: " + quantity);
        System.out.println("Data points: " + historicalData.size());
        System.out.println();
        
        resetMetrics();
        trades.clear();
        currentTrade = null;
        currentCapital = initialCapital;
        
        for (int i = 0; i < historicalData.size(); i++) {
            Candle currentCandle = historicalData.get(i);
            
            // Check if we should exit current position
            if (currentTrade != null && strategy.shouldExit(historicalData, i, currentTrade)) {
                closePosition(currentCandle, "Exit signal");
            }
            
            // Generate signal for new position
            if (currentTrade == null) {
                int signal = strategy.generateSignal(historicalData, i);
                
                if (signal == 1) { // BUY
                    openPosition(Trade.Type.LONG, currentCandle, "Buy signal");
                } else if (signal == -1) { // SELL/SHORT
                    openPosition(Trade.Type.SHORT, currentCandle, "Sell signal");
                }
            }
        }
        
        // Close any open position at the end
        if (currentTrade != null) {
            closePosition(historicalData.get(historicalData.size() - 1), "End of backtest");
        }
        
        return generateReport();
    }
    
    /**
     * Open a new position
     */
    private void openPosition(Trade.Type type, Candle candle, String signal) {
        // Check if we have enough capital
        double requiredCapital = candle.close * quantity;
        if (requiredCapital > currentCapital) {
            return; // Not enough capital
        }
        
        currentTrade = new Trade(type, candle.timestamp, candle.close, quantity, signal);
        System.out.println("📈 OPEN  " + currentTrade.type + " | " + 
            String.format("%.2f @ %s", candle.close, 
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(candle.timestamp)));
    }
    
    /**
     * Close current position
     */
    private void closePosition(Candle candle, String reason) {
        currentTrade.close(candle.timestamp, candle.close);
        
        // Calculate transaction costs
        double costs = 0;
        if (includeCosts) {
            double tradeValue = currentTrade.entryPrice * currentTrade.quantity;
            costs = TransactionCostCalculator.calculateRoundTripCost(tradeValue, tradeType);
            currentTrade.pnl -= costs; // Deduct costs from P&L
            totalCosts += costs;
        }
        
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
        
        // Track max capital and drawdown
        if (currentCapital > maxCapital) {
            maxCapital = currentCapital;
        }
        double drawdown = (maxCapital - currentCapital) / maxCapital * 100;
        if (drawdown > maxDrawdown) {
            maxDrawdown = drawdown;
        }
        
        String costStr = includeCosts ? String.format(" | Costs: %.2f", costs) : "";
        System.out.println("📉 CLOSE " + currentTrade.type + " | " + 
            String.format("%.2f @ %s | P&L: %.2f%s | Reason: %s", 
            candle.close, 
            new SimpleDateFormat("yyyy-MM-dd HH:mm").format(candle.timestamp),
            currentTrade.pnl,
            costStr,
            reason));
        
        currentTrade = null;
    }
    
    /**
     * Generate backtest report
     */
    private BacktestResult generateReport() {
        BacktestResult result = new BacktestResult();
        
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
        result.trades = new ArrayList<>(trades);
        
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
        }
        
        return result;
    }
    
    /**
     * Print detailed report
     */
    public void printReport(BacktestResult result) {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 BACKTEST RESULTS");
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
        if (result.totalCosts > 0) {
            System.out.println("   Total Costs:        ₹" + String.format("%,.2f", result.totalCosts));
            System.out.println("   Avg Cost/Trade:     ₹" + String.format("%.2f", result.totalCosts / result.totalTrades));
        }
        
        System.out.println("\n💰 PROFIT & LOSS");
        System.out.println("   Total Profit:       ₹" + String.format("%,.2f", result.totalProfit));
        System.out.println("   Total Loss:         ₹" + String.format("%,.2f", result.totalLoss));
        System.out.println("   Profit Factor:      " + String.format("%.2f", result.profitFactor));
        System.out.println("   Average Win:        ₹" + String.format("%,.2f", result.averageWin));
        System.out.println("   Average Loss:       ₹" + String.format("%,.2f", result.averageLoss));
        System.out.println("   Largest Win:        ₹" + String.format("%,.2f", result.largestWin));
        System.out.println("   Largest Loss:       ₹" + String.format("%,.2f", result.largestLoss));
        
        System.out.println("\n📉 RISK METRICS");
        System.out.println("   Max Drawdown:       " + String.format("%.2f%%", result.maxDrawdown));
        
        // Print recent trades
        System.out.println("\n📋 RECENT TRADES (Last 10)");
        int start = Math.max(0, result.trades.size() - 10);
        for (int i = start; i < result.trades.size(); i++) {
            Trade t = result.trades.get(i);
            System.out.println("   " + (i + 1) + ". " + t);
        }
        
        System.out.println("\n" + "=".repeat(80));
    }
}

/**
 * Backtest Result data structure
 */
class BacktestResult {
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
    public List<Trade> trades;
    
    /**
     * Export to JSON
     */
    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("strategy_name", strategyName);
        json.put("strategy_description", strategyDescription);
        json.put("initial_capital", initialCapital);
        json.put("final_capital", finalCapital);
        json.put("total_return", totalReturn);
        json.put("total_trades", totalTrades);
        json.put("winning_trades", winningTrades);
        json.put("losing_trades", losingTrades);
        json.put("win_rate", winRate);
        json.put("total_profit", totalProfit);
        json.put("total_loss", totalLoss);
        json.put("profit_factor", profitFactor);
        json.put("average_win", averageWin);
        json.put("average_loss", averageLoss);
        json.put("largest_win", largestWin);
        json.put("largest_loss", largestLoss);
        json.put("max_drawdown", maxDrawdown);
        
        return json;
    }
    
    /**
     * Export to CSV
     */
    public String toCSV() {
        StringBuilder csv = new StringBuilder();
        csv.append("Metric,Value\n");
        csv.append("Strategy Name,").append(strategyName).append("\n");
        csv.append("Initial Capital,").append(initialCapital).append("\n");
        csv.append("Final Capital,").append(finalCapital).append("\n");
        csv.append("Total Return %,").append(String.format("%.2f", totalReturn)).append("\n");
        csv.append("Total Trades,").append(totalTrades).append("\n");
        csv.append("Winning Trades,").append(winningTrades).append("\n");
        csv.append("Losing Trades,").append(losingTrades).append("\n");
        csv.append("Win Rate %,").append(String.format("%.2f", winRate)).append("\n");
        csv.append("Total Profit,").append(totalProfit).append("\n");
        csv.append("Total Loss,").append(totalLoss).append("\n");
        csv.append("Profit Factor,").append(String.format("%.2f", profitFactor)).append("\n");
        csv.append("Max Drawdown %,").append(String.format("%.2f", maxDrawdown)).append("\n");
        
        return csv.toString();
    }
}
