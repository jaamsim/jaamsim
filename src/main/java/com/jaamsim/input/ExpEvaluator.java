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

import com.jaamsim.units.Unit;
import com.sandwell.JavaSimulation.Entity;

/**
 * Utility class to bridge the expression parser and attribute assignment
 * @author Matt Chudleigh
 *
 */
public class ExpEvaluator {

	public static class Error extends Exception {
		Error(String err) {
			super(err);
		}
	}

	private static Entity getEntity(String[] names, double simTime, Entity thisEnt, Entity objEnt) throws Error {

		Entity ent;
		if (names[0] == "this")
			ent = thisEnt;
		else if (names[0] == "obj")
			ent = objEnt;
		else
			ent = Entity.getNamedEntity(names[0]);

		if (ent == null) {
			throw new Error(String.format("Could not find entity: %s", names[0]));
		}
		// Run the output chain up to the second last name
		for(int i = 1; i < names.length-1; ++i) {
			String outputName = names[i];
			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new Error(String.format("Output '%s' not found on entity '%s'", outputName, ent.getInputName()));
			}
			if (!Entity.class.isAssignableFrom(oh.getReturnType())) {
				throw new Error(String.format("Output '%s' is not an entity output", outputName));
			}

			ent = oh.getValue(simTime, Entity.class);
		}
		return ent;
	}

	private static class EntityContext implements ExpParser.ParseContext {

		public String errorString; // Used to mark an error during lookup because 'throws' screws with the interface

		// These are updated in updateContext() which must be called before any expression are evaluated
		private double simTime;
		private Entity thisEnt;
		private Entity objEnt;

		public void updateContext(double simTime, Entity thisEnt, Entity objEnt) {
			this.simTime = simTime;
			this.thisEnt = thisEnt;
			this.objEnt = objEnt;
		}

		@Override
		public ExpResult getVariableValue(String[] names) {
			try {
				Entity ent = getEntity(names, simTime, thisEnt, objEnt);

				String outputName = names[names.length-1];
				OutputHandle oh = ent.getOutputHandleInterned(outputName);
				if (oh == null) {
					errorString = String.format("Could not find output '%s' on entity '%s'", outputName, ent.getInputName());
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(oh.getValueAsDouble(simTime, 0), oh.unitType);

			} catch (Exception e) {
				errorString = e.getMessage();
			}
			return ExpResult.BAD_RESULT;
		}

		@Override
		public ExpParser.UnitData getUnitByName(String name) {
			Unit unit = Input.tryParseEntity(name, Unit.class);
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

	private static EntityContext EC = new EntityContext();

	public static ExpParser.ParseContext getContext() {
		return EC;
	}

	public static void runAssignment(ExpParser.Assignment assign, double simTime, Entity thisEnt, Entity objEnt) throws Error {
		Entity assignmentEnt = getEntity(assign.destination, simTime, thisEnt, objEnt);

		ExpResult result = evaluateExpression(assign.value, simTime, thisEnt, objEnt);

		String attribName = assign.destination[assign.destination.length-1];
		if (!assignmentEnt.hasAttribute(attribName)) {
			throw new Error(String.format("Entity '%s' does not have attribute '%s'", assignmentEnt, attribName));
		}
		assignmentEnt.setAttribute(attribName, result.value);
	}

	public static ExpResult evaluateExpression(ExpParser.Expression exp, double simTime, Entity thisEnt, Entity objEnt) throws Error
	{
		EC.errorString = null;
		EC.updateContext(simTime, thisEnt, objEnt);

		ExpResult value = exp.evaluate();
		if (EC.errorString != null)
			throw new Error(EC.errorString);

		return value;
	}
}
