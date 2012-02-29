/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2012 Ausenco Engineering Canada Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;

import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.ColorListInput;
import com.sandwell.JavaSimulation.ColourInput;
import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.DoubleListInput;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.EntityListListInput;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerInput;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.StringVector;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.InputAgent;
import com.sandwell.JavaSimulation3D.TextLabel;
import com.sandwell.JavaSimulation3D.util.LabelShape;
import com.sandwell.JavaSimulation3D.util.Line;
import com.sandwell.JavaSimulation3D.util.Rectangle;
import com.sandwell.JavaSimulation3D.util.Shape;

public class Graph extends DisplayEntity  {
	private static final ArrayList<Graph> allInstances;

	protected final IntegerInput numberOfPoints;	 // Total number of values that can be shown on the graph (the more the sharper the graph)
	protected final DoubleInput startTime; 	 // start time for drawing the graph
	protected final DoubleInput endTime; 	 // end time for drawing the graph
	private final DoubleInput timeInterval; // Time interval used to label x axis
	private final DoubleInput yAxisStart;
	private final DoubleInput yAxisEnd;
	private final DoubleInput yAxisInterval;
	private final DoubleListInput yLines; // Horizontal lines
	private final DoubleInput secondaryYAxisStart;
	private final DoubleInput secondaryYAxisEnd;
	private final DoubleInput secondaryYAxisInterval;
	private final ColorListInput yLinesColor;
	private final DoubleListInput xLines; // Vertical lines
	private final ColorListInput xLinesColor;

	protected final EntityListInput<Entity> targetEntityList; //  list the entity that graph is being shown for
	protected ArrayList<Entity> targetEntities;
	protected final EntityListInput<Entity> secondaryTargetEntityList;

	protected Method targetMethod;  // Target method for the primary y axis
	protected Method secondaryTargetMethod;
	private boolean timeInputParameter; // true => time is passing to the target method(graph may show future values)

	private final StringInput targetMethodName;
	private final StringInput secondaryTargetMethodName;
	protected final EntityListListInput<Entity> targetInputParameters; // List of all input parameters to target method for each entity
	private final EntityListListInput<Entity> secondaryTargetInputParameters;
	protected ArrayList<ArrayList<Entity>> targetParameters;
	protected ArrayList<double[]> valuesList;	// holds all the values for each point
	protected ArrayList<Line> linesList;	// holds all the lines for each point each item is Line[][]
	private ArrayList<double[]> secondaryValuesList;
	protected ArrayList<Line> secondaryLinesList;
	private ArrayList<Integer> primaryIndex; // number of primary points to be plotted from the start of each series
	private ArrayList<Integer> secondaryIndex; // number of secondary points to be plotted from the start of each series
	private ArrayList<Integer> primaryRemovedIndex; // number of primary points not to be plotted from the end of each series;
	private ArrayList<Integer> secondaryRemovedIndex; // number of secondary points not to be plotted from the end of each series;

	private final StringInput yAxisTitle; // Title for the y axis
	private final StringInput secondaryYAxisTitle; // Title for the secondary y axis
	private final StringInput title;	   // Title of the graph (shown on the center top)

	protected Rectangle fillModel; // fill of the background frame
	protected final StringInput labelFontName; // For all the texts
	protected Rectangle outlineModel; // outline of the background frame
	protected Rectangle graphFillModel;
	protected Rectangle graphOutlineModel;
	protected Vector3d graphSize;   // graph size
	protected Vector3d graphOrigin; // bottom left position of the graph

	// A list of the line thickness for corresponding item in targetEntityList
	protected final DoubleListInput lineWidths;
	protected final DoubleListInput secondaryLineWidths;
	protected final ColorListInput lineColorsList;	// list of line colours for each entity. Each element contains a vector of colours for each series for that entity
	protected ArrayList<ColoringAttributes> lineColors;
	protected final ColorListInput secondaryLineColorsList;

	private final ColourInput graphColor;
	private final ColourInput labelFontColor;
	private final ColourInput tickColor;
	private final ColourInput backgroundColor;
	private final ColourInput borderColor;
	private final ColourInput titleColor;

	private final DoubleInput labelTextHeight; // Text height for axes labels
	private final DoubleInput titleTextHeight; // Title text height
	private final DoubleInput yAxisTitleTextHeight; // y axis title text height
	private final DoubleInput xAxisLabelGap; // Distance between labels and x axis
	private final DoubleInput yAxisLabelGap; // Distance between labels and y axis
	private final DoubleInput titleGap; // gap between title and graph top
	private final DoubleInput yAxisTitleGap; // gap between y axis title and y axis labels
	private final DoubleInput topMargin;		// Empty margin at top
	private final DoubleInput bottomMargin;	// Empty margin at bottom
	private final DoubleInput leftMargin;		// Empty margin at left
	private final DoubleInput rightMargin;		// Empty margin at right

	private Vector3d legendCenter;			// TopLeft corner of the legend
	private Vector3d legendSize;			// size of legend
	private DoubleInput legendTextHeight;			// height of text in legend
	private Vector3d legendMarkerSize;			// size of the marker for each series
	private final DoubleInput seriesLabelGap;				// gap between the marker and the text for the series
	private final DoubleInput seriesMakerGap;				// gap between the left border and the maker

	LabelShape yAxisLabel;
	LabelShape titleLabel;

	static int ENTITY_ONLY = 0;
	static int PARAMETER_ONLY = 1;
	static int ENTITY_PARAMETER = 2;

	private final IntegerInput yAxisPrecision; // number of decimal places to show in the y-axis labels
	private final IntegerInput secondaryYAxisPrecision; // number of decimal places to show in the secondary y-axis labels
	private final IntegerInput xAxisPrecision; // number of decimal places to show in the x-axis labels
	private final StringInput xAxisUnits; // text shown after each x-axis label
	private final DoubleInput xAxisMultiplier; // the value to multiply each x-axis label by
	private final DoubleInput yAxisMultiplier; // the value to multiply each y-axis label by
	private final DoubleInput secondaryYAxisMultiplier; // the value to multiply each secondary y-axis label by

	static {
		allInstances = new ArrayList<Graph>();
	}
	{
		lineWidths = new DoubleListInput("LineWidths", "Data", new DoubleVector());
		this.addInput(lineWidths, true);

		secondaryLineWidths = new DoubleListInput("SecondaryLineWidths", "Data", new DoubleVector());
		this.addInput(secondaryLineWidths, true);

		xLines = new DoubleListInput("XLines", "Data", new DoubleVector());
		this.addInput(xLines, true);

		yLines = new DoubleListInput("YLines", "Data", new DoubleVector());
		this.addInput(yLines, true);

		numberOfPoints = new IntegerInput("NumberOfPoints", "Data", 100);
		numberOfPoints.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(numberOfPoints, true);

		startTime = new DoubleInput("StartTime", "X Axis", -24.0d);
		this.addInput(startTime, true);

		endTime = new DoubleInput("EndTime", "X Axis", 0.0);
		this.addInput(endTime, true);

		timeInterval = new DoubleInput("TimeInterval", "X Axis", 6.0d);
		this.addInput(timeInterval, true);

		yAxisStart = new DoubleInput("YAxisStart", "Y Axis", 0.0);
		this.addInput(yAxisStart, true);

		yAxisEnd = new DoubleInput("YAxisEnd", "Y Axis", 5.0d);
		this.addInput(yAxisEnd, true);

		yAxisInterval = new DoubleInput("YAxisInterval", "Y Axis", 1.0d);
		this.addInput(yAxisInterval, true);

		secondaryYAxisStart = new DoubleInput("SecondaryYAxisStart", "Y Axis", 0.0);
		this.addInput(secondaryYAxisStart, true);

		secondaryYAxisEnd = new DoubleInput("SecondaryYAxisEnd", "Y Axis", 0.0);
		this.addInput(secondaryYAxisEnd, true);

		secondaryYAxisInterval = new DoubleInput("SecondaryYAxisInterval", "Y Axis", 0.0);
		this.addInput(secondaryYAxisInterval, true);

		labelTextHeight = new DoubleInput("LabelTextHeight", "Labels", 0.05);
		this.addInput(labelTextHeight, true);

		yAxisTitleTextHeight = new DoubleInput("YAxisTitleTextHeight", "Labels", 0.05d);
		this.addInput(yAxisTitleTextHeight, true);

		titleTextHeight = new DoubleInput("TitleTextHeight", "Labels", 0.0);
		this.addInput(titleTextHeight, true);

		xAxisLabelGap = new DoubleInput("XAxisLabelGap", "Labels", 0.0);
		this.addInput(xAxisLabelGap, true);

		yAxisLabelGap = new DoubleInput("YAxisLabelGap", "Labels", 0.0);
		this.addInput(yAxisLabelGap, true);

		titleGap = new DoubleInput("TitleGap", "Labels", 0.05d);
		this.addInput(titleGap, true);

		yAxisTitleGap = new DoubleInput("YAxisTitleGap", "Labels", 0.0);
		this.addInput(yAxisTitleGap, true);

		topMargin = new DoubleInput("TopMargin", "Background", 0.20d);
		this.addInput(topMargin, true);

		bottomMargin = new DoubleInput("BottomMargin", "Background", 0.20d);
		this.addInput(bottomMargin, true);

		leftMargin = new DoubleInput("LeftMargin", "Background", 0.20d);
		this.addInput(leftMargin, true);

		rightMargin = new DoubleInput("RightMargin", "Background", 0.10d);
		this.addInput(rightMargin, true);

		legendTextHeight = new DoubleInput("LegendTextHeight", "Legend", 0.5);
		this.addInput(legendTextHeight, true);

		seriesMakerGap = new DoubleInput("LegendSeriesMarkerGap", "Legend", 0.0);
		this.addInput(seriesMakerGap, true);

		seriesLabelGap = new DoubleInput("LegendSeriesLabelGap", "Legend", 0.0);
		this.addInput(seriesLabelGap, true);

		xAxisMultiplier = new DoubleInput("XAxisMultiplier", "X Axis", 1.0);
		this.addInput(xAxisMultiplier, true);

		yAxisMultiplier = new DoubleInput("YAxisMultiplier", "Y Axis", 1.0);
		this.addInput(yAxisMultiplier, true);

		secondaryYAxisMultiplier = new DoubleInput("SecondaryYAxisMultiplier", "Y Axis", 1.0);
		this.addInput(secondaryYAxisMultiplier, true);

		yAxisPrecision = new IntegerInput("YAxisPrecision", "Labels", 0);
		yAxisPrecision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(yAxisPrecision, true);

		secondaryYAxisPrecision = new IntegerInput("SecondaryYAxisPrecision", "Labels", 0);
		secondaryYAxisPrecision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(secondaryYAxisPrecision, true);

		xAxisPrecision = new IntegerInput("XAxisPrecision", "Labels", 0);
		xAxisPrecision.setValidRange(0, Integer.MAX_VALUE);
		this.addInput(xAxisPrecision, true);

		targetEntityList = new EntityListInput<Entity>(Entity.class, "TargetEntity", "Data", new ArrayList<Entity>(0));
		targetEntityList.setUnique(false);
		this.addInput(targetEntityList, true);

		secondaryTargetEntityList = new EntityListInput<Entity>(Entity.class, "SecondaryTargetEntity", "Data", new ArrayList<Entity>(0));
		secondaryTargetEntityList.setUnique(false);
		this.addInput(secondaryTargetEntityList, true);

		lineColorsList = new ColorListInput( "LineColours", "Data", new ArrayList<ColoringAttributes>() );
		this.addInput(lineColorsList, true, "LineColors");

		secondaryLineColorsList = new ColorListInput( "SecondaryLineColours", "Data", new ArrayList<ColoringAttributes>() );
		this.addInput(secondaryLineColorsList, true, "SecondaryLineColors");

		yLinesColor = new ColorListInput( "YLinesColor", "Y Axis", new ArrayList<ColoringAttributes>() );
		this.addInput(yLinesColor, true, "YLinesColour");

		xLinesColor = new ColorListInput( "XLinesColor", "X Axis", new ArrayList<ColoringAttributes>() );
		this.addInput(xLinesColor, true, "XLinesColour");

		graphColor = new ColourInput("GraphColor", "Background", Shape.getColorWithName( "ivory" ));
		this.addInput(graphColor, true, "GraphColour");

		labelFontColor = new ColourInput("LabelFontColor", "Labels", Shape.getColorWithName( "blue" ));
		this.addInput(labelFontColor, true, "LabelFontColour");

		tickColor = new ColourInput("TickColor", "Labels", Shape.getColorWithName( "darkblue" ));
		this.addInput(tickColor, true, "TickColour");

		backgroundColor = new ColourInput("BackgroundColor", "Background", Shape.getColorWithName( "gray95" ));
		this.addInput(backgroundColor, true, "BackgroundColour");

		borderColor = new ColourInput("BorderColor", "Background", Shape.getColorWithName( "black" ));
		this.addInput(borderColor, true, "BorderColour");

		titleColor = new ColourInput("TitleColor", "Labels", Shape.getColorWithName( "brick" ));
		this.addInput(titleColor, true, "TitleColour");

		labelFontName = new StringInput("LabelFontName", "Labels", "Verdana");
		this.addInput(labelFontName, true);

		yAxisTitle = new StringInput("YAxisTitle", "Labels", "Y-Axis");
		this.addInput(yAxisTitle, true);

		secondaryYAxisTitle = new StringInput("SecondaryYAxisTitle", "Labels", "");
		this.addInput(secondaryYAxisTitle, true);

		title = new StringInput("Title", "Labels", "Title");
		this.addInput(title, true);

		xAxisUnits = new StringInput("XAxisUnits", "Labels", "h");
		this.addInput(xAxisUnits, true);

		targetMethodName = new StringInput("TargetMethod", "Data", "");
		this.addInput(targetMethodName, true);

		secondaryTargetMethodName = new StringInput("SecondaryTargetMethod", "Data", "");
		this.addInput(secondaryTargetMethodName, true);

		targetInputParameters = new EntityListListInput<Entity>( Entity.class, "TargetInputParameters", "Data", new ArrayList<ArrayList<Entity>>() );
		this.addInput(targetInputParameters, true);

		secondaryTargetInputParameters = new EntityListListInput<Entity>( Entity.class, "SecondaryTargetInputParameters", "Data", new ArrayList<ArrayList<Entity>>() );
		this.addInput(secondaryTargetInputParameters, true);
	}

	public Graph() {
		allInstances.add(this);
		this.setupEditableKeywordsForSizeAndPosition();
		valuesList = new ArrayList<double[]>();
		linesList = new ArrayList<Line>();
		secondaryValuesList = new ArrayList<double[]>();
		secondaryLinesList = new ArrayList<Line>();
		legendCenter= new Vector3d(0,0,0);			// TopLeft corner of the legend
		legendSize = null;			// size of legend
		legendMarkerSize = null;
	}

	public static ArrayList<? extends Graph> getAll() {
		return allInstances;
	}

	public void kill() {
		super.kill();
		allInstances.remove(this);
	}

	public void initialize(){

		targetEntities = new ArrayList<Entity>();
		lineColors = new ArrayList<ColoringAttributes>();
		targetParameters = new ArrayList<ArrayList<Entity>>(targetInputParameters.getValue());
		for( int i = 0; i < targetEntityList.getValue().size(); i++ ) {
			Entity ent = targetEntityList.getValue().get( i );
			targetEntities.add(ent);
			lineColors.add(lineColorsList.getValue().get(i));
		}

		targetMethod = targetMethodForYAxis("Primary");
		secondaryTargetMethod = targetMethodForYAxis("Secondary");
	}

	/**
	 *
	 * @param yAxis: "Primary" or "Secondary"
	 */
	public Method targetMethodForYAxis(String yAxis) {

		ArrayList<Entity> entityList=null;
		String methodName=null;
		ArrayList<ArrayList<Entity>> inputParameters=null;
		Method method=null;
		ArrayList<double[]> values;

		// Primary y axis
		if(yAxis.equalsIgnoreCase("Primary") ){
			entityList = targetEntities;
			methodName = targetMethodName.getValue();
			inputParameters = targetParameters;
			values = valuesList;
		}

		// Secondary y axis
		else{
			entityList = secondaryTargetEntityList.getValue();
			methodName = secondaryTargetMethodName.getValue();
			inputParameters = secondaryTargetInputParameters.getValue();
			values = secondaryValuesList;
		}

		int numberOfSeries =  entityList.size();
		Entity ent = null;

		// Populate target method from its name and parameters class type
		// Loop through all the input parameters to make sure the input is fine
		for( int i = 0; i < numberOfSeries; i ++ ) {
			ent = entityList.get(i);
			if(ent == null)
				continue;
			Method previousMethod = method;
			Class<?>[] currentParameterTypes = null;
			ArrayList<Entity> currentParameters = null;

			if( inputParameters.size() != 0 ) {
				currentParameters = inputParameters.get(i);
			}
			else{
				currentParameters = new ArrayList<Entity>();
			}
			currentParameterTypes = new Class[ currentParameters.size() ];
			for( int j = 0; j < currentParameters.size(); j++ ) {

				// obtain classes of parameters
				currentParameterTypes[ j ] = currentParameters.get(j).getClass();
			}

			values.add( new double [ numberOfPoints.getValue() ] );

			// try to find the targetMethod
			try {
				method = ent.getClass().getMethod( methodName, currentParameterTypes );
			}
			catch (SecurityException e) {
				throw new SecurityException( "Method:" + ent + "." + methodName + " is not accessible" );
			} catch ( NoSuchMethodException e) {

				// Target method accepts time as input parameter
				if(currentParameters.size() == 0){
					timeInputParameter = true;
					currentParameterTypes = new Class[] { double.class };
					try {
						method = ent.getClass().getMethod( methodName, currentParameterTypes );
					}
					catch (SecurityException e2) {
						throw new SecurityException( "Method:" + ent + "." + methodName + " is not accessible" );
					} catch ( NoSuchMethodException e2) {
						throw new ErrorException("Method: " + methodName + " does not exist, could not invoke.");
					}
				}
				else {
					// user defined parameter type may be a subclass of the defined parameter types
					// Get a list of all methods defined by the target
					ArrayList<Method> matchingMethods = new ArrayList<Method>();
					Method[] methods = ent.getClass().getMethods();

					// try to find method with the same name
					for (int j = 0; j < methods.length; j++) {
						if( methods[j].getName().equals( methodName ) ) {
							matchingMethods.add(methods[j]);
						}
					}

					// for all the method with a matching name, check if parameter matches
					for( int m = 0 ; m < matchingMethods.size() && method == null; m++ ){
						Class<?>[] paratypes = matchingMethods.get(m).getParameterTypes();

						// if number of parameters are not the same, try next method
						if( paratypes.length != currentParameterTypes.length ){
							break;
						}

						// check if parameter types are the same or subclass of defined parameters
						for( int j = 0 ; j < paratypes.length; j++ ){

							// if parameter is not a subclass of the defined parameter class
							if( ! paratypes[j].isInstance( currentParameters.get(j) )){
								break;
							}
						}
						method = matchingMethods.get(m);
					}
				}

				// A method was not found
				if( method == null ) {
					throw new ErrorException("Method: " + methodName + " does not exist, could not invoke.");
				}
				if(previousMethod != null && ! method.equals(previousMethod)){
					throw new ErrorException("Two different methods: " + methodName +
											 " found for input parameters:" + inputParameters);
				}
				previousMethod = method;
			}
		}
		if(endTime.getValue() > 0 && ! timeInputParameter){
			throw new ErrorException(" %s -- value for endTime must not be positive(%f) when the input parameter is not time",
				this, endTime.getValue()
			);
		}
		return method;
	}

	/**
	 * This method updates the DisplayEntity for changes in the given input
	 */
	public void updateForInput( Input<?> in ) {
		super.updateForInput( in );
		modelNeedsRender = true;
	}

	public void render( double time ) {
		super.render(time);

		if( modelNeedsRender ) {
			clearModel();
			this.createLegend();
			this.drawGraphFrame();
			modelNeedsRender = false;
		}

		// Plot series on the primary y-axis
		this.render(valuesList, linesList, yAxisStart.getValue(),
				yAxisEnd.getValue(), targetEntities, primaryIndex,
				primaryRemovedIndex);

		// Plot series on the secondary y-axis
		this.render(secondaryValuesList, secondaryLinesList,
				secondaryYAxisStart.getValue(), secondaryYAxisEnd.getValue(),
				secondaryTargetEntityList.getValue(), secondaryIndex,
				secondaryRemovedIndex);
	}

	/**
	 * Plot data series on the graph
	 * @param values - the data values to plot
	 * @param lines - the lines to be plotted
	 * @param yMin - the minimum y-axis value
	 * @param yMax - the maximum y-axis value
	 * @entities - the entities for which the data is plotted
	 * @index - the number of points to plot from the start of the series
	 * @removedIndex - the number of points not to plot from the end of the series
	 */
	public void render(ArrayList<double[]> values, ArrayList<Line> lines,
			double yMin, double yMax, ArrayList<Entity> entities,
			ArrayList<Integer> index, ArrayList<Integer> removedIndex) {

		double xLength = endTime.getValue() - startTime.getValue();
		double yLength = yMax - yMin;
		double xInterval = xLength/(numberOfPoints.getValue() -1);

		// Display the points for each line
		for(int ind = 0; ind < lines.size(); ind++) {

			// Entity has been removed
			if(entities.get(ind) == null) {
				continue;
			}

			Line currentLine = lines.get(ind);
			double[] currentValues = values.get(ind);
			double[] points = new double[(currentValues.length - 1) * 6];

			for (int i = 0; i < index.get(ind) - 1; i++) {
				int ind1 = i * 6;
				points[ind1 + 0] = graphOrigin.x + graphSize.x  - ((index.get(ind) - i - 1) * xInterval) * graphSize.x / xLength;
				points[ind1 + 1] = Math.max(Math.min(graphOrigin.y + (currentValues[i] - yMin) * graphSize.y / yLength, graphOrigin.y + graphSize.y), graphOrigin.y);
				points[ind1 + 2] = 0.0d;
				points[ind1 + 3] = graphOrigin.x + graphSize.x  - ((index.get(ind) - i - 2) * xInterval) * graphSize.x / xLength;
				points[ind1 + 4] = Math.max(Math.min(graphOrigin.y + (currentValues[i + 1] - yMin) * graphSize.y / yLength, graphOrigin.y + graphSize.y), graphOrigin.y);
				points[ind1 + 5] = 0.0d;
			}
			currentLine.assignPoints(points);
			addShape( currentLine );
		}
	}

	/**
	 * Draw labels, tick marks, horizontal and vertical lines
	 */
	public void drawGraphFrame( ) {
		if( traceFlag ) this.trace( "drawGraphFrame()" );

		double xLength = endTime.getValue() - startTime.getValue();
		double xLengthToNow = 0 - startTime.getValue();
		double yLength = yAxisEnd.getValue() - yAxisStart.getValue();
		double secondYLength = secondaryYAxisEnd.getValue() - secondaryYAxisStart.getValue();

		// Title text height is not defined in the input
		double titleTextH = titleTextHeight.getValue();
		if( titleTextH == 0.0 && ! title.getValue().isEmpty() ) {
			titleTextH = labelTextHeight.getValue();
		}

		// Title text height is not defined in the input
		double yAxisTitleTextH = yAxisTitleTextHeight.getValue();
		if( yAxisTitleTextH == 0.0 && ! yAxisTitle.getValue().isEmpty() ) {
			yAxisTitleTextH = labelTextHeight.getValue();
		}

		double xInterval = xLengthToNow/( numberOfPoints.getValue() - 1 );
		double textWidth = 0.0;

		// Background fill graphics
		fillModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_FILLED, "fillModel" );
		fillModel.setColor(backgroundColor.getValue());
		addShape( fillModel );

		// Background outline graphics
		outlineModel = new Rectangle( 1.0, 1.0, Rectangle.SHAPE_OUTLINE, "outlineModel" );
		outlineModel.setColor(borderColor.getValue());
		addShape( outlineModel );

		Vector3d graphExtent = this.getSize();
		// Draw graphic rectangle
		graphSize = new Vector3d();
		graphSize.setX( ( graphExtent.x - ( leftMargin.getValue() + rightMargin.getValue()  ) ) / graphExtent.x );
		graphSize.setY( ( graphExtent.y - ( topMargin.getValue() + bottomMargin.getValue() ) ) / graphExtent.y
		);

		// Center position of the graph
		Vector3d graphCenter = new Vector3d( ( ( leftMargin.getValue() ) / 2 -	rightMargin.getValue()/2 ) / graphExtent.x,
				(( bottomMargin.getValue() ) / 2 - ( topMargin.getValue() ) / 2 ) / graphExtent.y , 0.0
		);

		// Fill graphics
		graphFillModel = new Rectangle( graphSize.x, graphSize.y, Rectangle.SHAPE_FILLED, "fillModel" );
		addShape( graphFillModel );
		graphFillModel.setColor( graphColor.getValue() );
		graphFillModel.setCenter( graphCenter.x, graphCenter.y );

		// Outline graphics
		graphOutlineModel = new Rectangle( graphSize.x, graphSize.y, Rectangle.SHAPE_OUTLINE, "outlineModel" );
		addShape( graphOutlineModel );
		graphOutlineModel.setColor( borderColor.getValue() );
		graphOutlineModel.setCenter( graphCenter.x, graphCenter.y );

		graphOrigin = new Vector3d( graphCenter.x - graphSize.x/2, graphCenter.y - graphSize.y/2, 0.0  );

		/** A) Draw Horizontal lines **/
		for( int i = 0; i < yLines.getValue().size(); i++ ) {
			double yPos = graphOrigin.y + ( yLines.getValue().get( i ) - yAxisStart.getValue())*graphSize.y / yLength;
			Line each = new Line( graphOrigin.x, yPos, graphOrigin.x + graphSize.x, yPos, "yLine" + i );
			ColoringAttributes currentColor = Shape.getColorWithName( "gray67" );
			if(yLinesColor.getValue().size() > 1){
				currentColor = yLinesColor.getValue().get(i);
			}
			else if(yLinesColor.getValue().size() == 1){
				currentColor = yLinesColor.getValue().get(0);
			}
			each.setColor(currentColor);
			addShape( each );
		}

		/** B) Draw Vertical lines **/
		for( int i = 0; i < xLines.getValue().size(); i++ ) {
			double xPos = graphOrigin.x + ( xLines.getValue().get( i ) - startTime.getValue())*graphSize.x / xLength;
			Line each = new Line( xPos, graphOrigin.y , xPos, graphOrigin.y + graphSize.y , "xLine" + i );
			ColoringAttributes currentColor = Shape.getColorWithName( "gray67" );
			if(xLinesColor.getValue().size() > 1){
				currentColor = xLinesColor.getValue().get(i);
			}
			else if(xLinesColor.getValue().size() == 1){
				currentColor = xLinesColor.getValue().get(0);
			}

			each.setColor(currentColor);
			addShape( each );
		}

		/** C) Create y axis labels and ticks **/
		LabelShape aLabel = new LabelShape( "0", labelTextHeight.getValue()* graphExtent.y/graphExtent.x/ graphExtent.y, labelTextHeight.getValue()/
				graphExtent.y, 0.0, 0.0, labelFontName.getValue()
		);
		double characterWidth =  aLabel.getTextWidth()/graphExtent.x; // label character width on graph
				if( traceFlag )
					this.traceLine( "width=" + labelTextHeight.getValue()* graphExtent.y/graphExtent.x/ graphExtent.y + "  " +
							"Height=" + labelTextHeight.getValue()/ graphExtent.y
					);
		double characterHight= aLabel.getTextHeight()/graphExtent.y;  // label character height on graph
		double xTickSize = characterHight*graphExtent.y/2;
		double yTickSize = characterWidth*graphExtent.x/2;

		double maxYLabelWidth = 0; // the real length of the widest y axis labels
		for( int i = 0; i * yAxisInterval.getValue() <= yLength; i ++ ) {
			String text = String.format( "%." + yAxisPrecision + "f",  ( i * yAxisInterval.getValue() + yAxisStart.getValue() ) *
					yAxisMultiplier.getValue()
			);
			textWidth = labelTextHeight.getValue() * text.length() * graphExtent.y/graphExtent.x;
			double yPos = graphOrigin.y + (i * yAxisInterval.getValue() * graphSize.y )/yLength; // current label
			double xPos = graphOrigin.x - yAxisLabelGap.getValue() - characterWidth*graphExtent.x-yTickSize;
			aLabel = new LabelShape( text, textWidth/ graphExtent.y, labelTextHeight.getValue()/ graphExtent.y, xPos, yPos, labelFontName.getValue()  );
			maxYLabelWidth = Math.max( maxYLabelWidth, aLabel.getTextWidth() );
			if( traceFlag ) this.traceLine( "length=" + text.length() );
			if( traceFlag ) this.traceLine( "width=" + textWidth/ graphExtent.y + "   height=" +
					labelTextHeight.getValue()/ graphExtent.y
			);
			aLabel.setRightJustified( true ); // numbers on y axis are right justified
			aLabel.setForColor( labelFontColor.getValue() );
			this.getModel().addChild( aLabel );

			// y axis tick marks
			Line each = new Line( graphOrigin.x - yTickSize, yPos, graphOrigin.x, yPos , "yTick" + i );
			each.setColor( tickColor.getValue() );
			addShape( each );
		}

		/** C-2nd) Create Secondary y axis labels and ticks **/
		double maxSecondYLabelWidth = 0; // the real length of the widest secondary y axis labels
		if(secondaryYAxisEnd.getValue()>0){
			for( int i = 0; i * secondaryYAxisInterval.getValue() <= secondYLength; i ++ ) {
				String text = String.format( "%." + secondaryYAxisPrecision + "f",  ( i * secondaryYAxisInterval.getValue() + secondaryYAxisStart.getValue() ) *
						secondaryYAxisMultiplier.getValue()
				);
				textWidth = labelTextHeight.getValue() * text.length() * graphExtent.y/graphExtent.x;
				double yPos = graphOrigin.y + (i * secondaryYAxisInterval.getValue() * graphSize.y )/secondYLength; // current label
				double xPos = graphOrigin.x + graphSize.x + yAxisLabelGap.getValue() + characterWidth*graphExtent.x+yTickSize;
				aLabel = new LabelShape( text, textWidth/ graphExtent.y, labelTextHeight.getValue()/ graphExtent.y, xPos, yPos, labelFontName.getValue()  );
				maxSecondYLabelWidth = Math.max( maxYLabelWidth, aLabel.getTextWidth() );
				if( traceFlag ) this.traceLine( "length=" + text.length() );
				if( traceFlag ) this.traceLine( "width=" + textWidth/ graphExtent.y + "   height=" +
						labelTextHeight.getValue()/ graphExtent.y
				);
				aLabel.setLeftJustified( true ); // numbers on secondary axis are left justified
				aLabel.setForColor( labelFontColor.getValue() );
				this.getModel().addChild( aLabel );

				// Secondary y axis tick marks
				Line each = new Line( graphOrigin.x + graphSize.x + yTickSize, yPos, graphOrigin.x + graphSize.x, yPos , "yTick" + i );
				each.setColor( tickColor.getValue() );
				addShape( each );
			}
		}

		/** D) y axis label **/
		if( ! yAxisTitle.getValue().isEmpty() ) {
			textWidth = yAxisTitleTextHeight.getValue() * yAxisTitle.getValue().length();
			yAxisLabel = new LabelShape( yAxisTitle.getValue(), textWidth/graphExtent.x , yAxisTitleTextHeight.getValue()/graphExtent.y,
				graphOrigin.x - (maxYLabelWidth + (yAxisTitleGap.getValue()+yAxisTitleTextHeight.getValue())/
						graphExtent.x+yTickSize*2),  0, labelFontName.getValue()
			);
			yAxisLabel.setForColor( titleColor.getValue() );
			yAxisLabel.rotate( 90 );
			this.getModel().addChild( yAxisLabel );
		}

		/** D-2nd) Secondary y axis title **/
		if( ! secondaryYAxisTitle.getValue().isEmpty() ) {
			textWidth = yAxisTitleTextHeight.getValue() * secondaryYAxisTitle.getValue().length();
			LabelShape label = new LabelShape( secondaryYAxisTitle.getValue(), textWidth/graphExtent.x , yAxisTitleTextHeight.getValue()/graphExtent.y,
				graphOrigin.x + graphSize.x + (maxSecondYLabelWidth + (yAxisTitleGap.getValue()+yAxisTitleTextHeight.getValue())/
						graphExtent.x+yTickSize),  0, labelFontName.getValue()
			);
			label.setForColor( titleColor.getValue() );
			label.rotate( -90 );
			this.getModel().addChild( label );
		}

		/** E) Create x axis labels and ticks **/

		// E-1) From the start time to end time
		for( int i = 0; startTime.getValue() + i * timeInterval.getValue() <= endTime.getValue(); i++ ) {

			// Now point is handled separately
			if( startTime.getValue() + i * timeInterval.getValue() == 0 ) {
				continue;
			}
			double xIntervalsToPoint = ( i * timeInterval.getValue()) / xInterval ;
			double xPos = graphOrigin.x + ( xIntervalsToPoint * xInterval * graphSize.x)/xLength;
			double yPos = graphOrigin.y - xAxisLabelGap.getValue()/graphExtent.y - characterHight * graphExtent.y - xTickSize;
			String text = String.format( "%." + xAxisPrecision + "f" + xAxisUnits, (startTime.getValue() + i * timeInterval.getValue())
				* xAxisMultiplier.getValue()
			) ;
			textWidth = labelTextHeight.getValue() * text.length() * graphExtent.y/graphExtent.x;
			aLabel = new LabelShape( text, textWidth/ graphExtent.y,labelTextHeight.getValue()/ graphExtent.y, xPos , yPos, labelFontName.getValue() );
			aLabel.setForColor( labelFontColor.getValue() );
			this.getModel().addChild( aLabel );

			// x tick marks
			Line each = new Line( xPos, graphOrigin.y - xTickSize, xPos, graphOrigin.y, "xTick" + 2 );
			each.setColor( tickColor.getValue() );
			addShape( each );
		}

		// E-2) Now point(time = 0) and its tick mark on x axis
		double timeIntervalsToNowPoint = (-startTime.getValue() /  xInterval);
		double xNowPos = graphOrigin.x + ( timeIntervalsToNowPoint * xInterval * graphSize.x )/xLength;
		double yNowPos = graphOrigin.y - xAxisLabelGap.getValue()/graphExtent.y - characterHight * graphExtent.y -xTickSize;
		textWidth = labelTextHeight.getValue() * "Now".length() * graphExtent.y/graphExtent.x;
		aLabel = new LabelShape( "Now", textWidth/ graphExtent.y,labelTextHeight.getValue()/ graphExtent.y, xNowPos, yNowPos, labelFontName.getValue() );
		aLabel.setForColor( labelFontColor.getValue() );
		this.getModel().addChild( aLabel );
		Line each = new Line( xNowPos, graphOrigin.y - xTickSize, xNowPos, graphOrigin.y, "NowTick" );
		each.setColor( tickColor.getValue() );
		addShape( each );

		/** F) Title **/
		if( ! title.getValue().isEmpty() ) {
			textWidth = titleTextH * title.getValue().length() * graphExtent.y/graphExtent.x;
			titleLabel = new LabelShape( title.getValue(), textWidth/ graphExtent.y , titleTextH/ graphExtent.y , 0.0,
				graphOrigin.y + graphSize.y + ( titleTextH + titleGap.getValue() ) / graphExtent.y, labelFontName.getValue()
			);
			titleLabel.setForColor( titleColor.getValue() );
			this.getModel().addChild( titleLabel );
		}
	}

	/**
	 * create legend for graph
	 */
	public void createLegend(){


		if( legendSize == null ){
			return;
		}

		// list of lines on the graph
		int numberOfLines = targetEntities.size();

		// temp values to draw squares and make labels
		Rectangle temp;
		TextLabel lineLabel;

		// values for Rectangle will be based on Graph center as 0,0. adjust coordinates
		double inputsLegendYTopLeft = legendCenter.getY() + legendSize.getY()/2;

		double legendXCenter = ( legendCenter.getX() - this.getPositionForAlignment(new Vector3d()).x ) / this.getSize().x;
		double legendYCenter = ( legendCenter.getY() - this.getPositionForAlignment(new Vector3d()).y ) / this.getSize().y;

		double legendWidth =  legendSize.getX()/this.getSize().x;
		double legendHeight = legendSize.getY()/this.getSize().y;

		double legendXTopLeft = legendXCenter - legendWidth/2;
		double legendYTopLeft = legendYCenter + legendHeight/2;

		// size each mark
		double xIncrement = legendWidth/8;
		//double yIncrement = legendHeight/(2*numberOfLines);
		double yIncrement = legendHeight/(numberOfLines+1);

		// Size of marker for each series
		double markerSizeX = 0.2 * legendWidth;
		double markerSizeY = 0.8 * yIncrement;
		if( legendMarkerSize != null ){
			markerSizeX = legendMarkerSize.getX()/this.getSize().x;
			markerSizeY = legendMarkerSize.getY()/this.getSize().y;
		}

		// marker Center
		double markerCenterX = legendXTopLeft + xIncrement;
		// Gap from left margin is set
		if( seriesMakerGap.getValue() > 0 ){

			// center of marker = left margin + gap + half of markerSize
			// leftMargin = centerOfLegend - LengendWidth/2
			markerCenterX = legendXCenter - legendWidth/2 + seriesMakerGap.getValue() + markerSizeX/2;
		}

		// double series text label
		double seriesTextX = legendCenter.getX() - legendWidth/2;
		if( seriesLabelGap.getValue() > 0 ){
			seriesTextX = legendCenter.getX() - legendSize.getX()/2  + seriesLabelGap.getValue();
		}


		//LegendFrame
		temp = new Rectangle( legendXCenter, legendYCenter, legendCenter.getZ(), legendWidth, legendHeight, Rectangle.SHAPE_OUTLINE );
		temp.setColor( borderColor.getValue() );
		temp.setLayer( 0 );
		addShape( temp );

		// create a rectangle and a name for each entity and its series
		for( int eInd = 0; eInd < targetEntities.size(); eInd++ ){
			// add colour of the entity-series
			temp = new Rectangle( markerSizeX, markerSizeY, Rectangle.SHAPE_FILLED, "LineColourLegend" );
			temp.setColor( lineColors.size() == targetEntities.size() ?
				lineColors.get(eInd):
				Shape.getColorWithName("Orange") // Default color
			);
			// based on graph center as (0,0,0)
			temp.setCenter( markerCenterX  , legendYTopLeft - (eInd + 1) *  yIncrement, legendCenter.getZ() );

			temp.setLayer( 0 );
			addShape( temp );

			// add name of the entity-series
			lineLabel = new TextLabel();
			InputAgent.processEntity_Keyword_Value(lineLabel, "Text", targetEntities.get(eInd).toString());

			InputAgent.processEntity_Keyword_Value(lineLabel, "TextHeight", legendTextHeight.getValue().toString());
			// center is actually bottom left
			lineLabel.setPosition(new Vector3d(seriesTextX, inputsLegendYTopLeft - (4 * (eInd + 1) + 1) * legendSize.y / (4 * (numberOfLines + 1)), legendCenter.z));
			lineLabel.initializeGraphics();
			lineLabel.enterRegion( this.getCurrentRegion() );
		}
	}

	public void startGraph() {
		this.clearGraph();

		for( int i = 0; i < targetEntityList.getValue().size(); i++ ){
			Line temp = new Line(numberOfPoints.getValue(), "Line");
			temp.setColor(getLineColor(i, lineColors, targetEntities));
			temp.setLineAttributes(getLineWidth(i, lineWidths));
			linesList.add(temp);
			this.addShape(temp);
		}

		for( int i = 0; i < secondaryTargetEntityList.getValue().size(); i++ ){
			Line temp = new Line(numberOfPoints.getValue(), "Line");
			temp.setColor(getLineColor(i, secondaryLineColorsList.getValue(), secondaryTargetEntityList.getValue()));
			temp.setLineAttributes(getLineWidth(i, secondaryLineWidths));
			secondaryLinesList.add(temp);
			this.addShape(temp);
		}

		this.startProcess("processGraph");
	}

	protected ColoringAttributes getLineColor(int index, ArrayList<ColoringAttributes> colorList, ArrayList<Entity> targets) {
		ColoringAttributes currentLineColour = Shape.getColorWithName("Red"); // Default color
		if (colorList.size() == targets.size())
			currentLineColour = colorList.get(index);
		else if(colorList.size() == 1)
			currentLineColour = colorList.get(0);

		return currentLineColour;
	}

	protected LineAttributes getLineWidth(int index, DoubleListInput widthList) {
		LineAttributes la = new LineAttributes();
		double lineWidth = 1.0d; // Default
		if (widthList.getValue().size() > 1)
			lineWidth = widthList.getValue().get(index);
		else if (widthList.getValue().size() == 1)
			lineWidth = widthList.getValue().get(0);
		la.setLineWidth((float)lineWidth);

		return la;
	}

	/**
	 * Clear the given lines from the graph
	 */
	protected void clearGraph() {
		for (Line each : linesList) {
			if(each != null)
				this.removeShape(each);
		}
		linesList.clear();

		for (Line each : secondaryLinesList) {
			this.removeShape(each);
		}
		secondaryLinesList.clear();
	}

	/**
	 * Calculate values for the data series on the graph
	 */
	public void processGraph() {
		if( traceFlag ) this.trace( "processGraph()" );

		double xLength = endTime.getValue() - startTime.getValue();
		double xInterval = xLength/(numberOfPoints.getValue() -1);

		// Initialize primary y-axis
		primaryIndex = new ArrayList<Integer>();
		primaryRemovedIndex = new ArrayList<Integer>();
		for(int ind = 0; ind < linesList.size(); ind++) {
			double[] currentValues = valuesList.get(ind);

			primaryIndex.add(ind, 0);
			primaryRemovedIndex.add(ind, 0);
			for( int i = 0; i * xInterval < endTime.getValue(); i++ ) {
				Double presentValue = this.getCurrentValue( i * xInterval , ind, targetParameters, targetMethod, targetEntities );
				currentValues[primaryIndex.get(ind)] = presentValue;
				primaryIndex.set(ind, primaryIndex.get(ind)+1);
			}
		}

		// Initialize secondary y-axis
		secondaryIndex = new ArrayList<Integer>();
		secondaryRemovedIndex = new ArrayList<Integer>();
		for(int ind = 0; ind < secondaryLinesList.size(); ind++) {
			double[] currentValues = secondaryValuesList.get(ind);

			secondaryIndex.add(ind, 0);
			secondaryRemovedIndex.add(ind, 0);
			for( int i = 0; i * xInterval < endTime.getValue(); i++ ) {
				Double presentValue = this.getCurrentValue( i * xInterval , ind, secondaryTargetInputParameters.getValue(), secondaryTargetMethod, secondaryTargetEntityList.getValue() );
				currentValues[secondaryIndex.get(ind)] = presentValue;
				secondaryIndex.set(ind, secondaryIndex.get(ind)+1);
			}
		}

		while ( true ) {

			// Calculate values for the primary y-axis
			if(targetEntityList.getValue().size() > 0)
				this.processGraph(valuesList, linesList, targetParameters,
						targetMethod, targetEntities, primaryIndex,
						primaryRemovedIndex);

			// Calculate values for the secondary y-axis
			if(secondaryTargetEntityList.getValue().size() > 0)
				this.processGraph(secondaryValuesList, secondaryLinesList,
						secondaryTargetInputParameters.getValue(),
						secondaryTargetMethod,
						secondaryTargetEntityList.getValue(), secondaryIndex,
						secondaryRemovedIndex);

			scheduleWait( xInterval, 7 );
		}
	}

	/**
	 * Calculate values for the data series on the graph
	 * @param values - the data values to plot
	 * @param lines - the lines to be plotted
	 * @entities - the entities for which the data is plotted
	 * @index - the number of points to plot from the start of the series
	 * @removedIndex - the number of points not to plot from the end of the series
	 */
	public void processGraph(ArrayList<double[]> values, ArrayList<Line> lines,
			ArrayList<ArrayList<Entity>> parameters, Method method,
			ArrayList<Entity> entities, ArrayList<Integer> index,
			ArrayList<Integer> removedIndex) {

			for(int ind = 0; ind < lines.size(); ind++) {

				// Entity has been removed
				if(entities.get(ind) == null) {
					continue;
				}

				double[] currentValues = values.get(ind);

				double presentValue = this.getCurrentValue( getCurrentTime() + endTime.getValue() , ind, parameters, method, entities );

				if (index.get(ind) < currentValues.length) {
					currentValues[index.get(ind)] = presentValue;
					index.set(ind, index.get(ind)+1);
				}
				else {
					System.arraycopy(currentValues, 1, currentValues, 0, currentValues.length - 1);
					currentValues[currentValues.length - 1] = presentValue;
				}
			}
	}

	/**
	 * Access the target method of the target entity and return its value for a entity on ind
	 *
	 * 1) targetInputParameters has values => this values are passing to the method and returns a number
	 * 2) timeInputParameter == false  => the method should have no argument and returns a number
	 * 3) timeInputParameter == true   => the method has time input argument and returns a number
	 * yAxis: Primary or Secondary
	 * @return double
	 */
	protected Double getCurrentValue( double time , int ind, ArrayList<ArrayList<Entity>> parameters, Method method, ArrayList<Entity> entities ) {
		if( traceFlag ) this.trace( "getCurrentValue( " + time + " , " + ind + " )" );

		Entity currentEntity = entities.get(ind);
		Object[ ] params = new Object [ 0 ];
		if( parameters.size() != 0 ) {
			params = parameters.get(ind).toArray();
		}

		// Time is passing to the method as an argument
		if(timeInputParameter) {
			params = new Object [ ] { time };
		}

		double value = 0;
		try {
			// run target method and return its value
			value = (Double) method.invoke( currentEntity, params );
		}
		catch (IllegalArgumentException e) {
			this.error( "getCurrentValue", "Illegal argument has been passed to " + method, "parameters=" + params );
		}
		catch (IllegalAccessException e) {
			this.error( "getCurrentValue", "access to this method is prohibited", "method:" + method );
		}
		catch ( InvocationTargetException e) {
			this.error( "getCurrentValue", "exception happened in method" + method , e.getMessage() );
		}
		return value;
	}

	public void setupEditableKeywordsForSizeAndPosition() {
		if( traceFlag ) this.trace( "setupEditableKeywordsForSizeAndPosition()" );

		addEditableKeyword( "LegendCenter",			 "  -  ",   "  0,0  ", false, "Legend" );
		addEditableKeyword( "LegendSize",			 "  -  ",   "  0,0  ", false, "Legend" );
		addEditableKeyword( "LegendMarkerSize",	     "  -  ",   "   ", false, "Legend" );
	}

	public void readData_ForKeyword(StringVector data, String keyword, boolean syntaxOnly, boolean isCfgInput)
	throws InputErrorException {
		if( "LegendCenter".equalsIgnoreCase( keyword ) ) {
			legendCenter = Input.parseVector3d(data);
			return;
		}
		if( "LegendSize".equalsIgnoreCase( keyword ) ) {
			legendSize = Input.parseVector3d(data, 0.0d, Double.POSITIVE_INFINITY);
			return;
		}
		if( "LegendMarkerSize".equalsIgnoreCase( keyword ) ) {
			legendMarkerSize = Input.parseVector3d(data, 0.0d, Double.POSITIVE_INFINITY);
			return;
		}
		super.readData_ForKeyword( data, keyword, syntaxOnly, isCfgInput );
	}

	// ******************************************************************************************
	// OUTPUT METHODS
	// ******************************************************************************************

	/**
	 * Return the value for the given data point index for the given series index.
	 * @param seriesIndex - the index of the data series (starting from 1)
	 * @param pointIndex - the index of the data point (starting from 1)
	 */
	public double Series_Point( Integer seriesIndex, Integer pointIndex ) {
		return valuesList.get(seriesIndex - 1)[pointIndex - 1];
	}

	public void validate()
	throws InputErrorException {
		super.validate();

		if(yLinesColor.getValue().size() > 1) {
			Input.validateIndexedLists(yLines.getValue(), yLinesColor.getValue(), "YLines", "YLinesColor");
		}
		if(targetInputParameters.getValue().size() > 0)
			Input.validateIndexedLists(targetEntityList.getValue(), targetInputParameters.getValue(), "TargetEntityList", "TargetInputParameters");

		if(xLinesColor.getValue().size() > 1) {
			Input.validateIndexedLists(xLines.getValue(), xLinesColor.getValue(), "XLines", "XLinesColor");
		}

		if(lineColorsList.getValue().size() > 1){
			Input.validateIndexedLists(targetEntityList.getValue(), lineColorsList.getValue(),
					"TargetEntityList", "LinesColor"
			);
		}

		if(secondaryLineColorsList.getValue().size() > 1){
			Input.validateIndexedLists(secondaryTargetEntityList.getValue(), secondaryLineColorsList.getValue(),
					"SecondaryTargetEntityList", "SecondaryLinesColor"
			);
		}

		if(lineWidths.getValue().size() > 1)
			Input.validateIndexedLists(targetEntityList.getValue(), lineWidths.getValue(), "TargetEntity", "LineWidths");

		if(secondaryLineWidths.getValue().size() > 1)
			Input.validateIndexedLists(secondaryTargetEntityList.getValue(), secondaryLineWidths.getValue(), "SecondaryTargetEntity", "SecondaryLineWidths");

		for( int i = 0; i < yLines.getValue().size(); i++ ) {
			double y = yLines.getValue().get( i );
			if( y > yAxisEnd.getValue() || y < yAxisStart.getValue() ) {
				throw new InputErrorException("value for yLines should be in (%f, %f) range -- it is (%f)",
					yAxisStart.getValue(), yAxisEnd.getValue(), y);
			}
		}

		for( int i = 0; i < xLines.getValue().size(); i++ ) {
			double x = xLines.getValue().get( i );
			if( x < startTime.getValue() || x > endTime.getValue() ) {
				throw new InputErrorException("value for xLines should be in (%f, %f) range -- it is (%f)",
					startTime.getValue(), endTime.getValue(), x);
			}
		}
	}
}
