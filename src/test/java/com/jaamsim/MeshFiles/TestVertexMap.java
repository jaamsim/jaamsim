package com.jaamsim.MeshFiles;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.junit.Test;

import com.jaamsim.math.Vec4d;

public class TestVertexMap {

	@Test
	public void testAdd() throws Throwable {
		Vec4d vecA = new Vec4d(1, 2, 3, 1);
		Vec4d vecB = new Vec4d(1, 3, 3, 1);
		Vec4d vecCa = new Vec4d(1, 4, 3, 1);
		Vec4d vecCb = new Vec4d(1, 4, 3, 1);
		Vec4d vecD = new Vec4d(1, 4, 3.00001, 1);

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
		assertTrue(verts.get(1).getTexCoord().equals4(vecD));
		assertTrue(!verts.get(1).getTexCoord().equals4(vecCb));
	}
}
