/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.resourceObjects;

import com.jaamsim.BasicObjects.DowntimeEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.StateUserEntity;

public class ResourceUnit extends StateUserEntity implements Seizable {

	public ResourceUnit() {}

	@Override
	public ResourcePool getResourcePool() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canSeize(DisplayEntity ent) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void seize(DisplayEntity ent) {
		// TODO Auto-generated method stub
	}

	@Override
	public void release() {
		// TODO Auto-generated method stub
	}

	@Override
	public DisplayEntity getAssignment() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void thresholdChanged() {
		// TODO Auto-generated method stub
	}

	@Override
	public boolean canStartDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void prepareForDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void startDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

	@Override
	public void endDowntime(DowntimeEntity down) {
		// TODO Auto-generated method stub
	}

}
