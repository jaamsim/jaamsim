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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.units.Unit;

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

	private static final HashMap<Class<? extends Entity>, ArrayList<OutputStaticInfo>> outputInfoCache;

	static {
		outputInfoCache = new HashMap<>();
	}

	public OutputHandle(Entity e, String outputName) {
		ent = e;
		outputInfo = OutputHandle.getOutputInfo(e.getClass(), outputName);
		unitType = outputInfo.unitType;
	}

	protected OutputHandle(Entity e) {
		ent = e;
	}

	/**
	 * A data class containing the 'static' (ie: class derived) information for a single output
	 */
	private static final class OutputStaticInfo {
		public Method method;
		public final String name;
		public final String desc;
		public final boolean reportable;
		public final Class<? extends Unit> unitType;
		public final int sequence;

		public OutputStaticInfo(Method m, Output a) {
			method = m;
			desc = a.description();
			reportable = a.reportable();
			name = a.name().intern();
			unitType = a.unitType();
			sequence = a.sequence();
		}
	}

	// Note: this method will not include attributes in the list. For a complete list use
	// Entity.hasOutput()
	public static boolean hasOutput(Class<? extends Entity> klass, String outputName) {
		return OutputHandle.getOutputInfo(klass, outputName) != null;
	}

	public static boolean hasOutputInterned(Class<? extends Entity> klass, String outputName) {
		return OutputHandle.getOutputInfoInterned(klass, outputName) != null;
	}

	private static OutputStaticInfo getOutputInfo(Class<? extends Entity> klass, String outputName) {
		for (OutputStaticInfo p : getOutputInfoImp(klass)) {
			if( p.name.equals(outputName) )
				return p;
		}
		return null;
	}

	private static OutputStaticInfo getOutputInfoInterned(Class<? extends Entity> klass, String outputName) {
		for (OutputStaticInfo p : getOutputInfoImp(klass)) {
			if( p.name == outputName )
				return p;
		}
		return null;
	}

	private static ArrayList<OutputStaticInfo> getOutputInfoImp(Class<? extends Entity> klass) {
		ArrayList<OutputStaticInfo> ret = outputInfoCache.get(klass);
		if (ret != null)
			return ret;

		// klass has not been cached yet, generate info
		ret = new ArrayList<>();
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

			ret.add(new OutputStaticInfo(m, a));
		}
		outputInfoCache.put(klass, ret);
		return ret;
	}

	/**
	 * Return a list of the OuputHandles for the given entity.
	 * @param e = the entity whose OutputHandles are to be returned.
	 * @return = ArrayList of OutputHandles.
	 */
	public static ArrayList<OutputHandle> getOutputHandleList(Entity e) {
		Class<? extends Entity> klass = e.getClass();
		ArrayList<OutputStaticInfo> list = getOutputInfoImp(klass);
		ArrayList<OutputHandle> ret = new ArrayList<>(list.size());
		for( OutputStaticInfo p : list ) {
			//ret.add( new OutputHandle(e, p) );
			ret.add( e.getOutputHandle(p.name) );  // required to get the correct unit type for the output
		}

		// Add the custom outputs
		for (String outputName : e.getCustomOutputNames()) {
			ret.add(e.getOutputHandle(outputName));
		}
		// Add the input outputs
		for (String outputName : e.getInputOutputNames()) {
			ret.add(e.getOutputHandle(outputName));
		}

		// And the attributes
		for (String attribName : e.getAttributeNames()) {
			ret.add(e.getOutputHandle(attribName));
		}

		Collections.sort(ret, new OutputHandleComparator());
		return ret;
	}

	private static class OutputHandleComparator implements Comparator<OutputHandle> {

		@Override
		public int compare(OutputHandle hand0, OutputHandle hand1) {
			Class<?> class0 = hand0.getDeclaringClass();
			Class<?> class1 = hand1.getDeclaringClass();

			if (class0 == class1) {
				if (hand0.getSequence() == hand1.getSequence())
					return 0;
				else if (hand0.getSequence() < hand1.getSequence())
					return -1;
				else
					return 1;
			}

			if (class0.isAssignableFrom(class1))
				return -1;
			else
				return 1;
		}
	}

	/**
	 * Returns true if any of the outputs for the specified class will be printed to the
	 * output report.
	 * @param klass - class whose outputs are to be checked.
	 * @return true if any of the outputs are reportable.
	 */
	public static boolean isReportable(Class<? extends Entity> klass) {
		ArrayList<OutputStaticInfo> list = getOutputInfoImp(klass);
		for( OutputStaticInfo p : list ) {
			if (p.reportable)
				return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked") // This suppresses the warning on the cast, which is effectively checked
	public <T> T getValue(double simTime, Class<T> klass) {
		if( outputInfo.method == null )
			return null;

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
	 * @return
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
	 * @return
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
		assert (outputInfo.method != null);
		return outputInfo.method.getReturnType();
	}

	public Class<?> getDeclaringClass() {
		assert (outputInfo.method != null);
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

	// Lookup an outputs return type from the class and output name only
	public static Class<?> getStaticOutputType(Class<?> klass, String outputName) {
		if (!Entity.class.isAssignableFrom(klass)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		ArrayList<OutputStaticInfo> infos = getOutputInfoImp((Class<? extends Entity>)klass);

		for (OutputStaticInfo info : infos) {
			if (info.name.equals(outputName)) {
				 return info.method.getReturnType();
			}
		}
		return null;
	}

	// Lookup an outputs return type from the unit type
	public static Class<? extends Unit> getStaticOutputUnitType(Class<?> klass, String outputName) {
		if (!Entity.class.isAssignableFrom(klass)) {
			return null;
		}

		@SuppressWarnings("unchecked")
		ArrayList<OutputStaticInfo> infos = getOutputInfoImp((Class<? extends Entity>)klass);

		for (OutputStaticInfo info : infos) {
			if (info.name.equals(outputName)) {
				 return info.unitType;
			}
		}
		return null;
	}

	@Override
	public String toString() {
		return getName();
	}

}
