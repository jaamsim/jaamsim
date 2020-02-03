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
@VERSION@

@DEFINES@

// For batch rendering the bind space variables are per-instance
#ifdef BATCH_RENDER
in mat4 bindSpaceMat;
in mat4 bindSpaceNorMat;
#else
uniform mat4 bindSpaceMat;
uniform mat4 bindSpaceNorMat;
#endif

uniform mat4 modelViewMat;
uniform mat4 projMat;
uniform mat4 normalMat;

uniform int maxNumBones;

in vec3 position;
in vec4 normal;
in vec2 texCoord;

in vec4 boneIndices;
in vec4 boneWeights;

const int MAX_BONES = 100;
uniform mat4 boneMatrices[MAX_BONES];

out vec2 texCoordFrag;
out vec3 normalFrag;

uniform float C;
uniform float FC;
out float interpZ;
out vec3 viewDir;

void main()
{
    vec4 pos = vec4(position, 1.0);

    vec4 animatedPos = vec4(0.0, 0.0, 0.0, 1.0);
    vec4 animatedNormal = vec4(0.0, 0.0, 0.0, 0.0);

    vec4 nor;
    nor.xyz = normalize(normal.xyz);
    nor.w = 0;

    vec4 bindSpacePos = bindSpaceMat * pos;
    vec4 bindSpaceNor = bindSpaceNorMat * nor;

    for (int b = 0; b < maxNumBones; ++b)
    {
        int boneIndex = int(boneIndices[b]);
        mat4 boneMat = mat4(1.0);
        if (boneIndex != -1) {
            boneMat = boneMatrices[boneIndex];
        }

        vec4 partialPos = boneMat * bindSpacePos;
        float weight = boneWeights[b];
        animatedPos.xyz += (partialPos * weight).xyz;

        vec4 partialNormal = boneMatrices[boneIndex] * bindSpaceNor;
        animatedNormal.xyz += (partialNormal * weight).xyz;
    }
    if (maxNumBones == 0) {
        animatedPos = bindSpacePos;
        animatedNormal = bindSpaceNor;
    }

    vec4 eyeSpacePos = modelViewMat * animatedPos;
    gl_Position = projMat * eyeSpacePos;

    normalFrag = (normalMat * animatedNormal).xyz;

    texCoordFrag = texCoord;

    viewDir = normalize(eyeSpacePos.xyz);

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;
}
