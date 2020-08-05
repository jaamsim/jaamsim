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

import java.util.ArrayList;
import java.util.HashMap;

import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Camera;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderable;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.Shader;
import com.jaamsim.render.VisibilityInfo;
import com.jogamp.opengl.GL2GL3;

public class TessString implements Renderable {

private TessFont _font;
private int[] _contents;

private final float[] _color;

private long _pickingID;

/**
 * The transform to global space (will be gone when the scene graph is finalized)
 */
private Mat4d _trans;

private AABB _bounds;

private VisibilityInfo _visInfo;

// Cached data needed to draw this string, prevents hitting the font character map
private int[] starts;
private int[] numVerts;
private double[] advances;

private static HashMap<Integer, Integer> VAOMap = new HashMap<>();

public TessString(TessFont font, String contents, Color4d color,
        Transform trans, double textHeight, VisibilityInfo visInfo, long pickingID) {

	this(font, contents, color, trans.getMat4dRef(), textHeight, visInfo, pickingID);

}


public TessString(TessFont font, String contents, Color4d color,
		Mat4d trans, double textHeight, VisibilityInfo visInfo, long pickingID) {
	_font = font;
	_color = color.toFloats();
	_trans = new Mat4d(trans);
	_visInfo = visInfo;

	// Adjust the scale factor of the transform to account for the requested text height
	double heightScale = textHeight / _font.getNominalHeight();

	_trans.scale3(heightScale);
	//_trans.setScale( trans.getScale() * heightScale);

	_pickingID = pickingID;

	starts = new int[contents.length()];
	numVerts = new int[contents.length()];
	advances = new double[contents.length()];

	double maxWidth = 0;
	double width = 0;
	double height = _font.getNominalHeight();
	double yAdvance = 0;
	_contents = RenderUtils.stringToCodePoints(contents);
	for (int i = 0; i < _contents.length; ++i) {
		if (_contents[i] == '\n') {
			width = 0;
			yAdvance += _font.getLineAdvance();
			continue;
		}

		TessChar tc = _font.getTessChar(_contents[i]);
		assert(tc != null);
		width += tc.getAdvance();
		maxWidth = Math.max(width, maxWidth);

		starts[i] = tc.getStartIndex();
		numVerts[i] = tc.getNumVerts();
		advances[i] = tc.getAdvance();
	}

	// As the renderer draws characters from the bottom left, but the model specifies text labels in the center,
	// we need to offset the transform
	Mat4d align = new Mat4d();
	align.setTranslate3(new Vec4d(-maxWidth/2, -height/2 + yAdvance/2, 0, 1.0d));
	_trans.mult4(align);

	ArrayList<Vec4d> vs = new ArrayList<>(4);

	vs.add(new Vec4d( maxWidth,  height+yAdvance, 0, 1.0d));
	vs.add(new Vec4d(        0,  height+yAdvance, 0, 1.0d));
	vs.add(new Vec4d(        0,                0, 0, 1.0d));
	vs.add(new Vec4d( maxWidth,                0, 0, 1.0d));

	_bounds = new AABB(vs, _trans);
}

@Override
public void render(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
	GL2GL3 gl = renderer.getGL();

	if (!VAOMap.containsKey(contextID)) {
		setupVAO(contextID, renderer);
	}

	int vao = VAOMap.get(contextID);
	gl.glBindVertexArray(vao);

	// Render the string
	Shader s = renderer.getShader(Renderer.ShaderHandle.FONT);

	s.useShader(gl);
	int prog = s.getProgramHandle();

	// Setup uniforms for this object
	Mat4d modelViewProjMat = new Mat4d();
	cam.getViewMat4d(modelViewProjMat);
	modelViewProjMat.mult4(_trans);

	Mat4d projMat = cam.getProjMat4d();
	modelViewProjMat.mult4(projMat, modelViewProjMat);

	int modelViewProjMatVar = gl.glGetUniformLocation(prog, "modelViewProjMat");
	gl.glUniformMatrix4fv(modelViewProjMatVar, 1, false, RenderUtils.MarshalMat4d(modelViewProjMat), 0);

	int colorVar = gl.glGetUniformLocation(prog, "color");
	gl.glUniform4fv(colorVar, 1, _color, 0);

	int cVar = gl.glGetUniformLocation(prog, "C");
	gl.glUniform1f(cVar, Camera.C);

	int fcVar = gl.glGetUniformLocation(prog, "FC");
	gl.glUniform1f(fcVar, Camera.FC);

	int advanceVar = gl.glGetUniformLocation(prog, "advance");

	int posVar = gl.glGetAttribLocation(prog, "position");
	gl.glEnableVertexAttribArray(posVar);

	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, _font.getGLBuffer(gl));
	gl.glVertexAttribPointer(posVar, 2, GL2GL3.GL_FLOAT, false, 0, 0);
	gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, 0);


	// Send out one draw call per character
	float advanceX = 0;
	float advanceY = 0;

	gl.glDisable(GL2GL3.GL_CULL_FACE);

	for (int i = 0; i < _contents.length; ++i) {

		if (_contents[i] == '\n') {
			advanceX = 0;
			advanceY -= _font.getLineAdvance();
			continue;
		}

		gl.glUniform2f(advanceVar, advanceX, advanceY);

		gl.glDrawArrays(GL2GL3.GL_TRIANGLES, starts[i], numVerts[i]);

		advanceX += advances[i];
	}
	gl.glEnable(GL2GL3.GL_CULL_FACE);

	// Cleanup
	gl.glDisableVertexAttribArray(posVar);
}

private void setupVAO(int contextID, Renderer renderer) {
	GL2GL3 gl = renderer.getGL();

	int vao = renderer.generateVAO(contextID, gl);
	VAOMap.put(contextID, vao);

}

@Override
public long getPickingID() {
	return _pickingID;
}

@Override
public AABB getBoundsRef() {
	return _bounds;
}

@Override
public double getCollisionDist(Ray r, boolean precise)
{
	return _bounds.collisionDist(r);

	// TODO precise collision
}

@Override
public boolean hasTransparent() {
	return false;
}


@Override
public void renderTransparent(int contextID, Renderer renderer, Camera cam, Ray pickRay) {
}

@Override
public boolean renderForView(int viewID, Camera cam) {
	double dist = cam.distToBounds(getBoundsRef());
	return _visInfo.isVisible(viewID, dist);
}
} // class

