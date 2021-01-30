package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.Unit;

public class InOutHandle extends ValueHandle {

	private final Input<?> in;
	private final String name;
	private final Class<?> returnType;
	private final Class<? extends Entity> entClass;
	private final Class<? extends Unit> unitType;
	private final int sequence;

	public InOutHandle(Entity ent, Input<?> in, String name, int seq, Class<? extends Unit> ut) {
		super(ent);
		this.in = in;
		this.name = name;

		this.returnType = in.getDefaultValue().getClass();
		assert(this.returnType != null);

		this.entClass = ent.getClass();
		this.sequence = seq;
		this.unitType = ut;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <V> V getValue(double simTime, Class<V> klass) {
		// The cast should be safe, but the type checker does not know that
		return (V) in.getValue();
	}

	@Override
	public Class<?> getReturnType() {
		return returnType;
	}

	@Override
	public Class<? extends Entity> getDeclaringClass() {
		return entClass;
	}

	@Override
	public String getDescription() {
		return String.format("The input value for input: %s", name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	@Override
	public boolean isReportable() {
		return true;
	}

	@Override
	public int getSequence() {
		return sequence;
	}
	@Override
	public boolean canCache() {
		return true;
	}

	/**
	 * Checks the output for all possible numerical types and returns a double representing the value
	 * @param simTime
	 * @param def - the default value if the return is null or not a number value
	 */
	@Override
	public double getValueAsDouble(double simTime, double def) {
		Class<?> retType = this.getReturnType();

		if (retType == double.class)
			return this.getValue(simTime, double.class);

		if (retType == int.class)
			return this.getValue(simTime, int.class).doubleValue();

		if (retType == boolean.class)
			return this.getValue(simTime, boolean.class) ? 1.0d : 0.0d;

		if (retType == float.class)
			return this.getValue(simTime, float.class).doubleValue();

		if (retType == long.class)
			return this.getValue(simTime, long.class).doubleValue();

		if (retType == short.class)
			return this.getValue(simTime, short.class).doubleValue();

		if (retType == char.class)
			return this.getValue(simTime, char.class).charValue();

		if (retType == Double.class) {
			Double val = getValue(simTime, Double.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Integer.class) {
			Integer val = getValue(simTime, Integer.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Boolean.class) {
			Boolean val = getValue(simTime, Boolean.class);
			if (val == null) return def;
			return val.booleanValue() ? 1.0d : 0.0d;
		}
		if (retType == Float.class) {
			Float val = getValue(simTime, Float.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Long.class) {
			Long val = getValue(simTime, Long.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Short.class) {
			Short val = getValue(simTime, Short.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Character.class) {
			Character val = getValue(simTime, Character.class);
			if (val == null) return def;
			return val.charValue();
		}

		return def;
	}
}

