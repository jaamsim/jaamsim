/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestParser {

@Test
public void testTokenize() {
	ArrayList<String> tok = new ArrayList<>();

	// Test the basic delimiter handling, runs of [ ,\t] are one delimiter
	tok.clear();
	Parser.tokenize(tok, "A A,A\tA  A,,A\t\tA ,\tA", false);
	tokenMatch(tok, "A", "A,A", "A", "A,,A", "A", ",", "A");

	tok.clear();
	Parser.tokenize(tok, " A A,A\tA  A,,A\t\tA ,\tA\t,  ", false);
	tokenMatch(tok, "A", "A,A", "A", "A,,A", "A", ",", "A", ",");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ ARG}KEYWORD\t{ARG ARG,}", false);
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG", "}", "KEYWORD", "{", "ARG", "ARG,", "}");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ 'ARG  '}KEYWORD\t{ARG' ARG',}", false);
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG  ", "}", "KEYWORD", "{", "ARG", " ARG", ",", "}");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ ARG }#FOO ,\t     ", false);
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG", "}", "#FOO ,\t     ");

	tok.clear();
	Parser.tokenize(tok, "'OBJECT''KEYWORD''   ", false);
	tokenMatch(tok, "OBJECT", "KEYWORD", "   \n");

	tok.clear();
	Parser.tokenize(tok, "'OBJECT\"{#}''KEYWORD''   'a#bcd ", false);
	tokenMatch(tok, "OBJECT\"{#}", "KEYWORD", "   ", "a", "#bcd ");
}

private static void validateTokens(ArrayList<String> toks) {
	for (String each : toks) {
		// An empty String is not a valid token
		assertTrue(!each.isEmpty());

		// no further validation of line-quotes
		if (each.startsWith("\""))
			continue;

		// valid tokens connot contain one of the delimiters
		assertTrue(!each.contains("'"));

		// If a token contains { or } it must be one of those exactly
		if (each.equals("{"))
			assertTrue(each == "{");

		if (each.equals("}"))
			assertTrue(each == "}");
	}
}

private static void tokenMatch(ArrayList<String> toks, String... expected) {
	assertTrue(toks.size() == expected.length);
	validateTokens(toks);
	for (int i = 0; i < toks.size(); i++) {
		assertTrue(expected[i].equals(toks.get(i)));
	}
}

@Test
public void testQuoting() {
	assertTrue(Parser.needsQuoting("a "));
	assertTrue(Parser.needsQuoting("abraca}}dabra"));
	assertTrue(!Parser.needsQuoting("abracadabra"));
}

@Test
public void testQuoted() {
	assertTrue(Parser.isQuoted("'floob '"));
	assertTrue(Parser.isQuoted("' '"));
	assertTrue(Parser.isQuoted("''"));
	assertFalse(Parser.isQuoted("'"));
	assertFalse(Parser.isQuoted("'' "));
}
}
