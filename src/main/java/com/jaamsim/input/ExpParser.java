/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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

import java.util.ArrayList;

import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpParser {

	public interface UnOpFunc {
		public void checkUnits(ParseContext context, ExpResult val) throws ExpError;
		public ExpResult apply(ParseContext context, ExpResult val) throws ExpError;
		public ExpValResult validate(ParseContext context, ExpValResult val);
	}

	public interface BinOpFunc {
		public void checkUnits(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError;
		public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError;
		public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos);
	}

	public interface CallableFunc {
		public void checkUnits(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError;
		public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError;
		public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos);
	}

	public static class UnitData {
		double scaleFactor;
		Class<? extends Unit> unitType;
	}

	public interface VarResolver {
		public ExpResult resolve(EvalContext ec, ExpResult[] indices) throws ExpError;
		public ExpValResult	validate(boolean[] hasIndices);
	}

	public interface ParseContext {
		public UnitData getUnitByName(String name);
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a, Class<? extends Unit> b);
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num, Class<? extends Unit> denom);

		public VarResolver getVarResolver(String[] names, boolean[] hasIndices) throws ExpError;
		public void validateAssignmentDest(String[] destination) throws ExpError;
	}

	public interface EvalContext {
		//public ExpResult getVariableValue(String[] names, ExpResult[] indices) throws ExpError;
		//public boolean eagerEval();
	}
//	public interface ValContext {
//
//	}

	private interface ExpressionWalker {
		public void visit(ExpNode exp) throws ExpError;
		public ExpNode updateRef(ExpNode exp) throws ExpError;
	}

	////////////////////////////////////////////////////////////////////
	// Expression types

	public static class Expression {
		public final String source;

		private final ArrayList<Thread> executingThreads = new ArrayList<>();

		private ExpNode rootNode;
		public Expression(String source) {
			this.source = source;
		}
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			synchronized(executingThreads) {
				if (executingThreads.contains(Thread.currentThread())) {
					throw new ExpError(null, 0, "Expression recursion detected for expression: %s", source);
				}

				executingThreads.add(Thread.currentThread());
			}
			ExpResult res = null;
			try {
				res = rootNode.evaluate(ec);
			} finally {
				synchronized(executingThreads) {
					executingThreads.remove(Thread.currentThread());
				}
			}
			return res;
		}
		void setRootNode(ExpNode node) {
			rootNode = node;
		}

		@Override
		public String toString() {
			return source;
		}
	}

	private abstract static class ExpNode {
		public final ParseContext context;
		public final Expression exp;
		public final int tokenPos;
		public abstract ExpResult evaluate(EvalContext ec) throws ExpError;
		public abstract ExpValResult validate();
		public ExpNode(ParseContext context, Expression exp, int pos) {
			this.context = context;
			this.tokenPos = pos;
			this.exp = exp;
		}
		abstract void walk(ExpressionWalker w) throws ExpError;
		// Get a version of this node that skips runtime checks if safe to do so,
		// otherwise return null
		public ExpNode getNoCheckVer() {
			return null;
		}
	}

	private static class Constant extends ExpNode {
		public ExpResult val;
		public Constant(ParseContext context, ExpResult val, Expression exp, int pos) {
			super(context, exp, pos);
			this.val = val;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) {
			return val;
		}

		@Override
		public ExpValResult validate() {
			return new ExpValResult(ExpValResult.State.VALID, val.unitType, (ExpError)null);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	public static class Variable extends ExpNode {
		//private final String[] vals;
		private final ExpNode[] indexExps;
		private final VarResolver resolver;
		public Variable(ParseContext context, String[] vals, ExpNode[] indexExps, Expression exp, int pos) throws ExpError {
			super(context, exp, pos);
			boolean[] hasIndices = new boolean[indexExps.length];
			for (int i = 0; i < indexExps.length; ++i) {
				hasIndices[i] = indexExps[i] != null;
			}
			this.resolver = context.getVarResolver(vals, hasIndices);
			this.indexExps = indexExps;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			if (indexExps == null)
				return resolver.resolve(ec, null);

			ExpResult[] indices = new ExpResult[indexExps.length];
			for (int i = 0; i < indexExps.length; ++i) {
				if (indexExps[i] != null)
					indices[i] = indexExps[i].evaluate(ec);
			}
			return resolver.resolve(ec, indices);
		}
		@Override
		public ExpValResult validate() {
			boolean[] hasIndices = new boolean[indexExps.length];
			for (int i = 0; i < indexExps.length; ++i) {
				hasIndices[i] = indexExps[i] != null;
			}
			return resolver.validate(hasIndices);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	private static class UnaryOp extends ExpNode {
		public ExpNode subExp;
		protected final UnOpFunc func;
		public boolean canSkipRuntimeChecks = false;
		UnaryOp(ParseContext context, ExpNode subExp, UnOpFunc func, Expression exp, int pos) {
			super(context, exp, pos);
			this.subExp = subExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult subExpVal = subExp.evaluate(ec);
			func.checkUnits(context, subExpVal);
			return func.apply(context, subExpVal);
		}

		@Override
		public ExpValResult validate() {
			ExpValResult res = func.validate(context, subExp.validate());
			if (res.state == ExpValResult.State.VALID)
				canSkipRuntimeChecks = true;

			return res;
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			subExp.walk(w);

			subExp = w.updateRef(subExp);

			w.visit(this);
		}

		@Override
		public ExpNode getNoCheckVer() {
			if (canSkipRuntimeChecks)
				return new UnaryOpNoChecks(this);
			else
				return null;
		}
	}

	private static class UnaryOpNoChecks extends UnaryOp {
		UnaryOpNoChecks(UnaryOp uo) {
			super(uo.context, uo.subExp, uo.func, uo.exp, uo.tokenPos);
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult subExpVal = subExp.evaluate(ec);
			return func.apply(context, subExpVal);
		}

	}

	private static class BinaryOp extends ExpNode {
		public ExpNode lSubExp;
		public ExpNode rSubExp;
		public boolean canSkipRuntimeChecks = false;

		protected final BinOpFunc func;
		BinaryOp(ParseContext context, ExpNode lSubExp, ExpNode rSubExp, BinOpFunc func, Expression exp, int pos) {
			super(context, exp, pos);
			this.lSubExp = lSubExp;
			this.rSubExp = rSubExp;
			this.func = func;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult lRes = lSubExp.evaluate(ec);
			ExpResult rRes = rSubExp.evaluate(ec);
			func.checkUnits(context, lRes, rRes, exp.source, tokenPos);
			return func.apply(context, lRes, rRes, exp.source, tokenPos);
		}

		@Override
		public ExpValResult validate() {
			ExpValResult lRes = lSubExp.validate();
			ExpValResult rRes = rSubExp.validate();

			ExpValResult res = func.validate(context, lRes, rRes, exp.source, tokenPos);
			if (res.state == ExpValResult.State.VALID)
				canSkipRuntimeChecks = true;

			return res;
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			lSubExp.walk(w);
			rSubExp.walk(w);

			lSubExp = w.updateRef(lSubExp);
			rSubExp = w.updateRef(rSubExp);

			w.visit(this);
		}
		@Override
		public ExpNode getNoCheckVer() {
			if (canSkipRuntimeChecks)
				return new BinaryOpNoChecks(this);
			else
				return null;
		}
	}

	private static class BinaryOpNoChecks extends BinaryOp {
		BinaryOpNoChecks(BinaryOp bo) {
			super(bo.context, bo.lSubExp, bo.rSubExp, bo.func, bo.exp, bo.tokenPos);
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult lRes = lSubExp.evaluate(ec);
			ExpResult rRes = rSubExp.evaluate(ec);
			return func.apply(context, lRes, rRes, exp.source, tokenPos);
		}

	}


	public static class Conditional extends ExpNode {
		private ExpNode condExp;
		private ExpNode trueExp;
		private ExpNode falseExp;
		public Conditional(ParseContext context, ExpNode c, ExpNode t, ExpNode f, Expression exp, int pos) {
			super(context, exp, pos);
			condExp = c;
			trueExp = t;
			falseExp =f;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult condRes = condExp.evaluate(ec); //constCondRes != null ? constCondRes : condExp.evaluate(ec);
			if (condRes.value == 0)
				return falseExp.evaluate(ec);
			else
				return trueExp.evaluate(ec);
		}

		@Override
		public ExpValResult validate() {
			ExpValResult condRes = condExp.validate();
			ExpValResult trueRes = trueExp.validate();
			ExpValResult falseRes = falseExp.validate();

			ExpValResult.State state;
			if (	condRes.state  == ExpValResult.State.ERROR ||
					trueRes.state  == ExpValResult.State.ERROR ||
					falseRes.state == ExpValResult.State.ERROR)
				state = ExpValResult.State.ERROR;
			else if (	condRes.state  == ExpValResult.State.UNDECIDABLE ||
						trueRes.state  == ExpValResult.State.UNDECIDABLE ||
						falseRes.state == ExpValResult.State.UNDECIDABLE)
				state = ExpValResult.State.UNDECIDABLE;
			else
				state = ExpValResult.State.VALID;

			ArrayList<ExpError> errors = new ArrayList<>();
			errors.addAll(condRes.errors);
			errors.addAll(trueRes.errors);
			errors.addAll(falseRes.errors);

			// Check that both sides of the branch return the same unit types
			if (	trueRes.state  == ExpValResult.State.VALID &&
					falseRes.state == ExpValResult.State.VALID) {
				if (trueRes.unitType != falseRes.unitType) {
					state = ExpValResult.State.ERROR;
					ExpError unitError = new ExpError(exp.source, tokenPos,
							"Unit mismatch in conditional. True branch is %s, false branch is %s",
							trueRes.unitType.getSimpleName(), falseRes.unitType.getSimpleName());
					errors.add(0, unitError);
				}
			}
			return new ExpValResult(state, trueRes.unitType, errors);
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
		protected final ArrayList<ExpNode> args;
		protected final CallableFunc function;
		private boolean canSkipRuntimeChecks = false;
		public FuncCall(ParseContext context, CallableFunc function, ArrayList<ExpNode> args, Expression exp, int pos) {
			super(context, exp, pos);
			this.function = function;
			this.args = args;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult[] argVals = new ExpResult[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				argVals[i] = args.get(i).evaluate(ec);
			}
			function.checkUnits(context, argVals, exp.source, tokenPos);
			return function.call(context, argVals, exp.source, tokenPos);
		}
		@Override
		public ExpValResult validate() {
			ExpValResult[] argVals = new ExpValResult[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				argVals[i] = args.get(i).validate();
			}

			ExpValResult res = function.validate(context, argVals, exp.source, tokenPos);
			if (res.state == ExpValResult.State.VALID)
				canSkipRuntimeChecks = true;
			return res;
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
		@Override
		public ExpNode getNoCheckVer() {
			if (canSkipRuntimeChecks)
				return new FuncCallNoChecks(this);
			else
				return null;
		}
	}

	private static class FuncCallNoChecks extends FuncCall {
		FuncCallNoChecks(FuncCall fc) {
			super(fc.context, fc.function, fc.args, fc.exp, fc.tokenPos);
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult[] argVals = new ExpResult[args.size()];
			for (int i = 0; i < args.size(); ++i) {
				argVals[i] = args.get(i).evaluate(ec);
			}
			return function.call(context, argVals, exp.source, tokenPos);
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

	///////////////////////////////////////////////////
	// Operator Utility Functions

	// See if there are any existing errors, or this branch is undecidable
	private static ExpValResult mergeBinaryErrors(ExpValResult lval, ExpValResult rval) {
		if (	lval.state == ExpValResult.State.ERROR ||
				rval.state == ExpValResult.State.ERROR) {
			// Propagate the error, no further checking
			ArrayList<ExpError> errors = new ArrayList<>(lval.errors);
			errors.addAll(rval.errors);
			return new ExpValResult(ExpValResult.State.ERROR, lval.unitType, errors);
		}

		if (	lval.state == ExpValResult.State.UNDECIDABLE ||
				rval.state == ExpValResult.State.UNDECIDABLE) {
			return new ExpValResult(ExpValResult.State.UNDECIDABLE, lval.unitType, (ExpError)null);
		}

		return null;
	}

	private static ExpValResult mergeMultipleErrors(ExpValResult[] args) {

		for (ExpValResult val : args) {
			if (val.state == ExpValResult.State.ERROR) {
				// We have an error, merge all error results and return
				ArrayList<ExpError> errors = new ArrayList<>();
				for (ExpValResult errVal : args) {
					errors.addAll(errVal.errors);
				}
				return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, errors);
			}
		}
		for (ExpValResult val : args) {
			if (val.state == ExpValResult.State.UNDECIDABLE) {
				// At least one value in undecidable, propagate it
				return new ExpValResult(ExpValResult.State.UNDECIDABLE, DimensionlessUnit.class, (ExpError)null);
			}
		}
		return null;

	}

	private static ExpValResult validateComparison(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
		// Require both values to be the same unit type and return a dimensionless unit

		// Propagate errors
		ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
		if (mergedErrors != null) {
			return mergedErrors;
		}

		// Both sub values should be valid here
		if (lval.unitType != rval.unitType) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
			return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
		}

		return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
	}

	// Validate with all args using the same units, and a new result returning 'newType' unit type
	private static ExpValResult validateSameUnits(ParseContext context, ExpValResult[] args, String source, int pos, Class<? extends Unit> newType) {
		ExpValResult mergedErrors = mergeMultipleErrors(args);
		if (mergedErrors != null)
			return mergedErrors;

		for (int i = 1; i < args.length; ++ i) {
			if (args[0].unitType != args[i].unitType) {
				ExpError error = new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				return new ExpValResult(ExpValResult.State.ERROR, args[0].unitType, error);
			}
		}
		return new ExpValResult(ExpValResult.State.VALID, newType, (ExpError)null);
	}

	// Check that a single argument is not an error and is a dimensionless unit
	private static ExpValResult validateSingleArgDimensionless(ParseContext context, ExpValResult arg, String source, int pos) {
		if (	arg.state == ExpValResult.State.ERROR ||
				arg.state == ExpValResult.State.UNDECIDABLE)
			return arg;
		if (arg.unitType != DimensionlessUnit.class) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(arg.unitType, DimensionlessUnit.class));
			return new ExpValResult(ExpValResult.State.ERROR, arg.unitType, error);
		}
		return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
	}

	// Check that a single argument is not an error and is a dimensionless unit
	private static ExpValResult validateSingleArgDimensionlessOrAngle(ParseContext context, ExpValResult arg, String source, int pos) {
		if (	arg.state == ExpValResult.State.ERROR ||
				arg.state == ExpValResult.State.UNDECIDABLE)
			return arg;
		if (arg.unitType != DimensionlessUnit.class && arg.unitType != AngleUnit.class) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(arg.unitType, AngleUnit.class));
			return new ExpValResult(ExpValResult.State.ERROR, arg.unitType, error);
		}
		return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
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
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val) {
				return val;
			}
			@Override
			public void checkUnits(ParseContext context, ExpResult val)
					throws ExpError {
				// N/A
			}
		});

		addUnaryOp("+", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return new ExpResult(val.value, val.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val) {
				return val;
			}
			@Override
			public void checkUnits(ParseContext context, ExpResult val)
					throws ExpError {
				// N/A
			}
		});

		addUnaryOp("!", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return new ExpResult(val.value == 0 ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val) {
				return new ExpValResult(val.state, DimensionlessUnit.class, val.errors);
			}
			@Override
			public void checkUnits(ParseContext context, ExpResult val)
					throws ExpError {
				// N/A
			}
		});

		///////////////////////////////////////////////////
		// Binary operators
		addBinaryOp("+", 20, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value + rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, lval.unitType, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, lval.unitType, (ExpError)null);
			}
		});

		addBinaryOp("-", 20, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source,int pos) throws ExpError {
				return new ExpResult(lval.value - rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, lval.unitType, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, lval.unitType, (ExpError)null);
			}
		});

		addBinaryOp("*", 30, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				return new ExpResult(lval.value * rval.value, newType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, newType, (ExpError)null);
			}
		});

		addBinaryOp("/", 30, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				return new ExpResult(lval.value / rval.value, newType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, newType, (ExpError)null);
			}
		});

		addBinaryOp("^", 40, true, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != DimensionlessUnit.class ||
				    rval.unitType != DimensionlessUnit.class) {

					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(Math.pow(lval.value, rval.value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				if (	lval.unitType != DimensionlessUnit.class ||
						rval.unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
			}
		});

		addBinaryOp("%", 30, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value % rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return new ExpValResult(ExpValResult.State.ERROR, lval.unitType, error);
				}
				return new ExpValResult(ExpValResult.State.VALID, lval.unitType, (ExpError)null);
			}
		});

		addBinaryOp("==", 10, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value == rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("!=", 10, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value != rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("&&", 8, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return new ExpResult((lval.value!=0) && (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("||", 6, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return new ExpResult((lval.value!=0) || (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("<", 12, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value < rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("<=", 12, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value <= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp(">", 12, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value > rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp(">=", 12, false, new BinOpFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return new ExpResult(lval.value >= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		////////////////////////////////////////////////////
		// Functions
		addFunction("max", 2, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				ExpResult res = args[0];
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value > res.value)
						res = args[i];
				}
				return res;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSameUnits(context, args, source, pos, args[0].unitType);
			}
		});

		addFunction("min", 2, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				ExpResult res = args[0];
				for (int i = 1; i < args.length; ++ i) {
					if (args[i].value < res.value)
						res = args[i];
				}
				return res;
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSameUnits(context, args, source, pos, args[0].unitType);
			}
		});

		addFunction("abs", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) {
				return new ExpResult(Math.abs(args[0].value), args[0].unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return args[0];
			}
		});

		addFunction("ceil", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) {
				return new ExpResult(Math.ceil(args[0].value), args[0].unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return args[0];
			}
		});

		addFunction("floor", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) {
				return new ExpResult(Math.floor(args[0].value), args[0].unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return args[0];
			}
		});

		addFunction("signum", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) {
				return new ExpResult(Math.signum(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return new ExpValResult(args[0].state, DimensionlessUnit.class, args[0].errors);
			}
		});

		addFunction("sqrt", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.sqrt(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("cbrt", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.cbrt(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("indexOfMin", 2, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSameUnits(context, args, source, pos, DimensionlessUnit.class);
			}
		});

		addFunction("indexOfMax", 2, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				for (int i = 1; i < args.length; ++ i) {
					if (args[0].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				}
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSameUnits(context, args, source, pos, DimensionlessUnit.class);
			}
		});

		addFunction("choose", 2, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));

				for (int i = 2; i < args.length; ++ i) {
					if (args[1].unitType != args[i].unitType)
						throw new ExpError(source, pos, getUnitMismatchString(args[1].unitType, args[i].unitType));
				}
			}

			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				int k = (int) args[0].value;
				if (k < 1 || k >= args.length)
					throw new ExpError(source, pos,
							String.format("Invalid index: %s. Index must be between 1 and %s.", k, args.length-1));

				return new ExpResult(args[k].value, args[k].unitType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult mergedErrors = mergeMultipleErrors(args);
				if (mergedErrors != null)
					return mergedErrors;

				if (args[0].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}

				for (int i = 2; i < args.length; ++ i) {
					if (args[1].unitType != args[i].unitType) {
						ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
						return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
					}
				}
				return new ExpValResult(ExpValResult.State.VALID, args[1].unitType, (ExpError)null);
			}
		});

		///////////////////////////////////////////////////
		// Mathematical Constants
		addFunction("E", 0, 0, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.E, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
			}
		});

		addFunction("PI", 0, 0, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.PI, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
			}
		});

		///////////////////////////////////////////////////
		// Trigonometric Functions
		addFunction("sin", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class && args[0].unitType != AngleUnit.class)
					throw new ExpError(source, pos, getInvalidTrigUnitString(args[0].unitType));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.sin(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionlessOrAngle(context, args[0], source, pos);
			}
		});

		addFunction("cos", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class && args[0].unitType != AngleUnit.class)
					throw new ExpError(source, pos, getInvalidTrigUnitString(args[0].unitType));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.cos(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionlessOrAngle(context, args[0], source, pos);
			}
		});

		addFunction("tan", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class && args[0].unitType != AngleUnit.class)
					throw new ExpError(source, pos, getInvalidTrigUnitString(args[0].unitType));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.tan(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionlessOrAngle(context, args[0], source, pos);
			}
		});

		///////////////////////////////////////////////////
		// Inverse Trigonometric Functions
		addFunction("asin", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.asin(args[0].value), AngleUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("acos", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.acos(args[0].value), AngleUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("atan", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.atan(args[0].value), AngleUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("atan2", 2, 2, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
				if (args[1].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[1].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.atan2(args[0].value, args[1].value), AngleUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult mergedErrors = mergeMultipleErrors(args);
				if (mergedErrors != null)
					return mergedErrors;

				if (args[0].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}
				if (args[1].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[1].unitType, DimensionlessUnit.class));
					return new ExpValResult(ExpValResult.State.ERROR, DimensionlessUnit.class, error);
				}

				return new ExpValResult(ExpValResult.State.VALID, DimensionlessUnit.class, (ExpError)null);
			}
		});

		///////////////////////////////////////////////////
		// Exponential Functions
		addFunction("exp", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.exp(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("ln", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.log(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("log", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].unitType != DimensionlessUnit.class)
					throw new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
			}
			@Override
			public ExpResult call(ParseContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return new ExpResult(Math.log10(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});
	}

	private static String unitToString(Class<? extends Unit> unit) {
		ObjectType type = ObjectType.getObjectTypeForClass(unit);
		if (type == null)
			return "Unknown Unit";
		return type.getName();
	}

	private static String getUnitMismatchString(Class<? extends Unit> u0, Class<? extends Unit> u1) {
		String s0 = unitToString(u0);
		String s1 = unitToString(u1);

		return String.format("Unit mismatch: '%s' and '%s' are not compatible", s0, s1);
	}

	private static String getInvalidTrigUnitString(Class<? extends Unit> u0) {
		String s0 = unitToString(u0);
		return String.format("Invalid unit: %s. The input to a trigonometric function must be dimensionless or an angle.", s0);
	}

	private static String getInvalidUnitString(Class<? extends Unit> u0, Class<? extends Unit> u1) {
		String s0 = unitToString(u0);
		String s1 = unitToString(u1);
		if (u1 == DimensionlessUnit.class)
			return String.format("Invalid unit: %s. A dimensionless number is required.", s0);

		return String.format("Invalid unit: %s. Units of %s are required.", s0, s1);
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
			// N/A
		}

		/**
		 * Give a node a chance to swap itself out with a different subtree.
		 */
		@Override
		public ExpNode updateRef(ExpNode origNode) throws ExpError {
			// Note: Below we are passing 'null' as an EvalContext, this is not typically
			// acceptable, but is 'safe enough' when we know the expression is a constant
			if (origNode instanceof UnaryOp) {
				UnaryOp uo = (UnaryOp)origNode;
				if (uo.subExp instanceof Constant) {
					// This is an unary operation on a constant, we can replace it with a constant
					ExpResult val = uo.evaluate(null);
					return new Constant(uo.context, val, origNode.exp, uo.tokenPos);
				}
			}
			if (origNode instanceof BinaryOp) {
				BinaryOp bo = (BinaryOp)origNode;
				if ((bo.lSubExp instanceof Constant) && (bo.rSubExp instanceof Constant)) {
					// both sub expressions are constants, so replace the binop with a constant
					ExpResult val = bo.evaluate(null);
					return new Constant(bo.context, val, origNode.exp, bo.tokenPos);
				}
			}
			if (origNode instanceof FuncCall) {
				FuncCall fc = (FuncCall)origNode;
				boolean constArgs = true;
				for (int i = 0; i < fc.args.size(); ++i) {
					if (!(fc.args.get(i) instanceof Constant)) {
						constArgs = false;
					}
				}
				if (constArgs) {
					ExpResult val = fc.evaluate(null);
					return new Constant(fc.context, val, origNode.exp, fc.tokenPos);
				}
			}
			return origNode;
		}
	}

	private static ConstOptimizer CONST_OP = new ConstOptimizer();

	private static class RuntimeCheckOptimizer implements ExpressionWalker {

		@Override
		public void visit(ExpNode exp) throws ExpError {
			// N/A
		}

		@Override
		public ExpNode updateRef(ExpNode exp) throws ExpError {
			ExpNode noCheckVer = exp.getNoCheckVer();
			if (noCheckVer != null)
				return noCheckVer;
			else
				return exp;
		}
	}
	private static RuntimeCheckOptimizer RTC_OP = new RuntimeCheckOptimizer();

	private static ExpNode optimizeAndValidateExpression(String input, ExpNode expNode) throws ExpError {
		expNode.walk(CONST_OP);
		expNode = CONST_OP.updateRef(expNode); // Finally, give the entire expression a chance to optimize itself into a constant

		// Run the validation
		ExpValResult valRes = expNode.validate();
		if (valRes.state == ExpValResult.State.ERROR) {
			if (valRes.errors.size() == 0) {
				throw new ExpError(input, 0, "An unknown expression error occurred. This is probably a bug. Please inform the developers.");
			}

			// We received at least one error while validating.
			throw valRes.errors.get(0);
		}

		// Now that validation is complete, we can run the optimizer that removes runtime checks on validated nodes
		expNode.walk(RTC_OP);
		expNode = RTC_OP.updateRef(expNode); // Give the top level node a chance to optimize

		return expNode;
	}

	/**
	 * The main entry point to the expression parsing system, will either return a valid
	 * expression that can be evaluated, or throw an error.
	 */
	public static Expression parseExpression(ParseContext context, String input) throws ExpError {
		ArrayList<ExpTokenizer.Token> ts;
		ts = ExpTokenizer.tokenize(input);

		TokenList tokens = new TokenList(ts);

		Expression ret = new Expression(input);
		ExpNode expNode = parseExp(context, tokens, 0, ret);

		// Make sure we've parsed all the tokens
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked != null) {
			throw new ExpError(input, peeked.pos, "Unexpected additional values");
		}

		expNode = optimizeAndValidateExpression(input, expNode);

		ret.setRootNode(expNode);

		return ret;
	}

	private static ExpNode parseExp(ParseContext context, TokenList tokens, double bindPower, Expression exp) throws ExpError {
		ExpNode lhs = parseOpeningExp(context, tokens, bindPower, exp);
		// Now peek for a binary op to modify this expression

		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE) {
				break;
			}
			BinaryOpEntry binOp = getBinaryOp(peeked.value);
			if (binOp != null && binOp.bindingPower > bindPower) {
				// The next token is a binary op and powerful enough to bind us
				lhs = handleBinOp(context, tokens, lhs, binOp, exp, peeked.pos);
				continue;
			}
			// Specific check for binding the conditional (?:) operator
			if (peeked.value.equals("?") && bindPower == 0) {
				lhs = handleConditional(context, tokens, lhs, exp, peeked.pos);
				continue;
			}
			break;
		}

		// We have bound as many operators as we can, return it
		return lhs;
	}

	private static ExpNode handleBinOp(ParseContext context, TokenList tokens, ExpNode lhs, BinaryOpEntry binOp, Expression exp, int pos) throws ExpError {
		tokens.next(); // Consume the operator

		// For right associative operators, we weaken the binding power a bit at application time (but not testing time)
		double assocMod = binOp.rAssoc ? -0.5 : 0;
		ExpNode rhs = parseExp(context, tokens, binOp.bindingPower + assocMod, exp);
		//currentPower = oe.bindingPower;

		return new BinaryOp(context, lhs, rhs, binOp.function, exp, pos);
	}

	private static ExpNode handleConditional(ParseContext context, TokenList tokens, ExpNode lhs, Expression exp, int pos) throws ExpError {
		tokens.next(); // Consume the '?'

		ExpNode trueExp = parseExp(context, tokens, 0, exp);

		tokens.expect(ExpTokenizer.SYM_TYPE, ":", exp.source);

		ExpNode falseExp = parseExp(context, tokens , 0, exp);

		return new Conditional(context, lhs, trueExp, falseExp, exp, pos);
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

		String[] destination = parseIdentifier(nextTok, tokens, new Expression(input));
		context.validateAssignmentDest(destination);

		nextTok = tokens.next();
		if (nextTok == null || nextTok.type != ExpTokenizer.SYM_TYPE || !nextTok.value.equals("=")) {
			throw new ExpError(input, nextTok.pos, "Expected '=' in assignment");
		}

		Assignment ret = new Assignment();
		ret.destination = destination;
		ret.value = new Expression(input);

		ExpNode expNode = parseExp(context, tokens, 0, ret.value);

		expNode = optimizeAndValidateExpression(input, expNode);

		ret.value.setRootNode(expNode);

		return ret;
	}

	// The first half of expression parsing, parse a simple expression based on the next token
	private static ExpNode parseOpeningExp(ParseContext context, TokenList tokens, double bindPower, Expression exp) throws ExpError{
		ExpTokenizer.Token nextTok = tokens.next(); // consume the first token

		if (nextTok == null) {
			throw new ExpError(exp.source, exp.source.length(), "Unexpected end of string");
		}

		if (nextTok.type == ExpTokenizer.NUM_TYPE) {
			return parseConstant(context, nextTok.value, tokens, exp, nextTok.pos);
		}
		if (nextTok.type == ExpTokenizer.VAR_TYPE &&
		    !nextTok.value.equals("this")) {
			return parseFuncCall(context, nextTok.value, tokens, exp, nextTok.pos);
		}
		if (nextTok.type == ExpTokenizer.SQ_TYPE ||
		    nextTok.value.equals("this")) {
			return parseVariable(context, nextTok, tokens, exp, nextTok.pos);
		}

		// The next token must be a symbol

		// handle parenthesis
		if (nextTok.value.equals("(")) {
			ExpNode expNode = parseExp(context, tokens, 0, exp);
			tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source); // Expect the closing paren
			return expNode;
		}

		UnaryOpEntry oe = getUnaryOp(nextTok.value);
		if (oe != null) {
			ExpNode expNode = parseExp(context, tokens, oe.bindingPower, exp);
			return new UnaryOp(context, expNode, oe.function, exp, nextTok.pos);
		}

		// We're all out of tricks here, this is an unknown expression
		throw new ExpError(exp.source, nextTok.pos, "Can not parse expression");
	}

	private static ExpNode parseConstant(ParseContext context, String constant, TokenList tokens, Expression exp, int pos) throws ExpError {
		double mult = 1;
		Class<? extends Unit> ut = DimensionlessUnit.class;

		ExpTokenizer.Token peeked = tokens.peek();

		if (peeked != null && peeked.type == ExpTokenizer.SQ_TYPE) {
			// This constant is followed by a square quoted token, it must be the unit

			tokens.next(); // Consume unit token

			UnitData unit = context.getUnitByName(peeked.value);
			if (unit == null) {
				throw new ExpError(exp.source, peeked.pos, "Unknown unit: %s", peeked.value);
			}
			mult = unit.scaleFactor;
			ut = unit.unitType;
		}

		return new Constant(context, new ExpResult(Double.parseDouble(constant)*mult, ut), exp, pos);
	}


	private static ExpNode parseFuncCall(ParseContext context, String funcName, TokenList tokens, Expression exp, int pos) throws ExpError {

		tokens.expect(ExpTokenizer.SYM_TYPE, "(", exp.source);
		ArrayList<ExpNode> arguments = new ArrayList<>();

		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked == null) {
			throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in argument list");
		}
		boolean isEmpty = false;
		if (peeked.value.equals(")")) {
			// Special case with empty argument list
			isEmpty = true;
			tokens.next(); // Consume closing parens
		}

		while (!isEmpty) {
			ExpNode nextArg = parseExp(context, tokens, 0, exp);
			arguments.add(nextArg);

			ExpTokenizer.Token nextTok = tokens.next();
			if (nextTok == null) {
				throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in argument list.");
			}
			if (nextTok.value.equals(")")) {
				break;
			}

			if (nextTok.value.equals(",")) {
				continue;
			}

			// Unexpected token
			throw new ExpError(exp.source, nextTok.pos, "Unexpected token in arguement list");
		}

		FunctionEntry fe = getFunctionEntry(funcName);
		if (fe == null) {
			throw new ExpError(exp.source, pos, "Uknown function: \"%s\"", funcName);
		}

		if (fe.numMinArgs >= 0 && arguments.size() < fe.numMinArgs){
			throw new ExpError(exp.source, pos, "Function \"%s\" expects at least %d arguments. %d provided.",
							funcName, fe.numMinArgs, arguments.size());
		}

		if (fe.numMaxArgs >= 0 && arguments.size() > fe.numMaxArgs){
			throw new ExpError(exp.source, pos, "Function \"%s\" expects at most %d arguments. %d provided.",
							funcName, fe.numMaxArgs, arguments.size());
		}

		return new FuncCall(context, fe.function, arguments, exp, pos);
	}

	private static String[] parseIdentifier(ExpTokenizer.Token firstName, TokenList tokens, Expression exp) throws ExpError {
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
				throw new ExpError(exp.source, peeked.pos, "Expected Identifier after '.'");
			}

			vals.add(nextName.value.intern());
		}

		String[] ret = new String[vals.size()];
		for (int i = 0; i < ret.length; i++)
			ret[i] = vals.get(i);
		return ret;
	}

	private static Variable parseVariable(ParseContext context, ExpTokenizer.Token firstName, TokenList tokens, Expression exp, int pos) throws ExpError {
		ArrayList<String> vals = new ArrayList<>();
		ArrayList<ExpNode> indexExps = new ArrayList<>();
		vals.add(firstName.value.intern());
		indexExps.add(null);
		while (true) {

			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE || !peeked.value.equals(".")) {
				break;
			}

			// Next token is a '.' so parse another name

			tokens.next(); // consume
			ExpTokenizer.Token nextName = tokens.next();
			if (nextName == null || nextName.type != ExpTokenizer.VAR_TYPE) {
				throw new ExpError(exp.source, peeked.pos, "Expected Identifier after '.'");
			}


			vals.add(nextName.value.intern());

			peeked = tokens.peek();
			if (peeked != null && peeked.value.equals("(")) {
				// Optional index
				tokens.next(); // consume
				indexExps.add(parseExp(context, tokens, 0, exp));
				tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source);
			} else {
				indexExps.add(null);
			}


		}

		assert(vals.size() == indexExps.size());
		String[] namesArray = new String[vals.size()];
		ExpNode[] indexArray = new ExpNode[indexExps.size()];
		for (int i = 0; i < namesArray.length; i++) {
			namesArray[i] = vals.get(i);
			indexArray[i] = indexExps.get(i);
		}
		return new Variable(context, namesArray, indexArray, exp, pos);
	}

}
