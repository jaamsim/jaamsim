/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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
package com.jaamsim.Thresholds;

import java.util.ArrayList;

public interface ThresholdUser {

	/**
	 * Returns the Thresholds used by this object.
	 * @return the Threshold list.
	 */
	public abstract ArrayList<Threshold> getThresholds();

	/**
	 * Called whenever one of the Thresholds used by this object has changed
	 * its state from either open to closed or from closed to open.
	 */
	public abstract void thresholdChanged();
}
