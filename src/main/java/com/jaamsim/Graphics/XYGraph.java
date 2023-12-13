/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2023 JaamSim Software Inc.
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

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputCallback;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.UnitTypeInput;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public class XYGraph extends AbstractGraph {

	@Keyword(description = "Unit type for the x-axis. "
	                     + "MUST be entered before most other inputs for this axis.",
	         exampleList = {"DistanceUnit"})
	private final UnitTypeInput xUnitType;

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an "
	                     + "array of numbers with or without units.",
	         exampleList = {"'{1.0[m], 0.5[m]}'",
	                        "{ [Statistics1].HistogramBinFractions } { [Statistics2].HistogramBinFractions }"})
	protected final ExpressionInput yDataSource;

	@Keyword(description = "One or more sources of data for the x-axis values corresponding to the "
	                     + "primary y-axis data sources.\n"
	                     + "Each source is specified by an array of numbers with or without units.",
	         exampleList = {"'{1.0[m], 0.5[m]}'",
	                        "{ [Statistics1].HistogramBinCentres } { [Statistics2].HistogramBinCentres }"})
	protected final ExpressionInput xDataSource;

	@Keyword(description = "Set to TRUE if the primary series are to be shown as bars instead of lines.")
	protected final BooleanInput showBars;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n"
	                     + "Each source is graphed as a separate line or bar and is specified by an Entity and its Output.",
	         exampleList = {"{ Entity1 Output1 } { Entity2 Output2 }"})
	protected final ExpressionInput ySecondaryDataSource;

	@Keyword(description = "One or more sources of data for the x-axis values corresponding to the "
	                     + "secondary y-axis data sources.\n"
	                     + "Each source is specified by an array of numbers with or without units.",
	         exampleList = {"'{1.0[m], 0.5[m]}'",
	                        "{ [Statistics1].HistogramBinCentres } { [Statistics2].HistogramBinCentres }"})
	protected final ExpressionInput xSecondaryDataSource;

	@Keyword(description = "Set to TRUE if the secondary series are to be shown as bars instead of lines.")
	protected final BooleanInput secondaryShowBars;

	{
		xUnitType = new UnitTypeInput("XAxisUnitType", KEY_INPUTS, DimensionlessUnit.class);
		xUnitType.setCallback(xAxisUnitTypeCallback);
		this.addInput(xUnitType);

		yDataSource = new ExpressionInput("YDataSource", KEY_INPUTS, null);
		yDataSource.setResultType(ExpResType.COLLECTION);
		yDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(yDataSource);

		xDataSource = new ExpressionInput("XDataSource", KEY_INPUTS, null);
		xDataSource.setResultType(ExpResType.COLLECTION);
		xDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xDataSource);

		showBars = new BooleanInput("ShowBars", KEY_INPUTS, false);
		this.addInput(showBars);

		ySecondaryDataSource = new ExpressionInput("YSecondaryDataSource", KEY_INPUTS, null);
		ySecondaryDataSource.setResultType(ExpResType.COLLECTION);
		ySecondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(ySecondaryDataSource);

		xSecondaryDataSource = new ExpressionInput("XSecondaryDataSource", KEY_INPUTS, null);
		xSecondaryDataSource.setResultType(ExpResType.COLLECTION);
		xSecondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		this.addInput(xSecondaryDataSource);

		secondaryShowBars = new BooleanInput("SecondaryShowBars", KEY_INPUTS, false);
		this.addInput(secondaryShowBars);
	}

	public XYGraph() {}

	static final InputCallback xAxisUnitTypeCallback = new InputCallback() {
		@Override
		public void callback(Entity ent, Input<?> inp) {
			Class<? extends Unit> ut = ((UnitTypeInput) inp).getUnitType();
			((XYGraph) ent).setXAxisUnit(ut);
		}
	};

	@Override
	public void setInputsForDragAndDrop() {}

	@Override
	protected void setYAxisUnit(Class<? extends Unit> unitType) {
		super.setYAxisUnit(unitType);
		yDataSource.setUnitType(unitType);
	}

	@Override
	protected void setSecondaryYAxisUnit(Class<? extends Unit> unitType) {
		super.setSecondaryYAxisUnit(unitType);
		ySecondaryDataSource.setUnitType(unitType);
	}

	@Override
	protected void setXAxisUnit(Class<? extends Unit> unitType) {
		super.setXAxisUnit(unitType);
		xDataSource.setUnitType(unitType);
	}

	@Override
	public boolean showSecondaryYAxis() {
		return !ySecondaryDataSource.isDefault();
	}

	@Override
	public void updateGraphics(double simTime){
		super.updateGraphics(simTime);

		getPrimarySeries().clear();
		getSecondarySeries().clear();

		if (!yDataSource.isDefault() && !xDataSource.isDefault()) {
			ExpResult.Collection yCol = yDataSource.getNextResult(this, simTime).colVal;
			ExpResult.Collection xCol = xDataSource.getNextResult(this, simTime).colVal;

			int numPoints = Math.min(xCol.getSize(), yCol.getSize());
			populatePrimarySeriesInfo(1, numPoints, null);

			int series = -1;
			for (SeriesInfo info : getPrimarySeries()) {
				series++;
				info.numPoints = numPoints;
				info.indexOfLastEntry = numPoints - 1;
				info.lineColour = getLineColor(series);
				info.lineWidth = getLineWidth(series);
				for (int i = 0; i < info.numPoints; i++ ) {
					ExpResult ind = ExpResult.makeNumResult(i + 1, DimensionlessUnit.class);
					try {
						info.yValues[i] = yCol.index(ind).value;
						info.xValues[i] = xCol.index(ind).value;
					}
					catch (ExpError e) {}
				}
				//System.out.format("numPoints=%s, yValues=%s, xValues=%s%n", info.numPoints,
				//		Arrays.toString(info.yValues), Arrays.toString(info.xValues));
			}
		}

		if (!ySecondaryDataSource.isDefault() && !xSecondaryDataSource.isDefault()) {
			ExpResult.Collection ySecCol = ySecondaryDataSource.getNextResult(this, simTime).colVal;
			ExpResult.Collection xSecCol = xSecondaryDataSource.getNextResult(this, simTime).colVal;

			int numPointsSec = Math.min(xSecCol.getSize(), ySecCol.getSize());
			populateSecondarySeriesInfo(1, numPointsSec, null);

			int series = -1;
			for (SeriesInfo info : getSecondarySeries()) {
				series++;
				info.numPoints = numPointsSec;
				info.indexOfLastEntry = numPointsSec - 1;
				info.lineColour = getSecondaryLineColor(series);
				info.lineWidth = getSecondaryLineWidth(series);
				for (int i = 0; i < info.numPoints; i++ ) {
					ExpResult ind = ExpResult.makeNumResult(i + 1, DimensionlessUnit.class);
					try {
						info.yValues[i] = ySecCol.index(ind).value;
						info.xValues[i] = xSecCol.index(ind).value;
					}
					catch (ExpError e) {}
				}
			}
		}
	}

}
