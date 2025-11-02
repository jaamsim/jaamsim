/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2025 JaamSim Software Inc.
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

public class WindowDefaults {
	public final int DEFAULT_GUI_WIDTH;
	public final int DEFAULT_GUI_HEIGHT;
	public final int COL1_WIDTH;
	public final int COL2_WIDTH;
	public final int COL3_WIDTH;
	public final int COL4_WIDTH;
	public final int COL1_START;
	public final int COL2_START;
	public final int COL3_START;
	public final int COL4_START;
	public final int HALF_TOP;
	public final int HALF_BOTTOM;
	public final int TOP_START;
	public final int BOTTOM_START;
	public final int LOWER_HEIGHT;
	public final int LOWER_START;
	public final int VIEW_HEIGHT;
	public final int VIEW_WIDTH;
	public final int VIEW_OFFSET;

	public WindowDefaults(int winWidth, int winHeight, int guiHeight, int guiX, int guiY) {
		DEFAULT_GUI_WIDTH = winWidth;
		DEFAULT_GUI_HEIGHT = guiHeight;
		COL1_WIDTH = 220;
		COL4_WIDTH = 520;
		int middleWidth = DEFAULT_GUI_WIDTH - COL1_WIDTH - COL4_WIDTH;
		COL2_WIDTH = Math.max(520, middleWidth / 2);
		COL3_WIDTH = Math.max(420, middleWidth - COL2_WIDTH);
		VIEW_WIDTH = DEFAULT_GUI_WIDTH - COL1_WIDTH;

		COL1_START = guiX;
		COL2_START = COL1_START + COL1_WIDTH;
		COL3_START = COL2_START + COL2_WIDTH;
		COL4_START = Math.min(COL3_START + COL3_WIDTH, winWidth - COL4_WIDTH);

		HALF_TOP = (winHeight - DEFAULT_GUI_HEIGHT) / 2;
		HALF_BOTTOM = (winHeight - DEFAULT_GUI_HEIGHT - HALF_TOP);
		LOWER_HEIGHT = Math.min(250, (winHeight - DEFAULT_GUI_HEIGHT) / 3);
		VIEW_HEIGHT = winHeight - DEFAULT_GUI_HEIGHT - LOWER_HEIGHT;

		TOP_START = guiY + DEFAULT_GUI_HEIGHT;
		BOTTOM_START = TOP_START + HALF_TOP;
		LOWER_START = TOP_START + VIEW_HEIGHT;

		VIEW_OFFSET = 50;
	}
}
