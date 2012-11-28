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

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.PolygonProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.StringProxy;
import com.jaamsim.render.TessFontKey;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.Graph;

public class GraphObserver extends RenderObserver {

	private Graph _graphObservee;
	private ChangeWatcher.Tracker _observeeTracker;

	private List<Vector4d> graphRectPoints = null;
	private Matrix4d graphAreaTrans = null;
	private Matrix4d graphToWorldTrans = null;
	private Matrix4d objectTransComp = null; // The composed transform and scale as a Matrix4d
	private Transform objectTrans = null;
	private Vector4d objectScale = null;
	private final long pickingID;

	GraphObserver(Entity observee) {
		super(observee);

		try {
			_graphObservee = (Graph)observee;
			_observeeTracker = _graphObservee.getGraphicsChangeTracker();
		} catch (ClassCastException e) {
			// The observee is not a display entity
			_graphObservee = null;
			// Debug assert, not actually an error
			assert(false);
		}
		pickingID = _graphObservee.getEntityNumber();

	}

	@Override
	public void collectProxies(ArrayList<RenderProxy> out) {

		if (objectTrans == null || _observeeTracker.checkAndClear()) {
			updateObjectTrans();
		}

		++_cacheMisses;

		Color4d graphColour = _graphObservee.getGraphColour();
		Color4d borderColour = _graphObservee.getBorderColour();
		Color4d backgroundColour = _graphObservee.getBackgroundColour();

		if (graphColour == null ||
		    borderColour == null ||
		    backgroundColour == null)
		{
			return ;
		}

		// Add the main frame and graph frame
		out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, backgroundColour, false, 1, pickingID));
		out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, borderColour, true, 1, pickingID));

		out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, graphColour, false, 1, pickingID));
		out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, borderColour, true, 1, pickingID));

		ArrayList<Graph.SeriesInfo> primarySeries = _graphObservee.getPrimarySeries();

		double yMinPrimary = _graphObservee.getYAxisStart();
		double yMaxPrimary = _graphObservee.getYAxisEnd();

		// Labels, titles, graph lines, etc
		drawDecorations(out);

		// Draw the series
		for (int i = 0; i < primarySeries.size(); ++i) {
			drawSeries(primarySeries.get(i), yMinPrimary, yMaxPrimary, out);
		}

		ArrayList<Graph.SeriesInfo> secondarySeries = _graphObservee.getSecondarySeries();

		double yMinSecondary = _graphObservee.getSecondaryYAxisStart();
		double yMaxSecondary = _graphObservee.getSecondaryYAxisEnd();
		for (int i = 0; i < secondarySeries.size(); ++i) {
			drawSeries(secondarySeries.get(i), yMinSecondary, yMaxSecondary, out);
		}

	}

	private void drawSeries(Graph.SeriesInfo series, double yMin, double yMax,
	                        ArrayList<RenderProxy> out) {

		int numberOfPoints = _graphObservee.getNumberOfPoints();

		double xInc = 1.0 / (numberOfPoints - 1.0);

		double yRange = yMax - yMin;

		if (series.numPoints < 2) {
			return; // Nothing to display yet
		}

		double[] yVals = new double[series.numPoints];
		double[] xVals = new double[series.numPoints];
		for (int i = 0; i < series.numPoints; i++) {
			xVals[i] = (i*xInc) - (series.numPoints-1)*xInc + 0.5;
			yVals[i] = MathUtils.bound((series.values[i] - yMin) / yRange, 0, 1) - 0.5; // Bound the y values inside the graph range
		}

		ArrayList<Vector4d> seriesPoints = new ArrayList<Vector4d>((series.numPoints-1)*2);
		for (int i = 0; i < series.numPoints - 1 - series.removedPoints; i++) {
			seriesPoints.add(new Vector4d(xVals[i  ], yVals[i  ], -0));
			seriesPoints.add(new Vector4d(xVals[i+1], yVals[i+1], -0));
		}

		// Transform from graph area to world space
		RenderUtils.transformPointsLocal(graphToWorldTrans, seriesPoints);

		out.add(new LineProxy(seriesPoints, series.lineColour, series.lineWidth, pickingID));

	}

	private void drawDecorations(ArrayList<RenderProxy> out) {

		double yMin= _graphObservee.getYAxisStart();
		double yMax= _graphObservee.getYAxisEnd();

		double yRange = yMax - yMin;

		// Y Lines
		DoubleVector yLines = _graphObservee.getYLines();

		ArrayList<Color4d> yLineColours = _graphObservee.getYLineColours();

		for (int i = 0; i < yLines.size(); ++i) {
			double yPos = (yLines.get(i) - yMin) / yRange - 0.5;
			ArrayList<Vector4d> linePoints = new ArrayList<Vector4d>(2);

			linePoints.add(new Vector4d(-0.5, yPos, 0));
			linePoints.add(new Vector4d( 0.5, yPos, 0));

			Color4d colour = ColourInput.LIGHT_GREY;
			if (yLineColours.size() > i) {
				colour = yLineColours.get(i);
			} else if (yLineColours.size() >= 1) {
				colour = yLineColours.get(0);
			}

			// Transform to world space
			RenderUtils.transformPointsLocal(graphToWorldTrans, linePoints);

			out.add(new LineProxy(linePoints, colour, 1, pickingID));
		}

		// X lines
		double startTime = _graphObservee.getStartTime();
		double endTime = _graphObservee.getEndTime();
		double xRange = endTime - startTime;
		DoubleVector xLines = _graphObservee.getXLines();

		ArrayList<Color4d> xLineColours = _graphObservee.getXLineColours();

		for (int i = 0; i < xLines.size(); ++i) {
			double xPos = (xLines.get(i) - startTime) / xRange - 0.5;

			ArrayList<Vector4d> linePoints = new ArrayList<Vector4d>(2);

			linePoints.add(new Vector4d(xPos, -0.5, 0));
			linePoints.add(new Vector4d(xPos,  0.5, 0));

			Color4d colour = ColourInput.LIGHT_GREY;
			if (xLineColours.size() > i) {
				colour = xLineColours.get(i);
			} else if (xLineColours.size() >= 1) {
				colour = xLineColours.get(0);
			}

			// Transform to world space
			RenderUtils.transformPointsLocal(graphToWorldTrans, linePoints);

			out.add(new LineProxy(linePoints, colour, 1,
			                      _graphObservee.getEntityNumber()));
		}


		//Vector4d graphCenter = _graphObservee.getGraphCenter();
		Vector4d graphSize = _graphObservee.getGraphSize();
		Vector4d graphOrigin = _graphObservee.getGraphOrigin();

		// Title
		Vector4d objectSize = _graphObservee.getJaamMathSize();
		double titleHeight = _graphObservee.getTitleHeight();
		String titleText = _graphObservee.getTitle();
		Color4d titleColour = _graphObservee.getTitleColour();
		String fontName = _graphObservee.getFontName();

		TessFontKey fontKey = new TessFontKey(fontName);

		Vector4d titleCenter = new Vector4d(0, graphOrigin.y() + graphSize.y() + titleHeight, 0);

		// Compensate for the non-linear scaling in the parent object
		double xScaleFactor = objectSize.y() / objectSize.x();

		// These two matrices are needed to cancel out the object level non-uniform scaling for text objects
		// xScale if for horizontal text, yScale is for vertical text
		Matrix4d xScaleMat = Matrix4d.ScaleMatrix(new Vector4d(xScaleFactor, 1, 1));
		Matrix4d yScaleMat = Matrix4d.ScaleMatrix(new Vector4d(1/xScaleFactor, 1, 1));

		Matrix4d titleTrans = Matrix4d.TranslationMatrix(titleCenter);
		objectTransComp.mult(titleTrans, titleTrans);

		titleTrans.mult(xScaleMat, titleTrans);

		out.add(new StringProxy(titleText, fontKey, titleColour, titleTrans, titleHeight, pickingID));

		// Y axis labels and ticks

		double yAxisInterval = _graphObservee.getYAxisInterval();
		int yAxisPrecision = _graphObservee.getYAxisPrecision();
		double yAxisLabelGap = _graphObservee.getYAxisLabelGap();
		double xAxisLabelGap = _graphObservee.getXAxisLabelGap();
		double labelHeight = _graphObservee.getLabelHeight();
		double labelWidth = _graphObservee.getLabelHeight() * xScaleFactor;
		Color4d labelColour = _graphObservee.getLabelColour();

		double xTickSize = labelHeight/2 * xScaleFactor;
		double yTickSize = labelHeight/2;

		ArrayList<Vector4d> tickPoints = new ArrayList<Vector4d>();

		double minYLabelXPos = graphOrigin.x();
		// Y labels
		for (int i = 0; i * yAxisInterval <= yRange; ++i) {

			String text = String.format( "%." + yAxisPrecision + "f",  ( i * yAxisInterval + yMin ));

			// Find the rendered string size so we can right justify the labels
			Vector4d stringSize = RenderManager.inst().getRenderedStringSize(fontKey, labelHeight, text);

			double yPos = graphOrigin.y() + (i * yAxisInterval * graphSize.y() )/yRange; // current label
			// Right justify the label
			double rightJustifyFactor = stringSize.x() * xScaleFactor * 0.5;
			double xPos = graphOrigin.x() - yAxisLabelGap - xTickSize - labelWidth - rightJustifyFactor;

			if (xPos - stringSize.x() * xScaleFactor < minYLabelXPos) {
				minYLabelXPos = xPos - stringSize.x() * xScaleFactor;
			}

			Matrix4d labelTrans = Matrix4d.TranslationMatrix(new Vector4d(xPos, yPos, 0));

			objectTransComp.mult(labelTrans, labelTrans);
			labelTrans.mult(xScaleMat, labelTrans);

			out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, pickingID));

			Vector4d tickPointA = new Vector4d(graphOrigin.x()            , yPos, 0);
			Vector4d tickPointB = new Vector4d(graphOrigin.x() - xTickSize, yPos, 0);
			objectTransComp.mult(tickPointA, tickPointA);
			objectTransComp.mult(tickPointB, tickPointB);
			tickPoints.add(tickPointA);
			tickPoints.add(tickPointB);
		}

		double timeInterval = _graphObservee.getTimeInterval();
		int xAxisPrecision = _graphObservee.getXAxisPrecision();
		String xAxisUnits = _graphObservee.getXAxisUnits();


		// X labels
		for (int i = 0; startTime + i*timeInterval <= endTime; ++i) {

			double time = (startTime + i * timeInterval);
			String text;
			if (time == 0) {
				text = "Now";
			} else {
				text = String.format( "%." + xAxisPrecision + "f" + xAxisUnits, time);
			}

			double xPos = graphOrigin.x() + ( ( i * timeInterval) * graphSize.x())/xRange;
			double yPos = graphOrigin.y() - xAxisLabelGap/objectScale.y() - labelHeight - yTickSize;

			Matrix4d labelTrans = Matrix4d.TranslationMatrix(new Vector4d(xPos, yPos, 0));

			objectTransComp.mult(labelTrans, labelTrans);
			labelTrans.mult(xScaleMat, labelTrans);

			out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, pickingID));

			Vector4d tickPointA = new Vector4d(xPos, graphOrigin.y(), 0);
			Vector4d tickPointB = new Vector4d(xPos, graphOrigin.y() - yTickSize, 0);
			objectTransComp.mult(tickPointA, tickPointA);
			objectTransComp.mult(tickPointB, tickPointB);
			tickPoints.add(tickPointA);
			tickPoints.add(tickPointB);
		}

		out.add(new LineProxy(tickPoints, labelColour, 1, pickingID));

		// The Y Axis label
		String yAxisTitle = _graphObservee.getYAxisTitle();
		//String secondaryYAxisTitle = _graphObservee.getSecondaryYAxisTitle();
		double yAxisTitleHeight = _graphObservee.getYAxisTitleHeight();
		double yAxisTitleGap = _graphObservee.getYAxisTitleGap();
		double xPos = minYLabelXPos - yAxisTitleGap;

		Matrix4d ytitleTrans = Matrix4d.TranslationMatrix(new Vector4d(xPos, 0, 0));
		ytitleTrans.mult(Matrix4d.RotationMatrix(Math.PI/2, Vector4d.Z_AXIS), ytitleTrans);

		objectTransComp.mult(ytitleTrans, ytitleTrans);
		ytitleTrans.mult(yScaleMat, ytitleTrans);

		out.add(new StringProxy(yAxisTitle, fontKey, titleColour, ytitleTrans, yAxisTitleHeight, pickingID));

	}

	private void updateObjectTrans() {
		double simTime = _dispObservee.getCurrentTime();
		objectTrans = _dispObservee.getGlobalTrans(simTime);
		objectScale = _dispObservee.getJaamMathSize();

		objectTransComp = new Matrix4d();
		objectTrans.getMatrix(objectTransComp);
		objectTransComp.mult(Matrix4d.ScaleMatrix(objectScale), objectTransComp);

		Vector4d graphCenter = _graphObservee.getGraphCenter();
		Vector4d graphSize = _graphObservee.getGraphSize();
		graphAreaTrans = Matrix4d.TranslationMatrix(graphCenter);
		graphAreaTrans.mult(Matrix4d.ScaleMatrix(graphSize), graphAreaTrans);
		graphRectPoints = RenderUtils.transformPoints(graphAreaTrans, RenderUtils.RECT_POINTS);

		graphToWorldTrans = new Matrix4d();
		objectTransComp.mult(graphAreaTrans, graphToWorldTrans);

	}
}
