/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2021 JaamSim Software Inc.
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

import com.jaamsim.Graphics.BillboardText;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayText;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Graphics.TextEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringChoiceInput;
import com.jaamsim.input.StringListInput;
import com.jaamsim.input.ValueInput;
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

public class TextModel extends DisplayModel implements TextEntity {

	@Keyword(description = "The font to be used for the text.",
	         exampleList = { "Arial" })
	private final StringChoiceInput fontName;

	@Keyword(description = "The height of the text as displayed in the view window.",
	         exampleList = {"15 m"})
	protected final ValueInput textHeight;

	@Keyword(description = "The height of the text in pixels, used by billboard text and "
	                     + "overlay text.",
	         exampleList = {"15"})
	protected final IntegerInput textHeightInPixels;

	@Keyword(description = "The font styles to be applied to the text, e.g. Bold, Italic. ",
	         exampleList = { "Bold" })
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the text.",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput fontColor;

	@Keyword(description = "If TRUE, then a drop shadow appears for the text.",
	         exampleList = { "TRUE" })
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour for the drop shadow",
	         exampleList = { "red", "skyblue", "135 206 235" })
	private final ColourInput dropShadowColor;

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

		textHeight = new ValueInput("TextHeight", KEY_INPUTS, 0.3d);
		textHeight.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		textHeight.setUnitType(DistanceUnit.class);
		this.addInput(textHeight);

		textHeightInPixels = new IntegerInput("TextHeightInPixels", KEY_INPUTS, 10);
		textHeightInPixels.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(textHeightInPixels);

		fontColor = new ColourInput("FontColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", KEY_INPUTS, new ArrayList<String>(0));
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		this.addInput(fontStyle);

		dropShadow = new BooleanInput("DropShadow", KEY_INPUTS, false);
		this.addInput(dropShadow);

		dropShadowColor = new ColourInput("DropShadowColour", KEY_INPUTS, ColourInput.BLACK);
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", KEY_INPUTS, new Vec3d(-0.1d, -0.1d, -0.001d));
		this.addInput(dropShadowOffset);

		style = Font.PLAIN;
	}

	@Override
	public void updateForInput(Input<?> in) {
		super.updateForInput(in);

		if (in == fontStyle) {
			style = getStyle(fontStyle.getValue());
			return;
		}

		if (in == textHeight) {
			for (TextBasics text : getJaamSimModel().getClonesOfIterator(EntityLabel.class)) {
				if (text.getDisplayModelList().get(0) == this) {
					text.resizeForText();
				}
			}
			return;
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
	public Color4d getFontColor() {
		return fontColor.getValue();
	}

	@Override
	public double getTextHeight() {
		return textHeight.getValue();
	}

	public int getTextHeightInPixels() {
		return textHeightInPixels.getValue();
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
	public boolean getDropShadow() {
		return dropShadow.getValue();
	}

	@Override
	public Color4d getDropShadowColor() {
		return dropShadowColor.getValue();
	}

	@Override
	public Vec3d getDropShadowOffset() {
		return dropShadowOffset.getValue();
	}

	// ********************************************************************************************
	// Binding
	// ********************************************************************************************
	private static class Binding extends DisplayModelBinding {

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
			if (labelObservee == null || !labelObservee.getShow()) {
				return;
			}

			String text = labelObservee.getCachedText();
			double height = labelObservee.getTextHeight();
			Color4d color = labelObservee.getFontColor();
			TessFontKey fk = labelObservee.getTessFontKey();
			Vec3d textSize = RenderManager.inst().getRenderedStringSize(fk, height, text);
			Transform trans = labelObservee.getGlobalTransForSize(textSize);
			boolean ds = labelObservee.getDropShadow();
			Color4d dsColor = labelObservee.getDropShadowColor();
			Vec3d dsOffset = labelObservee.getDropShadowOffset();
			boolean editMode = labelObservee.isEditMode();
			int insertPos = labelObservee.getInsertPosition();
			int numSelected = labelObservee.getNumberSelected();

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

			// If the text is being edited, show the selection and the text insertion mark
			if (editMode) {
				double length = RenderManager.inst().getRenderedStringLength(fk, height, text);
				double ycoord = 0.5*height*1.5d;
				double zcoord = 0.01*height;

				// Highlight the selected text
				if (numSelected != 0) {
					int startPos = Math.min(insertPos, insertPos + numSelected);
					int endPos = Math.max(insertPos, insertPos + numSelected);

					// Calculate the position of the selected text in metres relative to the centre of the string
					double start = RenderManager.inst().getOffsetForStringPosition(fk, height, text, startPos) - 0.5d*length;
					double end = RenderManager.inst().getOffsetForStringPosition(fk, height, text, endPos) - 0.5d*length;

					ArrayList<Vec4d> rect = new ArrayList<>();
					rect.add(new Vec4d( start,  ycoord, -zcoord, 1.0d ));
					rect.add(new Vec4d( start, -ycoord, -zcoord, 1.0d ));
					rect.add(new Vec4d(   end, -ycoord, -zcoord, 1.0d ));
					rect.add(new Vec4d(   end,  ycoord, -zcoord, 1.0d ));
					Vec3d scale = new Vec3d(1.0d, 1.0d, 1.0d);
					cachedProxies.add(new PolygonProxy(rect, trans, scale,
							ColourInput.LIGHT_GREY, false, 1, vi, labelObservee.getEntityNumber()));
				}

				// Show the text insertion mark
				double insert = RenderManager.inst().getOffsetForStringPosition(fk, height, text, insertPos) - 0.5d*length;
				ArrayList<Vec4d> points = new ArrayList<>();
				points.add(new Vec4d( insert, -ycoord, zcoord, 1.0d ));
				points.add(new Vec4d( insert,  ycoord, zcoord, 1.0d ));
				RenderUtils.transformPointsLocal(trans, points, 0);
				cachedProxies.add(new LineProxy(points, ColourInput.BLACK, 1, vi, labelObservee.getEntityNumber()));
			}

			// Show the text
			cachedProxies.add(new StringProxy(text, fk, color, trans, height, vi, labelObservee.getEntityNumber()));

			// Show the drop shadow
			if (ds) {
				Transform dsTrans = new Transform(trans);
				Vec3d shadowTrans = new Vec3d(dsOffset);
				shadowTrans.scale3(height);
				shadowTrans.add3(dsTrans.getTransRef());
				dsTrans.setTrans(shadowTrans);

				cachedProxies.add(new StringProxy(text, fk, dsColor, dsTrans, height, vi, labelObservee.getEntityNumber()));
			}

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
			if (labelObservee == null || !labelObservee.getShow()) {
				return;
			}

			String text = labelObservee.getCachedText();
			IntegerVector pos = labelObservee.getScreenPosition();
			int height = (int) labelObservee.getTextHeight();
			boolean alignRight = labelObservee.getAlignRight();
			boolean alignBottom = labelObservee.getAlignBottom();
			Color4d color = labelObservee.getFontColor();
			TessFontKey fk = labelObservee.getTessFontKey();
			boolean ds = labelObservee.getDropShadow();
			Color4d dsColor = labelObservee.getDropShadowColor();
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
				double length = RenderManager.inst().getRenderedStringLength(fk, height, text);
				double margin = 0.25d*height;
				double textStart = alignRight ? pos.get(0) + length : pos.get(0);
				double top = pos.get(1) - margin;
				double bottom = pos.get(1) + height + margin;

				// Highlight the selected text
				if (numSelected != 0) {
					int startPos = Math.min(insertPos, insertPos + numSelected);
					int endPos = Math.max(insertPos, insertPos + numSelected);

					// Calculate the position of the selected text in pixels relative to the start of the string
					double startOffset = RenderManager.inst().getOffsetForStringPosition(fk, height, text, startPos);
					double endOffset = RenderManager.inst().getOffsetForStringPosition(fk, height, text, endPos);
					double start = textStart + startOffset * (alignRight ? -1.0d : 1.0d);
					double end = textStart + endOffset * (alignRight ? -1.0d : 1.0d);

					ArrayList<Vec2d> rect = new ArrayList<>(4);
					rect.add(new Vec2d( start, bottom ));
					rect.add(new Vec2d( start, top ));
					rect.add(new Vec2d(   end, top ));
					rect.add(new Vec2d(   end, bottom ));
					cachedProxies.add(new OverlayPolygonProxy(rect, ColourInput.LIGHT_GREY,
							!alignBottom, alignRight, vi, labelObservee.getEntityNumber()));
				}

				// Show the text insertion mark
				double insertOffset = RenderManager.inst().getOffsetForStringPosition(fk, height, text, insertPos);
				double insert = textStart + insertOffset * (alignRight ? -1.0d : 1.0d);
				ArrayList<Vec2d> points = new ArrayList<>(2);
				points.add(new Vec2d( insert, top ));
				points.add(new Vec2d( insert, bottom ));
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

			double length = RenderManager.inst().getRenderedStringLength(fkCache, heightCache, textCache);
			double margin = 0.5d*heightCache;
			double start = alignRightCache ? posCache.get(0) + length + margin
					: posCache.get(0) - margin;
			double end = alignRightCache ? posCache.get(0) - margin
					: posCache.get(0) + length + margin;
			double top = posCache.get(1) - margin;
			double bottom = posCache.get(1) + heightCache + margin;

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
			if (labelObservee == null || !labelObservee.getShow()) {
				return;
			}

			String text = labelObservee.getCachedText();
			int height = (int)labelObservee.getTextHeight();
			Color4d color = labelObservee.getFontColor();
			TessFontKey fk = labelObservee.getTessFontKey();
			boolean ds = labelObservee.getDropShadow();
			Color4d dsColor = labelObservee.getDropShadowColor();
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
