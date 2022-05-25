/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.JSON;

import java.util.ArrayList;
import java.util.HashMap;

public class JSONValue {
	public HashMap<String, JSONValue> mapVal;
	public ArrayList<JSONValue> listVal;
	public String stringVal;
	public double numVal;
	public boolean isKey = false;

	// If isKey is true, numVal will be one of the following values
	public static double NULL_VAL = 0.0;
	public static double TRUE_VAL = 1.0;
	public static double FALSE_VAL = 2.0;

	public boolean isMap() { return mapVal != null; }
	public boolean isList() { return listVal != null; }
	public boolean isString() { return stringVal != null; }
	public boolean isKeyword() { return isKey; }
	public boolean isNumber() {
		return mapVal == null && listVal == null && stringVal == null && !isKey;
	}

	// Helpers
	public boolean isNull() {
		return isKey && numVal == NULL_VAL;
	}
	public boolean isTrue() {
		return isKey && numVal == TRUE_VAL;
	}
	public boolean isFalse() {
		return isKey && numVal == FALSE_VAL;
	}

	public static JSONValue makeStringVal(String s) {
		JSONValue ret = new JSONValue();
		ret.stringVal = s;
		return ret;
	}
	public static JSONValue makeNumVal(double num) {
		JSONValue ret = new JSONValue();
		ret.numVal = num;
		return ret;
	}
	public static JSONValue makeTrueVal() {
		JSONValue ret = new JSONValue();
		ret.numVal = TRUE_VAL;
		ret.isKey = true;
		return ret;
	}
	public static JSONValue makeFalseVal() {
		JSONValue ret = new JSONValue();
		ret.numVal = FALSE_VAL;
		ret.isKey = true;
		return ret;
	}
	public static JSONValue makeNullVal() {
		JSONValue ret = new JSONValue();
		ret.numVal = NULL_VAL;
		ret.isKey = true;
		return ret;
	}
	public static JSONValue makeObject() {
		JSONValue ret = new JSONValue();
		ret.mapVal = new HashMap<>();
		return ret;
	}
	public static JSONValue makeArray() {
		JSONValue ret = new JSONValue();
		ret.listVal = new ArrayList<>();
		return ret;
	}
}