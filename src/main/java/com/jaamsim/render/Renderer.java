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
package com.jaamsim.render;

//import com.jaamsim.math.*;

import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.nio.IntBuffer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.opengl.DebugGL2;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.MeshFiles.MeshData;
import com.jaamsim.font.OverlayString;
import com.jaamsim.font.TessFont;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.util.ExceptionLogger;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.sandwell.JavaSimulation.ColourInput;

/**
 * The central renderer for JaamSim Renderer, Contains references to all context
 * specific data (like shader caches)
 *
 * @author Matt.Chudleigh
 *
 */
public class Renderer implements GLAnimatorControl {

	public enum ShaderHandle {
		MESH, FONT, HULL, OVERLAY_FONT, OVERLAY_FLAT, DEBUG, SKYBOX
	}

	static private Object idLock = new Object();
	static private int _nextID = 1;
	/**
	 * Get a system wide unique ID
	 * @return
	 */
	public static int getAssetID() {
		synchronized(idLock) {
			return _nextID++;
		}
	}

	private static boolean RENDER_DEBUG_INFO = false;
	private static boolean USE_DEBUG_GL = true;

	private EnumMap<ShaderHandle, Shader> _shaders;

	// Display _display = null;
	// Screen _screen = null;
	private GLContext _sharedContext = null;
	Map<Integer, Integer> _sharedVaoMap = new HashMap<Integer, Integer>();
	GLWindow _dummyWindow;
	private GLCapabilities _caps = null;

	private TexCache _texCache = new TexCache(this);

	// An initalization time flag specifying if the 'safest' graphical techniques should be used
	private boolean _safeGraphics;

	private final Thread _renderThread;
	private final Object _rendererLock = new Object();

	private final Map<MeshProtoKey, MeshProto> _protoCache;
	private final Map<TessFontKey, TessFont> _fontCache;

	private final HashMap<Integer, RenderWindow> _openWindows;

	private final Queue<RenderMessage> _renderMessages = new ArrayDeque<RenderMessage>();

	private final AtomicBoolean _displayNeeded = new AtomicBoolean(true);
	private final AtomicBoolean _initialized = new AtomicBoolean(false);
	private final AtomicBoolean _shutdown = new AtomicBoolean(false);
	private final AtomicBoolean _fatalError = new AtomicBoolean(false);

	private String _errorString; // This is the string that caused the fatal error
	private StackTraceElement[] _fatalStackTrace; // the stack trace from the fatal error

	private final ExceptionLogger _exceptionLogger;

	private TessFontKey _defaultFontKey = new TessFontKey(Font.SANS_SERIF, Font.PLAIN);
	private TessFontKey _defaultBoldFontKey = new TessFontKey(Font.SANS_SERIF, Font.BOLD);

	private final Object _sceneLock = new Object();
	private ArrayList<RenderProxy> _proxyScene = new ArrayList<RenderProxy>();

	private boolean _allowDelayedTextures;
	private double _sceneTimeMS;
	private double _loopTimeMS;

	private long _usedVRAM = 0;

	// A flag to track JOGL's GLAnimatorControl pause feature
	private boolean isPaused = false;

	// This may not be the best way to cache this
	//private GL2GL3 _currentGL = null;
	private GLContext _drawContext = null;

	private Skybox _skybox;

	// A cache of the current scene, needed by the individual windows to render
	private ArrayList<Renderable> _currentScene = new ArrayList<Renderable>();
	private ArrayList<OverlayRenderable> _currentOverlay = new ArrayList<OverlayRenderable>();

	public Renderer(boolean safeGraphics) throws RenderException {
		_safeGraphics = safeGraphics;
		_protoCache = new HashMap<MeshProtoKey, MeshProto>();
		_fontCache = new HashMap<TessFontKey, TessFont>();

		_exceptionLogger = new ExceptionLogger(1); // Print the call stack on the first exception of any kind

		_openWindows = new HashMap<Integer, RenderWindow>();

		_renderThread = new Thread(new Runnable() {
			@Override
			public void run() {
				mainRenderLoop();
			}
		}, "RenderThread");
		_renderThread.start();

	}

	private void mainRenderLoop() {

		//long startNanos = System.nanoTime();

		try {
			// GLProfile.initSingleton();
			GLProfile glp = GLProfile.get(GLProfile.GL2GL3);
			_caps = new GLCapabilities(glp);
			_caps.setSampleBuffers(true);
			_caps.setNumSamples(4);
			_caps.setDepthBits(24);

			// Create a dummy window
			_dummyWindow = GLWindow.create(_caps);
			_dummyWindow.setSize(128, 128);
			_dummyWindow.setPosition(-2000, -2000);

			// This is unfortunately necessary due to JOGL's (newt's?) involved
			// startup code
			// I can not find a way to make a context valid without a visible window
			_dummyWindow.setVisible(true);

			_sharedContext = _dummyWindow.getContext();
			assert (_sharedContext != null);

			_dummyWindow.setVisible(false);

//			long endNanos = System.nanoTime();
//			long ms = (endNanos - startNanos) /1000000L;
//			System.out.println("Creating shared context at:" + ms + "ms");

			initSharedContext();

			// Notify the main thread we're done
			synchronized (_initialized) {
				_initialized.set(true);
				_initialized.notifyAll();
			}

		} catch (Exception e) {

			_fatalError.set(true);
			_errorString = e.getLocalizedMessage();
			_fatalStackTrace = e.getStackTrace();
			System.out.println("Renderer encountered a fatal error:");
			e.printStackTrace();
		} finally {
			if (_sharedContext != null && _sharedContext.isCurrent())
				_sharedContext.release();
		}


//		endNanos = System.nanoTime();
//		ms = (endNanos - startNanos) /1000000L;
//		System.out.println("Started renderer loop after:" + ms + "ms");

		long lastLoopEnd = System.nanoTime();

		// Add a custom shutdown hook to make sure we're finished closing before JOGL tries to shutdown
		NativeWindowFactory.addCustomShutdownHook(true, new Runnable() {
			@Override
			public void run() {
				// Block JOGL shutting down until we're dead
				shutdown();
				while (_renderThread.isAlive()) {
					synchronized (this) {
						try {
							queueRedraw(); // Just in case the render thread got stalled somewhere
							wait(50);
						} catch (InterruptedException ex) {}
					}
				}
			}
		});

		while (!_shutdown.get()) {
			try {

				// If a fatal error was encountered, clean up the renderer
				if (_fatalError.get()) {
					// We should clean up everything we can, then die
					try {
						for (Entry<Integer, RenderWindow> entry : _openWindows.entrySet()){
							entry.getValue().getGLWindowRef().destroy();
							entry.getValue().getAWTFrameRef().dispose();
						}
					} catch(Exception e) {} // Ignore any exceptions, this is just a best effort cleanup


					try {
						_dummyWindow.destroy();
						_sharedContext.destroy();
						_dummyWindow = null;
						_sharedContext = null;
						_openWindows.clear();

						_currentScene = null;
						_currentOverlay = null;
						_caps = null;

						_fontCache.clear();
						_protoCache.clear();
						_shaders.clear();

					} catch (Exception e) { }

					break; // Exiting the loop will end the thread
				}

				// Run all render messages
				RenderMessage message;
				do {
					// Only lock the queue while reading messages, release it while
					// processing them
					message = null;
					synchronized (_renderMessages) {
						if (!_renderMessages.isEmpty()) {
							message = _renderMessages.remove();
						}
					}
					if (message != null) {
						try {
							handleMessage(message);
						} catch (Throwable t) {
							// Log this error but continue processing
							logException(t);
						}
					}

				} while (!_renderMessages.isEmpty());

				if (_displayNeeded.compareAndSet(true, false)) {
					updateRenderableScene();

					// Defensive copy the window list (in case a window is closed while we render)
					HashMap<Integer, RenderWindow> winds;
					synchronized (_openWindows) {
						winds = new HashMap<Integer, RenderWindow>(_openWindows);
					}
					if (!isPaused) {
						for (RenderWindow wind : winds.values()) {
							try {
								GLContext context = wind.getGLWindowRef().getContext();
								if (context != null && !_shutdown.get())
								{
									wind.getGLWindowRef().display();
								}
							} catch (Throwable t) {
								// Log it, but move on to the other windows
								logException(t);
							}
						}
					}
				}

				long loopEnd = System.nanoTime();
				_loopTimeMS = (loopEnd - lastLoopEnd) / 1000000;
				lastLoopEnd = loopEnd;

				try {
					synchronized (_displayNeeded) {
						if (!_displayNeeded.get()) {
							_displayNeeded.wait();
						}
					}
				} catch (InterruptedException e) {
					// Let's loop anyway...
				}

			} catch (Throwable t) {
				// Any other unexpected exceptions...
				logException(t);
			}
		}
	}

	/**
	 * Returns the shader object for this handle, should only be called from the render thread (during a render)
	 * @param h
	 * @return
	 */
	public Shader getShader(ShaderHandle h) {
		return _shaders.get(h);
	}

	/**
	 * Returns the MeshProto for the supplied key, should only be called from the render thread (during a render)
	 * @param key
	 * @return
	 */
	public MeshProto getProto(MeshProtoKey key) {
		MeshProto proto = _protoCache.get(key);
		if (proto == null) {
			// This prototype needs to be lazily loaded
			loadMeshProtoImp(key);
		}
		return _protoCache.get(key);
	}

	public TessFont getTessFont(TessFontKey key) {
		if (!_fontCache.containsKey(key)) {
			loadTessFontImp(key); // Try lazy initialization for now
		}

		return _fontCache.get(key);
	}

	public void setScene(ArrayList<RenderProxy> scene) {
		synchronized (_sceneLock) {
			_proxyScene = scene;
		}
	}

	public void queueRedraw() {
		synchronized(_displayNeeded) {
			_displayNeeded.set(true);
			_displayNeeded.notifyAll();
		}
	}

	private void addRenderMessage(RenderMessage msg) {
		_renderMessages.add(msg);
		queueRedraw();
	}

	public void setCameraInfoForWindow(int windowID, CameraInfo info) {
		synchronized (_renderMessages) {
			addRenderMessage(new SetCameraMessage(windowID, info));
		}
	}

	private void setCameraInfoImp(SetCameraMessage mes) {
		synchronized (_openWindows) {
			RenderWindow w = _openWindows.get(mes.windowID);
			if (w != null) {
				w.getCameraRef().setInfo(mes.cameraInfo);
			}
		}
	}

	/**
	 * Call this from any thread to shutdown the Renderer, will return
	 * immediately but the renderer will shutdown after the next redraw
	 */
	public void shutdown() {
		_shutdown.set(true);
		queueRedraw();
	}

	public GL2GL3 getGL() {
		return _drawContext.getGL().getGL2GL3();
	}

	/**
	 * Get a list of all the IDs of currently open windows
	 * @return
	 */
	public ArrayList<Integer> getOpenWindowIDs() {
		synchronized(_openWindows) {

			ArrayList<Integer> ret = new ArrayList<Integer>();
			for (int id : _openWindows.keySet()) {
				ret.add(id);
			}
			return ret;
		}
	}

	public String getWindowName(int windowID) {
		synchronized(_openWindows) {
			RenderWindow win = _openWindows.get(windowID);
			if (win == null) {
				return null;
			}
			return win.getName();
		}
	}

	public Frame getAWTFrame(int windowID) {
		synchronized(_openWindows) {
			RenderWindow win = _openWindows.get(windowID);
			if (win == null) {
				return null;
			}
			return win.getAWTFrameRef();
		}
	}

	public void focusWindow(int windowID) {
		synchronized(_openWindows) {
			RenderWindow win = _openWindows.get(windowID);
			if (win == null) {
				return;
			}
			win.getAWTFrameRef().setExtendedState(Frame.NORMAL);
			win.getAWTFrameRef().toFront();
		}
	}
	/**
	 * Construct a new window (a NEWT window specifically)
	 *
	 * @param width
	 * @param height
	 * @return
	 */
	private void createWindowImp(CreateWindowMessage message) {

		RenderGLListener listener = new RenderGLListener();

		RenderWindow window = new RenderWindow(message.x, message.y,
		                                       message.width, message.height,
		                                       message.title, message.name,
		                                       _sharedContext,
		                                       _caps, listener,
		                                       message.icon,
		                                       message.windowID,
		                                       message.viewID,
		                                       message.listener);
		listener.setWindow(window);

		synchronized (_openWindows) {
			_openWindows.put(message.windowID, window);
		}

		window.getGLWindowRef().setAnimator(this);

		GLWindowListener wl = new GLWindowListener(window.getWindowID());
		window.getGLWindowRef().addWindowListener(wl);
		window.getAWTFrameRef().addComponentListener(wl);
		window.getGLWindowRef().addMouseListener(new MouseHandler(window, message.listener));

		window.getAWTFrameRef().setVisible(true);

		queueRedraw();
	}

	public int createWindow(int x, int y, int width, int height, int viewID, String title, String name, Image icon,
	                         WindowInteractionListener listener) {
		synchronized (_renderMessages) {
			int windowID = getAssetID();
			addRenderMessage(new CreateWindowMessage(x, y, width, height, title,
					name, windowID, viewID, icon, listener));
			return windowID;
		}
	}

	public void setWindowDebugInfo(int windowID, String debugString, ArrayList<Long> debugIDs) {
		synchronized(_openWindows) {
			RenderWindow w = _openWindows.get(windowID);

			if (w != null) {
				w.setDebugString(debugString);
				w.setDebugIDs(debugIDs);
			}
		}
	}

	public int getNumOpenWindows() {
		synchronized(_openWindows) {
			return _openWindows.size();
		}
	}

	public void closeWindow(int windowID) {

		synchronized (_renderMessages) {
			addRenderMessage(new CloseWindowMessage(windowID));
		}

	}

	private void closeWindowImp(CloseWindowMessage msg) {
		RenderWindow window;
		synchronized(_openWindows) {
			window = _openWindows.get(msg.windowID);
			if (window == null) {
				return;
			}
		}
		windowCleanup(msg.windowID);
		window.getGLWindowRef().destroy();
		window.getAWTFrameRef().dispose();
	}

	private String readSource(String file) {
		URL res = Renderer.class.getResource(file);

		StringBuilder source = new StringBuilder();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(res.openStream()));

			while (true) {
				String line = reader.readLine();
				if (line == null) break;

				source.append(line).append("\n");
			}
			reader.close();
			reader = null;
		}
		catch (IOException e) {}

		return source.toString();
	}

private void createShader(ShaderHandle sh, String vert, String frag, GL2GL3 gl) {
	String vertsrc = readSource(vert);
	String fragsrc = readSource(frag);

	Shader s = new Shader(vertsrc, fragsrc, gl);
	if (s.isGood()) {
		_shaders.put(sh, s);
		return;
	}

	String failure = s.getFailureLog();
	throw new RenderException("Shader failed: " + sh.toString() + " " + failure);
}

/**
 * Create and compile all the shaders
 */
private void initShaders(GL2GL3 gl) throws RenderException {
	_shaders = new EnumMap<ShaderHandle, Shader>(ShaderHandle.class);
	String vert, frag;

	vert = "/resources/shaders/flat.vert";
	frag = "/resources/shaders/flat.frag";
	createShader(ShaderHandle.MESH, vert, frag, gl);

	vert = "/resources/shaders/font.vert";
	frag = "/resources/shaders/font.frag";
	createShader(ShaderHandle.FONT, vert, frag, gl);

	vert = "/resources/shaders/hull.vert";
	frag = "/resources/shaders/hull.frag";
	createShader(ShaderHandle.HULL, vert, frag, gl);

	vert = "/resources/shaders/overlay-font.vert";
	frag = "/resources/shaders/overlay-font.frag";
	createShader(ShaderHandle.OVERLAY_FONT, vert, frag, gl);

	vert = "/resources/shaders/overlay-flat.vert";
	frag = "/resources/shaders/overlay-flat.frag";
	createShader(ShaderHandle.OVERLAY_FLAT, vert, frag, gl);

	vert = "/resources/shaders/debug.vert";
	frag = "/resources/shaders/debug.frag";
	createShader(ShaderHandle.DEBUG, vert, frag, gl);

	vert = "/resources/shaders/skybox.vert";
	frag = "/resources/shaders/skybox.frag";
	createShader(ShaderHandle.SKYBOX, vert, frag, gl);

}

	/**
	 * Basic message dispatch
	 *
	 * @param message
	 */
	private void handleMessage(RenderMessage message) {
		assert (Thread.currentThread() == _renderThread);

		if (message instanceof CreateWindowMessage) {
			createWindowImp((CreateWindowMessage) message);
			return;
		}
		if (message instanceof SetCameraMessage) {
			setCameraInfoImp((SetCameraMessage) message);
			return;
		}
		if (message instanceof OffScreenMessage) {
			offScreenImp((OffScreenMessage) message);
			return;
		}
		if (message instanceof CloseWindowMessage) {
			closeWindowImp((CloseWindowMessage) message);
			return;
		}
		if (message instanceof CreateOffscreenTargetMessage) {
			populateOffscreenTarget(((CreateOffscreenTargetMessage)message).target);
		}
		if (message instanceof FreeOffscreenTargetMessage) {
			freeOffscreenTargetImp(((FreeOffscreenTargetMessage)message).target);
		}
	}

	private void initSharedContext() {
		assert (Thread.currentThread() == _renderThread);
		assert (_drawContext == null);

		int res = _sharedContext.makeCurrent();
		assert (res == GLContext.CONTEXT_CURRENT);

		if (USE_DEBUG_GL) {
			_sharedContext.setGL(new DebugGL2(_sharedContext.getGL().getGL2()));
		}

		GL2GL3 gl = _sharedContext.getGL().getGL2GL3();
		initShaders(gl);

		// Sub system specific intitializations
		DebugUtils.init(this, gl);
		Polygon.init(this, gl);
		MeshProto.init(this, gl);
		_texCache.init(gl);

		// Load the bad mesh proto
		MeshData badData = MeshDataCache.getBadMesh();
		MeshProto badProto = new MeshProto(badData, _safeGraphics, !_safeGraphics);
		_protoCache.put(MeshDataCache.BAD_MESH_KEY, badProto);
		badProto.loadGPUAssets(gl, this);

		_skybox = new Skybox();

		_sharedContext.release();
	}

	private void loadMeshProtoImp(final MeshProtoKey key) {

		//long startNanos = System.nanoTime();

		assert(_drawContext == null || !_drawContext.isCurrent());

		if (_protoCache.get(key) != null) {
			return; // This mesh has already been loaded
		}

		int res = _sharedContext.makeCurrent();
		assert (res == GLContext.CONTEXT_CURRENT);

		GL2GL3 gl = _sharedContext.getGL().getGL2GL3();

		MeshData data = MeshDataCache.getMeshData(key);
		MeshProto proto = new MeshProto(data, _safeGraphics, !_safeGraphics);

		assert (proto != null);
		proto.loadGPUAssets(gl, this);

		if (!proto.isLoadedGPU()) {
			// This did not load cleanly, clear it out and use the default bad mesh asset
			proto.freeResources(gl);

			System.out.printf("Could not load GPU assset: %s\n", key.getURL().toString());

			proto = _protoCache.get(MeshDataCache.BAD_MESH_KEY);
		}
		_protoCache.put(key, proto);

		_sharedContext.release();

//		long endNanos = System.nanoTime();
//		long ms = (endNanos - startNanos) /1000000L;
//		System.out.println("LoadMeshProtoImp time:" + ms + "ms");

	}

	private void loadTessFontImp(TessFontKey key) {
		if (_fontCache.get(key) != null) {
			return; // This font has already been loaded
		}

		TessFont tf = new TessFont(key);

		_fontCache.put(key, tf);
	}

	// Recreate the internal scene based on external input
	private void updateRenderableScene() {
		synchronized (_sceneLock) {
			long sceneStart = System.nanoTime();

			_currentScene = new ArrayList<Renderable>();
			_currentOverlay = new ArrayList<OverlayRenderable>();

			for (RenderProxy proxy : _proxyScene) {
				proxy.collectRenderables(this, _currentScene);
				proxy.collectOverlayRenderables(this, _currentOverlay);
			}

			long sceneTime = System.nanoTime() - sceneStart;
			_sceneTimeMS = sceneTime / 1000000.0;
		}
	}

	public static class PickResult {
		public double dist;
		public long pickingID;

		public PickResult(double dist, long pickingID) {
			this.dist = dist;
			this.pickingID = pickingID;
		}
	}

	/**
	 * Cast the provided ray into the current scene and return the list of bounds collisions
	 * @param ray
	 * @return
	 */
	public List<PickResult> pick(Ray pickRay, int viewID, boolean precise) {
		// Do not update the scene while a pick is underway
		ArrayList<PickResult> ret = new ArrayList<PickResult>();

		if (_currentScene == null) {
			return ret;
		}

		synchronized (_sceneLock) {
			for (Renderable r : _currentScene) {
				double rayDist = r.getCollisionDist(pickRay, precise);
				if (rayDist >= 0.0) {

					// Also check that this is visible
					double centerDist = pickRay.getDistAlongRay(r.getBoundsRef().center);

					if (r.renderForView(viewID, centerDist)) {
						ret.add(new PickResult(rayDist, r.getPickingID()));
					}
				}
			}
			return ret;
		}
	}

	public static class WindowMouseInfo {
		public int x, y;
		public int width, height;
		public int viewableX, viewableY;
		public boolean mouseInWindow;
		public CameraInfo cameraInfo;
	}

	/**
	 * Get Window specific information about the mouse. This is very useful for picking on the App side
	 * @param windowID
	 * @return
	 */
	public WindowMouseInfo getMouseInfo(int windowID) {
		synchronized(_openWindows) {
			RenderWindow w = _openWindows.get(windowID);

			if (w == null) {
				return null; // Not a valid window ID, or the window has closed
			}

			WindowMouseInfo info = new WindowMouseInfo();

			info.x = w.getMouseX();
			info.y = w.getMouseY();
			info.width = w.getViewableWidth();
			info.height = w.getViewableHeight();
			info.viewableX = w.getViewableX();
			info.viewableY = w.getViewableY();
			info.mouseInWindow = w.isMouseInWindow();
			info.cameraInfo = w.getCameraRef().getInfo();

			return info;
		}
	}

	public CameraInfo getCameraInfo(int windowID) {
		synchronized(_openWindows) {
			RenderWindow w = _openWindows.get(windowID);

			if (w == null) {
				return null; // Not a valid window ID, or the window has closed
			}

			return w.getCameraRef().getInfo();
		}
	}

	// Common cleanup code for window closing. Applies to both user closed and programatically closed windows
	private void windowCleanup(int windowID) {
		RenderWindow w;
		synchronized(_openWindows) {

			w = _openWindows.get(windowID);
			if (w == null) {
				return;
			}

			_openWindows.remove(windowID);
		}

		w.getAWTFrameRef().setVisible(false);

		// Fire the window closing callback
		w.getWindowListener().windowClosing();

	}

	private class GLWindowListener implements WindowListener, ComponentListener {

		private int _windowID;
		public GLWindowListener(int id) {
			_windowID = id;
		}

		private WindowInteractionListener getListener() {
			synchronized(_openWindows) {
				RenderWindow w = _openWindows.get(_windowID);

				if (w == null) {
					return null; // Not a valid window ID, or the window has closed
				}

				return w.getWindowListener();
			}
		}

		@Override
		public void windowDestroyNotify(WindowEvent we) {
			windowCleanup(_windowID);
		}

		@Override
		public void windowDestroyed(WindowEvent arg0) {
		}

		@Override
		public void windowGainedFocus(WindowEvent arg0) {
			WindowInteractionListener listener = getListener();
			if (listener != null) {
				listener.windowGainedFocus();
			}
		}

		@Override
		public void windowLostFocus(WindowEvent arg0) {
		}

		@Override
		public void windowMoved(WindowEvent arg0) {
		}

		@Override
		public void windowRepaint(WindowUpdateEvent arg0) {
		}

		@Override
		public void windowResized(WindowEvent arg0) {
		}

		private void updateWindowSizeAndPos() {
			RenderWindow w;
			synchronized(_openWindows) {
				w = _openWindows.get(_windowID);
				if (w == null) {
					return;
				}
			}
			w.getWindowListener().windowMoved(w.getWindowX(), w.getWindowY(), w.getWindowWidth(), w.getWindowHeight());
		}

		@Override
		public void componentHidden(ComponentEvent arg0) {
		}

		@Override
		public void componentMoved(ComponentEvent arg0) {
			updateWindowSizeAndPos();
		}

		@Override
		public void componentResized(ComponentEvent arg0) {
			updateWindowSizeAndPos();
		}

		@Override
		public void componentShown(ComponentEvent arg0) {
		}

	}

	private class RenderGLListener implements GLEventListener {

		private RenderWindow _window;
		private long _lastFrameNanos = 0;

		public void setWindow(RenderWindow win) {
			_window = win;
		}

		@Override
		public void init(GLAutoDrawable drawable) {
			synchronized (_rendererLock) {
				// Per window initialization
				if (USE_DEBUG_GL) {
					drawable.setGL(new DebugGL2(drawable.getGL().getGL2()));
				}

				GL2GL3 gl = drawable.getGL().getGL2GL3();

				// Some of this is probably redundant, but here goes
				gl.glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
				gl.glEnable(GL.GL_DEPTH_TEST);
				gl.glClearDepth(1.0);

				gl.glDepthFunc(GL2GL3.GL_LEQUAL);

				gl.glEnable(GL2GL3.GL_CULL_FACE);
				gl.glCullFace(GL2GL3.GL_BACK);

				gl.glEnable(GL.GL_MULTISAMPLE);

				gl.glBlendEquationSeparate(GL2GL3.GL_FUNC_ADD, GL2GL3.GL_MAX);
				gl.glBlendFuncSeparate(GL2GL3.GL_SRC_ALPHA, GL2GL3.GL_ONE_MINUS_SRC_ALPHA, GL2GL3.GL_ONE, GL2GL3.GL_ONE);

			}
		}

		@Override
		public void dispose(GLAutoDrawable drawable) {
			synchronized (_rendererLock) {

				GL2GL3 gl = drawable.getGL().getGL2GL3();

				Map<Integer, Integer> vaoMap = _window.getVAOMap();

				int[] vaos = new int[vaoMap.size()];
				int index = 0;
				for (int vao : vaoMap.values()) {
					vaos[index++] = vao;
				}
				if (vaos.length > 0) {
					gl.glDeleteVertexArrays(vaos.length, vaos, 0);
				}
			}
		}

		@Override
		public void display(GLAutoDrawable drawable) {
			synchronized (_rendererLock) {

				Camera cam = _window.getCameraRef();

				// The ray of the current mouse position (or null if the mouse is not hovering over the window)
				Ray pickRay = RenderUtils.getPickRay(getMouseInfo(_window.getWindowID()));

				PerfInfo pi = new PerfInfo();

				long startNanos = System.nanoTime();

				_allowDelayedTextures = true;

				// Cache the current scene. This way we don't need to lock it for the full render
				ArrayList<Renderable> scene = new ArrayList<Renderable>(_currentScene.size());
				ArrayList<OverlayRenderable> overlay = new ArrayList<OverlayRenderable>(_currentOverlay.size());
				synchronized(_sceneLock) {
					scene.addAll(_currentScene);
					overlay.addAll(_currentOverlay);
				}

				renderScene(drawable.getContext(), _window.getVAOMap(),
				            scene, overlay,
				            cam, _window.getViewableWidth(), _window.getViewableHeight(),
				            pickRay, _window.getViewID(), pi);

				GL2GL3 gl = drawable.getContext().getGL().getGL2GL3(); // Just to clean up the code below

				if (RENDER_DEBUG_INFO) {
					// Draw a window specific performance counter
					gl.glDisable(GL2GL3.GL_DEPTH_TEST);
					_drawContext = drawable.getContext();
					StringBuilder perf = new StringBuilder("Objects Culled: ").append(pi.objectsCulled);
					perf.append(" VRAM: ").append(_usedVRAM/(1024.0*1024.0)).append("MB");
					perf.append(" Frame time (ms) :").append(_lastFrameNanos / 1000000.0);
					perf.append(" SceneTime: ").append(_sceneTimeMS);
					perf.append(" Loop Time: ").append(_loopTimeMS);
					TessFont defFont = getTessFont(_defaultBoldFontKey);
					OverlayString os = new OverlayString(defFont, perf.toString(), ColourInput.BLACK,
					                                     10, 10, 15, false, false, DisplayModel.ALWAYS);
					os.render(_window.getVAOMap(), Renderer.this,
					          _window.getViewableWidth(), _window.getViewableHeight());

					// Also draw this window's debug string
					os = new OverlayString(defFont, _window.getDebugString(), ColourInput.BLACK,
					                       10, 10, 30, false, false, DisplayModel.ALWAYS);
					os.render(_window.getVAOMap(), Renderer.this,
					          _window.getViewableWidth(), _window.getViewableHeight());

					_drawContext = null;
					gl.glEnable(GL2GL3.GL_DEPTH_TEST);

				}

				gl.glFinish();

				long endNanos = System.nanoTime();
				_lastFrameNanos = endNanos - startNanos;
			}
		}

		@Override
		public void reshape(GLAutoDrawable drawable, int x, int y, int width,
				int height) {

			//_window.resized(width, height);
			Camera cam = _window.getCameraRef();
			cam.setAspectRatio((double) width / (double) height);
		}

	}

	/**
	 * Abstract base type for internal renderer messages
	 */
	private static class RenderMessage {
		@SuppressWarnings("unused")
		public long queueTime = System.nanoTime();
	}

	private static class CreateWindowMessage extends RenderMessage {
		public int x, y;
		public int width, height;
		public String title, name;
		public WindowInteractionListener listener;
		public int windowID, viewID;
		public Image icon;

		public CreateWindowMessage(int x, int y, int width, int height, String title,
				String name, int windowID, int viewID, Image icon, WindowInteractionListener listener) {
			this.x = x;
			this.y = y;
			this.width = width;
			this.height = height;
			this.title = title;
			this.name = name;
			this.listener = listener;
			this.windowID = windowID;
			this.viewID = viewID;
			this.icon = icon;
		}
	}

	private static class SetCameraMessage extends RenderMessage {
		public int windowID;
		public CameraInfo cameraInfo;
		public SetCameraMessage(int windowID, CameraInfo cameraInfo) {
			this.windowID = windowID;
			this.cameraInfo = cameraInfo;
		}
	}

	private static class OffScreenMessage extends RenderMessage {
		public ArrayList<RenderProxy> scene;
		public int viewID;
		public Camera cam;
		public int width, height;
		public Future<BufferedImage> result;
		public OffscreenTarget target;

		OffScreenMessage(ArrayList<RenderProxy> s, int vID, Camera c, int w, int h, Future<BufferedImage> r, OffscreenTarget t) {
			scene = s; viewID = vID; cam = c; width = w; height = h; result = r;
			target = t;
		}
	}

	private static class CloseWindowMessage extends RenderMessage {
		public int windowID;
		public CloseWindowMessage(int id) {
			windowID = id;
		}
	}

	private static class CreateOffscreenTargetMessage extends RenderMessage {
		public OffscreenTarget target;
	}

	private static class FreeOffscreenTargetMessage extends RenderMessage {
		public OffscreenTarget target;
	}

	public TexCache getTexCache() {
		return _texCache;
	}

	public static boolean debugDrawHulls() {
		return false;
	}

	public static boolean debugDrawAABBs() {
		return false;
	}

	public static boolean debugDrawArmatures() {
		return false;
	}

	public boolean isInitialized() {
		 return _initialized.get() && !_fatalError.get();
	}

	public boolean hasFatalError() {
		return _fatalError.get();
	}

	public String getErrorString() {
		return _errorString;
	}

	public StackTraceElement[] getFatalStackTrace() {
		return _fatalStackTrace;
	}

	public TessFontKey getDefaultFont() {
		return _defaultFontKey;
	}

	public boolean allowDelayedTextures() {
		return _allowDelayedTextures;
	}

	private void logException(Throwable t) {
		_exceptionLogger.logException(t);

		// For now print a synopsis for all exceptions thrown
		printExceptionLog();
		t.printStackTrace();
	}

	private void printExceptionLog() {
		System.out.println("Exceptions from Renderer: ");

		_exceptionLogger.printExceptionLog();

		System.out.println("");

	}

	/**
	 * Queue up an off screen rendering
	 * @param scene
	 * @param cam
	 * @param width
	 * @param height
	 * @return
	 */
	public Future<BufferedImage> renderOffscreen(ArrayList<RenderProxy> scene, int viewID, CameraInfo camInfo,
	                                   int width, int height, Runnable runWhenDone, OffscreenTarget target) {
		Future<BufferedImage> result = new Future<BufferedImage>(runWhenDone);

		Camera cam = new Camera(camInfo, (double)width/(double)height);

		synchronized (_renderMessages) {
			addRenderMessage(new OffScreenMessage(scene, viewID, cam, width, height, result, target));
		}

		synchronized (_displayNeeded) {
			_displayNeeded.set(true);
			_displayNeeded.notifyAll();
		}

		return result;
	}

	public OffscreenTarget createOffscreenTarget(int width, int height) {
		OffscreenTarget ret = new OffscreenTarget(width, height);

		synchronized (_renderMessages) {
			CreateOffscreenTargetMessage msg = new CreateOffscreenTargetMessage();
			msg.target = ret;
			addRenderMessage(msg);
		}
		return ret;
	}

	public void freeOffscreenTarget(OffscreenTarget target) {
		synchronized (_renderMessages) {
			FreeOffscreenTargetMessage msg = new FreeOffscreenTargetMessage();
			msg.target = target;
			addRenderMessage(msg);
		}
	}

	/**
	 * Create the resources for an OffscreenTarget
	 */
	private void populateOffscreenTarget(OffscreenTarget target) {


		int width = target.getWidth();
		int height = target.getHeight();

		_sharedContext.makeCurrent();
		GL3 gl = _sharedContext.getGL().getGL3(); // Just to clean up the code below

		// This does not support opengl 3, so for now we don't support off screen rendering
		if (gl == null) {
			_sharedContext.release();
			return;
		}

		// Create a new frame buffer for this draw operation
		int[] temp = new int[2];
		gl.glGenFramebuffers(2, temp, 0);
		int drawFBO = temp[0];
		int blitFBO = temp[1];

		gl.glGenTextures(2, temp, 0);
		int drawTex = temp[0];
		int blitTex = temp[1];

		gl.glGenRenderbuffers(1, temp, 0);
		int depthBuf = temp[0];

		gl.glBindTexture(GL3.GL_TEXTURE_2D_MULTISAMPLE, drawTex);
		gl.glTexImage2DMultisample(GL3.GL_TEXTURE_2D_MULTISAMPLE, 4, GL2GL3.GL_RGBA8, width, height, true);

		gl.glBindRenderbuffer(GL2GL3.GL_RENDERBUFFER, depthBuf);
		gl.glRenderbufferStorageMultisample(GL2GL3.GL_RENDERBUFFER, 4, GL2GL3.GL_DEPTH_COMPONENT, width, height);

		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, drawFBO);
		gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_COLOR_ATTACHMENT0, GL3.GL_TEXTURE_2D_MULTISAMPLE, drawTex, 0);

		gl.glFramebufferRenderbuffer(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_DEPTH_ATTACHMENT, GL2GL3.GL_RENDERBUFFER, depthBuf);

		int fbStatus = gl.glCheckFramebufferStatus(GL2GL3.GL_FRAMEBUFFER);
		assert(fbStatus == GL2GL3.GL_FRAMEBUFFER_COMPLETE);

		gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, blitTex);
		gl.glTexImage2D(GL2GL3.GL_TEXTURE_2D, 0, GL2GL3.GL_RGBA8, width, height,
		                0, GL2GL3.GL_RGBA, GL2GL3.GL_BYTE, null);

		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, blitFBO);
		gl.glFramebufferTexture2D(GL2GL3.GL_FRAMEBUFFER, GL2GL3.GL_COLOR_ATTACHMENT0, GL2GL3.GL_TEXTURE_2D, blitTex, 0);

		gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, 0);

		target.load(drawFBO, drawTex, depthBuf, blitFBO, blitTex);

		_sharedContext.release();

	}

	private void freeOffscreenTargetImp(OffscreenTarget target) {
		if (!target.isLoaded()) {
			return; // Nothing to free
		}
		_sharedContext.makeCurrent();
		GL2GL3 gl = _sharedContext.getGL().getGL2GL3(); // Just to clean up the code below

		int[] temp = new int[2];

		temp[0] = target.getDrawFBO();
		temp[1] = target.getBlitFBO();
		gl.glDeleteFramebuffers(2, temp, 0);

		temp[0] = target.getDrawTex();
		temp[1] = target.getBlitTex();
		gl.glDeleteTextures(2, temp, 0);

		temp[0] = target.getDepthBuffer();
		gl.glDeleteRenderbuffers(1, temp, 0);

		target.free();

		_sharedContext.release();
	}

	private void offScreenImp(OffScreenMessage message) {

		synchronized(_rendererLock) {
		try {

			boolean isTempTarget;
			OffscreenTarget target;
			if (message.target == null) {
				isTempTarget = true;
				target = new OffscreenTarget(message.width, message.height);
				populateOffscreenTarget(target);
			} else {
				isTempTarget = false;
				target = message.target;
				assert(target.getWidth() == message.width);
				assert(target.getHeight() == message.height);
			}

			int width = message.width;
			int height = message.height;

			if (!target.isLoaded()) {
				message.result.setFailed("Contexted not loaded. Is OpenGL 3 supported?");
				return;
			}
			assert(target.isLoaded());

			// Collect the renderables
			ArrayList<Renderable> renderables;
			ArrayList<OverlayRenderable> overlay;

			if (message.scene != null) {
				renderables = new ArrayList<Renderable>();
				overlay = new ArrayList<OverlayRenderable>();
				for (RenderProxy p : message.scene) {
					p.collectRenderables(this, renderables);
					p.collectOverlayRenderables(this, overlay);
				}
			} else {
				// Use the current current scene if one is not provided
				synchronized(_sceneLock) {
					renderables = new ArrayList<Renderable>(_currentScene);
					overlay = new ArrayList<OverlayRenderable>(_currentOverlay);
				}
			}

			_sharedContext.makeCurrent();
			GL2GL3 gl = _sharedContext.getGL().getGL2GL3(); // Just to clean up the code below

			gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, target.getDrawFBO());

			gl.glClearColor(0, 0, 0, 0);
			gl.glViewport(0, 0, width, height);
			gl.glEnable(GL2GL3.GL_DEPTH_TEST);
			gl.glDepthFunc(GL2GL3.GL_LEQUAL);

			_allowDelayedTextures = false;

			PerfInfo perfInfo = new PerfInfo();
			// Okay, now actually render this thing...
			renderScene(_sharedContext, _sharedVaoMap, renderables, overlay, message.cam,
			            width, height, null, message.viewID, perfInfo);

			gl.glFinish();

			gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, target.getBlitFBO());

			gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, target.getDrawFBO());

			gl.glBlitFramebuffer(0, 0, width, height, 0, 0, width, height, GL2GL3.GL_COLOR_BUFFER_BIT, GL2GL3.GL_NEAREST);

			gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, target.getBlitTex());

			IntBuffer pixels = target.getPixelBuffer();

			gl.glGetTexImage(GL2GL3.GL_TEXTURE_2D, 0, GL2GL3.GL_BGRA, GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV, pixels);

			gl.glBindTexture(GL2GL3.GL_TEXTURE_2D, 0);

			BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			for (int h = 0; h < height; ++h) {
				// Set this one scan line at a time, in the opposite order as java is y down
				img.setRGB(0, h, width, 1, pixels.array(), (height - 1 - h) * width, width);

			}

			message.result.setComplete(img);

			// Clean up
			gl.glBindFramebuffer(GL2GL3.GL_READ_FRAMEBUFFER, 0);
			gl.glBindFramebuffer(GL2GL3.GL_DRAW_FRAMEBUFFER, 0);

			if (isTempTarget) {
				freeOffscreenTargetImp(target);
			}

		} catch (GLException ex){
			message.result.setFailed(ex.getMessage());
		} finally {
			if (_sharedContext.isCurrent())
				_sharedContext.release();
		}
		} // synchronized(_rendererLock)
	}

	/**
	 * Returns true if the current thread is this renderer's render thread
	 * @return
	 */
	public boolean isRenderThread() {
		return (Thread.currentThread() == _renderThread);
	}

	private static class PerfInfo {
		public int objectsCulled = 0;
	}

private static class TransSortable implements Comparable<TransSortable> {
	public Renderable r;
	public double dist;

	@Override
	public int compareTo(TransSortable o) {
		// Sort such that largest distance sorts to front of list
		// by reversing argument order in compare.
		return Double.compare(o.dist, this.dist);
	}
}

	private void renderScene(GLContext context, Map<Integer, Integer> vaoMap,
	                        List<Renderable> scene, List<OverlayRenderable> overlay,
	                        Camera cam, int width, int height, Ray pickRay,
	                        int viewID, PerfInfo perfInfo) {

		final Vec4d viewDir = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		cam.getViewDir(viewDir);

		final Vec3d temp = new Vec3d();

		assert (_drawContext == null);
		_drawContext = context;
		GL2GL3 gl = _drawContext.getGL().getGL2GL3(); // Just to clean up the code below

		gl.glClear(GL2GL3.GL_COLOR_BUFFER_BIT
				| GL2GL3.GL_DEPTH_BUFFER_BIT);

		// The 'height' of a pixel 1 unit from the viewer
		double unitPixelHeight = 2 * Math.tan(cam.getFOV()/2.0) / height;

		ArrayList<TransSortable> transparents = new ArrayList<TransSortable>();

		if (scene == null)
			return;

		for (Renderable r : scene) {
			AABB bounds = r.getBoundsRef();
			double dist = cam.distToBounds(bounds);

			if (!r.renderForView(viewID, dist)) {
				continue;
			}

			if (!cam.collides(bounds)) {
				++perfInfo.objectsCulled;
				continue;
			}

			double apparentSize = 2 * bounds.radius.mag3() / Math.abs(dist);
			if (apparentSize < unitPixelHeight) {
				// This object is too small to draw
				++perfInfo.objectsCulled;
				continue;
			}
			if (r.hasTransparent()) {
				// Defer rendering of transparent objects
				TransSortable ts = new TransSortable();
				ts.r = r;
				temp.set3(r.getBoundsRef().center);
				temp.sub3(cam.getTransformRef().getTransRef());
				ts.dist = temp.dot3(viewDir);
				transparents.add(ts);
			}

			r.render(vaoMap, this, cam, pickRay);
		}

		gl.glEnable(GL2GL3.GL_BLEND);
		gl.glDepthMask(false);

		// Draw the skybox after
		_skybox.setTexture(cam.getInfoRef().skyboxTexture);
		_skybox.render(vaoMap, this, cam);

		Collections.sort(transparents);
		for (TransSortable ts : transparents) {

			AABB bounds = ts.r.getBoundsRef();
			if (!cam.collides(bounds)) {
				++perfInfo.objectsCulled;
				continue;
			}

			ts.r.renderTransparent(vaoMap, this, cam, pickRay);
		}

		gl.glDisable(GL2GL3.GL_BLEND);
		gl.glDepthMask(true);

		// Debug render AABBs
		if (debugDrawAABBs())
		{
			Color4d yellow = new Color4d(1, 1, 0, 1.0d);
			Color4d red = new Color4d(1, 0, 0, 1.0d);
			for (Renderable r : scene) {
				Color4d aabbColor = yellow;
				if (pickRay != null && r.getBoundsRef().collisionDist(pickRay) > 0) {
					aabbColor = red;
				}
				DebugUtils.renderAABB(vaoMap, this, r.getBoundsRef(), aabbColor, cam);
			}
		} // for renderables

		// Now draw the overlay
		gl.glDisable(GL2GL3.GL_DEPTH_TEST);

		if (overlay != null) {
			for (OverlayRenderable r : overlay) {
				if (!r.renderForView(viewID)) {
					continue;
				}

				r.render(vaoMap, this, width, height);
			}
		}

		gl.glEnable(GL2GL3.GL_DEPTH_TEST);

		gl.glBindVertexArray(0);

		_drawContext = null;
	}

	public void usingVRAM(long bytes) {
		_usedVRAM += bytes;
	}

	/////////////////////////////////////////////////////////////////////
	// Below are functions inherited from GLAnimatorControl
	// the interface is a bit of a mess and there's a lot here we don't need
	@Override
	public long getFPSStartTime() {
		// Not supported
		assert(false);
		return 0;
	}

	@Override
	public float getLastFPS() {
		// Not supported
		return 0;
	}

	@Override
	public long getLastFPSPeriod() {
		// Not supported
		return 0;
	}

	@Override
	public long getLastFPSUpdateTime() {
		// Not supported
		return 0;
	}

	@Override
	public float getTotalFPS() {
		// Not supported
		return 0;
	}

	@Override
	public long getTotalFPSDuration() {
		// Not supported
		return 0;
	}

	@Override
	public int getTotalFPSFrames() {
		// Not supported
		return 0;
	}

	@Override
	public int getUpdateFPSFrames() {
		// Not supported
		return 0;
	}

	@Override
	public void resetFPSCounter() {
		// Not supported
	}

	@Override
	public void setUpdateFPSFrames(int arg0, PrintStream arg1) {
		// Not supported
	}

	@Override
	public void add(GLAutoDrawable arg0) {
		// Not supported
		assert(false);
	}

	@Override
	public Thread getThread() {
		return _renderThread;
	}

	@Override
	public boolean isAnimating() {
		queueRedraw();
		return true;
	}

	@Override
	public boolean isPaused() {
		synchronized (_rendererLock) { // Make sure we aren't currently rendering
			return isPaused;
		}
	}

	@Override
	public boolean isStarted() {
		return true;
	}

	@Override
	public boolean pause() {
		synchronized(_rendererLock) {
			isPaused = true;
			return true;
		}
	}

	@Override
	public void remove(GLAutoDrawable arg0) {
		// Not supported
		assert(false);
	}

	@Override
	public boolean resume() {
		isPaused = false;
		queueRedraw();
		return true;
	}

	@Override
	public boolean start() {
		// Not supported
		assert(false);
		return false;
	}

	@Override
	public boolean stop() {
		// Not supported
		assert(false);
		return false;
	}
}
