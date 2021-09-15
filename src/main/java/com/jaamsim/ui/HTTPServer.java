package com.jaamsim.ui;

	import java.io.BufferedReader;
	import java.io.InputStreamReader;
	import java.io.PrintWriter;
	import java.net.ServerSocket;
	import java.net.Socket;

	public class HTTPServer {
		GUIFrame jaamsim;
		int port = 0;
		
	  HTTPServer(int port, GUIFrame jaamsimMain)
	  {
		  this.jaamsim = jaamsimMain;
	    try {

	     this.port = port;
	      // 
	      ServerSocket ss = new ServerSocket(port);

	      while(true) {

	        Socket client = ss.accept();

	        
	        // Get input and output streams to talk to the client
	        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
	        PrintWriter out = new PrintWriter(client.getOutputStream());

	        String input = in.readLine().toLowerCase();
	        System.out.println(input);
	        //
	        if(input.contains("start"))
	        {
	        	System.out.println("start");
	        	jaamsim.startSimulation();
	        }
	        else if(input.contains("stop"))
	        {
	        	System.out.println("stop");
	        	jaamsim.stopSimulation();
	        }
	        else if(input.contains("pause"))
	        {
	        	System.out.println("pause");
	        	jaamsim.pauseSimulation();
	        }
	        //
	        out.print("HTTP/1.1 200 \r\n"); // Version & status code
	        out.print("Content-Type: text/plain\r\n"); // The type of data
	        out.print("Connection: close\r\n"); // Will close stream
	        out.print("\r\n"); // End of headers	        
	        out.println("DONE: " + input);
	        //
	        out.close(); // Flush and close the output stream
	        in.close(); // Close the input stream
	        client.close(); // Close the socket itself
	        
//	        out.close(); // Flush and close the output stream
//	        in.close(); // Close the input stream
//	        client.close(); // Close the socket itself
	      } // Now loop again, waiting for the next connection
	    }
	    catch (Exception e) {
	      System.err.println(e);
	      System.err.println("Usage: java HttpMirror <port>");
	    }
	  }
	}
	
