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

//layout(location = 0) out vec4 output;
out vec4 outColour;

uniform float C;
uniform float FC;
in float interpZ;

uniform vec4 color;

void main() 
{
    if (interpZ < 0)
        discard;

    outColour = color;

    gl_FragDepth = log(interpZ*C+1)*FC;

}
