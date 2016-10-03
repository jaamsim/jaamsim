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
import com.jaamsim.input.ExpParser.EvalContext;
import com.jaamsim.input.ExpParser.OutputResolver;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

/**
 * Utility class to bridge the expression parser and attribute assignment
 * @author Matt Chudleigh
 *
 */
public class ExpEvaluator {

	private static ExpResType getTypeForClass(Class<?> klass) {
		if (klass == String.class) {
			return ExpResType.STRING;
		} else if (Entity.class.isAssignableFrom(klass)){
			return ExpResType.ENTITY;
		} else if (OutputHandle.isNumericType(klass) ||
			       klass == boolean.class ||
			       klass == Boolean.class) {
			return ExpResType.NUMBER;
		} else if (ExpCollections.isCollectionClass(klass)){
			return ExpResType.COLLECTION;
		} else {
			return null;
		}
	}

	private static ExpResult getResultFromOutput(OutputHandle oh, double simTime) {
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
			return ExpResult.makeNumResult(oh.getValueAsDouble(simTime, 0), oh.getUnitType());
		}

		if (ExpCollections.isCollectionClass(retType)) {
			return ExpCollections.getCollection(oh.getValue(simTime, retType), oh.getUnitType());
		}

		// No known type
		return null;
	}

	public static ExpResult getResultFromObject(Object val, Class<? extends Unit> unitType) throws ExpError {
		if (String.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeStringResult((String)val);
		}
		if (Entity.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeEntityResult((Entity)val);
		}
		if (Double.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeNumResult((Double)val, unitType);
		}
		if (ExpCollections.isCollectionClass(val.getClass())) {
			return ExpCollections.getCollection(val, unitType);
		}
		throw new ExpError(null, 0, "Unknown type in expression: %s", val.getClass().getSimpleName());
	}

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
				if (Entity.class.isAssignableFrom(oh.getReturnType())) {
					ent = oh.getValue(simTime, Entity.class);
				}
				else if (ExpResult.class.isAssignableFrom(oh.getReturnType())) {
					ExpResult res = oh.getValue(simTime, ExpResult.class);
					if (res.type == ExpResType.ENTITY) {
						ent = res.entVal;
					} else {
						throw new ExpError(null, 0, "Output '%s' did not resolve to an entity value", outputName);
					}
				}
				else {
					throw new ExpError(null, 0, "Output '%s' is not an entity output", outputName);
				}

			}

			if (ent == null) {
				// Build up the entity chain for the error report
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

		@Override
		public ExpResult getValFromName(String name) throws ExpError {
			Entity ent;
			if (name.equals("this"))
				ent = thisEnt;
			else {
				ent = Entity.getNamedEntity(name);
				if (ent != null) {
					addEntityReference(ent);
				}
			}

			if (ent == null) {
				throw new ExpError(null, 0, "Could not find entity: %s", name);
			}

			return ExpResult.makeEntityResult(ent);
		}

		@Override
		public OutputResolver getOutputResolver(String name) throws ExpError {
			return new EntityResolver(name);
		}

		@Override
		public OutputResolver getConstOutputResolver(ExpResult constEnt, String name) throws ExpError {

			if (constEnt.type != ExpResType.ENTITY) {
				throw new ExpError(null, 0, "Can not index a non-entity type");
			}

			OutputHandle oh = constEnt.entVal.getOutputHandle(name);

			if (oh == null) {
				throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", name, constEnt.entVal.getName());
			}

			if (oh.canCache()) {
				return new CachedResolver(oh);
			} else {
				return new EntityResolver(name);
			}
		}

	}

	private static class CachedResolver implements ExpParser.OutputResolver {

		private final OutputHandle handle;
		private final ExpResType type;

		public CachedResolver(OutputHandle oh) throws ExpError {

			handle = oh;

			Class<?> retType = oh.getReturnType();

			type = getTypeForClass(retType);
			if (type == null) {
				throw new ExpError(null, 0, "Output '%s' on entity '%s does not return a type compatible with the expression engine'",
				                   oh.getName(), oh.ent.getName());
			}
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult ent)
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
			case COLLECTION:
				return ExpCollections.getCollection(handle.getValue(eec.simTime, handle.getReturnType()), handle.getUnitType());
			default:
				assert(false);
				return ExpResult.makeNumResult(handle.getValueAsDouble(eec.simTime, 0), handle.getUnitType());
			}
		}

		@Override
		public ExpValResult validate(ExpValResult entValRes) {
			if (handle == null) {
				// There is no cached output handle, so we can not decide
				return ExpValResult.makeUndecidableRes();
			}

			Class<? extends Unit> ut = DimensionlessUnit.class;
			if (type == ExpResType.NUMBER)
				ut = handle.getUnitType();

			return ExpValResult.makeValidRes(type, ut);

		}

	}

	private static class EntityResolver implements ExpParser.OutputResolver {

		private final String outputName;

		public EntityResolver(String name) {
			outputName = name.intern();
		}

		@Override
		public ExpResult resolve(EvalContext ec, ExpResult entRes) throws ExpError {

			EntityEvalContext eec = (EntityEvalContext)ec;

			if (entRes.type != ExpResType.ENTITY) {
				throw new ExpError(null, 0, "Can not look up output on non-entity type");
			}

			Entity ent = entRes.entVal;

			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", outputName, ent.getName());
			}

			ExpResult res = getResultFromOutput(oh, eec.simTime);

			if (res == null)
				throw new ExpError(null, 0, "Output %s, on entity %s does not return a type compatible with expressions.",
				                   oh.getName(), oh.ent.getName());

			return res;

		}

		@Override
		public ExpValResult validate(ExpValResult entValRes) {

			if (entValRes.type != ExpResType.ENTITY) {
				return ExpValResult.makeErrorRes(new ExpError(null, 0, "Can not evalutate output on non-entity type"));
			}

			return ExpValResult.makeUndecidableRes();
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
