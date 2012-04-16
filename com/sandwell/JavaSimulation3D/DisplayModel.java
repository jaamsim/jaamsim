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

import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.Light;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Point2d;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

import com.eteks.sweethome3d.j3d.DAELoader;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vector3dInput;
import com.sandwell.JavaSimulation3D.util.Arc;
import com.sandwell.JavaSimulation3D.util.Circle;
import com.sandwell.JavaSimulation3D.util.Image;
import com.sandwell.JavaSimulation3D.util.Line;
import com.sandwell.JavaSimulation3D.util.PointWithSize;
import com.sandwell.JavaSimulation3D.util.Polygon;
import com.sandwell.JavaSimulation3D.util.RectangleArray;
import com.sandwell.JavaSimulation3D.util.Shape;
import com.sandwell.JavaSimulation3D.util.Rectangle;
import com.sandwell.JavaSimulation3D.util.SideBySideBars;
import com.sandwell.JavaSimulation3D.util.StackedBar;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.Scene;

public class DisplayModel extends Entity {
	// IMPORTANT: If you add a tag here, make sure to add it to the validTags
	public static final String TAG_CONTENTS = "CONTENTS";
	public static final String TAG_OUTLINES = "OUTLINES";
	public static final String TAG_TRACKFILL = "TRACKFILL";
	public static final String TAG_BODY = "BODY";
	public static final String TAG_SERVICE = "SERVICE";
	public static final String TAG_LINES = "LINES";
	public static final String TAG_SECONDARY_LINES = "SECONDARY_LINES";

	private static final ArrayList<DisplayModel> allInstances;

	// IMPORTANT: If you add a pre-defined model here, make sure to add it to the preDefined2DTypes
	// 1) Predefined Models
	private static final int MODEL_PIXELS = 0;
	private static final int MODEL_TRUCK2D = 1;
	private static final int MODEL_SHIP2D = 2;
	private static final int MODEL_RECTANGLE = 3;

	// Stacker Reclaimer
	private static final int MODEL_STACKER2D = 4;
	private static final int MODEL_RECLAIMER2D = 5;
	private static final int MODEL_BRIDGE2D = 6;
	private static final int MODEL_CRUSHER2D = 7;
	private static final int MODEL_GANTRY2D = 8;
	private static final int MODEL_DOZER2D = 9;
	private static final int MODEL_CRUSHER2ND2D = 10;
	private static final int MODEL_SLAVE_STACKER2D = 11;

	// ShipLoader
	private static final int MODEL_DUALQUADRANT2D = 12;
	private static final int MODEL_SINGLEQUADRANT2D = 13;
	private static final int MODEL_LINEAR2D = 14;
	private static final int MODEL_TRAVELLING2D = 15;

	// TrainGenerator
	private static final int MODEL_CIRCLE = 16;

	private static final int MODEL_ARROW2D = 17;
	private static final int MODEL_TRIANGLE = 18;
	private static final int MODEL_CONTENTS_PIXELS = 19;

	private static final int MODEL_CRUSHING_PLANT2D = 20;
	private static final int MODEL_BARGAUGE2D = 21;
	private static final int MODEL_MINISHIP2D = 22;

	// 2) Model from a file
	private static final int MODEL_FILE = 23;

	protected static final ArrayList<String> validTags;

	protected final Point3d modelSize;
	private double conversionFactorToMeters = 1.0d; // How many meters in one distance unit
	private final StringInput shape;
	private static ArrayList<String>validPredefined2DTypes;
	private static ArrayList<String>validFileExtentions;
	private int modelType;

	private BranchGroup branchGroup;		// Shapes are being added to this node

	private final Vector3dInput orientation; // Only for imported collada models

	private final BooleanInput enableCulling;
	private final ColourInput fillColour;
	private final ColourInput outlineColour;
	private final BooleanInput filled;
	private final BooleanInput dashed;
	private final BooleanInput bold;

	// Geometry information
	private int numberOfShape3Ds;
	private final StringVector  geometriesName = new StringVector();
	private final IntegerVector geometriesCount = new IntegerVector();
	private final IntegerVector geometriesTotalUniqueVertices = new IntegerVector();
	private final IntegerVector geometriesTotalVertices = new IntegerVector();
	private int	totalUniqueGeometries = 0;

	private boolean hasSharedGroup=false;

	private BufferedImage highResImage;
	private BufferedImage lowResImage;

	static {
		allInstances = new ArrayList<DisplayModel>();

		validPredefined2DTypes = new ArrayList<String>(1);
		validPredefined2DTypes.add("PIXELS");
		validPredefined2DTypes.add("TRUCK2D");
		validPredefined2DTypes.add("SHIP2D");
		validPredefined2DTypes.add("RECTANGLE");
		validPredefined2DTypes.add("STACKER2D");
		validPredefined2DTypes.add("RECLAIMER2D");
		validPredefined2DTypes.add("BRIDGE2D");
		validPredefined2DTypes.add("CRUSHER2D");
		validPredefined2DTypes.add("GANTRY2D");
		validPredefined2DTypes.add("DOZER2D");
		validPredefined2DTypes.add("CRUSHER2ND2D");
		validPredefined2DTypes.add("SLAVESTACKER2D");
		validPredefined2DTypes.add("DUALQUADRANT2D");
		validPredefined2DTypes.add("SINGLEQUADRANT2D");
		validPredefined2DTypes.add("LINEAR2D");
		validPredefined2DTypes.add("TRAVELLING2D");
		validPredefined2DTypes.add("CIRCLE");
		validPredefined2DTypes.add("ARROW2D");
		validPredefined2DTypes.add("TRIANGLE");
		validPredefined2DTypes.add("CONTENTSPIXELS");
		validPredefined2DTypes.add("CRUSHINGPLANT2D");
		validPredefined2DTypes.add("BARGAUGE2D");
		validPredefined2DTypes.add("MINISHIP2D");

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
	}
	{
		shape = new StringInput( "Shape", "DisplayModel", null );
		this.addInput( shape, true);

		orientation = new Vector3dInput("Orientation", "DisplayModel", new Vector3d(0, 0, 0));
		orientation.setUnits("rad");
		this.addInput(orientation, true);

		enableCulling = new BooleanInput("EnableCulling", "DisplayModel", true);
		this.addInput(enableCulling, true);

		fillColour = new ColourInput("FillColour", "DisplayModel", Shape.getPresetColor(Shape.COLOR_MED_GREY));
		this.addInput(fillColour, true, "FillColor");

		outlineColour = new ColourInput("OutlineColour", "DisplayModel", Shape.getPresetColor(Shape.COLOR_BLACK));
		this.addInput(outlineColour, true, "OutlineColor");

		filled = new BooleanInput("Filled", "DisplayModel", true);
		this.addInput(filled, true);

		dashed = new BooleanInput("Dashed", "DisplayModel", false);
		this.addInput(dashed, true);

		bold = new BooleanInput("Bold", "DisplayModel", false);
		this.addInput(bold, true);
	}

	public DisplayModel(){
		allInstances.add(this);
		modelType = 0;
		modelSize = new Point3d(1.0, 1.0, 1.0);
	}
	public static ArrayList<? extends DisplayModel> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
		if(branchGroup != null) {
			branchGroup.removeAllChildren();
		}
	}


	public static ArrayList<String> getValidExtentions() {
		return validFileExtentions;
	}

	private void initialize() {

		// Already has been initialized
		if(branchGroup != null){
			return;
		}

		// 1) Predefined 2D model
		if(validPredefined2DTypes.contains( shape.getValue().toUpperCase()) ){
			modelType = validPredefined2DTypes.indexOf(shape.getValue().toUpperCase());
			branchGroup = getPredefined2DBranchGroup();
		}

		// 2) Model from a file
		else {
			branchGroup = new BranchGroup();
			modelType = MODEL_FILE;
			String ext =  Util.getFileExtention(shape.getValue());
			if( ext.equalsIgnoreCase("DAE") || ext.equalsIgnoreCase("ZIP") || ext.equalsIgnoreCase("KMZ") ){
				Scene scene = null;

				// Load 3d Model file
				DAELoader collada = new DAELoader();

				// Ask loader to ignore lights, fogs...
				collada.setFlags(collada.getFlags() & ~(Loader.LOAD_LIGHT_NODES | Loader.LOAD_FOG_NODES
		                | Loader.LOAD_BACKGROUND_NODES | Loader.LOAD_VIEW_GROUPS));
				URL url=null;
				try {
					url = new URL(Util.getAbsoluteFilePath(shape.getValue()));

					// Zip file or GoogleEarth file
					if(ext.equalsIgnoreCase("ZIP") || ext.equalsIgnoreCase("KMZ")) {

						// Open the zip file
						ZipInputStream zipInputStream = new ZipInputStream(url.openStream());

						// Loop through zipEntries
						for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null; ) {

							String entryName = zipEntry.getName();
							if(!Util.getFileExtention(entryName).equalsIgnoreCase("DAE"))
								continue;

							// This zipEntry is a collada file, no need to look any further
							url = new URL("jar:" + url + "!/" + entryName );
							break;
						}
					}

					// Load the collada file
					scene = collada.load ( url );
					Object unitMeter = scene.getNamedObjects().remove("JaamSim-UnitMeter");
					if (unitMeter instanceof Double) {
						conversionFactorToMeters = ((Double)unitMeter).doubleValue();
					}
					if (Boolean.TRUE == scene.getNamedObjects().remove("JaamSim-HasSharedGroup")) {
						hasSharedGroup = true;
					}
				}
				catch (Exception ex){
					scene = null;
					throw new ErrorException( "%s \nCould not load %s", ex, url );
				}
				Transform3D rotation = new Transform3D();
				rotation.setEuler(orientation.getValue());
				TransformGroup tg = new TransformGroup(rotation);
				tg.addChild(scene.getSceneGroup());

				branchGroup.addChild(tg);
				if (scene.getSceneGroup().numChildren() == 0) {
			          throw new IllegalArgumentException("Empty model:" + url);
			    }

		        // Turn off lights because of the universal light in the region
		        setNodeLightOff(branchGroup);
			}

			// Image file
			else {
				Image image;
				try {
					image = new Image( 1.0, 1.0, new URL(Util.getAbsoluteFilePath(shape.getValue())) );
				}
				catch( Exception e ) {
					throw new ErrorException( "Could not load %s", shape.getValue() );
				}
				branchGroup.addChild(image);
			}
			this.forceDuplicateApperanceOfValidImportedTags(branchGroup);

			ArrayList<Geometry>	uniqueGeometries = new ArrayList<Geometry>();
			ArrayList<Shape3D> shape3DList = getShape3DsBelow(branchGroup);
			for(Shape3D each: shape3DList) {

				// NO culling at all
				if(! enableCulling.getValue()) {
					int culling = PolygonAttributes.CULL_NONE;

					Appearance appearance = each.getAppearance();
					if(appearance != null) {
						PolygonAttributes polygonAttributes = appearance.getPolygonAttributes();
						if( polygonAttributes != null){
							polygonAttributes.setCullFace(culling);
						}
						else {
							polygonAttributes = new PolygonAttributes();
							polygonAttributes.setCullFace(culling);
							appearance.setPolygonAttributes(polygonAttributes);
						}
					}
				}

				// 2) Geometry information
				numberOfShape3Ds++;
				if(each.getGeometry() != null ) {
					geometriesName.addLastIfNotPresent(each.getGeometry().getClass().toString());

					// New geometry is added
					if(geometriesName.size() > geometriesCount.size()) {
						geometriesCount.add(0);
						geometriesTotalUniqueVertices.add(0);
						geometriesTotalVertices.add(0);
					}

					// Geometry type in each
					int index = geometriesName.indexOf(each.getGeometry().getClass().toString());

					geometriesCount.addAt(each.numGeometries(), index);
					Enumeration<?> enumeration = each.getAllGeometries();

					// Total vertices for each
					while (enumeration.hasMoreElements()) {
						Object obj = enumeration.nextElement();
						if(! (obj instanceof GeometryArray) ) {
							System.out.println(obj);
							break;
						}
						GeometryArray geometryArray = (GeometryArray) obj;
						if(uniqueGeometries.contains(geometryArray)){
							geometriesTotalVertices.addAt(geometryArray.getVertexCount(), index);
						}
						else {
							totalUniqueGeometries++;
							uniqueGeometries.add(geometryArray);
							geometriesTotalUniqueVertices.addAt(geometryArray.getVertexCount(), index);
							geometriesTotalVertices.addAt(geometryArray.getVertexCount(), index);
						}
					}

				}
			}
		}

		// Compute the original modelSize

		// Assume an infinite boundary
		BoundingBox boundary = new BoundingBox(
				new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
				new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY) );

		DAELoader.computeBounds (branchGroup, boundary, new Transform3D());

		Point3d lower; // Lower bounds of the Model
		lower = new Point3d();
		boundary.getLower(lower);
		boundary.getUpper(modelSize);
		modelSize.sub( lower );
		modelSize.absolute();
	}

	public boolean hasSharedGroup() {
		return hasSharedGroup;
	}

	/**
	 * Turn off all the light in the node
	 */
	private void setNodeLightOff(Node node) {
		if (node instanceof Light) {
			((Light)node).setEnable(false);
		}
		else if (node instanceof Link) {
			setNodeLightOff(((Link)node).getSharedGroup());
		}
		else if (node instanceof Group) {
			Enumeration<?> enumeration = ((Group)node).getAllChildren();
			while (enumeration.hasMoreElements()) {
				setNodeLightOff((Node)enumeration.nextElement());
			}
		}
	}

	/**
	 * predefined 2D models are defined here
	 */
	private BranchGroup getPredefined2DBranchGroup(){
		BranchGroup bg = new BranchGroup();
		switch (modelType){
			case MODEL_PIXELS:
				PointWithSize pixels = new PointWithSize(6.0f, false, "pixels" );
				pixels = new PointWithSize(6.0f, false, "pixels" );
				pixels.setColor(Shape.getColorWithName("black"));
				pixels.setName(TAG_BODY);
				bg.addChild(pixels);
				break;
			case MODEL_CONTENTS_PIXELS:
				// When the contents disappears it shows a white pixel
				OrderedGroup orderedGroup = new OrderedGroup();
				pixels = new PointWithSize(6.0f, false, "pixels" );
				pixels.setColor(Shape.getColorWithName("white"));
				orderedGroup.addChild(pixels);
				pixels = new PointWithSize(6.0f, false, "pixels" );
				pixels.setColor(Shape.getColorWithName("black"));
				pixels.setName(TAG_CONTENTS);
				orderedGroup.addChild(pixels);
				bg.addChild(orderedGroup);
				break;
			case MODEL_TRUCK2D:
				orderedGroup = getDisplayModelForTruck2D();
				Rectangle rearTruck = new Rectangle(-0.125d, 0.0d, 0.0d, 0.75d, 1.0d, Rectangle.SHAPE_FILLED);
				rearTruck.setColor(Shape.getPresetColor(Shape.COLOR_WHITE));
				orderedGroup.addChild(rearTruck);
				StackedBar rectangleContents = new StackedBar();
				rectangleContents.setDimension(new Point2d( 0.75, 1.0 ));
				rectangleContents.setCenter(-0.5, 0.0, 0.0);

				// Make rectangleContents accessible
				rectangleContents.setName(TAG_CONTENTS);

				orderedGroup.addChild(rectangleContents);
				bg.addChild(orderedGroup);
				break;
			case MODEL_SHIP2D:
				bg.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
				bg.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
				orderedGroup = getDisplayModelForShip2D();
				rectangleContents = new StackedBar();
				rectangleContents.setDimension(new Point2d( 0.650, 0.6 ));
				rectangleContents.setCenter(-0.225, 0.0, 0.0);
				rectangleContents.setName(TAG_CONTENTS);
				orderedGroup.addChild(rectangleContents);
				bg.addChild(orderedGroup);
				break;
			case MODEL_RECTANGLE:
				bg.addChild(getDisplayModelForRectangle());
				break;
			case MODEL_STACKER2D:
			case MODEL_RECLAIMER2D:
				bg.addChild(getDisplayModelForStakerReclaimer2D());
				break;
			case MODEL_BRIDGE2D:
				bg.addChild(getDisplaModelForBridge2D());
				break;
			case MODEL_CRUSHER2D:
				bg.addChild(getDisplayModelForCrusher2D());
				break;
			case MODEL_GANTRY2D:
				bg.addChild(getDisplayModelForGantry2D());
				break;
			case MODEL_DOZER2D:
				bg.addChild(getDisplayModelForDozer2D());
				break;
			case MODEL_CRUSHER2ND2D:
				bg.addChild(getDisplayModelForCrusher2nd2D());
				break;
			case MODEL_SLAVE_STACKER2D:
				bg.addChild(getDisplayModelForSlaveStacker2D());
				break;
			case MODEL_DUALQUADRANT2D:
				bg.addChild(getDisplayModelForDualQuadrantGraphics());
				break;
			case MODEL_SINGLEQUADRANT2D:
				bg.addChild(getDisplayModelForSingleQuadrantGraphics());
				break;
			case MODEL_LINEAR2D:
				bg.addChild(getDisplayModelForLinearGraphics());
				break;
			case MODEL_TRAVELLING2D:
				bg.addChild(getDisplayModelForTravellingGraphics());
				break;
			case MODEL_CIRCLE:
				bg.addChild(getDisplayModelForCircle());
				break;
			case MODEL_ARROW2D:
				double [] verts = new double[] {-0.5f, 0.0f, 0.0f,
									  -0.1f, -0.5f, 0.0f,
									  -0.1f, -0.2f, 0.0f,
									  0.5f, -0.2f, 0.0f,
									  0.5f, 0.2f, 0.0f,
									  -0.1f, 0.2f, 0.0f,
									  -0.1f, 0.5f, 0.0f };

				// Create the arrow fill
				Shape fillModel = new Polygon( verts, Polygon.SHAPE_FILLED, "fillModel" );
				fillModel.setColor( 1 );
				fillModel.setName(TAG_CONTENTS);
				bg.addChild( fillModel );

				// Create the arrow outline
				Shape outlineModel = new Polygon( verts, Polygon.SHAPE_OUTLINE, "outlineModel" );
				outlineModel.setColor( 1 );
				outlineModel.setName(TAG_OUTLINES);
				bg.addChild( outlineModel );
				break;
			case MODEL_TRIANGLE:
				verts = new double[] {  0.5f, -0.5f, 0.0f,
										0.5f, 0.5f, 0.0f,
										-0.5f, 0.0f, 0.0f };
				Polygon myFill = new Polygon( verts, Polygon.SHAPE_FILLED, "myFill" );
				myFill.setColor( 6 );
				myFill.setName(TAG_CONTENTS);
				bg.addChild( myFill );
				Polygon myOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "myOutline" );
				myOutline.setColor( 1 );
				myOutline.setLayer( 1 );
				myOutline.setName(TAG_OUTLINES);
				bg.addChild( myOutline );
				break;
			case MODEL_CRUSHING_PLANT2D:
				bg.addChild(getDisplayModelForCrushingPlant2D());
				break;
			case MODEL_BARGAUGE2D:
				bg.addChild(getDisplayModelForBarGauge2D());
				break;
			case MODEL_MINISHIP2D:
				bg.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
				bg.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
				orderedGroup = getDisplayModelForMiniShip2D();
				rectangleContents = new StackedBar();
				rectangleContents.setDimension(new Point2d( 0.00650, 0.006 ));
				rectangleContents.setCenter(-0.00225, 0.0, 0.0);
				rectangleContents.setName(TAG_CONTENTS);
				orderedGroup.addChild(rectangleContents);
				bg.addChild(orderedGroup);
				break;
		}
		return bg;
	}

	/**
	 * Return a copy of DisplayModel which fits in 1 * 1 * 1 cube
	 * @return
	 */
	public BranchGroup getUnitDisplayModel() {

		DisplayModelBG dm = getDisplayModel();

		Transform3D	size = new Transform3D();

		// Isometric view
		if(modelSize.z > 0.00d) {
			size.rotX(-Math.acos(Math.tan(Math.PI/6.0d)));
			Transform3D tempRotate = new Transform3D();
			tempRotate.rotZ(-Math.PI/4.0d);
			size.mul(tempRotate);
		}

		// Find the longest side
		double maxLength = modelSize.x;
		if(maxLength < modelSize.y)
			maxLength = modelSize.y;
		if(maxLength < modelSize.z)
			maxLength = modelSize.z;

		// Divide the scale by the longest side size
		if(maxLength != 0.0d)
			size.setScale(1.0d/maxLength);

		TransformGroup tg = new TransformGroup(size);
		tg.addChild(dm);
		BranchGroup bg = new BranchGroup();

		// Can be removed
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.addChild(tg);
		return bg;
	}

	public DisplayModelBG getDisplayModel() {
		this.initialize();
		BranchGroup bg;
		if(modelType == MODEL_FILE) {
			bg = (BranchGroup)branchGroup.cloneTree();
		}
		else {
			bg = this.getPredefined2DBranchGroup();
		}
		DisplayModelBG displayModelBG = new DisplayModelBG(bg, new Vector3d(modelSize));
		return displayModelBG;
	}

	public Point3d getModelSize() {
		return modelSize;
	}

	public double getConversionFactorToMeters() {
		return conversionFactorToMeters;
	}

	public static DisplayModel getDefaultDisplayModelForClass(Class<? extends DisplayEntity> theClass) {
		for( ObjectType type : ObjectType.getAll() ) {
			if(type.getJavaClass() == theClass) {
				return type.getDefaultDisplayModel();
			}
		}
		return null;
	}

	public static ArrayList<Shape3D> getShape3DsBelow(Node node) {
		ArrayList<Shape3D> shape3DList = new ArrayList<Shape3D>();
		getShape3DsBelow(node, shape3DList);
		return shape3DList;
	}

	public static boolean nodeHasValidTag(Node node) {
		return (node.getName() != null && validTags.contains(node.getName()));
	}

	private static void getShape3DsBelow(Node node, ArrayList<Shape3D> shape3DList){

		if (node instanceof Group) {

			Enumeration<?> enumeration = ((Group)node).getAllChildren();
			while (enumeration.hasMoreElements ()) {
				getShape3DsBelow((Node)enumeration.nextElement(), shape3DList);
			}
		} else if (node instanceof Link) {
			getShape3DsBelow(((Link)node).getSharedGroup(), shape3DList);
		} else if (node instanceof Shape3D) {
			shape3DList.add((Shape3D)node);
		}
	}

	public void validate()
	throws InputErrorException {
		super.validate();
		if( shape.getValue() == null ) {
			throw new InputErrorException( "Shape is not found" );
		}
		if( ! validPredefined2DTypes.contains(shape.getValue().toUpperCase()) ){
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

	/**
	 * Forces the appearance of the all sub Shape3D(s) in ValidImportedTags to be duplicated when the
	 * node is cloned. The appearance is determining the color and visibility of a Shape3D. Each
	 * cloned DisplayModel should be able to change the color and visibility of its own imported tags
	 */
	private void forceDuplicateApperanceOfValidImportedTags(Node node) {
		if(node == null)
			return;

		if(nodeHasValidTag(node)){
			ArrayList<Shape3D> shape3DList = getShape3DsBelow(node);
			for(Shape3D each: shape3DList) {

				// Force the appearance to duplicate upon cloning
				Appearance appearance = each.getAppearance();
				if(appearance != null) {
					Material material = appearance.getMaterial();
					if(material != null) {
						material.setDuplicateOnCloneTree(true);
					}

					RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
					if(renderingAttributes != null) {
						renderingAttributes.setDuplicateOnCloneTree(true);
					}
				}
			}
		}

		if (node instanceof Group) {
			Enumeration<?> enumeration = ((Group)node).getAllChildren();
			while (enumeration.hasMoreElements ()) {
				forceDuplicateApperanceOfValidImportedTags((Node)enumeration.nextElement());
			}
		} else if (node instanceof Link) {
			forceDuplicateApperanceOfValidImportedTags(((Link)node).getSharedGroup());
		}
	}

	/**************************  Methods For Creating Predefined 2D Models  **************************/

	private OrderedGroup getDisplayModelForTruck2D(){
		OrderedGroup model2D = new OrderedGroup();

		Rectangle cab = new Rectangle(0.375d, 0.0d, 0.0d, 0.25d, 1.0d, Rectangle.SHAPE_FILLED);
		cab.setColor(Shape.getPresetColor(Shape.COLOR_YELLOW));
		model2D.addChild(cab);
		return model2D;
	}

	private OrderedGroup getDisplayModelForShip2D() {
		OrderedGroup model2D = new OrderedGroup();

		double[] hullVerts = {
			-0.35625d, -0.5d, 0.0d,
			0.35d, -0.5d, 0.0d,
			0.40625d, -0.42d, 0.0d,
			0.459375d, -0.3d, 0.0d,
			0.484375d, -0.21d, 0.0d,
			0.5d, -0.05d, 0.0d,
			0.5d, 0.05d, 0.0d,
			0.484375d, 0.21d, 0.0d,
			0.459375d, 0.3d, 0.0d,
			0.40625d, 0.42d, 0.0d,
			0.35d, 0.5d, 0.0d,
			-0.35625d, 0.5d, 0.0d,
			-0.4109375d, 0.45d, 0.0d,
			-0.4515625d, 0.36d, 0.0d,
			-0.5d, 0.23d, 0.0d,
			-0.5d, -0.23d, 0.0d,
			-0.4515625d, -0.36d, 0.0d,
			-0.4109375d, -0.45d, 0.0d,
			-0.35625d, -0.5d, 0.0d };

		// create the hull shape
		Polygon hull = new Polygon( hullVerts, Polygon.SHAPE_FILLED, "hull" );
		hull.setPoints( hullVerts );
		hull.setColor(Shape.getColorWithName("gray50"));
		hull.setName(TAG_BODY);
		model2D.addChild( hull );

		// create the hull outline
		Polygon hullOutline = new Polygon( hullVerts, Polygon.SHAPE_OUTLINE, "hullOutline" );
		hullOutline.setColor( 1 );
		model2D.addChild( hullOutline );

		// Create the cabin
		Rectangle cabin = new Rectangle( 0.125, 0.7, Rectangle.SHAPE_FILLED, "cabin" );

		//Rectangle cabin = new Rectangle( 0.125, 0.7, Rectangle.SHAPE_OUTLINE );
		cabin.setColor( 1 );
		cabin.setCenter( -0.325, 0.0, 0.0 );
		model2D.addChild( cabin );

		// create the background to display the fill, make if visible above the outline
		//FIXME losing an outline here
		//Rectangle fillBackground = new Rectangle( 5.0, 0.7, Rectangle.SHAPE_FILLED );
		Rectangle fillBackground = new Rectangle( 0.650, 0.6, Rectangle.SHAPE_FILLED, "fillBackground" );
		fillBackground.setColor( 8 );
		fillBackground.setCenter( 0.1, 0.0, 0.0 );
		model2D.addChild( fillBackground );

		return model2D;
	}

	private OrderedGroup getDisplayModelForMiniShip2D() {
		OrderedGroup model2D = new OrderedGroup();

		double[] hullVerts = {
			-0.0035625d, -0.005d, 0.000d,
			0.0035d, -0.005d, 0.000d,
			0.0040625d, -0.0042d, 0.000d,
			0.00459375d, -0.003d, 0.000d,
			0.00484375d, -0.0021d, 0.000d,
			0.005d, -0.0005d, 0.000d,
			0.005d, 0.0005d, 0.000d,
			0.00484375d, 0.0021d, 0.000d,
			0.00459375d, 0.003d, 0.000d,
			0.0040625d, 0.0042d, 0.000d,
			0.0035d, 0.005d, 0.000d,
			-0.0035625d, 0.005d, 0.000d,
			-0.004109375d, 0.0045d, 0.000d,
			-0.004515625d, 0.0036d, 0.000d,
			-0.005d, 0.0023d, 0.000d,
			-0.005d, -0.0023d, 0.000d,
			-0.004515625d, -0.0036d, 0.000d,
			-0.004109375d, -0.0045d, 0.000d,
			-0.0035625d, -0.005d, 0.000d };

		// create the hull shape
		Polygon hull = new Polygon( hullVerts, Polygon.SHAPE_FILLED, "hull" );
		hull.setPoints( hullVerts );
		hull.setColor(Shape.getColorWithName("gray50"));
		hull.setName(TAG_BODY);
		model2D.addChild( hull );

		// create the hull outline
		Polygon hullOutline = new Polygon( hullVerts, Polygon.SHAPE_OUTLINE, "hullOutline" );
		hullOutline.setColor( 1 );
		model2D.addChild( hullOutline );

		// Create the cabin
		Rectangle cabin = new Rectangle( 0.00125, 0.007, Rectangle.SHAPE_FILLED, "cabin" );

		//Rectangle cabin = new Rectangle( 0.125, 0.7, Rectangle.SHAPE_OUTLINE );
		cabin.setColor( 1 );
		cabin.setCenter( -0.00325, 0.0, 0.0 );
		model2D.addChild( cabin );

		// create the background to display the fill, make if visible above the outline
		//FIXME losing an outline here
		//Rectangle fillBackground = new Rectangle( 5.0, 0.7, Rectangle.SHAPE_FILLED );
		Rectangle fillBackground = new Rectangle( 0.00650, 0.006, Rectangle.SHAPE_FILLED, "fillBackground" );
		fillBackground.setColor( 8 );
		fillBackground.setCenter( 0.001, 0.0, 0.0 );
		model2D.addChild( fillBackground );

		return model2D;
	}

	private OrderedGroup getDisplayModelForRectangle() {
		OrderedGroup model2D = new OrderedGroup();
		if( filled.getValue() ) {
			Rectangle iconFillModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_FILLED, "iconFillModel" );
			iconFillModel.setColor(fillColour.getValue());
			iconFillModel.setName(TAG_CONTENTS);
			model2D.addChild(iconFillModel);
		}
		Rectangle iconOutlineModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_OUTLINE, "iconOutlineModel" );
		iconOutlineModel.setColor(outlineColour.getValue());
		iconOutlineModel.setName(TAG_OUTLINES);
		if( dashed.getValue() ) {
			if( bold.getValue() )
				iconOutlineModel.setLineStyle( Shape.LINE_DASH_2PX );
			else
				iconOutlineModel.setLineStyle( Shape.LINE_DASH_1PX );
		}
		else {
			if( bold.getValue() )
				iconOutlineModel.setLineStyle( Shape.LINE_SOLID_2PX );
			else
				iconOutlineModel.setLineStyle( Shape.LINE_SOLID_1PX );
		}
		model2D.addChild(iconOutlineModel);
		return model2D;
	}

	private OrderedGroup getDisplayModelForCircle() {
		OrderedGroup model2D = new OrderedGroup();
		if( filled.getValue() ) {
			Circle contentsCircle = new Circle(0.5, Circle.SHAPE_FILLED, "contentsCircle");
			contentsCircle.setColor(fillColour.getValue());
			contentsCircle.setName(TAG_CONTENTS);
			model2D.addChild(contentsCircle);
		}
		Circle circle = new Circle(0.5, Circle.SHAPE_OUTLINE, "circle");
		circle.setColor(outlineColour.getValue());
		circle.setName(TAG_OUTLINES);
		if( dashed.getValue() ) {
			if( bold.getValue() )
				circle.setLineStyle( Shape.LINE_DASH_2PX );
			else
				circle.setLineStyle( Shape.LINE_DASH_1PX );
		}
		else {
			if( bold.getValue() )
				circle.setLineStyle( Shape.LINE_SOLID_2PX );
			else
				circle.setLineStyle( Shape.LINE_SOLID_1PX );
		}
		model2D.addChild(circle);
		return model2D;
	}

	private OrderedGroup getDisplayModelForBarGauge2D() {
		OrderedGroup model2D = new OrderedGroup();

		Rectangle backgroundFill = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_FILLED, "backgroundFill" );
		backgroundFill.setColor(Shape.COLOR_WHITE);
		backgroundFill.setName(TAG_BODY);
		model2D.addChild(backgroundFill);

		SideBySideBars bars = new SideBySideBars();
		bars.setDimension(new Point2d( 1.0, 1.0 ));
		bars.setCenter(0.0, 0.0, 0.0);
		bars.setName(TAG_CONTENTS);
		model2D.addChild(bars);

		Rectangle backgroundOutline = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_OUTLINE, "backgroundOutline" );
		backgroundOutline.setColor(outlineColour.getValue());
		backgroundOutline.setName(TAG_OUTLINES);
		model2D.addChild(backgroundOutline);

		Line lines = new Line( -0.5, -0.5, 0.5, -0.5, "lines" );
		lines.setColor( Shape.COLOR_BLACK );
		lines.setName(TAG_LINES);
		model2D.addChild( lines );

		Line secondaryLines = new Line( -0.5, -0.5, 0.5, -0.5, "lines" );
		secondaryLines.setColor( Shape.COLOR_BLACK );
		secondaryLines.setName(TAG_SECONDARY_LINES);
		model2D.addChild( secondaryLines );

		return model2D;
	}

	/** Stacker Reclaimer get 2D methods  **/

	private OrderedGroup getDisplayModelForDozer2D() {
		OrderedGroup model2D = new OrderedGroup();

		// set the model to be a yellow rectangle with black border.
		Rectangle iconFillModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_FILLED, "iconFillModel" );
		iconFillModel.setColor( Shape.COLOR_YELLOW );
		iconFillModel.setLayer( 0 );
		model2D.addChild( iconFillModel );
		Rectangle iconOutlineModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_OUTLINE, "iconOutlineModel" );
		iconOutlineModel.setColor( Shape.COLOR_BLACK );
		iconOutlineModel.setLayer( 1 );
		model2D.addChild( iconOutlineModel );
		return model2D;
	}

	private OrderedGroup getDisplayModelForStakerReclaimer2D() {
		OrderedGroup model2D = new OrderedGroup();
		//  calculate intermediate values.
		double x = 1.0;
		double len = 8.0;

		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		//tot = (double)armLength + (double)trackWidth;

		//  create the arm and counter weight.
		//double offset = baseX + x * 2.5;
		double[] armVerts = { -1.0 * x, -1.0 * len, 0.0, // bottom left
				1.0 * x, -1.0 * len, 0.0, // bottom right
				1.0 * x, -2.0 * x, 0.0, // bottom right of track1, arm side
				4.0 * x, -2.0 * x, 0.0, // bottom right of track1
				4.0 * x, -1.0 * x, 0.0, // top right of track1
				1.0 * x, -1.0 * x, 0.0, // top right of track1, arm side
				1.0 * x, x, 0.0, // bottom right of track2, arm side
				4.0 * x, x, 0.0, // bottom right of track2
				4.0 * x, 2.0 * x, 0.0, // top right of track2
				1.0 * x, 2.0 * x, 0.0, // top right of track2, arm side
				1.0 * x, len * 0.75 - x, 0.0, // top right of arm, before balance
				2.0 * x, len * 0.75 - x, 0.0, // bottom right of balance
				2.0 * x, len * 0.75, 0.0, // top right of balance
				-2.0 * x, len * 0.75, 0.0, // top left of balance
				-2.0 * x, len * 0.75 - x, 0.0, // bottom left of balance
				-1.0 * x, len * 0.75 - x, 0.0, // bottom left or arm, before balance
				-1.0 * x, 2.0 * x, 0.0, // top left of track2, arm side
				-4.0 * x, 2.0 * x, 0.0, // top left of track2
				-4.0 * x, x, 0.0, // bottom left of track2
				-1.0 * x, x, 0.0, // bottom left of track2, arm side
				-1.0 * x, -1.0 * x, 0.0, // top left of track1, arm side
				-4.0 * x, -1.0 * x, 0.0, // top left of track1
				-4.0 * x, -2.0 * x, 0.0, // bottom left of track1
				-1.0 * x, -2.0 * x, 0.0, // bottom left of track1, arm side
				-1.0 * x, -1.0 * len, 0.0 }; // bottom left

		double[] armFillVerts = {
				-1.0 * x, -1.0 * len, 0.0, // bottom left
				1.0 * x, -1.0 * len, 0.0, // bottom right
				1.0 * x, -2.0 * x, 0.0, // bottom right of track1, arm side
				-1.0 * x, -2.0 * x, 0.0, // bottom left of track1, arm side

				-4.0 * x, -2.0 * x, 0.0, // bottom left of track1
				4.0 * x, -2.0 * x, 0.0, // bottom right of track1
				4.0 * x, -1.0 * x, 0.0, // top right of track1
				-4.0 * x, -1.0 * x, 0.0, // top left of track1

				-1.0 * x, -1.0 * x, 0.0, // top left of track1, arm side
				1.0 * x, -1.0 * x, 0.0, // top right of track1, arm side
				1.0 * x, x, 0.0, // bottom right of track2, arm side
				-1.0 * x, x, 0.0, // bottom left of track2, arm side

				-4.0 * x, x, 0.0, // bottom left of track2
				4.0 * x, x, 0.0, // bottom right of track2
				4.0 * x, 2.0 * x, 0.0, // top right of track2
				-4.0 * x, 2.0 * x, 0.0, // top left of track2

				-1.0 * x, 2.0 * x, 0.0, // top left of track2, arm side
				1.0 * x, 2.0 * x, 0.0, // top right of track2, arm side
				1.0 * x, len * 0.75 - x, 0.0, // top right of arm, before balance
				-1.0 * x, len * 0.75 - x, 0.0, // bottom left or arm, before balance

				-2.0 * x, len * 0.75 - x, 0.0, // bottom left of balance
				2.0 * x, len * 0.75 - x, 0.0, // bottom right of balance
				2.0 * x, len * 0.75, 0.0, // top right of balance
				-2.0 * x, len * 0.75, 0.0, // top left of balance
		};

		RectangleArray armFillModel = new RectangleArray( armFillVerts, RectangleArray.SHAPE_FILLED, "armFillModel" );
		armFillModel.setColor( 9 );
		Polygon armOutlineModel = new Polygon( armVerts, Polygon.SHAPE_OUTLINE, "armOutlineModel" );
		armOutlineModel.setColor( 1 );

		// create the body
		Circle bodyFillModel = new Circle( 2.5 * x, Circle.SHAPE_FILLED, "bodyFillModel" );
		bodyFillModel.setColor( 9 );

		Circle bodyOutlineModel = new Circle( 2.5 * x, Circle.SHAPE_OUTLINE, "bodyOutlineModel" );
		bodyOutlineModel.setColor( 1 );

		bodyFillModel.setCenter( 0.0, 0.0, 0.0 );
		bodyOutlineModel.setCenter( 0.0, 0.0, 0.0 );

		// create the conveyor, and set it to change when the handlingBrand changes
		double[] verts = { -0.5 * x, 0.0, 0.0, -0.5 * x, -len + x * 0.5, 0.0, 0.5 * x, -len + x * 0.5, 0.0, 0.5 * x, 0.0, 0.0 };
		double[] tempverts = { -0.5 * x, 0.0, 0.0, -0.5 * x, -len + x * 0.5, 0.0, 0.5 * x, -len + x * 0.5, 0.0, 0.5 * x, 0.0, 0.0 };

		// create the model for the conveyor portion of the Stacker/Reclaimer
		RectangleArray conveyorFillModel = new RectangleArray( tempverts, RectangleArray.SHAPE_FILLED, "conveyorFillModel" );
		conveyorFillModel.setColor( 4 );

		Polygon conveyorOutlineModel = new Polygon( verts, Polygon.SHAPE_OUTLINE, "conveyorOutlineModel" );
		conveyorOutlineModel.setColor( 1 );

		// create the bucketwheet and set it to change when the handlingBrand changes
		double[] bucketVerts = { -0.5 * x, x - len, 0.0, -0.5 * x, x + x - len, 0.0, -1.5 * x, x + x - len, 0.0, -1.5 * x, x + x + x - len, 0.0, -2.5 * x, x + x + x - len, 0.0, -2.5 * x, -len, 0.0, -1.5 * x, -len, 0.0, -1.5 * x, x - len, 0.0 };
		double[] bucketFillVerts = { -0.5 * x, x - len, 0.0, -0.5 * x, x + x - len, 0.0, -1.5 * x, x + x - len, 0.0, -1.5 * x, x - len, 0.0, -2.5 * x, -len, 0.0, -1.5 * x, -len, 0.0, -1.5 * x, x + x + x - len, 0.0, -2.5 * x, x + x + x - len, 0.0 };


		// Add in the order from the bellow to the top
		contents.addChild(conveyorFillModel);
		outlines.addChild(conveyorOutlineModel);
		if(modelType == MODEL_RECLAIMER2D) {
			// create the model for the conveyor portion of the Stacker/Reclaimer
			RectangleArray bucketFillModel = new RectangleArray( bucketFillVerts, Polygon.SHAPE_FILLED, "bucketFillModel" );
			bucketFillModel.setColor( 4 );

			Polygon bucketOutlineModel = new Polygon( bucketVerts, Polygon.SHAPE_OUTLINE, "bucketOutlineModel" );
			bucketOutlineModel.setColor( 1 );

			contents.addChild( bucketFillModel );
			outlines.addChild( bucketOutlineModel );
		}

		model2D.addChild( armFillModel );
		outlines.addChild( armOutlineModel );
		outlines.addChild(bodyOutlineModel);
		model2D.addChild(outlines);
		model2D.addChild( bodyFillModel );
		model2D.addChild(contents);
		return model2D;
	}

	/**
	 * Bridge Graphics for reclaimer
	 * Only reclaimers can be bridgeReclaimers -- no stackers or stackerreclaimers
	 */
	private OrderedGroup getDisplaModelForBridge2D() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		// draw the reclaimer
		Point3f a = new Point3f( -0.5f, -0.5f, 0.5f );
		Point3f b = new Point3f( 0.5f, -0.5f, 0.5f );
		Point3f c = new Point3f( 0.5f, -0.3333f, 0.5f );
		Point3f d = new Point3f( 0.1667f, -0.3333f, 0.5f );
		Point3f e = new Point3f( 0.1667f, 0.3333f, 0.5f );
		Point3f f = new Point3f( 0.5f, 0.3333f, 0.5f );
		Point3f g = new Point3f( 0.5f, 0.5f, 0.5f );
		Point3f h = new Point3f( -0.5f, 0.5f, 0.5f );
		Point3f i = new Point3f( -0.5f, 0.3333f, 0.5f );
		Point3f j = new Point3f( -0.1667f, 0.3333f, 0.5f );
		Point3f k = new Point3f( -0.1667f, -0.3333f, 0.5f );
		Point3f l = new Point3f( -0.5f, -0.3333f, 0.5f );
		double[] verts = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z, e.x, e.y, e.z, f.x, f.y, f.z, g.x, g.y, g.z, h.x, h.y, h.z, i.x, i.y, i.z, j.x, j.y, j.z, k.x, k.y, k.z, l.x, l.y, l.z };

		Rectangle backgroundFillBottomSide = new Rectangle( 1.0, 0.1667, Rectangle.SHAPE_FILLED, "backgroundFillBottomSide" );
		backgroundFillBottomSide.setColor( 9 );
		backgroundFillBottomSide.setCenter( 0.0, -0.41665, 0.5f );
		model2D.addChild( backgroundFillBottomSide );
		Rectangle backgroundFillTopSide = new Rectangle( 1.0, 0.1667, Rectangle.SHAPE_FILLED, "backgroundFillTopSide" );
		backgroundFillTopSide.setColor( 9 );
		backgroundFillTopSide.setCenter( 0.0, 0.41665, 0.5f );
		model2D.addChild( backgroundFillTopSide );
		Rectangle backgroundFillMiddle = new Rectangle( 0.3334, 1.0, Rectangle.SHAPE_FILLED, "backgroundFillMiddle" );
		backgroundFillMiddle.setColor( 9 );
		backgroundFillMiddle.setCenter( 0.0, 0.0, 0.5f );
		contents.addChild(backgroundFillMiddle);
		model2D.addChild( contents );
		Polygon backgroundOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "backgroundOutline" );
		backgroundOutline.setColor( 1 );
		outlines.addChild(backgroundOutline);
		model2D.addChild(outlines);

		return model2D;
	}

	/**
	 * Crusher Graphics for reclaimer
	 */
	private  OrderedGroup getDisplayModelForCrusher2D() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		LineAttributes la = new LineAttributes();
		la.setLineWidth( 2 );

		// Square
		Rectangle iconFillModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_FILLED, "iconFillModel" );
		iconFillModel.setColor( Shape.COLOR_LIGHT_GREY );
		contents.addChild(iconFillModel);
		model2D.addChild(contents);

		Rectangle iconOutlineModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_OUTLINE, "iconOutlineModel" );
		iconOutlineModel.setColor( Shape.COLOR_LIGHT_GREY );
		iconOutlineModel.setLayer( 8 );
		outlines.addChild(iconOutlineModel);
		model2D.addChild(outlines);

		// Triangle
		Point3f a = new Point3f( 0.45f, -0.45f, 0.0f );
		Point3f b = new Point3f( 0.0f, 0.25f, 0.0f );
		Point3f c = new Point3f( -0.45f, -0.45f, 0.0f );

		// order the verticies as a triangle mesh with normals
		final double[] verts = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z };
		Polygon triangle = new Polygon( verts, Polygon.SHAPE_FILLED, "triangle" );
		triangle.setColor( Shape.COLOR_LIGHT_GREY );
		model2D.addChild( triangle );

		Polygon triangleOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "triangleOutline" );
		triangleOutline.setColor( Shape.COLOR_BLACK );
		triangleOutline.setLineAttributes( la );
		model2D.addChild( triangleOutline );

		// Lines
		Line line1 = new Line( -0.45, -0.3, -0.1, 0.25, "line1" );
		line1.setColor( Shape.COLOR_BLACK );
		line1.setLineStyle( la );
		model2D.addChild( line1 );

		Line line2 = new Line( -0.1, 0.25, -0.45, 0.45, "line2" );
		line2.setColor( Shape.COLOR_BLACK );
		line2.setLineStyle( la );
		model2D.addChild( line2 );

		Line line3 = new Line( 0.45, -0.3, 0.1, 0.25, "line3" );
		line3.setColor( Shape.COLOR_BLACK );
		line3.setLineStyle( la );
		model2D.addChild( line3 );

		Line line4 = new Line( 0.1, 0.25, 0.45, 0.45, "line4" );
		line4.setColor( Shape.COLOR_BLACK );
		line4.setLineStyle( la );
		model2D.addChild( line4 );

		return model2D;
	}

	/**
	 * create gantry stacker graphics
	 * @param trackLength: the width of center of the stacker
	 * @param armWidth: the length of the arms
	 *
	 * Gantry Stacker will contain a square center and two rectangles on its side.
	 * The square's length is determined by the trackLength ( the width of the conveyor )
	 * The rectangle's length is determined by the armWidth ( the width of the stockpile )
	 * The rectangle's width is 75% of the trackLength
	 *
	 * Content will be stacked from the middle of the arm. Therefore, only half of the arm will have content
	 *
	 */
	private OrderedGroup getDisplayModelForGantry2D(){
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		double trackLength = 0.004;
		double armWidth = 0.008;

		if( trackLength == 0 || armWidth == 0 ){
			armWidth = 0.5;
			trackLength = 0.125;
		}

		// the length of the arm that will have content when it is stacking
		double fill = (armWidth + trackLength)/2;

		// body of the stacker should be smaller than the armWidth
		double body = Math.min(trackLength, trackLength*0.5+0.125*armWidth);

		Rectangle bodyCenterFill = new Rectangle( trackLength, trackLength, Rectangle.SHAPE_FILLED, "bodyCenterFill" );
		Rectangle bodyCenterOutline = new Rectangle( trackLength, trackLength, Rectangle.SHAPE_OUTLINE, "bodyCenterOutline" );

		// part between the arm and main body
		Rectangle bodyRightFill = new Rectangle( 0.85*trackLength, body, Rectangle.SHAPE_FILLED, "bodyRightFill" );
		Rectangle bodyRightOutline = new Rectangle(  0.85*trackLength, body, Rectangle.SHAPE_OUTLINE, "bodyRightOutline" );

		// part of the arm that will be filled with content
		Rectangle armRightInnerFill = new Rectangle( 0.75*trackLength, fill, Rectangle.SHAPE_FILLED, "armRightInnerFill" );
		// the whole arm
		Rectangle armRightOutline = new Rectangle( 0.75*trackLength, armWidth, Rectangle.SHAPE_OUTLINE, "armRightOutline" );
		Rectangle armRightOuterFill = new Rectangle( 0.75*trackLength, armWidth, Rectangle.SHAPE_FILLED, "armRightOuterFill" );

		// part between the arm and main body
		Rectangle bodyLeftFill = new Rectangle(  0.85*trackLength, body, Rectangle.SHAPE_FILLED, "bodyLeftFill" );
		Rectangle bodyLeftOutline = new Rectangle(  0.85*trackLength, body, Rectangle.SHAPE_OUTLINE, "bodyLeftOutline" );
		// part of the arm that will be filled with content
		Rectangle armLeftInnerFill = new Rectangle( 0.75*trackLength, fill, Rectangle.SHAPE_FILLED, "armLeftInnerFill" );
		// the whole arm
		Rectangle armLeftOutline = new Rectangle( 0.75*trackLength, armWidth, Rectangle.SHAPE_OUTLINE, "armLeftOutline" );
		Rectangle armLeftOuterFill = new Rectangle( 0.75*trackLength, armWidth, Rectangle.SHAPE_FILLED, "armLeftOuterFill" );

		bodyCenterFill.setCenter( 0, 0, 0.15 );
		bodyCenterOutline.setCenter( 0, 0, 0.15 );
		bodyCenterFill.setColor( Shape.COLOR_LIGHT_GREY );
		bodyCenterOutline.setColor( Shape.COLOR_BLACK  );

		bodyRightFill.setCenter( 0, body/2, 0.125);
		armRightInnerFill.setCenter( 0, fill/2, 0.1);
		armRightOuterFill.setCenter( 0, armWidth/2, 0.1 );

		bodyRightOutline.setCenter( 0, body/2, 0.125);
		armRightOutline.setCenter( 0, armWidth/2, 0.1 );

		bodyRightFill.setColor( Shape.COLOR_MED_GREY);
		armRightInnerFill.setColor( Shape.COLOR_LIGHT_GREY );
		armRightOuterFill.setColor( Shape.COLOR_LIGHT_GREY );

		bodyRightOutline.setColor( Shape.COLOR_BLACK);
		armRightOutline.setColor( Shape.COLOR_BLACK );

		bodyLeftFill.setCenter ( 0, -body/2, 0.125);
		armLeftInnerFill.setCenter( 0, -fill/2, 0.1 );
		armLeftOuterFill.setCenter( 0, -armWidth/2, 0.1);

		bodyLeftOutline.setCenter ( 0, -body/2, 0.125);
		armLeftOutline.setCenter( 0, -armWidth/2, 0.1);

		bodyLeftFill.setColor (Shape.COLOR_MED_GREY );
		armLeftInnerFill.setColor( Shape.COLOR_LIGHT_GREY );
		armLeftOuterFill.setColor( Shape.COLOR_LIGHT_GREY );

		bodyLeftOutline.setColor (Shape.COLOR_BLACK );
		armLeftOutline.setColor( Shape.COLOR_BLACK );

		// bottom to top
		model2D.addChild( armLeftOuterFill );
		model2D.addChild(armRightOuterFill);
		outlines.addChild(armRightOutline);
		model2D.addChild(armLeftInnerFill);
		outlines.addChild( armLeftOutline );

		model2D.addChild( armRightInnerFill );
		model2D.addChild( bodyLeftOutline );
		model2D.addChild( bodyRightOutline );

		contents.addChild(bodyCenterFill);
		outlines.addChild( bodyCenterOutline );
		model2D.addChild(outlines);
		model2D.addChild( bodyLeftFill );
		model2D.addChild( bodyRightFill );
		model2D.addChild(contents);

		return model2D;
	}

	public OrderedGroup getDisplayModelForCrusher2nd2D() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		LineAttributes la = new LineAttributes();
		la.setLineWidth( 2 );

		// Square
		Rectangle fillModel = new Rectangle( 0.0, 0.0, 1, 0.70, Rectangle.SHAPE_FILLED, "fillModel" );
		fillModel.setColor( Shape.COLOR_LIGHT_GREY );
		contents.addChild(fillModel);
		model2D.addChild(contents);

		Rectangle outlineModel = new Rectangle( 0.0, 0.0, 1.0, 0.70, Rectangle.SHAPE_OUTLINE, "outlineModel" );
		outlineModel.setColor( Shape.COLOR_LIGHT_GREY );
		outlineModel.setLayer( 8 );
		outlines.addChild(outlineModel);
		model2D.addChild(outlines);

		// Circles
		Circle circle1 = new Circle( 0.24d, 0.0d, 0.0d, 0.2, 36, Circle.SHAPE_FILLED );
		circle1.setColor( Shape.COLOR_LIGHT_GREY );
		model2D.addChild(circle1);

		Circle circle1Outline = new Circle( 0.24d, 0.0d, 0.0d, 0.2, 36, Circle.SHAPE_OUTLINE );
		circle1Outline.setColor( Shape.COLOR_BLACK );
		circle1Outline.setLineAttributes( la );
		model2D.addChild(circle1Outline);

		Circle circle2 = new Circle( -0.24d, 0.0d, 0.0d, 0.2, 36, Circle.SHAPE_FILLED );
		circle2.setColor( Shape.COLOR_LIGHT_GREY );
		model2D.addChild(circle2 );

		Circle circle2Outline = new Circle( -0.24d, 0.0d, 0.0d, 0.2, 36, Circle.SHAPE_OUTLINE );
		circle2Outline.setColor( Shape.COLOR_BLACK );
		circle2Outline.setLineAttributes( la );
		model2D.addChild(circle2Outline);

		return model2D;
	}

	/**
	 * create the initial graphics for the Stacker Recalimer from the specified values.
	 */
	public OrderedGroup getDisplayModelForSlaveStacker2D() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		double layer1 = 0.05;
		double layer2 = 0.06;
		double layer3 = 0.07;

		//  calculate intermediate values.
		double x = 1;
		double len = 8.0d;

		//  create the arm and counter weight.
		double[] armVerts = {
				-1.0 * x, -1.0 * len, layer1, // bottom left
				1.0 * x, -1.0 * len, layer1, // bottom right
				1.0 * x, -2.0 * x, layer1, // bottom right of track1, arm side
				4.0 * x, -2.0 * x, layer1, // bottom right of track1
				4.0 * x, -1.0 * x, layer1, // top right of track1
				1.0 * x, -1.0 * x, layer1, // top right of track1, arm side
				1.0 * x, x, layer1, // bottom right of track2, arm side
				4.0 * x, x, layer1, // bottom right of track2
				4.0 * x, 2.0 * x, layer1, // top right of track2
				1.0 * x, 2.0 * x, layer1, // top right of track2, arm side
				1.0 * x, len * 0.75 - x, layer1, // top right of arm, before balance
				1.0 * x, 1.0 * len, layer1, // top right
				-1.0 * x, 1.0 * len, layer1, // top left
				-1.0 * x, 2.0 * x, layer1, // top left of track2, arm side
				-4.0 * x, 2.0 * x, layer1, // top left of track2
				-4.0 * x, x, layer1, // bottom left of track2
				-1.0 * x, x, layer1, // bottom left of track2, arm side
				-1.0 * x, -1.0 * x, layer1, // top left of track1, arm side
				-4.0 * x, -1.0 * x, layer1, // top left of track1
				-4.0 * x, -2.0 * x, layer1, // bottom left of track1
				-1.0 * x, -2.0 * x, layer1, // bottom left of track1, arm side
				-1.0 * x, -1.0 * len, layer1 }; // bottom left

		double[] armFillVerts = {
				-1.0 * x, -1.0 * len, layer1, // bottom left
				1.0 * x, -1.0 * len, layer1, // bottom right
				1.0 * x, -2.0 * x, layer1, // bottom right of track1, arm side
				-1.0 * x, -2.0 * x, layer1, // bottom left of track1, arm side

				-4.0 * x, -2.0 * x, layer1, // bottom left of track1
				4.0 * x, -2.0 * x, layer1, // bottom right of track1
				4.0 * x, -1.0 * x, layer1, // top right of track1
				-4.0 * x, -1.0 * x, layer1, // top left of track1

				-1.0 * x, -1.0 * x, layer1, // top left of track1, arm side
				1.0 * x, -1.0 * x, layer1, // top right of track1, arm side
				1.0 * x, x, layer1, // bottom right of track2, arm side
				-1.0 * x, x, layer1, // bottom left of track2, arm side

				-4.0 * x, x, layer1, // bottom left of track2
				4.0 * x, x, layer1, // bottom right of track2
				4.0 * x, 2.0 * x, layer1, // top right of track2
				-4.0 * x, 2.0 * x, layer1, // top left of track2

				-1.0 * x, 2.0 * x, layer1, // top left of track2, arm side
				1.0 * x, 2.0 * x, layer1, // top right of track2, arm side
				1.0 * x, 1.0 * len, layer1, // top right
				-1.0 * x, 1.0 * len, layer1, // top left
		};

		RectangleArray armFillModel = new RectangleArray( armFillVerts, RectangleArray.SHAPE_FILLED, "armFillModel" );
		armFillModel.setColor( 9 );
		model2D.addChild(armFillModel);
		Polygon armOutlineModel = new Polygon( armVerts, Polygon.SHAPE_OUTLINE, "armOutlineModel" );
		armOutlineModel.setColor( 1 );
		armOutlineModel.setLayer( 1 );
		outlines.addChild(armOutlineModel);

		// create the body
		Circle bodyFillModel = new Circle( 2.5 * x, Circle.SHAPE_FILLED, "bodyFillModel" );
		bodyFillModel.setColor( 9 );
		bodyFillModel.setLayer( 5 );

		Circle bodyOutlineModel = new Circle( 2.5 * x, Circle.SHAPE_OUTLINE, "bodyOutlineModel" );
		bodyOutlineModel.setColor( 1 );
		bodyOutlineModel.setLayer( 6 );
		outlines.addChild( bodyOutlineModel );

		bodyFillModel.setCenter( 0.0, 0.0, layer2 );
		bodyOutlineModel.setCenter( 0.0, 0.0, layer2 );

		// create the conveyor, and set it to change when the handlingBrand changes
		//double[] verts = { -0.5 * x, 0.0, layer3, -0.5 * x, -len + x * 0.5, layer3, 0.5 * x, -len + x * 0.5, layer3, 0.5 * x, 0.0, layer3 };
		//double[] tempverts = { -0.5 * x, 0.0, layer3, -0.5 * x, -len + x * 0.5, layer3, 0.5 * x, -len + x * 0.5, layer3, 0.5 * x, 0.0, layer3 };

		double[] verts =     { -0.5 * x, len - x*0.5, layer3, -0.5 * x, -len + x * 0.5, layer3, 0.5 * x, -len + x * 0.5, layer3, 0.5 * x, len - x*0.5, layer3 };
		double[] tempverts = { -0.5 * x, len - x*0.5, layer3, -0.5 * x, -len + x * 0.5, layer3, 0.5 * x, -len + x * 0.5, layer3, 0.5 * x, len - x*0.5, layer3 };

		// create the model for the conveyor portion of the Stacker/Reclaimer
		RectangleArray conveyorFillModel = new RectangleArray( tempverts, RectangleArray.SHAPE_FILLED, "conveyorFillModel" );
		conveyorFillModel.setColor( 4 );
		conveyorFillModel.setLayer( 3 );
		contents.addChild( conveyorFillModel );

		Polygon conveyorOutlineModel = new Polygon( verts, Polygon.SHAPE_OUTLINE, "conveyorOutlineModel" );
		conveyorOutlineModel.setColor( 1 );
		conveyorOutlineModel.setLayer( 4 );
		outlines.addChild( conveyorOutlineModel );
		model2D.addChild(outlines);
		model2D.addChild(bodyFillModel);
		model2D.addChild(contents);


		return model2D;
	}
	/** ShipLoader Graphics Methods **/
	private OrderedGroup getDisplayModelForDualQuadrantGraphics() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		Line a = new Line( 0.4, 0.045454545, -0.4, -0.227272727, "DualQuadrant.a" );
		a.setColor( 1 );
		a.setLayer( 3 );
		model2D.addChild(a);
		Line b = new Line( -0.4, -0.227272727, 0.4, -0.5, "DualQuadrant.b" );
		b.setColor( 1 );
		b.setLayer( 3 );
		model2D.addChild(b);

		Line c = new Line( 0.4, 0.5, -0.4, 0.227272727, "DualQuadrant.c" );
		c.setColor( 1 );
		c.setLayer( 3 );
		model2D.addChild(c);
		Line d = new Line( -0.4, 0.227272727, 0.4, -0.045454545, "DualQuadrant.d" );
		d.setColor( 1 );
		d.setLayer( 3 );
		model2D.addChild(d);

		double[] verts = { -0.5, -0.272727273, 0.0, -0.5, 0.272727273, 0.0, 0.5, 0.272727273, 0.0, 0.5, 0.181818182, 0.0, -0.3, 0.181818182, 0.0, -0.3, -0.181818182, 0.0, 0.5, -0.181818182, 0.0, 0.5, -0.272727273, 0.0 };

		double[] verts2 = { -0.5, -0.272727273, 0.0,
				0.5, -0.272727273, 0.0,
				0.5, -0.181818182, 0.0,
				-0.5, -0.181818182, 0.0,

				-0.5, -0.181818182, 0.0,
				-0.3, -0.181818182, 0.0,
				-0.3, 0.181818182, 0.0,
				-0.5, 0.181818182, 0.0,

				-0.5, 0.181818182, 0.0,
				0.5, 0.181818182, 0.0,
				0.5, 0.272727273, 0.0,
				-0.5, 0.272727273, 0.0 };

		RectangleArray contentsFill = new RectangleArray( verts2, Rectangle.SHAPE_FILLED, "contentsFill" );
		contentsFill.setColor( 6 );
		contents.addChild(contentsFill);

		Polygon contentsOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "contentsOutline" );
		contentsOutline.setColor( 1 );
		contentsOutline.setLayer( 1 );
		outlines.addChild(contentsOutline);

		Arc e = new Arc( 0.6, -0.3, -0.227272727, Math.PI * -0.25, Math.PI * 0.25, "DualQuadrant.e" );
		e.setColor( 1 );
		e.setLayer( 3 );
		Arc f = new Arc( 0.6, -0.3, 0.227272727, Math.PI * -0.25, Math.PI * 0.25, "DualQuadrant.f" );
		f.setColor( 1 );
		f.setLayer( 3 );
		model2D.addChild(contents);
		model2D.addChild(outlines);
		model2D.addChild(e);
		model2D.addChild(f);
		return model2D;
	}

	private OrderedGroup getDisplayModelForSingleQuadrantGraphics() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		Line a = new Line( 0.4, 0.5, -0.4, 0.0, "SingleQuadrant.a" );
		a.setColor( 1 );
		a.setLayer( 3 );
		model2D.addChild(a);
		Line b = new Line( -0.4, 0.0, 0.4, -0.5, "SingleQuadrant.b" );
		b.setColor( 1 );
		b.setLayer( 3 );
		model2D.addChild(b);

		Rectangle contentsFill = new Rectangle( 1.0, 0.166666, Rectangle.SHAPE_FILLED, "contentsFill" );
		contentsFill.setColor( 6 );
		contents.addChild(contentsFill);

		Rectangle contentsOutline = new Rectangle( 1.0, 0.166666, Rectangle.SHAPE_OUTLINE, "contentsOutline" );
		contentsOutline.setColor( 1 );
		contentsOutline.setLayer( 1 );
		outlines.addChild(contentsOutline);

		Arc tempArc = new Arc( 0.6, -0.3, 0.0, Math.PI * -0.25, Math.PI * 0.25, "SingleQuadrant.tempArc" );
		tempArc.setColor( 1 );
		tempArc.setLayer( 3 );
		model2D.addChild(contents);
		model2D.addChild(outlines);
		model2D.addChild(tempArc);
		return model2D;
	}

	private OrderedGroup getDisplayModelForLinearGraphics() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		// Define Pier pieces
		Rectangle pierOutline1 = new Rectangle( 0.0, -0.25, 1.0, 0.1, Rectangle.SHAPE_FILLED, "pierOutline1");
		pierOutline1.setColor( 9 );
		pierOutline1.setLayer( 4 );
		model2D.addChild(pierOutline1);

		Rectangle pierOutline2 = new Rectangle( 0.0, 0.05, 0.15, 0.5, Rectangle.SHAPE_FILLED, "pierOutline2" );
		pierOutline2.setColor( 9 );
		pierOutline2.setLayer( 4 );
		model2D.addChild(pierOutline2);

		// Define delivery pieces
		// Contents
		double angle = .392;
		double newX1 = -0.075*Math.cos( angle ) - (-0.4*Math.sin( angle ));
		double newY1 = -0.075*Math.sin( angle ) + (-0.4*Math.cos( angle ));
		double newX2 = 0.075*Math.cos( angle ) - (-0.4*Math.sin( angle ));
		double newY2 = 0.075*Math.sin( angle ) + (-0.4*Math.cos( angle ));
		double newX3 = 0.075*Math.cos( angle ) - (0.2*Math.sin( angle ));
		double newY3 = 0.075*Math.sin( angle ) + (0.2*Math.cos( angle ));
		double newX4 = -0.075*Math.cos( angle ) - (0.2*Math.sin( angle ));
		double newY4 = -0.075*Math.sin( angle ) + (0.2*Math.cos( angle ));

		Point3f a = new Point3f( (float)newX1, (float)newY1, 0.0f );
		Point3f b = new Point3f( (float)newX2, (float)newY2, 0.0f );
		Point3f c = new Point3f( (float)newX3, (float)newY3, 0.0f );
		Point3f d = new Point3f( (float)newX4, (float)newY4, 0.0f );

		// order the verticies as a triangle mesh with normals
		double[] verts = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z , d.x, d.y, d.z };

	    Polygon contentsFill = new Polygon( verts, Polygon.SHAPE_FILLED, "contentsFill" );
		contentsFill.setColor( 8 );
		contentsFill.setLayer( 4 );
		contents.addChild(contentsFill);

		Polygon contentsOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "contentsOutline" );
		contentsOutline.setColor( 1 );
		contentsOutline.setLayer( 5 );
		outlines.addChild(contentsOutline);

		// End Piece
		newX1 = -0.03*Math.cos( angle ) - (-0.8*Math.sin( angle ));
		newY1 = -0.03*Math.sin( angle ) + (-0.8*Math.cos( angle ));
		newX2 = 0.03*Math.cos( angle ) - (-0.8*Math.sin( angle ));
		newY2 = 0.03*Math.sin( angle ) + (-0.8*Math.cos( angle ));
		newX3 = 0.03*Math.cos( angle ) - (-0.4*Math.sin( angle ));
		newY3 = 0.03*Math.sin( angle ) + (-0.4*Math.cos( angle ));
		newX4 = -0.03*Math.cos( angle ) - (-0.4*Math.sin( angle ));
		newY4 = -0.03*Math.sin( angle ) + (-0.4*Math.cos( angle ));

		a = new Point3f( (float)newX1, (float)newY1, 0.0f );
		b = new Point3f( (float)newX2, (float)newY2, 0.0f );
		c = new Point3f( (float)newX3, (float)newY3, 0.0f );
		d = new Point3f( (float)newX4, (float)newY4, 0.0f );

		// order the verticies as a triangle mesh with normals
		double[] verts2 = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z , d.x, d.y, d.z };

		Polygon endFill = new Polygon( verts2, Polygon.SHAPE_FILLED, "endFill" );
		endFill.setColor( 8 );
		endFill.setLayer( 4 );
		contents.addChild(endFill);

		Polygon endOutline = new Polygon( verts2, Polygon.SHAPE_OUTLINE, "endOutline" );
		endOutline.setColor( 1 );
		endOutline.setLayer( 5 );
		outlines.addChild(endOutline);
		model2D.addChild(contents);
		model2D.addChild(outlines);
		return model2D;
	}

	private OrderedGroup getDisplayModelForTravellingGraphics() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		// Part 1
		Rectangle contentsFill = new Rectangle( 0.0, -0.10, 0.4, 0.4, Rectangle.SHAPE_FILLED, "contentsFill" );
		contentsFill.setColor( 8 );
		contentsFill.setLayer( 4 );
		contents.addChild(contentsFill);

		Rectangle contentsOutline = new Rectangle( 0.0, -0.10, 0.4, 0.4, Rectangle.SHAPE_OUTLINE, "contentsOutline" );
		contentsOutline.setColor( 1 );
		contentsOutline.setLayer( 5 );
		outlines.addChild(contentsOutline);

		// Part 2
		Rectangle trackFill = new Rectangle( 0.0, 0.0, 1.0, 0.20, Rectangle.SHAPE_FILLED, "TrackFill" );
		trackFill.setColor( 9 );
		trackFill.setLayer( 3 );
		trackFill.setName(TAG_TRACKFILL);

		Rectangle trackOutline = new Rectangle( 0.0, 0.0, 1.0, 0.20, Rectangle.SHAPE_OUTLINE, "trackOutline" );
		trackOutline.setColor( 9 );
		trackOutline.setLayer( 5 );
		model2D.addChild(trackOutline);

		// Part 3
		Rectangle endFill = new Rectangle( 0.0, -0.40, 0.2, 0.2, Rectangle.SHAPE_FILLED, "endFill" );
		endFill.setColor( 8 );
		endFill.setLayer( 4 );
		contents.addChild(endFill);

		Rectangle endOutline = new Rectangle( 0.0, -0.40, 0.2, 0.2, Rectangle.SHAPE_OUTLINE, "endOutline" );
		endOutline.setColor( 1 );
		endOutline.setLayer( 5 );
		outlines.addChild(endOutline);
		model2D.addChild(contents);
		model2D.addChild(outlines);
		model2D.addChild(trackFill);
		return model2D;
	}

	private OrderedGroup getDisplayModelForCrushingPlant2D() {
		OrderedGroup model2D = new OrderedGroup();
		BranchGroup contents = new BranchGroup();
		BranchGroup outlines = new BranchGroup();
		contents.setName(TAG_CONTENTS);
		outlines.setName(TAG_OUTLINES);

		// Create the shape for the bottom
		Point3f a = new Point3f( -0.17659f, -0.5f, 0.0f );
		Point3f b = new Point3f( 0.15675f, -0.5f, 0.0f );
		Point3f c = new Point3f( 0.15675f, -0.1f, 0.0f );
		Point3f d = new Point3f( -0.17659f, -0.1f, 0.0f );
		double[] verts = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z };

		// create the bottom shape
		Polygon bottomFill = new Polygon( verts, Polygon.SHAPE_FILLED, "bottomFill" );
		bottomFill.setColor( 8 );
		contents.addChild( bottomFill );
		Polygon bottomOutline = new Polygon( verts, Polygon.SHAPE_OUTLINE, "bottomOutline" );
		bottomOutline.setColor( 1 );
		bottomOutline.setLayer( 1 );
		outlines.addChild( bottomOutline );

		a = new Point3f( -0.17659f, 0f, 0.0f );
		b = new Point3f( 0.15675f, 0f, 0.0f );
		c = new Point3f( 0.49008f, 0.5f, 0.0f );
		d = new Point3f( -0.50992f, 0.5f, 0.0f );
		double[] verts2 = { a.x, a.y, a.z, b.x, b.y, b.z, c.x, c.y, c.z, d.x, d.y, d.z };

		// create the top shape
		Polygon topFill = new Polygon( verts2, Polygon.SHAPE_FILLED, "topFill" );
		topFill.setColor( 8 );
		contents.addChild( topFill );
		Polygon topOutline = new Polygon( verts2, Polygon.SHAPE_OUTLINE, "topOutline" );
		topOutline.setColor( 1 );
		topOutline.setLayer( 1 );
		outlines.addChild( topOutline );
		model2D.addChild(contents);
		model2D.addChild(outlines);
		return model2D;
	}

	BufferedImage getLowResImage() {
		this.renderImages();
		return lowResImage;
	}

	BufferedImage getHighResImage() {
		this.renderImages();
		return highResImage;
	}

	void setImages(BufferedImage low, BufferedImage high) {
		highResImage = high;
		lowResImage = low;
	}

	private void renderImages(){
		// If we've already rendered, just return
		if (lowResImage != null || highResImage != null)
			return;

		// Avoid sharing problems with multiple VirtualUniverses
		if (this.hasSharedGroup())
			return;

		String offscreen = System.getProperty("JaamSim.offscreen");
		if ("FALSE".equals(offscreen))
			return;

		BranchGroupPrinter.renderBranchGroup_On(this);
	}
}
