package output_code.caching.LRU;

import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class LRUCachePerformanceTest {

    @Test
    public void testLargeCachePerformance() {
        int cacheSize = 100000;
        LRUCache<Integer, String> cache = new LRUCache<>(cacheSize);

        // Fill the cache
        for (int i = 0; i < cacheSize; i++) {
            cache.put(i, "value" + i);
        }

        // Access elements in a random order
        // Accessing random elements to simulate real-world usage and test retrieval performance.
        long startTime = System.currentTimeMillis();
        for (int i = 0; i < cacheSize * 2; i++) {
            int key = (int) (Math.random() * cacheSize);
            cache.get(key);
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Access Time:(ms) " + (endTime - startTime));
    }

    @Test
    public void testEvictionPerformance() {
        int cacheSize = 5000;
        LRUCache<Integer, String> cache = new LRUCache<>(cacheSize);

        // Fill the cache beyond its capacity
        for (int i = 0; i < cacheSize * 2; i++) {
            cache.put(i, "value" + i);
        }

        // Verify that the cache size is as expected. This checks that the eviction policy works correctly under load.
        assertEquals(cacheSize, cache.size());
    }

    @Test
    public void testGetPerformance() {
        int cacheSize = 100000;
        LRUCache<Integer, String> cache = new LRUCache<>(cacheSize);
        for (int i = 0; i < cacheSize; ++i) {
            cache.put(i, "Value " + i);
        }
        //Tests that it doesn't take longer than 500 ms to retrieve all elements in the cache
        assertTimeoutPreemptively(Duration.ofMillis(200), () -> {
            for (int i = 0; i < cacheSize; ++i) {
                cache.get(i);
            }
        });
    }
}