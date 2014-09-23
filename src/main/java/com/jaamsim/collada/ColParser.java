/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.collada;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.MeshFiles.VertexMap;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.RenderException;
import com.jaamsim.ui.LogBox;
import com.jaamsim.xml.XmlNode;
import com.jaamsim.xml.XmlParser;

/**
 * Inspired by the Collada loader for Sweethome3d by Emmanuel Puybaret / eTeks <info@eteks.com>.
 */
public class ColParser {

	private static boolean SHOW_COL_DEBUG = false;

	private static boolean keepRuntimeData = false;
	public static void setKeepData(boolean keepData) {
		keepRuntimeData = keepData;
	}

	public static MeshData parse(URI asset) throws RenderException {

		try {
			ColParser colParser = new ColParser(asset.toURL());

			colParser.processContent();

			return colParser.getData();

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
		DOUBLE_ARRAY_TAGS = new ArrayList<String>();
		DOUBLE_ARRAY_TAGS.add("float_array");
		DOUBLE_ARRAY_TAGS.add("float");
		DOUBLE_ARRAY_TAGS.add("rotate");
		DOUBLE_ARRAY_TAGS.add("translate");
		DOUBLE_ARRAY_TAGS.add("scale");
		DOUBLE_ARRAY_TAGS.add("lookat");
		DOUBLE_ARRAY_TAGS.add("matrix");
		DOUBLE_ARRAY_TAGS.add("color");

		INT_ARRAY_TAGS = new ArrayList<String>();
		INT_ARRAY_TAGS.add("int_array");
		INT_ARRAY_TAGS.add("vcount");
		INT_ARRAY_TAGS.add("p");
		INT_ARRAY_TAGS.add("h");
		INT_ARRAY_TAGS.add("v");

		STRING_ARRAY_TAGS = new ArrayList<String>();
		STRING_ARRAY_TAGS.add("Name_array");
		STRING_ARRAY_TAGS.add("IDREF_array");

		BOOLEAN_ARRAY_TAGS = new ArrayList<String>();
		BOOLEAN_ARRAY_TAGS.add("boolean_array");

	}

	private static class FaceSubGeo {
		public VertexMap vMap;
		int[] indices;
		public String materialSymbol;

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
		public final ArrayList<SceneNode> nodes = new ArrayList<SceneNode>();
	}

	private static class Geometry {
		public final Vector<FaceSubGeo> faceSubGeos = new Vector<FaceSubGeo>();
		public final Vector<LineSubGeo> lineSubGeos = new Vector<LineSubGeo>();
	}

	private final URL _contextURL;

	private final HashMap<String, Geometry> _geos = new HashMap<String, Geometry>();
	private final HashMap<String, String> _images = new HashMap<String, String>(); // Maps image names to files
	private final HashMap<String, String> _materials = new HashMap<String, String>(); // Maps materials to effects
	private final HashMap<String, Effect> _effects = new HashMap<String, Effect>(); // List of known effects
	private final HashMap<String, SceneNode> _namedNodes = new HashMap<String, SceneNode>();
	private final HashMap<String, VisualScene> _visualScenes = new HashMap<String, VisualScene>();

	// This stack is used to track node loops
	private final  Stack<SceneNode> _nodeStack = new Stack<SceneNode>();

	// This list tracks the combinations of sub geometries and effects loaded in the mesh proto and defines an implicit
	// index into the mesh proto. This should probably be made more explicit later
	private final ArrayList<FaceSubGeo> _loadedFaceGeos = new ArrayList<FaceSubGeo>();
	private final ArrayList<Effect> _loadedEffects = new ArrayList<Effect>();
	private final ArrayList<LineGeoEffectPair> _loadedLineGeos = new ArrayList<LineGeoEffectPair>();

	private MeshData _finalData = new MeshData(keepRuntimeData);

	private HashMap<String, Vec4d[]> _dataSources = new HashMap<String, Vec4d[]>();

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
		XmlNode scene = _colladaNode.findChildTag("scene", false);
		parseAssert(scene != null);

		XmlNode instVS = scene.findChildTag("instance_visual_scene", false);
		parseAssert(instVS != null);
		String vsURL = instVS.getAttrib("url");
		parseAssert(vsURL.charAt(0) == '#');

		Mat4d globalMat = getGlobalRot();
		globalMat.scale3(getScaleFactor());

		VisualScene vs = _visualScenes.get(vsURL.substring(1));
		for (SceneNode sn : vs.nodes) {
			visitNode(sn, globalMat);
		}

		_finalData.finalizeData();
	}

	private void visitNode(SceneNode node, Mat4d parentMat) {
		_nodeStack.push(node);

		// Update the current transform
		Mat4d currentMat = new Mat4d(parentMat);
		currentMat.mult4(node.trans);

		for (GeoInstInfo geoInfo : node.subGeo) {
			addGeoInst(geoInfo, currentMat);
		}

		// Add instance_node
		for (String nodeName : node.subInstanceNames) {
			parseAssert(nodeName.charAt(0) == '#');
			SceneNode instNode = _namedNodes.get(nodeName.substring(1));
			// Check for reference loops, make sure this node is not currently in the active node stack
			parseAssert(!_nodeStack.contains(instNode));

			parseAssert(instNode != null);
			node.subNodes.add(instNode);
		}

		// Finally continue visiting the scene
		for (SceneNode nextNode : node.subNodes) {
			visitNode(nextNode, currentMat);
		}

		_nodeStack.pop();
	}

	private Effect geoBindingToEffect(Map<String, String> materialMap, String symbol) {
		String materialId = materialMap.get(symbol);
		parseAssert(materialId != null);

		parseAssert(materialId.charAt(0) == '#');
		String effectId = _materials.get(materialId.substring(1));
		parseAssert(effectId != null);

		parseAssert(effectId.charAt(0) == '#');
		Effect effect = _effects.get(effectId.substring(1));
		parseAssert(effect != null);

		return effect;
	}

	private void addGeoInst(GeoInstInfo geoInfo, Mat4d mat) {
		parseAssert(geoInfo.geoName.charAt(0) == '#');
		Geometry geo = _geos.get(geoInfo.geoName.substring(1));

		for (FaceSubGeo subGeo : geo.faceSubGeos) {
			// Check if this geometry and material pair has been loaded yet
			Effect effect = geoBindingToEffect(geoInfo.materialMap, subGeo.materialSymbol);

			int geoID;
			if (_loadedFaceGeos.contains(subGeo)) {
				geoID = _loadedFaceGeos.indexOf(subGeo);
			} else {
				geoID = _loadedFaceGeos.size();
				_loadedFaceGeos.add(subGeo);
				_finalData.addSubMesh(subGeo.vMap.getVertList(), subGeo.indices);
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

			_finalData.addSubMeshInstance(geoID, matID, -1, mat, null, null);
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
			_finalData.addSubLineInstance(geoID, mat);

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

		HashMap<String, XmlNode> paramMap = new HashMap<String, XmlNode>();

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
		if (node.getNumChildren() != 1) {
			parseAssert(false);
			return null;
		}

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
		// Find this sampler in the map
		XmlNode sampler = paramMap.get(texName);
		parseAssert(sampler != null);

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

		String imageName = (String)initFrom.getContent();

		String img = _images.get(imageName);
		parseAssert(img != null);
		try {
			ret.texture = new URL(_contextURL, img).toURI();
			ret.relTexture = img;
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
			if (meshNode.getTag() == "mesh") {
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
		SceneNode sn = new SceneNode();
		sn.id = node.getFragID();

		if (sn.id != null) _namedNodes.put(sn.id, sn);

		if (parent != null) {
			parent.subNodes.add(sn);
		}

		// Build up the transformation matrix for this node
		for (XmlNode child : node.children()) {
			String childTag = child.getTag();
			Mat4d mat = null;
			if (childTag.equals("translate")) {
				mat = transToMat(child);
			}
			if (childTag.equals("rotate")) {
				mat = rotToMat(child);
			}
			if (childTag.equals("scale")) {
				mat = scaleToMat(child);
			}
			if (childTag.equals("matrix")) {
				mat = matToMat(child);
			}
			if (mat != null) {
				sn.trans.mult4(mat);
			}
		}

		// Now handle sub geometry, sub nodes and instance nodes
		for (XmlNode child : node.children()) {
			String childTag = child.getTag();
			if (childTag.equals("instance_geometry")) {
				GeoInstInfo geoInfo = processInstGeo(child);
				sn.subGeo.add(geoInfo);
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

	private GeoInstInfo processInstGeo(XmlNode instGeo) {
		GeoInstInfo instInfo = new GeoInstInfo();
		instInfo.geoName = instGeo.getAttrib("url");

		XmlNode bindMat = instGeo.findChildTag("bind_material", false);
		if (bindMat == null) return instInfo;

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
			// Make sure that if the asset is requesting a particular texture coordinate set, that it's set 0 (the only one we support)
			for (XmlNode sub : instMat.children()) {
				if (!sub.getTag().equals("bind_vertex_input")) {
					continue;
				}
				String inputSet = sub.getAttrib("input_set");
				if (inputSet != null && !inputSet.equals("0")) {
					throw new RenderException("Collada model using a texture coordinate set that is not 0. This is not currently supported");
				}
			}
		}
		return instInfo;
	}

	private Mat4d transToMat(XmlNode transNode) {
		double[] vals = (double[])transNode.getContent();
		parseAssert(vals != null && vals.length >= 3);
		Vec3d transVect = new Vec3d(vals[0], vals[1], vals[2]);
		Mat4d ret = new Mat4d();
		ret.setTranslate3(transVect);
		return ret;
	}

	private Mat4d rotToMat(XmlNode rotNode) {
		double[] vals = (double[])rotNode.getContent();
		parseAssert(vals != null && vals.length >= 4);

		Vec3d axis = new Vec3d(vals[0], vals[1], vals[2]);
		double rads = (float)Math.toRadians(vals[3]);

		Quaternion rot = new Quaternion();
		rot.setAxisAngle(axis, rads);

		Mat4d ret = new Mat4d();
		ret.setRot3(rot);
		return ret;
	}

	private Mat4d scaleToMat(XmlNode scaleNode) {
		double[] vals = (double[])scaleNode.getContent();
		parseAssert(vals != null && vals.length >= 3);
		Vec3d scaleVect = new Vec3d(vals[0], vals[1], vals[2]);
		Mat4d ret = new Mat4d();
		ret.scaleCols3(scaleVect);
		return ret;
	}

	private Mat4d matToMat(XmlNode scaleNode) {
		double[] vals = (double[])scaleNode.getContent();
		parseAssert(vals != null && vals.length >= 16);
		Mat4d ret = new Mat4d(vals);
		return ret;
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

		Vec4d[] posData = getDataArrayFromSource(smd.posDesc.source);

		lsg.materialSymbol = subGeo.getAttrib("material");
		parseAssert(lsg.materialSymbol != null);

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
		String geoTag = subGeo.getTag();

		SubMeshDesc smd = readGeometryInputs(subGeo);

		boolean hasNormal = smd.normDesc != null;

		if (geoTag.equals("triangles")) {
			parseTriangles(smd, subGeo);
		}
		if (geoTag.equals("polylist")) {
			parsePolylist(smd, subGeo);
		}
		if (geoTag.equals("polygons")) {
			parsePolygons(smd, subGeo);
		}

		int numVerts = smd.posDesc.indices.length;
		parseAssert(numVerts % 3 == 0);
		if (numVerts == 0) {
			return;
		}

		// Now the SubMeshDesc should be fully populated, and we can actually produce the final triangle arrays
		boolean hasTexCoords = (smd.texCoordDesc != null);
		FaceSubGeo fsg = new FaceSubGeo(numVerts);

		Vec4d[] posData = getDataArrayFromSource(smd.posDesc.source);

		Vec4d[] normData = null;
		if (hasNormal) {
			normData = getDataArrayFromSource(smd.normDesc.source);
		}
		Vec4d[] texCoordData = null;
		if (hasTexCoords)
			texCoordData = getDataArrayFromSource(smd.texCoordDesc.source);

		fsg.materialSymbol = subGeo.getAttrib("material");
		parseAssert(fsg.materialSymbol != null);

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
				texCoord = new Vec2d(texCoordData[smd.texCoordDesc.indices[i]]);
			}
			fsg.indices[i] = fsg.vMap.getVertIndex(pos, normal, texCoord);
		}
		geoData.faceSubGeos.add(fsg);
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
		parseAssert(ps.length >= count * 2 * smd.stride);

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
		if (smd.texCoordDesc != null) {
			smd.texCoordDesc.indices = new int[count*3];
		}

		for (int i = 0; i < count * 3; ++i) {
			int offset = i * smd.stride;
			smd.posDesc.indices[i] = ps[offset + smd.posDesc.offset];
			if (smd.normDesc != null) {
				smd.normDesc.indices[i] = ps[offset + smd.normDesc.offset];
			}
			if (smd.texCoordDesc != null) {
				smd.texCoordDesc.indices[i] = ps[offset + smd.texCoordDesc.offset];
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
		if (smd.texCoordDesc != null) {
			smd.texCoordDesc.indices = new int[numTriangles * 3];
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
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.normDesc.offset];
				}
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.normDesc.offset];
				}
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.texCoordDesc.offset];
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
			if (n.getTag() != "p") {
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
		if (smd.texCoordDesc != null) {
			smd.texCoordDesc.indices = new int[numTriangles * 3];
		}

		int nextWriteVert = 0;

		for (XmlNode n : subGeo.children()) {
			// Note: we do not support 'ph' tags (polygons with holes)
			if (n.getTag() != "p") {
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
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.normDesc.offset];
				}
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.posDesc.offset];
				if (smd.normDesc != null) {
					smd.normDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.normDesc.offset];
				}
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.texCoordDesc.offset];
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

			String set =  input.getAttrib("set");

			// We specifically only support texture coordinate set 0, which can be expressed in an annoying combination of ways
			if (semantic.equals("TEXCOORD0") ||
			    ((semantic.equals("TEXCOORD")) && (set == null || set.equals("0")))) {
				smd.texCoordDesc = new DataDesc();
				smd.texCoordDesc.source = source;
				smd.texCoordDesc.offset = offset;
			}
		}

		if (smd.posDesc == null) {
			throw new ColException("Could not find positions for mesh.");
		}

		smd.stride = maxOffset+1;

		return smd;
	}


	/**
	 * Return a meaningful list of Vectors from data source 'id'
	 * @param id
	 * @return
	 */
	Vec4d[] getDataArrayFromSource(String id) {
		// First check the cache
		Vec4d[] cached = _dataSources.get(id);
		if (cached != null) {
			return cached;
		}
		// Okay, this source hasn't be accessed yet
		XmlNode sourceNode = getNodeFromID(id);
		if (sourceNode == null) { throw new ColException("Could not find node with id: " + id); }

		XmlNode floatNode = sourceNode.findChildTag("float_array", false);
		if (floatNode == null) { throw new ColException("No float array in source: " + id); }

		int floatCount = Integer.parseInt(floatNode.getAttrib("count"));
		double[] values = (double[])floatNode.getContent();

		XmlNode techCommon = sourceNode.findChildTag("technique_common", false);
		if (techCommon == null) { throw new ColException("No technique_common in source: " + id); }

		XmlNode accessor = techCommon.findChildTag("accessor", false);
		if (accessor == null) { throw new ColException("No accessor in source: " + id); }

		int stride = Integer.parseInt(accessor.getAttrib("stride"));
		int count = Integer.parseInt(accessor.getAttrib("count"));

		parseAssert(floatCount >= count * stride);

		Vec4d[] ret = new Vec4d[count];

		int valueOffset = 0;
		for (int i = 0; i < count; ++i) {
			switch(stride) {
			case 2:
				ret[i] = new Vec4d(values[valueOffset], values[valueOffset+1], 0, 1);
				break;
			case 3:
				ret[i] = new Vec4d(values[valueOffset], values[valueOffset+1], values[valueOffset+2], 1);
				break;
			case 4:
				ret[i] = new Vec4d(values[valueOffset], values[valueOffset+1], values[valueOffset+2], values[valueOffset+3]);
				break;
			}
			valueOffset += stride;
		}

		_dataSources.put(id, ret);

		return ret;
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
		public DataDesc texCoordDesc;
		public int stride;
	}

	/**
	 * Information for a geometry instance (as opposed to a geometry)
	 * for now this is simply a map from material binding symbols to actual material IDs
	 * and a geometry name
	 */
	private static class GeoInstInfo {
		public String geoName;
		public final Map<String, String> materialMap = new HashMap<String, String>();
	}

	/**
	 * SceneNode is basically a container for Collada "node" tags, this is needed to allow the system to walk the
	 * node tree and properly honour the instance nodes.
	 * @author matt.chudleigh
	 */
	private static class SceneNode {

		public String id;
		public final Mat4d trans = new Mat4d();

		public final ArrayList<SceneNode> subNodes = new ArrayList<SceneNode>();
		public final ArrayList<String> subInstanceNames = new ArrayList<String>();
		public final ArrayList<GeoInstInfo> subGeo = new ArrayList<GeoInstInfo>();
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
