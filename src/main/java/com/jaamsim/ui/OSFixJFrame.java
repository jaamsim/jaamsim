/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2024 JaamSim Software Inc.
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

import java.awt.Dimension;
import java.awt.Point;

import javax.swing.JFrame;

/**
 * Corrects OS-dependent bugs in JFrame.
 */
public class OSFixJFrame extends JFrame {

	public OSFixJFrame(String name) {
		super(name);
	}

	public OSFixJFrame() {
		super();
	}

	@Override
	public void setLocation(int x, int y) {
		Point fix = OSFix.getLocationAdjustment();
		super.setLocation(x+fix.x, y+fix.y);
	}

	@Override
	public Point getLocation() {
		Point fix = OSFix.getLocationAdjustment();
		return new Point(super.getX()-fix.x, super.getY()-fix.y);
	}

	@Override
	public void setSize(int x, int y) {
		Point fix = OSFix.getSizeAdjustment();
		super.setSize(x+fix.x, y+fix.y);
	}

	@Override
	public Dimension getSize() {
		Point fix = OSFix.getSizeAdjustment();
		return new Dimension(super.getSize().width-fix.x, super.getSize().height-fix.y);
	}

}
