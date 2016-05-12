package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;


public class ExpValResult {

	public static enum State {
		VALID, ERROR, UNDECIDABLE
	}

	public final State state;
	public final ArrayList<ExpError> errors;

	public final Class<? extends Unit> unitType;

	public static ExpValResult makeValidRes(Class<? extends Unit> ut)
	{
		return new ExpValResult(State.VALID, ut, null);
	}

	public static ExpValResult makeUndecidableRes()
	{
		return new ExpValResult(State.UNDECIDABLE, DimensionlessUnit.class, null);
	}

	public static ExpValResult makeErrorRes(ArrayList<ExpError> es) {
		return new ExpValResult(State.ERROR, DimensionlessUnit.class, es);
	}

	public static ExpValResult makeErrorRes(ExpError error) {
		ArrayList<ExpError> es = new ArrayList<>(1);
		return new ExpValResult(State.ERROR, DimensionlessUnit.class, es);
	}

	private ExpValResult(State s, Class<? extends Unit> ut, ArrayList<ExpError> es) {
		state = s;
		unitType = ut;

		if (es == null)
			errors = new ArrayList<>();
		else
			errors = es;
	}
}
