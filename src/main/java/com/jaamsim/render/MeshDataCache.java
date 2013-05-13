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

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.MeshReader;
import com.jaamsim.collada.ColParser;

public class MeshDataCache {
	private static HashMap<MeshProtoKey, MeshData> dataMap = new HashMap<MeshProtoKey, MeshData>();
	private static Object mapLock = new Object();

	private static HashMap<MeshProtoKey, AtomicBoolean> loadingMap = new HashMap<MeshProtoKey, AtomicBoolean>();
	private static Object loadingLock = new Object();

	// Fetch, or lazily initialize the mesh data
	public static MeshData getMeshData(MeshProtoKey key) {
		synchronized (mapLock) {
			MeshData data = dataMap.get(key);
			if (data != null) {
				return data;
			}
		}

		AtomicBoolean loadingFlag = null;
		synchronized (loadingLock) {
			loadingFlag = loadingMap.get(key);
		}

		if (loadingFlag != null) {
			// Someone already triggered a delayed load for this mesh, let's just wait for that one...
			while (!loadingFlag.get()) {
				synchronized (loadingFlag) {
					try {
							loadingFlag.wait();
					} catch (InterruptedException ex) {}
				}
			}
			return dataMap.get(key);
		}

		// Release the lock long enough to load the model
		String fileString = key.getURL().toString();
		String ext = fileString.substring(fileString.length() - 3, fileString.length());

		MeshData data = null;
		if (ext.toUpperCase().equals("DAE")) {
			data = ColParser.parse(key.getURL());
		} else if (ext.toUpperCase().equals("JSM")) {
			data = MeshReader.parse(key.getURL());
		} else {
			assert(false);
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
		}.run();
	}
}
