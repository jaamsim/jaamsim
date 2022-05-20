package com.jaamsim.JSON;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

import com.jaamsim.input.ExpCollections;
import com.jaamsim.input.ExpResType;
import com.jaamsim.input.ExpResult;

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
public class TestJSON {

	@Test
	public void testTokenize() throws Throwable {
		String test = "[[  	]]{\"woot\\\"\\n\" -42e+10[] true false null";
		ArrayList<JSONTokenizer.Token> toks =  JSONTokenizer.tokenize(test);

		assertTrue(toks.size() == 12);

		JSONTokenizer.Token stringTok = toks.get(5);
		assertTrue(stringTok.type == JSONTokenizer.STRING_TYPE);
		assertTrue(stringTok.value.equals("woot\"\n"));

		JSONTokenizer.Token numTok = toks.get(6);
		assertTrue(numTok.type == JSONTokenizer.NUM_TYPE);
		assertTrue(numTok.value.equals("-42e+10"));

		JSONTokenizer.Token nullTok = toks.get(11);
		assertTrue(nullTok.type == JSONTokenizer.KEYWORD_TYPE);
		assertTrue(nullTok.value.equals("null"));
	}

	@Test
	public void testStaticParse() throws Throwable {
		String testList = "[1, 2, 3, 4, \"foo\"]";
		ArrayList<JSONTokenizer.Token> listToks =  JSONTokenizer.tokenize(testList);

		JSONParser.Value listVal = JSONParser.parse(listToks);

		assertTrue(listVal.isList());
		assertTrue(listVal.listVal.size() == 5);

		assertTrue(listVal.listVal.get(1).numVal == 2.0);
		assertTrue(listVal.listVal.get(4).stringVal.equals("foo"));

		String testMap = "{\"foo\" : 1, \"bar\": 2 }";
		ArrayList<JSONTokenizer.Token> mapToks =  JSONTokenizer.tokenize(testMap);

		JSONParser.Value mapVal = JSONParser.parse(mapToks);

		assertTrue(mapVal.isMap());
		assertTrue(mapVal.mapVal.size() == 2);


		String testNested = "[{\"foo\":[1,2,3],\"bar\":42.24},[42,24,\"}}]]\"]]";
		ArrayList<JSONTokenizer.Token> nestedToks =  JSONTokenizer.tokenize(testNested);

		JSONParser.Value nestedVal = JSONParser.parse(nestedToks);

		assertTrue(nestedVal.isList());
	}

	@Test
	public void testMultiplePieces() throws Throwable {

		String str1 = "[{\"foo\":[1,2,3],\"ba";
		String str2 = "r\":42.24},[42,24, \"}}\\\"]]\"]]";
		JSONParser parser = new JSONParser();
		parser.addPiece(str1);
		assertTrue(!parser.isElementComplete());
		assertTrue(!parser.scanningError());
		parser.addPiece(str2);
		assertTrue(parser.isElementComplete());
		assertTrue(!parser.scanningError());

		JSONParser.Value val = parser.parse();

		assertTrue(val.isList());

	}

	@Test
	public void testWriteJSON() throws Throwable {
		JSONParser.Value mapVal = new JSONParser.Value();
		mapVal.mapVal = new HashMap<>();
		mapVal.mapVal.put("foo", JSONParser.Value.makeStringVal("fooey"));
		mapVal.mapVal.put("bar", JSONParser.Value.makeStringVal("baz"));
		mapVal.mapVal.put("num", JSONParser.Value.makeNumVal(42));
		mapVal.mapVal.put("truey", JSONParser.Value.makeTrueVal());

		String messyString = " \t\b\r\f \"Quote\" \\Slashquote\\ \n ";
		char shiftOut = 0xf; // And one random control character
		messyString = messyString + shiftOut;
		mapVal.mapVal.put("\"messy\"", JSONParser.Value.makeStringVal(messyString));

		String mapRes = JSONWriter.writeJSONValue(mapVal);
		assertTrue(mapRes.contains("\"foo\": \"fooey\""));
		assertTrue(mapRes.contains("\"bar\": \"baz\""));
		assertTrue(mapRes.contains("\"bar\": \"baz\""));
		assertTrue(mapRes.contains("\"num\": 42.0"));
		assertTrue(mapRes.contains("\"truey\": true"));
		assertTrue(mapRes.contains("\"\\\"messy\\\"\": \" \\t\\b\\r\\f \\\"Quote\\\" \\\\Slashquote\\\\ \\n \\u000f\""));
		assertTrue(mapRes.startsWith("{"));
		assertTrue(mapRes.endsWith("}"));

		JSONParser.Value listVal = new JSONParser.Value();
		listVal.listVal = new ArrayList<>();

		listVal.listVal.add(JSONParser.Value.makeNumVal(42));
		listVal.listVal.add(JSONParser.Value.makeNumVal(24));
		listVal.listVal.add(JSONParser.Value.makeStringVal("foo"));
		listVal.listVal.add(JSONParser.Value.makeFalseVal());
		listVal.listVal.add(JSONParser.Value.makeNullVal());

		String listRes = JSONWriter.writeJSONValue(listVal);
		assertTrue(listRes.equals("[42.0, 24.0, \"foo\", false, null]"));

		JSONParser.Value topMap = new JSONParser.Value();
		topMap.mapVal = new HashMap<>();
		topMap.mapVal.put("mappy", mapVal);
		topMap.mapVal.put("listy", listVal);

		String topRes = JSONWriter.writeJSONValue(topMap);

		assertTrue(topRes.startsWith("{"));
		assertTrue(topRes.endsWith("}"));
		assertTrue(topRes.contains("\"listy\": [42.0, 24.0, \"foo\", false, null]"));
		assertTrue(topRes.contains("\"mappy\": {"));
		assertTrue(topRes.contains("\"foo\": \"fooey\""));
		assertTrue(topRes.contains("\"bar\": \"baz\""));
		assertTrue(topRes.contains("\"num\": 42.0"));
		assertTrue(mapRes.contains("\"\\\"messy\\\"\": \" \\t\\b\\r\\f \\\"Quote\\\" \\\\Slashquote\\\\ \\n \\u000f\""));

		JSONParser.Value val = JSONParser.parse(topRes);

		assertTrue(val.isMap());
		assertTrue(val.mapVal.get("mappy").mapVal.get("num").numVal == 42);
		assertTrue(val.mapVal.get("listy").listVal.get(0).numVal == 42);
	}

	@Test
	public void testExpResultToJSON() throws Throwable {
		HashMap<String, ExpResult> map = new HashMap<>();
		map.put("foo", ExpResult.makeStringResult("fooey"));
		map.put("bar", ExpResult.makeStringResult("baz"));
		map.put("num", ExpResult.makeNumResult(42, null));

		ExpResult mapVal = ExpCollections.wrapCollection(map, null);

		JSONParser.Value convMap = JSONConverter.fromExpResult(mapVal);

		assertTrue(convMap.isMap());

		JSONParser.Value mapFoo = convMap.mapVal.get("foo");
		assertTrue(mapFoo.isString());
		assertTrue(mapFoo.stringVal.equals("fooey"));

		JSONParser.Value mapNum = convMap.mapVal.get("num");
		assertTrue(mapNum.isNumber());
		assertTrue(mapNum.numVal == 42);

		ArrayList<ExpResult> list = new ArrayList<>();
		list.add(ExpResult.makeNumResult(42, null));
		list.add(ExpResult.makeNumResult(24, null));
		list.add(ExpResult.makeStringResult("foo"));


		ExpResult arrayVal = ExpCollections.wrapCollection(list, null);

		JSONParser.Value convList = JSONConverter.fromExpResult(arrayVal);

		assertTrue(convList.isList());
		assertTrue(convList.listVal.get(0).isNumber());
		assertTrue(convList.listVal.get(0).numVal == 42);

		assertTrue(convList.listVal.get(2).isString());
		assertTrue(convList.listVal.get(2).stringVal.equals("foo"));

	}

	@Test
	public void testJSONToExpResult() throws Throwable {
		String testJSON = "{\"foo\":42, \"bar\": [\"baz\", \"batz\"]}";

		JSONParser.Value val = JSONParser.parse(testJSON);

		ExpResult res = JSONConverter.toExpResult(val);

		assertTrue(res.type == ExpResType.COLLECTION);
		ExpResult answer = res.colVal.index(ExpResult.makeStringResult("foo"));
		assertTrue(answer.value == 42.0);

		ExpResult listRes = res.colVal.index(ExpResult.makeStringResult("bar"));

		ExpResult listFirst = listRes.colVal.index(ExpResult.makeNumResult(1, null));
		assertTrue(listFirst.stringVal.equals("baz"));

		ExpResult listSecond = listRes.colVal.index(ExpResult.makeNumResult(2, null));
		assertTrue(listSecond.stringVal.equals("batz"));
	}
}

