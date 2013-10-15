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

import java.util.ArrayList;
import java.util.HashMap;

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
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.VisibilityInfo;
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

		MeshProtoKey meshKey = _cachedKeys.get(shapeString);

		if (meshKey == null) {
			// This has not been cached yet
			meshKey = RenderUtils.FileNameToMeshProtoKey(shapeString);
			assert(meshKey != null);
			_cachedKeys.put(shapeString, meshKey);
		}

		return _cachedKeys.get(shapeString);
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;

		private DisplayEntity dispEnt;

		private Transform transCache;
		private Vec3d scaleCache;
		private String colCache;
		private ArrayList<Action.Queue> actionsCache;
		private VisibilityInfo viCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)observee;
		}

		private void updateCache(double simTime) {

			// Gather some inputs
			Transform trans;
			Vec3d scale;
			long pickingID;

			if (dispEnt == null) {
				trans = Transform.ident;
				scale = DisplayModel.ONES;
				pickingID = 0;
			} else {
				trans = dispEnt.getGlobalTrans(simTime);
				scale = dispEnt.getSize();
				scale.mul3(getModelScale());
				pickingID = dispEnt.getEntityNumber();
			}

			String filename = colladaFile.getValue();

			ArrayList<Action.Queue> aqList = new ArrayList<Action.Queue>();
			for (Action.Binding b : actions.getValue()) {
				if( b.outputHandle == null )
					b.outputHandle = dispEnt.getOutputHandle(b.outputName);
				Action.Queue aq = new Action.Queue();
				aq.name = b.actionName;
				aq.time = b.outputHandle.getValueAsDouble(simTime, 0);

				aqList.add(aq);
			}

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || !compare(colCache, filename);
			dirty = dirty || !compare(actionsCache, aqList);
			dirty = dirty || !compare(viCache, vi);

			transCache = trans;
			scaleCache = scale;
			colCache = filename;
			actionsCache = aqList;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				registerCacheHit("ColladaModel");
				return;
			}

			registerCacheMiss("ColladaModel");

			cachedProxies = new ArrayList<RenderProxy>();

			MeshProtoKey meshKey = getCachedMeshKey(filename);

			AABB bounds = RenderManager.inst().getMeshBounds(meshKey, true);
			if (bounds == null || bounds.isEmpty()) {
				// This mesh has not been loaded yet, try again next time
				cachedProxies = null; // Invalidate the cache
				return;
			}

			// Tweak the transform and scale to adjust for the bounds of the
			// loaded model
			Vec3d boundsRad = new Vec3d(bounds.radius);
			if (boundsRad.z == 0) {
				boundsRad.z = 1;
			}

			Vec4d fixedScale = new Vec4d(0.5 * scale.x
					/ boundsRad.x, 0.5 * scale.y / boundsRad.y, 0.5
					* scale.z / boundsRad.z, 1.0d);

			Vec3d offset = new Vec3d(bounds.center);
			offset.scale3(-1.0d);
			offset.mul3(fixedScale);

			Transform fixedTrans = new Transform(trans);
			fixedTrans.merge(fixedTrans, new Transform(offset));

			cachedProxies.add(new MeshProxy(meshKey, fixedTrans, fixedScale, aqList, vi,
					pickingID));
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (dispEnt == null || !dispEnt.getShow()) {
				return;
			}

			updateCache(simTime);

			if (cachedProxies != null)
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

	@Output(name = "NumSubInstances")
	public int getNumSubInstancesOutput(double simTime) {
		MeshData data = getMeshData();
		if (data == null) return 0;

		return data.getNumSubInstances();

	}

	@Output(name = "NumSubMeshes")
	public int getNumSubMeshesOutput(double simTime) {
		MeshData data = getMeshData();
		if (data == null) return 0;

		return data.getNumSubMeshes();

	}

	@Output (name = "Actions")
	public String getActionsOutput(double simTime) {
		MeshProtoKey meshKey = getCachedMeshKey(colladaFile.getValue());
		ArrayList<Action.Description> actionDescs = RenderManager.inst().getMeshActions(meshKey, true);

		StringBuilder ret = new StringBuilder();
		for (Action.Description desc : actionDescs) {
			ret.append(desc.name + " ");
		}
		return ret.toString();
	}
}
