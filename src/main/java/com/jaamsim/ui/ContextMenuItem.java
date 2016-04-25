/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2016 Harvey Harrison
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

import com.jaamsim.basicsim.Entity;

public interface ContextMenuItem {
	public String getMenuText();
	public boolean supportsEntity(Entity ent);
	public void performAction(Entity ent, int x, int y);
}

