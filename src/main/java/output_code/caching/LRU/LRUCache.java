package output_code.caching.LRU;

import org.junit.Test;
import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
//import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

public class LRUCache<K, V> {

    private final int capacity;
    private final Map<K, V> cache;

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<K, V>(capacity, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                return size() > capacity;
            }
        };
    }

    public V get(K key) {
        return cache.get(key);
    }

    public void put(K key, V value) {
        cache.put(key, value);
    }

    public int size() {
        return cache.size();
    }


    public static void main(String[] args) {
        // Example Usage:
        LRUCache<Integer, String> lruCache = new LRUCache<>(3);
        lruCache.put(1, "Value 1");
        lruCache.put(2, "Value 2");
        lruCache.put(3, "Value 3");

        System.out.println("Value for key 1: " + lruCache.get(1)); // Output: Value 1
        lruCache.put(4, "Value 4"); // This will evict key 2

        System.out.println("Value for key 2: " + lruCache.get(2)); // Output: null (evicted)
        System.out.println("Value for key 4: " + lruCache.get(4)); // Output: Value 4
    }
}
