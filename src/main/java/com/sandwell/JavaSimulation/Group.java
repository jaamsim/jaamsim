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
package com.sandwell.JavaSimulation;

import java.util.ArrayList;

import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;

/**
 * Group class - for storing a list of objects
 *
 * For input of the form <object> <keyword> <value>:
 * If the group appears as the object in a line of input, then the keyword and value applies to each member of the group.
 * If the group appears as the value in a line of input, then the list of objects is used as the value.
 */
public class Group extends Entity {
	@Keyword(description = "If TRUE show the members of the group as a seperate table in the output " +
	                "reports, including an entry for \"Total\"",
	         example = "Group1 Reportable { TRUE }")
	private final BooleanInput reportable;

	private Class<?> type;
	private final ArrayList<KeywordIndex> groupKeywordValues;

	private final ArrayList<Entity> list; // list of objects in group

	{
		this.addInput(new Group.GroupListInput());
		this.addInput(new Group.GroupAppendListInput());
		this.addInput(new Group.GroupTypeInput());

		reportable = new BooleanInput("Reportable", "Key Inputs", true);
		this.addInput(reportable);
	}

	public Group() {
		list = new ArrayList<Entity>();
		type = null;
		groupKeywordValues = new ArrayList<KeywordIndex>();
	}

	private class GroupListInput extends Input<String> {
		public GroupListInput() {
			super("List", "Key Inputs", null);
		}

		@Override
		public void parse(KeywordIndex kw) {
			ArrayList<Entity> temp = Input.parseEntityList(kw, Entity.class, true);
			list.clear();
			list.addAll(temp);
			Group.this.checkType();
		}
	}

	private class GroupAppendListInput extends Input<String> {
		public GroupAppendListInput() {
			super("AppendList", "Key Inputs", null);
		}

		@Override
		public void parse(KeywordIndex kw) {
			int originalListSize = list.size();
			ArrayList<Entity> temp = Input.parseEntityList(kw, Entity.class, true);
			for (Entity each : temp) {
				if (!list.contains(each))
					list.add(each);
			}
			Group.this.checkType();
			// set values of appended objects to the group values
			if ( type != null ) {
				for ( int i = originalListSize; i < list.size(); i ++ ) {
					Entity ent = list.get( i );
					for ( int j = 0; j < groupKeywordValues.size(); j++  ) {
						KeywordIndex grpkw = groupKeywordValues.get(j);
						InputAgent.apply(ent, grpkw);
					}
				}
			}
		}
	}

	private class GroupTypeInput extends Input<String> {
		public GroupTypeInput() {
			super("GroupType", "Key Inputs", null);
		}

		@Override
		public void parse(KeywordIndex kw) {
			Input.assertCount(kw, 1);
			type = Input.parseEntityType(kw.getArg(0));
			Group.this.checkType();
		}
	}


	public void saveGroupKeyword(KeywordIndex kw) {
		ArrayList<String> toks = new ArrayList<String>(kw.numArgs() + 4);
		for (int i = 0; i < kw.numArgs(); i++)
			toks.add(kw.getArg(i));

		KeywordIndex saved = new KeywordIndex(toks, kw.keyword, 0, toks.size(), kw.context);
		groupKeywordValues.add(saved);

		// If there can never be elements in the group, throw a warning
		if( type == null && list.size() == 0 ) {
			InputAgent.logWarning("The group %s has no elements to apply keyword: %s", this, kw.keyword);
		}
	}

	private void checkType() {
		if (type == null)
			return;

		for (Entity each : this.getList()) {
			if (!type.isInstance(each))
				throw new InputErrorException("The Entity: %s is not of Type: %s", each, type.getSimpleName());
		}
	}

	// ******************************************************************************************
	// ACCESSING
	// ******************************************************************************************

	public ArrayList<Entity> getList() {
		return list;
	}

	public boolean isReportable() {
		return reportable.getValue();
	}
}
