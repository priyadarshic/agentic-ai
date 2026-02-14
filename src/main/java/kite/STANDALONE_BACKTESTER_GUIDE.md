# 📊 Standalone Intraday Backtester - Quick Guide

## 🎯 What This Program Does

This is a **standalone backtesting tool** that lets you:
- ✅ Test **multiple symbols** in a loop
- ✅ Compare **4 strategies** automatically
- ✅ Get **comprehensive reports** for each symbol
- ✅ See **cross-symbol comparisons**
- ✅ Export results to **CSV**
- ✅ **One-time authentication** (no repeated logins)

---

## 🚀 Quick Start

### 1. Compile:
```bash
javac -cp ".:json-20231013.jar" IntradayBacktester.java
```

### 2. Run:
```bash
java -cp ".:json-20231013.jar" IntradayBacktester
```

---

## 📋 Usage Flow

### Step 1: Authentication
```
🔐 AUTHENTICATION
Enter API Key: [your_key]
Enter API Secret: [your_secret]
✅ Authentication successful!
```

### Step 2: Configuration
```
⚙️  BACKTESTING CONFIGURATION
Enter initial capital (₹): 50000
Enter quantity per trade: 50
Enter number of days to backtest: 30
```

### Step 3: Symbol Loop
```
📊 Enter trading symbol: INFY
[Backtesting INFY...]
✅ Backtest complete! Add another symbol? yes

📊 Enter trading symbol: RELIANCE
[Backtesting RELIANCE...]
✅ Backtest complete! Add another symbol? yes

📊 Enter trading symbol: TCS
[Backtesting TCS...]
✅ Backtest complete! Add another symbol? no
```

### Step 4: Results
```
🏆 CROSS-SYMBOL COMPARISON REPORT
[Comprehensive comparison of all symbols]

💾 Export results to CSV? yes
✅ Results exported to: intraday_backtest_20250215_143022.csv
```

---

## 📊 Sample Output

### For Each Symbol:
```
================================================================================
📊 DETAILED RESULTS: INFY
================================================================================

Strategy              | Return %  | Trades   | Win Rate% | Profit ₹  | Costs ₹  | Auto-SQ
----------------------|-----------|----------|-----------|-----------|----------|--------
Supertrend           |    15.30% |       38 |      58.5% |      7650 |     4256 |       0 ✅
Opening Range        |    12.80% |       42 |      57.1% |      6400 |     4704 |       0 ✅
VWAP                 |    10.50% |       45 |      53.3% |      5250 |     5040 |       2 ⚠️
Scalping             |     8.20% |       67 |      49.3% |      4100 |     7504 |       5 ⚠️

🥇 BEST STRATEGY FOR INFY: Supertrend
   Return: 15.30%
   Total Profit: ₹7,650.00
   Win Rate: 58.5%
   Profit Factor: 2.85
   Max Drawdown: 6.50%
   Avg Trade Duration: 42 mins
   Total Costs: ₹4,256.00
   Cost Impact: 35.7%

📈 TRADE BREAKDOWN:
   Total Trades: 38
   Winning: 22 (Avg: ₹485.50)
   Losing: 16 (Avg: ₹235.00)
   Target Hits: 20
   Stop Loss Hits: 14
   Auto Square-offs: 0 ✅ Perfect!

📅 DAILY PERFORMANCE:
   Trading Days: 21
   Profitable Days: 15 (71.4%)
   Avg Profit/Day: ₹364.29
```

### Cross-Symbol Comparison:
```
================================================================================
🏆 CROSS-SYMBOL COMPARISON REPORT
================================================================================

📊 BEST STRATEGY PER SYMBOL:
Symbol          | Best Strategy        | Return %  | Profit ₹  | Trades   | Win Rate%
----------------|---------------------|-----------|-----------|----------|----------
INFY            | Supertrend          |    15.30% |      7650 |       38 |     58.5%
RELIANCE        | Opening Range       |    18.50% |      9250 |       35 |     62.8%
TCS             | Supertrend          |    14.20% |      7100 |       40 |     57.5%

🥇 OVERALL WINNERS:

🎯 HIGHEST RETURN:
   Symbol: RELIANCE
   Strategy: Opening Range Breakout
   Return: 18.50%
   Profit: ₹9,250.00

🎲 HIGHEST WIN RATE:
   Symbol: RELIANCE
   Strategy: Opening Range Breakout
   Win Rate: 62.8%

🛡️  LOWEST RISK (Min Drawdown):
   Symbol: TCS
   Strategy: Supertrend
   Max Drawdown: 5.80%

📈 STRATEGY PERFORMANCE ACROSS ALL SYMBOLS:
Strategy              | Avg Return | Symbols    | Best %    | Worst %
---------------------|-----------|-----------|-----------|----------
Supertrend           |    14.67% |         3 |    15.30% |    14.20%
Opening Range        |    13.87% |         3 |    18.50% |    10.20%
VWAP                 |    10.23% |         3 |    11.50% |     8.50%
Scalping             |     8.10% |         3 |     9.20% |     6.80%

💡 RECOMMENDATIONS:

1. BEST TRADING OPPORTUNITY:
   Trade RELIANCE using Opening Range Breakout
   Expected return: 18.50%
   Average profit/day: ₹440.48
   Capital needed for ₹500/day: ₹56,700

2. MOST CONSISTENT STRATEGY:
   Supertrend
   Works well on 3 symbols
   Average return: 14.67%

3. DIVERSIFIED PORTFOLIO:
   Trade multiple symbols to reduce risk
   Top 3 symbols:
   1. RELIANCE - Opening Range Breakout (18.50%)
   2. INFY - Supertrend (15.30%)
   3. TCS - Supertrend (14.20%)
```

---

## 💡 Key Features

### ✅ One-Time Authentication
- Login once at the start
- Test multiple symbols without re-login
- Automatic token management

### ✅ Comprehensive Analysis
For each symbol:
- All 4 strategies tested
- Detailed metrics
- Best strategy identified
- Cost breakdown
- Daily performance stats

### ✅ Cross-Symbol Comparison
- Best strategy per symbol
- Overall winners
- Strategy performance across symbols
- Portfolio recommendations

### ✅ CSV Export
Exports all results to CSV with columns:
```
Symbol, Strategy, Return %, Total Trades, Winning Trades,
Losing Trades, Win Rate %, Total Profit, Total Loss,
Profit Factor, Avg Win, Avg Loss, Total Costs,
Max Drawdown %, Target Hits, Stop Loss Hits, Auto Square-offs,
Profitable Days, Total Days, Avg Trade Duration
```

---

## 🎯 Use Cases

### Use Case 1: Find Best Stock
```
Test: INFY, RELIANCE, TCS, WIPRO, HDFCBANK
Goal: Find which stock is most profitable
Result: Get ranked list by return
```

### Use Case 2: Compare Strategies
```
Test: INFY with all 4 strategies
Goal: Find best strategy for INFY
Result: Detailed comparison with metrics
```

### Use Case 3: Build Portfolio
```
Test: 5-10 stocks
Goal: Select top 3 for daily trading
Result: Diversified portfolio recommendation
```

### Use Case 4: Research & Analysis
```
Test: Multiple symbols over 30-60 days
Goal: Understand market behavior
Result: CSV export for further analysis
```

---

## 📊 Metrics Explained

### Return Metrics
- **Return %**: Percentage gain/loss on initial capital
- **Profit ₹**: Absolute profit in rupees
- **Total Trades**: Number of trades executed

### Performance Metrics
- **Win Rate %**: Percentage of profitable trades
- **Profit Factor**: Total profit / Total loss (>1 is good)
- **Avg Win/Loss**: Average profit/loss per trade

### Risk Metrics
- **Max Drawdown %**: Largest peak-to-trough decline
- **Cost Impact %**: How much of profit goes to costs
- **Auto Square-offs**: Trades forced to close at 3:15 PM

### Daily Metrics
- **Profitable Days**: Days with net profit
- **Avg Profit/Day**: Average daily earnings
- **Trading Days**: Total days in backtest period

---

## 💰 Capital Requirements Calculator

The program automatically calculates capital needed for target daily profits:

**Example:**
```
Current Result:
- Capital: ₹50,000
- Avg Profit/Day: ₹440

To earn ₹500/day:
Capital needed = ₹50,000 × (₹500 / ₹440) = ₹56,700

To earn ₹1,000/day:
Capital needed = ₹50,000 × (₹1,000 / ₹440) = ₹113,400
```

---

## 🔧 Configuration Options

### Capital
```
Small: ₹25,000 - ₹50,000 (learning)
Medium: ₹50,000 - ₹1,00,000 (regular trading)
Large: ₹1,00,000+ (serious trading)
```

### Quantity
```
Conservative: 25-50 shares
Moderate: 50-100 shares
Aggressive: 100+ shares
```

### Days to Backtest
```
Quick test: 10-15 days
Standard: 30 days
Comprehensive: 60-90 days
```

---

## 📈 Recommended Workflow

### Day 1: Quick Test
```
Test 3-5 stocks
30-day backtest
Identify top performer
```

### Day 2: Deep Dive
```
Test top stock with longer period (60 days)
Analyze daily performance
Check for consistency
```

### Day 3: Portfolio
```
Test 5-10 stocks
Build diversified portfolio
Export to CSV for records
```

### Day 4+: Paper Trade
```
Simulate trades with top 3 stocks
Use best strategies identified
Track paper trading vs backtest
```

---

## ⚠️ Important Notes

### Transaction Costs Included
All costs are calculated:
- Brokerage: ₹20 per trade
- STT: 0.025%
- Transaction charges: 0.00297%
- GST: 18%
- Stamp duty: 0.003%
- Auto square-off penalty: ₹50 (if applicable)

### Auto Square-Off
- Positions closed by 3:15 PM
- Penalty charged if applicable
- 0 auto square-offs = Perfect timing ✅
- Multiple auto square-offs = Adjust entry timing ⚠️

### Data Quality
- Uses 5-minute candles
- Minimum 100 candles required
- More data = Better results
- Recommendation: 30+ days

---

## 🎯 Tips for Best Results

### 1. Test Multiple Symbols
```
Don't rely on single stock
Test 5-10 stocks
Build diversified portfolio
```

### 2. Look Beyond Returns
```
High return + High drawdown = Risky
Moderate return + Low drawdown = Better
Consider win rate and profit factor
```

### 3. Check Auto Square-offs
```
0 auto square-offs = Strategy timing is good ✅
Many auto square-offs = Adjust entry time ⚠️
```

### 4. Verify with Paper Trading
```
Backtest results ≠ Future results
Paper trade for 1-2 weeks
Then go live with small capital
```

### 5. Export and Analyze
```
Export to CSV
Analyze in Excel/Google Sheets
Track trends over time
Optimize based on data
```

---

## 📁 Output Files

### Console Output
Real-time backtest results displayed in terminal

### CSV Export
Filename format: `intraday_backtest_YYYYMMDD_HHMMSS.csv`

Contains all metrics for all symbols and strategies

Open in Excel for:
- Pivot tables
- Charts
- Further analysis
- Record keeping

---

## 🚀 Example Session

```bash
$ java -cp ".:json-20231013.jar" IntradayBacktester

📊 INTRADAY STRATEGY BACKTESTING SYSTEM

🔐 Authentication...
✅ Authentication successful!

⚙️  Configuration:
Enter initial capital: 50000
Enter quantity: 50
Enter days to backtest: 30

📊 Enter symbol: INFY
✅ Testing INFY... Done!

📊 Enter symbol: RELIANCE
✅ Testing RELIANCE... Done!

📊 Enter symbol: TCS
✅ Testing TCS... Done!

📊 Enter symbol: done

🏆 Comparison Report Generated!

💾 Export to CSV? yes
✅ Exported to: intraday_backtest_20250215_143022.csv

✅ SESSION COMPLETE
```

---

**Ready to find your best trading opportunities!** 🎯

This tool helps you systematically test and compare strategies across multiple stocks to build a profitable intraday trading system.
