/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */
@VERSION@

uniform mat4 modelViewProjMat;

uniform float advance;

in vec2 position;

uniform float C;
uniform float FC;
out float interpZ;

void main()
{
    vec4 pos;
    pos.xy = position;
    pos.x += advance;
    pos.z = 0; pos.w = 1;

    gl_Position = modelViewProjMat * pos;

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    gl_Position.z = 0;

}
