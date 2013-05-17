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

import java.util.ArrayList;

import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeyedVec3dInput;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.IntegerListInput;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.GUIFrame;
import com.sandwell.JavaSimulation3D.Region;

public class View extends Entity {
private static final ArrayList<View> allInstances;

public static final int NO_VIEW_ID = 0;
private static int nextID = 1;

private final int viewID;

@Keyword(desc = "The Region this View is within.",
         example = "View1 Region { Region1 }")
private final EntityInput<Region> region;

@Keyword(desc = "The position the view is looking at.",
         example = "View1 ViewCenter { 0 0 0 m }")
private final Vec3dInput center;

@Keyword(desc = "The position the view is looking from.",
         example = "View1 ViewPosition { 0 0 50 m }")
private final Vec3dInput position;

@Keyword(desc = "The size of the window in pixels (width, height).",
         example = "View1 WindowSize { 500 300 }")
private final IntegerListInput windowSize;

@Keyword(desc = "The position of the upper left corner of the window in pixels measured" +
                "from the top left corner of the screen.",
         example = "View1 WindowPosition { 220 110 }")
private final IntegerListInput windowPos;

@Keyword(desc = "The text to place in the title bar of the window",
         example = "View1 TilteBarText { 'An Example Title' }")
private final StringInput titleBar;

@Keyword(desc = "A Boolean indicating whether the view should show a window" +
                "after all inputs have been loaded",
         example = "View1 ShowWindow { FALSE }")
private final BooleanInput showOnStartup;

@Keyword(desc = "A Boolean indicating whether the view position can be moved (panned or rotated)",
 example = "View1 Movable { FALSE }")
private final BooleanInput movable;

@Keyword(desc = "The (optional) entity for this view to follow. Setting this input makes the view ignore ViewCenter " +
                "and interprets ViewPosition as a relative offset to this entity.",
         example = "View1 FollowEntity { Ship1 }")
private final EntityInput<DisplayEntity> followEntityInput;

@Keyword(desc = "The (optional) scripted curve for the view position to follow.",
 example = "View1 ScriptedViewPosition { { { 0 h } { 0 0 0 m } } { { 100 h } { 100 0 0 m } } }")
private final KeyedVec3dInput positionScriptInput;

@Keyword(desc = "The (optional) scripted curve for the view center to follow.",
example = "View1 ScriptedViewCenter { { { 0 h } { 0 0 0 m } } { { 100 h } { 100 0 0 m } } }")
private final KeyedVec3dInput centerScriptInput;

private Object setLock = new Object();

private double cachedSimTime = 0;

private ChangeWatcher dataDirtier = new ChangeWatcher();

static {
	allInstances = new ArrayList<View>();
}

{
	region = new EntityInput<Region>(Region.class, "Region", "Graphics", null);
	this.addInput(region, true);

	center = new Vec3dInput("ViewCenter", "Graphics", new Vec3d());
	center.setUnits("m");
	this.addInput(center, true);

	position = new Vec3dInput("ViewPosition", "Graphics", new Vec3d(5.0d, -5.0d, 5.0d));
	position.setUnits("m");
	this.addInput(position, true);

	IntegerVector defSize = new IntegerVector(2);
	defSize.add(GUIFrame.VIEW_WIDTH);
	defSize.add(GUIFrame.VIEW_HEIGHT);
	windowSize = new IntegerListInput("WindowSize", "Graphics", defSize);
	windowSize.setValidCount(2);
	windowSize.setValidRange(1, 8192);
	this.addInput(windowSize, true);

	IntegerVector defPos = new IntegerVector(2);
	defPos.add(GUIFrame.COL2_START);
	defPos.add(GUIFrame.TOP_START);
	windowPos = new IntegerListInput("WindowPosition", "Graphics", defPos);
	windowPos.setValidCount(2);
	windowPos.setValidRange(-8192, 8192);
	this.addInput(windowPos, true);

	titleBar = new StringInput("TitleBarText", "Graphics", null);
	this.addInput(titleBar, true);

	showOnStartup = new BooleanInput("ShowWindow", "Graphics", false);
	this.addInput(showOnStartup, true);

	movable = new BooleanInput("Movable", "Graphics", true);
	this.addInput(movable, true);

	followEntityInput = new EntityInput<DisplayEntity>(DisplayEntity.class, "FollowEntity", "Graphics", null);
	this.addInput(followEntityInput, true);

	positionScriptInput = new KeyedVec3dInput("ScriptedViewPosition", "Graphics", "m", "h");
	this.addInput(positionScriptInput, true);

	centerScriptInput = new KeyedVec3dInput("ScriptedViewCenter", "Graphics", "m", "h");
	this.addInput(centerScriptInput, true);
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

@Override
public void updateForInput( Input<?> in ) {
	if (in == position || in == center) {
		dataDirtier.changed();
		return;
	}

	// The entity inputs that this view is dependent on
	if (in == region || in == followEntityInput) {
		dataDirtier.clearDependents();
		if (region.getValue() != null) {
			dataDirtier.addDependent(region.getValue().getGraphicsDirtier());
		}
		if (followEntityInput.getValue() != null) {
			dataDirtier.addDependent(followEntityInput.getValue().getGraphicsDirtier());
		}
		return;
	}
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
			Vec4d ret = follow.getGlobalPosition();
			ret.add3(position.getValue());
			return ret;
		}

		Vec3d tmp = position.getValue();
		Vec4d ret = new Vec4d(tmp.x, tmp.y, tmp.z, 1.0d);
		if (region.getValue() != null) {
			Transform regTrans = region.getValue().getRegionTrans(0);
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
			Transform regTrans = region.getValue().getRegionTrans(0);
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
			Transform regTrans = region.getValue().getRegionTrans(0);
			regTrans.inverse(regTrans);
			regTrans.apply(pos, tempPos);
			regTrans.apply(center, tempCent);
		}

		// If this is following an entity, subtract that entity's position from the camera position (as it is interpreted as relative)

		if (isFollowing()) {
			tempPos.sub3(followEntityInput.getValue().getGlobalPosition(), tempPos);
		}

		String posVal = String.format("%f %f %f m", tempPos.x, tempPos.y, tempPos.z);
		InputAgent.processEntity_Keyword_Value(this, this.position, posVal);
		String cenVal = String.format("%f %f %f m", tempCent.x, tempCent.y, tempCent.z);
		InputAgent.processEntity_Keyword_Value(this, this.center, cenVal);
		dataDirtier.changed();
	}
}

public String getTitle() {
	if (titleBar.getValue() != null)
		return titleBar.getValue();
	else
		return this.getInputName();
}

public boolean showOnStart() {
	return showOnStartup.getValue();
}

public Region getRegion() {
	return region.getValue();
}

public void setRegion(Region reg) {
	InputAgent.processEntity_Keyword_Value(this, this.region, reg.getInputName());
}

public void setPosition(Vec3d pos) {
	String val = String.format("%f %f %f m", pos.x, pos.y, pos.z);
	InputAgent.processEntity_Keyword_Value(this, this.position, val);

	dataDirtier.changed();
}

public void setCenter(Vec3d cent) {
	String val = String.format("%f %f %f m", cent.x, cent.y, cent.z);
	InputAgent.processEntity_Keyword_Value(this, this.center, val);

	dataDirtier.changed();
}

public void setWindowPos(int x, int y, int width, int height) {
	String posVal = String.format("%d %d", x, y);
	String sizeVal = String.format("%d %d", width, height);
	InputAgent.processEntity_Keyword_Value(this, this.windowPos, posVal);
	InputAgent.processEntity_Keyword_Value(this, this.windowSize, sizeVal);

	FrameBox.valueUpdate();

}

public ChangeWatcher.Tracker getChangeTracker() {
	return dataDirtier.getTracker();
}

/**
 * Allow an outside influence to force a dirty state
 */
public void forceDirty() {
	dataDirtier.changed();
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

public void update(double simTime) {
	cachedSimTime = simTime;
	if (isScripted()) {
		forceDirty();
	}
}

}
