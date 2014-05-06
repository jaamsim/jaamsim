/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestParser {

@Test
public void testTokenize() {
	ArrayList<String> tok = new ArrayList<String>();

	// Test the basic delimiter handling, runs of [ ,\t] are one delimiter
	tok.clear();
	Parser.tokenize(tok, "A A,A\tA  A,,A\t\tA ,\tA");
	tokenMatch(tok, "A", "A,A", "A", "A,,A", "A", ",", "A");

	tok.clear();
	Parser.tokenize(tok, " A A,A\tA  A,,A\t\tA ,\tA\t,  ");
	tokenMatch(tok, "A", "A,A", "A", "A,,A", "A", ",", "A", ",");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ ARG}KEYWORD\t{ARG ARG,}");
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG", "}", "KEYWORD", "{", "ARG", "ARG,", "}");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ 'ARG  '}KEYWORD\t{ARG' ARG',}");
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG  ", "}", "KEYWORD", "{", "ARG", " ARG", ",", "}");

	tok.clear();
	Parser.tokenize(tok, "OBJECT KEYWORD{ ARG }\"FOO ,\t     ");
	tokenMatch(tok, "OBJECT", "KEYWORD", "{", "ARG", "}", "\"FOO ,\t     ");

	tok.clear();
	Parser.tokenize(tok, "'OBJECT''KEYWORD''   ");
	tokenMatch(tok, "OBJECT", "KEYWORD", "   ");
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
