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

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.MeshFiles.BlockWriter;
import com.jaamsim.MeshFiles.DataBlock;
import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.collada.ColParser;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.input.ActionListInput;
import com.jaamsim.input.Keyword;
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
import com.jaamsim.ui.MenuItem;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.MenuItemEntity;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.FileInput;
import com.sandwell.JavaSimulation3D.DisplayEntity;

public class ColladaModel extends DisplayModel implements MenuItemEntity {

	@Keyword(description = "The file containing the image to show, valid formats are: BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel ColladaFile { ..\\images\\CompanyIcon.png }")
	private final FileInput colladaFile;

	@Keyword(description = "A list of active actions and the entity output that drives them",
	         example = "Ship3DModel Actions { { ContentAction Contents } { BoomAngleAction BoomAngle } }")
	private final ActionListInput actions;

	private static HashMap<URI, MeshProtoKey> _cachedKeys = new HashMap<URI, MeshProtoKey>();

	private static final String[] validFileExtensions;
	private static final String[] validFileDescriptions;
	static {
		validFileExtensions = new String[5];
		validFileDescriptions = new String[5];

		validFileExtensions[0] = "ZIP";
		validFileExtensions[1] = "DAE";
		validFileExtensions[2] = "OBJ";
		validFileExtensions[3] = "JSM";
		validFileExtensions[4] = "JSB";

		validFileDescriptions[0] = "Zipped 3D Files (*.zip)";
		validFileDescriptions[1] = "COLLADA Files (*.dae)";
		validFileDescriptions[2] = "Wavefront Files (*.obj)";
		validFileDescriptions[3] = "JaamSim 3D Files (*.jsm)";
		validFileDescriptions[4] = "JaamSim 3D Binary Files (*.jsb)";
	}

	{
		colladaFile = new FileInput( "ColladaFile", "DisplayModel", null );
		colladaFile.setFileType("3D");
		colladaFile.setValidFileExtensions(validFileExtensions);
		colladaFile.setValidFileDescriptions(validFileDescriptions);
		this.addInput( colladaFile);

		actions = new ActionListInput("Actions", "DisplayModel", new ArrayList<Action.Binding>());
		this.addInput(actions);
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

	public URI getColladaFile() {
		return colladaFile.getValue();
	}

	/**
	 * Compares the specified file extension to the list of valid extensions.
	 *
	 * @param str - the file extension to be tested.
	 * @return - TRUE if the extension is valid.
	 */
	public static boolean isValidExtension(String str) {

		for (String ext : validFileExtensions) {
			if (str.equalsIgnoreCase(ext))
				return true;
		}
		return false;
	}

	/**
	 * Returns a file name extension filter for each of the supported file types.
	 *
	 * @return an array of file name extension filters.
	 */
	public static FileNameExtensionFilter[] getFileNameExtensionFilters() {
		return FileInput.getFileNameExtensionFilters("3D", validFileExtensions, validFileDescriptions);
	}

	public static MeshProtoKey getCachedMeshKey(URI shapeURI) {

		MeshProtoKey meshKey = _cachedKeys.get(shapeURI);

		if (meshKey == null) {
			// This has not been cached yet
			meshKey = RenderUtils.FileNameToMeshProtoKey(shapeURI);
			assert(meshKey != null);
			_cachedKeys.put(shapeURI, meshKey);
		}

		return _cachedKeys.get(shapeURI);
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;

		private DisplayEntity dispEnt;

		private Transform transCache;
		private Vec3d scaleCache;
		private URI colCache;
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

			URI filename = colladaFile.getValue();

			ArrayList<Action.Queue> aqList = new ArrayList<Action.Queue>();
			for (Action.Binding b : actions.getValue()) {
				Action.Queue aq = new Action.Queue();
				aq.name = b.actionName;
				aq.time = dispEnt.getOutputHandle(b.outputName).getValueAsDouble(simTime, 0);

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

	private void exportBinaryMesh(String outputName) {
		MeshProtoKey meshKey = getCachedMeshKey(colladaFile.getValue());

		try {
			ColParser.setKeepData(true);
			MeshData data = ColParser.parse(meshKey.getURL());
			DataBlock block = data.getDataAsBlock();
			File outFile = new File(outputName);
			FileOutputStream outStream = new FileOutputStream(outFile);
			BlockWriter.writeBlock(outStream, block);

			LogBox.formatRenderLog("Successfully exported: %s\n", outputName);
		} catch (Exception ex) {
			LogBox.formatRenderLog("Could not export model. Error: %s\n", ex.getMessage());
			LogBox.renderLogException(ex);
		}

	}

	@Override
	public void gatherMenuItems(ArrayList<MenuItem> list, int x, int y) {
		list.add(new MenuItem("Export 3D Binary File (*.jsb)") {

			@Override
			public void action() {

				// Create a file chooser
				File colFile = new File(colladaFile.getValue());
				final JFileChooser chooser = new JFileChooser(colFile);

				// Set the file extension filters
				chooser.setAcceptAllFileFilterUsed(true);
				FileNameExtensionFilter jsbFilter = new FileNameExtensionFilter("JaamSim 3D Binary Files (*.jsb)", "JSB");
				chooser.addChoosableFileFilter(jsbFilter);
				chooser.setFileFilter(jsbFilter);

				// Show the file chooser and wait for selection
				int returnVal = chooser.showDialog(null, "Export");

				// Create the selected graphics files
				if (returnVal == JFileChooser.APPROVE_OPTION) {
		            File file = chooser.getSelectedFile();
					String filePath = file.getPath();

					// Add the file extension ".jsb" if needed
					filePath = filePath.trim();
					if (filePath.indexOf(".") == -1)
						filePath = filePath.concat(".jsb");

					// Confirm overwrite if file already exists
					File temp = new File(filePath);
					if (temp.exists()) {

						int userOption = JOptionPane.showConfirmDialog( null,
								file.getName() + " already exists.\n" +
								"Do you wish to replace it?", "Confirm Save As",
								JOptionPane.YES_NO_OPTION,
								JOptionPane.WARNING_MESSAGE );

						if (userOption == JOptionPane.NO_OPTION) {
							return;
						}
					}

					// Export the JSB file
		            exportBinaryMesh(temp.getPath());
		        }
			}
		});
	}

}
