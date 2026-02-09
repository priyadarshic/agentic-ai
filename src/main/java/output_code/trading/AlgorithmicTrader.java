package output_code.trading;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AlgorithmicTrader {

    // Financial precision context
    private static final MathContext MC = MathContext.DECIMAL64; // Provides 16 digits of precision
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private static final int DISPLAY_SCALE = 2; // For displaying monetary values

    private static final BigDecimal INITIAL_CASH = new BigDecimal("10000.00", MC);
    private static final int TRADE_QUANTITY = 100; // Number of shares to buy/sell per trade
    private static final int SHORT_SMA_PERIOD = 5;
    private static final int LONG_SMA_PERIOD = 20;
    private static final long MARKET_DATA_INTERVAL_MS = 1000; // Simulate new price every 1 second
    private static final long SIMULATION_DURATION_SECONDS = 60; // Duration of the simulation

    private BigDecimal currentCash = INITIAL_CASH;
    private int sharesOwned = 0;
    private boolean positionOpen = false; // To prevent multiple buys without selling

    private Deque<BigDecimal> priceHistory = new LinkedList<>();
    private Random random = new Random();
    private ScheduledExecutorService marketDataFeed;

    public static void main(String[] args) {
        AlgorithmicTrader trader = new AlgorithmicTrader();
        trader.startTrading();
    }

    public void startTrading() {
        System.out.println("Starting Algorithmic Trading Simulation...");
        System.out.println("Initial Portfolio: Cash = $" + formatMoney(currentCash) + ", Shares = " + sharesOwned);

        marketDataFeed = Executors.newSingleThreadScheduledExecutor();
        marketDataFeed.scheduleAtFixedRate(this::processMarketData, 0, MARKET_DATA_INTERVAL_MS, TimeUnit.MILLISECONDS);

        // Schedule to stop trading after a defined duration
        marketDataFeed.schedule(this::stopTrading, SIMULATION_DURATION_SECONDS, TimeUnit.SECONDS);
    }

    private void processMarketData() {
        // Simulate real-time market data (e.g., stock price ticks)
        BigDecimal currentPrice = generateSimulatedPrice();
        priceHistory.addFirst(currentPrice); // Add new price to the front

        // Keep price history limited to the longest SMA period
        while (priceHistory.size() > LONG_SMA_PERIOD) {
            priceHistory.removeLast();
        }

        System.out.println("\n--- New Market Data ---");
        System.out.println("Current Price: $" + formatMoney(currentPrice));

        if (priceHistory.size() >= LONG_SMA_PERIOD) {
            executeStrategy(currentPrice);
        } else {
            System.out.println("Collecting enough data for SMA calculation (" + priceHistory.size() + "/" + LONG_SMA_PERIOD + " prices collected)...");
        }

        BigDecimal portfolioValue = currentCash.add(currentPrice.multiply(new BigDecimal(sharesOwned), MC), MC);
        System.out.println("Current Portfolio: Cash = $" + formatMoney(currentCash) +
                ", Shares = " + sharesOwned +
                ", Total Value = $" + formatMoney(portfolioValue));
    }

    private BigDecimal generateSimulatedPrice() {
        // Simple simulation: price fluctuates around a base value
        BigDecimal basePrice = new BigDecimal("100.00", MC);
        BigDecimal volatility = new BigDecimal("2.00", MC);
        // Add random fluctuation: (random.nextDouble() - 0.5) * volatility * 2
        BigDecimal fluctuation = new BigDecimal(random.nextDouble() - 0.5, MC)
                .multiply(volatility, MC)
                .multiply(new BigDecimal("2.0", MC), MC);
        return basePrice.add(fluctuation, MC);
    }

    /**
     * Calculates the Simple Moving Average (SMA) for a given period.
     * Iterates directly over the Deque's elements for efficiency.
     *
     * @param period The number of data points to include in the SMA calculation.
     * @return The calculated SMA as a BigDecimal.
     * @throws IllegalStateException if there is not enough data in the price history for the specified period.
     */
    private BigDecimal calculateSMA(int period) {
        if (priceHistory.size() < period) {
            // This case should ideally be prevented by the caller (executeStrategy)
            // but provides a clearer error than returning 0.0 or BigDecimal.ZERO.
            throw new IllegalStateException("Not enough data to calculate SMA for period: " + period + ". Required: " + period + ", Available: " + priceHistory.size());
        }
        BigDecimal sum = BigDecimal.ZERO;
        int count = 0;
        Iterator<BigDecimal> it = priceHistory.iterator();
        while (it.hasNext() && count < period) {
            sum = sum.add(it.next(), MC);
            count++;
        }
        return sum.divide(new BigDecimal(period), MC);
    }

    private void executeStrategy(BigDecimal currentPrice) {
        BigDecimal shortSMA = calculateSMA(SHORT_SMA_PERIOD);
        BigDecimal longSMA = calculateSMA(LONG_SMA_PERIOD);

        System.out.println("SMA(" + SHORT_SMA_PERIOD + "): $" + formatMoney(shortSMA) +
                ", SMA(" + LONG_SMA_PERIOD + "): $" + formatMoney(longSMA));

        // Buy signal: Short SMA crosses above Long SMA and no position is open
        if (shortSMA.compareTo(longSMA) > 0 && !positionOpen) {
            BigDecimal costEstimate = currentPrice.multiply(new BigDecimal(TRADE_QUANTITY), MC);
            if (currentCash.compareTo(costEstimate) >= 0) {
                buy(currentPrice, TRADE_QUANTITY);
                positionOpen = true;
            } else {
                System.out.println("BUY signal, but not enough cash. Current cash: $" + formatMoney(currentCash));
            }
        }
        // Sell signal: Short SMA crosses below Long SMA and a position is open
        else if (shortSMA.compareTo(longSMA) < 0 && positionOpen) {
            if (sharesOwned >= TRADE_QUANTITY) {
                sell(currentPrice, TRADE_QUANTITY);
                positionOpen = false;
            } else {
                System.out.println("SELL signal, but not enough shares. Current shares: " + sharesOwned);
            }
        } else {
            System.out.println("HOLD (No trade signal or conditions not met).");
        }
    }

    private void buy(BigDecimal price, int quantity) {
        BigDecimal cost = price.multiply(new BigDecimal(quantity), MC);
        currentCash = currentCash.subtract(cost, MC);
        sharesOwned += quantity;
        System.out.println("--- BUY Order Executed: " + quantity + " shares at $" + formatMoney(price) + " (Cost: $" + formatMoney(cost) + ")");
    }

    private void sell(BigDecimal price, int quantity) {
        BigDecimal revenue = price.multiply(new BigDecimal(quantity), MC);
        currentCash = currentCash.add(revenue, MC);
        sharesOwned -= quantity;
        System.out.println("--- SELL Order Executed: " + quantity + " shares at $" + formatMoney(price) + " (Revenue: $" + formatMoney(revenue) + ")");
    }

    public void stopTrading() {
        System.out.println("\n--- Stopping Trading Simulation ---");
        marketDataFeed.shutdown();
        try {
            if (!marketDataFeed.awaitTermination(5, TimeUnit.SECONDS)) {
                marketDataFeed.shutdownNow();
            }
        } catch (InterruptedException ex) {
            marketDataFeed.shutdownNow();
            Thread.currentThread().interrupt(); // Restore the interrupted status
        }

        // Liquidate any open positions at the last known price
        if (sharesOwned > 0 && !priceHistory.isEmpty()) {
            BigDecimal lastPrice = priceHistory.getFirst();
            System.out.println("\nLiquidating remaining " + sharesOwned + " shares at $" + formatMoney(lastPrice) + "...");
            currentCash = currentCash.add(lastPrice.multiply(new BigDecimal(sharesOwned), MC), MC);
            sharesOwned = 0; // Shares are now liquidated
        }

        // Calculate final portfolio value. If sharesOwned is 0 after liquidation, this is simply currentCash.
        BigDecimal finalPortfolioValue = currentCash.add(new BigDecimal(sharesOwned).multiply(
                priceHistory.isEmpty() ? BigDecimal.ZERO : priceHistory.getFirst(), MC
        ), MC);

        System.out.println("\nFinal Portfolio: Cash = $" + formatMoney(currentCash) +
                ", Shares = " + sharesOwned +
                ", Total Value = $" + formatMoney(finalPortfolioValue));
        System.out.println("Initial Value: $" + formatMoney(INITIAL_CASH));
        BigDecimal profitLoss = finalPortfolioValue.subtract(INITIAL_CASH, MC);
        System.out.println("Profit/Loss: $" + formatMoney(profitLoss));
        System.out.println("Simulation Ended.");
    }

    /**
     * Helper method to format BigDecimal monetary values for display.
     * @param value The BigDecimal value to format.
     * @return A string representation of the monetary value, e.g., "123.45".
     */
    private String formatMoney(BigDecimal value) {
        return value.setScale(DISPLAY_SCALE, ROUNDING_MODE).toPlainString();
    }
}