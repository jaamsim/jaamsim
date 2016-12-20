/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

public class Parser {

/**
 * Tokenize the given record and append to the given list of tokens
 *
 * Valid delimiter characters are space, tab and comma.
 *
 * @param tokens list of String tokens to append to
 * @param rec record to tokenize and append
 */
public static final void tokenize(ArrayList<String> tokens, String rec) {
	tokenize(tokens, rec, false);
}

/**
 * Tokenize the given record and append to the given list of tokens
 *
 * Valid delimiter characters are space, tab and comma.
 *
 * @param tokens list of String tokens to append to
 * @param rec record to tokenize and append
 * @param stripComments if true, do not append any commented tokens
 */
public static final void tokenize(ArrayList<String> tokens, String rec, boolean stripComments) {
	// Records can be divided into two pieces, the contents portion and possibly
	// a commented portion, the division point is the first " character, if no
	// quoting in a record, the entire line is contents for tokenizing
	int tokStart = -1;
	int quoteStart = -1;
	int cIndex = -1;
	int endOfRec = rec.length();
	for (int i = 0; i < rec.length(); i++) {
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
		if (c == '{' || c == '}' || c == ' ' || c == '\t') {
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

		if (c == '\"') {
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
		tokens.add(rec.substring(quoteStart + 1, endOfRec));

	// add comments if they exist including the leading " to denote it as commented
	if (!stripComments && cIndex > -1)
		tokens.add(rec.substring(cIndex, rec.length()));
}

public static final boolean needsQuoting(CharSequence s) {
	for (int i = 0; i < s.length(); ++i) {
		char c = s.charAt(i);
		if (c == ' ' || c == '\t' || c == '{' || c == '}')
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
	StringBuilder sb = new StringBuilder();
	if (!str.startsWith("'"))
		sb.append("'");
	sb.append(str);
	if (!str.endsWith("'"))
		sb.append("'");
	return sb.toString();
}
}
