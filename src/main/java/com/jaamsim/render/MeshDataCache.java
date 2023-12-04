/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2022 JaamSim Software Inc.
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

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

import com.jaamsim.MeshFiles.BlockReader;
import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.MeshReader;
import com.jaamsim.MeshFiles.ObjReader;
import com.jaamsim.collada.ColParser;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;

public class MeshDataCache {
	private static final HashMap<MeshProtoKey, MeshData> dataMap = new HashMap<>();
	private static final HashSet<MeshProtoKey> badMeshSet = new HashSet<>();
	private static MeshData badMesh = null;

	public static final MeshProtoKey BAD_MESH_KEY;

	static {
		try {
			BAD_MESH_KEY = new MeshProtoKey(TexCache.class.getResource("/resources/shapes/bad-mesh.jsm").toURI());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	// Fetch, or lazily initialize the mesh data
	public static MeshData getMeshData(MeshProtoKey key) {
		synchronized (dataMap) {
			MeshData data = dataMap.get(key);
			if (data != null) {
				return data;
			}
		}
		synchronized (badMeshSet) {
			if (badMeshSet.contains(key)) {
				return getBadMesh();
			}
		}

		// Release the lock long enough to load the model
		String fileString = key.getURI().toString();
		String ext = fileString.substring(fileString.length() - 3, fileString.length());

		MeshData data = null;
		try {
			if (ext.toUpperCase().equals("DAE")) {
				data = ColParser.parse(key.getURI());
			} else if (ext.toUpperCase().equals("JSM")) {
				data = MeshReader.parse(key.getURI());
			} else if (ext.toUpperCase().equals("JSB")) {
				data = BlockReader.parse(key.getURI());
			} else if (ext.toUpperCase().equals("OBJ")) {
				data = ObjReader.parse(key.getURI());
			} else {
				assert(false);
			}
		} catch (Exception ex) {
			String pre = "Unable to load 3D data file";
			String msg = ex.getMessage();
			String source = key.getURI().toString();
			String post = "Check the file path and ensure that the file name does not include "
					+ "invalid characters such as '#'";
			try {
				source = Paths.get(key.getURI()).toString();  // decode %20 as blank character
			}
			catch (Exception e) {}
			GUIFrame.invokeErrorDialog("3D Loader Error", source, 0, pre, msg, post);
			LogBox.formatRenderLog("%s\n%s\n%s\n%s", pre, msg, source, post);

			synchronized (badMeshSet) {
				badMeshSet.add(key);
				return getBadMesh();
			}
		}

		synchronized (dataMap) {
			dataMap.put(key, data);
		}
		return data;
	}

	public static boolean isMeshLoaded(MeshProtoKey key) {
		synchronized (dataMap) {
			return dataMap.containsKey(key);
		}
	}


	// Lazily load the bad mesh data
	public synchronized static MeshData getBadMesh() {
		if (badMesh == null) {
			badMesh = MeshReader.parse(BAD_MESH_KEY.getURI());
		}
		return badMesh;
	}
}
