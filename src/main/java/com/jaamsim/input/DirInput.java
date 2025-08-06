/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
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

import java.io.File;
import java.net.URI;

import com.jaamsim.basicsim.Entity;

public class DirInput extends StringInput {
	private URI dir;

	public DirInput(String key, String cat, String def) {
		super(key, cat, def);
		dir = null;
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		Input.assertCount(kw, 1);

		URI temp = Input.parseURI(thisEnt.getJaamSimModel(), kw);

		// If there is no context (e.g. reading from Input Editor),
		// and a config file exists, then resolve the config file uri against this one
		if( kw.context == null && thisEnt.getJaamSimModel().getConfigFile() != null ) {
			URI configDirURI = thisEnt.getJaamSimModel().getConfigFile().getParentFile().toURI();
			temp = configDirURI.resolve(temp.getSchemeSpecificPart());
		}

		try {
			File f = new File(temp);
			if (f.exists() && !f.isDirectory())
				throw new InputErrorException("File Entity parse error: %s is not a directory", kw.getArg(0));
		}
		catch (IllegalArgumentException e) {
			throw new InputErrorException("Unable to parse the directory:\n%s", kw.getArg(0));
		}

		value = kw.getArg(0);
		dir = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_DIR;
	}

	@Override
	public void reset() {
		super.reset();
		dir = null;
	}

	public File getDir() {
		if (dir == null) {
			return null;
		}

		try {
			File f = new File(dir);
			return f;
		}
		catch (IllegalArgumentException e) {}

		return null;
	}
}
