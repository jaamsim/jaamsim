/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Output;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.Vec3dInput;
import com.sandwell.JavaSimulation.Vector;

/**
 * Encapsulates the methods and data needed to display a simulation object in the 3D environment.
 * Extends the basic functionality of entity in order to have access to the basic system
 * components like the eventManager.
 */
public class DisplayEntity extends Entity {
	private static final ArrayList<DisplayEntity> allInstances;

	public static Simulation simulation;

	// A 'dirty' state tracker
	private ChangeWatcher graphicsDirtier = new ChangeWatcher();

	@Keyword(description = "The point in the region at which the alignment point of the object is positioned.",
	         example = "Object1 Position { -3.922 -1.830 0.000 m }")
	private final Vec3dInput positionInput;

	@Keyword(description = "The size of the object in { x, y, z } coordinates. If only the x and y coordinates are given " +
	                "then the z dimension is assumed to be zero.",
	         example = "Object1 Size { 15 12 0 m }")
	private final Vec3dInput sizeInput;

	@Keyword(description = "Euler angles defining the rotation of the object.",
	         example = "Object1 Orientation { 0 0 90 deg }")
	private final Vec3dInput orientationInput;

	@Keyword(description = "The point within the object about which its Position keyword is defined, " +
	                "expressed with respect to a unit box centered about { 0 0 0 }.",
	         example = "Object1 Alignment { -0.5 -0.5 0.0 }")
	private final Vec3dInput alignmentInput;

	@Keyword(description = "The name of the Region containing the object.  Applies an offset " +
			        "to the Position of the object corresponding to the Region's " +
			        "Position and Orientation values.",
	         example ="Object1 Region { Region1 }")
	private final EntityInput<Region> regionInput;

	private final Vec3d position = new Vec3d();
	private final Vec3d size = new Vec3d(1.0d, 1.0d, 1.0d);
	private final Vec3d orient = new Vec3d();
	private final Vec3d align = new Vec3d();

	private Region currentRegion;

	@Keyword(description = "The graphic representation of the object.  Accepts a list of objects where the distances defined in " +
	                "LevelOfDetail dictate which DisplayModel entry is used.",
	         example = "Object1 DisplayModel { Pixels }")
	private final EntityListInput<DisplayModel> displayModelList;

	@Keyword(description = "The name of an object with respect to which the Position keyword is referenced.",
	         example ="Object1Label RelativeEntity { Object1 }")
	private final EntityInput<DisplayEntity> relativeEntity;

	@Keyword(description = "If TRUE, the object is displayed in the simulation view windows.",
	         example = "Object1 Show { FALSE }")
	private final BooleanInput show;

	@Keyword(description = "If TRUE, the object is active and used in simulation runs.",
	         example = "Object1 Active { FALSE }")
	private final BooleanInput active;

	@Keyword(description = "If TRUE, the object can be positioned interactively using the GUI.",
	         example = "Object1 Movable { FALSE }")
	private final BooleanInput movable;

	@Keyword(description = "If TRUE, tool tips are displayed for the object on mouseover during the simulation run.",
	         example = "Simulation ToolTip { FALSE }")
	protected BooleanInput showToolTip;

	private ArrayList<DisplayModelBinding> modelBindings;

	public static class TagSet {
		public Map<String, Color4d[]> colours = new HashMap<String, Color4d[]>();
		public Map<String, DoubleVector> sizes = new HashMap<String, DoubleVector>();
		public Map<String, Boolean> visibility = new HashMap<String, Boolean>();

		/**
		 * A purely utility method to get the first colour, or default if the first colour is not present
		 */
		public Color4d getTagColourUtil(String tagName, Color4d def) {
			Color4d[] cs = colours.get(tagName);
			if (cs != null && cs.length > 0) {
				return cs[0];
			}
			return def;
		}

		public boolean isTagVisibleUtil(String tagName) {
			Boolean isVisible = visibility.get(tagName);
			return (isVisible == null || isVisible.booleanValue()); // Default to visible
		}

	}

	private TagSet tags = new TagSet();

	static {
		allInstances = new ArrayList<DisplayEntity>(100);
	}

	{
		positionInput = new Vec3dInput("Position", "Basic Graphics", new Vec3d());
		positionInput.setUnitType(DistanceUnit.class);
		this.addInput(positionInput, true);

		alignmentInput = new Vec3dInput("Alignment", "Basic Graphics", new Vec3d());
		this.addInput(alignmentInput, true);

		sizeInput = new Vec3dInput("Size", "Basic Graphics", new Vec3d(1.0d, 1.0d, 1.0d));
		sizeInput.setUnitType(DistanceUnit.class);
		this.addInput(sizeInput, true);

		orientationInput = new Vec3dInput("Orientation", "Basic Graphics", new Vec3d());
		orientationInput.setUnitType(AngleUnit.class);
		this.addInput(orientationInput, true);

		regionInput = new EntityInput<Region>(Region.class, "Region", "Basic Graphics", null);
		this.addInput(regionInput, true);

		relativeEntity = new EntityInput<DisplayEntity>(DisplayEntity.class, "RelativeEntity", "Basic Graphics", null);
		relativeEntity.setInvalidEntities(this);
		this.addInput(relativeEntity, true);

		displayModelList = new EntityListInput<DisplayModel>( DisplayModel.class, "DisplayModel", "Basic Graphics", null);
		this.addInput(displayModelList, true);
		displayModelList.setUnique(false);

		active = new BooleanInput("Active", "Basic Graphics", true);
		this.addInput(active, true);

		show = new BooleanInput("Show", "Basic Graphics", true);
		this.addInput(show, true);

		movable = new BooleanInput("Movable", "Basic Graphics", true);
		this.addInput(movable, true);

		showToolTip = new BooleanInput("ToolTip", "Basic Graphics", true);
		this.addInput(showToolTip, true);
	}

	/**
	 * Constructor: initializing the DisplayEntity's graphics
	 */
	public DisplayEntity() {
		super();

		setRegion(null);

		allInstances.add(this);

		DisplayModel dm = DisplayModel.getDefaultDisplayModelForClass(this.getClass());
		if(dm != null) {
			ArrayList<DisplayModel> defList = new ArrayList<DisplayModel>();
			defList.add(dm);
			displayModelList.setDefaultValue(defList);
		}
	}

	public static ArrayList<? extends DisplayEntity> getAll() {
		return allInstances;
	}

	@Override
	public void validate()
	throws InputErrorException {
		super.validate();

		if(getRelativeEntity() == this) {
			this.warning("validate()", "Relative Entities should not be defined in a circular loop", "");
		}

		if (getDisplayModelList() != null) {
			for (DisplayModel dm : getDisplayModelList()) {
				if (!dm.canDisplayEntity(this)) {
					this.error("validate()", String.format("Invalid Display model: %s for Entity", dm.getInputName()), "");
				}
			}
		}
	}

	/**
	 * Destroys the branchGroup hierarchy for the entity
	 */
	@Override
	public void kill() {
		super.kill();

		allInstances.remove(this);
		currentRegion = null;
	}

	/**
	 * Sets the Graphic Simulation object for the entity.
	 * @param newSimulation - states the new simulation the entity is controled by
	 */
	public static void setSimulation(Simulation newSimulation) {
		simulation = newSimulation;
	}

	public Region getCurrentRegion() {
		return currentRegion;
	}

	/**
	 * Removes the entity from its current region and assigns a new region
	 * @param newRegion - the region the entity will be assigned to
	 */
	public void setRegion( Region newRegion ) {
		currentRegion = newRegion;
		updateDirtyDependencies();
	}

	/**
	 * Update any internal stated needed by either renderer. This is a transition method to get away from
	 * java3D onto the new renderer.
	 *
	 * The JaamSim renderer will only call updateGraphics() while the Java3D renderer will call both
	 * updateGraphics() and render()
	 */
	public void updateGraphics(double simTime) {}

	private void calculateEulerRotation(Vec3d val, Vec3d euler) {
		double sinx = Math.sin(euler.x);
		double siny = Math.sin(euler.y);
		double sinz = Math.sin(euler.z);
		double cosx = Math.cos(euler.x);
		double cosy = Math.cos(euler.y);
		double cosz = Math.cos(euler.z);

		// Calculate a 3x3 rotation matrix
		double m00 = cosy * cosz;
		double m01 = -(cosx * sinz) + (sinx * siny * cosz);
		double m02 = (sinx * sinz) + (cosx * siny * cosz);

		double m10 = cosy * sinz;
		double m11 = (cosx * cosz) + (sinx * siny * sinz);
		double m12 = -(sinx * cosz) + (cosx * siny * sinz);

		double m20 = -siny;
		double m21 = sinx * cosy;
		double m22 = cosx * cosy;

		double x = m00 * val.x + m01 * val.y + m02 * val.z;
		double y = m10 * val.x + m11 * val.y + m12 * val.z;
		double z = m20 * val.x + m21 * val.y + m22 * val.z;

		val.set3(x, y, z);
	}

	public Vec3d getPositionForAlignment(Vec3d alignment) {
		Vec3d temp = new Vec3d(alignment);
		synchronized (position) {
			temp.sub3(align);
			temp.mul3(size);
			calculateEulerRotation(temp, orient);
			temp.add3(position);
		}

		return temp;
	}

	public Vec3d getOrientation() {
		synchronized (position) {
			return new Vec3d(orient);
		}
	}

	public void setOrientation(Vec3d orientation) {
		synchronized (position) {
			if (orient.equals3(orientation)) {
				return;
			}
			orient.set3(orientation);
			setGraphicsDataDirty();
		}
	}

	public void setSize(Vec3d size) {
		synchronized (position) {
			if (this.size.equals3(size)) {
				return;
			}
			this.size.set3(size);
			setGraphicsDataDirty();
		}
	}

	public Vec3d getPosition() {
		synchronized (position) {
			return new Vec3d(position);
		}
	}

	DisplayEntity getRelativeEntity() {
		return relativeEntity.getValue();
	}

	/**
	 * Returns the transform to global space including the region transform
	 * @return
	 */
	public Transform getGlobalTrans(double simTime) {
		return getGlobalTransForSize(size, simTime);
	}

	/**
	 *  This is a quirky method that returns the equivalent global transform for this entity as if 'sizeIn' where the actual
	 *  size. This is needed for TextModels, but may be refactored soon.
	 * @param sizeIn
	 * @param simTime
	 * @return
	 */
	public Transform getGlobalTransForSize(Vec3d sizeIn, double simTime) {
		// Okay, this math may be hard to follow, this is effectively merging two TRS transforms,
		// The first is a translation only transform from the alignment parameter
		// Then a transform is built up based on position and orientation
		// As size is a non-uniform scale it can not be represented by the jaamsim TRS Transform and therefore
		// not actually included in this result, except to adjust the alignment

		Vec3d temp = new Vec3d(sizeIn);
		temp.mul3(align);
		temp.scale3(-1.0d);
		Transform alignTrans = new Transform(temp);

		Quaternion rot = new Quaternion();
		rot.setEuler3(orient);

		Vec3d transVect = new Vec3d(position);
		DisplayEntity entity = this.getRelativeEntity();
		if(entity != null && entity != this) {
			transVect.add3(entity.position);
		}
		Transform ret = new Transform(transVect, rot, 1);
		ret.merge(ret, alignTrans);

		if (currentRegion != null) {
			Transform regionTrans = currentRegion.getRegionTrans(simTime);
			ret.merge(regionTrans, ret);
		}
		return ret;
	}

	/**
	 * Returns the global transform with scale factor all rolled into a Matrix4d
	 * @return
	 */
	public Mat4d getTransMatrix(double simTime) {
		Transform trans = getGlobalTrans(simTime);
		Mat4d ret = new Mat4d();
		trans.getMat4d(ret);
		ret.scaleCols3(getSize());
		return ret;
	}

	/**
	 * Returns the inverse global transform with scale factor all rolled into a Matrix4d
	 * @return
	 */
	public Mat4d getInvTransMatrix(double simTime) {
		return RenderUtils.getInverseWithScale(getGlobalTrans(simTime), size);
	}


	/**
	 * Returns a new Tracker of this DisplayEntity's graphics data state.
	 * If there has been a change in graphics data, the Tracker.hasChanged() will return true
	 * @return A new graphics state tracker
	 */
	public ChangeWatcher.Tracker getGraphicsChangeTracker() {
		return graphicsDirtier.getTracker();
	}

	public void setGraphicsDataDirty() {
		graphicsDirtier.changed();
	}

	public ChangeWatcher getGraphicsDirtier() {
		return graphicsDirtier;
	}

	private void updateDirtyDependencies() {
		graphicsDirtier.clearDependents();
		if (currentRegion != null) {
			graphicsDirtier.addDependent(currentRegion.getGraphicsDirtier());
		}
		if (relativeEntity.getValue() != null) {
			graphicsDirtier.addDependent(relativeEntity.getValue().getGraphicsDirtier());
		}
		if (getDisplayModelList() != null) {
			for (DisplayModel dm : getDisplayModelList()) {
				graphicsDirtier.addDependent(dm.getGraphicsDirtier());
			}
		}
	}

	/**
	 * Return the position in the global coordinate system
	 * @return
	 */
	public Vec4d getGlobalPosition() {
		Vec4d localPos = null;
		synchronized (position) {
			localPos = new Vec4d(position.x, position.y, position.z, 1.0d);
		}

		if (currentRegion != null) {
			Transform regionTrans = currentRegion.getRegionTrans(getCurrentTime());
			regionTrans.apply(localPos, localPos);
		}

		return localPos;
	}

	/*
	 * Returns the center relative to the origin
	 */
	public Vec3d getAbsoluteCenter() {
		Vec3d cent = this.getPositionForAlignment(new Vec3d());
		DisplayEntity entity = this.getRelativeEntity();
		if(entity != null && entity != this)
			cent.add3(entity.getPosition());

		return cent;
	}

	/**
	 *  Returns the extent for the DisplayEntity
	 */
	public Vec3d getSize() {
		synchronized (position) {
			return new Vec3d(size);
		}
	}

	public Vec3d getAlignment() {
		synchronized (position) {
			return new Vec3d(align);
		}
	}

	public void setAlignment(Vec3d align) {
		synchronized (position) {
			if (this.align.equals3(align)) {
				return;
			}

			this.align.set3(align);
			setGraphicsDataDirty();
		}
	}

	public void setPosition(Vec3d pos) {
		synchronized (position) {
			if (this.position.equals3(pos)) {
				return;
			}

			position.set3(pos);
			setGraphicsDataDirty();
		}
	}

	/**
	 * Set the global position for this entity, this takes into account the region
	 * transform and sets the local position accordingly
	 * @param pos - The new position in the global coordinate system
	 */
	public void setGlobalPosition(Vec4d pos) {
		Transform invReg = new Transform();

		if (currentRegion != null) {
			Transform regionTrans = currentRegion.getRegionTrans(getCurrentTime());
			regionTrans.inverse(invReg);
		}

		Vec4d localPos = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		invReg.apply(pos, localPos);

		setPosition(localPos);
		InputAgent.processEntity_Keyword_Value(this, positionInput, String.format( "%.6f %.6f %.6f m", localPos.x, localPos.y, localPos.z ));
		FrameBox.valueUpdate();
	}

	/*
	 * move object to argument point based on alignment
	 */
	public void setPositionForAlignment(Vec3d alignment, Vec3d position) {
		// Calculate the difference between the desired point and the current aligned position
		Vec3d diff = this.getPositionForAlignment(alignment);
		diff.sub3(position);
		diff.scale3(-1.0d);

		// add the difference to the current position and set the new position
		diff.add3(getPosition());
		setPosition(diff);
	}



	/**
	 Returns a vector of strings describing the DisplayEntity.
	 Override to add details
	 @return Vector - tab delimited strings describing the DisplayEntity
	 **/
	@Override
	public Vector getInfo() {
		Vector info = super.getInfo();
		if( getCurrentRegion() != null )
			info.addElement("Region\t" + getCurrentRegion().getInputName());
		else
			info.addElement( "Region\t<no region>" );
		return info;
	}

	public ArrayList<DisplayModel> getDisplayModelList() {
		return displayModelList.getValue();
	}

	public ArrayList<DisplayModelBinding> getDisplayBindings() {
		if (modelBindings == null) {
			// Populate the model binding list
			if (getDisplayModelList() == null) {
				modelBindings = new ArrayList<DisplayModelBinding>();
				return modelBindings;
			}
			modelBindings = new ArrayList<DisplayModelBinding>(getDisplayModelList().size());
			for (int i = 0; i < getDisplayModelList().size(); ++i) {
				DisplayModel dm = getDisplayModelList().get(i);
				modelBindings.add(dm.getBinding(this));
			}
		}
		return modelBindings;
	}

	public void dragged(Vec3d distance) {
		Vec3d newPos = this.getPosition();
		newPos.add3(distance);
		this.setPosition(newPos);

		// inform simulation and editBox of new positions
		InputAgent.processEntity_Keyword_Value(this, positionInput, String.format( "%.6f %.6f %.6f m", newPos.x, newPos.y, newPos.z ));
		FrameBox.valueUpdate();
	}

	public boolean isActive() {
		return active.getValue();
	}

	public boolean getShow() {
		return show.getValue();
	}

	public boolean isMovable() {
		return movable.getValue();
	}

	public boolean showToolTip() {
		return showToolTip.getValue();
	}

	/**
	 * This method updates the DisplayEntity for changes in the given input
	 */
	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if( in == positionInput ) {
			this.setPosition(  positionInput.getValue() );
			return;
		}
		if( in == sizeInput ) {
			this.setSize( sizeInput.getValue() );
			return;
		}
		if( in == orientationInput ) {
			this.setOrientation( orientationInput.getValue() );
			return;
		}
		if( in == alignmentInput ) {
			this.setAlignment( alignmentInput.getValue() );
			return;
		}
		if( in == regionInput ) {
			this.setRegion(regionInput.getValue());
		}

		if (in == displayModelList) {
			modelBindings = null; // Clear this on any change, and build it lazily later
		}

		if( in == relativeEntity || in == displayModelList) {
			updateDirtyDependencies();
		}
		setGraphicsDataDirty();
	}

	/////////////////////////////////
	// Tag system

	public void setTagColour(String tagName, Color4d ca) {
		Color4d cas[] = new Color4d[1] ;
		cas[0] = ca;
		setTagColours(tagName, cas);
	}

	public void setTagColours(String tagName, Color4d[] cas) {

		// Update the colour tags, but make sure to only dirty the state if something actually changed
		Color4d[] colours = tags.colours.get(tagName);
		boolean changed = false;

		if (colours == null || colours.length != cas.length) {
			colours = new Color4d[cas.length];
			changed = true;
		}

		for (int i = 0; i < cas.length; ++i) {
			Color4d newColour = cas[i];
			if (!newColour.equals(colours[i])) {
				changed = true;
			}
			colours[i] = newColour;
		}

		if (changed) {
			tags.colours.put(tagName, colours);
			setGraphicsDataDirty();
		}
	}

	public void setTagSize(String tagName, double size) {
		DoubleVector sizes = new DoubleVector();
		sizes.add(size);
		setTagSizes(tagName, sizes);
	}

	public void setTagSizes(String tagName, DoubleVector sizes) {
		DoubleVector oldSizes = tags.sizes.get(tagName);
		boolean changed = false;

		if (oldSizes == null || oldSizes.size() != sizes.size()) {
			changed = true;
		}
		else {
			for (int i = 0; i < sizes.size(); ++i) {
				if (!MathUtils.near(sizes.get(i), oldSizes.get(i))) {
					changed = true;
				}
			}
		}

		if (changed) {
			tags.sizes.put(tagName, sizes);
			setGraphicsDataDirty();
		}
	}

	public void setTagVisibility(String tagName, boolean isVisible) {
		Boolean oldVisible = tags.visibility.get(tagName);

		if (oldVisible == null || oldVisible != isVisible) {
			tags.visibility.put(tagName, isVisible);
			setGraphicsDataDirty();
		}
	}

	/**
	 * Get all tags for this entity
	 * @return
	 */
	public TagSet getTagSet() {
		return tags;
	}

	////////////////////////////////////////////////////////////////////////
	// Outputs
	////////////////////////////////////////////////////////////////////////

	@Output(name = "Position",
	        description = "The DisplayEntity's position in region space.",
	        unitType = DistanceUnit.class)
	public Vec3d getPosOutput(double simTime) {
		return getPosition();
	}

	@Output(name = "Size",
	        description = "The DisplayEntity's size in meters.",
	        unitType = DistanceUnit.class)
	public Vec3d getSizeOutput(double simTime) {
		return getSize();
	}

	@Output(name = "Orientation",
	        description = "The XYZ euler angles describing the DisplayEntity's current rotation.",
	        unitType = AngleUnit.class)
	public Vec3d getOrientOutput(double simTime) {
		return getOrientation();
	}

	@Output(name = "Alignment",
	        description = "The point on the DisplayEntity that aligns direction with the position output.\n" +
	                      "The components should be in the range [-0.5, 0.5]",
	        unitType = DimensionlessUnit.class)
	public Vec3d getAlignOutput(double simTime) {
		return getAlignment();
	}

}
