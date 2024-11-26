/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
 * Copyright (C) 2017-2024 JaamSim Software Inc.
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
package com.jaamsim.Samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.ArrayListInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class SampleListInput extends ArrayListInput<SampleProvider> {

	private final ArrayList<Class<? extends Unit>> unitTypeList;
	private boolean dimensionless = false;
	private double minValue = Double.NEGATIVE_INFINITY;
	private double maxValue = Double.POSITIVE_INFINITY;
	private boolean integerValue = false;

	public SampleListInput(String key, String cat, ArrayList<SampleProvider> def) {
		super(key, cat, def);
		unitTypeList = new ArrayList<>();
	}

	public SampleListInput(String key, String cat, double def) {
		this(key, cat, new ArrayList<SampleProvider>(Arrays.asList(new SampleConstant(def))));
	}

	public SampleListInput(String key, String cat, int def) {
		this(key, cat, new ArrayList<SampleProvider>(Arrays.asList(new SampleConstant(def))));
	}

	private void setUnitTypeList(ArrayList<Class<? extends Unit>> utList) {

		if (utList.equals(unitTypeList))
			return;

		// Save the new unit types
		if (!isDef)
			setValid(false);
		unitTypeList.clear();
		unitTypeList.addAll(utList);

		// Set the units for the default value column in the Input Editor
		if (defValue == null)
			return;
		for (int i=0; i<defValue.size(); i++) {
			SampleProvider p = defValue.get(i);
			if (p instanceof SampleConstant)
				((SampleConstant) p).setUnitType(getUnitType(i));
		}
	}

	public void setUnitType(Class<? extends Unit> u) {
		ArrayList<Class<? extends Unit>> utList = new ArrayList<>(1);
		utList.add(u);
		this.setUnitTypeList(utList);
	}

	public void setDimensionless(boolean bool) {
		dimensionless = bool;
	}

	public void setIntegerValue(boolean bool) {
		integerValue = bool;
	}

	/**
	 * Returns the unit type for the specified expression.
	 * <p>
	 * If the number of expressions exceeds the number of unit types
	 * then the last unit type in the list is returned.
	 * @param i - index of the expression
	 * @return unit type for the expression
	 */
	public Class<? extends Unit> getUnitType(int i) {
		if (unitTypeList.isEmpty())
			return null;
		int k = Math.min(i, unitTypeList.size()-1);
		return unitTypeList.get(k);
	}

	public void setValidRange(double min, double max) {
		minValue = min;
		maxValue = max;
	}

	@Override
	public String applyConditioning(String str) {
		if (!str.contains("{")) {
			return str;
		}
		String[] array = Parser.splitSubstrings(str);
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < array.length; i++) {
			array[i] = SampleProvider.addQuotesIfNeeded(array[i]);
			sb.append("{").append(array[i]).append("}");
		}
		return sb.toString();
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();

		// Simple format without inner braces
		if (dimensionless && subArgs.size() == 1) {
			KeywordIndex subArg = subArgs.get(0);
			ArrayList<SampleProvider> temp = new ArrayList<>(subArg.numArgs());
			for (int i = 0; i < subArg.numArgs(); i++) {
				KeywordIndex argKw = new KeywordIndex(subArg, i, i + 1);
				try {
					SampleProvider sp = Input.parseSampleExp(argKw, thisEnt, minValue, maxValue, getUnitType(i));
					if (integerValue && sp instanceof SampleConstant)
						sp = new SampleConstant((int) sp.getNextSample(thisEnt, 0.0d));
					temp.add(sp);
				}
				catch (InputErrorException e) {
					String msg = e.getMessage();
					if (subArg.numArgs() > 1)
						msg = String.format(INP_ERR_ELEMENT, i + 1, e.getMessage());
					throw new InputErrorException(e.position, e.source, msg, e);
				}
			}
			value = temp;
			this.setValid(true);
			return;
		}

		// Normal format with inner braces
		ArrayList<SampleProvider> temp = new ArrayList<>(subArgs.size());
		for (int i = 0; i < subArgs.size(); i++) {
			KeywordIndex subArg = subArgs.get(i);
			try {
				SampleProvider sp = Input.parseSampleExp(subArg, thisEnt, minValue, maxValue, getUnitType(i));
				if (integerValue && sp instanceof SampleConstant)
					sp = new SampleConstant((int) sp.getNextSample(thisEnt, 0.0d));
				temp.add(sp);
			}
			catch (InputErrorException e) {
				String msg = e.getMessage();
				if (subArg.numArgs() > 1)
					msg = String.format(INP_ERR_ELEMENT, i + 1, e.getMessage());
				throw new InputErrorException(e.position, e.source, msg, e);
			}
		}
		value = temp;
		this.setValid(true);
	}

	@Override
	public String getValidInputDesc() {
		if (integerValue)
			return Input.VALID_SAMPLE_LIST_INTEGER;
		if (dimensionless) {
			return Input.VALID_SAMPLE_LIST_DIMLESS;
		}
		return Input.VALID_SAMPLE_LIST;
	}

	@Override
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>();
		JaamSimModel simModel = ent.getJaamSimModel();
		for (Entity each : simModel.getClonesOfIterator(Entity.class, SampleProvider.class)) {
			SampleProvider samp = (SampleProvider)each;
			if (unitTypeList.contains(samp.getUnitType()))
				list.add(each.getName());
		}
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || valueTokens == null || isDef)
			return;

		// No inner braces
		if (!valueTokens[0].equals("{")) {
			for (int i = 0; i < value.size(); i++) {
				if (value.get(i) instanceof SampleConstant && !integerValue) {
					toks.add(valueTokens[i]);
					// Single input value with dimensions
					if (!dimensionless && getUnitType(i) != DimensionlessUnit.class && i + 1 < valueTokens.length) {
						toks.add(valueTokens[i + 1]);
						return;
					}
				}
				else
					toks.add(value.get(i).toString());
			}
			return;
		}

		// With inner braces
		for (int i = 0; i < value.size(); i++) {
			toks.add("{");
			if (value.get(i) instanceof SampleConstant && !integerValue)
				getSubValueTokens(i, toks);
			else
				toks.add(value.get(i).toString());
			toks.add("}");
		}
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null || defValue.isEmpty()) {
			return "";
		}

		StringBuilder tmp = new StringBuilder();
		for (int i = 0; i < defValue.size(); i++) {
			if (i > 0)
				tmp.append(SEPARATOR);
			tmp.append(defValue.get(i));
		}

		return tmp.toString();
	}

	@Override
	public boolean removeReferences(Entity ent) {
		if (value == null)
			return false;
		boolean ret = value.removeAll(Collections.singleton(ent));
		return ret;
	}

	@Override
	public void appendEntityReferences(ArrayList<Entity> list) {
		if (value == null)
			return;
		for (SampleProvider samp : value) {
			if (samp instanceof Entity) {
				final Entity sampEnt = (Entity)samp;
				if (list.contains(sampEnt))
					continue;
				list.add(sampEnt);
				continue;
			}

			if (samp instanceof SampleExpression) {
				((SampleExpression) samp).appendEntityReferences(list);
				continue;
			}
		}
	}

	@Override
	public boolean useExpressionBuilder() {
		return true;
	}

	@Override
	public String getPresentValueString(Entity thisEnt, double simTime) {
		if (value == null)
			return "";
		JaamSimModel simModel = thisEnt.getJaamSimModel();

		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (SampleProvider samp : value) {
			if (!first) {
				sb.append(Input.SEPARATOR);
			}
			first = false;
			sb.append("{").append(Input.BRACE_SEPARATOR);
			Class<? extends Unit> ut = samp.getUnitType();
			if (ut == DimensionlessUnit.class) {
				sb.append(Double.toString(samp.getNextSample(thisEnt, simTime)));
			}
			else {
				String unitString = simModel.getDisplayedUnit(ut);
				double sifactor = simModel.getDisplayedUnitFactor(ut);
				sb.append(Double.toString(samp.getNextSample(thisEnt, simTime)/sifactor));
				sb.append("[").append(unitString).append("]");
			}
			sb.append(Input.BRACE_SEPARATOR).append("}");
		}
		return sb.toString();
	}

	public double getNextSample(int i, Entity thisEnt, double simTime) {
		try {
			return getValue().get(i).getNextSample(thisEnt, simTime);
		}
		catch (ErrorException e) {
			if (e.entName.isEmpty()) {
				e.entName = thisEnt.getName();
				e.keyword = getKeyword();
				e.index = i + 1;
			}
			throw e;
		}
		catch (Exception e) {
			throw new ErrorException("", -1, thisEnt.getName(), getKeyword(), i + 1,
					e.getMessage(), e);
		}
	}

	public double[] getNextDoubles(Entity thisEnt, double simTime) {
		double[] ret = new double[getListSize()];
		for (int i = 0; i < getListSize(); i++) {
			ret[i] = getNextSample(i, thisEnt, simTime);
			if (integerValue)
				ret[i] = (int) ret[i];
		}
		return ret;
	}

	public int[] getNextIntegers(Entity thisEnt, double simTime) {
		return getNextIntegers(thisEnt, simTime, getListSize());
	}

	public int[] getNextIntegers(Entity thisEnt, double simTime, int length) {
		int[] ret = new int[length];
		for (int i = 0; i < length; i++) {
			int ind = Math.min(i, getListSize() - 1);
			ret[i] = (int) getNextSample(ind, thisEnt, simTime);
		}
		return ret;
	}

}
