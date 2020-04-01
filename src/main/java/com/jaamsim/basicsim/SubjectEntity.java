/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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

import java.util.ArrayList;

public interface SubjectEntity {

	/**
	 * Records that the specified entity is monitoring this subject entity.
	 * @param obs - observer entity monitoring this subject entity
	 */
	public void registerObserver(ObserverEntity obs);

	/**
	 * Notifies the observers that are monitoring this subject entity that a state change has
	 * occurred.
	 */
	public void notifyObservers();

	/**
	 * Returns a list of the observers that are monitoring this subject entity.
	 * @return list of observers
	 */
	public ArrayList<ObserverEntity> getObserverList();

}
