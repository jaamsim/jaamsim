package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * Pareto Distribution
 * The function was translated so that it starts from 0.
 * adapted from: http://mathworld.wolfram.com/ParetoDistribution.html
 * From the link link:
 * 		a == Shape
 * 		b == Scale
 * @author Michael Bergman
 */
public class ParetoDistribution extends Distribution {


	@Keyword(description = "The shape parameter for the Pareto distribution.  A decimal value > 0.0.",
	         exampleList = {"5.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput scaleInput;


	@Keyword(description = "The shape parameter for the Pareto distribution.  A decimal value > 0.0.",
	         exampleList = {"2.0", "InputValue1", "'2 * [InputValue1].Value'"})
	private final SampleInput shapeInput;


	private final MRG1999a rng = new MRG1999a();

	{

		minValueInput.setDefaultValue(new SampleConstant(0.0d));
		minValueInput.setValidRange(0.0, Double.POSITIVE_INFINITY);

		scaleInput = new SampleInput("Mean", "Key Inputs", new SampleConstant(1.0d));
		scaleInput.setUnitType(UserSpecifiedUnit.class);
		scaleInput.setValidRange( 1.0e-10d, Double.POSITIVE_INFINITY );
		scaleInput.setEntity(this);
		this.addInput(scaleInput);

		shapeInput = new SampleInput("Shape", "Key Inputs", new SampleConstant(1.0d));
		shapeInput.setUnitType(DimensionlessUnit.class);
		shapeInput.setValidRange( 1.0e-10d, Double.POSITIVE_INFINITY );
		shapeInput.setEntity(this);
		this.addInput(shapeInput);

	}

	public ParetoDistribution() { }

	@Override
	public void earlyInit() {
		super.earlyInit();
		rng.setSeedStream(getStreamNumber(), getSubstreamNumber());
	}

	@Override
	protected void setUnitType(Class<? extends Unit> specified) {
		super.setUnitType(specified);
		scaleInput.setUnitType(specified);
	}


	@Override
	protected double getSample(double simTime) {
		// Inverse transform method
		double scale = scaleInput.getValue().getNextSample(simTime);
		double shape = shapeInput.getValue().getNextSample(simTime);
		return  scale / Math.pow(1 - rng.nextUniform(), 1 / shape) - scale;
	}


	@Override
	protected double getMean(double simTime) {

		double scale = scaleInput.getValue().getNextSample(simTime);
		double shape = shapeInput.getValue().getNextSample(simTime);

		if (shape <= 1.0)	return Double.POSITIVE_INFINITY;

		return scale / (shape - 1);
	}

	@Override
	protected double getStandardDev(double simTime) {

		double shape = shapeInput.getValue().getNextSample(simTime);

		if 		(shape <  1)	return Double.NaN;
		else if (shape <= 2)	return Double.POSITIVE_INFINITY;

		double scale = scaleInput.getValue().getNextSample(simTime);
		double ret = scale*scale*shape / ( (shape-2) * (shape-1) * (shape-1) );
		return Math.sqrt(ret);

	}


}
