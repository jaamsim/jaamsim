/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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

import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.IntegerListInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.Vec3d;
import com.jogamp.newt.event.KeyEvent;

/**
 * OverlayEntity is the superclass for DisplayEntities that have 2D overlay graphics instead of 3D graphics.  Overlay graphics
 * are those that appear relative to the View window and whose position and size are specified in pixels.
 * @author Harry King
 *
 */
public abstract class OverlayEntity extends DisplayEntity {

	@Keyword(description = "The position of the overlay, from the upper left corner of the window to the upper left corner " +
	                "of the overlay. Value is in pixels",
	         exampleList = {"20 20"})
	private final IntegerListInput screenPosition;

	@Keyword(description = "If this overlay should be aligned from the right edge of the window (instead of the left)",
	         exampleList = {"TRUE"})
	private final BooleanInput alignRight;

	@Keyword(description = "If this overlay should be aligned from the bottom edge of the window (instead of the top)",
	         exampleList = {"TRUE"})
	private final BooleanInput alignBottom;

	{
		IntegerVector defPos = new IntegerVector(2);
		defPos.add(10);
		defPos.add(10);
		screenPosition = new IntegerListInput("ScreenPosition", GRAPHICS, defPos);
		screenPosition.setValidCount(2);
		screenPosition.setValidRange(0, 2500);
		this.addInput(screenPosition);

		alignRight = new BooleanInput("AlignRight", GRAPHICS, false);
		this.addInput(alignRight);

		alignBottom = new BooleanInput("AlignBottom", GRAPHICS, false);
		this.addInput(alignBottom);
	}

	public boolean getAlignRight() {
		return alignRight.getValue();
	}

	public boolean getAlignBottom() {
		return alignBottom.getValue();
	}

	public IntegerVector getScreenPosition() {
		return screenPosition.getValue();
	}

	@Override
	public void dragged(Vec3d newPos) {}

	@Override
	public void handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		if (!isMovable())
			return;

		IntegerVector pos = screenPosition.getValue();
		int x = pos.get(0);
		int y = pos.get(1);

		switch (keyCode) {

			case KeyEvent.VK_LEFT:
				x += getAlignRight() ? 1 : -1;
				break;

			case KeyEvent.VK_RIGHT:
				x += getAlignRight() ? -1 : 1;
				break;

			case KeyEvent.VK_UP:
				y += getAlignBottom() ? 1 : -1;
				break;

			case KeyEvent.VK_DOWN:
				y += getAlignBottom() ? -1 : 1;
				break;

			default:
				return;
		}

		x = Math.max(0, x);
		y = Math.max(0, y);
		KeywordIndex kw = InputAgent.formatIntegers(screenPosition.getKeyword(), x, y);
		InputAgent.storeAndExecute(new KeywordCommand(this, kw));
	}

}
