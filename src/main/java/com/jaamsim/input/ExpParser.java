/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2018 JaamSim Software Inc.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import com.jaamsim.basicsim.ObjectType;
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

	public interface LazyBinOpFunc {
		public ExpResult apply(ParseContext pc, EvalContext ec, ExpNode lval, ExpNode rval, String source, int pos) throws ExpError;
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
		public void assign(ExpResult ent, ExpResult[] indices, ExpResult val) throws ExpError;
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
		public ParseClosure popClosure() {
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
			assert(topClose.freeVars.contains(varName));

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
			}
			catch (StackOverflowError e) {
				throw new ExpError(null, 0, "Excessive recursion detected in expression: %s, source");
			}
			finally {
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
		public ExpNode[] attribIndices;
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
				ExpResult[] indices = null;
				if (attribIndices != null) {
					indices = new ExpResult[attribIndices.length];
					for (int i = 0; i < attribIndices.length; ++i) {
						indices[i] = attribIndices[i].evaluate(ec);
					}
				}
				assigner.assign(ent, indices, value);

				return value;

			} finally {
				synchronized(executingThreads) {
					executingThreads.remove(Thread.currentThread());
				}
			}
		}
	}

	public abstract static class ExpNode {
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

			ArrayList<ExpResult> close = new ArrayList<>(vars.size());
			for (int i = 0; i < vars.size(); ++i) {
				if (i < params.size())
					close.add(params.get(i));
				else {
					close.add(vars.get(i));
				}
			}
			ec.pushClosure(close);
			ExpResult ret = body.evaluate(ec);
			ec.popClosure();

			return ret;
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

	private static class LazyBinaryOp extends BinaryOp {

		protected final LazyBinOpFunc lazyFunc;
		LazyBinaryOp(String name, ParseContext context, ExpNode lSubExp, ExpNode rSubExp, LazyBinOpFunc func, Expression exp, int pos) {
			super(name, context, lSubExp, rSubExp, null, exp, pos);
			this.lazyFunc = func;
		}

		@Override
		public ExpResult evaluate(EvalContext ec) throws ExpError {
			return lazyFunc.apply(context, ec, lSubExp, rSubExp, exp.source, tokenPos);
		}

		@Override
		public ExpNode getNoCheckVer() {
			return null;
		}
		@Override
		public ExpValResult validate() {
			ExpValResult lRes = lSubExp.validate();
			ExpValResult rRes = rSubExp.validate();

			ExpValResult res = lazyFunc.validate(context, lRes, rRes, exp.source, tokenPos);

			return res;
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
		public LazyBinOpFunc lazyFunction;
		public double bindingPower;
		public boolean rAssoc;
		public boolean isLazy;
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

	public static void addUnaryOp(String symbol, double bindPower, UnOpFunc func) {
		UnaryOpEntry oe = new UnaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		oe.bindingPower = bindPower;
		unaryOps.add(oe);
	}

	public static void addBinaryOp(String symbol, double bindPower, boolean rAssoc, BinOpFunc func) {
		BinaryOpEntry oe = new BinaryOpEntry();
		oe.symbol = symbol;
		oe.function = func;
		oe.bindingPower = bindPower;
		oe.rAssoc = rAssoc;
		oe.isLazy = false;
		binaryOps.add(oe);
	}

	public static void addLazyBinaryOp(String symbol, double bindPower, boolean rAssoc, LazyBinOpFunc func) {
		BinaryOpEntry oe = new BinaryOpEntry();
		oe.symbol = symbol;
		oe.lazyFunction = func;
		oe.bindingPower = bindPower;
		oe.rAssoc = rAssoc;
		oe.isLazy = true;
		binaryOps.add(oe);
	}

	public static void addFunction(String name, int numMinArgs, int numMaxArgs, CallableFunc func) {
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

	public static ArrayList<String> getFunctionNames() {
		ArrayList<String> ret = new ArrayList<>(functions.size());
		for (FunctionEntry fe : functions) {
			ret.add(fe.name);
		}
		Collections.sort(ret, Input.uiSortOrder);
		return ret;
	}

	////////////////////////////////////////////////////////
	// Statically initialize the operators and functions

	static {
		ExpOperators.InitOperatorsAndFuncs();

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

	public static void assertUnitType(Expression exp, Class<? extends Unit> unitType) {
		if (exp.validationResult.state != ExpValResult.State.VALID
				|| exp.validationResult.type != ExpResType.NUMBER)
			return;

		if (exp.validationResult.unitType != unitType) {
			throw new InputErrorException("Invalid unit returned by an expression: '%s'%n"
					+ "Received: %s, expected: %s",
					exp,
					ObjectType.getObjectTypeForClass(exp.validationResult.unitType),
					ObjectType.getObjectTypeForClass(unitType));
		}
	}

	public static void assertResultType(Expression exp, ExpResType type) {
		if (exp.validationResult.state != ExpValResult.State.VALID)
			return;

		if (exp.validationResult.type != type) {
			throw new InputErrorException("Incorrect result type returned by expression: '%s'%n"
					+ "Received: %s, expected: %s",
					exp, exp.validationResult.type, type);
		}
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

		if (binOp.isLazy)
			return new LazyBinaryOp(binOp.symbol, context, lhs, rhs, binOp.lazyFunction, exp, pos);
		else
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

		// Note we use a linked list here, as we will be adding nodes in reverse order
		LinkedList<ExpNode> indexExps = new LinkedList<>();

		// Check for an optional index at the end
		while(lhsNode instanceof IndexCollection) {
			// the lhs ended with an index, split that off
			IndexCollection ind = (IndexCollection)lhsNode;
			if (ind.indices.size() != 1)
				throw new ExpError(input, lhsNode.tokenPos, "Assignment to collections can only take a single index");

			ExpNode indexExp = ind.indices.get(0);
			indexExp = optimizeAndValidateExpression(input, indexExp, ret);

			indexExps.push(indexExp);
			lhsNode = ind.collection;

		}
		if (indexExps.size() > 0) {

			ret.attribIndices = indexExps.toArray(new ExpNode[indexExps.size()]);
		} else {
			ret.attribIndices = null;
		}

		// Now make sure the last node of the lhs ends with a output resolve
		if (!(lhsNode instanceof ResolveOutput)) {
			throw new ExpError(input, lhsNode.tokenPos, "Assignment left side must end with an output, followed by optional indices");
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

		if (nextTok.type == ExpTokenizer.NULL_TYPE) {
			return new Constant(context, ExpResult.makeEntityResult(null), exp, nextTok.pos);
		}

		if (nextTok.type == ExpTokenizer.VAR_TYPE) {
			ExpTokenizer.Token peeked = tokens.peek();
			if (peeked != null && peeked.type == ExpTokenizer.SYM_TYPE && peeked.value.equals("=")) {
				return parseLocalVar(context, nextTok.value, tokens, exp, nextTok.pos);
			}
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
		if (peeked == null) {
			throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in argument list");
		}
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
			if (peeked == null) {
				throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in argument list");
			}
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
		if (peeked == null) {
			throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in lambda parameter list");
		}

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
				if (peeked == null) {
					throw new ExpError(exp.source, exp.source.length(), "Unexpected end of input in lambda parameter list");
				}
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
		context.popClosure();

		tokens.expect(ExpTokenizer.SYM_TYPE, ")", exp.source);

		// Create the mapping needed to capture the free variables needed when this lambda is executed
		int[] varMap = new int[pc.boundVars.size() + pc.freeVars.size()];
		for (int i = 0; i < varMap.length; ++i) {
			if (i < pc.boundVars.size()) {
				varMap[i] = -1;
			} else {
				varMap[i] = context.getVarIndex(pc.freeVars.get(i - pc.boundVars.size()));
			}
		}

		return new LambdaNode(context, lambdaBody, varMap, vars.size(), exp, pos);

	}

	private static ExpNode parseLocalVar(ParseContext context, String varName, TokenList tokens, Expression exp, int pos) throws ExpError {

		if (context.isVarName(varName)) {
			throw new ExpError(exp.source, pos, "Can not declare a local variable with the same name as existing variable: %s", varName);
		}

		tokens.expect(ExpTokenizer.SYM_TYPE, "=", exp.source);
		ExpNode varBody = parseExp(context, tokens, 0, exp);
		tokens.expect(ExpTokenizer.SYM_TYPE, ";", exp.source);

		ParseClosure pc = new ParseClosure();
		pc.boundVars.add(varName);

		context.pushClosure(pc);
		ExpNode mainExp = parseExp(context, tokens, 0, exp);
		context.popClosure();

		// Create the mapping needed to capture the free variables needed when this lambda is executed
		int[] varMap = new int[pc.freeVars.size() + 1];
		for (int i = 0; i < varMap.length; ++i) {
			if (i < 1) {
				varMap[i] = -1;
			} else {
				varMap[i] = context.getVarIndex(pc.freeVars.get(i - 1));
			}
		}

		// Treat a local variable as an implicit lambda of a single variable followed immediately by it's evaluation
		ExpNode ln = new LambdaNode(context, mainExp, varMap, 1, exp, pos);

		ArrayList<ExpNode> indices = new ArrayList<>();
		indices.add(varBody);

		ExpNode evalNode = new IndexCollection(context, ln, indices, exp, pos);
		return evalNode;
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
