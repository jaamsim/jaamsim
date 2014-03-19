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

import com.jaamsim.input.Keyword;
import com.sandwell.JavaSimulation.IntegerInput;

/**
 * The MovingAverage block returns the average of the current input and the N-1 previous inputs.
 * Output(i) = 1/N * [ Input(i) + Input(i-1) + ... + Input(i-N+1) ]
 * where Input(i) = input value to the block for the i-th update,
 * and Output(i) = output value from the block for the i-th update.
 * @author Harry King
 *
 */
public class MovingAverage extends DoubleCalculation {

	@Keyword(description = "The number of input values over which to average.",
	         example = "MovingAverage-1 NumberOfSamples { 10 }")
	private final IntegerInput numberOfSamples;

	private double[] samples;  // The previous input values over which to average
	private int index;  // The next index to overwrite (the oldest value on the list)
	private int n;  // The number of inputs values over which to average
	private double average;  // The present value for the moving average

	{
		controllerRequired = true;

		numberOfSamples = new IntegerInput( "NumberOfSamples", "Key Inputs", 1);
		numberOfSamples.setValidRange( 1, Integer.MAX_VALUE);
		this.addInput( numberOfSamples);
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		samples = new double[ numberOfSamples.getValue() ];
		index = 0;
		n = numberOfSamples.getValue();
		average = 0.0;
	}

	@Override
	public double calculateValue(double simTime) {
		return average;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);

		// Overwrite the oldest value in the list
		samples[index] = this.getInputValue(simTime);

		// Set the index to the next oldest value
		index++;
		if( index >= n ) {
			index = 0;
		}

		// Calculate the average value
		double val = 0.0;
		for( int i=0; i<n; i++) {
			val += samples[i];
		}
		average = val/n;
		return;
	}

}
