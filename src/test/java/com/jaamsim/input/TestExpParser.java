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

import com.jaamsim.input.ExpParser.UnitData;
import com.jaamsim.units.AreaUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.SpeedUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;

public class TestExpParser {

	private static class PC implements ExpParser.ParseContext {
		@Override
		public UnitData getUnitByName(String name) {
			return null;
		}
		@Override
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a,
				Class<? extends Unit> b) {
			return DimensionlessUnit.class;
		}
		@Override
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num,
				Class<? extends Unit> denom) {
			return DimensionlessUnit.class;
		}
	}

	static PC pc = new PC();


	private static void testToken(ExpTokenizer.Token tok, int type, String val) {
		assertTrue(tok.type == type);
		assertTrue(tok.value.equals(val));
	}

	@Test
	public void testTokenize() throws ExpError {
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

		tokens = ExpTokenizer.tokenize("[123][abc]   [+-  2]");
		assertTrue(tokens.size() == 3);
		testToken(tokens.get(0), ExpTokenizer.SQ_TYPE, "123");
		testToken(tokens.get(1), ExpTokenizer.SQ_TYPE, "abc");
		testToken(tokens.get(2), ExpTokenizer.SQ_TYPE, "+-  2");

		// Test long symbol parsing
		tokens = ExpTokenizer.tokenize("&&||==<==&|");
		assertTrue(tokens.size() == 7);
		testToken(tokens.get(0), ExpTokenizer.SYM_TYPE, "&&");
		testToken(tokens.get(1), ExpTokenizer.SYM_TYPE, "||");
		testToken(tokens.get(2), ExpTokenizer.SYM_TYPE, "==");
		testToken(tokens.get(3), ExpTokenizer.SYM_TYPE, "<=");
		testToken(tokens.get(4), ExpTokenizer.SYM_TYPE, "=");
		testToken(tokens.get(5), ExpTokenizer.SYM_TYPE, "&");
		testToken(tokens.get(6), ExpTokenizer.SYM_TYPE, "|");

	}

	@Test
	public void testParser() throws ExpError {
		class EC implements ExpParser.EvalContext {
			@Override
			public ExpResult getVariableValue(String[] name) {
				if (name[0].equals("foo")) return new ExpResult(4, DimensionlessUnit.class);
				if (name[0].equals("bar")) return new ExpResult(3, DimensionlessUnit.class);
				return new ExpResult(1, DimensionlessUnit.class);
			}
			@Override
			public boolean eagerEval() { return true; }
		}
		EC ec = new EC();

		ExpParser.Expression exp = ExpParser.parseExpression(pc, "2*5 + 3*5*(3-1)+2");
		double val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "max(3, 42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "min(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == -5);

		exp = ExpParser.parseExpression(pc, "max(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 602);

		exp = ExpParser.parseExpression(pc, "indexOfMin(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 3);

		exp = ExpParser.parseExpression(pc, "indexOfMax(3, 42, -5, 602)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "abs(-42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "abs(+42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

		exp = ExpParser.parseExpression(pc, "[foo]*[bar]");
		val = exp.evaluate(ec).value;
		assertTrue(val == 12);

		exp = ExpParser.parseExpression(pc, "50/2/5"); // left associative
		val = exp.evaluate(ec).value;
		assertTrue(val == 5);

		exp = ExpParser.parseExpression(pc, "2^2^3"); // right associative
		val = exp.evaluate(ec).value;
		assertTrue(val == 256);

		exp = ExpParser.parseExpression(pc, "1 + 2^2*4 + 2*[foo]");
		val = exp.evaluate(ec).value;
		assertTrue(val == 25);

		exp = ExpParser.parseExpression(pc, "1 + 2^(2*4) + 2");
		val = exp.evaluate(ec).value;
		assertTrue(val == 259);

		exp = ExpParser.parseExpression(pc, "2----2"); // A quadruple negative
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "2---+-2"); // Still a quadruple negative
		val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "-1+1");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "(((((1+1)))*5))");
		val = exp.evaluate(ec).value;
		assertTrue(val == 10);

		exp = ExpParser.parseExpression(pc, "!42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "!0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 == 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 == 41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 != 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 != 41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42 || 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "0 || 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "0 || 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42 && 0");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "0 && 42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "1 && 2");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "!(1&&42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "!!(1&&42)");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);

		exp = ExpParser.parseExpression(pc, "42<41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "41<42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "41>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42<=41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "41<=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>=41");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "41>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "42>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>=42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 1);
		exp = ExpParser.parseExpression(pc, "42>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);
		exp = ExpParser.parseExpression(pc, "42>42");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "1==0?42:24");
		val = exp.evaluate(ec).value;
		assertTrue(val == 24);
		exp = ExpParser.parseExpression(pc, "1==1?42:24");
		val = exp.evaluate(ec).value;
		assertTrue(val == 42);

	}

	@Test
	public void testVariables() throws ExpError {
		class EC implements ExpParser.EvalContext {
			@Override
			public ExpResult getVariableValue(String[] name) {
				if (name.length < 1 || !name[0].equals("foo")) return new ExpResult(0, DimensionlessUnit.class);

				if (name.length >= 3 && name[1].equals("bar") && name[2].equals("baz")) return new ExpResult(4, DimensionlessUnit.class);
				if (name.length >= 2 && name[1].equals("bonk")) return new ExpResult(5, DimensionlessUnit.class);

				return new ExpResult(-1, DimensionlessUnit.class);
			}
			@Override
			public boolean eagerEval() { return true; }
		}
		EC ec = new EC();

		ExpParser.Expression exp = ExpParser.parseExpression(pc, "[foo].bar.baz");
		double val = exp.evaluate(ec).value;
		assertTrue(val == 4);

		exp = ExpParser.parseExpression(pc, "[foo].bar.baz*4");
		val = exp.evaluate(ec).value;
		assertTrue(val == 16);

		exp = ExpParser.parseExpression(pc, "[foo].bonk");
		val = exp.evaluate(ec).value;
		assertTrue(val == 5);

		exp = ExpParser.parseExpression(pc, "[bob].is.your.uncle");
		val = exp.evaluate(ec).value;
		assertTrue(val == 0);

		exp = ExpParser.parseExpression(pc, "[foo]");
		val = exp.evaluate(ec).value;
		assertTrue(val == -1);

		class ThisEC implements ExpParser.EvalContext {
			@Override
			public ExpResult getVariableValue(String[] name) {
				if (name[0].equals("this")) return new ExpResult(42, DimensionlessUnit.class);

				return new ExpResult(-1, DimensionlessUnit.class);
			}
			@Override
			public boolean eagerEval() { return true; }
		}
		ThisEC tec = new ThisEC();

		exp = ExpParser.parseExpression(pc, "this.stuff");
		val = exp.evaluate(tec).value;
		assertTrue(val == 42);
	}

	@Test
	public void testUnits() throws ExpError {
		class UnitPC implements ExpParser.ParseContext {
			@Override
			public UnitData getUnitByName(String name) {
				UnitData ret = new UnitData();
				if (name.equals("s")) {
					ret.scaleFactor = 1;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("min")) {
					ret.scaleFactor = 60;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("hr")) {
					ret.scaleFactor = 3600;
					ret.unitType = TimeUnit.class;
					return ret;
				}
				if (name.equals("m")) {
					ret.scaleFactor = 1;
					ret.unitType = DistanceUnit.class;
					return ret;
				}
				if (name.equals("km")) {
					ret.scaleFactor = 1000;
					ret.unitType = DistanceUnit.class;
					return ret;
				}
				return null;
			}
			@Override
			public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a,
					Class<? extends Unit> b) {
				return Unit.getMultUnitType(a, b);
			}

			@Override
			public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num,
					Class<? extends Unit> denom) {
				return Unit.getDivUnitType(num, denom);
			}

		}
		UnitPC upc = new UnitPC();

		class EC implements ExpParser.EvalContext {
			@Override
			public ExpResult getVariableValue(String[] name) throws ExpError {
				throw new ExpError(null, 0, "Variables not supported in test");
			}
			@Override
			public boolean eagerEval() { return true; }
		}
		EC ec = new EC();

		ExpParser.Expression exp = ExpParser.parseExpression(upc, "1[km] + 1[m]");
		ExpResult res = exp.evaluate(ec);
		assertTrue(res.value == 1001);
		assertTrue(res.unitType == DistanceUnit.class);

		exp = ExpParser.parseExpression(upc, "1[hr] + 1[min] + 5[s]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 3665);
		assertTrue(res.unitType == TimeUnit.class);

		boolean threw = false;
		try {
			exp = ExpParser.parseExpression(upc, "1[hr] + 1[m]");
			res = exp.evaluate(ec);
			assertTrue(false);
		} catch (ExpError ex) {
			threw = true;
		}
		assertTrue(threw);

		exp = ExpParser.parseExpression(upc, "max(1[hr], 1[s])");
		res = exp.evaluate(ec);
		assertTrue(res.value == 3600);
		assertTrue(res.unitType == TimeUnit.class);

		exp = ExpParser.parseExpression(upc, "6*1[km]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 6000);
		assertTrue(res.unitType == DistanceUnit.class);

		threw = false;
		try {
			exp = ExpParser.parseExpression(upc, "1[parsec]");
		} catch(ExpError ex) {
			threw = true;
		}
		assertTrue(threw);

		exp = ExpParser.parseExpression(upc, "1[m]/1[s]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 1);
		assertTrue(res.unitType == SpeedUnit.class);

		exp = ExpParser.parseExpression(upc, "5[m]*1[km]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 5000);
		assertTrue(res.unitType == AreaUnit.class);

		exp = ExpParser.parseExpression(upc, "5[m]/1[m]");
		res = exp.evaluate(ec);
		assertTrue(res.value == 5);
		assertTrue(res.unitType == DimensionlessUnit.class);

	}

	@Test
	public void testAssignment() throws ExpError {

		class EC implements ExpParser.EvalContext {
			@Override
			public ExpResult getVariableValue(String[] name) {
				return new ExpResult(-1, DimensionlessUnit.class);
			}
			@Override
			public boolean eagerEval() { return true; }
		}
		EC ec = new EC();

		ExpParser.Assignment assign = ExpParser.parseAssignment(pc, "[foo].bar = 40 + 2");

		assertTrue(assign.destination.length == 2);
		assertTrue(assign.destination[0].equals("foo"));
		assertTrue(assign.destination[1].equals("bar"));
		assertTrue(assign.value.evaluate(ec).value == 42);

		assign = ExpParser.parseAssignment(pc, "this.bar = 40 + 2");
		assertTrue(assign.destination.length == 2);
		assertTrue(assign.destination[0].equals("this"));
		assertTrue(assign.destination[1].equals("bar"));
		assertTrue(assign.value.evaluate(ec).value == 42);

	}
}
