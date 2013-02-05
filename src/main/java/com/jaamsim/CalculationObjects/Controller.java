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
package com.jaamsim.CalculationObjects;

import com.sandwell.JavaSimulation.DoubleInput;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.Simulation;
import com.sandwell.JavaSimulation3D.DisplayEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;

/**
 * The Controller object simulates the operation of a Programmable Logic Controller
 * @author Harry King
 *
 */
public class Controller extends DisplayEntity {

	@Keyword(desc = "The sampling time for the Controller.",
	         example = "Controller1 SamplingTime { 100 ms }")
	private final DoubleInput samplingTimeInput;

	private final ArrayList<CalculationEntity> calculationEntityList;  // List of the CalculationEntities controller by this Controller.
	private int count;  // Number of times that the controller has initiated its calculations.


	{
		samplingTimeInput = new DoubleInput( "SamplingTime", "Key Inputs", 1.0d);
		samplingTimeInput.setValidRange( 0.0, Double.POSITIVE_INFINITY);
		samplingTimeInput.setUnits( "h");
		this.addInput( samplingTimeInput, true);
	}

	public Controller() {
		calculationEntityList = new ArrayList<CalculationEntity>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		count = 0;

		// Prepare a list of the calculation entities managed by this controller
		calculationEntityList.clear();
		for (CalculationEntity ent : Simulation.getClonesOf(CalculationEntity.class)) {
			if (ent.getController() == this)
				calculationEntityList.add(ent);
		}

		// Sort the calculation entities into the correct sequence
        Collections.sort( calculationEntityList, new Comparator<CalculationEntity>(){

			@Override
			public int compare( CalculationEntity ent1, CalculationEntity ent2) {
               return (int)( ent1.getSequenceNumber() -  ent2.getSequenceNumber() );
            }
        });
	}

	@Override
	public void startUp() {
		super.startUp();

		//Loop infinitely over the calculation entities
		while( true ) {

			// Update the last value for each entity
			for( CalculationEntity ent : calculationEntityList ) {
				ent.update();
			}

			// Increment the number of cycles
			count++;

			// Wait for the samplingTime
			this.scheduleWait( samplingTimeInput.getValue() );
		}
	}

	public int getCount() {
		return count;
	}
}
