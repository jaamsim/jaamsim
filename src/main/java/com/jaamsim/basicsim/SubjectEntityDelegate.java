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

public class SubjectEntityDelegate implements SubjectEntity {

	private final SubjectEntity subject;
	private final ArrayList<ObserverEntity> observerList = new ArrayList<>();

	public SubjectEntityDelegate(SubjectEntity subj) {
		subject = subj;
	}

	public void clear() {
		observerList.clear();
	}

	@Override
	public void registerObserver(ObserverEntity obs) {
		observerList.add(obs);
	}

	@Override
	public void notifyObservers() {
		Entity ent = (Entity) subject;
		if (ent.isTraceFlag()) ent.trace(0, "notifyObservers: %s", observerList);

		for (ObserverEntity obs : observerList) {
			obs.observerUpdate(subject);
		}
	}

	@Override
	public ArrayList<ObserverEntity> getObserverList() {
		return observerList;
	}

	@Override
	public String toString() {
		return String.format("%s: %s", subject, observerList);
	}

}
