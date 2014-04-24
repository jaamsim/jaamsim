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
import java.util.List;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EnumInput;

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

	enum ValidShapes {
		PIXELS,
		TRUCK2D,
		SHIP2D,
		RECTANGLE,
		STACKER2D,
		RECLAIMER2D,
		BRIDGE2D,
		CRUSHER2D,
		GANTRY2D,
		DOZER2D,
		CRUSHER2ND2D,
		SLAVESTACKER2D,
		DUALQUADRANT2D,
		SINGLEQUADRANT2D,
		LINEAR2D,
		TRAVELLING2D,
		CIRCLE,
		ARROW2D,
		TRIANGLE,
		CONTENTSPIXELS,
		CRUSHINGPLANT2D,
		BARGAUGE2D,
		MINISHIP2D,
		GRINDINGROLL2D,
		SCREEN2D,
		SAGMILL2D,
		RECTANGLEWITHARROWS,
	}
	@Keyword(description = "The shape of a display model determines the appearance of the display model. The shape may be " +
	                "one of the following: Pixels (for a square of 6x6 pixels), Truck2D, Ship2D, Icon (for a rectangle), " +
	                "Circle.",
	         example = "Ship3DModel Shape { CIRCLE }")
	private final EnumInput<ValidShapes> shape;

	@Keyword(description = "The colour for the filled part of the display model.",
	         example = "Product2D FillColour { red }")
	private final ColourInput fillColour;

	@Keyword(description = "The colour for the outline part of the display model.",
	         example = "Berth2D OutlineColour { magenta }")
	private final ColourInput outlineColour;

	@Keyword(description = "If the value is true, then the display model will have a solid fill. Otherwise, the display model " +
	                "will appear as hollow.",
	         example = "Berth2D Filled { FALSE }")
	private final BooleanInput filled;

	@Keyword(description = "If the value is true, then the display model outline will be a dashed line. Otherwise, the outline " +
	                "will be a solid line.",
	         example = "StockpileLine2D Dashed { TRUE }")
	private final BooleanInput dashed;

	@Keyword(description = "If the value is true, then the display model outline will be a bold line. Otherwise the outline " +
	                "will be one pixel wide line.",
	         example = "Berth2D Bold { TRUE }")
	private final BooleanInput bold;

	static {
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
		shape = new EnumInput<ValidShapes>(ValidShapes.class, "Shape", "DisplayModel", ValidShapes.CIRCLE);
		this.addInput(shape);

		fillColour = new ColourInput("FillColour", "DisplayModel", ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		outlineColour = new ColourInput("OutlineColour", "DisplayModel", ColourInput.BLACK);
		this.addInput(outlineColour);
		this.addSynonym(outlineColour, "OutlineColor");

		filled = new BooleanInput("Filled", "DisplayModel", true);
		this.addInput(filled);

		dashed = new BooleanInput("Dashed", "DisplayModel", false);
		this.addInput(dashed);

		bold = new BooleanInput("Bold", "DisplayModel", false);
		this.addInput(bold);
	}

	public DisplayModelCompat() {}

	public String getShapeName() {
		return shape.getValue().name();
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

		private DisplayEntity dispEnt;

		private Transform transCache;
		private Vec3d scaleCache;
		private DisplayEntity.TagSet tagsCache;
		private ValidShapes shapeCache;
		private VisibilityInfo viCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)ent;

			if (dispEnt != null) {
			}
		}

		private void updateCache(double simTime) {

			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();
			VisibilityInfo vi = getVisibilityInfo();
			ValidShapes sc = shape.getValue();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || !tags.isSame(tagsCache);
			dirty = dirty || !compare(shapeCache, sc);
			dirty = dirty || !compare(viCache, vi);

			transCache = trans;
			scaleCache = scale;
			tagsCache = new DisplayEntity.TagSet(tags);
			shapeCache = sc;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				registerCacheHit("DisplayModelCompat");
				return;
			}

			registerCacheMiss("DisplayModelCompat");

			cachedProxies = new ArrayList<RenderProxy>();

			List<Vec4d> points = null;
			switch (shapeCache) {
			case SHIP2D:
				addShipProxies(simTime);
				return;
			case TRUCK2D:
				addTruckProxies(simTime);
				return;
			case BARGAUGE2D:
				addBarGaugeProxies(simTime);
				return;
			case CRUSHINGPLANT2D:
				addCrushingPlantProxies(simTime);
				return;
			case ARROW2D:
				addArrowProxies(simTime);
				return;
			case SINGLEQUADRANT2D:
				addSingleQuadProxies(simTime);
				return;
			case DUALQUADRANT2D:
				addDualQuadProxies(simTime);
				return;
			case TRAVELLING2D:
				addTravellingProxies(simTime);
				return;
			case STACKER2D:
			case RECLAIMER2D:
				addStackerProxies(simTime);
				return;
			case CRUSHER2D:
				addCrusher2DProxies(simTime);
				return;
			case CIRCLE:
				points = RenderUtils.CIRCLE_POINTS;
				break;
			case RECTANGLE:
				points = RenderUtils.RECT_POINTS;
				break;
			case TRIANGLE:
				points = RenderUtils.TRIANGLE_POINTS;
				break;
			case BRIDGE2D:
			case CONTENTSPIXELS:
			case CRUSHER2ND2D:
			case DOZER2D:
			case GANTRY2D:
			case GRINDINGROLL2D:
			case LINEAR2D:
			case MINISHIP2D:
			case PIXELS:
			case RECTANGLEWITHARROWS:
			case SAGMILL2D:
			case SCREEN2D:
			case SLAVESTACKER2D:
				// these are currently not implemented
				return;
			}

			// Gather some inputs

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
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			// This is slightly quirky behaviour, as a null entity will be shown because we use that for previews
			if (dispEnt == null || !dispEnt.getShow()) {
				return;
			}

			updateCache(simTime);

			out.addAll(cachedProxies);
		}

		private Transform getTransform(double simTime) {
			if (dispEnt == null) {
				return Transform.ident;
			}
			return dispEnt.getGlobalTrans(simTime);
		}
		private Vec3d getScale() {
			if (dispEnt == null) {
				return DisplayModel.ONES;
			}
			Vec3d size = dispEnt.getSize();
			size.mul3(getModelScale());
			return size;
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

		private void addArrowProxies(double simTime) {
			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Color4d fillColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.BLACK);

			cachedProxies.add(new PolygonProxy(arrowHeadVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(arrowTailVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour= tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(arrowOutlineVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addShipProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Now this is very 'shippy' behaviour and basically hand copied from the old DisplayModels (and supporting cast)

			// Hull
			Color4d hullColour = tags.getTagColourUtil(DisplayModelCompat.TAG_BODY, ColourInput.LIGHT_GREY);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, hullColour, false, 1, getVisibilityInfo(), pickingID));

			// Outline
			Color4d outlineColour= tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			Transform cabinTrans = new Transform(trans);
			cabinTrans.getTransRef().z += scale.y * 0.01; // Bump the cabin up a touch to prevent Z fighting
			// Cabin
			cachedProxies.add(new PolygonProxy(shipCabinVerts, cabinTrans, scale, ColourInput.BLACK, false, 1, getVisibilityInfo(), pickingID));

			// Add the contents parcels
			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);

			cachedProxies.addAll(buildContents(sizes, colours, shipContentsTrans, trans, scale, pickingID));
		}

		private void addTruckProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			// Add a yellow rectangle for the cab
			cachedProxies.add(new PolygonProxy(truckCabVerts, trans, scale, ColourInput.YELLOW, false, 1, getVisibilityInfo(), pickingID));

			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);

			cachedProxies.addAll(buildContents(sizes, colours, truckContentsTrans, trans, scale, pickingID));
		}

		private void addBarGaugeProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			DoubleVector sizes = tags.sizes.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d[] colours = tags.colours.get(DisplayModelCompat.TAG_CONTENTS);
			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d backgroundColour = tags.getTagColourUtil(DisplayModelCompat.TAG_BODY, ColourInput.WHITE);
			if (sizes == null) {
				sizes = new DoubleVector();
			}

			double width = 1.0;

			if (sizes.size() != 0) {
				width = 1.0 / sizes.size();
			}

			// Add the background and outline
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, backgroundColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

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

				cachedProxies.add(new PolygonProxy(contentsPoints, trans, scale, colours[i], false, 1, getVisibilityInfo(), pickingID));

			}
		}

		private void addCrushingPlantProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
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

		private void addSingleQuadProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
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


		private void addDualQuadProxies(double simTime) {
			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
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

		private void addTravellingProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
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

		private void addStackerProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Transform contentsTrans = new Transform(trans);
			contentsTrans.getTransRef().z += scale.y * 0.001;

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d contentsColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);
			Color4d trackColour = ColourInput.MED_GREY;

			// This is gross, but until we have proper draw ordering it's the kind of thing we have to do to keep the
			// Stacker-reclaimer appearing above the stock piles reliably
			Vec3d fixedScale = new Vec3d(scale);
			fixedScale.z = 0.1;

			cachedProxies.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, trackColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(stackerRect2Verts, contentsTrans, fixedScale, contentsColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(stackerRect2Verts, contentsTrans, fixedScale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addCrusher2DProxies(double simTime) {
			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			DisplayEntity.TagSet tags = getTags();

			Color4d outlineColour = tags.getTagColourUtil(DisplayModelCompat.TAG_OUTLINES, ColourInput.BLACK);
			Color4d contentsColour = tags.getTagColourUtil(DisplayModelCompat.TAG_CONTENTS, ColourInput.MED_GREY);

			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, contentsColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			cachedProxies.add(new PolygonProxy(crusher2DVerts, trans, scale, ColourInput.LIGHT_GREY, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(crusher2DVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			// Add the lines
			Mat4d lineTrans = new Mat4d();
			trans.getMat4d(lineTrans);
			lineTrans.scaleCols3(scale);
			List<Vec4d> points = RenderUtils.transformPoints(lineTrans, crusher2DLines, 0);
			cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, getVisibilityInfo(), pickingID));

		}

		// A disturbingly deep helper to allow trucks and ships to share contents building code
		// This class needs to either die or get refactored
		private List<RenderProxy> buildContents(DoubleVector sizes, Color4d[] colours, Mat4d subTrans,
		                                        Transform trans, Vec3d scale, long pickingID) {
			List<RenderProxy> ret = new ArrayList<RenderProxy>();

			if (sizes == null || colours == null || sizes.size() != colours.length) {
				// We are either out of sync or this is a ShipType, either way draw an empty cargo hold
				// Add a single grey rectangle
				List<Vec4d> contentsPoints = new ArrayList<Vec4d>();
				contentsPoints.add(new Vec4d( 1, -0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d( 1,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d( 0,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d( 0, -0.5, 0.001, 1.0d));
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
				contentsPoints.add(new Vec4d(  end, -0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(  end,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(start,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(start, -0.5, 0.001, 1.0d));

				sizeOffset += size;

				for (int j = 0; j < contentsPoints.size(); ++j) {
					contentsPoints.get(j).mult4(subTrans, contentsPoints.get(j));
				}

				ret.add(new PolygonProxy(contentsPoints, trans, scale, colours[i], false, 1, getVisibilityInfo(), pickingID));
			}
			return ret;
		}

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

	private static List<Vec4d> crusher2DVerts;
	private static List<Vec4d> crusher2DLines;

	private static List<Vec4d> hullVerts;
	private static List<Vec4d> shipCabinVerts;
	private static Mat4d shipContentsTrans;
	private static Mat4d truckContentsTrans;

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
		shipContentsTrans.scaleCols3(new Vec3d(0.65, 0.6, 1));

		truckContentsTrans = new Mat4d();
		truckContentsTrans.setTranslate3(new Vec3d(-0.5, 0.0, 0.0));
		truckContentsTrans.scaleCols3(new Vec3d(0.75, 1, 1));

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

		crusher2DVerts = new ArrayList<Vec4d>();
		crusher2DVerts.add(new Vec4d( 0.45, -0.45, 0.0, 1.0));
		crusher2DVerts.add(new Vec4d(  0.0,  0.25, 0.0, 1.0));
		crusher2DVerts.add(new Vec4d(-0.45, -0.45, 0.0, 1.0));

		crusher2DLines = new ArrayList<Vec4d>();
		crusher2DLines.add(new Vec4d(-0.45, -0.30, 0.0, 1.0));
		crusher2DLines.add(new Vec4d(-0.10,  0.25, 0.0, 1.0));

		crusher2DLines.add(new Vec4d(-0.10,  0.25, 0.0, 1.0));
		crusher2DLines.add(new Vec4d(-0.45,  0.45, 0.0, 1.0));

		crusher2DLines.add(new Vec4d( 0.45, -0.30, 0.0, 1.0));
		crusher2DLines.add(new Vec4d( 0.10,  0.25, 0.0, 1.0));

		crusher2DLines.add(new Vec4d( 0.10,  0.25, 0.0, 1.0));
		crusher2DLines.add(new Vec4d( 0.45,  0.45, 0.0, 1.0));

	}
}
