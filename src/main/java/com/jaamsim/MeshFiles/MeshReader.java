/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2015 JaamSim Software Inc.
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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import com.jaamsim.basicsim.Log;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Action;
import com.jaamsim.render.Armature;
import com.jaamsim.render.RenderException;
import com.jaamsim.ui.LogBox;
import com.jaamsim.xml.XmlNode;
import com.jaamsim.xml.XmlParser;


// TODO: Delete this whole class, this is now deprecated
public class MeshReader {

	public static MeshData parse(URI asset) throws RenderException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);

		try {
			MeshReader reader = new MeshReader(asset.toURL());
			reader.processContent();


			return reader.getMeshData();

		} catch (Exception e) {
			Log.logException(e);
			throw new RenderException(e.getMessage());
		}
	}

	private static void parseAssert(boolean b) {
		if (!b) {
			throw new RenderException("Failed JSM parsing assert");
		}
	}

	private XmlParser _parser;
	private final URL contentURL;
	private MeshData finalData;

	private XmlNode _meshObjectNode;

	public MeshReader(URL asset) {
		contentURL = asset;
	}

	static final List<String> DOUBLE_ARRAY_TAGS;
	static final List<String> INT_ARRAY_TAGS;
	static final List<String> STRING_ARRAY_TAGS;
	static final List<String> BOOLEAN_ARRAY_TAGS;

	static {
		DOUBLE_ARRAY_TAGS = new ArrayList<>();
		DOUBLE_ARRAY_TAGS.add("Positions");
		DOUBLE_ARRAY_TAGS.add("Normals");
		DOUBLE_ARRAY_TAGS.add("TexCoords");
		DOUBLE_ARRAY_TAGS.add("BoneIndices");
		DOUBLE_ARRAY_TAGS.add("BoneWeights");
		DOUBLE_ARRAY_TAGS.add("Color");
		DOUBLE_ARRAY_TAGS.add("Matrix");
		DOUBLE_ARRAY_TAGS.add("T");
		DOUBLE_ARRAY_TAGS.add("W");
		DOUBLE_ARRAY_TAGS.add("X");
		DOUBLE_ARRAY_TAGS.add("Y");
		DOUBLE_ARRAY_TAGS.add("Z");

		INT_ARRAY_TAGS = new ArrayList<>();
		INT_ARRAY_TAGS.add("Faces");

		STRING_ARRAY_TAGS = new ArrayList<>();
		STRING_ARRAY_TAGS.add("BoneNames");

		BOOLEAN_ARRAY_TAGS = new ArrayList<>();

	}

	private void processContent() {

		_parser = new XmlParser(contentURL);

		_parser.setDoubleArrayTags(DOUBLE_ARRAY_TAGS);
		_parser.setIntArrayTags(INT_ARRAY_TAGS);
		_parser.setBooleanArrayTags(BOOLEAN_ARRAY_TAGS);
		_parser.setStringArrayTags(STRING_ARRAY_TAGS);

		_parser.parse();

		finalData = new MeshData(false);

		_meshObjectNode = _parser.getRootNode().findChildTag("MeshObject", false);
		parseAssert(_meshObjectNode != null);

		parseGeometries();
		parseMaterials();
		parseArmatures();

		parseInstances();

		MeshData.TreeNode emptyTree = new MeshData.TreeNode();
		emptyTree.trans = new MeshData.StaticTrans(new Mat4d());
		finalData.setTree(emptyTree);

		finalData.finalizeData();
	}

	private MeshData getMeshData() {
		return finalData;
	}

	private void parseGeometries() {
		XmlNode geosNode = _meshObjectNode.findChildTag("Geometries", false);
		for (XmlNode child : geosNode.children()) {
			if (!child.getTag().equals("Geometry")) {
				continue;
			}
			parseGeometry(child);
		}
	}

	private void parseMaterials() {
		XmlNode matsNode = _meshObjectNode.findChildTag("Materials", false);
		for (XmlNode child : matsNode.children()) {
			if (!child.getTag().equals("Material")) {
				continue;
			}
			parseMaterial(child);
		}
	}

	private void parseArmatures() {
		XmlNode armsNode = _meshObjectNode.findChildTag("Armatures", false);
		if (armsNode == null) {
			return; // Armatures are optional
		}
		for (XmlNode child : armsNode.children()) {
			if (!child.getTag().equals("Armature")) {
				continue;
			}
			parseArmature(child);
		}
	}

	private void parseInstances() {
		XmlNode instNode = _meshObjectNode.findChildTag("MeshInstances", false);
		for (XmlNode child : instNode.children()) {
			if (!child.getTag().equals("MeshInstance")) {
				continue;
			}
			parseInstance(child);
		}
	}

	private void parseGeometry(XmlNode geoNode) {
		int numVerts = Integer.parseInt(geoNode.getAttrib("vertices"));
		// Start by parsing vertices
		XmlNode vertsNode = geoNode.findChildTag("Positions", false);
		parseAssert(vertsNode != null);
		parseAssert(vertsNode.getAttrib("dims").equals("3"));

		double[] positions = (double[])vertsNode.getContent();
		parseAssert(positions.length == numVerts * 3);

		XmlNode normNode = geoNode.findChildTag("Normals", false);
		parseAssert(normNode != null);
		parseAssert(normNode.getAttrib("dims").equals("3"));

		double[] normals = (double[])normNode.getContent();
		parseAssert(normals.length == numVerts * 3);

		XmlNode texCoordNode = geoNode.findChildTag("TexCoords", false);
		double[] texCoords = null;
		boolean hasTex = false;
		if (texCoordNode != null) {
			parseAssert(texCoordNode.getAttrib("dims").equals("2"));

			texCoords = (double[])texCoordNode.getContent();
			parseAssert(texCoords.length == numVerts * 2);
			hasTex = true;
		}

		XmlNode boneIndicesNode = geoNode.findChildTag("BoneIndices", false);
		XmlNode boneWeightsNode = geoNode.findChildTag("BoneWeights", false);
		double[] boneIndices = null;
		double[] boneWeights = null;
		boolean hasBoneInfo = false;
		int numBoneWeights = 0;
		if (boneIndicesNode != null) {
			numBoneWeights = Integer.parseInt(boneIndicesNode.getAttrib("entriesPerVert"));
			parseAssert(numBoneWeights == Integer.parseInt(boneWeightsNode.getAttrib("entriesPerVert"))); // Make sure these are the same
			parseAssert(numBoneWeights <= 4); // TODO handle more than this by discarding extras and renormalizing

			boneIndices = (double[])boneIndicesNode.getContent();
			boneWeights = (double[])boneWeightsNode.getContent();
			parseAssert(boneIndices.length == numBoneWeights * numVerts);
			parseAssert(boneWeights.length == numBoneWeights * numVerts);

			hasBoneInfo = true;
		}


		// Finally get the indices
		XmlNode faceNode = geoNode.findChildTag("Faces", false);
		parseAssert(faceNode != null);
		int numTriangles = Integer.parseInt(faceNode.getAttrib("count"));
		parseAssert(faceNode.getAttrib("type").equals("Triangles"));

		int[] indices = (int[])faceNode.getContent();
		parseAssert(numTriangles*3 == indices.length);

		ArrayList<Vertex> verts = new ArrayList<>(numVerts);

		for (int i = 0; i < numVerts; ++i) {
			Vec3d posVec = new Vec3d(positions[i*3+0], positions[i*3+1], positions[i*3+2]);
			Vec3d normVec = new Vec3d(normals[i*3+0], normals[i*3+1], normals[i*3+2]);
			Vec2d texCoordVec = null;
			Vec4d boneIndicesVec = null;
			Vec4d boneWeightsVec = null;
			if (hasTex) {
				texCoordVec = new Vec2d(texCoords[i*2+0], texCoords[i*2+1]);
			}

			if (hasBoneInfo) {
				boneIndicesVec = new Vec4d(0, 0, 0, 0);
				boneWeightsVec = new Vec4d(0, 0, 0, 0);
				if (numBoneWeights >= 1) {
					boneIndicesVec.x = boneIndices[i*numBoneWeights + 0];
					boneWeightsVec.x = boneWeights[i*numBoneWeights + 0];
				}
				if (numBoneWeights >= 2) {
					boneIndicesVec.y = boneIndices[i*numBoneWeights + 1];
					boneWeightsVec.y = boneWeights[i*numBoneWeights + 1];
				}
				if (numBoneWeights >= 3) {
					boneIndicesVec.z = boneIndices[i*numBoneWeights + 2];
					boneWeightsVec.z = boneWeights[i*numBoneWeights + 2];
				}
				if (numBoneWeights >= 4) {
					boneIndicesVec.w = boneIndices[i*numBoneWeights + 3];
					boneWeightsVec.w = boneWeights[i*numBoneWeights + 3];
				}
			}

			verts.add(new Vertex(posVec, normVec, texCoordVec, boneIndicesVec, boneWeightsVec));
		}

		finalData.addSubMesh(verts, indices);
	}

	private void parseMaterial(XmlNode matNode) {
		XmlNode diffuseNode = matNode.findChildTag("Diffuse", false);
		parseAssert(diffuseNode != null);
		XmlNode textureNode = diffuseNode.findChildTag("Texture", false);
		if (textureNode != null) {
			parseAssert(textureNode.getAttrib("coordIndex").equals("0"));
			String file = (String)textureNode.getContent();
			try {
				URI texURI = new URL(contentURL, file).toURI();
				finalData.addMaterial(texURI, file, null, null, null, 1, MeshData.NO_TRANS, null);
				return;
			} catch (MalformedURLException ex) {
				parseAssert(false);
			} catch (URISyntaxException e) {
				parseAssert(false);
			}
		}
		// Not a texture so it must be a color
		XmlNode colorNode = diffuseNode.findChildTag("Color", false);
		parseAssert(colorNode != null);
		double[] c = (double[])colorNode.getContent();
		parseAssert(c.length == 4);
		Color4d color = new Color4d(c[0], c[1], c[2], c[3]);
		finalData.addMaterial(null, null, color, null, null, 1, MeshData.NO_TRANS, null);
	}

	private void parseBone(XmlNode boneNode, Armature arm, String parentName) {
		String name = boneNode.getAttrib("name");
		double length = Double.parseDouble(boneNode.getAttrib("length"));
		XmlNode matNode = boneNode.findChildTag("Matrix", false);
		Mat4d mat = nodeToMat4d(matNode);
		arm.addBone(name, mat, parentName, length);

		for (XmlNode child : boneNode.children()) {
			if (!child.getTag().equals("Bone")) {
				continue;
			}
			parseBone(child, arm, name);
		}
	}

	private void parseKeys(XmlNode node, Action.Channel chan, boolean isTrans) {
		XmlNode tNode = node.findChildTag("T", false);
		XmlNode xNode = node.findChildTag("X", false);
		XmlNode yNode = node.findChildTag("Y", false);
		XmlNode zNode = node.findChildTag("Z", false);
		parseAssert(tNode != null);
		parseAssert(xNode != null);
		parseAssert(yNode != null);
		parseAssert(zNode != null);
		double[] ts = (double[])tNode.getContent();
		double[] xs = (double[])xNode.getContent();
		double[] ys = (double[])yNode.getContent();
		double[] zs = (double[])zNode.getContent();

		parseAssert(ts.length == xs.length);
		parseAssert(ts.length == ys.length);
		parseAssert(ts.length == zs.length);

		double [] ws = null;
		if (!isTrans) {
			XmlNode wNode = node.findChildTag("W", false);
			parseAssert(wNode != null);
			ws = (double[])wNode.getContent();
			parseAssert(ts.length == ws.length);
		}

		for (int i = 0; i < ts.length; ++i) {
			if (!isTrans) {
				Action.RotKey rk = new Action.RotKey();
				rk.time = ts[i];
				rk.rot = new Quaternion(xs[i], ys[i], zs[i], ws[i]);
				chan.rotKeys.add(rk);
			} else {
				Action.TransKey tk = new Action.TransKey();
				tk.time = ts[i];
				tk.trans = new Vec3d(xs[i], ys[i], zs[i]);
				chan.transKeys.add(tk);
			}
		}
	}

	private void populateChannel(XmlNode node, Action.Channel chan) {

		XmlNode rotNode = node.findChildTag("Rotation", false);
		if (rotNode != null) {
			chan.rotKeys = new ArrayList<>();
			parseKeys(rotNode, chan, false);
		}

		XmlNode transNode = node.findChildTag("Location", false);
		if (transNode != null) {
			chan.transKeys = new ArrayList<>();
			parseKeys(transNode, chan, true);
		}

	}

	private Action.Channel parseGroup(XmlNode groupNode) {
		Action.Channel chan = new Action.Channel();
		chan.name = groupNode.getAttrib("name");
		parseAssert(chan.name != null);

		populateChannel(groupNode, chan);

		return chan;
	}

	private Action parseAction(XmlNode actionNode) {
		Action act = new Action();
		act.name = actionNode.getAttrib("name");
		act.duration = Double.parseDouble(actionNode.getAttrib("length"));
		parseAssert(act.name != null);

		for (XmlNode child : actionNode.children()) {
			if (!child.getTag().equals("Group")) {
				continue;
			}
			Action.Channel channel = parseGroup(child);
			act.channels.add(channel);
		}
		return act;
	}

	private void parseArmature(XmlNode armNode) {
		Armature arm = new Armature();

		for (XmlNode child : armNode.children()) {
			if (!child.getTag().equals("Bone")) {
				continue;
			}
			parseBone(child, arm, null);
		}

		// Now parse the actions for this armature
		for (XmlNode child : armNode.children()) {
			if (!child.getTag().equals("Action")) {
				continue;
			}
			Action act = parseAction(child);
			arm.addAction(act);
		}

	}

	private Action parseInstAction(XmlNode actNode) {
		Action act = new Action();
		act.name = actNode.getAttrib("name");
		act.duration = Double.parseDouble(actNode.getAttrib("length"));
		parseAssert(act.name != null);

		// Submesh Instance actions can only have a single channel
		act.channels = new ArrayList<>(1);
		Action.Channel chan = new Action.Channel();
		populateChannel(actNode, chan);
		act.channels.add(chan);
		return act;
	}

	private void parseInstance(XmlNode instNode) {
		int geoIndex = Integer.parseInt(instNode.getAttrib("geoIndex"));
		int matIndex = Integer.parseInt(instNode.getAttrib("matIndex"));

		XmlNode matrixNode = instNode.findChildTag("Matrix", false);
		parseAssert(matrixNode != null);

		Mat4d mat = nodeToMat4d(matrixNode);

		int armIndex = -1;
		String armIndString = instNode.getAttrib("armIndex");
		if (armIndString != null) {
			armIndex = Integer.parseInt(armIndString);
		}

		XmlNode boneNamesNode = instNode.findChildTag("BoneNames", false);
		String[] boneNames = null;
		if (boneNamesNode != null) {
			boneNames = (String[])boneNamesNode.getContent();
		}

		// Iff we have an armature, we also have bone names
		parseAssert((armIndex==-1) == (boneNames==null));

		XmlNode actionsNode = instNode.findChildTag("Actions", false);
		if (actionsNode != null) {
			for (XmlNode child : actionsNode.children()) {
				if (!child.getTag().equals("Action")) {
					continue;
				}
				Action act = parseInstAction(child);
				parseAssert(act.channels.size() == 1); // Sub instance key frames can only have one channel
			}
		}

		finalData.addStaticMeshInstance(geoIndex, matIndex, mat);
	}

	private Mat4d nodeToMat4d(XmlNode node) {
		double[] matDoubles = (double[])node.getContent();
		parseAssert(matDoubles.length == 16);

		Mat4d mat = new Mat4d(matDoubles);
		mat.transpose4();
		return mat;
	}
}
