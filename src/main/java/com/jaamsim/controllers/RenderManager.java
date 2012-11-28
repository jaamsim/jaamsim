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
package com.jaamsim.controllers;

import java.awt.Frame;
import java.awt.Image;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.vecmath.Vector3d;

import com.jaamsim.font.TessFont;
import com.jaamsim.input.WindowInteractionListener;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Matrix4d;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vector4d;
import com.jaamsim.observers.CubeObserver;
import com.jaamsim.observers.DisplayModelObserver;
import com.jaamsim.observers.DisplayModelState;
import com.jaamsim.observers.RenderObserver;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.Future;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.render.MeshProto;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.OffscreenTarget;
import com.jaamsim.render.PreviewCache;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.render.util.ExceptionLogger;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.Input;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModel;
import com.sandwell.JavaSimulation3D.GUIFrame;
import com.sandwell.JavaSimulation3D.Graph;
import com.sandwell.JavaSimulation3D.InputAgent;
import com.sandwell.JavaSimulation3D.ObjectSelector;
import com.sandwell.JavaSimulation3D.Region;

/**
 * Top level owner of the JaamSim renderer. This class both owns and drives the Renderer object, but is also
 * responsible for gathering rendering data every frame.
 * @author Matt Chudleigh
 *
 */
public class RenderManager implements DragSourceListener {

	private static RenderManager s_instance = null;
	/**
	 * Basic singleton pattern
	 */
	public static void initialize() {
		s_instance = new RenderManager();
	}

	public static RenderManager inst() { return s_instance; }

	private final Thread _managerThread;
	private final Renderer _renderer;
	private final AtomicBoolean _finished = new AtomicBoolean(false);
	private final AtomicBoolean _fatalError = new AtomicBoolean(false);
	private final AtomicBoolean _redraw = new AtomicBoolean(false);

	private final AtomicBoolean _screenshot = new AtomicBoolean(false);

	// These values are used to limit redraw rate, the stored values are time in milliseconds
	// returned by System.currentTimeMillis()
	private final AtomicLong _lastDraw = new AtomicLong(0);
	private final AtomicLong _scheduledDraw = new AtomicLong(0);

	private final ArrayList<RenderObserver> _observers = new ArrayList<RenderObserver>();

	private final ExceptionLogger _exceptionLogger;

	private final static double FPS = 60;
	private final Timer _timer;

	private final HashMap<Integer, CameraControl> _windowControls = new HashMap<Integer, CameraControl>();
	private final HashMap<Integer, View> _windowToViewMap= new HashMap<Integer, View>();
	private int _activeWindowID = -1;

	private long _lastEntitySequence = 0;

	private final Object _popupLock;
	private JPopupMenu _lastPopup;

	/**
	 * The last scene rendered
	 */
	private ArrayList<RenderProxy> _cachedScene;

	private DisplayEntity _selectedEntity = null;

	private double _simTime = 0.0d;

	private boolean _isDragging = false;
	private long _dragHandleID = 0;

	// The object type for drag-and-drop operation, if this is null, the user is not dragging
	private ObjectType _dndObjectType;
	private long _dndDropTime = 0;

	private VideoRecorder _recorder;

	private PreviewCache _previewCache = new PreviewCache();

	// Below are special PickingIDs for resizing and dragging handles
	public static final long MOVE_PICK_ID = -1;

	// For now this order is implicitly the same as the handle order in RenderObserver, don't re arrange it without touching
	// the handle list
	public static final long RESIZE_POSX_PICK_ID = -2;
	public static final long RESIZE_NEGX_PICK_ID = -3;
	public static final long RESIZE_POSY_PICK_ID = -4;
	public static final long RESIZE_NEGY_PICK_ID = -5;
	public static final long RESIZE_PXPY_PICK_ID = -6;
	public static final long RESIZE_PXNY_PICK_ID = -7;
	public static final long RESIZE_NXPY_PICK_ID = -8;
	public static final long RESIZE_NXNY_PICK_ID = -9;

	public static final long ROTATE_PICK_ID = -10;

	public static final long LINEDRAG_PICK_ID = -11;

	// Line nodes start at this constant and proceed into the negative range, therefore this should be the lowest defined constant
	public static final long LINENODE_PICK_ID = -12;

	private RenderManager() {
		_renderer = new Renderer();

		_exceptionLogger = new ExceptionLogger();

		createTestCubeAsset();

		_managerThread = new Thread(new Runnable() {
			public void run() {
				renderManagerLoop();
			}
		}, "RenderManagerThread");
		_managerThread.start();

		// Start the display timer
		_timer = new Timer("RedrawThread");
		TimerTask displayTask = new TimerTask() {
			public void run() {

				// Is a redraw scheduled
				long currentTime = System.currentTimeMillis();
				long scheduledTime = _scheduledDraw.get();
				long lastRedraw = _lastDraw.get();

				// Only draw if the scheduled time is before now and after the last redraw
				if (scheduledTime < lastRedraw || currentTime < scheduledTime) {
					return;
				}

				synchronized(_redraw) {
					if (_renderer.getNumOpenWindows() == 0 && !_screenshot.get()) {
						return; // Do not queue a redraw if there are no open windows
					}
					_redraw.set(true);
					_redraw.notifyAll();
				}
			}
		};

		_timer.scheduleAtFixedRate(displayTask, 0, (long) (1000 / (FPS*2)));

		_popupLock = new Object();
	}

	public void registerObserver(RenderObserver obs) {
		synchronized(_observers) {
			_observers.add(obs);
		}
	}

	public void deregisterObserver(RenderObserver obs) {
		synchronized(_observers) {
			_observers.remove(obs);
		}
	}

	public void updateTime(double simTime) {
		_simTime = simTime;

		queueRedraw();
	}

	public void queueRedraw() {
		long scheduledTime = _scheduledDraw.get();
		long lastRedraw = _lastDraw.get();

		if (scheduledTime > lastRedraw) {
			// A draw is scheduled
			return;
		}

		long newDraw = System.currentTimeMillis();
		long frameTime = (long)(1000.0/FPS);
		if (newDraw - lastRedraw < frameTime) {
			// This would be scheduled too soon
			newDraw = lastRedraw + frameTime;
		}
		_scheduledDraw.set(newDraw);
	}

	public void createWindow(View view) {

		// First see if this window has already been opened
		for (Map.Entry<Integer, CameraControl> entry : _windowControls.entrySet()) {
			if (entry.getValue().getView() == view) {
				// This view has a window, just reshow that one
				focusWindow(entry.getKey());
				return;
			}
		}

		IntegerVector windSize = (IntegerVector)view.getInput("WindowSize").getValue();
		IntegerVector windPos = (IntegerVector)view.getInput("WindowPosition").getValue();

		Image icon = GUIFrame.getWindowIcon();

		CameraControl control = new CameraControl(_renderer, view);
		int windowID = _renderer.createWindow(windPos.get(0), windPos.get(1),
		                                      windSize.get(0), windSize.get(1),
		                                      view.getID(),
		                                      view.getTitle(), view.getInputName(),
		                                      icon, control);

		_windowControls.put(windowID, control);
		_windowToViewMap.put(windowID, view);

		dirtyAllEntities();
	}

	public void closeAllWindows() {
		ArrayList<Integer> windIDs = _renderer.getOpenWindowIDs();
		for (int id : windIDs) {
			_renderer.closeWindow(id);
		}
	}

	public void windowClosed(int windowID) {
		_windowControls.remove(windowID);
		_windowToViewMap.remove(windowID);
	}

	public void setActiveWindow(int windowID) {
		_activeWindowID = windowID;
	}

	public static boolean isGood() {
		return (s_instance != null && !s_instance._finished.get() && !s_instance._fatalError.get());
	}

	private void dirtyAllEntities() {
		for (int i = 0; i < DisplayEntity.getAll().size(); ++i) {
			DisplayEntity.getAll().get(i).setGraphicsDataDirty();
		}
	}

	private void renderManagerLoop() {

		while (!_finished.get() && !_fatalError.get()) {
			try {

				if (_renderer.hasFatalError()) {
					// Well, something went horribly wrong
					_fatalError.set(true);
					System.out.printf("Renderer failed with error: %s\n", _renderer.getErrorString());

					// Do some basic cleanup
					_observers.clear();
					_windowControls.clear();
					_previewCache.clear();

					_timer.cancel();

					break;
				}

				if (!_renderer.isInitialized()) {
					// Give the renderer a chance to initialize
					try {
						Thread.sleep(100);
					} catch(InterruptedException e) {}
					continue;
				}

				// Hack, check if the entity sequence has been updated, and if so force a recheck of default
				// observers
				if (Entity.getEntitySequence() != _lastEntitySequence) {
					_lastEntitySequence = Entity.getEntitySequence();
					ArrayList<? extends Entity> allEnts = Entity.getAllCopy();
					for (int i = 0; i < allEnts.size(); ++i) {
						Entity e = allEnts.get(i);
						e.initDefaultObserver();
					}

					// Clear any Observers that no longer observe live entities
					clearDeadObservers();
				}

				for (CameraControl cc : _windowControls.values()) {
					cc.checkForUpdate();
				}

				_lastDraw.set(System.currentTimeMillis());

				_cachedScene = new ArrayList<RenderProxy>();
				DisplayModelObserver.clearCacheCounters();

				long startNanos = System.nanoTime();

				// Update all graphical entities in the simulation
				for (int i = 0; i < DisplayEntity.getAll().size(); i++) {
					try {
						DisplayEntity de = DisplayEntity.getAll().get(i);
						de.updateGraphics(_simTime);
					}
					// Catch everything so we don't screw up the behavior handling
					catch (Throwable e) {
						//e.printStackTrace();
					}
				}

				for (RenderObserver obs : _observers) {
					try {
						obs.collectProxies(_cachedScene);
					} catch (Throwable t) {
						// Log the exception in the exception list
						logException(t);
					}
				}

				// Collect selection proxies second so they always appear on top, this will need to be
				// better thought out soon....
				//TODO not suck here
				for (RenderObserver obs : _observers) {
					try {
						// Grab the drawable proxies if this object is selected
						if (obs.isObserving(_selectedEntity)) {
							obs.collectSelectionProxies(_cachedScene);
						}
					} catch (Throwable t) {
						// Log the exception in the exception list
						logException(t);
					}
				}

				long endNanos = System.nanoTime();

				_renderer.setScene(_cachedScene);

				String cacheString = " Hits: " + DisplayModelObserver.getCacheHits() + " Misses: " + DisplayModelObserver.getCacheMisses() +
				                     " Total: " + _observers.size();

				double gatherMS = (endNanos - startNanos) / 1000000.0;

				// Do some picking debug
				ArrayList<Integer> windowIDs = _renderer.getOpenWindowIDs();
				for (int id : windowIDs) {
					Renderer.WindowMouseInfo mouseInfo = _renderer.getMouseInfo(id);

					if (mouseInfo == null || !mouseInfo.mouseInWindow) {
						// Not currently picking for this window
						_renderer.setWindowDebugInfo(id, cacheString + " Not picking. GatherTime (ms)" + gatherMS, new ArrayList<Long>());
						continue;
					}

					List<PickData> picks = pickForMouse(id);
					ArrayList<Long> debugIDs = new ArrayList<Long>(picks.size());

					String debugString = cacheString + " Picked " + picks.size() + " entities at (" + mouseInfo.x + ", " + mouseInfo.y + "): ";

					for (PickData pd : picks) {
						debugString += Entity.idToName(pd.id);
						debugString += ", ";
						debugIDs.add(pd.id);
					}

					debugString += " Gather time (ms)" + gatherMS;

					_renderer.setWindowDebugInfo(id, debugString, debugIDs);
				}

				if (GUIFrame.getShuttingDownFlag()) {
					shutdown();
				}

				_renderer.queueRedraw();
				_redraw.set(false);

				if (_screenshot.get()) {
					takeScreenShot();
				}

				// Wait for a redraw request
				synchronized(_redraw) {
					while (!_redraw.get()) {
						try {
							_redraw.wait(30);
						} catch (InterruptedException e) {}
					}
				}
			} catch (Throwable t) {
				// Make a note of it, but try to keep going
				logException(t);
				printExceptionLog();
			}
		}

		_exceptionLogger.printExceptionLog();

	}

	/**
	 * Add a debug cube as an asset, is a cube from -0.5 to 0.5 in all 3 axis
	 */
	private void createTestCubeAsset() {
		// Build up a test cube to display...

		Vector4d[] verts = new Vector4d[36];
		Vector4d[] norms = new Vector4d[36];

		int i = 0;
		// +X
		verts[i++] = new Vector4d( 1, -1, -1 );
		verts[i++] = new Vector4d( 1, -1,  1 );
		verts[i++] = new Vector4d( 1,  1,  1 );

		verts[i++] = new Vector4d( 1, -1, -1 );
		verts[i++] = new Vector4d( 1,  1,  1 );
		verts[i++] = new Vector4d( 1,  1, -1 );

		// -X
		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d(-1,  1, -1 );
		verts[i++] = new Vector4d(-1,  1,  1 );

		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d(-1,  1,  1 );
		verts[i++] = new Vector4d(-1, -1,  1 );

		// +Y
		verts[i++] = new Vector4d(-1,  1, -1 );
		verts[i++] = new Vector4d( 1,  1, -1 );
		verts[i++] = new Vector4d( 1,  1,  1 );

		verts[i++] = new Vector4d(-1,  1, -1 );
		verts[i++] = new Vector4d( 1,  1,  1 );
		verts[i++] = new Vector4d(-1,  1,  1 );

		// -Y
		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d(-1, -1,  1 );
		verts[i++] = new Vector4d( 1, -1,  1 );

		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d( 1, -1,  1 );
		verts[i++] = new Vector4d( 1, -1, -1 );

		// +Z
		verts[i++] = new Vector4d(-1, -1,  1 );
		verts[i++] = new Vector4d( 1, -1,  1 );
		verts[i++] = new Vector4d( 1,  1,  1 );

		verts[i++] = new Vector4d(-1, -1,  1 );
		verts[i++] = new Vector4d( 1,  1,  1 );
		verts[i++] = new Vector4d(-1,  1,  1 );

		// -Z
		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d(-1,  1, -1 );
		verts[i++] = new Vector4d( 1,  1, -1 );

		verts[i++] = new Vector4d(-1, -1, -1 );
		verts[i++] = new Vector4d( 1,  1, -1 );
		verts[i++] = new Vector4d( 1, -1, -1 );

		i = 0;
		// Normals
		//+X
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(1, 0, 0);
		}
		//-X
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(-1, 0, 0);
		}

		//+Y
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(0, 1, 0);
		}
		//-Y
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(0, -1, 0);
		}

		//+Z
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(0, 0, 1);
		}
		//-Z
		for(int j = 0; j < 6; ++j) {
			norms[i++] = new Vector4d(0, 0, -1);
		}

		// Scale down to a 1x1x1 box
		Matrix4d scaleMat = Matrix4d.ScaleMatrix(0.5);
		for (Vector4d v : verts) {
			scaleMat.mult(v, v);
		}

		MeshProto proto = new MeshProto();
		proto.addSubMesh(verts, norms, null, null, new Vector4d(1,0,0), MeshProto.NO_TRANS, Vector4d.ORIGIN);
		proto.generateHull();

		_renderer.takeMeshProto(CubeObserver.CUBE_KEY, proto);
	}

	private void clearDeadObservers() {
		for (int i = 0 ; i < _observers.size();) {
			if (!_observers.get(i).hasObservee()) {
				// Dead observee
				_observers.remove(i);
			} else {
				i++;
			}
		}
	}

	// Temporary dumping ground until I find a better place for this
	public void popupMenu(int windowID) {
		synchronized (_popupLock) {

			Renderer.WindowMouseInfo mouseInfo = _renderer.getMouseInfo(windowID);
			if (mouseInfo == null) {
				// Somehow this window was closed along the way, just ignore this click
				return;
			}

			final Frame awtFrame = _renderer.getAWTFrame(windowID);
			if (awtFrame == null) {
				return;
			}

			List<PickData> picks = pickForMouse(windowID);

			ArrayList<DisplayEntity> ents = new ArrayList<DisplayEntity>();

			for (PickData pd : picks) {
				if (!pd.isEntity) { continue; }
				Entity ent = Entity.idToEntity(pd.id);
				if (ent == null) { continue; }
				if (!(ent instanceof DisplayEntity)) { continue; }

				DisplayEntity de = (DisplayEntity)ent;

				ents.add(de);
			}

			if (!mouseInfo.mouseInWindow) {
				// Somehow this window does not currently have the mouse over it.... ignore?
				return;
			}

			final JPopupMenu menu = new JPopupMenu();
			_lastPopup = menu;

			menu.setLightWeightPopupEnabled(false);
			final int menuX = mouseInfo.x + awtFrame.getInsets().left;
			final int menuY = mouseInfo.y + awtFrame.getInsets().top;

			if (ents.size() == 0) { return; } // Nothing to show

			if (ents.size() == 1) {
				populateMenu(menu, ObjectSelector.getMenuItems(ents.get(0), menuX, menuY));
			}
			else {
				// Several entities, let the user pick the interesting entity first
				for (final DisplayEntity de : ents) {
					JMenuItem thisItem = new JMenuItem(de.getInputName());
					thisItem.addActionListener( new ActionListener() {

						public void actionPerformed( ActionEvent event ) {
							menu.removeAll();
							populateMenu(menu, ObjectSelector.getMenuItems(de, menuX, menuY));
							menu.show(awtFrame, menuX, menuY);
						}
					} );

					menu.add( thisItem );
				}
			}

			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					menu.show(awtFrame, menuX, menuY);
					menu.repaint();
				}
			});

		} // synchronized (_popupLock)
	}

	public void handleSelection(int windowID) {

		List<PickData> picks = pickForMouse(windowID);

		Collections.sort(picks, new SelectionSorter());

		for (PickData pd : picks) {
			// Select the first entity after sorting
			if (pd.isEntity) {
				DisplayEntity ent = (DisplayEntity)Entity.idToEntity(pd.id);
				if (!ent.isMovable()) {
					continue;
				}
				FrameBox.setSelectedEntity(ent);
				queueRedraw();
				return;
			}
		}

		FrameBox.setSelectedEntity(null);
		queueRedraw();
	}

	/**
	 * Utility, convert a window and mouse coordinate into a list of picking IDs for that pixel
	 * @param windowID
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	private List<PickData> pickForMouse(int windowID) {
		Renderer.WindowMouseInfo mouseInfo = _renderer.getMouseInfo(windowID);

		if (mouseInfo == null || !mouseInfo.mouseInWindow) {
			// The mouse is not actually in the window, or the window was closed along the way
			return new ArrayList<PickData>(); // empty set
		}

		Ray pickRay = RenderUtils.getPickRay(mouseInfo);

		return pickForRay(pickRay);
	}


	/**
	 * PickData represents enough information to sort a list of picks based on a picking preference
	 * metric. For now it holds the object size and distance from pick point to object center
	 *
	 */
	private static class PickData {
		public long id;
		public double size;
		boolean isEntity;

		/**
		 * This pick does not correspond to an entity, and is a handle or other UI element
		 * @param id
		 */
		public PickData(long id) {
			this.id = id;
			size = 0;
			isEntity = false;
		}
		/**
		 * This pick was an entity
		 * @param id - the id
		 * @param ent - the entity
		 */
		public PickData(long id, DisplayEntity ent) {
			this.id = id;
			size= ent.getSize().length();

			isEntity = true;
		}
	}

	/**
	 * This Comparator sorts based on entity selection preference
	 */
	private class SelectionSorter implements Comparator<PickData> {

		@Override
		public int compare(PickData p0, PickData p1) {
			if (p0.isEntity && !p1.isEntity) {
				return -1;
			}
			if (!p0.isEntity && p1.isEntity) {
				return 1;
			}
			if (p0.size == p1.size) {
				return 0;
			}
			return (p0.size < p1.size) ? -1 : 1;
		}

	}

	/**
	 * This Comparator sorts based on interaction handle priority
	 */
	private class HandleSorter implements Comparator<PickData> {

		@Override
		public int compare(PickData p0, PickData p1) {
			int p0priority = getHandlePriority(p0.id);
			int p1priority = getHandlePriority(p1.id);
			if (p0priority == p1priority)
				return 0;

			return (p0priority < p1priority) ? 1 : -1;
		}
	}

	/**
	 * This determines the priority for interaction handles if several are selectable at drag time
	 * @param handleID
	 * @return
	 */
	private static int getHandlePriority(long handleID) {
		if (handleID == MOVE_PICK_ID) return 1;
		if (handleID == LINEDRAG_PICK_ID) return 1;

		if (handleID <= LINENODE_PICK_ID) return 2;

		if (handleID == ROTATE_PICK_ID) return 3;

		if (handleID == RESIZE_POSX_PICK_ID) return 4;
		if (handleID == RESIZE_NEGX_PICK_ID) return 4;
		if (handleID == RESIZE_POSY_PICK_ID) return 4;
		if (handleID == RESIZE_NEGY_PICK_ID) return 4;

		if (handleID == RESIZE_PXPY_PICK_ID) return 5;
		if (handleID == RESIZE_PXNY_PICK_ID) return 5;
		if (handleID == RESIZE_NXPY_PICK_ID) return 5;
		if (handleID == RESIZE_NXNY_PICK_ID) return 5;

		return 0;
	}


	/**
	 * Perform a pick from this world space ray
	 * @param pickRay - the ray
	 * @return
	 */
	private List<PickData> pickForRay(Ray pickRay) {
		List<Renderer.PickResult> picks = _renderer.pick(pickRay);

		List<PickData> uniquePicks = new ArrayList<PickData>();

		// IDs that have already been added
		Set<Long> knownIDs = new HashSet<Long>();

		for (Renderer.PickResult pick : picks) {
			if (knownIDs.contains(pick.pickingID)) {
				continue;
			}
			knownIDs.add(pick.pickingID);

			DisplayEntity ent = (DisplayEntity)Entity.idToEntity(pick.pickingID);
			if (ent == null) {
				// This object is not an entity, but may be a picking handle
				uniquePicks.add(new PickData(pick.pickingID));
			} else {
				uniquePicks.add(new PickData(pick.pickingID, ent));
			}
		}

		return uniquePicks;
	}

	/**
	 * Pick on a window at a position other than the current mouse position
	 * @param windowID
	 * @param x
	 * @param y
	 * @return
	 */
	private Ray getRayForMouse(int windowID, int x, int y) {
		Renderer.WindowMouseInfo mouseInfo = _renderer.getMouseInfo(windowID);
		if (mouseInfo == null) {
			return new Ray();
		}

		return RenderUtils.getPickRayForPosition(mouseInfo.cameraInfo, x, y, mouseInfo.width, mouseInfo.height);
	}

	public Vector4d getRenderedStringSize(TessFontKey fontKey, double textHeight, String string) {
		TessFont font = _renderer.getTessFont(fontKey);

		return font.getStringSize(textHeight, string);
	}

	private void logException(Throwable t) {
		_exceptionLogger.logException(t);

		// And print the output
		printExceptionLog();
	}

	private void printExceptionLog() {
		System.out.println("Exceptions from RenderManager: ");

		_exceptionLogger.printExceptionLog();

		System.out.println("");
	}

	public void populateMenu(JPopupMenu menu, ArrayList<ObjectSelector.DEMenuItem> menuItems) {

		for (final ObjectSelector.DEMenuItem item : menuItems) {
			JMenuItem mi = new JMenuItem(item.menuName);
			mi.addActionListener( new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					item.action();
				}
			});
			menu.add(mi);
		}
	}

	public void setSelection(Entity ent) {
		if (ent instanceof DisplayEntity)
			_selectedEntity = (DisplayEntity)ent;
		else
			_selectedEntity = null;

		_isDragging = false;
		queueRedraw();
	}

	/**
	 * This method gives the RenderManager a chance to handle mouse drags before the CameraControl
	 * gets to handle it (note: this may need to be refactored into a proper event handling heirarchy)
	 * @param dragInfo
	 * @return
	 */
	public boolean handleDrag(WindowInteractionListener.DragInfo dragInfo) {

		if (!_isDragging) {
			// We have not cached a drag handle ID, so don't claim this and let CameraControl have control back
			return false;
		}
		// We should have a selected entity
		assert(_selectedEntity != null);

		// Any quick outs go here
		if (!_selectedEntity.isMovable()) {
			return false;
		}

		// We don't drag with control down
		if (dragInfo.controlDown()) {
			return false;
		}

		// Find the start and current world space positions

		Ray currentRay = getRayForMouse(dragInfo.windowID, dragInfo.x, dragInfo.y);
		Ray lastRay = getRayForMouse(dragInfo.windowID,
		                             dragInfo.x - dragInfo.dx,
		                             dragInfo.y - dragInfo.dy);

		Transform trans = _selectedEntity.getGlobalTrans();

		Vector4d size = _selectedEntity.getJaamMathSize();
		Matrix4d transMat = _selectedEntity.getTransMatrix();
		Matrix4d invTransMat = _selectedEntity.getInvTransMatrix();

		Plane entityPlane = new Plane(); // Defaults to XY
		entityPlane.transform(trans, entityPlane); // Transform the plane to world space

		double currentDist = entityPlane.collisionDist(currentRay);
		double lastDist = entityPlane.collisionDist(lastRay);

		if (currentDist < 0 || currentDist == Double.POSITIVE_INFINITY ||
		       lastDist < 0 ||    lastDist == Double.POSITIVE_INFINITY)
		{
			// The plane is parallel or behind one of the rays...
			return true; // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vector4d currentPoint = currentRay.getPointAtDist(currentDist);
		Vector4d lastPoint = lastRay.getPointAtDist(lastDist);

		Vector4d entSpaceCurrent = new Vector4d(); // entSpacePoint is the current point in model space
		invTransMat.mult(currentPoint, entSpaceCurrent);

		Vector4d entSpaceLast = new Vector4d(); // entSpaceLast is the last point in model space
		invTransMat.mult(lastPoint, entSpaceLast);

		Vector4d delta = new Vector4d();
		currentPoint.sub3(lastPoint, delta);

		Vector4d entSpaceDelta = new Vector4d();
		entSpaceCurrent.sub3(entSpaceLast, entSpaceDelta);

		// Handle each handle by type...
		if (_dragHandleID == MOVE_PICK_ID) {
			// We are dragging
			if (dragInfo.shiftDown()) {
				Vector4d entPos = _selectedEntity.getGlobalPosition();

				double zDiff = getZDiff(entPos, currentRay, lastRay);

				entPos.data[2] += zDiff;
				_selectedEntity.setGlobalPosition(entPos);

				return true;
			}

			Vector4d pos = _selectedEntity.getGlobalPosition();
			pos.addLocal3(delta);
			_selectedEntity.setGlobalPosition(pos);
			return true;
		}

		// Handle resize
		if (_dragHandleID <= RESIZE_POSX_PICK_ID &&
		    _dragHandleID >= RESIZE_NXNY_PICK_ID) {

			Vector4d pos = _selectedEntity.getGlobalPosition();
			Vector3d scale = _selectedEntity.getSize();
			Vector4d fixedPoint = new Vector4d();

			if (_dragHandleID == RESIZE_POSX_PICK_ID) {
				//scale.x = 2*entSpaceCurrent.x() * size.x();
				scale.x += entSpaceDelta.x() * size.x();
				fixedPoint = new Vector4d(-0.5,  0.0, 0.0);
			}
			if (_dragHandleID == RESIZE_POSY_PICK_ID) {
				scale.y += entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d( 0.0, -0.5, 0.0);
			}
			if (_dragHandleID == RESIZE_NEGX_PICK_ID) {
				scale.x -= entSpaceDelta.x() * size.x();
				fixedPoint = new Vector4d( 0.5,  0.0, 0.0);
			}
			if (_dragHandleID == RESIZE_NEGY_PICK_ID) {
				scale.y -= entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d( 0.0,  0.5, 0.0);
			}

			if (_dragHandleID == RESIZE_PXPY_PICK_ID) {
				scale.x += entSpaceDelta.x() * size.x();
				scale.y += entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d(-0.5, -0.5, 0.0);
			}
			if (_dragHandleID == RESIZE_PXNY_PICK_ID) {
				scale.x += entSpaceDelta.x() * size.x();
				scale.y -= entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d(-0.5,  0.5, 0.0);
			}
			if (_dragHandleID == RESIZE_NXPY_PICK_ID) {
				scale.x -= entSpaceDelta.x() * size.x();
				scale.y += entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d( 0.5, -0.5, 0.0);
			}
			if (_dragHandleID == RESIZE_NXNY_PICK_ID) {
				scale.x -= entSpaceDelta.x() * size.x();
				scale.y -= entSpaceDelta.y() * size.y();
				fixedPoint = new Vector4d( 0.5,  0.5, 0.0);
			}

			// Handle the case where the scale is pulled through itself. Fix the scale,
			// and swap the currently selected handle
			if (scale.x <= 0.00005) {
				scale.x = 0.0001;
				if (_dragHandleID == RESIZE_POSX_PICK_ID) { _dragHandleID = RESIZE_NEGX_PICK_ID; }
				else if (_dragHandleID == RESIZE_NEGX_PICK_ID) { _dragHandleID = RESIZE_POSX_PICK_ID; }

				else if (_dragHandleID == RESIZE_PXPY_PICK_ID) { _dragHandleID = RESIZE_NXPY_PICK_ID; }
				else if (_dragHandleID == RESIZE_PXNY_PICK_ID) { _dragHandleID = RESIZE_NXNY_PICK_ID; }
				else if (_dragHandleID == RESIZE_NXPY_PICK_ID) { _dragHandleID = RESIZE_PXPY_PICK_ID; }
				else if (_dragHandleID == RESIZE_NXNY_PICK_ID) { _dragHandleID = RESIZE_PXNY_PICK_ID; }
			}

			if (scale.y <= 0.00005) {
				scale.y = 0.0001;
				if (_dragHandleID == RESIZE_POSY_PICK_ID) { _dragHandleID = RESIZE_NEGY_PICK_ID; }
				else if (_dragHandleID == RESIZE_NEGY_PICK_ID) { _dragHandleID = RESIZE_POSY_PICK_ID; }

				else if (_dragHandleID == RESIZE_PXPY_PICK_ID) { _dragHandleID = RESIZE_PXNY_PICK_ID; }
				else if (_dragHandleID == RESIZE_PXNY_PICK_ID) { _dragHandleID = RESIZE_PXPY_PICK_ID; }
				else if (_dragHandleID == RESIZE_NXPY_PICK_ID) { _dragHandleID = RESIZE_NXNY_PICK_ID; }
				else if (_dragHandleID == RESIZE_NXNY_PICK_ID) { _dragHandleID = RESIZE_NXPY_PICK_ID; }
			}

			Vector4d oldFixed = new Vector4d();
			transMat.mult(fixedPoint, oldFixed);
			_selectedEntity.setSize(scale);
			transMat = _selectedEntity.getTransMatrix(); // Get the new matrix
			Vector4d newFixed = new Vector4d();
			transMat.mult(fixedPoint, newFixed);
			Vector4d posAdjust = new Vector4d();
			oldFixed.sub3(newFixed, posAdjust); // posAdjust is how much the fixed point moved

			pos.addLocal3(posAdjust);
			_selectedEntity.setGlobalPosition(pos);

			Vector3d vec = _selectedEntity.getSize();
			InputAgent.processEntity_Keyword_Value(_selectedEntity, "Size", String.format( "%.6f %.6f %.6f %s", vec.x, vec.y, vec.z, "m" ));
			FrameBox.valueUpdate();
			return true;
		}

		if (_dragHandleID == ROTATE_PICK_ID) {

			Vector3d align = _selectedEntity.getAlignment();

			Vector4d rotateCenter = new Vector4d(align.x, align.y, align.z);
			transMat.mult(rotateCenter, rotateCenter);

			Vector4d a = new Vector4d();
			lastPoint.sub3(rotateCenter, a);
			Vector4d b = new Vector4d();
			currentPoint.sub3(rotateCenter, b);

			Vector4d aCrossB = new Vector4d();
			a.cross(b, aCrossB);

			double sinTheta = aCrossB.z() / a.mag3() / b.mag3();
			double theta = Math.asin(sinTheta);

			Vector3d orient = _selectedEntity.getOrientation();
			orient.z += theta;
			InputAgent.processEntity_Keyword_Value(_selectedEntity, "Orientation", String.format("%f %f %f rad", orient.x, orient.y, orient.z));
			FrameBox.valueUpdate();
			return true;
		}
		if (_dragHandleID == LINEDRAG_PICK_ID) {
			// Dragging a line object

			if (dragInfo.shiftDown()) {
				ArrayList<Vector3d> screenPoints = null;
				if (_selectedEntity instanceof HasScreenPoints)
					screenPoints = ((HasScreenPoints)_selectedEntity).getScreenPoints();
				if (screenPoints == null || screenPoints.size() == 0) return true; // just ignore this
				// Find the geometric median of the points
				Vector4d medPoint = RenderUtils.getGeometricMedian(screenPoints);

				double zDiff = getZDiff(medPoint, currentRay, lastRay);
				_selectedEntity.dragged(new Vector3d(0, 0, zDiff));
				return true;
			}

			Region reg = _selectedEntity.getCurrentRegion();
			Transform regionInvTrans = new Transform();
			if (reg != null) {
				regionInvTrans = reg.getRegionTrans(0.0d);
				regionInvTrans.inverse(regionInvTrans);
			}
			Vector4d localDelta = new Vector4d();
			regionInvTrans.apply(delta, localDelta);

			_selectedEntity.dragged(new Vector3d(localDelta.x(), localDelta.y(), localDelta.z()));
			return true;
		}

		if (_dragHandleID <= LINENODE_PICK_ID) {
			int nodeIndex = (int)(-1*(_dragHandleID - LINENODE_PICK_ID));
			ArrayList<Vector3d> screenPoints = null;
			if (_selectedEntity instanceof HasScreenPoints)
				screenPoints = ((HasScreenPoints)_selectedEntity).getScreenPoints();

			// Note: screenPoints is not a defensive copy, but we'll put it back into itself
			// in a second so everything should be safe
			if (screenPoints == null || nodeIndex >= screenPoints.size()) {
				// huh?
				return false;
			}
			Vector3d point = screenPoints.get(nodeIndex);

			if (dragInfo.shiftDown()) {
				double zDiff = getZDiff(new Vector4d(point.x, point.y, point.z), currentRay, lastRay);
				point.z += zDiff;
			} else {
				Plane pointPlane = new Plane(Vector4d.Z_AXIS, point.z);
				Vector4d diff = RenderUtils.getPlaneCollisionDiff(pointPlane, currentRay, lastRay);
				point.x += diff.x();
				point.y += diff.y();
				point.z += 0;
			}

			Input<?> pointsInput = _selectedEntity.getInput("Points");
			assert(pointsInput != null);
			if (pointsInput == null) {
				_selectedEntity.setGraphicsDataDirty();
				return true;
			}

			StringBuilder sb = new StringBuilder();
			String pointFormatter = " { %.3f %.3f %.3f m }";
			if (pointsInput.getUnits() == "")
				pointFormatter = " { %.3f %.3f %.3f }";

			for(Vector3d pt : screenPoints) {
				sb.append(String.format(pointFormatter, pt.x, pt.y, pt.z));
			}

			InputAgent.processEntity_Keyword_Value(_selectedEntity, pointsInput, sb.toString());
			FrameBox.valueUpdate();
			_selectedEntity.setGraphicsDataDirty();
			return true;
		}

		return false;
	}

	/**
	 * Get the difference in Z height from projecting the two rays onto the vertical plane
	 * defined at the provided centerPoint
	 * @param centerPoint
	 * @param currentRay
	 * @param lastRay
	 * @return
	 */
	private double getZDiff(Vector4d centerPoint, Ray currentRay, Ray lastRay) {

		// Create a plane, orthogonal to the camera, but parallel to the Z axis
		Vector4d normal = new Vector4d(currentRay.getDirRef());
		normal.data[2] = 0; // 0 the z component
		normal.normalizeLocal3();

		double planeDist = centerPoint.dot3(normal);

		Plane plane = new Plane(normal, planeDist);

		double currentDist = plane.collisionDist(currentRay);
		double lastDist = plane.collisionDist(lastRay);

		if (currentDist < 0 || currentDist == Double.POSITIVE_INFINITY ||
		       lastDist < 0 ||    lastDist == Double.POSITIVE_INFINITY)
		{
			// The plane is parallel or behind one of the rays...
			return 0; // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vector4d currentPoint = currentRay.getPointAtDist(currentDist);
		Vector4d lastPoint = lastRay.getPointAtDist(lastDist);

		return currentPoint.z() - lastPoint.z();
	}

	private void splitLineEntity(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);

		Matrix4d rayMatrix = Matrix4d.RaySpace(currentRay);

		HasScreenPoints hsp = (HasScreenPoints)_selectedEntity;
		assert(hsp != null);

		ArrayList<Vector3d> points = hsp.getScreenPoints();

		int splitInd = 0;
		Vector4d nearPoint = null;
		// Find a line segment we are near
		for (;splitInd < points.size() - 1; ++splitInd) {
			Vector4d a = new Vector4d(points.get(splitInd  ).x, points.get(splitInd  ).y, points.get(splitInd  ).z);
			Vector4d b = new Vector4d(points.get(splitInd+1).x, points.get(splitInd+1).y, points.get(splitInd+1).z);

			nearPoint = RenderUtils.rayClosePoint(rayMatrix, a, b);

			double rayAngle = RenderUtils.angleToRay(rayMatrix, nearPoint);

			if (rayAngle > 0 && rayAngle < 0.01309) { // 0.75 degrees in radians
				break;
			}
		}

		if (splitInd == points.size() - 1) {
			// No appropriate point was found
			return;
		}

		// If we are here, we have a segment to split, at index i

		StringBuilder sb = new StringBuilder();
		String pointFormatter = " { %.3f %.3f %.3f m }";

		for(int i = 0; i <= splitInd; ++i) {
			Vector3d pt = points.get(i);
			sb.append(String.format(pointFormatter, pt.x, pt.y, pt.z));
		}

		sb.append(String.format(pointFormatter, nearPoint.x(), nearPoint.y(), nearPoint.z()));

		for (int i = splitInd+1; i < points.size(); ++i) {
			Vector3d pt = points.get(i);
			sb.append(String.format(pointFormatter, pt.x, pt.y, pt.z));
		}

		Input<?> pointsInput = _selectedEntity.getInput("Points");
		InputAgent.processEntity_Keyword_Value(_selectedEntity, pointsInput, sb.toString());
		FrameBox.valueUpdate();
		_selectedEntity.setGraphicsDataDirty();
	}

	private void removeLineNode(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);

		Matrix4d rayMatrix = Matrix4d.RaySpace(currentRay);

		HasScreenPoints hsp = (HasScreenPoints)_selectedEntity;
		assert(hsp != null);

		ArrayList<Vector3d> points = hsp.getScreenPoints();
		// Find a point that is within the threshold

		if (points.size() <= 2) {
			return;
		}

		int removeInd = 0;
		// Find a line segment we are near
		for ( ;removeInd < points.size(); ++removeInd) {
			Vector4d p = new Vector4d(points.get(removeInd).x, points.get(removeInd).y, points.get(removeInd).z);

			double rayAngle = RenderUtils.angleToRay(rayMatrix, p);

			if (rayAngle > 0 && rayAngle < 0.01309) { // 0.75 degrees in radians
				break;
			}

			if (removeInd == points.size()) {
				// No appropriate point was found
				return;
			}
		}

		StringBuilder sb = new StringBuilder();
		String pointFormatter = " { %.3f %.3f %.3f m }";

		for(int i = 0; i < points.size(); ++i) {
			if (i == removeInd) {
				continue;
			}

			Vector3d pt = points.get(i);
			sb.append(String.format(pointFormatter, pt.x, pt.y, pt.z));
		}

		Input<?> pointsInput = _selectedEntity.getInput("Points");
		InputAgent.processEntity_Keyword_Value(_selectedEntity, pointsInput, sb.toString());
		FrameBox.valueUpdate();
		_selectedEntity.setGraphicsDataDirty();
	}



	private boolean isMouseHandleID(long id) {
		return (id < 0); // For now all negative IDs are mouse handles, this may change
	}

	public boolean handleMouseButton(int windowID, int x, int y, int button, boolean isDown, int modifiers) {

		if (button != 1) { return false; }
		if (!isDown) {
			// Click released
			_isDragging = false;
			return true; // handled
		}

		if ((modifiers & WindowInteractionListener.MOD_CTRL) != 0) {
			// Check if we can split a line segment
			_isDragging = false;
			if (_selectedEntity != null && _selectedEntity instanceof HasScreenPoints) {
				if ((modifiers & WindowInteractionListener.MOD_SHIFT) != 0) {
					removeLineNode(windowID, x, y);
				} else {
					splitLineEntity(windowID, x, y);
				}
				return true;
			}
			return false;
		}

		Ray pickRay = getRayForMouse(windowID, x, y);

		List<PickData> picks = pickForRay(pickRay);

		Collections.sort(picks, new HandleSorter());

		if (picks.size() == 0) {
			return false;
		}

		// See if we are hovering over any interaction handles
		for (PickData pd : picks) {
			if (isMouseHandleID(pd.id)) {
				// this is a mouse handle, remember the handle for future drag events
				_isDragging = true;
				_dragHandleID = pd.id;
				return true;
			}
		}
		return false;
	}

	public void clearSelection() {
		_selectedEntity = null;
		_isDragging = false;

	}

	public void hideExistingPopups() {
		synchronized (_popupLock) {
			if (_lastPopup == null) {
				return;
			}

			_lastPopup.setVisible(false);
			_lastPopup = null;
		}
	}

	public boolean isDragAndDropping() {
		// This is such a brutal hack to work around newt's lack of drag and drop support
		// Claim we are still dragging for up to 10ms after the last drop failed...
		long currTime = System.nanoTime();
		return _dndObjectType != null &&
		       ((currTime - _dndDropTime) < 100000000); // Did the last 'drop' happen less than 100 ms ago?
	}

	public void startDragAndDrop(ObjectType ot) {
		_dndObjectType = ot;
	}

	public void mouseMoved(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = Plane.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// I dunno...
			return;
		}

		Vector4d xyPlanePoint = currentRay.getPointAtDist(dist);

		GUIFrame.instance().showLocatorPosition(new Vector3d(xyPlanePoint.x(), xyPlanePoint.y(), xyPlanePoint.z()), null);
		queueRedraw();
	}


	public void createDNDObject(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = Plane.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// Unfortunate...
			return;
		}

		Vector4d creationPoint = currentRay.getPointAtDist(dist);

		// Create a new instance
		Class<? extends Entity> proto  = _dndObjectType.getJavaClass();
		String name = proto.getSimpleName();
		Entity ent = InputAgent.defineEntityWithUniqueName(proto, name, true);

		// We are no longer drag-and-dropping
		_dndObjectType = null;
		FrameBox.setSelectedEntity(ent);

		if (!(ent instanceof DisplayEntity)) {
			// This object is not a display entity, so the rest of this method does not apply
			return;
		}

		View view = _windowToViewMap.get(windowID);
		Input<?> visibleViewInput = ent.getInput("VisibleViews");
		if (visibleViewInput != null && view != null) {
			InputAgent.processEntity_Keyword_Value(ent, visibleViewInput, view.getName());
		}

		DisplayEntity dEntity  = (DisplayEntity) ent;

		try {
			Vector3d v = new Vector3d(creationPoint.data);
			dEntity.dragged(v);
		}
		catch (InputErrorException e) {}

		boolean isFlat = false;

		ArrayList<DisplayModel> displayModels = dEntity.getDisplayModelList().getValue();
		if (displayModels != null && displayModels.size() > 0) {
			DisplayModelState dms = new DisplayModelState(displayModels.get(0));
			isFlat = dms.isFlatModel();
		}
		if (dEntity instanceof HasScreenPoints) {
			isFlat = true;
		}
		if (dEntity instanceof Graph) {
			isFlat = true;
		}

		if (isFlat) {
			Vector3d size = (Vector3d)dEntity.getInput("Size").getValue();
			String sizeString = String.format("%.3f %.3f 0.0 m", size.x, size.y);
			InputAgent.processEntity_Keyword_Value(dEntity, "Size", sizeString);
		} else {
			InputAgent.processEntity_Keyword_Value(dEntity, "Alignment", "0.0 0.0 -0.5");
		}
		FrameBox.valueUpdate();
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent arg0) {
		// Clear the dragging flag
		_dndDropTime = System.nanoTime();
	}

	@Override
	public void dragEnter(DragSourceDragEvent arg0) {}

	@Override
	public void dragExit(DragSourceEvent arg0) {}

	@Override
	public void dragOver(DragSourceDragEvent arg0) {}

	@Override
	public void dropActionChanged(DragSourceDragEvent arg0) {}

	public Vector4d getMeshSize(String shapeString) {
		//TODO: work on meshes that have not been preloaded
		MeshProtoKey key = DisplayModelObserver.getCachedMeshKey(shapeString);
		if (key == null) {
			// Not loaded or bad mesh
			return new Vector4d(1, 1, 1);
		}
		AABB bounds = getMeshBounds(key, true);

		return bounds.getRadius();
	}

	public AABB getMeshBounds(MeshProtoKey key, boolean block) {
		AABB cachedBounds = _renderer.getProtoBounds(key);
		if (cachedBounds == null) {
			// This has not been loaded yet, queue the renderer to load the asset
			_renderer.loadAsset(key);
			if (!block) {
				return null;
			}

			// Block here until the
			Object notifier = _renderer.getProtoBoundsLock();
			while (cachedBounds == null) {
				synchronized(notifier) {
					try {
						notifier.wait();
					} catch (InterruptedException e) {

					}
				}
				cachedBounds = _renderer.getProtoBounds(key);
			}
		}
		return cachedBounds;
	}

	/**
	 * Set the current windows camera to an isometric view
	 */
	public void setIsometricView() {
		CameraControl control = _windowControls.get(_activeWindowID);
		if (control == null) return;

		// The constant is acos(1/sqrt(3))
		control.setRotationAngles(0.955316, Math.PI/4);
	}

	/**
	 * Set the current windows camera to an XY plane view
	 */
	public void setXYPlaneView() {
		CameraControl control = _windowControls.get(_activeWindowID);
		if (control == null) return;

		control.setRotationAngles(0.0, 0.0);
	}

	public ArrayList<Integer> getOpenWindowIDs() {
		return _renderer.getOpenWindowIDs();
	}

	public String getWindowName(int windowID) {
		return _renderer.getWindowName(windowID);
	}

	public void focusWindow(int windowID) {
		_renderer.focusWindow(windowID);
	}

	/**
	 * Queue up an off screen rendering, this simply passes the call directly to the renderer
	 * @param scene
	 * @param camInfo
	 * @param width
	 * @param height
	 * @return
	 */
	public Future<BufferedImage> renderOffscreen(ArrayList<RenderProxy> scene, CameraInfo camInfo, int viewID,
	                                   int width, int height, Runnable runWhenDone) {
		return _renderer.renderOffscreen(scene, viewID, camInfo, width, height, runWhenDone, null);
	}

	/**
	 * Return a FutureImage of the equivalent screen renderer from the given position looking at the given center
	 * @param cameraPos
	 * @param viewCenter
	 * @param width - width of returned image
	 * @param height - height of returned image
	 * @param target - optional target to prevent re-allocating GPU resources
	 * @return
	 */
	public Future<BufferedImage> renderScreenShot(Vector4d cameraPos, int viewID, Vector4d viewCenter,
	                                    int width, int height, OffscreenTarget target) {

		Vector4d viewDiff = new Vector4d();
		cameraPos.sub3(viewCenter, viewDiff);

		double rotZ = Math.atan2(viewDiff.x(), -viewDiff.y());

		double xyDist = Math.hypot(viewDiff.x(), viewDiff.y());

		double rotX = Math.atan2(xyDist, viewDiff.z());

		if (Math.abs(rotX) < 0.005) {
			rotZ = 0; // Don't rotate if we are looking straight up or down
		}

		double viewDist = viewDiff.mag3();

		Quaternion rot = Quaternion.Rotation(rotZ, Vector4d.Z_AXIS);
		rot.mult(Quaternion.Rotation(rotX, Vector4d.X_AXIS), rot);

		Transform trans = new Transform(cameraPos, rot, 1);

		CameraInfo camInfo = new CameraInfo(Math.PI/3, viewDist*0.1, viewDist*10, trans);

		return _renderer.renderOffscreen(_cachedScene, viewID, camInfo, width, height, null, target);
	}

	public Future<BufferedImage> getPreviewForDisplayModel(DisplayModelState dms, Runnable notifier) {
		return _previewCache.getPreview(dms, notifier);
	}

	public OffscreenTarget createOffscreenTarget(int width, int height) {
		return _renderer.createOffscreenTarget(width, height);
	}

	public void freeOffscreenTarget(OffscreenTarget target) {
		_renderer.freeOffscreenTarget(target);
	}

	public void resetRecorder(ArrayList<View> views, String filePrefix, boolean saveImages, boolean saveVideo) {
		if (_recorder != null) {
			_recorder.freeResources();
		}

		_recorder = new VideoRecorder(views, filePrefix, 1500, 1000, saveImages, saveVideo);
	}

	public void endRecording() {
		_recorder.freeResources();
		_recorder = null;
	}

	private void takeScreenShot() {

		if (_recorder != null)
			_recorder.sample();

		synchronized(_screenshot) {
			_screenshot.set(false);
			_screenshot.notifyAll();
		}
	}

	public void blockOnScreenShot() {
		assert(!_screenshot.get());

		synchronized (_screenshot) {
			_screenshot.set(true);
			queueRedraw();
			while (_screenshot.get()) {
				try {
					_screenshot.wait();
				} catch (InterruptedException ex) {}
			}
		}
	}

	public void shutdown() {
		_finished.set(true);
		if (_renderer != null) {
			_renderer.shutdown();
		}
	}
}

