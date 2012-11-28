/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.observers;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.ImageProxy;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.MeshProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.TexCache;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModel;


/**
 * A transition Observer type, this simply tries to reimplement as much of DisplayModel as is reasonably easy to do
 * @author matt.chudleigh
 *
 */
public class DisplayModelObserver extends RenderObserver {

	// Since Arrows aren't convex, we need some more convoluted vertices
	private static List<Vector4d> arrowHeadVerts;
	private static List<Vector4d> arrowTailVerts;
	private static List<Vector4d> arrowOutlineVerts;

	private static List<Vector4d> truckCabVerts;

	private static List<Vector4d> crushingPlantTopVerts;
	private static List<Vector4d> crushingPlantBotVerts;

	private static List<Vector4d> singleQuadLinePoints;
	private static List<Vector4d> singleQuadRectVerts;

	private static List<Vector4d> dualQuadLinePoints;
	private static List<Vector4d> dualQuadOutlineVerts;
	private static List<Vector4d> dualQuadRect0Verts;
	private static List<Vector4d> dualQuadRect1Verts;
	private static List<Vector4d> dualQuadRect2Verts;

	private static List<Vector4d> travellingRect1Verts;
	private static List<Vector4d> travellingRect2Verts;
	private static List<Vector4d> travellingRect3Verts;

	private static List<Vector4d> stackerRect1Verts;
	private static List<Vector4d> stackerRect2Verts;

	private static List<Vector4d> hullVerts;
	private static List<Vector4d> shipCabinVerts;
	private static Matrix4d shipContentsTrans;
	private static Matrix4d truckContentsTrans;

	private static ArrayList<String> imageExtensions;

	private static ArrayList<String> modelExtensions;

	static {
		hullVerts = new ArrayList<Vector4d>(20);
		hullVerts.add(new Vector4d(-0.35625d, -0.5d, 0.0d));
		hullVerts.add(new Vector4d(0.35d, -0.5d, 0.0d));
		hullVerts.add(new Vector4d(0.40625d, -0.42d, 0.0d));
		hullVerts.add(new Vector4d(0.459375d, -0.3d, 0.0d));
		hullVerts.add(new Vector4d(0.484375d, -0.21d, 0.0d));
		hullVerts.add(new Vector4d(0.5d, -0.05d, 0.0d));
		hullVerts.add(new Vector4d(0.5d, 0.05d, 0.0d));
		hullVerts.add(new Vector4d(0.484375d, 0.21d, 0.0d));
		hullVerts.add(new Vector4d(0.459375d, 0.3d, 0.0d));
		hullVerts.add(new Vector4d(0.40625d, 0.42d, 0.0d));
		hullVerts.add(new Vector4d(0.35d, 0.5d, 0.0d));
		hullVerts.add(new Vector4d(-0.35625d, 0.5d, 0.0d));
		hullVerts.add(new Vector4d(-0.4109375d, 0.45d, 0.0d));
		hullVerts.add(new Vector4d(-0.4515625d, 0.36d, 0.0d));
		hullVerts.add(new Vector4d(-0.5d, 0.23d, 0.0d));
		hullVerts.add(new Vector4d(-0.5d, -0.23d, 0.0d));
		hullVerts.add(new Vector4d(-0.4515625d, -0.36d, 0.0d));
		hullVerts.add(new Vector4d(-0.4109375d, -0.45d, 0.0d));
		hullVerts.add(new Vector4d(-0.35625d, -0.5d, 0.0d));

		Matrix4d shipCabinTrans = Matrix4d.TranslationMatrix(new Vector4d(-0.325, 0, 0));
		shipCabinTrans.mult(Matrix4d.ScaleMatrix(0.125, 0.7, 0), shipCabinTrans);
		shipCabinVerts = RenderUtils.transformPoints(shipCabinTrans, RenderUtils.RECT_POINTS);

		shipContentsTrans = Matrix4d.TranslationMatrix(new Vector4d(-0.225, 0.0, 0.0));
		shipContentsTrans.mult(Matrix4d.ScaleMatrix(0.65, 0.6, 0), shipContentsTrans);

		truckContentsTrans = Matrix4d.TranslationMatrix(new Vector4d(-0.5, 0.0, 0.0));
		truckContentsTrans.mult(Matrix4d.ScaleMatrix(0.75, 1, 0), truckContentsTrans);

		arrowHeadVerts = new ArrayList<Vector4d>(3);
		arrowHeadVerts.add(new Vector4d(-0.5,  0.0, 0.0));
		arrowHeadVerts.add(new Vector4d(-0.1, -0.5, 0.0));
		arrowHeadVerts.add(new Vector4d(-0.1,  0.5, 0.0));

		arrowTailVerts = new ArrayList<Vector4d>(4);
		arrowTailVerts.add(new Vector4d(-0.1, -0.2, 0.0));
		arrowTailVerts.add(new Vector4d( 0.5, -0.2, 0.0));
		arrowTailVerts.add(new Vector4d( 0.5,  0.2, 0.0));
		arrowTailVerts.add(new Vector4d(-0.1,  0.2, 0.0));

		arrowOutlineVerts = new ArrayList<Vector4d>(7);
		arrowOutlineVerts.add(new Vector4d(-0.5,  0.0, 0.0));
		arrowOutlineVerts.add(new Vector4d(-0.1, -0.5, 0.0));
		arrowOutlineVerts.add(new Vector4d(-0.1, -0.2, 0.0));
		arrowOutlineVerts.add(new Vector4d( 0.5, -0.2, 0.0));
		arrowOutlineVerts.add(new Vector4d( 0.5,  0.2, 0.0));
		arrowOutlineVerts.add(new Vector4d(-0.1,  0.2, 0.0));
		arrowOutlineVerts.add(new Vector4d(-0.1,  0.5, 0.0));

		truckCabVerts = new ArrayList<Vector4d>(4);
		truckCabVerts.add(new Vector4d( 0.5,  0.5, 0.0));
		truckCabVerts.add(new Vector4d(0.25,  0.5, 0.0));
		truckCabVerts.add(new Vector4d(0.25, -0.5, 0.0));
		truckCabVerts.add(new Vector4d( 0.5, -0.5, 0.0));

		crushingPlantBotVerts = new ArrayList<Vector4d>(4);
		crushingPlantBotVerts.add(new Vector4d( -0.17659f, -0.5f, 0.0f ));
		crushingPlantBotVerts.add(new Vector4d( 0.15675f, -0.5f, 0.0f ));
		crushingPlantBotVerts.add(new Vector4d( 0.15675f, -0.1f, 0.0f ));
		crushingPlantBotVerts.add(new Vector4d( -0.17659f, -0.1f, 0.0f ));

		crushingPlantTopVerts = new ArrayList<Vector4d>(4);
		crushingPlantTopVerts.add(new Vector4d( -0.17659f, 0f, 0.0f ));
		crushingPlantTopVerts.add(new Vector4d( 0.15675f, 0f, 0.0f ));
		crushingPlantTopVerts.add(new Vector4d( 0.49008f, 0.5f, 0.0f ));
		crushingPlantTopVerts.add(new Vector4d( -0.50992f, 0.5f, 0.0f ));

		singleQuadLinePoints = new ArrayList<Vector4d>();
		singleQuadLinePoints.add(new Vector4d( 0.4,  0.5, 0.0));
		singleQuadLinePoints.add(new Vector4d(-0.4,  0.0, 0.0));

		singleQuadLinePoints.add(new Vector4d(-0.4,  0.0, 0.0));
		singleQuadLinePoints.add(new Vector4d( 0.4, -0.5, 0.0));
		// Also add the arc lines
		List<Vector4d> singleArcPoints = RenderUtils.getArcPoints(0.6, new Vector4d(-0.3, 0, 0),
		                                                          Math.PI *  0.25,
		                                                          Math.PI *  0.75, 10);
		singleQuadLinePoints.addAll(singleArcPoints);

		singleQuadRectVerts = new ArrayList<Vector4d>();
		singleQuadRectVerts.add(new Vector4d( 0.5, -0.0833333, 0.0));
		singleQuadRectVerts.add(new Vector4d( 0.5,  0.0833333, 0.0));

		singleQuadRectVerts.add(new Vector4d(-0.5,  0.0833333, 0.0));
		singleQuadRectVerts.add(new Vector4d(-0.5, -0.0833333, 0.0));

		dualQuadLinePoints = new ArrayList<Vector4d>();
		dualQuadLinePoints.add(new Vector4d(0.4, 0.045454545, 0.0));
		dualQuadLinePoints.add(new Vector4d(-0.4, -0.227272727, 0.0));

		dualQuadLinePoints.add(new Vector4d(-0.4, -0.227272727, 0.0));
		dualQuadLinePoints.add(new Vector4d(0.4, -0.5, 0.0));

		dualQuadLinePoints.add(new Vector4d( 0.4, 0.5, 0.0));
		dualQuadLinePoints.add(new Vector4d( -0.4, 0.227272727, 0.0));

		dualQuadLinePoints.add(new Vector4d( -0.4, 0.227272727, 0.0));
		dualQuadLinePoints.add(new Vector4d( 0.4, -0.045454545, 0.0));
		List<Vector4d> dualArcPoints = RenderUtils.getArcPoints(0.6, new Vector4d(-0.3, -0.227272727, 0),
		                                                        Math.PI *  0.25,
		                                                        Math.PI *  0.75, 10);
		dualQuadLinePoints.addAll(dualArcPoints);
		dualArcPoints = RenderUtils.getArcPoints(0.6, new Vector4d(-0.3, 0.227272727, 0),
		                                         Math.PI *  0.25,
		                                         Math.PI *  0.75, 10);
		dualQuadLinePoints.addAll(dualArcPoints);

		dualQuadOutlineVerts = new ArrayList<Vector4d>();
		dualQuadOutlineVerts.add(new Vector4d(-0.5, -0.272727273, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(-0.5, 0.272727273, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(0.5, 0.272727273, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(0.5, 0.181818182, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(-0.3, 0.181818182, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(-0.3, -0.181818182, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(0.5, -0.181818182, 0.0));
		dualQuadOutlineVerts.add(new Vector4d(0.5, -0.272727273, 0.0));


		dualQuadRect0Verts = new ArrayList<Vector4d>();
		dualQuadRect0Verts.add(new Vector4d(-0.5, -0.272727273, 0.0));
		dualQuadRect0Verts.add(new Vector4d(0.5, -0.272727273, 0.0));
		dualQuadRect0Verts.add(new Vector4d(0.5, -0.181818182, 0.0));
		dualQuadRect0Verts.add(new Vector4d(-0.5, -0.181818182, 0.0));

		dualQuadRect1Verts = new ArrayList<Vector4d>();
		dualQuadRect1Verts.add(new Vector4d(-0.5, -0.181818182, 0.0));
		dualQuadRect1Verts.add(new Vector4d(-0.3, -0.181818182, 0.0));
		dualQuadRect1Verts.add(new Vector4d(-0.3, 0.181818182, 0.0));
		dualQuadRect1Verts.add(new Vector4d(-0.5, 0.181818182, 0.0));

		dualQuadRect2Verts = new ArrayList<Vector4d>();
		dualQuadRect2Verts.add(new Vector4d(-0.5, 0.181818182, 0.0));
		dualQuadRect2Verts.add(new Vector4d(0.5, 0.181818182, 0.0));
		dualQuadRect2Verts.add(new Vector4d(0.5, 0.272727273, 0.0));
		dualQuadRect2Verts.add(new Vector4d(-0.5, 0.272727273, 0.0));

		travellingRect1Verts = new ArrayList<Vector4d>();
		travellingRect1Verts.add(new Vector4d(-0.2, -0.3, 0));
		travellingRect1Verts.add(new Vector4d( 0.2, -0.3, 0));
		travellingRect1Verts.add(new Vector4d( 0.2,  0.1, 0));
		travellingRect1Verts.add(new Vector4d(-0.2,  0.1, 0));

		travellingRect2Verts = new ArrayList<Vector4d>();
		travellingRect2Verts.add(new Vector4d(-0.5, -0.1, 0));
		travellingRect2Verts.add(new Vector4d( 0.5, -0.1, 0));
		travellingRect2Verts.add(new Vector4d( 0.5,  0.1, 0));
		travellingRect2Verts.add(new Vector4d(-0.5,  0.1, 0));

		travellingRect3Verts = new ArrayList<Vector4d>();
		travellingRect3Verts.add(new Vector4d(-0.1, -0.5, 0));
		travellingRect3Verts.add(new Vector4d( 0.1, -0.5, 0));
		travellingRect3Verts.add(new Vector4d( 0.1,  0.1, 0));
		travellingRect3Verts.add(new Vector4d(-0.1,  0.1, 0));

		stackerRect1Verts = new ArrayList<Vector4d>();
		stackerRect1Verts.add(new Vector4d( 0.3, -0.3, 0.1));
		stackerRect1Verts.add(new Vector4d( 0.3,  0.3, 0.1));
		stackerRect1Verts.add(new Vector4d(-0.3,  0.3, 0.1));
		stackerRect1Verts.add(new Vector4d(-0.3, -0.3, 0.1));

		stackerRect2Verts = new ArrayList<Vector4d>();
		stackerRect2Verts.add(new Vector4d(-0.1,  0.0, 0.1));
		stackerRect2Verts.add(new Vector4d(-0.1, -0.5, 0.1));
		stackerRect2Verts.add(new Vector4d( 0.1, -0.5, 0.1));
		stackerRect2Verts.add(new Vector4d( 0.1,  0.0, 0.1));

		imageExtensions = new ArrayList<String>();
		imageExtensions.add(".PNG");
		imageExtensions.add(".JPG");
		imageExtensions.add(".BMP");
		imageExtensions.add(".GIF");

		modelExtensions = new ArrayList<String>();
		modelExtensions.add(".DAE");
		modelExtensions.add(".ZIP");
	}

	private ArrayList<RenderProxy> _cachedProxies;
	private ChangeWatcher.Tracker _observeeTracker;

	private static HashMap<String, MeshProtoKey> _cachedKeys = new HashMap<String, MeshProtoKey>();

	DisplayModelObserver(Entity observee) {
		super(observee);
		try {
		_observeeTracker = ((DisplayEntity)observee).getGraphicsChangeTracker();
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	private void updateCache() {
		if (_cachedProxies != null && !_observeeTracker.checkAndClear()) {
			// Nothing changed
			++_cacheHits;
			return;
		}

		++_cacheMisses;

		double simTime = _dispObservee.getCurrentTime();
		Transform trans = _dispObservee.getGlobalTrans(simTime);
		Vector4d scale = _dispObservee.getJaamMathSize();

		long pickingID = _dispObservee.getEntityNumber();
		DisplayEntity.TagSet tags = _dispObservee.getTagSet();

		_cachedProxies = new ArrayList<RenderProxy>();

		// Find the list of display model
		for (DisplayModel dm : _dispObservee.getDisplayModelList().getValue()) {

			DisplayModelState dms = new DisplayModelState(dm);
			collectProxiesForDisplayModel(dms, tags, trans, scale,
			                              pickingID, _cachedProxies);
		}
	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {

		if (_dispObservee == null) { return; }
		if (!_dispObservee.getShow()) { return; }

		updateCache();

		out.addAll(_cachedProxies);
	}

	public static void collectProxiesForDisplayModel(DisplayModelState dms, DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                           long pickingID, ArrayList<RenderProxy> out) {

		String shapeString = dms.shape.toUpperCase();
		// Handle Ship elsewhere to keep this method readable
		if (shapeString.equals("SHIP2D")) {
			addShipProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("TRUCK2D")) {
			addTruckProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("BARGAUGE2D")) {
			addBarGaugeProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("CRUSHINGPLANT2D")) {
			addCrushingPlantProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("ARROW2D")) {
			addArrowProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("SINGLEQUADRANT2D")) {
			addSingleQuadProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("DUALQUADRANT2D")) {
			addDualQuadProxies(tags, trans, scale, pickingID, out);
			return;
		}
		if (shapeString.equals("TRAVELLING2D")) {
			addTravellingProxies(tags, trans, scale, pickingID, out);
			return;
		}

		if (shapeString.equals("STACKER2D") ||
		    shapeString.equals("RECLAIMER2D") ) {
			addStackerProxies(tags, trans, scale, pickingID, out);
			return;
			}

		List<Vector4d> points = null;
		if (shapeString.equals("CIRCLE")) {
			points = RenderUtils.CIRCLE_POINTS;
		}
		if (shapeString.equals("RECTANGLE")) {
			points = RenderUtils.RECT_POINTS;
		}
		if (shapeString.equals("TRIANGLE")) {
			points = RenderUtils.TRIANGLE_POINTS;
		}

		if (points == null || points.size() == 0) {
			// Not a known shape, try to find an extension we recognize
			if (dms.shape.length() <= 4) { return; } // can not be a filename

			String ext = dms.shape.substring(dms.shape.length() - 4, dms.shape.length());

			if (imageExtensions.contains(ext.toUpperCase())) {
				addImageProxy(dms.shape, trans, scale, dms.transparent, dms.compressedTexture, pickingID, out);
			}
			if (modelExtensions.contains(ext.toUpperCase())) {
				addModelProxy(dms.shape, trans, scale, pickingID, out);
			}
			return;
		}

		// Gather some inputs

		if (tags.isTagVisibleUtil(DisplayModel.TAG_OUTLINES))
		{
			Color4d colour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, dms.outlineColour);
			out.add(new PolygonProxy(points, trans, scale, colour, true, (dms.bold ? 2 : 1), pickingID));
		}

		if (dms.filled && tags.isTagVisibleUtil(DisplayModel.TAG_CONTENTS))
		{
			Color4d colour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, dms.fillColour);
			out.add(new PolygonProxy(points, trans, scale, colour, false, 1, pickingID));
		}
	}

	private static void addArrowProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
            long pickingID, ArrayList<RenderProxy> out) {
		Color4d fillColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.BLACK);

		out.add(new PolygonProxy(arrowHeadVerts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(arrowTailVerts, trans, scale, fillColour, false, 1, pickingID));

		Color4d outlineColour= tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		out.add(new PolygonProxy(arrowOutlineVerts, trans, scale, outlineColour, true, 1, pickingID));

	}

	private static void addShipProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
            long pickingID, ArrayList<RenderProxy> out) {

		// Now this is very 'shippy' behaviour and basically hand copied from the old DisplayModels (and supporting cast)

		// Hull
		Color4d hullColour = tags.getTagColourUtil(DisplayModel.TAG_BODY, ColourInput.LIGHT_GREY);
		out.add(new PolygonProxy(hullVerts, trans, scale, hullColour, false, 1, pickingID));

		// Outline
		Color4d outlineColour= tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		out.add(new PolygonProxy(hullVerts, trans, scale, outlineColour, true, 1, pickingID));

		// Cabin
		out.add(new PolygonProxy(shipCabinVerts, trans, scale, ColourInput.BLACK, false, 1, pickingID));

		// Add the contents parcels
		DoubleVector sizes = tags.sizes.get(DisplayModel.TAG_CONTENTS);
		Color4d[] colours = tags.colours.get(DisplayModel.TAG_CONTENTS);

		out.addAll(buildContents(sizes, colours, shipContentsTrans, trans, scale, pickingID));

	}

	private static void addTruckProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                             long pickingID, ArrayList<RenderProxy> out) {

		// Add a yellow rectangle for the cab
		out.add(new PolygonProxy(truckCabVerts, trans, scale, ColourInput.YELLOW, false, 1, pickingID));

		DoubleVector sizes = tags.sizes.get(DisplayModel.TAG_CONTENTS);
		Color4d[] colours = tags.colours.get(DisplayModel.TAG_CONTENTS);

		out.addAll(buildContents(sizes, colours, truckContentsTrans, trans, scale, pickingID));
	}

	private static void addBarGaugeProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                long pickingID, ArrayList<RenderProxy> out) {
		DoubleVector sizes = tags.sizes.get(DisplayModel.TAG_CONTENTS);
		Color4d[] colours = tags.colours.get(DisplayModel.TAG_CONTENTS);
		Color4d[] outlineColour = tags.colours.get(DisplayModel.TAG_OUTLINES);
		Color4d[] backgroundColour = tags.colours.get(DisplayModel.TAG_BODY);
		if (sizes == null) {
			sizes = new DoubleVector();
		}
		if (outlineColour == null || outlineColour.length < 1) {
			outlineColour = new Color4d[1];
			outlineColour[0] = ColourInput.BLACK;
		}
		if (backgroundColour == null || backgroundColour.length < 1) {
			backgroundColour = new Color4d[1];
			backgroundColour[0] = ColourInput.WHITE;
		}

		double width = 1.0;

		if (sizes.size() != 0) {
			width = 1.0 / sizes.size();
		}

		// Addthe background and outline
		out.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, backgroundColour[0], false, 1, pickingID));
		out.add(new PolygonProxy(RenderUtils.RECT_POINTS, trans, scale, outlineColour[0], true, 1, pickingID));

		if (colours == null || colours.length < sizes.size()) {
			return;
		} // Bail out, not properly initialized

		for (int i = 0; i < sizes.size(); ++i) {
			// Add a rectangle for each size

			double size = sizes.get(i);

			double startX = i*width - 0.5;
			double endX = (i+1)*width - 0.5;

			double startY = -0.5;
			double endY = size - 0.5;

			List<Vector4d> contentsPoints = new ArrayList<Vector4d>();
			contentsPoints.add(new Vector4d(  endX, startY, 0));
			contentsPoints.add(new Vector4d(  endX,   endY, 0));
			contentsPoints.add(new Vector4d(startX,   endY, 0));
			contentsPoints.add(new Vector4d(startX, startY, 0));

			out.add(new PolygonProxy(contentsPoints, trans, scale, colours[0], false, 1, pickingID));

		}
	}

	private static void addCrushingPlantProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                     long pickingID, ArrayList<RenderProxy> out) {

		Color4d outlineColour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		Color4d fillColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.MED_GREY);

		// Top
		out.add(new PolygonProxy(crushingPlantTopVerts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(crushingPlantTopVerts, trans, scale, outlineColour, true, 1, pickingID));

		// Bottom
		out.add(new PolygonProxy(crushingPlantBotVerts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(crushingPlantBotVerts, trans, scale, outlineColour, true, 1, pickingID));
	}

	private static void addSingleQuadProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                  long pickingID, ArrayList<RenderProxy> out) {

		// Add the lines
		Matrix4d lineTrans = new Matrix4d();
		trans.getMatrix(lineTrans);
		lineTrans.mult(Matrix4d.ScaleMatrix(scale), lineTrans);
		List<Vector4d> points = RenderUtils.transformPoints(lineTrans, singleQuadLinePoints);
		out.add(new LineProxy(points, ColourInput.BLACK, 1, pickingID));

		Color4d outlineColour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		Color4d fillColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.MED_GREY);

		out.add(new PolygonProxy(singleQuadRectVerts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(singleQuadRectVerts, trans, scale, outlineColour, true, 1, pickingID));
	}

	private static void addDualQuadProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                long pickingID, ArrayList<RenderProxy> out) {
		// Add the lines
		Matrix4d lineTrans = new Matrix4d();
		trans.getMatrix(lineTrans);
		lineTrans.mult(Matrix4d.ScaleMatrix(scale), lineTrans);
		List<Vector4d> points = RenderUtils.transformPoints(lineTrans, dualQuadLinePoints);
		out.add(new LineProxy(points, ColourInput.BLACK, 1, pickingID));

		Color4d outlineColour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		Color4d fillColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.MED_GREY);

		out.add(new PolygonProxy(dualQuadRect0Verts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(dualQuadRect1Verts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(dualQuadRect2Verts, trans, scale, fillColour, false, 1, pickingID));

		out.add(new PolygonProxy(dualQuadOutlineVerts, trans, scale, outlineColour, true, 1, pickingID));

	}

	private static void addTravellingProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
	                                  long pickingID, ArrayList<RenderProxy> out) {

		Color4d outlineColour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		Color4d fillColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.MED_GREY);
		Color4d trackColour = tags.getTagColourUtil(DisplayModel.TAG_TRACKFILL, ColourInput.MED_GREY);

		out.add(new PolygonProxy(travellingRect1Verts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(travellingRect1Verts, trans, scale, outlineColour, true, 1, pickingID));

		out.add(new PolygonProxy(travellingRect3Verts, trans, scale, fillColour, false, 1, pickingID));
		out.add(new PolygonProxy(travellingRect3Verts, trans, scale, outlineColour, true, 1, pickingID));

		out.add(new PolygonProxy(travellingRect2Verts, trans, scale, trackColour, false, 1, pickingID));
		out.add(new PolygonProxy(travellingRect2Verts, trans, scale, trackColour, true, 1, pickingID));
	}

	private static void addStackerProxies(DisplayEntity.TagSet tags, Transform trans, Vector4d scale,
            long pickingID, ArrayList<RenderProxy> out) {

		Color4d outlineColour = tags.getTagColourUtil(DisplayModel.TAG_OUTLINES, ColourInput.BLACK);
		Color4d contentsColour = tags.getTagColourUtil(DisplayModel.TAG_CONTENTS, ColourInput.MED_GREY);
		Color4d trackColour = ColourInput.MED_GREY;

		// This is gross, but until we have proper draw ordering it's the kind of thing we have to do to keep the
		// Stacker-reclaimer appearing above the stock piles reliably
		Vector4d fixedScale = new Vector4d(scale);
		fixedScale.data[2] = 0.1;

		out.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, trackColour, false, 1, pickingID));
		out.add(new PolygonProxy(stackerRect1Verts, trans, fixedScale, outlineColour, true, 1, pickingID));

		out.add(new PolygonProxy(stackerRect2Verts, trans, fixedScale, contentsColour, false, 1, pickingID));
		out.add(new PolygonProxy(stackerRect2Verts, trans, fixedScale, outlineColour, true, 1, pickingID));

	}

	private static void addImageProxy(String filename, Transform trans, Vector4d scale, boolean isTransparent,
            boolean compressedTexture, long pickingID, ArrayList<RenderProxy> out) {
		try {
			out.add(new ImageProxy(new URL(Util.getAbsoluteFilePath(filename)), trans,
			                       scale, isTransparent, compressedTexture, pickingID));
		} catch (MalformedURLException e) {
			out.add(new ImageProxy(TexCache.BAD_TEXTURE, trans,
                    scale, isTransparent, compressedTexture, pickingID));
		}
	}

	/**
	 * Add a collada model proxy
	 * @param filename
	 * @param trans
	 * @param scale
	 * @param isTransparent
	 * @param pickingID
	 * @param out
	 */
	private static void addModelProxy(String filename, Transform trans, Vector4d scale,
                                      long pickingID, ArrayList<RenderProxy> out) {
		MeshProtoKey meshKey = _cachedKeys.get(filename);

		// We have not loaded this file before, cache the mesh proto key so we don't dig through a zip file every scene
		if (meshKey == null) {
			try {
				URL meshURL = new URL(Util.getAbsoluteFilePath(filename));

				String ext = filename.substring(filename.length() - 4, filename.length());

				if (ext.toUpperCase().equals(".ZIP")) {
					// This is a zip, use a zip stream to actually pull out the .dae file
					ZipInputStream zipInputStream = new ZipInputStream(meshURL.openStream());

					// Loop through zipEntries
					for (ZipEntry zipEntry; (zipEntry = zipInputStream.getNextEntry()) != null; ) {

						String entryName = zipEntry.getName();
						if(!Util.getFileExtention(entryName).equalsIgnoreCase("DAE"))
							continue;

						// This zipEntry is a collada file, no need to look any further
						meshURL = new URL("jar:" + meshURL + "!/" + entryName );
						break;
					}
				}

				meshKey = new MeshProtoKey(meshURL);
				_cachedKeys.put(filename, meshKey);
			} catch (MalformedURLException e) {
				e.printStackTrace();
				assert(false);
			} catch (IOException e) {
				assert(false);
			}
		}

		if (meshKey != null) {
			AABB bounds = RenderManager.inst().getMeshBounds(meshKey, true);
			if (bounds == null) {
				// This mesh has not been loaded yet, try again next time
				return;
			}

			// Tweak the transform and scale to adjust for the bounds of the loaded model
			Vector4d offset = bounds.getCenter();
			Vector4d boundsRad = bounds.getRadius();
			if (boundsRad.data[2] == 0) {
				boundsRad.data[2] = 1;
			}

			Vector4d fixedScale = new Vector4d(0.5 * scale.x() / boundsRad.x(),
                                               0.5 * scale.y() / boundsRad.y(),
                                               0.5 * scale.z() / boundsRad.z());

			offset.data[0] *= -1 * fixedScale.x();
			offset.data[1] *= -1 * fixedScale.y();
			offset.data[2] *= -1 * fixedScale.z();

			Transform fixedTrans = new Transform(trans);
			fixedTrans.merge(new Transform(offset), fixedTrans);

			out.add(new MeshProxy(meshKey, fixedTrans, fixedScale, pickingID));
		}

	}

	// A disturbingly deep helper to allow trucks and ships to share contents building code
	// This class needs to either die or get refactored
	private static List<RenderProxy> buildContents(DoubleVector sizes, Color4d[] colours, Matrix4d subTrans,
	                                        Transform trans, Vector4d scale, long pickingID) {
		List<RenderProxy> ret = new ArrayList<RenderProxy>();

		if (sizes == null || colours == null || sizes.size() != colours.length) {
			// We are either out of sync or this is a ShipType, either way draw an empty cargo hold
			// Add a single grey rectangle
			List<Vector4d> contentsPoints = new ArrayList<Vector4d>();
			contentsPoints.add(new Vector4d( 1, -0.5, 0));
			contentsPoints.add(new Vector4d( 1,  0.5, 0));
			contentsPoints.add(new Vector4d( 0,  0.5, 0));
			contentsPoints.add(new Vector4d( 0, -0.5, 0));
			RenderUtils.transformPointsLocal(subTrans, contentsPoints);
			ret.add(new PolygonProxy(contentsPoints, trans, scale, ColourInput.LIGHT_GREY, false, 1, pickingID));
			return ret;
		}

		double totalSize = sizes.sum();
		double sizeOffset = 0;
		for (int i = 0; i < sizes.size(); ++i) {
			// Add a rectangle for

			double size = sizes.get(i);
			double start = sizeOffset / totalSize;
			double end = (sizeOffset + size) / totalSize;

			List<Vector4d> contentsPoints = new ArrayList<Vector4d>();
			contentsPoints.add(new Vector4d(  end, -0.5, 0));
			contentsPoints.add(new Vector4d(  end,  0.5, 0));
			contentsPoints.add(new Vector4d(start,  0.5, 0));
			contentsPoints.add(new Vector4d(start, -0.5, 0));

			sizeOffset += size;

			RenderUtils.transformPointsLocal(subTrans, contentsPoints);

			ret.add(new PolygonProxy(contentsPoints, trans, scale, colours[i], false, 1, pickingID));
		}
		return ret;
	}

	public static MeshProtoKey getCachedMeshKey(String shape) {
		// simply look into the key cache for now
		return _cachedKeys.get(shape);
	}
}
