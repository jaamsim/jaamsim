/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2020 JaamSim Software Inc.
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

in mat4 instMat;
uniform mat4 modelViewMat;
uniform mat4 projMat;

in vec4 position;

in vec4 vertColor;
out vec4 fragColor;

uniform float C;
uniform float FC;
out float interpZ;

void main()
{

    gl_Position = projMat * modelViewMat * instMat * position;

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;

    fragColor = vertColor;
}
