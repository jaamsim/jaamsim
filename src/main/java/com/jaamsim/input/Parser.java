/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
import java.util.regex.Pattern;

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
	final int cIndex = rec.indexOf("\"");
	final int endOfRec = cIndex == -1 ? rec.length() : cIndex;

	int tokStart = -1;
	int quoteStart = -1;
	for (int i = 0; i < endOfRec; i++) {
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

public static final boolean needsQuoting(String s) {
	for (int i = 0; i < s.length(); ++i) {
		char c = s.charAt(i);
		if (c == ' ' || c == '\t' || c == '{' || c == '}')
			return true;
	}
	return false;
}

private static final Pattern isquoted = Pattern.compile("'.*'");
public static final boolean isQuoted(String s) {
	return isquoted.matcher(s).matches();
}

/**
 * Remove all commented tokens (starting with the " character)
 * @param tokens
 */
public static final void removeComments(ArrayList<String> tokens) {
	for (int i = tokens.size() - 1; i >= 0; i--) {
		if (tokens.get(i).startsWith("\""))
			tokens.remove(i);
	}
}
}
