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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;

import com.jaamsim.ui.LogBox;

/**
 * Class encapsulating file input/output methods and file access.
 */
public class FileEntity {
	public static int ALIGNMENT_LEFT = 0;
	public static int ALIGNMENT_RIGHT = 1;

	private File backingFileObject;
	private BufferedWriter outputStream;

	private DecimalFormat formatter;

	public FileEntity(String fileName) {
		this(fileName, false);
	}

	public FileEntity(String fileName, boolean append) {
		backingFileObject = new File( fileName);
		formatter = new DecimalFormat( "##0.00" );

		try {
			backingFileObject.createNewFile();
			outputStream = new BufferedWriter( new FileWriter( backingFileObject, append ) );
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
			if( outputStream != null ) {
				outputStream.flush();
				outputStream.close();
				outputStream = null;
			}
		}
		catch( IOException e ) {
			outputStream = null;
			LogBox.logLine( "Unable to close FileEntity: " + backingFileObject.getName() );
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

	public void putStringTabs( String input, int tabs ) {
		putString( input );
		putTabs( tabs );
	}

	/**
	 * Delete the file
	 */
	public void delete() {
		try {
			if( backingFileObject.exists() ) {
				if( !backingFileObject.delete() ) {
					throw new ErrorException( "Failed to delete " + backingFileObject.getName() );
				}
			}
		}
		catch( SecurityException e ) {
			throw new ErrorException( "Unable to delete " + backingFileObject.getName() + "(" + e.getMessage() + ")" );
		}
	}
}
