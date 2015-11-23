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
package com.jaamsim.MeshFiles;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;

public class TestVertexMap {

	@Test
	public void testAdd() throws Throwable {
		Vec3d vecA = new Vec3d(1, 2, 3);
		Vec3d vecB = new Vec3d(1, 3, 3);
		Vec2d vecCa = new Vec2d(1, 4);
		Vec2d vecCb = new Vec2d(1, 4);
		Vec2d vecD = new Vec2d(1, 5);

		VertexMap map = new VertexMap();
		int ind0 = map.getVertIndex(vecA, vecB, vecCa);
		int ind1 = map.getVertIndex(vecA, vecB, vecCa);
		int ind2 = map.getVertIndex(vecA, vecB, vecCb);
		int ind3 = map.getVertIndex(vecA, vecB, vecD);

		assertTrue(ind0 == 0);
		assertTrue(ind1 == 0);
		assertTrue(ind2 == 0);
		assertTrue(ind3 == 1);

		ArrayList<Vertex> verts = map.getVertList();
		assertTrue(verts.get(1).getTexCoord().equals2(vecD));
	}
}
