package output_code;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * SimpleWebServer is a basic HTTP server that listens on a specified port and
 * responds to requests with a simple HTML page.
 */
public class SimpleWebServer {

    private static final String HTML_RESPONSE = "<html><body><h1>Hello, World!</h1><p>This is a simple web server example, created through Gemini AI.</p></body></html>";
    private static final String ERR_RESPONSE = "<html><body><h1>Hello, World!</h1><p>INTERNAL Server ERROR.</p></body></html>";

    /**
     * Main method to start the web server.
     *
     * @param args Command line arguments (not used).
     * @throws Exception if an error occurs during server creation or startup.
     */
    public static void main(String[] args) throws Exception {
        // Define the port number the server will listen on. 8080 is a common choice.
        int port = 8080;

        // Create an HTTP server that binds to the specified port.
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Create a context for the server. This maps a URI path to a handler.
        // In this case, any request to the root path "/" will be handled by the MyHandler class.
        server.createContext("/", new MyHandler());

        // Set an executor for the server. This determines how requests are handled concurrently.
        // Using null defaults to the system's default executor, which uses a system default executor.
        // Be aware that the system default executor has a finite thread pool, and may lead to resource contention
        // under heavy load. For production environments, consider using a custom executor with a configured thread pool.
        server.setExecutor(null); // creates a default executor

        // Start the server. The server will now listen for incoming connections.
        server.start();

        // Print a message to the console indicating that the server has started.
        System.out.println("Server started on port " + port);
    }

    /**
     * MyHandler is an inner class that implements the HttpHandler interface.
     * It's responsible for handling incoming HTTP requests.
     */
    static class MyHandler implements HttpHandler {
        /**
         * Handles incoming HTTP requests.
         *
         * @param exchange The HttpExchange object representing the request and response.
         * @throws IOException if an I/O error occurs while handling the request.
         */
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // The handle method is called whenever a request is received on the registered context.

                // Set the response headers. We indicate that the response is an HTML document.
                exchange.getResponseHeaders().set("Content-Type", "text/html");

                // Send the HTTP status code 200 (OK) to the client.
                exchange.sendResponseHeaders(200, HTML_RESPONSE.getBytes().length);

                // Get the output stream to write the response body.
                OutputStream os = exchange.getResponseBody();

                // Write the response message to the output stream.
                os.write(HTML_RESPONSE.getBytes());

                // Close the output stream. This is important to signal the end of the response.
                os.close();
            } catch (IOException e) {
                // Exception handling to prevent server crashes due to client-side issues or network problems.
                System.err.println("Error handling request: " + e.getMessage());
            }
        }
    }
}