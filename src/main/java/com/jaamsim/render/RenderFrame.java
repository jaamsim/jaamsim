/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.awt.Frame;

import com.jogamp.opengl.awt.GLCanvas;


public class RenderFrame extends Frame {

	/**
	 * Package level constructor. This will only be created by Renderer
	 */

	RenderFrame(int width, int height, String title, GLCanvas canvas) {
		super(title);
		setSize(width, height);
		setLayout(new java.awt.BorderLayout());
		add(canvas, java.awt.BorderLayout.CENTER);
		validate();
		setVisible(true);
	}
}
