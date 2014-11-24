/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.Unit;

/**
 * Utility class to bridge the expression parser and attribute assignment
 * @author Matt Chudleigh
 *
 */
public class ExpEvaluator {

	private static Entity getEntity(String[] names, double simTime, Entity thisEnt) throws ExpError {

		Entity ent;
		if (names[0] == "this")
			ent = thisEnt;
		else
			ent = Entity.getNamedEntity(names[0]);

		if (ent == null) {
			throw new ExpError(null, 0, "Could not find entity: %s", names[0]);
		}
		// Run the output chain up to the second last name
		for(int i = 1; i < names.length-1; ++i) {
			String outputName = names[i];
			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Output '%s' not found on entity '%s'", outputName, ent.getName());
			}
			if (!Entity.class.isAssignableFrom(oh.getReturnType())) {
				throw new ExpError(null, 0, "Output '%s' is not an entity output", outputName);
			}

			ent = oh.getValue(simTime, Entity.class);

			if (ent == null) {
				// Build up the entity chain
				StringBuilder b = new StringBuilder();
				if (names[0].equals("this"))
					b.append("this");
				else
					b.append("[").append(names[0]).append("]");

				for(int j = 1; j <= i; ++j) {
					b.append(".").append(names[j]);
				}

				throw new ExpError(null, 0, "Null entity in expression chain: %s", b.toString());
			}
		}
		return ent;
	}

	private static class EntityParseContext implements ExpParser.ParseContext {

		@Override
		public ExpParser.UnitData getUnitByName(String name) {
			Unit unit = Input.tryParseUnit(name, Unit.class);
			if (unit == null) {
				return null;
			}

			ExpParser.UnitData ret = new ExpParser.UnitData();
			ret.scaleFactor = unit.getConversionFactorToSI();
			ret.unitType = unit.getClass();

			return ret;
		}

		@Override
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a,
				Class<? extends Unit> b) {
			return Unit.getMultUnitType(a, b);
		}

		@Override
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num,
				Class<? extends Unit> denom) {
			return Unit.getDivUnitType(num, denom);
		}
	}

	private static EntityParseContext EC = new EntityParseContext();

	private static class EntityEvalContext implements ExpParser.EvalContext {

		// These are updated in updateContext() which must be called before any expression are evaluated
		private double simTime;
		private Entity thisEnt;

		public EntityEvalContext(double simTime, Entity thisEnt) {
			this.simTime = simTime;
			this.thisEnt = thisEnt;
		}

		@Override
		public ExpResult getVariableValue(String[] names) throws ExpError {
			Entity ent = getEntity(names, simTime, thisEnt);

			String outputName = names[names.length-1];
			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", outputName, ent.getName());
			}
			return new ExpResult(oh.getValueAsDouble(simTime, 0), oh.unitType);

		}

		@Override
		public boolean eagerEval() {
			return false;
		}
	}
	public static ExpParser.ParseContext getParseContext() {
		return EC;
	}

	public static void runAssignment(ExpParser.Assignment assign, double simTime, Entity thisEnt) throws ExpError {
		Entity assignmentEnt = getEntity(assign.destination, simTime, thisEnt);

		ExpResult result = evaluateExpression(assign.value, simTime, thisEnt);

		String attribName = assign.destination[assign.destination.length-1];
		if (!assignmentEnt.hasAttribute(attribName)) {
			throw new ExpError(null, 0, "Entity '%s' does not have attribute '%s'", assignmentEnt, attribName);
		}
		assignmentEnt.setAttribute(attribName, result.value);
	}

	public static ExpResult evaluateExpression(ExpParser.Expression exp, double simTime, Entity thisEnt) throws ExpError
	{
		EntityEvalContext evalContext = new EntityEvalContext(simTime, thisEnt);
		return exp.evaluate(evalContext);
	}
}
