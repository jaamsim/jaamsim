/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;

public class ShapeModel extends DisplayModel {

	public static final String TAG_CONTENTS = "CONTENTS";
	public static final String TAG_CONTENTS2 = "CONTENTS2";
	public static final String TAG_CAPACITY = "CAPACITY";
	public static final String TAG_OUTLINES = "OUTLINES";
	public static final String TAG_BODY = "BODY";

	enum ValidShapes {
		RECTANGLE,
		CIRCLE,
		ARROW2D,
		TRIANGLE,
		PENTAGON,
		HEXAGON,
		OCTAGON,
		PENTAGRAM,
		HEPTAGRAM,
		OCTAGRAM,
		BARGAUGE2D,
	}

	@Keyword(description = "The shape of a display model determines the appearance of the display model.",
	         exampleList = { "CIRCLE" })
	private final EnumInput<ValidShapes> shape;

	@Keyword(description = "The colour for the filled part of the display model.",
	         exampleList = { "red" })
	private final ColourInput fillColour;

	@Keyword(description = "The colour for the outline part of the display model.",
	         exampleList = { "magenta" })
	private final ColourInput lineColour;

	@Keyword(description = "If the value is true, then the display model will have a solid fill. Otherwise, the display model " +
	                "will appear as hollow.",
	         exampleList = { "FALSE" })
	private final BooleanInput filled;

	@Keyword(description = "Determines whether or not the shape is outlined. "
	                     + "If TRUE, it is outlined with a uniform colour. "
	                     + "If FALSE, it is drawn without an outline.",
	         exampleList = {"FALSE"})
	private final BooleanInput outlined;

	@Keyword(description = "If the value is true, then the display model outline will be a bold line. Otherwise the outline " +
	                "will be one pixel wide line.",
	         exampleList = { "TRUE" })
	private final BooleanInput bold;

	@Keyword(description = "Width of the outline in pixels.",
	         exampleList = { "3" })
	private final IntegerInput lineWidth;

	{
		shape = new EnumInput<>(ValidShapes.class, "Shape", GRAPHICS, ValidShapes.CIRCLE);
		this.addInput(shape);

		fillColour = new ColourInput("FillColour", GRAPHICS, ColourInput.MED_GREY);
		this.addInput(fillColour);
		this.addSynonym(fillColour, "FillColor");

		lineColour = new ColourInput("LineColour", GRAPHICS, ColourInput.BLACK);
		this.addInput(lineColour);
		this.addSynonym(lineColour, "OutlineColour");
		this.addSynonym(lineColour, "OutlineColor");

		filled = new BooleanInput("Filled", GRAPHICS, true);
		this.addInput(filled);

		outlined = new BooleanInput("Outlined", GRAPHICS, true);
		this.addInput(outlined);

		bold = new BooleanInput("Bold", GRAPHICS, false);
		bold.setHidden(true);
		this.addInput(bold);

		lineWidth = new IntegerInput("LineWidth", GRAPHICS, 1);
		lineWidth.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(lineWidth);
	}

	public ShapeModel() {}

	public String getShapeName() {
		return shape.getValue().name();
	}

	public boolean isFilled() {
		return filled.getValue();
	}

	public boolean isOutlined() {
		return outlined.getValue();
	}

	public int getLineWidth() {
		if (!bold.isDefault() && lineWidth.isDefault())
			return bold.getValue() ? 2 : 1;
		return lineWidth.getValue();
	}

	public Color4d getFillColour() {
		return fillColour.getValue();
	}

	public Color4d getLineColour() {
		return lineColour.getValue();
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
		private boolean outlinedCache;
		private int lineWidthCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)ent;
		}

		private void updateCache(double simTime) {

			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			HashMap<String, Tag> tags = getTags();
			VisibilityInfo vi = getVisibilityInfo();
			ValidShapes sc = shape.getValue();
			Color4d fc = fillColour.getValue();
			Color4d oc =  lineColour.getValue();
			boolean fill = filled.getValue();
			boolean outln = outlined.getValue();
			int width = getLineWidth();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || dirty_tags(tagsCache, tags);
			dirty = dirty || !compare(shapeCache, sc);
			dirty = dirty || !compare(viCache, vi);
			dirty = dirty || fillColourCache != fc;
			dirty = dirty || outlineColourCache != oc;
			dirty = dirty || filledCache != fill;
			dirty = dirty || outlinedCache != outln;
			dirty = dirty || lineWidthCache != width;

			transCache = trans;
			scaleCache = scale;
			tagsCache = new HashMap<>(tags);
			shapeCache = sc;
			viCache = vi;
			fillColourCache = fc;
			outlineColourCache = oc;
			filledCache = fill;
			outlinedCache = outln;
			lineWidthCache = width;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				registerCacheHit("DisplayModelCompat");
				return;
			}

			registerCacheMiss("DisplayModelCompat");

			cachedProxies = new ArrayList<>();

			List<Vec4d> points = null;
			switch (shapeCache) {
			case BARGAUGE2D:
				addBarGaugeProxies(simTime);
				return;
			case ARROW2D:
				addArrowProxies(simTime);
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
			case PENTAGON:
				points = RenderUtils.PENTAGON_POINTS;
				break;
			case HEXAGON:
				points = RenderUtils.HEXAGON_POINTS;
				break;
			case OCTAGON:
				points = RenderUtils.OCTAGON_POINTS;
				break;
			case PENTAGRAM:
				points = RenderUtils.PENTAGRAM_POINTS;
				break;
			case HEPTAGRAM:
				points = RenderUtils.HEPTAGRAM_POINTS;
				break;
			case OCTAGRAM:
				points = RenderUtils.OCTAGRAM_POINTS;
				break;
			}

			// Gather some inputs
			if (outlinedCache && isTagVisible(ShapeModel.TAG_OUTLINES))
			{
				Color4d colour = getTagColor(ShapeModel.TAG_OUTLINES, outlineColourCache);
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, true, width, getVisibilityInfo(), pickingID));
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

	}

	private static final HashMap<String, Tag> emptyTagSet = new HashMap<>(0);

	// Begin static data
	// Since Arrows aren't convex, we need some more convoluted vertices
	private static List<Vec4d> arrowHeadVerts;
	private static List<Vec4d> arrowTailVerts;
	private static List<Vec4d> arrowOutlineVerts;

	static {
		arrowHeadVerts = new ArrayList<>(3);
		arrowHeadVerts.add(new Vec4d( 0.5,  0.0, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d( 0.1,  0.5, 0.0, 1.0d));
		arrowHeadVerts.add(new Vec4d( 0.1, -0.5, 0.0, 1.0d));

		arrowTailVerts = new ArrayList<>(4);
		arrowTailVerts.add(new Vec4d( 0.1,  0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d(-0.5,  0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d(-0.5, -0.2, 0.0, 1.0d));
		arrowTailVerts.add(new Vec4d( 0.1, -0.2, 0.0, 1.0d));

		arrowOutlineVerts = new ArrayList<>(7);
		arrowOutlineVerts.add(new Vec4d( 0.5,  0.0, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.1,  0.5, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.1,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.5,  0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d(-0.5, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.1, -0.2, 0.0, 1.0d));
		arrowOutlineVerts.add(new Vec4d( 0.1, -0.5, 0.0, 1.0d));
	}
}
