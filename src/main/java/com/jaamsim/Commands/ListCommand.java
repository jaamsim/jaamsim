/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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

import java.util.ArrayList;

public class ListCommand implements Command {

	private final ArrayList<Command> list;

	private final static String ELLIPSIS = "...";
	private final static int MAX_LENGTH = 40;

	public ListCommand(ArrayList<Command> lst) {
		list = lst;
	}

	@Override
	public void execute() {
		for (Command cmd : list) {
			cmd.execute();
		}
	}

	@Override
	public void undo() {
		for (Command cmd : list) {
			cmd.undo();
		}
	}

	@Override
	public Command tryMerge(Command cmd) {
		if (!(cmd instanceof ListCommand)) {
			return null;
		}
		ListCommand arrayCmd = (ListCommand) cmd;
		if (list.size() != arrayCmd.list.size())
			return null;

		ArrayList<Command> newList = new ArrayList<>(list.size());
		for (int i = 0; i < list.size(); i++) {
			Command mergedCmd = list.get(i).tryMerge(arrayCmd.list.get(i));
			if (mergedCmd == null)
				return null;
			newList.add(mergedCmd);
		}
		return new ListCommand(newList);
	}

	@Override
	public boolean isChange() {
		for (Command cmd : list) {
			if (cmd.isChange())
				return true;
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < list.size(); i++) {
			if (i > 0)
				sb.append("; ");
			if (sb.length() > MAX_LENGTH) {
				sb.append(ELLIPSIS);
				break;
			}
			sb.append(list.get(i));
		}
		return sb.toString();
	}

}
