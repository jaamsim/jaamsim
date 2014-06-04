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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.input.Keyword;
import com.jaamsim.input.Output;
import com.jaamsim.input.ValueListInput;
import com.jaamsim.units.DimensionlessUnit;
import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation.EntityListInput;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation3D.DisplayEntity;
/**
 * ObjectSelector is the super-class for model components that select an entity from a list.
 * @author Harry King
 *
 */
public abstract class ObjectSelector extends DisplayEntity {

	@Keyword(description = "The list of entities from which to select.",
	         example = "ObjectDist1 EntityList { Ent1  Ent2 }")
	private final EntityListInput<DisplayEntity> entityListInput;

	@Keyword(description = "The list of probabilities corresponding to the entities in the EntityList.  Must sum to 1.0.",
	         example = "ObjectDist1 ProbabilityList { 0.3  0.7 }")
	private final ValueListInput probabilityListInput;

	private int presentIndex;  // the index for entity that has been selected at present
	private int totalCount;  // the total number of samples that have been selected
	private int[] sampleCount;  // number of times each entity has been selected
	private double[] sampleDifference;  // (actual number of samples) - (expected number)

	{
		entityListInput = new EntityListInput<DisplayEntity>( DisplayEntity.class, "EntityList", "Key Inputs", null);
		this.addInput( entityListInput);

		probabilityListInput = new ValueListInput( "ProbabilityList", "Key Inputs", null);
		probabilityListInput.setUnitType(DimensionlessUnit.class);
		this.addInput( probabilityListInput);
	}

	@Override
	public void validate() {
		super.validate();

		// The EntityList must be specified
		if( entityListInput.getValue().size() == 0 ) {
			throw new InputErrorException( "The EntityList input must not be empty.");
		}

		// The number of entries in the EntityList and ProbabilityList inputs must match
		if( probabilityListInput.getValue().size() != entityListInput.getValue().size() ) {
			throw new InputErrorException( "The number of entries for ProbabilityList and EntityList must be equal" );
		}

		// The entries in the ProbabilityList must sum to 1.0
		if( Math.abs( probabilityListInput.getValue().sum() - 1.0 ) > 1.0e-10 ) {
			throw new InputErrorException( "The entries in the ProbabilityList must sum to 1.0" );
		}
	}

	@Override
	public void earlyInit() {
		super.earlyInit();

		presentIndex = -1;
		totalCount = 0;
		sampleCount = new int[ entityListInput.getValue().size() ];
		sampleDifference = new double[ entityListInput.getValue().size() ];
	}

	/**
	 * Set the index of the next sample.
	 */
	private void setNextSample() {

		// Sample a non-zero value from the distribution
		presentIndex = this.getNextIndex();

		// Collect statistics on the sampled values
		totalCount++;
		sampleCount[presentIndex]++;
		for(int i=0; i<sampleCount.length; i++) {
			sampleDifference[i] = sampleCount[i] - totalCount*probabilityListInput.getValue().get(i);
		}
	}

	/**
	 * Select the index of the next sample.
	 */
	protected abstract int getNextIndex();

	/**
	 * Return the present sample.
	 */
	public DisplayEntity getValue() {
		return entityListInput.getValue().get(presentIndex);
	}

	/**
	 * Returns the next sample from the probability distribution.
	 */
	public DisplayEntity nextValue() {
		this.setNextSample();
		return ( this.getValue() );
	}

	public int getNumberOfSamples() {
		return totalCount;
	}

	public int getSampleCount( int i ) {
		return sampleCount[i];
	}

	public DoubleVector getProbabilityList() {
		return probabilityListInput.getValue();
	}

	@Output( name="NumberOfSamples",
			 description="The number of times the distribution has been sampled.")
	public int getNumberOfSamples( double simTime ) {
		return totalCount;
	}

	@Output( name="SampleCount",
			 description="The number samples for each entity.")
	public int[] getSampleCount( double simTime ) {
		return sampleCount;
	}

	@Output( name="SampleDifference",
			 description="The difference between the actual number samples for each entity and the expected number.")
	public double[] getSampleDifference( double simTime ) {
		return sampleDifference;
	}
}
