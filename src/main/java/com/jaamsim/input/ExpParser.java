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

import java.util.ArrayList;

import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpParser {

	public interface UnOpFunc {
		public ExpResult apply(ParseContext context, ExpResult val);
	}

	public interface BinOpFunc {
		public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval);
	}

	public interface CallableFunc {
		public ExpResult call(ParseContext context, ExpResult[] args);
	}

	public static class UnitData {
		double scaleFactor;
		Class<? extends Unit> unitType;
	}

	public interface ParseContext {
		public ExpResult getVariableValue(String[] names);
		public UnitData getUnitByName(String name);
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a, Class<? extends Unit> b);
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num, Class<? extends Unit> denom);
	}

	private interface ExpressionWalker {
		public void visit(Expression exp);
		public Expression updateRef(Expression exp);
	}

	////////////////////////////////////////////////////////////////////
	// Expression types

	public abstract static class Expression {
		public ParseContext context;
		public abstract ExpResult evaluate();
		public Expression(ParseContext context) {
			this.context = context;
		}
		abstract void walk(ExpressionWalker w);
	}

	private static class Constant extends Expression {
		public ExpResult val;
		public Constant(ParseContext context, ExpResult val) {
			super(context);
			this.val = val;
		}
		@Override
		public ExpResult evaluate() {
			return val;
		}
		@Override
		void walk(ExpressionWalker w) {
			w.visit(this);
		}
	}

	public static class Variable extends Expression {
		private String[] vals;
		public Variable(ParseContext context, String[] vals) {
			super(context);
			this.vals = vals;
		}
		@Override
		public ExpResult evaluate() {
			return context.getVariableValue(vals);
		}
		@Override
		void walk(ExpressionWalker w) {
			w.visit(this);
		}
	}

	private static class UnaryOp extends Expression {
		public Expression subExp;
		private UnOpFunc func;
		UnaryOp(ParseContext context, Expression subExp, UnOpFunc func) {
			super(context);
			this.subExp = subExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate() {
			return func.apply(context, subExp.evaluate());
		}
		@Override
		void walk(ExpressionWalker w) {
			subExp.walk(w);

			subExp = w.updateRef(subExp);

			w.visit(this);
		}
	}

	private static class BinaryOp extends Expression {
		public Expression lSubExp;
		public Expression rSubExp;
		public ExpResult lConstVal;
		public ExpResult rConstVal;

		private final BinOpFunc func;
		BinaryOp(ParseContext context, Expression lSubExp, Expression rSubExp, BinOpFunc func) {
			super(context);
			this.lSubExp = lSubExp;
			this.rSubExp = rSubExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate() {
			ExpResult lRes = lConstVal != null ? lConstVal : lSubExp.evaluate();
			ExpResult rRes = rConstVal != null ? rConstVal : rSubExp.evaluate();
			return func.apply(context, lRes, rRes);
		}

		@Override
		void walk(ExpressionWalker w) {
			lSubExp.walk(w);
			rSubExp.walk(w);

			lSubExp = w.updateRef(lSubExp);
			rSubExp = w.updateRef(rSubExp);

			w.visit(this);
		}
	}

	public static class Conditional extends Expression {
		private Expression condExp;
		private Expression trueExp;
		private Expression falseExp;
		private ExpResult constCondRes;
		private ExpResult constTrueRes;
		private ExpResult constFalseRes;
		public Conditional(ParseContext context, Expression c, Expression t, Expression f) {
			super(context);
			condExp = c;
			trueExp = t;
			falseExp =f;
		}
		@Override
		public ExpResult evaluate() {
			ExpResult condRes = constCondRes != null ? constCondRes : condExp.evaluate();
			if (condRes.value == 0)
				return constFalseRes != null ? constFalseRes : falseExp.evaluate();
			else
				return constTrueRes != null ? constTrueRes : trueExp.evaluate();
		}
		@Override
		void walk(ExpressionWalker w) {
			condExp.walk(w);
			trueExp.walk(w);
			falseExp.walk(w);

			condExp = w.updateRef(condExp);
			trueExp = w.updateRef(trueExp);
			falseExp = w.updateRef(falseExp);

			w.visit(this);
		}
	}

	public static class FuncCall extends Expression {
		private ArrayList<Expression> args;
		private ArrayList<ExpResult> constResults;
		private CallableFunc function;
		public FuncCall(ParseContext context, CallableFunc function, ArrayList<Expression> args) {
			super(context);
			this.function = function;
			this.args = args;
			constResults = new ArrayList<ExpResult>(args.size());
			for (int i = 0; i < args.size(); ++i) {
				constResults.add(null);
			}
		}

		@Override
		public ExpResult evaluate() {
			ExpResult[] argVals = new ExpResult[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				ExpResult constArg = constResults.get(i);
				argVals[i] = constArg != null ? constArg : args.get(i).evaluate();
			}
			return function.call(context, argVals);
		}
		@Override
		void walk(ExpressionWalker w) {
			for (int i = 0; i < args.size(); ++i) {
				args.get(i).walk(w);
			}

			for (int i = 0; i < args.size(); ++i) {
				args.set(i, w.updateRef(args.get(i)));
			}

			w.visit(this);
		}
	}

	public static class Assignment {
		public String[] destination;
		public Expression value;
	}

	///////////////////////////////////////////////////////////
	// Entries for user definable operators and functions

	private static class UnaryOpEntry {
		public String symbol;
		public UnOpFunc function;
		public double bindingPower;
	}

	private static class BinaryOpEntry {
		public String symbol;
		public BinOpFunc function;
		public double bindingPower;
		public boolean rAssoc;
	}

	private static class FunctionEntry {
		public String name;
		public CallableFunc function;
		public int numArgs;
	}

	private static ArrayList<UnaryOpEntry> unaryOps = new ArrayList<UnaryOpEntry>();
	private static ArrayList<BinaryOpEntry> binaryOps = new ArrayList<BinaryOpEntry>();
	private static ArrayList<FunctionEntry> functions = new ArrayList<FunctionEntry>();

	private static void addUnaryOp(String symbol, double bindPower, UnOpFunc func) {
		UnaryOpEntry oe = new UnaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		oe.bindingPower = bindPower;
		unaryOps.add(oe);
	}

	private static void addBinaryOp(String symbol, double bindPower, boolean rAssoc, BinOpFunc func) {
		BinaryOpEntry oe = new BinaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		oe.bindingPower = bindPower;
		oe.rAssoc = rAssoc;
		binaryOps.add(oe);
	}

	private static void addFunction(String name, int numArgs, CallableFunc func) {
		FunctionEntry fe = new FunctionEntry();
		fe.name = name;
		fe.function = func;
		fe.numArgs = numArgs;
		functions.add(fe);
	}

	private static UnaryOpEntry getUnaryOp(String symbol) {
		for (UnaryOpEntry oe: unaryOps) {
			if (oe.symbol.equals(symbol))
				return oe;
		}
		return null;
	}
	private static BinaryOpEntry getBinaryOp(String symbol) {
		for (BinaryOpEntry oe: binaryOps) {
			if (oe.symbol.equals(symbol))
				return oe;
		}
		return null;
	}

	private static FunctionEntry getFunctionEntry(String funcName) {
		for (FunctionEntry fe : functions) {
			if (fe.name.equals(funcName)){
				return fe;
			}
		}
		return null;
	}

	////////////////////////////////////////////////////////
	// Statically initialize the operators and functions

	static {

		///////////////////////////////////////////////////
		// Unary Operators
		addUnaryOp("-", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return new ExpResult(-val.value, val.unitType);
			}
		});

		addUnaryOp("+", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return new ExpResult(val.value, val.unitType);
			}
		});

		addUnaryOp("!", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return new ExpResult(val.value == 0 ? 1 : 0, DimensionlessUnit.class);
			}
		});

		///////////////////////////////////////////////////
		// Binary operators
		addBinaryOp("+", 20, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value + rval.value, lval.unitType);
			}
		});

		addBinaryOp("-", 20, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value - rval.value, lval.unitType);
			}
		});

		addBinaryOp("*", 30, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value * rval.value, newType);
			}
		});

		addBinaryOp("/", 30, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value / rval.value, newType);
			}
		});

		addBinaryOp("^", 40, true, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != DimensionlessUnit.class ||
				    rval.unitType != DimensionlessUnit.class) {

					return ExpResult.BAD_RESULT;
				}

				return new ExpResult(Math.pow(lval.value, rval.value), DimensionlessUnit.class);
			}
		});

		addBinaryOp("==", 10, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value == rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("!=", 10, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value != rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("&&", 8, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				return new ExpResult((lval.value!=0) && (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("||", 6, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				return new ExpResult((lval.value!=0) || (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("<", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value < rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("<=", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value <= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp(">", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value > rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp(">=", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval){
				if (lval.unitType != rval.unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(lval.value >= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		////////////////////////////////////////////////////
		// Functions
		addFunction("max", 2, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args) {
				if (args[0].unitType != args[1].unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(Math.max(args[0].value, args[1].value), args[0].unitType);
			}
		});

		addFunction("min", 2, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args) {
				if (args[0].unitType != args[1].unitType) {
					return ExpResult.BAD_RESULT;
				}
				return new ExpResult(Math.min(args[0].value, args[1].value), args[0].unitType);
			}
		});

		addFunction("abs", 1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args) {
				return new ExpResult(Math.abs(args[0].value), args[0].unitType);
			}
		});

	}

	public static class Error extends Exception {
		Error(String err) {
			super(err);
		}
	}

	/**
	 * A utility class to make dealing with a list of tokens easier
	 *
	 */
	private static class TokenList {
		ArrayList<ExpTokenizer.Token> tokens;
		int pos;

		TokenList(ArrayList<ExpTokenizer.Token> tokens) {
			this.tokens = tokens;
			this.pos = 0;
		}

		public void expect(int type, String val) throws Error {
			if (pos == tokens.size()) {
				throw new Error(String.format("Expected \"%s\", past the end of input", val));
			}

			ExpTokenizer.Token nextTok = tokens.get(pos);

			if (nextTok.type != type || !nextTok.value.equals(val)) {
				throw new Error(String.format("Expected \"%s\", got \"%s\" at position %d", val, nextTok.value, nextTok.pos));
			}
			pos++;
		}

		public ExpTokenizer.Token next() {
			if (pos >= tokens.size()) {
				return null;
			}
			return tokens.get(pos++);
		}

		public ExpTokenizer.Token peek() {
			if (pos >= tokens.size()) {
				return null;
			}
			return tokens.get(pos);
		}
	}

	private static class ConstOptimizer implements ExpressionWalker {

		@Override
		public void visit(Expression exp) {
			if (exp instanceof BinaryOp) {
				BinaryOp bo = (BinaryOp)exp;
				if (bo.lSubExp instanceof Constant) {
					// Just the left is a constant, store it in the binop
					bo.lConstVal = bo.lSubExp.evaluate();
				}
				if (bo.rSubExp instanceof Constant) {
					// Just the right is a constant, store it in the binop
					bo.rConstVal = bo.rSubExp.evaluate();
				}
			}
			if (exp instanceof Conditional) {
				Conditional cond = (Conditional)exp;
				if (cond.condExp instanceof Constant) {
					cond.constCondRes = cond.condExp.evaluate();
				}
				if (cond.trueExp instanceof Constant) {
					cond.constTrueRes = cond.trueExp.evaluate();
				}
				if (cond.falseExp instanceof Constant) {
					cond.constFalseRes = cond.falseExp.evaluate();
				}
			}
			if (exp instanceof FuncCall) {
				FuncCall fc = (FuncCall)exp;
				for (int i = 0; i < fc.args.size(); ++i) {
					if (fc.args.get(i) instanceof Constant) {
						fc.constResults.set(i, fc.args.get(i).evaluate());
					}
				}
			}
		}

		/**
		 * Give a node a chance to swap itself out with a different subtree.
		 */
		@Override
		public Expression updateRef(Expression exp) {
			if (exp instanceof UnaryOp) {
				UnaryOp uo = (UnaryOp)exp;
				if (uo.subExp instanceof Constant) {
					// This is an unary operation on a constant, we can replace it with a constant
					ExpResult val = uo.evaluate();
					return new Constant(uo.context, val);
				}
			}
			if (exp instanceof BinaryOp) {
				BinaryOp bo = (BinaryOp)exp;
				if ((bo.lSubExp instanceof Constant) && (bo.rSubExp instanceof Constant)) {
					// both sub expressions are constants, so replace the binop with a constant
					ExpResult val = bo.evaluate();
					return new Constant(bo.context, val);
				}
			}
			return exp;
		}
	}

	private static ConstOptimizer CONST_OP = new ConstOptimizer();

	/**
	 * The main entry point to the expression parsing system, will either return a valid
	 * expression that can be evaluated, or throw an error.
	 */
	public static Expression parseExpression(ParseContext context, String input) throws Error {
		ArrayList<ExpTokenizer.Token> ts;
		try {
			ts = ExpTokenizer.tokenize(input);
		} catch (ExpTokenizer.Error ex){
			throw new Error(ex.getMessage());
		}

		TokenList tokens = new TokenList(ts);

		Expression exp = parseExp(context, tokens, 0);

		// Make sure we've parsed all the tokens
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked != null) {
			throw new Error(String.format("Unexpected additional values at position: %d", peeked.pos));
		}

		exp.walk(CONST_OP);
		exp = CONST_OP.updateRef(exp); // Finally, give the entire expression a chance to optimize itself into a constant
		return exp;
	}

	private static Expression parseExp(ParseContext context, TokenList tokens, double bindPower) throws Error {
		Expression lhs = parseOpeningExp(context, tokens, bindPower);
		// Now peek for a binary op to modify this expression

		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE) {
				break;
			}
			BinaryOpEntry binOp = getBinaryOp(peeked.value);
			if (binOp != null && binOp.bindingPower > bindPower) {
				// The next token is a binary op and powerful enough to bind us
				lhs = handleBinOp(context, tokens, lhs, binOp);
				continue;
			}
			// Specific check for binding the conditional (?:) operator
			if (peeked.value.equals("?") && bindPower == 0) {
				lhs = handleConditional(context, tokens, lhs);
				continue;
			}
			break;
		}

		// We have bound as many operators as we can, return it
		return lhs;
	}

	private static Expression handleBinOp(ParseContext context, TokenList tokens, Expression lhs, BinaryOpEntry binOp) throws Error {
		tokens.next(); // Consume the operator

		// For right associative operators, we weaken the binding power a bit at application time (but not testing time)
		double assocMod = binOp.rAssoc ? -0.5 : 0;
		Expression rhs = parseExp(context, tokens, binOp.bindingPower + assocMod);
		//currentPower = oe.bindingPower;

		return new BinaryOp(context, lhs, rhs, binOp.function);
	}

	private static Expression handleConditional(ParseContext context, TokenList tokens, Expression lhs) throws Error {
		tokens.next(); // Consume the '?'

		Expression trueExp = parseExp(context, tokens, 0);

		tokens.expect(ExpTokenizer.SYM_TYPE, ":");

		Expression falseExp = parseExp(context, tokens , 0);

		return new Conditional(context, lhs, trueExp, falseExp);
	}

	public static Assignment parseAssignment(ParseContext context, String input) throws Error {
		ArrayList<ExpTokenizer.Token> ts;
		try {
			ts = ExpTokenizer.tokenize(input);
		} catch (ExpTokenizer.Error ex){
			throw new Error(ex.getMessage());
		}

		TokenList tokens = new TokenList(ts);

		ExpTokenizer.Token nextTok = tokens.next();
		if (nextTok == null || (nextTok.type != ExpTokenizer.SQ_TYPE &&
			                    !nextTok.value.equals("this") &&
			                    !nextTok.value.equals("obj"))) {
			throw new Error("Assignments must start with an identifier");
		}
		ArrayList<String> destination = parseIdentifier(nextTok, tokens);

		nextTok = tokens.next();
		if (nextTok == null || nextTok.type != ExpTokenizer.SYM_TYPE || !nextTok.value.equals("=")) {
			throw new Error("Expected '=' in assignment");
		}

		Expression exp = parseExp(context, tokens, 0);

		Assignment ret = new Assignment();
		ret.destination = destination.toArray(STRING_ARRAY_TYPE);
		ret.value = exp;
		return ret;
	}

	// Static array to make ArrayList.toArray work
	private static final String[] STRING_ARRAY_TYPE = new String[0];

	// The first half of expression parsing, parse a simple expression based on the next token
	private static Expression parseOpeningExp(ParseContext context, TokenList tokens, double bindPower) throws Error{
		ExpTokenizer.Token nextTok = tokens.next(); // consume the first token

		if (nextTok == null) {
			throw new Error("Unexpected end of string");
		}

		if (nextTok.type == ExpTokenizer.NUM_TYPE) {
			return parseConstant(context, nextTok.value, tokens);
		}
		if (nextTok.type == ExpTokenizer.VAR_TYPE &&
				!nextTok.value.equals("this") &&
				!nextTok.value.equals("obj")) {
			return parseFuncCall(context, nextTok.value, tokens);
		}
		if (nextTok.type == ExpTokenizer.SQ_TYPE ||
				nextTok.value.equals("this") ||
				nextTok.value.equals("obj")) {
			ArrayList<String> vals = parseIdentifier(nextTok, tokens);
			return new Variable(context, vals.toArray(STRING_ARRAY_TYPE));
		}

		// The next token must be a symbol

		// handle parenthesis
		if (nextTok.value.equals("(")) {
			Expression exp = parseExp(context, tokens, 0);
			tokens.expect(ExpTokenizer.SYM_TYPE, ")"); // Expect the closing paren
			return exp;
		}

		UnaryOpEntry oe = getUnaryOp(nextTok.value);
		if (oe != null) {
			Expression exp = parseExp(context, tokens, oe.bindingPower);
			return new UnaryOp(context, exp, oe.function);
		}

		// We're all out of tricks here, this is an unknown expression
		throw new Error(String.format("Can not parse expression at %d", nextTok.pos));
	}

	private static Expression parseConstant(ParseContext context, String constant, TokenList tokens) throws Error {
		double mult = 1;
		Class<? extends Unit> ut = DimensionlessUnit.class;

		ExpTokenizer.Token peeked = tokens.peek();

		if (peeked != null && peeked.type == ExpTokenizer.SQ_TYPE) {
			// This constant is followed by a square quoted token, it must be the unit

			tokens.next(); // Consume unit token

			UnitData unit = context.getUnitByName(peeked.value);
			if (unit == null) {
				throw new Error(String.format("Unknown unit: %s", peeked.value));
			}
			mult = unit.scaleFactor;
			ut = unit.unitType;
		}

		return new Constant(context, new ExpResult(Double.parseDouble(constant)*mult, ut));
	}


	private static Expression parseFuncCall(ParseContext context, String funcName, TokenList tokens) throws Error {

		tokens.expect(ExpTokenizer.SYM_TYPE, "(");
		ArrayList<Expression> arguments = new ArrayList<Expression>();

		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked == null) {
			throw new Error("Unexpected end of input in argument list");
		}
		boolean isEmpty = false;
		if (peeked.value.equals(")")) {
			// Special case with empty argument list
			isEmpty = true;
			tokens.next(); // Consume closing parens
		}

		while (!isEmpty) {
			Expression nextArg = parseExp(context, tokens, 0);
			arguments.add(nextArg);

			ExpTokenizer.Token nextTok = tokens.next();
			if (nextTok == null) {
				throw new Error("Unexpected end of input in argument list.");
			}
			if (nextTok.value.equals(")")) {
				break;
			}

			if (nextTok.value.equals(",")) {
				continue;
			}

			// Unexpected token
			throw new Error(String.format("Unexpected token in arguement list at: %d", nextTok.pos));
		}

		FunctionEntry fe = getFunctionEntry(funcName);
		if (fe == null) {
			throw new Error(String.format("Uknown function: \"%s\"", funcName));
		}

		if (fe.numArgs != arguments.size()){
			throw new Error(String.format("Function \"%s\" expects %d arguments. %d provided.",
					funcName, fe.numArgs, arguments.size()));
		}
		return new FuncCall(context, fe.function, arguments);
	}

	private static ArrayList<String> parseIdentifier(ExpTokenizer.Token firstName, TokenList tokens) throws Error {
		ArrayList<String> vals = new ArrayList<String>();
		vals.add(firstName.value.intern());
		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE || !peeked.value.equals(".")) {
				break;
			}
			// Next token is a '.' so parse another name

			tokens.next(); // consume
			ExpTokenizer.Token nextName = tokens.next();
			if (nextName == null || nextName.type != ExpTokenizer.VAR_TYPE) {
				throw new Error(String.format("Expected Identifier after '.' at pos: %d", peeked.pos));
			}

			vals.add(nextName.value.intern());
		}

		return vals;
	}
}
