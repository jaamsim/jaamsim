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

public class ExpError extends Exception {
	public final String source;
	public final int pos;

	ExpError(String source, int pos, String msg) {
		super(msg);
		this.source = source;
		this.pos = pos;
	}

	ExpError(String source, int pos, String fmt, Object... args) {
		this(source, pos, String.format(fmt, args));
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.getMessage()).append("\n");

		if (source == null)
			return sb.toString();

		// Otherwise, append the source expression and an 'arrow' pointing at the error
		String src = source.replaceAll("%", "%%");
		sb.append(src).append("\n");

		for (int i = 0; i < pos; ++i) {
			sb.append(" ");
		}

		sb.append("^\n");

		return sb.toString();
	}
}
