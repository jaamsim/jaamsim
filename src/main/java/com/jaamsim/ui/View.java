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
package com.jaamsim.ui;

import java.net.URI;
import java.util.ArrayList;
import java.util.Locale;

import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.KeyedVec3dInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.StringInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;
import com.sandwell.JavaSimulation3D.Region;

public class View extends Entity {
private static final ArrayList<View> allInstances;

public static final int NO_VIEW_ID = 0;
private static int nextID = 1;

private final int viewID;

private boolean keepWindowOpen;  // used by GUIFrame to determine whether a window is open or closed

@Keyword(description = "The Region this View is within.",
         example = "View1 Region { Region1 }")
private final EntityInput<Region> region;

@Keyword(description = "The position the view is looking at.",
         example = "View1 ViewCenter { 0 0 0 m }")
private final Vec3dInput center;

@Keyword(description = "The position the view is looking from.",
         example = "View1 ViewPosition { 0 0 50 m }")
private final Vec3dInput position;

@Keyword(description = "The size of the window in pixels (width, height).",
         example = "View1 WindowSize { 500 300 }")
private final IntegerListInput windowSize;

@Keyword(description = "The position of the upper left corner of the window in pixels measured" +
                "from the top left corner of the screen.",
         example = "View1 WindowPosition { 220 110 }")
private final IntegerListInput windowPos;

@Keyword(description = "The text to place in the title bar of the window",
         example = "View1 TilteBarText { 'An Example Title' }")
private final StringInput titleBar;

@Keyword(description = "A Boolean indicating whether the view should show a window" +
                "after all inputs have been loaded",
         example = "View1 ShowWindow { FALSE }")
private final BooleanInput showWindow;

@Keyword(description = "A Boolean indicating whether the view position can be moved (panned or rotated)",
 example = "View1 Movable { FALSE }")
private final BooleanInput movable;

@Keyword(description = "The (optional) entity for this view to follow. Setting this input makes the view ignore ViewCenter " +
                "and interprets ViewPosition as a relative offset to this entity.",
         example = "View1 FollowEntity { Ship1 }")
private final EntityInput<DisplayEntity> followEntityInput;

@Keyword(description = "The (optional) scripted curve for the view position to follow.",
 example = "View1 ScriptedViewPosition { { { 0 h } { 0 0 0 m } } { { 100 h } { 100 0 0 m } } }")
private final KeyedVec3dInput positionScriptInput;

@Keyword(description = "The (optional) scripted curve for the view center to follow.",
example = "View1 ScriptedViewCenter { { { 0 h } { 0 0 0 m } } { { 100 h } { 100 0 0 m } } }")
private final KeyedVec3dInput centerScriptInput;

@Keyword(description = "The image file to use as the background for this view.",
example = "View1 SkyboxImage { '/resources/images/sky_map_2048x1024.jpg' }")
private final FileInput skyboxImage;

private Object setLock = new Object();

private double cachedSimTime = 0;

static {
	allInstances = new ArrayList<View>();
}

{
	region = new EntityInput<Region>(Region.class, "Region", "Graphics", null);
	this.addInput(region);

	center = new Vec3dInput("ViewCenter", "Graphics", new Vec3d());
	center.setUnitType(DistanceUnit.class);
	this.addInput(center);

	position = new Vec3dInput("ViewPosition", "Graphics", new Vec3d(5.0d, -5.0d, 5.0d));
	position.setUnitType(DistanceUnit.class);
	this.addInput(position);

	IntegerVector defSize = new IntegerVector(2);
	defSize.add(GUIFrame.VIEW_WIDTH);
	defSize.add(GUIFrame.VIEW_HEIGHT);
	windowSize = new IntegerListInput("WindowSize", "Graphics", defSize);
	windowSize.setValidCount(2);
	windowSize.setValidRange(1, 8192);
	this.addInput(windowSize);

	IntegerVector defPos = new IntegerVector(2);
	defPos.add(GUIFrame.COL2_START);
	defPos.add(GUIFrame.TOP_START);
	windowPos = new IntegerListInput("WindowPosition", "Graphics", defPos);
	windowPos.setValidCount(2);
	windowPos.setValidRange(-8192, 8192);
	this.addInput(windowPos);

	titleBar = new StringInput("TitleBarText", "Graphics", null);
	this.addInput(titleBar);

	showWindow = new BooleanInput("ShowWindow", "Graphics", false);
	this.addInput(showWindow);

	movable = new BooleanInput("Movable", "Graphics", true);
	this.addInput(movable);

	followEntityInput = new EntityInput<DisplayEntity>(DisplayEntity.class, "FollowEntity", "Graphics", null);
	this.addInput(followEntityInput);

	positionScriptInput = new KeyedVec3dInput("ScriptedViewPosition", "Graphics");
	positionScriptInput.setUnitType(DistanceUnit.class);
	this.addInput(positionScriptInput);

	centerScriptInput = new KeyedVec3dInput("ScriptedViewCenter", "Graphics");
	centerScriptInput.setUnitType(DistanceUnit.class);
	this.addInput(centerScriptInput);

	skyboxImage = new FileInput("SkyboxImage", "Graphics", null);
	this.addInput(skyboxImage);

}

public View() {
	allInstances.add(this);
	viewID = nextID++;
}

public static ArrayList<View> getAll() {
	return allInstances;
}

@Override
public void kill() {
	super.kill();
	allInstances.remove(this);
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

		KeywordIndex kw = InputAgent.formatPointInputs(this.position.getKeyword(), tempPos, "m");
		InputAgent.apply(this, kw);
		kw = InputAgent.formatPointInputs(this.center.getKeyword(), tempCent, "m");
		InputAgent.apply(this, kw);
	}
}

public String getTitle() {
	if (titleBar.getValue() != null)
		return titleBar.getValue();
	else
		return this.getInputName();
}

public boolean showWindow() {
	return showWindow.getValue();
}

public Region getRegion() {
	return region.getValue();
}

public void setRegion(Region reg) {
	ArrayList<String> tokens = new ArrayList<String>(1);
	tokens.add(reg.getInputName());
	KeywordIndex kw = new KeywordIndex(tokens, region.getKeyword(), 0, tokens.size(), null);
	InputAgent.apply(this, kw);
}

public void setPosition(Vec3d pos) {
	KeywordIndex kw = InputAgent.formatPointInputs(position.getKeyword(), pos, "m");
	InputAgent.apply(this, kw);
}

public void setCenter(Vec3d cent) {
	KeywordIndex kw = InputAgent.formatPointInputs(center.getKeyword(), cent, "m");
	InputAgent.apply(this, kw);
}

public void setWindowPos(int x, int y, int width, int height) {
	ArrayList<String> tokens = new ArrayList<String>(2);
	tokens.add(String.format((Locale)null, "%d", x));
	tokens.add(String.format((Locale)null, "%d", y));
	KeywordIndex kw = new KeywordIndex(tokens, this.windowPos.getKeyword(), 0, tokens.size(), null);
	InputAgent.apply(this, kw);

	tokens.clear();
	tokens.add(String.format((Locale)null, "%d", width));
	tokens.add(String.format((Locale)null, "%d", height));
	kw = new KeywordIndex(tokens, this.windowSize.getKeyword(), 0, tokens.size(), null);
	InputAgent.apply(this, kw);
}

public IntegerVector getWindowPos() {
	return windowPos.getValue();
}

public IntegerVector getWindowSize() {
	return windowSize.getValue();
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

}
