/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

/**
 * OutputHandle is a class that represents all the useful runtime information for an output,
 * specifically a reference to the runtime annotation and the method it points to
 * @author matt.chudleigh
 *
 */
public class OutputHandle {

	public Entity ent;
	public OutputStaticInfo outputInfo;
	public Class<? extends Unit> unitType;

	private static final HashMap<Class<? extends Entity>, HashMap<String, OutputStaticInfo>> outputInfoCache;

	static {
		outputInfoCache = new HashMap<>();
	}

	OutputHandle(Entity e, OutputStaticInfo info) {
		ent = e;
		outputInfo = info;
		unitType = outputInfo.unitType;
		if (unitType == UserSpecifiedUnit.class)
			unitType = e.getUserUnitType();
	}

	protected OutputHandle(Entity e) {
		ent = e;
	}

	/**
	 * A data class containing the 'static' (ie: class derived) information for a single output
	 */
	private static final class OutputStaticInfo {
		public final Method method;
		public final String name;
		public final String desc;
		public final boolean reportable;
		public final Class<? extends Unit> unitType;
		public final int sequence;

		public OutputStaticInfo(Method m, Output a) {
			method = m;
			desc = a.description();
			reportable = a.reportable();
			name = a.name();
			unitType = a.unitType();
			sequence = a.sequence();
		}
	}

	// Note: this method will not include attributes in the list. For a complete list use
	// Entity.hasOutput()
	public static boolean hasOutput(Class<? extends Entity> klass, String outputName) {
		return getOutputInfoImp(klass).get(outputName) != null;
	}

	private static HashMap<String, OutputStaticInfo> getOutputInfoImp(Class<? extends Entity> klass) {
		HashMap<String, OutputStaticInfo> ret = outputInfoCache.get(klass);
		if (ret != null)
			return ret;

		// klass has not been cached yet, generate info
		ret = new HashMap<>();
		for (Method m : klass.getMethods()) {
			Output a = m.getAnnotation(Output.class);
			if (a == null)
				continue;

			// Check that this method only takes a single double (simTime) parameter
			Class<?>[] paramTypes = m.getParameterTypes();
			if (paramTypes.length != 1 ||
			    paramTypes[0] != double.class) {
				continue;
			}
			OutputStaticInfo info = new OutputStaticInfo(m, a);
			ret.put(info.name, info);
		}
		outputInfoCache.put(klass, ret);
		return ret;
	}

	public static OutputHandle getOutputHandle(Entity e, String outputName) {
		OutputStaticInfo info = getOutputInfoImp(e.getClass()).get(outputName);
		if (info == null)
			return null;

		OutputHandle ret = new OutputHandle(e, info);
		return ret;
	}

	/**
	 * Return a list of the OuputHandles for the given entity.
	 * @param e = the entity whose OutputHandles are to be returned.
	 * @return = ArrayList of OutputHandles.
	 */
	public static ArrayList<OutputHandle> getAllOutputHandles(Entity e) {
		Class<? extends Entity> klass = e.getClass();
		ArrayList<OutputHandle> ret = new ArrayList<>();
		for (OutputStaticInfo p : getOutputInfoImp(klass).values()) {
			OutputHandle oh = new OutputHandle(e, p);
			ret.add(oh); // required to get the correct unit type for the output
		}

		return ret;
	}

	/**
	 * Returns true if any of the outputs for the specified class will be printed to the
	 * output report.
	 * @param klass - class whose outputs are to be checked.
	 * @return true if any of the outputs are reportable.
	 */
	public static boolean isReportable(Class<? extends Entity> klass) {
		for (OutputStaticInfo p : getOutputInfoImp(klass).values()) {
			if (p.reportable)
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getValue(double simTime, Class<T> klass) {
		T ret = null;
		try {
			if (!klass.isAssignableFrom(outputInfo.method.getReturnType()))
				return null;

			ret = (T)outputInfo.method.invoke(ent, simTime);
		}
		catch (InvocationTargetException ex) {
			throw new ErrorException(ex.getTargetException());
		}
		catch (IllegalAccessException | ClassCastException ex) {
			throw new ErrorException(ex);
		}
		return ret;
	}

	public boolean canCache() {
		return true;
	}

	public boolean isNumericValue() {
		return isNumericType(this.getReturnType());
	}

	public boolean isIntegerValue() {
		return isIntegerType(this.getReturnType());
	}

	public static boolean isNumericType(Class<?> rtype) {

		if (rtype == double.class) return true;
		if (rtype == int.class) return true;
		if (rtype == long.class) return true;
		if (rtype == float.class) return true;
		if (rtype == short.class) return true;
		if (rtype == char.class) return true;

		if (rtype == Double.class) return true;
		if (rtype == Integer.class) return true;
		if (rtype == Long.class) return true;
		if (rtype == Float.class) return true;
		if (rtype == Short.class) return true;
		if (rtype == Character.class) return true;

		return false;
	}

	public static boolean isIntegerType(Class<?> rtype) {

		if (rtype == int.class) return true;
		if (rtype == long.class) return true;

		if (rtype == Integer.class) return true;
		if (rtype == Long.class) return true;

		return false;
	}

	/**
	 * Checks the output for all possible numerical types and returns a double representing the value
	 * @param simTime
	 * @param def - the default value if the return is null or not a number value
	 */
	public double getValueAsDouble(double simTime, double def, Unit u) {
		double ret = getValueAsDouble(simTime, def);
		Class<? extends Unit> ut = this.getUnitType();
		if (u == null)
			return ret;

		if (u.getClass() != ut)
			throw new ErrorException("Unit Mismatch");

		ret /= u.getConversionFactorToSI();
		return ret;
	}

	/**
	 * Checks the output for all possible numerical types and returns a double representing the value
	 * @param simTime
	 * @param def - the default value if the return is null or not a number value
	 */
	public double getValueAsDouble(double simTime, double def) {
		Class<?> retType = this.getReturnType();

		if (retType == double.class)
			return this.getValue(simTime, double.class);

		if (retType == int.class)
			return this.getValue(simTime, int.class).doubleValue();

		if (retType == boolean.class)
			return this.getValue(simTime, boolean.class) ? 1.0d : 0.0d;

		if (retType == float.class)
			return this.getValue(simTime, float.class).doubleValue();

		if (retType == long.class)
			return this.getValue(simTime, long.class).doubleValue();

		if (retType == short.class)
			return this.getValue(simTime, short.class).doubleValue();

		if (retType == char.class)
			return this.getValue(simTime, char.class).charValue();

		if (retType == Double.class) {
			Double val = getValue(simTime, Double.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Integer.class) {
			Integer val = getValue(simTime, Integer.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Boolean.class) {
			Boolean val = getValue(simTime, Boolean.class);
			if (val == null) return def;
			return val.booleanValue() ? 1.0d : 0.0d;
		}
		if (retType == Float.class) {
			Float val = getValue(simTime, Float.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Long.class) {
			Long val = getValue(simTime, Long.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Short.class) {
			Short val = getValue(simTime, Short.class);
			if (val == null) return def;
			return val.doubleValue();
		}
		if (retType == Character.class) {
			Character val = getValue(simTime, Character.class);
			if (val == null) return def;
			return val.charValue();
		}

		return def;
	}

	public Class<?> getReturnType() {
		return outputInfo.method.getReturnType();
	}

	public Class<?> getDeclaringClass() {
		return outputInfo.method.getDeclaringClass();
	}

	public void setUnitType(Class<? extends Unit> ut) {
		unitType = ut;
	}

	public Class<? extends Unit> getUnitType() {
		return unitType;
	}

	public String getDescription() {
		return outputInfo.desc;
	}

	public String getName() {
		return outputInfo.name;
	}

	public boolean isReportable() {
		return outputInfo.reportable;
	}

	public int getSequence() {
		return outputInfo.sequence;
	}

	@Override
	public String toString() {
		return getName();
	}

}
