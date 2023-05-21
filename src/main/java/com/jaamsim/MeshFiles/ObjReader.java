/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015-2023 JaamSim Software Inc.
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
package com.jaamsim.MeshFiles;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.RenderException;
import com.jaamsim.ui.LogBox;

public class ObjReader {
	public static MeshData parse(URI asset) throws RenderException {

		try {
			ObjReader reader = new ObjReader(asset.toURL());
			reader.processContent();

			return reader.getMeshData();

		} catch (Exception e) {
			LogBox.renderLogException(e);
			throw new RenderException(e.getMessage());
		}
	}

	private void parseAssert(boolean b) {
		if (!b) {
			throw new RenderException(String.format("Failed OBJ parsing assert at line: %d", lineNum));
		}
	}

	private final URL contentURL;
	private MeshData data;

	private static class FaceVert {
		public int v;
		public int t;
		public int n;
	}

	private static class Material {
		public Color4d ambient;
		public Color4d spec;
		public Color4d diffuse;
		public URI diffuseTex;
		public String relDiffuseTex;
		public double shininess = 1.0;
		public double alpha = 1.0;
	}

	private Material parsingMat;

	private final HashMap<String, Material> materialMap = new HashMap<>();

	private final ArrayList<Vec3d> vertices = new ArrayList<>();
	private final ArrayList<Vec2d> texCoords = new ArrayList<>();
	private final ArrayList<Vec3d> normals = new ArrayList<>();
	private final ArrayList<FaceVert> faces = new ArrayList<>();

	private String activeMat = null;

	private int numLoadedMeshes = 0;

	private int lineNum = 0;

	private final HashMap<String, Integer> loadedMaterials = new HashMap<>();

	public ObjReader(URL asset) {
		contentURL = asset;
	}

	private void processContent() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(contentURL.openStream()));

			data = new MeshData(false);
			while(true) {
				String line = br.readLine();
				lineNum++;
				if (line == null) break;

				if (line.length() == 0 || line.charAt(0) == '#') {
					continue;
				}
				while (line.charAt(line.length()-1) == '\\')
				{
					String nextLine = br.readLine();
					lineNum++;
					line = line.substring(0, line.length() - 1) + " " + nextLine;
				}
				parseLine(line);
			}

		} catch (Exception ex) {
			String msg = String.format("An error occurred while parsing line %s:%n%s",
					lineNum, ex.getLocalizedMessage());
			throw new RenderException(msg, ex);
		}

		finishCurrentMesh();

		MeshData.TreeNode root = new MeshData.TreeNode();
		root.trans = new MeshData.StaticTrans(new Mat4d());

		data.setTree(root);

		data.finalizeData();
	}

	// Load the material in the return data (if necessary) and return the appropriate index
	private int getMaterialIndex(String matName) {
		Integer loaded = loadedMaterials.get(matName);
		if (loaded != null) {
			return loaded;
		}
		// Otherwise load it
		int newIndex = loadedMaterials.size();
		Material mat = materialMap.get(matName);
		parseAssert(mat != null);

		int transType = mat.alpha == 1.0 ? MeshData.NO_TRANS : MeshData.A_ONE_TRANS;
		data.addMaterial(mat.diffuseTex, mat.relDiffuseTex, mat.diffuse, mat.ambient, mat.spec, mat.shininess, transType, new Color4d(1, 1, 1, mat.alpha));
		loadedMaterials.put(matName, newIndex);
		return newIndex;
	}

	private void finishCurrentMesh() {
		if (faces.size() == 0) {
			return;
		}

		VertexMap map = new VertexMap();

		// Now build up the geometry
		int[] vertIndices = new int[faces.size()];

		for (int i = 0; i < faces.size(); ++i) {
			FaceVert fv = faces.get(i);
			Vec3d pos = vertices.get(fv.v - 1);
			Vec2d texCoord = null;
			if (fv.t != -1)
				texCoord = texCoords.get(fv.t - 1);
			Vec3d normal = null;
			if (fv.n != -1) {
				normal = normals.get(fv.n - 1);
			} else {
				// This face does not have a normal, we'd better generate one from the vertices
				int faceInd = i / 3;
				Vec3d p0 = vertices.get(faces.get(faceInd*3 + 0).v-1);
				Vec3d p1 = vertices.get(faces.get(faceInd*3 + 1).v-1);
				Vec3d p2 = vertices.get(faces.get(faceInd*3 + 2).v-1);
				Vec3d d0 = new Vec3d();
				d0.sub3(p1, p0);
				Vec3d d1 = new Vec3d();
				d1.sub3(p2, p0);
				normal = new Vec3d();
				normal.cross3(d0, d1);
				normal.normalize3();
			}

			vertIndices[i] = map.getVertIndex(pos, normal, texCoord);
		}

		int matIndex = getMaterialIndex(activeMat);

		data.addSubMesh(map.getVertList(), vertIndices);
		data.addStaticMeshInstance(numLoadedMeshes++,  matIndex, new Mat4d());

		faces.clear();
	}

	private void parseLine(String line) {
		String[] tokens = line.trim().split("\\s+");
		if (tokens.length == 0) {
			return;
		}
		if (tokens[0].equals("v")) {
			parseVertex(tokens);
		} else if (tokens[0].equals("vt")) {
			parseTexCoord(tokens);
		} else if (tokens[0].equals("vn")) {
			parseNormal(tokens);
		} else if (tokens[0].equals("f")) {
			parseFaces(tokens);
		} else if (tokens[0].equals("mtllib")) {
			parseMaterial(tokens);
		} else if (tokens[0].equals("usemtl")) {
			parseAssert(tokens.length == 2);
			finishCurrentMesh();
			activeMat = tokens[1];
		} else if (tokens[0].equals("g")) {
			// Just ignore groups
		} else if (tokens[0].equals("o")) {
			// Just ignore objects
		} else {
			System.err.printf("Ignoring line: %s\n", line);
		}
	}

	private MeshData getMeshData() {
		return data;
	}

	private void parseVertex(String[] tokens) {
		parseAssert(tokens.length >= 4 && tokens.length <= 5);
		Vec4d vert = new Vec4d();
		try {
			vert.x = Double.parseDouble(tokens[1]);
			vert.y = Double.parseDouble(tokens[2]);
			vert.z = Double.parseDouble(tokens[3]);
			vert.w = 1;
			if (tokens.length > 4) {
				vert.w = Double.parseDouble(tokens[4]);
			}

			vertices.add(vert);
		} catch (NumberFormatException ex) {
			parseAssert(false);
			return;
		}
	}

	private void parseTexCoord(String[] tokens) {
		parseAssert(tokens.length >= 3 && tokens.length <= 4);
		Vec2d texCoord = new Vec2d();
		try {
			texCoord.x = Double.parseDouble(tokens[1]);
			texCoord.y = Double.parseDouble(tokens[2]);

			texCoords.add(texCoord);
		} catch (NumberFormatException ex) {
			parseAssert(false);
			return;
		}
	}

	private void parseNormal(String[] tokens) {
		parseAssert(tokens.length == 4);
		Vec3d normal = new Vec3d();
		try {
			normal.x = Double.parseDouble(tokens[1]);
			normal.y = Double.parseDouble(tokens[2]);
			normal.z = Double.parseDouble(tokens[3]);

			normal.normalize3();
			normals.add(normal);
		} catch (NumberFormatException ex) {
			parseAssert(false);
			return;
		}
	}

	private void parseFaces(String[] tokens) {
		parseAssert(tokens.length >= 4);

		FaceVert[] fvs = new FaceVert[tokens.length - 1];
		for (int i = 0; i < tokens.length - 1; ++i) {

			boolean hasNormal = false;
			boolean hasTex = false;

			fvs[i] = new FaceVert();
			String[] indices = tokens[i+1].split("/");
			parseAssert(indices.length > 0);
			fvs[i].v = Integer.parseInt(indices[0]);

			if (indices.length < 2 || indices[1].isEmpty()) {
				fvs[i].t = -1;
			} else {
				fvs[i].t = Integer.parseInt(indices[1]);
				hasTex = true;
			}

			if (indices.length < 3 || indices[2].isEmpty()) {
				fvs[i].n = -1;
			} else {
				fvs[i].n = Integer.parseInt(indices[2]);
				hasNormal = true;
			}

			// Check for relative indexing
			if (fvs[i].v < 0) {
				fvs[i].v = vertices.size() + 1 + fvs[i].v;
			}
			if (fvs[i].t < 0 && hasTex) {
				fvs[i].t = texCoords.size() + 1 + fvs[i].t;
			}
			if (fvs[i].n < 0 && hasNormal) {
				fvs[i].n = normals.size() + 1 + fvs[i].n;
			}
		}

		for (int i = 2; i < tokens.length-1; ++i) {
			faces.add(fvs[0]);
			faces.add(fvs[i-1]);
			faces.add(fvs[i]);
		}
	}

	private static final Pattern whitespace = Pattern.compile("\\s+");
	private void parseMaterial(String[] tokens) {
		parseAssert(tokens.length == 2);
		String mtlFile = tokens[1];
		try {
			URL mtlURL = new URL(contentURL, mtlFile);

			BufferedReader br = new BufferedReader(new InputStreamReader(mtlURL.openStream()));

			while (true) {
				String line = br.readLine();
				if (line == null) break;

				if (line.isEmpty() || line.charAt(0) == '#') continue;

				String[] mtlTokens = whitespace.split(line.trim());
				parseMTLLine(mtlTokens);
			}

		} catch (MalformedURLException ex) {
			throw new RenderException(String.format("Could not open mtl file: %s", mtlFile));
		} catch (IOException ex) {
			throw new RenderException(String.format("Could not open mtl file: %s", mtlFile));
		}
	}

	private void parseMTLLine(String[] tokens) throws MalformedURLException {
		if (tokens.length == 0) return;

		if (tokens[0].equals("newmtl")) {
			parseAssert(tokens.length == 2);
			parsingMat = new Material();
			materialMap.put(tokens[1], parsingMat);
		} else if (tokens[0].equals("Kd")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(parsingMat != null);
			parsingMat.diffuse = parseColor(tokens);
		} else if (tokens[0].equals("Ka")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(parsingMat != null);
			parsingMat.ambient = parseColor(tokens);
		} else if (tokens[0].equals("Ks")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(parsingMat != null);
			parsingMat.spec = parseColor(tokens);
		} else if (tokens[0].equals("Ns")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(tokens.length == 2);
			parseAssert(parsingMat != null);
			parsingMat.shininess = Double.parseDouble(tokens[1]);
		} else if (tokens[0].equals("d")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(tokens.length == 2);
			parseAssert(parsingMat != null);
			parsingMat.alpha = Double.parseDouble(tokens[1]);
		} else if (tokens[0].equals("map_Kd")) {
			if (tokens.length == 1) return; // Ignore empty tags
			parseAssert(tokens.length == 2);
			parseAssert(parsingMat != null);
			try {
				parsingMat.diffuseTex = new URL(contentURL, tokens[1]).toURI();
			} catch (URISyntaxException e) {
				parseAssert(false);
			}
			parsingMat.relDiffuseTex = tokens[1];
		}
	}

	private Color4d parseColor(String[] tokens) {
		parseAssert(tokens.length == 4);
		double r = Double.parseDouble(tokens[1]);
		double g = Double.parseDouble(tokens[2]);
		double b = Double.parseDouble(tokens[3]);
		return new Color4d(r, g, b, 1);
	}
}
