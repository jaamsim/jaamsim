/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2021 JaamSim Software Inc.
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
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.units.Unit;

public class ExpResult {

	public interface Iterator {
		public boolean hasNext();
		public ExpResult nextKey() throws ExpError;
	}

	public interface Collection {
		public ExpResult index(ExpResult index) throws ExpError;

		// Collections have a copy-on-write feature, where the old reference may be invalidated
		// after assigning, always use the value returned as the new collection
		public Collection assign(ExpResult key, ExpResult value) throws ExpError;

		public Iterator getIter();

		public int getSize();

		public String getOutputString(JaamSimModel simModel);

		public Collection getCopy();
	}

	public final ExpResType type;

	public final double value;
	public final Class<? extends Unit> unitType;

	public final String stringVal;
	public final Entity entVal;
	public final Collection colVal;
	public final ExpParser.LambdaClosure lcVal;

	public static ExpResult makeNumResult(double val, Class<? extends Unit> ut) {
		return new ExpResult(ExpResType.NUMBER, val, ut, null, null, null, null);
	}

	public static ExpResult makeStringResult(String str) {
		return new ExpResult(ExpResType.STRING, 0, null, str, null, null, null);
	}

	public static ExpResult makeEntityResult(Entity ent) {
		return new ExpResult(ExpResType.ENTITY, 0, null, null, ent, null, null);
	}

	public static ExpResult makeCollectionResult(Collection col) {
		return new ExpResult(ExpResType.COLLECTION, 0, null, null, null, col, null);
	}

	public static ExpResult makeLambdaResult(ExpParser.LambdaClosure lc) {
		return new ExpResult(ExpResType.LAMBDA, 0, null, null, null, null, lc);
	}

	private ExpResult(ExpResType type, double val, Class<? extends Unit> ut, String str, Entity ent, Collection col, ExpParser.LambdaClosure lc) {
		this.type = type;
		value = val;
		unitType = ut;

		stringVal = str;
		entVal = ent;
		colVal = col;
		lcVal = lc;
	}

	public <T> T getValue(double simTime, Class<T> klass) {
		// Make a best effort to return the type
		if (klass.isAssignableFrom(ExpResult.class))
			return klass.cast(this);

		if (type == ExpResType.STRING && klass.isAssignableFrom(String.class)) {
			return klass.cast(stringVal);
		}

		if (type == ExpResType.ENTITY && klass.isAssignableFrom(Entity.class)) {
			return klass.cast(entVal);
		}

		if (klass.equals(double.class) || klass.equals(Double.class)) {
			if (type == ExpResType.NUMBER)
			return klass.cast(value);
		}

		return null;
	}

	public ExpResult getCopy() {
		if (type == ExpResType.COLLECTION) {
			return makeCollectionResult(colVal.getCopy());
		}
		return this;
	}

	public String getOutputString(JaamSimModel simModel) {
		switch (type) {
		case NUMBER:
			double factor = 1.0d;
			String unitString = Unit.getSIUnit(unitType);
			if (simModel != null) {
				factor = simModel.getDisplayedUnitFactor(unitType);
				unitString = simModel.getDisplayedUnit(unitType);
			}
			if (unitString.isEmpty())
				return String.format("%s", value);
			return String.format("%s[%s]", value/factor, unitString);
		case STRING:
			return String.format("\"%s\"", stringVal);
		case ENTITY:
			if (entVal == null)
				return "null";
			return String.format("[%s]", entVal.getName());
		case COLLECTION:
			return colVal.getOutputString(simModel);
		case LAMBDA:
			return "function|" + lcVal.getNumParams()+"|";

		default:
			assert(false);
			return "???";
		}
	}

	// Like 'getOutputString' but does not quote string types, making it more useful in string formaters
	public String getFormatString() {
		switch (type) {
		case NUMBER:
			String unitString = Unit.getSIUnit(unitType);
			if (unitString.isEmpty())
				return String.format("%s", value);
			return String.format("%s[%s]", value, unitString);
		case STRING:
			return stringVal;
		case ENTITY:
			if (entVal == null)
				return "null";
			return String.format("[%s]", entVal.getName());
		case COLLECTION:
			return colVal.getOutputString(null);
		case LAMBDA:
			return "function|" + lcVal.getNumParams()+"|";

		default:
			assert(false);
			return "???";
		}
	}

	@Override
	public String toString() {
		return getOutputString(null);
	}

	@Override
	public boolean equals(Object o) {
		if (ExpResult.class != o.getClass()) {
			return false;
		}
		ExpResult other = (ExpResult)o;

		switch (type) {
		case NUMBER:
			return this.value == other.value && this.unitType == other.unitType;
		case STRING:
			return this.stringVal.equals(other.stringVal);
		case ENTITY:
			return this.entVal == other.entVal;
		case COLLECTION:
			return this.colVal == other.colVal;

		default:
			return false;
		}

	}

}
