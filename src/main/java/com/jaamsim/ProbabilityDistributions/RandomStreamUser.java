/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018-2023 JaamSim Software Inc.
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
package com.jaamsim.ProbabilityDistributions;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.input.InputAgent;

public interface RandomStreamUser {

	/**
	 * Returns the random seed used by this object.
	 * @return random seed
	 */
	public int getStreamNumber();

	/**
	 * Returns the keyword used to enter the random seed.
	 * @return random seed keyword
	 */
	public String getStreamNumberKeyword();

	public static void setUniqueRandomSeed(RandomStreamUser rsu) {
		Entity ent = (Entity) rsu;
		JaamSimModel simModel = ent.getJaamSimModel();

		// Do nothing if a valid seed has been set previously
		int seed = rsu.getStreamNumber();
		if (seed >= 0 && simModel.getRandomStreamUsers(seed).size() <= 1)
			return;

		// Set the smallest seed value that has not been used already
		seed = simModel.getSmallestAvailableStreamNumber();
		String key = rsu.getStreamNumberKeyword();
		InputAgent.applyIntegers(ent, key, seed);

		// Always mark the entity and keyword as 'edited' so that the seed value is saved to the
		// configuration file (required for SubModel.update)
		ent.getInput(key).setEdited(true);
		ent.setEdited();
	}

}
