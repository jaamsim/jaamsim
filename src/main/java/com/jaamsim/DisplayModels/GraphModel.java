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
package com.jaamsim.DisplayModels;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
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

public class GraphModel extends DisplayModel {

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof Graph;
	}

	private class Binding extends DisplayModelBinding {

		private Graph graphObservee;
		private ChangeWatcher.Tracker observeeTracker;

		private List<Vec4d> graphRectPoints = null;
		private Mat4d graphAreaTrans = null;
		private Mat4d graphToWorldTrans = null;
		private Mat4d objectTransComp = null; // The composed transform and scale as a Matrix4d
		private Transform objectTrans = null;
		private Vec4d objectScale = null;
		private long pickingID;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				graphObservee = (Graph)observee;
				if (graphObservee != null) {
					observeeTracker = graphObservee.getGraphicsChangeTracker();
					pickingID = graphObservee.getEntityNumber();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				graphObservee = null;
			}
		}

		@Override
		public void collectProxies(ArrayList<RenderProxy> out) {
			if (graphObservee == null || !graphObservee.getShow()) {
				return;
			}

			if (objectTrans == null || observeeTracker.checkAndClear()) {
				updateObjectTrans();
			}

			++_cacheMisses;
			registerCacheMiss("Graph");

			Color4d graphColour = graphObservee.getGraphColour();
			Color4d borderColour = graphObservee.getBorderColour();
			Color4d backgroundColour = graphObservee.getBackgroundColour();

			if (graphColour == null ||
			    borderColour == null ||
			    backgroundColour == null)
			{
				return ;
			}

			// Add the main frame and graph frame
			out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, backgroundColour, false, 1, getVisibilityInfo(), pickingID));
			out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, borderColour, true, 1, getVisibilityInfo(), pickingID));

			out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, graphColour, false, 1, getVisibilityInfo(), pickingID));
			out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, borderColour, true, 1, getVisibilityInfo(), pickingID));

			ArrayList<Graph.SeriesInfo> primarySeries = graphObservee.getPrimarySeries();

			double yMinPrimary = graphObservee.getYAxisStart();
			double yMaxPrimary = graphObservee.getYAxisEnd();

			// Labels, titles, graph lines, etc
			drawDecorations(out);

			// Draw the series
			for (int i = 0; i < primarySeries.size(); ++i) {
				drawSeries(primarySeries.get(i), yMinPrimary, yMaxPrimary, out);
			}

			ArrayList<Graph.SeriesInfo> secondarySeries = graphObservee.getSecondarySeries();

			double yMinSecondary = graphObservee.getSecondaryYAxisStart();
			double yMaxSecondary = graphObservee.getSecondaryYAxisEnd();
			for (int i = 0; i < secondarySeries.size(); ++i) {
				drawSeries(secondarySeries.get(i), yMinSecondary, yMaxSecondary, out);
			}

		}

		private void drawSeries(Graph.SeriesInfo series, double yMin, double yMax,
		                        ArrayList<RenderProxy> out) {

			int numberOfPoints = graphObservee.getNumberOfPoints();

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

			ArrayList<Vec4d> seriesPoints = new ArrayList<Vec4d>((series.numPoints-1)*2);
			for (int i = 0; i < series.numPoints - 1 - series.removedPoints; i++) {
				seriesPoints.add(new Vec4d(xVals[i  ], yVals[i  ], -0, 1.0d));
				seriesPoints.add(new Vec4d(xVals[i+1], yVals[i+1], -0, 1.0d));
			}

			// Transform from graph area to world space
			for (int i = 0; i < seriesPoints.size(); ++i) {
				seriesPoints.get(i).mult4(graphToWorldTrans, seriesPoints.get(i));
			}

			out.add(new LineProxy(seriesPoints, series.lineColour, series.lineWidth, getVisibilityInfo(), pickingID));

		}

		private void drawDecorations(ArrayList<RenderProxy> out) {

			double yMin= graphObservee.getYAxisStart();
			double yMax= graphObservee.getYAxisEnd();

			double yRange = yMax - yMin;

			double secYMin= graphObservee.getSecondaryYAxisStart();
			double secYMax= graphObservee.getSecondaryYAxisEnd();

			double secYRange = secYMax - secYMin;

			// Y Lines
			DoubleVector yLines = graphObservee.getYLines();

			ArrayList<Color4d> yLineColours = graphObservee.getYLineColours();

			for (int i = 0; i < yLines.size(); ++i) {
				double yPos = (yLines.get(i) - yMin) / yRange - 0.5;
				ArrayList<Vec4d> linePoints = new ArrayList<Vec4d>(2);

				linePoints.add(new Vec4d(-0.5, yPos, 0, 1.0d));
				linePoints.add(new Vec4d( 0.5, yPos, 0, 1.0d));

				Color4d colour = ColourInput.LIGHT_GREY;
				if (yLineColours.size() > i) {
					colour = yLineColours.get(i);
				} else if (yLineColours.size() >= 1) {
					colour = yLineColours.get(0);
				}

				// Transform to world space
				for (int j = 0; j < linePoints.size(); ++j) {
					linePoints.get(j).mult4(graphToWorldTrans, linePoints.get(j));
				}

				out.add(new LineProxy(linePoints, colour, 1, getVisibilityInfo(), pickingID));
			}

			// X lines
			double startTime = graphObservee.getStartTime();
			double endTime = graphObservee.getEndTime();
			double xRange = endTime - startTime;
			DoubleVector xLines = graphObservee.getXLines();

			ArrayList<Color4d> xLineColours = graphObservee.getXLineColours();

			for (int i = 0; i < xLines.size(); ++i) {
				double xPos = (xLines.get(i) - startTime) / xRange - 0.5;

				ArrayList<Vec4d> linePoints = new ArrayList<Vec4d>(2);

				linePoints.add(new Vec4d(xPos, -0.5, 0, 1.0d));
				linePoints.add(new Vec4d(xPos,  0.5, 0, 1.0d));

				Color4d colour = ColourInput.LIGHT_GREY;
				if (xLineColours.size() > i) {
					colour = xLineColours.get(i);
				} else if (xLineColours.size() >= 1) {
					colour = xLineColours.get(0);
				}

				// Transform to world space
				for (int j = 0; j < linePoints.size(); ++j) {
					linePoints.get(j).mult4(graphToWorldTrans, linePoints.get(j));
				}

				out.add(new LineProxy(linePoints, colour, 1,
						getVisibilityInfo(), graphObservee.getEntityNumber()));
			}


			//Vector4d graphCenter = _graphObservee.getGraphCenter();
			Vec3d graphSize = graphObservee.getGraphSize();
			Vec3d graphOrigin = graphObservee.getGraphOrigin();

			// Title
			Vec4d objectSize = graphObservee.getJaamMathSize(1.0d);
			double titleHeight = graphObservee.getTitleHeight();
			String titleText = graphObservee.getTitle();
			Color4d titleColour = graphObservee.getTitleColour();
			String fontName = graphObservee.getFontName();

			TessFontKey fontKey = new TessFontKey(fontName);

			Vec4d titleCenter = new Vec4d(0, graphOrigin.y + graphSize.y + titleHeight, 0, 1.0d);

			// Compensate for the non-linear scaling in the parent object
			double xScaleFactor = objectSize.y / objectSize.x;

			// These two matrices are needed to cancel out the object level non-uniform scaling for text objects
			// xScale if for horizontal text, yScale is for vertical text
			Vec3d xScaleVec = new Vec3d(xScaleFactor, 1, 1);

			Vec3d yScaleVec = new Vec3d(1/xScaleFactor, 1, 1);

			Mat4d titleTrans = new Mat4d();
			titleTrans.setTranslate3(titleCenter);
			titleTrans.mult4(objectTransComp, titleTrans);
			titleTrans.scaleCols3(xScaleVec);

			out.add(new StringProxy(titleText, fontKey, titleColour, titleTrans, titleHeight, getVisibilityInfo(), pickingID));

			// Y axis labels and ticks

			double yAxisInterval = graphObservee.getYAxisInterval();
			int yAxisPrecision = graphObservee.getYAxisPrecision();
			double yAxisLabelGap = graphObservee.getYAxisLabelGap();
			double xAxisLabelGap = graphObservee.getXAxisLabelGap();
			double labelHeight = graphObservee.getLabelHeight();
			double labelWidth = graphObservee.getLabelHeight() * xScaleFactor;
			Color4d labelColour = graphObservee.getLabelColour();

			double xAxisMult = graphObservee.getXAxisMultiplier();
			double yAxisMult = graphObservee.getYAxisMultiplier();
			double secYAxisMult = graphObservee.getSecondaryYAxisMultiplier();

			double xTickSize = labelHeight/2 * xScaleFactor;
			double yTickSize = labelHeight/2;

			ArrayList<Vec4d> tickPoints = new ArrayList<Vec4d>();

			double minYLabelXPos = graphOrigin.x;
			double maxYLabelXPos = graphOrigin.x + graphSize.x;
			// Y labels
			for (int i = 0; i * yAxisInterval <= yRange; ++i) {

				String text = String.format( "%." + yAxisPrecision + "f",  ( i * yAxisInterval + yMin ) * yAxisMult);

				// Find the rendered string size so we can right justify the labels
				Vec3d stringSize = RenderManager.inst().getRenderedStringSize(fontKey, labelHeight, text);

				double yPos = graphOrigin.y + (i * yAxisInterval * graphSize.y )/yRange; // current label
				// Right justify the label
				double rightJustifyFactor = stringSize.x * xScaleFactor * 0.5;
				double xPos = graphOrigin.x - yAxisLabelGap - xTickSize - labelWidth - rightJustifyFactor;

				if (xPos - stringSize.x * xScaleFactor < minYLabelXPos) {
					minYLabelXPos = xPos - stringSize.x * xScaleFactor;
				}

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, 0));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				Vec4d tickPointA = new Vec4d(graphOrigin.x            , yPos, 0, 1.0d);
				Vec4d tickPointB = new Vec4d(graphOrigin.x - xTickSize, yPos, 0, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			double secYAxisInterval = graphObservee.getSecondaryYAxisInterval();
			int secYAxisPrecision = graphObservee.getSecondaryYAxisPrecision();

			// Secondary Y labels
			for (int i = 0; i * secYAxisInterval <= secYRange; ++i) {
				// The defaults for the secondary axis allow for an infinite loop, check here and bail
				if (secYAxisInterval == 0 || yRange == 0) {
					break;
				}

				String text = String.format( "%." + secYAxisPrecision + "f",  ( i * secYAxisInterval + secYMin ) * secYAxisMult);

				// Find the rendered string size so we can right justify the labels
				Vec3d stringSize = RenderManager.inst().getRenderedStringSize(fontKey, labelHeight, text);

				double yPos = graphOrigin.y + (i * secYAxisInterval * graphSize.y )/secYRange; // current label
				// Right justify the label
				double leftJustifyFactor = stringSize.x * xScaleFactor * 0.5;
				double xPos = graphOrigin.x + graphSize.x + yAxisLabelGap + xTickSize + labelWidth + leftJustifyFactor;

				if (xPos + stringSize.x * xScaleFactor > maxYLabelXPos) {
					maxYLabelXPos = xPos + stringSize.x * xScaleFactor;
				}

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, 0));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				Vec4d tickPointA = new Vec4d(graphOrigin.x + graphSize.x            , yPos, 0, 1.0d);
				Vec4d tickPointB = new Vec4d(graphOrigin.x + graphSize.x + xTickSize, yPos, 0, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);

				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			double timeInterval = graphObservee.getTimeInterval();
			int xAxisPrecision = graphObservee.getXAxisPrecision();
			String xAxisUnits = graphObservee.getXAxisUnits();


			// X labels
			for (int i = 0; startTime + i*timeInterval <= endTime; ++i) {

				double time = (startTime + i * timeInterval);
				String text;
				if (time == 0) {
					text = "Now";
				} else {
					text = String.format( "%." + xAxisPrecision + "f" + xAxisUnits, time * xAxisMult);
				}

				double xPos = graphOrigin.x + ( ( i * timeInterval) * graphSize.x)/xRange;
				double yPos = graphOrigin.y - xAxisLabelGap/objectScale.y - labelHeight - yTickSize;

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, 0));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				Vec4d tickPointA = new Vec4d(xPos, graphOrigin.y, 0, 1.0d);
				Vec4d tickPointB = new Vec4d(xPos, graphOrigin.y - yTickSize, 0, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			out.add(new LineProxy(tickPoints, labelColour, 1, getVisibilityInfo(), pickingID));

			// The Y Axis label
			String yAxisTitle = graphObservee.getYAxisTitle();
			String secYAxisTitle = graphObservee.getSecondaryYAxisTitle();
			double yAxisTitleHeight = graphObservee.getYAxisTitleHeight();
			double yAxisTitleGap = graphObservee.getYAxisTitleGap();
			double xPos = minYLabelXPos - yAxisTitleGap;
			double secXPos = maxYLabelXPos + yAxisTitleGap;

			Mat4d ytitleTrans = new Mat4d();
			ytitleTrans.setTranslate3(new Vec3d(xPos, 0, 0));
			ytitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
			ytitleTrans.mult4(objectTransComp, ytitleTrans);
			ytitleTrans.scaleCols3(yScaleVec);

			out.add(new StringProxy(yAxisTitle, fontKey, titleColour, ytitleTrans, yAxisTitleHeight, getVisibilityInfo(), pickingID));

			Mat4d secYtitleTrans = new Mat4d();
			secYtitleTrans.setTranslate3(new Vec3d(secXPos, 0, 0));
			secYtitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
			secYtitleTrans.mult4(objectTransComp, secYtitleTrans);
			secYtitleTrans.scaleCols3(yScaleVec);

			out.add(new StringProxy(secYAxisTitle, fontKey, titleColour, secYtitleTrans, yAxisTitleHeight, getVisibilityInfo(), pickingID));

		}

		private void updateObjectTrans() {
			double simTime = graphObservee.getCurrentTime();
			objectTrans = graphObservee.getGlobalTrans(simTime);
			objectScale = graphObservee.getJaamMathSize(getModelScale());

			objectTransComp = new Mat4d();
			objectTrans.getMat4d(objectTransComp);
			objectTransComp.scaleCols3(objectScale);

			Vec3d graphCenter = graphObservee.getGraphCenter();
			Vec3d graphSize = graphObservee.getGraphSize();

			graphAreaTrans = new Mat4d();
			graphAreaTrans.setTranslate3(graphCenter);
			graphAreaTrans.scaleCols3(graphSize);

			graphRectPoints = RenderUtils.transformPoints(graphAreaTrans, RenderUtils.RECT_POINTS, 0);

			graphToWorldTrans = new Mat4d();
			graphToWorldTrans.mult4(objectTransComp, graphAreaTrans);

		}

	}
}
