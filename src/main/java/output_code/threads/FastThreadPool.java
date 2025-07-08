package output_code.threads;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FastThreadPool {

    private final BlockingQueue<Runnable> taskQueue;
    private final Thread[] workers;
    private final AtomicInteger workerCount = new AtomicInteger(0);
    private volatile boolean isShutdown = false;
    private final CountDownLatch shutdownLatch;
    private final Logger logger = Logger.getLogger(FastThreadPool.class.getName());

    public FastThreadPool(int poolSize) {
        this(poolSize, new LinkedBlockingQueue<>()); // Default to unbounded queue
    }

    public FastThreadPool(int poolSize, BlockingQueue<Runnable> taskQueue) {
        if (poolSize <= 0) {
            throw new IllegalArgumentException("Pool size must be greater than 0");
        }
        this.taskQueue = taskQueue;
        workers = new Thread[poolSize];
        this.shutdownLatch = new CountDownLatch(poolSize);

        for (int i = 0; i < poolSize; i++) {
            workers[i] = new WorkerThread();
            workers[i].start();
            workerCount.incrementAndGet();
        }
    }

    public Future<?> submit(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Thread pool is shut down");
        }
        FutureTask<?> futureTask = new FutureTask<>(task, null);
        try {
            taskQueue.put(futureTask);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
        return futureTask;
    }

    public void execute(Runnable task) {
        if (isShutdown) {
            throw new IllegalStateException("Thread pool is shut down");
        }
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void shutdown() {
        isShutdown = true;
        for (int i = 0; i < workers.length; i++) {
            workers[i].interrupt(); // Interrupt idle workers
        }

        try {
            shutdownLatch.await(); // Wait for all threads to complete
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public int getPoolSize() {
        return workers.length;
    }

    private class WorkerThread extends Thread {
        @Override
        public void run() {
            try {
                while (!isShutdown) {
                    Runnable task = null;
                    try {
                        task = taskQueue.poll(); // poll with timeout
                        if (task != null) {
                            task.run();
                        } else if (isShutdown && taskQueue.isEmpty()) {
                            break;
                        }
                    } catch (Exception e) {
                        // Handle exceptions during task execution
                        logger.log(Level.SEVERE, "Exception during task execution", e);
                    }
                }
            } finally {
                shutdownLatch.countDown();
                workerCount.decrementAndGet();
            }
        }
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        int poolSize = 3;
        FastThreadPool threadPool = new FastThreadPool(poolSize);

        Future<?>[] futures = new Future[10];

        for (int i = 0; i < 10; i++) {
            final int taskNumber = i;
            futures[i] = threadPool.submit(() -> {
                System.out.println("Task " + taskNumber + " executed by " + Thread.currentThread().getName());
                try {
                    Thread.sleep(100); // Simulate some work
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        // Wait for all tasks to complete
        for (Future<?> future : futures) {
            if (future != null) {
                future.get(); // Wait for the task to complete; throws exception if the task did
            }
        }

        threadPool.shutdown();
        System.out.println("Thread pool shut down.");
    }
}
