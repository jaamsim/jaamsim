/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import com.jaamsim.math.Color4d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vector3dInput;

public class DisplayModel extends Entity {
	// IMPORTANT: If you add a tag here, make sure to add it to the validTags
	public static final String TAG_CONTENTS = "CONTENTS";
	public static final String TAG_OUTLINES = "OUTLINES";
	public static final String TAG_TRACKFILL = "TRACKFILL";
	public static final String TAG_BODY = "BODY";
	public static final String TAG_SERVICE = "SERVICE";
	public static final String TAG_LINES = "LINES";
	public static final String TAG_SECONDARY_LINES = "SECONDARY_LINES";
	public static final String TAG_ARROW_DOWN = "ARROWS_DOWN";
	public static final String TAG_ARROW_UP = "ARROWS_UP";

	private static final ArrayList<DisplayModel> allInstances;

	protected static final ArrayList<String> validTags;

	private double conversionFactorToMeters = 1.0d; // How many meters in one distance unit

	@Keyword(desc = "The shape of a display model determines the appearance of the display model. The shape may be " +
	                "one of the following: Pixels (for a square of 6x6 pixels), Truck2D, Ship2D, Icon (for a rectangle), " +
	                "Circle.  A graphics file name with one of the following file extensions: DAE (for a collada version " +
	                "1.4.1 3D model file), BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel Shape { ..\\3DModels\\Ship3D.dae }")
	private final StringInput shape;
	private static ArrayList<String> definedTypes;
	private static ArrayList<String>validFileExtentions;

	@Keyword(desc = "Euler angles in radians defining the rotation of the display model." +
	                "Only for imported collada models.",
	         example = "Ship3DModel Orientation { 0 0 1.5707963 }")
	private final Vector3dInput orientation;


	@Keyword(desc = "If the value is TRUE, then load the collada file as it is. Otherwise, ignore all the culling in " +
	                "the model",
	         example = "Ship3DModel EnableCulling { FALSE }")
	private final BooleanInput enableCulling;

	@Keyword(desc = "The colour for the filled part of the display model.",
	         example = "Product2D FillColour { red }")
	private final ColourInput fillColour;

	@Keyword(desc = "The colour for the outline part of the display model.",
	         example = "Berth2D OutlineColour { magenta }")
	private final ColourInput outlineColour;

	@Keyword(desc = "If the value is true, then the display model will have a solid fill. Otherwise, the display model " +
	                "will appear as hollow.",
	         example = "Berth2D Filled { FALSE }")
	private final BooleanInput filled;

	@Keyword(desc = "If the value is true, then the display model outline will be a dashed line. Otherwise, the outline " +
	                "will be a solid line.",
	         example = "StockpileLine2D Dashed { TRUE }")
	private final BooleanInput dashed;

	@Keyword(desc = "If the value is true, then the display model outline will be a bold line. Otherwise the outline " +
	                "will be one pixel wide line.",
	         example = "Berth2D Bold { TRUE }")
	private final BooleanInput bold;

	@Keyword(desc = "Indicates the loaded image has an alpha channel (transparency information) that should be used " +
            "(this only affects image DisplayModels).",
     example = "CompanyLogo Transparent { TRUE }")
	private final BooleanInput transparent;
	@Keyword(desc = "Indicates the loaded image should use texture compression in video memory " +
	                "(this only affects image DisplayModels).",
	         example = "WorldMap CompressedTexture { TRUE }")
	private final BooleanInput compressedTexture;

	// A 'dirty' state tracker
	private ChangeWatcher graphicsDirtier = new ChangeWatcher();

	static {
		allInstances = new ArrayList<DisplayModel>();

		definedTypes = new ArrayList<String>(27);
		definedTypes.add("PIXELS");
		definedTypes.add("TRUCK2D");
		definedTypes.add("SHIP2D");
		definedTypes.add("RECTANGLE");
		definedTypes.add("STACKER2D");
		definedTypes.add("RECLAIMER2D");
		definedTypes.add("BRIDGE2D");
		definedTypes.add("CRUSHER2D");
		definedTypes.add("GANTRY2D");
		definedTypes.add("DOZER2D");
		definedTypes.add("CRUSHER2ND2D");
		definedTypes.add("SLAVESTACKER2D");
		definedTypes.add("DUALQUADRANT2D");
		definedTypes.add("SINGLEQUADRANT2D");
		definedTypes.add("LINEAR2D");
		definedTypes.add("TRAVELLING2D");
		definedTypes.add("CIRCLE");
		definedTypes.add("ARROW2D");
		definedTypes.add("TRIANGLE");
		definedTypes.add("CONTENTSPIXELS");
		definedTypes.add("CRUSHINGPLANT2D");
		definedTypes.add("BARGAUGE2D");
		definedTypes.add("MINISHIP2D");
		definedTypes.add("GRINDINGROLL2D");
		definedTypes.add("SCREEN2D");
		definedTypes.add("SAGMILL2D");
		definedTypes.add("RECTANGLEWITHARROWS");

		validFileExtentions = new ArrayList<String>(6);
		validFileExtentions.add("DAE");
		validFileExtentions.add("ZIP");
		validFileExtentions.add("KMZ");
		validFileExtentions.add("BMP");
		validFileExtentions.add("JPG");
		validFileExtentions.add("PNG");
		validFileExtentions.add("PCX");
		validFileExtentions.add("GIF");

		validTags = new ArrayList<String>();
		validTags.add(TAG_CONTENTS);
		validTags.add(TAG_OUTLINES);
		validTags.add(TAG_TRACKFILL);
		validTags.add(TAG_BODY);
		validTags.add(TAG_SERVICE);
		validTags.add(TAG_LINES);
		validTags.add(TAG_SECONDARY_LINES);
		validTags.add(TAG_ARROW_DOWN);
		validTags.add(TAG_ARROW_UP);
	}
	{
		shape = new StringInput( "Shape", "DisplayModel", null );
		this.addInput( shape, true);

		orientation = new Vector3dInput("Orientation", "DisplayModel", new Vector3d(0, 0, 0));
		orientation.setUnits("rad");
		this.addInput(orientation, true);

		enableCulling = new BooleanInput("EnableCulling", "DisplayModel", true);
		this.addInput(enableCulling, true);

		fillColour = new ColourInput("FillColour", "DisplayModel", ColourInput.MED_GREY);
		this.addInput(fillColour, true, "FillColor");

		outlineColour = new ColourInput("OutlineColour", "DisplayModel", ColourInput.BLACK);
		this.addInput(outlineColour, true, "OutlineColor");

		filled = new BooleanInput("Filled", "DisplayModel", true);
		this.addInput(filled, true);

		dashed = new BooleanInput("Dashed", "DisplayModel", false);
		this.addInput(dashed, true);

		bold = new BooleanInput("Bold", "DisplayModel", false);
		this.addInput(bold, true);

		transparent = new BooleanInput("Transparent", "DisplayModel", false);
		this.addInput(transparent, true);

		compressedTexture = new BooleanInput("CompressedTexture", "DisplayModel", false);
		this.addInput(compressedTexture, true);
}

	public DisplayModel(){
		allInstances.add(this);
	}

	public static ArrayList<? extends DisplayModel> getAll() {
		return allInstances;
	}

	public void validate()
	throws InputErrorException {
		super.validate();
		if( shape.getValue() == null ) {
			throw new InputErrorException( "Shape is not found" );
		}
		if( ! definedTypes.contains(shape.getValue().toUpperCase()) ){
			if( ! (FileEntity.fileExists(shape.getValue())) ) {
				throw new InputErrorException("File \"%s\" not found", shape.getValue());
			}
			else{
				String ext = Util.getFileExtention(shape.getValue());
				if(! validFileExtentions.contains(ext)){
					throw new InputErrorException("Invalid file format \"%s\"", shape.getValue());
				}
			}
		}
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public static ArrayList<String> getValidExtentions() {
		return validFileExtentions;
	}

	public double getConversionFactorToMeters() {
		return conversionFactorToMeters;
	}

	public String getShape() {
		return shape.getValue();
	}

	public Color4d getFillColor() {
		return fillColour.getValue();
	}

	public Color4d getOutlineColor() {
		return outlineColour.getValue();
	}

	public boolean isFilled() {
		return filled.getValue();
	}

	public boolean isBold() {
		return bold.getValue();
	}

	public boolean isTransparent() {
		return transparent.getValue();
	}

	public boolean useCompressedTexture() {
		return compressedTexture.getValue();
	}

	public static DisplayModel getDefaultDisplayModelForClass(Class<? extends DisplayEntity> theClass) {
		for( ObjectType type : ObjectType.getAll() ) {
			if(type.getJavaClass() == theClass) {
				return type.getDefaultDisplayModel();
			}
		}
		return null;
	}

	public ChangeWatcher getGraphicsDirtier() {
		return graphicsDirtier;
	}

	/**
	 * This method updates the DisplayEntity for changes in the given input
	 */
	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		graphicsDirtier.changed();
	}
}
