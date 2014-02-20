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
#version 120

uniform vec2 offset;
uniform vec2 size;

attribute vec2 position;
attribute vec2 texCoordVert;

varying vec2 texCoordFrag;

void main()
{

    gl_Position.xy = position * size + offset;
    gl_Position.z = 0;
    gl_Position.w = 1;

    texCoordFrag = texCoordVert;

}
