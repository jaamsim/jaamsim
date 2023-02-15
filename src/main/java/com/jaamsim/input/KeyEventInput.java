/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017-2021 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jogamp.newt.event.KeyEvent;

public class KeyEventInput extends Input<Integer> {

	public KeyEventInput(String key, String cat, Integer def) {
		super(key, cat, def);
	}

	private static final HashMap<String, Integer> keyCodeMap; // key code for a given key name
	private static final HashMap<Integer, String> keyNameMap; // key name for a given key code

	static {
		keyCodeMap = new HashMap<>();
		keyNameMap = new HashMap<>();
		initMaps();
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw) throws InputErrorException {
		Input.assertCount(kw, 1);

		// Many keys are represented by a name
		Integer temp = getKeyCode(kw.getArg(0));
		if (temp != null) {
			value = temp;
			return;
		}

		// Remaining keys are represented by the key code
		temp = Input.parseInteger(kw.getArg(0));
		value = temp;
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_KEYEVENT;
	}

	@Override
	public void getValueTokens(ArrayList<String> toks) {
		if (value == null || isDef)
			return;

		toks.add(getKeyName(value));
	}

	@Override
	public String getDefaultString(JaamSimModel simModel) {
		if (defValue == null)
			return null;

		String ret = getKeyName(defValue);
		if (ret != null)
			return ret;

		return defValue.toString();
	}

	public void setDefaultValue(String name) {
		setDefaultValue(getKeyCode(name));
	}

	public static Integer getKeyCode(String name) {
		Integer code;
		if (name.length() == 1) {
			code = (int) KeyEvent.utf16ToVKey(name.charAt(0));
		}
		else {
			code = keyCodeMap.get(name.toUpperCase());
		}
		return code;
	}

	public static String getKeyName(int code) {
		String name;
		name = keyNameMap.get(code);
		if (name == null) {
			name = String.valueOf((char) code);
		}
		return name;
	}

	private static void mapKeyEvent(String name, int code) {
		if (keyCodeMap.put(name, code) != null) {
			System.out.println(String.format("KeyEvent name added twice: %s ", name));
		}
		if (keyNameMap.put(code, name) != null) {
			System.out.println(String.format("KeyEvent code added twice: %s ", code));
		}
	}

	private static void initMaps() {
		mapKeyEvent("ADD",         KeyEvent.VK_ADD);
		mapKeyEvent("ALT",         KeyEvent.VK_ALT);
		mapKeyEvent("CONTROL",     KeyEvent.VK_CONTROL);
		mapKeyEvent("DECIMAL",     KeyEvent.VK_DECIMAL);
		mapKeyEvent("DELETE",      KeyEvent.VK_DELETE);
		mapKeyEvent("DIVIDE",      KeyEvent.VK_DIVIDE);
		mapKeyEvent("DOWN",        KeyEvent.VK_DOWN);
		mapKeyEvent("END",         KeyEvent.VK_END);
		mapKeyEvent("ENTER",       KeyEvent.VK_ENTER);
		mapKeyEvent("ESCAPE",      KeyEvent.VK_ESCAPE);
		mapKeyEvent("F1",          KeyEvent.VK_F1);
		mapKeyEvent("F2",          KeyEvent.VK_F2);
		mapKeyEvent("F3",          KeyEvent.VK_F3);
		mapKeyEvent("F4",          KeyEvent.VK_F4);
		mapKeyEvent("F5",          KeyEvent.VK_F5);
		mapKeyEvent("F6",          KeyEvent.VK_F6);
		mapKeyEvent("F7",          KeyEvent.VK_F7);
		mapKeyEvent("F8",          KeyEvent.VK_F8);
		mapKeyEvent("F9",          KeyEvent.VK_F9);
		mapKeyEvent("F10",         KeyEvent.VK_F10);
		mapKeyEvent("F11",         KeyEvent.VK_F11);
		mapKeyEvent("F12",         KeyEvent.VK_F12);
		mapKeyEvent("F13",         KeyEvent.VK_F13);
		mapKeyEvent("F14",         KeyEvent.VK_F14);
		mapKeyEvent("F15",         KeyEvent.VK_F15);
		mapKeyEvent("F16",         KeyEvent.VK_F16);
		mapKeyEvent("F17",         KeyEvent.VK_F17);
		mapKeyEvent("F18",         KeyEvent.VK_F18);
		mapKeyEvent("F19",         KeyEvent.VK_F19);
		mapKeyEvent("F20",         KeyEvent.VK_F20);
		mapKeyEvent("F21",         KeyEvent.VK_F21);
		mapKeyEvent("F22",         KeyEvent.VK_F22);
		mapKeyEvent("F23",         KeyEvent.VK_F23);
		mapKeyEvent("F24",         KeyEvent.VK_F24);
		mapKeyEvent("HOME",        KeyEvent.VK_HOME);
		mapKeyEvent("INSERT",      KeyEvent.VK_INSERT);
		mapKeyEvent("LEFT",        KeyEvent.VK_LEFT);
		mapKeyEvent("MULTIPLY",    KeyEvent.VK_MULTIPLY);
		mapKeyEvent("NUM_LOCK",    KeyEvent.VK_NUM_LOCK);
		mapKeyEvent("NUMPAD0",     KeyEvent.VK_NUMPAD0);
		mapKeyEvent("NUMPAD1",     KeyEvent.VK_NUMPAD1);
		mapKeyEvent("NUMPAD2",     KeyEvent.VK_NUMPAD2);
		mapKeyEvent("NUMPAD3",     KeyEvent.VK_NUMPAD3);
		mapKeyEvent("NUMPAD4",     KeyEvent.VK_NUMPAD4);
		mapKeyEvent("NUMPAD5",     KeyEvent.VK_NUMPAD5);
		mapKeyEvent("NUMPAD6",     KeyEvent.VK_NUMPAD6);
		mapKeyEvent("NUMPAD7",     KeyEvent.VK_NUMPAD7);
		mapKeyEvent("NUMPAD8",     KeyEvent.VK_NUMPAD8);
		mapKeyEvent("NUMPAD9",     KeyEvent.VK_NUMPAD9);
		mapKeyEvent("PAGE_DOWN",   KeyEvent.VK_PAGE_DOWN);
		mapKeyEvent("PAGE_UP",     KeyEvent.VK_PAGE_UP);
		mapKeyEvent("PLUS",        KeyEvent.VK_PLUS);
		mapKeyEvent("PRINTSCREEN", KeyEvent.VK_PRINTSCREEN);
		mapKeyEvent("RIGHT",       KeyEvent.VK_RIGHT);
		mapKeyEvent("SEPARATOR",   KeyEvent.VK_SEPARATOR);
		mapKeyEvent("SHIFT",       KeyEvent.VK_SHIFT);
		mapKeyEvent("SPACE",       KeyEvent.VK_SPACE);
		mapKeyEvent("SUBTRACT",    KeyEvent.VK_SUBTRACT);
		mapKeyEvent("UP",          KeyEvent.VK_UP);
	}

}
