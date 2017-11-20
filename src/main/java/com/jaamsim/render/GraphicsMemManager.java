/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2017 JaamSim Software Inc.
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
package com.jaamsim.render;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.opengl.GL2GL3;

public class GraphicsMemManager {

	private static final long TEX_FREE_TIMEOUT = 1000; // time since last use before freeing this texture (in ms)
	private static final long TEX_FREE_FRAMES = 10; // number of renderer frames where this texture is not used before being freed

	public static class TexHandle {
		private final AtomicBoolean valid = new AtomicBoolean();
		private final int texID;
		private final int width, height;
		private long lastUsedTime;
		private long lastUsedFrame;
		private final GraphicsMemManager manager;

		private TexHandle(int texID, int width, int height, GraphicsMemManager m) {
			this.texID = texID;
			this.width = width;
			this.height = height;
			this.valid.set(true);
			this.lastUsedTime = System.currentTimeMillis();
			this.lastUsedFrame = Long.MAX_VALUE;
			this.manager = m;
		}

		public boolean isValid() {
			return valid.get();
		}
		// Returns a GL texture ID valid for the current render cycle
		public int bind() {
			this.lastUsedTime = System.currentTimeMillis();
			this.lastUsedFrame = manager.frameCounter;
			assert(valid.get());
			return texID;
		}
		void invalidate() {
			valid.set(false);
		}
		public int getWidth() {
			return width;
		}
		public int getHeight() {
			return height;
		}
	}

	public static class BufferHandle {
		private final AtomicBoolean valid = new AtomicBoolean();
		private final int bufferID;
		private final int size;
		private long lastUsedTime;
		private long lastUsedFrame;
		private final GraphicsMemManager manager;

		private BufferHandle(int bufferID, int size, GraphicsMemManager m) {
			this.bufferID = bufferID;
			this.size = size;
			this.valid.set(true);
			this.lastUsedTime = System.currentTimeMillis();
			this.lastUsedFrame = Long.MAX_VALUE;
			this.manager = m;
		}

		public boolean isValid() {
			return valid.get();
		}
		// Returns a GL buffer ID valid for the current render cycle
		public int bind() {
			this.lastUsedTime = System.currentTimeMillis();
			this.lastUsedFrame = manager.frameCounter;
			assert(valid.get());
			return bufferID;
		}
		void invalidate() {
			valid.set(false);
		}
		public int getSize() {
			return size;
		}
	}

	private long frameCounter = 0;
	private final ArrayList<TexHandle> allocatedTextures = new ArrayList<>();
	private final ArrayList<BufferHandle> allocatedBuffers = new ArrayList<>();
	private final Renderer renderer;

	GraphicsMemManager(Renderer r) {
		this.renderer = r;
	}

	public TexHandle allocateTexture(int width, int height, GL2GL3 gl) {
		int[] i = new int[1];
		gl.glGenTextures(1, i, 0);
		int glTexID = i[0];

		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, glTexID);

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR_MIPMAP_LINEAR );
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR );

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_CLAMP_TO_EDGE);

		renderer.usingVRAM(width*height*4);

		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, 0);

		TexHandle h = new TexHandle(glTexID, width, height, this);
		allocatedTextures.add(h);
		return h;

	}

	public BufferHandle allocateBuffer(int size, GL2GL3 gl) {
		int[] i = new int[1];
		gl.glGenBuffers(1, i, 0);
		int glBufferID = i[0];

		renderer.usingVRAM(size);

		BufferHandle h = new BufferHandle(glBufferID, size, this);
		allocatedBuffers.add(h);
		return h;

	}

	public void tickFrame() {
		frameCounter++;
	}

	public void freeOldTextures() {
		GL2GL3 gl = renderer.getGL();

		Iterator<TexHandle> texIt = allocatedTextures.iterator();
		while(texIt.hasNext()) {
			TexHandle h = texIt.next();

			if (  (System.currentTimeMillis() - h.lastUsedTime > TEX_FREE_TIMEOUT ) &&
			      (frameCounter - h.lastUsedFrame > TEX_FREE_FRAMES) ) {
				// Free this texture
				h.invalidate();
				int[] texs = new int[1];
				texs[0] = h.texID;
				gl.glDeleteTextures(1, texs, 0);
				renderer.usingVRAM(-(h.width*h.height*4));
				texIt.remove();
			}
		}
		Iterator<BufferHandle> buffIt = allocatedBuffers.iterator();
		while(buffIt.hasNext()) {
			BufferHandle h = buffIt.next();

			if (  (System.currentTimeMillis() - h.lastUsedTime > TEX_FREE_TIMEOUT ) &&
			      (frameCounter - h.lastUsedFrame > TEX_FREE_FRAMES) ) {
				// Free this texture
				h.invalidate();
				int[] texs = new int[1];
				texs[0] = h.bufferID;
				gl.glDeleteBuffers(1, texs, 0);
				renderer.usingVRAM(-h.size);
				buffIt.remove();
			}
		}
	}
}
