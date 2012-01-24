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

import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.EntityInput;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Vector;
import com.sandwell.JavaSimulation.Vector3dInput;
import com.sandwell.JavaSimulation3D.util.Circle;
import com.sandwell.JavaSimulation3D.util.Cube;
import com.sandwell.JavaSimulation3D.util.Shape;

import javax.media.j3d.Node;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingSphere;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.DistanceLOD;
import javax.media.j3d.LineArray;
import javax.media.j3d.OrderedGroup;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Switch;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3d;

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

	// simulation properties
	/** Graphic Simulation object this model is running under **/
	protected static GraphicSimulation simulation;

	// J3D graphics properties
	private final BranchGroup branchGroup; // connects to the region
	private final TransformGroup rootTransform; // provides universal transformations for the DisplayEntity's entire model
	private final TransformGroup scaleTransform; // provides scaling for display models
	private final OrderedGroup displayNode; // container for DisplayEntity's specific model

	private boolean needsRender = true;
	private final Vector3dInput positionInput;
	private final Vector3dInput sizeInput;
	private final Vector3dInput orientationInput;
	private final Vector3dInput alignmentInput;
	private final EntityInput<Region> regionInput;
	private final DoubleInput mouseNodesExtentInput;

	private final Vector3d position = new Vector3d();
	private final Vector3d size = new Vector3d(1.0d, 1.0d, 1.0d);
	private final Vector3d orient = new Vector3d();
	private final Vector3d align = new Vector3d();
	private final Vector3d scale = new Vector3d(1.0d, 1.0d, 1.0d);

	private DoubleListInput levelOfDetail;

	private final EntityListInput<DisplayModel> displayModelList; // DisplayModel from the input
	protected final ArrayList<DisplayModelBG> displayModels; // DisplayModel which represents this DisplayEntity

	private final EntityInput<DisplayEntity> relativeEntity;

	private final BooleanInput show;
	private final BooleanInput active;
	private final BooleanInput movable;

	private boolean animated = false;

	// continuous motion properties
	/** a Vector of Vector3d with coordinates for points along the track to be traversed */
	protected Vector displayTrack;
	/** the distance of the entity along the track in screen units */
	protected double displayDistanceAlongTrack;
	/** Distance in screen units of each portion in the track */
	protected DoubleVector displayTrackDistanceList;
	/** Angle in radians for each portion in the track */
	protected Vector displayTrackOrientationList;
	/** Total distance in screen units of the track */
	protected double displayTrackTotalDistance;
	/** the display speed of the entity in screen units per hour for continuous motion */
	protected double displaySpeedAlongTrack;
	/** the time at which the entity was last moved */
	private double timeOfLastMovement;

	// resize and rotate listener
	private boolean showResize = false;
	private BranchGroup rotateSizeBounds; // holds the extent outline
	private LineArray resizeLine; // bounding box for the entity for picking

	// setup a default appearance for the default displayModel
	private static Appearance axisLineAppearance;

	// vector of mouseNodes
	private ArrayList<MouseNode> mouseNodes;
	protected double mouseNodesSize;

	protected BooleanInput showToolTip; // true => show the tooltip on the screen for the DisplayEntity
	protected boolean graphicsSetup = false;

	static {
		axisLineAppearance = new Appearance();
		axisLineAppearance.setLineAttributes( new javax.media.j3d.LineAttributes( 2, javax.media.j3d.LineAttributes.PATTERN_SOLID, false ) );
		allInstances = new ArrayList<DisplayEntity>(100);
	}

	{
		positionInput = new Vector3dInput("Position", "Graphics", new Vector3d());
		positionInput.setUnits("m");
		this.addInput(positionInput, true);

		alignmentInput = new Vector3dInput("Alignment", "Graphics", new Vector3d());
		this.addInput(alignmentInput, true);

		sizeInput = new Vector3dInput("Size", "Graphics", new Vector3d(1.0d,1.0d,1.0d));
		sizeInput.setUnits("m");
		this.addInput(sizeInput, true);

		orientationInput = new Vector3dInput("Orientation", "Graphics", new Vector3d());
		orientationInput.setUnits("rad");
		this.addInput(orientationInput, true);

		regionInput = new EntityInput<Region>(Region.class, "Region", "Graphics", simulation.getDefaultRegion());
		this.addInput(regionInput, true);

		showToolTip = new BooleanInput("ToolTip", "Graphics", true);
		this.addInput(showToolTip, true);

		mouseNodesExtentInput = new DoubleInput("MouseNodesExtent", "Graphics", 0.05d);
		mouseNodesExtentInput.setUnits("m");
		this.addInput(mouseNodesExtentInput, true);

		relativeEntity = new EntityInput<DisplayEntity>(DisplayEntity.class, "RelativeEntity", "Graphics", null);
		this.addInput(relativeEntity, true);

		displayModelList = new EntityListInput<DisplayModel>( DisplayModel.class, "DisplayModel", "Graphics", null);
		this.addInput(displayModelList, true);
		displayModelList.setUnique(false);

		levelOfDetail = new DoubleListInput("LevelOfDetail", "Graphics", null);
		levelOfDetail.setValidRange( 0.0d, Double.POSITIVE_INFINITY );
		this.addInput(levelOfDetail, true);

		show = new BooleanInput("Show", "Graphics", true);
		this.addInput(show, true);

		active = new BooleanInput("Active", "Graphics", true);
		this.addInput(active, true);

		movable = new BooleanInput("Movable", "Graphics", true);
		this.addInput(movable, true);
	}

	/**
	 * Constructor: initializing the DisplayEntity's graphics
	 */
	public DisplayEntity() {
		super();

		allInstances.add(this);
		// Create a branchgroup and make it detachable.
		branchGroup = new BranchGroup();
		branchGroup.setCapability( BranchGroup.ALLOW_DETACH );
		branchGroup.setCapability( BranchGroup.ENABLE_PICK_REPORTING );
		// Hold a reference to the DisplayEntity in the topmost branchgroup to
		// speed up picking lookups
		branchGroup.setUserData(this);

		// Initialize the root transform to be the identity
		rootTransform = new TransformGroup();
		rootTransform.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		rootTransform.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );
		rootTransform.setCapability( TransformGroup.ALLOW_CHILDREN_EXTEND );
		rootTransform.setCapability( TransformGroup.ALLOW_CHILDREN_READ );
		branchGroup.addChild( rootTransform );

		// Initialize the root transform to be the identity
		scaleTransform = new TransformGroup();
		scaleTransform.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
		scaleTransform.setCapability( TransformGroup.ALLOW_CHILDREN_WRITE );
		scaleTransform.setCapability( TransformGroup.ALLOW_CHILDREN_EXTEND );
		rootTransform.addChild( scaleTransform );

		displayTrack = new Vector( 1, 1 );
		displayDistanceAlongTrack = 0.0;
		displayTrackDistanceList = new DoubleVector( 1, 1 );
		displayTrackOrientationList = new Vector( 1, 1 );  // Vector of Vector3d
		displayTrackTotalDistance = 0.0;
		displaySpeedAlongTrack = 0.0;
		timeOfLastMovement = 0.0;

		// create a placeholder of the model
		displayNode = new OrderedGroup();
		displayNode.setCapability( BranchGroup.ALLOW_CHILDREN_WRITE );
		displayNode.setCapability( BranchGroup.ALLOW_CHILDREN_EXTEND );
		displayNode.setCapability( BranchGroup.ALLOW_CHILDREN_READ );
		scaleTransform.addChild( displayNode );

		mouseNodes = new ArrayList<MouseNode>();
		setMouseNodesSize(0.050);

		displayModels = new ArrayList<DisplayModelBG>();

		DisplayModel dm = this.getDefaultDisplayModel();
		if(dm != null) {
			ArrayList<DisplayModel> defList = new ArrayList<DisplayModel>();
			defList.add(dm);
			displayModelList.setDefaultValue(defList);
		}
	}

	public static ArrayList<? extends DisplayEntity> getAll() {
		return allInstances;
	}

	/**
	 * Sets the Graphic Simulation object for the entity.
	 * @param newSimulation - states the new simulation the entity is controled by
	 */
	public static void setSimulation( GraphicSimulation newSimulation ) {
		simulation = newSimulation;
	}

	/**
	 * Removes the entity from its current region and assigns a new region
	 * @param newRegion - the region the entity will be assigned to
	 */
	public void setRegion( Region newRegion ) {
		exitRegion();
		currentRegion = newRegion;
	}

	/**
	 * Visually adds the entity to its currently assigned region
	 */
	public void enterRegion() {
		if( currentRegion != null ) {
			currentRegion.addEntity( this );
		}
		else {
			throw new RuntimeException( "Region not set" );
		}

		if (!this.getShow()) {
			exitRegion();
		}

	}

	/**
	 * Removes the entity from its current region and visually adds the entity to the specified region.
	 * @param newRegion - the region the entity will be assigned to
	 */
	public void enterRegion( Region newRegion ) {
		setRegion( newRegion );
		enterRegion();
	}

	/**
	 * Visually removes the entity from its region.  The current region for the entity is maintained.
	 */
	public void exitRegion() {
		if( currentRegion != null ) {
			currentRegion.removeEntity( this );
		}
	}

	private void duplicate() {
		ObjectType type = ObjectType.getFor(this.getClass());

		if(type == null)
			return;

		DisplayEntity copiedEntity = (DisplayEntity)type.getNewInstance();

		// Unique name
		int i = 1;
		String name = String.format("Copy_of_%s", getInputName());
		while (Simulation.getNamedEntity(name) != null) {
			name = String.format("Copy%d_of_%s", i, getInputName());
			i++;
		}

		copiedEntity.setInputName(name);
		copiedEntity.setName(name);

		// Match all the inputs
		for(Input<?> each: this.getEditableInputs() ){
			String val = each.getValueString();
			if (val.isEmpty())
				continue;

			Input<?> copiedInput = copiedEntity.getInput(each.getKeyword());
			InputAgent.processEntity_Keyword_Value(copiedEntity, copiedInput, val);
		}
		Vector3d pos = copiedEntity.getPosition();
		Vector3d offset = copiedEntity.getSize();
		offset.scale(0.5);
		offset.setZ(0);
		pos.add(offset);
		copiedEntity.setPosition(pos);

		copiedEntity.initializeGraphics();
		copiedEntity.enterRegion();
		FrameBox.setSelectedEntity(copiedEntity);
	}

	private void addLabel() {
		ObjectType type = ObjectType.getFor(TextLabel.class);

		if(type == null)
			return;

		TextLabel label = (TextLabel)type.getNewInstance();

		// Unique name
		int i = 1;
		String name = String.format("Label_for_%s", getInputName());
		while (Simulation.getNamedEntity(name) != null) {
			name = String.format("Label%d_of_%s", i, getInputName());
			i++;
		}
		label.setName(name);
		label.setInputName(name);

		EditBox.processEntity_Keyword_Value(label, "RelativeEntity", this.getInputName() );
		EditBox.processEntity_Keyword_Value(label, "Position", "1.0, -1.0, 0.0" );
		EditBox.processEntity_Keyword_Value(label, "Region", currentRegion.getInputName() );
		EditBox.processEntity_Keyword_Value(label, "Text", this.getName());
		label.initializeGraphics();
		label.enterRegion();
		FrameBox.setSelectedEntity(label);
	}

	/**
	 * Destroys the branchGroup hierarchy for the entity
	 */
	public void kill() {
		super.kill();

		for (MouseNode each : mouseNodes) {
			each.kill();
		}

		allInstances.remove(this);
		exitRegion();

		if(OrbitBehavior.selectedEntity == this)
			OrbitBehavior.selectedEntity = null;

		currentRegion = null;

		clearModel();

		displayTrack = null;
		displayDistanceAlongTrack = 0.0;
		displayTrackDistanceList = null;
		displayTrackOrientationList = null;
		displayTrackTotalDistance = 0.0;
		displaySpeedAlongTrack = 0.0;
	}

	public void render(double time) {
		synchronized (position) {
			if (animated)
				this.animate(time);

			if (!needsRender && relativeEntity.getValue() == null)
				return;

			Transform3D temp = new Transform3D();
			temp.setEuler(orient);

			Vector3d offset = new Vector3d(size.x * align.x, size.y * align.y, size.z * align.z);
			temp.transform(offset);
			offset.sub(position, offset);
			offset.add(getOffsetToRelativeEntity());

			temp.setTranslation(offset);
			rootTransform.setTransform(temp);

			temp.setIdentity();
			temp.setScale(scale);
			scaleTransform.setTransform(temp);

			if( !graphicsSetup ) {
				this.setupGraphics();
				graphicsSetup = true;
			}

			if (showResize) {
				if( mouseNodes.size() > 0 ) {
					nodesOn();
				}
				else {
					if (rotateSizeBounds == null)
						makeResizeBounds();
					updateResizeBounds(size.x, size.y, size.z);
				}
			}
			else {
				if( mouseNodes.size() > 0 ) {
					nodesOff();
				}
				else {
					if (rotateSizeBounds != null) {
						rootTransform.removeChild(rotateSizeBounds);
						rotateSizeBounds = null;
					}
				}
			}

			for (DisplayModelBG each : displayModels) {
				each.setModelSize(size);
			}
			needsRender = false;
		}
	}

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
			needsRender = true;
		}
	}

	public void setAngle(double theta) {
		this.setOrientation(new Vector3d(0.0, 0.0, theta * Math.PI / 180.0));
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
		this.setAlignment(new Vector3d());
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

	public Vector getDisplayTrack() {
		return displayTrack;
	}

	public DoubleVector getDisplayTrackDistanceList() {
		return displayTrackDistanceList;
	}

	public Vector getDisplayTrackOrientationList() {
		return displayTrackOrientationList;
	}

	/**
	 * Method to set the display track of the entity
	 * @param track - the DisplayEntity's new track
	 */
	public void setDisplayTrack( Vector track ) {
		displayTrack = track;
	}

	/**
	 * Method to set the display track distances of the entity
	 * @param distances - the DisplayEntity's new track distances
	 */
	public void setDisplayTrackDistanceList( DoubleVector distances ) {
		displayTrackDistanceList = distances;
		displayTrackTotalDistance = distances.sum();
	}

	/**
	 * Method to set the display track orientations of the entity
	 * @param distances - the DisplayEntity's new track orientations
	 */
	public void setDisplayTrackOrientationList( Vector orientations ) {
		displayTrackOrientationList = orientations;
	}

	/**
	 * Method to set the display distance along track of the entity
	 * @param d - the DisplayEntity's new display distance along track
	 */
	public void setDisplayDistanceAlongTrack( double d ) {
		// this.trace( "setDisplayDistanceAlongTrack( "+d+" )" );
		displayDistanceAlongTrack = d;
	}

	/**
	 * Method to set the display distance along track of the entity
	 * @param d - the DisplayEntity's new display distance along track
	 */
	public void setDisplayTrackTotalDistance( double d ) {
		// this.trace( "setDisplayTrackTotalDistance( "+d+" )" );
		displayTrackTotalDistance = d;
	}

	/**
	 * Method to set the display speed of the entity
	 * @param s - the DisplayEntity's new display speed
	 */
	public void setDisplaySpeedAlongTrack( double s ) {

		// If the display speed is the same, do nothing
		if( s == displaySpeedAlongTrack ) {
			return;
		}

		// Was the object previously moving?
		if( displaySpeedAlongTrack > 0 ) {
			// Determine the time since the last movement for the entity
			double dt = getCurrentTime() - timeOfLastMovement;

			// Update the displayDistanceAlongTrack
			displayDistanceAlongTrack += ( displaySpeedAlongTrack * dt );
		}

		displaySpeedAlongTrack = s;
		timeOfLastMovement = getCurrentTime();

		if (s == 0.0)
			animated = false;
		else
			animated = true;
	}

	/**
	 *	Returns the display distance for the DisplayEntity
	 *	@return double - display distance for the DisplayEntity
	 **/
	public double getDisplayDistanceAlongTrack() {
		return displayDistanceAlongTrack;
	}

	public double getDisplayTrackTotalDistance() {
		return displayTrackTotalDistance;
	}

	public void setSize(Vector3d size) {
		synchronized (position) {
			this.size.set(size);
			needsRender = true;
		}

		this.setScale(size.x, size.y, size.z);
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

	public void setScale( double x, double y, double z ) {
		synchronized (position) {
			scale.set( x, y, z );
			needsRender = true;
		}
	}

	public Vector3d getScale() {
		synchronized (position) {
			return new Vector3d(scale);
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
			needsRender = true;
		}
	}

	public void setPosition(Vector3d pos) {
		synchronized (position) {
			position.set(pos);
			needsRender = true;
		}
	}

	/**
	 * Accessor to return the branchGroup of the display entity.
	 * @return BranchGroup - the attachment point for the DisplayEntity's graphics
	 */
	public BranchGroup getBranchGroup() {
		return branchGroup;
	}

	/**
	 Returns a vector of strings describing the DisplayEntity.
	 Override to add details
	 @return Vector - tab delimited strings describing the DisplayEntity
	 **/
	public Vector getInfo() {
		Vector info = super.getInfo();
		if( getCurrentRegion() != null )
			info.addElement( "Region" + "\t" + getCurrentRegion().getName() );
		else
			info.addElement( "Region\t<no region>" );
		return info;
	}

	private void updateResizeBounds(double x, double y, double z) {
		double radius = 0.10 * Math.min(x, y);

		((Circle)rotateSizeBounds.getChild(0)).setCenterRadius(-x, 0.0, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(1)).setCenterRadius(-x/2, y/2, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(2)).setCenterRadius(x/2, y/2, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(3)).setCenterRadius(0.0, y/2, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(4)).setCenterRadius( x/2, 0.0, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(5)).setCenterRadius(-x/2, 0.0, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(6)).setCenterRadius(-x/2, -y/2, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(7)).setCenterRadius( x/2, -y/2, 0.0, radius);
		((Circle)rotateSizeBounds.getChild(8)).setCenterRadius(0.0, -y/2, 0.0, radius);
		resizeLine.setCoordinate(1, new Point3d(-x, 0.0d, 0.0d));
		((Cube)rotateSizeBounds.getChild(10)).setSize(x, y, z);
	}

	private void makeResizeBounds() {
		rotateSizeBounds = new BranchGroup();
		rotateSizeBounds.setCapability(BranchGroup.ALLOW_DETACH);

		resizeLine = new LineArray(2, LineArray.COORDINATES);
		resizeLine.setCapability(LineArray.ALLOW_COORDINATE_WRITE);

		resizeLine.setCoordinate(0, new Point3d(0.0, 0.0, 0.0));
		resizeLine.setCoordinate(1, new Point3d(1.0, 0.0, 0.0));

		for (int i = 0; i < 9; i++) {
			Circle temp = new Circle(0.0, 0.0, 0.0, 0.1, 36, Circle.SHAPE_FILLED);
			temp.setColor(Shape.getColorWithName("darkgreen"));
			rotateSizeBounds.addChild(temp);
		}

		rotateSizeBounds.addChild(new Shape3D(resizeLine, axisLineAppearance));

		Cube boundsModel = new Cube(Cube.SHAPE_OUTLINE, "boundsModel");
		boundsModel.setColor(Shape.getColorWithName("mint"));
		rotateSizeBounds.addChild(boundsModel);
		rotateSizeBounds.compile();

		rootTransform.addChild(rotateSizeBounds);
	}

	public void setResizeBounds( boolean bool ) {
		synchronized (position) {
			showResize = bool;
			needsRender = true;
		}
	}


	/**
	 *  Accessor method to obtain the display model for the DisplayEntity
	 *  created: Feb 21, 2003 by JM
	 *  To change the model for the DisplayEntity, obtain the refence to the model using this method
	 *  then, add desired graphics to this Group.
	 *  @return the BranchGroup for the DisplayEntity that the display model can be added to.
	 */
	public OrderedGroup getModel() {
		return displayNode;
	}

	/**
	 * Adds a shape to the shapelist and to the DisplayEntity Java3D hierarchy.
	 * Takes into account the desired layer for the added shape.
	 *
	 * @param shape the shape to add.
	 */
	public void addShape( Shape shape ) {
		synchronized (getModel()) {
			// Make sure we don't get added twice, try to remove it first in case
			getModel().removeChild(shape);
			getModel().addChild(shape);
		}
	}

	/**
	 * Adds a mouseNode at the given position
	 * Empty in DisplayEntity -- must be overloaded for entities which can have mouseNodes such as RouteSegment and Path
	 * @param posn
	 */
	public void addMouseNodeAt( Vector3d posn ) {

	}

	/**
	 * Remove a shape from the shapelist and the java3d model. Return if the
	 * shape is not on the list.
	 *
	 * @param shape the shape to be removed.
	 */
	public void removeShape( Shape shape ) {
		synchronized (getModel()) {
			getModel().removeChild(shape);
		}
	}

	/** removes a components from the displayModel, clearing the graphics **/
	public void clearModel() {
		synchronized (position) {
			// remove all of the submodels from the model
			getModel().removeAllChildren();

			if(displayModels.size() != 0){

				DoubleVector distances;
				distances = new DoubleVector(0);
				if(levelOfDetail.getValue() != null){
					distances = levelOfDetail.getValue();
				}

				// LOD is defined
				if(distances.size() > 0){
					Enumeration<?> children = rootTransform.getAllChildren();
					while(children.hasMoreElements()){
						Node child = (Node) children.nextElement();
						if(child.getName() != null && child.getName().equalsIgnoreCase("lodTransformGroup")) {
							rootTransform.removeChild(child);
							for(DisplayModelBG each: displayModels){
								each.removeAllChildren();
							}
						}
					}
				}

				// One display model
				else{
					rootTransform.removeChild(displayModels.get(0));
					displayModels.get(0).removeAllChildren();
				}
				displayModels.clear();
			}
		}
	}

	void addScaledBG(BranchGroup node) {
		synchronized (scaleTransform) {
			scaleTransform.addChild(node);
		}
	}

	void removeScaledBG(BranchGroup node) {
		synchronized (scaleTransform) {
			scaleTransform.removeChild(node);
		}
	}

	/**
	 *	Allows overridding to add menu items for user menus
	 *   @param menu - the Popup menu to add items to
	 *	@param xLoc - the x location of the mouse when the mouse event occurred
	 *	@param yLoc - the y location of the mouse when the mouse event occurred
	 **/
	public void addMenuItems( JPopupMenu menu, final int xLoc, final int yLoc ) {

		JMenuItem input = new JMenuItem("Input Editor");
		input.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				EditBox.getInstance().setVisible(true);
				EditBox.getInstance().setExtendedState(JFrame.NORMAL);
				FrameBox.setSelectedEntity(DisplayEntity.this);
			}
		});
		menu.add(input);

		JMenuItem prop = new JMenuItem("Property Viewer");
		prop.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				PropertyBox.getInstance().setVisible(true);
				PropertyBox.getInstance().setExtendedState(JFrame.NORMAL);
				FrameBox.setSelectedEntity(DisplayEntity.this);
			}
		});
		menu.add(prop);

		JMenuItem output = new JMenuItem("Output Viewer");
		output.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				InfoBox.getInstance().setVisible(true);
				InfoBox.getInstance().setExtendedState(JFrame.NORMAL);
				FrameBox.setSelectedEntity(DisplayEntity.this);
			}
		});
		menu.add(output);

		if (!this.testFlag(Entity.FLAG_GENERATED)) {
			JMenuItem copy = new JMenuItem( "Copy" );
			copy.addActionListener( new ActionListener() {

				public void actionPerformed( ActionEvent event ) {
					duplicate();
				}
			} );

			// Only if this object is selected
			if(OrbitBehavior.selectedEntity == this)
				menu.add( copy );
		}

		JMenuItem delete = new JMenuItem( "Delete" );
		delete.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.this.kill();
				FrameBox.setSelectedEntity(null);
			}
		} );

		// Only if this object is selected
		if(OrbitBehavior.selectedEntity == this)
			menu.add( delete );

		JMenuItem displayModel = new JMenuItem( "Change Graphics" );
		displayModel.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {

				// More than one DisplayModel(LOD) or No DisplayModel
				if(displayModels.size() > 1 || displayModelList.getValue() == null)
					return;

				GraphicBox graphicBox = GraphicBox.getInstance( DisplayEntity.this, xLoc, yLoc );
				graphicBox.setVisible( true );
			}
		} );
		menu.add( displayModel );
		JMenuItem addLabel = new JMenuItem( "Add Label" );
		addLabel.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.this.addLabel();
			}
		} );
		menu.add( addLabel );
	}

	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		if( "Label".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword Label no longer has any effect");
			return;
		}
		if( "LABELBOTTOMLEFT".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword LABELBOTTOMLEFT no longer has any effect");
			return;
		}
		if( "TextHeight".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword TextHeight no longer has any effect");
			return;
		}
		if( "FontName".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword FontName no longer has any effect");
			return;
		}
		if( "FontStyle".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword FontStyle no longer has any effect");
			return;
		}
		if ("FontColour".equalsIgnoreCase(keyword) ||
		    "FontColor".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword FontColour no longer has any effect");
			return;
		}
		if( "Angle".equalsIgnoreCase( keyword ) ) {
			InputAgent.logWarning("The keyword Angle no longer has any effect");
			return;
		}

		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	/**
	 * Return the orientation for the specified distance along the display track
	 */
	public Vector3d getDisplayTrackOrientationForDistance( double distance ) {

		// If there is no orientation, return no orientation
		if( displayTrackOrientationList.size() == 0 ) {
			return ( new Vector3d( 0.0, 0.0, 0.0 ) );
		}

		// If there is only one orientation, return it
		if( displayTrackOrientationList.size() == 1 ) {
			return (Vector3d)displayTrackOrientationList.get( 0 );
		}

		distance = Math.max( distance, 0.0 );
		distance = Math.min( distance, displayTrackTotalDistance );

		// Determine the line for the position
		double totalLength = 0.0;
		for( int i = 0; i < displayTrackDistanceList.size(); i++ ) {
			totalLength += displayTrackDistanceList.get( i );
			if( distance <= totalLength ) {
				return (Vector3d)displayTrackOrientationList.get( i );
			}
		}

		throw new ErrorException( "Could not find orientation for obect " + this.getName() );
	}

	/**
	 * Return the screen coordinates for the specified distance along the display track
	 */
	public Vector3d getDisplayTrackCoordinatesForDistance( double distance ) {

		double ratio;
		Vector3d p1 = (Vector3d)displayTrack.get( 0 );
		Vector3d p2 = (Vector3d)displayTrack.lastElement();

		// Are the inputs valid for calculation?
		//if( distance - displayTrackTotalDistance > 0.2 ) {
		//	throw new ErrorException( this.getName() + " Specified distance must be less than or equal to the track distance. " + distance + " / " + displayTrackTotalDistance );
		//}
		if( distance < -0.2 ) {
			throw new ErrorException( " The specified distance must be positive. " );
		}

		//  Find the ratio of the track length to the specified distance
		if( displayTrackTotalDistance == 0.0 ) {
			ratio = 1.0;
		}
		else {
			distance = Math.max( distance, 0.0 );
			distance = Math.min( distance, displayTrackTotalDistance );
			ratio = (distance / displayTrackTotalDistance);
		}

		// Are there bends in the segment?
		if( displayTrack.size() > 2 ) {

			// Determine the line for the position and the fraction of the way through the line
			double totalLength = 0.0;
			double distanceInLine;
			for( int i = 0; i < displayTrackDistanceList.size(); i++ ) {
				distanceInLine = distance - totalLength;
				totalLength += displayTrackDistanceList.get( i );
				if( distance <= totalLength ) {
					p1 = (Vector3d)displayTrack.get( i );
					p2 = (Vector3d)displayTrack.get( i + 1 );
					if( displayTrackDistanceList.get( i ) > 0 ) {
						ratio = distanceInLine / displayTrackDistanceList.get( i );
					}
					else {
						ratio = 0.0;
					}
					break;
				}
			}
		}

		//  Calculate the resulting point
		Vector3d vec = new Vector3d();
		vec.sub(p2, p1);
		vec.scale(ratio);
		vec.add(p1);
		return vec;
	}

	private void animate(double t) {
		//this.trace( "updateMovementAtTime( " + t + " )" );

		// Determine the display speed
		double s = this.displaySpeedAlongTrack;
		if( s == 0.0 ) {
			return;
		}
		//this.trace( "Display Speed Along Track (SCREEN UNITS/h)= "+s );

		// Determine the time since the last movement for the entity
		double dt = t - timeOfLastMovement;
		if( dt <= 0.0 ) {
			return;
		}
		//this.trace( "Time of last movement = "+ this.getTimeOfLastMovement() );
		//this.trace( "Time For this update (dt) = "+dt );

		// Find the display distance moved since the last update
		double distance = this.getDisplayDistanceAlongTrack() + ( dt * s );

		//this.trace( "distance travelled during this update (SCREEN UNITS) = "+distance );

		this.setDisplayDistanceAlongTrack( distance );

		// Set the new location
		Vector3d loc = this.getDisplayTrackCoordinatesForDistance( distance );
		this.setPosition(loc);
		this.setAlignment(new Vector3d());
		timeOfLastMovement = t;
		this.setOrientation(this.getDisplayTrackOrientationForDistance(distance));
	}

	/**
	 * Move the DisplayEntity to the given destination over the given time.
	 * If a non-empty path is specified, then the DisplayEntity will follow the path.
	 * Otherwise, it will follow a straight line to the destination.
	 */
	public void moveToPosition_FollowingVectors3d_DuringTime_WithChangingOrientation( Vector3d destination, Vector path, double t, boolean orientationChanges ) {

		// Create a list of all the points to be followed
		Vector pts = new Vector();

		pts.addElement(this.getPosition());
		pts.addAllLast( path );
		pts.addElement( destination );

		// Store the new track and track distances
		Vector newDisplayTrack = new Vector( 1, 1 );
		DoubleVector newDisplayTrackDistanceList = new DoubleVector( 1, 1 );
		Vector newDisplayTrackOrientationList = new Vector( 1, 1 );
		double dx, dy, dz;
		Vector3d p1, p2;
		Vector3d prevPt, nextPt;

		// Add the present location to the display track
		newDisplayTrack.addElement(this.getPosition());

		for( int i = 0; i < (pts.size() - 1); i++ ) {
			prevPt = (Vector3d)newDisplayTrack.get( i );

			// Determine the offset to the next point
			p1 = (Vector3d)pts.get( i );
			p2 = (Vector3d)pts.get( i + 1 );
			dx = p2.x - p1.x;
			dy = p2.y - p1.y;
			dz = p2.z - p1.z;

			// Add the next point to the display track
			nextPt = new Vector3d( prevPt.x + dx, prevPt.y + dy, prevPt.z + dz );
			newDisplayTrack.addElement( nextPt );

			// Add the next distance to the display track distances
			Vector3d vec = new Vector3d();
			vec.sub( nextPt, prevPt );
			double distance = vec.length();
			newDisplayTrackDistanceList.add(distance);
			if( orientationChanges ) {
				newDisplayTrackOrientationList.addElement( new Vector3d( 0.0, -1.0 * Math.atan2( dz, Math.hypot( dx, dy ) ), Math.atan2( dy, dx ) ) );
			}
			else {
				newDisplayTrackOrientationList.addElement(this.getOrientation());
			}
		}

		// Set the new track and distances
		this.setDisplayTrack( newDisplayTrack );
		this.setDisplayTrackDistanceList( newDisplayTrackDistanceList );
		displayTrackTotalDistance = newDisplayTrackDistanceList.sum();
		this.setDisplayTrackOrientationList( newDisplayTrackOrientationList );

		// Set the distance along track
		displayDistanceAlongTrack = 0.0;

		// Set the display speed to the destination (in Java screen units per hour)
		this.setDisplaySpeedAlongTrack( displayTrackTotalDistance / t );

		// Give this event slightly higher priority in case the entity exits region after the wait.
		// We want to resume this method first so that we can set the origin of the entity on its present region before it exits region.
		scheduleWait( t, 4 );

		this.setDisplaySpeedAlongTrack( 0.0 );
		this.setPosition(destination);
	}

	/**
	 * Move the DisplayEntity to the given destination over the given time.
	 * If a non-empty path is specified, then the DisplayEntity will follow the path and the given orientations.
	 * Otherwise, it will follow a straight line to the destination.
	 */
	public void moveToPosition_FollowingVectors3d_DuringTime_WithOrientations( Vector3d destination, Vector path, double t, DoubleVector orientations ) {

		// Create a list of all the points to be followed
		Vector pts = new Vector();

		pts.addElement( this.getPositionForAlignment(new Vector3d()) );
		pts.addAllLast( path );
		pts.addElement( destination );

		// Store the new track and track distances
		Vector newDisplayTrack = new Vector( 1, 1 );
		DoubleVector newDisplayTrackDistanceList = new DoubleVector( 1, 1 );
		Vector newDisplayTrackOrientationList = new Vector( 1, 1 );
		double dx, dy;
		Vector3d p1, p2;
		Vector3d prevPt, nextPt;

		// Add the present location to the display track
		newDisplayTrack.addElement(this.getPositionForAlignment(new Vector3d()));

		for( int i = 0; i < (pts.size() - 1); i++ ) {
			prevPt = (Vector3d)newDisplayTrack.get( i );

			// Determine the offset to the next point
			p1 = (Vector3d)pts.get( i );
			p2 = (Vector3d)pts.get( i + 1 );
			dx = p2.x - p1.x;
			dy = p2.y - p1.y;

			// Add the next point to the display track
			nextPt = new Vector3d( prevPt.x + dx, prevPt.y + dy, 0.0 );
			newDisplayTrack.addElement( nextPt );

			// Add the next distance to the display track distances
			Vector3d vec = new Vector3d();
			vec.sub( nextPt, prevPt );
			double distance = vec.length();
			newDisplayTrackDistanceList.add(distance);
			if( i == 0 ) {
				newDisplayTrackOrientationList.addElement( new Vector3d( 0, 0, orientations.get( 0 ) * Math.PI / 180.0 ) );
			}
			else {
				if( i == pts.size() - 2 ) {
					newDisplayTrackOrientationList.addElement( new Vector3d( 0, 0, orientations.lastElement() * Math.PI / 180.0 ) );
				}
				else {
					newDisplayTrackOrientationList.addElement( new Vector3d( 0, 0, orientations.get( i-1 ) * Math.PI / 180.0 ) );
				}
			}
		}

		// Set the new track and distances
		this.setDisplayTrack( newDisplayTrack );
		this.setDisplayTrackDistanceList( newDisplayTrackDistanceList );
		displayTrackTotalDistance = newDisplayTrackDistanceList.sum();
		this.setDisplayTrackOrientationList( newDisplayTrackOrientationList );

		// Set the distance along track
		displayDistanceAlongTrack = 0.0;

		// Set the display speed to the destination (in Java screen units per hour)
		this.setDisplaySpeedAlongTrack( displayTrackTotalDistance / t );

		// Give this event slightly higher priority in case the entity exits region after the wait.
		// We want to resume this method first so that we can set the origin of the entity on its present region before it exits region.
		scheduleWait( t, 4 );

		this.setDisplaySpeedAlongTrack( 0.0 );
		this.setPosition(destination);
		this.setAlignment(new Vector3d());
	}

	public void updateGraphics() {}
	public void initializeGraphics() {}
	public void setupGraphics() {

		// No DisplayModel
		if(displayModelList.getValue() == null)
			return;

		clearModel();

		synchronized (position) {
			if(this.getDisplayModelList().getValue() != null){
				for(DisplayModel each: this.getDisplayModelList().getValue()){
					DisplayModelBG dm = each.getDisplayModel();
					dm.setModelSize(size);
					displayModels.add(dm);
				}
			}
			DoubleVector distances;
			distances = new DoubleVector(0);
			if(this.getLevelOfDetail() != null){
				distances = this.getLevelOfDetail();
			}

			// LOD is defined
			if(distances.size() > 0){
				Switch targetSwitch;
				DistanceLOD dLOD;
				float[] dists= new float[distances.size()];
				for(int i = 0; i < distances.size(); i++){
					dists[i] = (float)distances.get(i);
				}

				targetSwitch = new Switch();
				targetSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
				dLOD = new DistanceLOD(dists, new Point3f());
				BranchGroup lodBranchGroup = new BranchGroup();
				lodBranchGroup.setName("lodBranchGroup");

				// TODO: The bounding box should be big enough to include the camera. Otherwise, the model could not be seen
				BoundingSphere bounds = new BoundingSphere(new Point3d(0,0,0), Double.POSITIVE_INFINITY);
				for(DisplayModelBG each: displayModels){
					targetSwitch.addChild(each);
				}
				dLOD.addSwitch(targetSwitch);
				dLOD.setSchedulingBounds(bounds);

				lodBranchGroup.addChild(dLOD);
				lodBranchGroup.addChild(targetSwitch);
				rootTransform.addChild(lodBranchGroup);
			}

			// One display model
			else if (displayModels.size() > 0){
				rootTransform.addChild(displayModels.get(0));
			}
		}

		// Moving DisplayEntity with label (i.e. Truck and Train) is creating new DisplayEntity for its label when
		// enterRegion(). This causes the ConcurrentModificationException to the caller of this method if it
		// iterates through DisplayModel.getAll(). This is the place for the DisplayEntity to enterRegion though
		if(this.getClass() == DisplayEntity.class){
			enterRegion();
		}

	}

	public DisplayModel getDefaultDisplayModel(){
		return DisplayModel.getDefaultDisplayModelForClass(this.getClass());
	}

	public  EntityListInput<DisplayModel> getDisplayModelList() {
		return displayModelList;
	}

	public DoubleVector getLevelOfDetail() {
		return levelOfDetail.getValue();
	}

	/** Callback method so that the UI can inform a DisplayEntity it is about to be dragged by the user.<br>
	 *   Called by the user interface to allow the DisplayEntity to setup properties or UI before it is moved
	 *   in the user interface.
	 **/
	public void preDrag() {}

	/** Callback method so that the UI can inform a DisplayEntity that dragged by the user is complete.<br>
	 *   Called by the user interface to allow the DisplayEntity to setup properties or UI after it has been moved
	 *   in the user interface.
	 **/
	public void postDrag() {}

	public void dragged(Vector3d distance) {
		Vector3d newPos = this.getPosition();
		newPos.add(distance);
		this.setPosition(newPos);

		// update mouseNode positions
		for( int i = 0; i < this.getMouseNodes().size(); i++ ) {
			MouseNode node = this.getMouseNodes().get(i);
			Vector3d nodePos = node.getPosition();
			nodePos.add(distance);
			node.setPosition(nodePos);

			node.enterRegion( this.getCurrentRegion() );

		}
		// inform simulation and editBox of new positions
		this.updateInputPosition();
	}

	/**
	 *  Inform simulation and editBox of new positions.
	 *  This method works for any DisplayEntity that uses the keyword "Center".
	 *  Any DisplayEntity that does not use the keyword "Center" must overwrite this method.
	 */
	public void updateInputPosition() {
		Vector3d vec = this.getPosition();
		InputAgent.processEntity_Keyword_Value(this, positionInput, String.format( "%.3f %.3f %.3f %s", vec.x, vec.y, vec.z, positionInput.getUnits() ));
		InputAgent.addEditedEntity(this);
		FrameBox.valueUpdate();
	}

	/**
	 *  Inform simulation and editBox of new size.
	 *  This method works for any DisplayEntity that sets size via the keyword "Extent".
	 *  Any DisplayEntity that does not set size via the keyword "Extent" must overwrite this method.
	 */
	public void updateInputSize() {
		Vector3d vec = this.getSize();
		InputAgent.processEntity_Keyword_Value(this, sizeInput, String.format( "%.3f %.3f %.3f %s", vec.x, vec.y, vec.z, sizeInput.getUnits() ));
		InputAgent.addEditedEntity(this);
		FrameBox.valueUpdate();
	}

	/**
	 *  Inform simulation and editBox of new angle.
	 *  This method works for any DisplayEntity that sets angle via the keyword "Orientation".
	 *  Any DisplayEntity that does not set angle via the keyword "Orientation" must overwrite this method.
	 */
	public void updateInputOrientation() {
		Vector3d vec = this.getOrientation();
		InputAgent.processEntity_Keyword_Value(this, orientationInput, String.format( "%.3f %.3f %.3f %s", vec.x, vec.y, vec.z, orientationInput.getUnits() ));
		InputAgent.addEditedEntity(this);
		FrameBox.valueUpdate();
	}

	public void updateInputAlignment() {
		Vector3d vec = this.getAlignment();
		InputAgent.processEntity_Keyword_Value(this, alignmentInput, String.format( "%.3f %.3f %.3f", vec.x, vec.y, vec.z ));
		InputAgent.addEditedEntity(this);
		FrameBox.valueUpdate();
	}

	public ArrayList<MouseNode> getMouseNodes() {
		return mouseNodes;
	}

	public void killMouseNodes() {
		for( int i = this.getMouseNodes().size() - 1; i >= 0; i-- ) {
			MouseNode node = this.getMouseNodes().get( i );
			this.getMouseNodes().remove( node );
			node.kill();
		}
	}

	public double getMouseNodesSize() {
		return mouseNodesSize;
	}

	public void setMouseNodesSize( double size ) {
		mouseNodesSize = size;
		Vector3d nodeSize = new Vector3d(size, size, size);
		for (MouseNode each : this.getMouseNodes()) {
			each.setSize(nodeSize);
		}
	}

	public void nodesOn() {
		for (MouseNode each : mouseNodes) {
			each.enterRegion(currentRegion);
		}
	}

	public void nodesOff() {
		for (MouseNode each : mouseNodes) {
			each.exitRegion();
		}
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

	/**
	 * Method to return the name of the entity. Used when building Edit tree labels.
	 * @return the unique identifier of the entity, <region>/<entityName>, unless region == null or the default region
	 */
	public String toString() {
		if(( currentRegion == simulation.getDefaultRegion() ) || (currentRegion == null) ) {
			return getName();
		}
		else {
			return (currentRegion.getName()+"/"+getName() );
		}
	}

	public boolean showToolTip() {
		return showToolTip.getValue();
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
	 * This method updates the DisplayEntity for changes in the given input
	 */
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
			this.enterRegion( regionInput.getValue() );
			return;
		}
		if( in == mouseNodesExtentInput ) {
			this.setMouseNodesSize( mouseNodesExtentInput.getValue() );
			return;
		}
	}
}
