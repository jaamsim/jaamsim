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
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
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

		private List<Vec4d> graphRectPoints = null;
		private Mat4d graphAreaTrans = null;
		private Mat4d graphToWorldTrans = null;
		private Mat4d objectTransComp = null; // The composed transform and scale as a Matrix4d
		private Transform objectTrans = null;
		private Vec3d objectScale = null;
		private long pickingID;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				graphObservee = (Graph)observee;
				if (graphObservee != null) {
					pickingID = graphObservee.getEntityNumber();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				graphObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (graphObservee == null || !graphObservee.getShow()) {
				return;
			}

			updateObjectTrans(simTime);

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
			double zBump = 0.001;

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
			// Loop through one fewer points in the series to reduce the chance that the Render
			// will need to trap an index of of bound error
			//for (int i = 0; i < series.numPoints - 1 - series.removedPoints; i++) {
			for (int i = 0; i < series.numPoints - 2 - series.removedPoints; i++) {
				seriesPoints.add(new Vec4d(xVals[i  ], yVals[i  ], zBump, 1.0d));
				seriesPoints.add(new Vec4d(xVals[i+1], yVals[i+1], zBump, 1.0d));
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

			double graphZBump = 0.001;

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

				linePoints.add(new Vec4d(-0.5, yPos, graphZBump, 1.0d));
				linePoints.add(new Vec4d( 0.5, yPos, graphZBump, 1.0d));

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

				linePoints.add(new Vec4d(xPos, -0.5, graphZBump, 1.0d));
				linePoints.add(new Vec4d(xPos,  0.5, graphZBump, 1.0d));

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
			double decZBump = graphSize.x * 0.001;

			// Graph Title
			Vec3d objectSize = graphObservee.getSize();
			double titleHeight = graphObservee.getTitleHeight();
			double titleGap = graphObservee.getTitleGap();
			String titleText = graphObservee.getTitle();
			Color4d titleColour = graphObservee.getTitleColour();
			String fontName = graphObservee.getFontName();

			TessFontKey fontKey = new TessFontKey(fontName);

			Vec4d titleCenter = new Vec4d(0, graphOrigin.y + graphSize.y + titleGap + titleHeight/2, decZBump, 1.0d);

			// This factor is applied to lengths expressed as a fraction of the graph's y-extent and
			// converts then to fractions of the graph's x-extent
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

			// Y-Axis Labels and Tick Marks
			double yAxisInterval = graphObservee.getYAxisInterval();
			String yAxisLabelFormat = graphObservee.getYAxisLabelFormat();
			double yAxisLabelGap = graphObservee.getYAxisLabelGap();
			double labelHeight = graphObservee.getLabelHeight();
			Color4d labelColour = graphObservee.getLabelColour();

			Unit yAxisUnit = graphObservee.getYAxisUnit();
			double yAxisFactor = 1.0;
			if( yAxisUnit != null )
				yAxisUnit.getConversionFactorToSI();

			double xTickSize = labelHeight/2 * xScaleFactor; // horizontal tick marks for the y-axis
			double yTickSize = labelHeight/2; // vertical tick marks for the x-axis

			ArrayList<Vec4d> tickPoints = new ArrayList<Vec4d>();

			double minYLabelXPos = graphOrigin.x;

			for (int i = 0; i * yAxisInterval <= yRange; ++i) {

				String text = String.format( yAxisLabelFormat,  ( i * yAxisInterval + yMin )/yAxisFactor);
				double yPos = graphOrigin.y + (i * yAxisInterval * graphSize.y )/yRange; // current label

				// Right justify the labels
				Vec3d stringSize = RenderManager.inst().getRenderedStringSize(fontKey, labelHeight*xScaleFactor, text);
				double rightJustifyOffset = stringSize.x * 0.5;
				double xPos = graphOrigin.x - xTickSize - yAxisLabelGap*xScaleFactor - rightJustifyOffset;

				// Save the left-most extent of the labels
				minYLabelXPos = Math.min(minYLabelXPos, xPos - rightJustifyOffset);

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, decZBump));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				// Prepare the tick marks
				Vec4d tickPointA = new Vec4d(graphOrigin.x            , yPos, decZBump, 1.0d);
				Vec4d tickPointB = new Vec4d(graphOrigin.x - xTickSize, yPos, decZBump, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			// Secondary Y-Axis Labels and Tick Marks
			String secYAxisLabelFormat = graphObservee.getSecondaryYAxisLabelFormat();
			double secYAxisInterval = graphObservee.getSecondaryYAxisInterval();

			Unit secYAxisUnit = graphObservee.getSecondaryYAxisUnit();
			double secYAxisFactor = 1.0;
			if( secYAxisUnit != null )
				secYAxisUnit.getConversionFactorToSI();

			double maxYLabelXPos = graphOrigin.x + graphSize.x;

			if (! graphObservee.getSecondarySeries().isEmpty() ) {
				for (int i = 0; i * secYAxisInterval <= secYRange; ++i) {

					String text = String.format( secYAxisLabelFormat,  ( i * secYAxisInterval + secYMin )/secYAxisFactor);
					double yPos = graphOrigin.y + (i * secYAxisInterval * graphSize.y )/secYRange; // current label

					// Right justify the labels
					Vec3d stringSize = RenderManager.inst().getRenderedStringSize(fontKey, labelHeight*xScaleFactor, text);
					double leftJustifyOffset = stringSize.x * 0.5;
					double xPos = graphOrigin.x + graphSize.x + xTickSize + yAxisLabelGap*xScaleFactor + leftJustifyOffset;

					// Save the right-most extent of the labels
					maxYLabelXPos = Math.max(maxYLabelXPos, xPos + leftJustifyOffset);

					Mat4d labelTrans = new Mat4d();
					labelTrans.setTranslate3(new Vec3d(xPos, yPos, decZBump));
					labelTrans.mult4(objectTransComp, labelTrans);
					labelTrans.scaleCols3(xScaleVec);

					out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

					// Prepare the tick marks
					Vec4d tickPointA = new Vec4d(graphOrigin.x + graphSize.x            , yPos, decZBump, 1.0d);
					Vec4d tickPointB = new Vec4d(graphOrigin.x + graphSize.x + xTickSize, yPos, decZBump, 1.0d);
					tickPointA.mult4(objectTransComp, tickPointA);
					tickPointB.mult4(objectTransComp, tickPointB);

					tickPoints.add(tickPointA);
					tickPoints.add(tickPointB);
				}
			}

			// X-Axis Labels and Tick Marks
			double timeInterval = graphObservee.getTimeInterval();
			String xAxisFormat = graphObservee.getXAxisLabelFormat();
			double xAxisLabelGap = graphObservee.getXAxisLabelGap();

			TimeUnit xAxisUnit = graphObservee.getXAxisUnit();
			double xAxisFactor = 1.0;
			if( xAxisUnit != null )
				xAxisFactor = xAxisUnit.getConversionFactorToSI();

			for (int i = 0; startTime + i*timeInterval <= endTime; ++i) {

				double time = (startTime + i * timeInterval);
				String text;
				if (time == 0) {
					text = "Now";
				} else {
					text = String.format( xAxisFormat, time/xAxisFactor);
				}

				double xPos = graphOrigin.x + ( i * timeInterval * graphSize.x)/xRange;
				double yPos = graphOrigin.y - yTickSize - xAxisLabelGap - labelHeight/2;

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, decZBump));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, fontKey, labelColour, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				// Prepare the tick marks
				Vec4d tickPointA = new Vec4d(xPos, graphOrigin.y, decZBump, 1.0d);
				Vec4d tickPointB = new Vec4d(xPos, graphOrigin.y - yTickSize, decZBump, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			out.add(new LineProxy(tickPoints, labelColour, 1, getVisibilityInfo(), pickingID));

			// Primary Y-Axis Title
			String yAxisTitle = graphObservee.getYAxisTitle();
			double yAxisTitleHeight = graphObservee.getYAxisTitleHeight()*xScaleFactor;
			double yAxisTitleGap = graphObservee.getYAxisTitleGap()*xScaleFactor;
			double xPos = minYLabelXPos - yAxisTitleGap - yAxisTitleHeight/2;

			Mat4d ytitleTrans = new Mat4d();
			ytitleTrans.setTranslate3(new Vec3d(xPos, 0, decZBump));
			ytitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
			ytitleTrans.mult4(objectTransComp, ytitleTrans);
			ytitleTrans.scaleCols3(yScaleVec);

			out.add(new StringProxy(yAxisTitle, fontKey, titleColour, ytitleTrans, yAxisTitleHeight, getVisibilityInfo(), pickingID));

			// Secondary Y-Axis Title
			if (! graphObservee.getSecondarySeries().isEmpty() ) {
				String secYAxisTitle = graphObservee.getSecondaryYAxisTitle();
				double secXPos = maxYLabelXPos + yAxisTitleGap + yAxisTitleHeight/2;

				Mat4d secYtitleTrans = new Mat4d();
				secYtitleTrans.setTranslate3(new Vec3d(secXPos, 0, decZBump));
				secYtitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
				secYtitleTrans.mult4(objectTransComp, secYtitleTrans);
				secYtitleTrans.scaleCols3(yScaleVec);

				out.add(new StringProxy(secYAxisTitle, fontKey, titleColour, secYtitleTrans, yAxisTitleHeight, getVisibilityInfo(), pickingID));
			}

		}

		private void updateObjectTrans(double simTime) {
			objectTrans = graphObservee.getGlobalTrans(simTime);
			objectScale = graphObservee.getSize();
			objectScale.mul3(getModelScale());

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
