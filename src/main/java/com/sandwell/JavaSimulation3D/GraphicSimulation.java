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

import java.util.ArrayList;

import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.InputAgent;
import com.jaamsim.ui.ExceptionBox;
import com.jaamsim.ui.FrameBox;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Simulation;

/**
 * Abstracts the Java 3D interface out of simulation for those programs which do
 * not require that functionality. <br>
 * Simulation provides simulation functionality. <br>
 * GraphicSimulation provides user interaction functionality. <br>
 * <br>
 * Integrates with GUIFrame and OrbitBehaviour to provide user interaction.
 */
public class GraphicSimulation extends Simulation {
	protected String modelName = "Transportation Logistics Simulator";

	/**
	 *  Constructor for the Graphic Simulation.
	 *	Establishes the User Interface
	 *  Protected makes this a 'singleton' class -- only one instance of it exists.  is instantiated through 'getSimulation()' method.
	 */
	public GraphicSimulation() {
		// Create main frame
		DisplayEntity.setSimulation( this );
	}

	public void setModelName( String newModelName ) {
		modelName = newModelName;
	}

	public String getModelName() {
		return modelName;
	}

	@Override
	public void clear() {
		super.clear();

		// close warning/error trace file
		InputAgent.closeLogFile();

		ArrayList<FrameBox> boxes = new ArrayList<FrameBox>(FrameBox.getAllFB());
		for (FrameBox each : boxes) {
			each.dispose();
		}

		EntityPallet.clear();

		if (RenderManager.isGood()) {
			RenderManager.inst().closeAllWindows();
		}

		// Kill all entities except simulation
		while(Entity.getAll().size() > 1) {
			Entity ent = Entity.getAll().get(Entity.getAll().size()-1);
			ent.kill();
		}

		GUIFrame.instance().updateForSimulationState();
	}

	@Override
	public void start() {
		try {
			super.start();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void pause() {
		try {
			super.pause();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void stop() {
		try {
			super.stop();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void resume() {
		try {
			super.resume();
			GUIFrame.instance().updateForSimulationState();
		}
		catch( Throwable t ) {
			ExceptionBox.instance().setError(t);
		}
	}

	@Override
	public void updateForSimulationState() {
		super.updateForSimulationState();
		GUIFrame.instance().updateForSimulationState();
	}
}
