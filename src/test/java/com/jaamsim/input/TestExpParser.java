/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

public class TestExpParser {

	private static void testToken(ExpTokenizer.Token tok, int type, String val) {
		assertTrue(tok.type == type);
		assertTrue(tok.value.equals(val));
	}

	@Test
	public void testTokenize() throws ExpTokenizer.Error {

		ArrayList<ExpTokenizer.Token> tokens = ExpTokenizer.tokenize(" a b c 1 2 3 + -");

		assertTrue(tokens.size() == 8);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "a");
		testToken(tokens.get(1), ExpTokenizer.VAR_TYPE, "b");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "c");

		testToken(tokens.get(3), ExpTokenizer.NUM_TYPE, "1");
		testToken(tokens.get(4), ExpTokenizer.NUM_TYPE, "2");
		testToken(tokens.get(5), ExpTokenizer.NUM_TYPE, "3");

		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "+");
		testToken(tokens.get(7), ExpTokenizer.SYM_TYPE, "-");

		tokens = ExpTokenizer.tokenize("foo bar blarg123");
		assertTrue(tokens.size() == 3);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "foo");
		testToken(tokens.get(1), ExpTokenizer.VAR_TYPE, "bar");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "blarg123");


		tokens = ExpTokenizer.tokenize("bar.frump ( -12.3)");
		assertTrue(tokens.size() == 7);
		testToken(tokens.get(0), ExpTokenizer.VAR_TYPE, "bar");
		testToken(tokens.get(1), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(2), ExpTokenizer.VAR_TYPE, "frump");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, "(");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(5), ExpTokenizer.NUM_TYPE, "12.3");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, ")");

		tokens = ExpTokenizer.tokenize("-12.3e6 ... ---");
		assertTrue(tokens.size() == 8);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(1), ExpTokenizer.NUM_TYPE, "12.3e6");
		testToken(tokens.get(2), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, ".");
		testToken(tokens.get(5), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(7), ExpTokenizer.SYM_TYPE, "-");

		tokens = ExpTokenizer.tokenize("-42.3E-6");
		assertTrue(tokens.size() == 2);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "-");
		testToken(tokens.get(1), ExpTokenizer.NUM_TYPE, "42.3E-6");

	}
}
