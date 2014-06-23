/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jaamsim.MeshFiles.BlockReader;
import com.jaamsim.MeshFiles.DataBlock;
import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.MeshReader;
import com.jaamsim.MeshFiles.ObjReader;
import com.jaamsim.collada.ColParser;
import com.jaamsim.ui.LogBox;

public class MeshDataCache {
	private static HashMap<MeshProtoKey, MeshData> dataMap = new HashMap<MeshProtoKey, MeshData>();
	private static Object mapLock = new Object();

	private static HashMap<MeshProtoKey, AtomicBoolean> loadingMap = new HashMap<MeshProtoKey, AtomicBoolean>();
	private static Object loadingLock = new Object();

	private static HashSet<MeshProtoKey> badMeshSet = new HashSet<MeshProtoKey>();
	private static Object badMeshLock = new Object();
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
		synchronized (mapLock) {
			MeshData data = dataMap.get(key);
			if (data != null) {
				return data;
			}
		}
		synchronized (badMeshLock) {
			if (badMeshSet.contains(key)) {
				return getBadMesh();
			}
		}

		AtomicBoolean loadingFlag = null;
		synchronized (loadingLock) {
			loadingFlag = loadingMap.get(key);
		}

		if (loadingFlag != null) {
			// Someone already triggered a delayed load for this mesh, let's just wait for that one...
			synchronized (loadingFlag) {
				while (!loadingFlag.get()) {
					try {
						loadingFlag.wait();
					} catch (InterruptedException ex) {}
				}
			}
			return dataMap.get(key);
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
				DataBlock block = BlockReader.readBlockFromURI(key.getURI());
				data = new MeshData(false, block, key.getURI().toURL());
			} else if (ext.toUpperCase().equals("OBJ")) {
				data = ObjReader.parse(key.getURI());
			} else {
				assert(false);
			}
		} catch (Exception ex) {
			LogBox.formatRenderLog("Could not load mesh: %s \n Error: %s\n", key.getURI().toString(), ex.getMessage());
			synchronized (badMeshLock) {
				badMeshSet.add(key);
				return getBadMesh();
			}
		}

		synchronized (mapLock) {
			dataMap.put(key, data);
		}
		return data;
	}

	public static boolean isMeshLoaded(MeshProtoKey key) {
		synchronized (mapLock) {
			return dataMap.containsKey(key);
		}
	}

	/**
	 * Load the mesh in a new thread, then notify on 'notifier'
	 * @param key
	 * @param notifier
	 */
	public static void loadMesh(final MeshProtoKey key, final AtomicBoolean notifier) {
		assert(notifier != null);

		synchronized (loadingLock) {
			loadingMap.put(key, notifier);
		}

		new Thread() {
			@Override
			public void run() {

				getMeshData(key); // Cause the lazy initializer to load the mesh (or return quickly if already loaded)

				notifier.set(true);

				synchronized(notifier) {
					notifier.notifyAll();
				}
			}
		}.start();
	}

	// Lazily load the bad mesh data
	public synchronized static MeshData getBadMesh() {
		if (badMesh == null) {
			badMesh = MeshReader.parse(BAD_MESH_KEY.getURI());
		}
		return badMesh;
	}
}
