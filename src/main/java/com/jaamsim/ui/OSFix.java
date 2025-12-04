/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2024 JaamSim Software Inc.
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
package com.jaamsim.ui;

/**
 * Provides OS-dependent fixes for bugs in Swing.
 */
public class OSFix {
	private static final String OS_NAME;
	private static final OSFix resizeAdjust;
	private static final OSFix noresizeAdjust;

	static {
		OS_NAME = System.getProperty("os.name");
		switch (OS_NAME) {
		case "Windows 10":
		case "Windows 11":
			// Drop shadow border in Windows 10 and above: 7,0,7,7 (left, top, right, bottom)
			resizeAdjust = new OSFix(-7, 0, 15, 8);
			// Non-resizable drop shadow border in Windows 10 and above: 2,0,2,2 (left, top, right, bottom)
			noresizeAdjust = new OSFix(-2, 0, 5, 3);
			break;
		default:
			resizeAdjust = new OSFix(0, 0, 0, 0);
			noresizeAdjust = new OSFix(0, 0, 0, 0);
			break;
		}
	}

	public final int x;
	public final int y;
	public final int width;
	public final int height;

	private OSFix(int x, int y, int width, int height) {
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
	}

	public static final boolean isWindows() {
		return OS_NAME.startsWith("Win");
	}

	public static final OSFix get(boolean resizable) {
		if (resizable)
			return resizeAdjust;
		else
			return noresizeAdjust;
	}
}
