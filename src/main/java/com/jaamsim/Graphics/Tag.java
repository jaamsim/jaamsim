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
package com.jaamsim.Graphics;

import com.jaamsim.math.Color4d;

public class Tag {
	public final Color4d[] colors;
	public final double[] sizes;
	public final boolean visible;

	public Tag(Color4d[] c, double[] s, boolean v) {
		colors = c;
		sizes = s;
		visible = v;
	}

	public final boolean colorsMatch(Color4d[] c) {
		if (colors == null || colors.length != c.length) return false;

		for (int i = 0; i < colors.length; i++) {
			if (!colors[i].equals(c[i])) return false;
		}
		return true;
	}

	public final boolean sizesMatch(double[] s) {
		if (sizes == null || sizes.length != s.length) return false;

		for (int i = 0; i < sizes.length; i++) {
			if (sizes[i] != s[i]) return false;
		}
		return true;
	}

	public final boolean visMatch(boolean v) {
		return v == visible;
	}
}
