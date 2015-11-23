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
package com.jaamsim.datatypes;

import java.util.ArrayList;

import com.jaamsim.basicsim.ErrorException;

/**
 * This class implements a 2-dimensional table of doubles with a DoubleVector.
 */
public class DoubleTable {

	private ArrayList<DoubleVector> table;
	private int columns; // number of columns
	private int rows; // number of rows

	/**
	 * Construct an empty table, with default size 1 row and 1 column.
	 */
	public DoubleTable() {
		this( 1, 1 );
	}

	/**
	 * Construct a table of zeroes with the given number of rows and columns.
	 */
	public DoubleTable( int r, int c ) {
		// Set the number of rows and columns
		rows = r;
		columns = c;

		// Create the table
		table = new ArrayList<>(r);

		for( int i = 0; i < r; i++ ) {
			DoubleVector newRow = new DoubleVector( c, 1 );
			newRow.fillWithEntriesOf( c, 0.0 );
			table.add( newRow );
		}
	}

	/**
	 * Set the number of rows in the table.
	 */
	public void setRows( int r ) {

		// Delete rows
		for( int i = table.size(); i > r; i-- ) {
			table.remove(i - 1);
		}

		// Add rows
		for( int i = table.size(); i < r; i++ ) {
			DoubleVector newRow = new DoubleVector( columns, 1 );
			newRow.fillWithEntriesOf( columns, 0.0 );
			table.add(newRow);
		}

		// Set the number of rows
		rows = r;
	}

	/**
	 * Return the number of rows in the table.
	 */
	public int getRows() {
		return rows;
	}

	/**
	 * Set the number of columns in the table.
	 */
	public void setColumns( int c ) {

		// Delete columns
		for( int i = 0; i < table.size(); i++ ) {
			for( int j = 0; j < (columns - c); j++ ) {
				table.get( i ).remove(columns - 1 - j);
			}
		}

		// Add columns
		for( int i = 0; i < table.size(); i++ ) {
			for( int j = 0; j < (c - columns); j++ ) {
				table.get(i).add(0.0);
			}
		}

		// Set the number of columns
		columns = c;
	}

	/**
	 * Return the number of columns in the table.
	 */
	public int getColumns() {
		return columns;
	}

	/**
	 * Set the component at the given row and column of this table to be the given double.
	 */
	public void setElementAtAt( double value, int r, int c ) {
		if( (r < 0 || r >= rows) || (c < 0 || c >= columns) ) {
			throw new ErrorException( "index out of range for table" );
		}
		table.get(r).set(c, value);
	}

	/**
	 * Add the specified value to the value at the specified row and column.
	 */
	public void addAtAt( double value, int r, int c ) {
		if( (r < 0 || r >= rows) || (c < 0 || c >= columns) ) {
			throw new ErrorException( "index out of range for table" );
		}
		table.get(r).addAt(value, c);
	}

	/**
	 * Return the double at the given row and column in this table.
	 */
	public double get( int r, int c ) {
		if( (r < 0 || r >= rows) || (c < 0 || c >= columns) ) {
			throw new ErrorException( "index out of range for table" );
		}
		return table.get(r).get(c);
	}

	/**
	 * Delete the given row in this table.
	 */
	public void deleteRow( int r ) {
		if( r < 0 || r >= rows ) {
			throw new ErrorException( "index out of range for table" );
		}
		table.remove(r);

		// Decrement row count
		rows--;
	}

	/**
	 * Delete the given column in this table.
	 */
	public void deleteColumn( int c ) {
		if( c < 0 || c >= columns ) {
			throw new ErrorException( "index out of range for table" );
		}

		// Delete column
		for( int i = 0; i < table.size(); i++ ) {
			table.get(i).remove(c);
		}

		// Decrement column count
		columns--;
	}

	/**
	 * Return the given row values in this table.
	 */
	public DoubleVector getRowValues( int r ) {
		if( r < 0 || r >= rows ) {
			throw new ErrorException( "index out of range for table" );
		}

		DoubleVector values = new DoubleVector(table.get(r));
		return values;
	}

	/**
	 * Return the given row values in this table.
	 */
	public DoubleVector get( int r ) {
		if( r < 0 || r >= rows ) {
			throw new ErrorException( "index out of range for table" );
		}

		DoubleVector values = new DoubleVector(table.get(r));
		return values;
	}

	/**
	 * Return the given column values in this table.
	 */
	public DoubleVector getColumnValues( int c ) {
		if( c < 0 || c >= columns ) {
			throw new ErrorException( "index out of range for table" );
		}

		DoubleVector values = new DoubleVector( rows, 1 );

		for( int i = 0; i < table.size(); i++ ) {
			values.add(table.get(i).get(c));
		}

		return values;
	}

	/**
	 * Return a string representation of this table,
	 * containing the String representation of each element.
	 */
	@Override
	public String toString() {
		return table.toString();
	}
}
