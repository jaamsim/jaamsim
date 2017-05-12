/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016 JaamSim Software Inc.
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
import java.util.HashMap;

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
		public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError;
		public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos);
	}

	public static class UnitData {
		double scaleFactor;
		Class<? extends Unit> unitType;
	}

	public interface OutputResolver {
		public ExpResult resolve(EvalContext ec, ExpResult ent) throws ExpError;
		public ExpValResult validate(ExpValResult entValRes);
	}
	public interface Assigner {
		public void assign(ExpResult ent, ExpResult index, ExpResult val) throws ExpError;
	}

	private static class ParseClosure {
		public HashMap<String, ExpResult> parseConstants = new HashMap<>();
		public ArrayList<String> freeVars = new ArrayList<>();
		public ArrayList<String> boundVars = new ArrayList<>();
	}

	public static abstract class ParseContext {
		public ParseContext(HashMap<String, ExpResult> constVals) {
			ParseClosure initClosure = new ParseClosure();
			initClosure.parseConstants = constVals;
			closureStack.add(initClosure);
		}
		public abstract UnitData getUnitByName(String name);
		public abstract Class<? extends Unit> multUnitTypes(Class<? extends Unit> a, Class<? extends Unit> b);
		public abstract  Class<? extends Unit> divUnitTypes(Class<? extends Unit> num, Class<? extends Unit> denom);

		public abstract ExpResult getValFromLitName(String name, String source, int pos) throws ExpError;
		public abstract OutputResolver getOutputResolver(String name) throws ExpError;
		public abstract OutputResolver getConstOutputResolver(ExpResult constEnt, String name) throws ExpError;


		public abstract Assigner getAssigner(String attribName) throws ExpError;
		public abstract Assigner getConstAssigner(ExpResult constEnt, String attribName) throws ExpError;

		public ArrayList<ParseClosure> closureStack = new ArrayList<>();

		public void pushClosure(ParseClosure close) {
			closureStack.add(close);
		}
		public ParseClosure popClosure(ParseClosure close) {
			return closureStack.remove(closureStack.size()-1);
		}

		public boolean isVarName(String varName) {
			// Check the constant vars and bound vars for the whole stack to see if this is a valid variable
			for (ParseClosure close : closureStack) {
				if (close.parseConstants.containsKey(varName)) {
					return true;
				}
				if (close.boundVars.contains(varName)) {
					return true;
				}
			}
			return false;
		}

		public boolean isVarConstant(String varName) {
			for (ParseClosure close : closureStack) {
				if (close.parseConstants.containsKey(varName)) {
					return true;
				}
			}
			return false;
		}

		public void referenceVar(String varName, String source, int pos) throws ExpError {
			boolean pastVar = false;
			for (int i = 0; i < closureStack.size(); i++) {
				ParseClosure closure = closureStack.get(i);
				if (pastVar) {
					if (!closure.freeVars.contains(varName))
						closure.freeVars.add(varName);
				} else {
					if (closure.boundVars.contains(varName)) {
						pastVar = true;
					}
				}
			}
			if (!pastVar) {
				// Trying to reference an variable not bound on the entire closure stack
				throw new ExpError(source, pos, String.format("Unknown variable: %s", varName));
			}
		}

		public int getVarIndex(String varName) {
			// The index logic is that bound variables take the first indices, then free variables follow
			// in the order they are referenced
			ParseClosure topClose = closureStack.get(closureStack.size()-1);
			if (topClose.boundVars.contains(varName)) {
				return topClose.boundVars.indexOf(varName);
			}
			assert(topClose.boundVars.contains(varName));

			return topClose.freeVars.indexOf(varName) + topClose.boundVars.size();
		}

		public ExpResult getValFromConstVar(String varName, String source, int pos) throws ExpError {
			for (ParseClosure close : closureStack) {
				if (close.parseConstants.containsKey(varName)) {
					return close.parseConstants.get(varName);
				}
			}
			throw new ExpError(source, pos, String.format("Unknown constant variable: %s", varName));

		}
	}

	public static class EvalContext {
		private final ArrayList<ArrayList<ExpResult> > closureStack = new ArrayList<>();

		public EvalContext() {
			closureStack.add(new ArrayList<ExpResult>());
		}

		public void pushClosure(ArrayList<ExpResult> closure) {
			closureStack.add(closure);
		}
		public void popClosure() {
			closureStack.remove(closureStack.size()-1);
		}
		public ArrayList<ExpResult> getCurrentClosure() {
			return closureStack.get(closureStack.size()-1);
		}
	}

	private interface ExpressionWalker {
		public void visit(ExpNode exp) throws ExpError;
		public ExpNode updateRef(ExpNode exp) throws ExpError;
	}

	////////////////////////////////////////////////////////////////////
	// Expression types

	public static class Expression {
		public final String source;

		public ExpValResult validationResult;

		protected final ArrayList<Thread> executingThreads = new ArrayList<>();

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

	public static class Assignment extends Expression {
		public ExpNode entExp;
		public ExpNode attribIndex;
		public ExpNode valueExp;
		public Assigner assigner;
		public Assignment(String source) {
			super(source);
		}
		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			synchronized(executingThreads) {
				if (executingThreads.contains(Thread.currentThread())) {
					throw new ExpError(null, 0, "Expression recursion detected for expression: %s", source);
				}

				executingThreads.add(Thread.currentThread());
			}
			try {
				ExpResult ent = entExp.evaluate(ec);
				ExpResult value = valueExp.evaluate(ec);
				ExpResult index = null;
				if (attribIndex != null) {
					index = attribIndex.evaluate(ec);
				}
				assigner.assign(ent, index, value);

				return value;

			} finally {
				synchronized(executingThreads) {
					executingThreads.remove(Thread.currentThread());
				}
			}
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

	private static class Variable extends ExpNode {
		public int varIndex;
		public Variable(ParseContext context, int varIndex, Expression exp, int pos) {
			super(context, exp, pos);
			this.varIndex = varIndex;
		}
		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ArrayList<ExpResult> close = ec.getCurrentClosure();
			return close.get(varIndex);
		}

		@Override
		public ExpValResult validate() {
			return ExpValResult.makeUndecidableRes();
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			w.visit(this);
		}
	}

	public static class LambdaClosure {
		private final ExpNode body;
		private final ArrayList<ExpResult> vars;
		private final int numParams;

		public LambdaClosure(ExpNode body, ArrayList<ExpResult> vars, int numParams) {
			this.body = body;
			this.vars = vars;
			this.numParams = numParams;
		}

		public ExpResult evaluate(EvalContext ec, ArrayList<ExpResult> params) throws ExpError {
			// Fill in the context
			for (int i = 0; i < params.size(); ++i) {
				vars.set(i, params.get(i));
			}
			ec.pushClosure(vars);
			return body.evaluate(ec);
		}

		public int getNumParams() {
			return numParams;
		}
	}

	private static class LambdaNode extends ExpNode {
		private ExpNode lambdaBody;
		private final int[] varMap;
		private final int numParams;
		public LambdaNode(ParseContext context, ExpNode lambdaBody, int[] varMap, int numParams, Expression exp, int pos) {
			super(context, exp, pos);
			this.lambdaBody = lambdaBody;
			this.varMap = varMap;
			this.numParams = numParams;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			ArrayList<ExpResult> close = ec.getCurrentClosure();

			ArrayList<ExpResult> vars = new ArrayList<>(varMap.length);

			for (int i : varMap) {
				if (i < 0) {
					vars.add(null);
				} else {
					vars.add(close.get(i).getCopy());
				}
			}
			LambdaClosure lc = new LambdaClosure(lambdaBody, vars, numParams);
			return ExpResult.makeLambdaResult(lc);
		}

		@Override
		public ExpValResult validate() {
			return ExpValResult.makeUndecidableRes();
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			lambdaBody.walk(w);
			lambdaBody = w.updateRef(lambdaBody);

			w.visit(this);
		}
	}

	private static class ResolveOutput extends ExpNode {
		public ExpNode entNode;
		public String outputName;

		private final OutputResolver resolver;

		public ResolveOutput(ParseContext context, String outputName, ExpNode entNode, Expression exp, int pos) throws ExpError {
			super(context, exp, pos);

			this.entNode = entNode;
			this.outputName = outputName;

			try {
				if (entNode instanceof Constant) {
					this.resolver = context.getConstOutputResolver(entNode.evaluate(null), outputName);
				} else {
					this.resolver = context.getOutputResolver(outputName);
				}
			} catch (ExpError ex) {
				throw (fixError(ex, exp.source, pos));
			}

		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			try {
				ExpResult ent = entNode.evaluate(ec);

				return resolver.resolve(ec, ent);
			} catch (ExpError ex) {
				throw fixError(ex, exp.source, tokenPos);
			}

		}
		@Override
		public ExpValResult validate() {
			ExpValResult entValRes = entNode.validate();

			if (    entValRes.state == ExpValResult.State.ERROR ||
			        entValRes.state == ExpValResult.State.UNDECIDABLE) {
				fixValidationErrors(entValRes, exp.source, tokenPos);
				return entValRes;
			}
			ExpValResult res =  resolver.validate(entValRes);
			fixValidationErrors(res, exp.source, tokenPos);
			return res;
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			entNode.walk(w);
			entNode = w.updateRef(entNode);

			w.visit(this);
		}
	}

	private static class IndexCollection extends ExpNode {
		public ExpNode collection;
		public ArrayList<ExpNode> indices;

		public IndexCollection(ParseContext context, ExpNode collection, ArrayList<ExpNode> indices, Expression exp, int pos) throws ExpError {
			super(context, exp, pos);

			this.collection = collection;
			this.indices = indices;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			try {
				ExpResult colRes = collection.evaluate(ec);
				ArrayList<ExpResult> indResults = new ArrayList<>();
				for (ExpNode ind : indices) {
					indResults.add(ind.evaluate(ec));
				}

				if (colRes.type == ExpResType.COLLECTION) {
					if (indResults.size() != 1) {
						throw new ExpError(exp.source, tokenPos, "Collections can only be indexed with a single index");
					}
					return colRes.colVal.index(indResults.get(0));
				}
				if (colRes.type == ExpResType.LAMBDA) {
					if (indResults.size() != colRes.lcVal.getNumParams()) {
						throw new ExpError(exp.source, tokenPos, "Invalid number of parameter for lambda. Got: %d, expected: %d",
								indResults.size(), colRes.lcVal.getNumParams());
					}
					return colRes.lcVal.evaluate(ec, indResults);
				}

				throw new ExpError(exp.source, tokenPos, "Expression does not evaluate to a collection or lambda type.");

			} catch (ExpError ex) {
				throw fixError(ex, exp.source, tokenPos);
			}
		}
		@Override
		public ExpValResult validate() {
			ExpValResult colValRes = collection.validate();

			if (colValRes.state == ExpValResult.State.ERROR) {
				fixValidationErrors(colValRes, exp.source, tokenPos);
				return colValRes;
			}
			if (colValRes.state == ExpValResult.State.UNDECIDABLE) {
				return colValRes;
			}
			if (colValRes.type != ExpResType.COLLECTION && colValRes.type != ExpResType.LAMBDA) {
				return ExpValResult.makeErrorRes(new ExpError(exp.source, tokenPos, "Expression does not evaluate to a collection or lambda type."));
			}

			for (ExpNode ind : indices) {
				ExpValResult indValRes = ind.validate();

				if (indValRes.state == ExpValResult.State.ERROR) {
					fixValidationErrors(indValRes, exp.source, tokenPos);
					return indValRes;
				}
				if (indValRes.state == ExpValResult.State.UNDECIDABLE) {
					return indValRes;
				}
			}

			// TODO: validate collection types
			return ExpValResult.makeUndecidableRes();
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			collection.walk(w);
			collection = w.updateRef(collection);

			for (int i = 0; i < indices.size(); ++i) {
				indices.get(i).walk(w);
				ExpNode updated = w.updateRef(indices.get(i));
				indices.set(i, updated);
			}

			w.visit(this);
		}
	}

	private static class BuildArray extends ExpNode {
		public ArrayList<ExpNode> values;

		public BuildArray(ParseContext context, ArrayList<ExpNode> valueExps, Expression exp, int pos) throws ExpError {
			super(context, exp, pos);

			this.values = valueExps;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			try {
				ArrayList<ExpResult> res = new ArrayList<>();
				for (ExpNode e : values) {
					res.add(e.evaluate(ec));
				}
				// Using the heuristic that if the eval context is null, this is probably an evaluation as part of constant folding
				// even if this is wrong, the behavior will be correct, but possibly a bit slower
				boolean isConstant = ec == null;
				return ExpCollections.makeExpressionCollection(res, isConstant);
			} catch (ExpError ex) {
				throw fixError(ex, exp.source, tokenPos);
			}

		}
		@Override
		public ExpValResult validate() {
			for (ExpNode val : values) {
				ExpValResult valRes = val.validate();

				if (    valRes.state == ExpValResult.State.ERROR ||
				        valRes.state == ExpValResult.State.UNDECIDABLE) {
					fixValidationErrors(valRes, exp.source, tokenPos);
					return valRes;
				}
			}

			return ExpValResult.makeValidRes(ExpResType.COLLECTION, DimensionlessUnit.class);
		}

		@Override
		void walk(ExpressionWalker w) throws ExpError {
			for (int i = 0; i < values.size(); ++i) {
				values.get(i).walk(w);
				values.set(i, w.updateRef(values.get(i)));
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


	private static class Conditional extends ExpNode {
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

			// Check that both sides of the branch return the same unit types, for numerical types
			if (trueRes.type == ExpResType.NUMBER && trueRes.unitType != falseRes.unitType) {

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

	private static class FuncCall extends ExpNode {
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
			return function.call(ec, argVals, exp.source, tokenPos);
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
			return function.call(ec, argVals, exp.source, tokenPos);
		}

	}

	// Some errors can be throw without a known source or position, update such errors with the given info
	private static ExpError fixError(ExpError ex, String source, int pos) {
		ExpError exFixed = ex;
		if (ex.source == null) {
			exFixed = new ExpError(source, pos, ex.getMessage());
		}
		return exFixed;
	}

	private static void fixValidationErrors(ExpValResult res, String source, int pos) {
		if (res.state == ExpValResult.State.ERROR ) {
			for (int i = 0; i < res.errors.size(); ++i) {
				res.errors.set(i, fixError(res.errors.get(i), source, pos));
			}
		}
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

	// Make sure the single argument is a collection
	private static ExpValResult validateCollection(ParseContext context, ExpValResult arg, String source, int pos) {
		if (  arg.state == ExpValResult.State.ERROR ||
		      arg.state == ExpValResult.State.UNDECIDABLE) {
			return arg;
		}
		// Check that the argument is a collection
		if (arg.type != ExpResType.COLLECTION) {
			ExpError error = new ExpError(source, pos, "Expected Collection type argument");
			return ExpValResult.makeErrorRes(error);
		}

		return ExpValResult.makeUndecidableRes();
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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

		addFunction("maxCol", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					throw new ExpError(source, pos, "Can not get max of empty collection");
				}
				ExpResult ret = col.index(it.nextKey());
				Class<? extends Unit> ut = ret.unitType;
				if (ret.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Can not take max of non-numeric type in collection");
				}

				while (it.hasNext()) {
					ExpResult comp = col.index(it.nextKey());
					if (comp.unitType != ut) {
						throw new ExpError(source, pos, "Unmatched Unit types in collection: %s, %s",
						                   ut.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not take max of non-numeric type in collection");
					}
					if (comp.value > ret.value) {
						ret = comp;
					}
				}
				return ret;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateCollection(context, args[0], source, pos);
			}
		});

		addFunction("minCol", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					throw new ExpError(source, pos, "Can not get min of empty collection");
				}
				ExpResult ret = col.index(it.nextKey());
				Class<? extends Unit> ut = ret.unitType;
				if (ret.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Can not take min of non-numeric type in collection");
				}

				while (it.hasNext()) {
					ExpResult comp = col.index(it.nextKey());
					if (comp.unitType != ut) {
						throw new ExpError(source, pos, "Unmatched Unit types in collection: %s, %s",
						                   ut.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not take min of non-numeric type in collection");
					}
					if (comp.value < ret.value) {
						ret = comp;
					}
				}
				return ret;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateCollection(context, args[0], source, pos);
			}
		});

		addFunction("abs", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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

		addFunction("indexOfMaxCol", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					throw new ExpError(source, pos, "Can not get max of empty collection");
				}
				ExpResult retKey = it.nextKey();
				ExpResult ret = col.index(retKey);
				Class<? extends Unit> ut = ret.unitType;
				if (ret.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Can not take max of non-numeric type in collection");
				}

				while (it.hasNext()) {
					ExpResult compKey = it.nextKey();
					ExpResult comp = col.index(compKey);
					if (comp.unitType != ut) {
						throw new ExpError(source, pos, "Unmatched Unit types in collection: %s, %s",
						                   ut.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not take max of non-numeric type in collection");
					}
					if (comp.value > ret.value) {
						ret = comp;
						retKey = compKey;
					}
				}
				return retKey;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateCollection(context, args[0], source, pos);
			}
		});
		addFunction("indexOfMinCol", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					throw new ExpError(source, pos, "Can not get min of empty collection");
				}
				ExpResult retKey = it.nextKey();
				ExpResult ret = col.index(retKey);
				Class<? extends Unit> ut = ret.unitType;
				if (ret.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Can not take min of non-numeric type in collection");
				}

				while (it.hasNext()) {
					ExpResult compKey = it.nextKey();
					ExpResult comp = col.index(compKey);
					if (comp.unitType != ut) {
						throw new ExpError(source, pos, "Unmatched Unit types in collection: %s, %s",
						                   ut.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not take min of non-numeric type in collection");
					}
					if (comp.value < ret.value) {
						ret = comp;
						retKey = compKey;
					}
				}
				return retKey;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateCollection(context, args[0], source, pos);
			}
		});
		addFunction("indexOfNearest", 2, 2, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument as first argument.");
				}
				if (args[1].type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Expected numerical argument as second argument.");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult nearPoint = args[1];

				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					throw new ExpError(source, pos, "Can not get nearest value of empty collection.");
				}

				double nearestDist = Double.MAX_VALUE;
				ExpResult retKey = null;

				while (it.hasNext()) {
					ExpResult compKey = it.nextKey();
					ExpResult comp = col.index(compKey);
					if (comp.unitType != nearPoint.unitType) {
						throw new ExpError(source, pos, "Unmatched Unit types when finding nearest: %s, %s",
						                   nearPoint.unitType.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not find nearest value of non-numeric type in collection.");
					}
					double dist = Math.abs(comp.value - nearPoint.value);
					if (dist < nearestDist) {
						nearestDist = dist;
						retKey = compKey;
					}
				}
				return retKey;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[1].state == ExpValResult.State.ERROR || args[1].state == ExpValResult.State.UNDECIDABLE) {
					return args[1];
				}
				if (args[1].type != ExpResType.NUMBER) {
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "Second argument to 'indexOfNearest' must be a number."));
				}

				return validateCollection(context, args[0], source, pos);
			}
		});

		addFunction("map", 2, 2, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[1].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument as second argument.");
				}
				if (args[0].type != ExpResType.LAMBDA) {
					throw new ExpError(source, pos, "Expected function argument as first argument.");
				}

				LambdaClosure mapFunc = args[0].lcVal;
				if (mapFunc.numParams != 1) {
					throw new ExpError(source, pos, "Function passed to 'map' must take one parameter.");
				}

				ExpResult.Collection col = args[1].colVal;
				ExpResult.Iterator it = col.getIter();

				Class<? extends Unit> unitType = null;
				boolean firstVal = true;
				ArrayList<ExpResult> params = new ArrayList<>(1);

				ArrayList<ExpResult> results = new ArrayList<>();
				params.add(null);
				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult val = col.index(key);
					params.set(0, val);

					ExpResult result = mapFunc.evaluate(context, params);

					Class<? extends Unit> resUnitType = result.type == ExpResType.NUMBER ? result.unitType : null;
					if (firstVal) {
						unitType = resUnitType;
					} else {
						if (unitType != resUnitType) {
							throw new ExpError(source, pos, "All unit types of map results must match");
						}
					}
					results.add(result);
				}
				return ExpCollections.getCollection(results, unitType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[0].state == ExpValResult.State.ERROR || args[0].state == ExpValResult.State.UNDECIDABLE) {
					return args[0];
				}
				if (args[0].type != ExpResType.LAMBDA) {
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "First argument to 'map' must be a function."));
				}

				return validateCollection(context, args[1], source, pos);
			}
		});

		addFunction("size", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument");
				}

				ExpResult.Collection col = args[0].colVal;
				return ExpResult.makeNumResult(col.getSize(), DimensionlessUnit.class);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateCollection(context, args[0], source, pos);
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				int k = (int) args[0].value;
				if (k < 1 || k >= args.length)
					throw new ExpError(source, pos,
							String.format("Invalid index: %s. Index must be between 1 and %s.", k, args.length-1));

				return args[k];
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

				if (args.length < 2) {
					ExpError error = new ExpError(source, pos, "'choose' function must take at least 2 parameters.");
					return ExpValResult.makeErrorRes(error);
				}
				ExpResType valType = args[1].type;
				Class<? extends Unit> valUnitType = args[1].unitType;

				for (int i = 2; i < args.length; ++ i) {
					if (args[i].type != valType) {
						ExpError error = new ExpError(source, pos, "All parameter types to 'choose' must be the same type");
						return ExpValResult.makeErrorRes(error);
					}

					if (valType == ExpResType.NUMBER && valUnitType != args[i].unitType) {
						ExpError error = new ExpError(source, pos, getInvalidUnitString(args[0].unitType, DimensionlessUnit.class));
						return ExpValResult.makeErrorRes(error);
					}
				}
				return ExpValResult.makeValidRes(valType, valUnitType);
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
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
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				return ExpResult.makeNumResult(Math.log10(args[0].value), DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				return validateSingleArgDimensionless(context, args[0], source, pos);
			}
		});

		addFunction("notNull", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.ENTITY) {
					throw new ExpError(source, pos, "notNull requires entity as argument");
				}
			}
			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {

				return ExpResult.makeNumResult(args[0].entVal == null ? 0 : 1, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (	args[0].state == ExpValResult.State.ERROR ||
						args[0].state == ExpValResult.State.UNDECIDABLE)
					return args[0];
				if (args[0].type != ExpResType.ENTITY) {
					ExpError error = new ExpError(source, pos, "Argument must be an entity");
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);

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
		private final ArrayList<ExpTokenizer.Token> tokens;
		private int pos;

		public TokenList(ArrayList<ExpTokenizer.Token> tokens) {
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
			if (origNode instanceof BuildArray) {
				BuildArray ba = (BuildArray)origNode;
				boolean constArgs = true;
				for (ExpNode val : ba.values) {
					if (!(val instanceof Constant))
						constArgs = false;
				}
				if (constArgs) {
					ExpResult val = ba.evaluate(null);
					return new Constant(ba.context, val, origNode.exp, ba.tokenPos);
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
		// Parse as many indices as are present
		while (true) {
			ExpTokenizer.Token peeked = tokens.peek();

			if (peeked == null || peeked.type != ExpTokenizer.SYM_TYPE) {
				break;
			}

			if (peeked.value.equals(".")) {
				tokens.next(); // consume
				ExpTokenizer.Token outputName = tokens.next();
				if (outputName == null || outputName.type != ExpTokenizer.VAR_TYPE) {
					throw new ExpError(exp.source, peeked.pos, "Expected Identifier after '.'");
				}

				lhs = new ResolveOutput(context, outputName.value, lhs, exp, peeked.pos);
				continue;
			}

			if (peeked.value.equals("(")) {
				ArrayList<ExpNode> indices = parseIndices(context, tokens, exp);

				lhs = new IndexCollection(context, lhs, indices, exp, peeked.pos);
				continue;
			}

			// Not an index or output. Move on
			break;
		}

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

		Assignment ret = new Assignment(input);
		ExpNode lhsNode = parseExp(context, tokens, 0, ret);

		tokens.expect(ExpTokenizer.SYM_TYPE, "=", input);

		ExpNode rhsNode = parseExp(context, tokens, 0, ret);

		// Make sure we've parsed all the tokens
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked != null) {
			throw new ExpError(input, peeked.pos, "Unexpected additional values");
		}

		rhsNode = optimizeAndValidateExpression(input, rhsNode, ret);
		ret.valueExp = rhsNode;

		// Parsing is done, now we need to unwind the lhs expression to get the necessary components
		ExpNode indexExp = null;

		// Check for an optional index at the end
		if (lhsNode instanceof IndexCollection) {
			// the lhs ended with an index, split that off
			IndexCollection ind = (IndexCollection)lhsNode;
			if (ind.indices.size() != 1)
				throw new ExpError(input, lhsNode.tokenPos, "Assignment to collections can only take a single index");

			indexExp = ind.indices.get(0);
			lhsNode = ind.collection;

			indexExp = optimizeAndValidateExpression(input, indexExp, ret);
		}
		ret.attribIndex = indexExp;

		// Now make sure the last node of the lhs ends with a output resolve
		if (!(lhsNode instanceof ResolveOutput)) {
			throw new ExpError(input, lhsNode.tokenPos, "Assignment left side must end with an output");
		}

		ResolveOutput lhsResolve = (ResolveOutput)lhsNode;
		ExpNode entNode = lhsResolve.entNode;

		entNode = optimizeAndValidateExpression(input, entNode, ret);
		ret.entExp = entNode;

		if (ret.entExp instanceof Constant) {
			ExpResult ent = ret.entExp.evaluate(null);
			ret.assigner = context.getConstAssigner(ent, lhsResolve.outputName);
		} else {
			ret.assigner = context.getAssigner(lhsResolve.outputName);
		}

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
		if (nextTok.type == ExpTokenizer.STRING_TYPE) {

			// Return a literal string constant
			return new Constant(context, ExpResult.makeStringResult(nextTok.value), exp, nextTok.pos);
		}

		if (nextTok.type == ExpTokenizer.SQ_TYPE) {
			ExpResult namedVal = context.getValFromLitName(nextTok.value, exp.source, nextTok.pos);
			return new Constant(context, namedVal, exp, nextTok.pos);
		}

		if (nextTok.type == ExpTokenizer.VAR_TYPE) {
			if (context.isVarName(nextTok.value)) {
				if (context.isVarConstant(nextTok.value)) {
					ExpResult namedVal = context.getValFromConstVar(nextTok.value, exp.source, nextTok.pos);
					return new Constant(context, namedVal, exp, nextTok.pos);
				} else {
					context.referenceVar(nextTok.value, exp.source, nextTok.pos);
					int varIndex = context.getVarIndex(nextTok.value);

					return new Variable(context, varIndex, exp, nextTok.pos);
				}
			} else if (getFunctionEntry(nextTok.value) != null){
				return parseFuncCall(context, nextTok.value, tokens, exp, nextTok.pos);
			} else {
				throw new ExpError(exp.source, nextTok.pos, "Unknown variable or function: %s", nextTok.value);
			}
		}

		// The next token must be a symbol
		if (nextTok.value.equals("{")) {

			boolean foundComma = true;

			ArrayList<ExpNode> exps = new ArrayList<>();
			while(true) {
			// Parse an array
				ExpTokenizer.Token peeked = tokens.peek();
				if (peeked != null && peeked.value.equals("}")) {
					tokens.next(); // consume
					break;
				}

				if (!foundComma) {
					throw new ExpError(exp.source, peeked.pos, "Expected ',' or '}' in literal array.");
				}
				foundComma = false;

				exps.add(parseExp(context, tokens, 0, exp));
				peeked = tokens.peek();
				if (peeked != null && peeked.value.equals(",")) {
					tokens.next();
					foundComma = true;
				}
			}
			return new BuildArray(context, exps, exp, nextTok.pos);
		}

		if (nextTok.value.equals("|")) {
			return parseLambda(context, tokens, exp, nextTok.pos);
		}

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

	private static ArrayList<ExpNode> parseIndices(ParseContext context, TokenList tokens, Expression exp) throws ExpError {
		tokens.next(); // consume '('
		ExpTokenizer.Token peeked = tokens.peek();
		if (peeked.value.equals(")")) {
			// Empty list
			tokens.next();
			return new ArrayList<>();
		}

		ArrayList<ExpNode> indices = new ArrayList<>();
		while (true) {
			ExpNode indexExp = parseExp(context, tokens, 0, exp);
			indices.add(indexExp);

			peeked = tokens.peek();
			if (peeked.value.equals(")")) {
				break;
			}
			if (peeked.value.equals(",")) {
				tokens.next();
				continue;
			}
			throw new ExpError(exp.source, peeked.pos, "Unexpected token in index list");
		}

		tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source);
		return indices;
	}

	private static ExpNode parseLambda(ParseContext context, TokenList tokens, Expression exp, int pos) throws ExpError {
		ExpTokenizer.Token peeked = tokens.peek();

		ArrayList<String> vars = new ArrayList<>();
		if (!peeked.value.equals("|")) {
			while (true) {
				if (peeked.type != ExpTokenizer.VAR_TYPE) {
					throw new ExpError(exp.source, peeked.pos, "Expected variable name in lambda parameter list");
				}
				if (context.isVarName(peeked.value)) {
					throw new ExpError(exp.source, peeked.pos, "Variable name is the same as existing variable.");
				}
				if (getFunctionEntry(peeked.value) != null) {
					throw new ExpError(exp.source, peeked.pos, "Variable name is the same as built-in function name.");
				}
				vars.add(peeked.value);
				// consume var name
				tokens.next();
				peeked = tokens.peek();
				if (peeked.value.equals("|")) {
					break;
				}
				if (peeked.value.equals(",")) {
					tokens.next();
					peeked = tokens.peek();
					continue;
				}
				throw new ExpError(exp.source, peeked.pos, "Unexpected token in lambda parameter list");
			}
		}
		tokens.expect(ExpTokenizer.SYM_TYPE, "|", exp.source);

		tokens.expect(ExpTokenizer.SYM_TYPE, "(", exp.source);

		ParseClosure pc = new ParseClosure();
		pc.boundVars = vars;

		context.pushClosure(pc);
		ExpNode lambdaBody = parseExp(context, tokens, 0, exp);
		context.popClosure(pc);

		tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source);

		// Create the mapping needed to capture the free variables needed when this lambda is executed
		int[] varMap = new int[pc.boundVars.size() + pc.freeVars.size()];
		for (int i = 0; i < varMap.length; ++i) {
			if (i < pc.boundVars.size()) {
				varMap[i] = -1;
			} else {
				varMap[i] = context.getVarIndex(vars.get(i - pc.boundVars.size()));
			}
		}

		return new LambdaNode(context, lambdaBody, varMap, vars.size(), exp, pos);

	}
	private static ExpNode parseFuncCall(ParseContext context, String funcName, TokenList tokens, Expression exp, int pos) throws ExpError {

		FunctionEntry fe = getFunctionEntry(funcName);
		if (fe == null) {
			throw new ExpError(exp.source, pos, "Uknown function or variable: \"%s\"", funcName);
		}

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

}
