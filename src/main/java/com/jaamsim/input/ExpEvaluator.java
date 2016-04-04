/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 KMA Technologies
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

import java.util.ArrayList;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.ExpParser.EvalContext;
import com.jaamsim.input.ExpParser.VarResolver;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * Utility class to bridge the expression parser and attribute assignment
 * @author Matt Chudleigh
 *
 */
public class ExpEvaluator {

	private static Entity getEntity(Entity rootEnt, String[] names, ExpResult[] indices, double simTime) throws ExpError {

		Entity ent = rootEnt;

		// Run the output chain up to the second last name
		for(int i = 1; i < names.length-1; ++i) {
			String outputName = names[i];
			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Output '%s' not found on entity '%s'", outputName, ent.getName());
			}
			if (indices != null && indices[i] != null) {
				// This output has a defined index, for now we only support array lists for indexed outputs
				if (!ArrayList.class.isAssignableFrom(oh.getReturnType())) {
					throw new ExpError(null, 0, "Output '%s' has an index and is not an ArrayList output", outputName);
				}

				int index = (int)indices[i].value -1; // 1 based indexing
				ArrayList<?> outList = oh.getValue(simTime, ArrayList.class);
				if (index >= outList.size() || index < 0) {
					throw new ExpError(null, 0, "Index (%d) is out of bounds for array at output: %s", index + 1, outputName);
				}

				Object val = outList.get(index);
				if (!(val instanceof Entity)) {
					throw new ExpError(null, 0, "Non entity type from output: %s index: %d",outputName,  index + 1);
				}
				ent = (Entity)val;

			} else {
				if (!Entity.class.isAssignableFrom(oh.getReturnType())) {
					throw new ExpError(null, 0, "Output '%s' is not an entity output", outputName);
				}

				ent = oh.getValue(simTime, Entity.class);
			}

			if (ent == null) {
				// Build up the entity chain
				StringBuilder b = new StringBuilder();
				if (names[0].equals("this"))
					b.append("this");
				else
					b.append("[").append(names[0]).append("]");

				for(int j = 1; j <= i; ++j) {
					b.append(".").append(names[j]);
					if (indices != null && indices[j] != null) {
						b.append("(").append((int)indices[j].value).append(")");
					}
				}

				throw new ExpError(null, 0, "Null entity in expression chain: %s", b.toString());
			}
		}
		return ent;
	}

	private static class EntityParseContext implements ExpParser.ParseContext {

		private final Entity thisEnt;

		public EntityParseContext(Entity thisEnt) {
			this.thisEnt = thisEnt;
		}

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


		@Override
		public VarResolver getVarResolver(String[] names, boolean[] hasIndices) throws ExpError {

			if (names.length == 1) {
				throw new ExpError(null, 0, "You must specify an output or attribute for entity: %s", names[0]);
			}

			Entity rootEnt;
			if (names[0] == "this")
				rootEnt = thisEnt;
			else {
				rootEnt = Entity.getNamedEntity(names[0]);
			}

			if (rootEnt == null) {
				throw new ExpError(null, 0, "Could not find entity: %s", names[0]);
			}

			// Special case, if this is a simple output and we can cache the output handle, use an optimized resolver
			if (names.length == 2) {
				OutputHandle oh = rootEnt.getOutputHandleInterned(names[1]);
				if (oh == null) {
					throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", names[1], names[0]);
				}
				if (oh.canCache() && !hasIndices[1]) {
					return new CachedResolver(oh);
				}
			}

			return new EntityResolver(rootEnt, names);
		}

	}

	private static class CachedResolver implements ExpParser.VarResolver {

		private final OutputHandle handle;

		public CachedResolver(OutputHandle oh) {
			handle = oh;
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult[] indices)
				throws ExpError {
			EntityEvalContext eec = (EntityEvalContext)ec;
			double val = handle.getValueAsDouble(eec.simTime, 0);
			return new ExpResult(val, handle.getUnitType());
		}

		@Override
		public ExpValResult validate(boolean[] hasIndices) {
			return new ExpValResult(ExpValResult.State.VALID, handle.getUnitType(), (ExpError)null);
		}
	}

	private static class EntityResolver implements ExpParser.VarResolver {

		private final Entity rootEnt;
		private final String[] names;

		public EntityResolver(Entity rootEnt, String[] names) {
			this.rootEnt = rootEnt;
			this.names = names;
		}

		@Override
		public ExpResult resolve(ExpParser.EvalContext ec, ExpResult[] indices) throws ExpError {

			EntityEvalContext eec = (EntityEvalContext)ec;

			double simTime = eec.simTime;

			Entity ent = getEntity(rootEnt, names, indices, simTime);

			String outputName = names[names.length-1];
			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", outputName, ent.getName());
			}

			if (indices != null && indices[names.length-1] != null) {
				int index = (int)indices[names.length-1].value -1; // 1 based indexing

				if (ArrayList.class.isAssignableFrom(oh.getReturnType())) {
					ArrayList<?> outList = oh.getValue(simTime, ArrayList.class);

					if (index >= outList.size()  || index < 0) {
						return new ExpResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}
					Double value = (Double)outList.get(index);
					return new ExpResult(value, oh.unitType);
				} else if(DoubleVector.class.isAssignableFrom(oh.getReturnType())) {
					DoubleVector outList = oh.getValue(simTime, DoubleVector.class);

					if (index >= outList.size() || index < 0) {
						return new ExpResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}

					Double value = outList.get(index);
					return new ExpResult(value, oh.unitType);
				} else if(IntegerVector.class.isAssignableFrom(oh.getReturnType())) {
					IntegerVector outList = oh.getValue(simTime, IntegerVector.class);

					if (index >= outList.size() || index < 0) {
						return new ExpResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}

					Integer value = outList.get(index);
					return new ExpResult(value, oh.unitType);
				} else {
					throw new ExpError(null, 0, "Output '%s' has an index and is not an array type output", names[names.length-1]);
				}
			} else {
				return new ExpResult(oh.getValueAsDouble(simTime, 0), oh.unitType);
			}

		}

		@Override
		public ExpValResult validate(boolean[] hasIndices) {

			if (names.length == 2) {
				String outputName = names[1];
				OutputHandle oh = rootEnt.getOutputHandleInterned(outputName);
				if (oh == null) {
					ExpError error = new ExpError(null, 0, String.format("Could not find output '%s' on entity '%s'",
							outputName, rootEnt.getName()));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
				if (!hasIndices[1]) {
					Class<?> retType = oh.getReturnType();
					if (    !OutputHandle.isNumericType(retType) &&
					        retType != boolean.class &&
					        retType != Boolean.class) {
						ExpError error = new ExpError(null, 0, "Output: %s does not return a numeric type", names[1]);
						return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
					}
					return new ExpValResult(ExpValResult.State.VALID, oh.unitType, (ExpError)null);
				} else {
					// Indexed final output
					if (oh.getReturnType() == DoubleVector.class ||
						oh.getReturnType() == IntegerVector.class) {
						return new ExpValResult(ExpValResult.State.VALID, oh.unitType, (ExpError)null);
					}
					if (oh.getReturnType() == ArrayList.class) {
						//TODO: find out if we can determine the contained class without an instance or if type erasure prevents that
						return new ExpValResult(ExpValResult.State.VALID, oh.unitType, (ExpError)null);
					}
					ExpError error = new ExpError(null, 0, "Output: %s is not a known array type");
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
			}

			// We have a 'chained' output, so now we must do best effort evaluation
			// The only failures we can detect from here out are unit errors and using an existing output in an invalid
			// way. Typos are not detectable because entities may have outputs from descendant classes or attributes

			Class<?> klass = rootEnt.getClass();
			for (int i = 1; i < names.length; ++i) {
				Class<? extends Unit> unitType = OutputHandle.getStaticOutputUnitType(klass, names[i]);
				klass = OutputHandle.getStaticOutputType(klass, names[i]);
				if (klass == null) {
					// Undecidable
					return new ExpValResult(ExpValResult.State.UNDECIDABLE, DimensionlessUnit.class, (ExpError)null);
				}

				if (i == names.length - 1) {
					// Last name in the chain, check that the type is numeric
					if (!OutputHandle.isNumericType(klass)) {
						ExpError error = new ExpError(null, 0, "Output: '%s' does not return a numeric type", names[i]);
						return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
					}
					//return new ExpResult(0, unitType);
					return new ExpValResult(ExpValResult.State.VALID, unitType, (ExpError)null);
				} else {
					if (!Entity.class.isAssignableFrom(klass)) {
						ExpError error = new ExpError(null, 0, "Output: '%s' must output an entity type", names[i]);
						return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
					}
				}
			}
			// We should never get here
			ExpError error = new ExpError(null, 0, "Validator logic error");
			return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
		}

	}

	private static class EntityEvalContext implements ExpParser.EvalContext {

		// These are updated in updateContext() which must be called before any expression are evaluated
		private final double simTime;
		//private final Entity thisEnt;

		public EntityEvalContext(double simTime) {
			this.simTime = simTime;
			//this.thisEnt = thisEnt;
		}
	}

	public static ExpParser.ParseContext getParseContext(Entity thisEnt) {
		return new EntityParseContext(thisEnt);
	}

	public static void runAssignment(ExpParser.Assignment assign, double simTime, Entity thisEnt) throws ExpError {

		Entity rootEnt;
		if (assign.destination[0] == "this")
			rootEnt = thisEnt;
		else {
			rootEnt = Entity.getNamedEntity(assign.destination[0]);
		}

		if (rootEnt == null) {
			throw new ExpError(null, 0, "Could not find entity: %s", assign.destination[0]);
		}

		Entity assignmentEnt = getEntity(rootEnt, assign.destination, null, simTime);

		ExpResult result = evaluateExpression(assign.value, simTime);

		String attribName = assign.destination[assign.destination.length-1];
		if (!assignmentEnt.hasAttribute(attribName)) {
			throw new ExpError(null, 0, "Entity '%s' does not have attribute '%s'", assignmentEnt, attribName);
		}
		assignmentEnt.setAttribute(attribName, result.value, result.unitType);
	}

	public static ExpResult evaluateExpression(ExpParser.Expression exp, double simTime) throws ExpError
	{
		EntityEvalContext evalContext = new EntityEvalContext(simTime);
		return exp.evaluate(evalContext);
	}
}
