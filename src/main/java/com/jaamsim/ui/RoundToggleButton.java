/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2015 Ausenco Engineering Canada Inc.
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

// Adapted from CC0-licensed code posted by "aterai" (TERAI Atshuhiro)
// on http://ateraimemo.com/Swing/RoundImageButton.html

package com.jaamsim.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JToggleButton;

class RoundToggleButton extends JToggleButton {

	protected Shape shape, base;

	public RoundToggleButton(Icon icon) {
		super(icon);
	}

	@Override public void updateUI() {
		super.updateUI();
		setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		//setBackground(Color.BLACK);
		setContentAreaFilled(false);
		setFocusPainted(false);
		setAlignmentY(Component.TOP_ALIGNMENT);
		initShape();
	}

	@Override public Dimension getPreferredSize() {
		Icon icon = getIcon();
		Insets i = getInsets();
		int iw = Math.max(icon.getIconWidth(), icon.getIconHeight());
		return new Dimension(iw + i.right + i.left, iw + i.top + i.bottom);
	}

	protected void initShape() {
		if (!getBounds().equals(base)) {
			Dimension s = getPreferredSize();
			base = getBounds();
			shape = new Ellipse2D.Float(0, 0, s.width - 1, s.height - 1);
		}
	}

	@Override protected void paintBorder(Graphics g) {
		initShape();
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(getBackground());
		g2.draw(shape);
		g2.dispose();
	}

	@Override public boolean contains(int x, int y) {
		initShape();
		return shape == null ? false : shape.contains(x, y);
	}
}
