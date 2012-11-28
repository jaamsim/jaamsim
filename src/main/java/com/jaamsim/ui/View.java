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

import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
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
import com.sandwell.JavaSimulation3D.InputAgent;
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

private Object setLock = new Object();

private ChangeWatcher dataDirtier = new ChangeWatcher();

static {
	allInstances = new ArrayList<View>();
}

{
	region = new EntityInput<Region>(Region.class, "Region", "Graphics", null);
	this.addInput(region, true);

	center = new Vec3dInput("ViewCenter", "Graphics", Vector4d.ORIGIN);
	center.setUnits("m");
	this.addInput(center, true);

	position = new Vec3dInput("ViewPosition", "Graphics", new Vector4d(5.0d, -5.0d, 5.0d));
	position.setUnits("m");
	this.addInput(position, true);

	IntegerVector defSize = new IntegerVector(2);
	defSize.add(1060);
	defSize.add(600);
	windowSize = new IntegerListInput("WindowSize", "Graphics", defSize);
	windowSize.setValidCount(2);
	windowSize.setValidRange(1, 8192);
	this.addInput(windowSize, true);

	IntegerVector defPos = new IntegerVector(2);
	defPos.add(220);
	defPos.add(110);
	windowPos = new IntegerListInput("WindowPosition", "Graphics", defPos);
	windowPos.setValidCount(2);
	windowPos.setValidRange(-8192, 8192);
	this.addInput(windowPos, true);

	titleBar = new StringInput("TitleBarText", "Graphics", null);
	this.addInput(titleBar, true);

	showOnStartup = new BooleanInput("ShowWindow", "Graphics", false);
	this.addInput(showOnStartup, true);
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


	if (in == region) {
		if (region.getValue() != null) {
			dataDirtier.clearDependents();
			dataDirtier.addDependent(region.getValue().getGraphicsDirtier());
		}
		else
			dataDirtier.clearDependents();
		return;
	}
}

public Vector4d getGlobalPosition() {
	synchronized (setLock) {
		Vector4d ret = new Vector4d(position.getValue());
		if (region.getValue() != null) {
			Transform regTrans = region.getValue().getRegionTrans(0);
			regTrans.apply(ret, ret);
		}
		return ret;
	}
}

public Vector4d getGlobalCenter() {
	synchronized (setLock) {
		Vector4d ret = new Vector4d(center.getValue());
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
public void updateCenterAndPos(Vector4d center, Vector4d pos) {
	synchronized (setLock){
		Vector4d tempPos = new Vector4d(pos);
		Vector4d tempCent = new Vector4d(center);

		if (region.getValue() != null) {
			Transform regTrans = region.getValue().getRegionTrans(0);
			regTrans.inverse(regTrans);
			regTrans.apply(pos, tempPos);
			regTrans.apply(center, tempCent);
		}

		String posVal = String.format("%f %f %f m", tempPos.x(), tempPos.y(), tempPos.z());
		InputAgent.processEntity_Keyword_Value(this, this.position, posVal);
		String cenVal = String.format("%f %f %f m", tempCent.x(), tempCent.y(), tempCent.z());
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

public void setPosition(Vector4d pos) {
	String val = String.format("%f %f %f m", pos.x(), pos.y(), pos.z());
	InputAgent.processEntity_Keyword_Value(this, this.position, val);

	dataDirtier.changed();
}

public void setCenter(Vector4d cent) {
	String val = String.format("%f %f %f m", cent.x(), cent.y(), cent.z());
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

public int getID() {
	return viewID;
}

}
