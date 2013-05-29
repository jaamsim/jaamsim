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
package com.jaamsim.DisplayModels;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.ActionListInput;
import com.jaamsim.input.Output;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Action;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.MeshDataCache;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.MeshProxy;
import com.jaamsim.render.RenderProxy;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class ColladaModel extends DisplayModel {

	@Keyword(description = "The file containing the image to show, valid formats are: BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel ColladaFile { ..\\images\\CompanyIcon.png }")
	private final StringInput colladaFile;

	@Keyword(description = "A list of active actions and the entity output that drives them",
	         example = "Ship3DModel Actions { { ContentAction Contents } { BoomAngleAction BoomAngle } }")
	private final ActionListInput actions;

	private static HashMap<String, MeshProtoKey> _cachedKeys = new HashMap<String, MeshProtoKey>();

	private static ArrayList<String> validFileExtentions;

	{
		colladaFile = new StringInput( "ColladaFile", "DisplayModel", null );
		this.addInput( colladaFile, true);

		actions = new ActionListInput("Actions", "DisplayModel", new ArrayList<Action.Binding>());
		this.addInput(actions, true);
	}
	static {
		validFileExtentions = new ArrayList<String>();
		validFileExtentions.add("DAE");
		validFileExtentions.add("ZIP");
		validFileExtentions.add("JSM");
	}

	public ColladaModel() {}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof DisplayEntity;
	}

	@Override
	public void validate() {
		String ext = Util.getFileExtention(colladaFile.getValue());
		if(! validFileExtentions.contains(ext)){
			throw new InputErrorException("Invalid file format \"%s\"", colladaFile.getValue());
		}
	}

	public String getColladaFile() {
		return colladaFile.getValue();
	}

	public static MeshProtoKey getCachedMeshKey(String shapeString) {
		return _cachedKeys.get(shapeString);
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker dmTracker;

		private DisplayEntity dispEnt;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)observee;

			if (dispEnt != null) {
				observeeTracker = dispEnt.getGraphicsChangeTracker();
			}
			dmTracker = dm.getGraphicsChangeTracker();
		}

		private void updateCache(double simTime) {
			if (cachedProxies != null && observeeTracker != null && !observeeTracker.checkAndClear()
			    && !dmTracker.checkAndClear()) {
				// Nothing changed
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			// Gather some inputs
			Transform trans;
			Vec3d scale;
			long pickingID;

			if (dispEnt == null) {
				trans = Transform.ident;
				scale = Vec4d.ONES;
				pickingID = 0;
			} else {
				trans = dispEnt.getGlobalTrans(simTime);
				scale = dispEnt.getSize();
				scale.mul3(getModelScale());
				pickingID = dispEnt.getEntityNumber();
			}

			cachedProxies = new ArrayList<RenderProxy>();

			String filename = colladaFile.getValue();

			MeshProtoKey meshKey = _cachedKeys.get(filename);

			// We have not loaded this file before, cache the mesh proto key so
			// we don't dig through a zip file every render
			if (meshKey == null) {
				try {
					URL meshURL = new URL(Util.getAbsoluteFilePath(filename));

					String ext = filename.substring(filename.length() - 4,
							filename.length());

					if (ext.toUpperCase().equals(".ZIP")) {
						// This is a zip, use a zip stream to actually pull out
						// the .dae file
						ZipInputStream zipInputStream = new ZipInputStream(meshURL.openStream());

						// Loop through zipEntries
						for (ZipEntry zipEntry; (zipEntry = zipInputStream
								.getNextEntry()) != null;) {

							String entryName = zipEntry.getName();
							if (!Util.getFileExtention(entryName)
									.equalsIgnoreCase("DAE"))
								continue;

							// This zipEntry is a collada file, no need to look
							// any further
							meshURL = new URL("jar:" + meshURL + "!/"
									+ entryName);
							break;
						}
					}

					meshKey = new MeshProtoKey(meshURL);
					_cachedKeys.put(filename, meshKey);
				} catch (MalformedURLException e) {
					e.printStackTrace();
					assert (false);
				} catch (IOException e) {
					assert (false);
				}
			}

			AABB bounds = RenderManager.inst().getMeshBounds(meshKey, true);
			if (bounds == null) {
				// This mesh has not been loaded yet, try again next time
				cachedProxies = null; // Invalidate the cache
				return;
			}

			// Tweak the transform and scale to adjust for the bounds of the
			// loaded model
			Vec4d offset = new Vec4d(bounds.getCenter());
			Vec4d boundsRad = new Vec4d(bounds.getRadius());
			if (boundsRad.z == 0) {
				boundsRad.z = 1;
			}

			Vec4d fixedScale = new Vec4d(0.5 * scale.x
					/ boundsRad.x, 0.5 * scale.y / boundsRad.y, 0.5
					* scale.z / boundsRad.z, 1.0d);

			offset.x *= -1 * fixedScale.x;
			offset.y *= -1 * fixedScale.y;
			offset.z *= -1 * fixedScale.z;

			Transform fixedTrans = new Transform(trans);
			fixedTrans.merge(fixedTrans, new Transform(offset));

			ArrayList<Action.Queue> aqList = new ArrayList<Action.Queue>();
			for (Action.Binding b : actions.getValue()) {
				Action.Queue aq = new Action.Queue();
				aq.name = b.actionName;
				aq.time = dispEnt.getOutputValue(b.outputName, simTime, double.class);
				aqList.add(aq);
			}

			cachedProxies.add(new MeshProxy(meshKey, fixedTrans, fixedScale, aqList, getVisibilityInfo(),
					pickingID));
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (dispEnt == null || !dispEnt.getShow()) {
				return;
			}

			updateCache(simTime);

			out.addAll(cachedProxies);
		}
	}

	private MeshData getMeshData() {
		MeshProtoKey key = _cachedKeys.get(colladaFile.getValue());
		if (key == null) return null;

		return MeshDataCache.getMeshData(key);
	}

	@Output(name = "Vertices")
	public int getNumVerticesOutput(double simTime) {
		MeshData data = getMeshData();
		if (data == null) return 0;

		return data.getNumVertices();
	}

	@Output(name = "Triangles")
	public int getNumTrianglesOutput(double simTime) {
		MeshData data = getMeshData();
		if (data == null) return 0;

		return data.getNumTriangles();
	}

	@Output(name = "VertexShareRatio")
	public double getVertexShareRatioOutput(double simTime) {
		MeshData data = getMeshData();
		if (data == null) return 0;

		double numTriangles = data.getNumTriangles();
		double numVertices = data.getNumVertices();
		return numTriangles / (numVertices/3);
	}

}
