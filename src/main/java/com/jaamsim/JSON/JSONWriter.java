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
import java.util.Iterator;

public class JSONWriter {

	public static String writeJSONValue(JSONValue val) {
		StringBuilder out = new StringBuilder();
		writeVal(val, out);
		return out.toString();
	}

	private static String getEscapeSeq(char c) {
		switch (c) {
		case '\n': return "\\n";
		case '\r': return "\\r";
		case '\t': return "\\t";
		case '\b': return "\\b";
		case '\f': return "\\f";
		case '"': return "\\\"";
		case '\\': return "\\\\";
		}
		if (c < 0x1f) {
			// This char is a control code without a short form, use the
			// explicit unicode codepoint format
			return String.format("\\u%04x", (int)c);
		}
		assert(false);
		return "";
	}

	private static boolean mustEscapeChar(char c) {
		if (c <= 0x1f) return true;
		if (c == '\\') return true;
		if (c == '"') return true;
		return false;
	}

	private static String escapeString(String s) {

		StringBuilder out = new StringBuilder();
		int pos = 0;
		while(pos < s.length()) {
			char c = s.charAt(pos++);
			if (mustEscapeChar(c)) {
				out.append(getEscapeSeq(c));
			} else {
				out.append(c);
			}
		}

		return out.toString();
	}

	private static void writeVal(JSONValue val, StringBuilder out) {
		if (val.isNumber()) {
			out.append(val.numVal);
			return;
		}
		if (val.isKeyword()) {
			if (val.isTrue()) out.append("true");
			if (val.isFalse()) out.append("false");
			if (val.isNull()) out.append("null");
			return;
		}
		if (val.isString()) {
			out.append('"');
			out.append(escapeString(val.stringVal));
			out.append('"');
			return;
		}

		if (val.isList()) {
			writeList(val.listVal, out);
			return;
		}

		if (val.isMap()) {
			writeMap(val.mapVal, out);
			return;
		}

		assert(false);
	}

	private static void writeList(ArrayList<JSONValue> list, StringBuilder out) {
		Iterator<JSONValue > it  = list.iterator();
		out.append("[");
		while(it.hasNext()) {
			JSONValue val = it.next();
			writeVal(val, out);
			if (it.hasNext()) {
				out.append(", ");
			}
		}
		out.append("]");
	}
	private static void writeMap(HashMap<String, JSONValue> map, StringBuilder out) {
		Iterator<HashMap.Entry<String, JSONValue>> it = map.entrySet().iterator();
		out.append("{");
		while(it.hasNext()) {
			HashMap.Entry<String, JSONValue> entry = it.next();
			out.append("\"");
			out.append(escapeString(entry.getKey()));
			out.append("\": ");
			writeVal(entry.getValue(), out);

			if (it.hasNext()) {
				out.append(", ");
			}
		}
		out.append("}");
	}
}
