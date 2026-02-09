# Trading Strategy Backtesting System

A complete algorithmic trading backtesting framework for Kite Connect API with multiple built-in strategies.

## 📦 Components

### Core Files

1. **TradingStrategy.java** - Strategy interface and implementations
   - `TradingStrategy` interface
   - `Candle` data structure
   - `Trade` record
   - 5 pre-built strategies:
     - Moving Average Crossover
     - RSI (Relative Strength Index)
     - Bollinger Bands
     - MACD
     - Breakout

2. **BacktestEngine.java** - Backtesting engine
   - Runs strategies on historical data
   - Calculates performance metrics
   - Generates detailed reports
   - Exports results to CSV/JSON

3. **HistoricalDataFetcher.java** - Data management
   - Fetches historical data from Kite Connect API
   - Handles rate limiting (3 requests/second)
   - Saves/loads data to CSV
   - Manages instrument tokens

4. **BacktestingApp.java** - Complete integrated application
   - Full authentication flow
   - Data fetching
   - Strategy execution
   - Results comparison

5. **BacktestDemo.java** - Standalone demo (no API required)
   - Works with simulated data
   - Perfect for learning and testing
   - No authentication needed

## 🚀 Quick Start

### Option 1: Demo Mode (No API Required)

```bash
# Compile
javac -cp ".:json-20231013.jar" *.java

# Run demo
java -cp ".:json-20231013.jar" BacktestDemo
```

This will:
- Generate 1 year of simulated market data
- Run all 5 strategies
- Compare performance
- Display results

### Option 2: Live Data Mode (Requires Kite Connect)

```bash
# Set environment variables
export KITE_API_KEY="your_api_key"
export KITE_API_SECRET="your_api_secret"

# Compile
javac -cp ".:json-20231013.jar" *.java

# Run
java -cp ".:json-20231013.jar" BacktestingApp
```

## 📊 Built-in Trading Strategies

### 1. Moving Average Crossover
**Description**: Buy when fast MA crosses above slow MA, sell when it crosses below.

**Parameters**:
- `fastPeriod` (default: 10) - Fast moving average period
- `slowPeriod` (default: 20) - Slow moving average period

**Best For**: Trending markets

**Usage**:
```java
TradingStrategy strategy = new MovingAverageCrossoverStrategy();
Map<String, Object> params = new HashMap<>();
params.put("fastPeriod", 10);
params.put("slowPeriod", 20);
strategy.initialize(params);
```

### 2. RSI Strategy
**Description**: Buy when RSI < oversold threshold, sell when RSI > overbought threshold.

**Parameters**:
- `period` (default: 14) - RSI calculation period
- `oversold` (default: 30) - Oversold threshold
- `overbought` (default: 70) - Overbought threshold

**Best For**: Range-bound markets, mean reversion

**Usage**:
```java
TradingStrategy strategy = new RSIStrategy();
Map<String, Object> params = new HashMap<>();
params.put("period", 14);
params.put("oversold", 30.0);
params.put("overbought", 70.0);
strategy.initialize(params);
```

### 3. Bollinger Bands
**Description**: Buy when price touches lower band, sell when price touches upper band.

**Parameters**:
- `period` (default: 20) - Moving average period
- `stdDev` (default: 2.0) - Standard deviation multiplier

**Best For**: Volatility-based trading, mean reversion

**Usage**:
```java
TradingStrategy strategy = new BollingerBandsStrategy();
Map<String, Object> params = new HashMap<>();
params.put("period", 20);
params.put("stdDev", 2.0);
strategy.initialize(params);
```

### 4. MACD
**Description**: Buy when MACD crosses above signal line, sell when it crosses below.

**Parameters**:
- `fast` (default: 12) - Fast EMA period
- `slow` (default: 26) - Slow EMA period
- `signal` (default: 9) - Signal line period

**Best For**: Trend identification and momentum trading

**Usage**:
```java
TradingStrategy strategy = new MACDStrategy();
Map<String, Object> params = new HashMap<>();
params.put("fast", 12);
params.put("slow", 26);
params.put("signal", 9);
strategy.initialize(params);
```

### 5. Breakout Strategy
**Description**: Buy when price breaks above recent high, sell when it breaks below recent low.

**Parameters**:
- `lookback` (default: 20) - Lookback period for high/low

**Best For**: Momentum and breakout trading

**Usage**:
```java
TradingStrategy strategy = new BreakoutStrategy();
Map<String, Object> params = new HashMap<>();
params.put("lookback", 20);
strategy.initialize(params);
```

## 📈 Historical Data API

### Fetching Data

```java
HistoricalDataFetcher fetcher = new HistoricalDataFetcher(apiKey, accessToken);

// Get instrument token
long instrumentToken = fetcher.getInstrumentToken("NSE", "INFY");

// Fetch historical data
List<Candle> data = fetcher.fetchHistoricalData(
    instrumentToken,
    HistoricalDataFetcher.Interval.DAY,
    fromDate,
    toDate,
    false // includeOI
);

// Save to CSV
fetcher.saveToCSV(data, "infy_historical.csv");

// Load from CSV
List<Candle> loadedData = fetcher.loadFromCSV("infy_historical.csv");
```

### Data Intervals

| Interval | Max Days Per Request | Use Case |
|----------|---------------------|----------|
| MINUTE | 60 | Intraday scalping |
| THREE_MINUTE | 100 | Short-term trading |
| FIVE_MINUTE | 100 | Day trading |
| TEN_MINUTE | 100 | Day trading |
| FIFTEEN_MINUTE | 200 | Swing trading |
| THIRTY_MINUTE | 200 | Swing trading |
| SIXTY_MINUTE | 400 | Position trading |
| DAY | 2000 | Long-term backtesting |

### Rate Limits

- Historical data: **3 requests per second**
- The fetcher automatically handles rate limiting
- Large date ranges are split into chunks

## 🎯 Creating Custom Strategies

### Step 1: Implement TradingStrategy Interface

```java
public class MyCustomStrategy implements TradingStrategy {
    
    private int parameter1;
    private double parameter2;
    
    @Override
    public String getName() {
        return "My Custom Strategy";
    }
    
    @Override
    public String getDescription() {
        return "Description of what my strategy does";
    }
    
    @Override
    public void initialize(Map<String, Object> parameters) {
        this.parameter1 = (Integer) parameters.get("param1");
        this.parameter2 = (Double) parameters.get("param2");
    }
    
    @Override
    public int generateSignal(List<Candle> candles, int currentIndex) {
        // Your logic here
        // Return 1 for BUY, -1 for SELL, 0 for HOLD
        
        if (/* buy condition */) {
            return 1;
        } else if (/* sell condition */) {
            return -1;
        }
        
        return 0;
    }
    
    @Override
    public boolean shouldExit(List<Candle> candles, int currentIndex, Trade currentTrade) {
        // Your exit logic here
        return false;
    }
}
```

### Step 2: Use in Backtesting

```java
TradingStrategy myStrategy = new MyCustomStrategy();
Map<String, Object> params = new HashMap<>();
params.put("param1", 10);
params.put("param2", 0.5);
myStrategy.initialize(params);

BacktestEngine engine = new BacktestEngine(myStrategy, 100000, 50);
engine.loadHistoricalData(historicalData);
BacktestResult result = engine.runBacktest();
```

## 📊 Performance Metrics

The backtesting engine calculates the following metrics:

### Return Metrics
- **Total Return %** - Overall return on investment
- **Net P&L** - Total profit/loss in rupees
- **Final Capital** - Ending portfolio value

### Trade Metrics
- **Total Trades** - Number of trades executed
- **Winning Trades** - Number of profitable trades
- **Losing Trades** - Number of losing trades
- **Win Rate %** - Percentage of winning trades

### Profit Metrics
- **Total Profit** - Sum of all profits
- **Total Loss** - Sum of all losses
- **Profit Factor** - Total profit / Total loss
- **Average Win** - Average profit per winning trade
- **Average Loss** - Average loss per losing trade
- **Largest Win** - Biggest single profit
- **Largest Loss** - Biggest single loss

### Risk Metrics
- **Max Drawdown %** - Maximum peak-to-trough decline

## 🔧 Example Workflows

### Workflow 1: Test Single Strategy

```java
// 1. Get historical data
HistoricalDataFetcher fetcher = new HistoricalDataFetcher(apiKey, accessToken);
List<Candle> data = fetcher.fetchHistoricalData(
    instrumentToken, Interval.DAY, fromDate, toDate, false
);

// 2. Configure strategy
TradingStrategy strategy = new MovingAverageCrossoverStrategy();
Map<String, Object> params = new HashMap<>();
params.put("fastPeriod", 10);
params.put("slowPeriod", 20);
strategy.initialize(params);

// 3. Run backtest
BacktestEngine engine = new BacktestEngine(strategy, 100000, 50);
engine.loadHistoricalData(data);
BacktestResult result = engine.runBacktest();

// 4. View results
engine.printReport(result);
```

### Workflow 2: Optimize Strategy Parameters

```java
List<BacktestResult> results = new ArrayList<>();

// Test different parameter combinations
for (int fast = 5; fast <= 20; fast += 5) {
    for (int slow = 20; slow <= 50; slow += 10) {
        TradingStrategy strategy = new MovingAverageCrossoverStrategy();
        Map<String, Object> params = new HashMap<>();
        params.put("fastPeriod", fast);
        params.put("slowPeriod", slow);
        strategy.initialize(params);
        
        BacktestEngine engine = new BacktestEngine(strategy, 100000, 50);
        engine.loadHistoricalData(data);
        BacktestResult result = engine.runBacktest();
        
        results.add(result);
    }
}

// Find best combination
BacktestResult best = results.stream()
    .max(Comparator.comparingDouble(r -> r.totalReturn))
    .orElse(null);

System.out.println("Best parameters: " + best.strategyDescription);
```

### Workflow 3: Walk-Forward Analysis

```java
// Split data into training and testing periods
int trainSize = (int) (data.size() * 0.7);
List<Candle> trainData = data.subList(0, trainSize);
List<Candle> testData = data.subList(trainSize, data.size());

// Optimize on training data
BacktestEngine trainEngine = new BacktestEngine(strategy, 100000, 50);
trainEngine.loadHistoricalData(trainData);
BacktestResult trainResult = trainEngine.runBacktest();

// Validate on test data
BacktestEngine testEngine = new BacktestEngine(strategy, 100000, 50);
testEngine.loadHistoricalData(testData);
BacktestResult testResult = testEngine.runBacktest();

// Compare results
System.out.println("Training Return: " + trainResult.totalReturn + "%");
System.out.println("Testing Return: " + testResult.totalReturn + "%");
```

## 💾 Data Management

### Save Historical Data

```java
// Fetch and save
List<Candle> data = fetcher.fetchHistoricalData(...);
fetcher.saveToCSV(data, "nifty_2024.csv");
```

### Load Saved Data

```java
// Load from file (no API call needed)
List<Candle> data = fetcher.loadFromCSV("nifty_2024.csv");
```

### CSV Format

```
timestamp,open,high,low,close,volume,oi
2024-01-01 09:15:00,21500.50,21550.25,21480.00,21530.75,5000000,0
2024-01-01 09:16:00,21530.75,21560.00,21520.00,21545.50,3200000,0
...
```

## ⚠️ Important Considerations

### Backtesting Limitations

1. **Look-ahead bias** - Ensure strategy only uses past data
2. **Survivorship bias** - Test on delisted stocks too
3. **Transaction costs** - Add brokerage, taxes, slippage
4. **Market impact** - Large orders affect prices
5. **Over-optimization** - Don't overfit to historical data

### Best Practices

✅ **DO**:
- Test on different time periods
- Include transaction costs
- Use out-of-sample testing
- Consider market regimes
- Validate with paper trading
- Keep strategies simple

❌ **DON'T**:
- Overfit to historical data
- Ignore transaction costs
- Test on single instrument only
- Use future data in signals
- Trade immediately after backtest
- Ignore risk management

## 📚 Additional Resources

### Kite Connect API Documentation
- Official Docs: https://kite.trade/docs/connect/v3/
- Historical Data: https://kite.trade/docs/connect/v3/historical/
- Forum: https://kite.trade/forum/

### Algorithmic Trading Resources
- Quantopian Lectures (archived)
- QuantConnect Documentation
- "Algorithmic Trading" by Ernest Chan
- "Advances in Financial Machine Learning" by Marcos López de Prado

## 🔜 Future Enhancements

Planned features:
- [ ] Transaction cost modeling
- [ ] Position sizing algorithms
- [ ] Risk management (stop loss, take profit)
- [ ] Portfolio backtesting (multiple instruments)
- [ ] Monte Carlo simulation
- [ ] Walk-forward optimization
- [ ] Strategy combination/ensemble
- [ ] Live trading integration
- [ ] WebSocket for real-time data
- [ ] Performance visualization charts

## 📄 License

This is a sample implementation for educational purposes. Use at your own risk.

## ⚖️ Disclaimer

**IMPORTANT**: 
- Past performance does not guarantee future results
- Backtesting results may not reflect actual trading
- Always paper trade before live trading
- Trading involves substantial risk of loss
- This software is provided "as is" without warranty

---

Happy Backtesting! 📈
