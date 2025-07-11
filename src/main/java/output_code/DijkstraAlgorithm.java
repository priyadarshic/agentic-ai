package output_code;

import java.util.Arrays;

public class DijkstraAlgorithm {

    // Function to implement Dijkstra's algorithm
    public static void dijkstra(int[][] graph, int startVertex) {
        int numVertices = graph.length;

        // dist[i] will hold the shortest distance from startVertex to i
        int[] dist = new int[numVertices];

        // sptSet[i] will be true if vertex i is included in shortest path tree
        boolean[] sptSet = new boolean[numVertices];

        // Initialize distances to infinity and sptSet to false
        Arrays.fill(dist, Integer.MAX_VALUE);
        Arrays.fill(sptSet, false);

        // Distance of source vertex from itself is always 0
        dist[startVertex] = 0;

        // Find shortest path for all vertices
        for (int count = 0; count < numVertices - 1; count++) {
            // Pick the minimum distance vertex from the set of vertices
            // not yet processed. u is always equal to startNode in first iteration.
            int u = minDistance(dist, sptSet);

            // Mark the picked vertex as processed
            sptSet[u] = true;

            // Update dist value of the adjacent vertices of the picked vertex.
            for (int v = 0; v < numVertices; v++) {
                // Update dist[v] only if:
                // 1. it is not in sptSet,
                // 2. there is an edge from u to v,
                // 3. total weight of path from startVertex to v through u is
                //    smaller than current value of dist[v]
                if (!sptSet[v] && graph[u][v] != 0 && dist[u] != Integer.MAX_VALUE
                        && dist[u] + graph[u][v] < dist[v]) {
                    dist[v] = dist[u] + graph[u][v];
                }
            }
        }

        // Print the constructed distance array
        printSolution(dist, startVertex);
    }

    // A utility function to find the vertex with minimum distance value,
    // from the set of vertices not yet included in shortest path tree
    private static int minDistance(int[] dist, boolean[] sptSet) {
        // Initialize min value
        int min = Integer.MAX_VALUE, minIndex = -1;

        for (int v = 0; v < dist.length; v++) {
            if (!sptSet[v] && dist[v] <= min) {
                min = dist[v];
                minIndex = v;
            }
        }

        return minIndex;
    }

    // A utility function to print the constructed distance array
    private static void printSolution(int[] dist, int startVertex) {
        System.out.println("Dijkstra's Shortest Path from vertex " + startVertex + ":");
        for (int i = 0; i < dist.length; i++) {
            System.out.println("To vertex " + i + " = " + dist[i]);
        }
    }

    // Main method to test the algorithm
    public static void main(String[] args) {
        /* Let us create the example graph used in the above diagram */
        int[][] graph = new int[][]{
                {0, 4, 0, 0, 0, 0, 0, 8, 0},
                {4, 0, 8, 0, 0, 0, 0, 11, 0},
                {0, 8, 0, 7, 0, 4, 0, 0, 2},
                {0, 0, 7, 0, 9, 14, 0, 0, 0},
                {0, 0, 0, 9, 0, 10, 0, 0, 0},
                {0, 0, 4, 14, 10, 0, 2, 0, 0},
                {0, 0, 0, 0, 0, 2, 0, 1, 6},
                {8, 11, 0, 0, 0, 0, 1, 0, 7},
                {0, 0, 2, 0, 0, 0, 6, 7, 0}
        };

        dijkstra(graph, 0); // Calculate shortest path from source vertex 0
    }
}