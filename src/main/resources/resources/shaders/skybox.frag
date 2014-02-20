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

#define PI 3.1415926

//out vec4 outColor;

varying vec3 interpPos;
uniform sampler2D tex;

float atan2(in float x, in float y) {
    float offset = 0.0;
    if (x < 0) {
        offset = (y < 0) ? -PI : PI;
    }
    return atan(y/x) + offset;
}

void main()
{
    vec2 texCoords;
    texCoords.x = (-atan2(interpPos.x, interpPos.y) + PI) / (2*PI);
    texCoords.y = (atan(interpPos.z / length(interpPos.xy)) + (PI/2)) / (PI);

    gl_FragColor = texture2D(tex, texCoords);
}
