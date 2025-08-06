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
package com.jaamsim.input;

import java.awt.image.BufferedImage;
import java.net.URI;

import javax.imageio.ImageIO;

import com.jaamsim.basicsim.Entity;

public class ImageInput extends Input<BufferedImage> {

	public ImageInput(String key, String cat, BufferedImage def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		BufferedImage temp;
		URI uri = Input.parseURI(thisEnt.getJaamSimModel(), kw);

		// Confirm that the file exists
		if (!InputAgent.fileExists(uri))
			throw new InputErrorException("The specified file does not exist.\n" +
					"File path = %s", kw.getArg(0));

		try {
			temp = ImageIO.read(uri.toURL());
		}
		catch (Exception ex) {
			throw new InputErrorException("Bad image file");
		}

		value = temp;
	}

}
