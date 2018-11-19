/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.basicsim;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;

/**
 * Class encapsulating file input/output methods and file access.
 */
public class FileEntity {
	private File backingFileObject;
	private BufferedWriter outputStream;

	public FileEntity(String fileName) {
		this(fileName, false);
	}

	public FileEntity(String fileName, boolean append) {
		backingFileObject = new File(fileName);

		try {
			backingFileObject.createNewFile();
			outputStream = new BufferedWriter( new FileWriter(backingFileObject, append) );
		}
		catch (IOException e) {
			throw new InputErrorException("IOException thrown trying to open file: '%s'%n%s",
					fileName, e.getMessage());
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("IllegalArgumentException thrown trying to open file: "
					+ "'%s'%n%s",
					fileName, e.getMessage());
		}
		catch (SecurityException e) {
			throw new InputErrorException("SecurityException thrown trying to open file: '%s'%n%s",
					fileName, e.getMessage());
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
			InputAgent.logMessage( "Unable to close FileEntity: " + backingFileObject.getName() );
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

	public void format(String format, Object... args) {
		write(String.format(format, args));
	}

	public void newLine() {
		try {
			outputStream.newLine();
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
