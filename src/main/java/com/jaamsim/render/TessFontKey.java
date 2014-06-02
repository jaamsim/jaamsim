/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
