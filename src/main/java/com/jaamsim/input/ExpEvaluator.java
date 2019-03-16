/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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

import java.util.HashMap;
import java.util.Map;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.input.ExpParser.Assigner;
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
		if (val == null)
			return ExpResult.makeEntityResult(null);

		if (ExpResult.class.isAssignableFrom(val.getClass())) {
			return (ExpResult)val;
		}
		if (String.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeStringResult((String)val);
		}
		if (Entity.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeEntityResult((Entity)val);
		}
		if (Double.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeNumResult((Double)val, unitType);
		}
		if (Integer.class.isAssignableFrom(val.getClass())) {
			return ExpResult.makeNumResult((Integer)val, unitType);
		}
		if (ExpCollections.isCollectionClass(val.getClass())) {
			return ExpCollections.getCollection(val, unitType);
		}
		throw new ExpError(null, 0, "Unknown type in expression: %s", val.getClass().getSimpleName());
	}

	public static class EntityParseContext extends ExpParser.ParseContext {
		private final Entity thisEnt;
		private final String source;

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

		public EntityParseContext(Entity ent, HashMap<String, ExpResult> constants, String source) {
			super(constants);
			this.thisEnt = ent;
			this.source = source;
		}

		@Override
		public ExpParser.UnitData getUnitByName(String name) {
			Unit unit = Input.tryParseUnit(thisEnt.getJaamSimModel(), name, Unit.class);
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
		public ExpResult getValFromLitName(String name, String source, int pos) throws ExpError {
			Entity ent = thisEnt.getJaamSimModel().getNamedEntity(name);
			if (ent == null) {
				throw new ExpError(source, pos, "Could not find entity: %s", name);
			}

			addEntityReference(ent);
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

			if (constEnt.entVal == null) {
				throw new ExpError(null, 0, "Trying to resolve output on null entity");
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

		@Override
		public Assigner getAssigner(String attribName) throws ExpError {
			return new EntityAssigner(attribName);
		}

		@Override
		public Assigner getConstAssigner(ExpResult constEnt, String attribName)
				throws ExpError {
			// TODO: const optimization
			return new EntityAssigner(attribName);
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
			double simTime = 0;
			if (ec != null) {
				EntityEvalContext eec = (EntityEvalContext)ec;
				simTime = eec.simTime;
			}

			switch (type) {
			case NUMBER:
				double val = handle.getValueAsDouble(simTime, 0);
				return ExpResult.makeNumResult(val, handle.getUnitType());
			case ENTITY:
				return ExpResult.makeEntityResult(handle.getValue(simTime, Entity.class));
			case STRING:
				return ExpResult.makeStringResult(handle.getValue(simTime, String.class));
			case COLLECTION:
				return ExpCollections.getCollection(handle.getValue(simTime, handle.getReturnType()), handle.getUnitType());
			default:
				assert(false);
				return ExpResult.makeNumResult(handle.getValueAsDouble(simTime, 0), handle.getUnitType());
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

			double simTime = 0;
			if (ec != null) {
				EntityEvalContext eec = (EntityEvalContext)ec;
				simTime = eec.simTime;
			}

			if (entRes.type != ExpResType.ENTITY) {
				throw new ExpError(null, 0, "Can not look up output on non-entity type");
			}

			Entity ent = entRes.entVal;
			if (ent == null) {
				throw new ExpError(null, 0, "Trying to resolve output on null entity");
			}

			OutputHandle oh = ent.getOutputHandleInterned(outputName);
			if (oh == null) {
				throw new ExpError(null, 0, "Could not find output '%s' on entity '%s'", outputName, ent.getName());
			}

			ExpResult res = getResultFromOutput(oh, simTime);

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

	private static class EntityAssigner implements ExpParser.Assigner {

		private final String attribName;
		EntityAssigner(String attribName) {
			this.attribName = attribName;
		}

		@Override
		public void assign(ExpResult ent, ExpResult index, ExpResult val) throws ExpError {
			if (ent.type != ExpResType.ENTITY) {
				throw new ExpError(null, 0, "Can not execute assignment, not assigning to an entity");
			}
			Entity assignEnt = ent.entVal;
			if (assignEnt == null) {
				throw new ExpError(null, 0, "Trying to assign to a null entity");
			}

			assignEnt.setAttribute(attribName, index, val);
		}

	}

	private static class EntityEvalContext extends ExpParser.EvalContext {

		private final double simTime;

		public EntityEvalContext(double simTime) {
			this.simTime = simTime;
		}

	}

	public static EntityParseContext getParseContext(Entity thisEnt, String source) {
		HashMap<String, ExpResult> constants = new HashMap<>();
		constants.put("this", ExpResult.makeEntityResult(thisEnt));

		return new EntityParseContext(thisEnt, constants, source);
	}

	public static ExpResult evaluateExpression(ExpParser.Expression exp, double simTime) throws ExpError
	{
		EntityEvalContext evalContext = new EntityEvalContext(simTime);
		return exp.evaluate(evalContext);
	}
}
