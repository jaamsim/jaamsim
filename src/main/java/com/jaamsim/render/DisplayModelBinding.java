/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Tag;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.ColourInput;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;

/**
 * Represents the One-to-one mapping of DisplayModels to Entities
 * Any graphical caching goes in here, while configuration information goes in the DisplayModel
 * @author matt.chudleigh
 *
 */
public abstract class DisplayModelBinding {

	protected Entity observee;
	protected DisplayModel dm;

	private static final Color4d MINT = ColourInput.getColorWithName("mint");

	private static final boolean _saveCacheMissData = false;
	private static final HashMap<String, CacheCounter> cacheMissData = new HashMap<>();

	//protected DisplayEntity _dispObservee;

	private List<Vec4d> handlePoints = null;

	private List<Vec4d> rotateHandlePoints = null;

	private final static ArrayList<Vec4d> HANDLE_POINTS;
	private final static ArrayList<Vec4d> ROTATE_POINTS;

	private static int cacheHits = 0;
	private static int cacheMisses = 0;

	static {
		// NOTE: the order of the points corresponds to the list of static picking IDs in RenderManager,
		// both need to be changed together
		HANDLE_POINTS = new ArrayList<>(8);
		// Sides
		HANDLE_POINTS.add(new Vec4d( 0.5,    0, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d(-0.5,    0, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d(   0,  0.5, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d(   0, -0.5, 0, 1.0d));

		// Corners
		HANDLE_POINTS.add(new Vec4d( 0.5,  0.5, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d( 0.5, -0.5, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d(-0.5,  0.5, 0, 1.0d));
		HANDLE_POINTS.add(new Vec4d(-0.5, -0.5, 0, 1.0d));

		ROTATE_POINTS = new ArrayList<>(2);
		// Sides
		ROTATE_POINTS.add(new Vec4d(1.0, 0, 0, 1.0d));
		ROTATE_POINTS.add(new Vec4d(0.5, 0, 0, 1.0d));
	}

	public DisplayModelBinding(Entity ent, DisplayModel dm) {
		this.observee = ent;
		this.dm = dm;
	}

	public abstract void collectProxies(double simTime, ArrayList<RenderProxy> out);

	public boolean isBoundTo(Entity ent) {
		return ent == observee;
	}

	private void updatePoints(double simTime) {

		if (!(observee instanceof DisplayEntity))
		{
			return;
		}
		DisplayEntity de = (DisplayEntity)observee;
		// Convert the points to world space

		Transform trans = de.getGlobalTrans();
		Vec3d scale = de.getSize();
		scale.mul3(dm.getModelScale());

		Mat4d mat = new Mat4d(trans.getMat4dRef());
		mat.scaleCols3(scale);

		handlePoints = RenderUtils.transformPoints(mat, HANDLE_POINTS, 0);

		rotateHandlePoints = RenderUtils.transformPoints(mat, ROTATE_POINTS, 0);
	}

	/**
	 * Collects the proxies for the green box and handles that are shown around the selected
	 * entity.
	 * @param simTime - present simulation time
	 * @param out - array to which the render proxies are appended
	 */
	public void collectSelectionProxies(double simTime, ArrayList<RenderProxy> out) {
		collectSelectionBox(simTime, out);
	}

	// This is exposed differently than above, because of the weird type heirarchy around
	// ScreenPointsObservers. This can't just be overloaded, because sometime we want it back....
	protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {

		if (!(observee instanceof DisplayEntity))
		{
			return;
		}

		DisplayEntity de = (DisplayEntity)observee;
		Transform trans = de.getGlobalTrans();
		Vec3d scale = de.getSize();
		scale.mul3(dm.getModelScale());

		PolygonProxy outline = new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale,
		                                        MINT, true, 1,
		                                        getVisibilityInfo(), RenderManager.MOVE_PICK_ID);
		outline.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(outline);

		updatePoints(simTime);

		for (int i = 0; i < 8; ++i) {

			List<Vec4d> pl = new ArrayList<>(1);

			pl.add(handlePoints.get(i));
			PointProxy point = new PointProxy(pl, ColourInput.GREEN, 8, getVisibilityInfo(), RenderManager.RESIZE_POSX_PICK_ID - i);
			point.setHoverColour(ColourInput.LIGHT_GREY);
			point.setCollisionAngle(0.004363); // 0.25 degrees in radians

			out.add(point);
		}

		// Add the rotate handle
		List<Vec4d> pl = new ArrayList<>(1);
		pl.add(new Vec4d(rotateHandlePoints.get(0)));
		PointProxy point = new PointProxy(pl, ColourInput.GREEN, 8, getVisibilityInfo(), RenderManager.ROTATE_PICK_ID);
		point.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(point);

		LineProxy rotateLine = new LineProxy(rotateHandlePoints, MINT, 1, getVisibilityInfo(), RenderManager.ROTATE_PICK_ID);
		rotateLine.setHoverColour(ColourInput.LIGHT_GREY);
		out.add(rotateLine);
	}

	public static int getCacheHits() {
		return cacheHits;
	}

	public static int getCacheMisses() {
		return cacheMisses;
	}
	public static void clearCacheCounters() {
		cacheHits = 0;
		cacheMisses = 0;
	}

	public static void clearCacheMissData() {
		cacheMissData.clear();
	}

	private static final boolean saveCacheMissData() {
		return _saveCacheMissData;
	}


	private static class CacheCounter {
		int misses = 0;
		int hits = 0;

		CacheCounter() {}
	}

	public static void registerCacheHit(String type) {
		cacheHits++;
		if (!saveCacheMissData()) {
			return;
		}

		CacheCounter cc = cacheMissData.get(type);
		if (cc == null) {
			cc = new CacheCounter();
			cacheMissData.put(type, cc);
		}
		cc.hits++;
	}

	public static int getCacheHitCount(String type) {
		CacheCounter cc = cacheMissData.get(type);
		if (cc == null)
			return 0;

		return cc.hits;
	}

	public static void registerCacheMiss(String type) {
		cacheMisses++;
		if (!saveCacheMissData()) {
			return;
		}

		CacheCounter cc = cacheMissData.get(type);
		if (cc == null) {
			cc = new CacheCounter();
			cacheMissData.put(type, cc);
		}
		cc.misses++;
	}

	public static int getCacheMissCount(String type) {
		CacheCounter cc = cacheMissData.get(type);
		if (cc == null)
			return 0;

		return cc.misses;
	}

	public VisibilityInfo getVisibilityInfo() {

		// If set, the DisplayEntity overrides the visibility info for the DisplayModel
		if (observee instanceof DisplayEntity) {
			DisplayEntity de = (DisplayEntity)observee;
			if (de.getVisibilityInfo() != null) {
				return de.getVisibilityInfo();
			}
		}

		return dm.getVisibilityInfo();
	}

	/**
	 * A utility method to compare values while respecting null, used for caching
	 * @param cache
	 * @param val
	 */
	protected <T> boolean compare(T cache, T val) {
		if (cache == val)
			return true;

		// We tested above for the both-null case, if only one is null, not equal
		if (cache == null || val == null)
			return false;

		return cache.equals(val);
	}

	protected static boolean dirty_tags(HashMap<String, Tag> cache, HashMap<String, Tag> val) {
		if (cache.size() != val.size()) return true;

		for (Entry<String, Tag> each : cache.entrySet()) {
			if (val.get(each.getKey()) != each.getValue())
				return true;
		}
		return false;
	}

	/**
	 * A utility method to compare values while respecting null, used for caching
	 * @param cache
	 * @param val
	 */
	protected static boolean dirty_vec3d(Vec3d cache, Vec3d val) {
		if (cache == val)
			return false;

		// We tested above for the both-null case, if only one is null, not equal
		if (cache == null || val == null)
			return true;

		return !cache.equals3(val);
	}

	/**
	 * A utility method to compare values while respecting null, used for caching
	 * @param cache
	 * @param val
	 */
	protected static boolean dirty_vec4d(Vec4d cache, Vec4d val) {
		if (cache == val)
			return false;

		// We tested above for the both-null case, if only one is null, not equal
		if (cache == null || val == null)
			return true;

		return !cache.equals4(val);
	}

	/**
	 * A utility method to compare values while respecting null, used for caching
	 * @param cache
	 * @param val
	 */
	protected static boolean dirty_col4d(Color4d cache, Color4d val) {
		if (cache == val)
			return false;

		// We tested above for the both-null case, if only one is null, not equal
		if (cache == null || val == null)
			return true;

		return !cache.equals4(val);
	}

	protected <T> boolean compareArray(T[] cache, T[] val) {
		if (cache == val)
			return true;

		// We tested above for the both-null case, if only one is null, not equal
		if (cache == null || val == null)
			return false;

		return Arrays.deepEquals(cache, val);
	}
}
