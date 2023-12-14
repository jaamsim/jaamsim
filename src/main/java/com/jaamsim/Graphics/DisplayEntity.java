/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2023 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.BooleanProviders.BooleanProvInput;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.DisplayModels.ColladaModel;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.DisplayModels.PolylineModel;
import com.jaamsim.DisplayModels.ShapeModel;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.SubModels.CompoundEntity;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.ObserverEntity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.EntityListInput;
import com.jaamsim.input.EnumInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Output;
import com.jaamsim.input.RegionInput;
import com.jaamsim.input.RelativeEntityInput;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.input.Vec3dInput;
import com.jaamsim.input.Vec3dListInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jogamp.newt.event.KeyEvent;

/**
 * Encapsulates the methods and data needed to display a simulation object in the 3D environment.
 * Extends the basic functionality of entity in order to have access to the basic system
 * components like the eventManager.
 */
public class DisplayEntity extends Entity {

	@Keyword(description = "The location of the object in {x, y, z} coordinates.",
	         exampleList = {"-3.922 -1.830 0.000 m"})
	protected final Vec3dInput positionInput;

	@Keyword(description = "The point within the object that is located at the coordinates of "
	                     + "its Position input. Expressed with respect to a unit box centered "
	                     + "about { 0 0 0 }.",
	         exampleList = {"-0.5 -0.5 0.0"})
	protected final Vec3dInput alignmentInput;

	@Keyword(description = "The size of the object in {x, y, z} coordinates.",
	         exampleList = {"15 12 0 m"})
	protected final Vec3dInput sizeInput;

	@Keyword(description = "Euler angles defining the rotation of the object.",
	         exampleList = {"0 0 90 deg"})

	protected final Vec3dInput orientationInput;

	@Keyword(description = "A list of points in {x, y, z} coordinates that defines a polyline.",
	         exampleList = {"{ 1.0 1.0 0.0 m } { 2.0 2.0 0.0 m } { 3.0 3.0 0.0 m }",
	                       "{ 1.0 1.0 m } { 2.0 2.0 m } { 3.0 3.0 m }"})
	protected final Vec3dListInput pointsInput;

	@Keyword(description = "The type of curve interpolation used for line type entities.")
	protected final EnumInput<PolylineInfo.CurveType> curveTypeInput;

	@Keyword(description = "If a Region is specified, the Position and Orientation inputs for "
	                     + "the present object are relative to the Position and Orientation "
	                     + "of the specified Region. If the specified Region is moved or "
	                     + "rotated, the present object is moved to maintain its relative "
	                     + "position and orientation.",
	         exampleList = {"Region1"})
	protected final EntityInput<Region> regionInput;

	@Keyword(description = "If an object is specified, the Position input for the present object "
	                     + "is relative to the Position for the specified object. If the "
	                     + "specified object is moved, the present object is moved to maintain "
	                     + "its relative position.",
	         exampleList = {"DisplayEntity1"})
	protected final RelativeEntityInput relativeEntity;

	@Keyword(description = "The graphic representation of the object. If a list of DisplayModels "
	                     + "is entered, each one is displayed provided that its DrawRange "
	                     + "input is satisfied. This feature allows the object's appearance to "
	                     + "change with its distance from the View window's camera.")
	protected final EntityListInput<DisplayModel> displayModelListInput;

	@Keyword(description = "If TRUE, the object is displayed in the View windows.")
	private final BooleanProvInput showInput;

	@Keyword(description = "If TRUE, the object will respond to mouse clicks and can be "
	                     + "positioned by dragging with the mouse.")
	private final BooleanProvInput movable;

	@Keyword(description = "The view windows on which this entity will be visible.")
	private final EntityListInput<View> visibleViews;

	@Keyword(description = "The minimum and maximum distance from the camera for which this "
	                     + "entity is displayed.",
	         exampleList = {"0 100 m"})
	private final ValueListInput drawRange;

	private final Vec3d position = new Vec3d();
	private final ArrayList<Vec3d> points = new ArrayList<>();
	private final Vec3d size = new Vec3d(1.0d, 1.0d, 1.0d);
	private final Vec3d orient = new Vec3d();
	private final Vec3d align = new Vec3d();
	private final ArrayList<DisplayModel> displayModelList = new ArrayList<>();
	private boolean show;

	private Region currentRegion;

	private ArrayList<DisplayModelBinding> modelBindings;
	private VisibilityInfo visInfo = null;

	private final HashMap<String, Tag> tagMap = new HashMap<>();

	private static final ArrayList<Vec3d> defPoints =  new ArrayList<>();
	private static final DoubleVector defRange = new DoubleVector(0.0d, Double.POSITIVE_INFINITY);
	static {
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
	}

	{
		positionInput = new Vec3dInput("Position", GRAPHICS, new Vec3d());
		positionInput.setUnitType(DistanceUnit.class);
		positionInput.setCallback(positionCallback);
		this.addInput(positionInput);

		alignmentInput = new Vec3dInput("Alignment", GRAPHICS, new Vec3d());
		alignmentInput.setCallback(alignmentCallback);
		this.addInput(alignmentInput);

		sizeInput = new Vec3dInput("Size", GRAPHICS, new Vec3d(1.0d, 1.0d, 1.0d));
		sizeInput.setUnitType(DistanceUnit.class);
		sizeInput.setValidRange(0.0d, Double.POSITIVE_INFINITY);
		sizeInput.setCallback(sizeCallback);
		this.addInput(sizeInput);

		orientationInput = new Vec3dInput("Orientation", GRAPHICS, new Vec3d());
		orientationInput.setUnitType(AngleUnit.class);
		orientationInput.setCallback(orientationCallback);
		this.addInput(orientationInput);

		pointsInput = new Vec3dListInput("Points", GRAPHICS, defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		pointsInput.setCallback(pointsCallback);
		this.addInput(pointsInput);

		curveTypeInput = new EnumInput<>(PolylineInfo.CurveType.class, "CurveType", GRAPHICS,
				PolylineInfo.CurveType.LINEAR);
		curveTypeInput.setCallback(curveTypeCallback);
		this.addInput(curveTypeInput);

		regionInput = new RegionInput("Region", GRAPHICS, null);
		regionInput.setCallback(regionCallback);
		this.addInput(regionInput);

		relativeEntity = new RelativeEntityInput("RelativeEntity", GRAPHICS, null);
		this.addInput(relativeEntity);

		displayModelListInput = new EntityListInput<>( DisplayModel.class, "DisplayModel", GRAPHICS, null);
		displayModelListInput.addValidClass(ColladaModel.class);
		displayModelListInput.addValidClass(ShapeModel.class);
		displayModelListInput.addValidClass(ImageModel.class);
		displayModelListInput.addInvalidClass(IconModel.class);
		displayModelListInput.setCallback(displayModelListCallback);
		this.addInput(displayModelListInput);
		displayModelListInput.setUnique(false);

		showInput = new BooleanProvInput("Show", GRAPHICS, true);
		showInput.setCallback(showCallback);
		this.addInput(showInput);

		movable = new BooleanProvInput("Movable", GRAPHICS, true);
		this.addInput(movable);

		visibleViews = new EntityListInput<>(View.class, "VisibleViews", GRAPHICS, null);
		visibleViews.setDefaultText("All Views");
		visibleViews.setCallback(updateRangeVisibilityCallback);
		this.addInput(visibleViews);

		drawRange = new ValueListInput("DrawRange", GRAPHICS, defRange);
		drawRange.setUnitType(DistanceUnit.class);
		drawRange.setValidCount(2);
		drawRange.setValidRange(0, Double.POSITIVE_INFINITY);
		drawRange.setCallback(updateRangeVisibilityCallback);
		this.addInput(drawRange);
	}

	/**
	 * Constructor: initializing the DisplayEntity's graphics
	 */
	public DisplayEntity() {

		ObjectType type = this.getObjectType();
		if (type == null)
			return;

		// Set the default DisplayModel
		displayModelListInput.setDefaultValue(type.getDefaultDisplayModel());
		this.setDisplayModelList(type.getDefaultDisplayModel());

		// Set the default size
		sizeInput.setDefaultValue(type.getDefaultSize());
		this.setSize(type.getDefaultSize());

		// Set the default Alignment
		alignmentInput.setDefaultValue(type.getDefaultAlignment());
		this.setAlignment(type.getDefaultAlignment());

		this.setShow(getShowInput());

		// Choose which set of keywords to show
		this.setGraphicsKeywords();
	}

	@Override
	public void setInputsForDragAndDrop() {

		// Determine whether the entity should sit on top of the x-y plane
		boolean alignBottom = true;
		ArrayList<DisplayModel> displayModels = displayModelListInput.getValue();
		if (displayModels != null && displayModels.size() > 0) {
			DisplayModel dm0 = displayModels.get(0);
			if (dm0 instanceof ShapeModel || dm0 instanceof ImageModel || dm0 instanceof TextModel )
				alignBottom = false;
		}

		if (this.usePointsInput() || alignmentInput.getHidden() || getSize().z == 0.0d) {
			alignBottom = false;
		}

		if (alignBottom)
			InputAgent.applyArgs(this, "Alignment", "0.0", "0.0", "-0.5");
	}

	static final InputCallback positionCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			Vec3dInput v3dinp = (Vec3dInput)inp;

			if (de.usePointsInput())
				return;
			de.setPosition(v3dinp.getValue());
		}
	};

	static final InputCallback pointsCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			Vec3dListInput v3dinp = (Vec3dListInput)inp;

			if (!de.usePointsInput())
				return;
			de.updateForPointsInput(v3dinp.getValue());
		}
	};

	static final InputCallback sizeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			Vec3dInput v3dinp = (Vec3dInput)inp;

			de.setSize(v3dinp.getValue());
		}
	};

	static final InputCallback orientationCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			Vec3dInput v3dinp = (Vec3dInput)inp;

			de.setOrientation(v3dinp.getValue());
		}
	};

	static final InputCallback alignmentCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			Vec3dInput v3dinp = (Vec3dInput)inp;

			de.setAlignment(v3dinp.getValue());
		}
	};

	static final InputCallback showCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			DisplayEntity de = (DisplayEntity)ent;
			de.setShow(de.getShowInput());
		}
	};

	static final InputCallback curveTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((DisplayEntity)ent).invalidateScreenPoints();
		}
	};

	static final InputCallback displayModelListCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((DisplayEntity)ent).displayModelCallback();
		}
	};

	void displayModelCallback() {
		boolean bool = usePointsInput();
		this.setDisplayModelList(displayModelListInput.getValue());
		setGraphicsKeywords();

		// Refresh the contents of the Input Editor
		GUIListener gui = getJaamSimModel().getGUIListener();
		if (gui != null && gui.isSelected(this) && usePointsInput() != bool)
			gui.updateInputEditor();
	}

	static final InputCallback regionCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((DisplayEntity)ent).updateRegionCallback();
		}
	};

	void updateRegionCallback() {
		this.setRegion(regionInput.getValue());
	}

	static final InputCallback updateRangeVisibilityCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			((DisplayEntity)ent).updateRangeVisibility();
		}
	};

	void updateRangeVisibility() {
		if (visibleViews.isDefault() && drawRange.isDefault()) {
			visInfo = null;
		}
		double minDist = drawRange.getValue().get(0);
		double maxDist = drawRange.getValue().get(1);
		// It's possible for the distance to be behind the camera, yet have the object visible (distance is to center)
		// So instead use negative infinity in place of zero to never cull when close to the camera.
		if (minDist == 0.0) {
			minDist = Double.NEGATIVE_INFINITY;
		}
		visInfo = new VisibilityInfo(visibleViews.getValue(), minDist, maxDist);
	}

	@Override
	public void validate()
	throws InputErrorException {
		super.validate();

		if (getDisplayModelList() != null) {
			for (DisplayModel dm : getDisplayModelList()) {
				if (!dm.canDisplayEntity(this)) {
					throw new InputErrorException("Invalid DisplayModel: %s for this object",
							dm.getName());
				}
			}
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		// Required for a pooled clone that was modified by a 'SetGraphics' object
		resetGraphics();
	}

	public void updateForPointsInput(ArrayList<Vec3d> pts) {
		setPoints(pts);

		// Set the position to the point half way between the first and last nodes
		Vec3d pos = new Vec3d(pts.get(0));
		pos.add3(pts.get(pts.size() - 1));
		pos.scale3(0.5d, pos);
		setPosition(pos);
	}

	/**
	 * Restores the initial appearance of this entity.
	 */
	public void resetGraphics() {

		// Normal objects
		if (!usePointsInput()) {
			setPosition(positionInput.getValue());
			pointsInput.reset();
		}

		// Polyline objects
		else {
			updateForPointsInput(pointsInput.getValue());
			positionInput.reset();
		}

		this.setSize(sizeInput.getValue());
		this.setAlignment(alignmentInput.getValue());
		this.setOrientation(orientationInput.getValue());
		this.setDisplayModelList(displayModelListInput.getValue());
		this.setRegion(regionInput.getValue());
		this.setShow(getShowInput());
	}

	public boolean isPositionNominal() {
		return position.equals3(positionInput.getValue());
	}

	public boolean isPointsNominal() {
		return points.equals(pointsInput.getValue());
	}

	public boolean isSizeNominal() {
		return size.equals3(sizeInput.getValue());
	}

	public boolean isAlignmentNominal() {
		return align.equals3(alignmentInput.getValue());
	}

	public boolean isOrientationNominal() {
		return orient.equals3(orientationInput.getValue());
	}

	public boolean isDisplayModelNominal() {
		return displayModelList.isEmpty() && displayModelListInput.getValue() == null
				|| displayModelList.equals(displayModelListInput.getValue());
	}

	public boolean isRegionNominal() {
		return currentRegion == regionInput.getValue();
	}

	public boolean isShowNominal() {
		return show == getShowInput();
	}

	public boolean isGraphicsNominal() {
		return isPositionNominal() && isPointsNominal() && isSizeNominal() && isAlignmentNominal()
				&& isOrientationNominal() && isDisplayModelNominal() && isRegionNominal()
				&& isShowNominal();
	}

	private void showStandardGraphicsKeywords(boolean bool) {
		positionInput.setHidden(!bool);
		sizeInput.setHidden(!bool);
		alignmentInput.setHidden(!bool);
		orientationInput.setHidden(!bool);
	}

	private void showPolylineGraphicsKeywords(boolean bool) {
		pointsInput.setHidden(!bool);
		curveTypeInput.setHidden(!bool);
	}

	public boolean usePointsInput() {
		ArrayList<DisplayModel> dmList = displayModelListInput.getValue();
		if (dmList == null || dmList.isEmpty())
			return false;
		return dmList.get(0) instanceof PolylineModel;
	}

	private void setGraphicsKeywords() {

		// No displaymodel
		if (this instanceof OverlayEntity || displayModelListInput.getValue() == null) {
			showStandardGraphicsKeywords(false);
			showPolylineGraphicsKeywords(false);
			regionInput.setHidden(true);
			relativeEntity.setHidden(true);
			showInput.setHidden(true);
			return;
		}

		// Polyline type displaymodel
		if (usePointsInput()) {
			showStandardGraphicsKeywords(false);
			showPolylineGraphicsKeywords(true);
			return;
		}

		// Standard displaymodel
		showStandardGraphicsKeywords(true);
		showPolylineGraphicsKeywords(false);
	}

	public Region getCurrentRegion() {
		synchronized (position) {
			return currentRegion;
		}
	}

	public void setRegion(Region newRegion) {
		synchronized (position) {
			currentRegion = newRegion;
		}
	}

	public Vec3d getPosition() {
		synchronized (position) {
			return new Vec3d(position);
		}
	}

	public void setPosition(Vec3d pos) {
		synchronized (position) {
			position.set3(pos);
		}
	}

	public Vec3d getSize() {
		synchronized (position) {
			return new Vec3d(size);
		}
	}

	public void setSize(Vec3d size) {
		synchronized (position) {
			this.size.set3(size);
		}
	}

	public Vec3d getOrientation() {
		synchronized (position) {
			return new Vec3d(orient);
		}
	}

	public void setOrientation(Vec3d orientation) {
		synchronized (position) {
			orient.set3(orientation);
		}
	}

	public Vec3d getAlignment() {
		synchronized (position) {
			return new Vec3d(align);
		}
	}

	public void setAlignment(Vec3d align) {
		synchronized (position) {
			this.align.set3(align);
		}
	}

	public final boolean getShowInput() {
		return getShowInput(0.0d);
	}

	public final boolean getShow() {
		return getShow(0.0d);
	}

	public boolean getShow(double simTime) {
		if (isPooled())
			return false;
		boolean ret;
		if (!showInput.isConstant()) {
			ret = getShowInput(simTime);
		}
		else {
			synchronized (position) {
				ret = show;
			}
		}

		if (getParent() instanceof CompoundEntity) {
			CompoundEntity sub = (CompoundEntity) getParent();
			ret = ret && (sub.isShowComponents(simTime) || getSimulation().isShowSubModels());
		}
		return ret;
	}

	public boolean getShowInput(double simTime) {
		return showInput.getNextBoolean(this, simTime);
	}

	public void setShow(boolean bool) {
		synchronized (position) {
			show = bool;
		}
	}

	public boolean isMovable() {
		return movable.getNextBoolean(this, 0.0d);
	}

	public DisplayEntity getRelativeEntity() {
		return relativeEntity.getValue();
	}

	public ArrayList<String> getRelativeEntityOptions() {
		return relativeEntity.getValidOptions(this);
	}

	public ArrayList<String> getRegionOptions() {
		return regionInput.getValidOptions(this);
	}

	public ArrayList<View> getVisibleViews() {
		return visibleViews.getValue();
	}

	/**
	 * Sets the orientation to the specified value relative its its normal orientation.
	 * @param relOrient - Euler angles for relative orientation
	 */
	public void setRelativeOrientation(Vec3d relOrient) {
		Quaternion rotQ = new Quaternion();
		rotQ.setEuler3(relOrient);
		setRelativeOrientation(rotQ);
	}

	public void setRelativeOrientation(Quaternion rotQ) {
		Quaternion q = new Quaternion();
		q.setEuler3(orientationInput.getValue());
		q.mult(rotQ, q);
		setOrientation(q.getEuler3());
	}

	/**
	 * Returns the entity's size in the global coordinate system after applying its orientation.
	 * @return global size
	 */
	public Vec3d getGlobalSize() {
		Vec3d ret = getSize();
		Vec3d xdir = new Vec3d(ret.x, 0.0d, 0.0d);
		Vec3d ydir = new Vec3d(0.0d, ret.y, 0.0d);
		Vec3d zdir = new Vec3d(0.0d, 0.0d, ret.z);

		Mat4d mat = new Mat4d();
		mat.setEuler3(orientationInput.getValue());
		xdir.mult3(mat, xdir);
		ydir.mult3(mat, ydir);
		zdir.mult3(mat, zdir);

		ret.x = Math.abs(xdir.x) + Math.abs(ydir.x) + Math.abs(zdir.x);
		ret.y = Math.abs(xdir.y) + Math.abs(ydir.y) + Math.abs(zdir.y);
		ret.z = Math.abs(xdir.z) + Math.abs(ydir.z) + Math.abs(zdir.z);
		return ret;
	}

	/**
	 * Update any internal stated needed by either renderer.
	 */
	public void updateGraphics(double simTime) {
	}

	private void calculateEulerRotation(Vec3d val, Vec3d euler) {
		Mat4d mat = new Mat4d();
		mat.setEuler3(euler);
		val.mult3(mat, val);
	}

	/**
	 * Returns the local coordinates corresponding to a specified position in the entity's
	 * internal coordinate system relative to its centre.
	 * @param pos - internal position
	 * @return local position
	 */
	public Vec3d getPositionForAlignment(Vec3d pos) {
		Vec3d temp = new Vec3d(pos);
		synchronized (position) {
			temp.sub3(align);
			temp.mul3(size);
			calculateEulerRotation(temp, orient);
			temp.add3(position);
		}
		return temp;
	}

	/**
	 * Returns the local coordinates for the centre of the entity.
	 * @return local coordinates for the entity's centre
	 */
	public Vec3d getCentre() {
		return getPositionForAlignment(new Vec3d());
	}

	/**
	 * Sets the position of the entity so that its centre is located at the specified position.
	 * @param pos - local coordinates for the entity's centre
	 */
	public void setCentre(Vec3d pos) {
		setPositionForAlignment(pos, new Vec3d());
	}

	/**
	 * Sets the position of the entity so that its specified alignment point is located at the
	 * specified position.
	 * @param pos - local coordinates
	 * @param algn - alignment point within the entity
	 */
	public void setPositionForAlignment(Vec3d pos, Vec3d algn) {
		Vec3d newPos = new Vec3d(pos);
		Vec3d temp = new Vec3d(algn);
		synchronized (position) {
			temp.sub3(align);
			temp.mul3(size);
			calculateEulerRotation(temp, orient);
			newPos.sub3(temp);
			position.set3(newPos);
		}
	}

	/**
	 * Returns the global coordinates for the given position in the entity's internal coordinate
	 * system, relative to its centre.
	 * @param pos - position in internal coordinates
	 * @return position in global coordinates
	 */
	public Vec3d getGlobalPositionForPosition(Vec3d pos) {
		Vec3d temp = new Vec3d(pos);
		synchronized (position) {
			Vec3d scaledAlign = new Vec3d(align);
			scaledAlign.mul3(size);
			temp.sub3(scaledAlign);
			calculateEulerRotation(temp, orient);
			temp.add3(getPosition());
			temp = getGlobalPosition(temp);
		}
		return temp;
	}

	/**
	 * Returns the transformation that converts a point in the entity's
	 * coordinates to the global coordinate system.
	 * <p>
	 * The entity's coordinate system is centred on the entity's alignment point
	 * and its axes are rotated by the entity's orientation angles. It is NOT
	 * scaled by the entity's size, so the coordinates still have units of
	 * metres. The effects of the RelativeEntity and Region inputs are included
	 * in the transformation.
	 * @return global coordinates for the point.
	 */
	public Transform getGlobalTrans() {
		return getGlobalTransForSize(size);
	}

	/**
	 * Returns the equivalent global transform for this entity as if 'sizeIn' where the actual
	 * size.
	 * @param sizeIn
	 */
	public Transform getGlobalTransForSize(Vec3d sizeIn) {
		// Okay, this math may be hard to follow, this is effectively merging two TRS transforms,
		// The first is a translation only transform from the alignment parameter
		// Then a transform is built up based on position and orientation
		// As size is a non-uniform scale it can not be represented by the jaamsim TRS Transform and therefore
		// not actually included in this result, except to adjust the alignment

		// Alignment transformations
		Vec3d temp = new Vec3d(sizeIn);
		temp.mul3(align);
		temp.scale3(-1.0d);
		Transform alignTrans = new Transform(temp);

		// Orientation transformation
		Quaternion rot = new Quaternion();
		rot.setEuler3(orient);
		Transform ret = new Transform(null, rot, 1);

		// Combine the alignment and orientation transformations
		ret.merge(ret, alignTrans);

		// Convert the alignment/orientation transformation to the global coordinate system
		if (currentRegion != null)
			ret.merge(currentRegion.getRegionTransForVectors(), ret);

		// Offset the transformation by the entity's global position vector
		ret.getTransRef().add3(getGlobalPosition());

		return ret;
	}

	/**
	 * Returns the transformation that converts a point in the global
	 * coordinate system to the entity's coordinates.
	 * <p>
	 * The entity's coordinate system is centred on the entity's alignment point
	 * and its axes are rotated by the entity's orientation angles. It is NOT
	 * scaled by the entity's size, so the coordinates still have units of
	 * metres. The effects of the RelativeEntity and Region inputs are included
	 * in the transformation.
	 * @return local coordinates for the point.
	 */
	public Transform getEntityTransForSize(Vec3d sizeIn) {
		Transform trans = new Transform();
		getGlobalTransForSize(sizeIn).inverse(trans);
		return trans;
	}

	/**
	 * Returns the global transform with scale factor all rolled into a Matrix4d
	 */
	public Mat4d getTransMatrix(Vec3d scale) {
		Transform trans = getGlobalTrans();
		Mat4d ret = new Mat4d();
		trans.getMat4d(ret);
		ret.scaleCols3(scale);
		return ret;
	}

	/**
	 * Returns the inverse global transform with scale factor all rolled into a Matrix4d
	 */
	public Mat4d getInvTransMatrix() {
		return RenderUtils.getInverseWithScale(getGlobalTrans(), size);
	}

	/**
	 * Return the position in the global coordinate system
	 */
	public Vec3d getGlobalPosition() {
		return getGlobalPosition(getPosition());
	}

	/**
	 * Convert the specified local coordinate to the global coordinate system
	 * @param pos - a position in the entity's local coordinate system
	 */
	public Vec3d getGlobalPosition(Vec3d pos) {

		Vec3d ret = new Vec3d(pos);

		// Position is relative to another entity
		DisplayEntity ent = this.getRelativeEntity();
		if (ent != null) {
			if (currentRegion != null)
				currentRegion.getRegionTransForVectors().multAndTrans(ret, ret);
			ret.add3(ent.getGlobalPosition());
			return ret;
		}

		// Position is given in a local coordinate system
		if (currentRegion != null)
			currentRegion.getRegionTrans().multAndTrans(ret, ret);

		return ret;
	}

	/**
	 * Returns the global coordinates for a specified array of local coordinates.
	 */
	public ArrayList<Vec3d> getGlobalPosition(ArrayList<Vec3d> pts) {
		ArrayList<Vec3d> ret = new ArrayList<>(pts.size());
		for (Vec3d pt : pts) {
			ret.add(getGlobalPosition(pt));
		}
		return ret;
	}

	/**
	 * Returns the local coordinates for a specified array of global coordinates.
	 * @param pts - a position in the global coordinate system
	 */
	public ArrayList<Vec3d> getLocalPosition(ArrayList<Vec3d> pts) {
		ArrayList<Vec3d> ret = new ArrayList<>(pts.size());
		for (Vec3d pt : pts) {
			ret.add(getLocalPosition(pt));
		}
		return ret;
	}

	public void setGlobalPosition(Vec3d pos) {
		setPosition(getLocalPosition(pos));
	}

	public void setGlobalPositionForAlignment(Vec3d pos, Vec3d algn) {
		setPositionForAlignment(getLocalPosition(pos), algn);
	}

	/**
	 * Returns the local coordinates for this entity corresponding to the
	 * specified global coordinates.
	 * @param pos - a position in the global coordinate system
	 */
	public Vec3d getLocalPosition(Vec3d pos) {

		Vec3d localPos = new Vec3d(pos);

		// Position is relative to another entity
		DisplayEntity ent = this.getRelativeEntity();
		if (ent != null) {
			localPos.sub3(ent.getGlobalPosition());
			if (currentRegion != null)
				currentRegion.getInverseRegionTransForVectors().multAndTrans(localPos, localPos);
			return localPos;
		}

		// Position is given in a local coordinate system
		if (currentRegion != null)
			currentRegion.getInverseRegionTrans().multAndTrans(pos, localPos);

		return localPos;
	}

	/**
	 * Returns the transformation to global coordinates from the local
	 * coordinate system determined by the entity's Region and RelativeEntity
	 * inputs.
	 * <p>
	 * Note that this local coordinate system is centred on the position of
	 * the RelativeEntity, not on the position of this entity.
	 * @return transformation to global coordinates.
	 */
	public Transform getGlobalPositionTransform() {
		Transform ret =  new Transform(null, null, 1.0d);

		// Position is relative to another entity
		DisplayEntity relEnt = this.getRelativeEntity();
		if (relEnt != null) {
			if (currentRegion != null)
				ret = currentRegion.getRegionTransForVectors();
			ret.getTransRef().add3(relEnt.getGlobalPosition());
			return ret;
		}

		// Position is given in a local coordinate system
		if (currentRegion != null)
			ret = currentRegion.getRegionTrans();

		return ret;
	}

	/**
	 * Returns the first entry in the 'DisplayModel' input that is an instance of the specified
	 * class (or sub-class) or that implements the specified interface.
	 * Null is returned if no such DisplayModel is found.
	 * @return first DisplayModel that is an instance of the class or implements the interface
	 */
	@SuppressWarnings("unchecked")
	public <T> T getDisplayModel(Class<T> klass) {
		ArrayList<DisplayModel> list = displayModelListInput.getValue();
		if (list == null)
			return null;
		for (DisplayModel model : list) {
			if (klass.isAssignableFrom(model.getClass()))
				return (T) model;
		}
		return null;
	}

	public ArrayList<DisplayModel> getDisplayModelList() {
		return displayModelList;
	}

	public void setDisplayModelList(ArrayList<DisplayModel> dmList) {
		if (dmList != null && dmList.equals(displayModelList))
			return;
		displayModelList.clear();
		if (dmList == null)
			return;
		for (DisplayModel dm : dmList) {
			displayModelList.add(dm);
		}
		clearBindings(); // Clear this on any change, and build it lazily later
	}

	public final void clearBindings() {
		modelBindings = null;
	}

	public ArrayList<DisplayModelBinding> getDisplayBindings() {
		if (modelBindings == null) {
			// Populate the model binding list
			if (getDisplayModelList() == null) {
				modelBindings = new ArrayList<>();
				return modelBindings;
			}
			modelBindings = new ArrayList<>(getDisplayModelList().size());
			for (int i = 0; i < getDisplayModelList().size(); ++i) {
				DisplayModel dm = getDisplayModelList().get(i);
				modelBindings.add(dm.getBinding(this));
			}
		}
		return modelBindings;
	}

	public VisibilityInfo getVisibilityInfo() {
		return visInfo;
	}

	public void dragged(int x, int y, Vec3d newPos) {
		JaamSimModel simModel = getJaamSimModel();

		// Normal objects
		if (!usePointsInput()) {
			KeywordIndex kw = simModel.formatVec3dInput(positionInput.getKeyword(), newPos, DistanceUnit.class);
			InputAgent.apply(this, kw);
			return;
		}

		// Polyline objects
		Vec3d dist = new Vec3d(newPos);
		ArrayList<Vec3d> pts = pointsInput.getValue();
		dist.sub3(pts.get(0));
		KeywordIndex kw = simModel.formatPointsInputs(pointsInput.getKeyword(), pts, dist);
		InputAgent.apply(this, kw);
	}

	/**
	 * Performs the specified keyboard event.
	 * @param keyCode - newt key code
	 * @param keyChar - alphanumeric character for the key (if applicable)
	 * @param shift - true if the Shift key is held down
	 * @param control - true if the Control key is held down
	 * @param alt - true if the Alt key is held down
	 * @return true if the key event was consumed by this entity
	 */
	public boolean handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (!isMovable())
			return false;
		JaamSimModel simModel = getJaamSimModel();

		double inc = getSimulation().getIncrementSize();
		if (getSimulation().isSnapToGrid())
			inc = Math.max(inc, getSimulation().getSnapGridSpacing());

		Vec3d offset = new Vec3d();
		switch (keyCode) {

			case KeyEvent.VK_LEFT:
				offset.x -= inc;
				break;

			case KeyEvent.VK_RIGHT:
				offset.x += inc;
				break;

			case KeyEvent.VK_UP:
				if (shift)
					offset.z += inc;
				else
					offset.y += inc;
				break;

			case KeyEvent.VK_DOWN:
				if (shift)
					offset.z -= inc;
				else
					offset.y -= inc;
				break;

			default:
				return false;
		}

		// Normal object
		if (!usePointsInput()) {
			Vec3d pos = getPosition();
			pos.add3(offset);
			if (getSimulation().isSnapToGrid())
				pos = getSimulation().getSnapGridPosition(pos, pos, shift);
			String posKey = positionInput.getKeyword();
			KeywordIndex posKw = simModel.formatVec3dInput(posKey, pos, DistanceUnit.class);
			InputAgent.storeAndExecute(new KeywordCommand(this, posKw));
			return true;
		}

		// Polyline object
		if (getSimulation().isSnapToGrid()) {
			Vec3d pts0 = new Vec3d(getPoints().get(0));
			pts0.add3(offset);
			pts0 = getSimulation().getSnapGridPosition(pts0, pts0, shift);
			offset = new Vec3d(pts0);
			offset.sub3(getPoints().get(0));
		}
		String ptsKey = pointsInput.getKeyword();
		KeywordIndex ptsKw = simModel.formatPointsInputs(ptsKey, getPoints(), offset);

		InputAgent.storeAndExecute(new KeywordCommand(this, ptsKw));
		return true;
	}

	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (keyCode == KeyEvent.VK_DELETE) {
			GUIListener gui = getJaamSimModel().getGUIListener();
			if (gui == null)
				return;
			try {
				gui.deleteEntity(this);
				FrameBox.setSelectedEntity(null, false);
			}
			catch (ErrorException e) {
				gui.invokeErrorDialogBox("User Error", e.getMessage());
			}
		}
	}

	public void handleMouseClicked(short count, Vec3d globalCoord, boolean shift, boolean control, boolean alt) {}

	public boolean handleDrag(Vec3d currentPt, Vec3d firstPt) {
		return false;
	}

	/**
	 * Returns whether this entity can link in the specified direction to another entity.
	 * @param dir - entity flow direction
	 * @return true if a link can be started
	 */
	public boolean canLink(boolean dir) {
		return false;
	}

	/**
	 * An overloadable method that is called when the 'create link' feature is enabled and selection changes
	 * @param nextEnt
	 */
	public void linkTo(DisplayEntity nextEnt, boolean dir) {}

	/**
	 * Set the inputs for the two entities affected by a 'split' operation.
	 * @param splitEnt - entity split from the original
	 */
	public void setInputsForSplit(DisplayEntity splitEnt) {
		// Do nothing in default behavior
	}

	private final Object screenPointLock = new Object();
	private PolylineInfo[] cachedPointInfo;
	private ArrayList<Vec3d> cachedCurvePoints;

	protected final void invalidateScreenPoints() {
		synchronized(screenPointLock) {
			cachedPointInfo = null;
			cachedCurvePoints = null;
		}
	}

	public final PolylineInfo[] getScreenPoints(double simTime) {
		synchronized(screenPointLock) {
			if (cachedPointInfo == null)
				cachedPointInfo = this.buildScreenPoints(simTime);
			return cachedPointInfo;
		}
	}

	public PolylineInfo[] buildScreenPoints(double simTime) {
		PolylineInfo[] ret = new PolylineInfo[1];
		ret[0] = new PolylineInfo(getCurvePoints(), null, -1, -1.0d);
		return ret;
	}

	public ArrayList<Vec3d> getPoints() {
		synchronized(screenPointLock) {
			return new ArrayList<>(points);
		}
	}

	public ArrayList<Vec3d> getCurvePoints() {
		synchronized(screenPointLock) {
			if (cachedCurvePoints == null)
				cachedCurvePoints = this.buildCurvePoints();
			return cachedCurvePoints;
		}
	}

	private ArrayList<Vec3d> buildCurvePoints() {
		ArrayList<Vec3d> ret = null;
		switch (this.getCurveType()) {
		case LINEAR:
			ret = getPoints();
			break;
		case BEZIER:
			ret = PolylineInfo.getBezierPoints(getPoints());
			break;
		case SPLINE:
			ret = PolylineInfo.getSplinePoints(getPoints());
			break;
		case CIRCULAR_ARC:
			ret = PolylineInfo.getCircularArcPoints(getPoints());
			break;
		default:
			assert(false);
			error("Invalid CurveType");
		}
		return ret;
	}

	public void setPoints(ArrayList<Vec3d> pts) {
		synchronized (screenPointLock) {
			points.clear();
			points.addAll(pts);
		}
		invalidateScreenPoints();
	}

	public void setGlobalPoints(ArrayList<Vec3d> pts) {
		synchronized (screenPointLock) {
			points.clear();
			points.addAll(getLocalPosition(pts));
		}
		invalidateScreenPoints();
	}

	public boolean selectable() {
		return true;
	}

	protected PolylineInfo.CurveType getCurveType() {
		return curveTypeInput.getValue();
	}

	public final void setTagColour(String tagName, Color4d ca) {
		Color4d cas[] = new Color4d[1] ;
		cas[0] = ca;
		setTagColours(tagName, cas);
	}

	public final void setTagColours(String tagName, Color4d[] cas) {
		Tag t = tagMap.get(tagName);
		if (t == null) {
			t = new Tag(cas, null, true);
			tagMap.put(tagName, t);
			return;
		}

		if (t.colorsMatch(cas))
			return;
		else
			tagMap.put(tagName, new Tag(cas, t.sizes, t.visible));
	}

	public final void setTagSize(String tagName, double size) {
		double s[] = new double[1] ;
		s[0] = size;
		setTagSizes(tagName, s);
	}

	public final void setTagSizes(String tagName, double[] sizes) {
		Tag t = tagMap.get(tagName);
		if (t == null) {
			t = new Tag(null, sizes, true);
			tagMap.put(tagName, t);
			return;
		}

		if (t.sizesMatch(sizes))
			return;
		else
			tagMap.put(tagName, new Tag(t.colors, sizes, t.visible));
	}

	public final void setTagVisibility(String tagName, boolean isVisible) {
		Tag t = tagMap.get(tagName);
		if (t == null) {
			t = new Tag(null, null, isVisible);
			tagMap.put(tagName, t);
			return;
		}

		if (t.visMatch(isVisible))
			return;
		else
			tagMap.put(tagName, new Tag(t.colors, t.sizes, isVisible));
	}

	/**
	 * Get all tags for this entity
	 */
	public HashMap<String, Tag> getTagSet() {
		return tagMap;
	}

	public Vec3d getSourcePoint() {
		return this.getSourcePoint(true);
	}

	public Vec3d getSinkPoint() {
		return this.getSinkPoint(true);
	}

	/**
	 * Returns the global position at which entities depart from this entity, if relevant.
	 * @param dir - true = normal direction, false = reverse direction
	 * @return arrival location
	 */
	public Vec3d getSourcePoint(boolean dir) {
		if (usePointsInput() && !pointsInput.getValue().isEmpty()) {
			ArrayList<Vec3d> points = pointsInput.getValue();
			Vec3d localPt = points.get(0);
			if (dir)
				localPt = points.get(points.size() - 1);
			return getGlobalPosition(localPt);
		}
		return getGlobalPosition();
	}

	/**
	 * Returns the global position at which entities arrive at this entity, if relevant.
	 * @param dir - true = normal direction, false = reverse direction
	 * @return departure location
	 */
	public Vec3d getSinkPoint(boolean dir) {
		if (usePointsInput() && !pointsInput.getValue().isEmpty()) {
			ArrayList<Vec3d> points = pointsInput.getValue();
			Vec3d localPt = points.get(0);
			if (!dir)
				localPt = points.get(points.size() - 1);
			return getGlobalPosition(localPt);
		}
		return getGlobalPosition();
	}

	/**
	 * Returns the distance from the arrival/departure location at which an entity flow arrow
	 * begins or ends.
	 * @return distance from the arrival/departure location
	 */
	public double getRadius() {
		double scale = 1.0d;
		if (currentRegion != null)
			scale = currentRegion.getGlobalScale();
		if (usePointsInput())
			return 0.05d * scale;
		double ret = Math.min(getSize().x, getSize().y)/2.0 + 0.05d;
		return ret * scale;
	}

	public double getMinRadius() {
		double scale = 1.0d;
		if (currentRegion != null)
			scale = currentRegion.getGlobalScale();
		return 0.05d * scale;
	}

	public ArrayList<ObserverEntity> getObserverList() {
		return new ArrayList<>();
	}

	public ArrayList<DisplayEntity> getDestinationEntities() {
		return new ArrayList<>();
	}

	public ArrayList<DisplayEntity> getSourceEntities() {
		return new ArrayList<>();
	}

	public ArrayList<DirectedEntity> getDestinationDirEnts(boolean dir) {
		if (dir) {
			try {
				return DirectedEntity.getList(getDestinationEntities(), true);
			}
			catch (Exception e) {}
		}
		return new ArrayList<>();
	}

	public ArrayList<DirectedEntity> getSourceDirEnts(boolean dir) {
		if (dir) {
			try {
				return DirectedEntity.getList(getSourceEntities(), true);
			}
			catch (Exception e) {}
		}
		return new ArrayList<>();
	}

	public ArrayList<DirectedEntity> getNextList(boolean dir) {
		ArrayList<DirectedEntity> ret = new ArrayList<>();
		ret.addAll(getDestinationDirEnts(dir));
		DirectedEntity thisDe = new DirectedEntity(this, dir);
		for (DisplayEntity ent : getJaamSimModel().getClonesOfIterator(DisplayEntity.class)) {
			if (ent.getSourceDirEnts(true).contains(thisDe)) {
				ret.add(new DirectedEntity(ent, true));
			}
			if (ent.getSourceDirEnts(false).contains(thisDe)) {
				ret.add(new DirectedEntity(ent, false));
			}
		}
		return ret;
	}

	public ArrayList<DirectedEntity> getPreviousList(boolean dir) {
		ArrayList<DirectedEntity> ret = new ArrayList<>();
		ret.addAll(getSourceDirEnts(dir));
		DirectedEntity thisDe = new DirectedEntity(this, dir);
		for (DisplayEntity ent : getJaamSimModel().getClonesOfIterator(DisplayEntity.class)) {
			if (ent.getDestinationDirEnts(true).contains(thisDe)) {
				ret.add(new DirectedEntity(ent, true));
			}
			if (ent.getDestinationDirEnts(false).contains(thisDe)) {
				ret.add(new DirectedEntity(ent, false));
			}
		}
		return ret;
	}

	/**
	 * Sets the region, position, and orientation to match the specified entity and offset.
	 * @param ent - entity whose position, etc. is to be matched
	 * @param offset - new position of this entity relative to the specified entity
	 */
	public final void moveToProcessPosition(DisplayEntity ent, Vec3d offset) {
		setRegion(ent.getCurrentRegion());
		Vec3d pos = ent.getGlobalPosition();
		pos.add3(offset);
		setGlobalPosition(pos);
		setRelativeOrientation(ent.getOrientation());
	}

	/**
	 * Returns the first parent that is visible in the chain of parents.
	 * @return first visible parent
	 */
	public DisplayEntity getVisibleParent() {
		Entity ent = this.getParent();
		while (ent != null && ent instanceof DisplayEntity) {
			if (((DisplayEntity) ent).getShow()) {
				return (DisplayEntity) ent;
			}
			ent = ent.getParent();
		}
		return null;
	}

	////////////////////////////////////////////////////////////////////////
	// Outputs
	////////////////////////////////////////////////////////////////////////

	@Output(name = "Region",
	 description = "The present coordinate system in which the DisplayEntity's position and"
	             + "orientation are given.",
	    sequence = 1)
	public Region getRegionOutput(double simTime) {
		return getCurrentRegion();
	}

	@Output(name = "Position",
	 description = "The present {x, y, z} coordinates of the DisplayEntity in its Region.",
	    unitType = DistanceUnit.class,
	    sequence = 2)
	public Vec3d getPosOutput(double simTime) {
		return getPosition();
	}

	@Output(name = "Size",
	 description = "The present {x, y, z} components of the DisplayEntity's size.",
	    unitType = DistanceUnit.class,
	    sequence = 3)
	public Vec3d getSizeOutput(double simTime) {
		return getSize();
	}

	@Output(name = "Orientation",
	 description = "The present {x, y, z} Euler angles of the DisplayEntity's rotation.",
	    unitType = AngleUnit.class,
	    sequence = 4)
	public Vec3d getOrientOutput(double simTime) {
		return getOrientation();
	}

	@Output(name = "Alignment",
	 description = "The present {x, y, z} coordinates of a point on the DisplayEntity that aligns "
	             + "direction with the position output. Each component should be in the range "
	             + "[-0.5, 0.5].",
	    unitType = DimensionlessUnit.class,
	    sequence = 5)
	public Vec3d getAlignOutput(double simTime) {
		return getAlignment();
	}

	@Output(name = "Show",
	 description = "Returns TRUE if the object is shown in one or more view windows.",
	    sequence = 6)
	public boolean getShowOutput(double simTime) {
		return getShow(simTime);
	}

	@Output(name = "GraphicalLength",
	 description = "Polyline type objects: the length of the polyline determined by its "
	             + "Points and CurveType inputs.\n"
	             + "Non-polyline type objects: the largest of the Size inputs.",
	    unitType = DistanceUnit.class,
	    sequence = 7)
	public double getGraphicalLength(double simTime) {
		if (usePointsInput()) {
			return PolylineInfo.getLength(getCurvePoints());
		}
		Vec3d vec = getSize();
		return Math.max(Math.max(vec.x, vec.y), vec.z);
	}

	@Output(name = "ObserverList",
	 description = "The observers that are monitoring the state of this entity.",
	    sequence = 8)
	public ArrayList<ObserverEntity> getObserverList(double simTime) {
		return getObserverList();
	}

	@Output(name = "NextList",
	 description = "The entities that are immediately downstream from this entity.",
	    sequence = 9)
	public ArrayList<DirectedEntity> getNextList(double simTime) {
		return getNextList(true);
	}

	@Output(name = "PreviousList",
	 description = "The entities that are immediately upstream from this entity.",
	    sequence = 10)
	public ArrayList<DirectedEntity> getPreviousList(double simTime) {
		return getPreviousList(true);
	}

	@Output(name = "EntityReferenceList",
	 description = "The entities that appear in the inputs to this entity.",
	    sequence = 11)
	public ArrayList<DisplayEntity> getEntityReferenceList(double simTime) {
		ArrayList<Entity> list = getEntityReferences();
		ArrayList<DisplayEntity> ret = new ArrayList<>(list.size());
		for (Entity ent : list) {
			if (!(ent instanceof DisplayEntity) || ent == this
					|| ent instanceof OverlayEntity || ent instanceof Region)
				continue;
			ret.add((DisplayEntity) ent);
		}
		return ret;
	}

}
