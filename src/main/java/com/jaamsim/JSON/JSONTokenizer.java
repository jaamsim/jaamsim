/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2022 JaamSim Software Inc.
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
package com.jaamsim.JSON;

import java.util.ArrayList;

public class JSONTokenizer {

	public static final int STRING_TYPE = 0;
	public static final int NUM_TYPE = 1;
	public static final int SYM_TYPE = 2;
	public static final int KEYWORD_TYPE = 3;

	public static class Token {
		public int type;
		public String value;
		public int pos;
	}

	public static Token symTok(char c, int pos) {
		Token ret = new Token();
		ret.type = SYM_TYPE;
		ret.value = String.valueOf(c);
		ret.pos = pos;
		return ret;

	}

	public static boolean isWhiteSpace(char c) {
		boolean isWhite = c == ' ';
		isWhite = isWhite || c == '\t';
		isWhite = isWhite || c == '\r';
		isWhite = isWhite || c == '\n';
		return isWhite;
	}

	public static boolean isNumberStart(char c) {
		return (c >= '0' && c <= '9') || c == '-';
	}

	public static char nextChar(String input, int pos) {
		if (pos >= input.length()) {
			return '$';
		}
		return input.charAt(pos);
	}

	private static int getStringToken(ArrayList<Token> res, int startPos, String input) throws JSONError{
		int closePos = startPos + 1;

		while (closePos < input.length()) {
			char c = nextChar(input, closePos);;
			if (c == '"' && nextChar(input, closePos-1) != '\\')
				break;

			closePos++;
		}
		if (closePos == input.length()) {
			throw new JSONError(input, startPos, "No closing quote character for string.");
		}

		// Scan the string and un-escape any sequences in it
		StringBuilder sb = new StringBuilder();
		int pos = startPos;
		while (pos < closePos) {
			char c = nextChar(input, pos++);

			if (c < ' ') {
				throw new JSONError(input, pos, "Invalid character in string");
			}

			if (c != '\\') {
				sb.append(c);
				continue;
			}
			assert(pos != closePos);

			c = nextChar(input, pos++);
			switch(c) {
			case '\"':
				sb.append('\"');
				continue;
			case '\\':
				sb.append('\\');
				continue;
			case '/':
				sb.append('/');
				continue;
			case 'b':
				sb.append('\b');
				continue;
			case 'f':
				sb.append('\f');
				continue;
			case 'n':
				sb.append('\n');
				continue;
			case 'r':
				sb.append('\r');
				continue;
			case 't':
				sb.append('\t');
				continue;
			case 'u':
				if (closePos - pos < 4) {
					throw new JSONError(input, pos, "Unicode escape sequence is too short");
				}
				String unicodeStr = input.substring(pos, pos+4);
				try {
					int unicodeVal = Integer.parseUnsignedInt(unicodeStr, 16);
					pos+=4;
					sb.appendCodePoint(unicodeVal);
				} catch (NumberFormatException ex) {
					throw new JSONError(input, pos, "Could not parse unicode escape sequence");
				}
				sb.append('\t');
				continue;
			}

		}
		Token tok = new Token();
		tok.type = STRING_TYPE;
		tok.value = sb.toString();
		tok.pos = startPos;
		res.add(tok);
		return closePos+1;

	}

	private static String getDigits(int startPos, String input) {
		StringBuilder sb = new StringBuilder();
		int pos = startPos;
		char c = nextChar(input, pos++);
		while(c >= '0' && c <= '9') {
			sb.append(c);
			c = nextChar(input, pos++);
		}
		return sb.toString();
	}

	private static int getNumToken(ArrayList<Token> res, int startPos, String input) throws JSONError {
		int pos = startPos;
		char c = nextChar(input, pos);
		StringBuilder numStr = new StringBuilder();

		if (c == '-') {
			numStr.append(c);
			pos++;
		}

		String intStr = getDigits(pos, input);
		if (intStr.length() == 0) {
			throw new JSONError(input, pos, "Number format error");
		}

		numStr.append(intStr);
		pos += intStr.length();

		c = nextChar(input, pos);
		if (c == '.') {
			numStr.append(c);
			pos++;
			String fracStr = getDigits(pos, input);
			if (fracStr.length() == 0) {
				throw new JSONError(input, pos, "Number format error");
			}
			numStr.append(fracStr);
			pos+=fracStr.length();
			c = nextChar(input, pos);
		}
		if (c == 'e' || c == 'E') {
			numStr.append(c);
			pos++;
			c = nextChar(input, pos);
			if (c == '+' || c == '-') {
				numStr.append(c);
				pos++;
			}
			String expStr = getDigits(pos, input);
			if (expStr.length() == 0) {
				throw new JSONError(input, pos, "Number format error");
			}
			numStr.append(expStr);
			pos+=expStr.length();
		}

		Token tok = new Token();
		tok.type = NUM_TYPE;
		tok.value = numStr.toString();
		tok.pos = startPos;
		res.add(tok);
		return pos;

	}

	private static int geKeywordToken(ArrayList<Token> res, int pos, String input) throws JSONError {
		String keyword = null;
		int endPos = -1;
		if (input.length() >= pos + 4 && input.substring(pos, pos+4).equals("true")) {
			keyword = "true";
			endPos = pos+4;
		}
		if (input.length() >= pos + 5 && input.substring(pos, pos+5).equals("false")) {
			keyword = "false";
			endPos = pos+5;
		}
		if (input.length() >= pos + 4 && input.substring(pos, pos+4).equals("null")) {
			keyword = "null";
			endPos = pos+4;
		}

		if (keyword == null) {
			throw new JSONError(input, pos, "Unexpected value");
		}

		Token tok = new Token();
		tok.type = KEYWORD_TYPE;
		tok.value = keyword;
		tok.pos = pos;
		res.add(tok);
		return endPos;
	}

	public static ArrayList<Token> tokenize(String input) throws JSONError {
		int pos = 0;

		ArrayList<Token> res = new ArrayList<>();

		while (pos < input.length()) {
			char c = nextChar(input, pos++);
			if (isWhiteSpace(c)) {
				continue;
			}

			if (c == '[' || c == ']' || c == '{' || c == '}' || c == ',' || c == ':') {
				res.add(symTok(c,pos));
				continue;
			}
			if (c == '\"') {
				pos = getStringToken(res, pos, input);
				continue;
			}
			if (isNumberStart(c)) {
				pos = getNumToken(res, pos-1, input);
				continue;
			}
			// Otherwise check for the 3 simple reserved words
			if (c == 't' || c == 'f' || c == 'n') {
				pos = geKeywordToken(res, pos-1, input);
				continue;
			}
			throw new JSONError(input, pos-1, "Unexpected value");
		}
		return res;
	}
}
