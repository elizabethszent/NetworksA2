import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * The clientthread class implements the Runnable interface, enabling each instance to run in its own thread.
 * This class handles incoming client requests, processes HTTP GET requests, serves files, and sends appropriate
 * HTTP responses (including error handling).
 */
class clientthread implements Runnable {

    private final Socket clientSocket;
    private final int requsttimeout = 5000;

    /**
     * Constructor that initializes the client thread with the connected client socket.
     * 
     * @param clientSocket The socket associated with the connected client.
     */
    clientthread(Socket clientSocket) {
        this.clientSocket = clientSocket;

    }

    /**
     * Main logic of the client thread, executed when the thread is started.
     * Processes incoming HTTP GET requests from the client, validates the requests, and serves the requested file.
     * If the request is invalid or the file is not found, appropriate HTTP error responses are sent.
     */
    public void run() {
        try{
        OutputStream outputstream = clientSocket.getOutputStream();// Get the output stream to send data to the client
        try {
            clientSocket.setSoTimeout(requsttimeout);// Set a timeout for client request (5 seconds)
            InputStream inputStream = clientSocket.getInputStream(); // Input stream to receive data from the client

            // Use BufferedReader to read the input stream line by line (client's request)
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String request = reader.readLine();
            System.out.println("Request: " + request);//print request
            
            //to print out whole request line and header lines)
            String currentRequestLine = reader.readLine(); 
            while(!(currentRequestLine.equals(""))){
                System.out.println("Request: " + currentRequestLine);//print request
                currentRequestLine = reader.readLine();
            }

            // Split the request line into components (e.g., GET /path HTTP/1.1)
            String[] splitrequest = request.split(" ");
            splitrequest[1] = splitrequest[1].replace("/", "");

            // If the request is null, return (no processing needed)
            if (request == null){
                return;
            }
            // Check if the request is a valid HTTP GET request
            else if (! (splitrequest.length == 3  && splitrequest[0].equals("GET") && splitrequest[2].equals("HTTP/1.1"))){
                sendErrorResponse(outputstream, 400, "Bad Request"); // Send 400 Bad Request if the format is invalid
                return;
            }
            // Check if the requested file exists on the server
            else if (!(new File(splitrequest[1]).isFile())){
                sendErrorResponse(outputstream, 402, "Not Found");// Send 404 Not Found if the file doesn't exist
                return;
            }
            // File exists, proceed
            else{
                    File file = new File(splitrequest[1]);
                    sendokresponse(outputstream, file);// Send 200 OK response
                    // Prepare to write the file as binary
                    BufferedOutputStream bos = new BufferedOutputStream(outputstream);
                    byte[] buffer = new byte[4096]; //buffer for reading chunks of data
                    int bytesRead;//variable to store the number of bytes read in each iteration
                    int totalBytesRead = 0; //track total number of bytes read
                    InputStream fileInputStream = new FileInputStream(file); // Open the file

                    // Retrieve the file's content length (size in bytes)
                    int contentlength = Integer.parseInt(ServerUtils.getContentLength(file));
                    // Read the file and write it to the client in chunks of 4096 bytes
                    while (totalBytesRead < contentlength && (bytesRead = fileInputStream.read(buffer)) != -1) {
                        bos.write(buffer, 0, bytesRead); // Send the bytes to the client
                        totalBytesRead += bytesRead;// Update total bytes sent
                }
                bos.flush();//ensure all data is written to the output stream

                // Close the socket and streams
                inputStream.close();
                clientSocket.close();
                outputstream.close();
            }
        }catch (SocketTimeoutException e) {
            // If the client request takes longer than the timeout, send 408 Request Timeout
            sendErrorResponse(outputstream, 408,"Request Timeout");
            return;

        }
        catch(IOException e){
            // Handle any I/O errors during request processing
        }
    }
    catch(IOException e){
        //Handle any errors during socket output stream acquisition
    }
    }

     /**
     * Sends an HTTP error response to the client. The response includes the error code and an appropriate status message.
     * 
     * @param outputstream The output stream to write the response to.
     * @param errornum     The HTTP status code (e.g., 400, 404, 408).
     * @param statusphrase The HTTP status phrase (e.g., "Bad Request", "Not Found", "Request Timeout").
     */
    private void sendErrorResponse(OutputStream outputstream, int errornum, String statusphrase) {

        try {
            // Use BufferedWriter to write the error response
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputstream));
            // Form the HTTP error response message
            String errormessage = "HTTP/1.1 " + errornum +" "+ statusphrase + "\r\n" + "Date: " + ServerUtils.getCurrentDate() +"\r\n " + "Server: SpookySeason\r\n" + "Connection: close\r\n\r\n" ; //HTTP/1.1 status-code status-phrase
            System.out.println(errormessage); //print out errormessage
            // Write and flush the error message to the client
            bufferedWriter.write(errormessage);
            bufferedWriter.flush();
            outputstream.flush();
            
        } catch (Exception e) {
            // Handle any exceptions during error response sending
        }
    }

     /**
     * Sends a successful (HTTP 200 OK) response to the client, including file metadata such as content length and type.
     * 
     * @param outputstream The output stream to write the response to.
     * @param file         The file being served to the client.
     */
    private void sendokresponse(OutputStream outputstream,File file) {

        try {
            // Use BufferedWriter to write the success response
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputstream));

            // Form the HTTP 200 OK response message with file details
            String message = "HTTP/1.1 200 OK\r\n" + "Date: " + ServerUtils.getCurrentDate() +"\r\n" + "Server: SpookySeason\r\n" + "Last-Modified: " + ServerUtils.getLastModified(file) 
            + "\r\n" + "Content-Length: " + ServerUtils.getContentLength(file) + "\r\n" + "Content-Type: " + ServerUtils.getContentType(file) + "\r\n" + "Connection: close\r\n\r\n";
            System.out.println(message);
            // Write and flush the response message to the client
            bufferedWriter.write(message);//print out errormessage
            bufferedWriter.flush();
            outputstream.flush();

            
            }  catch (Exception e) {
             // Handle any exceptions during successful response sending
        }
    }

}
