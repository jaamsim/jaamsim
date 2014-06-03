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
		if (names[0].equals("this"))
			ent = thisEnt;
		else if (names[0].equals("obj"))
			ent = objEnt;
		else
			ent = Entity.getNamedEntity(names[0]);

		if (ent == null) {
			throw new Error(String.format("Could not find entity: %s", names[0]));
		}
		// Run the output chain up to the second last name
		for(int i = 1; i < names.length-1; ++i) {
			String outputName = names[i];
			OutputHandle oh = ent.getOutputHandle(outputName);
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

	private static class EntityLookup implements ExpParser.VarTable {

		public String errorString; // Used to mark an error during lookup because 'throws' screws with the interface
		private double simTime;
		private Entity thisEnt;
		private Entity objEnt;

		public EntityLookup(double simTime, Entity thisEnt, Entity objEnt) {
			this.simTime = simTime;
			this.thisEnt = thisEnt;
			this.objEnt = objEnt;
		}

		@Override
		public ExpResult getVariableValue(String[] names) {
			try {
				errorString = null;
				Entity ent = getEntity(names, simTime, thisEnt, objEnt);
				String outputName = names[names.length-1];
				OutputHandle oh = ent.getOutputHandle(outputName);
				if (oh == null) {
					errorString = String.format("Could not find output '%s' on entity '%s'", outputName, ent.getInputName());
					return new ExpResult(Double.NaN);
				}
				return new ExpResult(oh.getValueAsDouble(simTime, 0));

			} catch (Exception e) {
				errorString = e.getMessage();
			}
			return new ExpResult(Double.NaN);
		}
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
		EntityLookup el = new EntityLookup(simTime, thisEnt, objEnt);

		ExpResult value = exp.evaluate(el);
		if (el.errorString != null)
			throw new Error(el.errorString);

		return value;
	}
}
