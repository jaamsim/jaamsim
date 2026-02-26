package com.jaamsim.units;

public class ForceUnit extends Unit {
	static {
		Unit.setSIUnit(ForceUnit.class, "N");
	}

	public ForceUnit() {}

}
