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
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
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
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.Graph;

public class GraphModel extends DisplayModel {

	@Keyword(description = "The text height for the graph title.",
	         example = "Graph1 TitleTextHeight { 0.05 }")
	private final ValueInput titleTextHeight;

	@Keyword(description = "The text height for the x-axis title.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 XAxisTitleTextHeight { 0.05 }")
	private final ValueInput xAxisTitleTextHeight;

	@Keyword(description = "The text height for the y-axis title.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 YAxisTitleTextHeight { 0.05 }")
	private final ValueInput yAxisTitleTextHeight;

	@Keyword(description = "The text height for both x- and y-axis labels.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 LabelTextHeight { 0.025 }")
	private final ValueInput labelTextHeight;

	@Keyword(description = "The gap between the title and top of the graph.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 TitleGap { 0.025 }")
	private final ValueInput titleGap;

	@Keyword(description = "The gap between the x-axis labels and the x-axis.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 XAxisLabelGap { 0.025 }")
	private final ValueInput xAxisLabelGap;

	@Keyword(description = "The gap between the x-axis title and the x-axis labels.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 xAxisTitleGap { 0.025 }")
	private final ValueInput xAxisTitleGap;

	@Keyword(description = "The gap between the y-axis and its labels.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 YAxisLabelGap { 0.025 }")
	private final ValueInput yAxisLabelGap;

	@Keyword(description = "The gap between the y-axis title and the y-axis labels.\n" +
			"Expressed as a fraction of the total graph height.",
	         example = "Graph1 yAxisTitleGap { 0.025 }")
	private final ValueInput yAxisTitleGap;

	@Keyword(description = "The margin between the top of the graph and the top of the graph object.\n" +
			"Expressed as a fraction of the total graph height." +
	                "side of the graph.",
	         example = "Graph1 TopMargin { 0.10 }")
	private final ValueInput topMargin;

	@Keyword(description = "The margin between the bottom of the graph and the bottom of the graph object.\n" +
	                "Expressed as a fraction of the total graph height.",
             example = "Graph1 BottomMargin { 0.10 }")
	private final ValueInput bottomMargin;

	@Keyword(description = "The margin between the left side of the graph and the left side of the graph object.\n" +
	                "Expressed as a fraction of the total graph height.",
	         example = "Graph1 LeftMargin { 0.20 }")
	private final ValueInput leftMargin;

	@Keyword(description = "The margin between the right side of the graph and the right side of the graph object.\n" +
	                "Expressed as a fraction of the total graph height.",
	         example = "Graph1 RightMargin { 0.20 }")
	private final ValueInput rightMargin;

	@Keyword(description = "The text model to be used for the graph title.\n" +
			"Determines the font, color, and style (bold, italics) for the text.",
	         example = "Graph1 TitleTextModel { TextModelDefault }")
	protected final EntityInput<TextModel> titleTextModel;

	@Keyword(description = "The text model to be used for the axis titles (x-axis, y-axis, and secondary y-axis).\n" +
			"Determines the font, color, and style (bold, italics) for the text.",
	         example = "Graph1 AxisTitleTextModel { TextModelDefault }")
	protected final EntityInput<TextModel> axisTitleTextModel;

	@Keyword(description = "The text model to be used for the numbers next to the tick marks on each axis" +
			" (x-axis, y-axis, and secondary y-axis).\n" +
			"Determines the font, color, and style (bold, italics) for the text.",
	         example = "Graph1 LabelTextModel { TextModelDefault }")
	protected final EntityInput<TextModel> labelTextModel;

	@Keyword(description = "The color of the graph background, defined by a color keyword or an RGB value.",
	         example = "Graph1 GraphColor { floralwhite }")
	private final ColourInput graphColor;

	@Keyword(description = "The color for the outer pane background, defined using a color keyword or an RGB value.",
	         example = "Graph1 BackgroundColor { floralwhite }")
	private final ColourInput backgroundColor;

	@Keyword(description = "The color of the graph border, defined using a color keyword or an RGB value.",
	         example = "Graph1 BorderColor { red }")
	private final ColourInput borderColor;

	{
		titleTextHeight = new ValueInput("TitleTextHeight", "Key Inputs", 0.05d);
		titleTextHeight.setUnitType(DimensionlessUnit.class);
		this.addInput(titleTextHeight);

		xAxisTitleTextHeight = new ValueInput("XAxisTitleTextHeight", "Key Inputs", 0.05d);
		xAxisTitleTextHeight.setUnitType(DimensionlessUnit.class);
		this.addInput(xAxisTitleTextHeight);

		yAxisTitleTextHeight = new ValueInput("YAxisTitleTextHeight", "Key Inputs", 0.05d);
		yAxisTitleTextHeight.setUnitType(DimensionlessUnit.class);
		this.addInput(yAxisTitleTextHeight);

		labelTextHeight = new ValueInput("LabelTextHeight", "Key Inputs", 0.025d);
		labelTextHeight.setUnitType(DimensionlessUnit.class);
		this.addInput(labelTextHeight);

		titleGap = new ValueInput("TitleGap", "Key Inputs", 0.05d);
		titleGap.setUnitType(DimensionlessUnit.class);
		this.addInput(titleGap);

		xAxisTitleGap = new ValueInput("XAxisTitleGap", "Key Inputs", 0.025d);
		xAxisTitleGap.setUnitType(DimensionlessUnit.class);
		this.addInput(xAxisTitleGap);

		xAxisLabelGap = new ValueInput("XAxisLabelGap", "Key Inputs", 0.025d);
		xAxisLabelGap.setUnitType(DimensionlessUnit.class);
		this.addInput(xAxisLabelGap);

		yAxisTitleGap = new ValueInput("YAxisTitleGap", "Key Inputs", 0.025d);
		yAxisTitleGap.setUnitType(DimensionlessUnit.class);
		this.addInput(yAxisTitleGap);

		yAxisLabelGap = new ValueInput("YAxisLabelGap", "Key Inputs", 0.025d);
		yAxisLabelGap.setUnitType(DimensionlessUnit.class);
		this.addInput(yAxisLabelGap);

		topMargin = new ValueInput("TopMargin", "Key Inputs", 0.15d);
		topMargin.setUnitType(DimensionlessUnit.class);
		this.addInput(topMargin);

		bottomMargin = new ValueInput("BottomMargin", "Key Inputs", 0.175d);
		bottomMargin.setUnitType(DimensionlessUnit.class);
		this.addInput(bottomMargin);

		leftMargin = new ValueInput("LeftMargin", "Key Inputs", 0.21d);
		leftMargin.setUnitType(DimensionlessUnit.class);
		this.addInput(leftMargin);

		rightMargin = new ValueInput("RightMargin", "Key Inputs", 0.21d);
		rightMargin.setUnitType(DimensionlessUnit.class);
		this.addInput(rightMargin);

		titleTextModel = new EntityInput<TextModel>(TextModel.class, "TitleTextModel", "Key Inputs", null);
		this.addInput(titleTextModel);

		axisTitleTextModel = new EntityInput<TextModel>(TextModel.class, "AxisTitleTextModel", "Key Inputs", null);
		this.addInput(axisTitleTextModel);

		labelTextModel = new EntityInput<TextModel>(TextModel.class, "LabelTextModel", "Key Inputs", null);
		this.addInput(labelTextModel);

		graphColor = new ColourInput("GraphColor", "Key Inputs", ColourInput.getColorWithName("ivory"));
		this.addInput(graphColor);
		this.addSynonym(graphColor, "GraphColour");

		backgroundColor = new ColourInput("BackgroundColor", "Key Inputs", ColourInput.getColorWithName("gray95"));
		this.addInput(backgroundColor);
		this.addSynonym(backgroundColor, "BackgroundColour");

		borderColor = new ColourInput("BorderColor", "Key Inputs", ColourInput.BLACK);
		this.addInput(borderColor);
		this.addSynonym(borderColor, "BorderColour");
	}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof Graph;
	}

	private class Binding extends DisplayModelBinding {

		// Size and position of the graph area (excluding the titles, labels, etc.) scaled to the unit cube
		protected Vec3d graphSize;   // graph size
		protected Vec3d graphOrigin; // bottom left position of the graph area,
		protected Vec3d graphCenter; // Center point of the graph area

		private Graph graphObservee;

		private List<Vec4d> graphRectPoints = null;
		private Mat4d graphAreaTrans = null;
		private Mat4d graphToWorldTrans = null;
		private Mat4d objectTransComp = null; // The composed transform and scale as a Matrix4d
		private Transform objectTrans = null;
		private Vec3d objectScale = null;
		private long pickingID;

		private double xScaleFactor;
		private Vec3d xScaleVec;
		private Vec3d yScaleVec;

		private double xMin;
		private double xMax;
		private double xRange;
		private double xAxisInterval;

		private double yMin;
		private double yMax;
		private double yRange;
		private double yAxisInterval;

		private double secYMin;
		private double secYMax;
		private double secYRange;
		private double secYAxisInterval;

		private boolean timeTrace;

		private double yAxisTitleHeight;

		private TessFontKey axisTitleFontKey;
		private Color4d axisTitleFontColor;

		private TessFontKey labelFontKey;
		private Color4d labelFontColor;
		private double labelHeight; // height of the label text
		private double xAxisTickSize; // vertical tick marks for the x-axis
		private double yAxisTickSize; // horizontal tick marks for the y-axis

		private double zBump;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);

			graphSize = new Vec3d();
			graphOrigin = new Vec3d();
			graphCenter = new Vec3d();

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

			registerCacheMiss("Graph");

			// This factor is applied to lengths expressed as a fraction of the graph's y-extent and
			// converts then to fractions of the graph's x-extent
			Vec3d objectSize = graphObservee.getSize();
			xScaleFactor = objectSize.y / objectSize.x;

			// These two matrices are needed to cancel out the object level non-uniform scaling for text objects
			// xScale if for horizontal text, yScale is for vertical text
			xScaleVec = new Vec3d(xScaleFactor, 1, 1);
			yScaleVec = new Vec3d(1/xScaleFactor, 1, 1);

			xMin = graphObservee.getXAxisStart();
			xMax = graphObservee.getXAxisEnd();
			xRange = xMax - xMin;
			xAxisInterval = graphObservee.getXAxisInterval();

			yMin= graphObservee.getYAxisStart();
			yMax= graphObservee.getYAxisEnd();
			yRange = yMax - yMin;
			yAxisInterval = graphObservee.getYAxisInterval();

			secYMin= graphObservee.getSecondaryYAxisStart();
			secYMax= graphObservee.getSecondaryYAxisEnd();
			secYRange = secYMax - secYMin;
			secYAxisInterval = graphObservee.getSecondaryYAxisInterval();

			timeTrace = graphObservee.getTimeTrace();

			yAxisTitleHeight = yAxisTitleTextHeight.getValue()*xScaleFactor; // scaled height of the y-axis title

			if( axisTitleTextModel.getValue() == null ) {
				axisTitleFontKey = TextModel.getDefaultTessFontKey();
				axisTitleFontColor = ColourInput.BLACK;
			}
			else {
				axisTitleFontKey = axisTitleTextModel.getValue().getTessFontKey();
				axisTitleFontColor = axisTitleTextModel.getValue().getFontColor();
			}

			if( labelTextModel.getValue() == null ) {
				labelFontKey = TextModel.getDefaultTessFontKey();
				labelFontColor = ColourInput.BLACK;
			}
			else {
				labelFontKey = labelTextModel.getValue().getTessFontKey();
				labelFontColor = labelTextModel.getValue().getFontColor();
			}

			labelHeight = labelTextHeight.getValue();
			xAxisTickSize = labelHeight/2; // vertical tick marks for the x-axis
			yAxisTickSize = xAxisTickSize * xScaleFactor; // horizontal tick marks for the y-axis

			zBump = 0.001 * objectSize.x;  // z-coordinate;

			// Draw the main frame
			out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, backgroundColor.getValue(), false, 1, getVisibilityInfo(), pickingID));
			out.add(new PolygonProxy(RenderUtils.RECT_POINTS, objectTrans, objectScale, borderColor.getValue(), true, 1, getVisibilityInfo(), pickingID));

			// Draw the graph frame
			out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, graphColor.getValue(), false, 1, getVisibilityInfo(), pickingID));
			out.add(new PolygonProxy(graphRectPoints, objectTrans, objectScale, borderColor.getValue(), true, 1, getVisibilityInfo(), pickingID));

			// Draw the graph title
			drawGraphTitle(out);

			// Draw the x-axis, y-axis, and axis titles
			drawXAxis(out);
			drawYAxis(out);

			// Draw the secondary y-axis title (if used)
			if( graphObservee.showSecondaryYAxis() )
				drawSecondaryYAxis(out);

			// Draw the selected grid lines
			drawXLines(out);
			drawYLines(out);

			// Draw the primary series
			ArrayList<Graph.SeriesInfo> primarySeries = graphObservee.getPrimarySeries();
			for (int i = 0; i < primarySeries.size(); ++i) {
				drawSeries(primarySeries.get(i), yMin, yMax, simTime, out);
			}

			// Draw the secondary series
			ArrayList<Graph.SeriesInfo> secondarySeries = graphObservee.getSecondarySeries();
			for (int i = 0; i < secondarySeries.size(); ++i) {
				drawSeries(secondarySeries.get(i), secYMin, secYMax, simTime, out);
			}
		}

		private void drawSeries(Graph.SeriesInfo series, double yMinimum, double yMaximum, double simTime, ArrayList<RenderProxy> out) {

			if (series.numPoints < 2)
				return; // Nothing to display yet

			double yRange = yMaximum - yMinimum;  // yRange can be either the primary or secondary range

			double[] yVals = new double[series.numPoints];
			double[] xVals = new double[series.numPoints];

			for (int i = 0; i < series.numPoints; i++) {
				if( timeTrace )
					xVals[i] = MathUtils.bound((series.xValues[i] - simTime - xMin) / xRange, 0, 1) - 0.5;
				else
					xVals[i] = MathUtils.bound((series.xValues[i] - xMin) / xRange, 0, 1) - 0.5;

				yVals[i] = MathUtils.bound((series.yValues[i] - yMinimum) / yRange, 0, 1) - 0.5;
			}

			ArrayList<Vec4d> seriesPoints = new ArrayList<Vec4d>((series.numPoints-1)*2);
			for (int i = 0; i < series.numPoints - 1; i++) {
				seriesPoints.add(new Vec4d(xVals[i  ], yVals[i  ], zBump, 1.0d));
				seriesPoints.add(new Vec4d(xVals[i+1], yVals[i+1], zBump, 1.0d));
			}

			// Transform from graph area to world space
			for (int i = 0; i < seriesPoints.size(); ++i) {
				seriesPoints.get(i).mult4(graphToWorldTrans, seriesPoints.get(i));
			}

			out.add(new LineProxy(seriesPoints, series.lineColour, series.lineWidth, getVisibilityInfo(), pickingID));
		}

		private void drawGraphTitle(ArrayList<RenderProxy> out) {

			String titleText = graphObservee.getTitle();

			TessFontKey titleFontKey;
			Color4d titleFontColor;
			if( titleTextModel.getValue() == null ) {
				titleFontKey = TextModel.getDefaultTessFontKey();
				titleFontColor = ColourInput.BLACK;
			}
			else {
				titleFontKey = titleTextModel.getValue().getTessFontKey();
				titleFontColor = titleTextModel.getValue().getFontColor();
			}

			Vec4d titleCenter = new Vec4d(0, graphOrigin.y + graphSize.y + titleGap.getValue() + titleTextHeight.getValue()/2, zBump, 1.0d);

			Mat4d titleTrans = new Mat4d();
			titleTrans.setTranslate3(titleCenter);
			titleTrans.mult4(objectTransComp, titleTrans);
			titleTrans.scaleCols3(xScaleVec);

			out.add(new StringProxy(titleText, titleFontKey, titleFontColor, titleTrans, titleTextHeight.getValue(), getVisibilityInfo(), pickingID));
		}

		private void drawXAxis(ArrayList<RenderProxy> out) {

			String xAxisFormat = graphObservee.getXAxisLabelFormat();
			ArrayList<Vec4d> tickPoints = new ArrayList<Vec4d>();

			double xAxisFactor = 1.0;
			if( graphObservee.getXAxisUnit() != null )
				xAxisFactor = graphObservee.getXAxisUnit().getConversionFactorToSI();

			for (int i = 0; xMin + i*xAxisInterval <= xMax; ++i) {

				double x = (xMin + i * xAxisInterval);
				String text;
				if( timeTrace && x == 0 ) {
					text = "Now";
				} else {
					text = String.format( xAxisFormat, x/xAxisFactor);
				}

				double xPos = graphOrigin.x + ( i * xAxisInterval * graphSize.x)/xRange;
				double yPos = graphOrigin.y - xAxisTickSize - xAxisLabelGap.getValue() - labelHeight/2;

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, zBump));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, labelFontKey, labelFontColor, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				// Prepare the tick marks
				Vec4d tickPointA = new Vec4d(xPos, graphOrigin.y, zBump, 1.0d);
				Vec4d tickPointB = new Vec4d(xPos, graphOrigin.y - xAxisTickSize, zBump, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			out.add(new LineProxy(tickPoints, labelFontColor, 1, getVisibilityInfo(), pickingID));

			// X-Axis Title
			String xAxisTitle = graphObservee.getXAxisTitle();
			Vec4d titleCenter = new Vec4d(0, graphOrigin.y - xAxisTickSize - xAxisLabelGap.getValue() - labelHeight
					- xAxisTitleGap.getValue() - xAxisTitleTextHeight.getValue()/2, zBump, 1.0d);

			Mat4d xtitleTrans = new Mat4d();
			xtitleTrans.setTranslate3(titleCenter);
			xtitleTrans.mult4(objectTransComp, xtitleTrans);
			xtitleTrans.scaleCols3(xScaleVec);

			out.add(new StringProxy(xAxisTitle, axisTitleFontKey, axisTitleFontColor, xtitleTrans,
					xAxisTitleTextHeight.getValue(), getVisibilityInfo(), pickingID));
		}

		private void drawYAxis(ArrayList<RenderProxy> out) {

			String yAxisLabelFormat = graphObservee.getYAxisLabelFormat();

			Unit yAxisUnit = graphObservee.getYAxisUnit();
			double yAxisFactor = 1.0;
			if( yAxisUnit != null )
				yAxisFactor = yAxisUnit.getConversionFactorToSI();

			ArrayList<Vec4d> tickPoints = new ArrayList<Vec4d>();

			double minYLabelXPos = graphOrigin.x;

			for (int i = 0; i * yAxisInterval <= yRange; ++i) {

				String text = String.format( yAxisLabelFormat,  ( i * yAxisInterval + yMin )/yAxisFactor);
				double yPos = graphOrigin.y + (i * yAxisInterval * graphSize.y )/yRange;

				// Right justify the labels
				Vec3d stringSize = RenderManager.inst().getRenderedStringSize(labelFontKey,	labelHeight*xScaleFactor, text);
				double xPos = graphOrigin.x - yAxisTickSize - yAxisLabelGap.getValue()*xScaleFactor - stringSize.x/2;

				// Save the left-most extent of the labels
				minYLabelXPos = Math.min(minYLabelXPos, xPos - stringSize.x/2);

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, zBump));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, labelFontKey, labelFontColor, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				// Prepare the tick marks

				Vec4d tickPointA = new Vec4d(graphOrigin.x                , yPos, zBump, 1.0d);
				Vec4d tickPointB = new Vec4d(graphOrigin.x - yAxisTickSize, yPos, zBump, 1.0d);

				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);
				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			out.add(new LineProxy(tickPoints, labelFontColor, 1, getVisibilityInfo(), pickingID));

			// Primary Y-Axis Title
			String yAxisTitle = graphObservee.getYAxisTitle();
			double xPos = minYLabelXPos - yAxisTitleGap.getValue()*xScaleFactor - yAxisTitleHeight/2;

			Mat4d ytitleTrans = new Mat4d();
			ytitleTrans.setTranslate3(new Vec3d(xPos, 0, zBump));
			ytitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
			ytitleTrans.mult4(objectTransComp, ytitleTrans);
			ytitleTrans.scaleCols3(yScaleVec);

			out.add(new StringProxy(yAxisTitle, axisTitleFontKey, axisTitleFontColor, ytitleTrans,
					yAxisTitleHeight, getVisibilityInfo(), pickingID));
		}

		private void drawSecondaryYAxis(ArrayList<RenderProxy> out) {

			ArrayList<Vec4d> tickPoints = new ArrayList<Vec4d>();

			// Secondary Y-Axis Labels and Tick Marks
			String secYAxisLabelFormat = graphObservee.getSecondaryYAxisLabelFormat();

			Unit secYAxisUnit = graphObservee.getSecondaryYAxisUnit();
			double secYAxisFactor = 1.0;
			if( secYAxisUnit != null )
				secYAxisFactor = secYAxisUnit.getConversionFactorToSI();

			double maxYLabelXPos = graphOrigin.x + graphSize.x;

			for (int i = 0; i * secYAxisInterval <= secYRange; ++i) {

				String text = String.format( secYAxisLabelFormat,  ( i * secYAxisInterval + secYMin )/secYAxisFactor);
				double yPos = graphOrigin.y + (i * secYAxisInterval * graphSize.y )/secYRange;

				// Right justify the labels
				Vec3d stringSize = RenderManager.inst().getRenderedStringSize(labelFontKey,	labelHeight*xScaleFactor, text);
				double xPos = graphOrigin.x + graphSize.x + yAxisTickSize + yAxisLabelGap.getValue()*xScaleFactor + stringSize.x/2;

				// Save the right-most extent of the labels
				maxYLabelXPos = Math.max(maxYLabelXPos, xPos + stringSize.x/2);

				Mat4d labelTrans = new Mat4d();
				labelTrans.setTranslate3(new Vec3d(xPos, yPos, zBump));
				labelTrans.mult4(objectTransComp, labelTrans);
				labelTrans.scaleCols3(xScaleVec);

				out.add(new StringProxy(text, labelFontKey, labelFontColor, labelTrans, labelHeight, getVisibilityInfo(), pickingID));

				// Prepare the tick marks
				Vec4d tickPointA = new Vec4d(graphOrigin.x + graphSize.x                , yPos, zBump, 1.0d);
				Vec4d tickPointB = new Vec4d(graphOrigin.x + graphSize.x + yAxisTickSize, yPos, zBump, 1.0d);
				tickPointA.mult4(objectTransComp, tickPointA);
				tickPointB.mult4(objectTransComp, tickPointB);

				tickPoints.add(tickPointA);
				tickPoints.add(tickPointB);
			}

			out.add(new LineProxy(tickPoints, labelFontColor, 1, getVisibilityInfo(), pickingID));

			// Secondary Y-Axis Title
			String secYAxisTitle = graphObservee.getSecondaryYAxisTitle();
			double secXPos = maxYLabelXPos + yAxisTitleGap.getValue()*xScaleFactor + yAxisTitleHeight/2;

			Mat4d secYtitleTrans = new Mat4d();
			secYtitleTrans.setTranslate3(new Vec3d(secXPos, 0, zBump));
			secYtitleTrans.setEuler3(new Vec3d(0, 0, Math.PI/2));
			secYtitleTrans.mult4(objectTransComp, secYtitleTrans);
			secYtitleTrans.scaleCols3(yScaleVec);

			out.add(new StringProxy(secYAxisTitle, axisTitleFontKey, axisTitleFontColor, secYtitleTrans,
					yAxisTitleHeight, getVisibilityInfo(), pickingID));
		}

		private void drawXLines(ArrayList<RenderProxy> out) {

			DoubleVector xLines = graphObservee.getXLines();
			ArrayList<Color4d> xLineColours = graphObservee.getXLineColours();

			for (int i = 0; i < xLines.size(); ++i) {
				double xPos = (xLines.get(i) - xMin) / xRange - 0.5;

				ArrayList<Vec4d> linePoints = new ArrayList<Vec4d>(2);

				linePoints.add(new Vec4d(xPos, -0.5, zBump, 1.0d));
				linePoints.add(new Vec4d(xPos,  0.5, zBump, 1.0d));

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
		}

		private void drawYLines(ArrayList<RenderProxy> out) {

			DoubleVector yLines = graphObservee.getYLines();
			ArrayList<Color4d> yLineColours = graphObservee.getYLineColours();

			for (int i = 0; i < yLines.size(); ++i) {
				double yPos = (yLines.get(i) - yMin) / yRange - 0.5;
				ArrayList<Vec4d> linePoints = new ArrayList<Vec4d>(2);

				linePoints.add(new Vec4d(-0.5, yPos, zBump, 1.0d));
				linePoints.add(new Vec4d( 0.5, yPos, zBump, 1.0d));

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
		}

		private void updateObjectTrans(double simTime) {

			// Set graph proportions
			Vec3d graphExtent = graphObservee.getSize();
			double xScaleFactor = graphExtent.y / graphExtent.x;

			// Draw graphic rectangle
			graphSize.x = 1.0 - ( leftMargin.getValue() + rightMargin.getValue() ) * xScaleFactor;
			graphSize.y = 1.0 - ( topMargin.getValue() + bottomMargin.getValue() );
			graphSize.z = 1;

			// Center position of the graph
			graphCenter = new Vec3d( leftMargin.getValue()/2 - rightMargin.getValue()/2,
					bottomMargin.getValue()/2 - topMargin.getValue()/2, 0.0 );

			graphOrigin = new Vec3d( graphCenter.x - graphSize.x/2, graphCenter.y - graphSize.y/2, 0.0  );

			objectTrans = graphObservee.getGlobalTrans(simTime);
			objectScale = graphObservee.getSize();
			objectScale.mul3(getModelScale());

			objectTransComp = new Mat4d();
			objectTrans.getMat4d(objectTransComp);
			objectTransComp.scaleCols3(objectScale);

			graphAreaTrans = new Mat4d();
			graphAreaTrans.setTranslate3(graphCenter);
			graphAreaTrans.scaleCols3(graphSize);

			graphRectPoints = RenderUtils.transformPoints(graphAreaTrans, RenderUtils.RECT_POINTS, 0);

			graphToWorldTrans = new Mat4d();
			graphToWorldTrans.mult4(objectTransComp, graphAreaTrans);

		}

	}
}
