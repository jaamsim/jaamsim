/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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
import java.util.HashMap;
import java.util.Map;

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

	public static class EntityParseContext implements ExpParser.ParseContext {

		private final String source;
		private final Entity thisEnt;

		private final HashMap<Entity, String> entityReferences = new HashMap<>();

		private void addEntityReference(Entity ent) {
			entityReferences.put(ent, ent.getName());
		}

		// Return a version of the expression string updated for an entities that have changed their names
		// since the expression was parsed
		public String getUpdatedSource() {
			String ret = source;
			for (Map.Entry<Entity, String> entEntry : entityReferences.entrySet()) {
				Entity ent = entEntry.getKey();
				String oldName = entEntry.getValue();
				if (ent.getName() != null && ent.getName().equals(oldName)) {
					// This name did not change
					continue;
				}
				String newName = ent.getName();
				if (ent.getName() == null) {
					// An entity with a null name means the entity has been deleted
					newName = "**DeletedEntity**";
				}
				ret = ret.replace("["+oldName+"]", "["+newName+"]");
			}
			return ret;
		}

		public EntityParseContext(Entity thisEnt, String source) {
			this.thisEnt = thisEnt;
			this.source = source;
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

			addEntityReference(unit);
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

			assert(names.length != 0);
			assert(!hasIndices[0]);

			Entity rootEnt;
			if (names[0] == "this")
				rootEnt = thisEnt;
			else {
				rootEnt = Entity.getNamedEntity(names[0]);
				if (rootEnt != null) {
					addEntityReference(rootEnt);
				}
			}

			if (rootEnt == null) {
				throw new ExpError(null, 0, "Could not find entity: %s", names[0]);
			}

			// The directly named case
			if (names.length == 1) {
				return new DirectResolver(rootEnt);
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

		@Override
		public void validateAssignmentDest(String[] destination) throws ExpError {
			Entity rootEnt;
			if (destination[0] == "this")
				rootEnt = thisEnt;
			else {
				rootEnt = Entity.getNamedEntity(destination[0]);
				if (rootEnt != null)
					addEntityReference(rootEnt);
			}

			if (rootEnt == null) {
				throw new ExpError(null, 0, "Could not find entity: %s", destination[0]);
			}
		}

	}

	// DirectResolver are for direct access to entities
	private static class DirectResolver implements ExpParser.VarResolver {

		Entity res;
		public DirectResolver(Entity ent) {
			res = ent;
		}
		@Override
		public ExpResult resolve(EvalContext ec, ExpResult[] indices)
				throws ExpError {
			return ExpResult.makeEntityResult(res);
		}

		@Override
		public ExpValResult validate(boolean[] hasIndices) {
			// This should already be checked to not have indices

			return ExpValResult.makeValidRes(ExpResType.ENTITY, DimensionlessUnit.class);
		}

	}

	private static class CachedResolver implements ExpParser.VarResolver {

		private final OutputHandle handle;
		private final ExpResType type;

		public CachedResolver(OutputHandle oh) throws ExpError {
			handle = oh;
			Class<?> retType = oh.getReturnType();
			if (retType == String.class) {
				type = ExpResType.STRING;
			} else if (Entity.class.isAssignableFrom(retType)){
				type = ExpResType.ENTITY;
			} else if (OutputHandle.isNumericType(retType) ||
			           retType == boolean.class ||
			           retType == Boolean.class) {
				type = ExpResType.NUMBER;
			} else {
				throw new ExpError(null, 0, "Output '%s' on entity '%s does not return a type compatible with the expression engine'",
						oh.getName(), oh.ent.getName());
			}
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult[] indices)
				throws ExpError {
			EntityEvalContext eec = (EntityEvalContext)ec;
			switch (type) {
			case NUMBER:
				double val = handle.getValueAsDouble(eec.simTime, 0);
				return ExpResult.makeNumResult(val, handle.getUnitType());
			case ENTITY:
				return ExpResult.makeEntityResult(handle.getValue(eec.simTime, Entity.class));
			case STRING:
				return ExpResult.makeStringResult(handle.getValue(eec.simTime, String.class));
			default:
				assert(false);
				return ExpResult.makeNumResult(handle.getValueAsDouble(eec.simTime, 0), handle.getUnitType());
			}
		}

		@Override
		public ExpValResult validate(boolean[] hasIndices) {
			switch (type) {
			case NUMBER:
				return ExpValResult.makeValidRes(ExpResType.NUMBER, handle.getUnitType());
			case ENTITY:
				return ExpValResult.makeValidRes(ExpResType.ENTITY, DimensionlessUnit.class);
			case STRING:
				return ExpValResult.makeValidRes(ExpResType.STRING, DimensionlessUnit.class);
			default:
				// This should not be reachable
				assert(false);
				return ExpValResult.makeUndecidableRes();
			}
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
				ExpResult indResult = indices[names.length-1];

				if (Map.class.isAssignableFrom(oh.getReturnType())) {
					// This is a map class, let's just try to index it and see what happens
					Map<?,?> map = oh.getValue(simTime, Map.class);
					Object key;
					switch (indResult.type) {
					case ENTITY:
						key = indResult.entVal;
						break;
					case NUMBER:
						key = new Double(indResult.value);
						break;
					case STRING:
						key = indResult.stringVal;
						break;
					default:
						assert(false);
						key = null;
						break;
					}
					Object val = map.get(key);
					if (val == null) {
						throw new ExpError(null, 0, "Empty result indexing output: '%s'", names[names.length-1]);
					}
					// Try to cast this back into something we understand
					if (String.class.isAssignableFrom(val.getClass())) {
						return ExpResult.makeStringResult((String)val);
					}
					if (Entity.class.isAssignableFrom(val.getClass())) {
						return ExpResult.makeEntityResult((Entity)val);
					}
					if (Double.class.isAssignableFrom(val.getClass())) {
						return ExpResult.makeNumResult((Double)val, oh.unitType);
					}
					throw new ExpError(null, 0, "Output '%s' returned an unknown type: %s", names[names.length-1], val.getClass().getSimpleName());
				}

				if (ArrayList.class.isAssignableFrom(oh.getReturnType())) {
					if (indResult.type != ExpResType.NUMBER) {
						throw new ExpError(null, 0, "Output '%s' is not being indexed by a number", names[names.length-1]);
					}
					ArrayList<?> outList = oh.getValue(simTime, ArrayList.class);

					if (index >= outList.size()  || index < 0) {
						return ExpResult.makeNumResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}
					Double value = (Double)outList.get(index);
					return ExpResult.makeNumResult(value, oh.unitType);
				} else if(DoubleVector.class.isAssignableFrom(oh.getReturnType())) {
					if (indResult.type != ExpResType.NUMBER) {
						throw new ExpError(null, 0, "Output '%s' is not being indexed by a number", names[names.length-1]);
					}
					DoubleVector outList = oh.getValue(simTime, DoubleVector.class);

					if (index >= outList.size() || index < 0) {
						return ExpResult.makeNumResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}

					Double value = outList.get(index);
					return ExpResult.makeNumResult(value, oh.unitType);
				} else if(IntegerVector.class.isAssignableFrom(oh.getReturnType())) {
					if (indResult.type != ExpResType.NUMBER) {
						throw new ExpError(null, 0, "Output '%s' is not being indexed by a number", names[names.length-1]);
					}
					IntegerVector outList = oh.getValue(simTime, IntegerVector.class);

					if (index >= outList.size() || index < 0) {
						return ExpResult.makeNumResult(0, oh.unitType); // TODO: Is this how we want to handle this case?
					}

					Integer value = outList.get(index);
					return ExpResult.makeNumResult(value, oh.unitType);
				} else {
					throw new ExpError(null, 0, "Output '%s' has an index and is not an array type output", names[names.length-1]);
				}
			} else {
				Class<?> retType = oh.getReturnType();
				if (retType == ExpResult.class) {
					// This is already an expression, so return it
					return oh.getValue(simTime, ExpResult.class);
				}
				if (retType == String.class) {
					return ExpResult.makeStringResult(oh.getValue(simTime, String.class));
				}
				if (Entity.class.isAssignableFrom(retType)) {
					return ExpResult.makeEntityResult(oh.getValue(simTime, Entity.class));
				}
				if (    OutputHandle.isNumericType(retType) ||
				        retType == boolean.class ||
				        retType == Boolean.class) {
					return ExpResult.makeNumResult(oh.getValueAsDouble(simTime, 0), oh.unitType);
				}
				throw new ExpError(null, 0, "Output %s, on entity %s does not return a type compatible with expressions.", oh.getName(), oh.ent.getName());
			}

		}

		private ExpValResult validateFinalClass(Class<?> retType, Class<? extends Unit> unitType) {
			if (    OutputHandle.isNumericType(retType) ||
			        retType != boolean.class ||
			        retType != Boolean.class) {
				return ExpValResult.makeValidRes(ExpResType.NUMBER, unitType);
			} else if (retType == String.class) {
				return ExpValResult.makeValidRes(ExpResType.STRING, DimensionlessUnit.class);
			} else if (Entity.class.isAssignableFrom(retType)) {
				return ExpValResult.makeValidRes(ExpResType.ENTITY, DimensionlessUnit.class);
			} else if (retType == ExpResult.class) {
				// This is another expression, so do the checks at runtime
				return ExpValResult.makeUndecidableRes();
			}

			ExpError error = new ExpError(null, 0, "Output: %s does not return a numeric type", names[1]);
			return ExpValResult.makeErrorRes(error);

		}

		@Override
		public ExpValResult validate(boolean[] hasIndices) {

			if (names.length == 2) {
				String outputName = names[1];
				OutputHandle oh = rootEnt.getOutputHandleInterned(outputName);
				if (oh == null) {
					ExpError error = new ExpError(null, 0, String.format("Could not find output '%s' on entity '%s'",
							outputName, rootEnt.getName()));
					return ExpValResult.makeErrorRes(error);
				}
				if (!hasIndices[1]) {
					Class<?> retType = oh.getReturnType();
					return validateFinalClass(retType, oh.getUnitType());

				} else {
					// Indexed final output
					if (oh.getReturnType() == DoubleVector.class ||
						oh.getReturnType() == IntegerVector.class) {
						return ExpValResult.makeValidRes(ExpResType.NUMBER, oh.getUnitType());
					}
					if (oh.getReturnType() == ArrayList.class) {
						//TODO: find out if we can determine the contained class without an instance or if type erasure prevents that
						return ExpValResult.makeUndecidableRes();
					}
					// Maps are completely unpredictable currently
					if (Map.class.isAssignableFrom(oh.getReturnType())) {
						return ExpValResult.makeUndecidableRes();
					}
					ExpError error = new ExpError(null, 0, "Output: %s is not a known array type");
					return ExpValResult.makeErrorRes(error);
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
					return ExpValResult.makeUndecidableRes();
				}

				if (i == names.length - 1) {
					// Last name in the chain, check that the type is a valid value type
					return validateFinalClass(klass, unitType);
				} else {
					if (!Entity.class.isAssignableFrom(klass)) {
						ExpError error = new ExpError(null, 0, "Output: '%s' must output an entity type", names[i]);
						return ExpValResult.makeErrorRes(error);
					}
				}
			}
			// We should never get here
			ExpError error = new ExpError(null, 0, "Validator logic error");
			return ExpValResult.makeErrorRes(error);
		}

	}

	private static class EntityEvalContext implements ExpParser.EvalContext {

		private final double simTime;

		public EntityEvalContext(double simTime) {
			this.simTime = simTime;
		}
	}

	public static EntityParseContext getParseContext(Entity thisEnt, String source) {
		return new EntityParseContext(thisEnt, source);
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
		assignmentEnt.setAttribute(attribName, result);
	}

	public static ExpResult evaluateExpression(ExpParser.Expression exp, double simTime) throws ExpError
	{
		EntityEvalContext evalContext = new EntityEvalContext(simTime);
		return exp.evaluate(evalContext);
	}
}
