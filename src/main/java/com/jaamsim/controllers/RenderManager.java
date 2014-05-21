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

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.Image;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.DisplayModels.ImageModel;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.font.TessFont;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Quaternion;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Action;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.Future;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.render.MeshDataCache;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.OffscreenTarget;
import com.jaamsim.render.PreviewCache;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.render.WindowInteractionListener;
import com.jaamsim.render.util.ExceptionLogger;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.LogBox;
import com.jaamsim.ui.ObjectSelector;
import com.jaamsim.ui.View;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.ObjectType;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.DisplayModelCompat;
import com.sandwell.JavaSimulation3D.GUIFrame;
import com.sandwell.JavaSimulation3D.Graph;
import com.sandwell.JavaSimulation3D.Region;

/**
 * Top level owner of the JaamSim renderer. This class both owns and drives the Renderer object, but is also
 * responsible for gathering rendering data every frame.
 * @author Matt Chudleigh
 *
 */
public class RenderManager implements DragSourceListener {

	private final static int EXCEPTION_STACK_THRESHOLD = 10; // The number of recoverable exceptions until a stack trace is output
	private final static int EXCEPTION_PRINT_RATE = 30; // The number of total exceptions until the overall log is printed

	private int numberOfExceptions = 0;

	private static RenderManager s_instance = null;
	/**
	 * Basic singleton pattern
	 */
	public static void initialize(boolean safeGraphics) {
		s_instance = new RenderManager(safeGraphics);
	}

	public static RenderManager inst() { return s_instance; }

	private final Thread managerThread;
	private final Renderer renderer;
	private final AtomicBoolean finished = new AtomicBoolean(false);
	private final AtomicBoolean fatalError = new AtomicBoolean(false);
	private final AtomicBoolean redraw = new AtomicBoolean(false);

	private final AtomicBoolean screenshot = new AtomicBoolean(false);

	// These values are used to limit redraw rate, the stored values are time in milliseconds
	// returned by System.currentTimeMillis()
	private long lastDraw = 0;
	private long scheduledDraw = 0;
	private final Object timingLock = new Object();

	private final ExceptionLogger exceptionLogger;

	private final static double FPS = 60;
	private final Timer timer;

	private final HashMap<Integer, CameraControl> windowControls = new HashMap<Integer, CameraControl>();
	private final HashMap<Integer, View> windowToViewMap= new HashMap<Integer, View>();
	private int activeWindowID = -1;

	private final Object popupLock;
	private JPopupMenu lastPopup;

	/**
	 * The last scene rendered
	 */
	private ArrayList<RenderProxy> cachedScene;

	private DisplayEntity selectedEntity = null;

	private long simTick = 0;

	private long dragHandleID = 0;
	private Vec3d dragCollisionPoint;

	// The object type for drag-and-drop operation, if this is null, the user is not dragging
	private ObjectType dndObjectType;
	private long dndDropTime = 0;

	// The video recorder to sample
	private VideoRecorder recorder;

	private PreviewCache previewCache = new PreviewCache();

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

	private RenderManager(boolean safeGraphics) {
		renderer = new Renderer(safeGraphics);

		exceptionLogger = new ExceptionLogger(EXCEPTION_STACK_THRESHOLD);

		managerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				renderManagerLoop();
			}
		}, "RenderManagerThread");
		managerThread.start();

		// Start the display timer
		timer = new Timer("RedrawThread");
		TimerTask displayTask = new TimerTask() {
			@Override
			public void run() {

				synchronized(timingLock) {
					// Is a redraw scheduled
					long currentTime = System.currentTimeMillis();

					// Only draw if the scheduled time is before now and after the last redraw
					// but never skip a draw if a screen shot is requested
					if (!screenshot.get() && (scheduledDraw < lastDraw || currentTime < scheduledDraw)) {
						return;
					}

					lastDraw = currentTime;

					synchronized(redraw) {
						if (windowControls.size() == 0 && !screenshot.get()) {
							return; // Do not queue a redraw if there are no open windows
						}
						redraw.set(true);
						redraw.notifyAll();
					}
				}
			}
		};

		timer.scheduleAtFixedRate(displayTask, 0, (long) (1000 / (FPS*2)));

		popupLock = new Object();
	}

	public static final void updateTime(long simTick) {
		if (!RenderManager.isGood())
			return;

		RenderManager.inst().simTick = simTick;
		RenderManager.inst().queueRedraw();
	}

	public static final void redraw() {
		if (!isGood()) return;

		inst().queueRedraw();
	}

	private void queueRedraw() {
		synchronized(timingLock) {
			long currentTime = System.currentTimeMillis();

			if (scheduledDraw > lastDraw) {
				// A draw is scheduled
				return;
			}

			long newDraw = currentTime;
			long frameTime = (long)(1000.0/FPS);
			if (newDraw < lastDraw + frameTime) {
				// This would be scheduled too soon
				newDraw = lastDraw + frameTime;
			}
			scheduledDraw = newDraw;
		}
	}

	public void createWindow(View view) {

		// First see if this window has already been opened
		for (Map.Entry<Integer, CameraControl> entry : windowControls.entrySet()) {
			if (entry.getValue().getView() == view) {
				// This view has a window, just reshow that one
				focusWindow(entry.getKey());
				return;
			}
		}

		IntegerVector windSize = view.getWindowSize();
		IntegerVector windPos = view.getWindowPos();

		Image icon = GUIFrame.getWindowIcon();

		CameraControl control = new CameraControl(renderer, view);

		int windowID = renderer.createWindow(windPos.get(0), windPos.get(1),
		                                      windSize.get(0), windSize.get(1),
		                                      view.getID(),
		                                      view.getTitle(), view.getInputName(),
		                                      icon, control);

		control.setWindowID(windowID);
		windowControls.put(windowID, control);
		windowToViewMap.put(windowID, view);

		queueRedraw();
	}

	public static final void clear() {
		if (!isGood()) return;

		RenderManager.inst().closeAllWindows();
	}

	private void closeAllWindows() {
		ArrayList<Integer> windIDs = renderer.getOpenWindowIDs();
		for (int id : windIDs) {
			renderer.closeWindow(id);
		}
	}

	public void windowClosed(int windowID) {

		// Update the state of the window in the input file
		View v = windowToViewMap.get(windowID);
		if (!v.getKeepWindowOpen())
			InputAgent.processEntity_Keyword_Value(v, "ShowWindow", "FALSE");
		v.setKeepWindowOpen(false);

		windowControls.remove(windowID);
		windowToViewMap.remove(windowID);
	}

	public void setActiveWindow(int windowID) {
		activeWindowID = windowID;
	}

	public static boolean isGood() {
		return (s_instance != null && !s_instance.finished.get() && !s_instance.fatalError.get());
	}

	/**
	 * Ideally, this states that it is safe to call initialize() (assuming isGood() returned false)
	 * @return
	 */
	public static boolean canInitialize() {
		return s_instance == null;
	}

	private void renderManagerLoop() {

		while (!finished.get() && !fatalError.get()) {
			try {

				if (renderer.hasFatalError()) {
					// Well, something went horribly wrong
					fatalError.set(true);
					LogBox.formatRenderLog("Renderer failed with error: %s\n", renderer.getErrorString());

					LogBox.getInstance().setVisible(true);

					// Do some basic cleanup
					windowControls.clear();
					previewCache.clear();

					timer.cancel();

					break;
				}

				if (!renderer.isInitialized()) {
					// Give the renderer a chance to initialize
					try {
						Thread.sleep(100);
					} catch(InterruptedException e) {}
					continue;
				}

				double renderTime = FrameBox.ticksToSeconds(simTick);
				redraw.set(false);

				for (int i = 0; i < View.getAll().size(); i++) {
					View v = View.getAll().get(i);
					v.update(renderTime);
				}

				for (CameraControl cc : windowControls.values()) {
					cc.checkForUpdate();
				}

				cachedScene = new ArrayList<RenderProxy>();
				DisplayModelBinding.clearCacheCounters();
				DisplayModelBinding.clearCacheMissData();

				boolean screenShotThisFrame = screenshot.get();

				long startNanos = System.nanoTime();

				ArrayList<DisplayModelBinding> selectedBindings = new ArrayList<DisplayModelBinding>();

				// Update all graphical entities in the simulation
				for (int i = 0; i < DisplayEntity.getAll().size(); i++) {
					DisplayEntity de;
					try {
						de = DisplayEntity.getAll().get(i);
					}
					catch (IndexOutOfBoundsException e) {
						break;
					}

					try {
						de.updateGraphics(renderTime);
					}
					// Catch everything so we don't screw up the behavior handling
					catch (Throwable e) {
						logException(e);
					}
				}

				long updateNanos = System.nanoTime();

				int totalBindings = 0;
				for (int i = 0; i < DisplayEntity.getAll().size(); i++) {
					DisplayEntity de;
					try {
						de = DisplayEntity.getAll().get(i);
					} catch (IndexOutOfBoundsException ex) {
						// This is probably the end of the list, so just move on
						break;
					}

					for (DisplayModelBinding binding : de.getDisplayBindings()) {
						try {
							totalBindings++;
							binding.collectProxies(renderTime, cachedScene);
							if (binding.isBoundTo(selectedEntity)) {
								selectedBindings.add(binding);
							}
						} catch (Throwable t) {
							// Log the exception in the exception list
							logException(t);
						}
					}
				}

				// Collect selection proxies second so they always appear on top
				for (DisplayModelBinding binding : selectedBindings) {
					try {
						binding.collectSelectionProxies(renderTime, cachedScene);
					} catch (Throwable t) {
						// Log the exception in the exception list
						logException(t);
					}
				}

				long endNanos = System.nanoTime();

				renderer.setScene(cachedScene);

				String cacheString = " Hits: " + DisplayModelBinding.getCacheHits() + " Misses: " + DisplayModelBinding.getCacheMisses() +
				                     " Total: " + totalBindings;

				double gatherMS = (endNanos - updateNanos) / 1000000.0;
				double updateMS = (updateNanos - startNanos) / 1000000.0;

				String timeString = "Gather time (ms): " + gatherMS + " Update time (ms): " + updateMS;

				// Do some picking debug
				ArrayList<Integer> windowIDs = renderer.getOpenWindowIDs();
				for (int id : windowIDs) {
					Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(id);

					if (mouseInfo == null || !mouseInfo.mouseInWindow) {
						// Not currently picking for this window
						renderer.setWindowDebugInfo(id, cacheString + " Not picking. " + timeString, new ArrayList<Long>());
						continue;
					}

					List<PickData> picks = pickForMouse(id, false);
					ArrayList<Long> debugIDs = new ArrayList<Long>(picks.size());

					StringBuilder dbgMsg = new StringBuilder(cacheString);
					dbgMsg.append(" Picked ").append(picks.size());
					dbgMsg.append(" entities at (").append(mouseInfo.x);
					dbgMsg.append(", ").append(mouseInfo.y).append("): ");
					for (PickData pd : picks) {
						Entity ent = Entity.idToEntity(pd.id);
						if (ent != null)
							dbgMsg.append(ent.getInputName());

						dbgMsg.append(", ");
						debugIDs.add(pd.id);
					}
					dbgMsg.append(timeString);

					renderer.setWindowDebugInfo(id, dbgMsg.toString(), debugIDs);
				}

				if (GUIFrame.getShuttingDownFlag()) {
					shutdown();
				}

				renderer.queueRedraw();

				if (screenShotThisFrame) {
					takeScreenShot();
				}

			} catch (Throwable t) {
				// Make a note of it, but try to keep going
				logException(t);
			}

			// Wait for a redraw request
			synchronized(redraw) {
				while (!redraw.get()) {
					try {
						redraw.wait();
					} catch (InterruptedException e) {}
				}
			}

		}

		exceptionLogger.printExceptionLog();

	}

	public void popupMenu(final int windowID) {
		try {
			// Transfer control from the NEWT-EDT to the AWT-EDT
			EventQueue.invokeAndWait(new Runnable() {
				@Override
				public void run() {
					popupMenuImp(windowID);
				}
			});
		} catch (InvocationTargetException ex) {
			assert(false);
		} catch (InterruptedException ex) {
			assert(false);
		}
	}

	// Temporary dumping ground until I find a better place for this
	// Note: this is intentionally package private to be called by an inner class
	void popupMenuImp(int windowID) {
		synchronized (popupLock) {

			Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(windowID);
			if (mouseInfo == null) {
				// Somehow this window was closed along the way, just ignore this click
				return;
			}

			final Frame awtFrame = renderer.getAWTFrame(windowID);
			if (awtFrame == null) {
				return;
			}

			List<PickData> picks = pickForMouse(windowID, false);

			ArrayList<DisplayEntity> ents = new ArrayList<DisplayEntity>();

			for (PickData pd : picks) {
				if (!pd.isEntity) { continue; }
				Entity ent = Entity.idToEntity(pd.id);
				if (ent == null) { continue; }
				if (!(ent instanceof DisplayEntity)) { continue; }

				DisplayEntity de = (DisplayEntity)ent;
				if (!de.isMovable()) { continue; }  // only a movable DisplayEntity responds to a right-click

				ents.add(de);
			}

			if (!mouseInfo.mouseInWindow) {
				// Somehow this window does not currently have the mouse over it.... ignore?
				return;
			}

			final JPopupMenu menu = new JPopupMenu();
			lastPopup = menu;

			menu.setLightWeightPopupEnabled(false);
			final int menuX = mouseInfo.x + awtFrame.getInsets().left;
			final int menuY = mouseInfo.y + awtFrame.getInsets().top;

			if (ents.size() == 0) { return; } // Nothing to show

			if (ents.size() == 1) {
				ObjectSelector.populateMenu(menu, ents.get(0), menuX, menuY);
			}
			else {
				// Several entities, let the user pick the interesting entity first
				for (final DisplayEntity de : ents) {
					JMenuItem thisItem = new JMenuItem(de.getInputName());
					thisItem.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							menu.removeAll();
							ObjectSelector.populateMenu(menu, de, menuX, menuY);
							menu.show(awtFrame, menuX, menuY);
						}
					} );

					menu.add( thisItem );
				}
			}

			menu.show(awtFrame, menuX, menuY);
			menu.repaint();
		} // synchronized (_popupLock)
	}

	public void handleSelection(int windowID) {

		List<PickData> picks = pickForMouse(windowID, false);

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

		// If no entity is found, set the selected entity to the view window
		FrameBox.setSelectedEntity(windowToViewMap.get(windowID));
		queueRedraw();
	}

	/**
	 * Utility, convert a window and mouse coordinate into a list of picking IDs for that pixel
	 * @param windowID
	 * @param mouseX
	 * @param mouseY
	 * @return
	 */
	private List<PickData> pickForMouse(int windowID, boolean precise) {
		Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(windowID);

		View view = windowToViewMap.get(windowID);
		if (mouseInfo == null || view == null || !mouseInfo.mouseInWindow) {
			// The mouse is not actually in the window, or the window was closed along the way
			return new ArrayList<PickData>(); // empty set
		}

		Ray pickRay = RenderUtils.getPickRay(mouseInfo);

		return pickForRay(pickRay, view.getID(), precise);
	}


	/**
	 * PickData represents enough information to sort a list of picks based on a picking preference
	 * metric. For now it holds the object size and distance from pick point to object center
	 *
	 */
	private static class PickData {
		public long id;
		public double size;
		public double dist;
		boolean isEntity;

		/**
		 * This pick does not correspond to an entity, and is a handle or other UI element
		 * @param id
		 */
		public PickData(long id, double d) {
			this.id = id;
			size = 0;
			dist = d;
			isEntity = false;
		}
		/**
		 * This pick was an entity
		 * @param id - the id
		 * @param ent - the entity
		 */
		public PickData(long id, double d, DisplayEntity ent) {
			this.id = id;
			size = ent.getSize().mag3();
			dist = d;

			isEntity = true;
		}
	}

	/**
	 * This Comparator sorts based on entity selection preference
	 */
	private static class SelectionSorter implements Comparator<PickData> {

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
	private static class HandleSorter implements Comparator<PickData> {

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

	public Vec3d getNearestPick(int windowID) {
		Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(windowID);

		View view = windowToViewMap.get(windowID);
		if (mouseInfo == null || view == null || !mouseInfo.mouseInWindow) {
			// The mouse is not actually in the window, or the window was closed along the way
			return null;
		}

		Ray pickRay = RenderUtils.getPickRay(mouseInfo);

		List<Renderer.PickResult> picks = renderer.pick(pickRay, view.getID(), true);

		if (picks.size() == 0) {
			return null;
		}

		double pickDist = Double.POSITIVE_INFINITY;

		for (Renderer.PickResult pick : picks) {
			if (pick.dist < pickDist && pick.pickingID >= 0) {
				// Negative pickingIDs are reserved for interaction handles and are therefore not
				// part of the content
				pickDist = pick.dist;
			}
		}
		if (pickDist == Double.POSITIVE_INFINITY) {
			return null;
		}
		return pickRay.getPointAtDist(pickDist);
	}

	/**
	 * Perform a pick from this world space ray
	 * @param pickRay - the ray
	 * @return
	 */
	private List<PickData> pickForRay(Ray pickRay, int viewID, boolean precise) {
		List<Renderer.PickResult> picks = renderer.pick(pickRay, viewID, precise);

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
				uniquePicks.add(new PickData(pick.pickingID, pick.dist));
			} else {
				uniquePicks.add(new PickData(pick.pickingID, pick.dist, ent));
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
		Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(windowID);
		if (mouseInfo == null) {
			return new Ray();
		}

		return RenderUtils.getPickRayForPosition(mouseInfo.cameraInfo, x, y, mouseInfo.width, mouseInfo.height);
	}

	public Vec3d getRenderedStringSize(TessFontKey fontKey, double textHeight, String string) {
		TessFont font = renderer.getTessFont(fontKey);

		return font.getStringSize(textHeight, string);
	}

	private void logException(Throwable t) {
		exceptionLogger.logException(t);

		numberOfExceptions++;

		// Only print the exception log periodically (this can get a bit spammy)
		if (numberOfExceptions % EXCEPTION_PRINT_RATE == 0) {
			LogBox.renderLog("Recoverable Exceptions from RenderManager: ");
			exceptionLogger.printExceptionLog();
			LogBox.renderLog("");
		}
	}

	public static void setSelection(Entity ent) {
		if (!RenderManager.isGood())
			return;

		RenderManager.inst().setSelectEntity(ent);
	}

	private void setSelectEntity(Entity ent) {
		if (ent instanceof DisplayEntity)
			selectedEntity = (DisplayEntity)ent;
		else
			selectedEntity = null;

		queueRedraw();
	}

	/**
	 * This method gives the RenderManager a chance to handle mouse drags before the CameraControl
	 * gets to handle it (note: this may need to be refactored into a proper event handling heirarchy)
	 * @param dragInfo
	 * @return
	 */
	public boolean handleDrag(WindowInteractionListener.DragInfo dragInfo) {

		// Any quick outs go here
		if (!dragInfo.controlDown()) {
			return false;
		}

		if (dragHandleID == 0) {
			return true;
		}

		DisplayEntity dispEnt = selectedEntity;
		if (dispEnt == null || !dispEnt.isMovable()) {
			return true;
		}

		// Find the start and current world space positions

		Ray currentRay = getRayForMouse(dragInfo.windowID, dragInfo.x, dragInfo.y);
		Ray lastRay = getRayForMouse(dragInfo.windowID,
		                             dragInfo.x - dragInfo.dx,
		                             dragInfo.y - dragInfo.dy);

		double simTime = FrameBox.ticksToSeconds(simTick);
		Transform trans = dispEnt.getGlobalTrans(simTime);

		Vec3d size = dispEnt.getSize();
		Mat4d transMat = dispEnt.getTransMatrix(simTime);
		Mat4d invTransMat = dispEnt.getInvTransMatrix(simTime);

		Plane entityPlane = new Plane(); // Defaults to XY
		entityPlane.transform(trans, entityPlane, new Vec3d()); // Transform the plane to world space

		double currentDist = entityPlane.collisionDist(currentRay);
		double lastDist = entityPlane.collisionDist(lastRay);

		if (dragHandleID != MOVE_PICK_ID &&
		    (currentDist < 0 || currentDist == Double.POSITIVE_INFINITY ||
		        lastDist < 0 ||    lastDist == Double.POSITIVE_INFINITY))
		{
			// The plane is parallel or behind one of the rays...
			// Moving uses a different plane, so we'll test that below
			return true; // Just ignore it for now...
		}

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vec3d currentPoint = currentRay.getPointAtDist(currentDist);
		Vec3d lastPoint = lastRay.getPointAtDist(lastDist);

		Vec3d entSpaceCurrent = new Vec3d(); // entSpacePoint is the current point in model space
		entSpaceCurrent.multAndTrans3(invTransMat, currentPoint);

		Vec3d entSpaceLast = new Vec3d(); // entSpaceLast is the last point in model space
		entSpaceLast.multAndTrans3(invTransMat, lastPoint);

		Vec3d delta = new Vec3d();
		delta.sub3(currentPoint, lastPoint);

		Vec3d entSpaceDelta = new Vec3d();
		entSpaceDelta.sub3(entSpaceCurrent, entSpaceLast);

		// Handle each handle by type...
		if (dragHandleID == MOVE_PICK_ID) {
			// We are dragging

			// Dragging may not happen in the entity's XY plane, so we need to re-do some of the work above
			Plane dragPlane = new Plane(new Vec3d(0, 0, 1), dragCollisionPoint.z); // XY plane at collistion point

			if (dragInfo.shiftDown()) {
				Vec3d entPos = dispEnt.getGlobalPosition();

				double zDiff = RenderUtils.getZDiff(dragCollisionPoint, currentRay, lastRay);

				entPos.z += zDiff;
				dispEnt.setGlobalPosition(entPos);

				return true;
			}

			double cDist = dragPlane.collisionDist(currentRay);
			double lDist = dragPlane.collisionDist(lastRay);

			if (cDist < 0 || cDist == Double.POSITIVE_INFINITY ||
			    lDist < 0 || lDist == Double.POSITIVE_INFINITY)
				return true;

			Vec3d cPoint = currentRay.getPointAtDist(cDist);
			Vec3d lPoint = lastRay.getPointAtDist(lDist);

			Vec3d del = new Vec3d();
			del.sub3(cPoint, lPoint);

			Vec3d pos = dispEnt.getGlobalPosition();
			pos.add3(del);
			dispEnt.setGlobalPosition(pos);
			return true;
		}

		// Handle resize
		if (dragHandleID <= RESIZE_POSX_PICK_ID &&
		    dragHandleID >= RESIZE_NXNY_PICK_ID) {

			Vec3d pos = dispEnt.getGlobalPosition();
			Vec3d scale = dispEnt.getSize();
			Vec4d fixedPoint = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

			if (dragHandleID == RESIZE_POSX_PICK_ID) {
				//scale.x = 2*entSpaceCurrent.x() * size.x();
				scale.x += entSpaceDelta.x * size.x;
				fixedPoint = new Vec4d(-0.5,  0.0, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_POSY_PICK_ID) {
				scale.y += entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d( 0.0, -0.5, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_NEGX_PICK_ID) {
				scale.x -= entSpaceDelta.x * size.x;
				fixedPoint = new Vec4d( 0.5,  0.0, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_NEGY_PICK_ID) {
				scale.y -= entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d( 0.0,  0.5, 0.0, 1.0d);
			}

			if (dragHandleID == RESIZE_PXPY_PICK_ID) {
				scale.x += entSpaceDelta.x * size.x;
				scale.y += entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d(-0.5, -0.5, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_PXNY_PICK_ID) {
				scale.x += entSpaceDelta.x * size.x;
				scale.y -= entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d(-0.5,  0.5, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_NXPY_PICK_ID) {
				scale.x -= entSpaceDelta.x * size.x;
				scale.y += entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d( 0.5, -0.5, 0.0, 1.0d);
			}
			if (dragHandleID == RESIZE_NXNY_PICK_ID) {
				scale.x -= entSpaceDelta.x * size.x;
				scale.y -= entSpaceDelta.y * size.y;
				fixedPoint = new Vec4d( 0.5,  0.5, 0.0, 1.0d);
			}

			// Handle the case where the scale is pulled through itself. Fix the scale,
			// and swap the currently selected handle
			if (scale.x <= 0.00005) {
				scale.x = 0.0001;
				if (dragHandleID == RESIZE_POSX_PICK_ID) { dragHandleID = RESIZE_NEGX_PICK_ID; }
				else if (dragHandleID == RESIZE_NEGX_PICK_ID) { dragHandleID = RESIZE_POSX_PICK_ID; }

				else if (dragHandleID == RESIZE_PXPY_PICK_ID) { dragHandleID = RESIZE_NXPY_PICK_ID; }
				else if (dragHandleID == RESIZE_PXNY_PICK_ID) { dragHandleID = RESIZE_NXNY_PICK_ID; }
				else if (dragHandleID == RESIZE_NXPY_PICK_ID) { dragHandleID = RESIZE_PXPY_PICK_ID; }
				else if (dragHandleID == RESIZE_NXNY_PICK_ID) { dragHandleID = RESIZE_PXNY_PICK_ID; }
			}

			if (scale.y <= 0.00005) {
				scale.y = 0.0001;
				if (dragHandleID == RESIZE_POSY_PICK_ID) { dragHandleID = RESIZE_NEGY_PICK_ID; }
				else if (dragHandleID == RESIZE_NEGY_PICK_ID) { dragHandleID = RESIZE_POSY_PICK_ID; }

				else if (dragHandleID == RESIZE_PXPY_PICK_ID) { dragHandleID = RESIZE_PXNY_PICK_ID; }
				else if (dragHandleID == RESIZE_PXNY_PICK_ID) { dragHandleID = RESIZE_PXPY_PICK_ID; }
				else if (dragHandleID == RESIZE_NXPY_PICK_ID) { dragHandleID = RESIZE_NXNY_PICK_ID; }
				else if (dragHandleID == RESIZE_NXNY_PICK_ID) { dragHandleID = RESIZE_NXPY_PICK_ID; }
			}

			Vec4d oldFixed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			oldFixed.mult4(transMat, fixedPoint);
			dispEnt.setSize(scale);
			transMat = dispEnt.getTransMatrix(simTime); // Get the new matrix

			Vec4d newFixed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			newFixed.mult4(transMat, fixedPoint);

			Vec4d posAdjust = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			posAdjust.sub3(oldFixed, newFixed);

			pos.add3(posAdjust);
			dispEnt.setGlobalPosition(pos);

			Vec3d vec = dispEnt.getSize();
			InputAgent.processEntity_Keyword_Value(dispEnt, "Size", String.format((Locale)null, "%.6f %.6f %.6f %s", vec.x, vec.y, vec.z, "m" ));
			FrameBox.valueUpdate();
			return true;
		}

		if (dragHandleID == ROTATE_PICK_ID) {

			Vec3d align = dispEnt.getAlignment();

			Vec4d rotateCenter = new Vec4d(align.x, align.y, align.z, 1.0d);
			rotateCenter.mult4(transMat, rotateCenter);

			Vec4d a = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			a.sub3(lastPoint, rotateCenter);
			Vec4d b = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			b.sub3(currentPoint, rotateCenter);

			Vec4d aCrossB = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
			aCrossB.cross3(a, b);

			double sinTheta = aCrossB.z / a.mag3() / b.mag3();
			double theta = Math.asin(sinTheta);

			Vec3d orient = dispEnt.getOrientation();
			orient.z += theta;
			InputAgent.processEntity_Keyword_Value(dispEnt, "Orientation", String.format((Locale)null, "%f %f %f rad", orient.x, orient.y, orient.z));
			FrameBox.valueUpdate();
			return true;
		}
		if (dragHandleID == LINEDRAG_PICK_ID) {
			// Dragging a line object

			if (dragInfo.shiftDown()) {
				ArrayList<Vec3d> screenPoints = null;
				if (dispEnt instanceof HasScreenPoints) {
					HasScreenPoints.PointsInfo[] pointInfos = ((HasScreenPoints)dispEnt).getScreenPoints();
					if (pointInfos != null && pointInfos.length != 0)
						screenPoints = pointInfos[0].points;
				}
				if (screenPoints == null || screenPoints.size() == 0) return true; // just ignore this
				// Find the geometric median of the points
				Vec4d medPoint = RenderUtils.getGeometricMedian(screenPoints);

				double zDiff = RenderUtils.getZDiff(medPoint, currentRay, lastRay);
				dispEnt.dragged(new Vec3d(0, 0, zDiff));
				return true;
			}

			Region reg = dispEnt.getCurrentRegion();
			Transform regionInvTrans = new Transform();
			if (reg != null) {
				regionInvTrans = reg.getRegionTrans();
				regionInvTrans.inverse(regionInvTrans);
			}
			Vec3d localDelta = new Vec3d();
			regionInvTrans.multAndTrans(delta, localDelta);

			dispEnt.dragged(localDelta);
			return true;
		}

		if (dragHandleID <= LINENODE_PICK_ID) {
			int nodeIndex = (int)(-1*(dragHandleID - LINENODE_PICK_ID));
			ArrayList<Vec3d> screenPoints = null;
			if (dispEnt instanceof HasScreenPoints) {
				HasScreenPoints.PointsInfo[] pointInfos = ((HasScreenPoints)dispEnt).getScreenPoints();
				if (pointInfos != null && pointInfos.length != 0)
					screenPoints = pointInfos[0].points;
			}

			// Note: screenPoints is not a defensive copy, but we'll put it back into itself
			// in a second so everything should be safe
			if (screenPoints == null || nodeIndex >= screenPoints.size()) {
				// huh?
				return false;
			}
			Vec3d point = screenPoints.get(nodeIndex);

			if (dragInfo.shiftDown()) {
				double zDiff = RenderUtils.getZDiff(point, currentRay, lastRay);
				point.z += zDiff;
			} else {
				Plane pointPlane = new Plane(null, point.z);
				Vec3d diff = RenderUtils.getPlaneCollisionDiff(pointPlane, currentRay, lastRay);
				point.x += diff.x;
				point.y += diff.y;
				point.z += 0;
			}

			Input<?> pointsInput = dispEnt.getInput("Points");
			assert(pointsInput != null);
			if (pointsInput == null) {
				return true;
			}

			StringBuilder sb = new StringBuilder();
			String pointFormatter = " { %.3f %.3f %.3f m }";
			for(Vec3d pt : screenPoints) {
				sb.append(String.format((Locale)null, pointFormatter, pt.x, pt.y, pt.z));
			}

			InputAgent.processEntity_Keyword_Value(dispEnt, pointsInput, sb.toString());
			FrameBox.valueUpdate();
			return true;
		}

		return false;
	}

	private void splitLineEntity(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);

		Mat4d rayMatrix = MathUtils.RaySpace(currentRay);

		HasScreenPoints hsp = (HasScreenPoints)selectedEntity;
		assert(hsp != null);

		HasScreenPoints.PointsInfo[] pointInfos = hsp.getScreenPoints();
		assert(pointInfos != null && pointInfos.length != 0);

		ArrayList<Vec3d> points = pointInfos[0].points;

		int splitInd = 0;
		Vec4d nearPoint = null;
		// Find a line segment we are near
		for (;splitInd < points.size() - 1; ++splitInd) {
			Vec4d a = new Vec4d(points.get(splitInd  ).x, points.get(splitInd  ).y, points.get(splitInd  ).z, 1.0d);
			Vec4d b = new Vec4d(points.get(splitInd+1).x, points.get(splitInd+1).y, points.get(splitInd+1).z, 1.0d);

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
		Locale loc = null;
		String pointFormatter = " { %.3f %.3f %.3f m }";

		for(int i = 0; i <= splitInd; ++i) {
			Vec3d pt = points.get(i);
			sb.append(String.format(loc, pointFormatter, pt.x, pt.y, pt.z));
		}

		sb.append(String.format(loc, pointFormatter, nearPoint.x, nearPoint.y, nearPoint.z));

		for (int i = splitInd+1; i < points.size(); ++i) {
			Vec3d pt = points.get(i);
			sb.append(String.format(loc, pointFormatter, pt.x, pt.y, pt.z));
		}

		Input<?> pointsInput = selectedEntity.getInput("Points");
		InputAgent.processEntity_Keyword_Value(selectedEntity, pointsInput, sb.toString());
		FrameBox.valueUpdate();
	}

	private void removeLineNode(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);

		Mat4d rayMatrix = MathUtils.RaySpace(currentRay);

		HasScreenPoints hsp = (HasScreenPoints)selectedEntity;
		assert(hsp != null);

		HasScreenPoints.PointsInfo[] pointInfos = hsp.getScreenPoints();
		if (pointInfos == null || pointInfos.length == 0)
			return;

		ArrayList<Vec3d> points = pointInfos[0].points;
		// Find a point that is within the threshold

		if (points.size() <= 2) {
			return;
		}

		int removeInd = 0;
		// Find a line segment we are near
		for ( ;removeInd < points.size(); ++removeInd) {
			Vec4d p = new Vec4d(points.get(removeInd).x, points.get(removeInd).y, points.get(removeInd).z, 1.0d);

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
		Locale loc = null;
		String pointFormatter = " { %.3f %.3f %.3f m }";

		for(int i = 0; i < points.size(); ++i) {
			if (i == removeInd) {
				continue;
			}

			Vec3d pt = points.get(i);
			sb.append(String.format(loc, pointFormatter, pt.x, pt.y, pt.z));
		}

		Input<?> pointsInput = selectedEntity.getInput("Points");
		InputAgent.processEntity_Keyword_Value(selectedEntity, pointsInput, sb.toString());
		FrameBox.valueUpdate();
	}



	private boolean isMouseHandleID(long id) {
		return (id < 0); // For now all negative IDs are mouse handles, this may change
	}

	public boolean handleMouseButton(int windowID, int x, int y, int button, boolean isDown, int modifiers) {

		if (button != 1) { return false; }
		if (!isDown) {
			// Click released
			dragHandleID = 0;
			return true; // handled
		}

		boolean controlDown = (modifiers & WindowInteractionListener.MOD_CTRL) != 0;
		boolean altDown = (modifiers & WindowInteractionListener.MOD_ALT) != 0;

		if (controlDown && altDown) {
			// Check if we can split a line segment
			if (selectedEntity != null && selectedEntity instanceof HasScreenPoints) {
				if ((modifiers & WindowInteractionListener.MOD_SHIFT) != 0) {
					removeLineNode(windowID, x, y);
				} else {
					splitLineEntity(windowID, x, y);
				}
				return true;
			}
		}

		if (!controlDown) {
			return false;
		}

		Ray pickRay = getRayForMouse(windowID, x, y);

		View view = windowToViewMap.get(windowID);
		if (view == null) {
			return false;
		}

		List<PickData> picks = pickForRay(pickRay, view.getID(), true);

		Collections.sort(picks, new HandleSorter());

		if (picks.size() == 0) {
			return false;
		}

		double mouseHandleDist = Double.POSITIVE_INFINITY;
		double entityDist = Double.POSITIVE_INFINITY;
		// See if we are hovering over any interaction handles
		for (PickData pd : picks) {
			if (isMouseHandleID(pd.id) && mouseHandleDist == Double.POSITIVE_INFINITY) {
				// this is a mouse handle, remember the handle for future drag events
				dragHandleID = pd.id;
				mouseHandleDist = pd.dist;
			}
			if (selectedEntity != null && pd.id == selectedEntity.getEntityNumber()) {
				// We clicked on the selected entity
				entityDist = pd.dist;
			}
		}
		// The following logical condition effectively checks if we hit the entity first, and did not select
		// any mouse handle other than the move handle
		if (entityDist != Double.POSITIVE_INFINITY &&
		    entityDist < mouseHandleDist &&
		    (dragHandleID == 0 || dragHandleID == MOVE_PICK_ID)) {

			// Use the entity collision point for dragging instead of the handle collision point
			dragCollisionPoint = pickRay.getPointAtDist(entityDist);
			dragHandleID = MOVE_PICK_ID;
			return true;
		}
		if (mouseHandleDist != Double.POSITIVE_INFINITY) {
			// We hit a mouse handle
			dragCollisionPoint = pickRay.getPointAtDist(mouseHandleDist);
			return true;
		}

		return false;
	}

	public void clearSelection() {
		selectedEntity = null;
	}

	public void hideExistingPopups() {
		synchronized (popupLock) {
			if (lastPopup == null) {
				return;
			}

			lastPopup.setVisible(false);
			lastPopup = null;
		}
	}

	public boolean isDragAndDropping() {
		// This is such a brutal hack to work around newt's lack of drag and drop support
		// Claim we are still dragging for up to 10ms after the last drop failed...
		long currTime = System.nanoTime();
		return dndObjectType != null &&
		       ((currTime - dndDropTime) < 100000000); // Did the last 'drop' happen less than 100 ms ago?
	}

	public void startDragAndDrop(ObjectType ot) {
		dndObjectType = ot;
	}

	public void mouseMoved(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = Plane.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// I dunno...
			return;
		}

		Vec3d xyPlanePoint = currentRay.getPointAtDist(dist);
		GUIFrame.instance().showLocatorPosition(xyPlanePoint);
		queueRedraw();
	}


	public void createDNDObject(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = Plane.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// Unfortunate...
			return;
		}

		Vec3d creationPoint = currentRay.getPointAtDist(dist);

		// Create a new instance
		Class<? extends Entity> proto  = dndObjectType.getJavaClass();
		String name = proto.getSimpleName();
		Entity ent = InputAgent.defineEntityWithUniqueName(proto, name, "", true);

		// We are no longer drag-and-dropping
		dndObjectType = null;
		FrameBox.setSelectedEntity(ent);

		if (!(ent instanceof DisplayEntity)) {
			// This object is not a display entity, so the rest of this method does not apply
			return;
		}

		DisplayEntity dEntity  = (DisplayEntity) ent;

		try {
			dEntity.dragged(creationPoint);
		}
		catch (InputErrorException e) {}

		boolean isFlat = false;

		// Shudder....
		ArrayList<DisplayModel> displayModels = dEntity.getDisplayModelList();
		if (displayModels != null && displayModels.size() > 0) {
			DisplayModel dm0 = displayModels.get(0);
			if (dm0 instanceof DisplayModelCompat || dm0 instanceof ImageModel || dm0 instanceof TextModel )
				isFlat = true;
		}
		if (dEntity instanceof HasScreenPoints) {
			isFlat = true;
		}
		if (dEntity instanceof Graph) {
			isFlat = true;
		}

		if (isFlat) {
			Vec3d size = dEntity.getSize();

			ArrayList<String> tokens = new ArrayList<String>();
			tokens.add(dEntity.getInputName());
			tokens.add("Size");
			tokens.add("{");
			tokens.add(String.format((Locale)null, "%.3f", size.x));
			tokens.add(String.format((Locale)null, "%.3f", size.y));
			tokens.add("0.0");
			tokens.add("m");
			tokens.add("}");

			KeywordIndex kw = new KeywordIndex(tokens, 1, tokens.size() - 1, null);
			InputAgent.apply(dEntity, kw);
		} else {
			ArrayList<String> tokens = new ArrayList<String>();
			tokens.add(dEntity.getInputName());
			tokens.add("Alignment");
			tokens.add("{");
			tokens.add("0.0");
			tokens.add("0.0");
			tokens.add("-0.5");
			tokens.add("}");

			KeywordIndex kw = new KeywordIndex(tokens, 1, tokens.size() - 1, null);
			InputAgent.apply(dEntity, kw);
		}
		FrameBox.valueUpdate();
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent arg0) {
		// Clear the dragging flag
		dndDropTime = System.nanoTime();
	}

	@Override
	public void dragEnter(DragSourceDragEvent arg0) {}

	@Override
	public void dragExit(DragSourceEvent arg0) {}

	@Override
	public void dragOver(DragSourceDragEvent arg0) {}

	@Override
	public void dropActionChanged(DragSourceDragEvent arg0) {}

	public AABB getMeshBounds(MeshProtoKey key, boolean block) {
		if (block || MeshDataCache.isMeshLoaded(key)) {
			return MeshDataCache.getMeshData(key).getDefaultBounds();
		}

		// The mesh is not loaded and we are non-blocking, so trigger a mesh load and return
		MeshDataCache.loadMesh(key, new AtomicBoolean());
		return null;
	}

	public ArrayList<Action.Description> getMeshActions(MeshProtoKey key, boolean block) {
		if (block || MeshDataCache.isMeshLoaded(key)) {
			return MeshDataCache.getMeshData(key).getActionDescriptions();
		}

		// The mesh is not loaded and we are non-blocking, so trigger a mesh load and return
		MeshDataCache.loadMesh(key, new AtomicBoolean());
		return null;
	}

	/**
	 * Set the current windows camera to an isometric view
	 */
	public void setIsometricView() {
		CameraControl control = windowControls.get(activeWindowID);
		if (control == null) return;

		// The constant is acos(1/sqrt(3))
		control.setRotationAngles(0.955316, Math.PI/4);
	}

	/**
	 * Set the current windows camera to an XY plane view
	 */
	public void setXYPlaneView() {
		CameraControl control = windowControls.get(activeWindowID);
		if (control == null) return;

		// Do not look straight down the Z axis as that is actually a degenerate state
		control.setRotationAngles(0.0000001, 0.0);
	}

	public View getActiveView() {
		return windowToViewMap.get(activeWindowID);
	}

	public ArrayList<Integer> getOpenWindowIDs() {
		return renderer.getOpenWindowIDs();
	}

	public String getWindowName(int windowID) {
		return renderer.getWindowName(windowID);
	}

	public void focusWindow(int windowID) {
		renderer.focusWindow(windowID);
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
		return renderer.renderOffscreen(scene, viewID, camInfo, width, height, runWhenDone, null);
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
	public Future<BufferedImage> renderScreenShot(Vec3d cameraPos, Vec3d viewCenter, int viewID,
	                                              int width, int height, OffscreenTarget target) {

		Vec3d viewDiff = new Vec3d();
		viewDiff.sub3(cameraPos, viewCenter);

		double rotZ = Math.atan2(viewDiff.x, -viewDiff.y);

		double xyDist = Math.hypot(viewDiff.x, viewDiff.y);

		double rotX = Math.atan2(xyDist, viewDiff.z);

		if (Math.abs(rotX) < 0.005) {
			rotZ = 0; // Don't rotate if we are looking straight up or down
		}

		double viewDist = viewDiff.mag3();
		Quaternion rot = new Quaternion();
		rot.setRotZAxis(rotZ);

		Quaternion tmp = new Quaternion();
		tmp.setRotXAxis(rotX);

		rot.mult(rot, tmp);

		Transform trans = new Transform(cameraPos, rot, 1);

		CameraInfo camInfo = new CameraInfo(Math.PI/3, viewDist*0.1, viewDist*10, trans, null);

		return renderer.renderOffscreen(null, viewID, camInfo, width, height, null, target);
	}

	public Future<BufferedImage> getPreviewForDisplayModel(DisplayModel dm, Runnable notifier) {
		return previewCache.getPreview(dm, notifier);
	}

	public OffscreenTarget createOffscreenTarget(int width, int height) {
		return renderer.createOffscreenTarget(width, height);
	}

	public void freeOffscreenTarget(OffscreenTarget target) {
		renderer.freeOffscreenTarget(target);
	}

	private void takeScreenShot() {

		if (recorder != null)
			recorder.sample();

		synchronized(screenshot) {
			screenshot.set(false);
			recorder = null;
			screenshot.notifyAll();
		}
	}

	public void blockOnScreenShot(VideoRecorder recorder) {
		assert(!screenshot.get());

		synchronized (screenshot) {
			screenshot.set(true);
			this.recorder = recorder;
			queueRedraw();
			while (screenshot.get()) {
				try {
					screenshot.wait();
				} catch (InterruptedException ex) {}
			}
		}
	}

	public void shutdown() {
		timer.cancel();
		finished.set(true);
		if (renderer != null) {
			renderer.shutdown();
		}
	}

	/**
	 * Delete the currently selected entity
	 */
	public void deleteSelected() {
		if (selectedEntity == null) {
			return;
		}
		selectedEntity.kill();
		FrameBox.setSelectedEntity(null);
	}

	public static void setDebugInfo(boolean showDebug) {
		if (!isGood()) {
			return;
		}
		s_instance.renderer.setDebugInfo(showDebug);
		s_instance.queueRedraw();
	}
}

