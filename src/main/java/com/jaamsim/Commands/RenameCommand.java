/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2020 JaamSim Software Inc.
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
package com.jaamsim.Commands;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;

public class RenameCommand implements Command {

	private final Entity entity;
	private final String oldName;
	private final String newName;

	public RenameCommand(Entity ent, String name) {
		this(ent, ent.getName(), name);
	}

	public RenameCommand(Entity ent, String name0, String name1) {
		entity = ent;
		oldName = name0;
		newName = name1;
	}

	private static void rename(Entity ent, String name) {
		ent.setName(name);
		ent.getJaamSimModel().setSessionEdited(true);

		// Update the entity's label
		if (ent instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity) ent;
			EntityLabel label = EntityLabel.getLabel(dEnt);
			if (label != null) {
				label.setName(name + "_Label");
				label.updateForTargetNameChange();
			}
		}

		// Update the entries in the Object Selector
		GUIListener gui = ent.getJaamSimModel().getGUIListener();
		if (gui != null) {
			gui.updateObjectSelector();
			gui.updateModelBuilder();
		}
	}

	@Override
	public void execute() {
		rename(entity, newName);
	}

	@Override
	public void undo() {
		rename(entity, oldName);
	}

	@Override
	public Command tryMerge(Command cmd) {
		if (!(cmd instanceof RenameCommand)) {
			return null;
		}
		RenameCommand renameCmd = (RenameCommand) cmd;
		if (entity != renameCmd.entity) {
			return null;
		}
		return new RenameCommand(entity, oldName, renameCmd.newName);
	}

	@Override
	public boolean isChange() {
		return !newName.equals(oldName);
	}

	@Override
	public JaamSimModel getJaamSimModel() {
		return entity.getJaamSimModel();
	}

	@Override
	public String toString() {
		return String.format("Rename '%s' to '%s'", oldName, newName);
	}

}
