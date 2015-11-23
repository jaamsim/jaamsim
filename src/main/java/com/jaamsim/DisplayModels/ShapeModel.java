/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Tag;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EnumInput;
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

public class ShapeModel extends DisplayModel {
	// IMPORTANT: If you add a tag here, make sure to add it to the validTags
	public static final String TAG_CONTENTS = "CONTENTS";
	public static final String TAG_CONTENTS2 = "CONTENTS2";
	public static final String TAG_CAPACITY = "CAPACITY";
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
	@Keyword(description = "The shape of a display model determines the appearance of the display model.",
	         exampleList = { "CIRCLE" })
	private final EnumInput<ValidShapes> shape;

	@Keyword(description = "The colour for the filled part of the display model.",
	         exampleList = { "red" })
	private final ColourInput fillColour;

	@Keyword(description = "The colour for the outline part of the display model.",
	         exampleList = { "magenta" })
	private final ColourInput outlineColour;

	@Keyword(description = "If the value is true, then the display model will have a solid fill. Otherwise, the display model " +
	                "will appear as hollow.",
	         exampleList = { "FALSE" })
	private final BooleanInput filled;

	@Keyword(description = "If the value is true, then the display model outline will be a bold line. Otherwise the outline " +
	                "will be one pixel wide line.",
	         exampleList = { "TRUE" })
	private final BooleanInput bold;

	static {
		validTags = new ArrayList<>();
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
		shape = new EnumInput<>(ValidShapes.class, "Shape", "Key Inputs", ValidShapes.CIRCLE);
		this.addInput(shape);

		fillColour = new ColourInput("FillColour", "Key Inputs", ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		outlineColour = new ColourInput("OutlineColour", "Key Inputs", ColourInput.BLACK);
		this.addInput(outlineColour);
		this.addSynonym(outlineColour, "OutlineColor");

		filled = new BooleanInput("Filled", "Key Inputs", true);
		this.addInput(filled);

		bold = new BooleanInput("Bold", "Key Inputs", false);
		this.addInput(bold);
	}

	public ShapeModel() {}

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
		private HashMap<String, Tag> tagsCache = emptyTagSet;
		private ValidShapes shapeCache;
		private VisibilityInfo viCache;
		private Color4d fillColourCache;
		private Color4d outlineColourCache;
		private boolean filledCache;
		private boolean boldCache;

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
			HashMap<String, Tag> tags = getTags();
			VisibilityInfo vi = getVisibilityInfo();
			ValidShapes sc = shape.getValue();
			Color4d fc = fillColour.getValue();
			Color4d oc =  outlineColour.getValue();
			boolean fill = filled.getValue();
			boolean bld = bold.getValue();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || dirty_tags(tagsCache, tags);
			dirty = dirty || !compare(shapeCache, sc);
			dirty = dirty || !compare(viCache, vi);
			dirty = dirty || fillColourCache != fc;
			dirty = dirty || outlineColourCache != oc;
			dirty = dirty || filledCache != fill;
			dirty = dirty || boldCache != bld;

			transCache = trans;
			scaleCache = scale;
			tagsCache = new HashMap<>(tags);
			shapeCache = sc;
			viCache = vi;
			fillColourCache = fc;
			outlineColourCache = oc;
			filledCache = fill;
			boldCache = bld;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				registerCacheHit("DisplayModelCompat");
				return;
			}

			registerCacheMiss("DisplayModelCompat");

			cachedProxies = new ArrayList<>();

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
			if (isTagVisible(ShapeModel.TAG_OUTLINES))
			{
				Color4d colour = getTagColor(ShapeModel.TAG_OUTLINES, outlineColourCache);
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, true, (boldCache ? 2 : 1), getVisibilityInfo(), pickingID));
			}

			if (filledCache && isTagVisible(ShapeModel.TAG_CONTENTS))
			{
				Color4d colour = getTagColor(ShapeModel.TAG_CONTENTS, fillColourCache);
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, false, 1, getVisibilityInfo(), pickingID));
			}
		}

		private boolean isTagVisible(String name) {
			Tag t = tagsCache.get(name);
			if (t == null) return true;
			return t.visible;
		}

		private Color4d getTagColor(String name, Color4d def) {
			Tag t = tagsCache.get(name);
			if (t == null || t.colors == null || t.colors.length == 0) return def;
			return t.colors[0];
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
			return dispEnt.getGlobalTrans();
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

		private HashMap<String, Tag> getTags() {
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

			Color4d fillColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.BLACK);

			cachedProxies.add(new PolygonProxy(arrowHeadVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(arrowTailVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour= getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(arrowOutlineVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

		}

		private void addShipProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();

			// Now this is very 'shippy' behaviour and basically hand copied from the old DisplayModels (and supporting cast)

			// Hull
			Color4d hullColour = getTagColor(ShapeModel.TAG_BODY, ColourInput.LIGHT_GREY);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, hullColour, false, 1, getVisibilityInfo(), pickingID));

			// Outline
			Color4d outlineColour= getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			cachedProxies.add(new PolygonProxy(hullVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			Transform cabinTrans = new Transform(trans);
			cabinTrans.getTransRef().z += scale.y * 0.01; // Bump the cabin up a touch to prevent Z fighting
			// Cabin
			cachedProxies.add(new PolygonProxy(shipCabinVerts, cabinTrans, scale, ColourInput.BLACK, false, 1, getVisibilityInfo(), pickingID));

			// Add the contents parcels
			Tag t = tagsCache.get(ShapeModel.TAG_CONTENTS);
			double[] sizes = null;
			Color4d[] colours = null;
			if (t != null) {
				sizes = t.sizes;
				colours = t.colors;
			}
			Tag t2 = tagsCache.get(ShapeModel.TAG_OUTLINES);
			double[] outlineSizes = null;
			if(t2 != null ) {
				outlineSizes = t2.sizes;
			}
			cachedProxies.addAll(buildContents(sizes, colours, outlineSizes, shipContentsTrans, trans, scale, pickingID));
		}

		private void addTruckProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();

			// Add a yellow rectangle for the cab
			cachedProxies.add(new PolygonProxy(truckCabVerts, trans, scale, ColourInput.YELLOW, false, 1, getVisibilityInfo(), pickingID));

			Tag t = tagsCache.get(ShapeModel.TAG_CONTENTS);
			double[] sizes = null;
			Color4d[] colours = null;
			if (t != null) {
				sizes = t.sizes;
				colours = t.colors;
			}
			cachedProxies.addAll(buildContents(sizes, colours, null, truckContentsTrans, trans, scale, pickingID));
		}

		private void addBarGaugeProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();

			Tag tag_contents = tagsCache.get(ShapeModel.TAG_CONTENTS);
			if (tag_contents == null || tag_contents.sizes == null || tag_contents.colors == null ||
			    tag_contents.colors.length < tag_contents.sizes.length) {
				// Bail out, not properly initialized
				return;
			}

			double[] sizes = tag_contents.sizes;

			Tag tag_contents2 = tagsCache.get(ShapeModel.TAG_CONTENTS2);
			double[] rescapacities = null;
			Color4d[] rescolours = null;
			if (tag_contents2 != null) {
				rescapacities = tag_contents2.sizes;
				rescolours = tag_contents2.colors;
			}
			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d backgroundColour = getTagColor(ShapeModel.TAG_BODY, ColourInput.WHITE);

			Tag tag_cap = tagsCache.get(ShapeModel.TAG_CAPACITY);
			double[] capacities;
			if (tag_cap == null || tag_cap.sizes == null || tag_cap.sizes.length != sizes.length) {
				capacities = new double[sizes.length];
				for (int i = 0; i < sizes.length; ++i) {
					capacities[i] = 1.0d;
				}
			}
			else {
				capacities = tag_cap.sizes;
			}


			double totalCap = 0;
			for (int i = 0; i < capacities.length; ++i) {
				totalCap += capacities[i];
			}
			if (totalCap == 0) totalCap = 1; // Guard against div by 0

			// Add the background and outline
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, backgroundColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));

			double accumWidth = 0;
			for (int i = 0; i < sizes.length; ++i) {
				// Add a rectangle for each size

				double size = sizes[i];
				double width = capacities[i]/totalCap;

				double startX = accumWidth - 0.5;
				double endX = accumWidth + width - 0.5;

				accumWidth += width;

				double startY = -0.5;
				double endY = size - 0.5;

				List<Vec4d> contentsPoints = new ArrayList<>();
				contentsPoints.add(new Vec4d(  endX, startY, 0, 1.0d));
				contentsPoints.add(new Vec4d(  endX,   endY, 0, 1.0d));
				contentsPoints.add(new Vec4d(startX,   endY, 0, 1.0d));
				contentsPoints.add(new Vec4d(startX, startY, 0, 1.0d));

				cachedProxies.add(new PolygonProxy(contentsPoints, trans, scale, tag_contents.colors[i], false, 1, getVisibilityInfo(), pickingID));

				if (rescapacities != null) {
					double startResY = endY;
					double endResY = startResY + rescapacities[i];
					List<Vec4d> rescontentsPoints = new ArrayList<>();
					rescontentsPoints.add(new Vec4d(  endX, startResY, 0, 1.0d));
					rescontentsPoints.add(new Vec4d(  endX,   endResY, 0, 1.0d));
					rescontentsPoints.add(new Vec4d(startX,   endResY, 0, 1.0d));
					rescontentsPoints.add(new Vec4d(startX, startResY, 0, 1.0d));

					cachedProxies.add(new PolygonProxy(rescontentsPoints, trans, scale, rescolours[i], false, 1, getVisibilityInfo(), pickingID));
				}
			}
		}

		private void addCrushingPlantProxies(double simTime) {

			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();

			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);

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

			// Add the lines
			Mat4d lineTrans = new Mat4d();
			trans.getMat4d(lineTrans);
			lineTrans.scaleCols3(scale);
			List<Vec4d> points = RenderUtils.transformPoints(lineTrans, singleQuadLinePoints, 0);
			cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);

			cachedProxies.add(new PolygonProxy(singleQuadRectVerts, trans, scale, fillColour, false, 1, getVisibilityInfo(), pickingID));
			cachedProxies.add(new PolygonProxy(singleQuadRectVerts, trans, scale, outlineColour, true, 1, getVisibilityInfo(), pickingID));
		}


		private void addDualQuadProxies(double simTime) {
			// Gather some inputs
			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();

			// Add the lines
			Mat4d lineTrans = new Mat4d();
			trans.getMat4d(lineTrans);
			lineTrans.scaleCols3(scale);
			List<Vec4d> points = RenderUtils.transformPoints(lineTrans, dualQuadLinePoints, 0);
			cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, getVisibilityInfo(), pickingID));

			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);

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


			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d fillColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);
			Color4d trackColour = getTagColor(ShapeModel.TAG_TRACKFILL, ColourInput.MED_GREY);

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

			Transform contentsTrans = new Transform(trans);
			contentsTrans.getTransRef().z += scale.y * 0.001;

			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d contentsColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);
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

			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d contentsColour = getTagColor(ShapeModel.TAG_CONTENTS, ColourInput.MED_GREY);

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
		private List<RenderProxy> buildContents(double[] sizes, Color4d[] colours, double[] outlineSizes, Mat4d subTrans,
		                                        Transform trans, Vec3d scale, long pickingID) {
			List<RenderProxy> ret = new ArrayList<>();

			if (sizes == null || colours == null || sizes.length != colours.length) {
				// We are either out of sync or this is a ShipType, either way draw an empty cargo hold
				// Add a single grey rectangle
				List<Vec4d> contentsPoints = new ArrayList<>();
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

			double totalSize = 0.0d;
			for (int i = 0; i < sizes.length; ++i) {
				totalSize += sizes[i];
			}
			double sizeOffset = 0;

			double totalOutlineSize = 0.0d;
			int indexOfNextHold = 0;

			// The following properties are expressed as a fraction of total cargo
			double endOfNextHold = 1.0;
			double gapBetweenHolds = 0.0075;

			if( outlineSizes != null ) {
				for (int i = 0; i < outlineSizes.length; i++) {
					totalOutlineSize += outlineSizes[i];
				}

				// Determine the end of the next hold
				if( outlineSizes.length > 0 )
					endOfNextHold = outlineSizes[0] / totalOutlineSize;
			}

			for (int i = 0; i < sizes.length; ++i) {
				// Add a rectangle for each parcel

				double size = sizes[i];
				double start = sizeOffset / totalSize;
				double end = (sizeOffset + size) / totalSize;

				List<Vec4d> contentsPoints = new ArrayList<>();
				contentsPoints.add(new Vec4d(  end, -0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(  end,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(start,  0.5, 0.001, 1.0d));
				contentsPoints.add(new Vec4d(start, -0.5, 0.001, 1.0d));

				sizeOffset += size;

				// Is this parcel the end of the hold?
				if( Math.abs( end - endOfNextHold ) < 1.0E-9 ) {
					sizeOffset += (gapBetweenHolds * totalSize);

					// Update the end of the next hold
					indexOfNextHold++;
					if( outlineSizes != null ) {
						if( indexOfNextHold < outlineSizes.length )
							endOfNextHold += gapBetweenHolds + (outlineSizes[indexOfNextHold] / totalOutlineSize);
					}
				}

				for (int j = 0; j < contentsPoints.size(); ++j) {
					contentsPoints.get(j).mult4(subTrans, contentsPoints.get(j));
				}

				ret.add(new PolygonProxy(contentsPoints, trans, scale, colours[i], false, 1, getVisibilityInfo(), pickingID));
			}

			if( outlineSizes != null ) {
				double outlineSizeOffset = 0;

				for (int i = 0; i < outlineSizes.length; i++) {
					// Add a rectangle for each hold

					double outlineSize = outlineSizes[i];
					double start = outlineSizeOffset / totalOutlineSize;
					double end = (outlineSizeOffset + outlineSize) / totalOutlineSize;

					List<Vec4d> outlinePoints = new ArrayList<>();
					outlinePoints.add(new Vec4d(  end, -0.5, 0.001, 1.0d));
					outlinePoints.add(new Vec4d(  end,  0.5, 0.001, 1.0d));
					outlinePoints.add(new Vec4d(start,  0.5, 0.001, 1.0d));
					outlinePoints.add(new Vec4d(start, -0.5, 0.001, 1.0d));

					outlineSizeOffset += outlineSize + ( gapBetweenHolds * totalOutlineSize );

					for (int j = 0; j < outlinePoints.size(); ++j) {
						outlinePoints.get(j).mult4(subTrans, outlinePoints.get(j));
					}

					ret.add(new PolygonProxy(outlinePoints, trans, scale, ColourInput.BLACK, true, 1, getVisibilityInfo(), pickingID));

				}
			}
			return ret;
		}

	}

	private static final HashMap<String, Tag> emptyTagSet = new HashMap<>(0);

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
		hullVerts = new ArrayList<>(20);
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
		shipContentsTrans.setTranslate3(new Vec3d(-0.25, 0.0, 0.0));
		shipContentsTrans.scaleCols3(new Vec3d(0.65, 0.6, 1));

		truckContentsTrans = new Mat4d();
		truckContentsTrans.setTranslate3(new Vec3d(-0.5, 0.0, 0.0));
		truckContentsTrans.scaleCols3(new Vec3d(0.75, 1, 1));

		arrowHeadVerts = new ArrayList<>(3);
		arrowHeadVerts.add(new Vec4d(-0.5,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(-0.1, -0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d(-0.1,  0.5, 0.0, 1.0d));

		arrowTailVerts = new ArrayList<>(4);
		arrowTailVerts.add(new Vec4d(-0.1, -0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d( 0.5, -0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d( 0.5,  0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d(-0.1,  0.2, 0.0, 1.0d));

		arrowOutlineVerts = new ArrayList<>(7);
		arrowOutlineVerts.add(new Vec4d(-0.5,  0.0, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1, -0.5, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.5, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.5,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.1,  0.5, 0.0, 1.0d));

		truckCabVerts = new ArrayList<>(4);
		truckCabVerts.add(new Vec4d( 0.5,  0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d(0.25,  0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d(0.25, -0.5, 0.0, 1.0d));
		truckCabVerts.add(new Vec4d( 0.5, -0.5, 0.0, 1.0d));

		crushingPlantBotVerts = new ArrayList<>(4);
		crushingPlantBotVerts.add(new Vec4d( -0.17659f, -0.5f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( 0.15675f, -0.5f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( 0.15675f, -0.1f, 0.0f, 1.0d ));
		crushingPlantBotVerts.add(new Vec4d( -0.17659f, -0.1f, 0.0f, 1.0d ));

		crushingPlantTopVerts = new ArrayList<>(4);
		crushingPlantTopVerts.add(new Vec4d( -0.17659f, 0f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( 0.15675f, 0f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( 0.49008f, 0.5f, 0.0f, 1.0d ));
		crushingPlantTopVerts.add(new Vec4d( -0.50992f, 0.5f, 0.0f, 1.0d ));

		singleQuadLinePoints = new ArrayList<>();
		singleQuadLinePoints.add(new Vec4d( 0.4,  0.5, 0.0, 1.0d));
		singleQuadLinePoints.add(new Vec4d(-0.4,  0.0, 0.0, 1.0d));

		singleQuadLinePoints.add(new Vec4d(-0.4,  0.0, 0.0, 1.0d));
		singleQuadLinePoints.add(new Vec4d( 0.4, -0.5, 0.0, 1.0d));
		// Also add the arc lines
		List<Vec4d> singleArcPoints = RenderUtils.getArcPoints(0.6, new Vec4d(-0.3, 0, 0, 1.0d),
		                                                          Math.PI *  0.25,
		                                                          Math.PI *  0.75, 10);
		singleQuadLinePoints.addAll(singleArcPoints);

		singleQuadRectVerts = new ArrayList<>();
		singleQuadRectVerts.add(new Vec4d( 0.5, -0.0833333, 0.0, 1.0d));
		singleQuadRectVerts.add(new Vec4d( 0.5,  0.0833333, 0.0, 1.0d));

		singleQuadRectVerts.add(new Vec4d(-0.5,  0.0833333, 0.0, 1.0d));
		singleQuadRectVerts.add(new Vec4d(-0.5, -0.0833333, 0.0, 1.0d));

		dualQuadLinePoints = new ArrayList<>();
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

		dualQuadOutlineVerts = new ArrayList<>();
		dualQuadOutlineVerts.add(new Vec4d(-0.5, -0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.5, 0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, 0.272727273, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, 0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.3, 0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(-0.3, -0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, -0.181818182, 0.0, 1.0d));
		dualQuadOutlineVerts.add(new Vec4d(0.5, -0.272727273, 0.0, 1.0d));


		dualQuadRect0Verts = new ArrayList<>();
		dualQuadRect0Verts.add(new Vec4d(-0.5, -0.272727273, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(0.5, -0.272727273, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(0.5, -0.181818182, 0.0, 1.0d));
		dualQuadRect0Verts.add(new Vec4d(-0.5, -0.181818182, 0.0, 1.0d));

		dualQuadRect1Verts = new ArrayList<>();
		dualQuadRect1Verts.add(new Vec4d(-0.5, -0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.3, -0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.3, 0.181818182, 0.0, 1.0d));
		dualQuadRect1Verts.add(new Vec4d(-0.5, 0.181818182, 0.0, 1.0d));

		dualQuadRect2Verts = new ArrayList<>();
		dualQuadRect2Verts.add(new Vec4d(-0.5, 0.181818182, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(0.5, 0.181818182, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(0.5, 0.272727273, 0.0, 1.0d));
		dualQuadRect2Verts.add(new Vec4d(-0.5, 0.272727273, 0.0, 1.0d));

		travellingRect1Verts = new ArrayList<>();
		travellingRect1Verts.add(new Vec4d(-0.2, -0.3, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d( 0.2, -0.3, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d( 0.2,  0.1, 0, 1.0d));
		travellingRect1Verts.add(new Vec4d(-0.2,  0.1, 0, 1.0d));

		travellingRect2Verts = new ArrayList<>();
		travellingRect2Verts.add(new Vec4d(-0.5, -0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d( 0.5, -0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d( 0.5,  0.1, 0, 1.0d));
		travellingRect2Verts.add(new Vec4d(-0.5,  0.1, 0, 1.0d));

		travellingRect3Verts = new ArrayList<>();
		travellingRect3Verts.add(new Vec4d(-0.1, -0.5, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d( 0.1, -0.5, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d( 0.1,  0.1, 0, 1.0d));
		travellingRect3Verts.add(new Vec4d(-0.1,  0.1, 0, 1.0d));

		stackerRect1Verts = new ArrayList<>();
		stackerRect1Verts.add(new Vec4d( 0.3, -0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d( 0.3,  0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d(-0.3,  0.3, 0.1, 1.0d));
		stackerRect1Verts.add(new Vec4d(-0.3, -0.3, 0.1, 1.0d));

		stackerRect2Verts = new ArrayList<>();
		stackerRect2Verts.add(new Vec4d(-0.1,  0.0, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d(-0.1, -0.5, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d( 0.1, -0.5, 0.1, 1.0d));
		stackerRect2Verts.add(new Vec4d( 0.1,  0.0, 0.1, 1.0d));

		crusher2DVerts = new ArrayList<>();
		crusher2DVerts.add(new Vec4d( 0.45, -0.45, 0.0, 1.0));
		crusher2DVerts.add(new Vec4d(  0.0,  0.25, 0.0, 1.0));
		crusher2DVerts.add(new Vec4d(-0.45, -0.45, 0.0, 1.0));

		crusher2DLines = new ArrayList<>();
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
