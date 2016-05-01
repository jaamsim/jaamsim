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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;

import com.jaamsim.input.Input;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.ui.LogBox;


public class MeshWriter {

	private OutputStreamWriter out;
	private int indentLength = 0;
	boolean isGood = false;
	URI baseDirURI;

	public MeshWriter(String filename) {
		try {
			OutputStream os = new FileOutputStream(filename);
			out = new OutputStreamWriter(os, Charset.forName("UTF-8"));

			baseDirURI = new File(filename).toURI().resolve(".");
		} catch (Exception ex) {
			LogBox.renderLogException(ex);
			return;
		}
		isGood = true;
	}

	private void indent() throws IOException {
		for (int i = 0; i < indentLength; ++i) {
			out.write(Input.SEPARATOR);
		}
	}

	private void startTag(String tag) throws IOException {
		indent(); indentLength++;
		out.write(tag);
		out.write("\n");
	}

	private void endTag(String tag) throws IOException {
		indentLength--; indent();
		out.write(tag);
		out.write("\n");
	}

	public boolean write(MeshData data) {

		try {
			if (!isGood) {
				return false;
			}

			startTag("<MeshObject version='0.1'>");

			startTag("<Geometries>");

			int meshNumber = 0;
			for (MeshData.SubMeshData subMesh : data.getSubMeshData()) {
				writeSubMesh(subMesh, meshNumber++);
			}
			endTag("</Geometries>");

			startTag("<Materials>");

			int matNumber = 0;
			for (MeshData.Material mat : data.getMaterials()) {
				writeMaterial(mat, matNumber++);
			}
			endTag("</Materials>");

			startTag("<MeshInstances>");
			for (MeshData.StaticMeshInstance inst : data.getStaticMeshInstances()) {
				writeMeshInstance(inst);
			}
			endTag("</MeshInstances>");

			endTag("</MeshObject>");

			out.close();
		} catch (IOException ex) {
			LogBox.renderLogException(ex);
			return false;
		}

		return true;
	}

	private void writeSubMesh(MeshData.SubMeshData subMesh, int meshNumber) throws IOException {

		startTag(String.format("<Geometry vertices='%d' ID='Mesh%d'>", subMesh.verts.size(), meshNumber));

		startTag("<Positions dims='3'>");
		indent();
		for (Vec3d v : subMesh.verts) {
			out.write(String.format("%f %f %f ", v.x, v.y, v.z));
		}
		out.write("\n");
		endTag("</Positions>");

		startTag("<Normals dims='3'>");
		indent();
		for (Vec3d n : subMesh.normals) {
			out.write(String.format("%f %f %f ", n.x, n.y, n.z));
		}
		out.write("\n");
		endTag("</Normals>");

		if (subMesh.texCoords != null && subMesh.texCoords.size() != 0) {
			// This mesh has tex coordinates
			startTag("<TexCoords index='0' dims='2'>");
			indent();
			for (Vec2d t : subMesh.texCoords) {
				out.write(String.format("%f %f ", t.x, t.y));
			}
			out.write("\n");
			endTag("</TexCoords>");
		}
		// Output the faces list
		startTag(String.format("<Faces type='Triangles' count='%d'>", subMesh.indices.length/3));
		indent();
		for (int i : subMesh.indices) {
			out.write(String.format("%d ", i));
		}
		out.write("\n");
		endTag("</Faces>");
		endTag("</Geometry>");
	}

	private void writeMaterial(MeshData.Material mat, int meshNumber) throws IOException {
		startTag(String.format("<Material ID='Mat%d'>", meshNumber));
		startTag("<Diffuse>");
		if (mat.colorTex == null) {
			// Flat color
			startTag("<Color>");
			indent();
			out.write(String.format("%f %f %f %f\n", mat.diffuseColor.r,
			                                         mat.diffuseColor.g,
			                                         mat.diffuseColor.b,
			                                         mat.diffuseColor.a));
			endTag("</Color>");
		} else {
			startTag("<Texture coordIndex='0'>");
			indent();
			URI tex = baseDirURI.relativize(mat.colorTex);
			out.write(tex.toString());
			out.write("\n");
			endTag("</Texture>");
		}
		endTag("</Diffuse>");
		endTag("</Material>");
	}

	private void writeMeshInstance(MeshData.StaticMeshInstance inst) throws IOException {
		startTag(String.format("<MeshInstance geoIndex='%d' matIndex='%d'>", inst.subMeshIndex, inst.materialIndex));
		startTag("<Matrix>");
		double[] cmData = inst.transform.toCMDataArray();
		indent();
		for (double d : cmData) {
			out.write(String.format("%f ", d));
		}
		out.write("\n");
		endTag("</Matrix>");
		endTag("</MeshInstance>");
	}
}
