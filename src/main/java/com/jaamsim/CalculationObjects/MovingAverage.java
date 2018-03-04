/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
package com.jaamsim.CalculationObjects;

import com.jaamsim.input.IntegerInput;
import com.jaamsim.input.Keyword;

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
	         exampleList = {"10"})
	private final IntegerInput numberOfSamples;

	private double[] samples;  // The previous input values over which to average
	private int index;  // The next index to overwrite (the oldest value on the list)
	private int n;  // The number of inputs values over which to average
	private double average;  // The present value for the moving average

	{
		numberOfSamples = new IntegerInput("NumberOfSamples", KEY_INPUTS, 1);
		numberOfSamples.setValidRange(1, Integer.MAX_VALUE);
		this.addInput(numberOfSamples);
	}

	public MovingAverage() {
		samples = new double[1];
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		samples = new double[numberOfSamples.getValue()];
		index = 0;
		n = numberOfSamples.getValue();
		average = 0.0;
	}

	@Override
	public double calculateValue(double simTime, double inputVal, double lastTime, double lastInputVal, double lastVal) {
		return average + (inputVal - samples[index])/n;
	}

	@Override
	public void update(double simTime) {
		super.update(simTime);

		// Overwrite the oldest value in the list
		samples[index] = this.getInputValue(simTime);

		// Set the index to the next oldest value
		index++;
		if (index >= n) {
			index = 0;
		}

		// Calculate the average value
		double val = 0.0;
		for (int i=0; i<n; i++) {
			val += samples[i];
		}
		average = val/n;
		return;
	}

}
