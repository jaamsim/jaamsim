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

import javax.vecmath.Vector3d;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Vector;
import com.sandwell.JavaSimulation.Vector3dInput;

/**
 * Encapsulates the methods and data needed to display a simulation object in the 3D environment.
 * Extends the basic functionality of entity in order to have access to the basic system
 * components like the eventManager.
 *
 *  <h3>Visual Heirarchy</h3>
 *
 *		GraphicsSimulation.rootLocale	(Locale)<br>
 *			-stores regionBranchGroups in the Vector: regionModels<br>
 *			-this vector corrects the J3D memory leak when the visual universe has no viewers<br>
 *<br>
 *		Region.branchGroup			(BranchGroup from DisplayEntity)<br>
 *<br>
 *		Region.rootTransform		(TransformGroup from DisplayEntity)<br>
 *<br>
 *		Region.scaleTransform		(TransformGroup from DisplayEntity)<br>
 *<br>
 *		DisplayEntity.branchGroup		(BranchGroup)<br>
 *			** allows detach, pick reporting<br>
 *			- provides the container to add and remove from a region<br>
 *			- the basis for selecting an object from the visual display<br>
 *<br>
 *		DisplayEntity.rootTransform		(TransformGroup)<br>
 *			** allows writing and extending children, changing transforms<br>
 *			- provides the basis for local transformations of position and orientation<br>
 *			- host for visual utilities bounds, name and axis<br>
 *<br>
 *		DisplayEntity.scaleTransform		(TransformGroup)<br>
 *			** allows changing transforms<br>
 *			- provides the basis for local transformation of scaling<br>
 *			- used to scale model, but not visual utilities like bounds and axis<br>
 *<br>
 *		DisplayEntity.displayModel		(BranchGroup)<br>
 *			** allows detach, reading, writing, extending children<br>
 *			- provides the container for user models to be added to the visual universe<br>
 *			- capabilities to allow manipulation of the model's components<br>
 *<br>
 *		Shape3D, BranchGroup, SharedGroup, Shape2D (Rectangle, Circle, Label, ...)	(Node)<br>
 *			- model for the DisplayEntity comprised of components of Shape3Ds<br>
 *			- may be loaded from a geometry file<br>
 *
 *
 *
 *  <h3>DisplayEntity Structure</h3>
 *
 *	DisplayEntity is defined to be compatible with Audition DisplayPerformers, as such origin and extent
 *  are provided.  Since the display of objects using Java3D is not bound to 2 dimensions nor a fixed
 *  range of pixels, some changes are required.<br>
 *<br>
 *	- extent does not provides physical visual bounds of the DisplayEntity, rather it is a convenience used
 *    to determine where the logical dimensions of the object.  It has no effect on visual display<br>
 *<br>
 *  - origin represents the bottom left (in 2D space) of the DisplayEntity.  It is calculate from the
 *    more fundamental center and is related by the extent value.  Origin can still be used for positioning
 *    but rather center should be used for future compatibility.<br>
 *<br>
 *  - center represents the position of the object in 3D space.  This is the default positioning mechanism
 *    for DisplayEntities.  Setting the origin will be translated into setting the center.  The center represents
 *    the position on the Region of the local origin for the displayModel.<br>
 */
public class DisplayEntity extends Entity {
	private static final ArrayList<DisplayEntity> allInstances;

	public static GraphicSimulation simulation;

	// A 'dirty' state tracker
	private ChangeWatcher graphicsDirtier = new ChangeWatcher();

	@Keyword(desc = "The point in the region at which the alignment point of the object is positioned.",
	         example = "Object1 Position { -3.922 -1.830 0.000 m }")
	private final Vector3dInput positionInput;

	@Keyword(desc = "The size of the object in { x, y, z } coordinates. If only the x and y coordinates are given " +
	                "then the z dimension is assumed to be zero.",
	         example = "Object1 Size { 15 12 0 m }")
	private final Vector3dInput sizeInput;

	@Keyword(desc = "Euler angles defining the rotation of the object.",
	         example = "Object1 Orientation { 0 0 90 deg }")
	private final Vector3dInput orientationInput;

	@Keyword(desc = "The point within the object about which its Position keyword is defined, " +
	                "expressed with respect to a unit box centered about { 0 0 0 }.",
	         example = "Object1 Alignment { -0.5 -0.5 0.0 }")
	private final Vector3dInput alignmentInput;

	@Keyword(desc = "The name of the Region containing the object.  Applies an offset " +
			        "to the Position of the object corresponding to the Region's " +
			        "Position and Orientation values.",
	         example ="Object1 Region { Region1 }")
	private final EntityInput<Region> regionInput;

	private final Vector3d position = new Vector3d();
	private final Vector3d size = new Vector3d(1.0d, 1.0d, 1.0d);
	private final Vector3d orient = new Vector3d();
	private final Vector3d align = new Vector3d();

	private Region currentRegion;

	@Keyword(desc = "A list of viewing distances for displaying each value in the DisplayModel keyword." +
	                "The list is given in ascending order of viewing distance.",
	         example = "Object1 DisplayModel  { Ship3D Ship2D Pixels }\n" +
	                   "Object1 LevelOfDetail { 5 10 100 }")
	private DoubleListInput levelOfDetail;


	@Keyword(desc = "The graphic representation of the object.  Accepts a list of objects where the distances defined in " +
	                "LevelOfDetail dictate which DisplayModel entry is used.",
	         example = "Object1 DisplayModel { Pixels }")
	private final EntityListInput<DisplayModel> displayModelList;

	@Keyword(desc = "The name of an object with respect to which the Position keyword is referenced.",
	         example ="Object1Label RelativeEntity { Object1 }")
	private final EntityInput<DisplayEntity> relativeEntity;


	@Keyword(desc = "If TRUE, the object is displayed in the simulation view windows.",
	         example = "Object1 Show { FALSE }")
	private final BooleanInput show;

	@Keyword(desc = "If TRUE, the object is active and used in simulation runs.",
	         example = "Object1 Active { FALSE }")
	private final BooleanInput active;

	@Keyword(desc = "If TRUE, the object can be positioned interactively using the GUI.",
	         example = "Object1 Movable { FALSE }")
	private final BooleanInput movable;

	@Keyword(desc = "If TRUE, tool tips are displayed for the object on mouseover during the simulation run.",
	         example = "Simulation ToolTip { FALSE }")
	protected BooleanInput showToolTip;

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
		positionInput = new Vector3dInput("Position", "Basic Graphics", new Vector3d());
		positionInput.setUnits("m");
		this.addInput(positionInput, true);

		alignmentInput = new Vector3dInput("Alignment", "Basic Graphics", new Vector3d());
		this.addInput(alignmentInput, true);

		sizeInput = new Vector3dInput("Size", "Basic Graphics", new Vector3d(1.0d,1.0d,1.0d));
		sizeInput.setUnits("m");
		this.addInput(sizeInput, true);

		orientationInput = new Vector3dInput("Orientation", "Basic Graphics", new Vector3d());
		orientationInput.setUnits("rad");
		this.addInput(orientationInput, true);

		regionInput = new EntityInput<Region>(Region.class, "Region", "Basic Graphics", null);
		this.addInput(regionInput, true);

		relativeEntity = new EntityInput<DisplayEntity>(DisplayEntity.class, "RelativeEntity", "Basic Graphics", null);
		relativeEntity.setInvalidEntities(this);
		this.addInput(relativeEntity, true);

		levelOfDetail = new DoubleListInput("LevelOfDetail", "Basic Graphics", null);
		levelOfDetail.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		this.addInput(levelOfDetail, true);

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

	public void validate()
	throws InputErrorException {
		super.validate();
		Input.validateIndexedLists(displayModelList.getValue(), levelOfDetail.getValue(), "DisplayModel", "LevelOfDetail");
		if(getRelativeEntity() == this) {
			this.warning("validate()", "Relative Entities should not be defined in a circular loop", "");
		}
	}

	/**
	 * Destroys the branchGroup hierarchy for the entity
	 */
	public void kill() {
		super.kill();

		allInstances.remove(this);
		currentRegion = null;
	}

	/**
	 * Sets the Graphic Simulation object for the entity.
	 * @param newSimulation - states the new simulation the entity is controled by
	 */
	public static void setSimulation( GraphicSimulation newSimulation ) {
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
	 * Visually adds the entity to its currently assigned region
	 */
	public void enterRegion() {}

	/**
	 * Visually removes the entity from its region.  The current region for the entity is maintained.
	 */
	public void exitRegion() {}

	/**
	 * Update any internal stated needed by either renderer. This is a transition method to get away from
	 * java3D onto the new renderer.
	 *
	 * The JaamSim renderer will only call updateGraphics() while the Java3D renderer will call both
	 * updateGraphics() and render()
	 */
	public void updateGraphics(double simTime) {}

	private void calculateEulerRotation(Vector3d val, Vector3d euler) {
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

		val.set(x, y, z);
	}

	public Vector3d getPositionForAlignment(Vector3d alignment) {
		Vector3d temp = new Vector3d(alignment);
		synchronized (position) {
			temp.sub(align);
			temp.x *= size.x;
			temp.y *= size.y;
			temp.z *= size.z;
			calculateEulerRotation(temp, orient);
			temp.add(position);
		}

		return temp;
	}

	public Vector3d getOrientation() {
		synchronized (position) {
			return new Vector3d(orient);
		}
	}

	public void setOrientation(Vector3d orientation) {
		synchronized (position) {
			orient.set(orientation);
			setGraphicsDataDirty();
		}
	}

    /**  Translates the object to a new position, the same distance from centerOfOrigin,
     *   but at angle theta radians
     *   counterclockwise
     * @param centerOfOrigin
     * @param Theta
     */
	public void polarTranslationAroundPointByAngle( Vector3d centerOfOrigin, double Theta ) {
		Vector3d centerOfEntityRotated = new Vector3d();
		Vector3d centerOfEntity = this.getPositionForAlignment(new Vector3d());
		centerOfEntityRotated.x = centerOfOrigin.x + Math.cos( Theta ) * ( centerOfEntity.x - centerOfOrigin.x ) - Math.sin( Theta ) * ( centerOfEntity.y - centerOfOrigin.y );
		centerOfEntityRotated.y = centerOfOrigin.y + Math.cos( Theta ) * ( centerOfEntity.y - centerOfOrigin.y ) + Math.sin( Theta ) * ( centerOfEntity.x - centerOfOrigin.x );
		centerOfEntityRotated.z = centerOfOrigin.z;
		this.setPosition(centerOfEntityRotated);
	}

    /** Get the position of the DisplayEntity if it Rotates (counterclockwise) around the centerOfOrigin by
     *  Theta radians assuming the object centers at pos
     *
     * @param centerOfOrigin
     * @param Theta
     * @param pos
     */
	public Vector3d getCoordinatesForRotationAroundPointByAngleForPosition( Vector3d centerOfOrigin, double Theta, Vector3d pos ) {
		Vector3d centerOfEntityRotated = new Vector3d();
		centerOfEntityRotated.x = centerOfOrigin.x + Math.cos( Theta ) * ( pos.x - centerOfOrigin.x ) - Math.sin( Theta ) * ( pos.y - centerOfOrigin.y );
		centerOfEntityRotated.y = centerOfOrigin.y + Math.cos( Theta ) * ( pos.y - centerOfOrigin.y ) + Math.sin( Theta ) * ( pos.x - centerOfOrigin.x );
		centerOfEntityRotated.z = centerOfOrigin.z;
		return centerOfEntityRotated;
	}


	public void setSize(Vector3d size) {
		synchronized (position) {
			this.size.set(size);
			setGraphicsDataDirty();
		}
	}

	public Vector3d getPosition() {
		synchronized (position) {
			return new Vector3d(position);
		}
	}

	DisplayEntity getRelativeEntity() {
		return relativeEntity.getValue();
	}

	private Vector3d getOffsetToRelativeEntity() {
		Vector3d offset = new Vector3d();
		DisplayEntity entity = this.getRelativeEntity();
		if(entity != null && entity != this) {
			offset.add( entity.getPosition() );
		}
		return offset;
	}

	/**
	 * Returns the transform to global space including the region transform
	 * @return
	 */
	public Transform getGlobalTrans(double simTime) {
		// Okay, this math may be hard to follow, this is effectively merging two TRS transforms,
		// The first is a translation only transform from the alignment parameter
		// Then a transform is built up based on position and orientation
		// As size is a non-uniform scale it can not be represented by the jaamsim TRS Transform and therefore
		// not actually included in this result, except to adjust the alignment

		Transform alignTrans = new Transform(new Vector4d(align.x * size.x * -1,
                                                    align.y * size.y * -1,
                                                    align.z * size.z * -1),
                                       Quaternion.ident, 1);

		Quaternion rot = Quaternion.FromEuler(orient.x, orient.y, orient.z);

		Vector4d transVect = new Vector4d(position.x, position.y, position.z);
		DisplayEntity entity = this.getRelativeEntity();
		if(entity != null && entity != this) {
			transVect.addLocal3(new Vector4d(entity.position.x, entity.position.y, entity.position.z));
		}
		Transform ret = new Transform(transVect, rot, 1);
		ret.merge(alignTrans, ret);

		if (currentRegion != null) {
			Transform regionTrans = currentRegion.getRegionTrans(simTime);
			regionTrans.merge(ret, ret);
		}
		return ret;
	}
	public Transform getGlobalTrans() {
		return getGlobalTrans(getCurrentTime());
	}

	/**
	 * Returns the global transform with scale factor all rolled into a Matrix4d
	 * @return
	 */
	public Matrix4d getTransMatrix() {
		Transform trans = getGlobalTrans();
		Matrix4d ret = new Matrix4d();
		trans.getMatrix(ret);
		ret.mult(Matrix4d.ScaleMatrix(getJaamMathSize()), ret);

		return ret;
	}

	/**
	 * Returns the inverse global transform with scale factor all rolled into a Matrix4d
	 * @return
	 */
	public Matrix4d getInvTransMatrix() {
		Vector4d s = new Vector4d(size.x, size.y, size.z);

		return RenderUtils.getInverseWithScale(getGlobalTrans(), s);
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
		if (displayModelList.getValue() != null) {
			for (int i = 0; i < displayModelList.getValue().size(); ++i) {
				DisplayModel dm = displayModelList.getValue().get(i);

				graphicsDirtier.addDependent(dm.getGraphicsDirtier());
			}
		}
	}


	/**
	 * just like getSize() but returns a JammSim math library Vector4d
	 * @return
	 */
	public Vector4d getJaamMathSize() {
		synchronized (position) {
			return new Vector4d(size.x, size.y, size.z);
		}
	}

	/**
	 * Return the position in the global coordinate system
	 * @return
	 */
	public Vector4d getGlobalPosition() {
		Vector4d localPos = null;
		synchronized (position) {
			localPos = new Vector4d(position.x, position.y, position.z);
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
	public Vector3d getAbsoluteCenter() {
		Vector3d cent = this.getPositionForAlignment(new Vector3d());
		cent.add(getOffsetToRelativeEntity());
		return cent;
	}

	public Vector3d getAbsolutePositionForAlignment(Vector3d alignment) {
		Vector3d extent = this.getSize();

		alignment.x *= extent.x;
		alignment.y *= extent.y;
		alignment.z *= extent.z;
		calculateEulerRotation(alignment, orient);

		alignment.add(this.getAbsoluteCenter());

		return alignment;
	}

	/**
	 *  Returns the extent for the DisplayEntity
	 *  @return Vector3d - width, height and depth of the bounds for the DisplayEntity
	 */
	public Vector3d getSize() {
		synchronized (position) {
			return new Vector3d(size);
		}
	}

	public Vector3d getAlignment() {
		synchronized (position) {
			return new Vector3d(align);
		}
	}

	public void setAlignment(Vector3d align) {
		synchronized (position) {
			this.align.set(align);
			setGraphicsDataDirty();
		}
	}

	public void setPosition(Vector3d pos) {
		synchronized (position) {
			position.set(pos);
			setGraphicsDataDirty();
		}
	}

	/**
	 * Set the global position for this entity, this takes into account the region
	 * transform and sets the local position accordingly
	 * @param pos - The new position in the global coordinate system
	 */
	public void setGlobalPosition(Vector4d pos) {
		Transform invReg = new Transform();

		if (currentRegion != null) {
			Transform regionTrans = currentRegion.getRegionTrans(getCurrentTime());
			regionTrans.inverse(invReg);
		}

		Vector4d localPos = new Vector4d();
		invReg.apply(pos, localPos);

		Vector3d j3dPos = new Vector3d();
		j3dPos.x = localPos.x();
		j3dPos.y = localPos.y();
		j3dPos.z = localPos.z();
		setPosition(j3dPos);
		InputAgent.processEntity_Keyword_Value(this, positionInput, String.format( "%.6f %.6f %.6f %s", j3dPos.x, j3dPos.y, j3dPos.z, positionInput.getUnits() ));
		FrameBox.valueUpdate();
	}

	/**
	 Returns a vector of strings describing the DisplayEntity.
	 Override to add details
	 @return Vector - tab delimited strings describing the DisplayEntity
	 **/
	public Vector getInfo() {
		Vector info = super.getInfo();
		if( getCurrentRegion() != null )
			info.addElement("Region\t" + getCurrentRegion().getInputName());
		else
			info.addElement( "Region\t<no region>" );
		return info;
	}

	public  EntityListInput<DisplayModel> getDisplayModelList() {
		return displayModelList;
	}

	public DoubleVector getLevelOfDetail() {
		return levelOfDetail.getValue();
	}

	public void dragged(Vector3d distance) {
		Vector3d newPos = this.getPosition();
		newPos.add(distance);
		this.setPosition(newPos);

		// inform simulation and editBox of new positions
		InputAgent.processEntity_Keyword_Value(this, positionInput, String.format( "%.6f %.6f %.6f %s", newPos.x, newPos.y, newPos.z, positionInput.getUnits() ));
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
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );

		if( in == positionInput ) {
			this.setPosition(  positionInput.getValue() );
		}
		if( in == sizeInput ) {
			this.setSize( sizeInput.getValue() );
		}
		if( in == orientationInput ) {
			this.setOrientation( orientationInput.getValue() );
		}
		if( in == alignmentInput ) {
			this.setAlignment( alignmentInput.getValue() );
		}
		if( in == regionInput ) {
			this.setRegion(regionInput.getValue());
		}

		if( in == relativeEntity ||
		    in == displayModelList ) {
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
		boolean oldVisible = tags.visibility.get(tagName);

		if (oldVisible != isVisible) {
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
}
