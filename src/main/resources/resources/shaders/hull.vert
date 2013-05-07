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
#version 130

uniform mat4 modelViewMat;
uniform mat4 projMat;

in vec4 position;

uniform float C;
uniform float FC;
out float interpZ;

void main()
{

    gl_Position = projMat * modelViewMat * position;

    // Logarithmic depth buffer
    interpZ = gl_Position.w;
    float logIn = interpZ*C+1;
    float logVal = 0;
    // Linearize for negative values (
    if (logIn < 0) {
        logVal = interpZ*C;
    } else {
        logVal = log(logIn);
    }
    gl_Position.z = (2*logVal*FC - 1)*gl_Position.w;

}
