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

import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpParser {

	public interface UnOpFunc {
		public ExpResult apply(ParseContext context, ExpResult val) throws ExpError;
	}

	public interface BinOpFunc {
		public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError;
	}

	public interface CallableFunc {
		public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError;
	}

	public static class UnitData {
		double scaleFactor;
		Class<? extends Unit> unitType;
	}

	public interface ParseContext {
		public UnitData getUnitByName(String name);
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a, Class<? extends Unit> b);
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num, Class<? extends Unit> denom);
	}

	public interface EvalContext {
		public ExpResult getVariableValue(String[] names) throws ExpError;
		public boolean eagerEval();
	}

	private interface ExpressionWalker {
		public void visit(ExpNode exp) throws ExpError;
		public ExpNode updateRef(ExpNode exp) throws ExpError;
	}

	////////////////////////////////////////////////////////////////////
	// Expression types

	public static class Expression {
		public final String source;

		private final ExpNode rootNode;
		public Expression(ExpNode rootNode, String source) {
			this.source = source;
			this.rootNode = rootNode;
		}
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			return rootNode.evaluate(ec);
		}
	}

	private abstract static class ExpNode {
		public final ParseContext context;
		public final String source;
		public final int tokenPos;
		public abstract ExpResult evaluate(EvalContext ec) throws ExpError;
		public ExpNode(ParseContext context, String source, int pos) {
			this.context = context;
			this.tokenPos = pos;
			this.source = source;
		}
		abstract void walk(ExpressionWalker w) throws ExpError;
	}

	private static class Constant extends ExpNode {
		public ExpResult val;
		public Constant(ParseContext context, ExpResult val, String source, int pos) {
			super(context, source, pos);
			this.val = val;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) {
			return val;
		}
		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	public static class Variable extends ExpNode {
		private String[] vals;
		public Variable(ParseContext context, String[] vals, String source, int pos) {
			super(context, source, pos);
			this.vals = vals;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			return ec.getVariableValue(vals);
		}
		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	private static class UnaryOp extends ExpNode {
		public ExpNode subExp;
		private UnOpFunc func;
		UnaryOp(ParseContext context, ExpNode subExp, UnOpFunc func, String source, int pos) {
			super(context, source, pos);
			this.subExp = subExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			return func.apply(context, subExp.evaluate(ec));
		}
		@Override
		void walk(ExpressionWalker w) throws ExpError {
			subExp.walk(w);

			subExp = w.updateRef(subExp);

			w.visit(this);
		}
	}

	private static class BinaryOp extends ExpNode {
		public ExpNode lSubExp;
		public ExpNode rSubExp;
		public ExpResult lConstVal;
		public ExpResult rConstVal;

		private final BinOpFunc func;
		BinaryOp(ParseContext context, ExpNode lSubExp, ExpNode rSubExp, BinOpFunc func, String source, int pos) {
			super(context, source, pos);
			this.lSubExp = lSubExp;
			this.rSubExp = rSubExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult lRes = lConstVal != null ? lConstVal : lSubExp.evaluate(ec);
			ExpResult rRes = rConstVal != null ? rConstVal : rSubExp.evaluate(ec);
			return func.apply(context, lRes, rRes, source, tokenPos);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			lSubExp.walk(w);
			rSubExp.walk(w);

			lSubExp = w.updateRef(lSubExp);
			rSubExp = w.updateRef(rSubExp);

			w.visit(this);
		}
	}

	public static class Conditional extends ExpNode {
		private ExpNode condExp;
		private ExpNode trueExp;
		private ExpNode falseExp;
		private ExpResult constCondRes;
		private ExpResult constTrueRes;
		private ExpResult constFalseRes;
		public Conditional(ParseContext context, ExpNode c, ExpNode t, ExpNode f, String source, int pos) {
			super(context, source, pos);
			condExp = c;
			trueExp = t;
			falseExp =f;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			if (ec.eagerEval())
				return eagerEval(ec);
			else
				return lazyEval(ec);
		}

		private ExpResult lazyEval(EvalContext ec) throws ExpError {
			ExpResult condRes = constCondRes != null ? constCondRes : condExp.evaluate(ec);
			if (condRes.value == 0)
				return constFalseRes != null ? constFalseRes : falseExp.evaluate(ec);
			else
				return constTrueRes != null ? constTrueRes : trueExp.evaluate(ec);
		}

		private ExpResult eagerEval(EvalContext ec) throws ExpError {
			ExpResult  condRes =  constCondRes != null ?  constCondRes :  condExp.evaluate(ec);
			ExpResult  trueRes =  constTrueRes != null ?  constTrueRes :  trueExp.evaluate(ec);
			ExpResult falseRes = constFalseRes != null ? constFalseRes : falseExp.evaluate(ec);
			if (condRes.value == 0)
				return falseRes;
			else
				return trueRes;
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			condExp.walk(w);
			trueExp.walk(w);
			falseExp.walk(w);

			condExp = w.updateRef(condExp);
			trueExp = w.updateRef(trueExp);
			falseExp = w.updateRef(falseExp);

			w.visit(this);
		}
	}

	public static class FuncCall extends ExpNode {
		private ArrayList<ExpNode> args;
		private ArrayList<ExpResult> constResults;
		private CallableFunc function;
		public FuncCall(ParseContext context, CallableFunc function, ArrayList<ExpNode> args, String source, int pos) {
			super(context, source, pos);
			this.function = function;
			this.args = args;
			constResults = new ArrayList<>(args.size());
			for (int i = 0; i < args.size(); ++i) {
				constResults.add(null);
			}
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult[] argVals = new ExpResult[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				ExpResult constArg = constResults.get(i);
				argVals[i] = constArg != null ? constArg : args.get(i).evaluate(ec);
			}
			return function.call(context, argVals, source, tokenPos);
		}
		@Override
		void walk(ExpressionWalker w) throws ExpError {
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
		public int numMinArgs;
		public int numMaxArgs;
	}

	private static ArrayList<UnaryOpEntry> unaryOps = new ArrayList<>();
	private static ArrayList<BinaryOpEntry> binaryOps = new ArrayList<>();
	private static ArrayList<FunctionEntry> functions = new ArrayList<>();

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

	private static void addFunction(String name, int numMinArgs, int numMaxArgs, CallableFunc func) {
		FunctionEntry fe = new FunctionEntry();
		fe.name = name;
		fe.function = func;
		fe.numMinArgs = numMinArgs;
		fe.numMaxArgs = numMaxArgs;
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
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value + rval.value, lval.unitType);
			}
		});

		addBinaryOp("-", 20, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source,int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value - rval.value, lval.unitType);
			}
		});

		addBinaryOp("*", 30, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value * rval.value, newType);
			}
		});

		addBinaryOp("/", 30, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value / rval.value, newType);
			}
		});

		addBinaryOp("^", 40, true, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != DimensionlessUnit.class ||
				    rval.unitType != DimensionlessUnit.class) {

					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}

				return new ExpResult(Math.pow(lval.value, rval.value), DimensionlessUnit.class);
			}
		});

		addBinaryOp("==", 10, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value == rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("!=", 10, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value != rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("&&", 8, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return new ExpResult((lval.value!=0) && (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("||", 6, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return new ExpResult((lval.value!=0) || (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("<", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value < rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp("<=", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value <= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp(">", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value > rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		addBinaryOp(">=", 12, false, new BinOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
				return new ExpResult(lval.value >= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
		});

		////////////////////////////////////////////////////
		// Functions
		addFunction("max", 2, -1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}

				ExpResult res = args[0];
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value > res.value)
						res = args[i];
				}
				return res;
			}
		});

		addFunction("min", 2, -1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {

				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}

				ExpResult res = args[0];
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value < res.value)
						res = args[i];
				}
				return res;
			}
		});

		addFunction("abs", 1, 1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) {
				return new ExpResult(Math.abs(args[0].value), args[0].unitType);
			}
		});

		addFunction("indexOfMin", 2, -1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}

				ExpResult res = args[0];
				int index = 0;
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value < res.value) {
						res = args[i];
						index = i;
					}
				}
				return new ExpResult(index + 1, DimensionlessUnit.class);
			}
		});

		addFunction("indexOfMax", 2, -1, new CallableFunc() {
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}

				ExpResult res = args[0];
				int index = 0;
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value > res.value) {
						res = args[i];
						index = i;
					}
				}
				return new ExpResult(index + 1, DimensionlessUnit.class);
			}
		});
	}

	private static String unitToString(Class<? extends Unit> unit) {
		for (ObjectType type : ObjectType.getAll()) {
			if (type.getJavaClass() == unit) {
				return type.getName();
			}
		}
		return "Unknown Unit";
	}

	private static String getUnitMismatchString(Class<? extends Unit> u0, Class<? extends Unit> u1) {
		String s0 = unitToString(u0);
		String s1 = unitToString(u1);

		return String.format("Unit mismatch: '%s' and '%s' are not compatible", s0, s1);
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

		public void expect(int type, String val, String source) throws ExpError {
			if (pos == tokens.size()) {
				throw new ExpError(source, source.length(), String.format("Expected \"%s\", past the end of input", val));
			}

			ExpTokenizer.Token nextTok = tokens.get(pos);

			if (nextTok.type != type || !nextTok.value.equals(val)) {
				throw new ExpError(source, nextTok.pos, String.format("Expected \"%s\", got \"%s\"", val, nextTok.value));
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
		public void visit(ExpNode exp) throws ExpError {
			// Note: Below we are passing 'null' as an EvalContext, this is not typically
			// acceptable, but is 'safe enough' when we know the expression is a constant

			if (exp instanceof BinaryOp) {
				BinaryOp bo = (BinaryOp)exp;
				if (bo.lSubExp instanceof Constant) {
					// Just the left is a constant, store it in the binop
					bo.lConstVal = bo.lSubExp.evaluate(null);
				}
				if (bo.rSubExp instanceof Constant) {
					// Just the right is a constant, store it in the binop
					bo.rConstVal = bo.rSubExp.evaluate(null);
				}
			}
			if (exp instanceof Conditional) {
				Conditional cond = (Conditional)exp;
				if (cond.condExp instanceof Constant) {
					cond.constCondRes = cond.condExp.evaluate(null);
				}
				if (cond.trueExp instanceof Constant) {
					cond.constTrueRes = cond.trueExp.evaluate(null);
				}
				if (cond.falseExp instanceof Constant) {
					cond.constFalseRes = cond.falseExp.evaluate(null);
				}
			}
			if (exp instanceof FuncCall) {
				FuncCall fc = (FuncCall)exp;
				for (int i = 0; i < fc.args.size(); ++i) {
					if (fc.args.get(i) instanceof Constant) {
						fc.constResults.set(i, fc.args.get(i).evaluate(null));
					}
				}
			}
		}

		/**
		 * Give a node a chance to swap itself out with a different subtree.
		 */
		@Override
		public ExpNode updateRef(ExpNode exp) throws ExpError {
			if (exp instanceof UnaryOp) {
				UnaryOp uo = (UnaryOp)exp;
				if (uo.subExp instanceof Constant) {
					// This is an unary operation on a constant, we can replace it with a constant
					ExpResult val = uo.evaluate(null);
					return new Constant(uo.context, val, exp.source, uo.tokenPos);
				}
			}
			if (exp instanceof BinaryOp) {
				BinaryOp bo = (BinaryOp)exp;
				if ((bo.lSubExp instanceof Constant) && (bo.rSubExp instanceof Constant)) {
					// both sub expressions are constants, so replace the binop with a constant
					ExpResult val = bo.evaluate(null);
					return new Constant(bo.context, val, exp.source, bo.tokenPos);
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
	public static Expression parseExpression(ParseContext context, String input) throws ExpError {
		ArrayList<ExpTokenizer.Token> ts;
		ts = ExpTokenizer.tokenize(input);

		TokenList tokens = new TokenList(ts);

		ExpNode exp = parseExp(context, tokens, 0, input);

		// Make sure we've parsed all the tokens
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked != null) {
			throw new ExpError(input, peeked.pos, "Unexpected additional values");
		}

		exp.walk(CONST_OP);
		exp = CONST_OP.updateRef(exp); // Finally, give the entire expression a chance to optimize itself into a constant
		return new Expression(exp, input);
	}

	private static ExpNode parseExp(ParseContext context, TokenList tokens, double bindPower, String source) throws ExpError {
		ExpNode lhs = parseOpeningExp(context, tokens, bindPower, source);
		// Now peek for a binary op to modify this expression

		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE) {
				break;
			}
			BinaryOpEntry binOp = getBinaryOp(peeked.value);
			if (binOp != null && binOp.bindingPower > bindPower) {
				// The next token is a binary op and powerful enough to bind us
				lhs = handleBinOp(context, tokens, lhs, binOp, source, peeked.pos);
				continue;
			}
			// Specific check for binding the conditional (?:) operator
			if (peeked.value.equals("?") && bindPower == 0) {
				lhs = handleConditional(context, tokens, lhs, source, peeked.pos);
				continue;
			}
			break;
		}

		// We have bound as many operators as we can, return it
		return lhs;
	}

	private static ExpNode handleBinOp(ParseContext context, TokenList tokens, ExpNode lhs, BinaryOpEntry binOp, String source, int pos) throws ExpError {
		tokens.next(); // Consume the operator

		// For right associative operators, we weaken the binding power a bit at application time (but not testing time)
		double assocMod = binOp.rAssoc ? -0.5 : 0;
		ExpNode rhs = parseExp(context, tokens, binOp.bindingPower + assocMod, source);
		//currentPower = oe.bindingPower;

		return new BinaryOp(context, lhs, rhs, binOp.function, source, pos);
	}

	private static ExpNode handleConditional(ParseContext context, TokenList tokens, ExpNode lhs, String source, int pos) throws ExpError {
		tokens.next(); // Consume the '?'

		ExpNode trueExp = parseExp(context, tokens, 0, source);

		tokens.expect(ExpTokenizer.SYM_TYPE, ":", source);

		ExpNode falseExp = parseExp(context, tokens , 0, source);

		return new Conditional(context, lhs, trueExp, falseExp, source, pos);
	}

	public static Assignment parseAssignment(ParseContext context, String input) throws ExpError {
		ArrayList<ExpTokenizer.Token> ts;
		ts = ExpTokenizer.tokenize(input);

		TokenList tokens = new TokenList(ts);

		ExpTokenizer.Token nextTok = tokens.next();
		if (nextTok == null || (nextTok.type != ExpTokenizer.SQ_TYPE &&
		                        !nextTok.value.equals("this"))) {
			throw new ExpError(input, 0, "Assignments must start with an identifier");
		}
		ArrayList<String> destination = parseIdentifier(nextTok, tokens, input);

		nextTok = tokens.next();
		if (nextTok == null || nextTok.type != ExpTokenizer.SYM_TYPE || !nextTok.value.equals("=")) {
			throw new ExpError(input, nextTok.pos, "Expected '=' in assignment");
		}

		ExpNode exp = parseExp(context, tokens, 0, input);

		Assignment ret = new Assignment();
		ret.destination = destination.toArray(STRING_ARRAY_TYPE);
		ret.value = new Expression(exp, input);
		return ret;
	}

	// Static array to make ArrayList.toArray work
	private static final String[] STRING_ARRAY_TYPE = new String[0];

	// The first half of expression parsing, parse a simple expression based on the next token
	private static ExpNode parseOpeningExp(ParseContext context, TokenList tokens, double bindPower, String source) throws ExpError{
		ExpTokenizer.Token nextTok = tokens.next(); // consume the first token

		if (nextTok == null) {
			throw new ExpError(source, source.length(), "Unexpected end of string");
		}

		if (nextTok.type == ExpTokenizer.NUM_TYPE) {
			return parseConstant(context, nextTok.value, tokens, source, nextTok.pos);
		}
		if (nextTok.type == ExpTokenizer.VAR_TYPE &&
		    !nextTok.value.equals("this")) {
			return parseFuncCall(context, nextTok.value, tokens, source, nextTok.pos);
		}
		if (nextTok.type == ExpTokenizer.SQ_TYPE ||
		    nextTok.value.equals("this")) {
			ArrayList<String> vals = parseIdentifier(nextTok, tokens, source);
			return new Variable(context, vals.toArray(STRING_ARRAY_TYPE), source, nextTok.pos);
		}

		// The next token must be a symbol

		// handle parenthesis
		if (nextTok.value.equals("(")) {
			ExpNode exp = parseExp(context, tokens, 0, source);
			tokens.expect(ExpTokenizer.SYM_TYPE, ")", source); // Expect the closing paren
			return exp;
		}

		UnaryOpEntry oe = getUnaryOp(nextTok.value);
		if (oe != null) {
			ExpNode exp = parseExp(context, tokens, oe.bindingPower, source);
			return new UnaryOp(context, exp, oe.function, source, nextTok.pos);
		}

		// We're all out of tricks here, this is an unknown expression
		throw new ExpError(source, nextTok.pos, "Can not parse expression");
	}

	private static ExpNode parseConstant(ParseContext context, String constant, TokenList tokens, String source, int pos) throws ExpError {
		double mult = 1;
		Class<? extends Unit> ut = DimensionlessUnit.class;

		ExpTokenizer.Token peeked = tokens.peek();

		if (peeked != null && peeked.type == ExpTokenizer.SQ_TYPE) {
			// This constant is followed by a square quoted token, it must be the unit

			tokens.next(); // Consume unit token

			UnitData unit = context.getUnitByName(peeked.value);
			if (unit == null) {
				throw new ExpError(source, peeked.pos, "Unknown unit: %s", peeked.value);
			}
			mult = unit.scaleFactor;
			ut = unit.unitType;
		}

		return new Constant(context, new ExpResult(Double.parseDouble(constant)*mult, ut), source, pos);
	}


	private static ExpNode parseFuncCall(ParseContext context, String funcName, TokenList tokens, String source, int pos) throws ExpError {

		tokens.expect(ExpTokenizer.SYM_TYPE, "(", source);
		ArrayList<ExpNode> arguments = new ArrayList<>();

		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked == null) {
			throw new ExpError(source, source.length(), "Unexpected end of input in argument list");
		}
		boolean isEmpty = false;
		if (peeked.value.equals(")")) {
			// Special case with empty argument list
			isEmpty = true;
			tokens.next(); // Consume closing parens
		}

		while (!isEmpty) {
			ExpNode nextArg = parseExp(context, tokens, 0, source);
			arguments.add(nextArg);

			ExpTokenizer.Token nextTok = tokens.next();
			if (nextTok == null) {
				throw new ExpError(source, source.length(), "Unexpected end of input in argument list.");
			}
			if (nextTok.value.equals(")")) {
				break;
			}

			if (nextTok.value.equals(",")) {
				continue;
			}

			// Unexpected token
			throw new ExpError(source, nextTok.pos, "Unexpected token in arguement list");
		}

		FunctionEntry fe = getFunctionEntry(funcName);
		if (fe == null) {
			throw new ExpError(source, pos, "Uknown function: \"%s\"", funcName);
		}

		if (fe.numMinArgs > 0 && arguments.size() < fe.numMinArgs){
			throw new ExpError(source, pos, "Function \"%s\" expects at least %d arguments. %d provided.",
							funcName, fe.numMinArgs, arguments.size());
		}

		if (fe.numMaxArgs > 0 && arguments.size() > fe.numMaxArgs){
			throw new ExpError(source, pos, "Function \"%s\" expects at most %d arguments. %d provided.",
							funcName, fe.numMaxArgs, arguments.size());
		}

		return new FuncCall(context, fe.function, arguments, source, pos);
	}

	private static ArrayList<String> parseIdentifier(ExpTokenizer.Token firstName, TokenList tokens, String source) throws ExpError {
		ArrayList<String> vals = new ArrayList<>();
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
				throw new ExpError(source, peeked.pos, "Expected Identifier after '.'");
			}

			vals.add(nextName.value.intern());
		}

		return vals;
	}
}
