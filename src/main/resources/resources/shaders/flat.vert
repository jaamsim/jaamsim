/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
#version 120

uniform mat4 bindSpaceMat;
uniform mat4 bindSpaceNorMat;
uniform mat4 modelViewMat;
uniform mat4 projMat;
uniform mat4 normalMat;

attribute vec3 position;
attribute vec4 normal;
attribute vec2 texCoord;

varying vec2 texCoordFrag;
varying vec3 normalFrag;

uniform float C;
uniform float FC;
varying float interpZ;
varying vec3 viewDir;

void main()
{
    vec4 pos = vec4(position, 1.0);

    vec4 nor;
    nor.xyz = normalize(normal.xyz);
    nor.w = 0;

    vec4 bindSpacePos = bindSpaceMat * pos;
    vec4 bindSpaceNor = bindSpaceNorMat * nor;

    vec4 eyeSpacePos = modelViewMat * bindSpacePos;
    gl_Position = projMat * eyeSpacePos;

    normalFrag = (normalMat * bindSpaceNor).xyz;

    texCoordFrag = texCoord;

    viewDir = normalize(eyeSpacePos.xyz);

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;
}
