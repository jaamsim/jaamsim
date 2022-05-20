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
import java.util.Map;

import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpError;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;
import com.jaamsim.units.DimensionlessUnit;

public class JSONConverter {

	public static JSONParser.Value fromExpResult(ExpResult val) throws ExpError {
		if (val.type == ExpResType.NUMBER) {
			return JSONParser.Value.makeNumVal(val.value);
		}
		if (val.type == ExpResType.STRING) {
			return JSONParser.Value.makeStringVal(val.stringVal);
		}

		if (val.type == ExpResType.ENTITY) {
			return JSONParser.Value.makeStringVal(val.entVal.getName());
		}

		if (val.type == ExpResType.LAMBDA) {
			throw new ExpError(null, 0, "Can not convert lambda value to JSON");
		}

		if (val.type == ExpResType.COLLECTION) {
			return fromCollection(val.colVal);
		}

		throw new ExpError(null, 0, "Internal error: invalid expression value type");

	}

	private static JSONParser.Value fromCollection(ExpResult.Collection col) throws ExpError {
		ExpResult.Iterator it = col.getIter();
		JSONParser.Value ret = new JSONParser.Value();
		if (!it.hasNext()) {
			// Emit an empty list for any empty object type
			ret.listVal = new ArrayList<>();
			return ret;
		}
		boolean seenFirst = false;
		boolean isObject = false;
		int valueCount = 1;
		while(it.hasNext()) {
			ExpResult key = it.nextKey();
			ExpResult val = col.index(key);
			if (!seenFirst) {
				// Check the type of the first key to determine if this is a JSON object or a JSON array
				if (key.type == ExpResType.NUMBER) {
					isObject = false;
					ret.listVal = new ArrayList<>();
				} else if (key.type == ExpResType.STRING) {
					isObject = true;
					ret.mapVal = new HashMap<>();
				}
				seenFirst = true;
			}

			if (isObject) {
				if (key.type != ExpResType.STRING) {
					throw new ExpError(null, 0, "When converting to JSON all keys in a map must be strings");
				}
				ret.mapVal.put(key.stringVal, fromExpResult(val));
			} else {
				// not an object, therefore is an array
				if (key.type != ExpResType.NUMBER) {
					throw new ExpError(null, 0, "When converting to JSON all keys in an array must be numbers");
				}
				if (key.value != valueCount) {
					throw new ExpError(null, 0, "Internal error, key values out of sync");
				}
				ret.listVal.add(fromExpResult(val));
				valueCount++;
			}
		}
		return ret;
	}

	public static ExpResult toExpResult(JSONParser.Value val) {
		if (val.isMap()) {
			HashMap<String, ExpResult> expMap = new HashMap<>();
			for (Map.Entry<String, JSONParser.Value> s : val.mapVal.entrySet()) {
				expMap.put(s.getKey(), toExpResult(s.getValue()));
			}
			return ExpCollections.wrapCollection(expMap, DimensionlessUnit.class);
		}
		if (val.isList()) {
			ArrayList<ExpResult> expList = new ArrayList<>();
			for (JSONParser.Value v : val.listVal) {
				expList.add(toExpResult(v));
			}
			return ExpCollections.wrapCollection(expList, DimensionlessUnit.class);
		}
		if (val.isString()) {
			return ExpResult.makeStringResult(val.stringVal);
		}
		if (val.isNumber()) {
			return ExpResult.makeNumResult(val.numVal, DimensionlessUnit.class);
		}
		if (val.isTrue()) {
			return ExpResult.makeNumResult(1.0, DimensionlessUnit.class);
		}
		if (val.isFalse()) {
			return ExpResult.makeNumResult(0.0, DimensionlessUnit.class);
		}
		if (val.isNull()) {
			assert(false); // This needs to be audited to be sure it's safe
			return ExpResult.makeEntityResult(null);
		}
		assert(false);
		return null;
	}
}
