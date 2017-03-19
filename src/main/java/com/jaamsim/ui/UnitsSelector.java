/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JMenu;
import javax.swing.JRadioButtonMenuItem;

import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.units.Unit;

public class UnitsSelector {

	public static void populateMenu(JMenu menu) {

		// Loop through the unit types that have been defined
		for (String utName : Unit.getUnitTypeList()) {
			ObjectType ot = Input.parseEntity(utName, ObjectType.class);
			final Class<? extends Unit> ut = Input.checkCast(ot.getJavaClass(), Unit.class);

			ArrayList<? extends Unit> unitList = Unit.getUnitList(ut);
			if (unitList.isEmpty())
				continue;

			// For each unit type create a sub-menu of units from which to select
			String selectedUnitName = Unit.getDisplayedUnit(ut);
			JMenu subMenu = new JMenu(utName);
			for (final Unit u : unitList) {
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(u.getName());
				if (u.getName().equals(selectedUnitName)) {
					item.setSelected(true);
				}
				item.addActionListener( new ActionListener() {

					@Override
					public void actionPerformed( ActionEvent event ) {
						Unit.setPreferredUnit(ut, u);
						ArrayList<String> toks = new ArrayList<>();
						for (Unit pref : Unit.getPreferredUnitList()) {
							toks.add(pref.getName());
						}
						KeywordIndex kw = new KeywordIndex("DisplayedUnits", toks, null);
						InputAgent.apply(Simulation.getInstance(), kw);
					}
				} );
				subMenu.add(item);
			}
			menu.add(subMenu);
		}
	}

}
