/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.ui;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.KeyedVec3dInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.units.DistanceUnit;

public class View extends Entity {
	private static final ArrayList<View> allInstances;

	public static final int OMNI_VIEW_ID = -1;
	public static final int NO_VIEW_ID = 0;

	private final int viewID;

	private boolean keepWindowOpen;  // used by GUIFrame to determine whether a window is open or closed

	@Keyword(description = "The region in which the view's coordinates are given.",
	         exampleList = {"Region1"})
	private final EntityInput<Region> region;

	@Keyword(description = "The position at which the view camera is pointed.",
	         exampleList = {"0 0 0 m"})
	private final Vec3dInput center;

	@Keyword(description = "The position of the view camera.",
	         exampleList = {"0 0 50 m"})
	private final Vec3dInput position;

	@Keyword(description = "The size of the window in pixels (width, height).",
	         exampleList = {"500 300"})
	private final IntegerListInput windowSize;

	@Keyword(description = "The position of the upper left corner of the window in pixels "
	                     + "measured from the top left corner of the screen.",
	         exampleList = {"220 110"})
	private final IntegerListInput windowPos;

	@Keyword(description = "Text to place in the title bar of the view window. The window must "
	                     + "be closed and re-opened manually after changing the title.",
	         exampleList = {"'An Example Title'"})
	private final StringInput titleBar;

	@Keyword(description = "If TRUE, the view window is displayed on screen.",
	         exampleList = {"FALSE"})
	private final BooleanInput showWindow;

	@Keyword(description = "A Boolean indicating whether the view can be panned or rotated.",
	         exampleList = {"FALSE"})
	private final BooleanInput movable;

	@Keyword(description = "A Boolean indicating whether the view is locked to a downward view "
	                     + "(the 2D default).",
	         exampleList = {"FALSE"})
	private final BooleanInput lock2D;

	@Keyword(description = "The (optional) entity for this view to follow. Setting this input "
	                     + "makes the view ignore ViewCenter and interprets ViewPosition as a "
	                     + "relative offset to this entity.",
	         exampleList = {"Ship1"})
	private final EntityInput<DisplayEntity> followEntityInput;

	@Keyword(description = "The (optional) scripted curve for the view position to follow.",
	         exampleList = {"{{ 0 h } { 0 0 0 m }} {{ 100 h } { 100 0 0 m }}"})
	private final KeyedVec3dInput positionScriptInput;

	@Keyword(description = "The (optional) scripted curve for the view center to follow.",
	         exampleList = {"{{ 0 h } { 0 0 0 m }} {{ 100 h } { 100 0 0 m }}"})
	private final KeyedVec3dInput centerScriptInput;

	@Keyword(description = "The image file to use as the background for this view.",
	         exampleList = {"'<res>/images/sky_map_2048x1024.jpg'"})
	private final FileInput skyboxImage;

	private final Object setLock = new Object();

	private double cachedSimTime = 0;

	static {
		allInstances = new ArrayList<>();
	}

	{
		attributeDefinitionList.setHidden(true);
		namedExpressionInput.setHidden(true);

		region = new EntityInput<>(Region.class, "Region", GRAPHICS, null);
		this.addInput(region);

		center = new Vec3dInput("ViewCenter", GRAPHICS, new Vec3d());
		center.setUnitType(DistanceUnit.class);
		center.setPromptReqd(false);
		this.addInput(center);

		position = new Vec3dInput("ViewPosition", GRAPHICS, new Vec3d(5.0d, -5.0d, 5.0d));
		position.setUnitType(DistanceUnit.class);
		position.setPromptReqd(false);
		this.addInput(position);

		IntegerVector defSize = new IntegerVector(2);
		defSize.add(GUIFrame.VIEW_WIDTH);
		defSize.add(GUIFrame.VIEW_HEIGHT);
		windowSize = new IntegerListInput("WindowSize", GRAPHICS, defSize);
		windowSize.setValidCount(2);
		windowSize.setValidRange(1, 8192);
		windowSize.setPromptReqd(false);
		this.addInput(windowSize);

		IntegerVector defPos = new IntegerVector(2);
		defPos.add(GUIFrame.COL2_START);
		defPos.add(GUIFrame.TOP_START);
		windowPos = new IntegerListInput("WindowPosition", GRAPHICS, defPos);
		windowPos.setValidCount(2);
		windowPos.setValidRange(-8192, 8192);
		windowPos.setPromptReqd(false);
		this.addInput(windowPos);

		titleBar = new StringInput("TitleBarText", GRAPHICS, null);
		this.addInput(titleBar);

		showWindow = new BooleanInput("ShowWindow", GRAPHICS, false);
		showWindow.setPromptReqd(false);
		this.addInput(showWindow);

		movable = new BooleanInput("Movable", GRAPHICS, true);
		this.addInput(movable);

		lock2D = new BooleanInput("Lock2D", GRAPHICS, false);
		this.addInput(lock2D);

		followEntityInput = new EntityInput<>(DisplayEntity.class, "FollowEntity", GRAPHICS, null);
		this.addInput(followEntityInput);

		positionScriptInput = new KeyedVec3dInput("ScriptedViewPosition", GRAPHICS);
		positionScriptInput.setUnitType(DistanceUnit.class);
		this.addInput(positionScriptInput);

		centerScriptInput = new KeyedVec3dInput("ScriptedViewCenter", GRAPHICS);
		centerScriptInput.setUnitType(DistanceUnit.class);
		this.addInput(centerScriptInput);

		skyboxImage = new FileInput("SkyboxImage", GRAPHICS, null);
		this.addInput(skyboxImage);

	}

	public View() {
		allInstances.add(this);
		viewID = getJaamSimModel().getNextViewID();
	}

	public static ArrayList<View> getAll() {
		return allInstances;
	}

	@Override
	public void kill() {
		super.kill();
		allInstances.remove(this);
		if (RenderManager.isGood()) {
			RenderManager.inst().closeWindow(this);
		}
	}

	@Override
	public void restore(String name) {
		super.restore(name);
		allInstances.add(this);
		if (RenderManager.isGood()) {
			RenderManager.inst().createWindow(this);
		}
	}


	private static class WindowSizePosUpdater implements Runnable {
		private final IntegerVector pos;
		private final IntegerVector size;
		private final Frame window;

		public WindowSizePosUpdater(Frame w, IntegerVector p, IntegerVector s) {
			window = w;
			pos = p;
			size = s;
		}

		@Override
		public void run() {
			if (pos != null) {
				Point fix = OSFix.getLocationAdustment();
				window.setLocation(pos.get(0)+fix.x, pos.get(1)+fix.y);
			}

			if (size != null) {
				Point fix = OSFix.getSizeAdustment();
				window.setSize(size.get(0)+fix.x, size.get(1)+fix.y);
			}
		}

		void doUpdate() {
			if (EventQueue.isDispatchThread()) {
				this.run();
				return;
			}

			try {
				EventQueue.invokeAndWait(this);
			}
			catch (InvocationTargetException | InterruptedException e) {} //ignore
		}
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		// Do nothing if the renderer has not be initialized successfully
		if (!RenderManager.isGood())
			return;

		if (in == windowPos) {
			final Frame window = RenderManager.getOpenWindowForView(this);
			if (window != null)
				new WindowSizePosUpdater(window, windowPos.getValue(), null).doUpdate();
			return;
		}
		if (in == windowSize) {
			final Frame window = RenderManager.getOpenWindowForView(this);
			if (window != null)
				new WindowSizePosUpdater(window, null, windowSize.getValue()).doUpdate();
			return;
		}
		if (in == showWindow) {
			if (showWindow.getValue()) {
				RenderManager.inst().createWindow(this);
			}
			else {
				RenderManager.inst().closeWindow(this);
			}
			return;
		}
	}

	public Vec3d getViewCenter() {
		return center.getValue();
	}

	public Vec3d getViewPosition() {
		return position.getValue();
	}

	public Vec3d getGlobalPosition() {
		synchronized (setLock) {

			// Check if this is following a script
			if (positionScriptInput.hasKeys()) {
				return positionScriptInput.getValueForTime(cachedSimTime);
			}

			// Is this view following an entity?
			DisplayEntity follow = followEntityInput.getValue();
			if (follow != null) {
				Vec3d ret = follow.getGlobalPosition();
				ret.add3(position.getValue());
				return ret;
			}

			Vec3d tmp = position.getValue();
			Vec4d ret = new Vec4d(tmp.x, tmp.y, tmp.z, 1.0d);
			if (region.getValue() != null) {
				Transform regTrans = region.getValue().getRegionTrans();
				regTrans.apply(ret, ret);
			}
			return ret;
		}
	}

	public Vec3d getGlobalCenter() {
		synchronized (setLock) {

			// Check if this is following a script
			if (centerScriptInput.hasKeys()) {
				return centerScriptInput.getValueForTime(cachedSimTime);
			}

			DisplayEntity follow = followEntityInput.getValue();
			if (follow != null) {
				return follow.getGlobalPosition();
			}

			Vec3d tmp = center.getValue();
			Vec4d ret = new Vec4d(tmp.x, tmp.y, tmp.z, 1.0d);
			if (region.getValue() != null) {
				Transform regTrans = region.getValue().getRegionTrans();
				regTrans.apply(ret, ret);
			}
			return ret;
		}
	}

	/**
	 * updateCenterAndPos is used only by the mouse interaction code. It takes the camera view center and camera position in global
	 * coordinates and sets the corresponding inputs (in region coordinates).
	 * @param center - view center in world coordinates
	 * @param pos - camera position in world coordinates
	 */
	public void updateCenterAndPos(Vec3d center, Vec3d pos) {
		synchronized (setLock){

			if (isScripted())
				return;

			Vec3d tempPos = new Vec3d(pos);
			Vec3d tempCent = new Vec3d(center);

			if (region.getValue() != null) {
				Transform regTrans = region.getValue().getRegionTrans();
				regTrans.inverse(regTrans);
				regTrans.multAndTrans(pos, tempPos);
				regTrans.multAndTrans(center, tempCent);
			}

			// If this is following an entity, subtract that entity's position from the camera position (as it is interpreted as relative)

			if (isFollowing()) {
				tempPos.sub3(followEntityInput.getValue().getGlobalPosition(), tempPos);
			}

			KeywordIndex posKw = InputAgent.formatVec3dInput("ViewPosition", tempPos, DistanceUnit.class);
			KeywordIndex ctrKw = InputAgent.formatVec3dInput("ViewCenter", tempCent, DistanceUnit.class);
			InputAgent.storeAndExecute(new KeywordCommand(this, posKw, ctrKw));
		}
	}

	public String getTitle() {
		if (titleBar.getValue() != null)
			return titleBar.getValue();
	    else
		    return this.getName();
	}

	public boolean showWindow() {
		return showWindow.getValue();
	}

	public Region getRegion() {
		return region.getValue();
	}

	public void setWindowPos(int x, int y, int width, int height) {
		ArrayList<KeywordIndex> kwList = new ArrayList<>(2);

		IntegerVector pos = windowPos.getValue();
		Point posFix = OSFix.getLocationAdustment();
		if (pos.get(0) != x - posFix.x || pos.get(1) != y - posFix.y) {
			ArrayList<String> tokens = new ArrayList<>(2);
			tokens.add(String.format((Locale)null, "%d", x - posFix.x));
			tokens.add(String.format((Locale)null, "%d", y - posFix.y));
			KeywordIndex posKw = new KeywordIndex(this.windowPos.getKeyword(), tokens, null);
			kwList.add(posKw);
		}

		IntegerVector size = windowSize.getValue();
		Point sizeFix = OSFix.getSizeAdustment();
		if (size.get(0) != width - sizeFix.x || size.get(1) != height - sizeFix.y) {
			ArrayList<String> tokens = new ArrayList<>(2);
			tokens.add(String.format((Locale)null, "%d", width - sizeFix.x));
			tokens.add(String.format((Locale)null, "%d", height - sizeFix.y));
			KeywordIndex sizeKw = new KeywordIndex(this.windowSize.getKeyword(), tokens, null);
			kwList.add(sizeKw);
		}

		if (kwList.isEmpty())
			return;

		KeywordIndex[] kws = new KeywordIndex[kwList.size()];
		kwList.toArray(kws);
		InputAgent.storeAndExecute(new KeywordCommand(this, kws));
	}

	public IntegerVector getWindowPos() {
		Point fix = OSFix.getLocationAdustment();  //FIXME
		IntegerVector ret = new IntegerVector(windowPos.getValue());
		ret.addAt(fix.x, 0);
		ret.addAt(fix.y, 1);
		return ret;
	}

	public IntegerVector getWindowSize() {
		Point fix = OSFix.getSizeAdustment();  //FIXME
		IntegerVector ret = new IntegerVector(windowSize.getValue());
		ret.addAt(fix.x, 0);
		ret.addAt(fix.y, 1);
		return ret;
	}

	public void setKeepWindowOpen(boolean b) {
		keepWindowOpen = b;
	}

	public boolean getKeepWindowOpen() {
		return keepWindowOpen;
	}

	public int getID() {
		return viewID;
	}

	public boolean isMovable() {
		return movable.getValue();
	}

	public boolean isFollowing() {
		return followEntityInput.getValue() != null;
	}

	public boolean isScripted() {
		return positionScriptInput.hasKeys() || centerScriptInput.hasKeys();
	}

	public void setLock2D(boolean bLock2D) {
		synchronized (setLock) {
			ArrayList<String> toks = new ArrayList<>();
			toks.add(bLock2D ? "TRUE" : "FALSE");
			KeywordIndex kw = new KeywordIndex(lock2D.getKeyword(), toks, null);
			InputAgent.apply(this, kw);
		}

	}

	public boolean is2DLocked() {
		return lock2D.getValue();
	}

	public URI getSkyboxTexture() {
		URI file = skyboxImage.getValue();
		if (file == null || file.toString().equals("")) {
			return null;
		}
		return file;
	}

	public void update(double simTime) {
		cachedSimTime = simTime;
	}

	@Output(name = "PointOfInterest",
	 description = "The point at which the view will zoom towards or rotate around.",
	    unitType = DistanceUnit.class,
	    sequence = 1)
	public Vec3d getPointOfInterest(double simTime) {
		if (!RenderManager.isGood())
			return new Vec3d();
		return RenderManager.inst().getPOI(this);
	}

	@Output(name = "DistanceToPOI",
	 description = "The distance from the camera position to the point of interest.",
	    unitType = DistanceUnit.class,
	    sequence = 2)
	public double geDistanceToPOI(double simTime) {
		if (!RenderManager.isGood())
			return Double.NaN;

		Vec3d poi = RenderManager.inst().getPOI(this);
		if (poi == null)
			return Double.NaN;

		Vec3d vec = new Vec3d(getViewPosition());
		vec.sub3(poi);
		return vec.mag3();
	}

}
