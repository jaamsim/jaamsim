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

#define PI 3.1415926

//out vec4 outColor;

varying vec3 interpPos;
uniform sampler2D tex;

float atan2(in float x, in float y) {
    float offset = 0.0;
    if (x < 0) {
        offset = (y < 0) ? -PI : PI;
    }
    return atan(y/x) + offset;
}

void main()
{
    vec2 texCoords;
    texCoords.x = (-atan2(interpPos.x, interpPos.y) + PI) / (2*PI);
    texCoords.y = (atan(interpPos.z / length(interpPos.xy)) + (PI/2)) / (PI);

    gl_FragColor = texture2D(tex, texCoords);
    gl_FragColor.a = 1.0;
}
