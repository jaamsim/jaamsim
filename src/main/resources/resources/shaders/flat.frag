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
#version 120

// This will be replaced with appropriate defines as a macro expansion
@DEFINES@

varying vec2 texCoordFrag;
varying vec3 normalFrag;

uniform float C;
uniform float FC;
varying float interpZ;

//layout(location = 0) out vec4 output;
//out vec4 outColour;

#ifdef DIFF_TEX
// Use a diffuse texture
uniform sampler2D diffuseTex;
#define DIFF_VAL texture2D(diffuseTex, texCoordFrag)

#else
// Constant diffuse color
uniform vec4 diffuseColor;
#define DIFF_VAL diffuseColor

#endif

const int MAX_LIGHTS = 10;

uniform vec3 lightDir[MAX_LIGHTS];
uniform float lightIntensity[MAX_LIGHTS];
uniform int numLights;

varying vec3 viewDir;

uniform vec3 ambientColor;
uniform vec3 specColor;
uniform float shininess;

void main()
{
    gl_FragColor.a = 1;

    vec3 n = normalize(normalFrag);

    vec4 dColor = DIFF_VAL;
    gl_FragColor.a = dColor.a;

    vec3 d = vec3(0, 0, 0);
    vec3 s = vec3(0, 0, 0);

    float light = 0;
    for (int i = 0; i < numLights; ++i) {
        vec3 l = lightDir[i];

        float lDotN = dot(n, l);
        d += max(0, -1*lDotN) * lightIntensity[i] * dColor.rgb;

        if (shininess > 2) { // shininess == 1 is default, but all real values will be > 2
            vec3 ref = 2*lDotN*n - l;
            float specExp = pow(max(0, dot(ref, viewDir)), shininess);

            s += specExp * lightIntensity[i] * specColor;
        }
    }

    gl_FragColor.rgb = s + d + ambientColor * 0.1;

    gl_FragDepth = log(interpZ*C+1)*FC;
}
