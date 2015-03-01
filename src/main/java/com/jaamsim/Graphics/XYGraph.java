/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2009-2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Graphics;

import java.util.ArrayList;

import com.jaamsim.Samples.SampleExpListInput;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColorListInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Color4d;
import com.jaamsim.units.UserSpecifiedUnit;

public class XYGraph extends GraphBasics {

	// Key Inputs category

	@Keyword(description = "One or more sources of data to be graphed on the primary y-axis.\n" +
			"Each source is graphed as a separate line or bar and is specified by an Entity and its Output.",
     example = "XYGraph1 DataSource { { Entity1 Output1 } { Entity2 Output2 } }")
	protected final SampleExpListInput dataSource;

	@Keyword(description = "A list of colors for the primary series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple series, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the series.",
	         example = "XYGraph1 SeriesColors { { red } { green } }")
	protected final ColorListInput seriesColorsList;

	@Keyword(description = "Set to TRUE if the primary series are to be shown as bars instead of lines.",
     example = "XYGraph1 ShowBars { TRUE }")
	protected final BooleanInput showBars;

	@Keyword(description = "One or more sources of data to be graphed on the secondary y-axis.\n" +
			"Each source is graphed as a separate line or bar and is specified by an Entity and its Output.",
     example = "XYGraph1 SecondaryDataSource { { Entity1 Output1 } { Entity2 Output2 } }")
	protected final SampleExpListInput secondaryDataSource;

	@Keyword(description = "A list of colors for the secondary series to be displayed.\n" +
			"Each color can be specified by either a color keyword or an RGB value.\n" +
			"For multiple series, each color must be enclosed in braces.\n" +
			"If only one color is provided, it is used for all the series.",
	         example = "XYGraph1 SecondarySeriesColors { { red } { green } }")
	protected final ColorListInput secondarySeriesColorsList;

	@Keyword(description = "Set to TRUE if the secondary series are to be shown as bars instead of lines.",
     example = "XYGraph1 SecondaryShowBars { TRUE }")
	protected final BooleanInput secondaryShowBars;

	{
		// Key Inputs category

		dataSource = new SampleExpListInput("DataSource", "Key Inputs", null);
		dataSource.setUnitType(UserSpecifiedUnit.class);
		dataSource.setEntity(this);
		this.addInput(dataSource);

		ArrayList<Color4d> defSeriesColor = new ArrayList<>(0);
		defSeriesColor.add(ColourInput.getColorWithName("red"));
		seriesColorsList = new ColorListInput("SeriesColours", "Key Inputs", defSeriesColor);
		seriesColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(seriesColorsList);
		this.addSynonym(seriesColorsList, "LineColors");

		showBars = new BooleanInput("ShowBars", "Key Inputs", false);
		this.addInput(showBars);

		secondaryDataSource = new SampleExpListInput("SecondaryDataSource", "Key Inputs", null);
		secondaryDataSource.setUnitType(UserSpecifiedUnit.class);
		secondaryDataSource.setEntity(this);
		this.addInput(secondaryDataSource);

		ArrayList<Color4d> defSecondaryLineColor = new ArrayList<>(0);
		defSecondaryLineColor.add(ColourInput.getColorWithName("black"));
		secondarySeriesColorsList = new ColorListInput("SecondarySeriesColours", "Key Inputs", defSecondaryLineColor);
		secondarySeriesColorsList.setValidCountRange(1, Integer.MAX_VALUE);
		this.addInput(secondarySeriesColorsList);
		this.addSynonym(secondarySeriesColorsList, "SecondaryLineColors");

		secondaryShowBars = new BooleanInput("SecondaryShowBars", "Key Inputs", false);
		this.addInput(secondaryShowBars);
	}

}
