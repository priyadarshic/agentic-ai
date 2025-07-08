package output_code.caching.LFU;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class LFUCache<K, V> {

    private final int capacity;
    private final Map<K, V> cache;
    private final Map<K, Integer> counts;
    private final Map<Integer, LinkedHashSet<K>> lists;
    private int minFrequency;

    /**
     * Constructs an LFUCache with a given capacity.
     *
     * @param capacity the maximum number of entries the cache can hold.
     */
    public LFUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new HashMap<>(capacity);
        this.counts = new HashMap<>(capacity);
        this.lists = new HashMap<>();
        this.minFrequency = 1;
    }

    /**
     * Retrieves a value from the cache.
     *
     * @param key the key whose associated value is to be returned.
     * @return the value to which the specified key is mapped, or -1 if this cache contains no mapping for the key.
     */
    public V get(K key) {
        if (!cache.containsKey(key)) {
            return null;
        }

        int count = counts.get(key);
        counts.put(key, count + 1);
        lists.get(count).remove(key);

        // Handle the edge case where lists.get(count) returns null after removing the key
        if (lists.get(count).isEmpty()) {
            if (count == minFrequency) {
                minFrequency++;
            }
            lists.remove(count); // Remove empty list
        }

        if (!lists.containsKey(count + 1)) {
            lists.put(count + 1, new LinkedHashSet<>());
        }
        lists.get(count + 1).add(key);

        return cache.get(key);
    }

    /**
     * Associates the specified value with the specified key in this cache.
     *
     * @param key   the key with which the specified value is to be associated.
     * @param value the value to be associated with the specified key.
     */
    public void put(K key, V value) {
        if (capacity <= 0) {
            return;
        }

        if (cache.containsKey(key)) {
            cache.put(key, value);
            get(key);
            return;
        }

        if (cache.size() == capacity) {
            K evict = lists.get(minFrequency).iterator().next();
            lists.get(minFrequency).remove(evict);
            cache.remove(evict);
            counts.remove(evict);
            if (lists.get(minFrequency).isEmpty()) {
                lists.remove(minFrequency);
            }
        }

        cache.put(key, value);
        counts.put(key, 1);
        minFrequency = 1;

        if (!lists.containsKey(1)) {
            lists.put(1, new LinkedHashSet<>());
        }
        lists.get(1).add(key);
    }



    // Performance test (example)
    public static void main(String[] args) {
        int capacity = 1000;
        int numOperations = 100000;
        LFUCache<Integer, Integer> cache = new LFUCache<>(capacity);

        long startTime = System.nanoTime();
        for (int i = 0; i < numOperations; i++) {
            cache.put(i % 100, i);
            cache.get(i % 100);
        }
        long endTime = System.nanoTime();

        long duration = (endTime - startTime) / 1000000;  // in milliseconds
        System.out.println("Performance test: " + numOperations + " operations took " + duration + " ms");
    }
}
