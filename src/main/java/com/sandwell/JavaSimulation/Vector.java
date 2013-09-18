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


/**
 * This custom vector class creates convenience methods needed by the Audition
 * conversion code.
 */
public class Vector extends java.util.Vector<Object> {

	public Vector() {
		super();
	}

	public Vector( int initialSize ) {
		super( initialSize );
	}

	public Vector( int initialSize, int sizeIncrement ) {
		super( initialSize, sizeIncrement );
	}

	public Vector( Vector originalVector ) {
		super( originalVector.size() );
		for( int i = 0; i < originalVector.size(); i++ ) {
			addElement( originalVector.get( i ) );
		}
	}

	/**
	 * Fill Vector with num of Double Vectors
	 */
	public void initializeVectorwithDoubleVector( int num ){

		if( size() != num ) {
			this.clear();

			for( int timesRepeat = 0; timesRepeat < num; timesRepeat++ ) {
				this.add( new DoubleVector( 1 ) );
			}
		}
	}

	/**
	 * Fill current vector with vectorSize double of Vector, each Vector contains doubleVectorSize of Double Vectors
	 */
	public void initializeVectorwithDoubleVector( int vectorSize, int doubleVectorSize){

		if( size() != vectorSize ) {
			this.clear();

			for( int timesRepeat = 0; timesRepeat < vectorSize; timesRepeat++ ) {
				this.add( new Vector( doubleVectorSize ) );
				for( int timesRepeat2 = 0; timesRepeat2 < doubleVectorSize; timesRepeat2++ ) {
					((Vector)this.lastElement()).add( new DoubleVector( 1 ) );
				}
			}

		}
	}


	public int indexOfString( String testString ) {
		for( int i = 0; i < size(); i++ ) {
			try {
				String elementString = (String)get( i );
				if( elementString.equals( testString ) ) {
					return i;
				}
			}
			catch( ClassCastException e ) {
				continue;
			}
		}
		return -1;
	}

	public void addAllLast( Vector addVector ) {
		for( int i = 0; i < addVector.size(); i++ ) {
			addElement( addVector.get( i ) );
		}
	}

	public void addAllLastIfNotPresent( Vector addVector ) {
		for( int i = 0; i < addVector.size(); i++ ) {
			addIfNotPresent( addVector.get( i ) );
		}
	}

	public void addLastIfNotPresent( Object objectToAdd ) {
		if( objectToAdd instanceof String ) {
			for( int i = 0; i < size(); i++ ) {
				if( get( i ).equals( objectToAdd ) ) {
					return;
				}
			}
			addElement( objectToAdd );
		}
		else {
			for( int i = 0; i < size(); i++ ) {
				if( get( i ) == objectToAdd ) {
					return;
				}
			}
			addElement( objectToAdd );
		}
	}

	public void addIfNotPresent( Object objectToAdd ) {
		addLastIfNotPresent( objectToAdd );
	}

	@Override
	public int indexOf( Object objectToFind ) {
		for( int i = 0; i < size(); i++ ) {
			if( get( i ) == objectToFind ) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Return a string containing the contents of the vector.
	 * This method assumes that each element of the vector is an Entity.
	 */
	@Override
	public String toString() {

		Entity ent;
		StringBuilder ret = new StringBuilder("{ ");

		if( size() > 0 ) {
			if( this.get( 0 ) instanceof Entity ) {
				ent = (Entity)this.get( 0 );
				ret.append(ent.getName());
			}
			else {
				ret.append(this.get(0));
			}

			for( int i = 1; i < size(); i++ ) {
				if( this.get( i ) instanceof Entity ) {
					ent = (Entity)this.get( i );
					ret.append(", ");
					ret.append(ent.getName());
				}
				else {
					ret.append(", ");
					ret.append(this.get(i));
				}
			}
			ret.append(" }");
		}
		else {
			ret.append("}");
		}
		return ret.toString();
	}

	public int indexOfVector( Vector testVector ) {
		Vector elementVector;
		for( int i = 0; i < size(); i++ ) {
			try {
				elementVector = (Vector)get( i );
				if( elementVector.equals( testVector ) ) {
					return i;
				}
			}
			catch( ClassCastException e ) {
				continue;
			}
		}
		return -1;
	}

	public Vector intersection( Vector with ) {

		Vector ints = new Vector( 1, 1 );
		for( int i = 0; i < with.size(); i++ ) {
			if( this.indexOf( with.get( i ) ) >=0 ) {
				ints.add( with.get( i ) );
			}
		}
		return ints;
	}
}
