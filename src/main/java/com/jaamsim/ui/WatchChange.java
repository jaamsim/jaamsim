package com.jaamsim.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;



import java.io.*;


public class WatchChange extends Thread {
	GUIFrame jaamsimMain;
  
  // This will reference one line at a time
  String line = null, oldLine;
  final String watchFile;
  
  WatchService watcher;

  WatchChange(String watchFile, GUIFrame jaamsimMain) 
  {
      this.watchFile = watchFile;
	  this.jaamsimMain = jaamsimMain;
    try {
      Path path = Paths.get(watchFile).getParent();
      watcher = path.getFileSystem().newWatchService();
      path.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }


  public void run() {
	  
    while (true) {
      try {
        WatchKey watchKey = watcher.take();
        List<WatchEvent<?>> events = watchKey.pollEvents();
        for (WatchEvent<?> event : events) {
          // You can listen for these events too :
          //     StandardWatchEventKinds.ENTRY_DELETE
          //     StandardWatchEventKinds.ENTRY_CREATE
          if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
        	  System.out.println("Change in Directory " + event.context().toString());
        	
        	  
        	  try {
                  // FileReader reads text files in the default encoding.
                  FileReader fileReader = 
                      new FileReader(watchFile);

                  // Always wrap FileReader in BufferedReader.
                  BufferedReader bufferedReader = 
                      new BufferedReader(fileReader);
                  line = bufferedReader.readLine().trim().toUpperCase();
                  System.out.println("line="  + line);
                  System.out.println("oldline="  + oldLine);
                  
                  if(oldLine == null || !oldLine.equals(line))
                  {
	                  if(line.equals("INITIALIZE"))
	                  {
	                	  // do nothing...
	                  }
	                  else if(line.equals("START"))
	                  {
	                      System.out.println("DO START");
	                	  jaamsimMain.startSimulation();
	                  }
	                  else if(line.equals("PAUSE"))
	                  {
	                      System.out.println("DO PAUSE");
	                	  jaamsimMain.pauseSimulation();
	                  }
	                  else if(line.equals("STOP"))
	        		  {
	                      System.out.println("DO STOP");
	                	  jaamsimMain.stopSimulation();
	                  }
                	  oldLine = line;
                  }
                  // Always close files.
                  bufferedReader.close();         
              }
     	          	  
              catch(FileNotFoundException ex) {
                  System.out.println(
                      "Unable to open file '" + 
                    		  watchFile + "'");                
              }
              catch(IOException ex) {
                  System.out.println(
                      "Error reading file '" 
                      + watchFile + "'");                  
                  // Or we could just do this: 
                  // ex.printStackTrace();
              }
          }
       }
        
        watchKey.reset();
      }
      catch (Exception e) {
        System.out.println("Error: " + e.toString());
      }
    }
  }
}