/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2016-2022 JaamSim Software Inc.
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
package com.jaamsim.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.Commands.Command;
import com.jaamsim.Commands.CoordinateCommand;
import com.jaamsim.Commands.DefineCommand;
import com.jaamsim.Commands.DefineViewCommand;
import com.jaamsim.Commands.DeleteCommand;
import com.jaamsim.Commands.KeywordCommand;
import com.jaamsim.Commands.RenameCommand;
import com.jaamsim.DisplayModels.TextModel;
import com.jaamsim.Graphics.BillboardText;
import com.jaamsim.Graphics.DirectedEntity;
import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.EntityLabel;
import com.jaamsim.Graphics.FillEntity;
import com.jaamsim.Graphics.LineEntity;
import com.jaamsim.Graphics.OverlayEntity;
import com.jaamsim.Graphics.OverlayText;
import com.jaamsim.Graphics.Region;
import com.jaamsim.Graphics.Text;
import com.jaamsim.Graphics.TextBasics;
import com.jaamsim.Graphics.TextEntity;
import com.jaamsim.Graphics.View;
import com.jaamsim.ProbabilityDistributions.RandomStreamUser;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.basicsim.GUIListener;
import com.jaamsim.basicsim.JaamSimModel;
import com.jaamsim.basicsim.RunManager;
import com.jaamsim.basicsim.Simulation;
import com.jaamsim.controllers.RateLimiter;
import com.jaamsim.controllers.RenderManager;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.events.EventManager;
import com.jaamsim.events.EventTimeListener;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.InputErrorException;
import com.jaamsim.input.KeywordIndex;
import com.jaamsim.input.Parser;
import com.jaamsim.math.Color4d;
import com.jaamsim.math.MathUtils;
import com.jaamsim.math.Vec3d;
import com.jaamsim.rng.MRG1999a;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;

/**
 * The main window for a Graphical Simulation.  It provides the controls for managing then
 * EventManager (run, pause, ...) and the graphics (zoom, pan, ...)
 */
public class GUIFrame extends OSFixJFrame implements EventTimeListener, GUIListener {
	private static GUIFrame instance;

	private static RunManager runManager;
	private static final ArrayList<RunManager> runManagerList = new ArrayList<>();
	private static final AtomicLong modelCount = new AtomicLong(0);  // number of JaamSimModels

	private final ArrayList<View> views = new ArrayList<>();
	private int nextViewID = 1;

	// global shutdown flag
	static private AtomicBoolean shuttingDown;

	private JMenu fileMenu;
	private JMenu editMenu;
	private JMenu toolsMenu;
	private JMenu viewsMenu;
	private JMenu optionMenu;
	private JMenu unitsMenu;
	private JMenu windowMenu;
	private JMenu helpMenu;
	private JToggleButton snapToGrid;
	private JToggleButton xyzAxis;
	private JToggleButton grid;
	private JCheckBoxMenuItem alwaysTop;
	private JCheckBoxMenuItem graphicsDebug;
	private JMenuItem printInputItem;
	private JMenuItem saveConfigurationMenuItem;  // "Save"
	private JLabel clockDisplay;
	private JLabel speedUpDisplay;
	private JLabel remainingDisplay;

	private JMenuItem undoMenuItem;
	private JMenuItem redoMenuItem;
	private JMenuItem copyMenuItem;
	private JMenuItem pasteMenuItem;
	private JMenuItem deleteMenuItem;

	private JToggleButton controlRealTime;
	private JSpinner spinner;

	private JButton fileSave;
	private JButton undo;
	private JButton redo;
	private JButton undoDropdown;
	private JButton redoDropdown;
	private final ArrayList<Command> undoList = new ArrayList<>();
	private final ArrayList<Command> redoList = new ArrayList<>();

	private JToggleButton showLabels;
	private JToggleButton showSubModels;
	private JToggleButton presentMode;

	private JToggleButton showReferences;
	private JToggleButton showLinks;
	private JToggleButton createLinks;
	private JButton nextButton;
	private JButton prevButton;
	private JToggleButton reverseButton;

	private JButton copyButton;
	private JButton pasteButton;
	private JToggleButton find;

	private JTextField dispModel;
	private JButton modelSelector;
	private JButton editDmButton;

	private JButton clearButton;

	private Entity selectedEntity;
	private ButtonGroup alignmentGroup;
	private JToggleButton alignLeft;
	private JToggleButton alignCentre;
	private JToggleButton alignRight;

	private JToggleButton bold;
	private JToggleButton italic;
	private JTextField font;
	private JButton fontSelector;
	private JTextField textHeight;
	private JButton largerText;
	private JButton smallerText;
	private ColorIcon colourIcon;
	private JButton fontColour;

	private JButton increaseZ;
	private JButton decreaseZ;

	private JToggleButton outline;
	private JSpinner lineWidth;
	private ColorIcon lineColourIcon;
	private JButton lineColour;

	private JToggleButton fill;
	private ColorIcon fillColourIcon;
	private JButton fillColour;

	private RoundToggleButton controlStartResume;
	private ImageIcon runPressedIcon;
	private ImageIcon pausePressedIcon;
	private RoundToggleButton controlStop;
	private JTextField pauseTime;
	private JTextField gridSpacing;

	private JLabel locatorPos;

	//JButton toolButtonIsometric;
	private JToggleButton lockViewXYPlane;

	private int lastValue = -1;
	private JProgressBar progressBar;
	private static ArrayList<Image> iconImages = new ArrayList<>();

	private static final RateLimiter rateLimiter;

	private static boolean SAFE_GRAPHICS;
	private static boolean OPTIONAL_GRAPHICS;

	private static File userManualFile;

	// Collection of default window parameters
	int DEFAULT_GUI_WIDTH;
	int COL1_WIDTH;
	int COL2_WIDTH;
	int COL3_WIDTH;
	int COL4_WIDTH;
	int COL1_START;
	int COL2_START;
	int COL3_START;
	int COL4_START;
	int HALF_TOP;
	int HALF_BOTTOM;
	int TOP_START;
	int BOTTOM_START;
	int LOWER_HEIGHT;
	int LOWER_START;
	int VIEW_HEIGHT;
	int VIEW_WIDTH;

	int VIEW_OFFSET = 50;

	private static final String DEFAULT_MODEL_NAME = "Model";

	private static final String LAST_USED_FOLDER = "";
	private static final String LAST_USED_3D_FOLDER = "3D_FOLDER";
	private static final String LAST_USED_IMAGE_FOLDER = "IMAGE_FOLDER";

	private static final String RUN_TOOLTIP = GUIFrame.formatToolTip("Run (space key)", "Starts or resumes the simulation run.");
	private static final String PAUSE_TOOLTIP = "<html><b>Pause</b></html>";  // Use a small tooltip for Pause so that it does not block the simulation time display

	static {
		try {
			if (OSFix.isWindows())
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			else
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
		}
		catch (Exception e) {
			LogBox.logLine("Unable to change look and feel.");
		}

		try {
			iconImages.clear();
			Toolkit toolkit = Toolkit.getDefaultToolkit();
			iconImages.add(toolkit.getImage(GUIFrame.class.getResource("/resources/images/icon-16.png")));
			iconImages.add(toolkit.getImage(GUIFrame.class.getResource("/resources/images/icon-32.png")));
			iconImages.add(toolkit.getImage(GUIFrame.class.getResource("/resources/images/icon-64.png")));
			iconImages.add(toolkit.getImage(GUIFrame.class.getResource("/resources/images/icon-128.png")));
		}
		catch (Exception e) {
			LogBox.logLine("Unable to load icon file.");
		}

		shuttingDown = new AtomicBoolean(false);
		rateLimiter = RateLimiter.create(60);
	}

	private GUIFrame() {
		super();

		getContentPane().setLayout( new BorderLayout() );
		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );

		// Initialize the working environment
		initializeMenus();
		initializeButtonBar();
		initializeMainToolBars();

		this.setIconImages(GUIFrame.getWindowIcons());

		//Set window size
		setResizable( true );  //FIXME should be false, but this causes the window to be sized
		                       //      and positioned incorrectly in the Windows 7 Aero theme
		pack();

		controlStartResume.requestFocusInWindow();

		controlStartResume.setSelected( false );
		controlStartResume.setEnabled( false );
		controlStop.setSelected( false );
		controlStop.setEnabled( false );
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled( false );
		JPopupMenu.setDefaultLightWeightPopupEnabled( false );

		this.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				close();
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				showWindows();
			}

			@Override
			public void windowIconified(WindowEvent e) {
				updateUI();
			}

			@Override
			public void windowActivated(WindowEvent e) {
				showWindows();
			}
		});

		addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				JaamSimModel sim = getJaamSimModel();
				if (sim.getSimulation() == null)
					return;
				sim.getSimulation().setControlPanelWidth(getSize().width);
			}

			@Override
			public void componentMoved(ComponentEvent e) {
				JaamSimModel sim = getJaamSimModel();
				Simulation simulation = sim.getSimulation();
				if (simulation == null)
					return;
				windowOffset = new Point(getLocation().x - initLocation.x,
						getLocation().y - initLocation.y);
				updateToolLocations(simulation);
				updateViewLocations();
			}
		});
	}

	private Point windowOffset = new Point();
	private Point initLocation = new Point(getX(), getY()); // bypass the OSFix correction

	public Point getRelativeLocation(int x, int y) {
		return new Point(x - windowOffset.x, y - windowOffset.y);
	}

	public Point getGlobalLocation(int x, int y) {
		return new Point(x + windowOffset.x, y + windowOffset.y);
	}

	public static JaamSimModel getJaamSimModel() {
		if (runManager == null)
			return null;
		return runManager.getJaamSimModel();
	}

	public static RunManager getRunManager() {
		return runManager;
	}

	/**
	 * Sets the model to be displayed by the user interface.
	 * @param mgr - run manager to be displayed
	 */
	public static void setRunManager(RunManager mgr) {
		JaamSimModel sm = mgr.getJaamSimModel();
		if (mgr == runManager)
			return;

		// Add the new model to the list of models
		if (!runManagerList.contains(mgr))
			runManagerList.add(mgr);

		// Remove the previous model if it is unedited and unsaved
		JaamSimModel sim = getJaamSimModel();
		if (sim != null && sim.getConfigFile() == null && !sim.isSessionEdited()
				&& sim.getName().startsWith(DEFAULT_MODEL_NAME))
			runManagerList.remove(runManager);

		// Clear the listeners for the previous model
		if (sim != null) {
			sim.setGUIListener(null);
		}

		// Delete the run progress window, if open
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().dispose();

		runManager = mgr;

		GUIFrame gui = getInstance();
		if (gui == null)
			return;
		RenderManager.clear();
		EntityPallet.update();
		ObjectSelector.allowUpdate();
		gui.resetViews();
		gui.setTitle(sm);
		gui.clearButtons();
		gui.clearUndoRedo();

		// Set the listeners for the new model
		sm.setGUIListener(gui);

		// Pass the simulation time for the new model to the user interface
		gui.initSpeedUp(sm.getSimTime());
		gui.tickUpdate(sm.getSimTicks());
		gui.updateForSimulationState(sm.getSimState());
	}

	public void setTitle(JaamSimModel sm) {
		setTitle(sm, 0);
	}

	public void setTitle(JaamSimModel sm, int val) {
		String str = "JaamSim";
		if (sm.getSimulation() != null)
			str = sm.getSimulation().getModelName();
		StringBuilder sb = new StringBuilder();
		if (val > 0)
			sb.append(val).append("% ");
		sb.append(sm.getName()).append(" - ").append(str);
		setTitle(sb.toString());
	}

	private static JaamSimModel getNextJaamSimModel() {
		long num = modelCount.incrementAndGet();
		return new JaamSimModel(DEFAULT_MODEL_NAME + num);
	}

	@Override
	public Dimension getPreferredSize() {
		Point fix = OSFix.getSizeAdustment();
		return new Dimension(DEFAULT_GUI_WIDTH + fix.x, super.getPreferredSize().height);
	}

	public static synchronized GUIFrame getInstance() {
		return instance;
	}

	private static synchronized GUIFrame createInstance() {
		instance = new GUIFrame();
		UIUpdater updater = new UIUpdater(instance);
		GUIFrame.registerCallback(new Runnable() {
			@Override
			public void run() {
				SwingUtilities.invokeLater(updater);
			}
		});
		return instance;
	}

	public static final void registerCallback(Runnable r) {
		rateLimiter.registerCallback(r);
	}

	public static final void updateUI() {
		rateLimiter.queueUpdate();
	}

	public void showWindows() {
		if (!RenderManager.isGood())
			return;

		// Identity the view window that is active
		View activeView = RenderManager.inst().getActiveView();

		// Re-open the view windows
		for (int i = 0; i < views.size(); i++) {
			View v = views.get(i);
			if (v != null && v.showWindow() && v != activeView)
				RenderManager.inst().createWindow(v);
		}

		// Re-open the active view window last
		if (activeView != null)
			RenderManager.inst().createWindow(activeView);

		// Re-open the tools
		showActiveTools(getJaamSimModel().getSimulation());
		updateUI();
	}

	/**
	 * Perform exit window duties
	 */
	void close() {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		JaamSimModel sim = getJaamSimModel();
		// check for unsaved changes
		if (sim.isSessionEdited()) {
			boolean confirmed = GUIFrame.showSaveChangesDialog(this);
			if (!confirmed) {
				return;
			}
		}
		runManagerList.remove(runManager);
		runManager.close();
		if (runManagerList.isEmpty())
			GUIFrame.shutdown(0);
		setRunManager(runManagerList.get(0));
		FrameBox.setSelectedEntity(sim.getSimulation(), false);
	}

	/**
	 * Sets up the Control Panel's menu bar.
	 */
	private void initializeMenus() {

		// Set up the individual menus
		this.initializeFileMenu();
		this.initializeEditMenu();
		this.initializeToolsMenu();
		this.initializeViewsMenu();
		this.initializeOptionsMenu();
		this.initializeUnitsMenu();
		this.initializeWindowMenu();
		this.initializeHelpMenu();

		// Add the individual menu to the main menu
		JMenuBar mainMenuBar = new JMenuBar();
		mainMenuBar.add( fileMenu );
		mainMenuBar.add( editMenu );
		mainMenuBar.add( toolsMenu );
		mainMenuBar.add( viewsMenu );
		mainMenuBar.add( optionMenu );
		mainMenuBar.add( unitsMenu );
		mainMenuBar.add( windowMenu );
		mainMenuBar.add( helpMenu );

		// Add main menu to the window
		setJMenuBar( mainMenuBar );
	}

	// ******************************************************************************************************
	// MENU BAR
	// ******************************************************************************************************

	/**
	 * Sets up the File menu in the Control Panel's menu bar.
	 */
	private void initializeFileMenu() {

		// File menu creation
		fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic(KeyEvent.VK_F);
		fileMenu.setEnabled( false );

		// 1) "New" menu item
		JMenuItem newMenuItem = new JMenuItem( "New" );
		newMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/New-16.png")) );
		newMenuItem.setMnemonic(KeyEvent.VK_N);
		newMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
		newMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.newModel();
			}
		} );
		fileMenu.add( newMenuItem );

		// 2) "Open" menu item
		JMenuItem configMenuItem = new JMenuItem( "Open..." );
		configMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Open-16.png")) );
		configMenuItem.setMnemonic(KeyEvent.VK_O);
		configMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
		configMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.load();
			}
		} );
		fileMenu.add( configMenuItem );

		// 3) "Save" menu item
		saveConfigurationMenuItem = new JMenuItem( "Save" );
		saveConfigurationMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Save-16.png")) );
		saveConfigurationMenuItem.setMnemonic(KeyEvent.VK_S);
		saveConfigurationMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		saveConfigurationMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.save();
			}
		} );
		fileMenu.add( saveConfigurationMenuItem );

		// 4) "Save As..." menu item
		JMenuItem saveConfigurationAsMenuItem = new JMenuItem( "Save As..." );
		saveConfigurationAsMenuItem.setMnemonic(KeyEvent.VK_V);
		saveConfigurationAsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.ALT_MASK + ActionEvent.CTRL_MASK));
		saveConfigurationAsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.saveAs();

			}
		} );
		fileMenu.add( saveConfigurationAsMenuItem );
		fileMenu.addSeparator();

		// 5) "Import..." menu item
		JMenu importGraphicsMenuItem = new JMenu( "Import..." );
		importGraphicsMenuItem.setMnemonic(KeyEvent.VK_I);

		JMenuItem importImages = new JMenuItem( "Images..." );
		importImages.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntityFactory.importImages(GUIFrame.this);
			}
		} );
		importGraphicsMenuItem.add( importImages );

		JMenuItem import3D = new JMenuItem( "3D Assets..." );
		import3D.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				DisplayEntityFactory.import3D(GUIFrame.this);
			}
		} );
		importGraphicsMenuItem.add( import3D );

		fileMenu.add( importGraphicsMenuItem );

		// 6) "Print Input Report" menu item
		printInputItem = new JMenuItem( "Print Input Report" );
		printInputItem.setMnemonic(KeyEvent.VK_I);
		printInputItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				InputAgent.printInputFileKeywords(sim);
			}
		} );
		fileMenu.add( printInputItem );

		// 7) "Exit" menu item
		JMenuItem exitMenuItem = new JMenuItem( "Exit" );
		exitMenuItem.setMnemonic(KeyEvent.VK_X);
		exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
		exitMenuItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				close();
			}
		});
		fileMenu.addSeparator();
		fileMenu.add( exitMenuItem );
	}

	/**
	 * Sets up the Edit menu in the Control Panel's menu bar.
	 */
	private void initializeEditMenu() {

		// Edit menu creation
		editMenu = new JMenu( "Edit" );
		editMenu.setMnemonic(KeyEvent.VK_E);

		// 1) "Undo" menu item
		undoMenuItem = new JMenuItem("Undo");
		undoMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Undo-16.png")) );
		undoMenuItem.setMnemonic(KeyEvent.VK_U);
		undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_Z, ActionEvent.CTRL_MASK));
		undoMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				undo();
			}
		} );
		editMenu.add( undoMenuItem );

		// 2) "Redo" menu item
		redoMenuItem = new JMenuItem("Redo");
		redoMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Redo-16.png")) );
		redoMenuItem.setMnemonic(KeyEvent.VK_R);
		redoMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		redoMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				redo();
			}
		} );
		editMenu.add( redoMenuItem );
		editMenu.addSeparator();

		// 3) "Copy" menu item
		copyMenuItem = new JMenuItem("Copy");
		copyMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Copy-16.png")) );
		copyMenuItem.setMnemonic(KeyEvent.VK_C);
		copyMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_C, ActionEvent.CTRL_MASK));
		copyMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (selectedEntity == null)
					return;
				copyToClipboard(selectedEntity);
			}
		} );
		editMenu.add( copyMenuItem );

		// 4) "Paste" menu item
		pasteMenuItem = new JMenuItem("Paste");
		pasteMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Paste-16.png")) );
		pasteMenuItem.setMnemonic(KeyEvent.VK_P);
		pasteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_V, ActionEvent.CTRL_MASK));
		pasteMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				pasteEntityFromClipboard();
			}
		} );
		editMenu.add( pasteMenuItem );

		// 5) "Delete" menu item
		deleteMenuItem = new JMenuItem("Delete");
		deleteMenuItem.setMnemonic(KeyEvent.VK_D);
		deleteMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_DELETE, 0));
		deleteMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (selectedEntity == null)
					return;
				try {
					deleteEntity(selectedEntity);
					FrameBox.setSelectedEntity(null, false);
				}
				catch (ErrorException e) {
					GUIFrame.showErrorDialog("User Error", e.getMessage());
				}
			}
		} );
		editMenu.add( deleteMenuItem );
		editMenu.addSeparator();

		// 6) "Find" menu item
		JMenuItem findMenuItem = new JMenuItem("Find");
		findMenuItem.setIcon( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Find-16.png")) );
		findMenuItem.setMnemonic(KeyEvent.VK_F);
		findMenuItem.setAccelerator(KeyStroke.getKeyStroke(
		        KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		findMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				FindBox.getInstance().showDialog();
			}
		} );
		editMenu.add( findMenuItem );
	}

	/**
	 * Sets up the Tools menu in the Control Panel's menu bar.
	 */
	private void initializeToolsMenu() {

		// Tools menu creation
		toolsMenu = new JMenu( "Tools" );
		toolsMenu.setMnemonic(KeyEvent.VK_T);

		// 1) "Show Basic Tools" menu item
		JMenuItem showBasicToolsMenuItem = new JMenuItem( "Show Basic Tools" );
		showBasicToolsMenuItem.setMnemonic(KeyEvent.VK_B);
		showBasicToolsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				clearPresentationMode();
				EntityPallet.getInstance().toFront();
				ObjectSelector.getInstance().toFront();
				EditBox.getInstance().toFront();
				OutputBox.getInstance().toFront();
				KeywordIndex[] kws = new KeywordIndex[4];
				kws[0] = InputAgent.formatBoolean("ShowModelBuilder", true);
				kws[1] = InputAgent.formatBoolean("ShowObjectSelector", true);
				kws[2] = InputAgent.formatBoolean("ShowInputEditor", true);
				kws[3] = InputAgent.formatBoolean("ShowOutputViewer", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kws));
			}
		} );
		toolsMenu.add( showBasicToolsMenuItem );

		// 2) "Close All Tools" menu item
		JMenuItem closeAllToolsMenuItem = new JMenuItem( "Close All Tools" );
		closeAllToolsMenuItem.setMnemonic(KeyEvent.VK_C);
		closeAllToolsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				KeywordIndex[] kws = new KeywordIndex[7];
				kws[0] = InputAgent.formatBoolean("ShowModelBuilder", false);
				kws[1] = InputAgent.formatBoolean("ShowObjectSelector", false);
				kws[2] = InputAgent.formatBoolean("ShowInputEditor", false);
				kws[3] = InputAgent.formatBoolean("ShowOutputViewer", false);
				kws[4] = InputAgent.formatBoolean("ShowPropertyViewer", false);
				kws[5] = InputAgent.formatBoolean("ShowLogViewer", false);
				kws[6] = InputAgent.formatBoolean("ShowEventViewer", false);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kws));
			}
		} );
		toolsMenu.add( closeAllToolsMenuItem );

		// 3) "Model Builder" menu item
		JMenuItem objectPalletMenuItem = new JMenuItem( "Model Builder" );
		objectPalletMenuItem.setMnemonic(KeyEvent.VK_O);
		objectPalletMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				EntityPallet.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowModelBuilder", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.addSeparator();
		toolsMenu.add( objectPalletMenuItem );

		// 4) "Object Selector" menu item
		JMenuItem objectSelectorMenuItem = new JMenuItem( "Object Selector" );
		objectSelectorMenuItem.setMnemonic(KeyEvent.VK_S);
		objectSelectorMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				ObjectSelector.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowObjectSelector", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( objectSelectorMenuItem );

		// 5) "Input Editor" menu item
		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.setMnemonic(KeyEvent.VK_I);
		inputEditorMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				EditBox.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowInputEditor", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( inputEditorMenuItem );

		// 6) "Output Viewer" menu item
		JMenuItem outputMenuItem = new JMenuItem( "Output Viewer" );
		outputMenuItem.setMnemonic(KeyEvent.VK_U);
		outputMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				OutputBox.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowOutputViewer", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( outputMenuItem );

		// 7) "Property Viewer" menu item
		JMenuItem propertiesMenuItem = new JMenuItem( "Property Viewer" );
		propertiesMenuItem.setMnemonic(KeyEvent.VK_P);
		propertiesMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				PropertyBox.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowPropertyViewer", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( propertiesMenuItem );

		// 8) "Log Viewer" menu item
		JMenuItem logMenuItem = new JMenuItem( "Log Viewer" );
		logMenuItem.setMnemonic(KeyEvent.VK_L);
		logMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				LogBox.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowLogViewer", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( logMenuItem );

		// 9) "Event Viewer" menu item
		JMenuItem eventsMenuItem = new JMenuItem( "Event Viewer" );
		eventsMenuItem.setMnemonic(KeyEvent.VK_E);
		eventsMenuItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				clearPresentationMode();
				EventViewer.getInstance().toFront();
				KeywordIndex kw = InputAgent.formatBoolean("ShowEventViewer", true);
				JaamSimModel sim = getJaamSimModel();
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
			}
		} );
		toolsMenu.add( eventsMenuItem );

		// 10) "Reset Positions and Sizes" menu item
		JMenuItem resetItem = new JMenuItem( "Reset Positions and Sizes" );
		resetItem.setMnemonic(KeyEvent.VK_R);
		resetItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				clearPresentationMode();
				JaamSimModel sim = getJaamSimModel();
				sim.getSimulation().resetWindowPositionsAndSizes();
			}
		} );
		toolsMenu.addSeparator();
		toolsMenu.add( resetItem );
	}

	/**
	 * Sets up the Views menu in the Control Panel's menu bar.
	 */
	private void initializeViewsMenu() {
		viewsMenu = new JMenu("Views");
		viewsMenu.setMnemonic(KeyEvent.VK_V);
		viewsMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {

				// 1) Select from the available view windows
				for (View view : getInstance().getViews()) {
					JMenuItem item = new JMenuItem(view.getName());
					item.addActionListener(new ActionListener() {
						@Override
						public void actionPerformed(ActionEvent e) {
							if (!RenderManager.isGood()) {
								if (RenderManager.canInitialize()) {
									RenderManager.initialize(SAFE_GRAPHICS);
								} else {
									// A fatal error has occurred, don't try to initialize again
									return;
								}
							}
							KeywordIndex kw = InputAgent.formatBoolean("ShowWindow", true);
							InputAgent.storeAndExecute(new KeywordCommand(view, kw));
							FrameBox.setSelectedEntity(view, false);
						}
					});
					viewsMenu.add(item);
				}

				// 2) "Define New View" menu item
				JMenuItem defineItem = new JMenuItem("Define New View");
				defineItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(ActionEvent e) {
						if (!RenderManager.isGood()) {
							if (RenderManager.canInitialize()) {
								RenderManager.initialize(SAFE_GRAPHICS);
							} else {
								// A fatal error has occurred, don't try to initialize again
								return;
							}
						}

						JaamSimModel sim = getJaamSimModel();
						String name = InputAgent.getUniqueName(sim, "View", "");
						IntegerVector winPos = null;
						Vec3d pos = null;
						Vec3d center = null;
						ArrayList<View> viewList = getInstance().getViews();
						if (!viewList.isEmpty()) {
							View lastView = viewList.get(viewList.size() - 1);
							winPos = (IntegerVector) lastView.getInput("WindowPosition").getValue();
							winPos = new IntegerVector(winPos);
							winPos.set(0, winPos.get(0) + VIEW_OFFSET);
							pos = lastView.getViewPosition();
							center = lastView.getViewCenter();
						}
						InputAgent.storeAndExecute(new DefineViewCommand(sim, name, pos, center, winPos));
					}
				});
				viewsMenu.addSeparator();
				viewsMenu.add(defineItem);

				// 3) "Reset Positions and Sizes" menu item
				JMenuItem resetItem = new JMenuItem( "Reset Positions and Sizes" );
				resetItem.setMnemonic(KeyEvent.VK_R);
				resetItem.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed( ActionEvent e ) {
						for (View v : getInstance().getViews()) {
							KeywordIndex posKw = InputAgent.formatArgs("WindowPosition");
							KeywordIndex sizeKw = InputAgent.formatArgs("WindowSize");
							InputAgent.storeAndExecute(new KeywordCommand(v, posKw, sizeKw));
						}
					}
				} );
				viewsMenu.addSeparator();
				viewsMenu.add(resetItem);
			}

			@Override
			public void menuCanceled(MenuEvent arg0) {
			}

			@Override
			public void menuDeselected(MenuEvent arg0) {
				viewsMenu.removeAll();
			}
		});
	}

	/**
	 * Sets up the Options menu in the Control Panel's menu bar.
	 */
	private void initializeOptionsMenu() {

		optionMenu = new JMenu( "Options" );
		optionMenu.setMnemonic(KeyEvent.VK_O);

		// 1) "Always on top" check box
		alwaysTop = new JCheckBoxMenuItem( "Always on top", false );
		alwaysTop.setMnemonic(KeyEvent.VK_A);
		optionMenu.add( alwaysTop );
		alwaysTop.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				if( GUIFrame.this.isAlwaysOnTop() ) {
					GUIFrame.this.setAlwaysOnTop( false );
				}
				else {
					GUIFrame.this.setAlwaysOnTop( true );
				}
			}
		} );

		// 2) "Graphics Debug Info" check box
		graphicsDebug = new JCheckBoxMenuItem( "Graphics Debug Info", false );
		graphicsDebug.setMnemonic(KeyEvent.VK_D);
		optionMenu.add( graphicsDebug );
		graphicsDebug.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				RenderManager.setDebugInfo(graphicsDebug.getState());
			}
		});
	}

	/**
	 * Sets up the Units menu in the Control Panel's menu bar.
	 */
	private void initializeUnitsMenu() {

		unitsMenu = new JMenu( "Units" );
		unitsMenu.setMnemonic(KeyEvent.VK_U);

		unitsMenu.addMenuListener( new MenuListener() {

			@Override
			public void menuCanceled(MenuEvent arg0) {}

			@Override
			public void menuDeselected(MenuEvent arg0) {
				unitsMenu.removeAll();
			}

			@Override
			public void menuSelected(MenuEvent arg0) {
				UnitsSelector.populateMenu(unitsMenu);
				unitsMenu.setVisible(true);
			}
		});
	}

	/**
	 * Sets up the Windows menu in the Control Panel's menu bar.
	 */
	private void initializeWindowMenu() {
		windowMenu = new JMenu( "Window" );
		windowMenu.setMnemonic(KeyEvent.VK_W);
		windowMenu.addMenuListener(new MenuListener() {

			@Override
			public void menuSelected(MenuEvent e) {
				for (RunManager mgr : runManagerList) {
					JaamSimModel sm = mgr.getJaamSimModel();
					JRadioButtonMenuItem item = new JRadioButtonMenuItem(sm.getName());
					if (mgr == runManager)
						item.setSelected(true);
					item.addActionListener( new ActionListener() {

						@Override
						public void actionPerformed( ActionEvent event ) {
							setRunManager(mgr);
							FrameBox.setSelectedEntity(sm.getSimulation(), false);
						}

					} );
					windowMenu.add( item );
				}
			}

			@Override
			public void menuCanceled(MenuEvent arg0) {
			}

			@Override
			public void menuDeselected(MenuEvent arg0) {
				windowMenu.removeAll();
			}
		});
	}

	/**
	 * Sets up the Help menu in the Control Panel's menu bar.
	 */
	private void initializeHelpMenu() {

		// Help menu creation
		helpMenu = new JMenu( "Help" );
		helpMenu.setMnemonic(KeyEvent.VK_H);

		// 1) "About" menu item
		JMenuItem aboutMenu = new JMenuItem( "About" );
		aboutMenu.setMnemonic(KeyEvent.VK_A);
		aboutMenu.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				AboutBox about = new AboutBox();
				about.setLocationRelativeTo(null);
				about.setVisible(true);
			}
		} );
		helpMenu.add( aboutMenu );

		// 2) "Help" menu item
		JMenuItem helpItem = new JMenuItem( "Help" );
		helpItem.setMnemonic(KeyEvent.VK_H);
		helpItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
		helpItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				String topic = "";
				if (selectedEntity != null)
					topic = selectedEntity.getObjectType().getName();
				HelpBox.getInstance().showDialog(topic);
			}
		} );
		helpMenu.add( helpItem );

		// 3) "Examples" menu item
		JMenuItem exampleItem = new JMenuItem( "Examples" );
		exampleItem.setMnemonic(KeyEvent.VK_E);
		exampleItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				String topic = "";
				if (selectedEntity != null)
					topic = selectedEntity.getObjectType().getName();
				ExampleBox.getInstance().showDialog(topic);
			}
		} );
		helpMenu.add( exampleItem );

		// 4) "User Manual" menu item
		JMenuItem userManualItem = new JMenuItem( "User Manual" );
		userManualItem.setMnemonic(KeyEvent.VK_U);
		userManualItem.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!Desktop.isDesktopSupported()) {
					GUIFrame.showErrorDialog("File Error", "Cannot open the User Manual file");
					return;
				}
				try {
					if (userManualFile == null) {
						InputStream is = GUIFrame.class.getResourceAsStream("/resources/documents/JaamSim User Manual.pdf");
						userManualFile = File.createTempFile("JaamSim-", ".pdf");
						userManualFile.deleteOnExit();
						Files.copy(is, userManualFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
					}
					Desktop.getDesktop().open(userManualFile);
				}
				catch (Exception e) {
					GUIFrame.showErrorDialog("File Error", "Cannot open the User Manual file");
				}
			}
		} );
		helpMenu.add( userManualItem );
	}

	/**
	 * Returns the pixel length of the string with specified font
	 */
	private static int getPixelWidthOfString_ForFont(String str, Font font) {
		FontMetrics metrics = new FontMetrics(font) {};
		Rectangle2D bounds = metrics.getStringBounds(str, null);
		return (int)bounds.getWidth();
	}

	// ******************************************************************************************************
	// BUTTON BAR
	// ******************************************************************************************************

	/**
	 * Sets up the Control Panel's button bar.
	 */
	public void initializeButtonBar() {

		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );
		Dimension separatorDim = new Dimension(11, 20);
		Dimension gapDim = new Dimension(5, separatorDim.height);

		JToolBar buttonBar = new JToolBar();
		buttonBar.setMargin( smallMargin );
		buttonBar.setFloatable(false);
		buttonBar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );

		getContentPane().add( buttonBar, BorderLayout.NORTH );

		// File new, open, and save buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addFileNewButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addFileOpenButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addFileSaveButton(buttonBar, noMargin);

		// Undo and redo buttons
		buttonBar.addSeparator(separatorDim);
		addUndoButtons(buttonBar, noMargin);

		// Presentation Mode button
		buttonBar.addSeparator(separatorDim);
		addPresentationModeButton(buttonBar, noMargin);

		// 2D, axes, and grid buttons
		buttonBar.addSeparator(separatorDim);
		add2dButton(buttonBar, smallMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addShowAxesButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addShowGridButton(buttonBar, noMargin);

		// Show labels button
		buttonBar.add(Box.createRigidArea(gapDim));
		addShowLabelsButton(buttonBar, noMargin);

		// Show sub-models button
		buttonBar.add(Box.createRigidArea(gapDim));
		addShowSubModelsButton(buttonBar, noMargin);

		// Snap-to-grid button and field
		buttonBar.addSeparator(separatorDim);
		addSnapToGridButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addSnapToGridField(buttonBar, noMargin);

		// Show and create links buttons
		buttonBar.addSeparator(separatorDim);
		addShowReferencesButton(buttonBar, noMargin);
		buttonBar.add(Box.createRigidArea(gapDim));
		addShowLinksButton(buttonBar, noMargin);

		buttonBar.add(Box.createRigidArea(gapDim));
		addCreateLinksButton(buttonBar, noMargin);

		// Previous and Next buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addPreviousButton(buttonBar, noMargin);
		buttonBar.add(Box.createRigidArea(gapDim));
		addNextButton(buttonBar, noMargin);
		buttonBar.add(Box.createRigidArea(gapDim));
		addReverseButton(buttonBar, noMargin);

		// Show Copy and Paste buttons
		buttonBar.addSeparator(separatorDim);
		addCopyButton(buttonBar, noMargin);
		addPasteButton(buttonBar, noMargin);

		// Show Entity Finder button
		buttonBar.add(Box.createRigidArea(gapDim));
		addEntityFinderButton(buttonBar, noMargin);

		// DisplayModel field and button
		buttonBar.addSeparator(separatorDim);
		addDisplayModelSelector(buttonBar, noMargin);
		buttonBar.add(Box.createRigidArea(gapDim));
		addEditDisplayModelButton(buttonBar, noMargin);

		// Clear formatting button
		buttonBar.add(Box.createRigidArea(gapDim));
		addClearFormattingButton(buttonBar, noMargin);

		// Font selector and text height field
		buttonBar.addSeparator(separatorDim);
		addFontSelector(buttonBar, noMargin);
		buttonBar.add(Box.createRigidArea(gapDim));
		addTextHeightField(buttonBar, noMargin);

		// Larger and smaller text height buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addTextHeightButtons(buttonBar, noMargin);

		// Bold and Italic buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addFontStyleButtons(buttonBar, noMargin);

		// Font colour button
		buttonBar.add(Box.createRigidArea(gapDim));
		addFontColourButton(buttonBar, noMargin);

		// Text alignment buttons
		buttonBar.add(Box.createRigidArea(gapDim));
		addTextAlignmentButtons(buttonBar, noMargin);

		// Z-coordinate buttons
		buttonBar.addSeparator(separatorDim);
		addZButtons(buttonBar, noMargin);

		// Line buttons
		buttonBar.addSeparator(separatorDim);
		addOutlineButton(buttonBar, noMargin);
		addLineWidthSpinner(buttonBar, noMargin);
		addLineColourButton(buttonBar, noMargin);

		// Fill buttons
		buttonBar.addSeparator(separatorDim);
		addFillButton(buttonBar, noMargin);
		addFillColourButton(buttonBar, noMargin);
	}

	private void addFileNewButton(JToolBar buttonBar, Insets margin) {
		JButton fileNew = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/New-16.png")) );
		fileNew.setMargin(margin);
		fileNew.setFocusPainted(false);
		fileNew.setToolTipText(formatToolTip("New (Ctrl+N)", "Starts a new model."));
		fileNew.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.newModel();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( fileNew );
	}

	private void addFileOpenButton(JToolBar buttonBar, Insets margin) {
		JButton fileOpen = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Open-16.png")) );
		fileOpen.setMargin(margin);
		fileOpen.setFocusPainted(false);
		fileOpen.setToolTipText(formatToolTip("Open... (Ctrl+O)", "Opens a model."));
		fileOpen.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.load();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( fileOpen );
	}

	private void addFileSaveButton(JToolBar buttonBar, Insets margin) {
		fileSave = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Save-16.png")) );
		fileSave.setMargin(margin);
		fileSave.setFocusPainted(false);
		fileSave.setToolTipText(formatToolTip("Save (Ctrl+S)", "Saves the present model."));
		fileSave.setEnabled(false);
		fileSave.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				GUIFrame.this.save();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( fileSave );
	}

	private void addUndoButtons(JToolBar buttonBar, Insets margin) {

		// Undo button
		undo = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Undo-16.png")) );
		undo.setMargin(margin);
		undo.setFocusPainted(false);
		undo.setRequestFocusEnabled(false);
		undo.setToolTipText(formatToolTip("Undo (Ctrl+Z)", "Reverses the last change to the model."));
		undo.setEnabled(!undoList.isEmpty());
		undo.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				undo();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( undo );

		// Undo Dropdown Menu
		undoDropdown = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		undoDropdown.setMargin(margin);
		undoDropdown.setFocusPainted(false);
		undoDropdown.setRequestFocusEnabled(false);
		undoDropdown.setEnabled(!undoList.isEmpty());
		undoDropdown.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ScrollablePopupMenu menu = new ScrollablePopupMenu("UndoMenu");
				synchronized (undoList) {
					for (int i = 1; i <= undoList.size(); i++) {
						Command cmd = undoList.get(undoList.size() - i);
						final int num = i;
						JMenuItem item = new JMenuItem(cmd.toString());
						item.addActionListener( new ActionListener() {

							@Override
							public void actionPerformed( ActionEvent event ) {
								undo(num);
								controlStartResume.requestFocusInWindow();
							}
						} );
						menu.add(item);
					}
					menu.show(undoDropdown, 0, undoDropdown.getHeight());
				}
			}
		} );
		buttonBar.add( undoDropdown );

		// Redo button
		redo = new JButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Redo-16.png")) );
		redo.setMargin(margin);
		redo.setFocusPainted(false);
		redo.setRequestFocusEnabled(false);
		redo.setToolTipText(formatToolTip("Redo (Ctrl+Y)", "Re-performs the last change to the model that was undone."));
		redo.setEnabled(!redoList.isEmpty());
		redo.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				redo();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( redo );

		// Redo Dropdown Menu
		redoDropdown = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		redoDropdown.setMargin(margin);
		redoDropdown.setFocusPainted(false);
		redoDropdown.setRequestFocusEnabled(false);
		redoDropdown.setEnabled(!redoList.isEmpty());
		redoDropdown.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				ScrollablePopupMenu menu = new ScrollablePopupMenu("RedoMenu");
				synchronized (undoList) {
					for (int i = 1; i <= redoList.size(); i++) {
						Command cmd = redoList.get(redoList.size() - i);
						final int num = i;
						JMenuItem item = new JMenuItem(cmd.toString());
						item.addActionListener( new ActionListener() {

							@Override
							public void actionPerformed( ActionEvent event ) {
								redo(num);
								controlStartResume.requestFocusInWindow();
							}
						} );
						menu.add(item);
					}
					menu.show(redoDropdown, 0, redoDropdown.getHeight());
				}
			}
		} );
		buttonBar.add( redoDropdown );
	}

	private void add2dButton(JToolBar buttonBar, Insets margin) {
		lockViewXYPlane = new JToggleButton( "2D" );
		lockViewXYPlane.setMargin(margin);
		lockViewXYPlane.setFocusPainted(false);
		lockViewXYPlane.setRequestFocusEnabled(false);
		lockViewXYPlane.setToolTipText(formatToolTip("2D View",
				"Sets the camera position to show a bird's eye view of the 3D scene."));
		lockViewXYPlane.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				boolean bLock2D = lockViewXYPlane.isSelected();

				if (RenderManager.isGood()) {
					View currentView = RenderManager.inst().getActiveView();
					if (currentView != null) {
						currentView.setLock2D(bLock2D);
					}
				}
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( lockViewXYPlane );
	}

	private void addShowAxesButton(JToolBar buttonBar, Insets margin) {
		xyzAxis = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Axes-16.png")) );
		xyzAxis.setMargin(margin);
		xyzAxis.setFocusPainted(false);
		xyzAxis.setRequestFocusEnabled(false);
		xyzAxis.setToolTipText(formatToolTip("Show Axes",
				"Shows the unit vectors for the x, y, and z axes."));
		xyzAxis.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				DisplayEntity ent = (DisplayEntity) sim.getNamedEntity("XYZ-Axis");
				if (ent != null) {
					KeywordIndex kw = InputAgent.formatBoolean("Show", xyzAxis.isSelected());
					InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
				}
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( xyzAxis );
	}

	private void addShowGridButton(JToolBar buttonBar, Insets margin) {
		grid = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Grid-16.png")) );
		grid.setMargin(margin);
		grid.setFocusPainted(false);
		grid.setRequestFocusEnabled(false);
		grid.setToolTipText(formatToolTip("Show Grid",
				"Shows the coordinate grid on the x-y plane."));
		grid.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				DisplayEntity ent = (DisplayEntity) sim.getNamedEntity("XY-Grid");
				if (ent == null && sim.getNamedEntity("Grid100x100") != null) {
					InputAgent.storeAndExecute(new DefineCommand(sim, DisplayEntity.class, "XY-Grid"));
					ent = (DisplayEntity) sim.getNamedEntity("XY-Grid");
					KeywordIndex dmKw = InputAgent.formatArgs("DisplayModel", "Grid100x100");
					KeywordIndex sizeKw = InputAgent.formatArgs("Size", "100", "100", "0", "m");
					InputAgent.storeAndExecute(new KeywordCommand(ent, dmKw, sizeKw));
					grid.setSelected(true);
				}
				if (ent != null) {
					KeywordIndex kw = InputAgent.formatBoolean("Show", grid.isSelected());
					InputAgent.storeAndExecute(new KeywordCommand(ent, kw));
				}
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( grid );
	}

	private void addShowLabelsButton(JToolBar buttonBar, Insets margin) {
		showLabels = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/ShowLabels-16.png")) );
		showLabels.setMargin(margin);
		showLabels.setFocusPainted(false);
		showLabels.setRequestFocusEnabled(false);
		showLabels.setToolTipText(formatToolTip("Show Labels",
				"Displays the label for every entity in the model."));
		showLabels.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bool = showLabels.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("ShowLabels", bool);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				sim.showTemporaryLabels(bool);
				updateUI();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( showLabels );
	}

	private void addShowSubModelsButton(JToolBar buttonBar, Insets margin) {
		showSubModels = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/ShowSubModels-16.png")) );
		showSubModels.setMargin(margin);
		showSubModels.setFocusPainted(false);
		showSubModels.setRequestFocusEnabled(false);
		showSubModels.setToolTipText(formatToolTip("Show SubModels",
				"Displays the components of each sub-model."));
		showSubModels.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bool = showSubModels.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("ShowSubModels", bool);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				sim.showSubModels(bool);
				updateUI();
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( showSubModels );
	}

	private void addPresentationModeButton(JToolBar buttonBar, Insets margin) {
		presentMode = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/PresentationMode-16.png")) );
		presentMode.setMargin(margin);
		presentMode.setFocusPainted(false);
		presentMode.setRequestFocusEnabled(false);
		presentMode.setToolTipText(formatToolTip("Presentation Mode",
				"Closes the tool windows and expands the view window to its maximum size."));
		presentMode.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bool = presentMode.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("PresentationMode", bool);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				controlStartResume.requestFocusInWindow();
			}
		} );
		buttonBar.add( presentMode );
	}

	private void addSnapToGridButton(JToolBar buttonBar, Insets margin) {
		snapToGrid = new JToggleButton( new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Snap-16.png")) );
		snapToGrid.setMargin(margin);
		snapToGrid.setFocusPainted(false);
		snapToGrid.setRequestFocusEnabled(false);
		snapToGrid.setToolTipText(formatToolTip("Snap to Grid",
				"During repositioning, objects are forced to the nearest grid point."));
		snapToGrid.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				KeywordIndex kw = InputAgent.formatBoolean("SnapToGrid", snapToGrid.isSelected());
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				gridSpacing.setEnabled(snapToGrid.isSelected());
				controlStartResume.requestFocusInWindow();
			}
		} );

		buttonBar.add( snapToGrid );
	}

	private void addSnapToGridField(JToolBar buttonBar, Insets margin) {

		gridSpacing = new JTextField("1000000 m") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setSnapGridSpacing(this.getText().trim());
				}
				else if (fe.getID() == FocusEvent.FOCUS_GAINED) {
					gridSpacing.selectAll();
				}
				super.processFocusEvent( fe );
			}
		};

		gridSpacing.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setSnapGridSpacing(gridSpacing.getText().trim());
				controlStartResume.requestFocusInWindow();
			}
		});

		gridSpacing.setMaximumSize(gridSpacing.getPreferredSize());
		int hght = snapToGrid.getPreferredSize().height;
		gridSpacing.setPreferredSize(new Dimension(gridSpacing.getPreferredSize().width, hght));

		gridSpacing.setHorizontalAlignment(JTextField.RIGHT);
		gridSpacing.setToolTipText(formatToolTip("Snap Grid Spacing",
				"Distance between adjacent grid points, e.g. 0.1 m, 10 km, etc."));

		gridSpacing.setEnabled(snapToGrid.isSelected());

		buttonBar.add(gridSpacing);
	}

	private void addShowReferencesButton(JToolBar buttonBar, Insets margin) {
		showReferences = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/ShowReferences-16.png")));
		showReferences.setToolTipText(formatToolTip("Show References",
				"Shows arrows to indicate which objects appear in the inputs for other objects. "
				+ "If one or more objects are selected, then only the arrows associated with "
				+ "the selected objects are shown."));
		showReferences.setMargin(margin);
		showReferences.setFocusPainted(false);
		showReferences.setRequestFocusEnabled(false);
		showReferences.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bShow = showReferences.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("ShowReferences", bShow);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				setShowReferences(bShow);
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( showReferences );
	}

	private void addShowLinksButton(JToolBar buttonBar, Insets margin) {
		showLinks = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/ShowLinks-16.png")));
		showLinks.setToolTipText(formatToolTip("Show Entity Flow",
				"Shows arrows between objects to indicate the flow of entities."));
		showLinks.setMargin(margin);
		showLinks.setFocusPainted(false);
		showLinks.setRequestFocusEnabled(false);
		showLinks.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bShow = showLinks.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("ShowEntityFlow", bShow);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				setShowEntityFlow(bShow);
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( showLinks );
	}

	private void addCreateLinksButton(JToolBar buttonBar, Insets margin) {
		createLinks = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/MakeLinks-16.png")));
		createLinks.setToolTipText(formatToolTip("Create Entity Links",
				"When this is enabled, entities are linked when selection is changed."));
		createLinks.setMargin(margin);
		createLinks.setFocusPainted(false);
		createLinks.setRequestFocusEnabled(false);
		createLinks.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				boolean bCreate = createLinks.isSelected();
				if (RenderManager.isGood()) {
					if (bCreate) {
						FrameBox.setSelectedEntity(null, false);
						if (!showLinks.isSelected())
							showLinks.doClick();
					}
					RenderManager.inst().setCreateLinks(bCreate);
				}
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( createLinks );
	}

	private void addPreviousButton(JToolBar buttonBar, Insets margin) {
		prevButton = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Previous-16.png")));
		prevButton.setToolTipText(formatToolTip("Previous",
				"Selects the previous object in the chain of linked objects."));
		prevButton.setMargin(margin);
		prevButton.setFocusPainted(false);
		prevButton.setRequestFocusEnabled(false);
		prevButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (createLinks.isSelected())
					createLinks.doClick();
				if (selectedEntity != null && selectedEntity instanceof DisplayEntity) {
					DisplayEntity selectedDEnt = (DisplayEntity) selectedEntity;
					boolean dir = !reverseButton.isSelected();
					ArrayList<DirectedEntity> list = selectedDEnt.getPreviousList(dir);
					if (list.isEmpty())
						return;
					if (list.size() == 1) {
						setSelectedDEnt(list.get(0));
						return;
					}
					ScrollablePopupMenu menu = new ScrollablePopupMenu();
					for (DirectedEntity de : list) {
						JMenuItem item = new JMenuItem(de.toString());
						item.addActionListener( new ActionListener() {
							@Override
							public void actionPerformed( ActionEvent event ) {
								setSelectedDEnt(de);
								controlStartResume.requestFocusInWindow();
							}
						} );
						menu.add(item);
					}
					menu.show(prevButton, 0, prevButton.getHeight());
				}
			}
		});
		buttonBar.add( prevButton );
	}

	private void addNextButton(JToolBar buttonBar, Insets margin) {
		nextButton = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Next-16.png")));
		nextButton.setToolTipText(formatToolTip("Next",
				"Selects the next object in the chain of linked objects."));
		nextButton.setMargin(margin);
		nextButton.setFocusPainted(false);
		nextButton.setRequestFocusEnabled(false);
		nextButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (createLinks.isSelected())
					createLinks.doClick();
				if (selectedEntity != null && selectedEntity instanceof DisplayEntity) {
					DisplayEntity selectedDEnt = (DisplayEntity) selectedEntity;
					boolean dir = !reverseButton.isSelected();
					ArrayList<DirectedEntity> list = selectedDEnt.getNextList(dir);
					if (list.isEmpty())
						return;
					if (list.size() == 1) {
						setSelectedDEnt(list.get(0));
						return;
					}
					ScrollablePopupMenu menu = new ScrollablePopupMenu();
					for (DirectedEntity de : list) {
						JMenuItem item = new JMenuItem(de.toString());
						item.addActionListener( new ActionListener() {
							@Override
							public void actionPerformed( ActionEvent event ) {
								setSelectedDEnt(de);
								controlStartResume.requestFocusInWindow();
							}
						} );
						menu.add(item);
					}
					menu.show(nextButton, 0, nextButton.getHeight());
				}
			}
		});
		buttonBar.add( nextButton );
	}

	private void setSelectedDEnt(DirectedEntity de) {
		FrameBox.setSelectedEntity(de.entity, false);
		if (!reverseButton.isSelected() == de.direction)
			return;
		reverseButton.doClick();
	}

	private void addReverseButton(JToolBar buttonBar, Insets margin) {
		reverseButton = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Reverse-16.png")));
		reverseButton.setToolTipText(formatToolTip("Reverse Direction",
				"When enabled, the link arrows are shown for entities travelling in the reverse "
				+ "direction, and the Next and Previous buttons select the next/previous links "
				+ "for that direction."));
		reverseButton.setMargin(margin);
		reverseButton.setFocusPainted(false);
		reverseButton.setRequestFocusEnabled(false);
		reverseButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (createLinks.isSelected())
					createLinks.doClick();
				boolean bool = reverseButton.isSelected();
				if (RenderManager.isGood()) {
					RenderManager.inst().setLinkDirection(!bool);
					RenderManager.redraw();
				}
				updateUI();
			}
		});
		// Reverse button is not needed in the open source JaamSim
		//buttonBar.add( reverseButton );
	}

	private void addCopyButton(JToolBar buttonBar, Insets margin) {
		copyButton = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Copy-16.png")));
		copyButton.setToolTipText(formatToolTip("Copy (Ctrl+C)",
				"Copies the selected entity to the clipboard."));
		copyButton.setMargin(margin);
		copyButton.setFocusPainted(false);
		copyButton.setRequestFocusEnabled(false);
		copyButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (selectedEntity != null)
					copyToClipboard(selectedEntity);
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( copyButton );
	}

	private void addPasteButton(JToolBar buttonBar, Insets margin) {
		pasteButton = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Paste-16.png")));
		pasteButton.setToolTipText(formatToolTip("Paste (Ctrl+V)",
				"Pastes a copy of an entity from the clipboard to the location of the most recent "
				+ "mouse click."));
		pasteButton.setMargin(margin);
		pasteButton.setFocusPainted(false);
		pasteButton.setRequestFocusEnabled(false);
		pasteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				pasteEntityFromClipboard();
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( pasteButton );
	}

	private void addEntityFinderButton(JToolBar buttonBar, Insets margin) {
		find = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/Find-16.png")));
		find.setToolTipText(formatToolTip("Entity Finder (Ctrl+F)",
				"Searches for an entity with a given name."));
		find.setMargin(margin);
		find.setFocusPainted(false);
		find.setRequestFocusEnabled(false);
		find.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (find.isSelected()) {
					FindBox.getInstance().showDialog();
				}
				else {
					FindBox.getInstance().setVisible(false);
				}
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( find );
	}

	private void addDisplayModelSelector(JToolBar buttonBar, Insets margin) {

		dispModel = new JTextField("");
		dispModel.setEditable(false);
		dispModel.setHorizontalAlignment(JTextField.CENTER);
		dispModel.setPreferredSize(new Dimension(120, fileSave.getPreferredSize().height));
		dispModel.setToolTipText(formatToolTip("DisplayModel", "Sets the default appearance of the entity. "
				+ "A DisplayModel is analogous to a text style in a word processor."));
		buttonBar.add(dispModel);

		modelSelector = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		modelSelector.setMargin(margin);
		modelSelector.setFocusPainted(false);
		modelSelector.setRequestFocusEnabled(false);
		modelSelector.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof DisplayEntity))
					return;
				DisplayEntity dispEnt = (DisplayEntity) selectedEntity;
				if (!dispEnt.isDisplayModelNominal() || dispEnt.getDisplayModelList().size() != 1)
					return;
				final String presentModelName = dispEnt.getDisplayModelList().get(0).getName();
				Input<?> in = dispEnt.getInput("DisplayModel");
				ArrayList<String> choices = in.getValidOptions(selectedEntity);
				PreviewablePopupMenu menu = new PreviewablePopupMenu(presentModelName, choices, true) {

					@Override
					public void setValue(String str) {
						dispModel.setText(str);
						KeywordIndex kw = InputAgent.formatArgs("DisplayModel", str);
						InputAgent.storeAndExecute(new KeywordCommand(dispEnt, kw));
						controlStartResume.requestFocusInWindow();
					}

				};
				menu.show(dispModel, 0, dispModel.getPreferredSize().height);
			}
		});

		buttonBar.add(modelSelector);
	}

	private void addEditDisplayModelButton(JToolBar buttonBar, Insets margin) {
		editDmButton = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Edit-16.png")));
		editDmButton.setToolTipText(formatToolTip("Edit DisplayModel",
				"Selects the present DisplayModel so that its inputs can be edited."));
		editDmButton.setMargin(margin);
		editDmButton.setFocusPainted(false);
		editDmButton.setRequestFocusEnabled(false);
		editDmButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof DisplayEntity))
					return;
				DisplayEntity dEnt = (DisplayEntity) selectedEntity;
				if (dEnt.getDisplayModelList().size() != 1)
					return;
				FrameBox.setSelectedEntity(dEnt.getDisplayModelList().get(0), false);
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( editDmButton );
	}

	private void addClearFormattingButton(JToolBar buttonBar, Insets margin) {
		clearButton = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Clear-16.png")));
		clearButton.setToolTipText(formatToolTip("Clear Formatting",
				"Resets the format inputs for the selected Entity or DisplayModel to their default "
				+ "values."));
		clearButton.setMargin(margin);
		clearButton.setFocusPainted(false);
		clearButton.setRequestFocusEnabled(false);
		clearButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent event ) {
				if (selectedEntity == null)
					return;
				ArrayList<KeywordIndex> kwList = new ArrayList<>();
				for (Input<?> in : selectedEntity.getEditableInputs()) {
					String cat = in.getCategory();
					if (in.isDefault() || !cat.equals(Entity.FORMAT) && !cat.equals(Entity.FONT))
						continue;
					KeywordIndex kw = InputAgent.formatArgs(in.getKeyword());
					kwList.add(kw);
				}
				if (kwList.isEmpty())
					return;
				KeywordIndex[] kws = new KeywordIndex[kwList.size()];
				kwList.toArray(kws);
				InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kws));
				controlStartResume.requestFocusInWindow();
			}
		});
		buttonBar.add( clearButton );
	}

	private void addTextAlignmentButtons(JToolBar buttonBar, Insets margin) {

		ActionListener alignListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof TextBasics))
					return;
				TextBasics textEnt = (TextBasics) selectedEntity;
				Vec3d align = textEnt.getAlignment();
				align.x = alignLeft.isSelected() ? -0.5d : align.x;
				align.x = alignCentre.isSelected() ? 0.0d : align.x;
				align.x = alignRight.isSelected() ? 0.5d : align.x;
				if (align.x == textEnt.getAlignment().x)
					return;
				KeywordIndex kw = sim.formatVec3dInput("Alignment", align, DimensionlessUnit.class);

				Vec3d pos = textEnt.getPositionForAlignment(align);
				KeywordIndex posKw = sim.formatVec3dInput("Position", pos, DistanceUnit.class);

				InputAgent.storeAndExecute(new KeywordCommand(textEnt, kw, posKw));
				controlStartResume.requestFocusInWindow();
			}
		};

		alignLeft = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignLeft-16.png")));
		alignLeft.setMargin(margin);
		alignLeft.setFocusPainted(false);
		alignLeft.setRequestFocusEnabled(false);
		alignLeft.setToolTipText(formatToolTip("Align Left",
				"Aligns the text to the left margin."));
		alignLeft.addActionListener( alignListener );

		alignCentre = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignCentre-16.png")));
		alignCentre.setMargin(margin);
		alignCentre.setFocusPainted(false);
		alignCentre.setRequestFocusEnabled(false);
		alignCentre.setToolTipText(formatToolTip("Align Centre",
				"Centres the text between the right and left margins."));
		alignCentre.addActionListener( alignListener );

		alignRight = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/AlignRight-16.png")));
		alignRight.setMargin(margin);
		alignRight.setFocusPainted(false);
		alignRight.setRequestFocusEnabled(false);
		alignRight.setToolTipText(formatToolTip("Align Right",
				"Aligns the text to the right margin."));
		alignRight.addActionListener( alignListener );

		alignmentGroup = new ButtonGroup();
		alignmentGroup.add(alignLeft);
		alignmentGroup.add(alignCentre);
		alignmentGroup.add(alignRight);

		buttonBar.add( alignLeft );
		buttonBar.add( alignCentre );
		buttonBar.add( alignRight );
	}

	private void addFontStyleButtons(JToolBar buttonBar, Insets margin) {

		ActionListener alignmentListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof TextEntity))
					return;
				TextEntity textEnt = (TextEntity) selectedEntity;
				if (textEnt.isBold() == bold.isSelected()
						&& textEnt.isItalic() && italic.isSelected())
					return;
				ArrayList<String> stylesList = new ArrayList<>(2);
				if (bold.isSelected())
					stylesList.add("BOLD");
				if (italic.isSelected())
					stylesList.add("ITALIC");
				String[] styles = stylesList.toArray(new String[stylesList.size()]);
				ArrayList<KeywordIndex> kwList = new ArrayList<>(2);
				kwList.add( InputAgent.formatArgs("FontStyle", styles) );
				if (textEnt instanceof Text && ((Text) textEnt).isAutoSize()) {
					Text t = (Text) textEnt;
					int style = (bold.isSelected() ? Font.BOLD : 0) + (italic.isSelected() ? Font.ITALIC : 0);
					Vec3d size = t.getAutoSize(t.getFontName(), style, t.getTextHeight());
					if (Double.isFinite(size.x) && Double.isFinite(size.y)) {
						kwList.add( getJaamSimModel().formatVec3dInput("Size", size, DistanceUnit.class) );
					}
				}
				KeywordIndex[] kws = new KeywordIndex[kwList.size()];
				kwList.toArray(kws);
				InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kws));
				controlStartResume.requestFocusInWindow();
			}
		};

		bold = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Bold-16.png")));
		bold.setMargin(margin);
		bold.setFocusPainted(false);
		bold.setRequestFocusEnabled(false);
		bold.setToolTipText(formatToolTip("Bold", "Makes the text bold."));
		bold.addActionListener( alignmentListener );

		italic = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Italic-16.png")));
		italic.setMargin(margin);
		italic.setFocusPainted(false);
		italic.setRequestFocusEnabled(false);
		italic.setToolTipText(formatToolTip("Italic", "Italicizes the text."));
		italic.addActionListener( alignmentListener );

		buttonBar.add( bold );
		buttonBar.add( italic );
	}

	private void addFontSelector(JToolBar buttonBar, Insets margin) {

		font = new JTextField("");
		font.setEditable(false);
		font.setHorizontalAlignment(JTextField.CENTER);
		font.setPreferredSize(new Dimension(120, fileSave.getPreferredSize().height));
		font.setToolTipText(formatToolTip("Font", "Sets the font for the text."));
		buttonBar.add(font);

		fontSelector = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/dropdown.png")));
		fontSelector.setMargin(margin);
		fontSelector.setFocusPainted(false);
		fontSelector.setRequestFocusEnabled(false);
		fontSelector.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof TextEntity))
					return;
				final TextEntity textEnt = (TextEntity) selectedEntity;
				final String presentFontName = textEnt.getFontName();
				ArrayList<String> valuesInUse = GUIFrame.getFontsInUse(sim);
				ArrayList<String> choices = TextModel.validFontNames;
				PreviewablePopupMenu fontMenu = new PreviewablePopupMenu(presentFontName,
						valuesInUse, choices, true) {

					@Override
					public void setValue(String str) {
						font.setText(str);
						String name = Parser.addQuotesIfNeeded(str);
						ArrayList<KeywordIndex> kwList = new ArrayList<>(2);
						kwList.add( InputAgent.formatInput("FontName", name) );
						if (textEnt instanceof Text && ((Text) textEnt).isAutoSize()) {
							Text t = (Text) textEnt;
							Vec3d size = t.getAutoSize(str, t.getStyle(), t.getTextHeight());
							if (Double.isFinite(size.x) && Double.isFinite(size.y)) {
								kwList.add( getJaamSimModel().formatVec3dInput("Size", size, DistanceUnit.class) );
							}
						}
						KeywordIndex[] kws = new KeywordIndex[kwList.size()];
						kwList.toArray(kws);
						InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kws));
						controlStartResume.requestFocusInWindow();
					}

				};
				fontMenu.show(font, 0, font.getPreferredSize().height);
			}
		});

		buttonBar.add(fontSelector);
	}

	private void addTextHeightField(JToolBar buttonBar, Insets margin) {

		textHeight = new JTextField("1000000 m") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setTextHeight(this.getText().trim());
				}
				super.processFocusEvent( fe );
			}
		};

		textHeight.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setTextHeight(textHeight.getText().trim());
				controlStartResume.requestFocusInWindow();
			}
		});

		textHeight.setMaximumSize(textHeight.getPreferredSize());
		textHeight.setPreferredSize(new Dimension(textHeight.getPreferredSize().width,
				fileSave.getPreferredSize().height));

		textHeight.setHorizontalAlignment(JTextField.RIGHT);
		textHeight.setToolTipText(formatToolTip("Text Height",
				"Sets the height of the text, e.g. 0.1 m, 200 cm, etc."));

		buttonBar.add(textHeight);
	}

	private void addTextHeightButtons(JToolBar buttonBar, Insets margin) {

		ActionListener textHeightListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof TextEntity))
					return;
				TextEntity textEnt = (TextEntity) selectedEntity;

				double height = textEnt.getTextHeight();
				double spacing = sim.getSimulation().getSnapGridSpacing();
				if (textEnt instanceof OverlayText || textEnt instanceof BillboardText)
					spacing = 1.0d;
				height = Math.round(height/spacing) * spacing;

				if (event.getActionCommand().equals("LargerText")) {
					height += spacing;
				}
				else if (event.getActionCommand().equals("SmallerText")) {
					height -= spacing;
					height = Math.max(spacing, height);
				}

				String format = "%.1f  m";
				if (textEnt instanceof OverlayText || textEnt instanceof BillboardText)
					format = "%.0f";
				String str = String.format(format, height);
				textHeight.setText(str);
				ArrayList<KeywordIndex> kwList = new ArrayList<>(2);
				kwList.add( InputAgent.formatInput("TextHeight", str) );
				if (textEnt instanceof Text && ((Text) textEnt).isAutoSize()) {
					Text t = (Text) textEnt;
					Vec3d size = t.getAutoSize(t.getFontName(), t.getStyle(), height);
					if (Double.isFinite(size.x) && Double.isFinite(size.y)) {
						kwList.add( getJaamSimModel().formatVec3dInput("Size", size, DistanceUnit.class) );
					}
				}
				KeywordIndex[] kws = new KeywordIndex[kwList.size()];
				kwList.toArray(kws);
				InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kws));
				controlStartResume.requestFocusInWindow();
			}
		};

		largerText = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/LargerText-16.png")));
		largerText.setMargin(margin);
		largerText.setFocusPainted(false);
		largerText.setRequestFocusEnabled(false);
		largerText.setToolTipText(formatToolTip("Larger Text",
				"Increases the text height to the next higher multiple of the snap grid spacing."));
		largerText.setActionCommand("LargerText");
		largerText.addActionListener( textHeightListener );

		smallerText = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/SmallerText-16.png")));
		smallerText.setMargin(margin);
		smallerText.setFocusPainted(false);
		smallerText.setRequestFocusEnabled(false);
		smallerText.setToolTipText(formatToolTip("Smaller Text",
				"Decreases the text height to the next lower multiple of the snap grid spacing."));
		smallerText.setActionCommand("SmallerText");
		smallerText.addActionListener( textHeightListener );

		buttonBar.add( largerText );
		buttonBar.add( smallerText );
	}

	private void addFontColourButton(JToolBar buttonBar, Insets margin) {

		colourIcon = new ColorIcon(16, 16);
		colourIcon.setFillColor(Color.LIGHT_GRAY);
		colourIcon.setOutlineColor(Color.LIGHT_GRAY);
		fontColour = new JButton(colourIcon);
		fontColour.setMargin(margin);
		fontColour.setFocusPainted(false);
		fontColour.setRequestFocusEnabled(false);
		fontColour.setToolTipText(formatToolTip("Font Colour", "Sets the colour of the text."));
		fontColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof TextEntity))
					return;
				final TextEntity textEnt = (TextEntity) selectedEntity;
				final Color4d presentColour = textEnt.getFontColor();
				ArrayList<Color4d> coloursInUse = GUIFrame.getFontColoursInUse(sim);
				ColourMenu fontMenu = new ColourMenu(presentColour, coloursInUse, true) {

					@Override
					public void setColour(String colStr) {
						KeywordIndex kw = InputAgent.formatInput("FontColour", colStr);
						InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kw));
						controlStartResume.requestFocusInWindow();
					}

				};
				fontMenu.show(fontColour, 0, fontColour.getPreferredSize().height);
			}
		});

		buttonBar.add( fontColour );
	}

	private void addZButtons(JToolBar buttonBar, Insets margin) {

		ActionListener actionListener = new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof DisplayEntity)
						|| selectedEntity instanceof OverlayEntity)
					return;
				DisplayEntity dispEnt = (DisplayEntity) selectedEntity;

				double delta = sim.getSimulation().getSnapGridSpacing()/100.0d;
				Vec3d pos = dispEnt.getPosition();
				ArrayList<Vec3d> points = dispEnt.getPoints();
				Vec3d offset = new Vec3d();

				if (event.getActionCommand().equals("Up")) {
					pos.z += delta;
					offset.z += delta;
				}
				else if (event.getActionCommand().equals("Down")) {
					pos.z -= delta;
					offset.z -= delta;
				}

				// Normal object
				if (!dispEnt.usePointsInput()) {
					KeywordIndex posKw = sim.formatVec3dInput("Position", pos, DistanceUnit.class);
					InputAgent.storeAndExecute(new KeywordCommand(dispEnt, posKw));
					controlStartResume.requestFocusInWindow();
					return;
				}

				// Polyline object
				KeywordIndex ptsKw = sim.formatPointsInputs("Points", points, offset);
				InputAgent.storeAndExecute(new KeywordCommand(dispEnt, ptsKw));
				controlStartResume.requestFocusInWindow();
			}
		};

		increaseZ = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/PlusZ-16.png")));
		increaseZ.setMargin(margin);
		increaseZ.setFocusPainted(false);
		increaseZ.setRequestFocusEnabled(false);
		increaseZ.setToolTipText(formatToolTip("Move Up",
				"Increases the selected object's z-coordinate by one hundredth of the snap-grid "
				+ "spacing. By moving the object closer to the camera, it will appear on top of "
				+ "other objects with smaller z-coordinates."));
		increaseZ.setActionCommand("Up");
		increaseZ.addActionListener( actionListener );

		decreaseZ = new JButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/MinusZ-16.png")));
		decreaseZ.setMargin(margin);
		decreaseZ.setFocusPainted(false);
		decreaseZ.setRequestFocusEnabled(false);
		decreaseZ.setToolTipText(formatToolTip("Move Down",
				"Decreases the selected object's z-coordinate by one hundredth of the snap-grid "
				+ "spacing. By moving the object farther from the camera, it will appear below "
				+ "other objects with larger z-coordinates."));
		decreaseZ.setActionCommand("Down");
		decreaseZ.addActionListener( actionListener );

		buttonBar.add( increaseZ );
		buttonBar.add( decreaseZ );
	}

	private void addOutlineButton(JToolBar buttonBar, Insets margin) {
		outline = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Outline-16.png")));
		outline.setMargin(margin);
		outline.setFocusPainted(false);
		outline.setRequestFocusEnabled(false);
		outline.setToolTipText(formatToolTip("Show Outline", "Shows the outline."));
		outline.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof LineEntity)
						|| selectedEntity.getInput("Outlined") == null)
					return;
				LineEntity lineEnt = (LineEntity) selectedEntity;
				lineWidth.setEnabled(outline.isSelected());
				lineColour.setEnabled(outline.isSelected());
				if (lineEnt.isOutlined() == outline.isSelected())
					return;
				KeywordIndex kw = InputAgent.formatBoolean("Outlined", outline.isSelected());
				InputAgent.storeAndExecute(new KeywordCommand((Entity)lineEnt, kw));
				controlStartResume.requestFocusInWindow();
			}
		});

		buttonBar.add( outline );
	}

	private void addLineWidthSpinner(JToolBar buttonBar, Insets margin) {
		SpinnerNumberModel numberModel = new SpinnerNumberModel(1, 1, 10, 1);
		lineWidth = new JSpinner(numberModel);
		lineWidth.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				if (!(selectedEntity instanceof LineEntity)
						|| selectedEntity.getInput("LineWidth") == null)
					return;
				LineEntity lineEnt = (LineEntity) selectedEntity;
				int val = (int) lineWidth.getValue();
				if (val == lineEnt.getLineWidth())
					return;
				KeywordIndex kw = InputAgent.formatIntegers("LineWidth", val);
				InputAgent.storeAndExecute(new KeywordCommand((Entity)lineEnt, kw));
				controlStartResume.requestFocusInWindow();
			}
		});

		lineWidth.setToolTipText(formatToolTip("Line Width",
				"Sets the width of the line in pixels."));

		buttonBar.add( lineWidth );
	}

	private void addLineColourButton(JToolBar buttonBar, Insets margin) {

		lineColourIcon = new ColorIcon(16, 16);
		lineColourIcon.setFillColor(Color.LIGHT_GRAY);
		lineColourIcon.setOutlineColor(Color.LIGHT_GRAY);
		lineColour = new JButton(lineColourIcon);
		lineColour.setMargin(margin);
		lineColour.setFocusPainted(false);
		lineColour.setRequestFocusEnabled(false);
		lineColour.setToolTipText(formatToolTip("Line Colour",
				"Sets the colour of the line."));
		lineColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof LineEntity)
						|| selectedEntity.getInput("LineColour") == null)
					return;
				final LineEntity lineEnt = (LineEntity) selectedEntity;
				final Color4d presentColour = lineEnt.getLineColour();
				ArrayList<Color4d> coloursInUse = GUIFrame.getLineColoursInUse(sim);
				ColourMenu menu = new ColourMenu(presentColour, coloursInUse, true) {

					@Override
					public void setColour(String colStr) {
						KeywordIndex kw = InputAgent.formatInput("LineColour", colStr);
						InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kw));
						controlStartResume.requestFocusInWindow();
					}

				};
				menu.show(lineColour, 0, lineColour.getPreferredSize().height);
			}
		});

		buttonBar.add( lineColour );
	}

	private void addFillButton(JToolBar buttonBar, Insets margin) {
		fill = new JToggleButton(new ImageIcon(
				GUIFrame.class.getResource("/resources/images/Fill-16.png")));
		fill.setMargin(margin);
		fill.setFocusPainted(false);
		fill.setRequestFocusEnabled(false);
		fill.setToolTipText(formatToolTip("Show Fill",
				"Fills the entity with the selected colour."));
		fill.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				if (!(selectedEntity instanceof FillEntity)
						|| selectedEntity.getInput("Filled") == null)
					return;
				FillEntity fillEnt = (FillEntity) selectedEntity;
				fillColour.setEnabled(fill.isSelected());
				if (fillEnt.isFilled() == fill.isSelected())
					return;
				KeywordIndex kw = InputAgent.formatBoolean("Filled", fill.isSelected());
				InputAgent.storeAndExecute(new KeywordCommand((Entity)fillEnt, kw));
				controlStartResume.requestFocusInWindow();
			}
		});

		buttonBar.add( fill );
	}

	private void addFillColourButton(JToolBar buttonBar, Insets margin) {

		fillColourIcon = new ColorIcon(16, 16);
		fillColourIcon.setFillColor(Color.LIGHT_GRAY);
		fillColourIcon.setOutlineColor(Color.LIGHT_GRAY);
		fillColour = new JButton(fillColourIcon);
		fillColour.setMargin(margin);
		fillColour.setFocusPainted(false);
		fillColour.setRequestFocusEnabled(false);
		fillColour.setToolTipText(formatToolTip("Fill Colour",
				"Sets the colour of the fill."));
		fillColour.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (!(selectedEntity instanceof FillEntity)
						|| selectedEntity.getInput("FillColour") == null)
					return;
				final FillEntity fillEnt = (FillEntity) selectedEntity;
				final Color4d presentColour = fillEnt.getFillColour();
				ArrayList<Color4d> coloursInUse = GUIFrame.getFillColoursInUse(sim);
				ColourMenu menu = new ColourMenu(presentColour, coloursInUse, true) {

					@Override
					public void setColour(String colStr) {
						KeywordIndex kw = InputAgent.formatInput("FillColour", colStr);
						InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kw));
						controlStartResume.requestFocusInWindow();
					}

				};
				menu.show(fillColour, 0, fillColour.getPreferredSize().height);
			}
		});

		buttonBar.add( fillColour );
	}

	// ******************************************************************************************************
	// TOOL BAR
	// ******************************************************************************************************

	/**
	 * Sets up the Control Panel's main tool bar.
	 */
	public void initializeMainToolBars() {

		// Insets used in setting the tool bar components
		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );

		// Initialize the main tool bar
		JToolBar mainToolBar = new JToolBar();
		mainToolBar.setMargin( smallMargin );
		mainToolBar.setFloatable(false);
		mainToolBar.setLayout( new FlowLayout( FlowLayout.LEFT, 0, 0 ) );

		// Add the main tool bar to the display
		getContentPane().add( mainToolBar, BorderLayout.SOUTH );

		// Run/pause button
		addRunButton(mainToolBar, noMargin);

		Dimension separatorDim = new Dimension(11, controlStartResume.getPreferredSize().height);
		Dimension gapDim = new Dimension(5, separatorDim.height);

		// Reset button
		mainToolBar.add(Box.createRigidArea(gapDim));
		addResetButton(mainToolBar, noMargin);

		// Real time button
		mainToolBar.addSeparator(separatorDim);
		addRealTimeButton(mainToolBar, smallMargin);

		// Speed multiplier spinner
		mainToolBar.add(Box.createRigidArea(gapDim));
		addSpeedMultiplier(mainToolBar, noMargin);

		// Pause time field
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Pause Time:"));
		mainToolBar.add(Box.createRigidArea(gapDim));
		addPauseTime(mainToolBar, noMargin);

		// Simulation time display
		mainToolBar.addSeparator(separatorDim);
		addSimulationTime(mainToolBar, noMargin);

		// Run progress bar
		mainToolBar.add(Box.createRigidArea(gapDim));
		addRunProgress(mainToolBar, noMargin);

		// Remaining time display
		mainToolBar.add(Box.createRigidArea(gapDim));
		addRemainingTime(mainToolBar, noMargin);

		// Achieved speed multiplier
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Speed:"));
		addAchievedSpeedMultiplier(mainToolBar, noMargin);

		// Cursor position
		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add(new JLabel("Position:"));
		addCursorPosition(mainToolBar, noMargin);
	}

	private void addRunButton(JToolBar mainToolBar, Insets margin) {
		runPressedIcon = new ImageIcon(GUIFrame.class.getResource("/resources/images/run-pressed-24.png"));
		pausePressedIcon = new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-pressed-24.png"));

		controlStartResume = new RoundToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/run-24.png")));
		controlStartResume.setRolloverEnabled(true);
		controlStartResume.setRolloverIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/run-rollover-24.png")));
		controlStartResume.setPressedIcon(runPressedIcon);
		controlStartResume.setSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-24.png")));
		controlStartResume.setRolloverSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause-rollover-24.png")));
		controlStartResume.setToolTipText(RUN_TOOLTIP);
		controlStartResume.setMargin(margin);
		controlStartResume.setEnabled( false );
		controlStartResume.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				controlStartResume.setEnabled(false);
				if (controlStartResume.isSelected()) {
					boolean bool = GUIFrame.this.startSimulation();
					if (bool) {
						controlStartResume.setPressedIcon(pausePressedIcon);
					}
					else {
						controlStartResume.setSelected(false);
						controlStartResume.setEnabled(true);
					}
				}
				else {
					GUIFrame.this.pauseSimulation();
					controlStartResume.setPressedIcon(runPressedIcon);
				}
				controlStartResume.requestFocusInWindow();
			}
		} );
		mainToolBar.add( controlStartResume );

		// Listen for keyboard shortcuts for simulation speed
		controlStartResume.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {}
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_PERIOD) {   // same as the '>' key
					if (!spinner.isEnabled())
						return;
					spinner.setValue(spinner.getNextValue());
					return;
				}
				if (e.getKeyCode() == KeyEvent.VK_COMMA) {    // same as the '<' key
					if (!spinner.isEnabled())
						return;
					spinner.setValue(spinner.getPreviousValue());
					return;
				}
			}
			@Override
			public void keyReleased(KeyEvent e) {}
		});
	}

	private void addResetButton(JToolBar mainToolBar, Insets margin) {
		controlStop = new RoundToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-16.png")));
		controlStop.setToolTipText(formatToolTip("Reset",
				"Resets the simulation run time to zero."));
		controlStop.setPressedIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-pressed-16.png")));
		controlStop.setRolloverEnabled( true );
		controlStop.setRolloverIcon(new ImageIcon(GUIFrame.class.getResource("/resources/images/reset-rollover-16.png")));
		controlStop.setMargin(margin);
		controlStop.setEnabled( false );
		controlStop.addActionListener( new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				if (sim.getSimState() == JaamSimModel.SIM_STATE_RUNNING) {
					GUIFrame.this.pauseSimulation();
				}
				controlStartResume.requestFocusInWindow();
				boolean confirmed = GUIFrame.showConfirmStopDialog();
				if (!confirmed) {
					return;
				}

				GUIFrame.this.stopSimulation();
				initSpeedUp(0.0d);
				tickUpdate(0L);
			}
		} );
		mainToolBar.add( controlStop );
	}

	private void addRealTimeButton(JToolBar mainToolBar, Insets margin) {
		controlRealTime = new JToggleButton( " Real Time " );
		controlRealTime.setToolTipText(formatToolTip("Real Time Mode",
				"When selected, the simulation runs at a fixed multiple of wall clock time."));
		controlRealTime.setMargin(margin);
		controlRealTime.setFocusPainted(false);
		controlRealTime.setRequestFocusEnabled(false);
		controlRealTime.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed( ActionEvent event ) {
				JaamSimModel sim = getJaamSimModel();
				boolean bool = controlRealTime.isSelected();
				KeywordIndex kw = InputAgent.formatBoolean("RealTime", bool);
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				controlStartResume.requestFocusInWindow();
			}
		});
		mainToolBar.add( controlRealTime );
	}

	private void addSpeedMultiplier(JToolBar mainToolBar, Insets margin) {
		SpinnerNumberModel numberModel =
				new SpinnerModel(Simulation.DEFAULT_REAL_TIME_FACTOR,
				   Simulation.MIN_REAL_TIME_FACTOR, Simulation.MAX_REAL_TIME_FACTOR, 1);
		spinner = new JSpinner(numberModel);

		// show up to 6 decimal places
		JSpinner.NumberEditor numberEditor = new JSpinner.NumberEditor(spinner,"0.######");
		spinner.setEditor(numberEditor);

		// make sure spinner TextField is no wider than 9 digits
		int diff =
			((JSpinner.DefaultEditor)spinner.getEditor()).getTextField().getPreferredSize().width -
			getPixelWidthOfString_ForFont("9", spinner.getFont()) * 9;
		Dimension dim = spinner.getPreferredSize();
		dim.width -= diff;
		spinner.setPreferredSize(dim);
		spinner.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged( ChangeEvent e ) {
				JaamSimModel sim = getJaamSimModel();
				Double val = (Double) spinner.getValue();
				if (MathUtils.near(val, sim.getSimulation().getRealTimeFactor()))
					return;
				NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
				DecimalFormat df = (DecimalFormat)nf;
				df.applyPattern("0.######");
				KeywordIndex kw = InputAgent.formatArgs("RealTimeFactor", df.format(val));
				InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
				controlStartResume.requestFocusInWindow();
			}
		});

		spinner.setToolTipText(formatToolTip("Speed Multiplier (&lt and &gt keys)",
				"Target ratio of simulation time to wall clock time when Real Time mode is selected."));
		spinner.setEnabled(false);
		mainToolBar.add( spinner );
	}

	private void addPauseTime(JToolBar mainToolBar, Insets margin) {
		pauseTime = new JTextField("0000-00-00T00:00:00") {
			@Override
			protected void processFocusEvent(FocusEvent fe) {
				if (fe.getID() == FocusEvent.FOCUS_LOST) {
					GUIFrame.this.setPauseTime(this.getText());
				}
				else if (fe.getID() == FocusEvent.FOCUS_GAINED) {
					pauseTime.selectAll();
				}
				super.processFocusEvent( fe );
			}
		};

		pauseTime.setPreferredSize(new Dimension(pauseTime.getPreferredSize().width,
				pauseTime.getPreferredSize().height));

		pauseTime.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent evt) {
				GUIFrame.this.setPauseTime(pauseTime.getText());
				controlStartResume.requestFocusInWindow();
			}
		});

		pauseTime.setText("");
		pauseTime.setHorizontalAlignment(JTextField.RIGHT);
		pauseTime.setToolTipText(formatToolTip("Pause Time",
				"Time at which to pause the run, e.g. 3 h, 10 s, etc."));

		mainToolBar.add(pauseTime);
	}

	private void addSimulationTime(JToolBar mainToolBar, Insets margin) {
		clockDisplay = new JLabel( "", JLabel.CENTER );
		clockDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		clockDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		clockDisplay.setToolTipText(formatToolTip("Simulation Time",
				"The present simulation time"));
		mainToolBar.add( clockDisplay );
	}

	private void addRunProgress(JToolBar mainToolBar, Insets margin) {
		progressBar = new JProgressBar( 0, 100 );
		progressBar.setPreferredSize( new Dimension( 120, controlRealTime.getPreferredSize().height ) );
		progressBar.setValue( 0 );
		progressBar.setStringPainted( true );
		progressBar.setToolTipText(formatToolTip("Run Progress",
				"Percent of the present simulation run that has been completed."));
		mainToolBar.add( progressBar );
	}

	private void addRemainingTime(JToolBar mainToolBar, Insets margin) {
		remainingDisplay = new JLabel( "", JLabel.CENTER );
		remainingDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		remainingDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		remainingDisplay.setToolTipText(formatToolTip("Remaining Time",
				"The remaining time required to complete the present simulation run."));
		mainToolBar.add( remainingDisplay );
	}

	private void addAchievedSpeedMultiplier(JToolBar mainToolBar, Insets margin) {
		speedUpDisplay = new JLabel( "", JLabel.CENTER );
		speedUpDisplay.setPreferredSize( new Dimension( 110, 16 ) );
		speedUpDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		speedUpDisplay.setToolTipText(formatToolTip("Achieved Speed Multiplier",
				"The ratio of elapsed simulation time to elasped wall clock time that was achieved."));
		mainToolBar.add( speedUpDisplay );
	}

	private void addCursorPosition(JToolBar mainToolBar, Insets margin) {
		locatorPos = new JLabel( "", JLabel.CENTER );
		locatorPos.setPreferredSize( new Dimension( 140, 16 ) );
		locatorPos.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		locatorPos.setToolTipText(formatToolTip("Cursor Position",
				"The coordinates of the cursor on the x-y plane."));
		mainToolBar.add( locatorPos );
	}

	// ******************************************************************************************************
	// RUN STATUS UPDATES
	// ******************************************************************************************************

	private long resumeSystemTime;
	private long lastSystemTime;
	private double lastSimTime;
	private double speedUp;

	public void initSpeedUp(double simTime) {
		resumeSystemTime = System.currentTimeMillis();
		lastSystemTime = resumeSystemTime;
		lastSimTime = simTime;
	}

	/**
	 * Sets the values for the simulation time, run progress, speedup factor,
	 * and remaining run time in the Control Panel's status bar.
	 * @param simTime - the present simulation time in seconds.
	 */
	void setClock(double simTime) {
		JaamSimModel sim = getJaamSimModel();

		// Set the simulation time display
		String unit = getJaamSimModel().getDisplayedUnit(TimeUnit.class);
		double factor = getJaamSimModel().getDisplayedUnitFactor(TimeUnit.class);
		clockDisplay.setText(String.format("%,.2f  %s", simTime/factor, unit));

		// Set the run progress bar display
		Simulation simulation = sim.getSimulation();
		if (simulation == null) {
			setProgress(0);
			return;
		}
		int progress = (int) Math.round(simulation.getProgress(simTime) * 100.0d);
		this.setProgress(progress);

		// Show the overall progress in JaamSim's title bar
		if (sim.getSimState() >= JaamSimModel.SIM_STATE_CONFIGURED) {
			int overallProgress = (int) Math.round(runManager.getProgress() * 100.0d);
			setTitle(sim, overallProgress);
		}

		// Do nothing further if the simulation is not executing events
		if (sim.getSimState() != JaamSimModel.SIM_STATE_RUNNING)
			return;

		// Set the speedup factor display
		double duration = simulation.getRunDuration() + simulation.getInitializationTime();
		double timeElapsed = simTime - simulation.getStartTime();
		long cTime = System.currentTimeMillis();
		long elapsedMillis = cTime - lastSystemTime;
		if (elapsedMillis > 5000L || cTime - resumeSystemTime < 5000L) {

			// Determine the speed-up factor
			speedUp = (timeElapsed - lastSimTime) * 1000.0d / elapsedMillis;
			setSpeedUp(speedUp);

			if (elapsedMillis > 5000L) {
				lastSystemTime = cTime;
				lastSimTime = timeElapsed;
			}
		}

		// Set the remaining time display
		setRemaining( (duration - timeElapsed)/speedUp );
	}

	/**
	 * Displays the given value on the Control Panel's progress bar.
	 * @param val - the percent of the run that has completed.
	 */
	public void setProgress( int val ) {
		if (lastValue == val)
			return;

		// Set the progress bar value
		progressBar.setValue( val );
		progressBar.repaint(25);
		lastValue = val;
	}

	/**
	 * Write the given value on the Control Panel's speed up factor box.
	 * @param val - the speed up factor to write.
	 */
	public void setSpeedUp( double val ) {
		if (val == 0.0) {
			speedUpDisplay.setText("-");
		}
		else if (val >= 0.99) {
			speedUpDisplay.setText(String.format("%,.0f", val));
		}
		else {
			speedUpDisplay.setText(String.format("%,.6f", val));
		}
	}

	/**
	 * Write the given value on the Control Panel's remaining run time box.
	 * @param val - the remaining run time in seconds.
	 */
	public void setRemaining( double val ) {
		remainingDisplay.setText( getRemainingTimeString(val) );
	}

	/**
	 * Returns a string containing the remaining run time in the most appropriate unit.
	 * @param val - remaining run time in seconds
	 */
	public static String getRemainingTimeString(double val) {
		if (val < 0.0)
			return "-";
		else if (val < 60.0)
			return String.format("%.0f seconds left", val);
		else if (val < 3600.0)
			return String.format("%.1f minutes left", val/60.0);
		else if (val < 3600.0*24.0)
			return String.format("%.1f hours left", val/3600.0);
		else if (val < 3600.0*8760.0)
			return String.format("%.1f days left", val/(3600.0*24.0));
		else
			return String.format("%.1f years left", val/(3600.0*8760.0));
	}

	// ******************************************************************************************************
	// SIMULATION CONTROLS
	// ******************************************************************************************************

	/**
	 * Starts or resumes the simulation run.
	 * @return true if the simulation was started or resumed; false if cancel or close was selected
	 */
	public boolean startSimulation() {
		JaamSimModel sim = getJaamSimModel();
		double pauseTime = sim.getSimulation().getPauseTime();
		if (sim.getSimState() <= JaamSimModel.SIM_STATE_CONFIGURED) {
			boolean confirmed = true;
			if (sim.isSessionEdited()) {
				confirmed = GUIFrame.showSaveChangesDialog(this);
			}
			if (confirmed) {
				if (!sim.getSimulation().isRealTime()
						&& runManager.getNumberOfRuns() > 1) {
					RunProgressBox.getInstance().setShow(true);
				}
				new Thread(new Runnable() {
					@Override
					public void run() {
						runManager.start(pauseTime);
					}
				}).start();
			}
			return confirmed;
		}
		else if (sim.getSimState() == JaamSimModel.SIM_STATE_PAUSED) {
			if (!sim.getSimulation().isRealTime()
					&& runManager.getNumberOfRuns() > 1) {
				RunProgressBox.getInstance().setShow(true);
			}
			new Thread(new Runnable() {
				@Override
				public void run() {
					runManager.resume(pauseTime);
				}
			}).start();
			return true;
		}
		else
			throw new ErrorException( "Invalid Simulation State for Start/Resume" );
	}

	/**
	 * Pauses the simulation run.
	 */
	private void pauseSimulation() {
		if (getJaamSimModel().getSimState() == JaamSimModel.SIM_STATE_RUNNING)
			new Thread(new Runnable() {
				@Override
				public void run() {
					runManager.pause();
				}
			}).start();
		else
			throw new ErrorException( "Invalid Simulation State for pause" );
	}

	/**
	 * Stops the simulation run.
	 */
	public void stopSimulation() {
		JaamSimModel sim = getJaamSimModel();
		if (sim.getSimState() == JaamSimModel.SIM_STATE_RUNNING ||
		    sim.getSimState() == JaamSimModel.SIM_STATE_PAUSED ||
		    sim.getSimState() == JaamSimModel.SIM_STATE_ENDED) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					runManager.reset();
				}
			}).start();
			if (RunProgressBox.hasInstance())
				RunProgressBox.getInstance().dispose();
			FrameBox.stop();
			this.updateForSimulationState(JaamSimModel.SIM_STATE_CONFIGURED);
		}
		else
			throw new ErrorException( "Invalid Simulation State for stop" );
	}

	public static void updateForSimState(int state) {
		GUIFrame inst = GUIFrame.getInstance();
		if (inst == null)
			return;

		inst.updateForSimulationState(state);
	}

	/**
	 * Sets the state of the simulation run to the given state value.
	 * @param state - an index that designates the state of the simulation run.
	 */
	void updateForSimulationState(int state) {
		JaamSimModel sim = getJaamSimModel();
		sim.setSimState(state);

		switch (sim.getSimState()) {
			case JaamSimModel.SIM_STATE_LOADED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					if (fileMenu.getItem(i) == null)
						continue;
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < toolsMenu.getItemCount(); i++ ) {
					if (toolsMenu.getItem(i) == null)
						continue;
					toolsMenu.getItem(i).setEnabled(true);
				}

				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(-1);
				setProgress(0);
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				break;

			case JaamSimModel.SIM_STATE_UNCONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					if (fileMenu.getItem(i) == null)
						continue;
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < toolsMenu.getItemCount(); i++ ) {
					if (toolsMenu.getItem(i) == null)
						continue;
					toolsMenu.getItem(i).setEnabled(true);
				}

				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(-1);
				setProgress(0);
				controlStartResume.setEnabled( false );
				controlStartResume.setSelected( false );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( false );
				break;

			case JaamSimModel.SIM_STATE_CONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					if (fileMenu.getItem(i) == null)
						continue;
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < toolsMenu.getItemCount(); i++ ) {
					if (toolsMenu.getItem(i) == null)
						continue;
					toolsMenu.getItem(i).setEnabled(true);
				}

				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				setSpeedUp(0);
				setRemaining(-1);
				setProgress(0);
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				lockViewXYPlane.setEnabled( true );
				progressBar.setEnabled( true );
				break;

			case JaamSimModel.SIM_STATE_RUNNING:
				speedUpDisplay.setEnabled( true );
				remainingDisplay.setEnabled( true );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( true );
				controlStartResume.setToolTipText(PAUSE_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			case JaamSimModel.SIM_STATE_PAUSED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			case JaamSimModel.SIM_STATE_ENDED:
				controlStartResume.setEnabled( false );
				controlStartResume.setSelected( false );
				controlStartResume.setToolTipText(RUN_TOOLTIP);
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;

			default:
				throw new ErrorException( "Unrecognized Graphics State" );
		}
		fileMenu.setEnabled( true );
	}

	@Override
	public void updateAll() {
		GUIFrame.updateUI();
	}

	@Override
	public void updateObjectSelector() {
		ObjectSelector.allowUpdate();
		GUIFrame.updateUI();
	}

	@Override
	public void updateModelBuilder() {
		EntityPallet.update();
	}

	@Override
	public void updateInputEditor() {
		EditBox.getInstance().setEntity(null);
	}

	private void clearButtons() {
		if (createLinks.isSelected())
			createLinks.doClick();
		if (reverseButton.isSelected())
			reverseButton.doClick();
	}

	public void updateControls(Simulation simulation) {
		if (simulation == null)
			return;
		updateSaveButton();
		updateUndoButtons();
		updateForRealTime(simulation.isRealTime(), simulation.getRealTimeFactor());
		updateForPauseTime(simulation.getPauseTimeString());
		update2dButton();
		updateShowAxesButton();
		updateShowGridButton();
		updateNextPrevButtons();
		updateFindButton();
		updateFormatButtons(selectedEntity);
		updateForSnapToGrid(simulation.isSnapToGrid());
		updateForSnapGridSpacing(simulation.getSnapGridSpacingString());
		updateShowLabelsButton(simulation.isShowLabels());
		updateShowSubModelsButton(simulation.isShowSubModels());
		updatePresentationModeButton(simulation.isPresentationMode());
		updateShowReferencesButton(simulation.isShowReferences());
		updateShowEntityFlowButton(simulation.isShowEntityFlow());
		updateToolVisibilities(simulation);
		updateToolSizes(simulation);
		updateToolLocations(simulation);
		updateViewVisibilities();
		updateViewSizes();
		updateViewLocations();
		setControlPanelWidth(simulation.getControlPanelWidth());
	}

	private void updateSaveButton() {
		fileSave.setEnabled(getJaamSimModel().isSessionEdited());
	}

	/**
	 * updates RealTime button and Spinner
	 */
	private synchronized void updateForRealTime(boolean executeRT, double factorRT) {
		getJaamSimModel().getEventManager().setExecuteRealTime(executeRT, factorRT);
		controlRealTime.setSelected(executeRT);
		spinner.setValue(factorRT);
		spinner.setEnabled(executeRT);
	}

	/**
	 * updates PauseTime entry
	 */
	private void updateForPauseTime(String str) {
		if (pauseTime.getText().equals(str) || pauseTime.isFocusOwner())
			return;
		pauseTime.setText(str);
	}

	/**
	 * Sets the PauseTime keyword for Simulation.
	 * @param str - value to assign.
	 */
	private void setPauseTime(String str) {
		JaamSimModel sim = getJaamSimModel();
		String prevVal = sim.getSimulation().getPauseTimeString();
		if (prevVal.equals(str))
			return;

		// If the time is in RFC8601 format, enclose in single quotes
		if (str.contains("-") || str.contains(":"))
			Parser.addQuotesIfNeeded(str);

		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);

		// if we only got one token, and it isn't RFC8601 - add a unit
		if (tokens.size() == 1 && !tokens.get(0).contains("-") && !tokens.get(0).contains(":"))
			tokens.add(getJaamSimModel().getDisplayedUnit(TimeUnit.class));

		try {
			// Parse the keyword inputs
			KeywordIndex kw = new KeywordIndex("PauseTime", tokens, null);
			InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
		}
		catch (InputErrorException e) {
			pauseTime.setText(prevVal);
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	/**
	 * Assigns a new name to the given entity.
	 * @param ent - entity to be renamed
	 * @param newName - new absolute name for the entity
	 */
	@Override
	public void renameEntity(Entity ent, String newName) {
		JaamSimModel sim = getJaamSimModel();

		// If the name has not changed, do nothing
		if (ent.getName().equals(newName))
			return;

		// Check that the entity was defined AFTER the RecordEdits command
		if (!ent.isAdded())
			throw new ErrorException("Cannot rename an entity that was defined before the RecordEdits command.");

		// Get the new local name
		String localName = newName;
		if (newName.contains(".")) {
			String[] names = newName.split("\\.");
			if (names.length == 0)
				throw new ErrorException(InputAgent.INP_ERR_BADNAME, localName);
			localName = names[names.length - 1];
			names = Arrays.copyOf(names, names.length - 1);
			Entity parent = sim.getEntityFromNames(names);
			if (parent != ent.getParent())
				throw new ErrorException("Cannot rename the entity's parent");
		}

		// Check that the new name is valid
		if (!InputAgent.isValidName(localName))
			throw new ErrorException(InputAgent.INP_ERR_BADNAME, localName);

		// Check that the new name does not conflict with another entity
		if (sim.getNamedEntity(newName) != null)
			throw new ErrorException(InputAgent.INP_ERR_DEFINEUSED, newName,
					sim.getNamedEntity(newName).getClass().getSimpleName());

		// Rename the entity
		InputAgent.storeAndExecute(new RenameCommand(ent, newName));
	}

	@Override
	public void deleteEntity(Entity ent) {
		JaamSimModel sim = getJaamSimModel();

		if (ent.isGenerated())
			throw new ErrorException("Cannot delete an entity that was generated by a simulation "
					+ "object.");

		if (!ent.isAdded())
			throw new ErrorException("Cannot delete an entity that was defined prior to "
					+ "RecordEdits in the input file.");

		if (ent instanceof DisplayEntity && !((DisplayEntity) ent).isMovable())
			throw new ErrorException("Cannot delete an entity that is not movable.");

		// Delete any child entities
		for (Entity child : ent.getChildren()) {
			if (child.isGenerated() || child instanceof EntityLabel)
				child.kill();
			else
				deleteEntity(child);
		}

		// Region
		if (ent instanceof Region) {

			// Reset the Region input for the entities in this region
			KeywordIndex kw = InputAgent.formatArgs("Region");
			for (DisplayEntity e : sim.getClonesOfIterator(DisplayEntity.class)) {
				if (e == ent || e.getInput("Region").getValue() != ent)
					continue;
				InputAgent.storeAndExecute(new CoordinateCommand(e, kw));
			}
		}

		// DisplayEntity
		if (ent instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity) ent;

			// Kill the label
			EntityLabel label = EntityLabel.getLabel(dEnt);
			if (label != null)
				deleteEntity(label);

			// Reset the RelativeEntity input for entities
			KeywordIndex kw = InputAgent.formatArgs("RelativeEntity");
			for (DisplayEntity e : sim.getClonesOfIterator(DisplayEntity.class)) {
				if (e == ent || e.getInput("RelativeEntity").getValue() != ent)
					continue;
				InputAgent.storeAndExecute(new CoordinateCommand(e, kw));
			}
		}

		// Delete any references to this entity in the inputs to other entities
		for (Entity e : getJaamSimModel().getClonesOfIterator(Entity.class)) {
			if (e == ent)
				continue;
			ArrayList<KeywordIndex> oldKwList = new ArrayList<>();
			ArrayList<KeywordIndex> newKwList = new ArrayList<>();
			for (Input<?> in : e.getEditableInputs()) {
				ArrayList<String> oldTokens = in.getValueTokens();
				boolean changed = in.removeReferences(ent);
				if (!changed)
					continue;
				KeywordIndex oldKw = new KeywordIndex(in.getKeyword(), oldTokens, null);
				KeywordIndex newKw = new KeywordIndex(in.getKeyword(), in.getValueTokens(), null);
				oldKwList.add(oldKw);
				newKwList.add(newKw);
			}

			// Reload any inputs that have changed so that redo/undo works correctly
			if (newKwList.isEmpty())
				continue;
			KeywordIndex[] oldKws = new KeywordIndex[oldKwList.size()];
			KeywordIndex[] newKws = new KeywordIndex[newKwList.size()];
			oldKws = oldKwList.toArray(oldKws);
			newKws = newKwList.toArray(newKws);
			InputAgent.storeAndExecute(new KeywordCommand(e, 0, oldKws, newKws));
		}

		// Execute the delete command
		InputAgent.storeAndExecute(new DeleteCommand(ent));
	}

	@Override
	public void storeAndExecute(Command cmd) {
		synchronized (undoList) {
			if (!cmd.isChange())
				return;

			// Execute the command and catch an error if it occurs
			cmd.execute();

			// Attempt to merge the command with the previous one
			Command mergedCmd = null;
			if (!undoList.isEmpty()) {
				Command lastCmd = undoList.get(undoList.size() - 1);
				mergedCmd = lastCmd.tryMerge(cmd);
			}

			// If the new command can be combined, then change the entry for previous command
			if (mergedCmd != null) {
				if (mergedCmd.isChange())
					undoList.set(undoList.size() - 1, mergedCmd);
				else
					undoList.remove(undoList.size() - 1);
			}

			// If the new command cannot be combined, then add it to the undo list
			else {
				undoList.add(cmd);
			}

			// Clear the re-do list
			redoList.clear();
		}
		updateUI();
	}

	public void undo() {
		synchronized (undoList) {
			if (undoList.isEmpty())
				return;
			Command cmd = undoList.remove(undoList.size() - 1);
			redoList.add(cmd);
			cmd.undo();
		}
		updateUI();
	}

	public void redo() {
		synchronized (undoList) {
			if (redoList.isEmpty())
				return;
			Command cmd = redoList.remove(redoList.size() - 1);
			undoList.add(cmd);
			cmd.execute();
		}
		updateUI();
	}

	public void undo(int n) {
		synchronized (undoList) {
			for (int i = 0; i < n; i++) {
				undo();
			}
		}
	}

	public void redo(int n) {
		synchronized (undoList) {
			for (int i = 0; i < n; i++) {
				redo();
			}
		}
	}

	public void invokeUndo() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				undo();
			}
		});
	}

	public void invokeRedo() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				redo();
			}
		});
	}

	public void updateUndoButtons() {
		synchronized (undoList) {
			undo.setEnabled(!undoList.isEmpty());
			undoDropdown.setEnabled(!undoList.isEmpty());
			undoMenuItem.setEnabled(!undoList.isEmpty());

			redo.setEnabled(!redoList.isEmpty());
			redoDropdown.setEnabled(!redoList.isEmpty());
			redoMenuItem.setEnabled(!redoList.isEmpty());
		}
	}

	public void clearUndoRedo() {
		synchronized (undoList) {
			undoList.clear();
			redoList.clear();
		}
		updateUI();
	}

	private void updateForSnapGridSpacing(String str) {
		if (gridSpacing.getText().equals(str) || gridSpacing.hasFocus())
			return;
		gridSpacing.setText(str);
	}

	private void setSnapGridSpacing(String str) {
		JaamSimModel sim = getJaamSimModel();
		Input<?> in = sim.getSimulation().getInput("SnapGridSpacing");
		String prevVal = in.getValueString();
		if (prevVal.equals(str))
			return;

		if (str.isEmpty()) {
			gridSpacing.setText(prevVal);
			return;
		}

		try {
			KeywordIndex kw = InputAgent.formatInput("SnapGridSpacing", str);
			InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
		}
		catch (InputErrorException e) {
			gridSpacing.setText(prevVal);
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	private void updateForSnapToGrid(boolean bool) {
		snapToGrid.setSelected(bool);
		gridSpacing.setEnabled(bool);
	}

	private void update2dButton() {
		if (!RenderManager.isGood())
			return;
		View view = RenderManager.inst().getActiveView();
		if (view == null)
			return;
		lockViewXYPlane.setSelected(view.is2DLocked());
	}

	private void updateShowAxesButton() {
		DisplayEntity ent = (DisplayEntity) getJaamSimModel().getNamedEntity("XYZ-Axis");
		xyzAxis.setSelected(ent != null && ent.getShow());
	}

	private void updateShowGridButton() {
		DisplayEntity ent = (DisplayEntity) getJaamSimModel().getNamedEntity("XY-Grid");
		grid.setSelected(ent != null && ent.getShow());
	}

	private void updateNextPrevButtons() {
		if (selectedEntity != null && selectedEntity instanceof DisplayEntity) {
			boolean dir = !reverseButton.isSelected();
			DisplayEntity selectedDEnt = (DisplayEntity) selectedEntity;
			prevButton.setEnabled(!selectedDEnt.getPreviousList(dir).isEmpty());
			nextButton.setEnabled(!selectedDEnt.getNextList(dir).isEmpty());
			return;
		}
		prevButton.setEnabled(false);
		nextButton.setEnabled(false);
	}

	private void updateFindButton() {
		boolean bool = FindBox.hasInstance() && FindBox.getInstance().isVisible();
		find.setSelected(bool);
	}

	public static void setSelectedEntity(Entity ent) {
		if (instance == null)
			return;
		instance.setSelectedEnt(ent);
	}

	public void setSelectedEnt(Entity ent) {
		selectedEntity = ent;
	}

	@Override
	public boolean isSelected(Entity ent) {
		return ent == selectedEntity;
	}

	private void updateFormatButtons(Entity ent) {
		updateEditButtons(ent);
		updateDisplayModelButtons(ent);
		updateClearFormattingButton(ent);
		updateTextButtons(ent);
		updateZButtons(ent);
		updateLineButtons(ent);
		updateFillButtons(ent);
	}

	private void updateEditButtons(Entity ent) {
		boolean bool = (ent != null && ent != getJaamSimModel().getSimulation());
		copyButton.setEnabled(bool);
		copyMenuItem.setEnabled(bool);
		deleteMenuItem.setEnabled(bool);
	}

	public void updateDisplayModelButtons(Entity ent) {
		boolean bool = ent instanceof DisplayEntity
				&& ((DisplayEntity)ent).isDisplayModelNominal()
				&& ((DisplayEntity)ent).getDisplayModelList().size() == 1;

		dispModel.setEnabled(bool);
		modelSelector.setEnabled(bool);
		editDmButton.setEnabled(bool);
		if (!bool) {
			dispModel.setText("");
			return;
		}

		DisplayEntity dispEnt = (DisplayEntity) ent;
		String name = dispEnt.getDisplayModelList().get(0).getName();
		if (!dispModel.getText().equals(name))
			dispModel.setText(name);
	}

	public void updateClearFormattingButton(Entity ent) {
		if (ent == null) {
			clearButton.setEnabled(false);
			return;
		}
		boolean bool = false;
		for (Input<?> in : ent.getEditableInputs()) {
			String cat = in.getCategory();
			if (!cat.equals(Entity.FORMAT) && !cat.equals(Entity.FONT))
				continue;
			if (!in.isDefault()) {
				bool = true;
				break;
			}
		}
		clearButton.setEnabled(bool);
	}

	private void updateTextButtons(Entity ent) {
		boolean bool = ent instanceof TextEntity;

		boolean isAlignable = bool && ent instanceof DisplayEntity
				&& !(ent instanceof OverlayText) && !(ent instanceof BillboardText);
		alignLeft.setEnabled(isAlignable);
		alignCentre.setEnabled(isAlignable);
		alignRight.setEnabled(isAlignable);
		if (!isAlignable) {
			alignmentGroup.clearSelection();
		}

		bold.setEnabled(bool);
		italic.setEnabled(bool);
		font.setEnabled(bool);
		fontSelector.setEnabled(bool);
		textHeight.setEnabled(bool);
		largerText.setEnabled(bool);
		smallerText.setEnabled(bool);
		fontColour.setEnabled(bool);
		if (!bool) {
			font.setText("");
			textHeight.setText(null);
			bold.setSelected(false);
			italic.setSelected(false);
			colourIcon.setFillColor(Color.LIGHT_GRAY);
			colourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		if (isAlignable) {
			int val = (int) Math.signum(((DisplayEntity) ent).getAlignment().x);
			alignLeft.setSelected(val == -1);
			alignCentre.setSelected(val == 0);
			alignRight.setSelected(val == 1);
		}

		TextEntity textEnt = (TextEntity) ent;
		bold.setSelected(textEnt.isBold());
		italic.setSelected(textEnt.isItalic());
		String fontName = textEnt.getFontName();
		if (!font.getText().equals(fontName))
			font.setText(fontName);
		updateTextHeight(textEnt.getTextHeightString());

		Color4d col = textEnt.getFontColor();
		colourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		colourIcon.setOutlineColor(Color.DARK_GRAY);
		fontColour.repaint();
	}

	private void updateTextHeight(String str) {
		if (textHeight.getText().equals(str) || textHeight.hasFocus())
			return;
		textHeight.setText(str);
	}

	private void updateZButtons(Entity ent) {
		boolean bool = ent instanceof DisplayEntity;
		bool = bool && !(ent instanceof OverlayEntity);
		bool = bool && !(ent instanceof BillboardText);
		increaseZ.setEnabled(bool);
		decreaseZ.setEnabled(bool);
	}

	private void updateLineButtons(Entity ent) {
		boolean bool = ent instanceof LineEntity;
		outline.setEnabled(bool && ent instanceof FillEntity);
		lineWidth.setEnabled(bool);
		lineColour.setEnabled(bool);
		if (!bool) {
			lineWidth.setValue(1);
			lineColourIcon.setFillColor(Color.LIGHT_GRAY);
			lineColourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		LineEntity lineEnt = (LineEntity) ent;
		outline.setSelected(lineEnt.isOutlined());
		lineWidth.setEnabled(lineEnt.isOutlined());
		lineColour.setEnabled(lineEnt.isOutlined());

		lineWidth.setValue(Integer.valueOf(lineEnt.getLineWidth()));

		Color4d col = lineEnt.getLineColour();
		lineColourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		lineColourIcon.setOutlineColor(Color.DARK_GRAY);
		lineColour.repaint();
	}

	private void updateFillButtons(Entity ent) {
		boolean bool = ent instanceof FillEntity;
		fill.setEnabled(bool && ent.getInput("Filled") != null);
		fillColour.setEnabled(bool);
		if (!bool) {
			fillColourIcon.setFillColor(Color.LIGHT_GRAY);
			fillColourIcon.setOutlineColor(Color.LIGHT_GRAY);
			return;
		}

		FillEntity fillEnt = (FillEntity) ent;
		fill.setSelected(fillEnt.isFilled());
		fillColour.setEnabled(fillEnt.isFilled());

		Color4d col = fillEnt.getFillColour();
		fillColourIcon.setFillColor(new Color((float)col.r, (float)col.g, (float)col.b, (float)col.a));
		fillColourIcon.setOutlineColor(Color.DARK_GRAY);
		fillColour.repaint();
	}

	private void setTextHeight(String str) {
		if (!(selectedEntity instanceof TextEntity))
			return;
		TextEntity textEnt = (TextEntity) selectedEntity;
		if (str.equals(textEnt.getTextHeightString()))
			return;

		try {
			ArrayList<KeywordIndex> kwList = new ArrayList<>(2);
			kwList.add( InputAgent.formatInput("TextHeight", str) );
			if (textEnt instanceof Text && ((Text) textEnt).isAutoSize()) {
				Text t = (Text) textEnt;
				double textHeight = Input.parseDoubles(getJaamSimModel(), kwList.get(0), 0.0d, Double.POSITIVE_INFINITY, DistanceUnit.class).get(0);
				Vec3d size = t.getAutoSize(t.getFontName(), t.getStyle(), textHeight);
				if (Double.isFinite(size.x) && Double.isFinite(size.y)) {
					kwList.add( getJaamSimModel().formatVec3dInput("Size", size, DistanceUnit.class) );
				}
			}
			KeywordIndex[] kws = new KeywordIndex[kwList.size()];
			kwList.toArray(kws);
			InputAgent.storeAndExecute(new KeywordCommand(selectedEntity, kws));
		}
		catch (InputErrorException e) {
			textHeight.setText(textEnt.getTextHeightString());
			GUIFrame.showErrorDialog("Input Error", e.getMessage());
		}
	}

	public static ArrayList<Image> getWindowIcons() {
		return iconImages;
	}

	public void copyLocationToClipBoard(Vec3d pos) {
		String data = String.format("(%.3f, %.3f, %.3f)", pos.x, pos.y, pos.z);
		StringSelection stringSelection = new StringSelection(data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( stringSelection, null );
	}

	public static void showLocatorPosition(Vec3d pos) {
		GUIFrame inst = GUIFrame.getInstance();
		if (inst == null)
			return;

		inst.showLocator(pos);
	}

	private void showLocator(Vec3d pos) {
		if( pos == null ) {
			locatorPos.setText( "-" );
			return;
		}

		String unit = getJaamSimModel().getDisplayedUnit(DistanceUnit.class);
		double factor = getJaamSimModel().getDisplayedUnitFactor(DistanceUnit.class);
		locatorPos.setText(String.format((Locale)null, "%.3f  %.3f  %.3f  %s",
				pos.x/factor, pos.y/factor, pos.z/factor, unit));
	}

	public void enableSave(boolean bool) {
		saveConfigurationMenuItem.setEnabled(bool);
	}

	/**
	 * Sets variables used to determine the position and size of various
	 * windows based on the size of the computer display being used.
	 */
	private void calcWindowDefaults() {
		Dimension guiSize = this.getSize();
		Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

		DEFAULT_GUI_WIDTH = winSize.width;
		COL1_WIDTH = 220;
		COL4_WIDTH = 520;
		int middleWidth = DEFAULT_GUI_WIDTH - COL1_WIDTH - COL4_WIDTH;
		COL2_WIDTH = Math.max(520, middleWidth / 2);
		COL3_WIDTH = Math.max(420, middleWidth - COL2_WIDTH);
		VIEW_WIDTH = DEFAULT_GUI_WIDTH - COL1_WIDTH;

		COL1_START = this.getX();
		COL2_START = COL1_START + COL1_WIDTH;
		COL3_START = COL2_START + COL2_WIDTH;
		COL4_START = Math.min(COL3_START + COL3_WIDTH, winSize.width - COL4_WIDTH);

		HALF_TOP = (winSize.height - guiSize.height) / 2;
		HALF_BOTTOM = (winSize.height - guiSize.height - HALF_TOP);
		LOWER_HEIGHT = Math.min(250, (winSize.height - guiSize.height) / 3);
		VIEW_HEIGHT = winSize.height - guiSize.height - LOWER_HEIGHT;

		TOP_START = this.getY() + guiSize.height;
		BOTTOM_START = TOP_START + HALF_TOP;
		LOWER_START = TOP_START + VIEW_HEIGHT;
	}

	public void setShowReferences(boolean bool) {
		if (!RenderManager.isGood())
			return;
		RenderManager.inst().setShowReferences(bool);
	}

	public void setShowEntityFlow(boolean bool) {
		if (!RenderManager.isGood())
			return;
		RenderManager.inst().setShowLinks(bool);
	}

	private void updateShowLabelsButton(boolean bool) {
		if (showLabels.isSelected() == bool)
			return;
		showLabels.setSelected(bool);
		getJaamSimModel().showTemporaryLabels(bool);
		updateUI();
	}

	private void updateShowSubModelsButton(boolean bool) {
		if (showSubModels.isSelected() == bool)
			return;
		showSubModels.setSelected(bool);
		getJaamSimModel().showSubModels(bool);
		updateUI();
	}

	private void updatePresentationModeButton(boolean bool) {
		if (presentMode.isSelected() == bool)
			return;
		presentMode.setSelected(bool);
	}

	public void clearPresentationMode() {
		if (!presentMode.isSelected())
			return;
		presentMode.doClick();
	}

	private void updateShowReferencesButton(boolean bool) {
		if (showReferences.isSelected() == bool)
			return;
		showReferences.setSelected(bool);
		setShowReferences(bool);
		updateUI();
	}

	private void updateShowEntityFlowButton(boolean bool) {
		if (showLinks.isSelected() == bool)
			return;
		showLinks.setSelected(bool);
		setShowEntityFlow(bool);
		updateUI();
	}

	/**
	 * Re-open any Tools windows that have been closed temporarily.
	 */
	public void showActiveTools(Simulation simulation) {
		EntityPallet.getInstance().setVisible(simulation.isModelBuilderVisible());
		ObjectSelector.getInstance().setVisible(simulation.isObjectSelectorVisible());
		EditBox.getInstance().setVisible(simulation.isInputEditorVisible());
		OutputBox.getInstance().setVisible(simulation.isOutputViewerVisible());
		PropertyBox.getInstance().setVisible(simulation.isPropertyViewerVisible());
		LogBox.getInstance().setVisible(simulation.isLogViewerVisible());

		if (!simulation.isEventViewerVisible()) {
			if (EventViewer.hasInstance())
				EventViewer.getInstance().dispose();
			return;
		}
		EventViewer.getInstance().setVisible(true);
	}

	/**
	 * Closes all the Tools windows temporarily.
	 */
	public void closeAllTools() {
		EntityPallet.getInstance().setVisible(false);
		ObjectSelector.getInstance().setVisible(false);
		EditBox.getInstance().setVisible(false);
		OutputBox.getInstance().setVisible(false);
		PropertyBox.getInstance().setVisible(false);
		LogBox.getInstance().setVisible(false);

		if (EventViewer.hasInstance())
			EventViewer.getInstance().setVisible(false);
	}

	public boolean isIconified() {
		return getExtendedState() == Frame.ICONIFIED;
	}

	private void updateToolVisibilities(Simulation simulation) {
		boolean iconified = isIconified() || simulation.isPresentationMode();
		setFrameVisibility(EntityPallet.getInstance(), !iconified && simulation.isModelBuilderVisible());
		setFrameVisibility(ObjectSelector.getInstance(), !iconified && simulation.isObjectSelectorVisible());
		setFrameVisibility(EditBox.getInstance(), !iconified && simulation.isInputEditorVisible());
		setFrameVisibility(OutputBox.getInstance(), !iconified && simulation.isOutputViewerVisible());
		setFrameVisibility(PropertyBox.getInstance(), !iconified && simulation.isPropertyViewerVisible());
		setFrameVisibility(LogBox.getInstance(), !iconified && simulation.isLogViewerVisible());

		if (RunProgressBox.hasInstance())
			setFrameVisibility(RunProgressBox.getInstance(), !iconified && RunProgressBox.getInstance().getShow());

		if (!simulation.isEventViewerVisible()) {
			if (EventViewer.hasInstance())
				EventViewer.getInstance().dispose();
			return;
		}
		setFrameVisibility(EventViewer.getInstance(), !iconified);
	}

	private void setFrameVisibility(JFrame frame, boolean bool) {
		if (frame.isVisible() == bool)
			return;
		frame.setVisible(bool);
		if (bool)
			frame.toFront();
	}

	public void updateToolSizes(Simulation simulation) {
		EntityPallet.getInstance().setSize(simulation.getModelBuilderSize().get(0),
				simulation.getModelBuilderSize().get(1));
		ObjectSelector.getInstance().setSize(simulation.getObjectSelectorSize().get(0),
				simulation.getObjectSelectorSize().get(1));
		EditBox.getInstance().setSize(simulation.getInputEditorSize().get(0),
				simulation.getInputEditorSize().get(1));
		OutputBox.getInstance().setSize(simulation.getOutputViewerSize().get(0),
				simulation.getOutputViewerSize().get(1));
		PropertyBox.getInstance().setSize(simulation.getPropertyViewerSize().get(0),
				simulation.getPropertyViewerSize().get(1));
		LogBox.getInstance().setSize(simulation.getLogViewerSize().get(0),
				simulation.getLogViewerSize().get(1));
		if (EventViewer.hasInstance()) {
			EventViewer.getInstance().setSize(simulation.getEventViewerSize().get(0),
					simulation.getEventViewerSize().get(1));
		}
	}

	public void updateToolLocations(Simulation simulation) {
		setToolLocation(EntityPallet.getInstance(), simulation.getModelBuilderPos().get(0),
				simulation.getModelBuilderPos().get(1));
		setToolLocation(ObjectSelector.getInstance(), simulation.getObjectSelectorPos().get(0),
				simulation.getObjectSelectorPos().get(1));
		setToolLocation(EditBox.getInstance(), simulation.getInputEditorPos().get(0),
				simulation.getInputEditorPos().get(1));
		setToolLocation(OutputBox.getInstance(), simulation.getOutputViewerPos().get(0),
				simulation.getOutputViewerPos().get(1));
		setToolLocation(PropertyBox.getInstance(), simulation.getPropertyViewerPos().get(0),
				simulation.getPropertyViewerPos().get(1));
		setToolLocation(LogBox.getInstance(), simulation.getLogViewerPos().get(0),
				simulation.getLogViewerPos().get(1));
		if (EventViewer.hasInstance()) {
			setToolLocation(EventViewer.getInstance(), simulation.getEventViewerPos().get(0),
					simulation.getEventViewerPos().get(1));
		}
	}

	public void setToolLocation(JFrame tool, int x, int y) {
		Point pt = getGlobalLocation(x, y);
		tool.setLocation(pt);
	}

	private void updateViewVisibilities() {
		if (!RenderManager.isGood())
			return;
		boolean iconified = isIconified();
		for (View v : views) {
			boolean isVisible = RenderManager.inst().isVisible(v);
			if (!iconified && v.showWindow()) {
				if (!isVisible) {
					RenderManager.inst().createWindow(v);
				}
			}
			else {
				if (isVisible) {
					RenderManager.inst().closeWindow(v);
				}
			}
		}
	}

	private void updateViewSizes() {
		for (View v : views) {
			final Frame window = RenderManager.getOpenWindowForView(v);
			if (window == null)
				continue;
			IntegerVector size = getWindowSize(v);
			window.setSize(size.get(0), size.get(1));
		}
	}

	public void updateViewLocations() {
		for (View v : views) {
			final Frame window = RenderManager.getOpenWindowForView(v);
			if (window == null)
				continue;
			IntegerVector pos = getWindowPos(v);
			window.setLocation(pos.get(0), pos.get(1));
		}
	}

	public void setControlPanelWidth(int width) {
		int height = getSize().height;
		setSize(width, height);
	}

	public void setWindowDefaults(Simulation simulation) {
		// Set the defaults from the AWT thread to avoid synchronization problems with updateUI and
		// the SizePosAdapter for the tool windows
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				simulation.setModelBuilderDefaults(   COL1_START, TOP_START,     COL1_WIDTH, HALF_TOP    );
				simulation.setObjectSelectorDefaults( COL1_START, BOTTOM_START,  COL1_WIDTH, HALF_BOTTOM );
				simulation.setInputEditorDefaults(    COL2_START, LOWER_START,   COL2_WIDTH, LOWER_HEIGHT);
				simulation.setOutputViewerDefaults(   COL3_START, LOWER_START,   COL3_WIDTH, LOWER_HEIGHT);
				simulation.setPropertyViewerDefaults( COL4_START, LOWER_START,   COL4_WIDTH, LOWER_HEIGHT);
				simulation.setLogViewerDefaults(      COL4_START, LOWER_START,   COL4_WIDTH, LOWER_HEIGHT);
				simulation.setEventViewerDefaults(    COL4_START, LOWER_START,   COL4_WIDTH, LOWER_HEIGHT);
				simulation.setControlPanelWidthDefault(DEFAULT_GUI_WIDTH);
				View.setDefaultPosition(COL2_START, TOP_START);
				View.setDefaultSize(VIEW_WIDTH, VIEW_HEIGHT);
				updateControls(simulation);
				clearUndoRedo();
			}
		});
	}

	public ArrayList<View> getViews() {
		synchronized (views) {
			return views;
		}
	}

	@Override
	public void addView(View v) {
		synchronized (views) {
			views.add(v);
		}
	}

	@Override
	public void removeView(View v) {
		synchronized (views) {
			views.remove(v);
		}
	}

	@Override
	public void createWindow(View v) {
		if (!RenderManager.isGood())
			return;
		RenderManager.inst().createWindow(v);
	}

	@Override
	public void closeWindow(View v) {
		if (!RenderManager.isGood())
			return;
		RenderManager.inst().closeWindow(v);
	}

	@Override
	public int getNextViewID() {
		nextViewID++;
		return nextViewID;
	}

	private void resetViews() {
		synchronized (views) {
			views.clear();
			for (View v : getJaamSimModel().getClonesOfIterator(View.class)) {
				views.add(v);
			}
		}
	}

	public IntegerVector getWindowPos(View v) {
		Point fix = OSFix.getLocationAdustment();  //FIXME
		IntegerVector ret = new IntegerVector(v.getWindowPos());

		// Presentation mode
		View activeView = null;
		if (RenderManager.isGood())
			activeView = RenderManager.inst().getActiveView();
		if (presentMode.isSelected() && v == activeView) {
			ret.set(0, COL1_START);
			ret.set(1, TOP_START);
		}

		Point pt = getGlobalLocation(ret.get(0), ret.get(1));
		ret.set(0, pt.x + fix.x);
		ret.set(1, pt.y + fix.y);
		return ret;
	}

	public IntegerVector getWindowSize(View v) {
		Point fix = OSFix.getSizeAdustment();  //FIXME
		IntegerVector ret = new IntegerVector(v.getWindowSize());

		// Presentation mode
		View activeView = null;
		if (RenderManager.isGood())
			activeView = RenderManager.inst().getActiveView();
		if (presentMode.isSelected() && v == activeView) {
			ret.set(0, VIEW_WIDTH + COL1_WIDTH);
			ret.set(1, VIEW_HEIGHT + LOWER_HEIGHT);
		}

		ret.addAt(fix.x, 0);
		ret.addAt(fix.y, 1);
		return ret;
	}

	public void setWindowPos(View v, int x, int y, int width, int height) {
		if (presentMode.isSelected())
			return;
		Point posFix = OSFix.getLocationAdustment();
		Point sizeFix = OSFix.getSizeAdustment();
		Point pt = getRelativeLocation(x - posFix.x, y - posFix.y);
		v.setWindowPos(pt.x, pt.y, width - sizeFix.x, height - sizeFix.y);
	}

	@Override
	public Vec3d getPOI(View v) {
		if (!RenderManager.isGood())
			return new Vec3d();
		return RenderManager.inst().getPOI(v);
	}

	// ******************************************************************************************************
	// MAIN
	// ******************************************************************************************************

	public static void main( String args[] ) {
		// Process the input arguments and filter out directives
		ArrayList<String> configFiles = new ArrayList<>(args.length);
		boolean batch = false;
		boolean minimize = false;
		boolean quiet = false;
		boolean scriptMode = false;
		boolean headless = false;

		for (String each : args) {
			// Batch mode
			if (each.equalsIgnoreCase("-b") ||
			    each.equalsIgnoreCase("-batch")) {
				batch = true;
				continue;
			}
			// Script mode (command line I/O)
			if (each.equalsIgnoreCase("-s") ||
			    each.equalsIgnoreCase("-script")) {
				scriptMode = true;
				continue;
			}
			// z-buffer offset
			if (each.equalsIgnoreCase("-z") ||
			    each.equalsIgnoreCase("-zbuffer")) {
				// Parse the option, but do nothing
				continue;
			}
			// Minimize model window
			if (each.equalsIgnoreCase("-m") ||
			    each.equalsIgnoreCase("-minimize")) {
				minimize = true;
				continue;
			}
			// Minimize model window
			if (each.equalsIgnoreCase("-h") ||
			    each.equalsIgnoreCase("-headless")) {
				headless = true;
				batch = true;
				continue;
			}
			// Do not open default windows
			if (each.equalsIgnoreCase("-q") ||
			    each.equalsIgnoreCase("-quiet")) {
				quiet = true;
				continue;
			}
			if (each.equalsIgnoreCase("-sg") ||
			    each.equalsIgnoreCase("-safe_graphics")) {
				SAFE_GRAPHICS = true;
				continue;
			}
			if (each.equalsIgnoreCase("-og") ||
				    each.equalsIgnoreCase("-optional_graphics")) {
					OPTIONAL_GRAPHICS = true;
					continue;
				}
			// Not a program directive, add to list of config files
			configFiles.add(each);
		}

		// If not running in batch mode, create the splash screen
		JWindow splashScreen = null;
		if (!batch) {
			URL splashImage = GUIFrame.class.getResource("/resources/images/splashscreen.png");
			ImageIcon imageIcon = new ImageIcon(splashImage);
			Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
			int splashX = (screen.width - imageIcon.getIconWidth()) / 2;
			int splashY = (screen.height - imageIcon.getIconHeight()) / 2;

			// Set the window's bounds, centering the window
			splashScreen = new JWindow();
			splashScreen.setAlwaysOnTop(true);
			splashScreen.setBounds(splashX, splashY, imageIcon.getIconWidth(), imageIcon.getIconHeight());

			// Build the splash screen
			splashScreen.getContentPane().add(new JLabel(imageIcon));

			// Display it
			splashScreen.setVisible(true);
		}

		// create a graphic simulation
		LogBox.logLine("Loading Simulation Environment ... ");
		JaamSimModel simModel = getNextJaamSimModel();
		simModel.autoLoad();

		// Add the run manager
		RunManager runMgr = new RunManager(simModel);

		GUIFrame gui = null;
		if (!headless) {
			gui = GUIFrame.createInstance();
		}
		setRunManager(runMgr);

		if (!headless) {
			if (minimize)
				gui.setExtendedState(JFrame.ICONIFIED);
			// This is only here to initialize the static cache in the MRG1999a class to avoid future latency
			// when initializing other objects in drag+drop
			@SuppressWarnings("unused")
			MRG1999a cacher = new MRG1999a();
		}

		if (!batch && !headless) {
			// Begin initializing the rendering system
			RenderManager.initialize(SAFE_GRAPHICS);
		}

		LogBox.logLine("Simulation Environment Loaded");

		simModel.setBatchRun(batch);
		simModel.setScriptMode(scriptMode);

		// Show the Control Panel
		if (gui != null) {
			gui.setVisible(true);
			gui.calcWindowDefaults();
			gui.setLocation(gui.getX(), gui.getY());  //FIXME remove when setLocation is fixed for Windows 10
			gui.setWindowDefaults(simModel.getSimulation());
		}

		// Resolve all input arguments against the current working directory
		File user = new File(System.getProperty("user.dir"));
		// Process any configuration files passed on command line
		// (Multiple configuration files are not supported at present)
		for (int i = 0; i < configFiles.size(); i++) {
			//InputAgent.configure(gui, new File(configFiles.get(i)));
			File abs = new File((File)null, configFiles.get(i));
			File loadFile;
			if (abs.exists())
				loadFile = abs.getAbsoluteFile();
			else
				loadFile = new File(user, configFiles.get(i));

			Throwable t = GUIFrame.configure(loadFile);
			if (t != null) {
				// Hide the splash screen
				if (splashScreen != null) {
					splashScreen.dispose();
					splashScreen = null;
				}
				handleConfigError(t, loadFile);
			}
		}

		// If in script mode, load a configuration file from standard in
		if (scriptMode) {
			BufferedReader buf = new BufferedReader(new InputStreamReader(System.in));
			InputAgent.readBufferedStream(simModel, buf, null, "");
		}

		// If no configuration files were specified on the command line, then load the default configuration file
		if (configFiles.size() == 0 && !scriptMode) {
			// Load the default model from the AWT thread to avoid synchronization problems with updateUI and
			// setWindowDefaults
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					simModel.setRecordEdits(true);
					InputAgent.loadDefault(simModel);
					GUIFrame.updateForSimState(JaamSimModel.SIM_STATE_CONFIGURED);
				}
			});
		}

		// If in batch or quiet mode, close the any tools that were opened
		if (quiet || batch) {
			if (gui != null)
				gui.closeAllTools();
		}

		// Set RecordEdits mode (if it has not already been set in the configuration file)
		simModel.setRecordEdits(true);

		// Start the model if in batch mode
		if (batch) {
			if (simModel.getNumErrors() > 0)
				GUIFrame.shutdown(0);
			runMgr.start();
			return;
		}

		// Hide the splash screen
		if (splashScreen != null) {
			splashScreen.dispose();
			splashScreen = null;
		}

		// Bring the Control Panel to the front (along with any open Tools)
		if (gui != null)
			gui.toFront();

		// Set the selected entity to the Simulation object
		FrameBox.setSelectedEntity(simModel.getSimulation(), false);
	}

	public static boolean isOptionalGraphics() {
		return OPTIONAL_GRAPHICS;
	}

	/**
	 * this class is created so the next value will be value * 2 and the
	 * previous value will be value / 2
	 */
	public static class SpinnerModel extends SpinnerNumberModel {
		private double value;
		public SpinnerModel( double val, double min, double max, double stepSize) {
			super(val, min, max, stepSize);
		}

		@Override
		public Object getPreviousValue() {
			value = this.getNumber().doubleValue() / 2.0;
			if (value >= 1.0)
				value = Math.floor(value);

			// Avoid going beyond limit
			Double min = (Double)this.getMinimum();
			if (min.doubleValue() > value) {
				return min;
			}
			return value;
		}

		@Override
		public Object getNextValue() {
			value = this.getNumber().doubleValue() * 2.0;
			if (value >= 1.0)
				value = Math.floor(value);

			// Avoid going beyond limit
			Double max = (Double)this.getMaximum();
			if (max.doubleValue() < value) {
				return max;
			}
			return value;
		}
	}

	public static boolean getShuttingDownFlag() {
		return shuttingDown.get();
	}

	public static void shutdown(int errorCode) {

		shuttingDown.set(true);
		if (RenderManager.isGood()) {
			RenderManager.inst().shutdown();
		}

		System.exit(errorCode);
	}

	@Override
	public void exit(int errorCode) {
		shutdown(errorCode);
	}

	volatile long simTicks;

	private static class UIUpdater implements Runnable {
		private final GUIFrame frame;

		UIUpdater(GUIFrame gui) {
			frame = gui;
		}

		@Override
		public void run() {
			JaamSimModel sim = getJaamSimModel();
			if (sim == null)
				return;
			EventManager evt = sim.getEventManager();
			double callBackTime = evt.ticksToSeconds(frame.simTicks);

			frame.setClock(callBackTime);
			frame.updateControls(sim.getSimulation());
			FrameBox.updateEntityValues(callBackTime);

			if (RunProgressBox.hasInstance())
				RunProgressBox.getInstance().update();
		}
	}

	@Override
	public void tickUpdate(long tick) {
		if (tick == simTicks)
			return;

		simTicks = tick;
		RenderManager.updateTime(tick);
		GUIFrame.updateUI();
	}

	@Override
	public void timeRunning() {
		EventManager evt = EventManager.current();
		boolean running = evt.isRunning();
		if (running) {
			updateForSimulationState(JaamSimModel.SIM_STATE_RUNNING);
		}
		else {
			int state = JaamSimModel.SIM_STATE_PAUSED;
			if (!getJaamSimModel().getSimulation().canResume(simTicks))
				state = JaamSimModel.SIM_STATE_ENDED;
			updateForSimulationState(state);
		}
	}

	@Override
	public void handleInputError(Throwable t, Entity ent) {
		GUIFrame.invokeErrorDialog("Input Error",
				"JaamSim has detected the following input error during validation:",
				String.format("%s: %-70s", ent.getName(), t.getMessage()),
				"The error must be corrected before the simulation can be started.");

		GUIFrame.updateForSimState(JaamSimModel.SIM_STATE_CONFIGURED);
	}

	@Override
	public void handleError(Throwable t) {
		JaamSimModel sim = getJaamSimModel();
		if (t instanceof OutOfMemoryError) {
			OutOfMemoryError e = (OutOfMemoryError)t;
			InputAgent.logMessage(sim, "Out of Memory use the -Xmx flag during execution for more memory");
			InputAgent.logMessage(sim, "Further debug information:");
			InputAgent.logMessage(sim, "%s", e.getMessage());
			InputAgent.logStackTrace(sim, t);
			GUIFrame.shutdown(1);
			return;
		}
		else {
			EventManager evt = EventManager.current();
			long currentTick = evt.getTicks();
			double curSec = evt.ticksToSeconds(currentTick);
			InputAgent.logMessage(sim, "EXCEPTION AT TIME: %f s", curSec);
			InputAgent.logMessage(sim, "%s", t.getMessage());
			if (t.getCause() != null) {
				InputAgent.logMessage(sim, "Call Stack of original exception:");
				InputAgent.logStackTrace(sim, t.getCause());
			}
			InputAgent.logMessage(sim, "Thrown exception call stack:");
			InputAgent.logStackTrace(sim, t);
		}

		String msg = t.getMessage();
		if (msg == null)
			msg = "null";
		String source = "";
		int pos = -1;
		if (t instanceof InputErrorException) {
			source = ((InputErrorException) t).source;
			pos = ((InputErrorException) t).position;
		}
		if (t instanceof ErrorException) {
			source = ((ErrorException) t).source;
			pos = ((ErrorException) t).position;
		}
		GUIFrame.showErrorDialog("Runtime Error",
				source,
				pos,
				"JaamSim has detected the following runtime error condition:",
				msg,
				"Programmers can find more information by opening the Log Viewer.\n"
						+ "The simulation run must be reset to zero simulation time before it "
						+ "can be restarted.");
	}

	void newModel() {

		// Create the new JaamSimModel and load the default objects and inputs
		JaamSimModel simModel = getNextJaamSimModel();
		simModel.autoLoad();
		setWindowDefaults(simModel.getSimulation());

		// Add the run manager
		RunManager runMgr = new RunManager(simModel);

		// Set the Control Panel to the new JaamSimModel and reset the user interface
		setRunManager(runMgr);

		// Load the default model
		simModel.setRecordEdits(true);
		InputAgent.loadDefault(simModel);

		FrameBox.setSelectedEntity(simModel.getSimulation(), false);
	}

	void load() {

		LogBox.logLine("Loading...");

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(getConfigFolder());

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);

		// Show the file chooser and wait for selection
		int returnVal = chooser.showOpenDialog(this);

		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File chosenfile = chooser.getSelectedFile();
			load(chosenfile);
		}
	}

	public void load(File file) {

		// Create the new JaamSimModel and load the default objects and inputs
		JaamSimModel simModel = new JaamSimModel(file.getName());
		simModel.autoLoad();
		setWindowDefaults(simModel.getSimulation());

		// Add the run manager
		RunManager runMgr = new RunManager(simModel);

		// Set the Control Panel to the new JaamSimModel and reset the user interface
		setRunManager(runMgr);

		// Load the selected input file
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				simModel.setRecordEdits(false);

				Throwable ret = GUIFrame.configure(file);
				if (ret != null) {
					setCursor(Cursor.getDefaultCursor());
					handleConfigError(ret, file);
				}

				simModel.setRecordEdits(true);
				resetViews();
				FrameBox.setSelectedEntity(simModel.getSimulation(), false);
				setCursor(Cursor.getDefaultCursor());
			}
		});

		setConfigFolder(file.getParent());
	}

	static Throwable configure(File file) {
		GUIFrame.updateForSimState(JaamSimModel.SIM_STATE_UNCONFIGURED);
		JaamSimModel sim = getJaamSimModel();

		Throwable ret = null;
		try {
			sim.configure(file);
		}
		catch (Throwable t) {
			ret = t;
		}

		if (ret == null)
			LogBox.logLine("Configuration File Loaded");
		else
			LogBox.logLine("Configuration File Loaded - errors found");

		// show the present state in the user interface
		GUIFrame gui = GUIFrame.getInstance();
		if (gui != null) {
			gui.setProgress(0);
			gui.setTitle(sim);
			gui.updateForSimulationState(JaamSimModel.SIM_STATE_CONFIGURED);
			gui.enableSave(sim.isRecordEditsFound());
		}
		return ret;
	}

	static void handleConfigError(Throwable t, File file) {
		JaamSimModel sim = getJaamSimModel();
		if (t instanceof InputErrorException) {
			InputAgent.logMessage(sim, "Input Error: %s", t.getMessage());
			GUIFrame.showErrorOptionDialog("Input Error",
					String.format("Input errors were detected while loading file: '%s'\n\n"
							+ "%s\n\n"
							+ "Open '%s' with Log Viewer?",
							file.getName(), t.getMessage(), sim.getRunName() + ".log"));
			return;
		}

		InputAgent.logMessage(sim, "Fatal Error while loading file '%s': %s\n", file.getName(), t.getMessage());
		GUIFrame.showErrorDialog("Fatal Error",
				String.format("A fatal error has occured while loading the file '%s':", file.getName()),
				t.getMessage(),
				"");
	}

	/**
	 * Saves the configuration file.
	 * @param file = file to be saved
	 */
	private void setSaveFile(File file) {
		JaamSimModel sim = getJaamSimModel();
		try {
			sim.save(file);

			// Set the title bar to match the new run name
			setTitle(sim);
		}
		catch (Exception e) {
			GUIFrame.showErrorDialog("File Error", e.getMessage());
			LogBox.logException(e);
		}
	}

	boolean save() {
		LogBox.logLine("Saving...");
		JaamSimModel sim = getJaamSimModel();
		if( sim.getConfigFile() != null ) {
			setSaveFile(sim.getConfigFile());
			updateUI();
			return true;
		}

		boolean confirmed = saveAs();
		return confirmed;
	}

	boolean saveAs() {
		LogBox.logLine("Save As...");

		// Create a file chooser
		final JFileChooser chooser = new JFileChooser(getConfigFolder());

		// Set the file extension filters
		chooser.setAcceptAllFileFilterUsed(true);
		FileNameExtensionFilter cfgFilter =
				new FileNameExtensionFilter("JaamSim Configuration File (*.cfg)", "CFG");
		chooser.addChoosableFileFilter(cfgFilter);
		chooser.setFileFilter(cfgFilter);
		chooser.setSelectedFile(getJaamSimModel().getConfigFile());

		// Show the file chooser and wait for selection
		int returnVal = chooser.showSaveDialog(this);

		if (returnVal != JFileChooser.APPROVE_OPTION)
			return false;

		File file = chooser.getSelectedFile();

		// Add the file extension ".cfg" if needed
		String filePath = file.getPath();
		filePath = filePath.trim();
		if (file.getName().trim().indexOf('.') == -1) {
			filePath = filePath.concat(".cfg");
			file = new File(filePath);
		}

		// Confirm overwrite if file already exists
		if (file.exists()) {
			boolean confirmed = GUIFrame.showSaveAsDialog(file.getName());
			if (!confirmed) {
				return false;
			}
		}

		// Save the configuration file
		setSaveFile(file);

		setConfigFolder(file.getParent());
		updateUI();
		return true;
	}

	public void copyToClipboard(Entity ent) {
		if (ent == getJaamSimModel().getSimulation())
			return;
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		clpbrd.setContents(new StringSelection(ent.getName()), null);
	}

	public Entity getEntityFromClipboard() {
		Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		try {
			String name = (String)clpbrd.getData(DataFlavor.stringFlavor);
			return getJaamSimModel().getNamedEntity(name);
		}
		catch (Throwable err) {
			return null;
		}
	}

	public void pasteEntityFromClipboard() {
		Entity ent = getEntityFromClipboard();
		JaamSimModel sim = getJaamSimModel();
		if (ent == null || ent == sim.getSimulation())
			return;

		// Identify the region for the new entity
		Region region = null;
		if (selectedEntity != null && selectedEntity instanceof DisplayEntity
				&& !(ent instanceof OverlayEntity)) {
			if (selectedEntity instanceof Region)
				region = (Region) selectedEntity;
			else
				region = ((DisplayEntity) selectedEntity).getCurrentRegion();
		}

		// Create the new entity
		String otName = ent.getObjectType().getName();
		int length = otName.length();
		String localName = ent.getLocalName();
		String copyName = localName;
		String sep = "_Copy";
		if (localName.length() >= length && localName.substring(0, length).equals(otName)) {
			String str = localName.substring(length);
			if (str.isEmpty() || Input.isInteger(str)) {
				copyName = otName;
				sep = "";
			}
		}
		if (region != null && region.getParent() != sim.getSimulation())
			copyName = region.getParent().getName() + "." + copyName;
		copyName = InputAgent.getUniqueName(sim, copyName, sep);
		InputAgent.storeAndExecute(new DefineCommand(sim, ent.getClass(), copyName));

		// Copy the inputs
		Entity copiedEnt = sim.getNamedEntity(copyName);
		copiedEnt.copyInputs(ent);

		// Ensure that a random generator has a unique stream number
		if (copiedEnt instanceof RandomStreamUser) {
			RandomStreamUser rsu = (RandomStreamUser) copiedEnt;
			setUniqueRandomSeed(rsu);
		}

		// Set the region
		if (region != null)
			InputAgent.applyArgs(copiedEnt, "Region", region.getName());

		// Set the position
		if (ent instanceof DisplayEntity) {
			DisplayEntity dEnt = (DisplayEntity) copiedEnt;

			// If an entity is not selected, paste the new entity at the point of interest
			if (selectedEntity == null || !(selectedEntity instanceof DisplayEntity)
					|| selectedEntity instanceof Region) {
				if (RenderManager.isGood())
					RenderManager.inst().dragEntityToMousePosition(dEnt);
			}

			// If an entity is selected, paste the new entity next to the selected one
			else {
				int x = 0;
				int y = 0;
				if (selectedEntity instanceof OverlayEntity) {
					OverlayEntity olEnt = (OverlayEntity) selectedEntity;
					x = olEnt.getScreenPosition().get(0) + 10;
					y = olEnt.getScreenPosition().get(1) + 10;
				}
				DisplayEntity selectedDispEnt = (DisplayEntity) selectedEntity;
				Vec3d pos = selectedDispEnt.getGlobalPosition();
				pos.x += 0.5d * selectedDispEnt.getSize().x;
				pos.y -= 0.5d * selectedDispEnt.getSize().y;
				pos = dEnt.getLocalPosition(pos);
				if (sim.getSimulation().isSnapToGrid())
					pos = sim.getSimulation().getSnapGridPosition(pos);
				try {
					dEnt.dragged(x, y, pos);
				}
				catch (InputErrorException e) {}
			}

			// Add a label if required
			if (sim.getSimulation().isShowLabels() && EntityLabel.canLabel(dEnt))
				EntityLabel.showTemporaryLabel(dEnt, true, true);
		}

		// Copy the children
		copyChildren(ent, copiedEnt);

		// Select the new entity
		FrameBox.setSelectedEntity(copiedEnt, false);
	}

	public void copyChildren(Entity parent0, Entity parent1) {
		JaamSimModel sim = getJaamSimModel();

		// Create the copied children
		for (Entity child : parent0.getChildren()) {
			if (child.isGenerated() || child instanceof EntityLabel)
				continue;

			// Construct the new child's name
			String localName = child.getLocalName();
			String name = parent1.getName() + "." + localName;

			// Create the new child
			InputAgent.storeAndExecute(new DefineCommand(sim, child.getClass(), name));

			// Add a label if necessary
			if (child instanceof DisplayEntity) {
				Entity copiedChild = parent1.getChild(localName);
				EntityLabel label = EntityLabel.getLabel((DisplayEntity) child);
				if (label != null) {
					EntityLabel newLabel = EntityLabel.createLabel((DisplayEntity) copiedChild, true);
					InputAgent.applyBoolean(newLabel, "Show", label.getShowInput());
					newLabel.setShow(label.getShow());
				}
			}
		}

		// Set the early and normal inputs for each child
		for (int seq = 0; seq < 2; seq++) {
			for (Entity child : parent0.getChildren()) {
				String localName = child.getLocalName();
				Entity copiedChild = parent1.getChild(localName);
				copiedChild.copyInputs(child, seq, false, false);
			}
		}

		// Ensure that any random stream inputs have a unique stream number
		for (Entity copiedChild : parent1.getChildren()) {
			if (!(copiedChild instanceof RandomStreamUser))
				continue;
			RandomStreamUser rsu = (RandomStreamUser) copiedChild;
			setUniqueRandomSeed(rsu);
		}

		// Copy each child's children
		for (Entity child : parent0.getChildren()) {
			String localName = child.getLocalName();
			Entity copiedChild = parent1.getChild(localName);
			copyChildren(child, copiedChild);
		}
	}

	public void setUniqueRandomSeed(RandomStreamUser rsu) {
		Simulation simulation = getJaamSimModel().getSimulation();
		int seed = rsu.getStreamNumber();
		if (seed >= 0 && simulation.getRandomStreamUsers(seed).size() <= 1)
			return;
		seed = simulation.getLargestStreamNumber() + 1;
		String key = rsu.getStreamNumberKeyword();
		InputAgent.applyIntegers((Entity) rsu, key, seed);
	}

	public void invokeNew() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				newModel();
			}
		});
	}

	public void invokeOpen() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				load();
			}
		});
	}

	public void invokeSave() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				save();
			}
		});
	}

	public void invokeSaveAs() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				saveAs();
			}
		});
	}

	public void invokeExit() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				close();
			}
		});
	}

	public void invokeCopy(Entity ent) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				copyToClipboard(ent);
			}
		});
	}

	public void invokePaste() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				pasteEntityFromClipboard();
			}
		});
	}

	public void invokeFind() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				FindBox.getInstance().showDialog();
			}
		});
	}

	public void invokeHelp() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String topic = "";
				if (selectedEntity != null)
					topic = selectedEntity.getObjectType().getName();
				HelpBox.getInstance().showDialog(topic);
			}
		});
	}

	public void invokeRunPause() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				controlStartResume.doClick();
			}
		});
	}

	public void invokeSimSpeedUp() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!spinner.isEnabled())
					return;
				spinner.setValue(spinner.getNextValue());
			}
		});
	}

	public void invokeSimSpeedDown() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if (!spinner.isEnabled())
					return;
				spinner.setValue(spinner.getPreviousValue());
			}
		});
	}

	@Override
	public String getDefaultFolder() {
		return GUIFrame.getConfigFolder();
	}

	public static String getConfigFolder() {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		return prefs.get(LAST_USED_FOLDER, new File(".").getAbsolutePath());
	}

	public static void setConfigFolder(String path) {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		prefs.put(LAST_USED_FOLDER, path);
	}

	public static String getImageFolder() {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		return prefs.get(LAST_USED_IMAGE_FOLDER, getConfigFolder());
	}

	public static void setImageFolder(String path) {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		prefs.put(LAST_USED_IMAGE_FOLDER, path);
	}

	public static String get3DFolder() {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		return prefs.get(LAST_USED_3D_FOLDER, getConfigFolder());
	}

	public static void set3DFolder(String path) {
		Preferences prefs = Preferences.userRoot().node(instance.getClass().getName());
		prefs.put(LAST_USED_3D_FOLDER, path);
	}

	/**
	 * Returns a list of the names of the files contained in the specified resource folder.
	 * @param folder - name of the resource folder
	 * @return names of the files in the folder
	 */
	public static ArrayList<String> getResourceFileNames(String folder) {
		ArrayList<String> ret = new ArrayList<>();

		try {
			URI uri = GUIFrame.class.getResource(folder).toURI();

			// When developing in an IDE
			if (uri.getScheme().equals("file")) {
				File dir = new File(uri.getPath());
				for (File file : dir.listFiles()) {
					ret.add(file.getName());
				}
			}

			// When running in a built jar or executable
			if (uri.getScheme().equals("jar")) {
				try {
					FileSystem fs = FileSystems.newFileSystem(uri, Collections.<String, Object>emptyMap());
					Path path = fs.getPath(folder);
					Stream<Path> walk = Files.walk(path, 1);
					for (Iterator<Path> it = walk.iterator(); it.hasNext();){
						Path each = it.next();
						String file = each.toString();
						if (file.length() > folder.length()) {
							ret.add(file.substring(folder.length() + 1));
						}
					}
					walk.close();
					fs.close();
				} catch (IOException e) {}
			}
		} catch (URISyntaxException e) {}

		return ret;
	}

	// ******************************************************************************************************
	// DIALOG BOXES
	// ******************************************************************************************************

	/**
	 * Shows the "Confirm Save As" dialog box
	 * @param fileName - name of the file to be saved
	 * @return true if the file is to be overwritten.
	 */
	public static boolean showSaveAsDialog(String fileName) {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		int userOption = JOptionPane.showConfirmDialog(null,
				String.format("The file '%s' already exists.\n" +
						"Do you want to replace it?", fileName),
				"Confirm Save As",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE);
		return (userOption == JOptionPane.YES_OPTION);
	}

	/**
	 * Shows the "Confirm Stop" dialog box.
	 * @return true if the run is to be stopped.
	 */
	public static boolean showConfirmStopDialog() {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		int userOption = JOptionPane.showConfirmDialog( null,
				"WARNING: Are you sure you want to reset the simulation time to 0?",
				"Confirm Reset",
				JOptionPane.YES_OPTION,
				JOptionPane.WARNING_MESSAGE );
		return (userOption == JOptionPane.YES_OPTION);
	}

	/**
	 * Shows the "Save Changes" dialog box
	 * @return true for any response other than Cancel or Close.
	 */
	public static boolean showSaveChangesDialog(GUIFrame gui) {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		JaamSimModel sim = getJaamSimModel();
		String message;
		if (sim.getConfigFile() == null)
			message = "Do you want to save the changes you made?";
		else
			message = String.format("Do you want to save the changes you made to '%s'?", sim.getConfigFile().getName());

		Object[] options = {"Save", "Don't Save", "Cancel"};
		int userOption = JOptionPane.showOptionDialog( null,
				message,
				"Save Changes",
				JOptionPane.YES_NO_CANCEL_OPTION,
				JOptionPane.WARNING_MESSAGE,
				null,
				options,
				options[0]);

		if (userOption == JOptionPane.YES_OPTION) {
			boolean confirmed = gui.save();
			return confirmed;
		}
		else if (userOption == JOptionPane.NO_OPTION)
			return true;

		return false;
	}

	private static String getErrorMessage(String source, int position, String pre, String message, String post) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>");

		// Initial text prior to the message
		if (!pre.isEmpty()) {
			sb.append(html_replace(pre)).append("<br><br>");
		}

		// Error message
		if (!message.isEmpty()) {
			sb.append(html_replace(message)).append("<br>");
		}

		// Append the source expression with the error shown in red
		if (source != null && !source.isEmpty()) {

			// Use the same font as the Input Builder so that the expression looks the same
			Font font = UIManager.getDefaults().getFont("TextArea.font");
			String preStyle = String.format("<pre style=\"font-family: %s; font-size: %spt\">",
					font.getFamily(), font.getSize());

			// Source expression
			sb.append(preStyle);
			sb.append(html_replace(source.substring(0, position)));
			sb.append("<font color=\"red\">");
			sb.append(html_replace(source.substring(position)));
			sb.append("</font>");
			sb.append("</pre>");
		}

		// Final text after the message
		if (!post.isEmpty()) {
			if (source == null || source.isEmpty()) {
				sb.append("<br>");
			}
			sb.append(html_replace(post));
		}

		sb.append("</html>");
		return sb.toString();
	}

	/**
	 * Displays the Error Message dialog box
	 * @param title - text for the dialog box name
	 * @param source - expression that cause the error (if applicable)
	 * @param position - location of the error in the expression (if applicable)
	 * @param pre - text to appear prior to the error message
	 * @param message - error message
	 * @param post - text to appear after the error message
	 */
	public static void showErrorDialog(String title, String source, int position, String pre, String message, String post) {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		JaamSimModel sim = getJaamSimModel();
		if (sim == null || sim.isBatchRun())
			GUIFrame.shutdown(1);
		JPanel panel = new JPanel();
		panel.setLayout( new BorderLayout() );

		// Use the standard font for dialog boxes
		Font messageFont = UIManager.getDefaults().getFont("OptionPane.messageFont");

		// Message
		JTextPane msgPane = new JTextPane();
		msgPane.setOpaque(false);
		msgPane.setFont(messageFont);
		msgPane.setText(pre + "\n\n" + message);
		panel.add(msgPane, BorderLayout.NORTH);

		// Source
		if (source != null && !source.isEmpty() && position != -1) {
			JTextPane srcPane = new JTextPane() {
				@Override
				public Dimension getPreferredScrollableViewportSize() {
					Dimension ret = getPreferredSize();
					ret.width = Math.min(ret.width, 900);
					ret.height = Math.min(ret.height, 300);
					return ret;
				}
			};
			srcPane.setContentType("text/html");
			String msg = GUIFrame.getErrorMessage(source, position, "", "", "");
			srcPane.setText(msg);
			JScrollPane scrollPane = new JScrollPane(srcPane);
			scrollPane.setBorder(new EmptyBorder(10, 0, 10, 0));
			panel.add(scrollPane, BorderLayout.CENTER);
		}

		// Additional information
		JTextPane postPane = new JTextPane();
		postPane.setOpaque(false);
		postPane.setFont(messageFont);
		postPane.setText(post);
		panel.add(postPane, BorderLayout.SOUTH);

		panel.setMinimumSize( new Dimension( 600, 300 ) );
		JOptionPane.showMessageDialog(null, panel, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showErrorDialog(String title, String pre, String message, String post) {
		GUIFrame.showErrorDialog(title, "", -1, pre, message, post);
	}

	public static void showErrorDialog(String title, String message) {
		GUIFrame.showErrorDialog(title, "", -1, "", message, "");
	}

	@Override
	public void invokeErrorDialogBox(String title, String msg) {
		GUIFrame.invokeErrorDialog(title, msg);
	}

	/**
	 * Shows the Error Message dialog box from a non-Swing thread
	 * @param title - text for the dialog box name
	 * @param msg - error message
	 */
	public static void invokeErrorDialog(String title, String msg) {
		invokeErrorDialog(title, "", msg, "");
	}

	public static void invokeErrorDialog(String title, String pre, String message, String post) {
		invokeErrorDialog(title, "", -1, pre, message, post);
	}

	public static void invokeErrorDialog(String title, String source, int position, String pre, String message, String post) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				GUIFrame.showErrorDialog(title, source, position, pre, message, post);
			}
		});
	}

	/**
	 * Shows the Error Message dialog box with option to open the Log Viewer
	 * @param title - text for the dialog box name
	 * @param msg - error message
	 */
	public static void showErrorOptionDialog(String title, String msg) {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		JaamSimModel sim = getJaamSimModel();
		if (sim == null || sim.isBatchRun())
			GUIFrame.shutdown(1);

		Object[] options = {"Yes", "No"};
		int userOption = JOptionPane.showOptionDialog(null,
				msg,
				title,
				JOptionPane.YES_NO_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[0]);

		if (userOption == JOptionPane.YES_OPTION) {
			KeywordIndex kw = InputAgent.formatBoolean("ShowLogViewer", true);
			InputAgent.storeAndExecute(new KeywordCommand(sim.getSimulation(), kw));
		}
	}

	/**
	 * Shows the Error Message dialog box for the Input Editor
	 * @param title - text for the dialog box name
	 * @param source - input text
	 * @param pos - index of the error in the input text
	 * @param pre - text to appear before the error message
	 * @param msg - error message
	 * @param post - text to appear after the error message
	 * @return true if the input is to be re-edited
	 */
	public static boolean showErrorEditDialog(String title, String source, int pos, String pre, String msg, String post) {
		if (RunProgressBox.hasInstance())
			RunProgressBox.getInstance().setShow(false);
		String message = GUIFrame.getErrorMessage(source, pos, pre, msg, post);
		String[] options = { "Edit", "Reset" };
		int reply = JOptionPane.showOptionDialog(null,
				message,
				title,
				JOptionPane.OK_CANCEL_OPTION,
				JOptionPane.ERROR_MESSAGE,
				null,
				options,
				options[0]);
		return (reply == JOptionPane.OK_OPTION);
	}

	// ******************************************************************************************************
	// TOOL TIPS
	// ******************************************************************************************************
	private static final Pattern amp = Pattern.compile("&");
	private static final Pattern lt = Pattern.compile("<");
	private static final Pattern gt = Pattern.compile(">");
	private static final Pattern br = Pattern.compile("\n");

	public static final String html_replace(String str) {
		String desc = str;
		desc = amp.matcher(desc).replaceAll("&amp;");
		desc = lt.matcher(desc).replaceAll("&lt;");
		desc = gt.matcher(desc).replaceAll("&gt;");
		desc = br.matcher(desc).replaceAll("<BR>");
		return desc;
	}

	/**
	 * Returns the HTML code for pop-up tooltips in the Model Builder and Control Panel.
	 * @param name - name of the item whose tooltip is to be generated
	 * @param desc - text describing the item's function
	 * @return HTML for the tooltip
	 */
	public static String formatToolTip(String name, String desc) {
		return String.format("<html><p width=\"200px\"><b>%s</b><br>%s</p></html>",
				name, desc);
	}

	/**
	 * Returns the HTML code for a keyword's pop-up tooltip in the Input Editor.
	 * @param className - object whose keyword tooltip is to be displayed
	 * @param keyword - name of the keyword
	 * @param description - description of the keyword
	 * @param exampleList - a list of examples that show how the keyword can be used
	 * @return HTML for the tooltip
	 */
	public static String formatKeywordToolTip(String className, String keyword,
			String description, String validInputs, String... exampleList) {

		StringBuilder sb = new StringBuilder("<html><p width=\"350px\">");

		// Keyword name
		String key = html_replace(keyword);
		sb.append("<b>").append(key).append("</b><br>");

		// Description
		String desc = html_replace(description);
		sb.append(desc).append("<br><br>");

		// Valid Inputs
		if (validInputs != null) {
			sb.append(validInputs).append("<br><br>");
		}

		// Examples
		if (exampleList.length > 0) {
			sb.append("<u>Examples:</u>");
		}
		for (int i=0; i<exampleList.length; i++) {
			String item = html_replace(exampleList[i]);
			sb.append("<br>").append(item);
		}

		sb.append("</p></html>");
		return sb.toString();
	}

	/**
	 * Returns the HTML code for an output's pop-up tooltip in the Output Viewer.
	 * @param name - name of the output
	 * @param description - description of the output
	 * @return HTML for the tooltip
	 */
	public static String formatOutputToolTip(String name, String description) {
		String desc = html_replace(description);
		return String.format("<html><p width=\"250px\"><b>%s</b><br>%s</p></html>",
				name, desc);
	}

	/**
	 * Returns the HTML code for an entity's pop-up tooltip in the Input Builder.
	 * @param name - entity name
	 * @param type - object type for the entity
	 * @param description - description for the entity
	 * @return HTML for the tooltip
	 */
	public static String formatEntityToolTip(String name, String type, String description) {
		String desc = html_replace(description);
		return String.format("<html><p width=\"250px\"><b>%s</b> (%s)<br>%s</p></html>",
				name, type, desc);
	}

	static ArrayList<Color4d> getFillColoursInUse(JaamSimModel simModel) {
		ArrayList<Color4d> ret = new ArrayList<>();
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class, FillEntity.class)) {
			FillEntity fillEnt = (FillEntity) ent;
			if (ret.contains(fillEnt.getFillColour()))
				continue;
			ret.add(fillEnt.getFillColour());
		}
		Collections.sort(ret, ColourInput.colourComparator);
		return ret;
	}

	static ArrayList<Color4d> getLineColoursInUse(JaamSimModel simModel) {
		ArrayList<Color4d> ret = new ArrayList<>();
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class, LineEntity.class)) {
			LineEntity lineEnt = (LineEntity) ent;
			if (ret.contains(lineEnt.getLineColour()))
				continue;
			ret.add(lineEnt.getLineColour());
		}
		Collections.sort(ret, ColourInput.colourComparator);
		return ret;
	}

	static ArrayList<String> getFontsInUse(JaamSimModel simModel) {
		ArrayList<String> ret = new ArrayList<>();
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class, TextEntity.class)) {
			TextEntity textEnt = (TextEntity) ent;
			if (ret.contains(textEnt.getFontName()))
				continue;
			ret.add(textEnt.getFontName());
		}
		Collections.sort(ret);
		return ret;
	}

	static ArrayList<Color4d> getFontColoursInUse(JaamSimModel simModel) {
		ArrayList<Color4d> ret = new ArrayList<>();
		for (DisplayEntity ent : simModel.getClonesOfIterator(DisplayEntity.class, TextEntity.class)) {
			TextEntity textEnt = (TextEntity) ent;
			if (ret.contains(textEnt.getFontColor()))
				continue;
			ret.add(textEnt.getFontColor());
		}
		Collections.sort(ret, ColourInput.colourComparator);
		return ret;
	}
}
