package output_code.caching.LFU;

import org.junit.jupiter.api.Test;
import output_code.caching.LRU.LRUCache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// JUnit tests
public class LFUCacheTest {

    @Test
    public void testLargeCachePerformance() {
        int cacheSize = 100000;
        LFUCache<Integer, String> cache = new LFUCache<>(cacheSize);

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
    public void testBasicOperations() {
        LFUCache<Integer, Integer> cache = new LFUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(1, cache.get(1));
        cache.put(3, 3);
        assertNull(cache.get(2));
        assertEquals(3, cache.get(3));
        cache.put(4, 4);
        assertNull(cache.get(1));
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
    }

    @Test
    public void testCapacityZero() {
        LFUCache<Integer, Integer> cache = new LFUCache<>(0);
        cache.put(1, 1);
        assertNull(cache.get(1));
    }

    @Test
    public void testReinsertion() {
        LFUCache<Integer, Integer> cache = new LFUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1);
        cache.put(3, 3);
        assertNull(cache.get(2));
        cache.put(4, 4);
        assertNull(cache.get(3));
        assertEquals(1, cache.get(1));
        assertEquals(4, cache.get(4));
    }

    @Test
    public void testSameValue() {
        LFUCache<Integer, Integer> cache = new LFUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(1, 10);
        assertEquals(10, cache.get(1));
    }

    @Test
    public void testEvictionWithSameFrequency() {
        LFUCache<Integer, Integer> cache = new LFUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.get(1); // Frequency of 1 becomes 2
        cache.put(3, 3); // Evicts 2 since 2 has frequency 1 and 1 has frequency 2
        assertNull(cache.get(2));
    }
}