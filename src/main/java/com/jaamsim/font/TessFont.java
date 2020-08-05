/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.font;

//import com.jaamsim.math.*;

import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import com.jaamsim.math.Vec3d;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.TessFontKey;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;


/**
 * A simple tesselated font, takes an AWT font and creates renderable characters
 * from it The tesselator is based on the GLU tesselator. Vertex lists are
 * created lazily and cached indefinitely so this object may become quite large
 * as time goes on.
 *
 * In order to use this class, it should be passed to a TessString, which is a 'Renderable'
 *
 * @author Matt Chudleigh
 *
 */

public class TessFont {

private HashMap<Integer, TessChar> _charMap;

private final Font _font;
private final TessFontKey _key;
private final FontRenderContext _frc;
private final ArrayList<double[]> _vertices;

private final float _lineAdvance;

private boolean _glBufferDirty = true;

// The height of the 'classic' character used to scale the overall rendering height
private double _nominalHeight;

// System wide asset ID
private int _id;

private int _glVertBuffer = -1;

public TessFont(TessFontKey key) {
	_frc = new FontRenderContext(null, true, true);
	_font = new Font(key.getFontName(), key.getFontStyle(), 1);
	_key = key;
	_vertices = new ArrayList<>();

	_charMap = new HashMap<>();
	// Originally support all the basic latin characters (will lazily add new ones as needed)
	String initialChars= "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890.,/<>?;':\"[]{}!@#$%^&*()_+-= \t";

	for (int i = 0; i < initialChars.length(); ++i) {
		generateChar(initialChars.charAt(i)); // Note, none of these are supplementary, so this is safe
	}

	TextLayout tl = new TextLayout(initialChars, _font, _frc);
	_lineAdvance = tl.getAscent() + tl.getDescent() + tl.getLeading();

	_id = Renderer.getAssetID();

	_nominalHeight = _charMap.get((int)'A').getHeight();
}

/**
 * Retrieve the tesselated character representation of 'c'. Will try to load
 * a cached version but may need to generate a new tesselation. This will
 * only need to be done once. See preloadCache() to avoid lazy
 * initialization
 *
 * @param c
 * @return
 */

public synchronized TessChar getTessChar(int cp) {
	TessChar cachedChar = _charMap.get(cp);

	// Load any characters this font has not loaded before
	if (cachedChar == null) {
		generateChar(cp);
		cachedChar = _charMap.get(cp);
	}

	return cachedChar;
}

private static class CharTesselator extends GLUtessellatorCallbackAdapter {

	private int _type;
	private Vector<Double> _verts;
	private int vertsInPrim;
	boolean oddStrip; // Need to wind every other triangle in a triangle strip backwards (it's just part of the strip)

	private double[] temp; // Used to build up a triangle fan or strip

	public CharTesselator() {
		_verts = new Vector<>();
		temp = new double[4];
	}

	@Override
	public void begin(int type) {
		_type = type;
		vertsInPrim = 0;
		oddStrip = false;
//		if (type == GL.GL_TRIANGLES) {
//			LogBox.formatRenderLog("New Triangles");
//		}
//		if (type == GL.GL_TRIANGLE_FAN) {
//			LogBox.formatRenderLog("New Fan");
//		}
//		if (type == GL.GL_TRIANGLE_STRIP) {
//			LogBox.formatRenderLog("New Strip");
//		}
	}

	@Override
	public void end() {
		// Make sure we're still saving whole triangles
		assert((_verts.size() % 6) == 0);
	}

	@Override
	public void vertex(Object vertData) {
		double[] verts = (double[]) vertData;
		if (_type == GL2GL3.GL_TRIANGLES) {
			// For triangles, just add the vertices
			_verts.add(verts[0]); // x
			_verts.add(verts[1]); // y
			vertsInPrim = (vertsInPrim + 1) % 3;
//			if (vertsInPrim == 0) {
//				checkWinding("triangle");
//			}
			return;
		} else if (_type == GL2GL3.GL_TRIANGLE_FAN) {
			triangleStripFanVerts(verts, false);
			return;
		} else if (_type == GL2GL3.GL_TRIANGLE_STRIP) {
			triangleStripFanVerts(verts, true);
			return;
		} else {
			assert(false);
		}
	}

	/**
	 * Handle vertices for a triangle strip or fan
	 * @param verts
	 * @param isStrip
	 */
	private void triangleStripFanVerts(double[] verts, boolean isStrip) {
		if (vertsInPrim == 0) {
			temp[0] = verts[0]; temp[1] = verts[1];
			vertsInPrim++;
			return;
		} else if (vertsInPrim == 1) {
			temp[2] = verts[0]; temp[3] = verts[1];
			vertsInPrim++;
			return;
		}
		// Is the third or more vertex

		// If this is an odd number primitive in a strip, add the old vertices in reverse order
		if (isStrip && oddStrip) {
			_verts.add(temp[2]); _verts.add(temp[3]); _verts.add(temp[0]); _verts.add(temp[1]);
			_verts.add(verts[0]); _verts.add(verts[1]);
		} else {
			_verts.add(temp[0]); _verts.add(temp[1]); _verts.add(temp[2]); _verts.add(temp[3]);
			_verts.add(verts[0]); _verts.add(verts[1]);
		}
		oddStrip = !oddStrip;
		//checkWinding(isStrip ? "strip" : "fan");

		// If this is a strip, the first temp is overridden (not for a fan though)
		if (isStrip) {
			temp[0] = temp[2]; temp[1] = temp[3];
		}

		// Store this vert for the next triangle
		temp[2] = verts[0]; temp[3] = verts[1];

		vertsInPrim++;
		return;

	}

	/**
	 * Debug, check the winding of the current triangle, and output an error if it's wrong
	 */
//	private void checkWinding(String type) {
//		if (!type.equals("strip")) {
//			//return;
//		}
//		int vertLength = _verts.size();
//		Vector4d vert0 = new Vector4d(_verts.get(vertLength - 6), _verts.get(vertLength - 5), 0);
//		Vector4d vert1 = new Vector4d(_verts.get(vertLength - 4), _verts.get(vertLength - 3), 0);
//		Vector4d vert2 = new Vector4d(_verts.get(vertLength - 2), _verts.get(vertLength - 1), 0);
//		Vector4d zeroToOne = new Vector4d(0.0d, 0.0d, 0.0d, 1.0d);
//		Vector4d oneToTwo = new Vector4d(0.0d, 0.0d, 0.0d, 1.0d);
//		vert1.sub3(vert0, zeroToOne);
//		vert2.sub3(vert1, oneToTwo);
//		Vector4d cross = new Vector4d(0.0d, 0.0d, 0.0d, 1.0d);
//		zeroToOne.cross(oneToTwo, cross);
//		if (cross.data[2] < 0) {
//			// This triangle is backwards wound
//			LogBox.formatRenderLog("Triangle is wound backwards for: " + type);
//		}
//		else
//		{
//			LogBox.formatRenderLog("Triangle is good for: " + type);
//		}
//	}

	@Override
	public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
		// We don't include any additional data
		double[] newCoord = new double[2];
		newCoord[0] = coords[0];
		newCoord[1] = coords[1];

		outData[0] = newCoord;
	}

	@Override
	public void error(int errNum) {
		@SuppressWarnings("unused")
		String errorString = GLU.createGLU().gluErrorString(errNum);
		assert(false); // TODO: Handle this properly?
	}

	public Vector<Double> getVerts() {
		return _verts;
	}
}

private static class TessOutput {
	public Rectangle2D bounds;
	public double[] verts;
	public double[] advances; // one horizontal advance per character entered
}

private TessOutput tesselateString(String s) {
	GlyphVector gv = _font.createGlyphVector(_frc, s);

    Shape shape = gv.getOutline();
	//
    AffineTransform at = new AffineTransform();
    at.scale(1, -1);
	PathIterator pIt = shape.getPathIterator(at, _font.getSize()/200.0);

	// Create a GLU tesselator
	GLUtessellator tess = GLU.gluNewTess();
	CharTesselator tessAdapt = new CharTesselator();

	GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, tessAdapt);
	GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, tessAdapt);
	GLU.gluTessCallback(tess, GLU.GLU_TESS_END, tessAdapt);
	GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, tessAdapt);
	GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, tessAdapt);

	int winding = pIt.getWindingRule();

	if (winding == PathIterator.WIND_EVEN_ODD)
		GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
	else if (winding == PathIterator.WIND_NON_ZERO)
		GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
	else
		assert(false); // PathIterator should only return these two winding rules

	GLU.gluBeginPolygon(tess);
	GLU.gluTessNormal(tess, 0, 0, 1);
	double[] first = null;
	double[] v;
	while (!pIt.isDone()) {
		v = new double[3];
		int type = pIt.currentSegment(v);
		v[2] = 0.0;
		if (type == PathIterator.SEG_MOVETO) {
			first = v;
			GLU.gluNextContour(tess, GLU.GLU_UNKNOWN);
			GLU.gluTessVertex(tess, v, 0, v);
		}
		else if (type == PathIterator.SEG_LINETO) {
			GLU.gluTessVertex(tess, v, 0, v);
		}
		else if (type == PathIterator.SEG_CLOSE) {
			assert(first != null); // If this is true, there is an error in the AWT path iterator
			GLU.gluTessVertex(tess, first, 0, first);
			first = null;
		}
		else
		{
			assert(false); // The path itertor should not return other path types here
		}
		pIt.next();
	}
	GLU.gluEndPolygon(tess);

	int numVerts = tessAdapt.getVerts().size();
	double[] verts = new double[numVerts];
	int count = 0;
	for (double d : tessAdapt.getVerts()) {
		verts[count++] = d;
	}

	TessOutput ret = new TessOutput();
	ret.verts = verts;
	ret.bounds = gv.getVisualBounds();

	ret.advances = new double[s.length()];
	for (int i = 0; i < s.length(); ++i) {
		ret.advances[i] = gv.getGlyphMetrics(i).getAdvance();
	}
	return ret;
}

private void generateChar(int cp) {
	StringBuilder sb = new StringBuilder();
	sb.appendCodePoint(cp);
	String s = sb.toString();

	TessOutput tessed = tesselateString(s);
	int totalVerts = 0;
	for (double[] ds : _vertices) {
		totalVerts += ds.length;
	}

	assert((totalVerts % 2) == 0);
	assert((tessed.verts.length % 2) == 0);
	// startIndex is the index of points in the GL buffer this character starts at
	int startIndex = totalVerts / 2;

	// numVerts is the number of vertices in the GL buffer to draw
	int numVerts = tessed.verts.length / 2;

	// Append the verts to the list
	_vertices.add(tessed.verts);

	TessChar tc = new TessChar(cp, startIndex, numVerts, tessed.bounds.getWidth(), tessed.bounds.getHeight(), tessed.advances[0]);
	_charMap.put(cp, tc);

	_glBufferDirty = true;

}

private void setupBuffer(GL2GL3 gl) {
	// Create an OpenGL buffer
	int[] buffs = new int[1];
	gl.glGenBuffers(1, buffs, 0);
	_glVertBuffer = buffs[0];

	_glBufferDirty = true;
}

public TessFontKey getFontKey() {
	return _key;
}

public int getAssetID() {
	return _id;
}

public double getLineAdvance() {
	return _lineAdvance;
}

public synchronized int getGLBuffer(GL2GL3 gl) {

	// The buffer may not have been initialized yet
	if (_glVertBuffer == -1 ) {
		setupBuffer(gl);
	}

	if (_glBufferDirty) {
		int totalVerts = 0;
		for (double[] d : _vertices) {
			totalVerts += d.length;
		}

		FloatBuffer fb = FloatBuffer.allocate(totalVerts);
		for (double[] ds : _vertices) {
			for (double d : ds) {
				fb.put((float)d);
			}
		}
		fb.flip();

		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _glVertBuffer);
		gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, totalVerts * 4, fb, GL2GL3.GL_STATIC_DRAW);
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);

		_glBufferDirty = false;
	}

	return _glVertBuffer;
}

/**
 * Get the dimensions of the fully rendered string (useful for app level layout)
 * @param textHeight - the requested text height
 * @param string - the string to render
 * @return
 */
public Vec3d getStringSize(double textHeight, String string) {
	return new Vec3d(getStringLength(textHeight, string), textHeight, 0.0d);
}

public double getStringLength(double textHeight, String string) {
	if (string == null)
		return 0.0d;
	double width = 0.0d;
	for (int cp : RenderUtils.stringToCodePoints(string)) {
		TessChar tc = getTessChar(cp);
		width += tc.getAdvance();
	}
	return width * textHeight / getNominalHeight();
}

/**
 * Returns the index of the first character in the string whose x-coordinate
 * is closest to the given value.
 * @param textHeight - height of the text in metres
 * @param string - given string
 * @param x - x-coordinate whose index number is to be found
 * @return index number in the string.
 */
public int getStringPosition(double textHeight, String string, double x) {
	if (string == null)
		return 0;
	double scaledX = x / textHeight * getNominalHeight();
	double width = 0.0d;
	int[] cpList = RenderUtils.stringToCodePoints(string);
	for (int i=0; i < cpList.length; i++) {
		TessChar tc = getTessChar(cpList[i]);
		if (width + 0.5d*tc.getAdvance() >= scaledX)
			return i;
		width += tc.getAdvance();
	}
	return cpList.length;
}

public double getNominalHeight() {
	return _nominalHeight;
}

} // class TessFont
