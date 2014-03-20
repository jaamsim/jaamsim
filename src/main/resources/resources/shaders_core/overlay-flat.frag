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

in vec2 texCoordFrag;

out vec4 outColour;

uniform sampler2D tex;

uniform vec4 color;
uniform bool useTex;

void main()
{
    if (useTex) {
        outColour = texture(tex, texCoordFrag);
    } else {
        outColour = color;
    }
}
