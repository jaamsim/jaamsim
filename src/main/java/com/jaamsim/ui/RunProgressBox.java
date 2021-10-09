/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 JaamSim Software Inc.
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

import javax.swing.JFrame;

public class RunProgressBox extends JFrame {

	private static RunProgressBox myInstance;

	public RunProgressBox() {
		super("Run Progress");
		setType(Type.UTILITY);
		setAutoRequestFocus(false);
		setAlwaysOnTop(true);
		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setMinimumSize(new Dimension(600, 300));
		pack();
		setLocationRelativeTo(null);
	}

	public synchronized static RunProgressBox getInstance() {
		if (myInstance == null)
			myInstance = new RunProgressBox();
		return myInstance;
	}

	public synchronized static boolean hasInstance() {
		return myInstance != null;
	}

	private synchronized static void killInstance() {
		myInstance = null;
	}

	@Override
	public void dispose() {
		killInstance();
		super.dispose();
	}

}
