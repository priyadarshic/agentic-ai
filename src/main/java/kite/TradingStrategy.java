package kite;

import org.json.*;
import java.util.*;

/**
 * Trading Strategy Interface
 * Base interface for all trading strategies
 */
public interface TradingStrategy {
    
    /**
     * Strategy name
     */
    String getName();
    
    /**
     * Strategy description
     */
    String getDescription();
    
    /**
     * Initialize strategy with parameters
     */
    void initialize(Map<String, Object> parameters);
    
    /**
     * Generate trading signal
     * @param candles Historical candle data
     * @param currentIndex Current candle index
     * @return Signal: 1 (BUY), -1 (SELL), 0 (HOLD)
     */
    int generateSignal(List<Candle> candles, int currentIndex);
    
    /**
     * Check if position should be exited
     */
    boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade);
}

/**
 * Candle data structure
 */
class Candle {
    public Date timestamp;
    public double open;
    public double high;
    public double low;
    public double close;
    public long volume;
    public long openInterest; // For F&O
    
    public Candle(Date timestamp, double open, double high, double low, double close, long volume) {
        this.timestamp = timestamp;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.openInterest = 0;
    }
    
    public Candle(JSONArray candleData) {
        // Parse from Kite API format: [timestamp, open, high, low, close, volume, oi]
        this.timestamp = new Date(candleData.getString(0));
        this.open = candleData.getDouble(1);
        this.high = candleData.getDouble(2);
        this.low = candleData.getDouble(3);
        this.close = candleData.getDouble(4);
        this.volume = candleData.getLong(5);
        if (candleData.length() > 6) {
            this.openInterest = candleData.getLong(6);
        }
    }
    
    @Override
    public String toString() {
        return String.format("%s | O:%.2f H:%.2f L:%.2f C:%.2f V:%d", 
            timestamp, open, high, low, close, volume);
    }
}

/**
 * Trade record
 */
class Trade {
    public enum Type { LONG, SHORT }
    
    public Type type;
    public Date entryTime;
    public double entryPrice;
    public Date exitTime;
    public double exitPrice;
    public int quantity;
    public double pnl;
    public String signal; // Entry signal description
    
    public Trade(Type type, Date entryTime, double entryPrice, int quantity, String signal) {
        this.type = type;
        this.entryTime = entryTime;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.signal = signal;
    }
    
    public void close(Date exitTime, double exitPrice) {
        this.exitTime = exitTime;
        this.exitPrice = exitPrice;
        
        if (type == Type.LONG) {
            this.pnl = (exitPrice - entryPrice) * quantity;
        } else {
            this.pnl = (entryPrice - exitPrice) * quantity;
        }
    }
    
    public boolean isOpen() {
        return exitTime == null;
    }
    
    @Override
    public String toString() {
        String status = isOpen() ? "OPEN" : "CLOSED";
        return String.format("[%s] %s | Entry: %.2f @ %s | Exit: %.2f @ %s | P&L: %.2f",
            status, type, entryPrice, entryTime, 
            exitPrice != 0 ? exitPrice : 0, exitTime, pnl);
    }
}

/**
 * Moving Average Crossover Strategy
 * Buy when fast MA crosses above slow MA
 * Sell when fast MA crosses below slow MA
 */
class MovingAverageCrossoverStrategy implements TradingStrategy {
    
    private int fastPeriod = 10;
    private int slowPeriod = 20;
    private String name = "Moving Average Crossover";
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return String.format("MA Crossover (%d, %d) - Buy when fast MA crosses above slow MA", 
            fastPeriod, slowPeriod);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("fastPeriod")) {
            this.fastPeriod = (Integer) parameters.get("fastPeriod");
        }
        if (parameters.containsKey("slowPeriod")) {
            this.slowPeriod = (Integer) parameters.get("slowPeriod");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        if (currentIndex < slowPeriod + 1) {
            return 0; // Not enough data
        }
        
        double currentFastMA = calculateSMA(candles, currentIndex, fastPeriod);
        double previousFastMA = calculateSMA(candles, currentIndex - 1, fastPeriod);
        double currentSlowMA = calculateSMA(candles, currentIndex, slowPeriod);
        double previousSlowMA = calculateSMA(candles, currentIndex - 1, slowPeriod);
        
        // Bullish crossover
        if (previousFastMA <= previousSlowMA && currentFastMA > currentSlowMA) {
            return 1; // BUY
        }
        
        // Bearish crossover
        if (previousFastMA >= previousSlowMA && currentFastMA < currentSlowMA) {
            return -1; // SELL
        }
        
        return 0; // HOLD
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        int signal = generateSignal(candles, currentIndex);
        
        // Exit long on sell signal
        if (currentTrade.type == Trade.Type.LONG && signal == -1) {
            return true;
        }
        
        // Exit short on buy signal
        if (currentTrade.type == Trade.Type.SHORT && signal == 1) {
            return true;
        }
        
        return false;
    }
    
    private double calculateSMA(List<Candle> candles, int endIndex, int period) {
        double sum = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).close;
        }
        return sum / period;
    }
}

/**
 * RSI Strategy
 * Buy when RSI < oversold (default 30)
 * Sell when RSI > overbought (default 70)
 */
class RSIStrategy implements TradingStrategy {
    
    private int period = 14;
    private double oversoldThreshold = 30;
    private double overboughtThreshold = 70;
    private String name = "RSI Strategy";
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return String.format("RSI(%d) - Buy < %.0f, Sell > %.0f", 
            period, oversoldThreshold, overboughtThreshold);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            this.period = (Integer) parameters.get("period");
        }
        if (parameters.containsKey("oversold")) {
            this.oversoldThreshold = (Double) parameters.get("oversold");
        }
        if (parameters.containsKey("overbought")) {
            this.overboughtThreshold = (Double) parameters.get("overbought");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        if (currentIndex < period + 1) {
            return 0;
        }
        
        double rsi = calculateRSI(candles, currentIndex);
        
        if (rsi < oversoldThreshold) {
            return 1; // BUY (oversold)
        } else if (rsi > overboughtThreshold) {
            return -1; // SELL (overbought)
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        double rsi = calculateRSI(candles, currentIndex);
        
        // Exit long when overbought
        if (currentTrade.type == Trade.Type.LONG && rsi > overboughtThreshold) {
            return true;
        }
        
        // Exit short when oversold
        if (currentTrade.type == Trade.Type.SHORT && rsi < oversoldThreshold) {
            return true;
        }
        
        return false;
    }
    
    private double calculateRSI(List<Candle> candles, int endIndex) {
        double gainSum = 0;
        double lossSum = 0;
        
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            double change = candles.get(i).close - candles.get(i - 1).close;
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }
        
        double avgGain = gainSum / period;
        double avgLoss = lossSum / period;
        
        if (avgLoss == 0) {
            return 100;
        }
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}

/**
 * Bollinger Bands Strategy
 * Buy when price touches lower band
 * Sell when price touches upper band
 */
class BollingerBandsStrategy implements TradingStrategy {
    
    private int period = 20;
    private double stdDevMultiplier = 2.0;
    private String name = "Bollinger Bands";
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return String.format("Bollinger Bands(%d, %.1f) - Mean reversion", 
            period, stdDevMultiplier);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            this.period = (Integer) parameters.get("period");
        }
        if (parameters.containsKey("stdDev")) {
            this.stdDevMultiplier = (Double) parameters.get("stdDev");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        if (currentIndex < period) {
            return 0;
        }
        
        double[] bands = calculateBollingerBands(candles, currentIndex);
        double lowerBand = bands[0];
        double upperBand = bands[2];
        double currentPrice = candles.get(currentIndex).close;
        
        // Buy when price touches or goes below lower band
        if (currentPrice <= lowerBand) {
            return 1;
        }
        
        // Sell when price touches or goes above upper band
        if (currentPrice >= upperBand) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        double[] bands = calculateBollingerBands(candles, currentIndex);
        double middleBand = bands[1];
        double currentPrice = candles.get(currentIndex).close;
        
        // Exit when price returns to middle band
        if (currentTrade.type == Trade.Type.LONG && currentPrice >= middleBand) {
            return true;
        }
        
        if (currentTrade.type == Trade.Type.SHORT && currentPrice <= middleBand) {
            return true;
        }
        
        return false;
    }
    
    private double[] calculateBollingerBands(List<Candle> candles, int endIndex) {
        // Calculate SMA
        double sum = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            sum += candles.get(i).close;
        }
        double sma = sum / period;
        
        // Calculate standard deviation
        double variance = 0;
        for (int i = endIndex - period + 1; i <= endIndex; i++) {
            double diff = candles.get(i).close - sma;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / period);
        
        double lowerBand = sma - (stdDevMultiplier * stdDev);
        double upperBand = sma + (stdDevMultiplier * stdDev);
        
        return new double[]{lowerBand, sma, upperBand};
    }
}

/**
 * MACD Strategy
 * Buy when MACD crosses above signal line
 * Sell when MACD crosses below signal line
 */
class MACDStrategy implements TradingStrategy {
    
    private int fastPeriod = 12;
    private int slowPeriod = 26;
    private int signalPeriod = 9;
    private String name = "MACD";
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return String.format("MACD(%d, %d, %d) - Trend following", 
            fastPeriod, slowPeriod, signalPeriod);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("fast")) {
            this.fastPeriod = (Integer) parameters.get("fast");
        }
        if (parameters.containsKey("slow")) {
            this.slowPeriod = (Integer) parameters.get("slow");
        }
        if (parameters.containsKey("signal")) {
            this.signalPeriod = (Integer) parameters.get("signal");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        if (currentIndex < slowPeriod + signalPeriod) {
            return 0;
        }
        
        double[] currentMACD = calculateMACD(candles, currentIndex);
        double[] previousMACD = calculateMACD(candles, currentIndex - 1);
        
        // Bullish crossover
        if (previousMACD[0] <= previousMACD[1] && currentMACD[0] > currentMACD[1]) {
            return 1;
        }
        
        // Bearish crossover
        if (previousMACD[0] >= previousMACD[1] && currentMACD[0] < currentMACD[1]) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        int signal = generateSignal(candles, currentIndex);
        
        if (currentTrade.type == Trade.Type.LONG && signal == -1) {
            return true;
        }
        
        if (currentTrade.type == Trade.Type.SHORT && signal == 1) {
            return true;
        }
        
        return false;
    }
    
    private double[] calculateMACD(List<Candle> candles, int endIndex) {
        double fastEMA = calculateEMA(candles, endIndex, fastPeriod);
        double slowEMA = calculateEMA(candles, endIndex, slowPeriod);
        double macdLine = fastEMA - slowEMA;
        
        // Calculate signal line (EMA of MACD)
        // Simplified: using SMA instead of EMA for signal line
        double signalLine = macdLine; // Placeholder - full implementation would calculate EMA
        
        return new double[]{macdLine, signalLine};
    }
    
    private double calculateEMA(List<Candle> candles, int endIndex, int period) {
        double multiplier = 2.0 / (period + 1);
        double ema = candles.get(endIndex - period + 1).close;
        
        for (int i = endIndex - period + 2; i <= endIndex; i++) {
            ema = (candles.get(i).close - ema) * multiplier + ema;
        }
        
        return ema;
    }
}

/**
 * Breakout Strategy
 * Buy when price breaks above recent high
 * Sell when price breaks below recent low
 */
class BreakoutStrategy implements TradingStrategy {
    
    private int lookbackPeriod = 20;
    private String name = "Breakout";
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return String.format("Breakout(%d) - Momentum trading", lookbackPeriod);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("lookback")) {
            this.lookbackPeriod = (Integer) parameters.get("lookback");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        if (currentIndex < lookbackPeriod) {
            return 0;
        }
        
        double highestHigh = Double.MIN_VALUE;
        double lowestLow = Double.MAX_VALUE;
        
        for (int i = currentIndex - lookbackPeriod; i < currentIndex; i++) {
            highestHigh = Math.max(highestHigh, candles.get(i).high);
            lowestLow = Math.min(lowestLow, candles.get(i).low);
        }
        
        double currentPrice = candles.get(currentIndex).close;
        
        // Bullish breakout
        if (currentPrice > highestHigh) {
            return 1;
        }
        
        // Bearish breakout
        if (currentPrice < lowestLow) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        // Exit on opposite signal
        int signal = generateSignal(candles, currentIndex);
        
        if (currentTrade.type == Trade.Type.LONG && signal == -1) {
            return true;
        }
        
        if (currentTrade.type == Trade.Type.SHORT && signal == 1) {
            return true;
        }
        
        return false;
    }
}
