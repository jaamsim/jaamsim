/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2019 JaamSim Software Inc.
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

public interface GUIListener {

	public void handleInputError(Throwable t, Entity ent);
	public void updateControls();
	public void exit(int errorCode);
	public void showTool(String name, boolean bool);
	public void setToolLocation(String name, int x, int y);
	public void setToolSize(String name, int width, int height);
	public void setControlPanelWidth(int width);
	public void invokeErrorDialogBox(String title, String fmt, Object... args);

}
