/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

public class ExpTokenizer {

	public static final int VAR_TYPE = 0;
	public static final int NUM_TYPE = 1;
	public static final int SYM_TYPE = 2;
	public static final int SQ_TYPE = 3; // Square quoted tokens
	public static final int STRING_TYPE = 4; // A literal string
	public static final int NULL_TYPE = 5; // The specific null keyword

	public static class Token {
		public int type;
		public String value;
		public int pos;
	}

	public static boolean isWhiteSpace(char c) {
		if (Character.isWhitespace(c)) return true;
		return false;
	}

	// Use java style identifiers for now
	public static boolean isVarStartChar(char c) {
		if (Character.isJavaIdentifierStart(c)) return true;

		return false;
	}

	private static boolean isVarMemberChar(char c) {
		if (Character.isJavaIdentifierPart(c)) return true;

		return false;
	}

	private static boolean isNumMemberChar(char c) {
		if (Character.isDigit(c)) return true;
		if (c=='.') return true;

		return false;
	}

	// List of 'long' symbols to check for, in order
	private static ArrayList<String> longSymbols = new ArrayList<>();

	static {
		longSymbols.add("==");
		longSymbols.add("!=");
		longSymbols.add("<=");
		longSymbols.add(">=");
		longSymbols.add("&&");
		longSymbols.add("||");
	}

	public static ArrayList<Token> tokenize(String input) throws ExpError {
		int pos = 0;

		ArrayList<Token> res = new ArrayList<>();

		while (pos < input.length()) {
			char c = input.charAt(pos);
			if (isWhiteSpace(c)) {
				pos++;
				continue;
			}

			if (c == '#') {
				pos = skipComment(pos, input);
				continue;
			}
			if (c == '[') {
				// This is the beginning of a square quoted string
				pos = getSQToken(res, pos, input);
				continue;
			}
			if (c == '"') {
				// This is the beginning of a regular quoted string
				pos = getQuotedToken(res, pos, input);
				continue;
			}

			if (isVarStartChar(c)) {
				pos = getVarToken(res, pos, input);
				continue;
			}

			if (Character.isDigit(c)){
				pos = getNumToken(res, pos, input);
				continue;
			}

			pos = getSymbolToken(res, pos, input);
		}

		return res;
	}

	private static int getVarToken(ArrayList<Token> res, int startPos, String input) {
		Token newTok = new Token();
		newTok.type = VAR_TYPE;
		newTok.pos = startPos;

		int pos = startPos;
		StringBuilder sb = new StringBuilder();

		while (pos < input.length()) {
			char next = input.charAt(pos);
			if (!isVarMemberChar(next)) {
				break;
			}
			sb.append(next);
			++pos;
		}

		newTok.value = sb.toString();
		// If the string is specifically "null" this is actually a keyword
		if (newTok.value.equals("null")) {
			newTok.type = NULL_TYPE;
		}

		res.add(newTok);
		return pos;
	}

	private static int skipComment(int startPos, String input) throws ExpError {
		int closePos = startPos + 1;

		while (closePos < input.length()) {
			char c = input.charAt(closePos);
			if (c == '#')
				return closePos + 1;

			closePos++;
		}
		// Made it to the end of the input
		throw new ExpError(input, startPos, "No closing mark for comment");
	}

	private static int getSQToken(ArrayList<Token> res, int startPos, String input) throws ExpError {

		int closePos = startPos + 1;

		while (closePos < input.length()) {
			char c = input.charAt(closePos);
			if (c == '[')
				throw new ExpError(input, closePos, "Nested square brace");
			if (c == ']')
				break;

			closePos++;
		}

		if (closePos == input.length()) {
			throw new ExpError(input, startPos, "No closing square brace for brace");
		}

		Token newTok = new Token();
		newTok.pos = startPos;
		newTok.type = SQ_TYPE;
		newTok.value = input.substring(startPos + 1, closePos);
		res.add(newTok);
		return closePos + 1;

	}

	private static int getQuotedToken(ArrayList<Token> res, int startPos, String input) throws ExpError {

		int closePos = startPos + 1;

		while (closePos < input.length()) {
			char c = input.charAt(closePos);
			if (c == '"')
				break;

			closePos++;
		}

		if (closePos == input.length()) {
			throw new ExpError(input, startPos, "No closing quote character for string.");
		}

		Token newTok = new Token();
		newTok.pos = startPos;
		newTok.type = STRING_TYPE;
		newTok.value = input.substring(startPos + 1, closePos);
		res.add(newTok);
		return closePos + 1;
	}

	// TODO: Should this include 'f' or 'd' as in the java convention? Also, should we support hex?
	private static int getNumToken(ArrayList<Token> res, int startPos, String input) throws ExpError {
		Token newTok = new Token();
		newTok.type = NUM_TYPE;
		newTok.pos = startPos;

		int pos = startPos;
		StringBuilder sb = new StringBuilder();

		while (pos < input.length()) {
			char next = input.charAt(pos);
			if (!isNumMemberChar(next)) {
				break;
			}
			sb.append(next);
			++pos;
		}

		// Now check for an optional exponent
		if (pos < input.length() &&
		    (input.charAt(pos) == 'e' || input.charAt(pos) == 'E')) {

			sb.append(input.charAt(pos++));

			// Now check for an option -
			if (pos < input.length() && input.charAt(pos) == '-') {
				sb.append(input.charAt(pos++));
			}
			// An another digit
			while (pos < input.length()) {
				char next = input.charAt(pos);
				if (!Character.isDigit(next)) {
					break;
				}
				sb.append(next);
				++pos;
			}
		}

		newTok.value = sb.toString();

		// Check that this string can be parsed to a valid double
		try {
			Double.parseDouble(newTok.value);
		} catch (NumberFormatException ex) {
			throw new ExpError(input, startPos, "Error parsing number literal: " + newTok.value);
		}

		res.add(newTok);
		return pos;
	}

	private static int getSymbolToken(ArrayList<Token> res, int startPos, String input) {
		// For now, tokens are single character strings that are not numbers, variables or whitespace
		Token newTok = new Token();
		newTok.type = SYM_TYPE;
		newTok.pos = startPos;

		for (String s : longSymbols) {
			if (input.length() - startPos >= s.length() &&
				  input.substring(startPos, startPos + s.length()).equals(s)) {
				// This option matches the current long symbol
				newTok.value = s;
			}
		}
		if (newTok.value == null) {
			// Use a simple one character symbol
			newTok.value = input.substring(startPos, startPos + 1);
		}

		res.add(newTok);
		return startPos + newTok.value.length();

	}
}
