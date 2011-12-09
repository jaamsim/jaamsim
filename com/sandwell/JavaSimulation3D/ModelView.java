/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Canvas3D;
import javax.media.j3d.GraphicsConfigTemplate3D;
import javax.media.j3d.PhysicalBody;
import javax.media.j3d.PhysicalEnvironment;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.View;
import javax.media.j3d.ViewPlatform;

/**
 * Encapsulates the scenegraph for a single viewpoint.
 */
public class ModelView extends BranchGroup {
	// All views will have the same physicalBody and physicalEnvironment
	private static final PhysicalBody physicalBody;
	private static final PhysicalEnvironment physicalEnvironment;
	private static final GraphicsConfigTemplate3D graphicsTemplate;
	private static final GraphicsConfiguration graphicsConfig;

	// Transform group used to transform the view
	private final TransformGroup transformGroup;
	private final View view;
	private final ViewPlatform viewPlatform;
	private final Canvas3D canvas;

	static {
		physicalBody = new PhysicalBody();
		physicalEnvironment = new PhysicalEnvironment();
		graphicsTemplate = new GraphicsConfigTemplate3D();
		graphicsTemplate.setStereo(GraphicsConfigTemplate3D.UNNECESSARY);
		graphicsConfig = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getBestConfiguration(graphicsTemplate);
	}

	public ModelView() {
		// Set capabilities for branchgroup
		this.setCapability(ModelView.ALLOW_DETACH);

		// Create view object - distance from front to back clip planes should
		// be no greater than a factor of 1000 for z-buffer correctness
		view = new View();
		view.setBackClipPolicy(View.PHYSICAL_EYE);
		view.setBackClipDistance(OrbitBehavior.MAX_RADIUS);
		view.setFrontClipPolicy(View.PHYSICAL_EYE);
		view.setFrontClipDistance(OrbitBehavior.MIN_RADIUS);

		// Create viewPlatform and set capabilities
		viewPlatform = new ViewPlatform();

		// Create the transformGroup for the viewPlatform and set capabilities
		transformGroup = new TransformGroup();
		transformGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		canvas = new Canvas3D(graphicsConfig, false);

		// Attach all objects in the view hierarchy
		this.addChild(transformGroup);
		transformGroup.addChild(viewPlatform);
		view.attachViewPlatform(viewPlatform);
		view.setPhysicalBody(physicalBody);
		view.setPhysicalEnvironment(physicalEnvironment);
		view.addCanvas3D(canvas);
	}

	void setViewState(Transform3D trans, double fov, double radius) {
		transformGroup.setTransform(trans);
		view.setFieldOfView(fov);
		view.setFrontClipDistance(0.1 * radius);
		view.setBackClipDistance(300 * radius);
	}

	public Canvas3D getCanvas3D() {
		return canvas;
	}

	public long getFrameNumber() {
		return view.getFrameNumber();
	}

	public void destroy() {
		view.removeAllCanvas3Ds();
		view.attachViewPlatform(null);
		view.setPhysicalBody(null);
		view.setPhysicalEnvironment(null);
		this.removeAllChildren();
	}
}
