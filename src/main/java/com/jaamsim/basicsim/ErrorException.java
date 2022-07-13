/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import com.jaamsim.input.ExpError;

/**
 * Custom exception thrown when a program error is encountered.
 */
public class ErrorException extends RuntimeException {

	public String entName;
	public String keyword;
	public int index;
	public String source;
	public int position;

	public ErrorException(String src, int pos, String name, String msg) {
		this(src, pos, name, "", -1, msg, null);
	}

	public ErrorException(String src, int pos, String name, String msg, Throwable cause) {
		this(src, pos, name, "", -1, msg, cause);
	}

	public ErrorException(String src, int pos, String name, String key, int ind, String msg, Throwable cause) {
		super(msg, cause);
		entName = name;
		keyword = key;
		index = ind;
		source = src;
		position = pos;
	}

	public ErrorException(String format, Object... args) {
		this("", -1, "",  "", -1, String.format(format, args), null);
	}

	public ErrorException(Entity ent, String msg) {
		this("", -1, ent.getName(), "", -1, msg, null);
	}

	public ErrorException(Entity ent, ExpError e) {
		this(e.source, e.pos, ent.getName(), "", -1, e.getMessage(), e);
	}

	public ErrorException(Entity ent, String key, ExpError e) {
		this(e.source, e.pos, ent.getName(), key, -1, e.getMessage(), e);
	}

	public ErrorException(Entity ent, Throwable cause) {
		this("", -1, ent.getName(), "", -1, cause.getMessage(), cause);
	}

	public ErrorException( Throwable cause ) {
		this("", -1, "", "", -1, cause.getMessage(), cause);
	}

	@Override
	public String getMessage() {
		StringBuilder sb = new StringBuilder();
		if (entName != null && !entName.isEmpty())
			sb.append(entName);
		if (!keyword.isEmpty())
			sb.append(String.format(" keyword '%s'", keyword));
		if (index > 0)
			sb.append(String.format(", index (%d)", index));
		if (sb.length() > 0)
			sb.append(":\n");
		sb.append(super.getMessage());
		return sb.toString();
	}

}
