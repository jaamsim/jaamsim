package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;

public class InOutHandle extends OutputHandle {

	private final Input<?> in;
	private final String name;
	private final Class<?> returnType;
	private final Class<? extends Entity> entClass;

	public InOutHandle(Entity ent, Input<?> in) {
		super(ent);
		this.in = in;
		this.name = in.getKeyword();

		this.returnType = in.getDefaultValue().getClass();
		assert(this.returnType != null);

		this.entClass = ent.getClass();
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
	public boolean isReportable() {
		return true;
	}

	@Override
	public int getSequence() {
		return Integer.MAX_VALUE-1;
	}
	@Override
	public boolean canCache() {
		return true;
	}

}

