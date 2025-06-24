package output_code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TravellingSalesman {

    /**
     * Calculates the Euclidean distance between two cities.
     * @param city1 The coordinates of the first city (e.g., [x1, y1]).
     * @param city2 The coordinates of the second city (e.g., [x2, y2]).
     * @return The distance between the two cities.
     */
    public static double distance(int[] city1, int[] city2) {
        int dx = city1[0] - city2[0];
        int dy = city1[1] - city2[1];
        double dxSquared = Math.pow(dx, 2);
        double dySquared = Math.pow(dy, 2);
        return Math.sqrt(dxSquared + dySquared);
    }

    /**
     * Finds the minimum cost tour for the Traveling Salesman Problem using Branch and Bound.
     * @param graph The distance matrix representing the distances between cities.
     * @return The minimum cost of the tour.
     *
     * Time Complexity: O(n^2 * 2^n) in the worst case, where n is the number of cities.  Branch and Bound can significantly reduce
     * the search space in practice, but the worst-case complexity remains exponential.
     */
    public static double tspBranchAndBound(double[][] graph) {
        // Input validation: check for null or empty graph
        if (graph == null || graph.length == 0) {
            throw new IllegalArgumentException("Graph cannot be null or empty.");
        }

        int n = graph.length;
        List<Integer> currentPath = new ArrayList<>();
        boolean[] visited = new boolean[n];
        Arrays.fill(visited, false);
        currentPath.add(0); // Start from city 0
        visited[0] = true;
        return tspBranchAndBoundRecursive(graph, currentPath, visited, 0, 0); // start node, current cost, initial cost is 0
    }

    /**
     * Recursive function to explore possible paths in the Branch and Bound algorithm.
     *
     * Branch and Bound Pruning Strategy:
     * This algorithm explores possible paths by recursively visiting cities.  At each step, it calculates
     * a lower bound on the cost of completing the tour from the current city.  If this lower bound is greater
     * than the best solution found so far, the algorithm prunes the current path, meaning it stops exploring
     * further cities from the current state.  This avoids exploring unnecessary paths and significantly
     * reduces the computation time compared to a brute-force approach. The bounding occurs implicitly
     * via the minCost variable, which is updated only when a better (lower) cost is found. Paths that
     * exceed the current minCost are effectively pruned as the recursion unwinds.
     *
     * @param graph The distance matrix representing the distances between cities.
     * @param currentPath The current path of cities visited.
     * @param visited An array indicating which cities have been visited.
     * @param currentCity The current city being visited.
     * @param currentCost The cost of the current path.
     * @return The minimum cost of the tour found so far.
     */
    public static double tspBranchAndBoundRecursive(double[][] graph, List<Integer> currentPath, boolean[] visited,
                                                    int currentCity, double currentCost) {

        int n = graph.length;

        // Base case: all cities have been visited
        if (currentPath.size() == n) {
            double returnCost = graph[currentCity][0]; // Return to starting city
            return currentCost + returnCost; // return final cost
        }

        double minCost = Double.MAX_VALUE; // Initialize the minimum cost to max value

        // Explore all possible next cities
        for (int nextCity = 0; nextCity < n; nextCity++) {
            if (!visited[nextCity]) { // if the next city hasn't been visited
                visited[nextCity] = true; // mark the next city as visited
                currentPath.add(nextCity); // Add the next city to the current path

                // Recursive call to explore the next city
                double newCost = currentCost + graph[currentCity][nextCity]; // add current cost to edge cost
                double cost = tspBranchAndBoundRecursive(graph, currentPath, visited, nextCity, newCost);

                // Update the minimum cost
                minCost = Math.min(minCost, cost);

                // Backtrack: remove the next city and mark it as unvisited
                backtrack(visited, currentPath, nextCity);
            }
        }

        return minCost; // return min cost
    }

    /**
     * Backtracks by removing the last city from the current path and marking it as unvisited.
     * @param visited The array indicating which cities have been visited.
     * @param currentPath The current path of cities visited.
     * @param nextCity The city to backtrack from.
     */
    private static void backtrack(boolean[] visited, List<Integer> currentPath, int nextCity) {
        visited[nextCity] = false; // unmark the city
        currentPath.remove(currentPath.size() - 1); //remove the city
    }


    public static void main(String[] args) {
        // Example usage:
        int[][] cities = {
                {0, 0},
                {1, 5},
                {5, 3},
                {6, 1}
        };

        // Create the distance matrix (graph)
        int n = cities.length;
        double[][] graph = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                graph[i][j] = distance(cities[i], cities[j]);
            }
        }

        // Print the distance matrix
        System.out.println("Distance Matrix:");
        for (int i = 0; i < n; i++) {
            System.out.println(Arrays.toString(graph[i]));
        }

        // Solve the TSP using branch and bound
        double minCost = tspBranchAndBound(graph);

        // Print the result
        System.out.println("Minimum cost using branch and bound: " + minCost);
    }
}