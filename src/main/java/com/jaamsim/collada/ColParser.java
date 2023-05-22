/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.collada;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.MeshData.Trans;
import com.jaamsim.MeshFiles.VertexMap;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.AnimCurve;
import com.jaamsim.render.RenderException;
import com.jaamsim.ui.LogBox;
import com.jaamsim.xml.XmlNode;
import com.jaamsim.xml.XmlParser;

/**
 * Inspired by the Collada loader for Sweethome3d by Emmanuel Puybaret / eTeks <info@eteks.com>.
 */
public class ColParser {

	private static boolean SHOW_COL_DEBUG = false;

	private static final Effect DEFAULT_EFFECT;
	private static final String DEFAULT_MATERIAL_NAME = "JaamSimDefaultMaterial";

	private static boolean keepRuntimeData = false;
	public static void setKeepData(boolean keepData) {
		keepRuntimeData = keepData;
	}

	static {
		DEFAULT_EFFECT = new Effect();
		DEFAULT_EFFECT.diffuse = new ColorTex();
		DEFAULT_EFFECT.diffuse.color = new Color4d(0.5, 0.5, 0.5, 1);
		DEFAULT_EFFECT.transType = MeshData.NO_TRANS;
	}

	public static MeshData parse(URI asset) throws RenderException {

		try {
			ColParser colParser = new ColParser(asset.toURL());

			MeshData finalData = colParser.getData();
			finalData.setSource(asset.toString());

			colParser.processContent();

			return finalData;

		} catch (Exception e) {
			LogBox.renderLogException(e);
			throw new RenderException(e.getMessage());
		}
	}

	private static void parseAssert(boolean b) {
		if (!b) {
			throw new RenderException("Failed Collada parsing assert");
		}
	}

	static final List<String> DOUBLE_ARRAY_TAGS;
	static final List<String> INT_ARRAY_TAGS;
	static final List<String> STRING_ARRAY_TAGS;
	static final List<String> BOOLEAN_ARRAY_TAGS;

	static {
		DOUBLE_ARRAY_TAGS = new ArrayList<>();
		DOUBLE_ARRAY_TAGS.add("float_array");
		DOUBLE_ARRAY_TAGS.add("float");
		DOUBLE_ARRAY_TAGS.add("rotate");
		DOUBLE_ARRAY_TAGS.add("translate");
		DOUBLE_ARRAY_TAGS.add("scale");
		DOUBLE_ARRAY_TAGS.add("lookat");
		DOUBLE_ARRAY_TAGS.add("matrix");
		DOUBLE_ARRAY_TAGS.add("color");
		DOUBLE_ARRAY_TAGS.add("bind_shape_matrix");

		INT_ARRAY_TAGS = new ArrayList<>();
		INT_ARRAY_TAGS.add("int_array");
		INT_ARRAY_TAGS.add("vcount");
		INT_ARRAY_TAGS.add("p");
		INT_ARRAY_TAGS.add("h");
		INT_ARRAY_TAGS.add("v");

		STRING_ARRAY_TAGS = new ArrayList<>();
		STRING_ARRAY_TAGS.add("Name_array");
		STRING_ARRAY_TAGS.add("IDREF_array");

		BOOLEAN_ARRAY_TAGS = new ArrayList<>();
		BOOLEAN_ARRAY_TAGS.add("boolean_array");

	}

	private static class FaceSubGeo {
		public VertexMap vMap;
		int[] indices;

		public FaceSubGeo(int size) {
			vMap = new VertexMap();
			indices = new int[size];
		}
	}

	private static class LineSubGeo {
		public final Vec4d[] verts;
		public String materialSymbol;

		public LineSubGeo(int size) {
			verts = new Vec4d[size];
		}
	}

	private static class VisualScene {
		public final ArrayList<SceneNode> nodes = new ArrayList<>();
	}

	private static class Controller {
		private Mat4d bindSpaceMat;
		private String geometry;
	}

	private static class Geometry {
		// Note: the face information is lazily baked when it is first referenced because that is the first time we
		// know which texture coordinate set to use (then error if it is later referenced with different coordinate sets)
		public final ArrayList<SubMeshDesc> faceSubDescs = new ArrayList<>();
		public final ArrayList<LineSubGeo> lineSubGeos = new ArrayList<>();
	}

	private static class AnimSampler {
		public String inputSource;
		public String outputSource;
		public String inTangentSource;
		public String outTangentSource;
		public String interpSource;
	}

	private static class AnimChannel {
		public String target;
		public String actionName;
		public AnimCurve curve;
	}

	private final URL _contextURL;

	private final HashMap<String, Geometry> _geos = new HashMap<>();
	private final HashMap<String, String> _images = new HashMap<>(); // Maps image names to files
	private final HashMap<String, String> _materials = new HashMap<>(); // Maps materials to effects
	private final HashMap<String, Effect> _effects = new HashMap<>(); // List of known effects
	private final HashMap<String, SceneNode> _namedNodes = new HashMap<>();
	private final HashMap<String, VisualScene> _visualScenes = new HashMap<>();
	private final HashMap<String, Controller> _controllers = new HashMap<>();
	private final HashMap<String, AnimSampler> _samplers = new HashMap<>();
	private final HashMap<String, ArrayList<String>> _animClips = new HashMap<>();

	// This stack is used to track node loops
	private final  Stack<SceneNode> _nodeStack = new Stack<>();

	// This list tracks the combinations of sub geometries and effects loaded in the mesh proto and defines an implicit
	// index into the mesh proto. This should probably be made more explicit later
	private final ArrayList<SubMeshDesc> _loadedFaceGeos = new ArrayList<>();
	private final ArrayList<Effect> _loadedEffects = new ArrayList<>();
	private final ArrayList<LineGeoEffectPair> _loadedLineGeos = new ArrayList<>();

	private final ArrayList<AnimChannel> _animChannels = new ArrayList<>();

	private final MeshData _finalData = new MeshData(keepRuntimeData);

	private final HashMap<String, Vec4d[]> _vec4dSources = new HashMap<>();
	private final HashMap<String, double[][]> _dataSources = new HashMap<>();
	private final HashMap<String, String[]> _stringSources = new HashMap<>();

	private XmlNode _colladaNode;
	private XmlParser _parser;

	public ColParser(URL context) {
		_contextURL = context;
	}

	private XmlNode getNodeFromID(String fragID) {

		if (fragID.length() < 1 || fragID.charAt(0) != '#') {
			return null;
		}
		return _parser.getNodeByID(fragID.substring(1));
	}

	private void processContent() {

		_parser = new XmlParser(_contextURL);

		_parser.setDoubleArrayTags(DOUBLE_ARRAY_TAGS);
		_parser.setIntArrayTags(INT_ARRAY_TAGS);
		_parser.setBooleanArrayTags(BOOLEAN_ARRAY_TAGS);
		_parser.setStringArrayTags(STRING_ARRAY_TAGS);

		long startTime = System.nanoTime();
		_parser.parse();
		long parseTime = System.nanoTime();


		_colladaNode = _parser.getRootNode().findChildTag("COLLADA", false);
		parseAssert(_colladaNode != null);

		processGeos();
		long geoTime = System.nanoTime();
		processImages();
		processMaterials();
		processEffects();
		processNodes();
		processControllers();

		processAnimationClips();
		processAnimations();

		processVisualScenes();

		processScene();

		long sceneTime = System.nanoTime();

		double parseDurMS = (parseTime - startTime)/1000000.0;
		double geoDurMS = (geoTime - parseTime)/1000000.0;
		double sceneDurMS = (sceneTime - geoTime)/1000000.0;

		if (SHOW_COL_DEBUG) {
			LogBox.formatRenderLog("%s Parse: %.1f Geo: %.1f, Scene: %.1f\n", _contextURL.toString(), parseDurMS, geoDurMS, sceneDurMS);
		}
	}

	private double getScaleFactor() {
		XmlNode assetNode = _colladaNode.findChildTag("asset", false);
		if (assetNode == null) return 1;

		XmlNode unit = assetNode.findChildTag("unit", false);
		if (unit == null) return 1;

		String meter = unit.getAttrib("meter");
		if (meter == null) return 1;

		return Double.parseDouble(meter);
	}

	private String getUpAxis() {
		XmlNode assetNode = _colladaNode.findChildTag("asset", false);
		if (assetNode == null) return "Y_UP";

		XmlNode upAxisNode = assetNode.findChildTag("up_axis", false);
		if (upAxisNode == null) return "Y_UP";


		String ret = (String)upAxisNode.getContent();
		if (ret == null) {
			return "Y_UP";
		}
		return ret;
	}

	/**
	 * Returns a matrix that rotates which ever axis is specified into the Z axis (as JaamSim treats Z as up)
	 * @return
	 */
	private Mat4d getGlobalRot() {
		String up = getUpAxis();
		Mat4d ret = new Mat4d();
		if (up.equals("Z_UP")) {
			return ret;
		} else if (up.equals("X_UP")) {
			ret.d00 =  0; ret.d01 =  0; ret.d02 =  1;
			ret.d10 = -1; ret.d11 =  0; ret.d12 =  0;
			ret.d20 =  0; ret.d21 = -1; ret.d22 =  0;
			return ret;
		} else  { // Y_UP
			ret.d00 =  1; ret.d01 =  0; ret.d02 =  0;
			ret.d10 =  0; ret.d11 =  0; ret.d12 = -1;
			ret.d20 =  0; ret.d21 =  1; ret.d22 =  0;
			return ret;
		}

	}

	private void processScene() {
		for (AnimChannel chan : _animChannels) {
			attachChannelToScene(chan);
		}

		XmlNode scene = _colladaNode.findChildTag("scene", false);
		parseAssert(scene != null);

		XmlNode instVS = scene.findChildTag("instance_visual_scene", false);
		parseAssert(instVS != null);
		String vsURL = instVS.getAttrib("url");
		parseAssert(vsURL.charAt(0) == '#');

		Mat4d globalMat = getGlobalRot();
		globalMat.scale3(getScaleFactor());

		VisualScene vs = _visualScenes.get(vsURL.substring(1));
		MeshData.TreeNode treeRoot = new MeshData.TreeNode();
		treeRoot.trans = new MeshData.StaticTrans(globalMat);

		for (SceneNode sn : vs.nodes) {
			treeRoot.children.add(buildMeshTree(sn));
		}
		_finalData.setTree(treeRoot);

		_finalData.finalizeData();
	}


	private MeshData.TreeNode buildMeshTree(SceneNode node) {
		_nodeStack.push(node);

		// Create a series of TreeNodes, one for each transform
		MeshData.TreeNode ret = new MeshData.TreeNode();
		MeshData.TreeNode leaf = ret;
		if (node.transforms.size() == 0) {
			// No transforms, fill in an identity transform in the output tree
			ret.trans = new MeshData.StaticTrans(new Mat4d());
		} else {
			for (int i = 0; i < node.transforms.size(); ++i) {
				leaf.trans = node.transforms.get(i).toMeshDataTrans();

				if (i < node.transforms.size()-1) {
					// If this isn't the last node, create a new one for the next iteration
					MeshData.TreeNode newNode = new MeshData.TreeNode();
					leaf.children.add(newNode);
					leaf = newNode;
				}
			}
		}

		for (GeoInstInfo geoInfo : node.subGeo) {
			addGeoInstToTreeNode(geoInfo, leaf);
		}

		// Add instance_node
		for (String nodeName : node.subInstanceNames) {
			parseAssert(nodeName.charAt(0) == '#');
			SceneNode instNode = _namedNodes.get(nodeName.substring(1));
			// Check for reference loops, make sure this node is not currently in the active node stack
			parseAssert(!_nodeStack.contains(instNode));

			parseAssert(instNode != null);
			node.subNodes.add(instNode);

			leaf.children.add(buildMeshTree(instNode));
		}

		// Finally add children
		for (SceneNode nextNode : node.subNodes) {
			leaf.children.add(buildMeshTree(nextNode));
		}

		_nodeStack.pop();

		return ret;
	}

	private Effect geoBindingToEffect(Map<String, String> materialMap, String symbol) {
		if (symbol == DEFAULT_MATERIAL_NAME) {
			return DEFAULT_EFFECT;
		}

		String materialId = materialMap.get(symbol);
		parseAssert(materialId != null);

		return getEffectForMat(materialId);
	}

	private Effect getEffectForMat(String materialId) {
		parseAssert(materialId.charAt(0) == '#');
		String effectId = _materials.get(materialId.substring(1));
		parseAssert(effectId != null);

		parseAssert(effectId.charAt(0) == '#');
		Effect effect = _effects.get(effectId.substring(1));
		parseAssert(effect != null);

		return effect;
	}

	private void addGeoInstToTreeNode(GeoInstInfo geoInfo, MeshData.TreeNode node) {
		parseAssert(geoInfo.geoName.charAt(0) == '#');
		Geometry geo = _geos.get(geoInfo.geoName.substring(1));

		for (SubMeshDesc subGeo : geo.faceSubDescs) {
			// Check if this geometry and material pair has been loaded yet
			Effect effect = geoBindingToEffect(geoInfo.materialMap, subGeo.material);

			// Check that this instance of the subgeometry uses the same texture coordinate set as any previous
			if (geoInfo.usedTexSet != null) {
				if (subGeo.usedTexSet != null) {
					parseAssert(geoInfo.usedTexSet.intValue() == subGeo.usedTexSet.intValue());
				}
				subGeo.usedTexSet = geoInfo.usedTexSet;
			} else {
				subGeo.usedTexSet = 0;
			}

			int geoID;
			if (_loadedFaceGeos.contains(subGeo)) {
				geoID = _loadedFaceGeos.indexOf(subGeo);
			} else {
				geoID = _loadedFaceGeos.size();
				_loadedFaceGeos.add(subGeo);

				// Finally bake the face geometry information into a runtime format
				FaceSubGeo fsg = getFaceSubGeo(subGeo);
				_finalData.addSubMesh(fsg.vMap.getVertList(), fsg.indices);
			}

			int matID;
			if (_loadedEffects.contains(effect)) {
				matID = _loadedEffects.indexOf(effect);
			} else {
				matID = _loadedEffects.size();
				_loadedEffects.add(effect);
				_finalData.addMaterial(effect.diffuse.texture,
				                       effect.diffuse.relTexture,
				                       effect.diffuse.color,
				                       effect.ambient,
				                       effect.spec,
				                       effect.shininess,
				                       effect.transType,
				                       effect.transColour);
			}

			MeshData.AnimMeshInstance inst = new MeshData.AnimMeshInstance(geoID, matID);
			node.meshInstances.add(inst);
		}

		for (LineSubGeo subGeo : geo.lineSubGeos) {
			// Check if this geometry and material pair has been loaded yet
			Effect effect = geoBindingToEffect(geoInfo.materialMap, subGeo.materialSymbol);

			LineGeoEffectPair ge = new LineGeoEffectPair(subGeo, effect);
			int geoID;
			if (_loadedLineGeos.contains(ge)) {
				geoID = _loadedLineGeos.indexOf(ge);
			} else {
				geoID = _loadedLineGeos.size();
				_loadedLineGeos.add(ge);
				_finalData.addSubLine(subGeo.verts,
				                       effect.diffuse.color);
			}
			node.lineInstances.add(new MeshData.AnimLineInstance(geoID));

		}
	}

	private void processVisualScenes() {
		XmlNode libScenes = _colladaNode.findChildTag("library_visual_scenes", false);
		if (libScenes == null)
			return; // No scenes

		for (XmlNode child : libScenes.children()) {
			if (child.getTag().equals("visual_scene")) {
				processVisualScene(child);
			}
		}
	}

	private void processVisualScene(XmlNode scene) {
		String id = scene.getFragID();
		VisualScene vs = new VisualScene();
		_visualScenes.put(id, vs);

		for (XmlNode child : scene.children()) {
			if (child.getTag().equals("node")) {
				SceneNode node = processNode(child, null);
				vs.nodes.add(node);
			}
		}
	}

	private void processControllers() {
		XmlNode libControllers = _colladaNode.findChildTag("library_controllers", false);
		if (libControllers == null)
			return; // No effects

		for (XmlNode child : libControllers.children()) {
			if (child.getTag().equals("controller")) {
				processController(child);
			}
		}
	}

	private void processController(XmlNode controller) {
		XmlNode skin = controller.findChildTag("skin", false);

		String id = controller.getFragID();
		if (skin == null) {
			return; // We do not handle 'morph' controllers for now
		}

		Controller cont = new Controller();

		String source = skin.getAttrib("source");
		parseAssert(source != null);

		cont.geometry = source;

		XmlNode bindMat = skin.findChildTag("bind_space_matrix", false);
		if (bindMat == null) {
			cont.bindSpaceMat = new Mat4d(); // Default to identity
		} else {
			double[] data = (double[])bindMat.getContent();
			parseAssert(data.length == 16);
			cont.bindSpaceMat = new Mat4d(data);
		}

		_controllers.put(id, cont);
	}

	private void processAnimationClips() {
		XmlNode libClips = _colladaNode.findChildTag("library_animation_clips", false);
		if (libClips == null)
			return; // No animations

		for (XmlNode child : libClips.children()) {
			if (child.getTag().equals("animation_clip")) {
				processAnimationClip(child);
			}
		}
	}

	private void processAnimationClip(XmlNode clipNode) {
		String clipName = clipNode.getAttrib("name");
		if (clipName == null) {
			// Fall back to the ID if the name is not present
			clipName = clipNode.getFragID();
		}
		if (clipName == null) {
			return; // We can not identify this clip so act like it doesn't exist
		}
		ArrayList<String> instanceAnims = new ArrayList<>();
		_animClips.put(clipName, instanceAnims);

		for (XmlNode child : clipNode.children()) {
			if (child.getTag().equals("instance_animation")) {
				instanceAnims.add(child.getAttrib("url"));
			}
		}
	}

	private void processAnimations() {
		XmlNode libAnims = _colladaNode.findChildTag("library_animations", false);
		if (libAnims == null)
			return; // No animations

		for (XmlNode child : libAnims.children()) {
			if (child.getTag().equals("animation")) {
				processAnimation(child, "default");
			}
		}
	}

	private void processAnimation(XmlNode animation, String actionName) {
		// See if this animation has been instanced in a clip, if so that clips name is the action name
		String animID = animation.getFragID();

		if (animID != null) {
			for (Map.Entry<String, ArrayList<String>> entry : _animClips.entrySet()) {
				if (entry.getValue().contains("#" + animID)) {
					actionName = entry.getKey();
				}
			}
		}

		for (XmlNode child : animation.children()) {
			if (child.getTag().equals("animation")) {
				processAnimation(child, actionName); // Recurse through child animations
			}
			if (child.getTag().equals("sampler")) {
				processSampler(child);

			}
			if (child.getTag().equals("channel")) {
				processChannel(child, actionName);

			}
		}
	}

	private void processSampler(XmlNode samplerNode) {
		AnimSampler sampler = new AnimSampler();
		String id = samplerNode.getFragID();
		for (XmlNode child : samplerNode.children()) {
			if (!child.getTag().equals("input"))
				continue;

			String semantic = child.getAttrib("semantic");
			String source = child.getAttrib("source");

			if (semantic.equals("INPUT"))
				sampler.inputSource = source;
			if (semantic.equals("OUTPUT"))
				sampler.outputSource = source;
			if (semantic.equals("IN_TANGENT"))
				sampler.inTangentSource = source;
			if (semantic.equals("OUT_TANGENT"))
				sampler.outTangentSource = source;
			if (semantic.equals("INTERPOLATION"))
				sampler.interpSource = source;
		}
		_samplers.put(id, sampler);
	}

	private void processChannel(XmlNode channelNode, String actionName) {
		String source = channelNode.getAttrib("source");
		String target = channelNode.getAttrib("target");

		parseAssert(source.charAt(0) == '#');
		AnimSampler sampler = _samplers.get(source.substring(1));
		parseAssert(sampler != null);

		AnimChannel c = new AnimChannel();
		c.curve = buildAnimCurve(sampler);
		c.target = target;
		c.actionName = actionName;
		_animChannels.add(c);
	}

	private void processImages() {
		XmlNode libImage = _colladaNode.findChildTag("library_images", false);
		if (libImage == null)
			return; // No images

		for (XmlNode child : libImage.children()) {
			if (child.getTag().equals("image")) {
				processImage(child);
			}
		}
	}

	private void processImage(XmlNode imageNode) {
		// For now all we care about with images is the init_form contents and the name
		String id = imageNode.getFragID();
		if (id == null) return; // We do not care about images we can not reference

		XmlNode initFrom = imageNode.findChildTag("init_from", true);
		if (initFrom == null) {
			parseAssert(false);
			return;
		}

		String fileName = (String)initFrom.getContent();
		parseAssert(fileName != null);

		_images.put(id, fileName);
	}

	// According to the spec, image nodes can be in a lot of places
	private void scanNodeForImages(XmlNode node) {
		for (XmlNode child : node.children()) {
			if (child.getTag().equals("image")) {
				processImage(child);
			}
		}
	}

	private void processMaterials() {
		XmlNode libMats = _colladaNode.findChildTag("library_materials", false);
		if (libMats == null)
			return; // No materials

		for (XmlNode child : libMats.children()) {
			if (child.getTag().equals("material")) {
				processMaterial(child);
			}
		}
	}

	private void processMaterial(XmlNode matNode) {
		String id = matNode.getFragID();
		if (id == null) return; // We do not care about materials we can not reference

		XmlNode instEffect = matNode.findChildTag("instance_effect", true);
		if (instEffect == null) {
			parseAssert(false);
			return;
		}

		String effectURL = instEffect.getAttrib("url");
		if (effectURL == null) {
			parseAssert(false);
			return;
		}

		_materials.put(id, effectURL);
	}

	private void processEffects() {
		XmlNode libEffects = _colladaNode.findChildTag("library_effects", false);
		if (libEffects == null)
			return; // No effects

		for (XmlNode child : libEffects.children()) {
			if (child.getTag().equals("effect")) {
				processEffect(child);
			}
		}
	}

	private void processEffect(XmlNode effectNode) {
		String id = effectNode.getFragID();
		if (id == null) return; // We do not care about materials we can not reference

		scanNodeForImages(effectNode);

		XmlNode profCommon = effectNode.findChildTag("profile_COMMON", true);
		if (profCommon == null) {
			parseAssert(false);
			return; // There is no common profile
		}

		scanNodeForImages(profCommon);

		HashMap<String, XmlNode> paramMap = new HashMap<>();

		// Start by building a table of all params
		for (XmlNode child : profCommon.children()) {
			String tag = child.getTag();
			if (tag.equals("newparam")) {
				String sid = child.getAttrib("sid");
				if (sid != null) paramMap.put(sid, child);
			}
		}

		XmlNode technique = profCommon.findChildTag("technique", false);
		if (technique == null) {
			parseAssert(false);
			return; // There is no common profile
		}

		scanNodeForImages(technique);

		// Search technique for the kind of data we care about, for now find blinn, phong or lambert
		XmlNode diffuse = null;
		XmlNode ambient = null;
		XmlNode spec = null;
		XmlNode shininess = null;
		XmlNode transparency = null;
		XmlNode transparent = null;

		XmlNode blinn = technique.findChildTag("blinn", false);
		XmlNode phong = technique.findChildTag("phong", false);
		XmlNode lambert = technique.findChildTag("lambert", false);
		XmlNode constant = technique.findChildTag("constant", false);
		if (blinn != null) {
			diffuse = blinn.findChildTag("diffuse", false);
			ambient = blinn.findChildTag("ambient", false);
			spec = blinn.findChildTag("specular", false);
			shininess = blinn.findChildTag("shininess", false);
			transparency = blinn.findChildTag("transparency", false);
			transparent = blinn.findChildTag("transparent", false);
		}
		if (phong != null) {
			diffuse = phong.findChildTag("diffuse", false);
			ambient = phong.findChildTag("ambient", false);
			spec = phong.findChildTag("specular", false);
			shininess = phong.findChildTag("shininess", false);
			transparency = phong.findChildTag("transparency", false);
			transparent = phong.findChildTag("transparent", false);
		}
		if (lambert != null) {
			diffuse = lambert.findChildTag("diffuse", false);
			ambient = lambert.findChildTag("ambient", false);
			transparency = lambert.findChildTag("transparency", false);
			transparent = lambert.findChildTag("transparent", false);
		}
		if (constant != null) {
			diffuse = constant.findChildTag("emission", false);
			transparency = constant.findChildTag("transparency", false);
			transparent = constant.findChildTag("transparent", false);
		}

		// Now either parse diffuse as a color value or texture...
		Effect effect = new Effect();

		ColorTex diffuseCT = null;
		if (diffuse == null) {
			diffuseCT = new ColorTex();
			diffuseCT.color = new Color4d();
		} else {
			diffuseCT = getColorTex(diffuse, paramMap);
		}

		effect.diffuse = diffuseCT;
		effect.spec = getColor(spec);
		effect.ambient = getColor(ambient);
		effect.shininess = getFloat(shininess, 1.0);


		double alpha = 1.0;
		if (transparency != null) {
			XmlNode floatNode = transparency.findChildTag("float", false);
			parseAssert(floatNode != null);
			double[] floats = (double[])floatNode.getContent();
			parseAssert(floats != null && floats.length >= 1);
			alpha = floats[0];
		}

		String opaque = null;
		ColorTex transparentCT = null;
		if (transparent != null) {
			opaque = transparent.getAttrib("opaque");
			if (opaque != null)
			{
				parseAssert((opaque.equals("A_ONE") || opaque.equals("RGB_ZERO")));
			}
			transparentCT = getColorTex(transparent, paramMap);
		}

		if (opaque == null) {
			opaque = "A_ONE";
			if (alpha == 0.0) {
				// This model did not specify a transparency mode and has an alpha of 0
				// This should lead to a completely transparent material, but this is an encountered bug
				// in sketchup exported models, so we will assume they want it to be opaque
				alpha = 1.0;
			}
		}

		// There is a ton of conditions for us to handle transparency
		if (transparentCT != null) {

			if (transparentCT.color != null) {
				effect.transColour = new Color4d(transparentCT.color);
				// Solid transparent color
				if (opaque.equals("A_ONE")) {
					effect.transType = MeshData.A_ONE_TRANS;
				}
				if (opaque.equals("RGB_ZERO")) {
					effect.transType = MeshData.RGB_ZERO_TRANS;
					// Handle the weird luminance term for alpha in RGB_ZERO
					effect.transColour.a = effect.transColour.r * 0.212671 +
					                       effect.transColour.g * 0.715160 +
					                       effect.transColour.b * 0.072169;
				}
				// Bake the transparency term into the colour
				effect.transColour.r *= alpha;
				effect.transColour.g *= alpha;
				effect.transColour.b *= alpha;
				effect.transColour.a *= alpha;

				if ((effect.transColour.a >= 0.999 && opaque.equals("A_ONE")) ||
				    (effect.transColour.a <= 0.001 && opaque.equals("RGB_ZERO")) ) {
					effect.transType = MeshData.NO_TRANS; // Some meshes are effectively not transparent despite having the information
				}
			} else {
				// Transparent texture, we only support a very limited sub set of possible collada alpha mapping,
				// specifically only if the texture used is the alpha channel of the diffuse texture

				// We do not support trasparent textures and RGB_ZERO mode
				parseAssert(opaque.equals("A_ONE"));
				parseAssert(alpha == 1.0); // We do not support variable transparency with transparent textures

				parseAssert(transparentCT.texture != null);
				parseAssert(transparentCT.texture.equals(effect.diffuse.texture));
				effect.transType = MeshData.DIFF_ALPHA_TRANS;
			}
		} else {
			effect.transType = MeshData.NO_TRANS;
		}

		_effects.put(id,  effect);
	}

	// Parse a Color4d from the xml node
	private Color4d getColor(XmlNode node) {
		if (node == null) return new Color4d();

		if (node.getNumChildren() != 1) {
			parseAssert(false);
			return null;
		}

		XmlNode valNode = node.getChild(0);

		String tag = valNode.getTag();
		if (!tag.equals("color")) {
			return null;
		}

		double[] colVals = (double[])valNode.getContent();
		parseAssert(colVals != null && colVals.length >= 4);
		Color4d col = new Color4d(colVals[0], colVals[1], colVals[2], colVals[3]);
		return col;
	}

	// Parse a floating point value from the xml node
	private double getFloat(XmlNode node, double def) {
		if (node == null) return def;

		if (node.getNumChildren() != 1) {
			parseAssert(false);
			return def;
		}

		XmlNode valNode = node.getChild(0);

		String tag = valNode.getTag();
		parseAssert(tag.equals("float"));

		double[] vals = (double[])valNode.getContent();
		parseAssert(vals != null && vals.length >= 1);
		return vals[0];
	}

	private ColorTex getColorTex(XmlNode node, HashMap<String, XmlNode> paramMap) {

		// Use the first child entry
		// (ignore the duplicated <texture> entries added by the FBX Collada exporter)
		XmlNode valNode = node.getChild(0);

		String tag = valNode.getTag();
		ColorTex ret = new ColorTex();
		if (tag.equals("color")) {

			double[] colVals = (double[])valNode.getContent();
			parseAssert(colVals != null && colVals.length >= 4);
			Color4d col = new Color4d(colVals[0], colVals[1], colVals[2], colVals[3]);
			ret.color = col;
			return ret;
		}

		if (!tag.equals("texture")) {
			parseAssert(false);
			return null;
		}

		// Now we have the fun dealing with COLLADA's incredible indirectness
		String texName = valNode.getAttrib("texture");

		String texCoord = valNode.getAttrib("texcoord");

		// Find this sampler in the map
		// If no sampler is found, the image name defaults to the texture name
		String imageName = texName;
		XmlNode sampler = paramMap.get(texName);
		if (sampler != null) {
			XmlNode sampler2D = sampler.findChildTag("sampler2D", false);
			parseAssert(sampler2D != null);
			XmlNode source = sampler2D.findChildTag("source", false);
			parseAssert(source != null);

			String surfaceName = (String)source.getContent();

			XmlNode surfaceParam = paramMap.get(surfaceName);
			XmlNode surface = surfaceParam.findChildTag("surface", false);
			parseAssert(surface != null);
			parseAssert(surface.getAttrib("type").equals("2D"));

			XmlNode initFrom = surface.findChildTag("init_from", false);
			parseAssert(initFrom != null);

			imageName = (String)initFrom.getContent();
		}

		String img = _images.get(imageName);
		parseAssert(img != null);
		try {
			ret.texture = new URL(_contextURL, img).toURI();
			ret.relTexture = img;
			ret.texCoordName = texCoord;
		} catch (MalformedURLException ex) {
			LogBox.renderLogException(ex);
			parseAssert(false);
		} catch (URISyntaxException ex) {
			LogBox.renderLogException(ex);
			parseAssert(false);
		}
		return ret;
	}

	private void processGeos() {
		XmlNode libGeo = _colladaNode.findChildTag("library_geometries", false);
		if (libGeo == null)
			return; // No geometries

		for (XmlNode child : libGeo.children()) {
			if (child.getTag().equals("geometry")) {
				processGeo(child);
			}
		}
	}

	private void processGeo(XmlNode geoNode) {
		String geoID = geoNode.getFragID();
		if (geoID == null) {
			// This geometry can not be referenced, don't bother
			return;
		}

		Geometry geoData = new Geometry();

		for (XmlNode meshNode : geoNode.children()) {
			if (meshNode.getTag().equals("mesh")) {
				parseMesh(meshNode, geoData);
			}
		}

		_geos.put(geoID, geoData);
	}

	private void processNodes() {
		XmlNode libNodes = _colladaNode.findChildTag("library_nodes", false);
		if (libNodes == null)
			return; // No images

		for (XmlNode child : libNodes.children()) {
			if (child.getTag().equals("node")) {
				processNode(child, null);
			}
		}
	}

	private SceneNode processNode(XmlNode node, SceneNode parent) {
		SceneNode sn = new SceneNode(node.getFragID(), node.getAttrib("sid"));

		if (sn.id != null) _namedNodes.put(sn.id, sn);

		if (parent != null) {
			parent.subNodes.add(sn);
		}

		// Build up the transformation matrix for this node
		for (XmlNode child : node.children()) {
			String childTag = child.getTag();

			if (childTag.equals("translate")) {
				sn.transforms.add(new TranslationTrans(child));
			}
			if (childTag.equals("rotate")) {
				sn.transforms.add(new RotationTrans(child));
			}
			if (childTag.equals("scale")) {
				sn.transforms.add(new ScaleTrans(child));
			}
			if (childTag.equals("matrix")) {
				sn.transforms.add(new MatrixTrans(child));
			}
		}

		// Now handle sub geometry, sub nodes and instance nodes
		for (XmlNode child : node.children()) {
			String childTag = child.getTag();
			if (childTag.equals("instance_geometry")) {
				GeoInstInfo geoInfo = processInstGeo(child);
				sn.subGeo.add(geoInfo);
			}
			if (childTag.equals("instance_controller")) {
				SceneNode contNode = processInstController(child);
				sn.subNodes.add(contNode);
			}
			if (childTag.equals("instance_node")) {
				String nodeID = child.getAttrib("url");
				parseAssert(nodeID != null);
				sn.subInstanceNames.add(nodeID);
			}
			if (childTag.equals("node")) {
				processNode(child, sn);
			}
		}
		return sn;
	}

	private SceneNode processInstController(XmlNode instCont) {
		String controllerURL = instCont.getAttrib("url");

		parseAssert(controllerURL.charAt(0) == '#');

		Controller cont = _controllers.get(controllerURL.substring(1));

		GeoInstInfo instInfo = new GeoInstInfo();
		instInfo.geoName = cont.geometry;
		XmlNode bindMat = instCont.findChildTag("bind_material", false);

		if (bindMat != null) {
			addMatInfoToGeoInst(bindMat, instInfo);
		}

		// Now we need to add in an extra scene not to accommodate the bind space matrix held in the controller
		SceneNode sn = new SceneNode(null, null);

		sn.transforms.add(new MatrixTrans(cont.bindSpaceMat));
		sn.subGeo.add(instInfo);

		return sn;
	}

	private GeoInstInfo processInstGeo(XmlNode instGeo) {
		GeoInstInfo instInfo = new GeoInstInfo();
		instInfo.geoName = instGeo.getAttrib("url");
		XmlNode bindMat = instGeo.findChildTag("bind_material", false);

		if (bindMat != null) {
			addMatInfoToGeoInst(bindMat, instInfo);
		}

		return instInfo;
	}

	private void addMatInfoToGeoInst(XmlNode bindMat, GeoInstInfo instInfo) {

		XmlNode techCommon = bindMat.findChildTag("technique_common", false);
		parseAssert(techCommon != null);

		for (XmlNode instMat : techCommon.children()) {
			if (!instMat.getTag().equals("instance_material")) {
				continue;
			}
			String symbol = instMat.getAttrib("symbol");
			String target = instMat.getAttrib("target");
			parseAssert(symbol != null && target != null);
			instInfo.materialMap.put(symbol, target);

			Effect eff = getEffectForMat(target);
			// Make sure that if the asset is requesting a particular texture coordinate set, that it's set 0 (the only one we support)
			for (XmlNode sub : instMat.children()) {
				if (!sub.getTag().equals("bind_vertex_input")) {
					continue;
				}

				if (eff.diffuse.texture == null) {
					// This effect does not use a diffuse texture, we do not care
					continue;
				}

				// Find the texcoord we want for this material
				String texCoordName = eff.diffuse.texCoordName;
				String semantic = sub.getAttrib("semantic");

				if (texCoordName == null || !texCoordName.equals(semantic)) {
					// We don't care about this binding
					continue;
				}

				int texSet = 0;
				String texSetString = sub.getAttrib("input_set");
				if (texSetString != null) {
					texSet = Integer.parseInt(texSetString);
				}

				if (instInfo.usedTexSet != null) {
					// For now only one texture set can be used per instance
					parseAssert(instInfo.usedTexSet == texSet);
				}

				instInfo.usedTexSet = texSet;
			}
		}
	}

	private void parseMesh(XmlNode mesh, Geometry geoData) {

		// Now try to parse geometry type
		for (XmlNode subGeo : mesh.children()) {
			String geoTag = subGeo.getTag();
			if (geoTag.equals("polylist") ||
			    geoTag.equals("polygons") ||
			    geoTag.equals("triangles")) {

				generateTriangleGeo(subGeo, geoData);
			}

			if (geoTag.equals("lines") ||
			    geoTag.equals("linestrip")) {

				generateLineGeo(subGeo, geoData);
			}

		}
	}

	private void generateLineGeo(XmlNode subGeo, Geometry geoData) {
		String geoTag = subGeo.getTag();

		SubMeshDesc smd = readGeometryInputs(subGeo);

		if (geoTag.equals("lines")) {
			parseLines(smd, subGeo);
		}
		if (geoTag.equals("linestrip")) {
			parseLinestrip(smd, subGeo);
		}

		int numVerts = smd.posDesc.indices.length;
		parseAssert(numVerts % 2 == 0);

		// Now the SubMeshDesc should be fully populated, and we can actually produce the final triangle arrays
		LineSubGeo lsg = new LineSubGeo(numVerts);

		Vec4d[] posData = getVec4dArrayFromSource(smd.posDesc.source);

		lsg.materialSymbol = subGeo.getAttrib("material");
		if (lsg.materialSymbol == null) {
			lsg.materialSymbol = DEFAULT_MATERIAL_NAME;
		}

		for (int i = 0; i < numVerts; ++i) {
			lsg.verts[i] = posData[smd.posDesc.indices[i]];
			lsg.verts[i].w = 1;
		}
		geoData.lineSubGeos.add(lsg);

	}

	private Vec3d generateNormal(Vec4d p0, Vec4d p1, Vec4d p2, Vec4d t0, Vec4d t1) {
		t0.sub3(p1, p0);
		t1.sub3(p2, p0);
		Vec3d norm = new Vec3d();
		norm.cross3(t0, t1);
		norm.normalize3();
		return norm;
	}

	private void generateTriangleGeo(XmlNode subGeo, Geometry geoData) {
		SubMeshDesc smd = getSubMeshDesc(subGeo, geoData);

		if (smd.posDesc.indices.length == 0) {
			return; // There is no actual geometry here
		}

		String material = subGeo.getAttrib("material");
		if (material == null) {
			material = DEFAULT_MATERIAL_NAME;
		}

		smd.material = material;

		geoData.faceSubDescs.add(smd);
	}

	private SubMeshDesc getSubMeshDesc(XmlNode subGeo, Geometry geoData) {
		String geoTag = subGeo.getTag();

		SubMeshDesc smd = readGeometryInputs(subGeo);

		if (geoTag.equals("triangles")) {
			parseTriangles(smd, subGeo);
		}
		if (geoTag.equals("polylist")) {
			parsePolylist(smd, subGeo);
		}
		if (geoTag.equals("polygons")) {
			parsePolygons(smd, subGeo);
		}

		return smd;
	}

	private FaceSubGeo getFaceSubGeo(SubMeshDesc smd) {
		boolean hasNormal = smd.normDesc != null;

		int numVerts = smd.posDesc.indices.length;
		parseAssert(numVerts % 3 == 0);
		if (numVerts == 0) {
			return null;
		}

		// Now the SubMeshDesc should be fully populated, and we can actually produce the final triangle arrays
		FaceSubGeo fsg = new FaceSubGeo(numVerts);

		Vec4d[] posData = getVec4dArrayFromSource(smd.posDesc.source);

		Vec4d[] normData = null;
		if (hasNormal) {
			normData = getVec4dArrayFromSource(smd.normDesc.source);
		}

		boolean hasTexCoords = false;

		DataDesc texSetDesc = null;
		Vec4d[] texCoordData = null;
		if (smd.usedTexSet != null) {
			texSetDesc = smd.texCoordMap.get(smd.usedTexSet);
			if (texSetDesc != null) {
				hasTexCoords = true;
				texCoordData = getVec4dArrayFromSource(texSetDesc.source);
			}
		}

		Vec4d t0 = new Vec4d();
		Vec4d t1 = new Vec4d();

		Vec3d[] generatedNormals = null;
		if (!hasNormal) {
			// Generate one normal per face
			generatedNormals = new Vec3d[numVerts/3];
			for (int i = 0; i < numVerts / 3; ++i) {
				Vec4d p0 = posData[smd.posDesc.indices[i*3 + 0]];
				Vec4d p1 = posData[smd.posDesc.indices[i*3 + 1]];
				Vec4d p2 = posData[smd.posDesc.indices[i*3 + 2]];
				Vec3d norm = generateNormal(p0, p1, p2, t0, t1);
				generatedNormals[i] = norm;
			}
		}

		for (int i = 0; i < numVerts; ++i) {
			Vec3d pos = new Vec3d(posData[smd.posDesc.indices[i]]);

			Vec3d normal = null;
			if (hasNormal) {
				// Make sure the normal is actually present, treat negative indices as missing normals
				int normInd = smd.normDesc.indices[i];
				if (normInd < 0) {
					// We need to generate one
					int triInd = i/3;
					Vec4d p0 = posData[smd.posDesc.indices[triInd*3 + 0]];
					Vec4d p1 = posData[smd.posDesc.indices[triInd*3 + 1]];
					Vec4d p2 = posData[smd.posDesc.indices[triInd*3 + 2]];
					normal = generateNormal(p0, p1, p2, t0, t1);
				}
				else {
					normal = new Vec3d(normData[normInd]);
				}
			} else {
				normal = generatedNormals[i/3];
			}

			Vec2d texCoord = null;
			if (hasTexCoords) {
				texCoord = new Vec2d(texCoordData[texSetDesc.indices[i]]);
			}
			fsg.indices[i] = fsg.vMap.getVertIndex(pos, normal, texCoord);
		}

		return fsg;
	}

	private void readVertices(SubMeshDesc smd, int offset, XmlNode vertices) {
		// Check vertices for inputs
		for (XmlNode input : vertices.children()) {
			if (input.getTag() != "input") {
				continue;
			}

			String semantic = input.getAttrib("semantic");
			String source = input.getAttrib("source");
			if (source == null || semantic == null)
				throw new ColException("Bad Vertex Input tag: " + input.getFragID() + " in mesh.");

			if (semantic.equals("POSITION")) {
				smd.posDesc = new DataDesc();
				smd.posDesc.source = source;
				smd.posDesc.offset = offset;
			}
			if (semantic.equals("NORMAL")) {
				smd.normDesc = new DataDesc();
				smd.normDesc.source = source;
				smd.normDesc.offset = offset;
			}
		}
	}

	private void parseLines(SubMeshDesc smd, XmlNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		XmlNode pNode = subGeo.findChildTag("p", false);
		if (pNode == null)
			throw new ColException("No 'p' child in 'lines' in mesh.");

		int[] ps = (int[])pNode.getContent();

		if (ps.length < count * 2 * smd.stride) {
			// Collada error, there is not enough indices for the apparent count, but we will play along all the same
			count = ps.length / (2 * smd.stride);
		}

		smd.posDesc.indices = new int[count*2];
		for (int i = 0; i < count * 2; ++i) {
			int offset = i * smd.stride;
			smd.posDesc.indices[i] = ps[offset + smd.posDesc.offset];
		}
	}

	private void parseLinestrip(SubMeshDesc smd, XmlNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		// There should be 'count' number of 'p' tags in this element
		int[][] stripIndices = new int[count][];
		int numLines = 0;
		int nextIndex = 0;
		for (XmlNode child : subGeo.children()) {
			if (!child.getTag().equals("p")) continue;

			int[] ps = (int[])child.getContent();
			parseAssert(ps != null);
			parseAssert(nextIndex < count);

			stripIndices[nextIndex++] = ps;
			numLines += ps.length - 1;
		}

		// We now have a list of list of all indices, split this into lines
		int nextWriteIndex = 0;
		smd.posDesc.indices = new int[numLines * 2];
		for (int[] strip : stripIndices) {
			parseAssert(strip.length >= 2);

			for (int i = 1; i < strip.length; ++i) {
				smd.posDesc.indices[nextWriteIndex++] = strip[i-1];
				smd.posDesc.indices[nextWriteIndex++] = strip[i];
			}
		}
	}

	// Fill in the indices for 'smd'
	private void parseTriangles(SubMeshDesc smd, XmlNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		XmlNode pNode = subGeo.findChildTag("p", false);
		if (count == 0) {
			smd.posDesc.indices = new int[0];
			return;
		}
		if (pNode == null)
			throw new ColException("No 'p' child in 'triangles' in mesh.");

		int[] ps = (int[])pNode.getContent();
		parseAssert(ps.length >= count * 3 * smd.stride);

		smd.posDesc.indices = new int[count*3];
		if (smd.normDesc != null) {
			smd.normDesc.indices = new int[count*3];
		}

		for (DataDesc texDesc : smd.texCoordMap.values()) {
			texDesc.indices = new int[count*3];
		}

		for (int i = 0; i < count * 3; ++i) {
			int offset = i * smd.stride;
			smd.posDesc.indices[i] = ps[offset + smd.posDesc.offset];
			if (smd.normDesc != null) {
				smd.normDesc.indices[i] = ps[offset + smd.normDesc.offset];
			}
			for (DataDesc texDesc : smd.texCoordMap.values()) {
				texDesc.indices[i] = ps[offset + texDesc.offset];
			}
		}
	}

	// Note, this is definitely not correct, but for now assume all polygons are convex
	private void parsePolylist(SubMeshDesc smd, XmlNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		XmlNode pNode = subGeo.findChildTag("p", false);
		if (pNode == null)
			throw new ColException("No 'p' child in 'polygons' in mesh.");

		int[] ps = (int[])pNode.getContent();

		XmlNode vcountNode = subGeo.findChildTag("vcount", false);
		int[] vcounts;
		if (vcountNode != null)
			vcounts = (int[])vcountNode.getContent();
		else
			vcounts = new int[0];

		parseAssert(vcounts.length == count);
		int totalVerts = 0;
		int numTriangles = 0;
		for (int i : vcounts) {
			if (i == 0) {
				continue;
			}
			totalVerts += i;
			numTriangles += (i-2);
		}
		parseAssert(ps.length >= totalVerts * smd.stride);

		smd.posDesc.indices = new int[numTriangles * 3];
		if (smd.normDesc != null) {
			smd.normDesc.indices = new int[numTriangles * 3];
		}

		for (DataDesc texDesc : smd.texCoordMap.values()) {
			texDesc.indices = new int[numTriangles*3];
		}

		int nextWriteVert = 0;
		int readVertOffset = 0;
		for (int v : vcounts) {
			if (v == 0) {
				continue;
			}
			// v is the number of vertices in this polygon
			parseAssert(v >= 3);
			for (int i = 0; i < (v-2); ++i) {
				int vert0 = readVertOffset + i;
				int vert1 = readVertOffset + i + 1;
				int vert2 = readVertOffset + v - 1;

				smd.posDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;
			}
			readVertOffset += v;
		}
	}

	// Note, this is definitely not correct, but for now assume all polygons are convex
	private void parsePolygons(SubMeshDesc smd, XmlNode subGeo) {

		int numTriangles = 0;

		// Find the number of triangles, for this we will need to iterate over all the polygons
		for (XmlNode n : subGeo.children()) {
			// Note: we do not support 'ph' tags (polygons with holes)
			if (!n.getTag().equals("p")) {
				continue;
			}
			int[] ps = (int[])n.getContent();
			int numVerts = ps.length / smd.stride;
			parseAssert( (ps.length % smd.stride) == 0);
			parseAssert(numVerts >= 3);
			numTriangles += numVerts - 2;
		}

		smd.posDesc.indices = new int[numTriangles * 3];
		if (smd.normDesc != null) {
			smd.normDesc.indices = new int[numTriangles * 3];
		}

		for (DataDesc texDesc : smd.texCoordMap.values()) {
			texDesc.indices = new int[numTriangles*3];
		}

		int nextWriteVert = 0;

		for (XmlNode n : subGeo.children()) {
			// Note: we do not support 'ph' tags (polygons with holes)
			if (!n.getTag().equals("p")) {
				continue;
			}
			int[] ps = (int[])n.getContent();
			for(int i = 0; i < (ps.length / smd.stride) - 2; ++i) {
				int vert0 = 0;
				int vert1 = i + 1;
				int vert2 = i + 2;

				smd.posDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.normDesc.offset];
				}
				for (DataDesc texDesc : smd.texCoordMap.values()) {
					texDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + texDesc.offset];
				}
				nextWriteVert++;
			}
		}
	}

	private SubMeshDesc readGeometryInputs(XmlNode subGeo) {
		SubMeshDesc smd = new SubMeshDesc();

		int maxOffset = 0;
		for (XmlNode input : subGeo.children()) {
			if (!input.getTag().equals("input")) {
				continue;
			}
			String semantic = input.getAttrib("semantic");
			String source = input.getAttrib("source");
			if (source == null || semantic == null)
				throw new ColException("Bad Geometry Input tag: " + input.getFragID());
			int offset = Integer.parseInt(input.getAttrib("offset"));

			if (offset > maxOffset) { maxOffset = offset; }

			if (semantic.equals("VERTEX")) {
				XmlNode vertices = getNodeFromID(source);
				readVertices(smd, offset, vertices);
			}
			if (semantic.equals("NORMAL")) {
				smd.normDesc = new DataDesc();
				smd.normDesc.source = source;
				smd.normDesc.offset = offset;
			}

			String setString = input.getAttrib("set");
			int set = 0;

			if (setString != null)
				set = Integer.parseInt(setString);

			if (semantic.equals("TEXCOORD") || semantic.equals("TEXCOORD0")) {
				DataDesc texDesc = new DataDesc();
				texDesc.source = source;
				texDesc.offset = offset;
				smd.texCoordMap.put(set, texDesc);
			}
		}

		if (smd.posDesc == null) {
			throw new ColException("Could not find positions for mesh.");
		}

		smd.stride = maxOffset+1;

		return smd;
	}

	private static class SourceInfo {
		int stride;
		int offset;
		int count;
		int dataCount;
		Object dataArray;
	}
	private SourceInfo getInfoFromSource(String id, String arrayName) {

		SourceInfo ret = new SourceInfo();

		// Okay, this source hasn't be accessed yet
		XmlNode sourceNode = getNodeFromID(id);
		if (sourceNode == null) { throw new ColException("Could not find node with id: " + id); }

		XmlNode dataNode = sourceNode.findChildTag(arrayName, false);
		if (dataNode == null) { throw new ColException("No float array in source: " + id); }

		ret.dataCount = Integer.parseInt(dataNode.getAttrib("count"));
		ret.dataArray = dataNode.getContent();

		XmlNode techCommon = sourceNode.findChildTag("technique_common", false);
		if (techCommon == null) { throw new ColException("No technique_common in source: " + id); }

		XmlNode accessor = techCommon.findChildTag("accessor", false);
		if (accessor == null) { throw new ColException("No accessor in source: " + id); }

		ret.stride = Integer.parseInt(accessor.getAttrib("stride"));
		ret.count = Integer.parseInt(accessor.getAttrib("count"));
		String offsetString = accessor.getAttrib("offset");
		if (offsetString == null) {
			ret.offset = 0;
		} else {
			ret.offset = Integer.parseInt(offsetString);
		}

		parseAssert(ret.dataCount >= ret.count * ret.stride);

		return ret;
	}

	/**
	 * Return a meaningful list of Vectors from data source 'id'
	 * @param id
	 * @return
	 */
	private Vec4d[] getVec4dArrayFromSource(String id) {
		Vec4d[] cached = _vec4dSources.get(id);
		if (cached != null)
			return cached;

		double[][] data = getDataArrayFromSource(id);

		// convert to vec4ds
		Vec4d[] ret = new Vec4d[data.length];

		for (int i = 0; i < data.length; ++i) {
			double[] ds = data[i];
			switch (ds.length) {
			case 1:
				ret[i] = new Vec4d(ds[0], 0, 0, 1);
				break;
			case 2:
				ret[i] = new Vec4d(ds[0], ds[1], 0, 1);
				break;
			case 3:
				ret[i] = new Vec4d(ds[0], ds[1], ds[2], 1);
				break;
			case 4:
				ret[i] = new Vec4d(ds[0], ds[1], ds[2], ds[3]);
				break;
			default:
				throw new RenderException(String.format("Invalid number of elements in data Vector: %d", ds.length));
			}
		}
		_vec4dSources.put(id, ret);
		return ret;
	}

	private double[][] getDataArrayFromSource(String id) {

		// First check the cache
		double[][] cached = _dataSources.get(id);
		if (cached != null) {
			return cached;
		}

		SourceInfo info = getInfoFromSource(id, "float_array");

		double[][] ret = new double[info.count][];

		double[] values = null;
		try {
			values = (double[])info.dataArray;
		} catch (ClassCastException ex) {
			parseAssert(false);
		}

		int valueOffset = info.offset;
		for (int i = 0; i < info.count; ++i) {
			ret[i] = new double[info.stride];
			for (int j = 0; j < info.stride; j++) {
				ret[i][j] = values[valueOffset + j];
			}

			valueOffset += info.stride;
		}

		_dataSources.put(id, ret);

		return ret;
	}

	private String[] getStringArrayFromSource(String id) {
		// First check the cache
		String[] cached = _stringSources.get(id);
		if (cached != null) {
			return cached;
		}

		SourceInfo info = getInfoFromSource(id, "Name_array");

		String[] ret = new String[info.count];

		String[] values = null;
		try {
			values = (String[])info.dataArray;
		} catch (ClassCastException ex) {
			parseAssert(false);
		}

		int valueOffset = info.offset;
		for (int i = 0; i < info.count; ++i) {

			ret[i] = values[valueOffset];
			valueOffset += info.stride;
		}

		_stringSources.put(id, ret);

		return ret;
	}

	private AnimCurve buildAnimCurve(AnimSampler samp) {
		AnimCurve.ColCurve colData = new AnimCurve.ColCurve();

		double[][] inTemp = getDataArrayFromSource(samp.inputSource);
		colData.in = new double[inTemp.length];
		for (int i = 0; i < inTemp.length; ++i) {
			colData.in[i] = inTemp[i][0];
		}

		colData.out =    getDataArrayFromSource(samp.outputSource);
		colData.interp = getStringArrayFromSource(samp.interpSource);

		SourceInfo outInfo = getInfoFromSource(samp.outputSource, "float_array");
		colData.numComponents = outInfo.stride;

		if (samp.inTangentSource != null) {
			colData.inTan =  getDataArrayFromSource(samp.inTangentSource);
		}
		if (samp.outTangentSource != null) {
			colData.outTan = getDataArrayFromSource(samp.outTangentSource);
		}

		parseAssert(colData.in.length == colData.out.length);
		parseAssert(colData.in.length == colData.interp.length);
		parseAssert(colData.inTan == null  || colData.in.length == colData.inTan.length);
		parseAssert(colData.outTan == null || colData.in.length == colData.outTan.length);

		AnimCurve ret = AnimCurve.buildFromColCurve(colData);
		parseAssert(ret != null);
		return ret;
	}

	// Scan the scene, find the transform targeted and attach the AnimCurve to that transform
	private void attachChannelToScene(AnimChannel ch) {
		// Start by parsing the target
		int dotInd = ch.target.indexOf('.');
		if (dotInd == -1) dotInd = ch.target.length();
		int braceInd = ch.target.indexOf('(');
		if (braceInd == -1) braceInd = ch.target.length();

		int targetInd = Math.min(dotInd, braceInd);
		String target = ch.target.substring(targetInd);

		String[] pathSegments = ch.target.substring(0, targetInd).split("/");
		parseAssert(pathSegments.length > 0);

		SceneNode rootNode = _namedNodes.get(pathSegments[0]);
		Object currentNode = rootNode;
		for (int i = 1; i < pathSegments.length; ++i) {
			// Now scan the scene tree for the rest of the path
			parseAssert(currentNode instanceof SceneNode);
			SceneNode sn = (SceneNode)currentNode;

			currentNode = findSubNodeBySID(sn, pathSegments[i]);
			parseAssert(currentNode != null);
		}

		// We have now scanned the path and have the final node, use the target to find the actual curve to apply to
		parseAssert(currentNode instanceof SceneTrans);
		// The node at the end of the chain must be a transform
		SceneTrans trans = (SceneTrans)currentNode;
		trans.attachCurve(ch.curve, target, ch.actionName);

	}

	// Perform a breadth first tree scan of this sn for any sub object or Transform with this sid
	// This returns an Object, because valid results are both Transforms and SceneNodes. The caller
	// will need to cast into the appropriate type
	private Object findSubNodeBySID(SceneNode sn, String sid) {
		if (sid.equals(sn.sid)) {
			return sn;
		}
		// Start with the transforms
		for (SceneTrans trans : sn.transforms) {
			if (sid.equals(trans.sid)) {
				return trans;
			}
		}
		// Now the children
		for (SceneNode child : sn.subNodes) {
			if (sid.equals(child.sid)) {
				return child;
			}
		}
		// Now descend
		for (SceneNode child : sn.subNodes) {
			Object childRes = findSubNodeBySID(child, sid);
			if (childRes != null) {
				return childRes;
			}
		}
		return null;
	}

	public MeshData getData() {
		return _finalData;
	}

	/**
	 * This data structure is useful for turning COLLADA data arrays into flat arrays to be processed.
	 * @author matt.chudleigh
	 *
	 */
	private static class DataDesc {
		// The fragID of the source
		public String source;
		// The offset in the index array
		public int offset;
		int[] indices;
	}

	private static class SubMeshDesc {
		public DataDesc posDesc;
		public DataDesc normDesc;
		public HashMap<Integer, DataDesc> texCoordMap = new HashMap<>();
		public Integer usedTexSet = null;
		public int stride;
		public String material;
	}

	/**
	 * Information for a geometry instance (as opposed to a geometry)
	 * for now this is simply a map from material binding symbols to actual material IDs
	 * and a geometry name
	 */
	private static class GeoInstInfo {
		public String geoName;
		public final Map<String, String> materialMap = new HashMap<>();
		public Integer usedTexSet = null;
	}


	private static class AnimAction {
		public AnimCurve[] attachedCurves;
		public int[] attachedIndex;
	}
	/**
	 * Base class for scene node transforms
	 * @author matt.chudleigh
	 *
	 */
	private static abstract class SceneTrans {
		public final String sid;
		protected final Vec3d commonVect;

		protected SceneTrans(String sid) {
			this.sid = sid;
			commonVect = new Vec3d();
		}

		protected HashMap<String, AnimAction> actions = new HashMap<>();

		protected abstract Mat4d getStaticMat();
		protected abstract MeshData.Trans toAnimatedTransform();
		public MeshData.Trans toMeshDataTrans() {
			boolean stat = true;
			for (AnimAction act : actions.values()) {
				for (AnimCurve ac : act.attachedCurves) {
					if (ac != null) {
						stat = false;
					}
				}
			}

			// If there are no attached curves, we can output a static mesh
			if (stat) {
				return new MeshData.StaticTrans(getStaticMat());
			}
			return toAnimatedTransform();
		}


		public abstract void attachCurve(AnimCurve curve, String target, String actionName);

		protected boolean attachCommonCurves(AnimCurve curve, String tar, String actionName) {
			AnimAction act = actions.get(actionName);
			if (tar.equals(".X") || tar.equals("(0)")) {
				act.attachedCurves[0] = curve;
				act.attachedIndex[0] = 0;
				return true;
			}
			if (tar.equals(".Y") || tar.equals("(1)")) {
				act.attachedCurves[1] = curve;
				act.attachedIndex[1] = 0;
				return true;
			}
			if (tar.equals(".Z") || tar.equals("(2)")) {
				act.attachedCurves[2] = curve;
				act.attachedIndex[2] = 0;
				return true;
			}
			return false;
		}

		protected Vec3d getAnimatedVectAtTime(double time, String actionName) {
			Vec3d ret = new Vec3d(commonVect);
			AnimAction act = actions.get(actionName);
			if (act == null) {
				return ret;
			}

			if (act.attachedCurves[0] != null) {
				ret.x = act.attachedCurves[0].getValueForTime(time)[act.attachedIndex[0]];
			}
			if (act.attachedCurves[1] != null) {
				ret.y = act.attachedCurves[1].getValueForTime(time)[act.attachedIndex[1]];
			}
			if (act.attachedCurves[2] != null) {
				ret.z = act.attachedCurves[2].getValueForTime(time)[act.attachedIndex[2]];
			}
			return ret;
		}
		protected double[] getKeyTimes(String actionName) {
			AnimAction act = actions.get(actionName);
			TreeSet<Double> times = new TreeSet<>();
			for (AnimCurve ac : act.attachedCurves) {
				if (ac != null) {
					for (double time : ac.times) {
						times.add(time);
					}
				}
			}
			double[] ret = new double[times.size()];
			int index = 0;
			for (Double time : times) {
				ret[index++] = time;
			}

			// Sanity check
			for (int i = 0; i < ret.length - 1; ++i) {
				parseAssert(ret[0] < ret[1]);
			}
			return ret;
		}

		// Return a list of times with (oversample-1) additional points interspersed in each gap
		protected double[] oversampleTime(double[] originalTimes, int oversample) {
			double[] times = new double[(originalTimes.length-1)*oversample + 1];
			for (int i = 0; i < originalTimes.length - 1; ++i) {

				double cur = originalTimes[i];
				double next = originalTimes[i+1];
				for (int j = 0; j < oversample; ++j) {
					double scale = (double)j / (double)oversample;
					times[i*oversample +j] = cur*(1-scale) + next*scale;
				}
			}
			times[times.length-1] = originalTimes[originalTimes.length-1];

			return times;
		}
	}

	private static class TranslationTrans extends SceneTrans {

		public TranslationTrans(XmlNode transNode) {
			super(transNode.getAttrib("sid"));

			double[] vals = (double[])transNode.getContent();
			parseAssert(vals != null);
			parseAssert(vals.length >= 3);
			commonVect.set3(vals[0], vals[1], vals[2]);
		}

		@Override
		public Mat4d getStaticMat() {
			Mat4d ret = new Mat4d();
			ret.setTranslate3(commonVect);
			return ret;
		}

		@Override
		public void attachCurve(AnimCurve curve, String target, String actionName) {
			AnimAction act = actions.get(actionName);
			if (act == null) {
				// First curve bound for this action
				act = new AnimAction();
				act.attachedCurves = new AnimCurve[3];
				act.attachedIndex = new int[3];
				actions.put(actionName, act);
			}

			String tar = target.toUpperCase();
			if (attachCommonCurves(curve, tar, actionName)) {
				return;
			}

			if (target.equals("")) {
				// For an empty target, attach to all curves
				for (int i = 0; i < 3; ++i) {
					act.attachedCurves[i] = curve;
					act.attachedIndex[i] = i;
				}
				return;
			}
			parseAssert(false);
		}

		@Override
		protected Trans toAnimatedTransform() {
			Set<String> actionNames = actions.keySet();
			double[][] timesArray = new double[actionNames.size()][];
			Mat4d[][] matsArray = new Mat4d[actionNames.size()][];
			String[] names = new String[actionNames.size()];

			int actionInd = 0;
			for (String actionName : actions.keySet()) {
				double[] times = getKeyTimes(actionName);
				Mat4d[] mats = new Mat4d[times.length];
				for (int i = 0; i < times.length; ++i) {
					Vec3d animTranslation = getAnimatedVectAtTime(times[i], actionName);
					mats[i] = new Mat4d();
					mats[i].setTranslate3(animTranslation);
				}

				timesArray[actionInd] = times;
				matsArray[actionInd] = mats;
				names[actionInd] = actionName;

				++actionInd;
			}
			return new MeshData.AnimTrans(timesArray, matsArray, names, getStaticMat());
		}
	}

	private static class RotationTrans extends SceneTrans {
		private final double angle;

		public RotationTrans(XmlNode rotNode) {
			super(rotNode.getAttrib("sid"));
			double[] vals = (double[])rotNode.getContent();
			parseAssert(vals != null);
			parseAssert(vals.length >= 4);

			commonVect.set3(vals[0], vals[1], vals[2]);
			angle = (float)Math.toRadians(vals[3]);
		}

		@Override
		public Mat4d getStaticMat() {

			Quaternion rot = new Quaternion();
			rot.setAxisAngle(commonVect, angle);

			Mat4d ret = new Mat4d();
			ret.setRot3(rot);
			return ret;
		}
		@Override
		public void attachCurve(AnimCurve curve, String target, String actionName) {

			AnimAction act = actions.get(actionName);
			if (act == null) {
				// First curve bound for this action
				act = new AnimAction();
				act.attachedCurves = new AnimCurve[4];
				act.attachedIndex = new int[4];
				actions.put(actionName, act);
			}

			String tar = target.toUpperCase();
			if (attachCommonCurves(curve, tar, actionName)) {
				return;
			}
			if (tar.equals(".ANGLE") || tar.equals("(3)")) {
				act.attachedCurves[3] = curve;
				return;
			}
			if (target.equals("")) {
				// For an empty target, attach to all curves
				for (int i = 0; i < 4; ++i) {
					act.attachedCurves[i] = curve;
					act.attachedIndex[i] = i;
				}
				return;
			}

			parseAssert(false);
		}

		@Override
		protected Trans toAnimatedTransform() {
			Set<String> actionNames = actions.keySet();
			double[][] timesArray = new double[actionNames.size()][];
			Mat4d[][] matsArray = new Mat4d[actionNames.size()][];
			String[] names = new String[actionNames.size()];

			int actionInd = 0;
			for (Entry<String, AnimAction> eachAction : actions.entrySet()) {
				final String actionName = eachAction.getKey();
				double[] originalTimes = getKeyTimes(actionName);
				AnimAction act = eachAction.getValue();

				// Add new sample points because linearly interpolating a rotation matrix usually does not work correctly
				double[] times = oversampleTime(originalTimes, 4);

				Mat4d[] mats = new Mat4d[times.length];
				for (int i = 0; i < times.length; ++i) {
					double animAngle = Math.toRadians(angle);
					if (act.attachedCurves[3] != null) {
						animAngle = Math.toRadians(act.attachedCurves[3].getValueForTime(times[i])[act.attachedIndex[3]]);
					}

					Vec3d animAxis = getAnimatedVectAtTime(times[i], actionName);
					Quaternion rot = new Quaternion();
					rot.setAxisAngle(animAxis, animAngle);
					mats[i] = new Mat4d();
					mats[i].setRot3(rot);
				}
				timesArray[actionInd] = times;
				matsArray[actionInd] = mats;
				names[actionInd] = actionName;

			}

			return new MeshData.AnimTrans(timesArray, matsArray, names, getStaticMat());
		}
	}

	private static class ScaleTrans extends SceneTrans {

		public ScaleTrans(XmlNode scaleNode) {
			super(scaleNode.getAttrib("sid"));

			double[] vals = (double[])scaleNode.getContent();
			parseAssert(vals != null);
			parseAssert(vals.length >= 3);
			commonVect.set3(vals[0], vals[1], vals[2]);
		}

		@Override
		public Mat4d getStaticMat() {

			Mat4d ret = new Mat4d();
			ret.d00 = commonVect.x;
			ret.d11 = commonVect.y;
			ret.d22 = commonVect.z;
			return ret;
		}

		@Override
		public void attachCurve(AnimCurve curve, String target, String actionName) {
			AnimAction act = actions.get(actionName);
			if (act == null) {
				// First curve bound for this action
				act = new AnimAction();
				act.attachedCurves = new AnimCurve[3];
				act.attachedIndex = new int[3];
				actions.put(actionName, act);
			}

			String tar = target.toUpperCase();
			if (attachCommonCurves(curve, tar, actionName)) {
				return;
			}
			if (target.equals("")) {
				// For an empty target, attach to all curves
				for (int i = 0; i < 3; ++i) {
					act.attachedCurves[i] = curve;
					act.attachedIndex[i] = i;
				}
				return;
			}
			parseAssert(false);
		}

		@Override
		protected Trans toAnimatedTransform() {

			Set<String> actionNames = actions.keySet();
			double[][] timesArray = new double[actionNames.size()][];
			Mat4d[][] matsArray = new Mat4d[actionNames.size()][];
			String[] names = new String[actionNames.size()];

			int actionInd = 0;
			for (String actionName : actions.keySet()) {
				double[] times = getKeyTimes(actionName);
				Mat4d[] mats = new Mat4d[times.length];
				for (int i = 0; i < times.length; ++i) {
					Vec3d animScale = getAnimatedVectAtTime(times[i], actionName);
					mats[i] = new Mat4d();
					mats[i].d00 = animScale.x;
					mats[i].d11 = animScale.y;
					mats[i].d22 = animScale.z;
				}
				timesArray[actionInd] = times;
				matsArray[actionInd] = mats;
				names[actionInd] = actionName;
			}
			return new MeshData.AnimTrans(timesArray, matsArray, names, getStaticMat());
		}

	}

	private static class MatrixTrans extends SceneTrans {
		private final Mat4d matrix;

		public MatrixTrans(Mat4d mat) {
			super(null);
			matrix = new Mat4d(mat);
		}

		public MatrixTrans(XmlNode matNode) {
			super(matNode.getAttrib("sid"));
			double[] vals = (double[])matNode.getContent();
			parseAssert(vals != null);
			parseAssert(vals.length >= 16);
			matrix = new Mat4d(vals);
		}

		@Override
		public Mat4d getStaticMat() {
			return new Mat4d(matrix);
		}
		@Override
		public void attachCurve(AnimCurve curve, String target, String actionName) {
			if (!target.equals("") || curve.numComponents != 16) {
				throw new RenderException("Currently only support animating whole matrices");
			}
			AnimAction act = actions.get(actionName);
			if (act != null) {
				// TODO warn we are over-writing an action here
			}

			// We only support whole matrix animation and single curve per action currently, so the last curve
			// bound to an action will dominate
			act = new AnimAction();
			act.attachedCurves = new AnimCurve[1];
			act.attachedCurves[0] = curve;
			actions.put(actionName, act);
		}
		@Override
		protected Trans toAnimatedTransform() {
			Set<String> actionNames = actions.keySet();
			double[][] timesArray = new double[actionNames.size()][];
			Mat4d[][] matsArray = new Mat4d[actionNames.size()][];
			String[] names = new String[actionNames.size()];

			int actionInd = 0;
			for (Entry<String, AnimAction> eachAction : actions.entrySet()) {
				String actionName = eachAction.getKey();
				AnimAction act = eachAction.getValue();

				double[] times = getKeyTimes(actionName);
				Mat4d[] mats = new Mat4d[times.length];
				// Fill in the mats
				for (int i = 0; i < times.length; ++i) {
					double[] matVals = act.attachedCurves[0].getValueForTime(times[i]);
					mats[i] = new Mat4d(matVals);
				}

				names[actionInd] = actionName;
				timesArray[actionInd] = times;
				matsArray[actionInd] = mats;
				actionInd++;
			}

			return new MeshData.AnimTrans(timesArray, matsArray, names, getStaticMat());
		}
	}

	/**
	 * SceneNode is basically a container for Collada "node" tags, this is needed to allow the system to walk the
	 * node tree and properly honour the instance nodes.
	 * @author matt.chudleigh
	 */
	private static class SceneNode {
		public final String id;
		public final String sid;
		public final ArrayList<SceneTrans> transforms = new ArrayList<>();

		public final ArrayList<SceneNode> subNodes = new ArrayList<>();
		public final ArrayList<String> subInstanceNames = new ArrayList<>();
		public final ArrayList<GeoInstInfo> subGeo = new ArrayList<>();

		SceneNode(String id, String sid) {
			this.id = id;
			this.sid = sid;
		}
	}

	/**
	 * A union like data structure, this is either a color value or texture (it should always be one or the other, but never both)
	 * @author matt.chudleigh
	 *
	 */
	private static class ColorTex {
		public Color4d color;
		public URI texture;
		public String relTexture;
		public String texCoordName;
	}

	private static class Effect {
		// Only hold diffuse colour for now
		public ColorTex diffuse;
		public Color4d ambient;
		public Color4d spec;
		public double shininess;
		public int transType;
		public Color4d transColour;
	}

	private static class LineGeoEffectPair {
		public LineSubGeo geo;
		public Effect effect;
		@Override
		public int hashCode() { return geo.hashCode() ^ effect.hashCode(); }

		public LineGeoEffectPair(LineSubGeo g, Effect e) {
			this.geo = g;
			this.effect = e;
		}

		@Override
		public boolean equals(Object o) {
			if (o == null) return false;
			if (!(o instanceof LineGeoEffectPair)) return false;

			LineGeoEffectPair other = (LineGeoEffectPair)o;
			return other.geo == geo && other.effect == effect;
		}
	}

}
