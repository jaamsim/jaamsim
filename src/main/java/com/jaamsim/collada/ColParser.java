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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.MeshProto;
import com.jaamsim.render.RenderException;

/**
 * Inspired by the Collada loader for Sweethome3d by Emmanuel Puybaret / eTeks <info@eteks.com>.
 */
public class ColParser extends DefaultHandler {

	public static MeshProto parse(URL asset) throws RenderException {
		InputStream in;
		try {
			in = asset.openStream();
		} catch (IOException ex) {
			throw new RenderException("Can't read " + asset);
		}

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setValidating(false);

		try {
			SAXParser saxParser = factory.newSAXParser();
			ColParser handler = new ColParser(asset);

			saxParser.parse(in, handler);

			handler.processContent();
			return handler.getProto();

		} catch (Exception e) {
			e.printStackTrace();
			throw new RenderException(e.getMessage());
		}
	}

	static final List<String> DOUBLE_ARRAY_TAGS;
	static final List<String> INT_ARRAY_TAGS;
	static final List<String> STRING_ARRAY_TAGS;
	static final List<String> BOOLEAN_ARRAY_TAGS;

	static {
		DOUBLE_ARRAY_TAGS = new ArrayList<String>();
		DOUBLE_ARRAY_TAGS.add("float_array");
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
		public final Vec4d[] verts;
		public final Vec4d[] normals;
		public final Vec4d[] texCoords;
		public String materialSymbol;

		public FaceSubGeo(int size, boolean hasTex) {
			verts = new Vec4d[size];
			normals = new Vec4d[size];
			if (hasTex) {
				texCoords = new Vec4d[size];
			} else {
				texCoords = null;
			}
		}
	}

	private static class LineSubGeo {
		public final Vec4d[] verts;
		public String materialSymbol;

		public LineSubGeo(int size) {
			verts = new Vec4d[size];
		}
	}

	private class VisualScene {
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
	private final ArrayList<FaceGeoEffectPair> _loadedFaceGeos = new ArrayList<FaceGeoEffectPair>();
	private final ArrayList<LineGeoEffectPair> _loadedLineGeos = new ArrayList<LineGeoEffectPair>();

	private MeshProto _finalProto = new MeshProto();

	private HashMap<String, Vec4d[]> _dataSources = new HashMap<String, Vec4d[]>();

	private ColNode _rootNode;
	private ColNode _colladaNode;

	private ColNode _currentNode;

	// The _nodeIDMap is a mapping of fragment IDs to nodes to make data analysis easier
	private HashMap<String, ColNode> _nodeIDMap = new HashMap<String, ColNode>();

	private StringBuilder _contentBuilder;

	public ColParser(URL context) {
		_contextURL = context;
		_rootNode = new ColNode(null, "", "");
		_currentNode = _rootNode;
	}

	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {

		int IDIndex = attributes.getIndex("id");
		String fragID = null;
		if (IDIndex != -1) {
			fragID = attributes.getValue(IDIndex);
		}
		ColNode node = new ColNode(_currentNode, name, fragID);
		_currentNode.addChild(node);
		_currentNode = node;
		for (int i = 0; i < attributes.getLength(); ++i) {
			if (i == IDIndex) continue; // This attribute is special and handled

			String n = attributes.getQName(i);
			String v = attributes.getValue(i);
			node.addAttrib(n, v);
		}
		if (IDIndex != -1) {
			_nodeIDMap.put(fragID, node);
		}
		_contentBuilder = new StringBuilder();
	}

	@Override
	public void characters(char [] ch, int start, int length) throws SAXException {
		_contentBuilder.append(ch, start, length);
	}

	@Override
	public void endElement(String uri, String localName, String name) throws SAXException {
		// Handle the contents type based on the current nodes tag
		Object contents;
		if (DOUBLE_ARRAY_TAGS.contains(name)) {
			contents = parseDoubleArray();
		} else if (INT_ARRAY_TAGS.contains(name)) {
			contents = parseIntArray();
		} else if (BOOLEAN_ARRAY_TAGS.contains(name)) {
			contents = parseBooleanArray();
		} else if (STRING_ARRAY_TAGS.contains(name)) {
			contents = contentsToStringArray();
		} else {
			contents = _contentBuilder.toString().trim();
		}

		_currentNode.setContent(contents);
		_currentNode = _currentNode.getParent();
		_contentBuilder = new StringBuilder(); //
	}

	private ColNode getNodeFromID(String fragID) {

		if (fragID.length() < 1 || fragID.charAt(0) != '#') {
			return null;
		}
		return _nodeIDMap.get(fragID.substring(1));
	}

	private void processContent() {

		_colladaNode = _rootNode.findChildTag("COLLADA", false);
		assert(_colladaNode != null);

		processGeos();
		processImages();
		processMaterials();
		processEffects();
		processNodes();

		processVisualScenes();

		processScene();
	}

	private double getScaleFactor() {
		ColNode assetNode = _colladaNode.findChildTag("asset", false);
		if (assetNode == null) return 1;

		ColNode unit = assetNode.findChildTag("unit", false);
		if (unit == null) return 1;

		String meter = unit.getAttrib("meter");
		if (meter == null) return 1;

		return Double.parseDouble(meter);
	}

	private String getUpAxis() {
		ColNode assetNode = _colladaNode.findChildTag("asset", false);
		if (assetNode == null) return "Y_UP";

		ColNode upAxisNode = assetNode.findChildTag("up_axis", false);
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
		ColNode scene = _colladaNode.findChildTag("scene", false);
		assert(scene != null);

		ColNode instVS = scene.findChildTag("instance_visual_scene", false);
		assert(instVS != null);
		String vsURL = instVS.getAttrib("url");
		assert(vsURL.charAt(0) == '#');

		Mat4d globalMat = getGlobalRot();
		globalMat.scale3(getScaleFactor());

		VisualScene vs = _visualScenes.get(vsURL.substring(1));
		for (SceneNode sn : vs.nodes) {
			visitNode(sn, globalMat);
		}

		_finalProto.generateHull();
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
			assert(nodeName.charAt(0) == '#');
			SceneNode instNode = _namedNodes.get(nodeName.substring(1));
			// Check for reference loops, make sure this node is not currently in the active node stack
			assert(!_nodeStack.contains(instNode));

			assert(instNode != null);
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
		assert(materialId != null);

		assert(materialId.charAt(0) == '#');
		String effectId = _materials.get(materialId.substring(1));
		assert(effectId != null);

		assert(effectId.charAt(0) == '#');
		Effect effect = _effects.get(effectId.substring(1));
		assert(effect != null);

		return effect;
	}

	private void addGeoInst(GeoInstInfo geoInfo, Mat4d mat) {
		assert(geoInfo.geoName.charAt(0) == '#');
		Geometry geo = _geos.get(geoInfo.geoName.substring(1));

		for (FaceSubGeo subGeo : geo.faceSubGeos) {
			// Check if this geometry and material pair has been loaded yet
			Effect effect = geoBindingToEffect(geoInfo.materialMap, subGeo.materialSymbol);

			FaceGeoEffectPair ge = new FaceGeoEffectPair(subGeo, effect);
			int geoID;
			if (_loadedFaceGeos.contains(ge)) {
				geoID = _loadedFaceGeos.indexOf(ge);
			} else {
				geoID = _loadedFaceGeos.size();
				_loadedFaceGeos.add(ge);
				_finalProto.addSubMesh(subGeo.verts,
				                       subGeo.normals,
				                       subGeo.texCoords,
				                       effect.diffuse.texture,
				                       effect.diffuse.color,
				                       effect.transType, effect.transColour);
			}

			_finalProto.addSubMeshInstance(geoID, mat);
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
				_finalProto.addSubLine(subGeo.verts,
				                       effect.diffuse.color);
			}
			_finalProto.addSubLineInstance(geoID, mat);

		}
	}

	private void processVisualScenes() {
		ColNode libScenes = _colladaNode.findChildTag("library_visual_scenes", false);
		if (libScenes == null)
			return; // No scenes

		for (ColNode child : libScenes.children()) {
			if (child.getTag().equals("visual_scene")) {
				processVisualScene(child);
			}
		}
	}

	private void processVisualScene(ColNode scene) {
		String id = scene.getFragID();
		VisualScene vs = new VisualScene();
		_visualScenes.put(id, vs);

		for (ColNode child : scene.children()) {
			if (child.getTag().equals("node")) {
				SceneNode node = processNode(child, null);
				vs.nodes.add(node);
			}
		}
	}

	private void processImages() {
		ColNode libImage = _colladaNode.findChildTag("library_images", false);
		if (libImage == null)
			return; // No images

		for (ColNode child : libImage.children()) {
			if (child.getTag().equals("image")) {
				processImage(child);
			}
		}
	}

	private void processImage(ColNode imageNode) {
		// For now all we care about with images is the init_form contents and the name
		String id = imageNode.getFragID();
		if (id == null) return; // We do not care about images we can not reference

		ColNode initFrom = imageNode.findChildTag("init_from", true);
		if (initFrom == null) {
			assert(false);
			return;
		}

		String fileName = (String)initFrom.getContent();
		assert(fileName != null);

		_images.put(id, fileName);
	}

	private void processMaterials() {
		ColNode libMats = _colladaNode.findChildTag("library_materials", false);
		if (libMats == null)
			return; // No materials

		for (ColNode child : libMats.children()) {
			if (child.getTag().equals("material")) {
				processMaterial(child);
			}
		}
	}

	private void processMaterial(ColNode matNode) {
		String id = matNode.getFragID();
		if (id == null) return; // We do not care about materials we can not reference

		ColNode instEffect = matNode.findChildTag("instance_effect", true);
		if (instEffect == null) {
			assert(false);
			return;
		}

		String effectURL = instEffect.getAttrib("url");
		if (effectURL == null) {
			assert(false);
			return;
		}

		_materials.put(id, effectURL);
	}

	private void processEffects() {
		ColNode libEffects = _colladaNode.findChildTag("library_effects", false);
		if (libEffects == null)
			return; // No effects

		for (ColNode child : libEffects.children()) {
			if (child.getTag().equals("effect")) {
				processEffect(child);
			}
		}
	}

	private void processEffect(ColNode effectNode) {
		String id = effectNode.getFragID();
		if (id == null) return; // We do not care about materials we can not reference

		ColNode profCommon = effectNode.findChildTag("profile_COMMON", true);
		if (profCommon == null) {
			assert(false);
			return; // There is no common profile
		}

		HashMap<String, ColNode> paramMap = new HashMap<String, ColNode>();

		// Start by building a table of all params
		for (ColNode child : profCommon.children()) {
			String tag = child.getTag();
			if (tag.equals("newparam")) {
				String sid = child.getAttrib("sid");
				if (sid != null) paramMap.put(sid, child);
			}
		}

		ColNode technique = profCommon.findChildTag("technique", false);
		if (technique == null) {
			assert(false);
			return; // There is no common profile
		}
		// Search technique for the kind of data we care about, for now find blinn, phong or lambert
		ColNode diffuse = null;
		ColNode transparency = null;
		ColNode transparent = null;

		ColNode blinn = technique.findChildTag("blinn", false);
		ColNode phong = technique.findChildTag("phong", false);
		ColNode lambert = technique.findChildTag("lambert", false);
		ColNode constant = technique.findChildTag("constant", false);
		if (blinn != null) {
			diffuse = blinn.findChildTag("diffuse", false);
			transparency = blinn.findChildTag("transparency", false);
			transparent = blinn.findChildTag("transparent", false);
		}
		if (phong != null) {
			diffuse = phong.findChildTag("diffuse", false);
			transparency = phong.findChildTag("transparency", false);
			transparent = phong.findChildTag("transparent", false);
		}
		if (lambert != null) {
			diffuse = lambert.findChildTag("diffuse", false);
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

		String opaque = null;
		ColorTex transparentCT = null;
		if (transparent != null) {
			opaque = transparent.getAttrib("opaque");
			transparentCT = getColorTex(transparent, paramMap);
		}

		// There is a ton of conditions for us to handle transparency
		if (transparency != null &&
		    transparent != null &&
		    opaque != null &&
		    (opaque.equals("A_ONE") || opaque.equals("RGB_ZERO")) &&
		    transparentCT != null &&
		    transparentCT.color != null) {
			ColNode floatNode = transparency.findChildTag("float", false);
			assert(floatNode != null);

			double alpha = Double.parseDouble((String)floatNode.getContent());
			effect.transColour = new Color4d(transparentCT.color);
			if (opaque.equals("A_ONE")) {
				effect.transType = MeshProto.A_ONE_TRANS;
			}
			if (opaque.equals("RGB_ZERO")) {
				effect.transType = MeshProto.RGB_ZERO_TRANS;
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
				effect.transType = MeshProto.NO_TRANS; // Some meshes are effectively not transparent despite having the information
			}
		} else {
			effect.transType = MeshProto.NO_TRANS;
		}

		_effects.put(id,  effect);
	}

	private ColorTex getColorTex(ColNode node, HashMap<String, ColNode> paramMap) {
		if (node.getNumChildren() != 1) {
			assert(false);
			return null;
		}

		ColNode valNode = node.getChild(0);

		String tag = valNode.getTag();
		ColorTex ret = new ColorTex();
		if (tag.equals("color")) {

			double[] colVals = (double[])valNode.getContent();
			assert(colVals != null && colVals.length >= 4);
			Color4d col = new Color4d(colVals[0], colVals[1], colVals[2], colVals[3]);
			ret.color = col;
			return ret;
		}

		if (!tag.equals("texture")) {
			assert(false);
			return null;
		}

		// Now we have the fun dealing with COLLADA's incredible indirectness
		String texName = valNode.getAttrib("texture");
		// Find this sampler in the map
		ColNode sampler = paramMap.get(texName);
		assert(sampler != null);

		ColNode sampler2D = sampler.findChildTag("sampler2D", false);
		assert(sampler2D != null);
		ColNode source = sampler2D.findChildTag("source", false);
		assert(source != null);

		String surfaceName = (String)source.getContent();

		ColNode surfaceParam = paramMap.get(surfaceName);
		ColNode surface = surfaceParam.findChildTag("surface", false);
		assert(surface != null);
		assert(surface.getAttrib("type").equals("2D"));

		ColNode initFrom = surface.findChildTag("init_from", false);
		assert(initFrom != null);

		String imageName = (String)initFrom.getContent();

		String img = _images.get(imageName);
		assert(img != null);
		try {
			ret.texture = new URL(_contextURL, img);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			assert(false);
		}
		return ret;
	}

	private void processGeos() {
		ColNode libGeo = _colladaNode.findChildTag("library_geometries", false);
		if (libGeo == null)
			return; // No geometries

		for (ColNode child : libGeo.children()) {
			if (child.getTag().equals("geometry")) {
				processGeo(child);
			}
		}
	}

	private void processGeo(ColNode geoNode) {
		String geoID = geoNode.getFragID();
		if (geoID == null) {
			// This geometry can not be referenced, don't bother
			return;
		}

		Geometry geoData = new Geometry();

		for (ColNode meshNode : geoNode.children()) {
			if (meshNode.getTag() == "mesh") {
				parseMesh(meshNode, geoData);
			}
		}

		_geos.put(geoID, geoData);
	}

	private void processNodes() {
		ColNode libNodes = _colladaNode.findChildTag("library_nodes", false);
		if (libNodes == null)
			return; // No images

		for (ColNode child : libNodes.children()) {
			if (child.getTag().equals("node")) {
				processNode(child, null);
			}
		}
	}

	private SceneNode processNode(ColNode node, SceneNode parent) {
		SceneNode sn = new SceneNode();
		sn.id = node.getFragID();

		if (sn.id != null) _namedNodes.put(sn.id, sn);

		if (parent != null) {
			parent.subNodes.add(sn);
		}

		// Build up the transformation matrix for this node
		for (ColNode child : node.children()) {
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
		for (ColNode child : node.children()) {
			String childTag = child.getTag();
			if (childTag.equals("instance_geometry")) {
				GeoInstInfo geoInfo = processInstGeo(child);
				sn.subGeo.add(geoInfo);
			}
			if (childTag.equals("instance_node")) {
				String nodeID = child.getAttrib("url");
				assert(nodeID != null);
				sn.subInstanceNames.add(nodeID);
			}
			if (childTag.equals("node")) {
				processNode(child, sn);
			}
		}
		return sn;
	}

	private GeoInstInfo processInstGeo(ColNode instGeo) {
		GeoInstInfo instInfo = new GeoInstInfo();
		instInfo.geoName = instGeo.getAttrib("url");

		ColNode bindMat = instGeo.findChildTag("bind_material", false);
		if (bindMat == null) return instInfo;

		ColNode techCommon = bindMat.findChildTag("technique_common", false);
		assert(techCommon != null);

		for (ColNode instMat : techCommon.children()) {
			if (!instMat.getTag().equals("instance_material")) {
				continue;
			}
			String symbol = instMat.getAttrib("symbol");
			String target = instMat.getAttrib("target");
			assert(symbol != null && target != null);
			instInfo.materialMap.put(symbol, target);
			// TODO, properly handle rebinding vertex inputs
		}
		return instInfo;
	}

	private Mat4d transToMat(ColNode transNode) {
		double[] vals = (double[])transNode.getContent();
		assert(vals != null && vals.length >= 3);
		Vec3d transVect = new Vec3d(vals[0], vals[1], vals[2]);
		Mat4d ret = new Mat4d();
		ret.setTranslate3(transVect);
		return ret;
	}

	private Mat4d rotToMat(ColNode rotNode) {
		double[] vals = (double[])rotNode.getContent();
		assert(vals != null && vals.length >= 4);

		double rads = (float)Math.toRadians(vals[3]);
		Vec4d axis = new Vec4d(vals[0], vals[1], vals[2], 1.0d);
		Quaternion rot = Quaternion.Rotation(rads, axis);

		Mat4d ret = new Mat4d();
		ret.setRot3(rot);
		return ret;
	}

	private Mat4d scaleToMat(ColNode scaleNode) {
		double[] vals = (double[])scaleNode.getContent();
		assert(vals != null && vals.length >= 3);
		Vec3d scaleVect = new Vec3d(vals[0], vals[1], vals[2]);
		Mat4d ret = new Mat4d();
		ret.scaleCols3(scaleVect);
		return ret;
	}

	private Mat4d matToMat(ColNode scaleNode) {
		double[] vals = (double[])scaleNode.getContent();
		assert(vals != null && vals.length >= 16);
		Mat4d ret = new Mat4d(vals);
		return ret;
	}

	private void parseMesh(ColNode mesh, Geometry geoData) {

		// Now try to parse geometry type
		for (ColNode subGeo : mesh.children()) {
			String geoTag = subGeo.getTag();
			if (geoTag.equals("polylist") ||
			    geoTag.equals("triangles")) {

				generateTriangleGeo(subGeo, geoData);
			}

			if (geoTag.equals("lines") ||
			    geoTag.equals("linestrip")) {

				generateLineGeo(subGeo, geoData);
			}

		}
	}

	private void generateLineGeo(ColNode subGeo, Geometry geoData) {
		String geoTag = subGeo.getTag();

		SubMeshDesc smd = readGeometryInputs(subGeo);

		if (geoTag.equals("lines")) {
			parseLines(smd, subGeo);
		}
		if (geoTag.equals("linestrip")) {
			parseLinestrip(smd, subGeo);
		}

		int numVerts = smd.posDesc.indices.length;
		assert(numVerts % 2 == 0);

		// Now the SubMeshDesc should be fully populated, and we can actually produce the final triangle arrays
		LineSubGeo lsg = new LineSubGeo(numVerts);

		Vec4d[] posData = getDataArrayFromSource(smd.posDesc.source);

		lsg.materialSymbol = subGeo.getAttrib("material");
		assert(lsg.materialSymbol != null);

		for (int i = 0; i < numVerts; ++i) {
			lsg.verts[i] = posData[smd.posDesc.indices[i]];
			lsg.verts[i].w = 1;
		}
		geoData.lineSubGeos.add(lsg);

	}

	private void generateTriangleGeo(ColNode subGeo, Geometry geoData) {
		String geoTag = subGeo.getTag();

		SubMeshDesc smd = readGeometryInputs(subGeo);

		assert(smd.normDesc != null);

		if (geoTag.equals("triangles")) {
			parseTriangles(smd, subGeo);
		}
		if (geoTag.equals("polylist")) {
			parsePolylist(smd, subGeo);
		}

		int numVerts = smd.posDesc.indices.length;
		assert(numVerts % 3 == 0);

		// Now the SubMeshDesc should be fully populated, and we can actually produce the final triangle arrays
		boolean hasTexCoords = (smd.texCoordDesc != null);
		FaceSubGeo fsg = new FaceSubGeo(numVerts, hasTexCoords);

		Vec4d[] posData = getDataArrayFromSource(smd.posDesc.source);
		Vec4d[] normData = getDataArrayFromSource(smd.normDesc.source);
		Vec4d[] texCoordData = null;
		if (hasTexCoords)
			texCoordData = getDataArrayFromSource(smd.texCoordDesc.source);

		fsg.materialSymbol = subGeo.getAttrib("material");
		assert(fsg.materialSymbol != null);

		for (int i = 0; i < numVerts; ++i) {
			fsg.verts[i] = posData[smd.posDesc.indices[i]];
			fsg.verts[i].w = 1;

			fsg.normals[i] = normData[smd.normDesc.indices[i]];
			fsg.normals[i].w = 0;

			if (hasTexCoords) {
				fsg.texCoords[i] = texCoordData[smd.texCoordDesc.indices[i]];
			}
		}
		geoData.faceSubGeos.add(fsg);
	}

	private void readVertices(SubMeshDesc smd, int offset, ColNode vertices) {
		// Check vertices for inputs
		for (ColNode input : vertices.children()) {
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

	private void parseLines(SubMeshDesc smd, ColNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		ColNode pNode = subGeo.findChildTag("p", false);
		if (pNode == null)
			throw new ColException("No 'p' child in 'lines' in mesh.");

		int[] ps = (int[])pNode.getContent();
		assert(ps.length >= count * 2 * smd.stride);

		smd.posDesc.indices = new int[count*2];
		for (int i = 0; i < count * 2; ++i) {
			int offset = i * smd.stride;
			smd.posDesc.indices[i] = ps[offset + smd.posDesc.offset];
		}
	}

	private void parseLinestrip(SubMeshDesc smd, ColNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		// There should be 'count' number of 'p' tags in this element
		int[][] stripIndices = new int[count][];
		int numLines = 0;
		int nextIndex = 0;
		for (ColNode child : subGeo.children()) {
			if (!child.getTag().equals("p")) continue;

			int[] ps = (int[])child.getContent();
			assert(ps != null);
			assert(nextIndex < count);

			stripIndices[nextIndex++] = ps;
			numLines += ps.length - 1;
		}

		// We now have a list of list of all indices, split this into lines
		int nextWriteIndex = 0;
		smd.posDesc.indices = new int[numLines * 2];
		for (int[] strip : stripIndices) {
			assert(strip.length >= 2);

			for (int i = 1; i < strip.length; ++i) {
				smd.posDesc.indices[nextWriteIndex++] = strip[i-1];
				smd.posDesc.indices[nextWriteIndex++] = strip[i];
			}
		}
	}

	// Fill in the indices for 'smd'
	private void parseTriangles(SubMeshDesc smd, ColNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		ColNode pNode = subGeo.findChildTag("p", false);
		if (pNode == null)
			throw new ColException("No 'p' child in 'triangles' in mesh.");

		int[] ps = (int[])pNode.getContent();
		assert(ps.length >= count * 3 * smd.stride);

		smd.posDesc.indices = new int[count*3];
		smd.normDesc.indices = new int[count*3];
		if (smd.texCoordDesc != null) {
			smd.texCoordDesc.indices = new int[count*3];
		}

		for (int i = 0; i < count * 3; ++i) {
			int offset = i * smd.stride;
			smd.posDesc.indices[i] = ps[offset + smd.posDesc.offset];
			smd.normDesc.indices[i] = ps[offset + smd.normDesc.offset];
			if (smd.texCoordDesc != null) {
				smd.texCoordDesc.indices[i] = ps[offset + smd.texCoordDesc.offset];
			}
		}
	}

	// Note, this is definitely not correct, but for now assume all polygons are convex
	private void parsePolylist(SubMeshDesc smd, ColNode subGeo) {
		int count = Integer.parseInt(subGeo.getAttrib("count"));
		ColNode pNode = subGeo.findChildTag("p", false);
		if (pNode == null)
			throw new ColException("No 'p' child in 'polygons' in mesh.");

		int[] ps = (int[])pNode.getContent();

		ColNode vcountNode = subGeo.findChildTag("vcount", false);
		if (vcountNode == null)
			throw new ColException("No 'vcount' child in 'polygons' in mesh.");

		int[] vcounts = (int[])vcountNode.getContent();

		assert(vcounts.length == count);
		int totalVerts = 0;
		int numTriangles = 0;
		for (int i : vcounts) {
			totalVerts += i;
			numTriangles += (i-2);
		}
		assert(ps.length >= totalVerts * smd.stride);

		smd.posDesc.indices = new int[numTriangles * 3];
		smd.normDesc.indices = new int[numTriangles * 3];
		if (smd.texCoordDesc != null) {
			smd.texCoordDesc.indices = new int[numTriangles * 3];
		}

		int nextWriteVert = 0;
		int readVertOffset = 0;
		for (int v : vcounts) {
			// v is the number of vertices in this polygon
			assert(v >= 3);
			for (int i = 0; i < (v-2); ++i) {
				int vert0 = readVertOffset + i;
				int vert1 = readVertOffset + i + 1;
				int vert2 = readVertOffset + v - 1;

				smd.posDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.posDesc.offset];
				smd.normDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.normDesc.offset];
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert0*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.posDesc.offset];
				smd.normDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.normDesc.offset];
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert1*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;

				smd.posDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.posDesc.offset];
				smd.normDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.normDesc.offset];
				if (smd.texCoordDesc != null) {
					smd.texCoordDesc.indices[nextWriteVert] = ps[(vert2*smd.stride) + smd.texCoordDesc.offset];
				}
				nextWriteVert++;
			}
			readVertOffset += v;
		}
	}

	private SubMeshDesc readGeometryInputs(ColNode subGeo) {
		SubMeshDesc smd = new SubMeshDesc();

		int maxOffset = 0;
		for (ColNode input : subGeo.children()) {
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
				ColNode vertices = getNodeFromID(source);
				readVertices(smd, offset, vertices);
			}
			if (semantic.equals("NORMAL")) {
				smd.normDesc = new DataDesc();
				smd.normDesc.source = source;
				smd.normDesc.offset = offset;
			}
			if (semantic.equals("TEXCOORD") ||
			    semantic.equals("TEXCOORD0")) {
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

	private double[] parseDoubleArray() {
		String[] strings = contentsToStringArray();

		double[] ret = new double[strings.length];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Double.parseDouble(s);
		}
		return ret;
	}

	private int[] parseIntArray() {
		String[] strings = contentsToStringArray();

		int[] ret = new int[strings.length];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Integer.parseInt(s);
		}
		return ret;
	}

	private boolean[] parseBooleanArray() {
		String[] strings = contentsToStringArray();

		boolean[] ret = new boolean[strings.length];
		int index = 0;
		for (String s : strings) {
			ret[index++] = Boolean.parseBoolean(s);
		}
		return ret;
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
		ColNode sourceNode = getNodeFromID(id);
		if (sourceNode == null) { throw new ColException("Could not find node with id: " + id); }

		ColNode floatNode = sourceNode.findChildTag("float_array", false);
		if (floatNode == null) { throw new ColException("No float array in source: " + id); }

		int floatCount = Integer.parseInt(floatNode.getAttrib("count"));
		double[] values = (double[])floatNode.getContent();

		ColNode techCommon = sourceNode.findChildTag("technique_common", false);
		if (techCommon == null) { throw new ColException("No technique_common in source: " + id); }

		ColNode accessor = techCommon.findChildTag("accessor", false);
		if (accessor == null) { throw new ColException("No accessor in source: " + id); }

		int stride = Integer.parseInt(accessor.getAttrib("stride"));
		int count = Integer.parseInt(accessor.getAttrib("count"));

		assert(floatCount >= count * stride);

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

	private String[] contentsToStringArray() {
		String[] temp = _contentBuilder.toString().split("\\s+");

		// Annoyingly, split sometimes returns blank strings, so manually filter those out
		// (there is probably a much more elegant way of doing this)
		int numStrings = 0;
		for (String s : temp) {
			if (s.length() > 0) ++ numStrings;
		}
		String[] ret = new String[numStrings];
		int nextIdx = 0;
		for (String s : temp) {
			if (s.length() > 0)
				ret[nextIdx++] = s;
		}
		return ret;
	}

	public MeshProto getProto() {
		return _finalProto;
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
		public URL texture;
	}

	private static class Effect {
		// Only hold diffuse colour for now
		public ColorTex diffuse;
		public int transType;
		public Color4d transColour;
	}

	private static class FaceGeoEffectPair {
		public FaceSubGeo geo;
		public Effect effect;

		@Override
		public int hashCode() { return geo.hashCode() ^ effect.hashCode(); }

		public FaceGeoEffectPair(FaceSubGeo g, Effect e) {
			this.geo = g;
			this.effect = e;
		}

		public boolean equals(Object o) {
			if (o == null) return false;
			if (!(o instanceof FaceGeoEffectPair)) return false;

			FaceGeoEffectPair other = (FaceGeoEffectPair)o;
			return other.geo == geo && other.effect == effect;
		}
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

		public boolean equals(Object o) {
			if (o == null) return false;
			if (!(o instanceof LineGeoEffectPair)) return false;

			LineGeoEffectPair other = (LineGeoEffectPair)o;
			return other.geo == geo && other.effect == effect;
		}
	}

}
