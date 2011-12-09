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

import javax.media.j3d.Appearance;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.ConcurrentModificationException;

	/**
	 *
	 *
	 */
public class Util {
	private static final java.text.DecimalFormat formatter = new java.text.DecimalFormat( "0.000" );
	private static final java.text.DecimalFormat exponentFormat = new java.text.DecimalFormat( "0.###E0" );

	private static String cargoUnits = "";
	private static String volumetricUnits = "";
	private static String fuelUnits = "";
	private static String distanceUnits = "";
	private static String speedUnits = "";

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

	public static String getVolumetricUnits() {
		return volumetricUnits;
	}

	public static void setVolumetricUnits(String units) {
		volumetricUnits = units;
	}

	public static String getFuelUnits() {
		return fuelUnits;
	}

	public static void setFuelUnits(String units) {
		fuelUnits = units;
	}

	public static String getDistanceUnits() {
		return distanceUnits;
	}

	public static void setDistanceUnits(String units) {
		distanceUnits = units;
	}

	public static String getSpeedUnits() {
		return speedUnits;
	}

	public static void setSpeedUnits(String units) {
		speedUnits = units;
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

			StringVector cmd = new StringVector( 1, 1 );

			//skip over opening brace if present
			if (data.get(i).equals("{") ) {
				i++;
			}

			//iterate until closing brace, or end of entry
			for (int j = i; j < data.size(); j++){
				if (data.get(j).equals("}")) {
					j=data.size();
				} else {
					cmd.add(data.get(j));
				}
				i++;
			}

			//add to vector
			newData.add(cmd);

		}

		return newData;
	}

    //////////////////////// ************************************** /////////////////////////////
    /** This set of methods are being used by Entity and Shapes to generate information for
    	 the PropertyBox  **/

	private static String getField(Object obj, java.lang.reflect.Field field) {
		StringBuilder fieldString = new StringBuilder();

		try {
			// save the field's accessibility, reset after reading
			boolean accessible = field.isAccessible();
			field.setAccessible(true);

			// get the name and type for the property
			fieldString.append(field.getName());
			fieldString.append("\t");
			fieldString.append(field.getType().getSimpleName());
			fieldString.append("\t");
			fieldString.append(propertyFormatObject(field.get(obj)));

			// set the access for this field to what it originally was
			field.setAccessible(accessible);
		}
		catch( SecurityException e ) {
			System.out.println( e );
		}
		catch( IllegalAccessException e ) {
			System.out.println( e );
		}
		return fieldString.toString();
	}

    public static Vector getPropertiesOf( Object object ) {

    	if ( object == null ) {
    		return new Vector( 1, 1 );
    	}

    	Vector info = new Vector();
		Class<?> myClass = object.getClass();

		String name = "";
		if( object instanceof com.sandwell.JavaSimulation.Entity ) {
			name = ((com.sandwell.JavaSimulation.Entity) object).getName();
		}
		else if ( object instanceof com.sandwell.JavaSimulation3D.util.Shape ) {
			name = ((com.sandwell.JavaSimulation3D.util.Shape) object).getName();
		}
		info.addElement( "Name:\t\t" + name );
		info.addElement( "Reference:\t\t" + Integer.toHexString( object.hashCode() ) );
		info.addElement( "Class:\t\t" + myClass.getName() );

		Vector fields = getAllProperties( myClass );

		for( int i = 0; i < fields.size(); i++ ) {
			if (!(fields.get(i) instanceof java.lang.reflect.Field)) {
				info.addElement("");
				info.addElement("<html><pre style=\"font-family:verdana;font-size:100%;color:rgb(0,0,255);\"><b>" + ((Class<?>)fields.get(i)).getSimpleName() + ":");
				continue;
			}

			info.addElement(getField(object, (java.lang.reflect.Field)fields.get(i)));
		}

		return info;

	}

    /**
     * Return a list of properties for every super class
     * The first element in each row contains the name of the super class
     * @param object
     * @return
     */
    public static Vector getPropertiesBySuperClassesOf( Object object ) {

    	if ( object == null ) {
    		return new Vector( 1, 1 );
    	}

    	Vector infoBySuperClasses = new Vector();

    	Vector info = new Vector();
    	Class<?> myClass = object.getClass();
    	Vector fields = getAllProperties( myClass );

    	for( int i = 0; i < fields.size(); i++ ) {
			if (!(fields.get(i) instanceof java.lang.reflect.Field)) {
				if (info.size() > 0) {
					infoBySuperClasses.add(new Vector(info));
					info.clear();
				}
				info.addElement(((Class<?>)fields.get(i)).getSimpleName());
				continue;
			}
			info.addElement(getField(object, (java.lang.reflect.Field)fields.get(i)));
    	}
    	if( info.size() > 0 ) {
    		infoBySuperClasses.add( info );
    	}

    	return infoBySuperClasses;

    }

	public static Vector getAllProperties( Class<?> thisClass ) {
		if( thisClass == null || thisClass.getSuperclass() == null )
			return new Vector();

		Vector classProperties = getAllProperties( thisClass.getSuperclass() );
		java.lang.reflect.Field[] myFields = thisClass.getDeclaredFields();

		// Sort fields alphabetically
		for( int i = 0; i < myFields.length - 1; i++ ) {
			for( int j = i + 1; j < myFields.length; j++ ) {
				if( myFields[i].getName().compareToIgnoreCase( myFields[j].getName() ) > 0 ) {
					java.lang.reflect.Field temp = myFields[i];
					myFields[i] = myFields[j];
					myFields[j] = temp;
				}
			}
		}

		classProperties.add( thisClass );
		for( int i = 0; i < myFields.length; i++ ) {

			// Static variables are all capitalized (ignore them)
			if( myFields[i].getName().toUpperCase().equals( myFields[i].getName() ) ) {
				continue;
			}
			classProperties.add( myFields[i] );
		}

		return classProperties;
	}

	public static String propertyFormatObject( Object value ) {
		String ret = "";

//		if( value instanceof Entity ) {
//			/*
//			 * String cname = value.getClass().getName(); int pos =
//			 * cname.lastIndexOf('.'); cname = cname.substring(pos + 1); ret =
//			 * cname + "@" + Integer.toHexString(value.hashCode()) + " [" +
//			 * ((Entity)value).getName() + "] ";
//			 */
//
//			ret = ((Entity)value).getName();
//		}

//		else if( value == null )
		if( value == null )
			ret = "<null>";

//		else if( value instanceof Boolean || value instanceof Byte || value instanceof Character || value instanceof Double || value instanceof Float || value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Point || value instanceof DoubleVector || value instanceof IntegerVector || value instanceof Vector3d || value instanceof String || value instanceof StringVector || value instanceof BooleanVector )
//			ret = "" + value;
		else if( value instanceof Appearance && ((Appearance) value).getColoringAttributes() != null ) {
			ret = ((Appearance) value).getColoringAttributes().toString();
		}
		else if( value instanceof double[] ) {
			ret = propertyFormatDoubleArray( (double[])value );
		}

		else {
//			ret = value.getClass().getName() + "@" + Integer.toHexString( value.hashCode() );
			try {
				ret = value.toString();
			} catch (ConcurrentModificationException e) {
				return "";
			}
		}

		return ret;
	}

	public static String propertyFormatDoubleArray( double[] value ) {
		StringBuilder ret = new StringBuilder("[");
		for ( int i = 0; i < (value).length; i++ ){
			if( i != 0 ) {
				ret.append(", ");
			}
			ret.append(value[i]);
		}
		ret.append(" ]");
		return ret.toString();
	}

	/**
	 * This method returns the distance between two Vector3d points
	 * @param from
	 * @param to
	 * @return
	 */
	public static double distanceFrom_To( Vector3d from, Vector3d to ) {
    	Vector3d dist = new Vector3d( from );
    	dist.sub( to );
    	return dist.length();
	}

	/**
	 * Return the intersection position for two vectors.
	 * The method returns the distances a1 and a2 along each vector where the intersection occurs.
	 * a1 = ((p2 - p1) x n2) / (n1 x n2)
	 * a2 = ((p2 - p1) x n1) / (n1 x n2)
	 *
	 * @param p1 = start of vector 1
	 * @param n1 = unit vector in direction of vector 1
	 * @param p2 = start of vector 2
	 * @param n2 = unit vector in direction of vector 2
	 */
	public static DoubleVector getIntersectionPosition( Vector3d p1, Vector3d n1, Vector3d p2, Vector3d n2 ) {

		// Calculate the Vector between the start of each vector
		Vector3d diff = new Vector3d();
		diff.sub( p2, p1 );

		// Calculate the top cross product (P2-P1) X n2
		Vector3d crossProductTop1 = new Vector3d();
		Vector3d crossProductTop2 = new Vector3d();
		crossProductTop1.cross( diff, n2 );
		crossProductTop2.cross( diff, n1 );

		// Calculate the bottom cross product (n1 X n2)
		Vector3d crossProductBottom = new Vector3d();
		crossProductBottom.cross( n1 , n2 );

		// Calculate the distance from the start point to the intersect point
		double a1 = crossProductTop1.z / crossProductBottom.z;
		double a2 = crossProductTop2.z / crossProductBottom.z;

		DoubleVector ret = new DoubleVector( 2, 1 );
		ret.add( a1 );
		ret.add( a2 );
		return ret;
	}

	//	public static String propertyFormatVector( Vector value ) {
//
//		String ret;
//		Object item;
//
//		if( value.size() > 0 ) {
//			item = value.get( 0 );
//			ret = "{ " + propertyFormatObject( item );
//
//			for( int i = 1; i < value.size(); i++ ) {
//				item = value.get( i );
//				ret = ret + propertyFormatObject( item );
//			}
//			ret = ret + " }";
//		}
//		else {
//			ret = "{ }";
//		}
//		return ret;
//	}
    //////////////////////// ************************************** /////////////////////////////

    /**
     * Return the coordinates obtained by rotating the given point counter-clockwise the given number of degrees about the given center.
     */
    public static Point2d rotatePoint_degrees_center( Point2d pt, double theta, Vector3d cent ) {

		double rads = theta * Math.PI / 180.0;

		double newX = (pt.x-cent.x)*Math.cos(rads) - (pt.y-cent.y)*Math.sin(rads) + cent.x;
		double newY = (pt.x-cent.x)*Math.sin(rads) + (pt.y-cent.y)*Math.cos(rads) + cent.y;

		return new Point2d( newX, newY );
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

    /**
     * Returns the pixel height for a specified font
     * @param font
     * @return
     */
    public static int getPixelHeightForFont( Font font ) {
		FontMetrics metrics = new FontMetrics(font) {        };
		Rectangle2D bounds = metrics.getStringBounds( "I", null);
		return (int) bounds.getHeight();
    }
	/**
	 *	Returns a string of the elements of the StringVector separated by spaces.
	 **/
	public static String getStringForData(StringVector data) {
		StringBuilder temp = new StringBuilder();
		for (String each : data) {
			if (temp.length() > 0)
				temp.append(" ");
			temp.append(each);
		}

		return temp.toString().trim();
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
}
