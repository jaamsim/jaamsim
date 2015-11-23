/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.awt.Font;

public class TessFontKey {

	private String _fontName;
	private int _fontStyle;

	/**
	 * Font name and style, identical to what would be used to create an AWT font
	 * @param fontName
	 */
	public TessFontKey(String fontName) {
		this(fontName, Font.PLAIN);
	}

	public TessFontKey(String fontName, int fontStyle) {
		_fontName = fontName;
		_fontStyle = fontStyle;
	}

	public String getFontName() {
		return _fontName;
	}
	public int getFontStyle() {
		return _fontStyle;
	}

	@Override
	public int hashCode() {
		int code = _fontName.hashCode();
		code = code ^ _fontStyle * 71; // font style is less than 5 so add some addition bits
		return code;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TessFontKey)) {
			return false;
		}
		TessFontKey tfk = (TessFontKey)o;

		return _fontName.equals(tfk._fontName) && _fontStyle == tfk._fontStyle;
	}
}
