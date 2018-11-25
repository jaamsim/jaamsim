/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.Graphics;

public interface EditableText extends Editable {

	/**
	 * Assigns the present text.
	 * @param str - present text
	 */
	public void setText(String str);

	/**
	 * Returns the present text.
	 * @return present text
	 */
	public String getText();

	/**
	 * Sets the position in the text at which new characters will be inserted.
	 * @param pos - position in the text string
	 * @param shift - true is the shift key was pressed during the change in position
	 */
	public void setInsertPosition(int pos, boolean shift);

	/**
	 * Highlights the word the contains the present insert position.
	 */
	public void selectPresentWord();

	/**
	 * Returns the position in the text at which new characters will be inserted.
	 * 0 = beginning of the text
	 * @return insert position
	 */
	public int getInsertPosition();

	/**
	 * Returns the number of characters that have been highlighted relative to the insert position.
	 * The value is negative if the characters appear before the insert position.
	 * @return number of selected characters
	 */
	public int getNumberSelected();

}
