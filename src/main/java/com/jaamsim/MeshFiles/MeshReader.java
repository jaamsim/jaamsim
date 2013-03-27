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
import com.jaamsim.math.Vec4d;
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
		DOUBLE_ARRAY_TAGS.add("Vertices");
		DOUBLE_ARRAY_TAGS.add("Normals");
		DOUBLE_ARRAY_TAGS.add("TexCoords");
		DOUBLE_ARRAY_TAGS.add("Color");
		DOUBLE_ARRAY_TAGS.add("Matrix");

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
		XmlNode vertsNode = geoNode.findChildTag("Vertices", false);
		assert(vertsNode != null);
		assert(vertsNode.getAttrib("dims").equals("3"));

		double[] verts = (double[])vertsNode.getContent();
		assert(verts.length == numVerts * 3);

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

		// TODO: actually honor the indices
		Vec4d[] vertVects = new Vec4d[numVerts];
		Vec4d[] normVects = new Vec4d[numVerts];
		Vec4d[] texCoordVects = hasTex ? new Vec4d[numVerts] : null;

		for (int i = 0; i < numVerts; ++i) {
			vertVects[i] = new Vec4d(verts[i*3+0], verts[i*3+1], verts[i*3+2], 1);
			normVects[i] = new Vec4d(normals[i*3+0], normals[i*3+1], normals[i*3+2], 1);
			if (hasTex) {
				texCoordVects[i] = new Vec4d(texCoords[i*2+0], texCoords[i*2+1], 0, 1);
			}
		}

		finalData.addSubMesh(vertVects, normVects, texCoordVects);
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

	private void parseInstance(XmlNode instNode) {
		int geoIndex = Integer.parseInt(instNode.getAttrib("geoIndex"));
		int matIndex = Integer.parseInt(instNode.getAttrib("matIndex"));

		XmlNode matrixNode = instNode.findChildTag("Matrix", false);
		assert(matrixNode != null);
		double[] matDoubles = (double[])matrixNode.getContent();
		assert(matDoubles.length == 16);

		Mat4d mat = new Mat4d(matDoubles);
		mat.transpose4();

		finalData.addSubMeshInstance(geoIndex, matIndex, mat);
	}
}
