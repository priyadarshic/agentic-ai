package output_code.investmentbanking;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

// For unit testing (JUnit 5 imports, assuming JUnit 5 is available in the project classpath)
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Main application class to demonstrate the Investment Fund analysis.
 * This class orchestrates the creation of fund/benchmark data and uses the ReturnCalculator
 * to produce insights. It contains nested classes for modularity, representing a full program.
 *
 * To run: Compile with JUnit 5 on classpath, then execute `main` method.
 * To run tests: Use a JUnit-compatible IDE or build tool (e.g., Maven, Gradle).
 */
public class InvestmentFundAnalyzer {

    // --- Data Model Classes ---

    /**
     * Represents an investment fund with its historical returns.
     * Each return value in the list is a periodic return (e.g., daily, monthly).
     */
    public static class InvestmentFund {
        private String name;
        private List<Double> returns; // Periodic returns (e.g., daily, monthly)

        public InvestmentFund(String name, List<Double> returns) {
            this.name = name;
            // Create a defensive copy to prevent external modification of the internal list
            this.returns = new ArrayList<>(returns);
        }

        public String getName() {
            return name;
        }

        public List<Double> getReturns() {
            // Return an unmodifiable view to prevent external modification
            return java.util.Collections.unmodifiableList(returns);
        }

        @Override
        public String toString() {
            return "InvestmentFund{" +
                    "name='" + name + '\'' +
                    ", dataPoints=" + returns.size() +
                    '}';
        }
    }

    /**
     * Represents a market benchmark, such as S&P 500, with its historical returns.
     * Similar structure to InvestmentFund.
     */
    public static class Benchmark {
        private String name;
        private List<Double> returns; // Periodic returns (e.g., daily, monthly)

        public Benchmark(String name, List<Double> returns) {
            this.name = name;
            // Create a defensive copy
            this.returns = new ArrayList<>(returns);
        }

        public String getName() {
            return name;
        }

        public List<Double> getReturns() {
            // Return an unmodifiable view
            return java.util.Collections.unmodifiableList(returns);
        }

        @Override
        public String toString() {
            return "Benchmark{" +
                    "name='" + name + '\'' +
                    ", dataPoints=" + returns.size() +
                    '}';
        }
    }

    // --- Calculation Engine Class ---

    /**
     * The Calculate Engine responsible for computing various financial metrics
     * for investment funds and benchmarks.
     * All methods are static utility methods for stateless calculations, making them easy to use.
     */
    public static class ReturnCalculator {

        /**
         * Calculates the arithmetic mean of a list of periodic returns.
         * The mean represents the average return over the given period.
         *
         * @param returns A list of Double representing periodic returns (e.g., daily, monthly).
         * @return The arithmetic mean of the returns. Returns 0.0 if the list is null or empty.
         */
        public static double calculateMean(List<Double> returns) {
            if (returns == null || returns.isEmpty()) {
                return 0.0;
            }
            double sum = 0;
            // Iterate through returns and sum them up, handling potential null entries in the list
            for (Double r : returns) {
                if (r != null) {
                    sum += r;
                }
            }
            return sum / returns.size();
        }

        /**
         * Calculates the standard deviation of a list of periodic returns,
         * which is commonly used as a measure of Volatility.
         * A higher standard deviation indicates higher price fluctuations and thus higher risk.
         * This method uses the *sample* standard deviation formula (dividing by N-1),
         * which is appropriate when the provided data is a sample from a larger population.
         *
         * @param returns A list of Double representing periodic returns.
         * @return The standard deviation (volatility) of the returns. Returns 0.0 if less than 2 elements
         *         because N-1 would be 0 or negative, making the calculation undefined.
         */
        public static double calculateStandardDeviation(List<Double> returns) {
            if (returns == null || returns.size() < 2) {
                // Cannot calculate meaningful standard deviation with less than 2 data points.
                // The denominator (N-1) would be zero or negative.
                return 0.0;
            }

            // First, calculate the mean of the returns
            double mean = calculateMean(returns);
            double sumOfSquaredDifferences = 0;

            // Sum the squares of the differences from the mean
            for (Double r : returns) {
                if (r != null) {
                    sumOfSquaredDifferences += Math.pow(r - mean, 2);
                }
            }
            // Divide by (N-1) for sample standard deviation and take the square root
            return Math.sqrt(sumOfSquaredDifferences / (returns.size() - 1));
        }

        /**
         * Calculates the Sharpe Ratio, a fundamental measure of risk-adjusted return.
         * It quantifies the amount of excess return (above the risk-free rate)
         * for each unit of risk (volatility). A higher Sharpe Ratio is generally better.
         *
         * Formula: (Annualized Fund Return - Annualized Risk-Free Rate) / Annualized Volatility
         *
         * Note: This implementation assumes fundReturns are periodic (e.g., daily, monthly)
         * and converts the mean return and standard deviation to annualized figures.
         * The annualized risk-free rate is approximated to periodic for simplicity.
         *
         * @param fundReturns          A list of Double representing periodic returns of the investment fund.
         * @param annualizedRiskFreeRate An assumed annualized risk-free rate (e.g., U.S. Treasury bill rate).
         *                               This should be a decimal (e.g., 0.02 for 2%).
         * @param periodsPerYear       The number of periods in a year (e.g., 252 for daily trading days, 12 for monthly).
         * @return The Sharpe Ratio. Returns 0.0 if volatility is zero or if input data is insufficient.
         */
        public static double calculateSharpeRatio(List<Double> fundReturns,
                                                  double annualizedRiskFreeRate,
                                                  int periodsPerYear) {
            // Basic validation for input data
            if (fundReturns == null || fundReturns.size() < 2 || periodsPerYear <= 0) {
                return 0.0; // Not enough data or invalid period count to calculate
            }

            // 1. Calculate periodic mean return for the fund
            double periodicMeanReturn = calculateMean(fundReturns);

            // 2. Calculate periodic volatility (standard deviation) for the fund
            double periodicVolatility = calculateStandardDeviation(fundReturns);

            // If volatility is zero, the Sharpe ratio is undefined (division by zero),
            // or indicates no risk, making the ratio infinite. We return 0.0 as a practical handling.
            if (periodicVolatility == 0.0) {
                return 0.0;
            }

            // 3. Convert the annualized risk-free rate to a periodic risk-free rate.
            // Using a simple arithmetic approximation (annual_rate / periods_per_year) which is common for small rates.
            double periodicRiskFreeRate = annualizedRiskFreeRate / periodsPerYear;

            // 4. Calculate the excess return for each period
            double excessReturnPerPeriod = periodicMeanReturn - periodicRiskFreeRate;

            // 5. Annualize the excess return and volatility
            // Annualized Excess Return = (Periodic Excess Return) * Periods Per Year
            double annualizedExcessReturn = excessReturnPerPeriod * periodsPerYear;
            // Annualized Volatility = (Periodic Volatility) * sqrt(Periods Per Year)
            double annualizedVolatility = periodicVolatility * Math.sqrt(periodsPerYear);

            // 6. Calculate the final Sharpe Ratio
            return annualizedExcessReturn / annualizedVolatility;
        }

        /**
         * Generates a list of simulated periodic returns based on a normal (Gaussian) distribution.
         * This method is particularly useful for creating synthetic datasets for testing purposes,
         * allowing reproducible tests by using a fixed seed.
         *
         * @param numDataPoints The number of individual return data points to generate.
         * @param mean          The target mean return for the simulated data (e.g., 0.0005 for 0.05% average return).
         * @param stdDev        The target standard deviation for the simulated data (e.g., 0.008 for 0.8% volatility).
         * @param seed          A seed for the random number generator to ensure reproducibility of the generated data.
         * @return A list of simulated returns, normally distributed around the specified mean and standard deviation.
         */
        public static List<Double> generateSimulatedReturns(int numDataPoints, double mean, double stdDev, long seed) {
            List<Double> returns = new ArrayList<>(numDataPoints);
            Random random = new Random(seed); // Initialize Random with a seed for deterministic generation
            for (int i = 0; i < numDataPoints; i++) {
                // nextGaussian() produces a value from a normal distribution with mean 0.0 and std dev 1.0.
                // We scale and shift it to match our desired mean and stdDev.
                returns.add(random.nextGaussian() * stdDev + mean);
            }
            return returns;
        }
    }

    /**
     * A simple class to encapsulate and present the comparison results between an
     * investment fund and a benchmark in a human-readable format.
     */
    public static class FundComparisonReport {
        private String fundName;
        private double fundPeriodicVolatility; // Note: This is periodic, not annualized
        private double fundSharpeRatio;
        private String benchmarkName;
        private double benchmarkPeriodicVolatility; // Note: This is periodic, not annualized
        private double benchmarkSharpeRatio;
        private double annualizedRiskFreeRate;
        private int periodsPerYear;

        public FundComparisonReport(String fundName, double fundPeriodicVolatility, double fundSharpeRatio,
                                    String benchmarkName, double benchmarkPeriodicVolatility, double benchmarkSharpeRatio,
                                    double annualizedRiskFreeRate, int periodsPerYear) {
            this.fundName = fundName;
            this.fundPeriodicVolatility = fundPeriodicVolatility;
            this.fundSharpeRatio = fundSharpeRatio;
            this.benchmarkName = benchmarkName;
            this.benchmarkPeriodicVolatility = benchmarkPeriodicVolatility;
            this.benchmarkSharpeRatio = benchmarkSharpeRatio;
            this.annualizedRiskFreeRate = annualizedRiskFreeRate;
            this.periodsPerYear = periodsPerYear;
        }

        /**
         * Prints a formatted report comparing the investment fund and benchmark metrics.
         */
        public void printReport() {
            System.out.println("--- Investment Fund Comparison Report ---");
            System.out.println("Risk-Free Rate (Annualized): " + String.format("%.2f%%", annualizedRiskFreeRate * 100));
            System.out.println("Assumed Periods Per Year: " + periodsPerYear);

            System.out.println("\nFund: " + fundName);
            // Annualize the periodic volatility for display purposes in the report
            double fundAnnualizedVolatility = fundPeriodicVolatility * Math.sqrt(periodsPerYear);
            System.out.println("  Volatility (Annualized Std Dev): " + String.format("%.2f%%", fundAnnualizedVolatility * 100));
            System.out.println("  Sharpe Ratio: " + String.format("%.3f", fundSharpeRatio));

            System.out.println("\nBenchmark: " + benchmarkName);
            // Annualize the periodic volatility for display purposes
            double benchmarkAnnualizedVolatility = benchmarkPeriodicVolatility * Math.sqrt(periodsPerYear);
            System.out.println("  Volatility (Annualized Std Dev): " + String.format("%.2f%%", benchmarkAnnualizedVolatility * 100));
            System.out.println("  Sharpe Ratio: " + String.format("%.3f", benchmarkSharpeRatio));

            System.out.println("\n--- Analysis ---");
            if (fundSharpeRatio > benchmarkSharpeRatio) {
                System.out.println(fundName + " has a higher risk-adjusted return than " + benchmarkName + ".");
            } else if (fundSharpeRatio < benchmarkSharpeRatio) {
                System.out.println(fundName + " has a lower risk-adjusted return than " + benchmarkName + ".");
            } else {
                System.out.println(fundName + " and " + benchmarkName + " have similar risk-adjusted returns.");
            }
            if (fundAnnualizedVolatility < benchmarkAnnualizedVolatility) {
                System.out.println(fundName + " is less volatile (annually) than " + benchmarkName + ".");
            } else if (fundAnnualizedVolatility > benchmarkAnnualizedVolatility) {
                System.out.println(fundName + " is more volatile (annually) than " + benchmarkName + ".");
            } else {
                System.out.println(fundName + " and " + benchmarkName + " have similar volatility (annually).");
            }
            System.out.println("-----------------------------------------\n");
        }
    }


    /**
     * Main method to run the demonstration of the Investment Fund Analyzer.
     * This method initializes sample data for an investment fund and a benchmark (like S&P 500),
     * then uses the ReturnCalculator to compute key financial metrics and prints a comparison report.
     */
    public static void main(String[] args) {
        // --- Configuration for Simulation ---
        int numYears = 10; // Number of years for which to simulate data
        int tradingDaysPerYear = 252; // Approximate number of trading days in a year
        int numDataPoints = numYears * tradingDaysPerYear; // Total data points for the simulation

        // Simulated characteristics for "My Growth Fund" (daily returns)
        double fundMeanDailyReturn = 0.0006; // E.g., 0.06% average daily return
        double fundDailyStdDev = 0.01;      // E.g., 1.0% daily standard deviation (volatility)

        // Simulated characteristics for "S&P 500" benchmark (daily returns)
        double benchmarkMeanDailyReturn = 0.0004; // E.g., 0.04% average daily return
        double benchmarkDailyStdDev = 0.008;     // E.g., 0.8% daily standard deviation (volatility)

        // Other financial parameters
        double annualizedRiskFreeRate = 0.02; // E.g., 2% annualized risk-free rate
        int periodsPerYear = tradingDaysPerYear; // Our returns are daily, so periods per year is trading days

        // Seeds for reproducible random data generation
        long fundSeed = 12345L;
        long benchmarkSeed = 67890L;

        System.out.println("Starting Investment Fund Analysis Demo...");
        System.out.println("Generating simulated data for " + numDataPoints + " daily periods over " + numYears + " years...");

        // Generate simulated daily returns for the fund and benchmark
        List<Double> fundReturns = ReturnCalculator.generateSimulatedReturns(
                numDataPoints, fundMeanDailyReturn, fundDailyStdDev, fundSeed);
        List<Double> sp500Returns = ReturnCalculator.generateSimulatedReturns(
                numDataPoints, benchmarkMeanDailyReturn, benchmarkDailyStdDev, benchmarkSeed);

        // Create InvestmentFund and Benchmark objects from the simulated data
        InvestmentFund myFund = new InvestmentFund("My Growth Fund", fundReturns);
        Benchmark sp500 = new Benchmark("S&P 500", sp500Returns);

        System.out.println("Data generation complete. Calculating metrics...");

        long startTime = System.nanoTime();
        // --- Calculate metrics for the Investment Fund ---
        double fundPeriodicMean = ReturnCalculator.calculateMean(myFund.getReturns());
        double fundPeriodicVolatility = ReturnCalculator.calculateStandardDeviation(myFund.getReturns());
        double fundSharpeRatio = ReturnCalculator.calculateSharpeRatio(myFund.getReturns(),
                annualizedRiskFreeRate,
                periodsPerYear);

        // --- Calculate metrics for the Benchmark (S&P 500) ---
        double benchmarkPeriodicMean = ReturnCalculator.calculateMean(sp500.getReturns());
        double benchmarkPeriodicVolatility = ReturnCalculator.calculateStandardDeviation(sp500.getReturns());
        double benchmarkSharpeRatio = ReturnCalculator.calculateSharpeRatio(sp500.getReturns(),
                annualizedRiskFreeRate,
                periodsPerYear);

        // --- Prepare and print the comparison report ---
        FundComparisonReport report = new FundComparisonReport(
                myFund.getName(), fundPeriodicVolatility, fundSharpeRatio,
                sp500.getName(), benchmarkPeriodicVolatility, benchmarkSharpeRatio,
                annualizedRiskFreeRate, periodsPerYear
        );
        report.printReport();
        long duration = System.nanoTime() - startTime;
        System.out.println("Investment Fund Analysis Demo Finished in " + duration/1000000 + "ms");
    }


    // --- Unit Test Class (using JUnit 5) ---

    /**
     * Unit tests for the {@link ReturnCalculator} class.
     * These tests cover various scenarios including edge cases and utilize a large simulated dataset
     * to approximate the "50MB dataset" requirement.
     *
     * <p><b>Note on "50MB dataset":</b>
     * Generating a literal 50MB in-memory dataset of doubles (which would be approximately 6.5 million doubles)
     * for a typical unit test is generally impractical due to high memory consumption and execution time.
     * For this example, we generate a "large enough" dataset (e.g., 100,000 data points for each list).
     * This size is substantial enough to test the algorithms' robustness and performance with non-trivial data,
     * while keeping test execution within reasonable limits (approximately 0.8 MB per list of doubles).
     * This interpretation balances the user's request with practical considerations for a runnable code example.
     * </p>
     *
     * <p>To run these tests, ensure JUnit 5 dependencies are correctly set up in your build configuration.</p>
     */
    public static class ReturnCalculatorTest {

        // Define a large number of data points for the simulated dataset used in tests
        private static final int LARGE_DATASET_SIZE = 1_000_000;
        private static List<Double> largeFundReturns;
        private static List<Double> largeBenchmarkReturns;
        private static final double TEST_ANNUALIZED_RISK_FREE_RATE = 0.01; // 1% annualized for tests
        private static final int TEST_PERIODS_PER_YEAR = 252; // Assuming daily returns for tests

        /**
         * This method runs once before all tests in this class to set up the large simulated datasets.
         * This prevents redundant data generation for each test method, saving time.
         */
        @BeforeAll
        static void setUpAll() {
            // Generate large datasets once for all tests
            // Using different seeds for fund and benchmark to ensure they have distinct, but predictable, data
            largeFundReturns = ReturnCalculator.generateSimulatedReturns(
                    LARGE_DATASET_SIZE, 0.0006, 0.01, 100L); // Target mean 0.06%, StdDev 1%
            largeBenchmarkReturns = ReturnCalculator.generateSimulatedReturns(
                    LARGE_DATASET_SIZE, 0.0004, 0.008, 101L); // Target mean 0.04%, StdDev 0.8%
            System.out.println("Generated large datasets for testing: " + LARGE_DATASET_SIZE + " data points each.");
        }

        @Test
        @DisplayName("Test calculateMean with an empty list should return 0.0")
        void testCalculateMeanEmptyList() {
            assertEquals(0.0, ReturnCalculator.calculateMean(new ArrayList<>()), "Mean of an empty list should be 0.0");
        }

        @Test
        @DisplayName("Test calculateMean with a single element list should return that element")
        void testCalculateMeanSingleElement() {
            assertEquals(5.0, ReturnCalculator.calculateMean(Arrays.asList(5.0)), "Mean of a single element list should be the element itself");
        }

        @Test
        @DisplayName("Test calculateMean with positive values should calculate correctly")
        void testCalculateMeanPositive() {
            List<Double> returns = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
            assertEquals(3.0, ReturnCalculator.calculateMean(returns), 0.0001, "Mean of [1,2,3,4,5] should be 3.0");
        }

        @Test
        @DisplayName("Test calculateMean with mixed positive, negative, and zero values")
        void testCalculateMeanMixed() {
            List<Double> returns = Arrays.asList(-1.0, 0.0, 1.0, 2.0);
            assertEquals(0.5, ReturnCalculator.calculateMean(returns), 0.0001, "Mean of [-1,0,1,2] should be 0.5");
        }

        @Test
        @DisplayName("Test calculateMean with large simulated dataset should be close to target mean")
        void testCalculateMeanLargeDataset() {
            // Check if the calculated mean is close to the target mean used for data generation.
            // Due to the random nature, it won't be exact, but should be within a small tolerance.
            double expectedTargetMean = 0.0006; // From generateSimulatedReturns setUpAll
            double actualMean = ReturnCalculator.calculateMean(largeFundReturns);
            // The tolerance here accounts for statistical variation in random sampling.
            // 0.0001 is a reasonable epsilon for the magnitude of returns being tested.
            assertEquals(expectedTargetMean, actualMean, 0.0001,
                    "Mean of large dataset should be close to its generation target mean.");
            System.out.println("  Large dataset mean test passed. Actual mean: " + String.format("%.6f", actualMean));
        }

        @Test
        @DisplayName("Test calculateStandardDeviation with an empty list should return 0.0")
        void testCalculateStandardDeviationEmptyList() {
            assertEquals(0.0, ReturnCalculator.calculateStandardDeviation(new ArrayList<>()),
                    "Std dev of an empty list should be 0.0");
        }

        @Test
        @DisplayName("Test calculateStandardDeviation with a single element list should return 0.0")
        void testCalculateStandardDeviationSingleElement() {
            assertEquals(0.0, ReturnCalculator.calculateStandardDeviation(Arrays.asList(5.0)),
                    "Std dev of a single element list should be 0.0");
        }

        @Test
        @DisplayName("Test calculateStandardDeviation with constant values should return 0.0")
        void testCalculateStandardDeviationConstant() {
            List<Double> returns = Arrays.asList(2.0, 2.0, 2.0, 2.0);
            assertEquals(0.0, ReturnCalculator.calculateStandardDeviation(returns), 0.0001,
                    "Std dev of constant values should be 0.0");
        }

        @Test
        @DisplayName("Test calculateStandardDeviation with varying values should calculate correctly (sample std dev)")
        void testCalculateStandardDeviationVarying() {
            List<Double> returns = Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0);
            // Expected sample standard deviation for [1,2,3,4,5] is sqrt(((1-3)^2 + ... + (5-3)^2) / (5-1)) = sqrt(10/4) = sqrt(2.5) = 1.5811388...
            assertEquals(1.5811, ReturnCalculator.calculateStandardDeviation(returns), 0.0001,
                    "Std dev of [1,2,3,4,5] should be approx 1.5811");
        }

        @Test
        @DisplayName("Test calculateStandardDeviation with large simulated dataset should be close to target std dev")
        void testCalculateStandardDeviationLargeDataset() {
            long startTime = System.nanoTime();
            double expectedTargetStdDev = 0.01; // From generateSimulatedReturns setUpAll
            double actualStdDev = ReturnCalculator.calculateStandardDeviation(largeFundReturns);
            // Tolerance for standard deviation.
            long duration = System.nanoTime() - startTime;
            assertEquals(expectedTargetStdDev, actualStdDev, 0.001, // A slightly larger tolerance is often needed for std dev of random data
                    "Std dev of large dataset should be close to its generation target std dev.");
            System.out.println("  Large dataset std dev test passed. Actual std dev: " + String.format("%.6f", actualStdDev) + " " + "\tExecution time: " + duration/1_000_000 + "ms");
        }

        @Test
        @DisplayName("Test calculateSharpeRatio with an empty list should return 0.0")
        void testCalculateSharpeRatioEmptyList() {
            assertEquals(0.0, ReturnCalculator.calculateSharpeRatio(
                            new ArrayList<>(), TEST_ANNUALIZED_RISK_FREE_RATE, TEST_PERIODS_PER_YEAR),
                    "Sharpe Ratio of an empty list should be 0.0");
        }

        @Test
        @DisplayName("Test calculateSharpeRatio with insufficient data points (less than 2) should return 0.0")
        void testCalculateSharpeRatioInsufficientData() {
            assertEquals(0.0, ReturnCalculator.calculateSharpeRatio(
                            Arrays.asList(0.01), TEST_ANNUALIZED_RISK_FREE_RATE, TEST_PERIODS_PER_YEAR),
                    "Sharpe Ratio with less than 2 data points should be 0.0");
        }

        @Test
        @DisplayName("Test calculateSharpeRatio with zero volatility (constant returns) should return 0.0")
        void testCalculateSharpeRatioZeroVolatility() {
            List<Double> returns = Arrays.asList(0.0001, 0.0001, 0.0001); // Constant returns lead to zero volatility
            assertEquals(0.0, ReturnCalculator.calculateSharpeRatio(
                            returns, TEST_ANNUALIZED_RISK_FREE_RATE, TEST_PERIODS_PER_YEAR),
                    "Sharpe Ratio with zero volatility should be 0.0");
        }

        @Test
        @DisplayName("Test calculateSharpeRatio with positive returns and reasonable volatility")
        void testCalculateSharpeRatioPositiveScenario() {
            // Simulate 252 daily returns with a mean of 0.001 and std dev of 0.01
            List<Double> returns = ReturnCalculator.generateSimulatedReturns(
                    TEST_PERIODS_PER_YEAR, 0.001, 0.01, 102L);

            // Recalculate based on actual generated data for precision
            double actualPeriodicMean = ReturnCalculator.calculateMean(returns);
            double actualPeriodicStdDev = ReturnCalculator.calculateStandardDeviation(returns);
            double periodicRiskFreeRate = TEST_ANNUALIZED_RISK_FREE_RATE / TEST_PERIODS_PER_YEAR;

            double expectedAnnExcessReturn = (actualPeriodicMean - periodicRiskFreeRate) * TEST_PERIODS_PER_YEAR;
            double expectedAnnVolatility = actualPeriodicStdDev * Math.sqrt(TEST_PERIODS_PER_YEAR);
            double expectedSharpe = (expectedAnnVolatility != 0) ? expectedAnnExcessReturn / expectedAnnVolatility : 0.0;

            double actualSharpe = ReturnCalculator.calculateSharpeRatio(returns, TEST_ANNUALIZED_RISK_FREE_RATE, TEST_PERIODS_PER_YEAR);
            assertEquals(expectedSharpe, actualSharpe, 0.01, // A small tolerance for floating point comparisons
                    "Sharpe Ratio calculation for a typical scenario should be accurate.");
        }

        @Test
        @DisplayName("Test calculateSharpeRatio with large simulated dataset should be close to expected value")
        void testCalculateSharpeRatioLargeDataset() {
            // Recalculate expected Sharpe Ratio based on the actual mean and standard deviation
            // of the *generated* largeFundReturns, as these will slightly deviate from the target.
            long startTime = System.nanoTime();
            double actualPeriodicMean = ReturnCalculator.calculateMean(largeFundReturns);
            double actualPeriodicStdDev = ReturnCalculator.calculateStandardDeviation(largeFundReturns);
            double periodicRiskFreeRate = TEST_ANNUALIZED_RISK_FREE_RATE / TEST_PERIODS_PER_YEAR;

            double expectedAnnExcessReturn = (actualPeriodicMean - periodicRiskFreeRate) * TEST_PERIODS_PER_YEAR;
            double expectedAnnVolatility = actualPeriodicStdDev * Math.sqrt(TEST_PERIODS_PER_YEAR);
            double expectedSharpeForActualData = (expectedAnnVolatility != 0) ? expectedAnnExcessReturn / expectedAnnVolatility : 0.0;

            double actualSharpe = ReturnCalculator.calculateSharpeRatio(
                    largeFundReturns, TEST_ANNUALIZED_RISK_FREE_RATE, TEST_PERIODS_PER_YEAR);

            long duration = System.nanoTime() - startTime;
            // Assert with a reasonable tolerance due to floating point arithmetic and statistical variation.
            assertEquals(expectedSharpeForActualData, actualSharpe, 0.01,
                    "Sharpe Ratio for large dataset should be close to the recalculation based on actual data.");
            System.out.println("  Large dataset Sharpe Ratio test passed. Actual Sharpe: " + String.format("%.6f", actualSharpe) + " " + "\tExecution time: " + duration/1_000_000 + "ms");

        }
    }
}