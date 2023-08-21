/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import com.jaamsim.BooleanProviders.BooleanProvConstant;
import com.jaamsim.BooleanProviders.BooleanProvExpression;
import com.jaamsim.BooleanProviders.BooleanProvider;
import com.jaamsim.ColourProviders.ColourProvConstant;
import com.jaamsim.ColourProviders.ColourProvExpression;
import com.jaamsim.ColourProviders.ColourProvider;
import com.jaamsim.EntityProviders.EntityProvConstant;
import com.jaamsim.EntityProviders.EntityProvExpression;
import com.jaamsim.EntityProviders.EntityProvider;
import com.jaamsim.Samples.SampleConstant;
import com.jaamsim.Samples.SampleExpression;
import com.jaamsim.Samples.SampleProvider;
import com.jaamsim.Samples.TimeSeriesConstantDouble;
import com.jaamsim.StringProviders.StringProvConstant;
import com.jaamsim.StringProviders.StringProvExpression;
import com.jaamsim.StringProviders.StringProvSample;
import com.jaamsim.StringProviders.StringProvider;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.Group;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.datatypes.BooleanVector;
import com.jaamsim.datatypes.DoubleVector;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.ui.NaturalOrderComparator;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.TimeUnit;
import com.jaamsim.units.Unit;
import com.jaamsim.units.UserSpecifiedUnit;

public abstract class Input<T> {
	protected static final String INP_ERR_COUNT = "Expected an input with %s value(s), received: %s";
	protected static final String INP_ERR_RANGECOUNT = "Expected an input with %d to %d values, received: %s";
	protected static final String INP_ERR_RANGECOUNTMIN = "Expected an input with at least %d values, received: %s";
	protected static final String INP_ERR_EVENCOUNT = "Expected an input with even number of values, received: %s";
	protected static final String INP_ERR_ODDCOUNT = "Expected an input with odd number of values, received: %s";
	protected static final String INP_ERR_BOOLEAN = "Expected a boolean value, received: %s";
	protected static final String INP_ERR_INTEGER = "Expected an integer value, received: %s";
	protected static final String INP_ERR_INTEGERRANGE = "Expected an integer between %d and %d, received: %d";
	protected static final String INP_ERR_DOUBLE = "Expected an numeric value, received: %s";
	protected static final String INP_ERR_DOUBLERANGE = "Expected a number between %f and %f, received: %f";
	protected static final String INP_ERR_SAMPLERANGE = "Expected a number between %f and %f, received: %s which returns values between %f and %f";
	protected static final String INP_ERR_TIME = "Expected a time value (hh:mm or hh:mm:ss), received: %s";
	protected static final String INP_ERR_TIMEVALUE = "Expected a numeric value, 12 numeric values, or a probabilty distribution, received: %s";
	protected static final String INP_ERR_BADSUM = "List must sum to %f, received:%f";
	protected static final String INP_ERR_SUMRANGE = "Sum of list must be between %s and %s, sum: %s";
	protected static final String INP_ERR_MONOTONIC = "List must %s monotonically. Values starting at index %s are %s s, %s s, ...";
	protected static final String INP_ERR_BADCHOICE = "Expected one of %s, received: %s";
	protected static final String INP_ERR_ELEMENT = "Error parsing element %d: %s";
	protected static final String INP_ERR_ENTNAME = "Could not find an Entity named: %s";
	protected static final String INP_ERR_NOUNITFOUND = "A unit is required, could not parse '%s' as a %s";
	protected static final String INP_ERR_UNITNOTFOUND = "A unit of type '%s' is required";
	protected static final String INP_ERR_NOTUNIQUE = "List must contain unique entries, repeated entry: %s";
	protected static final String INP_ERR_NOTVALIDENTRY = "List must not contain: %s";
	protected static final String INP_ERR_ENTCLASS = "Expected a %s, %s is a %s";
	protected static final String INP_ERR_INTERFACE = "Expected an object implementing %s, %s does not";
	protected static final String INP_ERR_UNITS = "Unit types do not match";
	public    static final String INP_ERR_UNITUNSPECIFIED = "Unit type has not been specified";
	protected static final String INP_ERR_NOTSUBCLASS = "Expected a subclass of %s, got %s";
	protected static final String INP_ERR_BADDATE = "Expected a valid RFC8601 datetime, got: %s";
	public    static final String INP_ERR_BADCOLOUR = "Expected a valid colour name or RGB value with or without transparency, got: %s";
	protected static final String INP_ERR_BADEXP = "Error parsing expression: %s";
	protected static final String INP_VAL_LISTSET = "Values found for %s without %s being set";
	protected static final String INP_VAL_LISTSIZE = "%s and %s must be of equal size";
	protected static final String INP_ERR_BRACES = "List must contain equal numbers of opening and closing braces";
	public static final String INP_ERR_QUOTE = "A String cannot include a single quote or apostrophe (').";

	public static final String EXP_ERR_RESULT_TYPE = "Incorrect result type returned by expression.%n"
	                                               + "Received: %s, expected: %s";
	public static final String EXP_ERR_CLASS = "Incorrect class returned by expression.%n"
	                                         + "Received: %s, expected: %s";
	public static final String EXP_ERR_UNIT = "Incorrect unit type returned by expression.%n"
	                                        + "Received: %s, expected: %s";

	protected static final String VALID_SAMPLE_PROV = "Accepts a number with units of type %s, an object that returns such a number, or an expression that returns such a number.";
	protected static final String VALID_SAMPLE_PROV_DIMLESS = "Accepts a dimensionless number, an object that returns such a number, or an expression that returns such a number.";
	protected static final String VALID_SAMPLE_PROV_UNIT = "Accepts a number with or without units, an object that returns such a number, or an expression that returns such a number. "
	                                                     + "An input to the UnitType keyword MUST BE PROVIDED before an input to this keyword can be entered.";
	protected static final String VALID_SAMPLE_PROV_INTEGER = "Accepts a dimensionless integer, an object that returns such a number, or an expression that returns such a number.";
	protected static final String VALID_STRING_PROV = "Accepts a string or an expression that returns a string. "
	                                                + "Also accepts other types of expressions whose outputs will be converted to a string.";
	protected static final String VALID_ENTITY_PROV = "Accepts an entity name or an expression that returns an entity.";
	protected static final String VALID_ENTITY_PROV_TYPE = "Accepts the name of an entity of type %s or an expression that returns such an entity.";
	protected static final String VALID_BOOLEAN_PROV = "Accepts the text TRUE or FALSE or an expression that returns a dimensionless number (non-zero indicates TRUE, zero indicates FALSE). "
	                                                 + "Inputs of T, t, and 1 are interpreted as TRUE, while F, f, and 0 are interpreted as FALSE.";
	protected static final String VALID_COLOUR_PROV = "Accepts a colour name or an RGB value, with or without an optional transparency value, "
	                                                + "or an expression that returns a colour name or an array that contains an RGB value with or without a transparency value. "
	                                                + "The RGB and transparency value can be in either integer format (0 - 255) or decimal format (0.0 - 1.0)";
	protected static final String VALID_TIMESERIES_PROV = "Accepts a number with units of type %s or a TimeSeries that returns such a number.";
	protected static final String VALID_TIMESERIES_PROV_UNIT = "Accepts a number with or without units or a TimeSeries that returns such a number. "
	                                                         + "An input to the UnitType keyword MUST BE PROVIDED before an input to this keyword can be entered.";

	protected static final String VALID_COLOUR = "Accepts a colour name or an RGB value, with or without an optional transparency value. "
	                                           + "The RGB and transparency value can be in either integer format (0 - 255) or decimal format (0.0 - 1.0)";
	protected static final String VALID_INTEGER = "Accepts a dimensionless integer value.";
	protected static final String VALID_VALUE = "Accepts a number with units of type %s.";
	protected static final String VALID_VALUE_DIMLESS = "Accepts a dimensionless number.";
	protected static final String VALID_VALUE_UNIT = "Accepts a number with or without units. "
	                                               + "An input to the UnitType keyword MUST BE PROVIDED before an input to this keyword can be entered.";
	protected static final String VALID_ENTITY = "Accepts the name of an entity.";
	protected static final String VALID_ENTITY_TYPE = "Accepts the name of an entity of type %s.";
	protected static final String VALID_INTERFACE_ENTITY = "Accepts the name of an entity that supports the %s interface.";
	protected static final String VALID_BOOLEAN = "Accepts the text TRUE or FALSE. Inputs of T, t, and 1 are interpreted as TRUE, while F, f, and 0 are interpreted as FALSE.";
	protected static final String VALID_STRING = "Accepts a text string. The string must be enclosed by single quotes if it includes a space.";
	protected static final String VALID_DATE = "Accepts a calendar date and time in one of the following formats: 'YYYY-MM-DD hh:mm:ss.sss', 'YYYY-MM-DD hh:mm:ss', 'YYYY-MM-DD'";
	protected static final String VALID_FILE = "Accepts a file path enclosed by single quotes.";
	protected static final String VALID_DIR = "Accepts a directory path enclosed by single quotes.";
	protected static final String VALID_EXP_DIMLESS = "Accepts an expression that returns a dimensionless number. A value of zero implies FALSE; non-zero implies TRUE.";
	protected static final String VALID_EXP_NUM = "Accepts an expression that returns a number.";
	protected static final String VALID_EXP_STR = "Accepts an expression that returns a string.";
	protected static final String VALID_EXP_ENT = "Accepts an expression that returns an entity.";
	protected static final String VALID_EXP = "Accepts an expression.";
	protected static final String VALID_KEYEVENT = "Accepts a single character representing a key on the keyboard. "
	                                             + "For non-printing keys, enter the key's name such as HOME, ESCAPE, SPACE, F1, etc.";
	protected static final String VALID_VALUE_LIST = "Accepts a list of numbers separated by spaces, followed by a unit for these values, if required.";
	protected static final String VALID_VALUE_LIST_DIMLESS = "Accepts a list of dimensionless numbers separated by spaces.";
	protected static final String VALID_SAMPLE_LIST = "Accepts a list containing numbers with or without units, objects that return such a number, or expressions that return such a number. "
	                                                + "Each entry in the list must be enclosed by braces.";
	protected static final String VALID_SAMPLE_LIST_DIMLESS = "Accepts a list containing dimensionless numbers, objects that return such a number, or expressions that return such a number. "
	                                                        + "Each entry in the list can be enclosed by braces.";
	protected static final String VALID_SAMPLE_LIST_INTEGER = "Accepts a list containing dimensionless integers, objects that return such a number, or expressions that return such a number. "
            + "Each entry in the list can be enclosed by braces.";
	protected static final String VALID_UNIT_TYPE_LIST = "Accepts a list of unit types separated by spaces.";
	protected static final String VALID_STRING_PROV_LIST = "Accepts a list of strings or expressions that return strings. "
	                                                     + "Also accepts other types of expressions whose outputs will be converted to strings. "
	                                                     + "Each entry in the list must be enclosed by braces.";
	protected static final String VALID_COLOR_LIST = "Accepts a list of colours that are expressed as either a colour name or an RGB value. "
	                                               + "Each entry in the list must be enclosed by braces.";
	protected static final String VALID_ENTITY_LIST = "Accepts a list of entity names separated by spaces.";
	protected static final String VALID_FORMAT = "Accepts a Java format string for a number. For example, '%.3f' would print a number with three decimal places.";
	protected static final String VALID_VEC3D = "Accepts three numbers separated by spaces followed by a unit of type %s. "
	                                          + "If only two numbers are entered, the third value defaults to zero.";
	protected static final String VALID_VEC3D_DIMLESS = "Accepts three dimensionless numbers separated by spaces.";
	protected static final String VALID_VEC3D_LIST = "Accepts a list of vectors enclosed by braces. "
	                                               + "Each vector consists of three numbers separated by spaces followed by a unit of type %s. "
	                                               + "If a vector has only two numbers, the third value defaults to zero.";
	protected static final String VALID_ATTRIB_DEF = "Accepts a list of attribute definitions each consisting of an attribute name followed by an expression "
	                                               + "that sets the initial value for the attribute. "
	                                               + "Each definition in the list must be enclosed by braces.";
	protected static final String VALID_CUSTOM_OUT = "Accepts a list of custom output definitions each consisting of a custom output name, an expression, and a unit type (if required). "
	                                               + "Each definition in the list must be enclosed by braces.";
	protected static final String VALID_PASSTHROUGH = "Accepts a list of passthrough keyword definitions each consisting of the name for the new keyword and output followed by the unit type for the new output. "
	                                                + "The unit type defaults to DimensionlessUnit if no unit type is entered. "
	                                                + "Each definition in the list must be enclosed by braces.";
	protected static final String VALID_ACTION = "Accepts a list of action name and output name pairs. "
	                                           + "Each action/output pair consists of an action name followed by an output name. "
	                                           + "The names are separated by one or more spaces and enclosed braces.";
	protected static final String VALID_SCENARIO_NUMBER = "Accepts a dimensionless number, an expression that returns such a number, or a set of scenario indices separated by hyphens. "
                                                        + "For example, if three scenario indices have been defined with ranges of 3, 5, and 10, "
                                                        + "then scenario number 22 can be expressed as 1-3-2 because 22 = (1-1)*5*10 + (3-1)*10 + 2.";

	public static final String POSITIVE_INFINITY = "Infinity";
	public static final String NEGATIVE_INFINITY = "-Infinity";
	public static final String SEPARATOR = "  ";
	public static final String BRACE_SEPARATOR = " ";

	private String keyword; // the preferred name for the input keyword
	private final String category;
	private InputCallback callback;

	protected T defValue;
	protected T value;
	protected Input<T> protoInput;

	private boolean edited; // indicates if input has been edited for this entity
	private boolean promptReqd; // indicates whether to prompt the user to save the configuration file
	private boolean hidden; // Hide this input from the EditBox
	protected boolean isDef; // Is this input still the default value?
	protected String[] valueTokens; // value from .cfg file
	private String defText; // special text to show in the default column of the Input Editor
	private boolean isReqd;     // indicates whether this input must be provided by the user
	private boolean isValid;  // if false, the input is no longer valid and must be re-entered
	private boolean isLocked; // indicates whether the input can be changed through by the user

	public static final Comparator<Object> uiSortOrder = new NaturalOrderComparator();

	public Input(String key, String cat, T def) {
		keyword = key;
		category = cat;
		defValue = def;

		promptReqd = true;
		hidden = false;
		defText = null;
		isReqd = false;

		reset();
	}

	public void doCallback(Entity ent) {
		if (callback != null)
			callback.callback(ent, this);
	}

	public void setCallback(InputCallback back) {
		callback = back;
	}

	/**
	 * Sets the input to its default value.
	 */
	public void reset() {
		value = defValue;
		valueTokens = null;
		edited = false;
		isDef = true;
		isValid = true;
	}

	/**
	 * Assigns the internal state for this input to the same values as the
	 * specified input.
	 * <p>
	 * This method provides the same function as copyFrom by re-parsing the input data instead of
	 * copying the internal variables. This operation is much slower, but is needed for inputs that
	 * cannot be copied successfully using copyFrom, such as inputs that accept an expression.
	 * @param thisEnt TODO
	 * @param in - input object to be copied.
	 */
	public void parseFrom(Entity thisEnt, Input<?> in) {
		ArrayList<String> toks = new ArrayList<>(Arrays.asList(in.valueTokens));
		KeywordIndex kw = new KeywordIndex(in.getKeyword(), toks, null);
		parse(thisEnt, kw);
	}

	/**
	 * Deletes any use of the specified entity from this input.
	 * @param ent - entity whose references are to be deleted
	 * @return true if a reference was removed
	 */
	public boolean removeReferences(Entity ent) {
		return false;
	}

	/**
	 * Appends the entities referenced by the value for this input.
	 * @param list - list of entity references
	 */
	public void appendEntityReferences(ArrayList<Entity> list) {}

	/**
	 * Describes the valid inputs for this type of input.
	 * @return description of valid inputs
	 */
	public String getValidInputDesc() {
		return null;
	}

	/**
	 * Provides one or more example input strings for this type of input.
	 * @return examples of valid inputs
	 */
	public String[] getExamples() {
		return new String[0];
	}

	/**
	 * Corrects common input errors that can be detected prior to parsing.
	 * @param str - uncorrected input string
	 * @return corrected input string
	 */
	public String applyConditioning(String str) {
		return str;
	}

	@Override
	public String toString() {
		return String.format("%s", value);
	}

	public final String getKeyword() {
		return keyword;
	}

	public void setKeyword(String str) {
		keyword = str;
	}

	public final String getCategory() {
		return category;
	}

	public boolean isSynonym() {
		return false;
	}

	public void setDefaultText(String str) {
		defText = str;
	}

	public String getDefaultText() {
		return defText;
	}

	/**
	 * Sets the default value and returns the input to its new default state.
	 * @param val - new default value
	 */
	public void setDefaultValue(T val) {
		defValue = val;
		reset();
	}

	public T getDefaultValue() {
		return defValue;
	}

	/**
	 * Returns a string representing the default value for the input using the preferred units
	 * specified for the simulation model.
	 * @param simModel - simulation model
	 * @return string representing the default value
	 */
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null)
			return "";
		return defValue.toString();
	}

	public T getValue() {
		if (isDef && protoInput != null)
			return protoInput.getValue();
		return value;
	}

	@SuppressWarnings("unchecked")
	public void setProtoInput(Input<?> in) {
		protoInput = (Input<T>) in;
	}

	public Input<T> getProtoInput() {
		return protoInput;
	}

	public void setHidden(boolean hide) {
		hidden = hide;
	}

	public boolean getHidden() {
		return hidden;
	}

	public void setEdited(boolean bool) {
		edited = bool;
	}

	public boolean isEdited() {
		return edited;
	}

	public void setPromptReqd(boolean bool) {
		promptReqd = bool;
	}

	public boolean isPromptReqd() {
		return promptReqd;
	}

	public void setRequired(boolean bool) {
		isReqd = bool;
	}

	public boolean isRequired() {
		return isReqd;
	}

	public void setValid(boolean bool) {
		isValid = bool;
	}

	public boolean isValid() {
		return isValid;
	}

	public void setLocked(boolean bool) {
		isLocked = bool;
	}

	public boolean isLocked() {
		return isLocked;
	}

	public boolean useExpressionBuilder() {
		return false;
	}

	/**
	 * Returns a string representing the value for this input at the present simulation time and
	 * using the preferred units specified for the simulation model. Any expressions included in
	 * the input are evaluated.
	 * @param thisEnt - entity whose input is being evaluated
	 * @param simTime - present simulation time
	 * @return string representing the input value
	 */
	public String getPresentValueString(Entity thisEnt, double simTime) {
		return getValueString();
	}

	public void validate() throws InputErrorException {
		if (isReqd && isDefault() && !hidden)
			throw new InputErrorException("An input must be provided for the keyword '%s'.", keyword);
	}

	public void setTokens(KeywordIndex kw) {
		isDef = false;
		valueTokens = kw.getArgArray();
	}

	/**
	 * Add the given tokens to the present value tokens
	 */
	public void addTokens(String[] args) {

		// Create an array sized for the addition of new tokens
		String[] newValueTokens;
		if (valueTokens == null) {
			newValueTokens = new String[args.length - 1];

			// Copy the new tokens into the array
			System.arraycopy(args, 1, newValueTokens, 0, args.length - 1);
		}
		else {
			newValueTokens = new String[valueTokens.length + args.length - 1];

			// Copy the old tokens into the array
			System.arraycopy(valueTokens, 0, newValueTokens, 0, valueTokens.length);

			// Copy the new tokens into the array
			System.arraycopy(args, 1, newValueTokens, valueTokens.length, args.length - 1);
		}

		valueTokens = newValueTokens;
	}

	/**
	 * Remove the given tokens from the present value tokens
	 * @return - true if all the tokens were successfully removed
	 */
	public boolean removeTokens(String[] args) {

		int newSize = valueTokens.length - (args.length - 1);
		if( newSize >= 0 ) {

			// Create an array sized for the removal of tokens
			String[] newValueTokens = new String[newSize];
			int index = 0;

			// Loop through the original tokens
			for (int i = 0; i < valueTokens.length; i++ ) {

				// Determine if this token is to be kept
				boolean keep = true;
				for (int j = 1; j < args.length; j++) {
					if (args[j].equals(valueTokens[i])) {
						keep = false;
						break;
					}
				}

				// If the token is to be kept, add it to the array
				if (keep) {
					newValueTokens[index] = valueTokens[i];
					index++;
				}
			}

			// If the correct number of items were kept, reset valueTokens
			if (index == newSize) {
				valueTokens = newValueTokens;
				return true;
			}
		}
		return false;
	}

	/**
	 * Append the given tokens to the present value tokens
	 */
	public void appendTokens(String[] args) {

		// Determine if braces need to be added around original tokens
		boolean addBracesAroundOriginalTokens = false;

		// Determine the size for an array with original and new tokens
		int newSize;
		int valueTokensLength = 0;
		if (valueTokens == null)
			newSize = args.length;
		else {
			valueTokensLength = valueTokens.length;
			newSize = valueTokens.length + args.length;

			if (! valueTokens[0].equals( "{" )) {
				addBracesAroundOriginalTokens = true;
				newSize += 2;
			}
		}

		// Determine if braces need to be added around new tokens
		boolean addBracesAroundNewTokens = false;
		if (! args[0].equals( "{" )) {
			addBracesAroundNewTokens = true;
			newSize += 2;
		}

		// Create an array sized for the addition of new tokens
		String[] newValueTokens = new String[newSize];

		// Copy the old and new tokens into the array
		if (addBracesAroundOriginalTokens) {
			newValueTokens[0] = "{";
			System.arraycopy(valueTokens, 0, newValueTokens, 1, valueTokens.length);
			newValueTokens[valueTokens.length + 1] = "}";

			if (addBracesAroundNewTokens) {
				newValueTokens[valueTokens.length + 2] = "{";
				System.arraycopy(args, 0, newValueTokens, valueTokens.length+3, args.length);
				newValueTokens[newSize-1] = "}";
			}
			else {
				System.arraycopy(args, 0, newValueTokens, valueTokens.length+2, args.length);
			}
		}
		else {
			if (valueTokens != null)
				System.arraycopy(valueTokens, 0, newValueTokens, 0, valueTokens.length);

			if (addBracesAroundNewTokens) {
				newValueTokens[valueTokensLength] = "{";
				System.arraycopy(args, 0, newValueTokens, valueTokensLength+1, args.length);
				newValueTokens[newSize-1] = "}";
			}
			else {
				System.arraycopy(args, 0, newValueTokens, valueTokensLength, args.length);
			}
		}

		valueTokens = newValueTokens;
	}

	/**
	 * Returns whether the input has not been set and is not inherited from its protoInput
	 * @return true if the input has not been set and is not inherited
	 */
	public boolean isDefault() {
		return isDef && (protoInput == null || protoInput.isDefault());
	}

	/**
	 * Returns whether the input has not been set.
	 * @return true if the input has not been set
	 */
	public boolean isDef() {
		return isDef;
	}

	public int getSequenceNumber() {
		if (InputAgent.isEarlyInput(this))
			return 0;
		return 1;
	}

	/**
	 * Returns an array of white-space delimited strings that can be used to generate the input
	 * file entry for this input value.
	 * @return array of strings
	 */
	public ArrayList<String> getValueTokens() {
		ArrayList<String> ret = new ArrayList<>();
		getValueTokens(ret);
		return ret;
	}

	/**
	 * Populates an array of white-space delimited strings that can be used to generate the input
	 * file string for this input value.
	 * @param toks - array of strings to be populated
	 */
	public void getValueTokens(ArrayList<String> toks) {
		if (valueTokens == null)
			return;

		for (String each : valueTokens)
			toks.add(each);
	}

	/**
	 * Adds the value tokens within a specified pair of opening and closing braces.
	 * @param n - specifies the pair of opening and closing braces to choose
	 * @param toks - array of strings to be populated
	 */
	public void getSubValueTokens(int n, ArrayList<String> toks) {
		if (valueTokens == null)
			return;

		int index = -1;
		int level = 0;
		for (int i = 0; i < valueTokens.length; i++) {
			if (valueTokens[i].equals("{")) {
				level++;
				if (level == 1)
					index++;
				continue;
			}
			if (valueTokens[i].equals("}")) {
				level--;
				if (level == 0 && index == n)
					return;
				continue;
			}
			if (index == n) {
				toks.add(valueTokens[i]);
			}
		}
	}

	/**
	 * Returns the input file entry for this input or the entry inherited from its prototype.
	 * @return input file text
	 */
	public final String getValueString() {
		if (isDef && protoInput != null)
			return protoInput.getValueString();
		return getInputString();
	}

	/**
	 * Returns the input file entry for this input value.
	 * @return input file text
	 */
	public final String getInputString() {
		if (isDef) return "";
		ArrayList<String> tmp = new ArrayList<>();
		try {
			getValueTokens(tmp);
		} catch (Exception e) {
			InputAgent.logMessage("Error in input, value has been cleared. Keyword: %s",
					this.getKeyword());
			InputAgent.logStackTrace(e);
			this.reset();
		}
		return getValueString(tmp, false);
	}

	/**
	 * Returns the input value that has been inherited from the input's prototype.
	 * @return input file text
	 */
	public final String getInheritedValueString() {
		if (protoInput == null)
			return "";
		return protoInput.getValueString();
	}

	/**
	 * Returns the input file entry for the specified array of white-space delimited strings.
	 * @param tokens - array of strings for the input
	 * @param addLF - true if a newline character is to be added before each inner brace
	 * @return input file text
	 */
	public static final String getValueString(ArrayList<String> tokens, boolean addLF) {
		if (tokens.size() == 0) return "";

		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < tokens.size(); i++) {
			String dat = tokens.get(i);
			if (dat == null) continue;
			if (i > 0) {
				if (dat.equals("}") || tokens.get(i-1).equals("{")) {
					sb.append(Input.BRACE_SEPARATOR);
				}
				else if (dat.equals("{")) {
					if (addLF) {
						sb.append("\n");
					}
					else {
						sb.append(Input.BRACE_SEPARATOR);
					}
				}
				else {
					sb.append(Input.SEPARATOR);
				}
			}

			if (Parser.needsQuoting(dat) && !dat.equals("{") && !dat.equals("}"))
				sb.append("'").append(dat).append("'");
			else
				sb.append(dat);
		}
		return sb.toString();
	}

	/**
	 * Returns the value tokens for this input or that were inherited from its prototype input.
	 * @return value tokens
	 */
	public final String[] getValueArray() {
		if (isDef && protoInput != null)
			return protoInput.getValueArray();
		if (isDef)
			return new String[0];
		return valueTokens;
	}

	/**
	 * Returns the value tokens that has been inherited from the input's prototype.
	 * @return value tokens for the prototype
	 */
	public final String[] getInheritedValueArray() {
		if (protoInput == null)
			return new String[0];
		return protoInput.getValueArray();
	}

	public abstract void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException;


	public static void assertCount(DoubleVector input, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts == null || counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == input.size())
				return;
		}

		// Input size is not equal to any of the specified counts
		throw new InputErrorException(INP_ERR_COUNT, Arrays.toString(counts), input.toString());
	}

	/**
	 * Verifies that the correct number of inputs have been provided.
	 * @param kw - object containing the inputs.
	 * @param counts - list of valid numbers of inputs. All other numbers are invalid.
	 * @throws InputErrorException
	 */
	public static void assertCount(KeywordIndex kw, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == kw.numArgs())
				return;
		}

		// Input size is not equal to any of the specified counts
		if (counts.length == 1)
			throw new InputErrorException(INP_ERR_COUNT, counts[0], kw.argString());
		else {
			StringBuilder sb = new StringBuilder();
			sb.append(counts[0]);
			for (int i=1; i<counts.length-1; i++) {
				sb.append(", ").append(counts[i]);
			}
			sb.append(" or ").append(counts[counts.length-1]);
			throw new InputErrorException(INP_ERR_COUNT, sb.toString(), kw.argString());
		}
	}

	/**
	 * Verifies that the correct number of inputs have been provided.
	 * @param kw - object containing the inputs.
	 * @param min - minimum number of inputs that are valid
	 * @param max - maximum number of inputs that are valid
	 * @throws InputErrorException
	 */
	public static void assertCountRange(KeywordIndex kw, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(kw, min);
			return;
		}

		if (kw.numArgs() < min || kw.numArgs() > max) {
			if (max == Integer.MAX_VALUE)
				throw new InputErrorException(INP_ERR_RANGECOUNTMIN, min, kw.argString());
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, kw.argString());
		}
	}

	public static void assertCount(List<String> input, int... counts)
	throws InputErrorException {
		// If there is no constraint on the element count, return
		if (counts.length == 0)
			return;

		// If there is an exact constraint, check the count
		for (int each : counts) {
			if (each == input.size())
				return;
		}

		// Input size is not equal to any of the specified counts
		throw new InputErrorException(INP_ERR_COUNT, Arrays.toString(counts), input.toString());
	}

	public static void assertCountRange(DoubleVector input, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(input, min);
			return;
		}

		if (input.size() < min || input.size() > max) {
			if (max == Integer.MAX_VALUE)
				throw new InputErrorException(INP_ERR_RANGECOUNTMIN, min, input.toString());
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, input.toString());
		}
	}

	public static void assertCountRange(List<String> input, int min, int max)
	throws InputErrorException {
		// For a range with a single value, fall back to the exact test
		if (min == max) {
			Input.assertCount(input, min);
			return;
		}

		if (input.size() < min || input.size() > max) {
			if (max == Integer.MAX_VALUE)
				throw new InputErrorException(INP_ERR_RANGECOUNTMIN, min, input.toString());
			throw new InputErrorException(INP_ERR_RANGECOUNT, min, max, input.toString());
		}
	}

	public static void assertCountEven(KeywordIndex kw)
	throws InputErrorException {
		if ((kw.numArgs() % 2) != 0)
			throw new InputErrorException(INP_ERR_EVENCOUNT, kw.argString());
	}

	public static void assertCountOdd(KeywordIndex kw)
	throws InputErrorException {
		if ((kw.numArgs() % 2) == 0)
			throw new InputErrorException(INP_ERR_ODDCOUNT, kw.argString());
	}

	public static <T extends Entity> void assertNotPresent(ArrayList<? super T> list, T ent)
	throws InputErrorException {
		if (list.contains(ent))
			throw new InputErrorException(INP_ERR_NOTVALIDENTRY, ent.getName());
	}

	public static void assertSumTolerance(DoubleVector vec, double sum, double tol)
	throws InputErrorException {
		// Vector sum is within tolerance of given sum, no error
		if (Math.abs(vec.sum() - sum) < tol)
			return;

		throw new InputErrorException(INP_ERR_BADSUM, sum, vec.sum());
	}

	public static void assertSumRange(DoubleVector vec, double min, double max)
	throws InputErrorException {
		double sum = vec.sum();
		if ((MathUtils.nearGT(sum, min) || min == Double.NEGATIVE_INFINITY) && (MathUtils.nearLT(sum, max) || max == Double.POSITIVE_INFINITY))
			return;

		throw new InputErrorException(INP_ERR_SUMRANGE, min, max, sum);
	}

	public static void assertMonotonic(DoubleVector vec, int direction)
	throws InputErrorException {
		if (direction == 0)
			return;

		for (int i=1; i<vec.size(); i++) {
			double diff = vec.get(i) - vec.get(i-1);

			if (direction > 0 && diff < 0.0)
				throw new InputErrorException(INP_ERR_MONOTONIC, "increase", i-1, vec.get(i-1), vec.get(i));

			if (direction < 0 && diff > 0.0)
				throw new InputErrorException(INP_ERR_MONOTONIC, "decrease", i-1, vec.get(i-1), vec.get(i));
		}
	}

	public static void assertBracesMatch(KeywordIndex kw)
	throws InputErrorException {
		int depth = 0;
		for (int i= 0; i < kw.numArgs(); i++) {
			if (kw.getArg(i).equals("{")) {
				depth++;
				continue;
			}
			if (kw.getArg(i).equals("}")) {
				depth--;
				continue;
			}
		}
		if (depth != 0)
			throw new InputErrorException(INP_ERR_BRACES);
	}

	/**
	 * Converts a file path entry in a configuration file to a URI.
	 * @param kw - keyword input containing the file path data
	 * @return the URI corresponding to the file path data.
	 * @throws InputErrorException
	 */
	public static URI parseURI(KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);

		String arg = kw.getArg(0);

		// Convert the file path to a URI
		URI uri = null;
		try {
			if (kw.context != null)
				uri = InputAgent.getFileURI(kw.context.context, arg, kw.context.jail);
			else
				uri = InputAgent.getFileURI(null, arg, null);
		}
		catch (URISyntaxException ex) {
			throw new InputErrorException("File Entity parse error: %s", ex.getMessage());
		}

		if (uri == null)
			throw new InputErrorException("Unable to parse the file path:\n%s", arg);

		if (!uri.isOpaque() && uri.getPath() == null)
			 throw new InputErrorException("Unable to parse the file path:\n%s", arg);

		return uri;
	}

	public static boolean parseBoolean(String data)
	throws InputErrorException {
		if ("TRUE".equals(data)) {
			return true;
		}

		if ("FALSE".equals(data)) {
			return false;
		}

		throw new InputErrorException(INP_ERR_BOOLEAN, data);
	}

	public static BooleanVector parseBooleanVector(KeywordIndex kw)
	throws InputErrorException {
		BooleanVector temp = new BooleanVector(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			try {
				boolean element = Input.parseBoolean(kw.getArg(i));
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		return temp;
	}

	public static ArrayList<Color4d> parseColorVector(JaamSimModel simModel, KeywordIndex kw)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<Color4d> temp = new ArrayList<>(subArgs.size());

		for (int i = 0; i < subArgs.size(); i++) {
			try {
				Color4d element = Input.parseColour(simModel, subArgs.get(i));
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		return temp;
	}

	public static Class<? extends Entity> parseClass( String data ) {
		Class<? extends Entity> entProto = null;
		try {
			Class<?> proto = Class.forName( data );
			entProto = proto.asSubclass(Entity.class);
		}
		catch (ClassNotFoundException e) {
			throw new InputErrorException( "Class not found " + data );
		}
		return entProto;
	}

	public static int parseInteger(String data)
	throws InputErrorException {
		return Input.parseInteger(data, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	public static int parseInteger(String data, int minValue, int maxValue)
	throws InputErrorException {
		int temp;
		try {
			temp = Integer.parseInt(data);
		}
		catch (NumberFormatException e) {
			throw new InputErrorException(INP_ERR_INTEGER, data);
		}

		if (temp < minValue || temp > maxValue)
			throw new InputErrorException(INP_ERR_INTEGERRANGE, minValue, maxValue, temp);

		return temp;
	}

	public static boolean isInteger(String val) {
		try {
			Integer.parseInt(val);
			return true;
		}
		catch (NumberFormatException e) { return false; }
	}

	public static boolean isDouble(String val) {
		try {
			Double.parseDouble(val);
			return true;
		}
		catch (NumberFormatException e) { return false; }
	}

	public static IntegerVector parseIntegerVector(KeywordIndex kw, int minValue, int maxValue)
	throws InputErrorException {
		IntegerVector temp = new IntegerVector(kw.numArgs());

		for (int i = 0; i <kw.numArgs(); i++) {
			try {
				int element = Input.parseInteger(kw.getArg(i), minValue, maxValue);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		return temp;
	}

	public static double parseTime(String data, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseTime(data, minValue, maxValue, 1.0);
	}

	/**
	 * Convert the given String to a double and apply the given conversion factor
	 */
	public static double parseTime(String data, double minValue, double maxValue, double factor )
	throws InputErrorException {
		double value = 0.0d;

		// check for hh:mm:ss or hh:mm
		if (data.indexOf(':') > -1) {
			String[] splitDouble = data.split( ":" );
			if (splitDouble.length != 2 && splitDouble.length != 3)
				throw new InputErrorException(INP_ERR_TIME, data);

			try {
				double hour = Double.parseDouble(splitDouble[0]);
				double min = Double.parseDouble(splitDouble[1]);
				double sec = 0.0d;

				if (splitDouble.length == 3)
					sec = Double.parseDouble(splitDouble[2]);

				value = hour + (min / 60.0d) + (sec / 3600.0d);
			}
			catch (NumberFormatException e) {
				throw new InputErrorException(INP_ERR_TIME, data);
			}
		} else {
			value = Input.parseDouble(data);
		}
		value = value * factor;

		if (value < minValue || value > maxValue)
			throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, value);

		return value;
	}

	private static final Pattern is8601date = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
	private static final Pattern is8601short = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}");
	private static final Pattern is8601time = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}");
	private static final Pattern is8601full = Pattern.compile("\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}\\.\\d{1,6}");
	private static final Pattern isextendshort = Pattern.compile("\\d{1,}:\\d{2}");
	private static final Pattern isextendtime = Pattern.compile("\\d{1,}:\\d{2}:\\d{2}");
	private static final Pattern isextendfull = Pattern.compile("\\d{1,}:\\d{2}:\\d{2}.\\d{1,6}");
	private static final long usPerSec = 1000000;
	private static final long usPerMin = 60 * usPerSec;
	private static final long usPerHr  = 60 * usPerMin;
	private static final long usPerDay = 24 * usPerHr;
	public static final long usPerYr  = 365 * usPerDay;

	public static boolean isRFC8601DateTime(String input) {
		if (isRFC8601Date(input)) return true;
		if (isextendshort.matcher(input).matches()) return true;
		if (isextendtime.matcher(input).matches()) return true;
		if (isextendfull.matcher(input).matches()) return true;
		return false;
	}

	public static boolean isRFC8601Date(String input) {
		if (is8601short.matcher(input).matches()) return true;
		if (is8601time.matcher(input).matches()) return true;
		if (is8601full.matcher(input).matches()) return true;
		if (is8601date.matcher(input).matches()) return true;
		return false;
	}

	/**
	 * Parse an RFC8601 date time and returns the corresponding simulation time in seconds from the
	 * start of the simulation run.
	 * An RFC8601 date time looks like YYYY-MM-DD HH:MM:SS.mmm or YYYY-MM-DDTHH:MM:SS.mmm
	 * @param simModel - JaamSimModel
	 * @param input - date string
	 * @return simulation time in seconds
	 */
	public static double parseRFC8601DateTime(JaamSimModel simModel, String input) {
		if (isRFC8601Date(input)) {
			int[] date = parseRFC8601Date(input);
			long millis = simModel.getCalendarMillis(date[0], date[1] - 1, date[2], date[3], date[4], date[5], date[6]);
			return simModel.calendarMillisToSimTime(millis);
		}

		// hh:mm format
		if (isextendshort.matcher(input).matches()) {
			int len = input.length();
			int hh = Integer.parseInt(input.substring(0, len - 3));
			int mm = Integer.parseInt(input.substring(len - 2, len));

			if (mm < 0 || mm > 59)
				throw new InputErrorException(INP_ERR_BADDATE, input);

			long ret = 0;
			ret += hh * usPerHr;
			ret += mm * usPerMin;
			return ret * 1e-6;
		}

		// hh:mm:ss format
		if (isextendtime.matcher(input).matches()) {
			int len = input.length();
			int hh = Integer.parseInt(input.substring(0, len - 6));
			int mm = Integer.parseInt(input.substring(len - 5, len - 3));
			int ss = Integer.parseInt(input.substring(len - 2, len));

			if (mm < 0 || mm > 59 || ss < 0 || ss > 59)
				throw new InputErrorException(INP_ERR_BADDATE, input);

			long ret = 0;
			ret += hh * usPerHr;
			ret += mm * usPerMin;
			ret += ss * usPerSec;
			return ret * 1e-6;
		}

		// hh:mm:ss.ssssss format
		if (isextendfull.matcher(input).matches()) {
			int len = input.indexOf('.');
			int hh = Integer.parseInt(input.substring(0, len - 6));
			int mm = Integer.parseInt(input.substring(len - 5, len - 3));
			int ss = Integer.parseInt(input.substring(len - 2, len));

			if (mm < 0 || mm > 59 || ss < 0 || ss > 59)
				throw new InputErrorException(INP_ERR_BADDATE, input);

			// grab the us values and zero-pad to a full 6-digit number
			String usChars =  input.substring(len + 1, input.length());
			int us = 0;
			switch (usChars.length()) {
			case 1: us =  Integer.parseInt(usChars) * 100000; break;
			case 2: us =  Integer.parseInt(usChars) *  10000; break;
			case 3: us =  Integer.parseInt(usChars) *   1000; break;
			case 4: us =  Integer.parseInt(usChars) *    100; break;
			case 5: us =  Integer.parseInt(usChars) *     10; break;
			case 6: us =  Integer.parseInt(usChars) *      1; break;
			}
			long ret = 0;
			ret += hh * usPerHr;
			ret += mm * usPerMin;
			ret += ss * usPerSec;
			ret += us;
			return ret * 1e-6;
		}

		throw new InputErrorException(INP_ERR_BADDATE, input);
	}

	/**
	 * Parse an RFC8601 date time and return an array containing the date numbers.
	 * An RFC8601 date time looks like YYYY-MM-DD HH:MM:SS.mmm or YYYY-MM-DDTHH:MM:SS.mmm
	 * @param input - date string
	 * @return integer array containing the year, month (0 - 11), day of month (1 - 31),
	 *         hour of day (0 - 23), minute (0 - 59), second (0 - 59), millisecond (0 - 999)
	 */
	public static int[] parseRFC8601Date(String input) {
		int YY = 0, MM = 0, DD = 0, hh = 0, mm = 0, ss = 0, ms = 0;

		// YY-MM-DD hh:mm format
		if (is8601short.matcher(input).matches()) {
			YY = Integer.parseInt(input.substring(0, 4));
			MM = Integer.parseInt(input.substring(5, 7));
			DD = Integer.parseInt(input.substring(8, 10));
			hh = Integer.parseInt(input.substring(11, 13));
			mm = Integer.parseInt(input.substring(14, 16));
		}

		// YY-MM-DD hh:mm:ss format
		else if (is8601time.matcher(input).matches()) {
			YY = Integer.parseInt(input.substring(0, 4));
			MM = Integer.parseInt(input.substring(5, 7));
			DD = Integer.parseInt(input.substring(8, 10));
			hh = Integer.parseInt(input.substring(11, 13));
			mm = Integer.parseInt(input.substring(14, 16));
			ss = Integer.parseInt(input.substring(17, 19));
		}

		// YY-MM-DD hh:mm:ss.sss format
		else if (is8601full.matcher(input).matches()) {
			YY = Integer.parseInt(input.substring(0, 4));
			MM = Integer.parseInt(input.substring(5, 7));
			DD = Integer.parseInt(input.substring(8, 10));
			hh = Integer.parseInt(input.substring(11, 13));
			mm = Integer.parseInt(input.substring(14, 16));
			ss = Integer.parseInt(input.substring(17, 19));

			// grab the us values and zero-pad to a full 6-digit number
			String usChars =  input.substring(20, input.length());
			int us = 0;
			switch (usChars.length()) {
			case 1: us =  Integer.parseInt(usChars) * 100000; break;
			case 2: us =  Integer.parseInt(usChars) *  10000; break;
			case 3: us =  Integer.parseInt(usChars) *   1000; break;
			case 4: us =  Integer.parseInt(usChars) *    100; break;
			case 5: us =  Integer.parseInt(usChars) *     10; break;
			case 6: us =  Integer.parseInt(usChars) *      1; break;
			}
			ms = us/1000;
		}

		// YY-MM-DD format
		else if (is8601date.matcher(input).matches()) {
			YY = Integer.parseInt(input.substring(0, 4));
			MM = Integer.parseInt(input.substring(5, 7));
			DD = Integer.parseInt(input.substring(8, 10));
		}

		else {
			throw new InputErrorException(INP_ERR_BADDATE, input);
		}

		if (MM < 1 || MM > 12 || DD < 1 || DD > daysInMonth[MM - 1]
				|| hh < 0 || hh > 23 || mm < 0 || mm > 59
				|| ss < 0 || ss > 59 || ms < 0 || ms > 999)
			throw new InputErrorException(INP_ERR_BADDATE, input);

		return new int[]{YY, MM, DD, hh, mm, ss, ms};
	}

	private static final int[] daysInMonth;

	static {
		daysInMonth = new int[12];
		daysInMonth[0] = 31;
		daysInMonth[1] = 29;
		daysInMonth[2] = 31;
		daysInMonth[3] = 30;
		daysInMonth[4] = 31;
		daysInMonth[5] = 30;
		daysInMonth[6] = 31;
		daysInMonth[7] = 31;
		daysInMonth[8] = 30;
		daysInMonth[9] = 31;
		daysInMonth[10] = 30;
		daysInMonth[11] = 31;
	}

	public static double parseDouble(String data)
	throws InputErrorException {
		return Input.parseDouble(data, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY);
	}

	public static double parseDouble(String data, double minValue, double maxValue)
	throws InputErrorException {
		return Input.parseDouble(data, minValue, maxValue, 1.0);
	}

	/**
	 * Convert the given String to a double and apply the given conversion factor
	 */
	public static double parseDouble(String data, double minValue, double maxValue, double factor)
	throws InputErrorException {
		double temp;
		try {
			temp = Double.parseDouble(data) * factor;
		}
		catch (NumberFormatException e) {
			throw new InputErrorException(INP_ERR_DOUBLE, data);
		}

		if (temp < minValue || temp > maxValue)
			throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, temp);

		return temp;
	}

	/**
	 * Convert the given input to a DoubleVector and apply the given conversion factor
	 */
	public static DoubleVector parseDoubles(JaamSimModel simModel, KeywordIndex kw, double minValue, double maxValue, Class<? extends Unit> unitType)
	throws InputErrorException {

		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		double factor = 1.0d;
		int numArgs = kw.numArgs();
		int numDoubles = numArgs;
		boolean includeIndex = true;

		// Parse the unit portion of the input
		String unitName = Parser.removeEnclosure("[", kw.getArg(numArgs-1), "]");
		Unit unit = Input.tryParseUnit(simModel, unitName, unitType);


		// A unit is mandatory except for dimensionless values and time values in RFC8601 date/time format
		if (unit == null && unitType != DimensionlessUnit.class && unitType != TimeUnit.class)
			throw new InputErrorException(INP_ERR_NOUNITFOUND, kw.getArg(numArgs-1), unitType.getSimpleName());

		if (unit != null) {
			factor = unit.getConversionFactorToSI();
			numDoubles = numArgs - 1;
		}

		// Parse the numeric portion of the input
		DoubleVector temp = new DoubleVector(numDoubles);
		for (int i = 0; i < numDoubles; i++) {
			try {
				// Time input
				if (unitType == TimeUnit.class) {

					// RFC8601 date/time format
					if (Input.isRFC8601DateTime(kw.getArg(i))) {
						double element = Input.parseRFC8601DateTime(simModel, kw.getArg(i));
						if (element < minValue || element > maxValue)
							throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, element);
						temp.add(element);
					}
					// Normal format
					else {
						if (unit == null) {
							includeIndex = false;
							throw new InputErrorException(INP_ERR_NOUNITFOUND, kw.getArg(numArgs-1), unitType.getSimpleName());
						}
						double element = Input.parseDouble(kw.getArg(i), minValue, maxValue, factor);
						temp.add(element);
					}
				}
				// Non-time input
				else {
					double element = Input.parseDouble(kw.getArg(i), minValue, maxValue, factor);
					temp.add(element);
				}
			} catch (InputErrorException e) {
				if (includeIndex && numDoubles > 1)
					throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
				else
					throw e;
			}
		}
		return temp;
	}

	public static String parseString(String input, ArrayList<String> validList)
	throws InputErrorException {
		return parseString(input, validList, false);
	}

	public static String parseString(String input, ArrayList<String> validList, boolean caseSensitive)
	throws InputErrorException {
		for (String valid : validList) {
			if (caseSensitive && valid.equals(input))
				return valid;

			if (!caseSensitive && valid.equalsIgnoreCase(input))
				return valid;
		}

		throw new InputErrorException(INP_ERR_BADCHOICE, validList.toString(), input);
	}

	public static ArrayList<String> parseStrings(KeywordIndex kw, ArrayList<String> validList, boolean caseSensitive)
	throws InputErrorException {
		ArrayList<String> temp = new ArrayList<>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			try {
				String element = Input.parseString(kw.getArg(i), validList);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		return temp;
	}

	public static <T extends Enum<T>> T parseEnum(Class<T> aClass, String input) {
		try {
			return Enum.valueOf(aClass, input);
		} catch (IllegalArgumentException e) {
			throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), input);
		} catch (NullPointerException e) {
			throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), input);
		}
	}

	public static <T extends Enum<T>> ArrayList<T> parseEnumList(Class<T> aClass, KeywordIndex kw) {
		ArrayList<T> ret = new ArrayList<>(kw.numArgs());
		for (int i=0; i<kw.numArgs(); i++) {
			try {
				ret.add(Enum.valueOf(aClass, kw.getArg(i)));
			} catch (IllegalArgumentException e) {
				throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), kw.getArg(i));
			} catch (NullPointerException e) {
				throw new InputErrorException(INP_ERR_BADCHOICE, Arrays.toString(aClass.getEnumConstants()), kw.getArg(i));
			}
		}
		return ret;
	}

	public static Class<? extends Entity> parseEntityType(JaamSimModel simModel, String input)
	throws InputErrorException {
		ObjectType type = Input.tryParseEntity(simModel, input, ObjectType.class);
		if (type == null)
			throw new InputErrorException("Entity type not found: %s", input);

		Class<? extends Entity> klass = type.getJavaClass();
		if (klass == null)
			throw new InputErrorException("ObjectType %s does not have a java class set", input);

		return klass;
	}

	public static void assertUnitsMatch(Class<? extends Unit> u1, Class<? extends Unit> u2)
	throws InputErrorException {
		if (u1 != u2)
			throw new InputErrorException(INP_ERR_UNITS);
	}

	public static <T extends Entity> Class<? extends T> checkCast(Class<? extends Entity> klass, Class<T> parent) {
		try {
			return klass.asSubclass(parent);
		}
		catch (ClassCastException e) {
			throw new InputErrorException(INP_ERR_NOTSUBCLASS, parent.getName(), klass.getName());
		}
	}

	public static <T> T castImplements(Entity ent, Class<T> klass)
	throws InputErrorException {
		try {
			return klass.cast(ent);
		}
		catch (ClassCastException e) {
			throw new InputErrorException(INP_ERR_INTERFACE, klass.getName(), ent.getName());
		}
	}

	private static <T extends Entity> T castEntity(Entity ent, Class<T> aClass)
	throws InputErrorException {
		try {
			return aClass.cast(ent);
		}
		catch (ClassCastException e) {
			return null;
		}
	}

	public static <T extends Entity> T parseEntity(JaamSimModel simModel, String choice, Class<T> aClass)
	throws InputErrorException {
		Entity ent = simModel.getNamedEntity(choice);
		if (ent == null) {
			throw new InputErrorException(INP_ERR_ENTNAME, choice);
		}
		T t = Input.castEntity(ent, aClass);
		if (t == null) {
			throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), choice, ent.getClass().getSimpleName());
		}
		return t;
	}

	public static <T extends Entity> T tryParseEntity(JaamSimModel simModel, String choice, Class<T> aClass) {
		return Input.castEntity(simModel.getNamedEntity(choice), aClass);
	}

	public static <T extends Unit> T tryParseUnit(JaamSimModel simModel, String choice, Class<T> aClass) {
		return Input.castEntity(simModel.getNamedEntity(choice), aClass);
	}

	public static Unit parseUnit(JaamSimModel simModel, String str)
	throws InputErrorException {
		Unit u = Input.tryParseUnit(simModel, str, Unit.class);
		if (u == null)
			throw new InputErrorException("Could not find a unit named: %s", str);

		return u;
	}

	public static Class<? extends Unit> parseUnitType(JaamSimModel simModel, String utName) {
		ObjectType ot = Input.parseEntity(simModel, utName, ObjectType.class);
		Class<? extends Unit> ut = Input.checkCast(ot.getJavaClass(), Unit.class);
		return ut;
	}

	public static <T extends Entity> ArrayList<T> parseEntityList(JaamSimModel simModel, KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			Entity ent = simModel.getNamedEntity(kw.getArg(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, kw.getArg(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group && aClass != Group.class) {
				ArrayList<Entity> gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castEntity(gList.get(j), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), gList.get(j).getClass().getSimpleName());
					}
					temp.add(t);
				}
			} else {
				T t = Input.castEntity(ent, aClass);
				if (t == null) {
					throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), kw.getArg(i), ent.getClass().getSimpleName());
				}
				temp.add(t);
			}
		}

		if (unique)
			Input.assertUnique(temp);

		return temp;
	}

	public static <T extends Entity> ArrayList<ArrayList<T>> parseListOfEntityLists(JaamSimModel simModel, KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<KeywordIndex> subArgs = kw.getSubArgs();
		ArrayList<ArrayList<T>> temp = new ArrayList<>(subArgs.size());

		for (int i = 0; i < subArgs.size(); i++) {
			try {
				ArrayList<T> element = Input.parseEntityList(simModel, subArgs.get(i), aClass, unique);
				temp.add(element);
			} catch (InputErrorException e) {
				throw new InputErrorException(INP_ERR_ELEMENT, i+1, e.getMessage());
			}
		}
		return temp;
	}

	public static <T> T parseInterfaceEntity(JaamSimModel simModel, String choice, Class<T> aClass) {

		Entity ent = simModel.getNamedEntity(choice);
		if (ent == null) {
			throw new InputErrorException(INP_ERR_ENTNAME, choice);
		}

		T temp = Input.castImplements(ent, aClass);
		if (temp == null) {
			throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), choice, ent.getClass().getSimpleName());
		}

		return temp;
	}

	public static <T> ArrayList<T> parseInterfaceEntityList(JaamSimModel simModel, KeywordIndex kw, Class<T> aClass, boolean unique)
	throws InputErrorException {
		ArrayList<T> temp = new ArrayList<>(kw.numArgs());

		for (int i = 0; i < kw.numArgs(); i++) {
			Entity ent = simModel.getNamedEntity(kw.getArg(i));
			if (ent == null) {
				throw new InputErrorException(INP_ERR_ENTNAME, kw.getArg(i));
			}

			// If we found a group, expand the list of Entities
			if (ent instanceof Group) {
				ArrayList<Entity> gList = ((Group)ent).getList();
				for (int j = 0; j < gList.size(); j++) {
					T t = Input.castImplements(gList.get(j), aClass);
					if (t == null) {
						throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), gList.get(j), gList.get(j).getClass().getSimpleName());
					}
					temp.add(t);
				}
			} else {
				T t = Input.castImplements(ent, aClass);
				if (t == null) {
					throw new InputErrorException(INP_ERR_ENTCLASS, aClass.getSimpleName(), kw.getArg(i), ent.getClass().getSimpleName());
				}
				temp.add(t);
			}
		}

		if (unique)
			Input.assertUniqueInterface(temp);

		return temp;
	}

	public static Color4d parseColour(JaamSimModel simModel, KeywordIndex kw) {

		Input.assertCountRange(kw, 1, 4);

		// Color names
		if (kw.numArgs() <= 2) {
			Color4d colAtt = ColourInput.getColorWithName(kw.getArg(0));
			if( colAtt == null )
				throw new InputErrorException(Input.INP_ERR_BADCOLOUR, kw.getArg(0));

			if (kw.numArgs() == 1)
				return colAtt;

			double a = Input.parseDouble(kw.getArg(1), 0.0d, 255.0d);
			if (a > 1.0f)
				a /= 255.0d;
			return new Color4d(colAtt.r, colAtt.g, colAtt.b, a);
		}

		// RGB
		else {
			DoubleVector dbuf = Input.parseDoubles(simModel, kw, 0.0d, 255.0d, DimensionlessUnit.class);
			double r = dbuf.get(0);
			double g = dbuf.get(1);
			double b = dbuf.get(2);
			double a = 1.0d;
			if (dbuf.size() == 4)
				a = dbuf.get(3);

			if (r > 1.0f || g > 1.0f || b > 1.0f) {
				r /= 255.0d;
				g /= 255.0d;
				b /= 255.0d;
			}
			if (a > 1.0f) {
				a /= 255.0d;
			}

			return new Color4d(r, g, b, a);
		}
	}

	public static ColourProvider parseColourProvider(KeywordIndex kw, Entity thisEnt) {
		assertCountRange(kw, 1, 4);

		// Parse the input as a colour constant
		try {
			JaamSimModel simModel = thisEnt.getJaamSimModel();
			Color4d col = parseColour(simModel, kw);
			return new ColourProvConstant(col);
		}
		catch (Exception e) {
			if (kw.numArgs() > 1)
				throw e;
		}

		// Parse the input as an expression
		try {
			return new ColourProvExpression(kw.getArg(0), thisEnt);
		}
		catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	public static BooleanProvider parseBooleanProvider(KeywordIndex kw, Entity thisEnt) {
		assertCount(kw, 1);

		// Parse the input as an boolean constant
		if (kw.getArg(0).equals(BooleanInput.TRUE))
			return new BooleanProvConstant(true);

		if (kw.getArg(0).equals(BooleanInput.FALSE))
			return new BooleanProvConstant(false);

		// Parse the input as an expression
		try {
			return new BooleanProvExpression(kw.getArg(0), thisEnt);
		}
		catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	public static <T extends Entity> EntityProvider<T> parseEntityProvider(KeywordIndex kw, Entity thisEnt, Class<T> entClass) {
		assertCount(kw, 1);

		// Parse the input as an Entity
		try {
			T ent = parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), entClass);
			return new EntityProvConstant<>(ent);
		}
		catch (InputErrorException e) {}

		// Parse the input as an Expression
		try {
			return new EntityProvExpression<>(kw.getArg(0), thisEnt, entClass);
		}
		catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	public static StringProvider parseStringProvider(KeywordIndex kw, Entity thisEnt, Class<? extends Unit> unitType) {

		// Parse the input as a StringProvExpression
		if (kw.numArgs() == 1) {
			try {
				return new StringProvExpression(kw.getArg(0), thisEnt, unitType);
			} catch (ExpError e) {}
		}

		// Parse the input as a SampleProvider object
		try {
			SampleProvider samp = Input.parseSampleExp(kw, thisEnt, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, unitType);
			return new StringProvSample(samp);
		} catch (InputErrorException e) {}

		// If nothing else works, return the constant string
		assertCount(kw, 1);
		return new StringProvConstant(kw.getArg(0));
	}

	public static SampleProvider parseSampleExp(KeywordIndex kw,
			Entity thisEnt, double minValue, double maxValue, Class<? extends Unit> unitType) {

		if (unitType == UserSpecifiedUnit.class)
			throw new InputErrorException(INP_ERR_UNITUNSPECIFIED);

		if (unitType == DimensionlessUnit.class)
			assertCount(kw, 1);
		else
			assertCountRange(kw, 1, 2);

		// If there are exactly two inputs, then it must be a number and its unit
		if (kw.numArgs() == 2) {
			DoubleVector tmp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, minValue, maxValue, unitType);
			return new SampleConstant(unitType, tmp.get(0));
		}

		// If there is only one input, it could be a SampleProvider, a dimensionless constant, or an expression

		// 1) Try parsing a SampleProvider object
		SampleProvider s = null;
		try {
			Entity ent = Input.parseEntity(thisEnt.getJaamSimModel(), kw.getArg(0), Entity.class);
			s = Input.castImplements(ent, SampleProvider.class);
		}
		catch (InputErrorException e) {}

		if (s != null) {
			Input.assertUnitsMatch(unitType, s.getUnitType());
			return s;
		}

		// 2) Try parsing a constant value
		DoubleVector tmp = null;
		try {
			tmp = Input.parseDoubles(thisEnt.getJaamSimModel(), kw, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, DimensionlessUnit.class);
		}
		catch (InputErrorException e) {}

		if (tmp != null) {
			if (unitType != DimensionlessUnit.class)
				throw new InputErrorException(INP_ERR_UNITNOTFOUND, unitType.getSimpleName());
			if (tmp.get(0) < minValue || tmp.get(0) > maxValue)
				throw new InputErrorException(INP_ERR_DOUBLERANGE, minValue, maxValue, tmp.get(0));
			return new SampleConstant(unitType, tmp.get(0));
		}

		// 3) Try parsing an expression
		try {
			String expString = kw.getArg(0);
			return new SampleExpression(expString, thisEnt, unitType);
		}
		catch (ExpError e) {
			throw new InputErrorException(e);
		}
	}

	private static void assertUnique(ArrayList<? extends Entity> list) {
		for (int i = 0; i < list.size(); i++) {
			Entity ent = list.get(i);
			for (int j = i + 1; j < list.size(); j++) {
				if (ent == list.get(j)) {
					throw new InputErrorException(INP_ERR_NOTUNIQUE, ent.getName());
				}
			}
		}
	}

	private static void assertUniqueInterface(ArrayList<?> list) {
		for (int i = 0; i < list.size(); i++) {
			Entity ent = (Entity)list.get(i);
			for (int j = i + 1; j < list.size(); j++) {
				if (ent == list.get(j)) {
					throw new InputErrorException(INP_ERR_NOTUNIQUE, ent.getName());
				}
			}
		}
	}

	public static void validateIndexedLists(ListInput<?> keys, ListInput<?> vals)
	throws InputErrorException {
		// If no values set, no validation to be done
		if (vals.getValue() == null)
			return;

		// values are set but indexed list has not
		if (keys.getValue() == null)
			throw new InputErrorException(INP_VAL_LISTSET, keys.getKeyword(), vals.getKeyword());

		// Both are set, but of differing size
		if (keys.getListSize() != vals.getListSize())
			throw new InputErrorException(INP_VAL_LISTSIZE, keys.getKeyword(), vals.getKeyword());
	}

	public static void validateInputSize(ListInput<?> list1, ListInput<?> list2)
	throws InputErrorException {
		// One list is set but not the other
		if (list1.getValue() != null && list2.getValue() == null)
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword());

		if (list1.getValue() == null && list2.getValue() != null)
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword());

		// Both are set, but of differing size
		if (list1.getListSize() != list2.getListSize())
			throw new InputErrorException(INP_VAL_LISTSIZE, list1.getKeyword(), list2.getKeyword() );
	}

	public static void validateIndexedLists(ArrayList<?> keys, DoubleVector values, String keyName, String valueName)
	throws InputErrorException {
		// If no values set, no validation to be done
		if (values == null)
			return;

		// values are set but indexed list has not
		if (keys == null)
			throw new InputErrorException(INP_VAL_LISTSET, valueName, keyName);

		// Both are set, but of differing size
		if (keys.size() != values.size())
			throw new InputErrorException(INP_VAL_LISTSIZE, keyName, valueName);
	}

	/**
	 * Returns a list of valid options if the input has limited number of
	 * choices (e.g TRUE or FALSE for BooleanInput).
	 * <p>
	 * This method must be overridden for an input to be shown with a drop-down
	 * menu in the Input Editor.
	 * @param ent TODO
	 */
	public ArrayList<String> getValidOptions(Entity ent) {
		return null;
	}

	public String getDefaultStringForKeyInputs(Class<? extends Unit> unitType) {

		if (defValue == null)
			return "";

		if (defValue.getClass() == Boolean.class) {
			if((Boolean)defValue)
				return "TRUE";

			return "FALSE";
		}

		StringBuilder tmp = new StringBuilder();
		if (defValue.getClass() == Double.class ||
		   defValue.getClass() == Integer.class ||
		   Entity.class.isAssignableFrom(Double.class)) {
			if (defValue.equals(Integer.MAX_VALUE) || defValue.equals(Double.POSITIVE_INFINITY))
				return POSITIVE_INFINITY;

			if (defValue.equals(Integer.MIN_VALUE) || defValue.equals(Double.NEGATIVE_INFINITY))
				return NEGATIVE_INFINITY;

			tmp.append(defValue);
		} else if (defValue.getClass() == SampleConstant.class ||
					defValue.getClass() == TimeSeriesConstantDouble.class ) {
			return defValue.toString();
		} else if (defValue.getClass() == DoubleVector.class) {
			DoubleVector def = (DoubleVector)defValue;
			if (def.size() == 0)
				return "";

			tmp.append(def.get(0));
			for (int i = 1; i < def.size(); i++) {
				tmp.append(SEPARATOR);
				tmp.append(def.get(i));
			}
		} else if (defValue.getClass() == IntegerVector.class) {
			IntegerVector def = (IntegerVector)defValue;
			if (def.size() == 0)
				return "";

			tmp.append(def.get(0));
			for (int i = 1; i < def.size(); i++) {
				tmp.append(SEPARATOR);
				tmp.append(def.get(i));
			}
		} else if ( Entity.class.isAssignableFrom( defValue.getClass() ) ) {
			tmp.append(((Entity)defValue).getName());
		}
		else {
			return "?????";
		}

		tmp.append(SEPARATOR);
		tmp.append(Unit.getSIUnit(unitType));
		return tmp.toString();
	}
}
