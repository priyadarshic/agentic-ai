package output_code.caching.concurrentLRU;

import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentLRUCache<K, V> {

    private final int capacity;
    private final ConcurrentHashMap<K, V> cache;
    private final ConcurrentLinkedDeque<K> queue;
    private final Lock lock = new ReentrantLock();

    public ConcurrentLRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new ConcurrentHashMap<>(capacity);
        this.queue = new ConcurrentLinkedDeque<>();
    }

    public V get(K key) {
        V value = cache.get(key);
        if (value != null) {
            lock.lock();
            try {
                queue.remove(key);
                queue.addLast(key);
            } finally {
                lock.unlock();
            }
        }
        return value;
    }

    public void put(K key, V value) {
        lock.lock();
        try {
            if (cache.containsKey(key)) {
                cache.put(key, value);
                queue.remove(key);
                queue.addLast(key);
                return;
            }
            if (cache.size() >= capacity) {
                K lruKey = queue.poll();
                if (lruKey != null) {
                    cache.remove(lruKey);
                }
            }
            cache.put(key, value);
            queue.addLast(key);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        return cache.size();
    }

    public void clear() {
        cache.clear();
    }

    public static void main(String[] args) throws InterruptedException {
        // Basic Test Cases
        ConcurrentLRUCache<Integer, String> cache = new ConcurrentLRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        System.out.println("Cache size: " + cache.size());
        System.out.println("Value for key 1: " + cache.get(1));
        System.out.println("Value for key 2: " + cache.get(2));
        System.out.println("Value for key 3: " + cache.get(3));

        cache.put(4, "four"); // Evicts key 1

        System.out.println("Value for key 1 after eviction: " + cache.get(1));
        System.out.println("Cache size after eviction: " + cache.size());

        cache.put(2, "two-updated");
        System.out.println("Value for key 2 after update: " + cache.get(2));
        cache.get(4);
        cache.put(5, "five");
        System.out.println("Value for key 3 after key 5 is added: " + cache.get(3));

        cache.clear();
        System.out.println("Cache size after clear: " + cache.size());

        // Concurrent Test
        int numThreads = 10;
        int numOperations = 1000;
        int cacheSize = 100;

        ConcurrentLRUCache<Integer, Integer> concurrentCache = new ConcurrentLRUCache<>(cacheSize);
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executorService.submit(() -> {
                Random random = new Random();
                for (int j = 0; j < numOperations; j++) {
                    int key = random.nextInt(cacheSize);
                    int value = key * 2;

                    if (random.nextDouble() < 0.7) {
                        concurrentCache.put(key, value);
                    } else {
                        concurrentCache.get(key);
                    }
                }
            });
        }

        executorService.shutdown();
//        executorService.awaitTermination(1, TimeUnit.MINUTES);

        System.out.println("Concurrent Cache size after concurrent operations: " + concurrentCache.size());

        // Concurrent test to specifically test the concurrency aspects of the cache
        concurrentTest(10, 100, 50);

        // Performance Test
        performanceTest(100000, 1000);
    }

    static void concurrentTest(int numThreads, int numOperations, int cacheSize) throws InterruptedException {
        ConcurrentLRUCache<Integer, String> cache = new ConcurrentLRUCache<>(cacheSize);
        ExecutorService executorService = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executorService.submit(() -> {
                for (int j = 0; j < numOperations; j++) {
                    int key = j % (cacheSize / 2); // Reduced key range for contention
                    String value = "Thread-" + threadId + "-Value-" + j;
                    cache.put(key, value);
                    cache.get(key);
                }
            });
        }

        executorService.shutdown();
//        executorService.awaitTermination(1, TimeUnit.MINUTES);


        System.out.println("Concurrent Test Completed. Cache Size: " + cache.size());
    }

    static void performanceTest(int numOperations, int cacheSize) {
        ConcurrentLRUCache<Integer, String> cache = new ConcurrentLRUCache<>(cacheSize);
        Random random = new Random();

        long startTime = System.currentTimeMillis();
        int hits = 0;
        int misses = 0;

        for (int i = 0; i < numOperations; i++) {
            int key = random.nextInt(numOperations);
            if (random.nextDouble() < 0.7) {
                cache.put(key, "value" + i);
            } else {
                if (cache.get(key) != null) {
                    hits++;
                } else {
                    misses++;
                }
            }
        }
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;
        double hitRate = (double) hits / (hits + misses);

        System.out.println("Performance Test: " + numOperations + " operations with cache size " + cacheSize + " took " + duration + " milliseconds");
        System.out.println("Hit Rate: " + hitRate);
        System.out.println("Miss Rate: " + (1 - hitRate));
    }
}