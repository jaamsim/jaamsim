/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.Commands;

import java.util.ArrayList;
import java.util.Locale;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;

public class DefineViewCommand implements Command {

	private View view;
	private final String viewName;
	private final Vec3d viewPosition;
	private final Vec3d viewCenter;
	private final IntegerVector windowPosition;

	public DefineViewCommand(String str, Vec3d viewPos, Vec3d viewCntr, IntegerVector winPos) {
		view = null;
		viewName = str;
		viewPosition = viewPos;
		viewCenter = viewCntr;
		windowPosition = winPos;
	}

	@Override
	public void execute() {

		// Create the new view
		view = InputAgent.defineEntityWithUniqueName(View.class, viewName, "", true);

		// Position the window on the screen
		if (windowPosition != null) {
			ArrayList<String> arg = new ArrayList<>();
			arg.add(String.format((Locale)null, "%d", windowPosition.get(0)));
			arg.add(String.format((Locale)null, "%d", windowPosition.get(1)));
			InputAgent.apply(view, new KeywordIndex("WindowPosition", arg, null));
		}

		// Display the window
		RenderManager.inst().createWindow(view);
		FrameBox.setSelectedEntity(view, false);
		InputAgent.applyArgs(view, "ShowWindow", "TRUE");

		// Set the camera position
		if (viewPosition != null) {
			KeywordIndex kw1 = InputAgent.formatPointInputs("ViewPosition", viewPosition, "m");
			InputAgent.apply(view, kw1);
		}
		if (viewCenter != null) {
			KeywordIndex kw2 = InputAgent.formatPointInputs("ViewCenter", viewCenter, "m");
			InputAgent.apply(view, kw2);
		}
	}

	@Override
	public void undo() {
		view.kill();
		RenderManager.inst().closeWindow(view);
	}

	@Override
	public Command tryMerge(Command cmd) {
		return null;
	}

	@Override
	public String toString() {
		return String.format("New View: '%s'", viewName);
	}

}
