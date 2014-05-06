/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

public class ExpTokenizer {

	public static final int VAR_TYPE = 0;
	public static final int NUM_TYPE = 1;
	public static final int SYM_TYPE = 2;
	public static final int SQ_TYPE = 3; // Square quoted tokens

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

	public static class Error extends Exception {
		public Error(String err) {
			super(err);
		}
	}

	// List of 'long' symbols to check for, in order
	private static ArrayList<String> longSymbols = new ArrayList<String>();

	static {
		longSymbols.add("==");
		longSymbols.add("!=");
		longSymbols.add("<=");
		longSymbols.add(">=");
		longSymbols.add("&&");
		longSymbols.add("||");
	}

	public static ArrayList<Token> tokenize(String input) throws Error {
		int pos = 0;

		ArrayList<Token> res = new ArrayList<Token>();

		while (pos < input.length()) {
			char c = input.charAt(pos);
			if (isWhiteSpace(c)) {
				pos++;
				continue;
			}

			if (c == '[') {
				// This is the beginning of a square quoted string
				pos = getSQToken(res, pos, input);
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
		res.add(newTok);
		return pos;
	}

	private static int getSQToken(ArrayList<Token> res, int startPos, String input) throws Error {
		Token newTok = new Token();
		newTok.type = SQ_TYPE;
		newTok.pos = startPos;

		int closePos = startPos + 1;
		while (closePos < input.length()) {
			char c = input.charAt(closePos);
			if (c == '[')
				throw new Error(String.format("Nested square quotes at pos: %d", closePos));
			if (c == ']')
				break;

			closePos++;
		}

		if (closePos == input.length()) {
			throw new Error(String.format("No closing square brace for brace at pos: %d", startPos));
		}

		newTok.value = input.substring(startPos + 1, closePos);
		res.add(newTok);
		return closePos + 1;
	}

	// TODO: Should this include 'f' or 'd' as in the java convention? Also, should we support hex?
	private static int getNumToken(ArrayList<Token> res, int startPos, String input) throws Error {
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
			throw new Error("Error parsing number literal: " + newTok.value);
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
