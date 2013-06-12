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
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.OverlayStringProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringChoiceInput;
import com.sandwell.JavaSimulation.StringListInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.OverlayTextLabel;
import com.sandwell.JavaSimulation3D.TextLabel;

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

	@Keyword(description = "A set of { x, y, z } numbers that define the offset in each direction of the drop shadow from the TextLabel.",
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
		fontName = new StringChoiceInput("FontName", "Fixed Text", defFont);
		fontName.setChoices(validFontNames);
		this.addInput(fontName, true);

		fontColor = new ColourInput("FontColour", "Fixed Text", ColourInput.BLACK);
		this.addInput(fontColor, true, "FontColor");

		fontStyle = new StringListInput("FontStyle", "Fixed Text", new StringVector());
		fontStyle.setValidOptions(validStyles);
		fontStyle.setCaseSensitive(false);
		this.addInput(fontStyle, true);

		dropShadow = new BooleanInput( "DropShadow", "Fixed Text", false );
		this.addInput( dropShadow, true );

		dropShadowColor = new ColourInput("DropShadowColour", "Fixed Text", ColourInput.BLACK);
		this.addInput(dropShadowColor, true, "DropShadowColor");

		dropShadowOffset = new Vec3dInput("DropShadowOffset", "Fixed Text", new Vec3d(-0.1,-0.1,0.0));
		this.addInput(dropShadowOffset, true);

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
		if (ent instanceof TextLabel) {
			return new Binding(ent, this);
		} else if (ent instanceof OverlayTextLabel){
			return new OverlayBinding(ent, this);
		}
		assert(false);
		return null;
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return (ent instanceof TextLabel) || (ent instanceof OverlayTextLabel);
	}

	private class Binding extends DisplayModelBinding {

		private TextLabel labelObservee;
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker modelTracker;

		private StringProxy cachedProxy = null;
		private StringProxy cachedShadow = null;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (TextLabel)ent;
				if (labelObservee != null) {
					observeeTracker = labelObservee.getGraphicsChangeTracker();
					modelTracker = dm.getGraphicsChangeTracker();
				}
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

			if (!modelTracker.checkAndClear() && !observeeTracker.checkAndClear() &&
			    cachedProxy != null) {
				// Nothing changed
				if (cachedShadow != null) {
					out.add(cachedShadow);
				}
				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("TextLabel");

			TessFontKey fontKey = new TessFontKey(fontName.getChoice(), style);
			double height = labelObservee.getTextHeight();
			String text = labelObservee.getCachedText();
			Color4d textColour = fontColor.getValue();

			Vec3d textSize = RenderManager.inst().getRenderedStringSize(fontKey, height, text);
			Transform trans = labelObservee.getGlobalTransForSize(textSize, simTime);

			if (trans == null) {
				return;
			}

			double zBump = textSize.y * 0.01;
			trans.getTransRef().z += zBump;

			cachedProxy = new StringProxy(text, fontKey, textColour, trans, height, getVisibilityInfo(), labelObservee.getEntityNumber());

			if (dropShadow.getValue()) {
				Transform dsTrans = new Transform(trans);
				Vec3d shadowTrans = new Vec3d(dropShadowOffset.getValue());
				shadowTrans.z -= zBump;
				shadowTrans.scale3(height);
				shadowTrans.add3(dsTrans.getTransRef());
				dsTrans.setTrans(shadowTrans);

				Color4d dsColor = dropShadowColor.getValue();

				cachedShadow = new StringProxy(text, fontKey, dsColor, dsTrans, height, getVisibilityInfo(), labelObservee.getEntityNumber());
				out.add(cachedShadow);
			} else {
				cachedShadow = null;
			}

			out.add( cachedProxy );
		}
	}

	private class OverlayBinding extends DisplayModelBinding {

		private OverlayTextLabel labelObservee;
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker modelTracker;

		private OverlayStringProxy cachedProxy = null;
		private OverlayStringProxy cachedShadow = null;


		public OverlayBinding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				labelObservee = (OverlayTextLabel)ent;
				if (labelObservee != null) {
					observeeTracker = labelObservee.getGraphicsChangeTracker();
					modelTracker = dm.getGraphicsChangeTracker();
				}
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

			if (!modelTracker.checkAndClear() &&
			    !observeeTracker.checkAndClear() &&
			    cachedProxy != null) {
				// Nothing changed

				if (cachedShadow != null) {
					out.add(cachedShadow);
				}
				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("OverlayText");

			String text = labelObservee.getCachedText();

			Color4d color = fontColor.getValue();
			IntegerVector pos = labelObservee.screenPosition();
			int height = labelObservee.getTextHeight();

			boolean alignRight = labelObservee.alignRight();
			boolean alignBottom = labelObservee.alignBottom();

			TessFontKey fk = new TessFontKey(fontName.getChoice(), style);

			if (dropShadow.getValue()) {
				Color4d dsColor = dropShadowColor.getValue();

				Vec3d dsOffset = new Vec3d(dropShadowOffset.getValue());
				dsOffset.scale3(height);

				cachedShadow = new OverlayStringProxy(text, fk, dsColor, height,
				                                      pos.get(0) + (dsOffset.x * (alignRight ? -1 : 1)),
				                                      pos.get(1) - (dsOffset.y * (alignBottom ? -1 : 1)),
				                                      alignRight, alignBottom, getVisibilityInfo());
				out.add(cachedShadow);
			} else {
				cachedShadow = null;
			}

			cachedProxy = new OverlayStringProxy(text, fk, color, height, pos.get(0), pos.get(1),
			                                     alignRight, alignBottom, getVisibilityInfo());
			out.add(cachedProxy);
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}
	}

}
