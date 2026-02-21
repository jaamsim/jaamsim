/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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

import com.jaamsim.DisplayModels.IconModel;
import com.jaamsim.DisplayModels.ImageModel;

/**
 * Displays a two-dimensional picture
 * @author Harry King
 *
 */
public class Image extends AbstractShape {

	{
		displayModelListInput.clearValidClasses();
		displayModelListInput.addValidClass(ImageModel.class);
		displayModelListInput.addInvalidClass(IconModel.class);
	}

	public Image() {}

}
