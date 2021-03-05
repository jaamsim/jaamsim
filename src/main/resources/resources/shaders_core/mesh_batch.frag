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
#version 430

in vec2 texCoordFrag;
in vec3 normalFrag;

uniform float C;
uniform float FC;
in float interpZ;

//layout(location = 0) out vec4 output;
out vec4 outColour;

const int MAX_SAMPLERS = 32;

uniform sampler2D diffTexs[MAX_SAMPLERS];
flat in int diffTexIndexF;
in vec4 diffuseColorF;

const int MAX_LIGHTS = 10;

uniform vec3 lightDir[MAX_LIGHTS];
uniform float lightIntensity[MAX_LIGHTS];
uniform int numLights;

in vec3 viewDir;

in vec3 ambientColorF;
in vec3 specColorF;
in float shininessF;

void main()
{
    outColour.a = 1;

    vec3 n = normalize(normalFrag);

    vec4 dColor = vec4(0,0,0,0);
    //if (diffTexIndexF == -1 || diffTexIndexF >= MAX_SAMPLERS) {
    //if (diffTexIndexF != 0) {
    if (diffTexIndexF == -1) {
        dColor = diffuseColorF;
    } else {
        dColor = texture(diffTexs[diffTexIndexF], texCoordFrag);
    }
    //dColor += diffuseColorF;
    outColour.a = dColor.a;

    vec3 d = vec3(0, 0, 0);
    vec3 s = vec3(0, 0, 0);

    float light = 0;
    for (int i = 0; i < numLights; ++i) {
        vec3 l = lightDir[i];

        float lDotN = dot(n, l);
        d += max(0, -1*lDotN) * lightIntensity[i] * dColor.rgb;

        if (shininessF > 2) { // shininess == 1 is default, but all real values will be > 2
            vec3 ref = 2*lDotN*n - l;
            float specExp = pow(max(0, dot(ref, viewDir)), shininessF);

            s += specExp * lightIntensity[i] * specColorF;
        }
    }

    outColour.rgb = s + d + ambientColorF * 0.1;

    gl_FragDepth = log(interpZ*C+1)*FC;
}
