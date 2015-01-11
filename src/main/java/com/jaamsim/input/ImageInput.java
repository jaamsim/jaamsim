/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2014 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
package com.jaamsim.input;

import java.awt.image.BufferedImage;
import java.net.URI;

import javax.imageio.ImageIO;

public class ImageInput extends Input<BufferedImage> {

	public ImageInput(String key, String cat, BufferedImage def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw) throws InputErrorException {
		BufferedImage temp;
		URI uri = Input.parseURI(kw);

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
