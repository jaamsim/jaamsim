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

uniform float C;
uniform float FC;
in float interpZ;

//layout(location = 0) out vec4 output;
out vec4 outColour;

void main()
{
    if (interpZ < 0)
        discard;

    if (gl_FrontFacing) {
      outColour.rgb = vec3(0, 0.5, 0);
    } else {
      outColour.rgb = vec3(0.5, 0, 0);
    }
    outColour.a = 0.5;

    gl_FragDepth = log(interpZ*C+1)*FC;

}
