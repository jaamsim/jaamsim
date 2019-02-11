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
package com.jaamsim.Graphics;

import java.util.Arrays;

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

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("(");
		sb.append(Arrays.toString(sizes)).append(", ");
		sb.append(Arrays.toString(colors)).append(", ");
		sb.append(visible).append(")");
		return sb.toString();
	}

}
