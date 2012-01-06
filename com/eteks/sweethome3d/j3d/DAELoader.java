/*
 * DAELoader.java 16 Mai 2010
 *
 * Copyright (c) 2010 Emmanuel PUYBARET / eTeks <info@eteks.com>. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package com.eteks.sweethome3d.j3d;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.imageio.ImageIO;
import javax.media.j3d.Appearance;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.Bounds;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.Group;
import javax.media.j3d.IndexedGeometryArray;
import javax.media.j3d.IndexedLineArray;
import javax.media.j3d.IndexedLineStripArray;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.Texture;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.AxisAngle4f;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;
import javax.vecmath.Vector3f;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.sandwell.JavaSimulation3D.InputAgent;
import com.sun.j3d.loaders.IncorrectFormatException;
import com.sun.j3d.loaders.Loader;
import com.sun.j3d.loaders.LoaderBase;
import com.sun.j3d.loaders.ParsingErrorException;
import com.sun.j3d.loaders.Scene;
import com.sun.j3d.loaders.SceneBase;
import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;
import com.sun.j3d.utils.image.TextureLoader;

/**
 * A loader for DAE Collada 1.4.1 format as specified by
 * <a href="http://www.khronos.org/files/collada_spec_1_4.pdf">http://www.khronos.org/files/collada_spec_1_4.pdf</a>.
 * All texture coordinates are considered to belong to the same set (for example UVSET0).<br>
 * Note: this class is compatible with Java 3D 1.3.
 * @author Emmanuel Puybaret
 * @author apptaro (bug fixes)
 */
public class DAELoader extends LoaderBase implements Loader {
   /**
   * Returns the scene described in the given DAE file.
   */
  public Scene load(String file) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
    URL baseUrl;
    try {
      if (this.basePath != null) {
        baseUrl = new File(this.basePath).toURI().toURL();
      } else {
        baseUrl = new File(file).toURI().toURL();
      } 
    } catch (MalformedURLException ex) {
      throw new FileNotFoundException(file);
    }
    return load(new BufferedInputStream(new FileInputStream(file)), baseUrl);
  }

  /**
   * Returns the scene described in the given DAE file url.
   */
  public Scene load(URL url) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
    URL baseUrl = this.baseUrl;
    if (this.baseUrl == null) {
      baseUrl = url;
    } 
    InputStream in;
    try {
      in = url.openStream();
    } catch (IOException ex) {
      throw new FileNotFoundException("Can't read " + url);
    }
    return load(new BufferedInputStream(in), baseUrl);
  }

  /**
   * Returns the scene described in the given DAE file stream.
   */
  public Scene load(Reader reader) throws FileNotFoundException, IncorrectFormatException, ParsingErrorException {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the scene described in the given DAE file.
   */
  private Scene load(InputStream in, URL baseUrl) throws FileNotFoundException {
    try {
      return parseXMLStream(in, baseUrl);
    } catch (IOException ex) {
      throw new ParsingErrorException(ex.getMessage());
    } finally {
      try {
        in.close();
      } catch (IOException ex) {
        throw new ParsingErrorException(ex.getMessage());
      }
    }
  }

  /**
   * Returns the scene parsed from a Collada XML stream. 
   */
  private Scene parseXMLStream(InputStream in, 
                               URL baseUrl) throws IOException {
    try {
      SceneBase scene = new SceneBase();
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setValidating(false);
      SAXParser saxParser = factory.newSAXParser();
      saxParser.parse(in, new DAEHandler(scene, baseUrl));
      return scene;
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
  
  /**
   * SAX handler for DAE Collada stream.
   */
  private static class DAEHandler extends DefaultHandler {
    private final SceneBase scene;
    private final URL       baseUrl;
    private final Stack<Group>   parentGroups = new Stack<Group>();
    private final Stack<String>  parentElements = new Stack<String>();
    private final StringBuilder  buffer = new StringBuilder();
    private final List<Runnable> postProcessingBinders = new ArrayList<Runnable>();

    private final Map<String, Texture> textures = new HashMap<String, Texture>();
    private final Map<String, Appearance> effectAppearances = new HashMap<String, Appearance>();
    private final Map<String, String> materialEffects = new HashMap<String, String>();
    private final Map<String, String> surface2DIds = new HashMap<String, String>();
    private final Map<String, String> sampler2DIds = new HashMap<String, String>();
    private final Map<String, List<Geometry>> geometries = new HashMap<String, List<Geometry>>();
    private final Map<String, float []> sources = new HashMap<String, float []>();
    private final Map<String, float []> positions = new HashMap<String, float []>();
    private final Map<String, float []> normals = new HashMap<String, float []>();
    private final Map<String, float []> textureCoordinates = new HashMap<String, float []>();
    private final Map<String, float []> floatArrays = new HashMap<String, float []>();
    private final Map<float [], Integer> sourceAccessorStrides = new HashMap<float [], Integer>();
    private final Map<Geometry, String> geometryAppearances = new HashMap<Geometry, String>();
    private final List<int []> facesAndLinesPrimitives = new ArrayList<int[]>();
    private final List<int []> polygonsPrimitives = new ArrayList<int[]>();
    private final List<List<int []>> polygonsHoles = new ArrayList<List<int[]>>();
    private final Map<String, TransformGroup> nodes = new HashMap<String, TransformGroup>();
    private final Map<String, SharedGroup> instantiatedNodes = new HashMap<String, SharedGroup>();
    private final Map<String, TransformGroup> visualScenes = new HashMap<String, TransformGroup>();
    private TransformGroup visualScene;
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
    private String  techniqueProfile;
    private boolean inConstant;
    private String  geometryId;
    private String  meshSourceId;
    private String  verticesId;
    private String  floatArrayId;
    private String  geometryAppearance;
    private int     geometryVertexOffset;
    private int     geometryNormalOffset;
    private int     geometryTextureCoordinatesOffset;
    private String  axis;
    private float   floatValue;
    private String  opaque;
    private int     inputCount;

    public DAEHandler(SceneBase scene, URL baseUrl) {
      this.scene = scene;
      this.baseUrl = baseUrl;
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
          this.materialEffects.put(this.materialId, effectInstanceAnchor);
        }
      } else if ("effect".equals(name)) {
        this.effectId = attributes.getValue("id");
        this.effectAppearances.put(this.effectId, new Appearance());
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
          getAppearanceMaterial(this.effectId).setSpecularColor(0, 0, 0);
          getAppearanceMaterial(this.effectId).setShininess(1);
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
          final Appearance appearance = this.effectAppearances.get(this.effectId);
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                // Resolve texture at the end of the document
                appearance.setTexture(textures.get(textureId));
              }
            });
        }
      } else if ("geometry".equals(name)) {
        this.geometryId = attributes.getValue("id");
        this.geometries.put(this.geometryId, new ArrayList<Geometry>());
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
          this.geometryAppearance = attributes.getValue("material");
          this.inputCount = 0;
          this.facesAndLinesPrimitives.clear();
          this.polygonsPrimitives.clear();
          this.polygonsHoles.clear();
          this.vcount = null;
        } 
      } else if ("visual_scene".equals(name)) {
        TransformGroup visualSceneGroup = new TransformGroup();
        this.parentGroups.push(visualSceneGroup);
        this.visualScenes.put(attributes.getValue("id"), visualSceneGroup);
      } else if ("node".equals(name)) {
        TransformGroup nodeGroup = new TransformGroup();
        if (this.parentGroups.size() > 0) {
          // Add node to parent node only for children nodes
          this.parentGroups.peek().addChild(nodeGroup);
        }
        this.parentGroups.push(nodeGroup);
        this.nodes.put(attributes.getValue("id"), nodeGroup);
        String nodeName = attributes.getValue("name").toUpperCase();
        if (nodeName != null) {
			nodeGroup.setName(nodeName);

          this.scene.addNamedObject(nodeName, nodeGroup);
        }
      } else if ("node".equals(parent) && "instance_geometry".equals(name)) {
        String geometryInstanceUrl = attributes.getValue("url");
        if (geometryInstanceUrl.startsWith("#")) {
          final String geometryInstanceAnchor = geometryInstanceUrl.substring(1);
          final String nodeName = attributes.getValue("name");
          final Group parentGroup = new Group();
          this.parentGroups.peek().addChild(parentGroup);
          this.parentGroups.push(parentGroup);
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                int nameSuffix = 0;
                // Resolve URL at the end of the document
                for (Geometry geometry : geometries.get(geometryInstanceAnchor)) {
                  Shape3D shape = new Shape3D(geometry);
                  parentGroup.addChild(shape);
                  // Give a name to shape 
                  if (nodeName != null) {
                    if (nameSuffix == 0) {
                      scene.addNamedObject(nodeName, shape);
                    } else {
                      scene.addNamedObject(nodeName + "_" + nameSuffix, shape);
                    }
                    nameSuffix++;
                  }
                }
              }
            });
        }
      } else if ("instance_node".equals(name)) {
        String nodeInstanceUrl = attributes.getValue("url");
        if (nodeInstanceUrl.startsWith("#")) {
          final String nodeInstanceAnchor = nodeInstanceUrl.substring(1);
          final Group parentTransformGroup = this.parentGroups.peek();
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                // Resolve URL at the end of the document
                SharedGroup sharedGroup = instantiatedNodes.get(nodeInstanceAnchor);
                if (sharedGroup == null) {
                  sharedGroup = new SharedGroup();
                  sharedGroup.addChild(nodes.get(nodeInstanceAnchor));
                  instantiatedNodes.put(nodeInstanceAnchor, sharedGroup);
                }
                parentTransformGroup.addChild(new Link(sharedGroup));
                if(! scene.getNamedObjects().contains("JaamSim-HasSharedGroup")) {
                	scene.addNamedObject("JaamSim-HasSharedGroup", Boolean.TRUE);
                }
              }
            });
        }
      } else if ("instance_material".equals(name) && !this.parentGroups.empty()) {
        String materialInstanceTarget = attributes.getValue("target");
        if (materialInstanceTarget.startsWith("#")) {
          final String materialInstanceAnchor = materialInstanceTarget.substring(1);
          final String materialInstanceSymbol = attributes.getValue("symbol");
          final Group group = this.parentGroups.peek();
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                updateShapeAppearance(group, 
                    effectAppearances.get(materialEffects.get(materialInstanceAnchor)));
              }
              
              private void updateShapeAppearance(Node node, Appearance appearance) {
                if (node instanceof Group) {
                  Enumeration<?> enumeration = ((Group)node).getAllChildren();
                  while (enumeration.hasMoreElements ()) {
                    updateShapeAppearance((Node)enumeration.nextElement(), appearance);
                  }
                } else if (node instanceof Link) {
                  updateShapeAppearance(((Link)node).getSharedGroup(), appearance);
                } else if (node instanceof Shape3D) {
                  if (materialInstanceSymbol.equals(geometryAppearances.get(((Shape3D)node).getGeometry()))) {
                    ((Shape3D)node).setAppearance(appearance);
                  }
                }
              }
            });
        }
      } else if ("instance_visual_scene".equals(name)) {
        String visualSceneInstanceUrl = attributes.getValue("url");
        if (visualSceneInstanceUrl.startsWith("#")) {
          final String visualSceneInstanceAnchor = visualSceneInstanceUrl.substring(1);
          this.postProcessingBinders.add(new Runnable() {
              public void run() {
                // Resolve URL at the end of the document
                visualScene = visualScenes.get(visualSceneInstanceAnchor);
              }
            });
        }
      } else if("unit".equals(name)) {
    	  scene.addNamedObject("JaamSim-UnitMeter", Double.parseDouble(attributes.getValue("meter")));
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
		InputAgent.logWarning("%s: \"bad DAE file: %s", this.baseUrl, ex);
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
      } else if ("visual_scene".equals(name)
              || "node".equals(name)
              || "node".equals(parent) && "instance_geometry".equals(name)) {
        this.parentGroups.pop();
      } else if ("matrix".equals(name)) {
        mulTransformGroup(new Transform3D(this.floats));
      } else if ("node".equals(parent) && "rotate".equals(name)) {
        Transform3D rotation = new Transform3D();
        rotation.setRotation(new AxisAngle4f(this.floats [0], this.floats [1], this.floats [2], 
            (float)Math.toRadians(floats[3])));
        mulTransformGroup(rotation);
      } else if ("scale".equals(name)) {
        Transform3D scale = new Transform3D();
        scale.setScale(new Vector3d(this.floats [0], this.floats [1], this.floats [2]));
        mulTransformGroup(scale);
      } else if ("node".equals(parent) && "translate".equals(name)) {
        Transform3D translation = new Transform3D();
        translation.setTranslation(new Vector3f(this.floats [0], this.floats [1], this.floats [2]));
        mulTransformGroup(translation);
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
      } else if ("subject".equals(name)) {
        this.scene.addDescription(getCharacters());
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
		InputAgent.logWarning("%s: \"bad DAE file: %s", this.baseUrl, ex);
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
          URL textureImageUrl = new URL(baseUrl, getCharacters());
          BufferedImage textureImage = ImageIO.read(textureImageUrl);
          if (textureImage != null) {
            TextureLoader textureLoader = new TextureLoader(textureImage);
            Texture texture = textureLoader.getTexture();
            // Keep in user data the URL of the texture image
            texture.setUserData(textureImageUrl);
            this.textures.put(this.imageId, texture);
          }
        } catch (IOException ex) {
		// Ignore images at other format or not found
		InputAgent.logWarning("%s: \"%s: %s\"", this.baseUrl, ex, getCharacters());
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
        Appearance appearance = this.effectAppearances.get(this.effectId);
        if (transparencyValue > 0) {
          appearance.setTransparencyAttributes(new TransparencyAttributes(
              TransparencyAttributes.NICEST, transparencyValue)); // 0 means opaque in Java 3D
        } else {
          // Set default color if it doesn't exist yet
          Color3f black = new Color3f();
          if (appearance.getMaterial() == null) {
            appearance.setMaterial(new Material(black, black, black, black, 1));
            appearance.setColoringAttributes(new ColoringAttributes(black, ColoringAttributes.SHADE_GOURAUD));
          }
        }
        this.transparentColor = null;
        this.transparency = null;
        
        this.inPhongBlinnOrLambert = false;
        this.inConstant = false;
      } else if (this.inConstant || this.inPhongBlinnOrLambert) {
        // Set appearance attributes
        if ("color".equals(name)) {
          if ("emission".equals(parent)) {
            if (this.inPhongBlinnOrLambert) {
              getAppearanceMaterial(this.effectId).setEmissiveColor(
                  this.floats [0], this.floats [1], this.floats [2]);
            } else { // inConstant
              this.effectAppearances.get(this.effectId).setColoringAttributes(new ColoringAttributes(
                  this.floats [0], this.floats [1], this.floats [2], ColoringAttributes.SHADE_GOURAUD));
            }
          } else if ("ambient".equals(parent)) {
            getAppearanceMaterial(this.effectId).setAmbientColor(
                this.floats [0], this.floats [1], this.floats [2]);
          } else if ("diffuse".equals(parent)) {
            getAppearanceMaterial(this.effectId).setDiffuseColor(
                this.floats [0], this.floats [1], this.floats [2], this.floats [3]);
            // Always set coloring attributes in case geometries don't contain normals
            this.effectAppearances.get(this.effectId).setColoringAttributes(new ColoringAttributes(
                this.floats [0], this.floats [1], this.floats [2], ColoringAttributes.SHADE_GOURAUD));
          } else if ("specular".equals(parent)) {
            getAppearanceMaterial(this.effectId).setSpecularColor(
                this.floats [0], this.floats [1], this.floats [2]);
          } else if ("transparent".equals(parent)) {
            this.transparentColor = this.floats;
          }
        } else if ("float".equals(name)) {
          if ("shininess".equals(parent)) {
            getAppearanceMaterial(this.effectId).setShininess(this.floatValue);
          } else if ("transparency".equals(parent)) {
            this.transparency = this.floatValue;
          }
        }
      } else if ("double_sided".equals(name) 
                 && "1".equals(getCharacters())
                 && ("GOOGLEEARTH".equals(this.techniqueProfile)
                     || "MAX3D".equals(this.techniqueProfile)
                     || "MAYA".equals(this.techniqueProfile))) {
        this.effectAppearances.get(this.effectId).setPolygonAttributes(new PolygonAttributes(
            PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0));
      }
    }

    /**
     * Returns the material of the appearance at <code>effectId</code>. 
     */
    private Material getAppearanceMaterial(String effectId) {
      Appearance appearance = this.effectAppearances.get(effectId);
      Material material = appearance.getMaterial();
      if (material == null) {
        material = new Material();
        appearance.setMaterial(material);
      }
      return material;
    }

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
                 || "h".equals(name)
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
          if ("p".equals(name)) {
            this.polygonsPrimitives.add(integers);
          } else if ("h".equals(name)) {
            if (this.polygonsPrimitives.size() > this.polygonsHoles.size()) {
              this.polygonsHoles.add(new ArrayList<int[]>());
            }
            this.polygonsHoles.get(this.polygonsPrimitives.size() - 1).add(integers);
          } 
        }
      } else if ("triangles".equals(name)
                 || "trifans".equals(name)
                 || "tristrips".equals(name)
                 || "polylist".equals(name)
                 || "polygons".equals(name)
                 || "lines".equals(name)
                 || "linestrips".equals(name)) {
        Geometry geometry;
        if ("lines".equals(name)
            || "linestrips".equals(name)) {
          geometry = getLinesGeometry(name);
        } else {
          geometry = getFacesGeometry(name);
        }
        this.geometries.get(this.geometryId).add(geometry);
        if (this.geometryAppearance != null) {
          this.geometryAppearances.put(geometry, this.geometryAppearance);
        }
        this.geometryAppearance = null;
        this.geometryVertices = null;
        this.geometryNormals = null;
        this.geometryTextureCoordinates = null;
        this.facesAndLinesPrimitives.clear();
        this.polygonsPrimitives.clear();
        this.polygonsHoles.clear();
        this.vcount = null;
      }
    }

    /**
     * Returns the triangles or polygons geometry matching the read values.
     */
    private Geometry getFacesGeometry(String name) {
      int primitiveType;
      if ("triangles".equals(name)) {
        primitiveType = GeometryInfo.TRIANGLE_ARRAY;
      } else if ("trifans".equals(name)) {
        primitiveType = GeometryInfo.TRIANGLE_FAN_ARRAY;
      } else if ("tristrips".equals(name)) {
        primitiveType = GeometryInfo.TRIANGLE_STRIP_ARRAY;
      } else {
        primitiveType = GeometryInfo.POLYGON_ARRAY;
      }
      
      GeometryInfo geometryInfo = new GeometryInfo(primitiveType);
      geometryInfo.setCoordinates(this.geometryVertices);
      geometryInfo.setCoordinateIndices(getIndices(this.geometryVertexOffset));
      if (this.geometryNormals != null) {
        geometryInfo.setNormals(this.geometryNormals);
        geometryInfo.setNormalIndices(getIndices(this.geometryNormalOffset));
      }
      if (this.geometryTextureCoordinates != null) {
        Integer stride = this.sourceAccessorStrides.get(this.geometryTextureCoordinates);
        // Support only UV texture coordinates
        float [] textureCoordinates;
        if (stride > 2) {
          textureCoordinates = new float [this.geometryTextureCoordinates.length / stride * 2];
          for (int i = 0, j = 0; j < this.geometryTextureCoordinates.length; j += stride) {
            textureCoordinates [i++] = this.geometryTextureCoordinates [j];
            textureCoordinates [i++] = this.geometryTextureCoordinates [j + 1];
          }
        } else {
          textureCoordinates = this.geometryTextureCoordinates;
        }
        geometryInfo.setTextureCoordinateParams(1, 2);
        geometryInfo.setTextureCoordinates(0, textureCoordinates);
        geometryInfo.setTextureCoordinateIndices(0, getIndices(this.geometryTextureCoordinatesOffset));
      }
      
      if ("trifans".equals(name)
          || "tristrips".equals(name)) {
        int [] stripCounts = new int [this.facesAndLinesPrimitives.size()];
        for (int i = 0; i < stripCounts.length; i++) {
          stripCounts [i] = this.facesAndLinesPrimitives.get(i).length / this.inputCount;
        }
        geometryInfo.setStripCounts(stripCounts);
      } else if ("polylist".equals(name)) {
        geometryInfo.setStripCounts(this.vcount);
      } else if ("polygons".equals(name)) {
        int [] stripCounts = new int [this.facesAndLinesPrimitives.size() + this.polygonsPrimitives.size() 
                                      + this.polygonsHoles.size()];
        int [] contourCounts = new int [this.facesAndLinesPrimitives.size() + this.polygonsPrimitives.size()];
        int stripIndex = 0;
        int countourIndex = 0;
        for (int i = 0; i < this.facesAndLinesPrimitives.size(); i++) {
          stripCounts [stripIndex++] = this.facesAndLinesPrimitives.get(i).length / this.inputCount;
          contourCounts [countourIndex++] = 1; // One polygon 
        }
        for (int i = 0; i < this.polygonsPrimitives.size(); i++) {
          stripCounts [stripIndex++] = this.polygonsPrimitives.get(i).length / this.inputCount;
          List<int []> polygonHoles = this.polygonsHoles.get(i);
          for (int j = 0; j < polygonHoles.size(); j++) {
            stripCounts [stripIndex++] = polygonHoles.get(j).length / this.inputCount;
          }
          contourCounts [countourIndex++] = 1 + polygonHoles.size(); // One polygon + its holes count
        }
        geometryInfo.setStripCounts(stripCounts);
        geometryInfo.setContourCounts(contourCounts);
      }

      if (this.geometryNormals == null) {
        new NormalGenerator(Math.PI / 2).generateNormals(geometryInfo);
      }
      return geometryInfo.getGeometryArray(true, true, false);
    }

    /**
     * Returns the lines geometry matching the read values.
     */
    private Geometry getLinesGeometry(String name) {
      int format = IndexedGeometryArray.COORDINATES;
      if (this.geometryNormals != null) {
        format |= IndexedGeometryArray.NORMALS;
      }
      if (this.geometryTextureCoordinates != null) {
        format |= IndexedGeometryArray.TEXTURE_COORDINATE_2;
      }
      
      int [] coordinatesIndices = getIndices(this.geometryVertexOffset);
      IndexedGeometryArray geometry;
      if ("lines".equals(name)) {
        geometry = new IndexedLineArray(this.geometryVertices.length / 3, format, coordinatesIndices.length);
      } else { // linestrips
        int [] stripCounts = new int [this.facesAndLinesPrimitives.size()];
        for (int i = 0; i < stripCounts.length; i++) {
          stripCounts [i] = this.facesAndLinesPrimitives.get(i).length / this.inputCount;
        }
        geometry = new IndexedLineStripArray(this.geometryVertices.length / 3, format, coordinatesIndices.length, stripCounts);
      }
      
      geometry.setCoordinates(0, this.geometryVertices);
      geometry.setCoordinateIndices(0, coordinatesIndices);
      if (this.geometryNormals != null) {
        geometry.setNormals(0, this.geometryNormals);
        geometry.setNormalIndices(0, getIndices(this.geometryNormalOffset));
      }
      if (this.geometryTextureCoordinates != null) {
        geometry.setTextureCoordinates(0, 0, this.geometryTextureCoordinates);
        geometry.setTextureCoordinateIndices(0, 0, getIndices(this.geometryTextureCoordinatesOffset));
      }
      return geometry;
    }

    /**
     * Returns the indices at the given <code>indexOffset</code>.
     */
    private int [] getIndices(int indexOffset) {
      if (this.facesAndLinesPrimitives.size() == 1 && this.polygonsPrimitives.size() == 1 && this.inputCount == 1) {
        return facesAndLinesPrimitives.get(0);
      } else {
        int indexCount = getIndexCount(this.facesAndLinesPrimitives);
        indexCount += getIndexCount(this.polygonsPrimitives);
        for (List<int []> polygonHole : this.polygonsHoles) {
          indexCount += getIndexCount(polygonHole);
        }
        
        int [] indices = new int [indexCount / this.inputCount];
        int i = 0;
        for (int [] primitives : this.facesAndLinesPrimitives) {
          for (int k = indexOffset; k < primitives.length; k += this.inputCount) {
            indices [i++] = primitives [k];
          }
        }
        for (int j = 0; j < this.polygonsPrimitives.size(); j++) {
          int [] polygonPrimitives = polygonsPrimitives.get(j);
          for (int k = indexOffset; k < polygonPrimitives.length; k += this.inputCount) {
            indices [i++] = polygonPrimitives [k];
          }
          for (int [] polygonHole : this.polygonsHoles.get(j)) {
            for (int k = indexOffset; k < polygonHole.length; k += this.inputCount) {
              indices [i++] = polygonHole [k];
            }
          }
        }
        return indices;
      }
    }
    
    /**
     * Returns the total count of indices among the given <code>faceIndices</code>. 
     */
    private int getIndexCount(List<int []> faceIndices) {
      int indexCount = 0;
      for (int [] indices : faceIndices) {
        indexCount += indices.length;
      }
      return indexCount;
    }
    
    /**
     * Multiplies the transform at top of the transform groups stack by the 
     * given <code>transformMultiplier</code>.
     */
    private void mulTransformGroup(Transform3D transformMultiplier) {
      TransformGroup transformGroup = (TransformGroup)this.parentGroups.peek();
      Transform3D transform = new Transform3D();
      transformGroup.getTransform(transform);
      transform.mul(transformMultiplier);
      transformGroup.setTransform(transform);
    }

    @Override
    public void endDocument() throws SAXException {
      for (Runnable runnable : this.postProcessingBinders) {
        runnable.run();
      }

      if (this.visualScene != null) {
        Transform3D rootTransform = new Transform3D();
        this.visualScene.getTransform(rootTransform);

        BoundingBox bounds = new BoundingBox(
            new Point3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY),
            new Point3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        computeBounds(this.visualScene, bounds, new Transform3D());

        // Set orientation to Z_UP
        Transform3D axisTransform = new Transform3D();
        if ("Y_UP".equals(axis)) {
          axisTransform.rotX(Math.PI / 2);
        } else if ("X_UP".equals(axis)) {
          axisTransform.rotY(-Math.PI / 2);
        }
        rootTransform.mul(axisTransform);
        // Translate model to its center
        Point3d lower = new Point3d();
        bounds.getLower(lower);
        if (lower.x != Double.POSITIVE_INFINITY) {
          Point3d upper = new Point3d();
          bounds.getUpper(upper);
          Transform3D translation = new Transform3D();
          translation.setTranslation(
              new Vector3d(-lower.x - (upper.x - lower.x) / 2, 
                  -lower.y - (upper.y - lower.y) / 2, 
                  -lower.z - (upper.z - lower.z) / 2));      
          rootTransform.mul(translation);
        }
        
        this.visualScene.setTransform(rootTransform);

        BranchGroup sceneRoot = new BranchGroup();
        this.scene.setSceneGroup(sceneRoot);
        sceneRoot.addChild(this.visualScene);
      }
    }
  }
  /**
   * Combines the given <code>bounds</code> with the bounds of the given <code>node</code>
   * and its children.
   */
  public static void computeBounds(Node node, BoundingBox bounds, Transform3D parentTransformations) {
    if (node instanceof Group) {
      if (node instanceof TransformGroup) {
        parentTransformations = new Transform3D(parentTransformations);
        Transform3D transform = new Transform3D();
        ((TransformGroup)node).getTransform(transform);
        parentTransformations.mul(transform);
      }
      // Compute the bounds of all the node children
      Enumeration<?> enumeration = ((Group)node).getAllChildren();
      while (enumeration.hasMoreElements ()) {
        computeBounds((Node)enumeration.nextElement(), bounds, parentTransformations);
      }
    } else if (node instanceof Link) {
      computeBounds(((Link)node).getSharedGroup(), bounds, parentTransformations);
    } else if (node instanceof Shape3D) {
      Bounds shapeBounds = ((Shape3D)node).getBounds();
      shapeBounds.transform(parentTransformations);
      bounds.combine(shapeBounds);
    }
  }
}
