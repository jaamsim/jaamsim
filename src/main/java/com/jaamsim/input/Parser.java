/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2024 JaamSim Software Inc.
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
import java.util.regex.Pattern;

public class Parser {

/**
 * Tokenize the given record and append to the given list of tokens
 *
 * Valid delimiter characters are space, tab and comma.
 *
 * @param tokens list of String tokens to append to
 * @param rec record to tokenize and append
 * @param stripComments if true, do not append any commented tokens
 * @return true if in quoted context
 */
public static final boolean tokenize(ArrayList<String> tokens, String rec, boolean stripComments) {
	return tokenize(tokens, rec, false, stripComments);
}

public static final boolean tokenize(ArrayList<String> tokens, String rec, boolean quoted, boolean stripComments) {

	// Already in the quoted state
	int recStart = 0;
	if (quoted) {

		// Find the closing single quote
		for (int i = 0; i < rec.length(); i++) {
			char c = rec.charAt(i);
			if (c == '\'') {
				String lastRec = tokens.remove(tokens.size() - 1);
				tokens.add(lastRec + rec.substring(0, i));
				recStart = i + 1;
				break;
			}
		}

		// If no closing single quote is found, then append the entire record to the previous token
		if (recStart == 0) {
			String lastRec = tokens.remove(tokens.size() - 1);
			tokens.add(lastRec + rec + '\n');
			return true;
		}
	}

	// Records can be divided into two pieces, the contents portion and possibly
	// a commented portion, the division point is the first " character, if no
	// quoting in a record, the entire line is contents for tokenizing
	int tokStart = -1;
	int quoteStart = -1;
	int cIndex = -1;
	int endOfRec = rec.length();
	for (int i = recStart; i < rec.length(); i++) {
		char c = rec.charAt(i);
		if (c == '\'') {
			// end the current token
			if (tokStart != -1) {
				if (i - tokStart > 0) tokens.add(rec.substring(tokStart, i));
				tokStart = -1;
			}

			// Set the quoting state
			if (quoteStart != -1) {
				tokens.add(rec.substring(quoteStart + 1, i));
				quoteStart = -1;
			}
			else {
				quoteStart = i;
			}
			continue;
		}

		// we are currently quoted, skip
		if (quoteStart > -1)
			continue;

		// handle delimiter chars
		if (c == '{' || c == '}' || c == ' ' || c == '\t' || c == '\n') {
			if (tokStart != -1 && i - tokStart > 0) {
				tokens.add(rec.substring(tokStart, i));
				tokStart = -1;
			}

			if (c == '{')
				tokens.add("{");

			if (c == '}')
				tokens.add("}");

			continue;
		}

		// start a comment
		if (c == '#') {
			cIndex = i;
			endOfRec = i;
			break;
		}
		// start a new token
		if (tokStart == -1) tokStart = i;
	}

	// clean up the final trailing token
	if (tokStart != -1)
		tokens.add(rec.substring(tokStart, endOfRec));

	if (quoteStart != -1)
		tokens.add(rec.substring(quoteStart + 1, endOfRec) + '\n');

	// add comments if they exist including the leading # to denote it as commented
	if (!stripComments && cIndex > -1)
		tokens.add(rec.substring(cIndex, rec.length()));

	return quoteStart != -1;
}

public static final boolean needsQuoting(CharSequence s) {
	for (int i = 0; i < s.length(); ++i) {
		char c = s.charAt(i);
		if (c == ' ' || c == '\t' || c == '{' || c == '}' || c == '"' || c == '#' || c == '\n')
			return true;
	}
	return false;
}

public static final boolean isQuoted(CharSequence s) {
	if (s.length() < 2) return false;
	if (s.charAt(0) != '\'') return false;
	if (s.charAt(s.length() - 1) != '\'') return false;

	return true;
}

public static final String addQuotes(String str) {
	return addEnclosure("'", str, "'");
}

public static final String addQuotesIfNeeded(String str) {
	if (needsQuoting(str) && !isQuoted(str))
		return addQuotes(str);
	return str;
}

public static final String addEnclosure(String prefix, String str, String suffix) {
	StringBuilder sb = new StringBuilder();
	if (!str.startsWith(prefix))
		sb.append(prefix);
	sb.append(str);
	if (!str.endsWith(suffix))
		sb.append(suffix);
	return sb.toString();
}

public static final String removeEnclosure(String prefix, String str, String suffix) {
	if (!str.startsWith(prefix) && !str.endsWith(suffix))
		return str;
	int beginIndex = 0;
	int endIndex = str.length();
	if (str.startsWith(prefix))
		beginIndex = prefix.length();
	if (str.endsWith(suffix))
		endIndex -= suffix.length();
	return str.substring(beginIndex, endIndex);
}

/**
 * Parses the specified input string into an array of input substrings that were grouped by opening
 * and closing curly braces.
 * @param str - input string
 * @return array of substrings
 */
public static String[] splitSubstrings(String str) {
	str = str.trim();
	if (str.charAt(0) == '{') {
		str = str.substring(1);
	}
	if (!str.isEmpty() && str.charAt(str.length() - 1) == '}') {
		str = str.substring(0, str.length() - 1);
	}
	String[] ret = str.split("\\}\\s*\\{");
	for (int i = 0; i < ret.length; i++) {
		ret[i] = ret[i].trim();
	}
	return ret;
}

/**
 * Returns a formated input string that includes single quote marks when necessary around the
 * contents of each pair of curly braces.<p>
 * Note that the format for the returned string MUST be consistent with Input.getValueString
 * otherwise any carriage returns added in the Input Builder will be lost when
 * CellEditor.setInputValue is executed on the completion of editing.
 * @param str - input string to be formated
 * @return formatted input string
 */
public static final String addSubstringQuotesIfNeeded(String str) {
	String[] array = splitSubstrings(str);
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < array.length; i++) {
		if (i > 0)
			sb.append(Input.BRACE_SEPARATOR);
		sb.append("{").append(Input.BRACE_SEPARATOR);
		sb.append(addQuotesIfNeeded(array[i]));
		sb.append(Input.BRACE_SEPARATOR).append("}");
	}
	return sb.toString();
}

private static final Pattern splitter = Pattern.compile("\\s+");
public static final String addQuotesIfNeededToDefinitions(String str) {
	String[] array = Parser.splitSubstrings(str);
	StringBuilder sb = new StringBuilder();
	for (int i = 0; i < array.length; i++) {
		String[] args = splitter.split(array[i], 2);

		// Opening curly brace
		if (i > 0)
			sb.append(Input.BRACE_SEPARATOR);
		sb.append("{").append(Input.BRACE_SEPARATOR);

		// Name of the attribute or custom output
		sb.append(args[0]);

		// Parse the unit type if present
		if (args.length == 2) {
			String utName = "";
			if (args[1].endsWith("Unit")) {
				int index = args[1].lastIndexOf(' ') + 1;
				utName = args[1].substring(index);
				args[1] = args[1].substring(0, index).trim();
			}

			// Expression for the attribute or custom output value
			args[1] = Parser.addQuotesIfNeeded(args[1]);
			sb.append(Input.SEPARATOR).append(args[1]);

			// Unit type if present
			if (!utName.isEmpty()) {
				sb.append(Input.SEPARATOR).append(utName);
			}
		}
		sb.append(Input.BRACE_SEPARATOR).append("}");
	}
	return sb.toString();
}

}
