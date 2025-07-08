package output_code;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WebCrawler {

    private static final Logger LOGGER = Logger.getLogger(WebCrawler.class.getName());
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36";
    private static final int TIMEOUT = 5000; // Milliseconds

    public static void crawl(String startUrl, int maxPagesToCrawl) {
        crawl(startUrl, maxPagesToCrawl, true);
    }

    public static void crawl(String startUrl, int maxPagesToCrawl, boolean respectRobotsTxt) {
        Queue<String> queue = new LinkedList<>();
        Set<String> visited = new HashSet<>();
        String domain = getDomainName(startUrl);

        if (respectRobotsTxt) {
            RobotsTxtInfo robotsTxtInfo = readRobotsTxt(startUrl);
//            if (robotsTxtInfo != null && !robotsTxtInfo.isAllowed("/")) {
//                LOGGER.warning("Crawling is disallowed by robots.txt for path: /");
//                return;
//            }
        }
        queue.add(startUrl);
        visited.add(startUrl);

        while (!queue.isEmpty() && visited.size() <= maxPagesToCrawl) {
            String url = queue.remove();
            if (respectRobotsTxt) {
                RobotsTxtInfo robotsTxtInfo = readRobotsTxt(url);
                if (robotsTxtInfo != null && !robotsTxtInfo.isAllowed(url)) {
                    LOGGER.info("Skipping URL " + url + " due to robots.txt");
                    continue;
                }
            }

            try {
                HttpResponse response = fetchWithHttpClient(url);
                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode >= 300 && statusCode < 400) {
                    Header locationHeader = response.getFirstHeader("Location");
                    if (locationHeader != null) {
                        String redirectUrl = locationHeader.getValue();
                        LOGGER.info("Redirecting from " + url + " to " + redirectUrl);
                        if (!visited.contains(redirectUrl)) {
                            queue.add(redirectUrl);
                            visited.add(redirectUrl);
                        }
                        continue;
                    }
                }


                String html = EntityUtils.toString(response.getEntity());
                LOGGER.info("Crawling URL: " + url + " (Status: " + statusCode + ")");


                Document doc = Jsoup.parse(html, url);
                Elements links = doc.select("a[href]");


                for (Element link : links) {
                    String newUrl = link.attr("abs:href"); // Use abs:href to resolve relative URLs
                    if (newUrl.isEmpty()) {
                        continue;
                    }

                    if (isSameDomain(newUrl, domain) && !visited.contains(newUrl)) {
                        queue.add(newUrl);
                        visited.add(newUrl);
                    } else {
                        LOGGER.fine("Skipping external URL: " + newUrl);
                    }
                }

            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Error crawling " + url + ": " + e.getMessage(), e);
            }
        }

        System.out.println("Crawled " + visited.size() + " pages.");
    }


    private static HttpResponse fetchWithHttpClient(String url) throws IOException {
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(RequestConfig.custom()
                .setConnectTimeout(TIMEOUT)
                .setSocketTimeout(TIMEOUT)
                .setRedirectsEnabled(true)
                .build()).build();
        HttpGet request = new HttpGet(url);
        request.setHeader("User-Agent", USER_AGENT);
        return client.execute(request);
    }


    private static String getDomainName(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        } catch (URISyntaxException e) {
            LOGGER.log(Level.SEVERE, "Error getting domain from URL: " + url, e);
            return null;
        }
    }


    private static boolean isSameDomain(String url, String domain) {
        String urlDomain = getDomainName(url);
        return urlDomain != null && urlDomain.equals(domain);
    }

    private static RobotsTxtInfo readRobotsTxt(String baseUrl) {
        try {
            String robotsTxtUrl = baseUrl + "/robots.txt";
            HttpResponse response = fetchWithHttpClient(robotsTxtUrl);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                String content = EntityUtils.toString(response.getEntity());
                return new RobotsTxtInfo(content);
            } else {
                LOGGER.info("robots.txt not found or error fetching, status code: " + statusCode);
                return null;
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error reading robots.txt for " + baseUrl + ": " + e.getMessage(), e);
            return null;
        }
    }


    public static void main(String[] args) {
        // Set logging level
        LOGGER.setLevel(Level.INFO);
        crawl("https://emanual.robotis.com/", 100);
//        crawl("https://www.medium.com/", 100);


    }

    static class RobotsTxtInfo {
        private final Set<String> disallowedPaths = new HashSet<>();

        RobotsTxtInfo(String content) {
            String[] lines = content.split("\\r?\\n");
            boolean userAgentSpecific = false; // Flag to track if we're inside a User-agent block

            for (String line : lines) {
                line = line.trim();
                if (line.toLowerCase().startsWith("user-agent:")) {
                    String userAgent = line.substring("user-agent:".length()).trim().toLowerCase();
                    if (userAgent.equals("*") || userAgent.contains("crawler")) { // Simple check if it's a generic or crawler-specific rule
                        userAgentSpecific = true;
                    } else {
                        userAgentSpecific = false; // Reset the flag
                    }
                } else if (userAgentSpecific && line.toLowerCase().startsWith("disallow:")) {
                    String path = line.substring("disallow:".length()).trim();
                    disallowedPaths.add(path);
                }
            }
        }

        boolean isAllowed(String url) {
            try {
                URI uri = new URI(url);
                String path = uri.getPath();

                for (String disallowedPath : disallowedPaths) {
                    if (path.startsWith(disallowedPath)) {
                        return false;
                    }
                }
                return true;
            } catch (URISyntaxException e) {
                LOGGER.log(Level.WARNING, "Error parsing URL: " + url, e);
                return true; // If there's an error parsing the URL, assume it's allowed.
            }
        }
    }
}