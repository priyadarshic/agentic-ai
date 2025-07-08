package output_code;

import java.util.Arrays;
import java.util.Random;
import java.util.stream.DoubleStream;

public class CalculateStandardDeviation {

    public static void main(String[] args) {
        // Prepare a large dataset
        int datasetSize = 1000000;
        double range = 50.0;
        double[] data = generateRandomData(datasetSize, range);

        // Performance testing
        long startTime = System.nanoTime();
        double standardDeviationValue = calculateStandardDeviation(data);
        long endTime = System.nanoTime();

        double duration = (endTime - startTime) / 1000000.0; // in milliseconds

        System.out.println("Standard Deviation: " + standardDeviationValue);
        System.out.println("Dataset size: " + datasetSize);
        System.out.println("Execution time: " + duration + " ms");
    }

    /**
     * Calculates the standard deviation of a given array of doubles.
     *
     * @param data The input data as an array of doubles.
     * @return The standard deviation of the data.
     * @throws IllegalArgumentException if the input data is null or empty.
     */
    public static double calculateStandardDeviation(double[] data) {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Input data cannot be null or empty.");
        }

        double mean = calculateMean(data);
        double sumOfSquaredDifferences = 0.0;

        for (double value : data) {
            sumOfSquaredDifferences += Math.pow(value - mean, 2);
        }

        double variance = sumOfSquaredDifferences / data.length;
        return Math.sqrt(variance);
    }

    /**
     * Calculates the mean of a given array of doubles using streams.
     *
     * @param data The input data as an array of doubles.
     * @return The mean of the data.
     */
    private static double calculateMean(double[] data) {
        return Arrays.stream(data).average().orElse(0.0);
    }

    /**
     * Generates an array of random doubles.
     *
     * @param size  The size of the array to generate.
     * @param range The range of the random numbers (0 to range).
     * @return An array of random doubles.
     */
    private static double[] generateRandomData(int size, double range) {
        double[] data = new double[size];
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            data[i] = random.nextDouble() * range;
        }
        return data;
    }
}