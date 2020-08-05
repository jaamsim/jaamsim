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

uniform mat4 modelViewProjMat;

uniform vec2 advance;

attribute vec2 position;

uniform float C;
uniform float FC;
varying float interpZ;

void main()
{
    vec4 pos;
    pos.xy = position;
    pos.xy += advance;
    pos.z = 0; pos.w = 1;

    gl_Position = modelViewProjMat * pos;

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;

}
