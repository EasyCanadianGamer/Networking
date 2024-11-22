
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public final class WebServer {
    public static void main(String[] argv) throws Exception {
        // Define the ports to listen on
        int[] ports = {5555, 8888};

        // Create a new Selector
        Selector selector = Selector.open();

        // Open ServerSocketChannels and bind to ports
        for (int port : ports) {
            ServerSocketChannel serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false); // Non-blocking mode
            ServerSocket serverSocket = serverChannel.socket();
            serverSocket.bind(new InetSocketAddress(port));
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);
            System.out.println("Listening on port " + port);
        }

        // Create a thread pool with a fixed number of threads
        int threadPoolSize = 50; // Adjust based on expected load
        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        // Infinite loop to listen for incoming connections
        while (true) {
            // Wait for an event
            selector.select();

            // Get list of selection keys with pending events
            Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();

            while (keyIterator.hasNext()) {
                SelectionKey key = keyIterator.next();

                // Remove the key from the selected set; it's being processed
                keyIterator.remove();

                if (key.isAcceptable()) {
                    // Accept the new connection
                    ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
                    SocketChannel socketChannel = serverChannel.accept();

                    if (socketChannel != null) {
                        socketChannel.configureBlocking(true); // Switch to blocking mode for thread handling
                        Socket socket = socketChannel.socket();
                        int port = socket.getLocalPort();

                        System.out.println("Connection established with " + socket.getInetAddress() + " on port " + port);

                        // Determine which request handler to use based on the port
                        Runnable requestHandler;
                        if (port == 5555) {
                            requestHandler = new MovedRequest(socket);
                        } else if (port == 8888) {
                            requestHandler = new HttpRequest(socket);
                        } else {
                            // For any other ports, define default behavior or ignore
                            System.out.println("Received connection on unknown port " + port + ". Closing connection.");
                            socket.close();
                            continue;
                        }

                        // Submit the request handler to the thread pool
                        threadPool.submit(requestHandler);
                    }
                }
            }
        }
    }
}

class HttpRequest implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public HttpRequest(Socket socket) throws Exception {
        this.socket = socket;
    }

    // Implement the run() method of the Runnable interface.
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequest() throws Exception {
        // Get input and output streams
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Set up input stream filters
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        // Get the request line of the HTTP request message
        String requestLine = br.readLine();

        // Display the request line
        System.out.println();
        System.out.println(requestLine);

        // Get and display the header lines
        String headerLine;
        while ((headerLine = br.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Extract the filename from the request line
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken();  // skip over the method, which should be "GET"
        String fileName = tokens.nextToken();

        // Prepend a "." so that file request is within the current directory
        fileName = "." + fileName;

        // Open the requested file
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }

        // Construct the response message
        String statusLine;
        String contentTypeLine;
        String entityBody = null;
        if (fileExists) {
            // HTTP Status Line for a successful request
            statusLine = "HTTP/1.0 200 OK" + CRLF;

            // Content-Type header based on the file's MIME type
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        } else {
            // HTTP Status Line for a file not found
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;

            // Content-Type header for HTML content
            contentTypeLine = "Content-type: text/html" + CRLF;

            // Entity body containing the HTML error message
            entityBody = "<HTML>" +
                    "<HEAD><TITLE>Not Found</TITLE></HEAD>" +
                    "<BODY>Not Found</BODY></HTML>";
        }

        // Send the status line
        os.writeBytes(statusLine);

        // Send the content type line
        os.writeBytes(contentTypeLine);

        // Send a blank line to indicate the end of the header lines
        os.writeBytes(CRLF);

        // Send the entity body
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(entityBody);
        }

        // Close streams and socket
        os.close();
        br.close();
        socket.close();
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception {
        // Construct a 1K buffer to hold bytes on their way to the socket
        byte[] buffer = new byte[1024];
        int bytes;

        // Copy requested file into the socket's output stream
        while ((bytes = fis.read(buffer)) != -1) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName) {
        if (fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if (fileName.endsWith(".gif")) {
            return "image/gif";
        }
        if (fileName.endsWith(".jpeg") || fileName.endsWith(".jpg")) {
            return "image/jpeg";
        }
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".txt")) {
            return "text/plain";
        }
        if (fileName.endsWith(".css")) {
            return "text/css";
        }
        if (fileName.endsWith(".js")) {
            return "application/javascript";
        }
        // Add more MIME types as needed
        return "application/octet-stream";
    }
}

class MovedRequest implements Runnable {
    final static String CRLF = "\r\n";
    Socket socket;

    // Constructor
    public MovedRequest(Socket socket) {
        this.socket = socket;
    }

    // Implement the run() method of the Runnable interface.
    public void run() {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    private void processRequest() throws Exception {
        // Get output stream to send response
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());

        // Construct the response message
        String statusLine = "HTTP/1.1 301 Moved Permanently" + CRLF;
        String locationLine = "Location: http://www.google.com" + CRLF;
        String entityBody = "<HTML>" +
                "<HEAD><TITLE>Moved Permanently</TITLE></HEAD>" +
                "<BODY>Moved Permanently</BODY></HTML>";

        // Send the status line
        os.writeBytes(statusLine);

        // Send the Location header line
        os.writeBytes(locationLine);

        // Send a blank line to indicate the end of the header lines
        os.writeBytes(CRLF);

        // Send the entity body
        os.writeBytes(entityBody);

        // Close streams and socket
        os.close();
        socket.close();
    }
}

