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

public interface Command {

	/**
	 * Performs the change to the model.
	 */
	public void execute();

	/**
	 * Reverses the change to the model.
	 */
	public void undo();

	/**
	 * Attempts to merge this command with the specified command.
	 * @param cmd - command to merge
	 * @return merged command or null if commands are incompatible
	 */
	public Command tryMerge(Command cmd);

	/**
	 * Returns whether the command changes any inputs.
	 * @return true if one or more inputs are changed
	 */
	public boolean isChange();
}
