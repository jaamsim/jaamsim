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

uniform mat4 projMat;
uniform mat4 invViewMat;

attribute vec4 position;
varying vec3 interpPos;

void main()
{
    gl_Position = projMat * position;
    gl_Position.z = gl_Position.w; // Make sure this has the maximum depth value
    interpPos = (invViewMat * position).xyz;
}
