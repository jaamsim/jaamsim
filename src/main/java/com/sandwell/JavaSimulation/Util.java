/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2005-2011 Ausenco Engineering Canada Inc.
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

import java.awt.Font;
import java.awt.FontMetrics;

import javax.vecmath.Vector3d;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

	/**
	 *
	 *
	 */
public class Util {
	private static final java.text.DecimalFormat formatter = new java.text.DecimalFormat( "0.000" );
	private static final java.text.DecimalFormat exponentFormat = new java.text.DecimalFormat( "0.###E0" );

	private static String cargoUnits = "";
	private static String fuelUnits = "";

	public static final Comparator<Entity> nameSort = new Comparator<Entity>() {
		public int compare(Entity a, Entity b) {
			return a.getName().compareTo(b.getName());
		}
	};

	public static String getCargoUnits() {
		return cargoUnits;
	}

	public static void setCargoUnits(String units) {
		cargoUnits = units;
	}

	public static String getFuelUnits() {
		return fuelUnits;
	}

	public static void setFuelUnits(String units) {
		fuelUnits = units;
	}

	/**
	 *
	 *
	 * {  aaa, bbb,  ccc, ddd }
	 * {    ", bbb,  ccc, ddd }
	 * {  aaa, bbb,    ", ddd }
	 * { "aaa, bbb,  ccc, ddd }
	 * {  aaa, bbb, "ccc, ddd }
	 * { a"aa, bbb,  ccc, ddd } <-- disregard this possibility
	 * {  aaa, bbb, cc"c, ddd } <-- disregard this possibility
	 *
	 *
	 * Returns true if record is not empty; false, otherwise.
	 *
	 */
	public static boolean discardComment( Vector record ) {

		boolean containsCmd;
		int     recSize;
		int     indexIsolated; // Index of first cell containing ``"'' alone
		int     indexMixed;    // Index of first cell containing a string that starts with ``"''
		int     indexFirst;    // Index where the comment starts from

		if ( record == null ) {
			return false;
		}

		recSize = record.size();
		if ( recSize == 0 ) {
			return false;
		}

		// Find in record the index of the first cell that contains ``"''.
		// If none of the cell contains ``"'', index = -1
		indexIsolated = record.indexOfString( "\"" );
		if ( indexIsolated == -1 ) {
			indexIsolated = recSize;
		}

		// Find in record the index of the first cell that contains string of the form ``"aaa''.
		// Disregard strings of the forms ``aaa"bbb'' and ``aaa"''
		indexMixed = recSize;
		for ( int i = 0; i < recSize; i++ ) {
			if ( ((String)record.get(i)).startsWith( "\"" )) {
				indexMixed = i;
				break;
			}
		}

		// Set indexFirst to minimum of indexIsolated and indexMixed
		indexFirst = Math.min( indexIsolated, indexMixed );

		// Strip away comment string from record. Return true if record contains a command; false, otherwise
		// Note that indexFirst cannot be greater than recSize
		if ( indexFirst == recSize ) { // no comments found
			containsCmd = true;
		}
		else if ( indexFirst > 0 ) { // command and comment found
			containsCmd = true;

			// Discard elements from index to (recSize - 1)
			for ( int x = indexFirst; x < recSize; x++ ) {
				record.removeElementAt( indexFirst );
			}
		}
		else { // if (indexFirst == 0), it means that record contains only comment
			containsCmd = false;
			record.clear();
		}

		return containsCmd;

	} // discardComment


	/**
	 * if ( classFullName = "com.sandwell.MTM.Port" ), returns "Port"
	 * if ( classFullName = "Port" ), returns "Port"
	 */
	public static String classShortName( String classFullName ) {
		int indexLast = classFullName.lastIndexOf( '.' );
		String className = classFullName.substring( indexLast + 1 );

		return className;
	}

	/**
	 * if ( fileFullName = "C:\Projects\A01.cfg" ), returns "A01.cfg"
	 * if ( fileFullName = "A01.cfg" ), returns "A01.cfg"
	 */
	public static String fileShortName( String fileFullName ) {
		int indexLast = fileFullName.lastIndexOf( '\\' );
		indexLast = Math.max( indexLast, fileFullName.lastIndexOf( '/') );
		String fileName = fileFullName.substring( indexLast + 1 );

		return fileName;
	}

	public static String getAbsoluteFilePath( String filePath ) {
		String absoluteFilePath;
		try {
			java.io.File absFile = new java.io.File( filePath );
			if( absFile.isAbsolute() ) {
				absoluteFilePath = absFile.getCanonicalPath() + System.getProperty( "file.separator" );
				if(absFile.isFile()) {
					absoluteFilePath = "file:/" + absoluteFilePath;
				}
				else {
					absoluteFilePath += System.getProperty( "file.separator" );
				}
			}
			else {
				// For absolute files inside the resource folder of the jar file
				if (Simulation.class.getResource(filePath) != null) {
					absoluteFilePath = Simulation.class.getResource(filePath).toString();
				}
				else {

					// Does the relative filepath exist inside the jar file?
					String relativeURL = FileEntity.getRelativeURL( filePath );
					if (Simulation.class.getResource(relativeURL) != null) {
						absoluteFilePath = Simulation.class.getResource(relativeURL).getFile();
					}
					else {
						absoluteFilePath = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + filePath;
						absoluteFilePath = FileEntity.getRelativeURL(absoluteFilePath);
						if(FileEntity.fileExists(absoluteFilePath)) {
							absoluteFilePath = "file:/" + absoluteFilePath;
						}
					}
				}
			}
		}
		catch( Exception e ) {
			throw new ErrorException( e );
		}

		return absoluteFilePath;
	}

	public static String getFileExtention(String name){
		if(name.indexOf(".") < 0)
			return "";

		String ext = name.substring( name.lastIndexOf(".")).trim();
		return ext.replace(".", "").toUpperCase();
	}

	/**
	 * Format a number in decimal or exponent format depending on its magnitude.
	 */
	public static String formatNumber( double num ) {
		if( num > 999999.0 || num < -999999.0 )
			return exponentFormat.format( num );
		else if( num == 0.0 )
			return formatter.format( num );
		if( Math.log( num ) < -4.0 )
			return exponentFormat.format( num );
		else
			return formatter.format( num );
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest.  objectsToSort must be a vector of doubles.
	 * @param objectsToSort
	 */
	public static void quickSort(Vector objectsToSort) {
		DoubleVector crit1 = new DoubleVector(objectsToSort.size(), 1);
		// create crit1
		int objectsToSortSize = objectsToSort.size();
		for (int i = 0; i < objectsToSortSize; i++) {
			crit1.add( ( (Double)objectsToSort.get(i) ).doubleValue() );
		}

		quickSort(objectsToSort, crit1, crit1, crit1, crit1, crit1);
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest by crit1.  The criteria vectors are left unsorted.
	 * @param objectsToSort
	 * @param crit1
	 */
	public static void quickSort(Vector objectsToSort, DoubleVector crit1) {
		quickSort(objectsToSort, crit1, crit1, crit1, crit1, crit1);
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest by crit1,crit2.  The criteria vectors are left unsorted.
	 * @param objectsToSort
	 * @param crit1
	 * @param crit2
	 */
	public static void quickSort(Vector objectsToSort, DoubleVector crit1, DoubleVector crit2) {
		quickSort(objectsToSort, crit1, crit2, crit2, crit2, crit2);
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest by crit1,crit2,crit3.  The criteria vectors are left unsorted.
	 * @param objectsToSort
	 * @param crit1
	 * @param crit2
	 * @param crit3
	 */
	public static void quickSort(Vector objectsToSort, DoubleVector crit1, DoubleVector crit2, DoubleVector crit3) {
		quickSort(objectsToSort, crit1, crit2, crit3, crit3, crit3);
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest by crit1,crit2,crit3,crit4.  The criteria vectors are left unsorted.
	 * @param objectsToSort
	 * @param crit1
	 * @param crit2
	 * @param crit3
	 * @param crit4
	 */
	public static void quickSort(Vector objectsToSort, DoubleVector crit1, DoubleVector crit2, DoubleVector crit3, DoubleVector crit4) {
		quickSort(objectsToSort, crit1, crit2, crit3, crit4, crit4);
	}

	/**
	 * This method will sort the vector objectsToSort from Largest to Smallest by crit1,crit2,crit3,crit4, crit5.  The criteria vectors are left unsorted.
	 * @param objectsToSort
	 * @param crit1
	 * @param crit2
	 * @param crit3
	 * @param crit4
	 * @param crit5
	 */
	public static void quickSort(Vector objectsToSort, DoubleVector crit1, DoubleVector crit2, DoubleVector crit3, DoubleVector crit4, DoubleVector crit5)
	{
		Vector combinedObjectList = new Vector( objectsToSort.size(), 1 );

		int doLoop = objectsToSort.size();
		for ( int i = 0; i < doLoop; i++ ) {
			Vector combinedObject = new Vector(6,1);

			combinedObject.add(objectsToSort.get(i));
			combinedObject.add( new Double( crit1.get(i) ) );
			combinedObject.add( new Double( crit2.get(i) ) );
			combinedObject.add( new Double( crit3.get(i) ) );
			combinedObject.add( new Double( crit4.get(i) ) );
			combinedObject.add( new Double( crit5.get(i) ) );

			combinedObjectList.add(combinedObject);
		}

		if ( combinedObjectList.size() > 0 ) {
			qSort(combinedObjectList, 0, combinedObjectList.size() - 1);
		}
		doLoop = objectsToSort.size();
		for ( int i = 0; i < doLoop; i++ ) {
			objectsToSort.setElementAt( ( ( Vector ) combinedObjectList.get( i ) ).get( 0 ), i );
		}
	}

	/**
	 * This method will return true if the vector v1 is greater than v2, starting at index 1 in the list (the key is ignored)
	 * @param v1
	 * @param v2
	 * @return
	 */
	private static boolean combinedVectorIsGreaterThan(Vector v1, Vector v2) {

		( (Double)v1.get(1) ).doubleValue();

		if( ( (Double)v1.get( 1 ) ).doubleValue() > ( (Double)v2.get( 1 ) ).doubleValue() ) {
			return true;
		}
		if( ( (Double)v1.get( 1 ) ).doubleValue() == ( (Double)v2.get( 1 ) ).doubleValue() ) {
			if( ( (Double)v1.get( 2 ) ).doubleValue() > ( (Double)v2.get( 2 ) ).doubleValue() ) {
				return true;
			}
			if( ( (Double)v1.get( 2 ) ).doubleValue() == ( (Double)v2.get( 2 ) ).doubleValue() ) {
				if( ( (Double)v1.get( 3 ) ).doubleValue() > ( (Double)v2.get( 3 ) ).doubleValue() ){
					return true;
				}
				if( ( (Double)v1.get( 3 ) ).doubleValue() == ( (Double)v2.get( 3 ) ).doubleValue() ) {
					if( ( (Double)v1.get( 4 ) ).doubleValue() > ( (Double)v2.get( 4 ) ).doubleValue() ){
						return true;
					}
					if( ( (Double)v1.get( 4 ) ).doubleValue() == ( (Double)v2.get( 4 ) ).doubleValue() ) {
						if( ( (Double)v1.get( 5 ) ).doubleValue() > ( (Double)v2.get( 5 ) ).doubleValue() ) {
							return true;
						}

					}
				}
			}
		}

		return false;
	}

	/**
	 * list is a linked list of form list(i)(j), with j=0 being the object, j=1 being criteria1, etc.
	 * left and right are the boundaries of the list to sort.
	 * @param list
	 * @param left
	 * @param right
	 */
	private static void qSort(Vector list, int left, int right)
	{
		int l_hold = left;
		int r_hold = right;
		Vector pivot;
		int pivotIndex;

		// pick pivot from center of list, move to beginning of list
		pivot = (Vector)list.get( ( ( left + right ) /2 ) ) ;
		list.setElementAt( list.get(left), ((left+right)/2));
		list.setElementAt( pivot , left );

		pivot = (Vector)list.get(left);

		// while unsorted size is greater than zero
		while (left < right)
		{
			// compare right side to pivot
			while ( (combinedVectorIsGreaterThan( pivot, (Vector)list.get(right) ) ) && (left < right) )
				right--;
			// move to proper side of list
			if (left != right)
			{
				list.setElementAt( list.get( right ), left );
				left++;
			}
			// compare left side to pivot
			while ( (combinedVectorIsGreaterThan( (Vector)list.get(left), pivot ) ) && (left < right) )
				left++;
			// move to proper side of list
			if (left != right)
			{
				list.setElementAt( list.get( left ), right );
				right--;
			}
		}
		// restore pivot -- it was lost with 'setElementAt'
		list.setElementAt( pivot, left );
		pivotIndex = left;
		left = l_hold;
		right = r_hold;
		//System.out.println(list + " -- pivot = " + pivot + " left = " + left + " right = " + right);
		// sort left and right side of list
		if (left < pivotIndex)
			qSort(list, left, ( pivotIndex-1 ) );
		if (right > pivotIndex)
			qSort(list, ( pivotIndex+1 ), right);
	}

    /**
     * Return the factorial of the given number
     */
    public static int factorial( int n ) {
    	if( n == 1 ) {
    		return 1;
    	}
    	else {
    		return n * factorial( n - 1 );
    	}
    }

	/**
	 * Expects a StringVector of one of two forms:
	 * 1.  { entry entry entry } { entry entry entry }
	 * 2.  entry entry entry
	 * If format 1, returns a vector of stringvectors without braces.
	 * if format 2, returns a vector of a stringvector, size1.
	 * @param data
	 * @return
	 */
	public static ArrayList<StringVector> splitStringVectorByBraces(StringVector data) {
		ArrayList<StringVector> newData = new ArrayList<StringVector>();
		for (int i=0; i < data.size(); i++) {

			//skip over opening brace if present
			if (data.get(i).equals("{") )
				continue;

			StringVector cmd = new StringVector();

			//iterate until closing brace, or end of entry
			for (int j = i; j < data.size(); j++, i++){
				if (data.get(j).equals("}"))
					break;

				cmd.add(data.get(j));
			}

			//add to vector
			newData.add(cmd);
		}

		return newData;
	}

private static class CriteriaHolder implements Comparable<CriteriaHolder> {
private final Object obj;
private final double crit1;
private final double crit2;
private final double crit3;
private final double crit4;
private final double crit5;

CriteriaHolder(Object obj, double c1, double c2, double c3, double c4, double c5) {
	this.obj = obj;
	crit1 = c1;
	crit2 = c2;
	crit3 = c3;
	crit4 = c4;
	crit5 = c5;
}

@Override
/**
 * We sort from largest to smallest, so reverse the order in Double.compare.
 */
public int compareTo(CriteriaHolder u) {
	int comp;

	comp = Double.compare(u.crit1, this.crit1);
	if (comp != 0)
		return comp;

	comp = Double.compare(u.crit2, this.crit2);
	if (comp != 0)
		return comp;

	comp = Double.compare(u.crit3, this.crit3);
	if (comp != 0)
		return comp;

	comp = Double.compare(u.crit4, this.crit4);
	if (comp != 0)
		return comp;

	return Double.compare(u.crit5, this.crit5);
}
}

public static void mergeSort(Vector objectsToSort, DoubleVector crit1,
                             DoubleVector crit2, DoubleVector crit3,
                             DoubleVector crit4, DoubleVector crit5) {
	ArrayList<CriteriaHolder> temp = new ArrayList<Util.CriteriaHolder>(objectsToSort.size());
	for (int i = 0; i < objectsToSort.size(); i++) {
		temp.add(new CriteriaHolder(objectsToSort.get(i), crit1.get(i), crit2.get(i), crit3.get(i), crit4.get(i), crit5.get(i)));
	}
	Collections.sort(temp);
	objectsToSort.clear();
	for (int i = 0; i < temp.size(); i++) {
		objectsToSort.add(temp.get(i).obj);
	}
}

	/**
     * Returns the pixel length of the string with specified font
     * @param str
     * @param font
     * @return
     */
    public static int getPixelWidthOfString_ForFont( String str, Font font ) {
		FontMetrics metrics = new FontMetrics(font) {        };
		Rectangle2D bounds = metrics.getStringBounds( str, null);
		return (int) bounds.getWidth();
    }

    public static String HTMLString(String font, String size, String backgroundColor, String color, String accent, String text) {
    	StringBuilder ret = new StringBuilder("<html><pre style=\"font-family:");

		if (font == null)
			ret.append("verdana");
		else
			ret.append(font);

		if (size != null) {
			ret.append(";font-size:");
			ret.append(size);
		}

		if (backgroundColor != null) {
			ret.append(";background-color:");
			ret.append(backgroundColor);
		}

		if (color != null) {
			ret.append(";color:");
			ret.append(color);
		}

		ret.append("\">");

		if (accent != null) {
			ret.append(accent);
		}
		ret.append(text);

		return ret.toString();
    }

    /**
     * write the minimum values of p1,p2 into dest. p1 or p2 can be used as dest
     * @param p1 evaluate min value
     * @param p2 evaluate min value
     * @param dest overwrite dest value with min values of p1 and p2
     *
     */
    public static void minElement (Vector3d p1,Vector3d p2, Vector3d dest) {
    	dest.x = Math.min(p1.x, p2.x);
    	dest.y = Math.min(p1.y, p2.y);
    	dest.z = Math.min(p1.z, p2.z);
    }

    /**
     * write the max values of p1,p2 into dest. p1 or p2 can be used as dest
     * @param p1 evaluate min value
     * @param p2 evaluate min value
     * @param dest overwrite dest value with max values of p1 and p2
     *
     */
    public static void maxElement (Vector3d p1,Vector3d p2, Vector3d dest) {
    	dest.x = Math.max(p1.x, p2.x);
    	dest.y = Math.max(p1.y, p2.y);
    	dest.z = Math.max(p1.z, p2.z);
    }
}
