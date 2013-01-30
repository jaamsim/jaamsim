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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.ImageProxy;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.MeshProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.TexCache;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Vec3dInput;

public class DisplayModelCompat extends DisplayModel {
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

	protected static final ArrayList<String> validTags;

	private double conversionFactorToMeters = 1.0d; // How many meters in one distance unit

	@Keyword(desc = "The shape of a display model determines the appearance of the display model. The shape may be " +
	                "one of the following: Pixels (for a square of 6x6 pixels), Truck2D, Ship2D, Icon (for a rectangle), " +
	                "Circle.  A graphics file name with one of the following file extensions: DAE (for a collada version " +
	                "1.4.1 3D model file), BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel Shape { ..\\3DModels\\Ship3D.dae }")
	private final StringInput shape;
	private static ArrayList<String> definedTypes;
	private static ArrayList<String> validFileExtentions;

	@Keyword(desc = "Euler angles in radians defining the rotation of the display model." +
	                "Only for imported collada models.",
	         example = "Ship3DModel Orientation { 0 0 1.5707963 }")
	private final Vec3dInput orientation;


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


	static {
		definedTypes = new ArrayList<String>();
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

		validFileExtentions = new ArrayList<String>();
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

		orientation = new Vec3dInput("Orientation", "DisplayModel", new Vec3d(0, 0, 0));
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

	public DisplayModelCompat() {}

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

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof DisplayEntity);
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;
		private ChangeWatcher.Tracker observeeTracker;

		private DisplayEntity dispEnt;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)ent;

			if (dispEnt != null) {
				observeeTracker = dispEnt.getGraphicsChangeTracker();
			}
		}

		private void updateCache() {
			if (cachedProxies != null && observeeTracker != null && !observeeTracker.checkAndClear()) {
				// Nothing changed
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("DisplayModelCompat");

			cachedProxies = new ArrayList<RenderProxy>();

			String shapeString = shape.getValue().toUpperCase();

			if (shapeString.equals("SHIP2D")) {
				addShipProxies();
				return;
			}
			if (shapeString.equals("TRUCK2D")) {
				addTruckProxies();
				return;
			}
			if (shapeString.equals("BARGAUGE2D")) {
				addBarGaugeProxies();
				return;
			}
			if (shapeString.equals("CRUSHINGPLANT2D")) {
				addCrushingPlantProxies();
				return;
			}
			if (shapeString.equals("ARROW2D")) {
				addArrowProxies();
				return;
			}
			if (shapeString.equals("SINGLEQUADRANT2D")) {
				addSingleQuadProxies();
				return;
			}
			if (shapeString.equals("DUALQUADRANT2D")) {
				addDualQuadProxies();
				return;
			}
			if (shapeString.equals("TRAVELLING2D")) {
				addTravellingProxies();
				return;
			}

			if (shapeString.equals("STACKER2D") ||
			    shapeString.equals("RECLAIMER2D") ) {
				addStackerProxies();
				return;
				}

			List<Vec4d> points = null;
			if (shapeString.equals("CIRCLE")) {
				points = RenderUtils.CIRCLE_POINTS;
			}
			if (shapeString.equals("RECTANGLE")) {
				points = RenderUtils.RECT_POINTS;
			}
			if (shapeString.equals("TRIANGLE")) {
				points = RenderUtils.TRIANGLE_POINTS;
			}

			if (points == null || points.size() == 0) {
				// Not a known shape, try to find an extension we recognize
				if (shapeString.length() <= 4) { return; } // can not be a filename

				String ext = shapeString.substring(shapeString.length() - 4, shapeString.length());

				if (imageExtensions.contains(ext.toUpperCase())) {
					addImageProxy(shape.getValue());
				}
				if (modelExtensions.contains(ext.toUpperCase())) {
					addModelProxy(shape.getValue());
				}
				return;
			}

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			if (tags.isTagVisibleUtil(DisplayModelCompat.TAG_OUTLINES))
			{
				Color4d colour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, outlineColour.getValue());
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, true, (bold.getValue() ? 2 : 1), getVisibilityInfo(), pickingID));
			}

			if (filled.getValue() && tags.isTagVisibleUtil(DisplayModelCompat.TAG_CONTENTS))
			{
				Color4d colour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, fillColour.getValue());
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, false, 1, getVisibilityInfo(), pickingID));
			}
		}

		@Override
		public void collectProxies(ArrayList<RenderProxy> out) {
			// This is slightly quirky behaviour, as a null entity will be shown because we use that for previews
			if (dispEnt != null && !dispEnt.getShow()) {
				return;
			}

			updateCache();

			out.addAll(cachedProxies);
		}

		private Transform getTransform() {
			if (dispEnt == null) {
				return Transform.ident;
			}
			return dispEnt.getGlobalTrans();
		}
		private Vec4d getScale() {
			if (dispEnt == null) {
				return Vec4d.ONES;
			}
			return dispEnt.getJaamMathSize(getModelScale());
		}
		private long getPickingID() {
			if (dispEnt == null) {
				return 0;
			}
			return dispEnt.getEntityNumber();
		}

		private DisplayEntity.TagSet getTags() {
			if (dispEnt == null) {
				return emptyTagSet;
			}
			return dispEnt.getTagSet();
		}

		private void addArrowProxies() {
			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.BLACK);

			cachedProxies.add(new PolygonProxy(arrowHeadVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(arrowTailVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour= tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(arrowOutlineVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addShipProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Now this is very 'shippy' behaviour and basically hand copied from the old DisplayModels (and supporting cast)

			// Hull
			Color4d hullColour = tags.getTagColourUtil(DisplayModelCompat.TAG_BODY, ColourInput.LIGHT_GREY);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, hullColour, false, 1, getVisibilityInfo(), pickingID));

			// Outline
			Color4d outlineColour= tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			// Cabin
			cachedProxies.add(new PolygonProxy(shipCabinVerts, trans, scale, ColourInput.BLACK, false, 1, getVisibilityInfo(), pickingID));

			// Add the contents parcels
			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);

			cachedProxies.addAll(buildContents(sizes, colours, shipContentsTrans, trans, scale, pickingID));
		}

		private void addTruckProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Add a yellow rectangle for the cab
			cachedProxies.add(new PolygonProxy(truckCabVerts, trans, scale, ColourInput.YELLOW, false, 1, getVisibilityInfo(), pickingID));

			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);

			cachedProxies.addAll(buildContents(sizes, colours, truckContentsTrans, trans, scale, pickingID));
		}

		private void addBarGaugeProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] outlineColour = tags.colours.get(DisplayModelCompat.TAG_OUTLINES);
			Color4d[] backgroundColour = tags.colours.get(DisplayModelCompat.TAG_BODY);
			if (sizes == null) {
				sizes = new DoubleVector();
			}
			if (outlineColour == null || outlineColour.length < 1) {
				outlineColour = new Color4d[1];
				outlineColour[0] = ColourInput.BLACK;
			}
			if (backgroundColour == null || backgroundColour.length < 1) {
				backgroundColour = new Color4d[1];
				backgroundColour[0] = ColourInput.WHITE;
			}

			double width = 1.0;

			if (sizes.size() != 0) {
				width = 1.0 / sizes.size();
			}

			// Add the background and outline
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, backgroundColour[0], false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, outlineColour[0], true, 1, getVisibilityInfo(), pickingID));

			if (colours == null || colours.length < sizes.size()) {
				return;
			} // Bail out, not properly initialized

			for (int i = 0; i < sizes.size(); ++i) {
				// Add a rectangle for each size

				double size = sizes.get(i);

				double startX = i*width - 0.5;
				double endX = (i+1)*width - 0.5;

				double startY = -0.5;
				double endY = size - 0.5;

				List<Vec4d> contentsPoints = new ArrayList<Vec4d>();
				contentsPoints.add(new Vec4d(  endX, startY, 0, 1.0d));
				contentsPoints.add(new Vec4d(  endX,   endY, 0, 1.0d));
				contentsPoints.add(new Vec4d(startX,   endY, 0, 1.0d));
				contentsPoints.add(new Vec4d(startX, startY, 0, 1.0d));

				cachedProxies.add(new PolygonProxy(contentsPoints, trans, scale, colours[0], false, 1, getVisibilityInfo(), pickingID));

			}
		}

		private void addCrushingPlantProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);

			// Top
			cachedProxies.add(new PolygonProxy(crushingPlantTopVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(crushingPlantTopVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			// Bottom
			cachedProxies.add(new PolygonProxy(crushingPlantBotVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(crushingPlantBotVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));
		}

		private void addSingleQuadProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Add the lines
			Mat4d lineTrans = new Mat4d();
			trans.getMat4d(lineTrans);
			lineTrans.scaleCols3(scale);
			List<Vec4d> points = RenderUtils.transformPoints(lineTrans, singleQuadLinePoints, 0);
			cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);

			cachedProxies.add(new PolygonProxy(singleQuadRectVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(singleQuadRectVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));
		}


		private void addDualQuadProxies() {
			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Add the lines
			Mat4d lineTrans = new Mat4d();
			trans.getMat4d(lineTrans);
			lineTrans.scaleCols3(scale);
			List<Vec4d> points = RenderUtils.transformPoints(lineTrans, dualQuadLinePoints, 0);
			cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);

			cachedProxies.add(new PolygonProxy(dualQuadRect0Verts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(dualQuadRect1Verts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(dualQuadRect2Verts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(dualQuadOutlineVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addTravellingProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();


			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);
			Color4d trackColour = tags.getTagColourUtil(DisplayModelCompat.TAG_TRACKFILL, ColourInput.MED_GREY);

			cachedProxies.add(new PolygonProxy(travellingRect1Verts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(travellingRect1Verts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(travellingRect3Verts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(travellingRect3Verts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(travellingRect2Verts, trans, scale, trackColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(travellingRect2Verts, trans, scale, trackColour, true, 1, getVisibilityInfo(), pickingID));
		}

		private void addStackerProxies() {

			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d contentsColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);
			Color4d trackColour = ColourInput.MED_GREY;

			// This is gross, but until we have proper draw ordering it's the kind of thing we have to do to keep the
			// Stacker-reclaimer appearing above the stock piles reliably
			Vec4d fixedScale = new Vec4d(scale);
			fixedScale.z = 0.1;

			cachedProxies.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, trackColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(stackerRect2Verts, trans, fixedScale, contentsColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(stackerRect2Verts, trans, fixedScale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addImageProxy(String filename) {
			// Gather some inputs
			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();

			try {
				cachedProxies.add(new ImageProxy(new URL(Util.getAbsoluteFilePath(filename)), trans,
				                       scale, transparent.getValue(), compressedTexture.getValue(), getVisibilityInfo(), pickingID));
			} catch (MalformedURLException e) {
				cachedProxies.add(new ImageProxy(TexCache.BAD_TEXTURE, trans, scale,
				                                 transparent.getValue(), compressedTexture.getValue(), getVisibilityInfo(), pickingID));
			}
		}

		private void addModelProxy(String filename) {
			MeshProtoKey meshKey = _cachedKeys.get(filename);

			// We have not loaded this file before, cache the mesh proto key so
			// we don't dig through a zip file every render
			if (meshKey == null) {
				try {
					URL meshURL = new URL(Util.getAbsoluteFilePath(filename));

					String ext = filename.substring(filename.length() - 4,
							filename.length());

					if (ext.toUpperCase().equals(".ZIP")) {
						// This is a zip, use a zip stream to actually pull out
						// the .dae file
						ZipInputStream zipInputStream = new ZipInputStream(
								meshURL.openStream());

						// Loop through zipEntries
						for (ZipEntry zipEntry; (zipEntry = zipInputStream
								.getNextEntry()) != null;) {

							String entryName = zipEntry.getName();
							if (!Util.getFileExtention(entryName)
									.equalsIgnoreCase("DAE"))
								continue;

							// This zipEntry is a collada file, no need to look
							// any further
							meshURL = new URL("jar:" + meshURL + "!/"
									+ entryName);
							break;
						}
					}

					meshKey = new MeshProtoKey(meshURL);
					_cachedKeys.put(filename, meshKey);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					assert (false);
				} catch (IOException e) {
					assert (false);
				}
			}

			AABB bounds = RenderManager.inst().getMeshBounds(meshKey, true);
			if (bounds == null) {
				// This mesh has not been loaded yet, try again next time
				return;
			}

			Transform trans = getTransform();
			Vec4d scale = getScale();
			long pickingID = getPickingID();

			// Tweak the transform and scale to adjust for the bounds of the
			// loaded model
			Vec4d offset = new Vec4d(bounds.getCenter());
			Vec4d boundsRad = new Vec4d(bounds.getRadius());
			if (boundsRad.z == 0) {
				boundsRad.z = 1;
			}

			Vec4d fixedScale = new Vec4d(0.5 * scale.x
					/ boundsRad.x, 0.5 * scale.y / boundsRad.y, 0.5
					* scale.z / boundsRad.z, 1.0d);

			offset.x *= -1 * fixedScale.x;
			offset.y *= -1 * fixedScale.y;
			offset.z *= -1 * fixedScale.z;

			Transform fixedTrans = new Transform(trans);
			fixedTrans.merge(new Transform(offset), fixedTrans);

			cachedProxies.add(new MeshProxy(meshKey, fixedTrans, fixedScale, getVisibilityInfo(),
					pickingID));
		}

		// A disturbingly deep helper to allow trucks and ships to share contents building code
		// This class needs to either die or get refactored
		private List<RenderProxy> buildContents(DoubleVector sizes, Color4d[] colours, Mat4d subTrans,
		                                        Transform trans, Vec4d scale, long pickingID) {
			List<RenderProxy> ret = new ArrayList<RenderProxy>();

			if (sizes == null || colours == null || sizes.size() != colours.length) {
				// We are either out of sync or this is a ShipType, either way draw an empty cargo hold
				// Add a single grey rectangle
				List<Vec4d> contentsPoints = new ArrayList<Vec4d>();
				contentsPoints.add(new Vec4d( 1, -0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d( 1,  0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d( 0,  0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d( 0, -0.5, 0, 1.0d));
				for (int i = 0; i < contentsPoints.size(); ++i) {
					contentsPoints.get(i).mult4(subTrans, contentsPoints.get(i));
				}
				ret.add(new PolygonProxy(contentsPoints, trans, scale, ColourInput.LIGHT_GREY, false, 1, getVisibilityInfo(), pickingID));
				return ret;
			}

			double totalSize = sizes.sum();
			double sizeOffset = 0;
			for (int i = 0; i < sizes.size(); ++i) {
				// Add a rectangle for

				double size = sizes.get(i);
				double start = sizeOffset / totalSize;
				double end = (sizeOffset + size) / totalSize;

				List<Vec4d> contentsPoints = new ArrayList<Vec4d>();
				contentsPoints.add(new Vec4d(  end, -0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d(  end,  0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d(start,  0.5, 0, 1.0d));
				contentsPoints.add(new Vec4d(start, -0.5, 0, 1.0d));

				sizeOffset += size;

				for (int j = 0; j < contentsPoints.size(); ++j) {
					contentsPoints.get(j).mult4(subTrans, contentsPoints.get(j));
				}

				ret.add(new PolygonProxy(contentsPoints, trans, scale, colours[i], false, 1, getVisibilityInfo(), pickingID));
			}
			return ret;
		}

	}

	private static HashMap<String, MeshProtoKey> _cachedKeys = new HashMap<String, MeshProtoKey>();

	public static MeshProtoKey getCachedMeshKey(String shapeString) {
		return _cachedKeys.get(shapeString);
	}

	private DisplayEntity.TagSet emptyTagSet = new DisplayEntity.TagSet();

	// Begin static data
	// Since Arrows aren't convex, we need some more convoluted vertices
	private static List<Vec4d> arrowHeadVerts;
	private static List<Vec4d> arrowTailVerts;
	private static List<Vec4d> arrowOutlineVerts;

	private static List<Vec4d> truckCabVerts;

	private static List<Vec4d> crushingPlantTopVerts;
	private static List<Vec4d> crushingPlantBotVerts;

	private static List<Vec4d> singleQuadLinePoints;
	private static List<Vec4d> singleQuadRectVerts;

	private static List<Vec4d> dualQuadLinePoints;
	private static List<Vec4d> dualQuadOutlineVerts;
	private static List<Vec4d> dualQuadRect0Verts;
	private static List<Vec4d> dualQuadRect1Verts;
	private static List<Vec4d> dualQuadRect2Verts;

	private static List<Vec4d> travellingRect1Verts;
	private static List<Vec4d> travellingRect2Verts;
	private static List<Vec4d> travellingRect3Verts;

	private static List<Vec4d> stackerRect1Verts;
	private static List<Vec4d> stackerRect2Verts;

	private static List<Vec4d> hullVerts;
	private static List<Vec4d> shipCabinVerts;
	private static Mat4d shipContentsTrans;
	private static Mat4d truckContentsTrans;

	private static ArrayList<String> imageExtensions;

	private static ArrayList<String> modelExtensions;

	static {
		hullVerts = new ArrayList<Vec4d>(20);
		hullVerts.add(new Vec4d(-0.35625d, -0.5d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.35d, -0.5d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.40625d, -0.42d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.459375d, -0.3d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.484375d, -0.21d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.5d, -0.05d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.5d, 0.05d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.484375d, 0.21d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.459375d, 0.3d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.40625d, 0.42d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(0.35d, 0.5d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.35625d, 0.5d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.4109375d, 0.45d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.4515625d, 0.36d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.5d, 0.23d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.5d, -0.23d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.4515625d, -0.36d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.4109375d, -0.45d, 0.0d, 1.0d));
		hullVerts.add(new Vec4d(-0.35625d, -0.5d, 0.0d, 1.0d));

		Mat4d shipCabinTrans = new Mat4d();
		shipCabinTrans.setTranslate3(new Vec3d(-0.325, 0, 0));
		shipCabinTrans.scaleCols3(new Vec3d(0.125, 0.7, 0));

		shipCabinVerts = RenderUtils.transformPoints(shipCabinTrans, RenderUtils.RECT_POINTS, 0);

		shipContentsTrans = new Mat4d();
		shipContentsTrans.setTranslate3(new Vec3d(-0.225, 0.0, 0.0));
		shipContentsTrans.scaleCols3(new Vec3d(0.65, 0.6, 0));

		truckContentsTrans = new Mat4d();
		truckContentsTrans.setTranslate3(new Vec3d(-0.5, 0.0, 0.0));
		truckContentsTrans.scaleCols3(new Vec3d(0.75, 1, 0));

		arrowHeadVerts = new ArrayList<Vec4d>(3);
		arrowHeadVerts.add(new Vec4d(-0.5,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(-0.1, -0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(-0.1,  0.5, 0.0, 1.0d));

		arrowTailVerts = new ArrayList<Vec4d>(4);
		arrowTailVerts.add(new Vec4d(-0.1, -0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d( 0.5, -0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d( 0.5,  0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d(-0.1,  0.2, 0.0, 1.0d));

		arrowOutlineVerts = new ArrayList<Vec4d>(7);
		arrowOutlineVerts.add(new Vec4d(-0.5,  0.0, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1, -0.5, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.5, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.5,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1,  0.5, 0.0, 1.0d));

		truckCabVerts = new ArrayList<Vec4d>(4);
		truckCabVerts.add(new Vec4d( 0.5,  0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d(0.25,  0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d(0.25, -0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d( 0.5, -0.5, 0.0, 1.0d));

		crushingPlantBotVerts = new ArrayList<Vec4d>(4);
		crushingPlantBotVerts.add(new Vec4d( -0.17659f, -0.5f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( 0.15675f, -0.5f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( 0.15675f, -0.1f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( -0.17659f, -0.1f, 0.0f, 1.0d ));

		crushingPlantTopVerts = new ArrayList<Vec4d>(4);
		crushingPlantTopVerts.add(new Vec4d( -0.17659f, 0f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( 0.15675f, 0f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( 0.49008f, 0.5f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( -0.50992f, 0.5f, 0.0f, 1.0d ));

		singleQuadLinePoints = new ArrayList<Vec4d>();
		singleQuadLinePoints.add(new Vec4d( 0.4,  0.5, 0.0, 1.0d));
		singleQuadLinePoints.add(new Vec4d(-0.4,  0.0, 0.0, 1.0d));

		singleQuadLinePoints.add(new Vec4d(-0.4,  0.0, 0.0, 1.0d));
		singleQuadLinePoints.add(new Vec4d( 0.4, -0.5, 0.0, 1.0d));
		// Also add the arc lines
		List<Vec4d> singleArcPoints = RenderUtils.getArcPoints(0.6, new Vec4d(-0.3, 0, 0, 1.0d),
		                                                          Math.PI *  0.25,
		                                                          Math.PI *  0.75, 10);
		singleQuadLinePoints.addAll(singleArcPoints);

		singleQuadRectVerts = new ArrayList<Vec4d>();
		singleQuadRectVerts.add(new Vec4d( 0.5, -0.0833333, 0.0, 1.0d));
		singleQuadRectVerts.add(new Vec4d( 0.5,  0.0833333, 0.0, 1.0d));

		singleQuadRectVerts.add(new Vec4d(-0.5,  0.0833333, 0.0, 1.0d));
		singleQuadRectVerts.add(new Vec4d(-0.5, -0.0833333, 0.0, 1.0d));

		dualQuadLinePoints = new ArrayList<Vec4d>();
		dualQuadLinePoints.add(new Vec4d(0.4, 0.045454545, 0.0, 1.0d));
		dualQuadLinePoints.add(new Vec4d(-0.4, -0.227272727, 0.0, 1.0d));

		dualQuadLinePoints.add(new Vec4d(-0.4, -0.227272727, 0.0, 1.0d));
		dualQuadLinePoints.add(new Vec4d(0.4, -0.5, 0.0, 1.0d));

		dualQuadLinePoints.add(new Vec4d( 0.4, 0.5, 0.0, 1.0d));
		dualQuadLinePoints.add(new Vec4d( -0.4, 0.227272727, 0.0, 1.0d));

		dualQuadLinePoints.add(new Vec4d( -0.4, 0.227272727, 0.0, 1.0d));
		dualQuadLinePoints.add(new Vec4d( 0.4, -0.045454545, 0.0, 1.0d));
		List<Vec4d> dualArcPoints = RenderUtils.getArcPoints(0.6, new Vec4d(-0.3, -0.227272727, 0, 1.0d),
		                                                        Math.PI *  0.25,
		                                                        Math.PI *  0.75, 10);
		dualQuadLinePoints.addAll(dualArcPoints);
		dualArcPoints = RenderUtils.getArcPoints(0.6, new Vec4d(-0.3, 0.227272727, 0, 1.0d),
		                                         Math.PI *  0.25,
		                                         Math.PI *  0.75, 10);
		dualQuadLinePoints.addAll(dualArcPoints);

		dualQuadOutlineVerts = new ArrayList<Vec4d>();
		dualQuadOutlineVerts.add(new Vec4d(-0.5, -0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.5, 0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, 0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, 0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.3, 0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.3, -0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, -0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, -0.272727273, 0.0, 1.0d));


		dualQuadRect0Verts = new ArrayList<Vec4d>();
		dualQuadRect0Verts.add(new Vec4d(-0.5, -0.272727273, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(0.5, -0.272727273, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(0.5, -0.181818182, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(-0.5, -0.181818182, 0.0, 1.0d));

		dualQuadRect1Verts = new ArrayList<Vec4d>();
		dualQuadRect1Verts.add(new Vec4d(-0.5, -0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.3, -0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.3, 0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.5, 0.181818182, 0.0, 1.0d));

		dualQuadRect2Verts = new ArrayList<Vec4d>();
		dualQuadRect2Verts.add(new Vec4d(-0.5, 0.181818182, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(0.5, 0.181818182, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(0.5, 0.272727273, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(-0.5, 0.272727273, 0.0, 1.0d));

		travellingRect1Verts = new ArrayList<Vec4d>();
		travellingRect1Verts.add(new Vec4d(-0.2, -0.3, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d( 0.2, -0.3, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d( 0.2,  0.1, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d(-0.2,  0.1, 0, 1.0d));

		travellingRect2Verts = new ArrayList<Vec4d>();
		travellingRect2Verts.add(new Vec4d(-0.5, -0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d( 0.5, -0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d( 0.5,  0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d(-0.5,  0.1, 0, 1.0d));

		travellingRect3Verts = new ArrayList<Vec4d>();
		travellingRect3Verts.add(new Vec4d(-0.1, -0.5, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d( 0.1, -0.5, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d( 0.1,  0.1, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d(-0.1,  0.1, 0, 1.0d));

		stackerRect1Verts = new ArrayList<Vec4d>();
		stackerRect1Verts.add(new Vec4d( 0.3, -0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d( 0.3,  0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d(-0.3,  0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d(-0.3, -0.3, 0.1, 1.0d));

		stackerRect2Verts = new ArrayList<Vec4d>();
		stackerRect2Verts.add(new Vec4d(-0.1,  0.0, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d(-0.1, -0.5, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d( 0.1, -0.5, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d( 0.1,  0.0, 0.1, 1.0d));

		imageExtensions = new ArrayList<String>();
		imageExtensions.add(".PNG");
		imageExtensions.add(".JPG");
		imageExtensions.add(".BMP");
		imageExtensions.add(".GIF");

		modelExtensions = new ArrayList<String>();
		modelExtensions.add(".DAE");
		modelExtensions.add(".ZIP");
	}
}
