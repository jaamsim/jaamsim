/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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
package com.jaamsim.states;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;

public interface DowntimeUser {
	public String getName();
	public ArrayList<DowntimeEntity> getMaintenanceEntities();
	public ArrayList<DowntimeEntity> getBreakdownEntities();
	public boolean canStartDowntime(DowntimeEntity down);
	public void prepareForDowntime(DowntimeEntity down);
	public void startDowntime(DowntimeEntity down);
	public void endDowntime(DowntimeEntity down);
}
