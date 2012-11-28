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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Vector4d;
import com.jaamsim.render.MeshProto;
import com.jaamsim.render.RenderException;

/**
 * A loader for DAE Collada 1.4.1 format as specified by
 * <a href="http://www.khronos.org/files/collada_spec_1_4.pdf">http://www.khronos.org/files/collada_spec_1_4.pdf</a>.
 * All texture coordinates are considered to belong to the same set (for example UVSET0).<br>
 *
 * Inspired by the Collada loader for Sweethome3d by Emmanuel Puybaret / eTeks <info@eteks.com>.
 */
public class COLLoader {
   /**
   * Returns the scene described in the given DAE file.
   */

  public MeshProto load(String file) throws RenderException {

	URL url;
	try {

	  url = new File(file).toURI().toURL();
	  return load(new BufferedInputStream(new FileInputStream(file)), url);

	} catch (Exception e) {
		throw new RenderException(e.getMessage());
	}
  }

  /**
   * Returns the scene described in the given DAE file url.
   */
  public MeshProto load(URL url) throws RenderException {


	InputStream in;
    try {
      in = url.openStream();
    } catch (IOException ex) {
      throw new RenderException("Can't read " + url);
    }
    return load(new BufferedInputStream(in), url);

  }


  /**
   * Returns the scene described in the given DAE file.
   */
  private MeshProto load(InputStream in, URL url) throws RenderException {

	try {
      return parseXMLStream(in, url);
    } catch (IOException ex) {
      throw new RenderException(ex.getMessage());
    } finally {
      try {
        in.close();
      } catch (IOException ex) {
        throw new RenderException(ex.getMessage());
      }
    }
  }

  /**
   * Returns the scene parsed from a Collada XML stream.
   */
  private MeshProto parseXMLStream(InputStream in,
                               URL baseUrl) throws IOException {
    try {

      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      MeshProto proto = new MeshProto();
      saxParser.parse(in, new DAEHandler(proto, baseUrl));
      proto.generateHull();
      return proto;
    } catch (ParserConfigurationException ex) {
      IOException ex2 = new IOException("Can't parse XML stream");
      ex2.initCause(ex);
      throw ex2;
    } catch (SAXException ex) {
      IOException ex2 = new IOException("Can't parse XML stream");
      ex2.initCause(ex);
      throw ex2;
    }
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

  private static class SceneNode {

	public String id;
	public Matrix4d trans = new Matrix4d();

	public ArrayList<SceneNode> subNodes = new ArrayList<SceneNode>();
	public ArrayList<String> subInstanceNames = new ArrayList<String>();
	public ArrayList<GeoInstInfo> subGeo = new ArrayList<GeoInstInfo>();
  }

  private static class FaceSubGeo {
    public Vector4d[] verts;
    public Vector4d[] normals;
    public Vector4d[] texCoords;
    public String materialSymbol;
    //public String materialId; // This is later bound actual ID
    public FaceSubGeo(int size, boolean hasTex) {
    	verts = new Vector4d[size];
    	normals = new Vector4d[size];
    	if (hasTex) {
    	  texCoords = new Vector4d[size];
    	} else { texCoords = null; }
    }
  }

  private static class LineSubGeo {
    public Vector4d[] verts;
    public String materialSymbol;
    public LineSubGeo(int size) {
      verts = new Vector4d[size];
    }
  }

  private static class Geometry {
    public Vector<FaceSubGeo> faceSubGeos = new Vector<FaceSubGeo>();
    public Vector<LineSubGeo> lineSubGeos = new Vector<LineSubGeo>();
  }

  /**
   * Information for a geometry instance (as opposed to a geometry)
   * for now this is simply a map from material binding symbols to actual material IDs
   * and a geometry name
   */
  private static class GeoInstInfo {
	  public String geoName;
	  public Map<String, String> materialMap = new HashMap<String, String>();
  }

  private static class Effect {

	@SuppressWarnings("unused")
	public double shininess;
	public Vector4d specColor;
	public Vector4d diffuseColor;
	public URL diffuseTexture;
	Effect() {
		specColor = new Vector4d(0, 0, 0);
	}

	void setSpecularColor(double r, double g, double b) {
		specColor.data[0] = r;
		specColor.data[1] = g;
		specColor.data[2] = b;
	}
  }

  /**
   * SAX handler for DAE Collada stream.
   */
  private static class DAEHandler extends DefaultHandler {
    private final URL       url;
    private final MeshProto meshProto;
    //private boolean bFirstInstanceGeo = true;
    //private final Stack<Group>   parentGroups = new Stack<Group>();
    private final Stack<String>  parentElements = new Stack<String>();
    private final StringBuilder  buffer = new StringBuilder();
    private final List<Runnable> postProcessingBinders = new ArrayList<Runnable>();

    private final Map<String, URL> textures = new HashMap<String, URL>();
    private final Map<String, String> materialsToEffects = new HashMap<String, String>();
    private final Map<String, Effect> effects = new HashMap<String, Effect>();
    //private final Map<String, String> materialEffects = new HashMap<String, String>();
    private final Map<String, String> surface2DIds = new HashMap<String, String>();
    private final Map<String, String> sampler2DIds = new HashMap<String, String>();
    private final Map<String, Geometry> geometries = new HashMap<String, Geometry>();
    private final Map<String, float []> sources = new HashMap<String, float []>();
    private final Map<String, float []> positions = new HashMap<String, float []>();
    private final Map<String, float []> normals = new HashMap<String, float []>();
    private final Map<String, float []> textureCoordinates = new HashMap<String, float []>();
    private final Map<String, float []> floatArrays = new HashMap<String, float []>();
    private final Map<float [], Integer> sourceAccessorStrides = new HashMap<float [], Integer>();
    //private final Map<Geometry, String> geometryAppearances = new HashMap<Geometry, String>();
    private final List<int []> facesAndLinesPrimitives = new ArrayList<int[]>();
    private final Map<String, SceneNode> namedNodes = new HashMap<String, SceneNode>();
    private final ArrayList<SceneNode> rootNodes = new ArrayList<SceneNode>();
    private final ArrayList<FaceGeoEffectPair> loadedFaceGeos = new ArrayList<FaceGeoEffectPair>();
    private final ArrayList<LineGeoEffectPair> loadedLineGeos = new ArrayList<LineGeoEffectPair>();

    //private final List<int []> polygonsPrimitives = new ArrayList<int[]>();
    //private final List<List<int []>> polygonsHoles = new ArrayList<List<int[]>>();
    //private final Map<String, TransformGroup> nodes = new HashMap<String, TransformGroup>();
    //private final Map<String, SharedGroup> instantiatedNodes = new HashMap<String, SharedGroup>();
    //private final Map<String, TransformGroup> visualScenes = new HashMap<String, TransformGroup>();
    //private TransformGroup visualScene;
    private float [] floats;
    private float [] geometryVertices;
    private float [] geometryNormals;
    private float [] geometryTextureCoordinates;
    private int   [] vcount;
    private float [] transparentColor;
    private Float    transparency;

    private boolean inRootAsset;
    private boolean reverseTransparency;
    private String  imageId;
    private String  materialId;
    private String  effectId;
    private String  newParamSid;
    private boolean inSurface2D;
    private boolean inPhongBlinnOrLambert;
    @SuppressWarnings("unused")
	private String  techniqueProfile;
    private boolean inConstant;
    private String  geometryId;
    private String  meshSourceId;
    private String  verticesId;
    private String  floatArrayId;
    private String  subGeoMaterialSymbol;
    private int     geometryVertexOffset;
    private int     geometryNormalOffset;
    private int     geometryTextureCoordinatesOffset;
	private String  axis;
    private float   floatValue;
    private String  opaque;
    private int     inputCount;
    private final  Stack<SceneNode> nodeStack = new Stack<SceneNode>();
    private GeoInstInfo currentGeoInst;

    public DAEHandler(MeshProto proto, URL url) {
      this.meshProto = proto;
      this.url = url;
    }

    @Override
    public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
      buffer.setLength(0);
      String parent = this.parentElements.isEmpty() ? null : this.parentElements.peek();
      if (parent == null && !"COLLADA".equals(name)) {
        throw new SAXException("Expected COLLADA element");
      } else if ("COLLADA".equals(name)) {
        String version = attributes.getValue("version");
        if (!version.startsWith("1.4")) {
          throw new SAXException("Version " + version + " not supported");
        }
      } else if ("COLLADA".equals(parent) && "asset".equals(name)) {
        this.inRootAsset = true;
      } else if ("image".equals(name)) {
        this.imageId = attributes.getValue("id");
      } else if ("material".equals(name)) {
        this.materialId = attributes.getValue("id");
      } else if ("material".equals(parent) && "instance_effect".equals(name)) {
        String effectInstanceUrl = attributes.getValue("url");
        if (effectInstanceUrl.startsWith("#")) {
          final String effectInstanceAnchor = effectInstanceUrl.substring(1);
          this.materialsToEffects.put(this.materialId, effectInstanceAnchor);
        }
      } else if ("effect".equals(name)) {
        this.effectId = attributes.getValue("id");
        this.effects.put(this.effectId, new Effect());
      } else if (this.effectId != null) {
        if ("profile_COMMON".equals(parent) && "newparam".equals(name)) {
          this.newParamSid = attributes.getValue("sid");
        } else if ("newparam".equals(parent) && "surface".equals(name)
                   && "2D".equals(attributes.getValue("type"))) {
          this.inSurface2D = true;
        } else if ("extra".equals(parent) && "technique".equals(name)) {
          this.techniqueProfile = attributes.getValue("profile");
        } else if ("phong".equals(name) || "blinn".equals(name)) {
          this.inPhongBlinnOrLambert = true;
        } else if ("lambert".equals(name)) {
          this.inPhongBlinnOrLambert = true;
          effects.get(this.effectId).setSpecularColor(0, 0, 0);
          effects.get(this.effectId).shininess = 1;
        } else if ("constant".equals(name)) {
          this.inConstant = true;
        } else if (this.inConstant || this.inPhongBlinnOrLambert
                   && ("transparent".equals(name))) {
          this.opaque = attributes.getValue("opaque");
          if (this.opaque == null) {
            this.opaque = "A_ONE";
          }
        } else if ("texture".equals(name) && "diffuse".equals(parent)) {
          final String textureId = this.surface2DIds.get(this.sampler2DIds.get(attributes.getValue("texture")));
          final Effect effect = this.effects.get(this.effectId);
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                // Resolve texture at the end of the document
            	  effect.diffuseTexture = textures.get(textureId);
              }
            });
        }
      } else if ("geometry".equals(name)) {
        this.geometryId = attributes.getValue("id");
        this.geometries.put(this.geometryId, new Geometry());
      } else if (this.geometryId != null) {
        if ("mesh".equals(parent) && "source".equals(name)) {
          this.meshSourceId = attributes.getValue("id");
        } else if ("mesh".equals(parent) && "vertices".equals(name)) {
          this.verticesId = attributes.getValue("id");
        } else if (this.meshSourceId != null) {
          if ("float_array".equals(name)) {
            this.floatArrayId = attributes.getValue("id");
          } else if ("technique_common".equals(parent) && "accessor".equals(name)) {
            String floatArrayAnchor = attributes.getValue("source").substring(1);
            String stride = attributes.getValue("stride");
            this.sourceAccessorStrides.put(this.floatArrays.get(floatArrayAnchor),
                stride != null ? Integer.valueOf(stride) : 1);
          }
        } else if (this.verticesId != null && "input".equals(name)) {
          String sourceAnchor = attributes.getValue("source").substring(1);
          if ("POSITION".equals(attributes.getValue("semantic"))) {
            this.positions.put(this.verticesId, this.sources.get(sourceAnchor));
          } else if ("NORMAL".equals(attributes.getValue("semantic"))) {
            this.normals.put(this.verticesId, this.sources.get(sourceAnchor));
          } else if ("TEXCOORD".equals(attributes.getValue("semantic"))) {
            this.textureCoordinates.put(this.verticesId, this.sources.get(sourceAnchor));
          }
        } else if (this.verticesId == null && "input".equals(name)) {
          String sourceAnchor = attributes.getValue("source").substring(1);
          int offset = Integer.parseInt(attributes.getValue("offset"));
          if (this.inputCount < offset + 1) {
            this.inputCount = offset + 1;
          }
          if ("VERTEX".equals(attributes.getValue("semantic"))) {
            this.geometryVertices = this.positions.get(sourceAnchor);
            this.geometryVertexOffset = offset;
            if (this.geometryNormals == null) {
              this.geometryNormals = this.normals.get(sourceAnchor);
              this.geometryNormalOffset = offset;
            }
            if (this.geometryTextureCoordinates == null) {
              this.geometryTextureCoordinates = this.textureCoordinates.get(sourceAnchor);
              this.geometryTextureCoordinatesOffset = offset;
            }
          } else if ("NORMAL".equals(attributes.getValue("semantic"))) {
            this.geometryNormals = this.sources.get(sourceAnchor);
            this.geometryNormalOffset = offset;
          } else if ("TEXCOORD".equals(attributes.getValue("semantic"))) {
            this.geometryTextureCoordinates = this.sources.get(sourceAnchor);
            this.geometryTextureCoordinatesOffset = offset;
          }
        } else if ("triangles".equals(name)
                   || "trifans".equals(name)
                   || "tristrips".equals(name)
                   || "polylist".equals(name)
                   || "polygons".equals(name)
                   || "lines".equals(name)
                   || "linestrips".equals(name)) {
          this.subGeoMaterialSymbol = attributes.getValue("material");
          this.inputCount = 0;
          this.facesAndLinesPrimitives.clear();
          //this.polygonsPrimitives.clear();
          //this.polygonsHoles.clear();
          this.vcount = null;
        }
//      } else if ("visual_scene".equals(name)) {
//        TransformGroup visualSceneGroup = new TransformGroup();
//        //this.parentGroups.push(visualSceneGroup);
//        this.visualScenes.put(attributes.getValue("id"), visualSceneGroup);
      } else if ("node".equals(name)) {
    	  SceneNode node = new SceneNode();
		  node.id = attributes.getValue("id");

		  if (nodeStack.size() > 0) {
			  nodeStack.peek().subNodes.add(node);
		  }

		  nodeStack.push(node);

    	  if (parent.equals("visual_scene")) {
    		  // This is a root element
    		  rootNodes.add(node);
    	  }
    	  // Save a reference if it has a name
    	  if (node.id != null && !node.id.equals("")) {
    		  assert(!namedNodes.containsKey(node.id));
    		  namedNodes.put(node.id, node);
    	  }

      } else if ("node".equals(parent) && "instance_geometry".equals(name)) {
        String geometryInstanceUrl = attributes.getValue("url");
        if (!geometryInstanceUrl.startsWith("#")) {
        	assert false;
        	return;
        }
        final String geometryInstanceAnchor = geometryInstanceUrl.substring(1);
        //final Geometry geo = geometries.get(geometryInstanceAnchor);
        final GeoInstInfo geoInfo = new GeoInstInfo();
        geoInfo.geoName = geometryInstanceAnchor;
        currentGeoInst = geoInfo;
        nodeStack.peek().subGeo.add(geoInfo);

      } else if ("instance_node".equals(name)) {
        assert(parent.equals("node"));
        String nodeInstanceUrl = attributes.getValue("url");
        if (nodeInstanceUrl.startsWith("#") && nodeStack.size() > 0) {
          final String nodeInstanceAnchor = nodeInstanceUrl.substring(1);
          nodeStack.peek().subInstanceNames.add(nodeInstanceAnchor);
        }
      } else if ("instance_material".equals(name) /*&& !this.parentGroups.empty()*/) {
        String materialInstanceTarget = attributes.getValue("target");
        if (!materialInstanceTarget.startsWith("#")) {
          assert false;
        }

        final String materialInstanceAnchor = materialInstanceTarget.substring(1);
        final String materialInstanceSymbol = attributes.getValue("symbol");

        currentGeoInst.materialMap.put(materialInstanceSymbol, materialInstanceAnchor);

      }
      this.parentElements.push(name);
    }

    @Override
    public void characters(char [] ch, int start, int length) throws SAXException {
      this.buffer.append(ch, start, length);
    }

    @Override
    public void endElement(String uri, String localName, String name) throws SAXException {
      this.parentElements.pop();
      String parent = this.parentElements.isEmpty()
          ? null
          : this.parentElements.peek();

      if ("color".equals(name)
          || "float_array".equals(name)
          || "matrix".equals(name)
          || "rotate".equals(name)
          || "scale".equals(name)
          || "translate".equals(name)) {
        String [] floatValues = getCharacters().split("\\s");
        this.floats = new float [floatValues.length];
        int floatCount = 0;
        for (int i = 0; i < floatValues.length; i++) {
          if (floatValues [i].length() > 0) {
            float floatValue;
            try {
              floatValue = Float.parseFloat(floatValues [i]);
            } catch (NumberFormatException ex) {
		// This may happen with some bad DAE files
              System.out.println(ex.getMessage());
              floatValue = 0f;
            }
            this.floats [floatCount++] = floatValue;
          }
        }
        if (floatCount != floatValues.length) {
          float [] floats = new float [floatCount];
          System.arraycopy(this.floats, 0, floats, 0, floatCount);
          this.floats = floats;
        }
        if (this.floatArrayId != null) {
          this.floatArrays.put(this.floatArrayId, this.floats);
          this.floatArrayId = null;
        }
      } else if ("float".equals(name)) {
        this.floatValue = Float.parseFloat(getCharacters());
      }

      if (this.inRootAsset) {
        handleRootAssetElementsEnd(name);
      } else if ("image".equals(name)) {
        this.imageId = null;
      } else if (this.imageId != null) {
        handleImageElementsEnd(name);
      } else if ("material".equals(name)) {
        this.materialId = null;
      } else if ("effect".equals(name)) {
        this.effectId = null;
      } else if (this.effectId != null) {
        handleEffectElementsEnd(name, parent);
      } else if ("geometry".equals(name)) {
        this.geometryId = null;
      } if (this.geometryId != null) {
        handleGeometryElementsEnd(name, parent);
      } else if ("node".equals(name)) {
        this.nodeStack.pop();
      } else if ("node".equals(parent) && "rotate".equals(name)) {
        // Apply this rotation to our top level transform
        SceneNode node = nodeStack.peek();
        double rads = (float)Math.toRadians(floats[3]);
        Vector4d axis = new Vector4d(this.floats [0], this.floats [1], this.floats [2]);
        Quaternion rot = Quaternion.Rotation(rads, axis);
        Matrix4d rotMatrix = Matrix4d.RotationMatrix(rot);
        node.trans.mult(rotMatrix, node.trans);

      } else if ("node".equals(parent) && "scale".equals(name)) {
    	SceneNode node = nodeStack.peek();
        Matrix4d scaleMat = new Matrix4d();
        scaleMat.data[ 0] = this.floats[0];
        scaleMat.data[ 5] = this.floats[1];
        scaleMat.data[10] = this.floats[2];
        node.trans.mult(scaleMat, node.trans);

      } else if ("node".equals(parent) && "translate".equals(name)) {
      	SceneNode node = nodeStack.peek();
        Matrix4d transMat = Matrix4d.TranslationMatrix(new Vector4d(this.floats[0], this.floats[1], this.floats[2]));
        node.trans.mult(transMat, node.trans);
      } else if ("node".equals(parent) && "matrix".equals(name)) {
        SceneNode node = nodeStack.peek();
        if (floats.length >= 16)
        {
          // Convert to doubles
          double[] vals = new double[16];
          for (int i = 0; i < 16; ++i) {
            vals[i] = floats[i];
          }

          Matrix4d customMat = new Matrix4d(vals);
          customMat.transposeLocal(); // Transpose because collada is in row major order
          node.trans.mult(customMat, node.trans);
        }
      }
    }

    /**
     * Returns the trimmed string of last element value.
     */
    private String getCharacters() {
      return this.buffer.toString().trim();
    }

    /**
     * Handles the end tag of elements children of root "asset".
     */
    private void handleRootAssetElementsEnd(String name) {
      if ("asset".equals(name)) {
        this.inRootAsset = false;
      } else if ("up_axis".equals(name)) {
        this.axis = getCharacters();
//      } else if ("subject".equals(name)) {
//        this.scene.addDescription(getCharacters());
      } else if ("authoring_tool".equals(name)) {
        String tool = getCharacters();
        // Try to detect if DAE file was created by Google SketchUp version < 7.1
        if (tool.startsWith("Google SketchUp")) {
          String sketchUpVersion = tool.substring("Google SketchUp".length()).trim();
          if (sketchUpVersion.length() > 0) {
            int dotIndex = sketchUpVersion.indexOf('.');
            String majorVersionString = dotIndex == -1
                ? sketchUpVersion : sketchUpVersion.substring(0, dotIndex);
            try {
              int majorVersion = Integer.parseInt(majorVersionString);
              if (majorVersion < 7
                  || (majorVersion == 7
                      && (dotIndex >= sketchUpVersion.length() - 1 // No subversion
                          || sketchUpVersion.charAt(dotIndex + 1) < '1'))) {
                // From http://www.collada.org/public_forum/viewtopic.php?f=12&t=1667
                // let's reverse transparency
                this.reverseTransparency = true;
              }
            } catch (NumberFormatException ex) {
              // Too bad we won't know
              System.out.println(ex.getMessage());
            }
          }
        }
      }
    }

    /**
     * Handles the end tag of elements children of "image".
     */
    private void handleImageElementsEnd(String name) throws SAXException {
      if ("init_from".equals(name)) {
        try {
          URL textureURL = new URL(url, getCharacters());
          this.textures.put(this.imageId, textureURL);
        } catch (Exception ex) {
		// Ignore images at other format or not found
		System.out.println(ex.getMessage());
		assert false;
        }
      } else if ("data".equals(name)) {
        throw new SAXException("<data> not supported");
      }
    }

    /**
     * Handles the end tag of elements children of "effect".
     */
    private void handleEffectElementsEnd(String name, String parent) {
      if ("profile_COMMON".equals(parent) && "newparam".equals(name)) {
        this.newParamSid = null;
      } else if ("newparam".equals(parent) && "surface".equals(name)) {
        this.inSurface2D = false;
      } else if (this.newParamSid != null) {
        if (this.inSurface2D && "init_from".equals(name)) {
          this.surface2DIds.put(this.newParamSid, getCharacters());
        } else if ("sampler2D".equals(parent) && "source".equals(name)) {
          this.sampler2DIds.put(this.newParamSid, getCharacters());
        }
      } else if ("extra".equals(parent) && "technique".equals(name)) {
        this.techniqueProfile = null;
      } else if ("phong".equals(name) || "blinn".equals(name)
                 || "lambert".equals(name) || "constant".equals(name)) {
        float transparencyValue;
        if (this.transparentColor != null) {
          if ("RGB_ZERO".equals(this.opaque)) {
            transparencyValue = this.transparentColor [0] * 0.212671f
                + this.transparentColor [1] * 0.715160f
                + this.transparentColor [2] * 0.072169f;
            if (this.transparency != null) {
              transparencyValue *= this.transparency.floatValue();
            }
          } else { // A_ONE
            if (this.transparency != null) {
              transparencyValue = 1 - this.transparentColor [3] * this.transparency.floatValue();
            } else {
              transparencyValue = 1 - this.transparentColor [3];
            }
            if (this.reverseTransparency) {
              transparencyValue = 1 - transparencyValue;
            }
          }
        } else {
          transparencyValue = 0;
        }
//        if (transparencyValue > 0) {
//          appearance.setTransparencyAttributes(new TransparencyAttributes(
//              TransparencyAttributes.NICEST, transparencyValue)); // 0 means opaque in Java 3D
//        } else {
        // Set default color if it doesn't exist yet
//        Color3f black = new Color3f();
//        if (appearance.getMaterial() == null) {
//          appearance.setMaterial(new Material(black, black, black, black, 1));
//          appearance.setColoringAttributes(new ColoringAttributes(black, ColoringAttributes.SHADE_GOURAUD));
//          }
//        }
        this.transparentColor = null;
        this.transparency = null;

        this.inPhongBlinnOrLambert = false;
        this.inConstant = false;
      } else if (this.inConstant || this.inPhongBlinnOrLambert) {
        // Set appearance attributes
        if ("color".equals(name)) {
          if ("emission".equals(parent)) {
            if (this.inPhongBlinnOrLambert) {
//              getAppearanceMaterial(this.effectId).setEmissiveColor(
//                  this.floats [0], this.floats [1], this.floats [2]);
            } else { // inConstant
//              this.effectAppearances.get(this.effectId).setColoringAttributes(new ColoringAttributes(
//                  this.floats [0], this.floats [1], this.floats [2], ColoringAttributes.SHADE_GOURAUD));
            }
          } else if ("ambient".equals(parent)) {
//            getAppearanceMaterial(this.effectId).setAmbientColor(
//                this.floats [0], this.floats [1], this.floats [2]);
          } else if ("diffuse".equals(parent)) {
            effects.get(this.effectId).diffuseColor = new Vector4d(this.floats [0], this.floats [1], this.floats [2]);
          } else if ("specular".equals(parent)) {
//            getAppearanceMaterial(this.effectId).setSpecularColor(
//                this.floats [0], this.floats [1], this.floats [2]);
          } else if ("transparent".equals(parent)) {
            this.transparentColor = this.floats;
          }
        } else if ("float".equals(name)) {
          if ("shininess".equals(parent)) {
            effects.get(this.effectId).shininess = this.floatValue;
          } else if ("transparency".equals(parent)) {
            this.transparency = this.floatValue;
          }
        }
//      } else if ("double_sided".equals(name)
//                 && "1".equals(getCharacters())
//                 && ("GOOGLEEARTH".equals(this.techniqueProfile)
//                     || "MAX3D".equals(this.techniqueProfile)
//                     || "MAYA".equals(this.techniqueProfile))) {
//        this.effectAppearances.get(this.effectId).setPolygonAttributes(new PolygonAttributes(
//            PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0));
      }
    }

    /**
     * Returns the material of the appearance at <code>effectId</code>.
     */
//    private Material getAppearanceMaterial(String effectId) {
//      Appearance appearance = this.effectAppearances.get(effectId);
//      Material material = appearance.getMaterial();
//      if (material == null) {
//        material = new Material();
//        appearance.setMaterial(material);
//      }
//      return material;
//    }

    /**
     * Handles the end tag of elements children of "geometry".
     */
    private void handleGeometryElementsEnd(String name, String parent) {
      if ("mesh".equals(parent) && "source".equals(name)) {
        this.sources.put(this.meshSourceId, this.floats);
        this.meshSourceId = null;
      } else if ("mesh".equals(parent) && "vertices".equals(name)) {
        this.verticesId = null;
      } else if ("p".equals(name)
                 //|| "h".equals(name)
                 || "vcount".equals(name)) {
        // Get integers
        String [] intValues = getCharacters().split("\\s");
        int [] integers = new int [intValues.length];
        int intCount = 0;
        for (int i = 0; i < intValues.length; i++) {
          if (intValues [i].length() > 0) {
            integers [intCount++] = Integer.parseInt(intValues [i]);
          }
        }
        if (intCount != intValues.length) {
          int [] ints = new int [intCount];
          System.arraycopy(integers, 0, ints, 0, intCount);
          integers = ints;
        }

        if (!"ph".equals(parent) && "p".equals(name)) {
          this.facesAndLinesPrimitives.add(integers);
        } else if ("vcount".equals(name)) {
          this.vcount = integers;
        } else if ("ph".equals(parent)) {
          assert false; // We don't like holey polygons
//          if ("p".equals(name)) {
//            this.polygonsPrimitives.add(integers);
//          } else if ("h".equals(name)) {
//            if (this.polygonsPrimitives.size() > this.polygonsHoles.size()) {
//              this.polygonsHoles.add(new ArrayList<int[]>());
//            }
//            this.polygonsHoles.get(this.polygonsPrimitives.size() - 1).add(integers);
//          }
        }
      } else if ("triangles".equals(name)
                 || "trifans".equals(name)
                 || "tristrips".equals(name)
                 || "polylist".equals(name)
                 || "polygons".equals(name)) {

        FaceSubGeo subGeo = getFacesGeometry(name);
        this.geometries.get(this.geometryId).faceSubGeos.add(subGeo);
        subGeo.materialSymbol = this.subGeoMaterialSymbol;

        this.subGeoMaterialSymbol = null;
        this.geometryVertices = null;
        this.geometryNormals = null;
        this.geometryTextureCoordinates = null;
        this.facesAndLinesPrimitives.clear();
        this.vcount = null;
    } else if ("lines".equals(name)
            || "linestrip".equals(name)) {
   LineSubGeo subGeo = getLineGeometry(name);
   this.geometries.get(this.geometryId).lineSubGeos.add(subGeo);
   subGeo.materialSymbol = this.subGeoMaterialSymbol;

   this.subGeoMaterialSymbol = null;
   this.geometryVertices = null;
   this.geometryNormals = null;
   this.geometryTextureCoordinates = null;
   this.facesAndLinesPrimitives.clear();
   this.vcount = null;
 }
    }

    /**
     * Returns the triangles or polygons geometry matching the read values.
     */
    private FaceSubGeo getFacesGeometry(String name) {
      // Make sure the strides are what we expect
      //TODO: accept other strides
      assert this.sourceAccessorStrides.get(this.geometryVertices) == 3;
      assert this.sourceAccessorStrides.get(this.geometryNormals) == 3;
      if (this.geometryTextureCoordinates != null) {
          assert this.sourceAccessorStrides.get(this.geometryTextureCoordinates) == 2;
      }

      if ("triangles".equals(name)) {
    	  return getTrianglesGeo();
      } else if ("trifans".equals(name)) {
    	  //TODO
    	  //return getTrifanGeo();
      } else if ("tristrips".equals(name)) {
    	  //TODO
    	  //return getTristripGeo();
      } else if ("polygons".equals(name)) {
    	  //TODO
    	  //return getPolygonGeo();
      } else if ("polylist".equals(name)) {
    	  return getPolylistGeo();
      } else {
        assert false;
      }

      return null;

    }

    private LineSubGeo getLineGeometry(String name) {
      if ("lines".equals(name)) {
        return getLinesGeo();
      }else if ("linestrip".equals(name)) {
          return getLinestripGeo();
      }
      return null;
    }

    /**
     * Returns a SubGeo populated with triangle primitives by interpreting the cached primitives
     * as a list of triangles
     * @return
     */
    private FaceSubGeo getTrianglesGeo() {
      assert this.facesAndLinesPrimitives.size() == 1;

      boolean hasTexCoords = this.geometryTextureCoordinates != null;

      int[] vertIndices = getIndices(this.geometryVertexOffset);
      int[] normIndices = getIndices(this.geometryNormalOffset);
      int [] texIndices = null;
      if (hasTexCoords) {
        texIndices = getIndices(this.geometryTextureCoordinatesOffset);
      }
      assert vertIndices.length == normIndices.length;
      if (hasTexCoords) {
        assert vertIndices.length == texIndices.length;
      }
      assert vertIndices.length % 3 == 0;

      FaceSubGeo ret = new FaceSubGeo(vertIndices.length, hasTexCoords);

      // Write the vertices out directly for triangles (this is the format we want
      for (int i = 0; i < vertIndices.length; ++i) {
        addFaceVert(ret, i, i, vertIndices, normIndices, texIndices, hasTexCoords);
      }

      return ret;
    }

    private FaceSubGeo getPolylistGeo() {
      boolean hasTexCoords = this.geometryTextureCoordinates != null;

      assert this.facesAndLinesPrimitives.size() == 1;

      int[] vertIndices = getIndices(this.geometryVertexOffset);
      int[] normIndices = getIndices(this.geometryNormalOffset);
      int[] texIndices = null;
        if (hasTexCoords) {
          texIndices = getIndices(this.geometryTextureCoordinatesOffset);
        }

      int numOutVerts = 0;
      for (int vertCount : this.vcount) {
        assert vertCount >= 3;
        numOutVerts += (vertCount - 2) * 3; // (vertCount - 2) triangles per polygon, assuming convex
      }

      FaceSubGeo ret = new FaceSubGeo(numOutVerts, hasTexCoords);

      // Handle the individual polygons
      int inIndex = 0; // The index of the verts being read in
      int outIndex = 0; // The index of the verts being written to the SubGeo
      for (int vertCount : this.vcount) {
        for (int i = 1; i < vertCount - 1; ++ i) {
          addFaceVert(ret, outIndex++, inIndex +   0, vertIndices, normIndices, texIndices, hasTexCoords);
          addFaceVert(ret, outIndex++, inIndex +   i, vertIndices, normIndices, texIndices, hasTexCoords);
          addFaceVert(ret, outIndex++, inIndex + i+1, vertIndices, normIndices, texIndices, hasTexCoords);
        }
        inIndex += vertCount;
      }

      assert outIndex == numOutVerts;
      return ret;
    }

    /**
     * Utility, add a vertex (all of position, normal and texture) to the SubGeo at 'outIndex'
     * based on the supplied list of indices and the 'inIndex'. Assumes normal strides for components
     * @param geo
     * @param outIndex
     * @param inIndex
     * @param vertIndices
     * @param normIndices
     * @param texIndices
     */
    private void addFaceVert(FaceSubGeo geo, int outIndex, int inIndex, int[] vertIndices,
                         int[] normIndices, int[] texIndices, boolean hasTex) {
        int vertInd = vertIndices[inIndex];
        int normInd = normIndices[inIndex];
        int texInd = 0;
        if (hasTex) {
          texInd = texIndices[inIndex];
        }

    	geo.verts[outIndex] = new Vector4d(this.geometryVertices[vertInd*3 + 0],
                                          this.geometryVertices[vertInd*3 + 1],
                                          this.geometryVertices[vertInd*3 + 2],
                                          1);
        geo.normals[outIndex] = new Vector4d(this.geometryNormals[normInd*3 + 0],
                                            this.geometryNormals[normInd*3 + 1],
                                            this.geometryNormals[normInd*3 + 2],
                                            0);

        if (hasTex) {
          geo.texCoords[outIndex] = new Vector4d(this.geometryTextureCoordinates[texInd*2 + 0],
                                                this.geometryTextureCoordinates[texInd*2 + 1],
                                                0,
                                                1);
        }

    }

    private LineSubGeo getLinesGeo() {
        int[] vertIndices = getIndices(this.geometryVertexOffset);

        assert vertIndices.length % 2 == 0;

        LineSubGeo ret = new LineSubGeo(vertIndices.length);

        // Write the vertices out directly for triangles (this is the format we want
        for (int i = 0; i < vertIndices.length; ++i) {
          int vertInd = vertIndices[i];
          ret.verts[i] = new Vector4d(this.geometryVertices[vertInd*3 + 0],
                                      this.geometryVertices[vertInd*3 + 1],
                                      this.geometryVertices[vertInd*3 + 2]);
        }

        return ret;

    }

    private LineSubGeo getLinestripGeo() {
        int[] vertIndices = getIndices(this.geometryVertexOffset);

        assert vertIndices.length % 2 == 0;

        LineSubGeo ret = new LineSubGeo(vertIndices.length);

        // Write the vertices out directly for triangles (this is the format we want
        for (int i = 0; i < vertIndices.length - 1; ++i) {
          int vertInd = vertIndices[i];
          ret.verts[i] = new Vector4d(this.geometryVertices[vertInd*3 + 0],
                                      this.geometryVertices[vertInd*3 + 1],
                                      this.geometryVertices[vertInd*3 + 2]);
          ret.verts[i] = new Vector4d(this.geometryVertices[(vertInd+1)*3 + 0],
                                      this.geometryVertices[(vertInd+1)*3 + 1],
                                      this.geometryVertices[(vertInd+1)*3 + 2]);
        }

        return ret;

    }

    /**
     * Returns the lines geometry matching the read values.
     */
//    private Geometry getLinesGeometry(String name) {
//      int format = IndexedGeometryArray.COORDINATES;
//      if (this.geometryNormals != null) {
//        format |= IndexedGeometryArray.NORMALS;
//      }
//      if (this.geometryTextureCoordinates != null) {
//        format |= IndexedGeometryArray.TEXTURE_COORDINATE_2;
//      }
//
//      int [] coordinatesIndices = getIndices(this.geometryVertexOffset);
//      IndexedGeometryArray geometry;
//      if ("lines".equals(name)) {
//        geometry = new IndexedLineArray(this.geometryVertices.length / 3, format, coordinatesIndices.length);
//      } else { // linestrips
//        int [] stripCounts = new int [this.facesAndLinesPrimitives.size()];
//        for (int i = 0; i < stripCounts.length; i++) {
//          stripCounts [i] = this.facesAndLinesPrimitives.get(i).length / this.inputCount;
//        }
//        geometry = new IndexedLineStripArray(this.geometryVertices.length / 3, format, coordinatesIndices.length, stripCounts);
//      }
//
//      geometry.setCoordinates(0, this.geometryVertices);
//      geometry.setCoordinateIndices(0, coordinatesIndices);
//      if (this.geometryNormals != null) {
//        geometry.setNormals(0, this.geometryNormals);
//        geometry.setNormalIndices(0, getIndices(this.geometryNormalOffset));
//      }
//      if (this.geometryTextureCoordinates != null) {
//        geometry.setTextureCoordinates(0, 0, this.geometryTextureCoordinates);
//        geometry.setTextureCoordinateIndices(0, 0, getIndices(this.geometryTextureCoordinatesOffset));
//      }
//      return geometry;
//    }

    /**
     * Returns the indices at the given <code>indexOffset</code>.
     */
    private int [] getIndices(int indexOffset) {
//      if (this.facesAndLinesPrimitives.size() == 1 && this.polygonsPrimitives.size() == 1 && this.inputCount == 1) {
//        return facesAndLinesPrimitives.get(0);
//      }

      int indexCount = 0;
      for (int[] ints : this.facesAndLinesPrimitives) {
    	  indexCount += ints.length;
      }
//      indexCount += getIndexCount(this.polygonsPrimitives);
//      for (List<int []> polygonHole : this.polygonsHoles) {
//        indexCount += getIndexCount(polygonHole);
//      }

      int [] indices = new int [indexCount / this.inputCount];
      int i = 0;
      for (int [] primitives : this.facesAndLinesPrimitives) {
        for (int k = indexOffset; k < primitives.length; k += this.inputCount) {
          indices [i++] = primitives [k];
        }
      }
//      for (int j = 0; j < this.polygonsPrimitives.size(); j++) {
//        int [] polygonPrimitives = polygonsPrimitives.get(j);
//        for (int k = indexOffset; k < polygonPrimitives.length; k += this.inputCount) {
//          indices [i++] = polygonPrimitives [k];
//        }
//        for (int [] polygonHole : this.polygonsHoles.get(j)) {
//          for (int k = indexOffset; k < polygonHole.length; k += this.inputCount) {
//            indices [i++] = polygonHole [k];
//          }
//        }
//      }
      return indices;
    }

    /**
     * Returns the total count of indices among the given <code>faceIndices</code>.
     */
//    private int getIndexCount(List<int []> faceIndices) {
//      int indexCount = 0;
//      for (int [] indices : faceIndices) {
//        indexCount += indices.length;
//      }
//      return indexCount;
//    }

    private void addGeoInst(GeoInstInfo geoInfo, Matrix4d currentMat) {
      Geometry geo = geometries.get(geoInfo.geoName);

      for (FaceSubGeo subGeo : geo.faceSubGeos) {
        // Check if this geometry and material pair has been loaded yet
        String materialId = geoInfo.materialMap.get(subGeo.materialSymbol);
        String effectId = materialsToEffects.get(materialId);
        Effect effect = effects.get(effectId);

        FaceGeoEffectPair ge = new FaceGeoEffectPair(subGeo, effect);
        int geoID;
        if (loadedFaceGeos.contains(ge)) {
          geoID = loadedFaceGeos.indexOf(ge);
        } else {
          geoID = loadedFaceGeos.size();
          loadedFaceGeos.add(ge);
          meshProto.addSubMesh(subGeo.verts,
                               subGeo.normals,
                               subGeo.texCoords,
                               effect.diffuseTexture,
                               effect.diffuseColor,
                               MeshProto.NO_TRANS, Vector4d.ORIGIN);
        }

        // Now load an instance
        Matrix4d normalMat = currentMat.inverse();
        normalMat.transposeLocal();
        meshProto.addSubMeshInstance(geoID, currentMat, normalMat);
      }

      for (LineSubGeo subGeo : geo.lineSubGeos) {
        // Check if this geometry and material pair has been loaded yet
        String materialId = geoInfo.materialMap.get(subGeo.materialSymbol);
        String effectId = materialsToEffects.get(materialId);
        Effect effect = effects.get(effectId);

        LineGeoEffectPair ge = new LineGeoEffectPair(subGeo, effect);
        int geoID;
        if (loadedLineGeos.contains(ge)) {
          geoID = loadedLineGeos.indexOf(ge);
        } else {
          geoID = loadedLineGeos.size();
          loadedLineGeos.add(ge);
          meshProto.addSubLine(subGeo.verts,
                               effect.diffuseColor);
        }

        // Now load an instance
        Matrix4d normalMat = currentMat.inverse();
        normalMat.transposeLocal();
        meshProto.addSubLineInstance(geoID, currentMat, normalMat);
      }

    }

    // Visit the scene one node at a time and build up the output geometry
    private void visitNode(SceneNode node, Matrix4d parentMat) {
      nodeStack.push(node); // Mark this node as active

      // Update the current transform
      Matrix4d currentMat = new Matrix4d(parentMat);
      currentMat.mult(node.trans, currentMat);

      for (GeoInstInfo geoInfo : node.subGeo) {
        addGeoInst(geoInfo, currentMat);
      }

      // Add instance_node
      for (String nodeName : node.subInstanceNames) {
        SceneNode instNode = namedNodes.get(nodeName);
        // Check for reference loops, make sure this node is not currently in the active node stack
        assert(!nodeStack.contains(instNode));

        node.subNodes.add(instNode);
      }

      // Finally continue visiting the scene
      for (SceneNode nextNode : node.subNodes) {
        visitNode(nextNode, currentMat);
      }

      nodeStack.pop();
    }


    @Override
    public void endDocument() throws SAXException {
      for (Runnable runnable : this.postProcessingBinders) {
        runnable.run();
      }

      // Build up the scene

      // Repurpose the node stack to help us detect loops in the node references
      nodeStack.clear();

      Matrix4d globalMat = new Matrix4d();
      if (this.axis != null) {
    	  if (this.axis.equals("X_UP")) {
    		  globalMat = Matrix4d.RotationMatrix(Math.PI/2, Vector4d.Y_AXIS);
    		  globalMat.mult(Matrix4d.RotationMatrix(-Math.PI/2, Vector4d.Z_AXIS), globalMat);
    	  } else if (this.axis.equals("Y_UP")) {
    		  globalMat = Matrix4d.RotationMatrix(Math.PI/2, Vector4d.X_AXIS);
    	  }
      }

      for (SceneNode node : rootNodes) {
    	  visitNode(node, globalMat);
      }
    }
  } // class DAEHandler
}
