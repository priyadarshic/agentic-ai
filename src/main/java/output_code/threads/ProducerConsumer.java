package output_code.threads;

import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProducerConsumer {

    private static final int BUFFER_SIZE = 5;
    private static final int NUM_PRODUCERS = 2;
    private static final int NUM_CONSUMERS = 2;

    private static final BoundedBuffer buffer = new BoundedBuffer(BUFFER_SIZE);
    private static final Logger logger = Logger.getLogger(ProducerConsumer.class.getName());

    public static void main(String[] args) throws InterruptedException {

        Thread[] producerThreads = new Thread[NUM_PRODUCERS];
        Thread[] consumerThreads = new Thread[NUM_CONSUMERS];

        for (int i = 0; i < NUM_PRODUCERS; i++) {
            producerThreads[i] = new Thread(new Producer(buffer), "Producer-" + (i + 1));
            producerThreads[i].start();
        }

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumerThreads[i] = new Thread(new Consumer(buffer), "Consumer-" + (i + 1));
            consumerThreads[i].start();
        }

        // Let the threads run for a while
        Thread.sleep(5000);

        // Interrupt the threads to stop them
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            producerThreads[i].interrupt();
        }

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            consumerThreads[i].interrupt();
        }

        // Wait for the threads to finish
        for (int i = 0; i < NUM_PRODUCERS; i++) {
            try {
                producerThreads[i].join();
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Thread join interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        for (int i = 0; i < NUM_CONSUMERS; i++) {
            try {
                consumerThreads[i].join();
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Thread join interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Producer Consumer Simulation Completed.");
    }
}


class BoundedBuffer {
    private final Object[] buffer;
    private final int capacity;
    private int in; // index for next item to be put into buffer
    private int out; // index for next item to be taken out of buffer
    private int count; // number of items in the buffer

    private final Semaphore mutex = new Semaphore(1); // controls access to critical section
    private final Semaphore empty; // counts number of empty slots
    private final Semaphore full; // counts number of full slots

    private static final Logger logger = Logger.getLogger(BoundedBuffer.class.getName());


    public BoundedBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new Object[capacity];
        this.in = 0;
        this.out = 0;
        this.count = 0;
        this.empty = new Semaphore(capacity);
        this.full = new Semaphore(0);
    }


    public void put(Object item) throws InterruptedException {
        empty.acquire(); // decrement empty semaphore
        mutex.acquire(); // enter critical section
        try {
            buffer[in] = item;
            in = (in + 1) % capacity;
            count++;
            logger.log(Level.INFO, "{0} produced: {1}, Count: {2}", new Object[]{Thread.currentThread().getName(), item, count});
        } finally {
            mutex.release(); // leave critical section
            full.release(); // increment full semaphore
        }
    }

    public Object take() throws InterruptedException {
        full.acquire(); // decrement full semaphore
        mutex.acquire(); // enter critical section
        try {
            Object item = buffer[out];
            buffer[out] = null; // Important: to help garbage collection
            out = (out + 1) % capacity;
            count--;
            logger.log(Level.INFO, "{0} consumed: {1}, Count: {2}", new Object[]{Thread.currentThread().getName(), item, count});
            return item;
        } finally {
            mutex.release(); // leave critical section
            empty.release(); // increment empty semaphore
        }
    }
}


class Producer implements Runnable {
    private final BoundedBuffer buffer;
    private final Random random = new Random();
    private static final Logger logger = Logger.getLogger(Producer.class.getName());


    public Producer(BoundedBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Integer item = random.nextInt(100); // Produce a random integer
                buffer.put(item);
                Thread.sleep(random.nextInt(500)); // Simulate some work
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            logger.log(Level.INFO, "{0} interrupted.", Thread.currentThread().getName());
        }  catch (Exception e) {
            logger.log(Level.SEVERE, "Producer encountered an unexpected error", e);
        }
    }
}


class Consumer implements Runnable {
    private final BoundedBuffer buffer;
    private final Random random = new Random();
    private static final Logger logger = Logger.getLogger(Consumer.class.getName());


    public Consumer(BoundedBuffer buffer) {
        this.buffer = buffer;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                Object item = buffer.take();
                Thread.sleep(random.nextInt(500)); // Simulate some work
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Restore interrupted state
            logger.log(Level.INFO, "{0} interrupted.", Thread.currentThread().getName());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Consumer encountered an unexpected error", e);
        }
    }
}
