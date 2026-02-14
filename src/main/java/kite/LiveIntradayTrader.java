package kite;

import org.json.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * Live Intraday Trading Executor
 * Executes trades in real-time based on best backtested strategy
 */
public class LiveIntradayTrader {
    
    private KiteConnectAppWithServer kiteConnect;
    private IntradayStrategy strategy;
    private String tradingSymbol;
    private long instrumentToken;
    private int quantity;
    private double capital;
    
    // Risk management
    private double maxLossPerDay = 500; // ₹500 max loss per day
    private double maxProfitPerDay = 2000; // ₹2000 target per day
    private int maxTradesPerDay = 5; // Max 5 trades per day
    
    // State tracking
    private double todayPnL = 0;
    private int todayTrades = 0;
    private IntradayTrade currentPosition = null;
    private List<IntradayTrade> todayTradeHistory = new ArrayList<>();
    
    // Real-time data
    private List<Candle> todayCandles = new ArrayList<>();
    
    public LiveIntradayTrader(KiteConnectAppWithServer kiteConnect, 
                              IntradayStrategy strategy,
                              String tradingSymbol,
                              int quantity,
                              double capital) {
        this.kiteConnect = kiteConnect;
        this.strategy = strategy;
        this.tradingSymbol = tradingSymbol;
        this.quantity = quantity;
        this.capital = capital;
    }
    
    /**
     * Start live trading for the day
     */
    public void startLiveTrading() {
        try {
            System.out.println("\n" + "=".repeat(80));
            System.out.println("🚀 LIVE INTRADAY TRADING STARTED");
            System.out.println("=".repeat(80));
            System.out.println("Strategy: " + strategy.getName());
            System.out.println("Symbol: " + tradingSymbol);
            System.out.println("Quantity: " + quantity);
            System.out.println("Capital: ₹" + String.format("%,.2f", capital));
            System.out.println("Max Loss/Day: ₹" + maxLossPerDay);
            System.out.println("Target Profit/Day: ₹" + maxProfitPerDay);
            System.out.println("Max Trades/Day: " + maxTradesPerDay);
            System.out.println("=".repeat(80));
            
            // Get instrument token
            HistoricalDataFetcher fetcher = new HistoricalDataFetcher(
                    kiteConnect.getApiKey(),
                    kiteConnect.getAccessToken()
            );
            instrumentToken = fetcher.getInstrumentToken("NSE", tradingSymbol);
            System.out.println("\n✅ Instrument Token: " + instrumentToken);
            
            // Main trading loop (runs from 9:15 AM to 3:15 PM)
            while (isMarketOpen()) {
                // Fetch latest candle
                Candle latestCandle = fetchLatestCandle();
                if (latestCandle != null) {
                    todayCandles.add(latestCandle);
                }
                
                // Check if we should stop trading
                if (shouldStopTrading()) {
                    System.out.println("\n⚠️  Daily limit reached. Stopping trading.");
                    break;
                }
                
                // Manage existing position
                if (currentPosition != null) {
                    checkAndExitPosition(latestCandle);
                }
                
                // Look for new entry (if no position)
                if (currentPosition == null && todayTrades < maxTradesPerDay) {
                    checkAndEnterPosition(latestCandle);
                }
                
                // Display current status
                displayStatus();
                
                // Wait for next candle (5 minutes)
                Thread.sleep(5 * 60 * 1000); // 5 minutes
            }
            
            // End of day: Square off any open position
            if (currentPosition != null) {
                System.out.println("\n⏰ Market closing soon. Squaring off position...");
                squareOffPosition("EOD Square-off");
            }
            
            // Print day summary
            printDaySummary();
            
        } catch (Exception e) {
            System.err.println("\n❌ Error in live trading: " + e.getMessage());
            e.printStackTrace();
            
            // Emergency: Square off position
            if (currentPosition != null) {
                try {
                    squareOffPosition("Emergency Exit");
                } catch (Exception ex) {
                    System.err.println("Failed to square off: " + ex.getMessage());
                }
            }
        }
    }
    
    /**
     * Check if market is open
     */
    private boolean isMarketOpen() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // Weekend check
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            return false;
        }
        
        // Trading hours: 9:15 AM to 3:15 PM
        if (hour < 9 || (hour == 9 && minute < 15)) {
            return false;
        }
        if (hour > 15 || (hour == 15 && minute >= 15)) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Check if we should stop trading for the day
     */
    private boolean shouldStopTrading() {
        // Max loss reached
        if (todayPnL <= -maxLossPerDay) {
            System.out.println("🛑 Max loss limit reached: ₹" + todayPnL);
            return true;
        }
        
        // Target profit reached
        if (todayPnL >= maxProfitPerDay) {
            System.out.println("🎯 Target profit reached: ₹" + todayPnL);
            return true;
        }
        
        // Max trades reached
        if (todayTrades >= maxTradesPerDay) {
            System.out.println("🔢 Max trades limit reached: " + todayTrades);
            return true;
        }
        
        // Must square off before 3:15 PM
        Calendar cal = Calendar.getInstance();
        if (cal.get(Calendar.HOUR_OF_DAY) == 15 && cal.get(Calendar.MINUTE) >= 15) {
            return true;
        }
        
        return false;
    }
    
    /**
     * Fetch latest 5-minute candle
     */
    private Candle fetchLatestCandle() throws Exception {
        HistoricalDataFetcher fetcher = new HistoricalDataFetcher(
                kiteConnect.getApiKey(),
                kiteConnect.getAccessToken()
        );
        
        // Fetch last hour of 5-min data
        Calendar cal = Calendar.getInstance();
        Date toDate = cal.getTime();
        cal.add(Calendar.HOUR, -1);
        Date fromDate = cal.getTime();
        
        List<Candle> candles = fetcher.fetchHistoricalData(
            instrumentToken,
            HistoricalDataFetcher.Interval.FIVE_MINUTE,
            fromDate,
            toDate,
            false
        );
        
        if (candles.size() > 0) {
            return candles.get(candles.size() - 1); // Latest candle
        }
        
        return null;
    }
    
    /**
     * Check and enter new position
     */
    private void checkAndEnterPosition(Candle latestCandle) throws Exception {
        if (todayCandles.size() < 10) {
            return; // Need at least 10 candles
        }
        
        int signal = strategy.generateSignal(todayCandles, todayCandles.size() - 1, latestCandle.timestamp);
        
        if (signal == 1) {
            // BUY signal
            placeOrder("BUY", latestCandle.close);
        } else if (signal == -1) {
            // SELL signal (short)
            placeOrder("SELL", latestCandle.close);
        }
    }
    
    /**
     * Check and exit current position
     */
    private void checkAndExitPosition(Candle latestCandle) throws Exception {
        if (currentPosition == null) return;
        
        boolean shouldExit = strategy.shouldExit(
            todayCandles, 
            todayCandles.size() - 1, 
            currentPosition, 
            latestCandle.timestamp
        );
        
        if (shouldExit) {
            String exitReason = getExitReason(latestCandle);
            squareOffPosition(exitReason);
        }
    }
    
    /**
     * Place order on exchange
     */
    private void placeOrder(String transactionType, double price) throws Exception {
        System.out.println("\n📤 Placing " + transactionType + " order...");
        
        Map<String, String> orderParams = new HashMap<>();
        orderParams.put("variety", "regular");
        orderParams.put("exchange", "NSE");
        orderParams.put("tradingsymbol", tradingSymbol);
        orderParams.put("transaction_type", transactionType);
        orderParams.put("order_type", "LIMIT");
        orderParams.put("price", String.format("%.2f", price));
        orderParams.put("quantity", String.valueOf(quantity));
        orderParams.put("product", "MIS"); // Intraday
        orderParams.put("validity", "DAY");
        
        JSONObject response = kiteConnect.placeOrder(orderParams);
        String orderId = response.getString("order_id");
        
        System.out.println("✅ Order placed: " + orderId);
        
        // Create trade record
        IntradayTrade.Type type = transactionType.equals("BUY") ? 
            IntradayTrade.Type.LONG : IntradayTrade.Type.SHORT;
        
        double stopLoss = type == IntradayTrade.Type.LONG ? 
            price * 0.995 : price * 1.005;
        double target = type == IntradayTrade.Type.LONG ? 
            price * 1.01 : price * 0.99;
        
        currentPosition = new IntradayTrade(
            type, 
            new Date(), 
            price, 
            quantity, 
            strategy.getName(), 
            stopLoss, 
            target
        );
        
        todayTrades++;
        
        System.out.println("📊 Position opened: " + type + " @ ₹" + price);
        System.out.println("   Stop Loss: ₹" + String.format("%.2f", stopLoss));
        System.out.println("   Target: ₹" + String.format("%.2f", target));
    }
    
    /**
     * Square off current position
     */
    private void squareOffPosition(String reason) throws Exception {
        if (currentPosition == null) return;
        
        System.out.println("\n📥 Squaring off position...");
        
        // Place opposite order
        String transactionType = currentPosition.type == IntradayTrade.Type.LONG ? 
            "SELL" : "BUY";
        
        // Get current market price
        Candle latestCandle = fetchLatestCandle();
        double exitPrice = latestCandle.close;
        
        Map<String, String> orderParams = new HashMap<>();
        orderParams.put("variety", "regular");
        orderParams.put("exchange", "NSE");
        orderParams.put("tradingsymbol", tradingSymbol);
        orderParams.put("transaction_type", transactionType);
        orderParams.put("order_type", "MARKET");
        orderParams.put("quantity", String.valueOf(quantity));
        orderParams.put("product", "MIS");
        orderParams.put("validity", "DAY");
        
        JSONObject response = kiteConnect.placeOrder(orderParams);
        String orderId = response.getString("order_id");
        
        System.out.println("✅ Square-off order placed: " + orderId);
        
        // Close trade
        currentPosition.close(new Date(), exitPrice, reason);
        
        // Calculate actual P&L (considering costs)
        double costs = calculateTransactionCosts(currentPosition);
        currentPosition.pnl -= costs;
        
        todayPnL += currentPosition.pnl;
        todayTradeHistory.add(currentPosition);
        
        System.out.println("📊 Position closed: " + currentPosition.type);
        System.out.println("   Exit Price: ₹" + exitPrice);
        System.out.println("   Gross P&L: ₹" + String.format("%.2f", currentPosition.pnl + costs));
        System.out.println("   Costs: ₹" + String.format("%.2f", costs));
        System.out.println("   Net P&L: ₹" + String.format("%.2f", currentPosition.pnl));
        System.out.println("   Reason: " + reason);
        
        currentPosition = null;
    }
    
    /**
     * Get exit reason
     */
    private String getExitReason(Candle candle) {
        double currentPrice = candle.close;
        
        if (currentPosition.type == IntradayTrade.Type.LONG) {
            if (currentPrice >= currentPosition.target) return "Target Hit";
            if (currentPrice <= currentPosition.stopLoss) return "Stop Loss";
        } else {
            if (currentPrice <= currentPosition.target) return "Target Hit";
            if (currentPrice >= currentPosition.stopLoss) return "Stop Loss";
        }
        
        if (strategy.mustSquareOff(candle.timestamp)) {
            return "Auto Square-off";
        }
        
        return "Exit Signal";
    }
    
    /**
     * Calculate transaction costs
     */
    private double calculateTransactionCosts(IntradayTrade trade) {
        return TransactionCostCalculator.calculateRoundTripCost(
            trade.entryPrice * trade.quantity,
            TransactionCostCalculator.TradeType.EQUITY_INTRADAY
        );
    }
    
    /**
     * Display current status
     */
    private void displayStatus() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println("\n" + "-".repeat(80));
        System.out.println("⏰ " + sdf.format(new Date()));
        System.out.println("💰 Today's P&L: ₹" + String.format("%.2f", todayPnL));
        System.out.println("📊 Trades: " + todayTrades + "/" + maxTradesPerDay);
        
        if (currentPosition != null) {
            System.out.println("📈 Open Position: " + currentPosition.type + " @ ₹" + currentPosition.entryPrice);
        } else {
            System.out.println("💤 No open position");
        }
        System.out.println("-".repeat(80));
    }
    
    /**
     * Print day summary
     */
    private void printDaySummary() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("📊 END OF DAY SUMMARY");
        System.out.println("=".repeat(80));
        System.out.println("Total Trades: " + todayTrades);
        System.out.println("Total P&L: ₹" + String.format("%.2f", todayPnL));
        
        int wins = 0;
        int losses = 0;
        int targets = 0;
        int stopLosses = 0;
        
        for (IntradayTrade trade : todayTradeHistory) {
            if (trade.pnl > 0) wins++;
            else if (trade.pnl < 0) losses++;
            
            if (trade.hitTarget) targets++;
            if (trade.hitStopLoss) stopLosses++;
        }
        
        System.out.println("Winning Trades: " + wins);
        System.out.println("Losing Trades: " + losses);
        System.out.println("Win Rate: " + String.format("%.1f%%", (double)wins/todayTrades*100));
        System.out.println("Target Hits: " + targets);
        System.out.println("Stop Losses: " + stopLosses);
        System.out.println("=".repeat(80));
    }
    
    /**
     * Set risk parameters
     */
    public void setRiskParameters(double maxLoss, double maxProfit, int maxTrades) {
        this.maxLossPerDay = maxLoss;
        this.maxProfitPerDay = maxProfit;
        this.maxTradesPerDay = maxTrades;
    }
}
