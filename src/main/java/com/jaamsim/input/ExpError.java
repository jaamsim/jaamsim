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

		if (source == null)
			return super.getMessage();

		StringBuilder sb = new StringBuilder();
		sb.append("<html>");
		sb.append(super.getMessage()).append("<br><br>");

		// Append the source expression and an 'arrow' pointing at the error
		String src = source.replaceAll("%", "%%");
		sb.append("<pre><font color=\"red\">").append(src).append("<br>");

		for (int i = 0; i < pos; ++i) {
			sb.append(" ");
		}

		sb.append("<b>^</b></font></pre></html>");

		return sb.toString();
	}
}
