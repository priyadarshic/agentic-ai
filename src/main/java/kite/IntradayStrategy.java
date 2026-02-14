package kite;

import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Intraday Trading Strategy Interface
 * Specialized for day trading with auto square-off
 */
public interface IntradayStrategy {
    
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
     * @param candles Intraday candle data (5min/15min)
     * @param currentIndex Current candle index
     * @param currentTime Current market time
     * @return Signal: 1 (BUY), -1 (SELL/SHORT), 0 (HOLD)
     */
    int generateSignal(List<Candle> candles, int currentIndex, Date currentTime);
    
    /**
     * Check if position should be exited
     */
    boolean shouldExit(List<Candle> candles, int currentIndex, IntradayTrade currentTrade, Date currentTime);
    
    /**
     * Get optimal entry time (avoid first 15 mins)
     */
    default boolean isValidEntryTime(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        // Market: 9:15 AM to 3:30 PM
        // Entry window: 9:30 AM to 2:30 PM (avoid first/last hour)
        if (hour == 9 && minute < 30) return false; // First 15 mins
        if (hour >= 14 && minute >= 30) return false; // After 2:30 PM
        if (hour >= 15) return false; // After 3:00 PM
        
        return true;
    }
    
    /**
     * Check if we need to square off (before 3:15 PM to avoid charges)
     */
    default boolean mustSquareOff(Date time) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(time);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        // Square off by 3:15 PM to avoid auto square-off penalty
        return (hour == 15 && minute >= 15) || hour > 15;
    }
}

/**
 * Intraday Trade Record
 */
class IntradayTrade {
    public enum Type { LONG, SHORT }
    
    public Type type;
    public Date entryTime;
    public double entryPrice;
    public Date exitTime;
    public double exitPrice;
    public int quantity;
    public double pnl;
    public String signal;
    public double stopLoss;
    public double target;
    public boolean hitStopLoss;
    public boolean hitTarget;
    public boolean autoSquareOff;
    
    public IntradayTrade(Type type, Date entryTime, double entryPrice, int quantity, 
                         String signal, double stopLoss, double target) {
        this.type = type;
        this.entryTime = entryTime;
        this.entryPrice = entryPrice;
        this.quantity = quantity;
        this.signal = signal;
        this.stopLoss = stopLoss;
        this.target = target;
        this.hitStopLoss = false;
        this.hitTarget = false;
        this.autoSquareOff = false;
    }
    
    public void close(Date exitTime, double exitPrice, String exitReason) {
        this.exitTime = exitTime;
        this.exitPrice = exitPrice;
        
        if (type == Type.LONG) {
            this.pnl = (exitPrice - entryPrice) * quantity;
        } else {
            this.pnl = (entryPrice - exitPrice) * quantity;
        }
        
        if (exitReason.contains("Stop Loss")) {
            this.hitStopLoss = true;
        } else if (exitReason.contains("Target")) {
            this.hitTarget = true;
        } else if (exitReason.contains("Auto Square")) {
            this.autoSquareOff = true;
        }
    }
    
    public boolean isOpen() {
        return exitTime == null;
    }
    
    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String status = isOpen() ? "OPEN" : "CLOSED";
        String exitType = hitTarget ? "TARGET" : hitStopLoss ? "SL" : autoSquareOff ? "AUTO-SQ" : "EXIT";
        
        return String.format("[%s] %s | Entry: %.2f @ %s | Exit: %.2f @ %s | P&L: %.2f | %s",
            status, type, entryPrice, sdf.format(entryTime), 
            exitPrice != 0 ? exitPrice : 0, exitTime != null ? sdf.format(exitTime) : "-", 
            pnl, exitType);
    }
}

/**
 * Opening Range Breakout Strategy
 * Trade breakouts from first 15-30 minutes range
 */
class OpeningRangeBreakout implements IntradayStrategy {
    
    private int rangePeriodMinutes = 15;
    private double stopLossPercent = 0.5; // 0.5%
    private double targetPercent = 1.0; // 1.0%
    
    @Override
    public String getName() {
        return "Opening Range Breakout";
    }
    
    @Override
    public String getDescription() {
        return String.format("ORB(%d min) - SL: %.1f%%, Target: %.1f%%", 
            rangePeriodMinutes, stopLossPercent, targetPercent);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("rangePeriod")) {
            this.rangePeriodMinutes = (Integer) parameters.get("rangePeriod");
        }
        if (parameters.containsKey("stopLoss")) {
            this.stopLossPercent = (Double) parameters.get("stopLoss");
        }
        if (parameters.containsKey("target")) {
            this.targetPercent = (Double) parameters.get("target");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex, Date currentTime) {
        if (!isValidEntryTime(currentTime)) {
            return 0;
        }
        
        // Find opening range (first 15 mins)
        int rangeCandles = rangePeriodMinutes / 5; // Assuming 5-min candles
        if (currentIndex < rangeCandles + 1) {
            return 0;
        }
        
        // Calculate opening range high/low
        double rangeHigh = Double.MIN_VALUE;
        double rangeLow = Double.MAX_VALUE;
        
        for (int i = 0; i < rangeCandles; i++) {
            rangeHigh = Math.max(rangeHigh, candles.get(i).high);
            rangeLow = Math.min(rangeLow, candles.get(i).low);
        }
        
        double currentPrice = candles.get(currentIndex).close;
        
        // Bullish breakout
        if (currentPrice > rangeHigh) {
            return 1;
        }
        
        // Bearish breakdown
        if (currentPrice < rangeLow) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, IntradayTrade trade, Date currentTime) {
        double currentPrice = candles.get(currentIndex).close;
        
        // Check target
        if (trade.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= trade.target) {
                return true;
            }
            if (currentPrice <= trade.stopLoss) {
                return true;
            }
        } else {
            if (currentPrice <= trade.target) {
                return true;
            }
            if (currentPrice >= trade.stopLoss) {
                return true;
            }
        }
        
        // Must square off before 3:15 PM
        if (mustSquareOff(currentTime)) {
            return true;
        }
        
        return false;
    }
}

/**
 * VWAP Strategy
 * Trade based on price vs VWAP
 */
class VWAPStrategy implements IntradayStrategy {
    
    private double stopLossPercent = 0.4;
    private double targetPercent = 0.8;
    
    @Override
    public String getName() {
        return "VWAP";
    }
    
    @Override
    public String getDescription() {
        return String.format("VWAP - SL: %.1f%%, Target: %.1f%%", 
            stopLossPercent, targetPercent);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("stopLoss")) {
            this.stopLossPercent = (Double) parameters.get("stopLoss");
        }
        if (parameters.containsKey("target")) {
            this.targetPercent = (Double) parameters.get("target");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex, Date currentTime) {
        if (!isValidEntryTime(currentTime)) {
            return 0;
        }
        
        if (currentIndex < 5) {
            return 0;
        }
        
        double vwap = calculateVWAP(candles, currentIndex);
        double currentPrice = candles.get(currentIndex).close;
        double prevPrice = candles.get(currentIndex - 1).close;
        
        // Buy when price crosses above VWAP
        if (prevPrice <= vwap && currentPrice > vwap) {
            return 1;
        }
        
        // Sell when price crosses below VWAP
        if (prevPrice >= vwap && currentPrice < vwap) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, IntradayTrade trade, Date currentTime) {
        double currentPrice = candles.get(currentIndex).close;
        
        // Target/SL check
        if (trade.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= trade.target || currentPrice <= trade.stopLoss) {
                return true;
            }
        } else {
            if (currentPrice <= trade.target || currentPrice >= trade.stopLoss) {
                return true;
            }
        }
        
        // Auto square off
        if (mustSquareOff(currentTime)) {
            return true;
        }
        
        return false;
    }
    
    private double calculateVWAP(List<Candle> candles, int endIndex) {
        double cumVolumePrice = 0;
        double cumVolume = 0;
        
        for (int i = 0; i <= endIndex; i++) {
            Candle c = candles.get(i);
            double typical = (c.high + c.low + c.close) / 3;
            cumVolumePrice += typical * c.volume;
            cumVolume += c.volume;
        }
        
        return cumVolume > 0 ? cumVolumePrice / cumVolume : 0;
    }
}

/**
 * Supertrend Strategy
 * Trend-following intraday strategy
 */
class SupertrendStrategy implements IntradayStrategy {
    
    private int period = 10;
    private double multiplier = 3.0;
    private double stopLossPercent = 0.5;
    private double targetPercent = 1.5;
    
    @Override
    public String getName() {
        return "Supertrend";
    }
    
    @Override
    public String getDescription() {
        return String.format("Supertrend(%d, %.1f) - SL: %.1f%%, Target: %.1f%%", 
            period, multiplier, stopLossPercent, targetPercent);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("period")) {
            this.period = (Integer) parameters.get("period");
        }
        if (parameters.containsKey("multiplier")) {
            this.multiplier = (Double) parameters.get("multiplier");
        }
        if (parameters.containsKey("stopLoss")) {
            this.stopLossPercent = (Double) parameters.get("stopLoss");
        }
        if (parameters.containsKey("target")) {
            this.targetPercent = (Double) parameters.get("target");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex, Date currentTime) {
        if (!isValidEntryTime(currentTime)) {
            return 0;
        }
        
        if (currentIndex < period + 1) {
            return 0;
        }
        
        boolean currentTrend = calculateSupertrend(candles, currentIndex);
        boolean prevTrend = calculateSupertrend(candles, currentIndex - 1);
        
        // Bullish: Supertrend turns green
        if (!prevTrend && currentTrend) {
            return 1;
        }
        
        // Bearish: Supertrend turns red
        if (prevTrend && !currentTrend) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, IntradayTrade trade, Date currentTime) {
        double currentPrice = candles.get(currentIndex).close;
        
        // Target/SL
        if (trade.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= trade.target || currentPrice <= trade.stopLoss) {
                return true;
            }
        } else {
            if (currentPrice <= trade.target || currentPrice >= trade.stopLoss) {
                return true;
            }
        }
        
        // Trend reversal
        boolean currentTrend = calculateSupertrend(candles, currentIndex);
        if (trade.type == IntradayTrade.Type.LONG && !currentTrend) {
            return true;
        }
        if (trade.type == IntradayTrade.Type.SHORT && currentTrend) {
            return true;
        }
        
        // Auto square off
        if (mustSquareOff(currentTime)) {
            return true;
        }
        
        return false;
    }
    
    private boolean calculateSupertrend(List<Candle> candles, int index) {
        double atr = calculateATR(candles, index);
        Candle c = candles.get(index);
        
        double hl2 = (c.high + c.low) / 2;
        double upperBand = hl2 + (multiplier * atr);
        double lowerBand = hl2 - (multiplier * atr);
        
        // Simplified: price above lower band = bullish
        return c.close > lowerBand;
    }
    
    private double calculateATR(List<Candle> candles, int index) {
        double sum = 0;
        for (int i = index - period + 1; i <= index; i++) {
            if (i < 0) continue;
            Candle c = candles.get(i);
            double tr = c.high - c.low;
            sum += tr;
        }
        return sum / period;
    }
}

/**
 * Scalping Strategy
 * Quick trades based on momentum
 */
class ScalpingStrategy implements IntradayStrategy {
    
    private int rsiPeriod = 14;
    private double stopLossPercent = 0.3;
    private double targetPercent = 0.6;
    
    @Override
    public String getName() {
        return "Scalping";
    }
    
    @Override
    public String getDescription() {
        return String.format("Scalping RSI(%d) - SL: %.1f%%, Target: %.1f%%", 
            rsiPeriod, stopLossPercent, targetPercent);
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        if (parameters.containsKey("rsiPeriod")) {
            this.rsiPeriod = (Integer) parameters.get("rsiPeriod");
        }
        if (parameters.containsKey("stopLoss")) {
            this.stopLossPercent = (Double) parameters.get("stopLoss");
        }
        if (parameters.containsKey("target")) {
            this.targetPercent = (Double) parameters.get("target");
        }
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex, Date currentTime) {
        if (!isValidEntryTime(currentTime)) {
            return 0;
        }
        
        if (currentIndex < rsiPeriod + 1) {
            return 0;
        }
        
        double rsi = calculateRSI(candles, currentIndex);
        double prevRSI = calculateRSI(candles, currentIndex - 1);
        
        // Oversold bounce
        if (prevRSI < 30 && rsi >= 30) {
            return 1;
        }
        
        // Overbought drop
        if (prevRSI > 70 && rsi <= 70) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, IntradayTrade trade, Date currentTime) {
        double currentPrice = candles.get(currentIndex).close;
        
        // Quick exit on target/SL
        if (trade.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= trade.target || currentPrice <= trade.stopLoss) {
                return true;
            }
        } else {
            if (currentPrice <= trade.target || currentPrice >= trade.stopLoss) {
                return true;
            }
        }
        
        // Auto square off
        if (mustSquareOff(currentTime)) {
            return true;
        }
        
        return false;
    }
    
    private double calculateRSI(List<Candle> candles, int endIndex) {
        double gainSum = 0;
        double lossSum = 0;
        
        for (int i = endIndex - rsiPeriod + 1; i <= endIndex; i++) {
            if (i <= 0) continue;
            double change = candles.get(i).close - candles.get(i - 1).close;
            if (change > 0) {
                gainSum += change;
            } else {
                lossSum += Math.abs(change);
            }
        }
        
        double avgGain = gainSum / rsiPeriod;
        double avgLoss = lossSum / rsiPeriod;
        
        if (avgLoss == 0) return 100;
        
        double rs = avgGain / avgLoss;
        return 100 - (100 / (1 + rs));
    }
}
