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
package com.jaamsim.render;

import com.jogamp.opengl.GL2GL3;

/**
 * An easy to use wrapper around a shader, is created with filenames for vertex and fragment shader
 * source files, keeps shader handles around. Needs a cache later, but for now this will work
 * @author Matt.Chudleigh
 *
 */

public class Shader {

public enum ShaderStatus {
	GOOD, BAD_INPUT, VERT_FAILED, FRAG_FAILED, LINK_FAILED, VALIDATE_FAILED
}

private int _vsHandle;
private int _fsHandle;
private int _progHandle;

private ShaderStatus _status;

private String _vertCompLog;
private String _fragCompLog;
private String _progLinkLog;
private String _progValidateLog;

public Shader(String vertString, String fragString, GL2GL3 gl) {
	_vsHandle = gl.glCreateShader(GL2GL3.GL_VERTEX_SHADER);
	_fsHandle = gl.glCreateShader(GL2GL3.GL_FRAGMENT_SHADER);

	String[] sa = new String[1];
	int[] ia = new int[1];

	sa[0] = vertString;
	ia[0] = vertString.length();
	gl.glShaderSource(_vsHandle, 1, sa, ia, 0);

	sa[0] = fragString;
	ia[0] = fragString.length();
	gl.glShaderSource(_fsHandle, 1, sa, ia, 0);

	gl.glCompileShader(_vsHandle);
	_vertCompLog = getShaderLog(_vsHandle, gl);
	if (!checkCompileStatus(_vsHandle, gl)) {
		_status = ShaderStatus.VERT_FAILED;
		return;
	}

	gl.glCompileShader(_fsHandle);
	_fragCompLog = getShaderLog(_fsHandle, gl);
	if (!checkCompileStatus(_fsHandle, gl)) {
		_status = ShaderStatus.FRAG_FAILED;
		return;
	}

	_progHandle = gl.glCreateProgram();
	gl.glAttachShader(_progHandle, _vsHandle);
	gl.glAttachShader(_progHandle, _fsHandle);

	gl.glLinkProgram(_progHandle);
	_progLinkLog = getProgramLog(_progHandle, gl);
	if (!checkLinkStatus(gl)) {
		_status = ShaderStatus.LINK_FAILED;
		return;
	}

	gl.glValidateProgram(_progHandle);
	_progValidateLog = getProgramLog(_progHandle, gl);
	if (!checkValidateStatus(gl)) {
		_status = ShaderStatus.VALIDATE_FAILED;
		return;
	}

	_status = ShaderStatus.GOOD;
}

public boolean isGood() {
	return _status == ShaderStatus.GOOD;
}

public void useShader(GL2GL3 gl) {
	if (_status != ShaderStatus.GOOD) {
		assert(false);
	}

	gl.glUseProgram(_progHandle);
}

private boolean checkCompileStatus(int shaderHandle, GL2GL3 gl) {
	int[] res = new int[1];

	gl.glGetShaderiv(shaderHandle, GL2GL3.GL_COMPILE_STATUS, res, 0);
	return (res[0] == GL2GL3.GL_TRUE);
}

private boolean checkLinkStatus(GL2GL3 gl) {
	int[] res = new int[1];

	gl.glGetProgramiv(_progHandle, GL2GL3.GL_LINK_STATUS, res, 0);
	return (res[0] == GL2GL3.GL_TRUE);
}

private boolean checkValidateStatus(GL2GL3 gl) {
	int[] res = new int[1];

	gl.glGetProgramiv(_progHandle, GL2GL3.GL_VALIDATE_STATUS, res, 0);
	return (res[0] == GL2GL3.GL_TRUE);
}

public int getProgramHandle() {
	return _progHandle;
}

/**
 * Free the OpenGL resources used by this shader, can not be used again after
 */
public void clearProgram(GL2GL3 gl) {
	if (_status != ShaderStatus.GOOD) {
		return;
	}
	gl.glDeleteProgram(_progHandle);
}

private String getShaderLog(int shaderHandle, GL2GL3 gl) {
	int[] is = new int[1];
	gl.glGetShaderiv(shaderHandle, GL2GL3.GL_INFO_LOG_LENGTH, is, 0);
	int logLength = is[0];
	if (logLength == 0) {
		return "";
	}

	byte[] bs = new byte[logLength];

	gl.glGetShaderInfoLog(shaderHandle, logLength, is, 0,  bs, 0);

	StringBuilder sb = new StringBuilder();

	for(int i = 0; i < is[0]; ++i) {
		sb.append((char)bs[i]);
	}

	return sb.toString();
}

private String getProgramLog(int progHandle, GL2GL3 gl) {
	int[] is = new int[1];
	gl.glGetProgramiv(progHandle, GL2GL3.GL_INFO_LOG_LENGTH, is, 0);
	int logLength = is[0];
	if (logLength == 0) {
		return "";
	}

	byte[] bs = new byte[logLength];


	gl.glGetProgramInfoLog(progHandle, logLength, is, 0,  bs, 0);

	StringBuilder sb = new StringBuilder();

	for(int i = 0; i < is[0]; ++i) {
		sb.append((char)bs[i]);
	}

	return sb.toString();

}

public String getVertShaderLog() {
	return _vertCompLog;
}

public String getFragShaderLog() {
	return _fragCompLog;
}

public String getProgramLinkLog() {
	return _progLinkLog;
}

public String getProgramValidateLog() {
	return _progValidateLog;
}

/**
 * Utility to get the most recent failure information, this could be implemented by an outside class
 * with the other public information
 * @return - a human readable string to help debug the shader issues
 */
public String getFailureLog() {
	switch (_status) {
	case GOOD:
		return "No problems";
	case BAD_INPUT:
		return "Could not find an shader source";
	case VERT_FAILED:
		return "Vertex Shader compile failure: \n" + _vertCompLog;
	case FRAG_FAILED:
		return "Fragment Shader compile failure: \n" + _fragCompLog;
	case LINK_FAILED:
		return "Program link failure: \n" + _progLinkLog;
	case VALIDATE_FAILED:
		return "Program validation failure: \n" + _progValidateLog;
	}

	assert(false);
	return "";
}

} // class Shader
