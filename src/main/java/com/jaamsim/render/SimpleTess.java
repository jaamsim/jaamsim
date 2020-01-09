/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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

package com.jaamsim.render;

import java.util.ArrayList;
import java.util.List;

import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;

/**
 * A simple wrapper around the GLU tesselator that can tesselate single contour convex polygons in the XY plane
 * @author Matt Chudleigh
 *
 */
public class SimpleTess {

private static class Tess extends GLUtessellatorCallbackAdapter {
	private GLUtessellator gluTess;

	private ArrayList<Vec4d> verts;
	private int type;
	private int vertsInPrim;
	private Vec4d[] tempVerts = new Vec4d[2];
	boolean oddStrip;

	@Override
	public void begin(int type) {
		this.type = type;

		vertsInPrim = 0;
		oddStrip = false;
	}

	@Override
	public void end() {
		// Make sure we're still saving whole triangles
		assert((verts.size() % 3) == 0);
	}

	@Override
	public void vertex(Object vertData) {
		double[] vs = (double[]) vertData;
		Vec4d vert = new Vec4d(vs[0], vs[1], vs[2], 1.0);
		// For triangles, just add the vertices
		if (type == GL.GL_TRIANGLES) {
			verts.add(vert);
		} else if (type == GL.GL_TRIANGLE_FAN) {
			triangleStripFanVerts(vert, false);
			return;
		} else if (type == GL.GL_TRIANGLE_STRIP) {
			triangleStripFanVerts(vert, true);
			return;
		} else {
			assert(false);
		}
	}
	@Override
	public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
		// We don't include any additional data
		double[] newCoord = new double[3];

		newCoord[0] = coords[0];
		newCoord[1] = coords[1];
		newCoord[2] = coords[2];

		outData[0] = newCoord;
	}

	@Override
	public void error(int errNum) {
		@SuppressWarnings("unused")
		String errorString = GLU.createGLU().gluErrorString(errNum);
		assert(false); // TODO: Handle this properly?
	}

	/**
	 * Handle vertices for a triangle strip or fan
	 * @param verts
	 * @param isStrip
	 */
	private void triangleStripFanVerts(Vec4d vert, boolean isStrip) {
		if (vertsInPrim == 0) {
			tempVerts[0] = vert;
			vertsInPrim++;
			return;
		} else if (vertsInPrim == 1) {
			tempVerts[1] = vert;
			vertsInPrim++;
			return;
		}
		// Is the third or more vertex

		// If this is an odd number primitive in a strip, add the old vertices in reverse order
		if (isStrip && oddStrip) {
			verts.add(tempVerts[1]);
			verts.add(tempVerts[0]);
			verts.add(vert);
		} else {
			verts.add(tempVerts[0]);
			verts.add(tempVerts[1]);
			verts.add(vert);
		}
		oddStrip = !oddStrip;

		// If this is a strip, the first temp is overridden (not for a fan though)
		if (isStrip) {
			tempVerts[0] = tempVerts[1];
		}

		// Store this vert for the next triangle
		tempVerts[1] = vert;

		vertsInPrim++;
		return;

	}

	public void init() {
		gluTess = GLU.gluNewTess();

		GLU.gluTessCallback(gluTess, GLU.GLU_TESS_VERTEX, this);
		GLU.gluTessCallback(gluTess, GLU.GLU_TESS_BEGIN, this);
		GLU.gluTessCallback(gluTess, GLU.GLU_TESS_END, this);
		GLU.gluTessCallback(gluTess, GLU.GLU_TESS_COMBINE, this);
		GLU.gluTessCallback(gluTess, GLU.GLU_TESS_ERROR, this);

		GLU.gluTessProperty(gluTess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
}

	public void reset() {
		verts = new ArrayList<Vec4d>();
	}

	public List<Vec4d> tesselate(List<? extends Vec3d> outline) {
		reset();

		GLU.gluTessBeginPolygon(gluTess, null);
		GLU.gluTessNormal(gluTess, 0, 0, 1);
		GLU.gluTessBeginContour(gluTess);
		for (Vec3d v: outline) {
			double[] ps = new double[3];
			ps[0] = v.x; ps[1] = v.y; ps[2] = v.z;
			GLU.gluTessVertex(gluTess, ps, 0, ps);
		}
		GLU.gluTessEndContour(gluTess);
		GLU.gluTessEndPolygon(gluTess);

		List<Vec4d> ret = verts;
		verts = null;
		return ret;
	}

}

private static Tess TESS;

private static void init() {
	if (TESS == null) {
		TESS = new Tess();
		TESS.init();
	}
}

public static List<Vec4d> tesselate(List<? extends Vec3d> outline) {

	if (outline.size() < 3) {
		return new ArrayList<Vec4d>();
	}

	init();
	synchronized(TESS) {
		return TESS.tesselate(outline);
	}
}

} // class SimpleTess
