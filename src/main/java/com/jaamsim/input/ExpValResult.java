package com.jaamsim.input;

import java.util.ArrayList;

import com.jaamsim.units.Unit;


public class ExpValResult {

	public static enum State {
		VALID, ERROR, UNDECIDABLE
	}

	public State state;
	public ArrayList<ExpError> errors;

	public Class<? extends Unit> unitType;

	public ExpValResult(State s, Class<? extends Unit> ut, ExpError error) {
		state = s;
		unitType = ut;
		errors = new ArrayList<>();
		if (error != null) {
			errors.add(error);
		}
	}

	public ExpValResult(State s, Class<? extends Unit> ut, ArrayList<ExpError> es) {
		state = s;
		unitType = ut;
		errors = es;
	}
}
