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

public class JSONParser {

	public static class Value {
		public HashMap<String, Value> mapVal;
		public ArrayList<Value> listVal;
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

		public static Value makeStringVal(String s) {
			Value ret = new Value();
			ret.stringVal = s;
			return ret;
		}
		public static Value makeNumVal(double num) {
			Value ret = new Value();
			ret.numVal = num;
			return ret;
		}
		public static Value makeTrueVal() {
			Value ret = new Value();
			ret.numVal = TRUE_VAL;
			ret.isKey = true;
			return ret;
		}
		public static Value makeFalseVal() {
			Value ret = new Value();
			ret.numVal = FALSE_VAL;
			ret.isKey = true;
			return ret;
		}
		public static Value makeNullVal() {
			Value ret = new Value();
			ret.numVal = NULL_VAL;
			ret.isKey = true;
			return ret;
		}
		public static Value makeObject() {
			Value ret = new Value();
			ret.mapVal = new HashMap<>();
			return ret;
		}
		public static Value makeArray() {
			Value ret = new Value();
			ret.listVal = new ArrayList<>();
			return ret;
		}
	}

	public static boolean isSymTok(JSONTokenizer.Token tok, String sym) {
		return tok.type == JSONTokenizer.SYM_TYPE && tok.value.equals(sym);
	}
	public static boolean isStringTok(JSONTokenizer.Token tok) {
		return tok.type == JSONTokenizer.STRING_TYPE;
	}

	private static int parseMap(ArrayList<JSONTokenizer.Token> toks, int startPos, Value outVal) throws JSONError {
		// Consume the opening token
		int pos = startPos;
		JSONTokenizer.Token tok = toks.get(pos);
		HashMap<String, Value> vals = new HashMap<>();

		// special case: empty list
		if (isSymTok(tok, "]")) {
			// This map has terminated
			outVal.mapVal = vals;
			return pos+1;
		}

		while(pos < toks.size()) {
			if (!isStringTok(tok)) {
				throw new JSONError(null, tok.pos, "Map keys must be strings");
			}
			String key = tok.value;
			pos++;

			tok = toks.get(pos);
			if (!isSymTok(tok, ":")) {
				throw new JSONError(null, tok.pos, "Expected \":\"");
			}
			pos++;

			Value val = new Value();
			pos = parseElement(toks, pos, val);
			vals.put(key, val);

			tok = toks.get(pos);

			if (isSymTok(tok, "}")) {
				// Terminated
				outVal.mapVal = vals;
				return pos+1;
			}
			if (isSymTok(tok, ",")) {
				pos++;
				tok = toks.get(pos);
				continue;
			}
			throw new JSONError(null, tok.pos, "Unexpected symbol in map");

		}
		tok = toks.get(startPos);
		throw new JSONError(null, tok.pos, "Unterminated list");
	}

	private static int parseList(ArrayList<JSONTokenizer.Token> toks, int startPos, Value outVal) throws JSONError {
		// Consume the opening token
		int pos = startPos;
		JSONTokenizer.Token tok = toks.get(pos);
		ArrayList<Value> vals = new ArrayList<>();

		// special case: empty list
		if (isSymTok(tok, "]")) {
			// This list has terminated
			outVal.listVal = vals;
			return pos+1;
		}

		while(pos < toks.size()) {
			Value val = new Value();
			pos = parseElement(toks, pos, val);
			vals.add(val);

			tok = toks.get(pos);

			if (isSymTok(tok, "]")) {
				// Terminated
				outVal.listVal = vals;
				return pos+1;
			}
			if (isSymTok(tok, ",")) {
				tok = toks.get(pos++);
				continue;
			}
			throw new JSONError(null, tok.pos, "Unexpected symbol in list");

		}
		tok = toks.get(startPos);
		throw new JSONError(null, tok.pos, "Unterminated list");
	}

	private static int parseElement(ArrayList<JSONTokenizer.Token> toks, int startPos, Value outVal) throws JSONError {
		int pos = startPos;
		JSONTokenizer.Token startTok = toks.get(pos++);
		switch(startTok.type) {
		case JSONTokenizer.NUM_TYPE:
			try {
				outVal.numVal = Double.parseDouble(startTok.value);
				return pos;
			} catch(NumberFormatException ex) {
				throw new JSONError(null, startTok.pos, "Number format error");
			}
		case JSONTokenizer.STRING_TYPE:
			outVal.stringVal = startTok.value;
			return pos;
		case JSONTokenizer.SYM_TYPE:
			switch(startTok.value) {
			case "[":
				return parseList(toks, pos, outVal);
			case "{":
				return parseMap(toks, pos, outVal);
			default:
				throw new JSONError(null, startTok.pos, "Unexpected token to start element");
			}
		case JSONTokenizer.KEYWORD_TYPE:
			outVal.isKey = true;
			if (startTok.value.equals("true")) {
				outVal.numVal = JSONParser.Value.TRUE_VAL;
			}
			if (startTok.value.equals("false")) {
				outVal.numVal = JSONParser.Value.FALSE_VAL;
			}
			if (startTok.value.equals("null")) {
				outVal.numVal = JSONParser.Value.NULL_VAL;
			}
			return pos;

		default:
			throw new JSONError(null, startTok.pos, "Internal error: Unknown token type");
		}

	}

	public static Value parse(ArrayList<JSONTokenizer.Token> toks) throws JSONError {
		Value ret = new Value();
		parseElement(toks, 0, ret);
		return ret;
	}

	public static Value parse(String json) throws JSONError {
		ArrayList<JSONTokenizer.Token> toks =  JSONTokenizer.tokenize(json);
		return parse(toks);
	}

	ArrayList<String> pieces;
	private boolean scannerInString = false;
	private 	boolean scannerEscaping = false;
	private int scannerPiece = 0;
	//int scannerPos = 0;
	private boolean firstElemFound = false;
	private boolean topElemIsObj = false;
	private boolean scannerError = false;
	private int objDepth = 0;
	private int arrayDepth = 0;
	private boolean topElemIsComplete = false;

	public JSONParser() {
		pieces = new ArrayList<>();
	}

	public void addPiece(String piece) {
		pieces.add(piece);
	}

	public boolean scanningError() {
		return scannerError;
	}

	public boolean isObject() {
		return topElemIsObj;
	}

	// Scan all the existing pieces and see if the current element is possibly complete
	// The simple parser does not support partial elements. Attempting to parse incomplete data
	// will return a parse error
	public boolean isElementComplete() {
		if (scannerError) return false;
		if (topElemIsComplete) return true;

		int piecePos = 0;
		while(true) {
			if (scannerPiece >= pieces.size()) {
				break;
			}
			String curPiece = pieces.get(scannerPiece);
			if (piecePos >= curPiece.length()) {
				piecePos = 0;
				scannerPiece++;
				continue;
			}

			char scannedChar = curPiece.charAt(piecePos);
			piecePos++;

			if (scannerEscaping) {
				// TODO: be more selecting of the escape logic
				// All we currently care about is escaped quotes
				scannerEscaping = false;
				continue;
			}

			if (scannerInString) {
				if (scannedChar == '\\') {
					scannerEscaping = true;
					continue;
				}
				if (scannedChar == '"') {
					scannerInString = false;
					continue;
				}
				continue;
			}

			// Not in a string
			if (scannedChar == '"') {
				scannerInString = true;
				continue;
			}

			if (scannedChar == '{') {
				if (!firstElemFound) {
					firstElemFound = true;
					topElemIsObj = true;
				}
				objDepth++;
				continue;
			}
			if (scannedChar == '}') {
				if (objDepth <= 0) {
					scannerError = true;
					return false;
				}
				objDepth--;
				continue;
			}

			if (scannedChar == '[') {
				if (!firstElemFound) {
					firstElemFound = true;
					topElemIsObj = false;
				}
				arrayDepth++;
				continue;
			}
			if (scannedChar == ']') {
				if (arrayDepth <= 0) {
					scannerError = true;
					return false;
				}
				arrayDepth--;
				continue;
			}

		}

		if (objDepth == 0 && arrayDepth == 0 && firstElemFound) {
			topElemIsComplete = true;
			return true;
		}
		return false;
	}

	public Value parse() throws JSONError {
		// Build up a single string (this is not super efficient...)
		StringBuilder sb = new StringBuilder();
		for (String s: pieces) {
			sb.append(s);
		}
		String source = sb.toString();

		// Scan the listed pieces
		boolean isComp = isElementComplete();
		if (scannerError) {
			// The preliminary scanner detected a parse error
			throw new JSONError(source, -1, "Mismatched brackets detected");
		}
		if (!isComp) {
			// This element is not yet complete
			throw new JSONError(source, source.length(), "Incomplete JSON element");
		}
		ArrayList<JSONTokenizer.Token> toks = JSONTokenizer.tokenize(source);
		return JSONParser.parse(toks);

	}
}
