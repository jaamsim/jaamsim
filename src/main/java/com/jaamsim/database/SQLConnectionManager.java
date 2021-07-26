package com.jaamsim.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;

public class SQLConnectionManager extends DisplayEntity {

	private Properties properties;
	private static SQLConnectionManager single_instance = null;
	private Connection conn = null;

	 // DEFINE THE INPUT OBJECTS
	 @Keyword(description = "Database name",
	 exampleList = "JaamsimDB")
	 private final StringInput database;

	 @Keyword(description = "User",
			 exampleList = "admin sa PeterPaulAndMary")
			 private final StringInput user;

	 @Keyword(description = "Password",
			 exampleList = "secretIngredient")
			 private final StringInput password;

	 @Keyword(description = "Server",
			 exampleList = "jdbc:sqlserver://10.100.16.108:1433")
			 private final StringInput sqlServer;


	 // ADD THE INPUTS TO EACH INSTANCE THAT IS CREATED
	 {
		 database = new StringInput("DataBase", KEY_INPUTS, "");
		 this.addInput(database);

		 user = new StringInput("User", KEY_INPUTS, "");
		 this.addInput(user);

		 password = new StringInput("Password", KEY_INPUTS, "");
		 this.addInput(password);

		 sqlServer = new StringInput("SQLserver", KEY_INPUTS, "");
		 this.addInput(sqlServer);
	 }

	public SQLConnectionManager() {
		properties = new Properties();
		properties.put("database", database);
		properties.put("user", user);
		properties.put("password", password);
	}

	public static SQLConnectionManager getInstance() {

		if (single_instance == null) {
			single_instance = new SQLConnectionManager();
		}
		return single_instance;
	}

	@SuppressWarnings("unused")
	private void loadPorpertiesXML(String inXmlFile) {

		try {
			InputStream in = new FileInputStream(inXmlFile);
			properties.loadFromXML(in);
		} catch (InvalidPropertiesFormatException e) {
			e.printStackTrace();
		}

		catch (IOException e) {

			e.printStackTrace();
		}
	}

	public Connection connect() {

		if(conn != null)
			return conn;

		try {
			Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
		}

		catch (ClassNotFoundException e) {
			e.printStackTrace();
		}


		try {
			conn = DriverManager.getConnection(sqlServer.toString(), properties);
		} catch (SQLException se) {
			se.printStackTrace();
		}

		return conn;
	}

	public void closeConnection()
	{
		if(conn != null)
			try {
			conn.close();
			}
		catch (SQLException se) {
			se.printStackTrace();
		}
	}

	@Override
	public void finalize() {
		if(conn != null)
	    try {
	        conn.close();
	        System.out.println("Closed connection in the finalizer");
	    } catch (SQLException se) {
	    	se.printStackTrace();
	    }
	}
}
