/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.video;

import java.awt.image.BufferedImage;
import java.util.concurrent.TimeUnit;

public interface MediaWriter {

	public abstract void encodeVideo(int i, BufferedImage img, long _writeTimeMS, TimeUnit milliseconds);

	public abstract void close();

	public abstract void addVideoStream(int i, int j, int width, int height);

}
