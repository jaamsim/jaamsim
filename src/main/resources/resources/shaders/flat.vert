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

//uniform mat4 modelViewMat;
//uniform mat4 projMat;
uniform mat4 modelViewProjMat;
uniform mat4 normalMat;

in vec4 position;
in vec4 normal;
in vec2 texCoord;

out vec2 texCoordFrag;
out vec3 normalFrag;

void main()
{

    //gl_Position = projMat * modelViewMat * position;
    gl_Position = modelViewProjMat * position;

    vec4 nor;
    nor.xyz = normalize(normal.xyz);
    nor.w = 0;

    normalFrag = (normalMat * nor).xyz;

    texCoordFrag = texCoord;
}
