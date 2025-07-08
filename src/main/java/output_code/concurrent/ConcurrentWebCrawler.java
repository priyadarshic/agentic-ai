package output_code.concurrent;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrentWebCrawler {

    private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebCrawler.class);
    private static final int NUM_THREADS = 10;
    private static final int MAX_PAGES_TO_VISIT = 100;
    private static final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
    private static final Set<String> visitedURLs = new HashSet<>();
    private static final ExecutorService executor = Executors.newFixedThreadPool(NUM_THREADS);
    private static final Pattern URL_PATTERN = Pattern.compile("<a href=\"(.*?)\"");
    private static final Semaphore semaphore = new Semaphore(5); // Rate limiting: Max 5 concurrent requests
    private static final String USER_AGENT = "Mozilla/5.0 (compatible; WebCrawler/1.0; +http://www.example.com)"; // Replace with your bot's info

    public static void main(String[] args) throws InterruptedException {
        String startUrl = "https://medium.com/"; // Replace with your start URL
        queue.add(startUrl);

        for (int i = 0; i < NUM_THREADS; i++) {
            executor.execute(new CrawlerTask());
        }

        executor.shutdown();
//        executor.awaitTermination(Long.MAX_VALUE, java.util.concurrent.TimeUnit.NANOSECONDS);
        logger.info("Crawling completed. Visited {} pages.", visitedURLs.size());
    }

    static class CrawlerTask implements Runnable {
        @Override
        public void run() {
            try {
                while (true) {
                    try {
                        if (visitedURLs.size() >= MAX_PAGES_TO_VISIT) {
                            break;
                        }
                        String url = queue.take(); // Blocking operation: waits if queue is empty
                        if (visitedURLs.contains(url)) {
                            continue;
                        }

                        try {
                            semaphore.acquire(); // Acquire a permit before making the request
                            crawl(url);
                        } finally {
                            semaphore.release(); // Release the permit
                        }

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        logger.warn("Crawler task interrupted.");
                        return;
                    }
                }
            } finally {
                // Ensure shutdown even if there's an exception
                if (!executor.isShutdown()) {
                    executor.shutdownNow();
                }
            }
        }

        private void crawl(String url) {
            try {
                if (visitedURLs.add(url)) {
                    logger.info("Crawling: {}", url);
                    String content = readURLContent(url);
                    if (content != null) {
                        extractURLs(url, content);
                    }
                }
            } catch (IOException e) {
                logger.error("Error crawling {}: {}", url, e.getMessage());
            }
        }

        private String readURLContent(String urlStr) throws IOException {
            try {
                URL url = new URL(urlStr);
                URLConnection connection = url.openConnection();
                connection.setRequestProperty("User-Agent", USER_AGENT); // Set user agent
//                connection.setInstanceFollowRedirects(true);
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
                reader.close();
                return content.toString();
            } catch (IOException e) {
                logger.error("Failed to read content from: {} - {}", urlStr, e.getMessage());
                return null;
            }
        }

        private void extractURLs(String baseURL, String content) {
            Matcher matcher = URL_PATTERN.matcher(content);
            while (matcher.find()) {
                String link = matcher.group(1);
                try {
                    URI uri = new URI(link);
                    URI baseUri = new URI(baseURL);
                    URI absoluteUri = baseUri.resolve(uri);
                    URL absoluteURL = absoluteUri.toURL();
                    String absoluteURLString = absoluteURL.toString();

                    if ((absoluteURLString.startsWith("http") || absoluteURLString.startsWith("https")) && !visitedURLs.contains(absoluteURLString)) {
                        queue.add(absoluteURLString);
                    }

                } catch (MalformedURLException | URISyntaxException e) {
                    logger.error("Invalid URL found: {} - {}", link, e.getMessage());
                }

            }
        }
    }
}