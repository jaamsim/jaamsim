/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019-2024 JaamSim Software Inc.
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

import com.jaamsim.Commands.Command;
import com.jaamsim.Graphics.View;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.math.Vec3d;

public interface GUIListener extends EventTimeListener {

	public void handleInputError(Throwable t, Entity ent);
	public void invokeErrorDialogBox(String title, String msg);
	public void updateObjectSelector(Entity ent);
	public void updateModelBuilder();
	public void updateInputEditor(Entity ent);
	public void storeAndExecute(Command cmd);
	public void updateAll();
	public void deleteEntity(Entity ent);
	public void renameEntity(Entity ent, String newName);
	public void addView(View v);
	public void removeView(View v);
	public void createWindow(View v);
	public void closeWindow(View v);
	public int getNextViewID();
	public Vec3d getPOI(View v);
	public void allowResizing(boolean bool);

}
