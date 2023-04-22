/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2023 JaamSim Software Inc.
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

import java.util.Comparator;

import com.jaamsim.input.Input;

public class AutoCompleteComparator implements Comparator<Object> {

	private String name;

	public void setName(String str) {
		name = str.toUpperCase();
	}

	@Override
	public int compare(Object o1, Object o2) {
		String str1 = o1.toString().toUpperCase();
		String str2 = o2.toString().toUpperCase();
		boolean bool1 = str1.substring(0, name.length()).equals(name);
		boolean bool2 = str2.substring(0, name.length()).equals(name);
		if (bool1 != bool2) {
			return Boolean.compare(bool2, bool1);
		}
		return Input.uiSortOrder.compare(str1, str2);
	}

}
