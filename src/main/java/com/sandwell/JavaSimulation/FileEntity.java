/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.sandwell.JavaSimulation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.text.DecimalFormat;

/**
 * Class encapsulating file input/output methods and file access.
 */
public class FileEntity {
	public static int ALIGNMENT_LEFT = 0;
	public static int ALIGNMENT_RIGHT = 1;

	public static int FILE_READ = 0;
	public static int FILE_WRITE = 1;

	private File backingFileObject;
	private BufferedWriter outputStream;
	private BufferedReader inputStream;

	private DecimalFormat formatter;
	private static DecimalFormat staticFormatter;

	private long fileLength = -1;
	private long charsRead = 0;

	private String fname;

	public FileEntity( String fileName, int io_status, boolean append ) {

		// Case 1) the file exists inside the jar file
		try {
			// Check if the absolute file name exists
			if( fileName.contains( "file:/" ) ) {
				if(fileName.contains(".jar!")) {
					fname = fileName.replace( "%20", " " );
					int firstIndex = fname.indexOf( ".jar!" ) + ".jar!".length();
					String relativeURL = fname.substring( firstIndex );
					InputStream inStream = this.getClass().getResourceAsStream( relativeURL );
					inputStream = new BufferedReader( new InputStreamReader( inStream ) );
					inputStream.mark( 4096 );
					outputStream = null;
					fileLength = inStream.available();
					charsRead = 0;
					return;
				}
				fileName = fileName.replace("file:/", "");
			}

			// Check if the relative file name exists
			if (Simulation.class.getResource(fileName) != null) {
				fname = fileName;
				InputStream inStream = this.getClass().getResourceAsStream( fileName );

				inputStream = new BufferedReader( new InputStreamReader( inStream ) );
				inputStream.mark( 4096 );
				outputStream = null;
				fileLength = inStream.available();
				charsRead = 0;
				return;
			}
		}
		catch( Exception e ) {
			throw new ErrorException( e );
		}

		// Check if absolute file name was passed, otherwise use root directory
		backingFileObject = new File( fileName);

		init(io_status, append);
	}

	public FileEntity(URI file, int io_status, boolean append) {
		backingFileObject = new File(file);
		init(io_status, append);
	}

	private void init(int io_status, boolean append) {

		// Case 2) the file does not exist inside the jar file
		if( staticFormatter == null ) {
			staticFormatter = new DecimalFormat( "##0.00" );
		}
		formatter = new DecimalFormat( "##0.00" );

		fname = backingFileObject.getName();
		try {
			backingFileObject.createNewFile();
			if( io_status == FILE_WRITE ) {
				outputStream = new BufferedWriter( new FileWriter( backingFileObject, append ) );
				inputStream = null;
			}

			if( io_status == FILE_READ ) {
				inputStream = new BufferedReader( new FileReader( backingFileObject ) );
				inputStream.mark( 4096 );
				outputStream = null;
				fileLength = backingFileObject.length();
				charsRead = 0;
			}
		}
		catch( IOException e ) {
			throw new InputErrorException( "IOException thrown trying to open FileEntity: " + e );
		}
		catch( IllegalArgumentException e ) {
			throw new InputErrorException( "IllegalArgumentException thrown trying to open FileEntity (Should not happen): " + e );
		}
		catch( SecurityException e ) {
			throw new InputErrorException( "SecurityException thrown trying to open FileEntity: " + e );
		}
	}

	public void close() {
		try {
			if( inputStream != null ) {
				inputStream.close();
			}

			if( outputStream != null ) {
				outputStream.flush();
				outputStream.close();
			}
		}
		catch( IOException e ) {
			throw new ErrorException( "Unable to close FileEntity: " + e );
		}
	}

	public void flush() {
		try {
			if( outputStream != null ) {
				outputStream.flush();
			}
		}
		catch( IOException e ) {
			throw new ErrorException( "Unable to flush FileEntity: " + e );
		}
	}

	public void putString( String string ) {
		try {
			outputStream.write( string );
		}
		catch( IOException e ) {
			return;
		}
	}

	public void format(String format, Object... args) {
		putString(String.format(format, args));
	}

	public URI getFileURI() {
		return backingFileObject.toURI();
	}

	/**
	 * Prints the given string for the specified number of times.
	 */
	public void putString( String string, int count ) {
		try {
			for ( int i =0; i < count; i++ ) {
				outputStream.write( string );
			}
		}
		catch( IOException e ) {
			return;
		}
	}

	/**
	 * Generic string writing method.  All other methods will wrap this class.
	 */
	public void putString( String string, int putLength, int alignment ) {
		String spaces = "";

		for( int i = 0; i < putLength - string.length(); i++ ) {
			spaces = " " + spaces;
		}
		try {
			if( alignment == ALIGNMENT_LEFT ) {
				outputStream.write( string + spaces );
			}
			if( alignment == ALIGNMENT_RIGHT ) {
				outputStream.write( spaces + string );
			}
			outputStream.flush();
		}
		catch( IOException e ) {
			return;
		}
	}

	public void putStringLeft( String string, int putLength ) {
		putString( string, putLength, ALIGNMENT_LEFT );
	}

	public void putStringRight( String string, int putLength ) {
		putString( string, putLength, ALIGNMENT_RIGHT );
	}

	public void putInt( int putInteger ) {
		putString( Integer.toString( putInteger ), Integer.toString( putInteger ).length(), ALIGNMENT_LEFT );
	}

	public void putIntRight( int putInteger, int putLength ) {
		putString( Integer.toString( putInteger ), putLength, ALIGNMENT_RIGHT );
	}

	public void putDoublePadLeft( double putDouble, int decimalPlaces, int putLength ) {
		StringBuilder pattern = new StringBuilder("###");
		if( decimalPlaces > 0 ) {
			pattern.append(".");
			for( int i = 0; i < decimalPlaces; i++ ) {
				pattern.append("0");
			}
		}
		formatter.applyPattern(pattern.toString());

		putString( formatter.format( putDouble ), putLength, ALIGNMENT_LEFT );
	}

	public void putDoublePadRight( double putDouble, int decimalPlaces, int putLength ) {
		StringBuilder pattern = new StringBuilder("##0");
		if( decimalPlaces > 0 ) {
			pattern.append(".");
			for( int i = 0; i < decimalPlaces; i++ ) {
				pattern.append("0");
			}
		}
		formatter.applyPattern(pattern.toString());

		putString( formatter.format( putDouble ), putLength, ALIGNMENT_RIGHT );
	}

	public void putDoubleWithDecimals( double putDouble, int decimalPlaces ) {
		StringBuilder pattern = new StringBuilder("##0");
		if( decimalPlaces > 0 ) {
			pattern.append(".");
			for( int i = 0; i < decimalPlaces; i++ ) {
				pattern.append("0");
			}
		}
		formatter.applyPattern(pattern.toString());

		putString( formatter.format( putDouble ), formatter.format( putDouble ).length(), ALIGNMENT_LEFT );
	}

	public void putDoubleWithDecimalsTabs( double putDouble, int decimalPlaces, int tabs ) {
		StringBuilder pattern = new StringBuilder("##0");
		if( decimalPlaces > 0 ) {
			pattern.append(".");
			for( int i = 0; i < decimalPlaces; i++ ) {
				pattern.append("0");
			}
		}
		formatter.applyPattern(pattern.toString());

		putString( formatter.format( putDouble ), formatter.format( putDouble ).length(), ALIGNMENT_LEFT );
		putTabs( tabs );
	}

	public static String getDoubleWithDecimals( double putDouble, int decimalPlaces ) {
		StringBuilder pattern = new StringBuilder("##0.");
		for( int i = 0; i < decimalPlaces; i++ ) {
			pattern.append("0");
		}
		staticFormatter.applyPattern(pattern.toString());

		return staticFormatter.format( putDouble );
	}

	public void putAll( String putString ) {

		try {
			outputStream.write( putString );
		}
		catch( IOException e ) {
			return;
		}
	}

	public void newLine() {
		try {
			outputStream.newLine();
		}
		catch( IOException e ) {
			return;
		}
	}

	public void newLine( int numLines ) {
		for( int i = 0; i < numLines; i++ ) {
			newLine();
		}
	}

	public void putTab() {
		try {
			outputStream.write( "\t" );
		}
		catch( IOException e ) {
			return;
		}
	}

	public void putTabs( int numTabs ) {
		try {
			for( int i = 0; i < numTabs; i++ ) {
				outputStream.write( "\t" );
			}
		}
		catch( IOException e ) {
			return;
		}
	}

	public void write( String text ) {
		try {
			outputStream.write( text );
		}
		catch( IOException e ) {
			return;
		}
	}

	public void putSpaces( int numSpaces ) {
		try {
			for( int i = 0; i < numSpaces; i++ ) {
				outputStream.write( " " );
			}
		}
		catch( IOException e ) {
			return;
		}
	}

	public String readLine() {
		String line = null;
		try {
			line = inputStream.readLine();
		} catch (IOException e) {}

		return line;
	}

	public void toStart() {
		if( inputStream != null ) {
			try {
				if( backingFileObject != null ) {
					inputStream = new BufferedReader( new FileReader( backingFileObject ) );
					inputStream.mark( 4096 );
					return;
				}
			}
			catch( IOException e ) {
				// Done to re-open file reader if mark has gone too far
			}

			try {
				inputStream.close();
				inputStream = null;

				if( fname.contains( "file:/" ) ) {
					int firstIndex = fname.indexOf( ".jar!" ) + ".jar!".length();
					String relativeURL = fname.substring( firstIndex );
					InputStream inStream = this.getClass().getResourceAsStream( relativeURL );
					inputStream = new BufferedReader( new InputStreamReader( inStream ) );
				}
				else {
					inputStream = new BufferedReader( new FileReader( backingFileObject ) );
				}
				inputStream.mark( 4096 );
			}
			catch( IOException e ) {
				throw new ErrorException( "Unable to reset FileEntity to start: " + e );
			}
		}
	}

	public void putStringTabs( String input, int tabs ) {
		putString( input );
		putTabs( tabs );
	}

	public void putDoublePercentWithDecimals( double percent, int decDigits ) {
		putString( getDoubleWithDecimals( (percent * 100.0), decDigits ) + "%" );
	}

	/**
	 * Mark the current file position
	 */
	public void markCurrentPosition() {
		try {
			inputStream.mark( (int) fileLength );
		}
		catch( IOException e ) {
			throw new InputErrorException( "IOException thrown trying to mark FileEntity: " + e );
		}
	}

	/**
	 * Go to the last marked position in the file
	 */
	public void goToLastMarkedPosition() {
		try {
			inputStream.reset();
		}
		catch( IOException e ) {
			throw new InputErrorException( "IOException thrown trying to reset FileEntity: " + e + "  to a marked position" );
		}
	}

	public long getLength() {
		return fileLength;
	}

	public long getNumRead() {
		return charsRead;
	}

	public String getFileName() {
		return fname;
	}

	/**
	 * Delete the file
	 */
	public void delete() {
		try {
			if( backingFileObject.exists() ) {
				if( !backingFileObject.delete() ) {
					throw new ErrorException( "Failed to delete " + fname );
				}
			}
		}
		catch( SecurityException e ) {
			throw new ErrorException( "Unable to delete " + fname + "(" + e.getMessage() + ")" );
		}
	}
}
