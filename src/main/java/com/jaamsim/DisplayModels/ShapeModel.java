/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.Tag;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EnumInput;
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

public class ShapeModel extends AbstractShapeModel {

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

	{
		filled.setDefaultValue(true);
		outlined.setDefaultValue(true);

		shape = new EnumInput<>(ValidShapes.class, "Shape", KEY_INPUTS, ValidShapes.CIRCLE);
		this.addInput(shape);
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
		private LineEntity lineEnt;
		private FillEntity fillEnt;

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
			if (ent instanceof LineEntity)
				lineEnt = (LineEntity) ent;
			if (ent instanceof FillEntity)
				fillEnt = (FillEntity) ent;
		}

		private void updateCache(double simTime) {

			Transform trans = getTransform(simTime);
			Vec3d scale = getScale();
			long pickingID = getPickingID();
			HashMap<String, Tag> tags = getTags();
			VisibilityInfo vi = getVisibilityInfo();
			ValidShapes sc = shape.getValue();
			Color4d fc = (fillEnt == null) ? getFillColour(simTime) : fillEnt.getFillColour(simTime);
			Color4d oc =  (lineEnt == null) ? getLineColour(simTime) : lineEnt.getLineColour(simTime);
			boolean fill = (fillEnt == null) ? isFilled(simTime) : fillEnt.isFilled(simTime);
			boolean outln = (lineEnt == null) ? isOutlined(simTime) : lineEnt.isOutlined(simTime);
			int width = (lineEnt == null) ? getLineWidth(simTime) : lineEnt.getLineWidth(simTime);

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
				points = RenderUtils.ARROW2D_POINTS;
				break;
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
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, true, width, vi, pickingID));
			}

			if (filledCache && isTagVisible(ShapeModel.TAG_CONTENTS))
			{
				Color4d colour = getTagColor(ShapeModel.TAG_CONTENTS, fillColourCache);
				cachedProxies.add(new PolygonProxy(points, trans, scale, colour, false, 1, vi, pickingID));
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
			if (dispEnt == null || !dispEnt.getShow(simTime)) {
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

		private void addBarGaugeProxies(double simTime) {
			long pickingID = getPickingID();

			// Calculate the z-coordinate offset for the layers
			Vec3d entSize = dispEnt.getSize();
			double zBump = 0.001d;
			if (entSize.z > 0.0d)
				zBump *= entSize.x / entSize.z;

			// Height for each bar
			Tag tag_contents = tagsCache.get(ShapeModel.TAG_CONTENTS);
			if (tag_contents == null)
				tag_contents = tag_contents_def;
			double[] sizes = tag_contents.sizes;

			// Height for the background above each bar
			Tag tag_contents2 = tagsCache.get(ShapeModel.TAG_CONTENTS2);
			double[] rescapacities = null;
			Color4d[] rescolours = null;
			if (tag_contents2 != null) {
				rescapacities = tag_contents2.sizes;
				rescolours = tag_contents2.colors;
			}

			// Width for each bar
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

			// Calculate the total width
			double totalCap = 0;
			for (int i = 0; i < capacities.length; ++i) {
				totalCap += capacities[i];
			}
			if (totalCap == 0) totalCap = 1; // Guard against div by 0

			// Draw the background and outline
			Color4d outlineColour = getTagColor(ShapeModel.TAG_OUTLINES, ColourInput.BLACK);
			Color4d backgroundColour = getTagColor(ShapeModel.TAG_BODY, ColourInput.LIGHT_GREY);
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, transCache, scaleCache, backgroundColour, false, 1, viCache, pickingID));
			cachedProxies.add(new PolygonProxy(RenderUtils.RECT_POINTS, transCache, scaleCache, outlineColour, true, 1, viCache, pickingID));

			// Loop through the values to be displayed
			double accumWidth = 0;
			for (int i = 0; i < sizes.length; ++i) {

				// Draw a vertical bar with the specified height and width
				// Each bar is placed to the right of the previous one
				double size = sizes[i];
				double width = capacities[i]/totalCap;

				double startX = accumWidth - 0.5;
				double endX = accumWidth + width - 0.5;

				accumWidth += width;

				double startY = -0.5;
				double endY = size - 0.5;

				List<Vec4d> contentsPoints = new ArrayList<>();
				contentsPoints.add(new Vec4d(  endX, startY, zBump, 1.0d));
				contentsPoints.add(new Vec4d(  endX,   endY, zBump, 1.0d));
				contentsPoints.add(new Vec4d(startX,   endY, zBump, 1.0d));
				contentsPoints.add(new Vec4d(startX, startY, zBump, 1.0d));

				cachedProxies.add(new PolygonProxy(contentsPoints, transCache, scaleCache, tag_contents.colors[i], false, 1, viCache, pickingID));

				// Draw a second vertical bar above the first one
				if (rescapacities != null) {
					double startResY = endY;
					double endResY = startResY + rescapacities[i];
					List<Vec4d> rescontentsPoints = new ArrayList<>();
					rescontentsPoints.add(new Vec4d(  endX, startResY, zBump, 1.0d));
					rescontentsPoints.add(new Vec4d(  endX,   endResY, zBump, 1.0d));
					rescontentsPoints.add(new Vec4d(startX,   endResY, zBump, 1.0d));
					rescontentsPoints.add(new Vec4d(startX, startResY, zBump, 1.0d));

					cachedProxies.add(new PolygonProxy(rescontentsPoints, transCache, scaleCache, rescolours[i], false, 1, viCache, pickingID));
				}
			}
		}

	}

	private static final HashMap<String, Tag> emptyTagSet = new HashMap<>(0);
	private static final Tag tag_contents_def = new Tag(new Color4d[]{ColourInput.BLUE}, new double[]{0.5d}, true);
}
