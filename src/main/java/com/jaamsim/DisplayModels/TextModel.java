/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.ColourProviders.ColourProvInput;
import com.jaamsim.Graphics.BillboardText;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayText;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Graphics.TextEntity;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.BillboardStringProxy;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.OverlayLineProxy;
import com.jaamsim.render.OverlayPolygonProxy;
import com.jaamsim.render.OverlayStringProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.render.VisibilityInfo;
import com.jaamsim.units.DistanceUnit;

public class TextModel extends AbstractShapeModel implements TextEntity {

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The height of the text as displayed in the view window.",
	         exampleList = {"15 m"})
	protected final SampleInput textHeight;

	@Keyword(description = "The height of the text in pixels, used by billboard text and "
	                     + "overlay text.",
	         exampleList = {"15"})
	protected final SampleInput textHeightInPixels;

	@Keyword(description = "The font styles to be applied to the text, e.g. Bold, Italic. ",
	         exampleList = { "Bold" })
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the text.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourProvInput fontColor;

	@Keyword(description = "If TRUE, then a drop shadow appears for the text.",
	         exampleList = { "TRUE" })
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour for the drop shadow",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourProvInput dropShadowColor;

	@Keyword(description = "The { x, y, z } coordinates of the drop shadow's offset, expressed "
	                     + "as a decimal fraction of the text height.",
	         exampleList = { "0.1 -0.1 0.001" })
	private final Vec3dInput dropShadowOffset;

	private int style; // Font Style

	private static final int defFont;
	public static final ArrayList<String> validFontNames;
	public static final ArrayList<String> validStyles;

	static {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[ ] fontNames = ge.getAvailableFontFamilyNames();
		Arrays.sort(fontNames);
		validFontNames = new ArrayList<>(Arrays.asList(fontNames));
		int def = validFontNames.indexOf("Verdana");
		if (def > -1)
			defFont = def;
		else
			defFont = 0;

		validStyles = new ArrayList<>();
		validStyles.add("BOLD");
		validStyles.add("ITALIC");
	}

	{
		fontName = new StringChoiceInput("FontName", KEY_INPUTS, defFont);
		fontName.setChoices(validFontNames);
		this.addInput(fontName);

		textHeight = new SampleInput("TextHeight", KEY_INPUTS, 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		textHeight.setCallback(textheightCallback);
		this.addInput(textHeight);

		textHeightInPixels = new SampleInput("TextHeightInPixels", KEY_INPUTS, 10);
		textHeightInPixels.setValidRange(0, Double.POSITIVE_INFINITY);
		textHeightInPixels.setIntegerValue(true);
		this.addInput(textHeightInPixels);

		fontColor = new ColourProvInput("FontColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", KEY_INPUTS, new ArrayList<String>(0));
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		fontStyle.setCallback(fontstyleCallback);
		this.addInput(fontStyle);

		dropShadow = new BooleanInput("DropShadow", KEY_INPUTS, false);
		this.addInput(dropShadow);

		dropShadowColor = new ColourProvInput("DropShadowColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", KEY_INPUTS, new Vec3d(-0.1d, -0.1d, -0.001d));
		this.addInput(dropShadowOffset);

		style = Font.PLAIN;
	}

	static final InputCallback fontstyleCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((TextModel)ent).updatefontstyle();
		}
	};

	void updatefontstyle() {
		style = getStyle(fontStyle.getValue());
	}

	static final InputCallback textheightCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((TextModel)ent).updatetextheight();
		}
	};

	void updatetextheight() {
		for (TextBasics text : getJaamSimModel().getClonesOfIterator(EntityLabel.class)) {
			if (text.getDisplayModelList().get(0) == this) {
				text.resizeForText();
			}
		}
	}

	public static int getStyle(ArrayList<String> strArray) {
		int ret = Font.PLAIN;
		for(String each: strArray ) {
			if(each.equalsIgnoreCase("Bold") ) {
				ret += Font.BOLD;
			}
			else if (each.equalsIgnoreCase("Italic")) {
				ret += Font.ITALIC;
			}
		}
		return ret;
	}

	public static boolean isBold(int style) {
		return (style & Font.BOLD) != 0;
	}

	public static boolean isItalic(int style) {
		return (style & Font.ITALIC) != 0;
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		if (ent instanceof BillboardText) {
			return new BillboardBinding(ent, this);
		} else if (ent instanceof TextBasics) {
			return new Binding(ent, this);
		} else if (ent instanceof OverlayText){
			return new OverlayBinding(ent, this);
		}
		assert(false);
		return null;
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof TextBasics) || (ent instanceof OverlayText);
	}

	public TessFontKey getTessFontKey() {
		return new TessFontKey(fontName.getChoice(), style);
	}

	public static TessFontKey getDefaultTessFontKey() {
		return new TessFontKey("Verdana", Font.PLAIN);
	}

	@Override
	public Color4d getFontColor(double simTime) {
		return fontColor.getNextColour(TextModel.this, simTime);
	}

	@Override
	public double getTextHeight(double simTime) {
		return textHeight.getNextSample(this, simTime);
	}

	public int getTextHeightInPixels(double simTime) {
		return (int) textHeightInPixels.getNextSample(this, simTime);
	}

	@Override
	public String getTextHeightString() {
		if (textHeight.isDefault())
			return textHeight.getDefaultString(getJaamSimModel());
		return textHeight.getValueString();
	}

	public String getTextHeightInPixelsString() {
		if (textHeightInPixels.isDefault())
			return textHeightInPixels.getDefaultString(getJaamSimModel());
		return textHeightInPixels.getValueString();
	}

	@Override
	public String getFontName() {
		return fontName.getChoice();
	}

	@Override
	public int getStyle() {
		return getStyle(fontStyle.getValue());
	}

	@Override
	public boolean isBold() {
		return isBold(getStyle());
	}

	@Override
	public boolean isItalic() {
		return isItalic(getStyle());
	}

	@Override
	public boolean isDropShadow(double simTime) {
		return dropShadow.getValue();
	}

	@Override
	public Color4d getDropShadowColor(double simTime) {
		return dropShadowColor.getNextColour(TextModel.this, simTime);
	}

	@Override
	public Vec3d getDropShadowOffset() {
		return dropShadowOffset.getValue();
	}

	// ********************************************************************************************
	// Binding
	// ********************************************************************************************
	private class Binding extends DisplayModelBinding {

		private TextBasics labelObservee;

		private String textCache;
		private Transform transCache;

		private Color4d colorCache;
		private double heightCache;

		private TessFontKey fkCache;

		private boolean dropShadowCache;
		private Vec3d dsOffsetCache;
		private Color4d dsColorCache;

		private boolean editModeCache;
		private int insertPosCache;
		private int numSelectedCache;

		private VisibilityInfo viCache;

		private Vec3d scaleCache;
		private boolean filledCache;
		private boolean outlinedCache;
		private Color4d fillColorCache;
		private Color4d outlineColorCache;
		private int outlineWidthCache;

		private ArrayList<RenderProxy> cachedProxies = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (TextBasics)ent;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				labelObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (labelObservee == null || !labelObservee.getShow(simTime)) {
				return;
			}

			String text = labelObservee.getCachedText();
			double height = labelObservee.getTextHeight(simTime);
			Color4d color = labelObservee.getFontColor(simTime);
			TessFontKey fk = labelObservee.getTessFontKey();
			Vec3d textSize = RenderManager.inst().getRenderedStringSize(fk, height, text);
			Transform trans = labelObservee.getGlobalTransForSize(textSize);
			boolean ds = labelObservee.isDropShadow(simTime);
			Color4d dsColor = labelObservee.getDropShadowColor(simTime);
			Vec3d dsOffset = labelObservee.getDropShadowOffset();
			boolean editMode = labelObservee.isEditMode();
			int insertPos = labelObservee.getInsertPosition();
			int numSelected = labelObservee.getNumberSelected();
			Vec3d scale = labelObservee.getSize();
			scale.mul3(getModelScale());
			boolean filled = labelObservee.isFilled(simTime);
			boolean outlined = labelObservee.isOutlined(simTime);
			Color4d fillColor = labelObservee.getFillColour(simTime);
			Color4d outlineColor = labelObservee.getLineColour(simTime);
			int outlineWidth = labelObservee.getLineWidth(simTime);

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(textCache, text);
			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_col4d(colorCache, color);
			dirty = dirty || heightCache != height;
			dirty = dirty || !compare(fkCache, fk);
			dirty = dirty || dropShadowCache != ds;
			dirty = dirty || dirty_col4d(dsColorCache, dsColor);
			dirty = dirty || dirty_vec3d(dsOffsetCache, dsOffset);
			dirty = dirty || editMode != editModeCache;
			dirty = dirty || insertPos != insertPosCache;
			dirty = dirty || numSelected != numSelectedCache;
			dirty = dirty || !compare(viCache, vi);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || filledCache != filled;
			dirty = dirty || outlinedCache != outlined;
			dirty = dirty || dirty_col4d(fillColorCache, fillColor);
			dirty = dirty || dirty_col4d(outlineColorCache, outlineColor);
			dirty = dirty || outlineWidthCache != outlineWidth;

			textCache = text;
			transCache = trans;
			colorCache = color;
			heightCache = height;
			fkCache = fk;
			dropShadowCache = ds;
			dsColorCache = dsColor;
			dsOffsetCache = dsOffset;
			editModeCache = editMode;
			insertPosCache = insertPos;
			numSelectedCache = numSelected;
			viCache = vi;
			scaleCache = scale;
			filledCache = filled;
			outlinedCache = outlined;
			fillColorCache = fillColor;
			outlineColorCache = outlineColor;
			outlineWidthCache = outlineWidth;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				out.addAll(cachedProxies);
				registerCacheHit("TextLabel");
				return;
			}

			registerCacheMiss("TextLabel");

			if (trans == null) {
				return;
			}

			cachedProxies = new ArrayList<>();

			// Show the background rectangle
			if (filled || outlined) {
				double zcoord = 0.02*height;
				ArrayList<Vec4d> backgroundRect = new ArrayList<>();
				backgroundRect.add(new Vec4d( 0.5d,  0.5d, -zcoord, 1.0d ));
				backgroundRect.add(new Vec4d(-0.5d,  0.5d, -zcoord, 1.0d ));
				backgroundRect.add(new Vec4d(-0.5d, -0.5d, -zcoord, 1.0d ));
				backgroundRect.add(new Vec4d( 0.5d, -0.5d, -zcoord, 1.0d ));
				Transform backgroundTrans = labelObservee.getGlobalTrans();
				if (filled) {
					cachedProxies.add(new PolygonProxy(backgroundRect, backgroundTrans, scale,
							fillColor, false, 1, vi, labelObservee.getEntityNumber()));
				}
				if (outlined) {
					cachedProxies.add(new PolygonProxy(backgroundRect, backgroundTrans, scale,
							outlineColor, true, outlineWidth, vi, labelObservee.getEntityNumber()));
				}
			}

			// If the text is being edited, show the selection and the text insertion mark
			if (editMode) {
				Vec3d size = RenderManager.inst().getRenderedStringSize(fk, height, text);
				double zcoord = 0.01*height;

				// Highlight the selected text
				if (numSelected != 0) {
					int startPos = Math.min(insertPos, insertPos + numSelected);
					int endPos = Math.max(insertPos, insertPos + numSelected);
					int startSel = startPos;
					for (int i = startPos; i <= endPos; i++) {
						if (i < endPos && text.charAt(i) != '\n')
							continue;
						Vec3d start = RenderManager.inst().getOffsetForStringPosition(fk, height, text, startSel);
						Vec3d end = RenderManager.inst().getOffsetForStringPosition(fk, height, text, i);
						start.x -= 0.5d * size.x;
						start.y += 0.5d * size.y;
						end.x -= 0.5d * size.x;
						double top = start.y + 0.25d*height;
						double bottom = start.y - 1.25d*height;
						ArrayList<Vec4d> rect = new ArrayList<>();
						rect.add(new Vec4d( start.x, top,    -zcoord, 1.0d ));
						rect.add(new Vec4d( start.x, bottom, -zcoord, 1.0d ));
						rect.add(new Vec4d( end.x,   bottom, -zcoord, 1.0d ));
						rect.add(new Vec4d( end.x,   top,    -zcoord, 1.0d ));
						cachedProxies.add(new PolygonProxy(rect, trans, DisplayModel.ONES,
								ColourInput.LIGHT_GREY, false, 1, vi, labelObservee.getEntityNumber()));
						startSel = i + 1;
					}
				}

				// Show the text insertion mark
				Vec3d offset = RenderManager.inst().getOffsetForStringPosition(fk, height, text, insertPos);
				offset.x -= 0.5d * size.x;
				offset.y += 0.5d * size.y;
				ArrayList<Vec4d> points = new ArrayList<>();
				points.add(new Vec4d( offset.x, offset.y - 1.25d*height, zcoord, 1.0d ));
				points.add(new Vec4d( offset.x, offset.y + 0.25d*height, zcoord, 1.0d ));
				RenderUtils.transformPointsLocal(trans, points, 0);
				cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, vi, labelObservee.getEntityNumber()));
			}

			// Show the drop shadow
			if (ds) {
				Transform dsTrans = new Transform(trans);
				Vec3d shadowTrans = new Vec3d(dsOffset);
				shadowTrans.scale3(height);
				shadowTrans.add3(dsTrans.getTransRef());
				dsTrans.setTrans(shadowTrans);

				cachedProxies.add(new StringProxy(text, fk, dsColor, dsTrans, height, vi, labelObservee.getEntityNumber()));
			}

			// Show the text
			cachedProxies.add(new StringProxy(text, fk, color, trans, height, vi, labelObservee.getEntityNumber()));

			out.addAll(cachedProxies);
		}
	}

	// ********************************************************************************************
	// OverlayBinding
	// ********************************************************************************************
	private static class OverlayBinding extends DisplayModelBinding {

		private OverlayText labelObservee;

		private String textCache;

		private Color4d colorCache;
		private IntegerVector posCache;
		private int heightCache;

		private boolean alignRightCache;
		private boolean alignBottomCache;

		private TessFontKey fkCache;

		private boolean dropShadowCache;
		private Vec3d dsOffsetCache;
		private Color4d dsColorCache;

		private boolean editModeCache;
		private int insertPosCache;
		private int numSelectedCache;

		private VisibilityInfo viCache;

		private ArrayList<RenderProxy> cachedProxies = null;


		public OverlayBinding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (OverlayText)ent;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				labelObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (labelObservee == null || !labelObservee.getShow(simTime)) {
				return;
			}

			String text = labelObservee.getCachedText();
			IntegerVector pos = labelObservee.getScreenPosition();
			int height = (int) labelObservee.getTextHeight(simTime);
			boolean alignRight = labelObservee.getAlignRight();
			boolean alignBottom = labelObservee.getAlignBottom();
			Color4d color = labelObservee.getFontColor(simTime);
			TessFontKey fk = labelObservee.getTessFontKey();
			boolean ds = labelObservee.isDropShadow(simTime);
			Color4d dsColor = labelObservee.getDropShadowColor(simTime);
			Vec3d dsOffset = labelObservee.getDropShadowOffset();
			boolean editMode = labelObservee.isEditMode();
			int insertPos = labelObservee.getInsertPosition();
			int numSelected = labelObservee.getNumberSelected();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(textCache, text);
			dirty = dirty || dirty_col4d(colorCache, color);
			dirty = dirty || !compare(posCache, pos);
			dirty = dirty || heightCache != height;
			dirty = dirty || alignRightCache != alignRight;
			dirty = dirty || alignBottomCache != alignBottom;
			dirty = dirty || !compare(fkCache, fk);
			dirty = dirty || dropShadowCache != ds;
			dirty = dirty || dirty_col4d(dsColorCache, dsColor);
			dirty = dirty || dirty_vec3d(dsOffsetCache, dsOffset);
			dirty = dirty || editMode != editModeCache;
			dirty = dirty || insertPos != insertPosCache;
			dirty = dirty || numSelected != numSelectedCache;
			dirty = dirty || !compare(viCache, vi);

			textCache = text;
			colorCache = color;
			posCache = pos;
			heightCache = height;
			alignRightCache = alignRight;
			alignBottomCache = alignBottom;
			fkCache = fk;
			dropShadowCache = ds;
			dsColorCache = dsColor;
			dsOffsetCache = dsOffset;
			editModeCache = editMode;
			insertPosCache = insertPos;
			numSelectedCache = numSelected;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed

				out.addAll(cachedProxies);
				registerCacheHit("OverlayText");
				return;
			}

			registerCacheMiss("OverlayText");

			cachedProxies = new ArrayList<>();

			// If the text is being edited, show the selection and the text insertion mark
			if (editMode) {
				Vec3d size = RenderManager.inst().getRenderedStringSize(fk, height, text);
				double textStartX = alignRight ? pos.get(0) + size.x : pos.get(0);
				double textStartY = alignBottom ? pos.get(1) + size.y - height : pos.get(1);

				// Highlight the selected text
				if (numSelected != 0) {
					int startPos = Math.min(insertPos, insertPos + numSelected);
					int endPos = Math.max(insertPos, insertPos + numSelected);
					int startSel = startPos;
					for (int i = startPos; i <= endPos; i++) {
						if (i < endPos && text.charAt(i) != '\n')
							continue;
						Vec3d start = RenderManager.inst().getOffsetForStringPosition(fk, height, text, startSel);
						Vec3d end = RenderManager.inst().getOffsetForStringPosition(fk, height, text, i);
						double startX = textStartX + start.x * (alignRight ? -1.0d : 1.0d);
						double startY = textStartY - start.y * (alignBottom ? -1.0d : 1.0d);
						double endX = textStartX + end.x * (alignRight ? -1.0d : 1.0d);
						double top = startY - 0.25d*height;
						double bottom = startY + 1.25d*height;
						ArrayList<Vec2d> rect = new ArrayList<>(4);
						rect.add(new Vec2d( startX, bottom ));
						rect.add(new Vec2d( startX, top ));
						rect.add(new Vec2d(   endX, top ));
						rect.add(new Vec2d(   endX, bottom ));
						cachedProxies.add(new OverlayPolygonProxy(rect, ColourInput.LIGHT_GREY,
								!alignBottom, alignRight, vi, labelObservee.getEntityNumber()));
						startSel = i + 1;
					}
				}

				// Show the text insertion mark
				Vec3d offset = RenderManager.inst().getOffsetForStringPosition(fk, height, text, insertPos);
				double insertX = textStartX + offset.x * (alignRight ? -1.0d : 1.0d);
				double insertY = textStartY - offset.y * (alignBottom ? -1.0d : 1.0d);
				ArrayList<Vec2d> points = new ArrayList<>(2);
				points.add(new Vec2d( insertX, insertY + 1.25d*height ));
				points.add(new Vec2d( insertX, insertY - 0.25d*height ));
				cachedProxies.add(new OverlayLineProxy(points, ColourInput.BLACK,
						!alignBottom, alignRight, 1, vi, labelObservee.getEntityNumber()));
			}

			// Show the drop shadow
			if (ds) {
				dsOffset = new Vec3d(dsOffset);
				dsOffset.scale3(height);
				cachedProxies.add(new OverlayStringProxy(text, fk, dsColor, height,
				                                      pos.get(0) + (dsOffset.x * (alignRight ? -1 : 1)),
				                                      pos.get(1) - (dsOffset.y * (alignBottom ? -1 : 1)),
				                                      alignRight, alignBottom, vi, labelObservee.getEntityNumber()));
			}

			// Show the text
			cachedProxies.add(new OverlayStringProxy(text, fk, color, height, pos.get(0), pos.get(1),
			                                     alignRight, alignBottom, vi, labelObservee.getEntityNumber()));

			out.addAll(cachedProxies);
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {

			Vec3d size = RenderManager.inst().getRenderedStringSize(fkCache, heightCache, textCache);
			double margin = 0.5d*heightCache;
			double start = alignRightCache ? posCache.get(0) + size.x + margin
					: posCache.get(0) - margin;
			double end = alignRightCache ? posCache.get(0) - margin
					: posCache.get(0) + size.x + margin;
			double top = posCache.get(1) - margin;
			double bottom = posCache.get(1) + size.y + margin;

			ArrayList<Vec2d> rect = new ArrayList<>(8);
			rect.add(new Vec2d( start, bottom ));
			rect.add(new Vec2d(   end, bottom ));
			rect.add(new Vec2d(   end, bottom ));
			rect.add(new Vec2d(   end, top    ));
			rect.add(new Vec2d(   end, top    ));
			rect.add(new Vec2d( start, top    ));
			rect.add(new Vec2d( start, top    ));
			rect.add(new Vec2d( start, bottom ));

			OverlayLineProxy outline = new OverlayLineProxy(rect, ColourInput.GREEN,
					!alignBottomCache, alignRightCache, 1, viCache, labelObservee.getEntityNumber());
			out.add(outline);
		}
	}

	// ********************************************************************************************
	// BillboardBinding
	// ********************************************************************************************
	private static class BillboardBinding extends DisplayModelBinding {

		private BillboardText labelObservee;

		private String textCache;

		private Color4d colorCache;
		private Vec3d posCache;
		private int heightCache;

		private TessFontKey fkCache;

		private boolean dropShadowCache;
		private Vec3d dsOffsetCache;
		private Color4d dsColorCache;

		private VisibilityInfo viCache;

		private ArrayList<RenderProxy> cachedProxies = null;


		public BillboardBinding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (BillboardText)ent;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				labelObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (labelObservee == null || !labelObservee.getShow(simTime)) {
				return;
			}

			String text = labelObservee.getCachedText();
			int height = (int)labelObservee.getTextHeight(simTime);
			Color4d color = labelObservee.getFontColor(simTime);
			TessFontKey fk = labelObservee.getTessFontKey();
			boolean ds = labelObservee.isDropShadow(simTime);
			Color4d dsColor = labelObservee.getDropShadowColor(simTime);
			Vec3d dsOffset = labelObservee.getDropShadowOffset();
			Vec3d pos = labelObservee.getGlobalPosition();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(textCache, text);
			dirty = dirty || dirty_col4d(colorCache, color);
			dirty = dirty || heightCache != height;
			dirty = dirty || dirty_vec3d(posCache, pos);
			dirty = dirty || !compare(fkCache, fk);
			dirty = dirty || dropShadowCache != ds;
			dirty = dirty || dirty_col4d(dsColorCache, dsColor);
			dirty = dirty || dirty_vec3d(dsOffsetCache, dsOffset);
			dirty = dirty || !compare(viCache, vi);

			textCache = text;
			colorCache = color;
			posCache = pos;
			heightCache = height;
			fkCache = fk;
			dropShadowCache = ds;
			dsColorCache = dsColor;
			dsOffsetCache = dsOffset;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed

				out.addAll(cachedProxies);
				registerCacheHit("OverlayText");
				return;
			}

			registerCacheMiss("OverlayText");

			cachedProxies = new ArrayList<>();

			if (ds) {
				dsOffset = new Vec3d(dsOffset);
				dsOffset.scale3(height);
				cachedProxies.add(new BillboardStringProxy(text, fk, dsColor, height, pos, dsOffset.x, dsOffset.y,
				                                           vi, labelObservee.getEntityNumber()));
			}

			cachedProxies.add(new BillboardStringProxy(text, fk, color, height, pos, 0, 0,
			                                           vi, labelObservee.getEntityNumber()));
			out.addAll(cachedProxies);
		}
	}

}
