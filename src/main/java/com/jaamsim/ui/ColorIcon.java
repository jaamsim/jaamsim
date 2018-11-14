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
package com.jaamsim.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.Icon;

/**
 * Displays a rectangular icon for use in the user interface.
 * @author Harry King
 *
 */
public class ColorIcon implements Icon {

	private final int width;
	private final int height;
	private Color fillColor = Color.BLACK;
	private Color outlineColor = Color.BLACK;

	public ColorIcon(int w, int h) {
		width = w;
		height = h;
	}

	public void setFillColor(Color col) {
		fillColor = col;
	}

	public void setOutlineColor(Color col) {
		outlineColor = col;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y) {
		Graphics2D g2d = (Graphics2D) g.create();

		// Fill the rectangle with the selected color
		g2d.setColor(fillColor);
		g2d.fillRect(x, y, width - 1, height - 1);

		// Draw the outline around the rectangle
		g2d.setColor(outlineColor);
		g2d.drawRect(x, y, width - 1, height - 1);

		g2d.dispose();
	}

	@Override
	public int getIconWidth() {
		return width;
	}

	@Override
	public int getIconHeight() {
		return height;
	}

}
