/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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

import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.jaamsim.units.DistanceUnit;

public class DefineViewCommand implements Command {

	private final JaamSimModel simModel;
	private View view;
	private final String viewName;
	private final Vec3d viewPosition;
	private final Vec3d viewCenter;
	private final IntegerVector windowPos;

	public DefineViewCommand(JaamSimModel sim, String str, Vec3d viewPos, Vec3d viewCntr, IntegerVector winPos) {
		simModel = sim;
		view = null;
		viewName = str;
		viewPosition = viewPos;
		viewCenter = viewCntr;
		windowPos = winPos;
	}

	@Override
	public void execute() {

		// Create the new view
		view = InputAgent.defineEntityWithUniqueName(simModel, View.class, viewName, "", true);
		view.getJaamSimModel().setSessionEdited(true);

		// Position the window on the screen
		if (windowPos != null) {
			InputAgent.applyIntegers(view, "WindowPosition", windowPos.get(0), windowPos.get(1));
		}

		// Display the window
		RenderManager.inst().createWindow(view);
		FrameBox.setSelectedEntity(view, false);
		InputAgent.applyArgs(view, "ShowWindow", "TRUE");

		// Set the camera position
		if (viewPosition != null) {
			KeywordIndex kw1 = InputAgent.formatVec3dInput("ViewPosition", viewPosition, DistanceUnit.class);
			InputAgent.apply(view, kw1);
		}
		if (viewCenter != null) {
			KeywordIndex kw2 = InputAgent.formatVec3dInput("ViewCenter", viewCenter, DistanceUnit.class);
			InputAgent.apply(view, kw2);
		}
	}

	@Override
	public void undo() {
		view.kill();
		view.getJaamSimModel().setSessionEdited(true);
	}

	@Override
	public Command tryMerge(Command cmd) {
		return null;
	}

	@Override
	public boolean isChange() {
		return true;
	}

	@Override
	public JaamSimModel getJaamSimModel() {
		return simModel;
	}

	@Override
	public String toString() {
		return String.format("New View: '%s'", viewName);
	}

}
