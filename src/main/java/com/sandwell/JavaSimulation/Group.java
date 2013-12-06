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

import com.jaamsim.input.InputAgent;

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
	private final ArrayList<StringVector> groupKeywordValues;

	private final ArrayList<Entity> list; // list of objects in group

	{
		addEditableKeyword( "List",       "",   "", false, "Key Inputs" );
		addEditableKeyword( "AppendList", "",   "", true,  "Key Inputs" );
		addEditableKeyword( "GroupType",  "",   "", false, "Key Inputs" );

		reportable = new BooleanInput("Reportable", "Key Inputs", true);
		this.addInput(reportable, true);
	}

	public Group() {
		list = new ArrayList<Entity>();
		type = null;
		groupKeywordValues = new ArrayList<StringVector>();
	}

	/**
	 * Processes the input data corresponding to the specified keyword. If syntaxOnly is true,
	 * checks input syntax only; otherwise, checks input syntax and process the input values.
	 */
	@Override
	public void readData_ForKeyword(StringVector data, String keyword)
	throws InputErrorException {


		try {
			if( "List".equalsIgnoreCase( keyword ) ) {
				ArrayList<Entity> temp = Input.parseEntityList(data, Entity.class, true);
				list.clear();
				list.addAll(temp);
				this.checkType();
				return;
			}
			if( "AppendList".equalsIgnoreCase( keyword ) ) {
				int originalListSize = list.size();
				ArrayList<Entity> temp = Input.parseEntityList(data, Entity.class, true);
				for (Entity each : temp) {
					if (!list.contains(each))
						list.add(each);
				}
				this.checkType();
				// set values of appended objects to the group values
				if ( type != null ) {
					for ( int i = originalListSize; i < list.size(); i ++ ) {

						Entity ent = list.get( i );
						for ( int j = 0; j < this.getGroupKeywordValues().size(); j++  ) {
							StringVector currentData = new StringVector(this.getGroupKeywordValues().get(j));
							String currentKeyword = currentData.remove(0);
							Input<?> in = ent.getInput(currentKeyword);
							if(in != null) {
								InputAgent.apply(ent, in, currentData, null);
							}
								// The keyword is not on the editable keyword list
							else {
								InputAgent.logWarning("Keyword %s could not be found for Entity %s.", currentKeyword, ent.getInputName());
							}
						}
					}
				}

				return;
			}

			if( "GroupType".equalsIgnoreCase( keyword ) ) {
				Input.assertCount(data, 1);
				type = Input.parseEntityType(data.get(0));
				this.checkType();
				return;
			}
		}
		catch( Exception e ) {
			InputAgent.logError("Entity: %s Keyword: %s - %s", this.getName(), keyword, e.getMessage());
		}
	}

	public void saveGroupKeyword(StringVector data, String keyword) {

		// for all other keywords, keep track in keywordValues vector
		StringVector record = new StringVector( data );
		record.insertElementAt(keyword, 0);
		this.getGroupKeywordValues().add( record );

		// If there can never be elements in the group, throw a warning
		if( type == null && list.size() == 0 ) {
			InputAgent.logWarning("The group %s has no elements to apply keyword: %s", this, keyword);
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

	private ArrayList<StringVector> getGroupKeywordValues() {
		return groupKeywordValues;
	}
}
