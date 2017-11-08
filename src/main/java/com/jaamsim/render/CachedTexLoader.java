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

package com.jaamsim.render;

import java.net.URI;

public class CachedTexLoader implements TexLoader {

	private final URI texURI;
	private final boolean isCompressed;
	private final boolean isTransparent;

	public CachedTexLoader(URI uri,boolean isTransparent, boolean isCompressed) {
		this.texURI = uri;
		this.isCompressed = isCompressed;
		this.isTransparent = isTransparent;
	}

	@Override
	public boolean hasTransparent() {
		return isTransparent;
	}

	@Override
	public int getTexID(Renderer r) {
		return r.getTexCache().getTexID(r.getGL(), texURI, isTransparent, isCompressed, false);
	}

}
