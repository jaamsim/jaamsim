/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019 JaamSim Software Inc.
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLExtensions;

/**
 * A cache that ensures each texture object is only loaded once, looks up textures by URL to there
 * is a chance of a repeated texture if synonymous URLs are used
 * @author Matt.Chudleigh
 *
 */
public class TexCache {

	private static final int MAX_UNCOMPRESSED_SIZE = 64*1024*1024; // No texture can be more than 64 megs uncompressed

	private static class TexEntry {
		public int texID;
		public boolean hasAlpha;
		public boolean compressed;
		public boolean forcedCompressed;
		public TexEntry(int id, boolean alpha, boolean compressed, boolean forcedCompressed) {
			this.texID = id;
			this.hasAlpha = alpha;
			this.compressed = compressed;
			this.forcedCompressed = forcedCompressed;
		}
	}

	private static class LoadingEntry {
		public int bufferID;
		public URI imageURI;
		public boolean hasAlpha;
		public boolean compressed;
		public boolean forcedCompressed; // The user did not request a compressed texture, but we compressed it anyway
		public ByteBuffer data;
		public int width, height;
		public AtomicBoolean done = new AtomicBoolean(false);
		public AtomicBoolean failed = new AtomicBoolean(false);
		public final Object lock = new Object();

		public LoadingEntry(URI uri, ByteBuffer data, boolean alpha, boolean compressed, boolean forcedCompressed) {
			this.imageURI = uri;
			this.data = data;
			this.hasAlpha = alpha;
			this.compressed = compressed;
			this.forcedCompressed = forcedCompressed;
		}
	}

	private final Map<String, TexEntry> _texMap = new HashMap<>();
	private final Map<String, LoadingEntry> _loadingMap = new HashMap<>();

	private final EntryLoaderRunner entryLoader = new EntryLoaderRunner();

	private final Renderer _renderer;

	public static final URI BAD_TEXTURE;
	private int badTextureID = -1;

	public static final int LOADING_TEX_ID = -2;

	static {
		try {
			BAD_TEXTURE = TexCache.class.getResource("/resources/images/bad-texture.png").toURI();
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	public TexCache(Renderer r) {
		_renderer = r;
	}

	public void init(GL2GL3 gl) {
		LoadingEntry badLE = launchLoadImage(gl, BAD_TEXTURE, false, false);
		assert(badLE != null); // We should never fail to load the bad texture

		waitForTex(badLE);
		_loadingMap.remove(BAD_TEXTURE.toString());

		badTextureID = loadGLTexture(gl, badLE);
		assert(badTextureID != -1); // Hopefully OpenGL never naturally returns -1, but I don't think it should
	}

	public int getTexID(GL2GL3 gl, URI imageURI, boolean withAlpha, boolean compressed, boolean waitUntilLoaded) {

		if (imageURI == null) {
			return badTextureID;
		}

		// Scan the list of textures and load any that are ready
		ArrayList<String> loadedStrings = new ArrayList<>();
		for (Map.Entry<String, LoadingEntry> entry : _loadingMap.entrySet()) {
			LoadingEntry le = entry.getValue();
			if (le.done.get()) {
				loadedStrings.add(entry.getKey());
				int glTexID = loadGLTexture(gl, le);
				_texMap.put(le.imageURI.toString(), new TexEntry(glTexID, le.hasAlpha, le.compressed, le.forcedCompressed));
			}
		}
		for (String s : loadedStrings) {
			_loadingMap.remove(s);
		}

		String imageURIKey = imageURI.toString();
		if (_texMap.containsKey(imageURIKey)) {

			// There is an entry in the cache, but let's check the other attributes
			TexEntry entry = _texMap.get(imageURIKey);
			boolean found = true;
			if (withAlpha && !entry.hasAlpha) {
				found = false; // This entry does not have an alpha channel
			}
			if (entry.compressed && !compressed && !entry.forcedCompressed) {
				// The entry is compressed, but we requested an uncompressed image
				found = false;
			}

			if (found) {
				return entry.texID;
			}

			// The entry exists, but not as was requested, free the texture so we can reload it
			int[] texIDs = new int[1];
			texIDs[0] = entry.texID;
			gl.glDeleteTextures(1, texIDs, 0);

			_texMap.remove(imageURIKey);
		}

		boolean isLoading = _loadingMap.containsKey(imageURIKey);
		LoadingEntry le = null;
		if (!isLoading) {
			le = launchLoadImage(gl, imageURI, withAlpha, compressed);

			if (le == null) {
				// The image could not be found
				_texMap.put(imageURIKey, new TexEntry(badTextureID, withAlpha, compressed, false));
				return badTextureID;
			}
		}

		if (!waitUntilLoaded && _renderer.allowDelayedTextures()) {
			return LOADING_TEX_ID;
		}

		waitForTex(le);
		_loadingMap.remove(imageURIKey);

		int glTexID = loadGLTexture(gl, le);
		_texMap.put(le.imageURI.toString(), new TexEntry(glTexID, le.hasAlpha, le.compressed, le.forcedCompressed));

		return glTexID;
	}

	private LoadingEntry launchLoadImage(GL2GL3 gl, final URI imageURI, boolean transparent, boolean compressed) {

		Dimension dim = getImageDimension(imageURI);
		if (dim == null) {
			// Could not load image
			String path = Paths.get(imageURI).toString();  // decode %20 as blank character
			GUIFrame.invokeErrorDialog("3D Loader Error",
					String.format("Could not load texture file:\n %s", path));
			LogBox.formatRenderLog("Could not load texture file: %s\n", path);
			return null;
		}

		// Map an openGL buffer of size width*height*4
		int[] ids = new int[1];
		gl.glGenBuffers(1, ids, 0);

		int bufferSize = dim.width*dim.height*4;

		boolean forcedCompressed = false;
		if (!transparent && !compressed) {
			if (dim.width * dim.height * 3 > MAX_UNCOMPRESSED_SIZE) {
				// Always compress large textures and save the user from themselves
				compressed = true;
				forcedCompressed = true;
			}
		}

		if (compressed) {
			assert(gl.isExtensionAvailable(GLExtensions.EXT_texture_compression_s3tc));
			// Round width and height up to nearest multiple of 4 (the s3tc block size)
			int width = dim.width;
			if ((width&3)!= 0) {
				width = (width&~3)+4;
			}
			int height = dim.height;
			if ((height&3)!= 0) {
				height = (height&~3)+4;
			}
			bufferSize = width * height / 2;
		}

		ByteBuffer mappedBuffer = null;
			gl.glBindBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER, ids[0]);
			gl.glBufferData(GL2GL3.GL_PIXEL_UNPACK_BUFFER, bufferSize, null, GL2GL3.GL_STREAM_READ);
		try {
			mappedBuffer = gl.glMapBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER, GL2GL3.GL_WRITE_ONLY);
		} catch (GLException ex) {
			// A GL Exception here is most likely caused by an out of memory, this is recoverable and simply use the bad texture
			LogBox.formatRenderLog("Out of GRAM for image URL: %s\n", imageURI.toString());
			return null;
		}
		// Explicitly check for an error (we may not be using a DebugGL implementation, so the exception may not be thrown)
		if (gl.glGetError() != GL2GL3.GL_NO_ERROR) {
			LogBox.formatRenderLog("GL Error loading image URL: %s\n", imageURI.toString());
			return null;
		}

		gl.glBindBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER, 0);

		final LoadingEntry le = new LoadingEntry(imageURI, mappedBuffer, transparent, compressed, forcedCompressed);
		le.bufferID = ids[0];

		_loadingMap.put(imageURI.toString(), le);

		entryLoader.loadEntry(le);
		return le;
	}

	private int loadGLTexture(GL2GL3 gl, LoadingEntry le) {

		if (le.failed.get()) {
			return badTextureID;
		}

		int[] i = new int[1];
		gl.glGenTextures(1, i, 0);
		int glTexID = i[0];

		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, glTexID);

		if (le.compressed)
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR );
		else
			gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MIN_FILTER, GL2GL3.GL_LINEAR_MIPMAP_LINEAR );

		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_MAG_FILTER, GL2GL3.GL_LINEAR );
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_S, GL2GL3.GL_REPEAT);
		gl.glTexParameteri(GL2GL3.GL_TEXTURE_2D, GL2GL3.GL_TEXTURE_WRAP_T, GL2GL3.GL_REPEAT);

		gl.glPixelStorei(GL2GL3.GL_UNPACK_ALIGNMENT, 1);

		// Attempt to load to a proxy texture first, then see what happens
		int internalFormat = 0;
		if (le.hasAlpha && le.compressed) {
			// We do not currently support compressed textures with alpha
			assert(false);
			return badTextureID;
		} else if(le.hasAlpha && !le.compressed) {
			internalFormat = GL2GL3.GL_RGBA;
		} else if(!le.hasAlpha && le.compressed) {
			internalFormat = GL2GL3.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
		} else if(!le.hasAlpha && !le.compressed) {
			internalFormat = GL2GL3.GL_RGB;
		}

		gl.glBindBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER, le.bufferID);
		gl.glUnmapBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER);

		try {
			if (le.compressed) {
				gl.glCompressedTexImage2D(GL2GL3.GL_TEXTURE_2D, 0, internalFormat, le.width,
				                          le.height, 0, le.data.capacity(), 0);
				_renderer.usingVRAM(le.data.capacity());
			} else {
				gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, 0, internalFormat, le.width,
				                le.height, 0, GL2GL3.GL_BGRA, GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV, 0);
				_renderer.usingVRAM(le.width*le.height*4);
			}

			// Note we do not let openGL generate compressed mipmaps because it stalls the render thread really badly
			// in theory it could be generated in the worker thread, but not yet
			if (!le.compressed)
				gl.glGenerateMipmap(GL2GL3.GL_TEXTURE_2D);
		} catch (GLException ex) {
			// We do not have enough texture memory
			LogBox.renderLog(String.format("Error loading texture: %s", le.imageURI.toString()));
			LogBox.renderLog(String.format("  %s", ex.toString()));
			return badTextureID;
		}

		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, 0);

		gl.glBindBuffer(GL2GL3.GL_PIXEL_UNPACK_BUFFER, 0);
		i[0] = le.bufferID;
		gl.glDeleteBuffers(1, i, 0);

		// Finally queue a redraw in case an asset avoided drawing this one
		_renderer.queueRedraw();
		return glTexID;
	}

	public static Dimension getImageDimension(URI imageURI) {
		ImageInputStream inStream = null;
		try {
			inStream = ImageIO.createImageInputStream(imageURI.toURL().openStream());
			Iterator<ImageReader> it = ImageIO.getImageReaders(inStream);
			if (it.hasNext()) {
				ImageReader reader = it.next();
				reader.setInput(inStream);
				Dimension ret = new Dimension(reader.getWidth(0), reader.getHeight(0));
				reader.dispose();
				inStream.close();
				return ret;
			}
		} catch (IOException ex) {
		}

		return null;
	}

	private class EntryLoaderRunner implements Runnable {
		final ArrayList<LoadingEntry> list = new ArrayList<>();
		private Thread loadThread = null;

		@Override
		public void run() {
			while (true) {
				LoadingEntry le = null;
				synchronized (this) {
					if (list.isEmpty()) {
						loadThread = null;
						return;
					}
					le = list.remove(0);
				}

				loadImage(le);
			}
		}

		void loadEntry(LoadingEntry le) {
			synchronized (this) {
				list.add(le);
				if (loadThread == null) {
					loadThread = new Thread(this, "TextureLoadThread");
					loadThread.start();
				}
			}
		}
	}

	private void loadImage(LoadingEntry le) {
		BufferedImage img = null;
		try {
			img = ImageIO.read(le.imageURI.toURL());
		}
		catch(Exception e) {
			le.failed.set(true);
			le.done.set(true);
			return;
		}
		if (img == null) {
			le.failed.set(true);
			le.done.set(true);
			return;
		}

		int width = img.getWidth();
		int height = img.getHeight();

		le.width = width;
		le.height = height;

		AffineTransform flipper = new AffineTransform(1, 0,
		                                              0, -1,
		                                              0, height);

		BufferedImage bgr = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2 = bgr.createGraphics();

		if (!le.hasAlpha) {
			g2.setColor(Color.WHITE);
		} else {
			g2.setColor(new Color(0, 0, 0, 0));
		}
		g2.fillRect(0, 0, width, height);

		g2.drawImage(img, flipper, null);
		g2.dispose();
		DataBufferInt ints = (DataBufferInt)bgr.getData().getDataBuffer();

		if (le.compressed) {
			S3TexCompressor comp = new S3TexCompressor();
			IntBuffer intBuffer = (IntBuffer.wrap(ints.getData()));

			ByteBuffer compressed = comp.compress(intBuffer, le.width, le.height);
			le.data.put(compressed);
		} else {
			le.data.asIntBuffer().put(ints.getData());
		}

		le.done.set(true);
		synchronized(le.lock) {
			le.lock.notify();
		}

		_renderer.queueRedraw();
	}

	private void waitForTex(LoadingEntry le) {
		synchronized (le.lock) {
			while (!le.done.get()) {
				try {
					le.lock.wait();
				} catch (InterruptedException ex) {}
			}
		}

	}
}
