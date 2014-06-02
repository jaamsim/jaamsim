/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Arrays;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.Input;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.BillboardStringProxy;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.OverlayStringProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.render.VisibilityInfo;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.StringChoiceInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.BillboardText;
import com.sandwell.JavaSimulation3D.OverlayText;
import com.sandwell.JavaSimulation3D.Text;

public class TextModel extends DisplayModel {

	@Keyword(description = "The name of the font to be used for the label. The " +
	                "font name must be enclosed in single quotes.",
	         example = "TitleModel FontName { 'Arial' }")
	private final StringChoiceInput fontName;

	@Keyword(description = "A list of font styles to be applied to the label, e.g. Bold, Italic. ",
	         example = "TitleModel FontStyle { Bold }  ")
	private final StringListInput fontStyle;

	@Keyword(description = "The colour of the font, defined using a colour keyword or RGB values.",
	         example = "TitleModel FontColor { Red }")
	private final ColourInput fontColor;

	@Keyword(description = "A Boolean value.  If TRUE, then a drop shadow appears for the text label.",
	         example = "TitleModel  DropShadow { TRUE }")
	private final BooleanInput dropShadow;

	@Keyword(description = "The colour for the drop shadow, defined using a colour keyword or RGB values.",
	         example = "TitleModel  DropShadowColour { red }")
	private final ColourInput dropShadowColor;

	@Keyword(description = "A set of { x, y, z } numbers that define the offset in each direction of the drop shadow from the Text.",
	         example = "TitleModel  DropShadowOffset { 0.1 0.1 0.0 }")
	private final Vec3dInput dropShadowOffset;

	private int style; // Font Style

	private static final int defFont;
	private static final ArrayList<String> validFontNames;
	private static final ArrayList<String> validStyles;

	static {
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[ ] fontNames = ge.getAvailableFontFamilyNames();
		Arrays.sort(fontNames);
		validFontNames = new ArrayList<String>(Arrays.asList(fontNames));
		int def = validFontNames.indexOf("Verdana");
		if (def > -1)
			defFont = def;
		else
			defFont = 0;

		validStyles = new ArrayList<String>();
		validStyles.add("BOLD");
		validStyles.add("ITALIC");
	}

	{
		fontName = new StringChoiceInput("FontName", "Key Inputs", defFont);
		fontName.setChoices(validFontNames);
		this.addInput(fontName);

		fontColor = new ColourInput("FontColour", "Key Inputs", ColourInput.BLACK);
		this.addInput(fontColor);
		this.addSynonym(fontColor, "FontColor");

		fontStyle = new StringListInput("FontStyle", "Key Inputs", new ArrayList<String>(0));
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		this.addInput(fontStyle);

		dropShadow = new BooleanInput( "DropShadow", "Key Inputs", false );
		this.addInput( dropShadow );

		dropShadowColor = new ColourInput("DropShadowColour", "Key Inputs", ColourInput.BLACK);
		this.addInput(dropShadowColor);
		this.addSynonym(dropShadowColor, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", "Key Inputs", new Vec3d(-0.1d, -0.1d, -0.001d));
		this.addInput(dropShadowOffset);

		style = Font.PLAIN;
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if(in == fontStyle) {
			style = Font.PLAIN;
			for(String each: fontStyle.getValue() ) {
				if(each.equalsIgnoreCase("Bold") ) {
					style += Font.BOLD;
				}
				else if (each.equalsIgnoreCase("Italic")) {
					style += Font.ITALIC;
				}
			}
		}
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		if (ent instanceof BillboardText) {
			return new BillboardBinding(ent, this);
		} else if (ent instanceof Text) {
			return new Binding(ent, this);
		} else if (ent instanceof OverlayText){
			return new OverlayBinding(ent, this);
		}
		assert(false);
		return null;
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof Text) || (ent instanceof OverlayText);
	}

	private class Binding extends DisplayModelBinding {

		private Text labelObservee;

		private String textCache;
		private Transform transCache;

		private Color4d colorCache;
		private double heightCache;

		private TessFontKey fkCache;

		private boolean dropShadowCache;
		private Vec3d dsOffsetCache;
		private Color4d dsColorCache;

		private VisibilityInfo viCache;


		private ArrayList<RenderProxy> cachedProxies = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (Text)ent;
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
			Color4d color = getFontColorForText(text);
			TessFontKey fk = new TessFontKey(fontName.getChoice(), style);

			Vec3d textSize = RenderManager.inst().getRenderedStringSize(fk, height, text);
			Transform trans = labelObservee.getGlobalTransForSize(textSize, simTime);

			boolean ds = dropShadow.getValue();
			Color4d dsColor = dropShadowColor.getValue();
			Vec3d dsOffset = dropShadowOffset.getValue();

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
			dirty = dirty || !compare(viCache, vi);

			textCache = text;
			transCache = trans;
			colorCache = color;
			heightCache = height;
			fkCache = fk;
			dropShadowCache = ds;
			dsColorCache = dsColor;
			dsOffsetCache = dsOffset;
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

			cachedProxies = new ArrayList<RenderProxy>();

			cachedProxies.add(new StringProxy(text, fk, color, trans, height, vi, labelObservee.getEntityNumber()));

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

	private class OverlayBinding extends DisplayModelBinding {

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

			Color4d color = getFontColorForText(text);
			IntegerVector pos = labelObservee.getScreenPosition();
			int height = labelObservee.getTextHeight();

			boolean alignRight = labelObservee.getAlignRight();
			boolean alignBottom = labelObservee.getAlignBottom();

			TessFontKey fk = new TessFontKey(fontName.getChoice(), style);

			boolean ds = dropShadow.getValue();

			Color4d dsColor = dropShadowColor.getValue();

			Vec3d dsOffset = new Vec3d(dropShadowOffset.getValue());
			dsOffset.scale3(height);

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
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed

				out.addAll(cachedProxies);
				registerCacheHit("OverlayText");
				return;
			}

			registerCacheMiss("OverlayText");

			cachedProxies = new ArrayList<RenderProxy>();

			if (ds) {

				cachedProxies.add(new OverlayStringProxy(text, fk, dsColor, height,
				                                      pos.get(0) + (dsOffset.x * (alignRight ? -1 : 1)),
				                                      pos.get(1) - (dsOffset.y * (alignBottom ? -1 : 1)),
				                                      alignRight, alignBottom, vi));
			}

			cachedProxies.add(new OverlayStringProxy(text, fk, color, height, pos.get(0), pos.get(1),
			                                     alignRight, alignBottom, vi));

			out.addAll(cachedProxies);
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}
	}

	private class BillboardBinding extends DisplayModelBinding {

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

			Color4d color = getFontColorForText(text);
			int height = (int)labelObservee.getTextHeight();

			TessFontKey fk = new TessFontKey(fontName.getChoice(), style);

			boolean ds = dropShadow.getValue();

			Color4d dsColor = dropShadowColor.getValue();

			Vec3d dsOffset = new Vec3d(dropShadowOffset.getValue());
			dsOffset.scale3(height);

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

			cachedProxies = new ArrayList<RenderProxy>();

			if (ds) {
				cachedProxies.add(new BillboardStringProxy(text, fk, dsColor, height, pos, dsOffset.x, dsOffset.y, vi));
			}

			cachedProxies.add(new BillboardStringProxy(text, fk, color, height, pos, 0, 0, vi));

			out.addAll(cachedProxies);
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}
	}

	public TessFontKey getTessFontKey() {
		return new TessFontKey(fontName.getChoice(), style);
	}

	public static TessFontKey getDefaultTessFontKey() {
		return new TessFontKey("Verdana", Font.PLAIN);
	}

	public Color4d getFontColorForText(String text) {
		return fontColor.getValue();
	}

	public Color4d getFontColor() {
		return fontColor.getValue();
	}

}
