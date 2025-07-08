package output_code.caching.LRU;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

//Unit Tests
public class LRUCacheTest {


    @Test
    public void testBasicLRU() {
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        assertEquals("one", cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));

        cache.put(4, "four"); // This should evict 1
        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));

        cache.get(2); //move 2 to the top

        cache.put(5, "five"); //This should evict 3

        assertNull(cache.get(3));
        assertEquals("two", cache.get(2));
        assertEquals("four", cache.get(4));
        assertEquals("five", cache.get(5));

    }

    @Test
    public void testUpdateExistingKey() {
        LRUCache<String, Integer> cache = new LRUCache<>(2);
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("a", 3);

        assertEquals(Integer.valueOf(3), cache.get("a"));
        assertEquals(Integer.valueOf(2), cache.get("b"));
        assertEquals(2, cache.size());
    }

    @Test
    public void testCacheFull() {
        LRUCache<Integer, String> cache = new LRUCache<>(2);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three"); // Evicts 1

        assertNull(cache.get(1));
        assertEquals("two", cache.get(2));
        assertEquals("three", cache.get(3));
    }

    @Test
    public void testCapacityZero() {
        LRUCache<Integer, String> cache = new LRUCache<>(0);
        cache.put(1, "one");
        assertNull(cache.get(1));
    }

    @Test
    public void testNullKey() {
        LRUCache<String, Integer> cache = new LRUCache<>(1);
        cache.put(null, 10);
        assertEquals(Integer.valueOf(10), cache.get(null));
    }

    @Test
    public void testNullValue() {
        LRUCache<Integer, String> cache = new LRUCache<>(1);
        cache.put(1, null);
        assertNull(cache.get(1));
    }

    @Test
    public void testMultipleAccessesSameKey() {
        LRUCache<Integer, String> cache = new LRUCache<>(3);
        cache.put(1, "one");
        cache.put(2, "two");
        cache.put(3, "three");

        cache.get(1);
        cache.get(1);
        cache.get(1);

        cache.put(4, "four"); // Should evict 2

        assertNull(cache.get(2));
        assertEquals("one", cache.get(1));
        assertEquals("three", cache.get(3));
        assertEquals("four", cache.get(4));
    }
}
