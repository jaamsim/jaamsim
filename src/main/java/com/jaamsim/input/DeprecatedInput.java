/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2010-2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import com.jaamsim.basicsim.Entity;

public class DeprecatedInput extends Input<String> {
	private boolean fatal;

	public DeprecatedInput(String key, String msg) {
		super(key, "", "");
		value = msg;
		fatal = true;
	}

	public void setFatal(boolean fatal) {
		this.fatal = fatal;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		if (fatal)
			throw new InputErrorException(value);

		InputAgent.logWarning(thisEnt.getJaamSimModel(), "%s - %s", this.getKeyword(), value);
	}
}
