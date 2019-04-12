/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.util.ArrayList;

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
	private Class<?> type;
	private final ArrayList<KeywordIndex> groupKeywordValues;

	private final ArrayList<Entity> list; // list of objects in group

	@Keyword(description = "The list of objects included in the group.",
	         exampleList = {"DisplayEntity1 DisplayEntity2"})
	private final GroupListInput groupListInput;

	@Keyword(description = "A list of additional objects to be included in " +
			 		"the already existing list of group objects.  The added " +
			 		"objects will inherit all inputs previously set for the group.",
	         exampleList = {"DisplayEntity3 DisplayEntity4"})
	private final GroupAppendListInput groupAppendListInput;

	@Keyword(description = "The object type for the group.",
	         exampleList = {"DisplayEntity"})
	private final GroupTypeInput groupTypeInput;

	{
		groupListInput = new Group.GroupListInput();
		this.addInput(groupListInput);

		groupAppendListInput = new Group.GroupAppendListInput();
		this.addInput(groupAppendListInput);

		groupTypeInput = new Group.GroupTypeInput();
		this.addInput(groupTypeInput);

		this.removeInput(active);
	}

	public Group() {
		list = new ArrayList<>();
		type = null;
		groupKeywordValues = new ArrayList<>();
	}

	private class GroupListInput extends Input<String> {
		public GroupListInput() {
			super("List", KEY_INPUTS, null);
		}

		@Override
		public void parse(Entity thisEnt, KeywordIndex kw) {
			// If adding to the list
			if( kw.getArg( 0 ).equals( "++" ) ) {
				KeywordIndex subKw = new KeywordIndex(kw, 1);

				ArrayList<Entity> addedValues = Input.parseEntityList(thisEnt.getJaamSimModel(), subKw, Entity.class, true);
				for( Entity ent : addedValues ) {
					if( list.contains( ent ) )
						throw new InputErrorException(INP_ERR_NOTUNIQUE, ent.getName());
					list.add( ent );

					// set values of appended objects to the group values
					if ( type != null ) {
						for ( int j = 0; j < groupKeywordValues.size(); j++  ) {
							KeywordIndex grpkw = groupKeywordValues.get(j);
							InputAgent.apply(ent, grpkw);
						}
					}
				}
			}
			// If removing from the list
			else if( kw.getArg( 0 ).equals( "--" ) ) {
				KeywordIndex subKw = new KeywordIndex(kw, 1);

				ArrayList<Entity> removedValues = Input.parseEntityList(thisEnt.getJaamSimModel(), subKw, Entity.class, true);
				for( Entity ent : removedValues ) {
					if( ! list.contains( ent ) )
						InputAgent.logWarning(thisEnt.getJaamSimModel(),
								"Could not remove " + ent + " from " + this.getKeyword() );
					list.remove( ent );
				}
			}
			// Otherwise, just set the list normally
			else {
				ArrayList<Entity> temp = Input.parseEntityList(thisEnt.getJaamSimModel(), kw, Entity.class, true);
				list.clear();
				list.addAll(temp);
			}
			Group.this.checkType();
		}

		@Override
		public void setTokens(KeywordIndex kw) {
			isDef = false;

			String[] args = kw.getArgArray();
			if (args.length > 0) {

				// Consider the following input case:
				// Group1 List { ++ Entity1 ...
				if (args[0].equals( "++" )) {

					this.addTokens(args);
					return;
				}

				// Consider the following input case:
				// Group1 List { -- Entity1 ...
				if (args[0].equals( "--" )) {
					if (this.removeTokens(args))
						return;
				}
			}

			valueTokens = args;
		}
	}

	private class GroupAppendListInput extends Input<String> {
		public GroupAppendListInput() {
			super("AppendList", KEY_INPUTS, null);
		}

		@Override
		public void parse(Entity thisEnt, KeywordIndex kw) {
			int originalListSize = list.size();
			ArrayList<Entity> temp = Input.parseEntityList(thisEnt.getJaamSimModel(), kw, Entity.class, true);
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
			super("GroupType", KEY_INPUTS, null);
		}

		@Override
		public void parse(Entity thisEnt, KeywordIndex kw) {
			Input.assertCount(kw, 1);
			type = Input.parseEntityType(thisEnt.getJaamSimModel(), kw.getArg(0));
			Group.this.checkType();
		}
	}


	public void saveGroupKeyword(JaamSimModel simModel, KeywordIndex kw) {
		ArrayList<String> toks = new ArrayList<>(kw.numArgs());
		for (int i = 0; i < kw.numArgs(); i++)
			toks.add(kw.getArg(i));

		KeywordIndex saved = new KeywordIndex(kw.keyword, toks, kw.context);
		groupKeywordValues.add(saved);

		// If there can never be elements in the group, throw a warning
		if( type == null && list.size() == 0 ) {
			InputAgent.logWarning(simModel,
					"The group %s has no elements to apply keyword: %s", this, kw.keyword);
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
}
