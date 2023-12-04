/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2023 JaamSim Software Inc.
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
package com.jaamsim.controllers;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import com.jaamsim.Commands.Command;
import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Commands.ListCommand;
import com.jaamsim.DisplayModels.DisplayModel;
import com.jaamsim.GameObjects.GameEntity;
import com.jaamsim.Graphics.DirectedEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.Editable;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.Region;
import com.jaamsim.Graphics.View;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.ObjectType;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.math.AABB;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.Mat4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Plane;
import com.jaamsim.math.Ray;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.Action;
import com.jaamsim.render.CameraInfo;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.Future;
import com.jaamsim.render.LineProxy;
import com.jaamsim.render.MeshDataCache;
import com.jaamsim.render.MeshProtoKey;
import com.jaamsim.render.OffscreenTarget;
import com.jaamsim.render.PreviewCache;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.RenderUtils;
import com.jaamsim.render.Renderer;
import com.jaamsim.render.TessFontKey;
import com.jaamsim.render.TexCache;
import com.jaamsim.render.WindowInteractionListener;
import com.jaamsim.render.util.ExceptionLogger;
import com.jaamsim.ui.ContextMenu;
import com.jaamsim.ui.DragAndDropable;
import com.jaamsim.ui.FrameBox;
import com.jaamsim.ui.GUIFrame;
import com.jaamsim.ui.LogBox;
import com.jaamsim.units.AngleUnit;
import com.jaamsim.units.DistanceUnit;
import com.jogamp.newt.event.KeyEvent;

/**
 * Top level owner of the JaamSim renderer. This class both owns and drives the Renderer object, but is also
 * responsible for gathering rendering data every frame.
 * @author Matt Chudleigh
 *
 */
public class RenderManager implements DragSourceListener {
	private final static int EXCEPTION_STACK_THRESHOLD = 10; // The number of recoverable exceptions until a stack trace is output
	private final static int EXCEPTION_PRINT_RATE = 30; // The number of total exceptions until the overall log is printed

	/**
	 * Default plane used for Mouse click intersections.
	 */
	static final Plane XY_PLANE = new Plane();

	private int numberOfExceptions = 0;

	private static RenderManager s_instance = null;
	/**
	 * Basic singleton pattern
	 */
	public static void initialize(boolean safeGraphics) {
		Renderer render = new Renderer(safeGraphics);
		s_instance = new RenderManager(render);

		Thread managerThread = new Thread(new Runnable() {
			@Override
			public void run() {
				s_instance.renderManagerLoop();
			}
		}, "RenderManagerThread");
		managerThread.start();

	}

	public static RenderManager inst() { return s_instance; }

	private final Renderer renderer;
	private final AtomicBoolean finished = new AtomicBoolean(false);
	private final AtomicBoolean fatalError = new AtomicBoolean(false);
	private final RedrawCallback redraw = new RedrawCallback();

	// The video recorder to sample
	private final AtomicReference<VideoRecorder> recorder = new AtomicReference<>();
	private final ReentrantLock screenshotLock = new ReentrantLock();
	private final Condition screenshotWait = screenshotLock.newCondition();

	private final AtomicBoolean showReferences = new AtomicBoolean(false);
	private final AtomicBoolean showLinks = new AtomicBoolean(false);
	private final AtomicBoolean createLinks = new AtomicBoolean(false);
	private final AtomicBoolean linkDirection = new AtomicBoolean(true);
	private final double linkArrowSize = 0.2;

	private final ExceptionLogger exceptionLogger;

	private final HashMap<Integer, CameraControl> windowControls = new HashMap<>();
	private final HashMap<Integer, View> windowToViewMap= new HashMap<>();
	private int activeWindowID = -1;

	private final Object popupLock = new Object();
	private final Object sceneDragLock = new Object();
	private JPopupMenu lastPopup;

	private final ArrayList<DisplayEntity> selectedEntityList = new ArrayList<>();
	private Vec3d mousePosition = new Vec3d();
	private int mouseWindowID = -1;  // window containing the present mouse position

	private long simTick = 0;

	private long dragHandleID = 0;
	private Vec3d dragCollisionPoint;
	private Vec3d dragEntityPosition;
	private ArrayList<Vec3d> dragEntityPoints;
	private Vec3d dragEntityOrientation;
	private Vec3d dragEntitySize;
	private Mat4d dragEntityTransMat;
	private Mat4d dragEntityInvTransMat;
	private IntegerVector dragEntityScreenPosition;

	// The object type for drag-and-drop operation, if this is null, the user is not dragging
	private DragAndDropable dndObjectType;

	private final PreviewCache previewCache = new PreviewCache();

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

	public static final Color4d REF_LINK_COLOUR = new Color4d(138, 54, 15);  // Brown

	private RenderManager(Renderer r) {
		renderer = r;

		exceptionLogger = new ExceptionLogger(EXCEPTION_STACK_THRESHOLD);

		GUIFrame.registerCallback(redraw);
	}

	private class RedrawCallback implements Runnable {
		final AtomicBoolean redraw = new AtomicBoolean(false);

		@Override
		public void run() {
			synchronized(this) {
				if (windowControls.size() == 0 && recorder.get() == null) {
					return; // Do not queue a redraw if there are no open windows
				}
				redraw.set(true);
				this.notifyAll();
			}
		}

		final void beginDrawing() {
			redraw.set(false);
		}

		final void waitForRedraw() {
			// Wait for a redraw request
			synchronized(this) {
				while (!redraw.get()) {
					try {
						this.wait();
					} catch (InterruptedException e) {}
				}
			}
		}
	}

	public static final void updateTime(long simTick) {
		if (!RenderManager.isGood())
			return;

		RenderManager.inst().simTick = simTick;
	}

	public static final void redraw() {
		if (!isGood()) return;

		GUIFrame.updateUI();
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

		IntegerVector windSize = GUIFrame.getInstance().getWindowSize(view);
		IntegerVector windPos = GUIFrame.getInstance().getWindowPos(view);
		CameraControl control = new CameraControl(renderer, view);

		int windowID = renderer.createWindow(windPos.get(0), windPos.get(1),
		                                      windSize.get(0), windSize.get(1),
		                                      view.getID(),
		                                      view.getTitle(), view.getName(),
		                                      null, control);

		control.setWindowID(windowID);
		windowControls.put(windowID, control);
		windowToViewMap.put(windowID, view);

		LogBox.format("View window requested: %s", view.getName());

		GUIFrame.updateUI();
	}

	public void closeWindow(View view) {
		renderer.closeViewWindow(view.getID());
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
		if (v.showWindow() && !GUIFrame.getInstance().isIconified() && !v.isDead()
				&& GUIFrame.getJaamSimModel() == v.getJaamSimModel()) {
			KeywordIndex kw = InputAgent.formatBoolean("ShowWindow", false);
			InputAgent.storeAndExecute(new KeywordCommand(v, kw));
		}

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
	 */
	public static boolean canInitialize() {
		return s_instance == null;
	}

	private void renderManagerLoop() {
		boolean loopStarted = false;
		while (!finished.get() && !fatalError.get()) {
			try {

				if (renderer.hasFatalError()) {
					// Well, something went horribly wrong
					fatalError.set(true);
					LogBox.formatRenderLog("Renderer failed with error: %s\n", renderer.getErrorString());

					EventQueue.invokeAndWait(new Runnable() {
						@Override
						public void run() {
							LogBox.getInstance().setVisible(true);
						}
					});

					// Do some basic cleanup
					windowControls.clear();
					previewCache.clear();

					if (GUIFrame.isOptionalGraphics())
						break;

					GUIFrame.invokeErrorDialog("Graphics Error",
							"The 3D graphics system has encountered a fatal error:",
							renderer.getErrorString(),
							"In most cases, this type of error can be fixed by updating the "
							+ "computer's graphics drivers.\n"
							+ "See the User Manual for more information.");

					break;
				}

				if (!renderer.isInitialized()) {
					// Give the renderer a chance to initialize
					try {
						Thread.sleep(100);
					} catch(InterruptedException e) {}
					continue;
				}

				// Renderer has been initialized successfully
				if (!loopStarted) {
					LogBox.logLine("RenderManager loop started");
					loopStarted = true;
				}

				JaamSimModel simModel = GUIFrame.getJaamSimModel();
				double renderTime = simModel.getEventManager().ticksToSeconds(simTick);
				redraw.beginDrawing();

				ArrayList<View> views = GUIFrame.getInstance().getViews();
				for (int i = 0; i < views.size(); i++) {
					View v = views.get(i);
					v.update(renderTime);
				}

				for (CameraControl cc : windowControls.values()) {
					cc.checkForUpdate();
				}

				boolean screenShotThisFrame = (recorder.get() != null);

				int totalBindings = 0;
				long startNanos = System.nanoTime();
				long updateNanos = 0;
				long endNanos = 0;

				int maxRenderableEntities = simModel.getSimulation().getMaxEntitiesToDisplay();

				synchronized (sceneDragLock) {

					ArrayList<RenderProxy> cachedScene = new ArrayList<>();
					DisplayModelBinding.clearCacheCounters();
					DisplayModelBinding.clearCacheMissData();

					// Update all graphical entities in the simulation
					// All entities are updated regardless of the number or whether 'Show' is set
					// (required for Queue, etc.)
					updateGraphics(simModel, renderTime);

					// Show each entity in the model
					updateNanos = System.nanoTime();
					totalBindings = collectProxies(simModel, renderTime, maxRenderableEntities, cachedScene);

					// Show the green box around the selected entities
					// (collected second so they always appear on top)
					for (DisplayEntity ent : getSelectedEntityList()) {
						collectSelectionProxies(ent, renderTime, cachedScene);
					}

					// Show the displayable links for linked entities
					double delta = simModel.getSimulation().getSnapGridSpacing()/100.0d;
					if (showLinks.get()) {
						addLinkDisplays(simModel, linkDirection.get(), delta, cachedScene);
					}
					if (showReferences.get()) {
						addEntityReferenceDisplays(simModel, delta, cachedScene);
					}

					// Show a rubber band arrow from the selected entity to the mouse position
					if (createLinks.get() && isSingleEntitySelected() && mouseWindowID > 0) {
						DisplayEntity selectedEntity = getSelectedEntity();
						boolean dir = linkDirection.get();
						if (selectedEntity.canLink(dir)) {
							Vec3d source = selectedEntity.getSourcePoint(dir);
							Color4d linkColour = dir ? ColourInput.BLUE : ColourInput.RED;
							addLink(source, mousePosition, 0.0d, 0.0d, linkColour, linkArrowSize,
									delta, cachedScene);
						}
					}

					endNanos = System.nanoTime();
					renderer.setScene(cachedScene);
				} // sceneDragLock

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
					ArrayList<Long> debugIDs = new ArrayList<>(picks.size());

					StringBuilder dbgMsg = new StringBuilder(cacheString);
					dbgMsg.append(" Picked ").append(picks.size());
					dbgMsg.append(" entities at (").append(mouseInfo.x);
					dbgMsg.append(", ").append(mouseInfo.y).append("): ");
					for (PickData pd : picks) {
						Entity ent = simModel.idToEntity(pd.id);
						if (ent != null)
							dbgMsg.append(ent.getName());

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

			redraw.waitForRedraw();
		}

		exceptionLogger.printExceptionLog();

	}

	public void updateGraphics(JaamSimModel simModel, double simTime) {
		for (DisplayEntity de : simModel.getClonesOfIterator(DisplayEntity.class)) {
			try {
				de.updateGraphics(simTime);
			}
			// Catch everything so we don't screw up the behavior handling
			catch (Throwable e) {
				logException(e);
			}
		}
	}

	private int collectProxies(JaamSimModel simModel, double simTime, int maxEnts, ArrayList<RenderProxy> scene) {

		int numEnts = 0;
		int numBindings = 0;

		// Collect the render proxies for each entity
		for (DisplayEntity de : simModel.getClonesOfIterator(DisplayEntity.class)) {
			if (!de.getShow(simTime))
				continue;

			numEnts++;
			// There is an upper limit on number of entities
			if (numEnts > maxEnts) {
				break;
			}
			for (DisplayModelBinding binding : de.getDisplayBindings()) {
				try {
					numBindings++;
					binding.collectProxies(simTime, scene);
				} catch (Throwable t) {
					// Log the exception in the exception list
					logException(t);
				}
			}
		}

		return numBindings;
	}

	private void collectSelectionProxies(DisplayEntity ent, double simTime, ArrayList<RenderProxy> scene) {
		for (DisplayModelBinding binding : ent.getDisplayBindings()) {
			try {
				binding.collectSelectionProxies(simTime, scene);
			} catch (Throwable t) {
				// Log the exception in the exception list
				logException(t);
			}
		}
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

			ArrayList<DisplayEntity> ents = new ArrayList<>();

			for (PickData pd : picks) {
				if (!pd.isEntity) { continue; }
				Entity ent = GUIFrame.getJaamSimModel().idToEntity(pd.id);
				if (ent instanceof DisplayEntity) {
					DisplayEntity de = (DisplayEntity)ent;
					if (de.isMovable()) // only a movable DisplayEntity responds to a right-click
						ents.add(de);
				}
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
			final int nodeIndex = getNodeIndex(windowID, mouseInfo.x, mouseInfo.y);

			if (ents.size() == 0) {
				FrameBox.setSelectedEntity(null, false);
				ContextMenu.populateMenu(menu, null, nodeIndex, awtFrame, menuX, menuY);
			}

			else if (ents.size() == 1) {
				FrameBox.setSelectedEntity(ents.get(0), false);
				ContextMenu.populateMenu(menu, ents.get(0), nodeIndex, awtFrame, menuX, menuY);
			}

			else {
				// Several entities, let the user pick the interesting entity first
				Collections.sort(ents, Input.uiSortOrder);
				for (final DisplayEntity de : ents) {
					JMenuItem thisItem = new JMenuItem(de.getName());
					thisItem.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							FrameBox.setSelectedEntity(de, false);
							menu.removeAll();
							ContextMenu.populateMenu(menu, de, nodeIndex, awtFrame, menuX, menuY);
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

	public void handleMouseClicked(int windowID, int x, int y, int modifiers, short count) {

		boolean shiftDown = (modifiers & WindowInteractionListener.MOD_SHIFT) != 0;
		boolean controlDown = (modifiers & WindowInteractionListener.MOD_CTRL) != 0;
		boolean altDown = (modifiers & WindowInteractionListener.MOD_ALT) != 0;
		if (altDown)
			return;

		// Find the entity at this location
		List<PickData> picks = pickForMouse(windowID, false);
		Collections.sort(picks, new SelectionSorter());
		DisplayEntity ent = null;
		for (PickData pd : picks) {
			if (!pd.isEntity)
				continue;
			DisplayEntity e = (DisplayEntity) GUIFrame.getJaamSimModel().idToEntity(pd.id);
			if (e.isMovable()) {
				ent = e;
				break;
			}
		}

		// If no entity is found, set the selected entity to null
		if (ent == null) {
			FrameBox.setSelectedEntity(null, false);
			GUIFrame.updateUI();
			return;
		}

		// Select the entity
		if (controlDown && isEntitySelected()) {
			addSelectedEntity(ent);
			GUIFrame.updateUI();
			return;
		}
		FrameBox.setSelectedEntity(ent, true);

		// Handle the mouse click for an Overlay entity
		if (ent instanceof OverlayEntity) {
			OverlayEntity olEnt = (OverlayEntity) ent;
			Vec2d size = renderer.getViewableSize(windowID);
			olEnt.handleMouseClicked(count, x, y, (int)size.x, (int)size.y,
					shiftDown, controlDown, altDown);
			GUIFrame.updateUI();
			return;
		}

		// Handle the mouse click for a normal entity
		Vec3d globalCoord = getGlobalPositionForMouseData(windowID, x, y, ent);
		ent.handleMouseClicked(count, globalCoord, shiftDown, controlDown, altDown);
		GUIFrame.updateUI();
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
			return new ArrayList<>(); // empty set
		}

		// Look for overlay entities
		int x = mouseInfo.x;
		int y = mouseInfo.y;
		List<PickData> ret = pickForRasterCoord(x, y, view.getID());

		// Look for normal entities
		Ray pickRay = RenderUtils.getPickRay(mouseInfo);
		ret.addAll(pickForRay(pickRay, view.getID(), precise));

		return ret;
	}

	private List<PickData> pickForRasterCoord(int x, int y, int viewID) {
		List<PickData> ret = new ArrayList<>();
		Vec2d vec = new Vec2d(x, y);
		List<Long> overlayList = renderer.overlayPick(vec, viewID);
		for (Long id : overlayList) {
			ret.add(new PickData(id));
		}
		return ret;
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

		/**
		 * This pick was an overlay entity.
		 * @param id
		 */
		public PickData(long id) {
			this.id = id;
			size = 0;
			dist = 0;
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

			return Double.compare(p0.size, p1.size);
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

		List<PickData> uniquePicks = new ArrayList<>();

		// IDs that have already been added
		Set<Long> knownIDs = new HashSet<>();

		for (Renderer.PickResult pick : picks) {
			if (knownIDs.contains(pick.pickingID)) {
				continue;
			}
			knownIDs.add(pick.pickingID);

			DisplayEntity ent = (DisplayEntity)GUIFrame.getJaamSimModel().idToEntity(pick.pickingID);
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

	public static CameraInfo getCameraInfoForView(View view) {
		if (!isGood()) return null;

		RenderManager rman = RenderManager.inst();
		int winID = rman.getWindowID(view);
		Renderer.WindowMouseInfo mouseInfo = rman.renderer.getMouseInfo(winID);

		if (mouseInfo == null)
			return null;

		return mouseInfo.cameraInfo;
	}

	/**
	 * Returns the global coordinates for the given entity corresponding
	 * to given screen coordinates.
	 * @param windowID - view window that clicked
	 * @param x - horizontal raster coordinate
	 * @param y - vertical raster coordinate
	 * @param ent - entity whose local coordinates are returned
	 * @return local coordinate for the mouse click
	 */
	public Vec3d getGlobalPositionForMouseData(int windowID, int x, int y, DisplayEntity ent) {

		// Determine the plane in the global coordinate system that corresponds
		// to the entity's local X-Y plane
		Transform trans = ent.getGlobalTrans();
		Plane entityPlane = new Plane(); // Defaults to XY
		entityPlane.transform(trans, entityPlane);

		// Return the global coordinates for the point on the local X-Y plane
		// that lines up with the screen coordinates
		Ray mouseRay = getRayForMouse(windowID, x, y);
		double mouseDist = entityPlane.collisionDist(mouseRay);
		return mouseRay.getPointAtDist(mouseDist);
	}

	public Vec3d getRenderedStringSize(TessFontKey fontKey, double textHeight, String string) {
		return renderer.getTessFont(fontKey).getStringSize(textHeight, string);
	}

	public double getRenderedStringLength(TessFontKey fontKey, double textHeight, String string) {
		return renderer.getTessFont(fontKey).getStringLength(textHeight, string);
	}

	public int getRenderedStringPosition(TessFontKey fontKey, double textHeight, String string, double x, double y) {
		return renderer.getTessFont(fontKey).getStringPosition(textHeight, string, x, y);
	}

	/**
	 * Returns the x-coordinate for a given insertion position in a string.
	 * Insertion position i is the location prior to the i-th character in the string.
	 *
	 * @param i - insertion position
	 * @return x coordinate of the insertion position relative to the beginning of the string.
	 */
	public Vec3d getOffsetForStringPosition(TessFontKey fontKey, double textHeight, String string, int i) {
		return renderer.getTessFont(fontKey).getOffsetForString(textHeight, string.substring(0, i));
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

	public static void setSelection(Entity ent, boolean canMakeLink) {
		if (!RenderManager.isGood())
			return;

		RenderManager.inst().setSelectEntity(ent, canMakeLink);
	}

	private void setSelectEntity(Entity ent, boolean canMakeLink) {
		if (ent instanceof DisplayEntity) {
			DisplayEntity selectedEntity = (DisplayEntity) ent;
			DisplayEntity oldEnt = getSelectedEntity();
			selectedEntityList.clear();
			selectedEntityList.add(selectedEntity);

			boolean dir = linkDirection.get();
			if (createLinks.get() && canMakeLink && selectedEntity != oldEnt
					&& oldEnt != null && selectedEntity != null
					&& oldEnt.canLink(dir)) {
				try {
					oldEnt.linkTo(selectedEntity, dir);
				}
				catch (InputErrorException e) {}
			}

		}
		else if (ent instanceof View) {
			selectedEntityList.clear();
			View view = (View) ent;
			int windowID = getWindowID(view);
			focusWindow(windowID);
		}
		else {
			selectedEntityList.clear();
		}

		GUIFrame.updateUI();
	}

	private void addSelectedEntity(DisplayEntity ent) {
		if (!isSelectionValid(ent))
			return;
		selectedEntityList.add(ent);
	}

	private ArrayList<DisplayEntity> getSelectedEntityList() {
		return selectedEntityList;
	}

	private DisplayEntity getSelectedEntity() {
		if (selectedEntityList.isEmpty())
			return null;
		return selectedEntityList.get(0);
	}

	public boolean isEntitySelected() {
		return !selectedEntityList.isEmpty();
	}

	private boolean isSingleEntitySelected() {
		return selectedEntityList.size() == 1;
	}

	private boolean isSelectionValid(DisplayEntity ent) {
		if (!isEntitySelected())
			return true;
		DisplayEntity selectedEntity = getSelectedEntity();

		if (ent instanceof OverlayEntity && !(selectedEntity instanceof OverlayEntity))
			return false;

		if (!(ent instanceof OverlayEntity) && selectedEntity instanceof OverlayEntity)
			return false;

		if (ent.getCurrentRegion() != selectedEntity.getCurrentRegion())
			return false;

		if (ent.getParent() != selectedEntity.getParent())
			return false;

		// Is the new entity or any entities in its chain of relative entities already selected?
		DisplayEntity e = ent;
		while (e != null) {
			if (selectedEntityList.contains(e))
				return false;
			e = e.getRelativeEntity();
		}

		// Is the new entity a relative entity of any entities that are already selected?
		for (DisplayEntity de : selectedEntityList) {
			e = de.getRelativeEntity();
			while (e != null) {
				if (e == ent)
					return false;
				e = e.getRelativeEntity();
			}
		}

		return true;
	}

	/**
	 * This method gives the RenderManager a chance to handle mouse drags before the CameraControl
	 * gets to handle it (note: this may need to be refactored into a proper event handling heirarchy)
	 * @param dragInfo
	 * @return true if the drag action was handled successfully.
	 */
	public boolean handleDrag(WindowInteractionListener.DragInfo dragInfo) {

		synchronized(sceneDragLock) {

		// If control key is pressed and there is no object to move then do nothing (return true)
		// If control key is not pressed, then move the camera (return false)
		DisplayEntity selectedEntity = getSelectedEntity();
		if (selectedEntity == null || !selectedEntity.isMovable())
			return dragInfo.controlDown();

		// Overlay object
		if (selectedEntity instanceof OverlayEntity) {
			Vec2d size = renderer.getViewableSize(dragInfo.windowID);
			if (!dragInfo.controlDown()) {
				if (!isSingleEntitySelected())
					return false;
				OverlayEntity olEnt = (OverlayEntity) selectedEntity;
				return olEnt.handleDrag(dragInfo.x, dragInfo.y, dragInfo.startX, dragInfo.startY,
					(int)size.x, (int)size.y);
			}
			return handleOverlayMove(dragInfo.x, dragInfo.y, dragInfo.startX, dragInfo.startY,
					(int)size.x, (int)size.y);
		}

		// Find the start and current world space positions
		Ray firstRay = getRayForMouse(dragInfo.windowID, dragInfo.startX, dragInfo.startY);
		Ray currentRay = getRayForMouse(dragInfo.windowID, dragInfo.x, dragInfo.y);

		Transform trans = selectedEntity.getGlobalTrans();

		Plane entityPlane = new Plane(); // Defaults to XY
		entityPlane.transform(trans, entityPlane); // Transform the plane to world space

		double firstDist = entityPlane.collisionDist(firstRay);
		double currentDist = entityPlane.collisionDist(currentRay);

		// If the Control key is not pressed, then the selected entity handles the drag action
		if (!dragInfo.controlDown()) {
			if (!isSingleEntitySelected())
				return false;
			Vec3d firstPt = firstRay.getPointAtDist(firstDist);
			Vec3d currentPt = currentRay.getPointAtDist(currentDist);
			boolean ret = selectedEntity.handleDrag(currentPt, firstPt);
			return ret;
		}

		// Handle each handle by type...

		// Missed the selected entity and its handles
		if (dragHandleID == 0)
			return true;

		// MOVE
		if (dragHandleID == MOVE_PICK_ID) {
			if (!selectedEntity.isPositionNominal())
				return true;
			return handleMove(currentRay, firstRay, currentDist, firstDist, dragInfo.shiftDown());
		}

		// RESIZE
		if (dragHandleID <= RESIZE_POSX_PICK_ID && dragHandleID >= RESIZE_NXNY_PICK_ID) {
			if (!selectedEntity.isSizeNominal())
				return true;
			return handleResize(currentRay, firstRay, currentDist, firstDist);
		}

		// ROTATE
		if (dragHandleID == ROTATE_PICK_ID) {
			if (!selectedEntity.isOrientationNominal())
				return true;
			return handleRotate(currentRay, firstRay, currentDist, firstDist);
		}

		// LINE MOVE
		if (dragHandleID == LINEDRAG_PICK_ID) {
			if (!selectedEntity.isPointsNominal())
				return true;
			return handleLineMove(currentRay, firstRay, currentDist, firstDist, dragInfo.shiftDown());
		}

		// LINE NODE MOVE
		if (dragHandleID <= LINENODE_PICK_ID) {
			if (!selectedEntity.isPointsNominal())
				return true;
			return handleLineNodeMove(currentRay, firstRay, currentDist, firstDist, dragInfo.shiftDown());
		}

		return false;

		} // sceneDragLock
	}

	// Moves an overlay entity to a new position in the windows
	private boolean handleOverlayMove(int x, int y, int startX, int startY, int windowWidth, int windowHeight) {
		DisplayEntity selectedEntity = getSelectedEntity();
		OverlayEntity olEnt = (OverlayEntity) selectedEntity;
		IntegerVector lastPos = olEnt.getScreenPosition();

		if (dragEntityScreenPosition == null)
			return false;
		int dx = x - startX;
		int dy = y - startY;
		int posX = dragEntityScreenPosition.get(0) + dx * (olEnt.getAlignRight() ? -1 : 1);
		int posY = dragEntityScreenPosition.get(1) + dy * (olEnt.getAlignBottom() ? -1 : 1);
		posX = Math.min(Math.max(0, posX), windowWidth);
		posY = Math.min(Math.max(0, posY), windowHeight);
		KeywordIndex kw = InputAgent.formatIntegers("ScreenPosition", posX, posY);
		ArrayList<Command> cmdList = new ArrayList<>();
		cmdList.add(new KeywordCommand(olEnt, kw));

		// Move any additional entities that were selected
		int xOffset = (posX - lastPos.get(0)) * (olEnt.getAlignRight() ? -1 : 1);
		int yOffset = posY - lastPos.get(1) * (olEnt.getAlignBottom() ? -1 : 1);
		for (DisplayEntity ent : getSelectedEntityList()) {
			if (ent == selectedEntity)
				continue;
			olEnt = (OverlayEntity) ent;
			IntegerVector pos = olEnt.getScreenPosition();
			posX = pos.get(0) + xOffset * (olEnt.getAlignRight() ? -1 : 1);
			posY = pos.get(1) + yOffset * (olEnt.getAlignBottom() ? -1 : 1);
			posX = Math.min(Math.max(0, posX), windowWidth);
			posY = Math.min(Math.max(0, posY), windowHeight);
			kw = InputAgent.formatIntegers("ScreenPosition", posX, posY);
			cmdList.add(new KeywordCommand(olEnt, kw));
		}

		InputAgent.storeAndExecute(new ListCommand(cmdList));
		return true;
	}

	//Moves the selected entity to a new position in space
	private boolean handleMove(Ray currentRay, Ray firstRay, double currentDist, double firstDist, boolean shift) {
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();
		Simulation simulation = simModel.getSimulation();

		// Trap degenerate cases
		if (currentDist < 0 || currentDist == Double.POSITIVE_INFINITY ||
		      firstDist < 0 ||   firstDist == Double.POSITIVE_INFINITY)
			return true;

		// Global position of the entity prior to the drag
		Vec3d pos = new Vec3d(dragEntityPosition);

		// Vertical move
		if (shift) {
			double zDiff = RenderUtils.getZDiff(dragCollisionPoint, currentRay, firstRay);
			pos.z += zDiff;
		}

		// Horizontal move
		else {
			Plane dragPlane = new Plane(new Vec3d(0, 0, 1), dragCollisionPoint.z); // XY plane at collision point
			double cDist = dragPlane.collisionDist(currentRay);
			double lDist = dragPlane.collisionDist(firstRay);

			if (cDist < 0 || cDist == Double.POSITIVE_INFINITY ||
			    lDist < 0 || lDist == Double.POSITIVE_INFINITY)
				return true;

			Vec3d cPoint = currentRay.getPointAtDist(cDist);
			Vec3d lPoint = firstRay.getPointAtDist(lDist);

			Vec3d del = new Vec3d();
			del.sub3(cPoint, lPoint);
			pos.add3(del);
		}

		Vec3d localPos = selectedEntity.getLocalPosition(pos);
		if (simulation.isSnapToGrid())
			localPos = simulation.getSnapGridPosition(localPos, selectedEntity.getPosition(), shift);

		// Move the selected entity
		ArrayList<Command> cmdList = new ArrayList<>();
		if (!selectedEntity.usePointsInput()) {
			KeywordIndex kw = simModel.formatVec3dInput("Position", localPos, DistanceUnit.class);
			cmdList.add(new KeywordCommand(selectedEntity, kw));
		}
		else {
			ArrayList<Vec3d> points = selectedEntity.getPoints();
			Vec3d offset = new Vec3d(localPos);
			offset.sub3(selectedEntity.getPosition());
			KeywordIndex ptsKw = simModel.formatPointsInputs("Points", points, offset);
			cmdList.add(new KeywordCommand(selectedEntity, ptsKw));
		}

		// Move any additional entities that were selected
		Vec3d globalOffset = selectedEntity.getGlobalPosition(localPos);
		globalOffset.sub3(selectedEntity.getGlobalPosition());
		for (DisplayEntity ent : getSelectedEntityList()) {
			if (ent == selectedEntity)
				continue;
			pos = ent.getGlobalPosition();
			pos.add3(globalOffset);
			localPos = ent.getLocalPosition(pos);
			if (!ent.usePointsInput()) {
				KeywordIndex kw = simModel.formatVec3dInput("Position", localPos, DistanceUnit.class);
				cmdList.add(new KeywordCommand(ent, kw));
			}
			else {
				ArrayList<Vec3d> points = ent.getPoints();
				Vec3d globalPos = ent.getGlobalPosition(points.get(0));
				globalPos.add3(globalOffset);
				Vec3d offset = ent.getLocalPosition(globalPos);
				offset.sub3(points.get(0));
				KeywordIndex ptsKw = simModel.formatPointsInputs("Points", points, offset);
				cmdList.add(new KeywordCommand(ent, ptsKw));
			}
		}

		InputAgent.storeAndExecute(new ListCommand(cmdList));
		return true;
	}

	private boolean handleResize(Ray currentRay, Ray firstRay, double currentDist, double firstDist) {
		if (!isSingleEntitySelected())
			return false;
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();
		Simulation simulation = simModel.getSimulation();

		Vec3d currentPoint = currentRay.getPointAtDist(currentDist);
		Vec3d firstPoint = firstRay.getPointAtDist(firstDist);

		Vec3d entSpaceCurrent = new Vec3d(); // entSpacePoint is the current point in model space
		entSpaceCurrent.multAndTrans3(dragEntityInvTransMat, currentPoint);

		Vec3d entSpaceFirst = new Vec3d(); // entSpaceLast is the last point in model space
		entSpaceFirst.multAndTrans3(dragEntityInvTransMat, firstPoint);

		Vec3d entSpaceDelta = new Vec3d();
		entSpaceDelta.sub3(entSpaceCurrent, entSpaceFirst);

		Vec3d scale = new Vec3d(dragEntitySize);
		Vec4d fixedPoint = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);

		if (dragHandleID == RESIZE_POSX_PICK_ID) {
			scale.x += entSpaceDelta.x * dragEntitySize.x;
			fixedPoint = new Vec4d(-0.5,  0.0, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_POSY_PICK_ID) {
			scale.y += entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d( 0.0, -0.5, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_NEGX_PICK_ID) {
			scale.x -= entSpaceDelta.x * dragEntitySize.x;
			fixedPoint = new Vec4d( 0.5,  0.0, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_NEGY_PICK_ID) {
			scale.y -= entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d( 0.0,  0.5, 0.0, 1.0d);
		}

		if (dragHandleID == RESIZE_PXPY_PICK_ID) {
			scale.x += entSpaceDelta.x * dragEntitySize.x;
			scale.y += entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d(-0.5, -0.5, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_PXNY_PICK_ID) {
			scale.x += entSpaceDelta.x * dragEntitySize.x;
			scale.y -= entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d(-0.5,  0.5, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_NXPY_PICK_ID) {
			scale.x -= entSpaceDelta.x * dragEntitySize.x;
			scale.y += entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d( 0.5, -0.5, 0.0, 1.0d);
		}
		if (dragHandleID == RESIZE_NXNY_PICK_ID) {
			scale.x -= entSpaceDelta.x * dragEntitySize.x;
			scale.y -= entSpaceDelta.y * dragEntitySize.y;
			fixedPoint = new Vec4d( 0.5,  0.5, 0.0, 1.0d);
		}

		scale.x = Math.max(0.0d, scale.x);
		scale.y = Math.max(0.0d, scale.y);

		if (simulation.isSnapToGrid())
			scale = simulation.getSnapGridPosition(scale, dragEntitySize, false);

		// Determine the new position for the entity
		Vec4d oldFixed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		oldFixed.mult4(dragEntityTransMat, fixedPoint);

		Vec4d newFixed = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		Mat4d transMat = selectedEntity.getTransMatrix(scale); // Get the new matrix once size is changed
		newFixed.mult4(transMat, fixedPoint);

		Vec4d posAdjust = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		posAdjust.sub3(oldFixed, newFixed);
		Vec3d pos = selectedEntity.getGlobalPosition();
		pos.add3(posAdjust);
		Vec3d localPos = selectedEntity.getLocalPosition(pos);

		KeywordIndex sizeKw = simModel.formatVec3dInput("Size", scale, DistanceUnit.class);
		KeywordIndex posKw = simModel.formatVec3dInput("Position", localPos, DistanceUnit.class);
		InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, sizeKw, posKw));
		return true;
	}

	public static final double ANGLE_SPACING = Math.toRadians(1.0d);

	private boolean handleRotate(Ray currentRay, Ray firstRay, double currentDist, double firstDist) {
		if (!isSingleEntitySelected())
			return false;
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();
		Simulation simulation = simModel.getSimulation();

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vec3d currentPoint = currentRay.getPointAtDist(currentDist);
		Vec3d firstPoint = firstRay.getPointAtDist(firstDist);

		Vec3d align = selectedEntity.getAlignment();

		Vec4d rotateCenter = new Vec4d(align.x, align.y, align.z, 1.0d);
		rotateCenter.mult4(dragEntityTransMat, rotateCenter);

		Vec4d a = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		a.sub3(firstPoint, rotateCenter);
		Vec4d b = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		b.sub3(currentPoint, rotateCenter);

		Vec4d aCrossB = new Vec4d(0.0d, 0.0d, 0.0d, 1.0d);
		aCrossB.cross3(a, b);

		double sinTheta = aCrossB.z / a.mag3() / b.mag3();
		double cosTheta = a.dot3(b) / a.mag3() / b.mag3();
		double theta = Math.atan2(sinTheta, cosTheta);

		Vec3d orient = new Vec3d(dragEntityOrientation);
		orient.z += theta;
		if (simulation.isSnapToGrid())
			orient = Simulation.getSnapGridPosition(orient, dragEntityOrientation, true, ANGLE_SPACING);

		KeywordIndex kw = simModel.formatVec3dInput("Orientation", orient, AngleUnit.class);
		InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kw));
		return true;
	}

	private boolean handleLineMove(Ray currentRay, Ray firstRay, double currentDist, double firstDist, boolean shift) {
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();
		Simulation simulation = simModel.getSimulation();

		// The points where the previous pick ended and current position. Collision is with the entity's XY plane
		Vec3d currentPoint = currentRay.getPointAtDist(currentDist);
		Vec3d firstPoint = firstRay.getPointAtDist(firstDist);

		if (dragEntityPoints == null || dragEntityPoints.isEmpty())
			return true;

		// Global node positions at the start of the move
		ArrayList<Vec3d> globalPts = selectedEntity.getGlobalPosition(dragEntityPoints);

		Vec3d delta = new Vec3d();

		if (shift) {
			Vec4d medPoint = RenderUtils.getGeometricMedian(globalPts);
			delta.z = RenderUtils.getZDiff(medPoint, currentRay, firstRay);
		}
		else {
			delta.sub3(currentPoint, firstPoint);
		}

		// Align the first node to snap grid
		if (simulation.isSnapToGrid()) {
			Vec3d point = new Vec3d();
			point.add3(globalPts.get(0), delta);
			Vec3d localPos = selectedEntity.getLocalPosition(point);
			localPos = simulation.getSnapGridPosition(localPos, dragEntityPoints.get(0), shift);
			point = selectedEntity.getGlobalPosition(localPos);
			delta.sub3(point, globalPts.get(0));
		}

		// Set the new position for the line
		for (Vec3d pt : globalPts) {
			pt.add3(delta);
		}
		ArrayList<Vec3d> localPts = selectedEntity.getLocalPosition(globalPts);

		KeywordIndex ptsKw = simModel.formatPointsInputs("Points", localPts, new Vec3d());
		ArrayList<Command> cmdList = new ArrayList<>();
		cmdList.add(new KeywordCommand(selectedEntity, -1, ptsKw));

		// Move any additional entities that were selected
		Vec3d globalOffset = globalPts.get(0);
		globalOffset.sub3(selectedEntity.getGlobalPosition(selectedEntity.getPoints().get(0)));
		for (DisplayEntity ent : getSelectedEntityList()) {
			if (ent == selectedEntity)
				continue;
			if (!ent.usePointsInput()) {
				Vec3d pos = ent.getGlobalPosition();
				pos.add3(globalOffset);
				Vec3d localPos = ent.getLocalPosition(pos);
				KeywordIndex posKw = simModel.formatVec3dInput("Position", localPos, DistanceUnit.class);
				cmdList.add(new KeywordCommand(ent, posKw));
			}
			else {
				ArrayList<Vec3d> points = ent.getPoints();
				Vec3d globalPos = ent.getGlobalPosition(points.get(0));
				globalPos.add3(globalOffset);
				Vec3d offset = ent.getLocalPosition(globalPos);
				offset.sub3(points.get(0));
				ptsKw = simModel.formatPointsInputs("Points", points, offset);
				cmdList.add(new KeywordCommand(ent, ptsKw));
			}
		}

		InputAgent.storeAndExecute(new ListCommand(cmdList));
		return true;
	}

	private boolean handleLineNodeMove(Ray currentRay, Ray firstRay, double currentDist, double firstDist, boolean shift) {
		if (!isSingleEntitySelected())
			return false;
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();
		Simulation simulation = simModel.getSimulation();

		int nodeIndex = (int)(-1*(dragHandleID - LINENODE_PICK_ID));

		ArrayList<Vec3d> screenPoints = selectedEntity.getPoints();
		if (screenPoints == null || nodeIndex >= screenPoints.size())
			return false;

		// Global node position at the start of the move
		Vec3d point = selectedEntity.getGlobalPosition(dragEntityPoints.get(nodeIndex));

		// Global node position at the end of the move
		Vec3d diff = new Vec3d();
		if (shift) {
			diff.z = RenderUtils.getZDiff(point, currentRay, firstRay);
		} else {
			Plane pointPlane = new Plane(null, point.z);
			diff = RenderUtils.getPlaneCollisionDiff(pointPlane, currentRay, firstRay);
			diff.z = 0.0d;
		}
		point.add3(diff);
		Vec3d localPos = selectedEntity.getLocalPosition(point);

		// Align the node to snap grid
		if (simulation.isSnapToGrid()) {
			Vec3d oldPos = screenPoints.get(nodeIndex);
			localPos = simulation.getSnapGridPosition(localPos, oldPos, shift);
		}

		ArrayList<Vec3d> newPoints = new ArrayList<>();
		for (Vec3d v : screenPoints) {
			newPoints.add(v);
		}

		// Set the new position for the node
		newPoints.set(nodeIndex, localPos);

		KeywordIndex ptsKw = simModel.formatPointsInputs("Points", newPoints, new Vec3d());
		InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, nodeIndex, ptsKw));
		return true;
	}

	private void splitLineEntity(int windowID, int x, int y) {
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();

		Ray currentRay = getRayForMouse(windowID, x, y);

		Mat4d rayMatrix = MathUtils.RaySpace(currentRay);

		ArrayList<Vec3d> points = selectedEntity.getPoints();
		if (points == null || points.isEmpty())
			return;

		ArrayList<Vec3d> globalPoints = selectedEntity.getGlobalPosition(points);

		int splitInd = 0;
		Vec4d nearPoint = null;
		// Find a line segment we are near
		for (;splitInd < points.size() - 1; ++splitInd) {
			Vec4d a = new Vec4d(globalPoints.get(splitInd  ).x, globalPoints.get(splitInd  ).y, globalPoints.get(splitInd  ).z, 1.0d);
			Vec4d b = new Vec4d(globalPoints.get(splitInd+1).x, globalPoints.get(splitInd+1).y, globalPoints.get(splitInd+1).z, 1.0d);

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

		// Insert the new node
		points.add(splitInd + 1, selectedEntity.getLocalPosition(nearPoint));

		KeywordIndex ptsKw = simModel.formatPointsInputs("Points", points, new Vec3d());
		InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, splitInd + 1, ptsKw));
	}

	private void removeLineNode(int windowID, int x, int y) {
		DisplayEntity selectedEntity = getSelectedEntity();
		JaamSimModel simModel = selectedEntity.getJaamSimModel();

		ArrayList<Vec3d> points = selectedEntity.getPoints();
		if (points == null || points.size() <= 2)
			return;

		int removeInd = getNodeIndex(windowID, x, y);
		if (removeInd == -1)
			return;

		// Remove the selected node
		points.remove(removeInd);

		KeywordIndex ptsKw = simModel.formatPointsInputs("Points", points, new Vec3d());
		InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, removeInd, ptsKw));
	}

	public int getNodeIndex(int windowID, int x, int y) {
		if (!isSingleEntitySelected())
			return -1;

		DisplayEntity selectedEntity = getSelectedEntity();
		ArrayList<Vec3d> points = selectedEntity.getPoints();
		if (points == null)
			return -1;

		Ray currentRay = getRayForMouse(windowID, x, y);
		Mat4d rayMatrix = MathUtils.RaySpace(currentRay);

		ArrayList<Vec3d> globalPoints = selectedEntity.getGlobalPosition(points);
		for (int i = 0; i < points.size(); i++) {
			Vec4d p = new Vec4d(globalPoints.get(i).x, globalPoints.get(i).y, globalPoints.get(i).z, 1.0d);
			double rayAngle = RenderUtils.angleToRay(rayMatrix, p);
			if (rayAngle > 0 && rayAngle < 0.01309) { // 0.75 degrees in radians
				return i;
			}
		}
		return -1;
	}

	private boolean isMouseHandleID(long id) {
		return (id < 0); // For now all negative IDs are mouse handles, this may change
	}

	public boolean handleMouseButton(int windowID, int x, int y, int button, boolean isDown, int modifiers) {
		DisplayEntity selectedEntity = getSelectedEntity();

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
			if (selectedEntity != null) {
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

		// Record the data for the selected entity before it is dragged
		if (selectedEntity != null) {
			dragEntityPosition = selectedEntity.getGlobalPosition();
			dragEntityPoints = selectedEntity.getPoints();
			dragEntityOrientation = selectedEntity.getOrientation();
			dragEntitySize = selectedEntity.getSize();
			dragEntityTransMat = selectedEntity.getTransMatrix(dragEntitySize);
			dragEntityInvTransMat = selectedEntity.getInvTransMatrix();
			if (selectedEntity instanceof OverlayEntity) {
				dragEntityScreenPosition = ((OverlayEntity) selectedEntity).getScreenPosition();
			}
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
			if (selectedEntity.usePointsInput())
				dragHandleID = LINEDRAG_PICK_ID;
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
		selectedEntityList.clear();
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

	public void startDragAndDrop(DragAndDropable ot) {
		dndObjectType = ot;
	}

	public void mouseMoved(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = RenderManager.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// I dunno...
			return;
		}

		mousePosition = currentRay.getPointAtDist(dist);
		GUIFrame.showLocatorPosition(mousePosition);
	}

	public void mouseEntry(int windowID, int x, int y, boolean isInWindow) {
		mouseWindowID = isInWindow ? windowID : -1;
	}

	public Region getRegion(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		int viewID = getActiveView().getID();
		List<PickData> picks = pickForRay(currentRay, viewID, false);
		Collections.sort(picks, new SelectionSorter());
		for (PickData pd : picks) {
			if (!pd.isEntity)
				continue;
			Entity ent = GUIFrame.getJaamSimModel().idToEntity(pd.id);
			if (ent instanceof Region) {
				return (Region) ent;
			}
		}
		return null;
	}

	public void createDNDObject(int windowID, int x, int y) {
		JaamSimModel simModel = dndObjectType.getJaamSimModel();
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = RenderManager.XY_PLANE.collisionDist(currentRay);

		if (dist == Double.POSITIVE_INFINITY) {
			// Unfortunate...
			return;
		}

		// Set the sub-model for this location
		Entity parent = null;
		Region region = getRegion(windowID, x, y);
		if (region != null && region.getParent() != simModel.getSimulation()) {
			parent = region.getParent();
		}

		// Create a new instance
		Class<? extends Entity> klass  = dndObjectType.getJavaClass();
		String name = dndObjectType.getName();
		if (parent != null && !(OverlayEntity.class.isAssignableFrom(klass))) {
			name = parent.getName() + "." + name;
		}
		name = InputAgent.getUniqueName(simModel, name, "");
		Entity proto = (Entity) dndObjectType;
		if (proto instanceof ObjectType)
			proto = null;
		InputAgent.storeAndExecute(new DefineCommand(simModel, klass, proto, name));
		Entity ent = simModel.getNamedEntity(name);

		// Set input values for a dragged and dropped entity
		ent.setInputsForDragAndDrop();

		// Set the link from the selected entity
		boolean dir = linkDirection.get();
		if (createLinks.get() && isSingleEntitySelected() && ent instanceof DisplayEntity
				&& getSelectedEntity().canLink(dir)) {
			getSelectedEntity().linkTo((DisplayEntity) ent, dir);
		}

		// We are no longer drag-and-dropping
		dndObjectType = null;
		FrameBox.setSelectedEntity(ent, false);

		// Set the position for the entity
		if (ent instanceof DisplayEntity) {
			DisplayEntity dispEnt = (DisplayEntity) ent;

			// Set the region
			if (region != null && !(ent instanceof OverlayEntity))
				InputAgent.applyArgs(ent, "Region", region.getName());

			// Set the location
			Vec3d creationPoint = currentRay.getPointAtDist(dist);
			creationPoint = dispEnt.getLocalPosition(creationPoint);
			Simulation simulation = simModel.getSimulation();
			if (simulation.isSnapToGrid()) {
				creationPoint = simulation.getSnapGridPosition(creationPoint);
			}

			try {
				dispEnt.dragged(x, y, creationPoint);
			}
			catch (InputErrorException e) {}

			// Add labels for the entity and its children
			simModel.showTemporaryLabels(simulation.isShowLabels());

			// Set the focus on the window that received the entity
			renderer.requestFocus(windowID);
		}
	}

	@Override
	public void dragDropEnd(DragSourceDropEvent evt) {

		// Find the view windows that contain this screen point
		ArrayList<Integer> list = new ArrayList<>();
		for (int id : windowToViewMap.keySet()) {
			if (renderer.getAWTFrame(id).getBounds().contains(evt.getX(), evt.getY())) {
				list.add(id);
			}
		}
		if (list.isEmpty())
			return;

		// If multiple windows are found, try to select the one on top
		int windowID = list.get(0);
		if (list.contains(activeWindowID)) {
			windowID = activeWindowID;
		}

		// Convert the global screen points to ones for the window
		Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(windowID);
		if (mouseInfo == null)
			return;
		int x = evt.getX() - mouseInfo.viewableX;
		int y = evt.getY() - mouseInfo.viewableY;

		createDNDObject(windowID, x, y);
	}

	@Override
	public void dragEnter(DragSourceDragEvent arg0) {}

	@Override
	public void dragExit(DragSourceEvent arg0) {}

	@Override
	public void dragOver(DragSourceDragEvent arg0) {}

	@Override
	public void dropActionChanged(DragSourceDragEvent arg0) {}

	public AABB getMeshBounds(MeshProtoKey key) {
		return MeshDataCache.getMeshData(key).getDefaultBounds();
	}

	public ArrayList<Action.Description> getMeshActions(MeshProtoKey key) {
		return MeshDataCache.getMeshData(key).getActionDescriptions();
	}

	public Vec2d getImageDims(URI imageURI) {
		if (imageURI == null)
			return null;
		Dimension dim = TexCache.getImageDimension(imageURI);
		if (dim == null)
			return null;

		return new Vec2d(dim.getWidth(), dim.getHeight());
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
		setActiveWindow(windowID);
		renderer.focusWindow(windowID);
	}

	public void focusActiveWindow() {
		renderer.requestFocus(activeWindowID);
	}

	private int getWindowID(View view) {
		for (Map.Entry<Integer, View> entry : windowToViewMap.entrySet()) {
			if (entry.getValue() == view) {
				return entry.getKey();
			}
		}
		return -1;
	}

	public boolean isVisible(View view) {
		return windowToViewMap.containsValue(view);
	}

	public static Frame getOpenWindowForView(View view) {
		if (!isGood()) return null;

		RenderManager rman = RenderManager.inst();
		int winID = rman.getWindowID(view);
		return rman.renderer.getAWTFrame(winID);
	}

	public void setPOI(View view, Vec3d pt) {
		int winID = getWindowID(view);
		CameraControl control = windowControls.get(winID);
		if (control == null)
			return;
		control.setPOI(pt);
	}

	public Vec3d getPOI(View view) {
		int winID = getWindowID(view);
		CameraControl control = windowControls.get(winID);
		if (control == null)
			return null;
		return control.getPOI();
	}

	public Vec3d getPOI() {
		CameraControl control = windowControls.get(activeWindowID);
		if (control == null)
			return null;
		return control.getPOI();
	}

	/**
	 * Can this hardware perform off screen rendering. Note: this method returning true is necessary, but not sufficient to
	 * support off screen rendering.
	 */
	public static boolean canRenderOffscreen() {
		if (!isGood()) return false;

		RenderManager rman = RenderManager.inst();
		return rman.renderer.isGL3Supported();
	}

	/**
	 * Queue up an off screen rendering, this simply passes the call directly to the renderer
	 * @param scene
	 * @param camInfo
	 * @param width
	 * @param height
	 */
	public Future<BufferedImage> renderOffscreen(ArrayList<RenderProxy> scene, CameraInfo camInfo, int viewID,
	                                   int width, int height, Runnable runWhenDone) {
		return renderer.renderOffscreen(scene, viewID, camInfo, width, height, runWhenDone, null);
	}

	/**
	 * Returns an image of the specified simulation model.
	 * @param simModel - simulation model to render
	 * @param simTime - present simulation time
	 * @param camInfo - position and orientation of the view camera
	 * @param width - width of the returned image
	 * @param height - height of the returned image
	 * @return image of the model
	 */
	public Future<BufferedImage> renderOffscreen(JaamSimModel simModel, double simTime, CameraInfo camInfo, int width, int height) {

		// Update the graphics for each entity in the model
		updateGraphics(simModel, simTime);

		// Collect the display information for each entity
		ArrayList<RenderProxy> scene = new ArrayList<>();
		int maxEnts = simModel.getSimulation().getMaxEntitiesToDisplay();
		collectProxies(simModel, simTime, maxEnts, scene);

		// Show the entity flow links
		boolean showLinks = simModel.getSimulation().isShowEntityFlow();
		if (showLinks) {
			double delta = simModel.getSimulation().getSnapGridSpacing()/100.0d;
			addLinkDisplays(simModel, true, delta, scene);
		}

		return renderOffscreen(scene, camInfo, View.OMNI_VIEW_ID, width, height, null);
	}

	/**
	 * Return a FutureImage of the equivalent screen renderer from the given position looking at the given center
	 * @param view
	 * @param width - width of returned image
	 * @param height - height of returned image
	 * @param target - optional target to prevent re-allocating GPU resources
	 */
	public Future<BufferedImage> renderScreenShot(View view, int width, int height, OffscreenTarget target) {
		CameraInfo camInfo = view.getCameraInfo();
		return renderer.renderOffscreen(null, view.getID(), camInfo, width, height, null, target);
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
		VideoRecorder rec = recorder.get();
		if (rec != null)
			rec.sample();

		screenshotLock.lock();
		try {
			this.recorder.set(null);
			screenshotWait.signalAll();
		}
		finally {
			screenshotLock.unlock();
		}
	}

	public void blockOnScreenShot(VideoRecorder recorder) {
		screenshotLock.lock();
		try {
			this.recorder.set(recorder);
			GUIFrame.updateUI();
			while (this.recorder.get() != null) {
				screenshotWait.awaitUninterruptibly();
			}
		}
		finally {
			screenshotLock.unlock();
		}
	}

	public void shutdown() {
		finished.set(true);
		if (renderer != null) {
			renderer.shutdown();
		}
	}

	public boolean handleKeyPressed(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		DisplayEntity selectedEntity = getSelectedEntity();

		// Selected entity in edit mode
		if (selectedEntity instanceof Editable && ((Editable) selectedEntity).isEditMode()) {
			if (!isSingleEntitySelected())
				return false;
			selectedEntity.handleKeyPressed(keyCode, keyChar, shift, control, alt);
			return true;
		}

		// Key combinations for Control panel buttons
		if (control && keyCode == KeyEvent.VK_Z) {
			GUIFrame.getInstance().invokeUndo();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_Y) {
			GUIFrame.getInstance().invokeRedo();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_N) {
			GUIFrame.getInstance().invokeNew();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_O) {
			GUIFrame.getInstance().invokeOpen();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_S) {
			GUIFrame.getInstance().invokeSave();
			return true;
		}
		if (control && alt && keyCode == KeyEvent.VK_S) {
			GUIFrame.getInstance().invokeSaveAs();
			return true;
		}
		if (alt && keyCode == KeyEvent.VK_F4) {
			GUIFrame.getInstance().invokeExit();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_F) {
			GUIFrame.getInstance().invokeFind();
			return true;
		}
		if (keyCode == KeyEvent.VK_F1) {
			GUIFrame.getInstance().invokeHelp();
			return true;
		}
		if (keyCode == KeyEvent.VK_SPACE) {
			GUIFrame.getInstance().invokeRunPause();
			return true;
		}
		if (keyCode == KeyEvent.VK_PERIOD) {   // same as the '>' key
			GUIFrame.getInstance().invokeSimSpeedUp();
			return true;
		}
		if (keyCode == KeyEvent.VK_COMMA) {    // same as the '<' key
			GUIFrame.getInstance().invokeSimSpeedDown();
			return true;
		}
		if (control && keyCode == KeyEvent.VK_C) {
			if (selectedEntity != null) {
				GUIFrame.getInstance().invokeCopy();
				return true;
			}
		}
		if (control && keyCode == KeyEvent.VK_V) {
			GUIFrame.getInstance().invokePaste();
			return true;
		}

		// Selected entity not in edit mode
		if (selectedEntity != null) {
			boolean bool = false;
			for (DisplayEntity ent : getSelectedEntityList()) {
				boolean b = ent.handleKeyPressed(keyCode, keyChar, shift, control, alt);
				bool = bool || b;
			}
			return bool;
		}

		// Game entities
		boolean bool = false;
		if (keyCode != KeyEvent.VK_LEFT && keyCode != KeyEvent.VK_RIGHT
				&& keyCode != KeyEvent.VK_UP && keyCode != KeyEvent.VK_DOWN) {
			for (GameEntity gEnt : GUIFrame.getJaamSimModel().getClonesOfIterator(GameEntity.class)) {
				bool = bool || gEnt.handleKeyPressed(keyCode, keyChar, shift, control, alt);
			}
		}
		return bool;
	}

	public void handleKeyReleased(int keyCode, char keyChar, boolean shift, boolean control, boolean alt) {
		ArrayList<DisplayEntity> list = new ArrayList<>(getSelectedEntityList());
		for (DisplayEntity ent : list) {
			ent.handleKeyReleased(keyCode, keyChar, shift, control, alt);
		}
	}

	/**
	 * Returns the global coordinates corresponding to the specified raster position projected
	 * onto the x-y plane.
	 */
	public Vec3d getMousePosition(int windowID, int x, int y) {
		Ray currentRay = getRayForMouse(windowID, x, y);
		double dist = RenderManager.XY_PLANE.collisionDist(currentRay);
		if (dist == Double.POSITIVE_INFINITY)
			return null;
		return currentRay.getPointAtDist(dist);
	}

	/**
	 * Moves the entity to the last place at which the mouse was clicked.
	 * @param ent - entity to be positioned
	 */
	public void dragEntityToMousePosition(DisplayEntity ent) {
		CameraControl cam = windowControls.get(activeWindowID);
		if (cam == null)
			return;
		Vec3d pos = cam.getPOI();
		pos = ent.getLocalPosition(pos);
		Simulation simulation = ent.getSimulation();
		if (simulation.isSnapToGrid()) {
			pos = simulation.getSnapGridPosition(pos);
		}
		Renderer.WindowMouseInfo mouseInfo = renderer.getMouseInfo(activeWindowID);
		try {
			ent.dragged(mouseInfo.x, mouseInfo.y, pos);
		}
		catch (InputErrorException e) {}
	}

	public void setShowReferences(boolean bShow) {
		showReferences.set(bShow);
	}

	public void setShowLinks(boolean bShow) {
		showLinks.set(bShow);
	}

	public void setCreateLinks(boolean bCreate) {
		createLinks.set(bCreate);
	}

	public void setLinkDirection(boolean bool) {
		linkDirection.set(bool);
	}

	private void addLink(DirectedEntity sourceDE, DirectedEntity destDE, Color4d linkColour,
			double delta, ArrayList<RenderProxy> scene) {
		Vec3d source = sourceDE.getSourcePoint();
		Vec3d sink = destDE.getSinkPoint();
		double sourceRadius = sourceDE.entity.getRadius();
		double sinkRadius = destDE.entity.getRadius();

		// If the objects are too close, use the minimum radius values
		Vec3d arrowDir = new Vec3d();
		arrowDir.sub3(sink, source);
		double linkSize = arrowDir.mag3();
		if (linkSize - sourceRadius - sinkRadius < linkArrowSize) {
			sourceRadius = sourceDE.entity.getMinRadius();
			sinkRadius = destDE.entity.getMinRadius();
		}

		// Set the arrow head size for the region
		double arrowSize = linkArrowSize;
		if (destDE.entity.getCurrentRegion() != null)
			arrowSize *= destDE.entity.getCurrentRegion().getGlobalScale();

		addLink(source, sink, sourceRadius, sinkRadius, linkColour, arrowSize, delta, scene);
	}

	private void addLink(Vec3d source, Vec3d sink, double sourceRadius, double sinkRadius,
			Color4d linkColour, double arrowSize, double delta, ArrayList<RenderProxy> scene) {

		// Calculate the length of the arrow and a unit vector in its direction
		Vec3d arrowDir = new Vec3d();
		arrowDir.sub3(sink, source);
		double linkSize = arrowDir.mag3();
		arrowDir.normalize3();

		// Reduce the arrow length to allow for the radius values
		linkSize = linkSize - sourceRadius - sinkRadius;
		if (linkSize <= 0.0d)
			return;
		Vec3d temp = new Vec3d();
		temp.scale3(sourceRadius, arrowDir);
		Vec3d sourcePt = new Vec3d(source);
		sourcePt.add3(temp);
		temp.scale3(sinkRadius, arrowDir);
		Vec3d sinkPt = new Vec3d(sink);
		sinkPt.sub3(temp);

		// Reduce the arrow head size for a short arrow
		double arrowHeadSize = Math.min(linkSize*0.3, arrowSize);

		temp.scale3(arrowHeadSize, arrowDir);
		Vec3d arrowMidPoint = new Vec3d();
		arrowMidPoint.sub3(sinkPt, temp);
		Vec3d arrowHeadDir = new Vec3d();
		arrowHeadDir.cross3(arrowDir, new Vec3d(0,0,1));
		if (arrowHeadDir.mag3() == 0.0) {
			arrowHeadDir.set3(1, 0, 0);
		} else {
			arrowHeadDir.normalize3();
		}
		arrowHeadDir.scale3(arrowHeadSize*0.5);

		Vec3d arrowPoint0 = new Vec3d();
		Vec3d arrowPoint1 = new Vec3d();
		arrowPoint0.sub3(arrowMidPoint, arrowHeadDir);
		arrowPoint1.add3(arrowMidPoint, arrowHeadDir);

		Vec4d source4 = new Vec4d(sourcePt.x, sourcePt.y, sourcePt.z + delta, 1);
		Vec4d sink4 = new Vec4d(sinkPt.x, sinkPt.y, sinkPt.z + delta, 1);
		Vec4d ap0 = new Vec4d(arrowPoint0.x, arrowPoint0.y, arrowPoint0.z + delta, 1);
		Vec4d ap1 = new Vec4d(arrowPoint1.x, arrowPoint1.y, arrowPoint1.z + delta, 1);

		ArrayList<Vec4d> segments = new ArrayList<>(6);
		segments.add(source4);
		segments.add(sink4);

		// Now add the 'head' of the arrow
		segments.add(sink4);
		segments.add(ap0);

		segments.add(sink4);
		segments.add(ap1);

		scene.add(new LineProxy(segments, linkColour, 1, DisplayModel.ALWAYS, 0));
	}

	private void addLinkDisplays(JaamSimModel simModel, boolean dir, double delta,
			ArrayList<RenderProxy> scene) {
		Color4d linkColour = dir ? ColourInput.BLUE : ColourInput.RED;

		// Loop through the displayed objects in the model
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class)) {
			if (!ent.getShow())
				continue;
			DirectedEntity de = new DirectedEntity(ent, dir);

			// Destinations that receive entities from this object
			for (DirectedEntity dest : ent.getDestinationDirEnts(dir)) {
				if (!dest.entity.getShow())
					continue;
				addLink(de, dest, linkColour, delta, scene);
			}

			// Sources that direct entities to this object
			for (DirectedEntity source : ent.getSourceDirEnts(dir)) {
				if (!source.entity.getShow())
					continue;
				addLink(source, de, linkColour, delta, scene);
			}
		}
	}

	private void addEntityReferenceDisplays(JaamSimModel simModel, double delta, ArrayList<RenderProxy> scene) {

		// Loop through the displayed objects in the model
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class)) {
			if (!ent.getShow() || ent instanceof EntityLabel || ent instanceof OverlayEntity
					|| ent instanceof Region)
				continue;
			Vec3d sink = ent.getGlobalPosition();
			double sinkRadius = 2.0d * ent.getMinRadius();

			// Set the arrow head size for the region
			double arrowSize = 0.5d * linkArrowSize;
			if (ent.getCurrentRegion() != null)
				arrowSize *= ent.getCurrentRegion().getGlobalScale();

			// Find the sources and destinations for this entity
			ArrayList<DisplayEntity> srcList = new ArrayList<>(0);
			ArrayList<DisplayEntity> destList = new ArrayList<>(0);
			try {
				srcList = ent.getSourceEntities();
				destList = ent.getDestinationEntities();
			}
			catch (Exception e) {}

			// Loop through the entity's references, ignoring its sources and destinations
			for (Entity ref : ent.getEntityReferences()) {
				if (!(ref instanceof DisplayEntity) || !((DisplayEntity) ref).getShow()
						|| ref instanceof OverlayEntity || ref instanceof Region
						|| ref == ent
						|| destList.contains(ref) || srcList.contains(ref))
					continue;

				// If one or more entities are selected, show the arrows only for those entities
				if (!selectedEntityList.isEmpty() && !selectedEntityList.contains(ent)
						&& !selectedEntityList.contains(ref))
					continue;

				// Show an arrow for each reference
				Vec3d source = ((DisplayEntity) ref).getGlobalPosition();
				double sourceRadius = 0.0d;
				addLink(source, sink, sourceRadius, sinkRadius, REF_LINK_COLOUR, arrowSize, delta, scene);
			}
		}
	}

	public static void setDebugInfo(boolean showDebug) {
		if (!isGood()) {
			return;
		}
		s_instance.renderer.setDebugInfo(showDebug);
		GUIFrame.updateUI();
	}

}

