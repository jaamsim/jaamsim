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
package com.jaamsim.input;

/**
 * Custom exception thrown when an error due to bad input is encountered.
 */
public class InputErrorException extends RuntimeException {

	public String source;
	public int position;

	public InputErrorException(int pos, String src, String msg) {
		this(pos, src, msg, null);
	}

	public InputErrorException(int pos, String src, String msg, Throwable cause) {
		super(msg, cause);
		source = src;
		position = pos;
	}

	public InputErrorException(String format, Object... args) {
		this(-1, "", String.format(format, args), null);
	}

	public InputErrorException(ExpError e) {
		this(e.pos, e.source, e.getMessage(), e);
	}

}
