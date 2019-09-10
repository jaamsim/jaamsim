/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2019 JaamSim Software Inc.
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
import java.util.Comparator;

import com.jaamsim.input.ExpParser.BinOpFunc;
import com.jaamsim.input.ExpParser.CallableFunc;
import com.jaamsim.input.ExpParser.EvalContext;
import com.jaamsim.input.ExpParser.LambdaClosure;
import com.jaamsim.input.ExpParser.LazyBinOpFunc;
import com.jaamsim.input.ExpParser.ParseContext;
import com.jaamsim.input.ExpParser.UnOpFunc;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.Unit;

public class ExpOperators {

	private static void addUnaryOp(String symbol, double bindPower, UnOpFunc func) {
		ExpParser.addUnaryOp(symbol, bindPower, func);
	}

	private static void addBinaryOp(String symbol, double bindPower, boolean rAssoc, BinOpFunc func) {
		ExpParser.addBinaryOp(symbol, bindPower, rAssoc, func);
	}

	private static void addLazyBinaryOp(String symbol, double bindPower, boolean rAssoc, LazyBinOpFunc func) {
		ExpParser.addLazyBinaryOp(symbol, bindPower, rAssoc, func);
	}

	private static void addFunction(String name, int numMinArgs, int numMaxArgs, CallableFunc func) {
		ExpParser.addFunction(name, numMinArgs, numMaxArgs, func);
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
		return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
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


	private static String unitToString(Class<? extends Unit> unit) {
		return unit.getSimpleName();
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

	public static void InitOperatorsAndFuncs() {

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

				switch(lval.type) {
				case NUMBER:
					if (rval.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Operator '+' can only add numbers to numbers");
					}
					if (lval.unitType != rval.unitType) {
						throw new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType));
					}
					return;
				case LAMBDA:
					throw new ExpError(source, pos, "Can not add to a function value");
				case ENTITY:
					throw new ExpError(source, pos, "Can not add to an entity value");
				default:
					return;
				}
			}
			@Override
			public ExpResult apply(ParseContext context, ExpResult lval, ExpResult rval, String source, int pos) throws ExpError {
				switch(lval.type) {
				case NUMBER:
					return ExpResult.makeNumResult(lval.value + rval.value, lval.unitType);
				case STRING:
					return ExpResult.makeStringResult(lval.stringVal.concat(rval.getFormatString()));
				case COLLECTION:
					if (rval.type == ExpResType.COLLECTION) {
						return ExpCollections.appendCollections(lval.colVal, rval.colVal);
					} else {
						return ExpCollections.appendToCollection(lval.colVal, rval);
					}
				default:
					throw new ExpError(source, pos, "Invalid type used in addition");
				}
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				ExpValResult mergedErrors = mergeBinaryErrors(lval, rval);
				if (mergedErrors != null)
					return mergedErrors;

				switch(lval.type) {
				case NUMBER:
					if (rval.type != ExpResType.NUMBER) {
						return ExpValResult.makeErrorRes(new ExpError(source, pos, "Operator '+' can only add numbers to numbers"));
					}
					if (lval.unitType != rval.unitType) {
						return ExpValResult.makeErrorRes(new ExpError(source, pos, getUnitMismatchString(lval.unitType, rval.unitType)));
					}
					return ExpValResult.makeValidRes(ExpResType.NUMBER, lval.unitType);
				case LAMBDA:
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "Can not add to a function value"));
				case ENTITY:
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "Can not add to an entity value"));
				case COLLECTION:
					return ExpValResult.makeValidRes(ExpResType.COLLECTION, DimensionlessUnit.class);
				case STRING:
					return ExpValResult.makeValidRes(ExpResType.STRING, DimensionlessUnit.class);
				default:
					return ExpValResult.makeUndecidableRes();
				}
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

		addLazyBinaryOp("&&", 8, false, new LazyBinOpFunc() {
			@Override
			public ExpResult apply(ParseContext pc, EvalContext ec, ExpParser.ExpNode lval, ExpParser.ExpNode rval, String source, int pos) throws ExpError{
				ExpResult lRes = lval.evaluate(ec);
				if (lRes.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Left operand of '&&' must be a number");
				}
				if (lRes.value == 0)
					return ExpResult.makeNumResult(0, DimensionlessUnit.class);

				ExpResult rRes = rval.evaluate(ec);
				if (rRes.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Right operand of '&&' must be a number");
				}

				return ExpResult.makeNumResult((rRes.value!=0) ? 1 : 0, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult lval, ExpValResult rval, String source, int pos) {
				return validateComparison(context, lval, rval, source, pos);
			}
		});

		addLazyBinaryOp("||", 6, false, new LazyBinOpFunc() {
			@Override
			public ExpResult apply(ParseContext pc, EvalContext ec, ExpParser.ExpNode lval, ExpParser.ExpNode rval, String source, int pos) throws ExpError{
				ExpResult lRes = lval.evaluate(ec);
				if (lRes.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Left operand of '||' must be a number");
				}
				if (lRes.value != 0)
					return ExpResult.makeNumResult(1, DimensionlessUnit.class);

				ExpResult rRes = rval.evaluate(ec);
				if (rRes.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Right operand of '||' must be a number");
				}

				return ExpResult.makeNumResult((rRes.value!=0) ? 1 : 0, DimensionlessUnit.class);
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

		addFunction("sum", 1, 1, new CallableFunc() {
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
					return ExpResult.makeNumResult(0.0d, DimensionlessUnit.class);
				}
				ExpResult comp = col.index(it.nextKey());
				Class<? extends Unit> ut = comp.unitType;
				if (comp.type != ExpResType.NUMBER) {
					throw new ExpError(source, pos, "Can not sum non-numeric type in collection");
				}
				double ret = comp.value;

				while (it.hasNext()) {
					comp = col.index(it.nextKey());
					if (comp.unitType != ut) {
						throw new ExpError(source, pos, "Unmatched Unit types in collection: %s, %s",
						                   ut.getSimpleName(), comp.unitType.getSimpleName());
					}
					if (comp.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Can not sum non-numeric type in collection");
					}
					ret += comp.value;
				}
				return ExpResult.makeNumResult(ret, ut);
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

		addFunction("round", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				// N/A
			}
			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) {
				return ExpResult.makeNumResult(Math.round(args[0].value), args[0].unitType);
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

		addFunction("indexOf", 2, 2, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument as first argument.");
				}

				ExpResult.Collection col = args[0].colVal;
				ExpResult val = args[1];

				ExpResult.Iterator it = col.getIter();
				if (!it.hasNext()) {
					return ExpResult.makeNumResult(0.0d, DimensionlessUnit.class);
				}

				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult colVal = col.index(key);
					if (colVal.equals(val)) {
						return key;
					}
				}
				return ExpResult.makeNumResult(0.0d, DimensionlessUnit.class);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[1].state == ExpValResult.State.ERROR || args[1].state == ExpValResult.State.UNDECIDABLE) {
					return args[1];
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
				int numParams = mapFunc.getNumParams();
				if (numParams != 1 && numParams != 2) {
					throw new ExpError(source, pos, "Function passed to 'map' must take one or two parameters.");
				}

				ExpResult.Collection col = args[1].colVal;
				ExpResult.Iterator it = col.getIter();

				Class<? extends Unit> unitType = null;
				boolean firstVal = true;
				ArrayList<ExpResult> params = new ArrayList<>(numParams);

				ArrayList<ExpResult> results = new ArrayList<>();
				params.add(null);

				if (numParams == 2)
					params.add(null);

				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult val = col.index(key);
					params.set(0, val);

					if (numParams == 2)
						params.set(1, key);

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
				return ExpCollections.wrapCollection(results, unitType);
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

		addFunction("filter", 2, 2, new CallableFunc() {
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

				LambdaClosure filterFunc = args[0].lcVal;
				int numParams = filterFunc.getNumParams();
				if (numParams != 1 && numParams != 2) {
					throw new ExpError(source, pos, "Function passed to 'filter' must take one or two parameters.");
				}

				ExpResult.Collection col = args[1].colVal;
				ExpResult.Iterator it = col.getIter();

				Class<? extends Unit> unitType = null;
				boolean firstVal = true;
				ArrayList<ExpResult> params = new ArrayList<>(1);

				ArrayList<ExpResult> results = new ArrayList<>();
				params.add(null);
				if (numParams == 2)
					params.add(null);

				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult val = col.index(key);
					params.set(0, val);
					if (numParams == 2)
						params.set(1, key);

					ExpResult result = filterFunc.evaluate(context, params);
					if (result.type == ExpResType.NUMBER && result.value != 0) {
						results.add(val);
					}

					Class<? extends Unit> resUnitType = result.type == ExpResType.NUMBER ? result.unitType : null;
					if (firstVal) {
						unitType = resUnitType;
					}
				}
				return ExpCollections.wrapCollection(results, unitType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[0].state == ExpValResult.State.ERROR || args[0].state == ExpValResult.State.UNDECIDABLE) {
					return args[0];
				}
				if (args[0].type != ExpResType.LAMBDA) {
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "First argument to 'filter' must be a function."));
				}

				return validateCollection(context, args[1], source, pos);
			}
		});

		addFunction("reduce", 3, 3, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[2].type != ExpResType.COLLECTION) {
					throw new ExpError(source, pos, "Expected Collection type argument as first argument.");
				}
				if (args[0].type != ExpResType.LAMBDA) {
					throw new ExpError(source, pos, "Expected function argument as third argument.");
				}

				LambdaClosure reduceFunc = args[0].lcVal;
				if (reduceFunc.getNumParams() != 2) {
					throw new ExpError(source, pos, "Function passed to 'reduce' must take two parameters.");
				}

				ExpResult accum = args[1];

				ExpResult.Collection col = args[2].colVal;
				ExpResult.Iterator it = col.getIter();

				ArrayList<ExpResult> params = new ArrayList<>(2);
				params.add(null);
				params.add(null);

				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult val = col.index(key);
					params.set(0, val);
					params.set(1, accum);

					accum = reduceFunc.evaluate(context, params);
				}
				return accum;
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[0].state == ExpValResult.State.ERROR || args[0].state == ExpValResult.State.UNDECIDABLE) {
					return args[0];
				}
				if (args[0].type != ExpResType.LAMBDA) {
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "First argument to 'reduce' must be a function."));
				}

				return validateCollection(context, args[2], source, pos);
			}
		});

		addFunction("sort", 2, 2, new CallableFunc() {
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

				final LambdaClosure sortFunc = args[0].lcVal;
				if (sortFunc.getNumParams() != 2) {
					throw new ExpError(source, pos, "Function passed to 'sort' must take two parameters.");
				}

				ExpResult.Collection col = args[1].colVal;
				ExpResult.Iterator it = col.getIter();

				Class<? extends Unit> unitType = null;

				ArrayList<ExpResult> results = new ArrayList<>();
				while (it.hasNext()) {
					ExpResult key = it.nextKey();
					ExpResult val = col.index(key);
					results.add(val);
				}

				final ArrayList<ExpResult> params = new ArrayList<>(2);
				params.add(null);
				params.add(null);

				final EvalContext cont = context;

				Comparator<ExpResult> c = new Comparator<ExpResult>() {
					@Override
					public int compare(ExpResult arg0, ExpResult arg1) {
						params.set(0, arg0);
						params.set(1, arg1);

						ExpResult res;
						try {
							res = sortFunc.evaluate(cont, params);
						} catch (ExpError e) {
							// Wrap ExpError in a runtime error and extract below
							// (annoying checked exceptions...)
							throw new RuntimeException(e);
						}

						return (res.value == 0) ? 1 : -1;
					}
				};

				try {
					Collections.sort(results, c);
				} catch (RuntimeException e) {
					if (e.getCause() != null && ExpError.class.isAssignableFrom(e.getCause().getClass())) {
						throw (ExpError)e.getCause();
					} else {
						throw e;
					}
				}
				return ExpCollections.wrapCollection(results, unitType);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (args[0].state == ExpValResult.State.ERROR || args[0].state == ExpValResult.State.UNDECIDABLE) {
					return args[0];
				}
				if (args[0].type != ExpResType.LAMBDA) {
					return ExpValResult.makeErrorRes(new ExpError(source, pos, "First argument to 'sort' must be a function."));
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

		addFunction("range", 1, 3, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				for(ExpResult arg : args) {
					if (arg.type != ExpResType.NUMBER) {
						throw new ExpError(source, pos, "Only numbers may be passed to 'range'");
					}
				}
				// Ensure all units are the same
				for (int i = 1; i < args.length; ++i) {
					if (args[0].unitType != args[1].unitType) {
						throw new ExpError(source, pos, "All unit types to 'range' must be the same. %s != %s",
								args[0].unitType.getSimpleName(), args[i].unitType.getSimpleName());
					}
				}
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				double startVal = 1;
				double endVal = 0;
				if (args.length > 1) {
					startVal = args[0].value;
					endVal = args[1].value;
				} else {
					endVal = args[0].value;
				}

				if (startVal > endVal) {
					return ExpCollections.wrapCollection(new ArrayList<ExpResult>(), args[0].unitType);
				}

				double inc = 1;
				if (args.length > 2) {
					inc = args[2].value;
				}
				ArrayList<ExpResult> res = new ArrayList<>();
				double val = startVal;
				while (val <= endVal) {
					res.add(ExpResult.makeNumResult(val, args[0].unitType));
					val += inc;
				}
				return ExpCollections.wrapCollection(res, args[0].unitType);

			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult merge = mergeMultipleErrors(args);
				if (merge != null) {
					return merge;
				}
				for(ExpValResult arg : args) {
					if (arg.type != ExpResType.NUMBER) {
						return ExpValResult.makeErrorRes(new ExpError(source, pos, "Only numbers may be passed to 'range'"));
					}
				}
				// Ensure all units are the same
				for (int i = 1; i < args.length; ++i) {
					if (args[0].unitType != args[1].unitType) {
						return ExpValResult.makeErrorRes(new ExpError(source, pos, "All unit types to 'range' must be the same. %s != %s",
								args[0].unitType.getSimpleName(), args[i].unitType.getSimpleName()));
					}
				}
				return ExpValResult.makeValidRes(ExpResType.COLLECTION, args[0].unitType);
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

		addFunction("format", 1, -1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
			}

			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.STRING) {
					throw new ExpError(source, pos, "First parameter to 'format' must be a string");
				}
				// Build up the argument list
				Object[] strArgs = new Object[args.length-1];
				for (int i = 1; i < args.length; ++i) {
					if (args[i].type != ExpResType.NUMBER) {
						strArgs[i-1] = args[i].getFormatString();
					}
					else {
						if (args[i].unitType != DimensionlessUnit.class) {
							throw new ExpError(source, pos,
									"'format' argument %d must be a dimensionless number. "
									+ "Make it so by dividing it by 1 in the desired unit.\n"
									+ "Example: 'format(\"5km is %%f metres\", 5[km]/1[m])'",
									i + 1);
						}
						strArgs[i-1] = new Double(args[i].value);
					}
				}
				String ret = null;
				try {
					ret = String.format(args[0].stringVal, strArgs);
				} catch(RuntimeException e) {
					throw new ExpError(source, pos, "Error during 'format': %s", e.getMessage());
				}
				return ExpResult.makeStringResult(ret);
			}

			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				ExpValResult mergedErrors = mergeMultipleErrors(args);
				if (mergedErrors != null)
					return mergedErrors;

				if (args[0].type != ExpResType.STRING) {
					ExpError error = new ExpError(source, pos, "First parameter to 'format' must be a string");
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeUndecidableRes();
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

		addFunction("parseNumber", 1, 1, new CallableFunc() {
			@Override
			public void checkUnits(ParseContext context, ExpResult[] args,
					String source, int pos) throws ExpError {
				if (args[0].type != ExpResType.STRING) {
					throw new ExpError(source, pos, "parseNumber requires string as argument");
				}
			}
			@Override
			public ExpResult call(EvalContext context, ExpResult[] args, String source, int pos) throws ExpError {
				double val;
				try {
					val = Double.parseDouble(args[0].stringVal);
				}
				catch (Exception e) {
					val = Double.NaN;
				}
				return ExpResult.makeNumResult(val, DimensionlessUnit.class);
			}
			@Override
			public ExpValResult validate(ParseContext context, ExpValResult[] args, String source, int pos) {
				if (	args[0].state == ExpValResult.State.ERROR ||
						args[0].state == ExpValResult.State.UNDECIDABLE)
					return args[0];
				if (args[0].type != ExpResType.STRING) {
					ExpError error = new ExpError(source, pos, "Argument must be a string");
					return ExpValResult.makeErrorRes(error);
				}
				return ExpValResult.makeValidRes(ExpResType.NUMBER, DimensionlessUnit.class);
			}
		});

	}
}
