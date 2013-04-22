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
package com.jaamsim.MeshFiles;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.SAXParserFactory;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Armature;
import com.jaamsim.render.RenderException;
import com.jaamsim.xml.XmlNode;
import com.jaamsim.xml.XmlParser;

public class MeshReader {

	public static MeshData parse(URL asset) throws RenderException {

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);

		try {
			MeshReader reader = new MeshReader(asset);
			reader.processContent();


			return reader.getMeshData();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RenderException(e.getMessage());
		}
	}

	private XmlParser _parser;
	private URL contentURL;
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
		DOUBLE_ARRAY_TAGS = new ArrayList<String>();
		DOUBLE_ARRAY_TAGS.add("Positions");
		DOUBLE_ARRAY_TAGS.add("Normals");
		DOUBLE_ARRAY_TAGS.add("TexCoords");
		DOUBLE_ARRAY_TAGS.add("Color");
		DOUBLE_ARRAY_TAGS.add("Matrix");
		DOUBLE_ARRAY_TAGS.add("T");
		DOUBLE_ARRAY_TAGS.add("W");
		DOUBLE_ARRAY_TAGS.add("X");
		DOUBLE_ARRAY_TAGS.add("Y");
		DOUBLE_ARRAY_TAGS.add("Z");

		INT_ARRAY_TAGS = new ArrayList<String>();
		INT_ARRAY_TAGS.add("Faces");

		STRING_ARRAY_TAGS = new ArrayList<String>();

		BOOLEAN_ARRAY_TAGS = new ArrayList<String>();

	}

	private void processContent() {

		_parser = new XmlParser(contentURL);

		_parser.setDoubleArrayTags(DOUBLE_ARRAY_TAGS);
		_parser.setIntArrayTags(INT_ARRAY_TAGS);
		_parser.setBooleanArrayTags(BOOLEAN_ARRAY_TAGS);
		_parser.setStringArrayTags(STRING_ARRAY_TAGS);

		_parser.parse();

		finalData = new MeshData();

		_meshObjectNode = _parser.getRootNode().findChildTag("MeshObject", false);
		assert(_meshObjectNode != null);

		parseGeometries();
		parseMaterials();
		parseArmatures();

		parseInstances();

		finalData.generateHull();
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
		assert(vertsNode != null);
		assert(vertsNode.getAttrib("dims").equals("3"));

		double[] positions = (double[])vertsNode.getContent();
		assert(positions.length == numVerts * 3);

		XmlNode normNode = geoNode.findChildTag("Normals", false);
		assert(normNode != null);
		assert(normNode.getAttrib("dims").equals("3"));

		double[] normals = (double[])normNode.getContent();
		assert(normals.length == numVerts * 3);

		XmlNode texCoordNode = geoNode.findChildTag("TexCoords", false);
		double[] texCoords = null;
		boolean hasTex = false;
		if (texCoordNode != null) {
			assert(texCoordNode.getAttrib("dims").equals("2"));

			texCoords = (double[])texCoordNode.getContent();
			assert(texCoords.length == numVerts * 2);
			hasTex = true;
		}

		// Finally get the indices
		XmlNode faceNode = geoNode.findChildTag("Faces", false);
		assert(faceNode != null);
		int numTriangles = Integer.parseInt(faceNode.getAttrib("count"));
		assert(faceNode.getAttrib("type").equals("Triangles"));

		int[] indices = (int[])faceNode.getContent();
		assert(numTriangles*3 == indices.length);

		ArrayList<Vertex> verts = new ArrayList<Vertex>(numVerts);

		for (int i = 0; i < numVerts; ++i) {
			Vec4d posVec = new Vec4d(positions[i*3+0], positions[i*3+1], positions[i*3+2], 1);
			Vec4d normVec = new Vec4d(normals[i*3+0], normals[i*3+1], normals[i*3+2], 1);
			Vec4d texCoordVec = null;
			if (hasTex) {
				texCoordVec = new Vec4d(texCoords[i*2+0], texCoords[i*2+1], 0, 1);
			}
			verts.add(new Vertex(posVec, normVec, texCoordVec));
		}

		finalData.addSubMesh(verts, indices);
	}

	private void parseMaterial(XmlNode matNode) {
		XmlNode diffuseNode = matNode.findChildTag("Diffuse", false);
		assert(diffuseNode != null);
		XmlNode textureNode = diffuseNode.findChildTag("Texture", false);
		if (textureNode != null) {
			assert(textureNode.getAttrib("coordIndex").equals("0"));
			String file = (String)textureNode.getContent();
			try {
				URL texURL = new URL(contentURL, file);
				finalData.addMaterial(texURL, null, MeshData.NO_TRANS, null);
				return;
			} catch (MalformedURLException ex) {
				assert(false);
			}
		}
		// Not a texture so it must be a color
		XmlNode colorNode = diffuseNode.findChildTag("Color", false);
		assert(colorNode != null);
		double[] c = (double[])colorNode.getContent();
		assert(c.length == 4);
		Color4d color = new Color4d(c[0], c[1], c[2], c[3]);
		finalData.addMaterial(null, color, MeshData.NO_TRANS, null);
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

	private void parseKeys(XmlNode node, Armature.Channel chan, boolean isTrans) {
		XmlNode tNode = node.findChildTag("T", false);
		XmlNode xNode = node.findChildTag("X", false);
		XmlNode yNode = node.findChildTag("Y", false);
		XmlNode zNode = node.findChildTag("Z", false);
		assert(tNode != null);
		assert(xNode != null);
		assert(yNode != null);
		assert(zNode != null);
		double[] ts = (double[])tNode.getContent();
		double[] xs = (double[])xNode.getContent();
		double[] ys = (double[])yNode.getContent();
		double[] zs = (double[])zNode.getContent();

		assert(ts.length == xs.length);
		assert(ts.length == ys.length);
		assert(ts.length == zs.length);

		double [] ws = null;
		if (!isTrans) {
			XmlNode wNode = node.findChildTag("W", false);
			assert(wNode != null);
			ws = (double[])wNode.getContent();
			assert(ts.length == ws.length);
		}

		for (int i = 0; i < ts.length; ++i) {
			if (!isTrans) {
				Armature.RotKey rk = new Armature.RotKey();
				rk.time = ts[i];
				rk.rot = new Quaternion(xs[i], ys[i], zs[i], ws[i]);
				chan.rotKeys.add(rk);
			} else {
				Armature.TransKey tk = new Armature.TransKey();
				tk.time = ts[i];
				tk.trans = new Vec3d(xs[i], ys[i], zs[i]);
				chan.transKeys.add(tk);
			}
		}
	}

	private Armature.Channel parseGroup(XmlNode groupNode) {
		Armature.Channel chan = new Armature.Channel();
		chan.name = groupNode.getAttrib("name");
		assert(chan.name != null);

		XmlNode rotNode = groupNode.findChildTag("Rotation", false);
		if (rotNode != null) {
			chan.rotKeys = new ArrayList<Armature.RotKey>();
			parseKeys(rotNode, chan, false);
		}

		XmlNode transNode = groupNode.findChildTag("Location", false);
		if (transNode != null) {
			chan.transKeys = new ArrayList<Armature.TransKey>();
			parseKeys(transNode, chan, true);
		}

		return chan;
	}

	private Armature.Action parseAction(XmlNode actionNode) {
		Armature.Action act = new Armature.Action();
		act.name = actionNode.getAttrib("name");
		act.duration = Double.parseDouble(actionNode.getAttrib("length"));
		assert(act.name != null);

		for (XmlNode child : actionNode.children()) {
			if (!child.getTag().equals("Group")) {
				continue;
			}
			Armature.Channel channel = parseGroup(child);
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

		finalData.addArmature(arm);

		// Now parse the actions for this armature
		for (XmlNode child : armNode.children()) {
			if (!child.getTag().equals("Action")) {
				continue;
			}
			Armature.Action act = parseAction(child);
			arm.addAction(act);
		}

	}

	private void parseInstance(XmlNode instNode) {
		int geoIndex = Integer.parseInt(instNode.getAttrib("geoIndex"));
		int matIndex = Integer.parseInt(instNode.getAttrib("matIndex"));

		XmlNode matrixNode = instNode.findChildTag("Matrix", false);
		assert(matrixNode != null);

		Mat4d mat = nodeToMat4d(matrixNode);

		int armIndex = -1;
		String armIndString = instNode.getAttrib("armIndex");
		if (armIndString != null) {
			armIndex = Integer.parseInt(armIndString);
		}

		finalData.addSubMeshInstance(geoIndex, matIndex, armIndex, mat);
	}

	private Mat4d nodeToMat4d(XmlNode node) {
		double[] matDoubles = (double[])node.getContent();
		assert(matDoubles.length == 16);

		Mat4d mat = new Mat4d(matDoubles);
		mat.transpose4();
		return mat;
	}
}
