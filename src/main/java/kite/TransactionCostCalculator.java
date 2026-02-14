package kite;

/**
 * Transaction Cost Calculator
 * Calculates realistic brokerage and statutory charges for Zerodha
 */
public class TransactionCostCalculator {
    
    public enum TradeType {
        EQUITY_DELIVERY,
        EQUITY_INTRADAY,
        FUTURES,
        OPTIONS_BUY,
        OPTIONS_SELL
    }
    
    /**
     * Calculate total transaction cost for a trade
     * @param tradeValue Total value of the trade (price × quantity)
     * @param tradeType Type of trade
     * @return Total cost in rupees
     */
    public static double calculateCost(double tradeValue, TradeType tradeType) {
        double brokerage = calculateBrokerage(tradeValue, tradeType);
        double stt = calculateSTT(tradeValue, tradeType);
        double transactionCharges = calculateTransactionCharges(tradeValue, tradeType);
        double gst = (brokerage + transactionCharges) * 0.18; // 18% GST
        double sebi = tradeValue * 0.0000001; // ₹10 per crore
        double stampDuty = calculateStampDuty(tradeValue, tradeType);
        
        return brokerage + stt + transactionCharges + gst + sebi + stampDuty;
    }
    
    /**
     * Calculate brokerage
     */
    private static double calculateBrokerage(double tradeValue, TradeType tradeType) {
        switch (tradeType) {
            case EQUITY_DELIVERY:
                return 0; // Free on Zerodha
                
            case EQUITY_INTRADAY:
            case FUTURES:
                // ₹20 or 0.03%, whichever is lower
                return Math.min(20, tradeValue * 0.0003);
                
            case OPTIONS_BUY:
            case OPTIONS_SELL:
                return 20; // Flat ₹20
                
            default:
                return 0;
        }
    }
    
    /**
     * Calculate STT (Securities Transaction Tax)
     */
    private static double calculateSTT(double tradeValue, TradeType tradeType) {
        switch (tradeType) {
            case EQUITY_DELIVERY:
                return tradeValue * 0.001; // 0.1% on both buy and sell
                
            case EQUITY_INTRADAY:
                return tradeValue * 0.00025; // 0.025% on sell side only
                
            case FUTURES:
                return tradeValue * 0.000125; // 0.0125% on sell side only
                
            case OPTIONS_BUY:
                return 0; // No STT on options buy
                
            case OPTIONS_SELL:
                return tradeValue * 0.000625; // 0.0625% on sell side
                
            default:
                return 0;
        }
    }
    
    /**
     * Calculate transaction charges (NSE)
     */
    private static double calculateTransactionCharges(double tradeValue, TradeType tradeType) {
        switch (tradeType) {
            case EQUITY_DELIVERY:
            case EQUITY_INTRADAY:
                return tradeValue * 0.0000297; // 0.00297%
                
            case FUTURES:
            case OPTIONS_BUY:
            case OPTIONS_SELL:
                return tradeValue * 0.0000173; // 0.00173%
                
            default:
                return 0;
        }
    }
    
    /**
     * Calculate stamp duty
     */
    private static double calculateStampDuty(double tradeValue, TradeType tradeType) {
        switch (tradeType) {
            case EQUITY_DELIVERY:
                return tradeValue * 0.00015; // 0.015% on buy
                
            case EQUITY_INTRADAY:
                return tradeValue * 0.00003; // 0.003% on buy
                
            case FUTURES:
            case OPTIONS_BUY:
            case OPTIONS_SELL:
                return tradeValue * 0.00002; // 0.002% on buy
                
            default:
                return 0;
        }
    }
    
    /**
     * Calculate round-trip cost (buy + sell)
     * @param tradeValue Trade value
     * @param tradeType Trade type
     * @return Total round-trip cost
     */
    public static double calculateRoundTripCost(double tradeValue, TradeType tradeType) {
        // Buy side costs
        double buyCost = calculateCost(tradeValue, tradeType);
        
        // Sell side costs (STT is only on sell for most types)
        double sellCost = calculateCost(tradeValue, tradeType);
        
        return buyCost + sellCost;
    }
    
    /**
     * Calculate breakeven percentage
     * How much profit % needed to cover costs
     */
    public static double calculateBreakevenPercentage(double tradeValue, TradeType tradeType) {
        double totalCost = calculateRoundTripCost(tradeValue, tradeType);
        return (totalCost / tradeValue) * 100;
    }
    
    /**
     * Print detailed cost breakdown
     */
    public static void printCostBreakdown(double tradeValue, TradeType tradeType) {
        double brokerage = calculateBrokerage(tradeValue, tradeType);
        double stt = calculateSTT(tradeValue, tradeType);
        double transactionCharges = calculateTransactionCharges(tradeValue, tradeType);
        double gst = (brokerage + transactionCharges) * 0.18;
        double sebi = tradeValue * 0.0000001;
        double stampDuty = calculateStampDuty(tradeValue, tradeType);
        double totalBuy = brokerage + stt + transactionCharges + gst + sebi + stampDuty;
        
        System.out.println("\n💰 TRANSACTION COST BREAKDOWN");
        System.out.println("   Trade Value:          ₹" + String.format("%,.2f", tradeValue));
        System.out.println("   Trade Type:           " + tradeType);
        System.out.println("   " + "-".repeat(50));
        System.out.println("   Brokerage (one way):  ₹" + String.format("%.2f", brokerage));
        System.out.println("   STT:                  ₹" + String.format("%.2f", stt));
        System.out.println("   Transaction Charges:  ₹" + String.format("%.2f", transactionCharges));
        System.out.println("   GST (18%):            ₹" + String.format("%.2f", gst));
        System.out.println("   SEBI Charges:         ₹" + String.format("%.2f", sebi));
        System.out.println("   Stamp Duty:           ₹" + String.format("%.2f", stampDuty));
        System.out.println("   " + "-".repeat(50));
        System.out.println("   One-way Total:        ₹" + String.format("%.2f", totalBuy));
        System.out.println("   Round-trip Total:     ₹" + String.format("%.2f", totalBuy * 2));
        System.out.println("   Breakeven %:          " + 
            String.format("%.3f%%", calculateBreakevenPercentage(tradeValue, tradeType)));
    }
    
    /**
     * Demo/Test
     */
    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("ZERODHA TRANSACTION COST CALCULATOR");
        System.out.println("=".repeat(60));
        
        // Test 1: ₹1 Lakh Equity Intraday
        printCostBreakdown(100000, TradeType.EQUITY_INTRADAY);
        
        // Test 2: ₹1 Lakh Equity Delivery
        printCostBreakdown(100000, TradeType.EQUITY_DELIVERY);
        
        // Test 3: ₹5 Lakh Futures
        printCostBreakdown(500000, TradeType.FUTURES);
        
        // Test 4: ₹50,000 Options
        printCostBreakdown(50000, TradeType.OPTIONS_BUY);
        
        System.out.println("\n" + "=".repeat(60));
        System.out.println("KEY INSIGHTS:");
        System.out.println("=".repeat(60));
        System.out.println("• Delivery trading has ZERO brokerage on Zerodha!");
        System.out.println("• Intraday: Need ~0.08% profit to break even");
        System.out.println("• F&O: Need ~0.05% profit to break even");
        System.out.println("• Costs eat into profits significantly!");
        System.out.println("=".repeat(60));
    }
}
