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
