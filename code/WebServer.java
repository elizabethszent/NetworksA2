

/**
 * WebServer Class
 * 
 * Implements a multi-threaded web server
 * supporting non-persistent connections.
 * @author Elizabeth Szentmiklossy
 * @version 2024
 */


import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.*;
import java.util.logging.*;


public class WebServer extends Thread {
	// global logger object, configures in the driver class
	private static final Logger logger = Logger.getLogger("WebServer");

	private boolean shutdown = false; // shutdown flag
	private int Port;
	private ExecutorService executor = Executors.newCachedThreadPool(); // Create a cached thread pool to handle client requests concurrently
	
    /**
     * Constructor to initialize the web server
     * 
     * @param port 	Server port at which the web server listens > 1024
	 * @param root	Server's root file directory
	 * @param timeout	Idle connection timeout in milli-seconds
     * 
     */
	public WebServer(int port, String root, int timeout){
		this.Port = port;
	}

	
    /**
	 * Main method in the web server thread.
	 * The web server remains in listening mode 
	 * and accepts connection requests from clients 
	 * until it receives the shutdown signal.
	 * 
     */
  
	public void run(){
		try {		
		ServerSocket serverSocket = new ServerSocket(Port);// Create a ServerSocket to listen on the specified port
		serverSocket.setSoTimeout(1000); // Set a timeout for the server socket to 1000ms, allowing it to periodically check the shutdown condition
		

		// Loop to accept client connections until a shutdown signal is received
		while (!shutdown){
			try {
				// Accept a client connection (blocking call) and pass it to a new client thread for processing
				Socket clientSocket = serverSocket.accept(); 
				System.out.println("Port: " + clientSocket.getLocalPort() +" Address: " + clientSocket.getInetAddress());
				executor.execute(new clientthread(clientSocket));
			
				
			}  
			catch (SocketTimeoutException e) {

			}
			catch (Exception e) {
				e.printStackTrace();
				this.shutdown();
			}


		}
	} catch (Exception e) {
		e.printStackTrace();
		 this.shutdown();
	}
	}
	

    /**
     * Signals the web server to shutdown.
	 *
     */
	public void shutdown() {
		shutdown = true; //change flag to true
		try {
			executor.awaitTermination(10, TimeUnit.SECONDS); //wait 10 seconds for all client threads to terminate
		} catch (Exception e) {
			executor.shutdownNow(); //shutdown if timeout
		}

	}
	
}
