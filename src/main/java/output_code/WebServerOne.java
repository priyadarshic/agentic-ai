package output_code;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WebServerOne {

    // Logger for handling application logs
    private static final Logger LOGGER = Logger.getLogger(SimpleWebServer.class.getName());

    // Default port if none is provided
    private static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) throws Exception {
        // Determine the port number.  Use the DEFAULT_PORT if no command line arguments are provided.
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number provided. Using default port: " + DEFAULT_PORT);
            }
        }

        // Define the address and port for the server to listen on.
        InetSocketAddress address = new InetSocketAddress(port);

        // Create an HTTP server instance.  The '0' backlog means use the system default.
        HttpServer server = HttpServer.create(address, 0);

        // Create a context for handling requests to the root path ("/").
        // The 'MyHandler' class will be responsible for processing these requests.
        server.createContext("/", new MyHandler());

        // Use a ThreadPoolExecutor for handling incoming requests concurrently.
        // This allows for better control over thread management.
        int poolSize = Runtime.getRuntime().availableProcessors(); // Use number of available cores
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(poolSize);
        server.setExecutor(executor);

        // Start the server and begin listening for incoming client connections.
        server.start();

        // Print a message to the console indicating that the server has started.
        System.out.println("Server started on port " + port);
        LOGGER.info("Log: Server started on port " + port); // Log server startup
    }

    // A handler class that implements the HttpHandler interface.
    // This class will be responsible for processing incoming HTTP requests.
    static class MyHandler implements HttpHandler {

        // This method will be called when a request is received for the registered context.
        @Override
        public void handle(HttpExchange exchange) {
            try {
                // Prepare the response message. Using StringBuilder for efficiency.
                StringBuilder response = new StringBuilder();
                response.append("<!DOCTYPE html>");
                response.append("<html>");
                response.append("<head><title>Server Information</title></head>");
                response.append("<body>");
                response.append("<h1>Server Information</h1>");
                response.append("<p>Server Address: ").append(exchange.getLocalAddress().toString()).append("</p>");
                response.append("<p>Protocol: ").append(exchange.getProtocol()).append("</p>");
                response.append("<p>Request Method: ").append(exchange.getRequestMethod()).append("</p>");
                response.append("<p>Request Headers: ").append(exchange.getRequestHeaders()).append("</p>");
                response.append("<p>Remote Address: ").append(exchange.getRemoteAddress()).append("</p>");
                response.append("<p>Response Headers: ").append(exchange.getResponseHeaders()).append("</p>");
                response.append("</body>");
                response.append("</html>");




                String responseString = response.toString();

                // Set the response headers.  In this case, we are indicating that the response
                // will be an HTML document using UTF-8 encoding.
                exchange.getResponseHeaders().set("Content-Type", "text/html");

                // Set the HTTP response code to 200 (OK).
                exchange.sendResponseHeaders(200, responseString.getBytes().length);

                // Get the output stream from the HttpExchange object.
                OutputStream os = exchange.getResponseBody();

                // Write the response message to the output stream.
                os.write(responseString.getBytes());

                // Close the output stream to signal the end of the response.
                os.close();

                LOGGER.info("Request handled successfully for " + exchange.getRemoteAddress());

            } catch (IOException e) {
                // Log the error
                LOGGER.log(Level.SEVERE, "Error handling request", e);
                // Send an error response to the client
                String errorMessage = "<h1>Internal Server Error</h1><p>An error occurred while processing your request.</p>";
                try {
                    exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                    exchange.sendResponseHeaders(500, errorMessage.getBytes().length);
                    OutputStream os = exchange.getResponseBody();
                    os.write(errorMessage.getBytes());
                    os.close();
                } catch (IOException ex) {
                    // If we can't even send the error message, log it and give up
                    LOGGER.log(Level.SEVERE, "Failed to send error response", ex);
                }
            }
        }
    }
}
