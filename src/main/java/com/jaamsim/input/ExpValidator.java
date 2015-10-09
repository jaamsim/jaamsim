/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * Utility class to validate expressions at parse time
 * @author Matt Chudleigh
 *
 */
public class ExpValidator {

	private static Entity validateEntity(String[] names, Entity thisEnt) throws ExpError {
		if (names.length == 1) {
			throw new ExpError(null, 0, "You must specify an output or attribute for entity: %s", names[0]);
		}
		Entity ent;
		if (names[0] == "this")
			ent = thisEnt;
		else
			ent = Entity.getNamedEntity(names[0]);

		if (ent == null) {
			throw new ExpError(null, 0, "Could not find entity: %s", names[0]);
		}
		return ent;
	}

	private static class EntityValidateContext implements ExpParser.EvalContext {

		private final Entity thisEnt;
		public boolean undecidable = false;

		public EntityValidateContext(Entity thisEnt) {
			this.thisEnt = thisEnt;
		}

		@Override
		public ExpResult getVariableValue(String[] names, ExpResult[] indices) throws ExpError {

			Entity ent = validateEntity(names, thisEnt);

			if (names.length == 2) {
				String outputName = names[1];
				OutputHandle oh = ent.getOutputHandleInterned(outputName);
				if (oh == null) {
					throw new ExpError(null, 0, String.format("Could not find output '%s' on entity '%s'", outputName, ent.getName()));
				}
				Class<?> retType = oh.getReturnType();
				if (    !OutputHandle.isNumericType(retType) &&
				        retType != boolean.class &&
				        retType != Boolean.class) {
					throw new ExpError(null, 0, "Output: %s does not return a numeric type", names[1]);
				}
				return new ExpResult(0, oh.unitType);
			}

			// We have a 'chained' output, so now we must do best effort evaluation
			// The only failures we can detect from here out are unit errors and using an existing output in an invalid
			// way. Typos are not detectable because entities may have outputs from descendant classes or attributes

			Class<?> klass = ent.getClass();
			for (int i = 1; i < names.length; ++i) {
				Class<? extends Unit> unitType = OutputHandle.getStaticOutputUnitType(klass, names[i]);
				klass = OutputHandle.getStaticOutputType(klass, names[i]);
				if (klass == null) {
					// Undecidable
					undecidable = true;
					return new ExpResult(0, DimensionlessUnit.class);
				}

				if (i == names.length - 1) {
					// Last name in the chain, check that the type is numeric
					if (!OutputHandle.isNumericType(klass)) {
						throw new ExpError(null, 0, "Output: '%s' does not return a numeric type", names[i]);
					}
					return new ExpResult(0, unitType);
				} else {
					if (!Entity.class.isAssignableFrom(klass)) {
						throw new ExpError(null, 0, "Output: '%s' must output an entity type", names[i]);
					}
				}
			}
			// We should never get here
			throw new ExpError(null, 0, "Validator logic error");
		}

		@Override
		public boolean eagerEval() {
			return true;
		}
	}

	public static void validateAssignment(ExpParser.Assignment assign, Entity thisEnt) throws ExpError {
		String[] dest = assign.destination;
		Entity ent = validateEntity(dest, thisEnt);
		Class<? extends Unit> unitType = null;

		if (dest.length == 2) {
			if (!ent.hasAttribute(dest[1])) {
				throw new ExpError(null, 0, String.format("Could not find attribute '%s' on entity '%s'", dest[1], dest[0]));
			}
			unitType = ent.getAttributeUnitType(dest[1]);
		}
		// Otherwise we do not validate assignment destination yet

		validateExpression(assign.value, thisEnt, unitType);
	}

	// Validate an expression, if the expression is invalid, this will throw an ExpError detailing the problem
	public static void validateExpression(ExpParser.Expression exp, Entity thisEnt, Class<? extends Unit> ut) throws ExpError
	{
		EntityValidateContext valContext = new EntityValidateContext(thisEnt);
		try {
			ExpResult res = exp.evaluate(valContext);
			if (!valContext.undecidable && ut != null && res.unitType != ut)
				throw new InputErrorException("Expression returned an invalid unit for this input. Received: %s, expected: %s",
						res.unitType.getSimpleName(), ut.getSimpleName());
		}
		catch (ExpError ex) {
			if (valContext.undecidable) {
				// We got an error, but can not be sure it is a validation problem
				return;
			}
			throw ex;
		}
	}
}
