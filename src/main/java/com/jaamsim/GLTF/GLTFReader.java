/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2024 JaamSim Software Inc.
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
package com.jaamsim.GLTF;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import com.jaamsim.JSON.JSONError;
import com.jaamsim.JSON.JSONParser;
import com.jaamsim.JSON.JSONValue;
import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.VertexMap;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.RenderException;

public class GLTFReader {

	private static String getStringChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		JSONValue child = parentMap.get(childName);
		if (child == null || !child.isString()) {
			if (optional)
				return null;

			String msg = String.format("Missing string attribute: %s", childName);
			throw new RenderException(msg);
		}
		return child.stringVal;
	}

	private static Double getNumberChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		JSONValue child = parentMap.get(childName);
		if (child == null || !child.isNumber()) {
			if (optional)
				return null;

			String msg = String.format("Missing number attribute: %s", childName);
			throw new RenderException(msg);
		}
		return child.numVal;
	}

	// This is similar to getNumberChild but checks that the value is a whole integer and casts to Integer
	private static Integer getIntChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		Double val = getNumberChild(parentMap, childName, optional);
		if (val == null) {
			if (optional) {
				return null;
			}
			else {
				String msg = String.format("Missing integer attribute: %s", childName);
				throw new RenderException(msg);
			}
		}

		if (Math.floor(val) != val) {
			String msg = String.format("Property (%s): (%f) is not an integer", childName, val);
			throw new RenderException(msg);
		}
		return (int) Math.round(val);
	}

	private static ArrayList<JSONValue> getListChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		JSONValue child = parentMap.get(childName);
		if (child == null || !child.isList()) {
			if (optional)
				return null;
			String msg = String.format("Missing list attribute: %s", childName);
			throw new RenderException(msg);
		}
		return child.listVal;
	}

	private static double[] getNumberListChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		JSONValue child = parentMap.get(childName);
		if (child == null || !child.isList()) {
			if (optional)
				return null;

			String msg = String.format("Missing number list attribute: %s", childName);
			throw new RenderException(msg);
		}
		ArrayList<JSONValue> listVal = child.listVal;

		double[] ret = new double[listVal.size()];

		for (int i = 0; i < listVal.size(); ++i) {
			JSONValue v = listVal.get(i);
			if (!v.isNumber()) {
				throw new RenderException("Child of number list is not a number");
			}
			ret[i] = v.numVal;
		}
		return ret;
	}
	private static int[] getIntListChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {
		double[] doubles = getNumberListChild(parentMap, childName, optional);
		if (optional && doubles == null) return null;

		int[] ret = new int[doubles.length];
		for (int i = 0; i < doubles.length; ++i) {
			double num = doubles[i];
			if (Math.floor(num) != num) {
				String msg = String.format("Property (%s) contains (%f) which is not an integer", childName, num);
				throw new RenderException(msg);
			}
			ret[i] = (int) Math.round(num);
		}
		return ret;
	}

	// Walk through chained JSON maps based on a arbitrary list of keys and return the value found
	// returns null on any error
	private static JSONValue walkMap(HashMap<String, JSONValue> map, String... keys) {

		HashMap<String, JSONValue> currMap = map;
		for (int i = 0; i < keys.length; ++i) {
			String key = keys[i];
			JSONValue val = currMap.get(key);
			if (val == null) {
				return null;
			}
			if (i == keys.length-1) {
				// Last key
				return val;
			}
			if (!val.isMap()) {
				return null;
			}
			currMap = val.mapVal;
		}

		return null;
	}
	private static Integer walkMapForInt(HashMap<String, JSONValue> map, String... keys) {
		JSONValue val = walkMap(map, keys);
		if (val != null && val.isNumber()) {
			return (int)Math.round(val.numVal);
		}
		return null;
	}

	private static String walkMapForString(HashMap<String, JSONValue> map, String... keys) {
		JSONValue val = walkMap(map, keys);
		if (val != null && val.isString()) {
			return val.stringVal;
		}
		return null;
	}

	private static HashMap<String, JSONValue> getMapChild(HashMap<String, JSONValue> parentMap, String childName, boolean optional) {

		JSONValue child = parentMap.get(childName);
		if (child == null || !child.isMap()) {
			if (optional)
				return null;

			String msg = String.format("Missing map attribute: %s", childName);
			throw new RenderException(msg);
		}
		return child.mapVal;
	}

	private enum ComponentType {
		int8(1),
		uint8(1),
		int16(2),
		uint16(2),
		uint32(4),
		float32(4);

		final int size;

		ComponentType(int size) {
			this.size = size;
		}

		// Map
		static ComponentType getFromNum(int gltfTypeNum) {
			switch(gltfTypeNum) {
			case 5120:
				return ComponentType.int8;
			case 5121:
				return ComponentType.uint8;
			case 5122:
				return ComponentType.int16;
			case 5123:
				return ComponentType.uint16;
			case 5125:
				return ComponentType.uint32;
			case 5126:
				return ComponentType.float32;
			default:
				throw new RenderException(String.format("Unknown gltf value type: %d", gltfTypeNum));
			}
		}
	}

	private enum VectorType {
		SCALAR(1),
		VEC2(2),
		VEC3(3),
		VEC4(4),
		MAT2(4),
		MAT3(9),
		MAT4(16);

		final int numComps;

		VectorType(int components) {
			numComps = components;
		}

		private static VectorType getFromType(String vecType) {
			switch(vecType) {
			case "SCALAR":
				return VectorType.SCALAR;
			case "VEC2":
				return VectorType.VEC2;
			case "VEC3":
				return VectorType.VEC3;
			case "VEC4":
				return VectorType.VEC4;
			case "MAT2":
				return VectorType.MAT2;
			case "MAT3":
				return VectorType.MAT3;
			case "MAT4":
				return VectorType.MAT4;
			default:
				throw new RenderException(String.format("Unknown gltf accessor type: %s", vecType));

			}
		}

	}

	private static int readIntFromBuffer(ByteBuffer buff, int pos, ComponentType compType) {
		int val;
		switch(compType) {
		case int8:
			return buff.get(pos);
		case uint8:
			val = buff.get(pos);
			// Fixup Java's lack of unsigned integer types
			if (val < 0) {
				val += 256;
			}
			return val;
		case int16:
			return buff.getShort(pos);
		case uint16:
			val = buff.getShort(pos);
			// Fixup Java's lack of unsigned integer types
			if (val < 0) {
				val += (1 << 16);
			}
			return val;
		case uint32:
			val = buff.getInt(pos);
			if (val < 0) {
				long realVal = val + (1<<32);
				// We technically could represent this in a long but
				// this means there's a scalar value > 2 billion so we're going
				// to error out for now until we find a model where this is valid
				throw new RenderException(String.format("Exceptionally large integer value, probably an error: %d", realVal));
			}
			return val;
		case float32:
			float floatVal = buff.getFloat(pos);
			val = Math.round(floatVal);
			return val;
		default:
			throw new RenderException(String.format("Unknown component type: %s", compType));
		}


	}

	private final MeshData outputData = new MeshData(false);
	private final URI contextURI;
	private final ByteBuffer defaultBuffer;

	enum Interp {
		STEP,
		LINEAR,
		CUBICSPLINE;
	}
	private static class NodeAnimation {
		public float[] rotInput;
		public Quaternion[] rotValues;
		public Interp rotInterp;

		public float[] transInput;
		public Vec3d[] transValues;
		public Interp transInterp;

		public float[] scaleInput;
		public Vec3d[] scaleValues;
		public Interp scaleInterp;

		// Re-use Vec3d interpolation between translation and scale
		private static Vec3d interpVec3d(float time, float[] input, Vec3d[] output, Interp interp) {
			if (input == null) return null;

			if (time <= input[0]) {
				if (interp == Interp.CUBICSPLINE)
					return output[1];
				else
					return output[0];
			}
			if (time >=input[input.length-1]) {
				if (interp == Interp.CUBICSPLINE)
					return output[output.length-2];
				else
					return output[output.length-1];
			}
			Vec3d ret;
			Vec3d temp;
			for (int i = 0; i < input.length-1; ++i) {
				boolean inSeg = time >= input[i] && time < input[i+1];
				if (!inSeg) continue;

				float segDur = (input[i+1] - input[i]);
				float t = (time - input[i]) / segDur;
				switch(interp) {
				case STEP:
					return output[i];
				case LINEAR:
					ret = new Vec3d(output[i]);
					ret.scale3(t);
					temp = new Vec3d(output[i+1]);
					temp.scale3(1.0 - t);
					ret.add3(temp);
					return ret;
				case CUBICSPLINE:
					Vec3d vk = output[i*3 + 1];
					Vec3d vk1 = output[(i+1)*3 + 1];
					Vec3d bk = output[i*3 + 2];
					Vec3d ak1 = output[(i+1)*3];
					float t2 = t*t;
					float t3 = t*t2;
					float aTerm = 2*t3 - 3*t2 + 1;
					float bTerm = (t3 - 2*t2 + t)*segDur;
					float cTerm = -2*t3 + 3*t2;
					float dTerm = (t3 - t2)*segDur;
					ret = new Vec3d(vk);
					ret.scale3(aTerm);

					temp = new Vec3d(bk);
					temp.scale3(bTerm);
					ret.add3(temp);

					temp = new Vec3d(vk1);
					temp.scale3(cTerm);
					ret.add3(temp);

					temp = new Vec3d(ak1);
					temp.scale3(dTerm);
					ret.add3(temp);

					return ret;

				default:
					throw new RenderException("Uknown interpolation type");
				}
			}
			throw new RenderException("Internal error");
		}
		public Vec3d getTrans(float time) {
			return interpVec3d(time, transInput, transValues, transInterp);
		}
		public Vec3d getScale(float time) {
			return interpVec3d(time, scaleInput, scaleValues, scaleInterp);
		}
		public Quaternion getRot(float time) {
			if (rotInput == null) return null;

			if (time <= rotInput[0]) {
				if (rotInterp == Interp.CUBICSPLINE)
					return rotValues[1];
				else
					return rotValues[0];
			}
			if (time >=rotInput[rotInput.length-1]) {
				if (rotInterp == Interp.CUBICSPLINE)
					return rotValues[rotValues.length-2];
				else
					return rotValues[rotValues.length-1];
			}
			Quaternion ret;
			Quaternion temp;
			for (int i = 0; i < rotInput.length-1; ++i) {
				boolean inSeg = time >= rotInput[i] && time < rotInput[i+1];
				if (!inSeg) continue;

				float segDur = (rotInput[i+1] - rotInput[i]);
				float t = (time - rotInput[i]) / segDur;
				switch(rotInterp) {
				case STEP:
					return rotValues[i];
				case LINEAR:

					ret = new Quaternion();
					rotValues[i].slerp(rotValues[i+1], 1.0-t, ret);
					return ret;
				case CUBICSPLINE:
					Quaternion vk = rotValues[i*3 + 1];
					Quaternion vk1 = rotValues[(i+1)*3 + 1];
					Quaternion bk = rotValues[i*3 + 2];
					Quaternion ak1 = rotValues[(i+1)*3];

					float t2 = t*t;
					float t3 = t*t2;
					float aTerm = 2*t3 - 3*t2 + 1;
					float bTerm = (t3 - 2*t2 + t)*segDur;
					float cTerm = -2*t3 + 3*t2;
					float dTerm = (t3 - t2)*segDur;
					ret = new Quaternion(vk);
					ret.scale(aTerm);

					temp = new Quaternion(bk);
					temp.scale(bTerm);
					ret.add(temp);

					temp = new Quaternion(vk1);
					temp.scale(cTerm);
					ret.add(temp);

					temp = new Quaternion(ak1);
					temp.scale(dTerm);
					ret.add(temp);

					ret.normalize();
					return ret;

				default:
					throw new RenderException("Uknown interpolation type");
				}
			}
			throw new RenderException("Internal error");
		}
	}

	private static class SceneNode {
		public Quaternion rot;
		public Vec3d trans;
		public Vec3d scale;

		public boolean animated = false;
		public HashMap<Integer, NodeAnimation> animMap = new HashMap<>();

		public Mat4d mat;

		public int[] children;

		public Integer meshIdx = null;

		public boolean isValid() {
			if (mat == null)
				return true;

			// matrix is not null, then the others must be null
			return rot == null && trans == null && scale == null;
		}

		// fill the remaining elements
		public void fill() {
			if (mat != null) return;

			if (rot == null)
				rot = new Quaternion();
			if (trans == null)
				trans = new Vec3d(0.0, 0.0, 0.0);
			if (scale == null)
				scale = new Vec3d(1.0, 1.0, 1.0);
		}

		public Mat4d getStaticMatrix() {
			if (mat != null)
				return mat;

			fill();

			Mat4d res = new Mat4d();
			res.setRot3(rot);
			res.scaleCols3(scale);
			res.setTranslate3(trans);
			return res;
		}

		public Mat4d getAnimatedMatrix(int animIdx, float time) {
			if (!animated) {
				return getStaticMatrix();
			}

			NodeAnimation anim = animMap.get(animIdx);
			if (anim == null) {
				return getStaticMatrix();
			}

			Vec3d trans = anim.getTrans(time);
			if (trans == null)
				trans = new Vec3d(0, 0, 0);
			Vec3d scale = anim.getScale(time);
			if (scale == null)
				scale = new Vec3d(1, 1, 1);
			Quaternion rot = anim.getRot(time);
			if (rot == null)
				rot = new Quaternion();

			Mat4d res = new Mat4d();
			res.setRot3(rot);
			res.scaleCols3(scale);
			res.setTranslate3(trans);
			return res;

		}
	}

	private static class MeshPrimitive {
		final int posAcc;
		final Integer normAcc;
		final Integer texCoord0Acc;
		final Integer indicesAcc;
		final int material;

		MeshPrimitive(HashMap<String, JSONValue> prim) {
			Integer mode = getIntChild(prim, "mode", true);
			if (mode != null) {
				if (mode.intValue() != 4) {
					throw new RenderException(String.format("Currently unsupported GLTF geometry mode: %d", mode));
				}
			}

			HashMap<String, JSONValue> attribMap = getMapChild(prim, "attributes", false);

			posAcc = getIntChild(attribMap, "POSITION", true);
			normAcc = getIntChild(attribMap, "NORMAL", true);
			texCoord0Acc = getIntChild(attribMap, "TEXCOORD_0", true);

			indicesAcc = getIntChild(prim, "indices", true);
			material = getIntChild(prim, "material", true);
		}
	}

	private static class Mesh {
		final ArrayList<MeshPrimitive> primitives = new ArrayList<>();
	}

	private static class Buffer {
		final ByteBuffer contents;

		Buffer(ByteBuffer contents) {
			this.contents = contents;
		}
	}

	private static class BufferView {
		final int buffIdx;
		final int length;
		final int offset;
		final int stride;

		BufferView(HashMap<String, JSONValue> buffViewMap) {
			buffIdx = getIntChild(buffViewMap, "buffer", false);
			length = getIntChild(buffViewMap, "byteLength", false);

			// optional params
			Integer byteOffset = getIntChild(buffViewMap, "byteOffset", true);
			if (byteOffset == null)
				byteOffset = 0;

			offset = byteOffset;

			Integer byteStride = getIntChild(buffViewMap, "byteStride", true);
			if (byteStride == null)
				byteStride = 0;

			stride = byteStride;
		}
	}

	private static class Accessor {
		final int bufferView;
		final int byteOffset;
		final ComponentType compType;
		final VectorType vecType;
		final int count;

		final double[] min;
		final double[] max;

		Accessor(HashMap<String, JSONValue> accessorMap) {
			bufferView = getIntChild(accessorMap, "bufferView", false);
			count = getIntChild(accessorMap, "count", false);

			String vecTypeDesc = getStringChild(accessorMap, "type", false);
			vecType = VectorType.getFromType(vecTypeDesc);
			int compTypeNum = getIntChild(accessorMap, "componentType", false);
			compType = ComponentType.getFromNum(compTypeNum);

			Integer bOffset = getIntChild(accessorMap, "byteOffset", true);
			if (bOffset == null)
				byteOffset = 0;
			else
				byteOffset = bOffset;

			min = getNumberListChild(accessorMap, "min", true);
			max = getNumberListChild(accessorMap, "max", true);
		}
	}

	private static class Image {
		String uri;
		ByteBuffer imageData;
		String mimeType;
	}

	private static class Texture {
		final int source;

		Texture(HashMap<String, JSONValue> textureMap) {
			source = getIntChild(textureMap, "source", false);
		}
	}

	private static class Material {
		final int colorTex;
		final Color4d colorFactor;

		Material(HashMap<String, JSONValue> materialMap, int index) {
			Integer colorTex = walkMapForInt(materialMap, "pbrMetallicRoughness", "baseColorTexture", "index");

			if (colorTex == null) {
				// Color texture is missing, maybe this material has a spec/gloss extension
				colorTex = walkMapForInt(materialMap, "extensions", "KHR_materials_pbrSpecularGlossiness", "diffuseTexture", "index");
			}

			if (colorTex == null) {
				throw new RenderException(String.format("Material %d is missing color texture", index));
			}
			this.colorTex = colorTex;

			HashMap<String, JSONValue> pbrMap = getMapChild(materialMap, "pbrMetallicRoughness", true);
			double[] bcNums = null;
			if (pbrMap != null) {
				bcNums = getNumberListChild(pbrMap, "baseColorFactor", true);
			}
			if (bcNums != null) {
				if (bcNums.length != 4) {
					throw new RenderException("baseColorFactor must have 4 values");
				}
				this.colorFactor = new Color4d(bcNums[0],bcNums[1],bcNums[2],bcNums[3]);
			} else {
				this.colorFactor = new Color4d(1,1,1,1);
			}

		}
	}

	private static class Channel {
		final int samplerIdx;
		final int targetNode;
		final String targetPath;

		Channel(HashMap<String, JSONValue> chanMap) {
			samplerIdx = getIntChild(chanMap, "sampler", false);
			Integer targetNode = walkMapForInt(chanMap, "target", "node");
			String targetPath = walkMapForString(chanMap, "target", "path");
			if (targetNode == null || targetPath == null) {
				throw new RenderException("Incomplete animation channel");
			}
			this.targetNode = targetNode;
			this.targetPath = targetPath;
		}
	}

	private static class Sampler {
		final int inputAcc;
		final int outputAcc;
		final Interp interpolation;

		Sampler(HashMap<String, JSONValue> sampMap) {
			inputAcc = getIntChild(sampMap, "input", false);
			outputAcc = getIntChild(sampMap, "output", false);
			String interp = getStringChild(sampMap, "interpolation", true);
			if (interp == null)
				interpolation = Interp.LINEAR;
			else
				switch (interp) {
				case "STEP":
					interpolation = Interp.STEP;
					break;
				case "LINEAR":
					interpolation = Interp.LINEAR;
					break;
				case "CUBICSPLINE":
					interpolation = Interp.CUBICSPLINE;
					break;
				default:
					// throw unknown type here instead?
					interpolation = Interp.LINEAR;
				}
		}
	}

	private static class Animation {
		final ArrayList<Channel> channels = new ArrayList<>();
		final ArrayList<Sampler> samplers = new ArrayList<>();
		final String name;
		float start = Float.POSITIVE_INFINITY;
		float end = Float.NEGATIVE_INFINITY;

		Animation(String name) {
			this.name = name;
		}
	}

	// Members
	final HashMap<String, JSONValue> rootMap;

	// Lazily initialized members
	final HashMap<Integer, SceneNode> nodes = new HashMap<>();
	final HashMap<Integer, Mesh> meshes = new HashMap<>();

	final HashMap<Integer, Animation> animations = new HashMap<>();

	final HashMap<Integer, Buffer> buffers = new HashMap<>();
	final HashMap<Integer, BufferView> bufferViews = new HashMap<>();
	final HashMap<Integer, Accessor> accessors = new HashMap<>();

	final HashMap<Integer, Image> images = new HashMap<>();
	final HashMap<Integer, Texture> textures= new HashMap<>();
	final HashMap<Integer, Material> materials = new HashMap<>();

	final HashMap<Integer, int[]> processedMeshes = new HashMap<>();

	final HashMap<Integer, Integer> materialMap = new HashMap<>();
	//HashMap<Integer, int[]> primitiveMap = new HashMap<>();
	int numMats = 0;
	int numPrims = 0;

	// Many GLTF top level objects are lists of maps, this is a utility function
	// to get an indexed object from the list of a given name
	private HashMap<String, JSONValue> getRootObj(String typeName, int index) {
		ArrayList<JSONValue> topList = getListChild(rootMap, typeName, false);
		JSONValue objMap = topList.get(index);
		if (objMap == null || !objMap.isMap()) {
			String msg = String.format("GLTF file referencing missing %s: %d", typeName, index);
			throw new RenderException(msg);
		}
		return objMap.mapVal;
	}

	private Buffer getBuffer(int index) {
		Buffer buff = buffers.get(index);
		if (buff != null)
			return buff;

		HashMap<String, JSONValue> buffMap = getRootObj("buffers", index);
		String uri = getStringChild(buffMap, "uri", true);
		int byteLen = getIntChild(buffMap, "byteLength", false);

		if (byteLen > (1 << 30)) {
			// Cap buffers at an arbitrary 1GB. This can be increased if non-pathological assets need it
			String msg = String.format("Buffer byte length (%d) is too large", byteLen);
			throw new RenderException(msg);
		}

		if (uri == null) {
			if (index != 0) {
				throw new RenderException(String.format("Buffer %d is missing URI", index));
			}
			if (defaultBuffer == null) {
				throw new RenderException("Missing GLB binary data chunk");
			}

			// Check the chunk size
			int padding = 0;
			if ((byteLen % 4) != 0) {
				padding = 4 - byteLen % 4;
			}
			if (defaultBuffer.capacity() < byteLen + padding) {
				throw new RenderException("GLB default buffer too small");
			}

			buff = new Buffer(defaultBuffer);
			buffers.put(index, buff);
			return buff;
		}

		URI buffURI = contextURI.resolve(uri);

		if (buffURI.getScheme() != null && !buffURI.getScheme().equals("file")) {
			throw new RenderException(String.format("Can only load buffers from 'file' URLs currently. Invalid url: %s", buffURI.toString()));
		}


		//Path buffPath = FileSystems.getDefault().getPath(buffURL.getPath());
		ByteBuffer byteBuff;
		try {
			File buffFile = new File(buffURI);
			FileInputStream buffStream = new FileInputStream(buffFile);
			FileChannel buffChann = buffStream.getChannel();

			//FileChannel buffChann = FileChannel.open(buffPath, StandardOpenOption.READ);
			long fileSize = buffChann.size();
			if (fileSize < byteLen) {
				String msg = String.format("Buffer file (%s) too small. The file is %d bytes, but must be at least %d", uri, byteLen);
				buffStream.close();
				throw new RenderException(msg);
			}
			byteBuff = ByteBuffer.allocateDirect(byteLen);
			buffChann.read(byteBuff);

			// GLTF buffers are little endian
			byteBuff.order(ByteOrder.LITTLE_ENDIAN);

			buffStream.close();

		} catch (IOException io) {
			throw new RenderException(io.getMessage());
		}
		buff = new Buffer(byteBuff);
		buffers.put(index, buff);
		return buff;
	}

	private BufferView getBufferView(int index) {
		BufferView buffView = bufferViews.get(index);
		if (buffView != null)
			return buffView;

		HashMap<String, JSONValue> buffViewMap = getRootObj("bufferViews", index);
		buffView = new BufferView(buffViewMap);
		// Check that the buffer view fits in the buffer
		Buffer buff = getBuffer(buffView.buffIdx);
		if (buffView.offset + buffView.length > buff.contents.capacity()) {
			throw new RenderException(String.format("Buffer %d view is too large for buffer", index));
		}

		bufferViews.put(index, buffView);
		return buffView;
	}

	private Accessor getAccessor(int index) {
		Accessor accessor = accessors.get(index);
		if (accessor != null)
			return accessor;

		HashMap<String, JSONValue> accessorMap = getRootObj("accessors", index);

		accessor = new Accessor(accessorMap);

		// Check that the accessor fits inside the buffer view
		int numComps = accessor.vecType.numComps;
		int compSize = accessor.compType.size;

		BufferView view = getBufferView(accessor.bufferView);

		int stride = numComps*compSize;
		if (view.stride != 0) {
			stride = view.stride;
		}
		int accEnd = accessor.byteOffset + (accessor.count-1)*stride + numComps*compSize;
		if (accEnd > view.length) {
			throw new RenderException(String.format("Accessor %d does not fit in buffer view", index));
		}

		accessors.put(index, accessor);
		return accessor;
	}

	private Animation getAnimation(int index) {
		Animation anim = animations.get(index);
		if (anim != null)
			return anim;

		HashMap<String, JSONValue> animMap = getRootObj("animations", index);

		anim = new Animation(getStringChild(animMap, "name", true));

		ArrayList<JSONValue> samplers = getListChild(animMap, "samplers", false);
		for (JSONValue samp: samplers) {
			if (!samp.isMap()) {
				throw new RenderException("Animation sampler is not a JSON map");
			}
			anim.samplers.add(new Sampler(samp.mapVal));
		}

		ArrayList<JSONValue> channels = getListChild(animMap, "channels", false);
		for (JSONValue channel: channels) {
			if (!channel.isMap()) {
				throw new RenderException("Animation channel is not a JSON map");
			}
			anim.channels.add(new Channel(channel.mapVal));
		}

		animations.put(index, anim);
		return anim;
	}

	private Mesh getMesh(int index) {
		Mesh mesh = meshes.get(index);
		if (mesh != null)
			return mesh;

		HashMap<String, JSONValue> meshMap = getRootObj("meshes", index);

		mesh = new Mesh();
		ArrayList<JSONValue> primsList = getListChild(meshMap, "primitives", false);

		for (JSONValue prim: primsList) {
			if (!prim.isMap()) {
				throw new RenderException("Mesh primitive is not a JSON map");
			}
			mesh.primitives.add(new MeshPrimitive(prim.mapVal));
		}

		meshes.put(index, mesh);
		return mesh;
	}

	private Image getImage(int index) {
		Image image = images.get(index);
		if (image != null)
			return image;

		HashMap<String, JSONValue> imageMap = getRootObj("images", index);

		image = new Image();

		image.uri = getStringChild(imageMap, "uri", true);
		Integer bufferViewIdx = getIntChild(imageMap, "bufferView", true);
		image.mimeType = getStringChild(imageMap, "mimeType", true);

		if (bufferViewIdx != null) {
			// This is an embedded image
			BufferView view = getBufferView(bufferViewIdx);
			Buffer buff = getBuffer(view.buffIdx);
			if (view.stride != 0) {
				throw new RenderException("Image buffer view has a defined stride.");
			}
			if (image.uri != null) {
				throw new RenderException("Image declared with both URI and buffer view");
			}
			if (!image.mimeType.equals("image/png") && !image.mimeType.equals("image/jpeg")) {
				throw new RenderException(String.format("GLTF Image %d has unknown mime type: %s", index, image.mimeType));
			}

			// Allocate an indirect buffer for the image data
			// this technically is an extra copy but can be made to play more nicely with ImageIO
			image.imageData = ByteBuffer.allocate(view.length);
			buff.contents.position(view.offset);
			buff.contents.get(image.imageData.array(), 0, view.length);
			buff.contents.position(0);

			// Create a new URI format for embedded images
			try {
				image.uri = new URI("gltf-image", contextURI.getSchemeSpecificPart(), Integer.toString(index)).toString();
			} catch(URISyntaxException ex) {
				throw new RenderException(ex.getMessage());
			}

			// Add this image data to the final output
			outputData.addExplicitImage(image.uri, image.imageData);
		} else {
			if (image.uri == null) {
				throw new RenderException(String.format("GLTF Image %d defines neither URI or buffer view", index));
			}
			try {
				URI imageURI = new URI(image.uri);
				if (imageURI.getScheme() != null && imageURI.getScheme().equals("data")) {
					throw new RenderException("Images in Data URIs are not yet supported");
				}

			} catch(URISyntaxException ex) {
				throw new RenderException(ex.getMessage());
			}
		}

		images.put(index, image);
		return image;
	}

	private Texture getTexture(int index) {
		Texture texture = textures.get(index);
		if (texture != null)
			return texture;

		HashMap<String, JSONValue> textureMap = getRootObj("textures", index);

		texture = new Texture(textureMap);
		textures.put(index, texture);
		return texture;
	}

	private Material getMaterial(int index) {
		Material material = materials.get(index);
		if (material != null)
			return material;

		HashMap<String, JSONValue> materialMap = getRootObj("materials", index);

		material = new Material(materialMap, index);
		materials.put(index, material);
		return material;
	}

	private SceneNode getNode(int index) {
		SceneNode node = nodes.get(index);
		if (node != null) {
			return node;
		}

		HashMap<String, JSONValue> nodeMap = getRootObj("nodes", index);

		node = new SceneNode();
		node.meshIdx = getIntChild(nodeMap, "mesh", true);

		node.children = getIntListChild(nodeMap, "children", true);
		double[] matNums = getNumberListChild(nodeMap, "matrix", true);
		double[] rotNums = getNumberListChild(nodeMap, "rotation", true);
		double[] transNums = getNumberListChild(nodeMap, "translation", true);
		double[] scaleNums = getNumberListChild(nodeMap, "scale", true);

		if (matNums != null) {
			if (matNums.length != 16) {
				throw new RenderException("Node matrix property must have 16 values");
			}
			node.mat = new Mat4d(matNums);
			node.mat.transpose4(); // GLTF matrix is in column major order
		}
		if (rotNums != null) {
			if (rotNums.length != 4) {
				throw new RenderException("Node rotationproperty must have 4 values");
			}
			node.rot = new Quaternion(rotNums[0], rotNums[1], rotNums[2], rotNums[3]);
			node.rot.normalize();
		}
		if (transNums != null) {
			if (transNums.length != 3) {
				throw new RenderException("Node translation property must have 3 values");
			}
			node.trans = new Vec3d(transNums[0], transNums[1], transNums[2]);
		}
		if (scaleNums != null) {
			if (scaleNums.length != 3) {
				throw new RenderException("Node scale property must have 3 values");
			}
			node.scale = new Vec3d(scaleNums[0], scaleNums[1], scaleNums[2]);
		}

		node.fill();

		if (!node.isValid()) {
			throw new RenderException(String.format("Invalid scene node: %d", index));
		}

		nodes.put(index, node);
		return node;
	}

	private int getProcessedMaterial(int matIdx) {
		if (materialMap.get(matIdx) != null) {
			return materialMap.get(matIdx);
		}

		// This material has not yet been added to the output data
		int outMatIdx = numMats++;

		Material mat = getMaterial(matIdx);
		Texture colorTex = getTexture(mat.colorTex);
		Image colorImage = getImage(colorTex.source);
		URI colorURI = contextURI.resolve(colorImage.uri);

		outputData.addMaterial(colorURI, colorImage.uri, mat.colorFactor, null, null, 1, MeshData.NO_TRANS, null);
		return outMatIdx;
	}

	private Quaternion[] accToQuatArray(int accIdx) {
		Accessor acc = getAccessor(accIdx);
		BufferView view = getBufferView(acc.bufferView);
		Buffer buff = getBuffer(view.buffIdx);

		if (acc.vecType != VectorType.VEC4) {
			throw new RenderException(String.format("Accessor %d expected to be VEC3", accIdx));
		}
		// TODO: GLTF allows normalized integer values here
		if (acc.compType != ComponentType.float32) {
			throw new RenderException(String.format("Accessor %d expected to be float32. Only float based animation rotations are currently supported", accIdx));
		}

		int stride = 4*4; // float32 * vec4
		if (view.stride != 0) {
			stride = view.stride;
		}
		int offset = acc.byteOffset + view.offset;
		Quaternion[] ret = new Quaternion[acc.count];
		for (int i = 0; i < acc.count; ++i) {
			float xVal = buff.contents.getFloat(offset + i*stride + 0);
			float yVal = buff.contents.getFloat(offset + i*stride + 4);
			float zVal = buff.contents.getFloat(offset + i*stride + 8);
			float wVal = buff.contents.getFloat(offset + i*stride + 12);
			ret[i] = new Quaternion(xVal, yVal, zVal, wVal);
		}
		return ret;
	}

	private Vec3d[] accToVec3dArray(int accIdx) {
		Accessor acc = getAccessor(accIdx);
		BufferView view = getBufferView(acc.bufferView);
		Buffer buff = getBuffer(view.buffIdx);

		if (acc.vecType != VectorType.VEC3) {
			throw new RenderException(String.format("Accessor %d expected to be VEC3", accIdx));
		}
		if (acc.compType != ComponentType.float32) {
			throw new RenderException(String.format("Accessor %d expected to be float32", accIdx));
		}

		int stride = 4*3; // float32 * vec3
		if (view.stride != 0) {
			stride = view.stride;
		}
		int offset = acc.byteOffset + view.offset;
		Vec3d[] ret = new Vec3d[acc.count];
		for (int i = 0; i < acc.count; ++i) {
			float xVal = buff.contents.getFloat(offset + i*stride + 0);
			float yVal = buff.contents.getFloat(offset + i*stride + 4);
			float zVal = buff.contents.getFloat(offset + i*stride + 8);
			ret[i] = new Vec3d(xVal, yVal, zVal);
		}
		return ret;
	}

	private Vec2d[] accToVec2dArray(int accIdx) {
		Accessor acc = getAccessor(accIdx);
		BufferView view = getBufferView(acc.bufferView);
		Buffer buff = getBuffer(view.buffIdx);

		if (acc.vecType != VectorType.VEC2) {
			throw new RenderException(String.format("Accessor %d expected to be VEC2", accIdx));
		}
		if (acc.compType != ComponentType.float32) {
			throw new RenderException(String.format("Accessor %d expected to be float32", accIdx));
		}

		int stride = 4*2; // float32 * vec3
		if (view.stride != 0) {
			stride = view.stride;
		}
		int offset = acc.byteOffset + view.offset;
		Vec2d[] ret = new Vec2d[acc.count];

		for (int i = 0; i < acc.count; ++i) {
			float xVal = buff.contents.getFloat(offset + i*stride + 0);
			float yVal = buff.contents.getFloat(offset + i*stride + 4);
			ret[i] = new Vec2d(xVal, yVal);
		}
		return ret;
	}

	private float[] accToFloatArray(int accIdx) {
		Accessor acc = getAccessor(accIdx);
		BufferView view = getBufferView(acc.bufferView);
		Buffer buff = getBuffer(view.buffIdx);

		if (acc.vecType != VectorType.SCALAR) {
			throw new RenderException(String.format("Accessor %d expected to be SCALAR", accIdx));
		}

		if (acc.compType != ComponentType.float32) {
			throw new RenderException(String.format("Accessor %d expected to be float32", accIdx));
		}

		int stride = 4;

		if (view.stride != 0) {
			stride = view.stride;
		}

		int offset = acc.byteOffset + view.offset;
		float[] ret = new float[acc.count];

		for (int i = 0; i < acc.count; ++i) {
			ret[i] = buff.contents.getFloat(offset + i*stride);
		}
		return ret;
	}

	private int[] accToIntArray(int accIdx) {
		Accessor acc = getAccessor(accIdx);
		BufferView view = getBufferView(acc.bufferView);
		Buffer buff = getBuffer(view.buffIdx);

		if (acc.vecType != VectorType.SCALAR) {
			throw new RenderException(String.format("Accessor %d expected to be SCALAR", accIdx));
		}

		int stride = acc.compType.size;
		if (view.stride != 0) {
			stride = view.stride;
		}

		int offset = acc.byteOffset + view.offset;
		int[] ret = new int[acc.count];

		for (int i = 0; i < acc.count; ++i) {
			int pos = offset + i*stride;

			ret[i] = readIntFromBuffer(buff.contents, pos, acc.compType);
		}
		return ret;
	}

	private Vec3d[] generateNormals(Vec3d[] verts) {
		Vec3d[] ret = new Vec3d[verts.length];
		if (verts.length % 3 != 0) {
			throw new RenderException("Mesh vertices not divisible by 3");
		}
		Vec3d t0 = new Vec3d();
		Vec3d t1 = new Vec3d();

		int numTris = verts.length / 3;
		for (int i = 0; i < numTris; i++) {
			Vec3d x = verts[i*3 + 0];
			Vec3d y = verts[i*3 + 1];
			Vec3d z = verts[i*3 + 2];

			t0.sub3(y, x);
			t1.sub3(z, y);
			Vec3d norm = new Vec3d();
			norm.cross3(t0, t1);
			norm.normalize3();

			// Don't worry about degenerate faces, they will be removed later

			ret[i*3 + 0] = norm;
			ret[i*3 + 1] = norm;
			ret[i*3 + 2] = norm;
		}
		return ret;
	}

	// Returns a list of the sub meshes (each matching a mesh primitive) in the final MeshData
	private int[] getProcessedMesh(int meshIdx) {

		if (processedMeshes.get(meshIdx) != null) {
			return processedMeshes.get(meshIdx);
		}

		Mesh mesh = getMesh(meshIdx);

		int[] ret = new int[mesh.primitives.size()];
		for (int primIdx = 0; primIdx < ret.length; ++ primIdx) {
			MeshPrimitive prim = mesh.primitives.get(primIdx);

			Vec3d[] vertPos = accToVec3dArray(prim.posAcc);
			Vec3d[] vertNorms;
			if (prim.normAcc != null) {
				vertNorms = accToVec3dArray(prim.normAcc);
			} else {
				vertNorms = generateNormals(vertPos);
			}

			Vec2d[] texCoords = null;
			if (prim.texCoord0Acc != null) {
				texCoords = accToVec2dArray(prim.texCoord0Acc);
				for (Vec2d tc: texCoords) {
					// GLTF specifies the texture origin is top left,
					// while we use bottom left. Flip the y-coordinate
					tc.y = 1.0 - tc.y;
				}
			}

			int[] indices = null;
			if (prim.indicesAcc != null) {
				indices = accToIntArray(prim.indicesAcc);
			}

			int numVerts;
			if (indices != null) {
				numVerts = indices.length;
			} else {
				numVerts = vertPos.length;
			}

			// Optional validation code
			Accessor posAcc = getAccessor(prim.posAcc);
			if (posAcc.min == null || posAcc.max == null || posAcc.min.length != 3 || posAcc.max.length != 3) {
				throw new RenderException("Mesh position accessors missing min or max bounds");
			}
			Vec3d posMin = new Vec3d(posAcc.min[0], posAcc.min[1], posAcc.min[2]);
			Vec3d posMax = new Vec3d(posAcc.max[0], posAcc.max[1], posAcc.max[2]);
			AABB posBounds = new AABB(posMax, posMin);
			for (Vec3d pos: vertPos) {
				if (!posBounds.collides(pos, 0.0001)) {
					throw new RenderException("Mesh position fails bounds check");
				}
			}

			VertexMap vMap = new VertexMap();
			int[] outIndices = new int[numVerts];
			for (int i = 0; i < numVerts; ++i) {
				int vertIdx = i;
				if (indices != null) {
					vertIdx = indices[i];
				}
				Vec3d pos = vertPos[vertIdx];
				Vec3d norm = vertNorms[vertIdx];
				Vec2d texCoord = null;
				if (texCoords != null) {
					texCoord = texCoords[vertIdx];
				}
				outIndices[i] = vMap.getVertIndex(pos, norm, texCoord);
			}


			ret[primIdx] = numPrims++;
			outputData.addSubMesh(vMap.getVertList(), outIndices);
		}

		processedMeshes.put(meshIdx, ret);
		return ret;
	}

	public static MeshData parseGLTF(URI asset) throws RenderException {

		GLTFReader parser = new GLTFReader(asset, null, null);
		return parser.outputData;
	}

	public static MeshData parseGLB(URI asset) throws RenderException {

		ByteBuffer gltfBuff = null;
		ByteBuffer defaultBuff = null;
		try {
			if (asset.getScheme() != null && !asset.getScheme().equals("file")) {
				throw new RenderException("GLB assets must be files");
			}

			File buffFile = new File(asset);
			FileInputStream buffStream = new FileInputStream(buffFile);
			FileChannel buffChann = buffStream.getChannel();

			long fileSize = buffChann.size();
			if (fileSize > (1 << 30)) {
				// Arbitrary file size limit. We most likely do not want to support assets > 1GB
				buffStream.close();
				throw new RenderException("GLB asset is too large");
			}

			if (fileSize < 256) {
				// Arbitrary file size limit. It is likely impossible to have a meaningful GLB asset this small
				buffStream.close();
				throw new RenderException("GLB asset is too small");
			}

			ByteBuffer headerBuff = ByteBuffer.allocateDirect(20);
			buffChann.read(headerBuff);

			// GLB buffers are little endian
			headerBuff.order(ByteOrder.LITTLE_ENDIAN);
			headerBuff.flip();

			int magic = headerBuff.getInt();
			int version = headerBuff.getInt();
			int length = headerBuff.getInt();

			if (magic != 0x46546C67) {
				buffStream.close();
				throw new RenderException("Asset is not a GLB file");
			}
			if (version != 2) {
				buffStream.close();
				throw new RenderException("Unsupport GLTF version");
			}

			if (length != fileSize) {
				buffStream.close();
				throw new RenderException(String.format("GLB asset is incorrect size. Expected: %d, got: %d", length, fileSize));
			}

			int gltfChunkLength = headerBuff.getInt();
			int gltfChunkType = headerBuff.getInt();
			if (gltfChunkType != 0x4E4F534A) {
				buffStream.close();
				throw new RenderException("GLTF chunk missing in GLB file");
			}
			if (fileSize < gltfChunkLength + 20) {
				buffStream.close();
				throw new RenderException("GLTF chuck reported size is too large for GLB file");
			}
			gltfBuff = ByteBuffer.allocateDirect(gltfChunkLength);
			buffChann.read(gltfBuff);
			gltfBuff.flip();

			if (buffChann.position() != fileSize) {
				// We are not at the end of the file, there is a binary chunk
				ByteBuffer binHeaderBuff = ByteBuffer.allocateDirect(8);
				buffChann.read(binHeaderBuff);

				// GLB buffers are little endian
				binHeaderBuff.order(ByteOrder.LITTLE_ENDIAN);
				binHeaderBuff.flip();

				int binChunkLength = binHeaderBuff.getInt();
				int binChunkType = binHeaderBuff.getInt();
				if (binChunkType != 0x004E4942) {
					buffStream.close();
					throw new RenderException("Unknown second GLB chunk type");
				}
				if (binChunkLength > fileSize - buffChann.position()) {
					buffStream.close();
					throw new RenderException("GLB chunk size is too large");
				}
				defaultBuff = ByteBuffer.allocateDirect(binChunkLength);
				buffChann.read(defaultBuff);
				defaultBuff.order(ByteOrder.LITTLE_ENDIAN);
				defaultBuff.flip();
			}

			buffStream.close();

		} catch(RenderException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new RenderException(ex.getMessage());
		}
		GLTFReader parser = new GLTFReader(asset, gltfBuff, defaultBuff);
		return parser.outputData;
	}

	private GLTFReader(URI context, ByteBuffer gltfBuff, ByteBuffer defBuff) {
		contextURI = context;
		contextURI.normalize();
		defaultBuffer = defBuff;
		if (defaultBuffer == null)
			rootMap = setRootFromURI(context);
		else
			rootMap = setRootFromByteBuffer(gltfBuff);

		process();
	}

	private MeshData.TreeNode buildOutputNode(int nodeIdx, Stack<Integer> nodeStack) {
		if (nodeStack.contains(nodeIdx)) {
			throw new RenderException("Scene node loop detected");
		}
		nodeStack.push(nodeIdx);

		SceneNode node = getNode(nodeIdx);
		MeshData.TreeNode ret = new MeshData.TreeNode();

		Mat4d staticMat = node.getStaticMatrix();
		if (!node.animated) {
			ret.trans = new MeshData.StaticTrans(staticMat);
		} else {
			int numAnims = node.animMap.size();
			double[][] timesArray = new double[numAnims][];
			Mat4d[][] matsArray = new Mat4d[numAnims][];
			String[] names = new String[numAnims];
			int outIdx = 0;
			for (int animIdx : node.animMap.keySet()) {
				Animation anim = getAnimation(animIdx);

				names[outIdx] = anim.name;
				float duration = anim.end - anim.start;
				// TODO: more intelligent sampling logic
				float SAMPLE_RATE = 1.0f/20.0f;

				int numSamples = Math.round(duration/SAMPLE_RATE);
				timesArray[outIdx] = new double[numSamples];
				matsArray[outIdx] = new Mat4d[numSamples];
				float sampTime = anim.start;
				for (int i = 0; i < numSamples; ++i) {
					timesArray[outIdx][i] = sampTime;
					matsArray[outIdx][i] = node.getAnimatedMatrix(animIdx, sampTime);

					sampTime += SAMPLE_RATE;
				}
				outIdx++;
			}
			ret.trans = new MeshData.AnimTrans(timesArray, matsArray, names, staticMat);
		}

		if (node.meshIdx != null) {
			// Process the mesh and add a mesh instance

			Mesh mesh = getMesh(node.meshIdx);
			int[] subMeshes = getProcessedMesh(node.meshIdx);
			for (int i = 0; i < mesh.primitives.size(); ++i) {
				// Add a sub mesh instance for each primitive in the mesh
				int subMeshIdx = subMeshes[i];
				int matIdx = mesh.primitives.get(i).material;
				int outMatIdx = getProcessedMaterial(matIdx);

				// Note: JaamSim allows sub meshes to use arbitrary materials
				// while JLTF primitives have fixed materials therefore we
				// need to bind the materials when adding each sub mesh instance
				MeshData.AnimMeshInstance inst = new MeshData.AnimMeshInstance(subMeshIdx, outMatIdx);

				ret.meshInstances.add(inst);
			}
		}
		if (node.children != null) {
			for (int childIdx: node.children) {
				ret.children.add(buildOutputNode(childIdx, nodeStack));
			}
		}
		nodeStack.pop();
		return ret;
	}

	private HashMap<String, JSONValue> setRootFromByteBuffer(ByteBuffer buff) {
		String jsonString = StandardCharsets.UTF_8.decode(buff).toString();

		JSONParser jsonParser = new JSONParser();
		jsonParser.addPiece(jsonString);
		JSONValue root;
		try {
			root = jsonParser.parse();
			if (!root.isMap()) {
				throw new RenderException("Top level GLTF JSON error. File is not a map");
			}
		} catch (JSONError ex) {
			String msg = String.format("JSON parse error: %s", ex.getMessage());
			throw new RenderException(msg);
		}

		return root.mapVal;
	}

	private HashMap<String, JSONValue> setRootFromURI(URI asset) {
		InputStream inStream;
		try {
			inStream = asset.toURL().openStream();
		} catch (IOException ex) {
			throw new RenderException("Can't read " + asset);
		}
		BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
		JSONParser jsonParser = new JSONParser();
		br.lines().forEachOrdered(x -> jsonParser.addPiece(x));
		JSONValue root;
		try {
			root = jsonParser.parse();
			if (!root.isMap()) {
				throw new RenderException("Top level GLTF JSON error. File is not a map");
			}
		} catch (JSONError ex) {
			String msg = String.format("Could not read (%s). Error: %s", asset.toString(), ex.getMessage());
			throw new RenderException(msg);
		}

		return root.mapVal;
	}

	private void process() {

		Integer scene = getIntChild(rootMap, "scene", true);
		if (scene == null)
			scene = 0;
		ArrayList<JSONValue> scenes = getListChild(rootMap, "scenes", false);
		if (scenes.size() <= scene) {
			throw new RenderException(String.format("Scene %d missing from file", scene));
		}
		JSONValue sceneVal = scenes.get(scene);

		if (!sceneVal.isMap()) {
			throw new RenderException("Scene object must be a JSON object");
		}
		// Create a rotation to map 'forward' from GLTF convention to JaamSim convention
		// Specifically,
		// Fwd: +Z -> +X
		// Up: +Y -> + Z

		Quaternion rotY = new Quaternion();
		rotY.setRotYAxis(Math.PI/2.0);

		Quaternion rotX = new Quaternion();
		rotX.setRotXAxis(Math.PI/2.0);

		Quaternion finalRot = new Quaternion();
		finalRot.mult(rotX, rotY);

		Mat4d rotMat = new Mat4d();
		rotMat.setRot4(finalRot);

		JSONValue animList = rootMap.get("animations");
		if (animList != null && animList.isList()) {
			for (int i = 0; i <animList.listVal.size(); ++i) {
				Animation anim = getAnimation(i);

				for (Channel chan: anim.channels) {
					SceneNode targetNode = getNode(chan.targetNode);
					targetNode.animated = true;

					NodeAnimation nodeAnim = targetNode.animMap.get(i);
					if (nodeAnim == null) {
						nodeAnim = new NodeAnimation();
						targetNode.animMap.put(i, nodeAnim);
					}

					if (chan.samplerIdx < 0 || chan.samplerIdx >= anim.samplers.size()) {
						throw new RenderException("Animation sampler index out of bounds");
					}
					Sampler samp = anim.samplers.get(chan.samplerIdx);
					float[] inputArray = accToFloatArray(samp.inputAcc);

					// Validate the input array
					for (int ii = 0; ii < inputArray.length-1; ++ii) {
						if (inputArray[ii] > inputArray[ii+1]) {
							throw new RenderException("Animation keyframes are not monotonically increasing");
						}
					}
					if (inputArray[0] < anim.start) {
						anim.start = inputArray[0];
					}
					if (inputArray[inputArray.length-1] > anim.end) {
						anim.end = inputArray[inputArray.length-1];
					}

					switch(chan.targetPath) {
					case "rotation":
						if (nodeAnim.rotInput != null) {
							throw new RenderException("The same node is target of more than one rotation curve in same animation");
						}
						nodeAnim.rotInput = inputArray;
						nodeAnim.rotValues = accToQuatArray(samp.outputAcc);
						nodeAnim.rotInterp = samp.interpolation;
						break;
					case "translation":
						if (nodeAnim.transInput != null) {
							throw new RenderException("The same node is target of more than one scale curve in same animation");
						}
						nodeAnim.transInput = inputArray;
						nodeAnim.transValues = accToVec3dArray(samp.outputAcc);
						nodeAnim.transInterp = samp.interpolation;
						break;
					case "scale":
						if (nodeAnim.scaleInput != null) {
							throw new RenderException("The same node is target of more than one translation curve in same animation");
						}
						nodeAnim.scaleInput = inputArray;
						nodeAnim.scaleValues = accToVec3dArray(samp.outputAcc);
						nodeAnim.scaleInterp = samp.interpolation;
						break;
					case "weights":
						// Ignore for now
						break;
					default:
						throw new RenderException(String.format("Unknown animation channel target path: %s", chan.targetPath));
					}

				}
			}
		}

		int[] nodes = getIntListChild(sceneVal.mapVal, "nodes", false);

		// Stack for node loop detection
		Stack<Integer> nodeStack = new Stack<>();

		MeshData.TreeNode rootNode = new MeshData.TreeNode();
		rootNode.trans = new MeshData.StaticTrans(rotMat);
		for (int node: nodes) {
			rootNode.children.add(buildOutputNode(node, nodeStack));
		}

		outputData.setTree(rootNode);

		outputData.finalizeData();

	}
}
