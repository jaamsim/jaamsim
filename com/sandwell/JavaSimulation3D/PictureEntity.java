/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.util.Image;

/**
 * PictureEntity is an object which shows a picture in a region
 *
 * April 7, 2009 by ARM
 *
 * TODO: It does not show a transparent background properly if the center.y is
 * lower than the currentRegion.center.y
 *
 */
public class PictureEntity extends DisplayEntity {


	private Image image = null;

	//protected BranchGroup collapsedModel;		// alternate appearance when the region is collapsed


	/**
	 * Constrcutor creating a new locale in the simulation universe.
	 */
	public PictureEntity() {
		super();

		((BooleanInput)this.getInput("Movable")).setDefaultValue(false);
		addEditableKeyword( "Image",          "", "",           false, "Graphics" );
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 *
	 * Reads keyword from a configuration file:
	 *	 BKIMAGE	 - filename for the background image of the region
	 */
	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {

		if( "IMAGE".equalsIgnoreCase( keyword ) ) {
			String imageName = Util.getStringForData(data);
			try {
				setImage(imageName);
				return;
			}
			catch( Exception e ) {
				throw new InputErrorException( (("The value " + imageName + " for " + getName()) + " BKIMAGE is invalid") );
			}
		}

		// keyword is not Region specific, try DisplayEntity
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	/**
	 *	assigns a background image for the PictureEntity
	 **/
	public void setImage( String filename ) {

		if( image != null ) {
			getModel().removeChild( image );
		}

		try {
			// Determine the url of the file inside the jar file
			String relativeURL = FileEntity.getRelativeURL( filename );
			java.net.URL url = Simulation.class.getResource(relativeURL);

			// Is the file inside the jar file?
			if( url != null ) {
				image = new Image( 1.0, 1.0, url );
			}
			else {
				String pathSpecified = filename;
				String pathAbsolute;

				java.io.File absFile = new java.io.File( pathSpecified );
				if ( absFile.isAbsolute() ) {
					pathAbsolute = absFile.getCanonicalPath();
				} else {
					pathAbsolute = FileEntity.getRootDirectory() + System.getProperty( "file.separator" ) + pathSpecified;
				}
				filename = pathAbsolute;

				java.io.File absFilename = new java.io.File( filename );
				if ( !absFilename.exists() ) {
					//messageGenerator.writeInputWarning( "The image file " + filename + " does not exist." );
					//return;
					throw new InputErrorException( "The image file " + filename + " does not exist." );
				}

				image = new Image( 1.0, 1.0, filename );
			}
		} catch ( Exception e ) {
			throw new ErrorException( e );
		}

		this.getModel().addChild( image );
		this.setRegion( currentRegion );
		this.enterRegion();
	}
}