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
		public void checkTypeAndUnits(ParseContext context, ExpResult val, String source, int pos) throws ExpError;
		public ExpResult apply(ParseContext context, ExpResult val) throws ExpError;
		public ExpValResult validate(ParseContext context, ExpValResult val, String source, int pos);
	}

	public interface BinOpFunc {
		public void checkTypeAndUnits(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError;
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

	public interface OutputResolver {
		public ExpResult resolve(EvalContext ec, ExpResult ent, ExpResult index) throws ExpError;
		public ExpValResult validate(ExpValResult entValRes, ExpValResult indValRes);
	}

	public interface ParseContext {
		public UnitData getUnitByName(String name);
		public Class<? extends Unit> multUnitTypes(Class<? extends Unit> a, Class<? extends Unit> b);
		public Class<? extends Unit> divUnitTypes(Class<? extends Unit> num, Class<? extends Unit> denom);

		public ExpResult getValFromName(String name) throws ExpError;
		public OutputResolver getOutputResolver(String name) throws ExpError;
		public OutputResolver getConstOutputResolver(ExpResult constEnt, String name) throws ExpError;
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

		public ExpValResult validationResult;

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
			return ExpValResult.makeValidRes(val.type, val.unitType);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	public static class ResolveOutput extends ExpNode {
		public ExpNode entNode;
		public ExpNode index;
		public String outputName;

		private final OutputResolver resolver;

		public ResolveOutput(ParseContext context, String outputName, ExpNode entNode, ExpNode index, Expression exp, int pos) throws ExpError {
			super(context, exp, pos);

			this.entNode = entNode;
			this.index = index;
			this.outputName = outputName;

			if (entNode instanceof Constant && index == null) {
				this.resolver = context.getConstOutputResolver(entNode.evaluate(null), outputName);
			} else {
				this.resolver = context.getOutputResolver(outputName);
			}
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult ent = entNode.evaluate(ec);
			ExpResult indResult = null;
			if (index != null)
				indResult = index.evaluate(ec);

			return resolver.resolve(ec, ent, indResult);
		}
		@Override
		public ExpValResult validate() {
			ExpValResult entValRes = entNode.validate();
			ExpValResult indValRes = null;
			if (index != null)
				indValRes = index.validate();

			if (entValRes.state == ExpValResult.State.ERROR) {
				return entValRes;
			}
			if (indValRes != null && indValRes.state == ExpValResult.State.ERROR) {
				return indValRes;
			}

			if (entValRes.state == ExpValResult.State.UNDECIDABLE) {
				return entValRes;
			}
			if (indValRes != null && indValRes.state == ExpValResult.State.UNDECIDABLE) {
				return indValRes;
			}

			return resolver.validate(entValRes, indValRes);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			entNode.walk(w);
			entNode = w.updateRef(entNode);

			if (index != null) {
				index.walk(w);
				index = w.updateRef(index);
			}

			w.visit(this);
		}
	}


	private static class UnaryOp extends ExpNode {
		public ExpNode subExp;
		protected final UnOpFunc func;
		public String name;
		public boolean canSkipRuntimeChecks = false;
		UnaryOp(String name, ParseContext context, ExpNode subExp, UnOpFunc func, Expression exp, int pos) {
			super(context, exp, pos);
			this.subExp = subExp;
			this.func = func;
			this.name = name;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult subExpVal = subExp.evaluate(ec);
			func.checkTypeAndUnits(context, subExpVal, exp.source, tokenPos);
			return func.apply(context, subExpVal);
		}

		@Override
		public ExpValResult validate() {
			ExpValResult res = func.validate(context, subExp.validate(), exp.source, tokenPos);
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
		@Override
		public String toString() {
			return "UnaryOp: " + name;
		}
	}

	private static class UnaryOpNoChecks extends UnaryOp {
		UnaryOpNoChecks(UnaryOp uo) {
			super(uo.name, uo.context, uo.subExp, uo.func, uo.exp, uo.tokenPos);
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
		public String name;

		protected final BinOpFunc func;
		BinaryOp(String name, ParseContext context, ExpNode lSubExp, ExpNode rSubExp, BinOpFunc func, Expression exp, int pos) {
			super(context, exp, pos);
			this.lSubExp = lSubExp;
			this.rSubExp = rSubExp;
			this.func = func;
			this.name = name;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ExpResult lRes = lSubExp.evaluate(ec);
			ExpResult rRes = rSubExp.evaluate(ec);
			func.checkTypeAndUnits(context, lRes, rRes, exp.source, tokenPos);
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
		@Override
		public String toString() {
			return "BinaryOp: " + name;
		}
	}

	private static class BinaryOpNoChecks extends BinaryOp {
		BinaryOpNoChecks(BinaryOp bo) {
			super(bo.name, bo.context, bo.lSubExp, bo.rSubExp, bo.func, bo.exp, bo.tokenPos);
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

			if (	condRes.state  == ExpValResult.State.ERROR ||
					trueRes.state  == ExpValResult.State.ERROR ||
					falseRes.state == ExpValResult.State.ERROR) {
				// Error state, merge all returned errors
				ArrayList<ExpError> errors = new ArrayList<>();
				if (condRes.errors != null)
					errors.addAll(condRes.errors);
				if (trueRes.errors != null)
					errors.addAll(trueRes.errors);
				if (falseRes.errors != null)
					errors.addAll(falseRes.errors);
				return ExpValResult.makeErrorRes(errors);
			}
			else if (	condRes.state  == ExpValResult.State.UNDECIDABLE ||
						trueRes.state  == ExpValResult.State.UNDECIDABLE ||
						falseRes.state == ExpValResult.State.UNDECIDABLE) {
				return ExpValResult.makeUndecidableRes();
			}

			// All valid case

			// Check that both sides of the branch return the same type
			if (trueRes.type != falseRes.type) {

				ExpError typeError = new ExpError(exp.source, tokenPos,
						"Type mismatch in conditional. True branch is %s, false branch is %s",
						trueRes.type.toString(), falseRes.type.toString());
				return ExpValResult.makeErrorRes(typeError);
			}

			// Check that both sides of the branch return the same unit types
			if (trueRes.unitType != falseRes.unitType) {

				ExpError unitError = new ExpError(exp.source, tokenPos,
						"Unit mismatch in conditional. True branch is %s, false branch is %s",
						trueRes.unitType.getSimpleName(), falseRes.unitType.getSimpleName());
				return ExpValResult.makeErrorRes(unitError);
			}
			return ExpValResult.makeValidRes(trueRes.type, trueRes.unitType);
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
		@Override
		public String toString() {
			return "Conditional";
		}
	}

	public static class FuncCall extends ExpNode {
		protected final ArrayList<ExpNode> args;
		protected final CallableFunc function;
		private boolean canSkipRuntimeChecks = false;
		private final String name;
		public FuncCall(String name, ParseContext context, CallableFunc function, ArrayList<ExpNode> args, Expression exp, int pos) {
			super(context, exp, pos);
			this.function = function;
			this.args = args;
			this.name = name;
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
		@Override
		public String toString() {
			return "Function: " + name;
		}
	}

	private static class FuncCallNoChecks extends FuncCall {
		FuncCallNoChecks(FuncCall fc) {
			super(fc.name, fc.context, fc.function, fc.args, fc.exp, fc.tokenPos);
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
			ArrayList<ExpError> errors = new ArrayList<>();
			if (lval.errors != null)
				errors.addAll(lval.errors);
			if (rval.errors != null)
				errors.addAll(rval.errors);
			return ExpValResult.makeErrorRes(errors);
		}

		if (	lval.state == ExpValResult.State.UNDECIDABLE ||
				rval.state == ExpValResult.State.UNDECIDABLE) {
			return ExpValResult.makeUndecidableRes();
		}

		return null;
	}

	private static ExpValResult mergeMultipleErrors(ExpValResult[] args) {

		for (ExpValResult val : args) {
			if (val.state == ExpValResult.State.ERROR) {
				// We have an error, merge all error results and return
				ArrayList<ExpError> errors = new ArrayList<>();
				for (ExpValResult errVal : args) {
					if (errVal.errors != null)
						errors.addAll(errVal.errors);
				}
				return ExpValResult.makeErrorRes(errors);
			}
		}
		for (ExpValResult val : args) {
			if (val.state == ExpValResult.State.UNDECIDABLE) {
				// At least one value in undecidable, propagate it
				return ExpValResult.makeUndecidableRes();
			}
		}
		return null;

	}

	private static void checkBothNumbers(ExpResult lval, ExpResult rval, String source, int pos)
	throws ExpError {
		if (lval.type != ExpResType.NUMBER) {
			throw new ExpError(source, pos, "Left operand must be a number");
		}
		if (rval.type != ExpResType.NUMBER) {
			throw new ExpError(source, pos, "Right operand must be a number");
		}
	}

	private static ExpValResult validateBothNumbers(ExpValResult lval, ExpValResult rval, String source, int pos) {
		if (lval.type != ExpResType.NUMBER) {
			ExpError error = new ExpError(source, pos, "Left operand must be a number");
			return ExpValResult.makeErrorRes(error);
		}
		if (rval.type != ExpResType.NUMBER) {
			ExpError error = new ExpError(source, pos, "Right operand must be a number");
			return ExpValResult.makeErrorRes(error);
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

		ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
		if (numRes != null) {
			return numRes;
		}

		// Both sub values should be valid here
		if (lval.unitType != rval.unitType) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
			return ExpValResult.makeErrorRes(error);
		}

		return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
	}

	private static void checkTypedComparison(ParseContext context, ExpResult lval,
			ExpResult rval, String source, int pos) throws ExpError {

		// Check that the types are the same
		if (lval.type != rval.type) {
			throw new ExpError(source, pos, "Can not compare different types. LHS: %s, RHS: %s",
					ExpValResult.typeString(lval.type),
					ExpValResult.typeString(rval.type));
		}
		if (lval.type == ExpResType.NUMBER) {
			// Also check that the unit types are the same
			if (lval.unitType != rval.unitType) {
				throw new ExpError(source, pos, "Can not compare different unit types. LHS: %s, RHS: %s",
						lval.unitType.getSimpleName(),
						rval.unitType.getSimpleName());
			}
		}
	}
	private static boolean evaluteTypedEquality(ExpResult lval, ExpResult rval) {
		boolean equal;
		switch(lval.type) {
		case ENTITY:
			equal = lval.entVal == rval.entVal;
			break;
		case STRING:
			equal = lval.stringVal.equals(rval.stringVal);
			break;
		case NUMBER:
			equal = lval.value == rval.value;
			break;
		default:
			assert(false);
			equal = false;
		}
		return equal;
	}

	private static ExpValResult validateTypedComparison(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
		// Propagate errors
		ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
		if (mergedErrors != null) {
			return mergedErrors;
		}

		// Otherwise, check that the types are the same
		if (lval.type != rval.type) {
			ExpError err = new ExpError(source, pos, "Can not compare different types. LHS: %s, RHS: %s",
					ExpValResult.typeString(lval.type),
					ExpValResult.typeString(rval.type));
			return ExpValResult.makeErrorRes(err);
		}

		if (lval.type == ExpResType.NUMBER) {
			// Also check that the unit types are the same
			if (lval.unitType != rval.unitType) {
				ExpError err = new ExpError(source, pos, "Can not compare different unit types. LHS: %s, RHS: %s",
						lval.unitType.getSimpleName(),
						rval.unitType.getSimpleName());
				return ExpValResult.makeErrorRes(err);
			}
		}
		return ExpValResult.makeValidRes(lval.type, DimensionlessUnit.class);

	}

	// Validate with all args using the same units, and a new result returning 'newType' unit type
	private static ExpValResult validateSameUnits(ParseContext context, ExpValResult[] args, String source, int pos, Class<? extends Unit> newType) {
		ExpValResult mergedErrors = mergeMultipleErrors(args);
		if (mergedErrors != null)
			return mergedErrors;

		for (int i = 1; i < args.length; ++ i) {
			if (args[i].type != ExpResType.NUMBER) {
				ExpError error = new ExpError(source, pos, String.format("Argument %d must be a number", i+1));
				return ExpValResult.makeErrorRes(error);
			}

			if (args[0].unitType != args[i].unitType) {
				ExpError error = new ExpError(source, pos, getUnitMismatchString(args[0].unitType, args[i].unitType));
				return ExpValResult.makeErrorRes(error);

			}
		}
		return ExpValResult.makeValidRes(ExpResType.NUMBER, newType);
	}

	// Check that a single argument is not an error and is a dimensionless unit
	private static ExpValResult validateSingleArgDimensionless(ParseContext context, ExpValResult arg, String source, int pos) {
		if (	arg.state == ExpValResult.State.ERROR ||
				arg.state == ExpValResult.State.UNDECIDABLE)
			return arg;
		if (arg.type != ExpResType.NUMBER) {
			ExpError error = new ExpError(source, pos, "Argument must be a number");
			return ExpValResult.makeErrorRes(error);
		}
		if (arg.unitType != DimensionlessUnit.class) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(arg.unitType, DimensionlessUnit.class));
			return ExpValResult.makeErrorRes(error);
		}
		return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
	}

	// Check that a single argument is not an error and is a dimensionless unit or angle unit
	private static ExpValResult validateTrigFunction(ParseContext context, ExpValResult arg, String source, int pos) {
		if (	arg.state == ExpValResult.State.ERROR ||
				arg.state == ExpValResult.State.UNDECIDABLE)
			return arg;
		if (arg.type != ExpResType.NUMBER) {
			ExpError error = new ExpError(source, pos, "Argument must be a number");
			return ExpValResult.makeErrorRes(error);
		}
		if (arg.unitType != DimensionlessUnit.class && arg.unitType != AngleUnit.class) {
			ExpError error = new ExpError(source, pos, getUnitMismatchString(arg.unitType, DimensionlessUnit.class));
			return ExpValResult.makeErrorRes(error);
		}
		return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
	}


	////////////////////////////////////////////////////////
	// Statically initialize the operators and functions

	static {

		///////////////////////////////////////////////////
		// Unary Operators
		addUnaryOp("-", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return ExpResult.makeNumResult(-val.value, val.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val, String source, int pos) {
				if (val.state == ExpValResult.State.VALID && val.type != ExpResType.NUMBER) {
					ExpError err = new ExpError(source, pos, "Unary negation only applies to numbers");
					return ExpValResult.makeErrorRes(err);
				}
				return val;
			}
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult val, String source, int pos)
					throws ExpError {
				if (val.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Unary negation only applies to numbers");
				}
			}
		});

		addUnaryOp("+", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return ExpResult.makeNumResult(val.value, val.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val, String source, int pos) {
				if (val.state == ExpValResult.State.VALID && val.type != ExpResType.NUMBER) {
					ExpError err = new ExpError(source, pos, "Unary positive only applies to numbers");
					return ExpValResult.makeErrorRes(err);
				}
				return val;
			}
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult val, String source, int pos)
					throws ExpError {
				if (val.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Unary positive only applies to numbers");
				}
			}
		});

		addUnaryOp("!", 50, new UnOpFunc() {
			@Override
			public ExpResult apply(ParseContext context, ExpResult val){
				return ExpResult.makeNumResult(val.value == 0 ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult val, String source, int pos) {
				// If the sub expression result was valid, make it dimensionless, otherwise return the sub expression result
				if (val.state == ExpValResult.State.VALID) {
					if (val.type == ExpResType.NUMBER)
						return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);

					// The expression is valid, but not a number
					ExpError error = new ExpError(source, pos, "Argument must be a number");
					return ExpValResult.makeErrorRes(error);
				} else {
					return val;
				}
			}
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult val, String source, int pos)
					throws ExpError {
				if (val.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Unary not only applies to numbers");
				}
			}
		});

		///////////////////////////////////////////////////
		// Binary operators
		addBinaryOp("+", 20, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				boolean bothNumbers = (lval.type==ExpResType.NUMBER) && (rval.type==ExpResType.NUMBER);
				boolean bothStrings = (lval.type==ExpResType.STRING) && (rval.type==ExpResType.STRING);
				if (!bothNumbers && !bothStrings) {
					throw new ExpError(source, pos, "Operator '+' requires two numbers or two strings");
				}

				if (bothNumbers && lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				if (lval.type == ExpResType.STRING && rval.type == ExpResType.STRING) {
					return ExpResult.makeStringResult(lval.stringVal.concat(rval.stringVal));
				}

				return ExpResult.makeNumResult(lval.value + rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				boolean bothNumbers = (lval.type==ExpResType.NUMBER) && (rval.type==ExpResType.NUMBER);
				boolean bothStrings = (lval.type==ExpResType.STRING) && (rval.type==ExpResType.STRING);
				if (!bothNumbers && !bothStrings) {
					ExpError err = new ExpError(source, pos, "Operator '+' requires two numbers or two strings");
					return ExpValResult.makeErrorRes(err);
				}

				if (bothStrings) {
					return ExpValResult.makeValidRes(ExpResType.STRING, DimensionlessUnit.class);
				}

				// Both numbers
				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, lval.unitType);
			}
		});

		addBinaryOp("-", 20, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				checkBothNumbers(lval, rval, source, pos);

				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source,int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value - rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
				if (numRes != null) {
					return numRes;
				}

				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, lval.unitType);
			}
		});

		addBinaryOp("*", 30, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				checkBothNumbers(lval, rval, source, pos);

				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				return ExpResult.makeNumResult(lval.value * rval.value, newType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
				if (numRes != null) {
					return numRes;
				}

				Class<? extends Unit> newType = context.multUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, newType);
			}
		});

		addBinaryOp("/", 30, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				checkBothNumbers(lval, rval, source, pos);

				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				return ExpResult.makeNumResult(lval.value / rval.value, newType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
				if (numRes != null) {
					return numRes;
				}

				Class<? extends Unit> newType = context.divUnitTypes(lval.unitType, rval.unitType);
				if (newType == null) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, newType);
			}
		});

		addBinaryOp("^", 40, true, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				checkBothNumbers(lval, rval, source, pos);

				if (lval.unitType != DimensionlessUnit.class ||
				    rval.unitType != DimensionlessUnit.class) {

					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(Math.pow(lval.value, rval.value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
				if (numRes != null) {
					return numRes;
				}

				if (	lval.unitType != DimensionlessUnit.class ||
						rval.unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
			}
		});

		addBinaryOp("%", 30, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {

				checkBothNumbers(lval, rval, source, pos);

				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value % rval.value, lval.unitType);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				ExpValResult numRes = validateBothNumbers(lval, rval, source, pos);
				if (numRes != null) {
					return numRes;
				}

				if (lval.unitType != rval.unitType) {
					ExpError error = new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, lval.unitType);
			}
		});

		addBinaryOp("==", 10, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkTypedComparison(context, lval, rval, source, pos);
			}

			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				boolean equal = evaluteTypedEquality(lval, rval);
				return ExpResult.makeNumResult(equal ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateTypedComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("!=", 10, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkTypedComparison(context, lval, rval, source, pos);
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				boolean equal = evaluteTypedEquality(lval, rval);
				return ExpResult.makeNumResult(!equal ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateTypedComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("&&", 8, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return ExpResult.makeNumResult((lval.value!=0) && (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("||", 6, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos){
				return ExpResult.makeNumResult((lval.value!=0) || (rval.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("<", 12, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value < rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp("<=", 12, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value <= rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp(">", 12, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value > rval.value ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addBinaryOp(">=", 12, false, new BinOpFunc() {
			@Override
			public void checkTypeAndUnits(ParseContext context, ExpResult lval,
					ExpResult rval, String source, int pos) throws ExpError {
				checkBothNumbers(lval, rval, source, pos);
				if (lval.unitType != rval.unitType) {
					throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(lval.value >= rval.value ? 1 : 0, DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.abs(args[0].value), args[0].unitType);
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
				return ExpResult.makeNumResult(Math.ceil(args[0].value), args[0].unitType);
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
				return ExpResult.makeNumResult(Math.floor(args[0].value), args[0].unitType);
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
				return ExpResult.makeNumResult(Math.signum(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[0].state == ExpValResult.State.VALID) {
					if (args[0].type == ExpResType.NUMBER) {
						return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
					}
					ExpError err = new ExpError(source, pos, "First parameter must be a number");
					return ExpValResult.makeErrorRes(err);
				} else {
					return args[0];
				}
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
				return ExpResult.makeNumResult(Math.sqrt(args[0].value), DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.cbrt(args[0].value), DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(index + 1, DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(index + 1, DimensionlessUnit.class);
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

				return ExpResult.makeNumResult(args[k].value, args[k].unitType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult mergedErrors = mergeMultipleErrors(args);
				if (mergedErrors != null)
					return mergedErrors;

				if (args[0].type != ExpResType.NUMBER) {
					ExpError error = new ExpError(source, pos, "Parameter must be a number");
					return ExpValResult.makeErrorRes(error);
				}
				if (args[0].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
					return ExpValResult.makeErrorRes(error);
				}

				for (int i = 2; i < args.length; ++ i) {
					if (args[i].type != ExpResType.NUMBER) {
						ExpError error = new ExpError(source, pos, String.format("Parameter #%d must be a number", i+1));
						return ExpValResult.makeErrorRes(error);
					}
					// TODO: allow choose to return non-number types
					if (args[1].unitType != args[i].unitType) {
						ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
						return ExpValResult.makeErrorRes(error);
					}
				}
				return ExpValResult.makeValidRes(args[1].type, args[1].unitType);
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
				return ExpResult.makeNumResult(Math.E, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.PI, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.sin(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateTrigFunction(context, args[0], source, pos);
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
				return ExpResult.makeNumResult(Math.cos(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateTrigFunction(context, args[0], source, pos);
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
				return ExpResult.makeNumResult(Math.tan(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateTrigFunction(context, args[0], source, pos);
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
				return ExpResult.makeNumResult(Math.asin(args[0].value), AngleUnit.class);
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
				return ExpResult.makeNumResult(Math.acos(args[0].value), AngleUnit.class);
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
				return ExpResult.makeNumResult(Math.atan(args[0].value), AngleUnit.class);
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
				return ExpResult.makeNumResult(Math.atan2(args[0].value, args[1].value), AngleUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult mergedErrors = mergeMultipleErrors(args);
				if (mergedErrors != null)
					return mergedErrors;

				if (args[0].type != ExpResType.NUMBER || args[1].type != ExpResType.NUMBER ) {
					ExpError error = new ExpError(source, pos, "Both parameters must be numbers");
					return ExpValResult.makeErrorRes(error);
				}

				if (args[0].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
					return ExpValResult.makeErrorRes(error);
				}
				if (args[1].unitType != DimensionlessUnit.class) {
					ExpError error = new ExpError(source, pos, getInvalidUnitString(args[1].unitType, DimensionlessUnit.class));
					return ExpValResult.makeErrorRes(error);
				}

				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.exp(args[0].value), DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.log(args[0].value), DimensionlessUnit.class);
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
				return ExpResult.makeNumResult(Math.log10(args[0].value), DimensionlessUnit.class);
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

	private static ExpNode optimizeAndValidateExpression(String input, ExpNode expNode, Expression exp) throws ExpError {
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

		exp.validationResult = valRes;

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

		expNode = optimizeAndValidateExpression(input, expNode, ret);

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

		return new BinaryOp(binOp.symbol, context, lhs, rhs, binOp.function, exp, pos);
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

		expNode = optimizeAndValidateExpression(input, expNode, ret.value);

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
		if (nextTok.type == ExpTokenizer.DSQ_TYPE) {
			// Return a literal string constant
			return new Constant(context, ExpResult.makeStringResult(nextTok.value), exp, nextTok.pos);
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
			return new UnaryOp(oe.symbol, context, expNode, oe.function, exp, nextTok.pos);
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

		return new Constant(context, ExpResult.makeNumResult(Double.parseDouble(constant)*mult, ut), exp, pos);
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

		return new FuncCall(fe.name, context, fe.function, arguments, exp, pos);
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

	private static ExpNode parseVariable(ParseContext context, ExpTokenizer.Token firstName, TokenList tokens, Expression exp, int pos) throws ExpError {

		ExpNode curExp = new Constant(context, context.getValFromName(firstName.value), exp, pos);
		while (true) {

			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE || !peeked.value.equals(".")) {
				break;
			}

			int outputPos = peeked.pos;

			// Next token is a '.' so parse a ResolveOutput node

			tokens.next(); // consume
			ExpTokenizer.Token outputName = tokens.next();
			if (outputName == null || outputName.type != ExpTokenizer.VAR_TYPE) {
				throw new ExpError(exp.source, peeked.pos, "Expected Identifier after '.'");
			}


			ExpNode indexExp = null;

			peeked = tokens.peek();
			if (peeked != null && peeked.value.equals("(")) {
				// Optional index
				tokens.next(); // consume
				indexExp = parseExp(context, tokens, 0, exp);
				tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source);
			}

			curExp = new ResolveOutput(context, outputName.value, curExp, indexExp, exp, outputPos);

		}

		return curExp;
	}

}
