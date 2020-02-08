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
#version 330

in mat4 instSpaceMat;
in mat4 instSpaceNorMat;

uniform mat4 modelViewMat;
uniform mat4 projMat;
uniform mat4 normalMat;

in vec3 position;
in vec4 normal;
in vec2 texCoord;

out vec2 texCoordFrag;
out vec3 normalFrag;

uniform float C;
uniform float FC;
out float interpZ;
out vec3 viewDir;


in int diffTexIndexV;
in vec4 diffuseColorV;
in vec3 ambientColorV;
in vec3 specColorV;
in float shininessV;

flat out int diffTexIndexF;
out vec4 diffuseColorF;
out vec3 ambientColorF;
out vec3 specColorF;
out float shininessF;

void main()
{
    vec4 pos = vec4(position, 1.0);

    vec4 nor;
    nor.xyz = normalize(normal.xyz);
    nor.w = 0;

    vec4 instSpacePos = instSpaceMat * pos;
    vec4 instSpaceNor = instSpaceNorMat * nor;

    vec4 eyeSpacePos = modelViewMat * instSpacePos;
    gl_Position = projMat * eyeSpacePos;

    normalFrag = (normalMat * instSpaceNor).xyz;

    // Forward variables
    texCoordFrag = texCoord;
    diffTexIndexF = diffTexIndexV;
    diffuseColorF = diffuseColorV;
    ambientColorF = ambientColorV;
    specColorF = specColorV;
    shininessF = shininessV;

    viewDir = normalize(eyeSpacePos.xyz);

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;
}
