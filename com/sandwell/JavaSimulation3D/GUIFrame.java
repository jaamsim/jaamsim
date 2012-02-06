/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D;

import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.FileEntity;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation.Simulation;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JWindow;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import javax.swing.border.LineBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.vecmath.Point2d;
import javax.vecmath.Vector3d;

/**
 * The main window for a Graphical Simulation.  It provides the controls for managing then
 * EventManager (run, pause, ...) and the graphics (zoom, pan, ...)
 */
public class GUIFrame extends JFrame {
	private JMenu fileMenu;
	private JMenu viewMenu;
	private JMenu regionList;
	private JMenu windowList;
	private JMenu optionMenu;
	private JCheckBoxMenuItem showPosition;
	private JCheckBoxMenuItem showLatLong;
	private JCheckBoxMenuItem alwaysTop;
	private JCheckBoxMenuItem tooltip;
	private JMenuItem showEventViewer;
	private JMenuItem showEventTracker;
	private JMenuItem printInputItem;
	private JLabel clockDisplay;
	private JLabel speedUpDisplay;
	private JLabel remainingDisplay;

	private JToggleButton controlRealTime;
	private JSlider speedFactor;
	JLabel speedUpLabel;

	private JToggleButton controlStartResume;
	private JToggleButton controlStop;

	private JLabel locatorPos;
	private JLabel locatorLabel;

	private JToggleButton toolButtonTranslate;
	private JToggleButton toolButtonRotate;
	private JToggleButton toolButtonZoomBox;
	JButton toolButtonIsometric;
	JButton toolButtonXYPlane;
	JButton toolButtonUndo;
	JButton toolButtonRedo;

	private ButtonGroup toggleButtons;

	JToggleButton controlRecord; // button for screen captures
	JToggleButton addNode; // button for adding nodes

	private int lastValue = -1;
	private JProgressBar progressBar;
	private static Image iconImage;

	static {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			System.err.println("Unable to change look and feel.");
		}

		try {
			URL file = GUIFrame.class.getResource("/resources/images/icon.png");
			iconImage = Toolkit.getDefaultToolkit().getImage(file);
		}
		catch (Exception e) {
			System.err.println("Unable to load icon file.");
			iconImage = null;
		}
	}

	public GUIFrame(String windowName) {
		super(windowName);

		getContentPane().setLayout( new BorderLayout() );
		setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
		this.addWindowListener(new CloseListener());

		// Initialize the working environment
		initializeMenus();
		initializeMainToolBars();
		initializeStatusBar();

		this.setIconImage(GUIFrame.getWindowIcon());

		//Set window size and make visible
		pack();
		setResizable( false );
		setVisible( true );

		updateForRealTime();
		initialize();
		setProgress( 0 );
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled( false );
		JPopupMenu.setDefaultLightWeightPopupEnabled( false );
	}

	private class CloseListener extends WindowAdapter implements ActionListener {
		public void windowClosing(WindowEvent e) {
			GUIFrame.this.close();
		}

		public void actionPerformed( ActionEvent event ) {
			GUIFrame.this.close();
		}
	}

	/**
	 * Perform exit window duties
	 */
	void close() {

		// close warning/error trace file
		InputAgent.closeLogFile();

		// check for unsaved changes
		if (InputAgent.isSessionEdited()) {

			int userOption = JOptionPane.showConfirmDialog( null,
					"Do you want to save the changes?",
					"Confirm Exit Without Saving",
					JOptionPane.YES_NO_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE );

			if (userOption == JOptionPane.YES_OPTION) {
				InputAgent.save();
				System.exit( 0 );
			} else if (userOption == JOptionPane.NO_OPTION) {
				System.exit( 0 );
			}

		} else {
			System.exit( 0 );
		}
	}

	/**
	 * Clears the simulation and user interface for a new run
	 */
	public void clear() {

		// Clear the simulation
		DisplayEntity.simulation.clear();

		// Clear the title bar
		setTitle(DisplayEntity.simulation.getModelName());

		// Clear the status bar
		clockDisplay.setText( "------.--" );
		setProgress( 0 );
		speedUpDisplay.setText("------");
		remainingDisplay.setText("------");
		locatorPos.setText( "(-, -, -)" );

		// Read the autoload configuration file
		InputAgent.clear();
		InputAgent.readURL(InputAgent.class.getResource("/resources/inputs/autoload.cfg"));
	}

	public void initializeMenus() {

		// Initialize main menus
		JMenuBar mainMenuBar = new JMenuBar();

		// File menu creation
		fileMenu = new JMenu( "File" );
		fileMenu.setMnemonic( 'F' );
		fileMenu.setEnabled( false );

		JMenuItem newMenuItem = new JMenuItem( "New" );
		newMenuItem.setMnemonic( 'N' );
		newMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.simulation.pause();

				// check for unsaved changes
				if (InputAgent.isSessionEdited()) {

					int userOption = JOptionPane.showConfirmDialog( null,
					"A new simulation will overwrite the existing simulation without saving changes.  Do you wish to continue with a new simulation?",
					"Confirm New Simulation",
					JOptionPane.YES_OPTION,
					JOptionPane.WARNING_MESSAGE );

					if(userOption == JOptionPane.NO_OPTION) {
						return;
					}
				}
				clear();
				InputAgent.loadDefault();
				displayWindows();
			}
		} );
		fileMenu.add( newMenuItem );

		JMenuItem configMenuItem = new JMenuItem( "Open..." );
		configMenuItem.setMnemonic( 'O' );
		configMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				DisplayEntity.simulation.pause();

				// check for unsaved changes
				if (InputAgent.isSessionEdited()) {

					int userOption = JOptionPane.showConfirmDialog( null,
							"Opening a simulation will overwrite the existing simulation without saving changes.  Do you wish to continue opening a simulation?",
							"Confirm Open",
							JOptionPane.YES_OPTION,
							JOptionPane.WARNING_MESSAGE );

					if (userOption == JOptionPane.NO_OPTION) {
						return;
					}
				}
				InputAgent.load();
			}
		} );
		fileMenu.add( configMenuItem );

		JMenuItem saveConfigurationMenuItem = new JMenuItem( "Save" );
		saveConfigurationMenuItem.setMnemonic( 'S' );
		saveConfigurationMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.save();
			}
		} );
		fileMenu.add( saveConfigurationMenuItem );

		JMenuItem saveConfigurationAsMenuItem = new JMenuItem( "Save As..." );
		saveConfigurationAsMenuItem.setMnemonic( 'V' );
		saveConfigurationAsMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.saveAs();

			}
		} );
		fileMenu.add( saveConfigurationAsMenuItem );

		printInputItem = new JMenuItem( "Print Input Report" );
		printInputItem.setMnemonic( 'I' );
		printInputItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InputAgent.printInputFileKeywords();
			}
		} );
		fileMenu.add( printInputItem );

		JMenuItem exitMenuItem = new JMenuItem( "Exit" );
		exitMenuItem.setMnemonic( 'x' );
		exitMenuItem.addActionListener(new CloseListener());
		fileMenu.add( exitMenuItem );

		mainMenuBar.add( fileMenu );
		// End File menu creation

		// View menu creation
		viewMenu = new JMenu( "View" );
		viewMenu.setMnemonic( 'V' );

		JMenuItem objectPalletMenuItem = new JMenuItem( "Model Builder" );
		objectPalletMenuItem.setMnemonic( 'O' );
		objectPalletMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				EntityPallet.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( objectPalletMenuItem );

		JMenuItem objectSelectorMenuItem = new JMenuItem( "Object Selector" );
		objectSelectorMenuItem.setMnemonic( 'S' );
		objectSelectorMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				ObjectSelector.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( objectSelectorMenuItem );

		JMenuItem inputEditorMenuItem = new JMenuItem( "Input Editor" );
		inputEditorMenuItem.setMnemonic( 'I' );
		inputEditorMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				EditBox.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( inputEditorMenuItem );

		JMenuItem propertiesMenuItem = new JMenuItem( "Property Viewer" );
		propertiesMenuItem.setMnemonic( 'P' );
		propertiesMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				PropertyBox.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( propertiesMenuItem );

		JMenuItem outputsMenuItem = new JMenuItem( "Output Viewer" );
		outputsMenuItem.setMnemonic( 'U' );
		outputsMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				InfoBox.getInstance().setVisible(true);
			}
		} );
		viewMenu.add( outputsMenuItem );

		JMenuItem eventListMenuItem = new JMenuItem( "Event Viewer" );
		eventListMenuItem.setMnemonic( 'E' );
		eventListMenuItem.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				new EventViewer( DisplayEntity.simulation.getEventManager() );
			}
		} );
		viewMenu.add( eventListMenuItem );

		mainMenuBar.add( viewMenu );
		// End File menu creation

		// Window menu creation
		JMenu windowMenu = new JMenu( "Window" );
		windowMenu.setMnemonic( 'W' );

		// Intilize list of regions
		regionList = new RegionMenu("New Window");
		regionList.setMnemonic( 'N' );
		windowMenu.add( regionList );

		// Initialize list of windows
		windowList = new JMenu( "Select Window" );
		windowList.setMnemonic( 'S' );
		windowMenu.add( windowList );

		mainMenuBar.add( windowMenu );
		// End window menu creation

		optionMenu = new JMenu( "Options" );
		optionMenu.setMnemonic( 'O' );
		mainMenuBar.add( optionMenu );

		showPosition = new JCheckBoxMenuItem( "Show Position", true );
		showPosition.setMnemonic( 'P' );
		optionMenu.add( showPosition );

		showLatLong = new JCheckBoxMenuItem( "Show Lat Long", false );
		showLatLong.setMnemonic( 'g' );
		showLatLong.setEnabled( false );
		optionMenu.add( showLatLong );

		alwaysTop = new JCheckBoxMenuItem( "Always on top", false );
		alwaysTop.setMnemonic( 'A' );
		optionMenu.add( alwaysTop );
		alwaysTop.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				if( GUIFrame.this.isAlwaysOnTop() ) {
					GUIFrame.this.setAlwaysOnTop( false );
				}
				else {
					GUIFrame.this.setAlwaysOnTop( true );
				}
			}
		} );

		tooltip = new JCheckBoxMenuItem( "Tooltip", true );
		tooltip.setMnemonic( 'L' );
		optionMenu.add( tooltip );
		tooltip.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				OrbitBehavior.enableTooltip(((JCheckBoxMenuItem)e.getSource()).getState());
			}
		} );

		showPosition.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				setShowPositionXY();
			}
		} );

		showLatLong.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent e ) {
				setShowPositionLatLong();
			}
		} );

		// Help menu creation
		JMenu helpMenu = new JMenu( "Help" );
		helpMenu.setMnemonic( 'H' );


		if( Simulation.getCHMFile( "Java Simulation Environment" ) != null ) {
			JMenuItem spawnHelp = new JMenuItem( "Java Simulation Environment Manual" );
			spawnHelp.addActionListener( new ActionListener() {

				public void actionPerformed( ActionEvent event ) {
					Simulation.spawnHelp("Java Simulation Environment", "");
				}
			} );
			helpMenu.add(spawnHelp);
		}

		if( Simulation.getCHMFile( "Marine Shipping" ) != null ) {
			JMenuItem shipHelp = new JMenuItem( "Marine Shipping Manual" );
			shipHelp.addActionListener( new ActionListener() {

				public void actionPerformed( ActionEvent event ) {
					Simulation.spawnHelp("Marine Shipping", "");
				}
			} );
			helpMenu.add(shipHelp);
		}

		if( Simulation.getCHMFile( "Mine-to-Port Supply Chain" ) != null ) {
			JMenuItem bulkHelp = new JMenuItem( "Mine-to-Port Supply Chain Manual" );
			bulkHelp.addActionListener( new ActionListener() {

				public void actionPerformed( ActionEvent event ) {
					Simulation.spawnHelp("Mine-to-Port Supply Chain", "");
				}
			} );
			helpMenu.add(bulkHelp);
		}

		JMenuItem aboutMenu = new JMenuItem( "About" );
		aboutMenu.setMnemonic( 'A' );
		aboutMenu.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				new AboutBox();
			}
		} );
		helpMenu.add( aboutMenu );

		mainMenuBar.add( helpMenu );
		// End help menu creation

		// Add main menu to the window
		setJMenuBar( mainMenuBar );
	}

	private static class ViewButton extends JToggleButton implements ActionListener {
		private final int viewCode;

		public ViewButton(String iconLocation, String description, int viewCode) {
			super(new ImageIcon(GUIFrame.class.getResource(iconLocation)));
			this.setToolTipText(description);
			this.setMargin(new Insets(0, 0, 0, 0));
			this.setEnabled(false);
			this.viewCode = viewCode;
			this.addActionListener(this);
		}

		public void actionPerformed(ActionEvent event) {
			OrbitBehavior.setViewerBehaviour(viewCode);
		}
	}

	public void initializeMainToolBars() {


		// Insets used in setting the toolbar components
		Insets noMargin = new Insets( 0, 0, 0, 0 );
		Insets smallMargin = new Insets( 1, 1, 1, 1 );

		// Initilize the main toolbar
		JToolBar mainToolBar = new JToolBar();
		mainToolBar.setMargin( smallMargin );
		mainToolBar.setFloatable(false);

		// Create Run Label and run control buttons
		controlStartResume = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/run.png")));
		controlStartResume.setSelectedIcon(
				new ImageIcon(GUIFrame.class.getResource("/resources/images/pause.png")));
		controlStartResume.setToolTipText( "Run" );
		controlStartResume.setMargin( noMargin );
		controlStartResume.setEnabled( false );
		controlStartResume.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				JToggleButton startResume = (JToggleButton)event.getSource();
				if(startResume.isSelected()) {
					GUIFrame.this.startSimulation();
					startResume.setToolTipText( "Pause" );
				}
				else {
					GUIFrame.this.pauseSimulation();
					startResume.setToolTipText( "Run" );
				}
			}
		} );

		controlStop = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/stop.png")));
		controlStop.setToolTipText( "Stop" );
		controlStop.setMargin( noMargin );
		controlStop.setEnabled( false );
		controlStop.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING ) {
					GUIFrame.this.pauseSimulation();
				}
				int userOption = JOptionPane.showConfirmDialog( null,
						"WARNING: If you stop the run, it cannot continue from the present time and can only be re-started from time 0.  Do you really want to stop?",
						"Confirm Stop",
						JOptionPane.YES_OPTION,
						JOptionPane.WARNING_MESSAGE );

				// stop only if yes
				if (userOption == JOptionPane.YES_OPTION) {
					GUIFrame.this.stopSimulation();
				}
			}
		} );

		// Separators have 5 pixels before and after and the preferred height of controlStartResume button
		Dimension separatorDim = new Dimension(11, controlStartResume.getPreferredSize().height);

		// dimension for 5 pixels gaps
		Dimension gapDim = new Dimension(5, separatorDim.height);

		mainToolBar.add( controlStartResume );
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add( controlStop );

		// End run label and run control button creation

		// Create Real-Time scaling label and logarithmic slider
		speedUpLabel = new JLabel("1,000,000", JLabel.RIGHT);
		speedUpLabel.setBorder(LineBorder.createGrayLineBorder());

		// Set the preferred size to the maximum possible size of speedUpLabel (1,000,000)
		speedUpLabel.setPreferredSize(speedUpLabel.getPreferredSize());
		speedUpLabel.setText("10,000"); // Default value

		speedFactor = new JSlider(JSlider.HORIZONTAL, 0, 600, 400);
		speedFactor.addChangeListener( new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				int speed = (int)Math.pow(10, speedFactor.getValue() / 100.0d);
				DisplayEntity.simulation.setRealTimeFactor(speed);
				speedUpLabel.setText(String.format("%,d", speed));
			}
		} );

		//Turn on major tick marks.
		speedFactor.setMajorTickSpacing(100);
		speedFactor.setPaintTicks(true);
		speedFactor.setFocusable(false);

		mainToolBar.addSeparator(separatorDim);
		controlRealTime = new JToggleButton( "Real Time" );
		controlRealTime.setToolTipText( "Toggle Real Time" );
		controlRealTime.setMargin( smallMargin );
		controlRealTime.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				boolean enableRT = ((JToggleButton)event.getSource()).isSelected();
				DisplayEntity.simulation.setRealTimeFactor((int)Math.pow(10, speedFactor.getValue() / 100.0d));
				DisplayEntity.simulation.setRealTimeExecution(enableRT);
			}
		} );

		mainToolBar.add( controlRealTime );
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add( speedUpLabel );
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add(new JLabel("X"));
		mainToolBar.add(Box.createRigidArea(gapDim));
		mainToolBar.add( speedFactor );
		mainToolBar.addSeparator(separatorDim);
		// End creation of real-time label and menu

		// Create view control label and controls
		JLabel viewLabel = new JLabel( "   View Control:   " );

		toolButtonZoomBox = new ViewButton("/resources/images/zoomwindow.png", "Zoom box", OrbitBehavior.CHANGE_ZOOM_BOX);
		toolButtonTranslate = new ViewButton("/resources/images/trans.png", "Translate", OrbitBehavior.CHANGE_TRANSLATION);
		toolButtonRotate = new ViewButton("/resources/images/rotate.png", "Rotate", OrbitBehavior.CHANGE_ROTATION);

		toggleButtons = new ButtonGroup();
		toggleButtons.add( toolButtonTranslate );
		toggleButtons.add( toolButtonRotate );
		toggleButtons.add( toolButtonZoomBox );
		toolButtonTranslate.setSelected( true );

		mainToolBar.add( viewLabel );
		mainToolBar.add( toolButtonTranslate );
		mainToolBar.add( toolButtonRotate );
		mainToolBar.add( toolButtonZoomBox );
		mainToolBar.addSeparator(separatorDim);

		// add a button to show isometric view in windows
		toolButtonIsometric = new JButton( "Isometric" );
		toolButtonIsometric.setToolTipText( "Set Isometric View" );
		toolButtonIsometric.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				Sim3DWindow win = Sim3DWindow.lastActiveWindow;
				if (win != null)
					win.setViewerToIsometric();
			}
		} );
		mainToolBar.add( toolButtonIsometric );

		// add a button to show xy-plane view in windows
		toolButtonXYPlane = new JButton( "XY-Plane" );
		toolButtonXYPlane.setToolTipText( "Set XY-Plane View" );
		toolButtonXYPlane.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				Sim3DWindow win = Sim3DWindow.lastActiveWindow;
				if (win != null)
					win.setViewerToXYPlane();
			}
		} );
		mainToolBar.add( toolButtonXYPlane );

		mainToolBar.addSeparator(separatorDim);

		// add a button to undo the last step ( viewer and window )
		toolButtonUndo = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/previous.png")));
		toolButtonUndo.setToolTipText( "Previous view" );
		toolButtonUndo.setEnabled( false );
		toolButtonUndo.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				OrbitBehavior currentBehaviour = Sim3DWindow.lastActiveWindow.getBehavior();
				toolButtonUndo.setEnabled( currentBehaviour.undoPreviousStep() );
				toolButtonRedo.setEnabled( true );
			}
		} );
		mainToolBar.add( toolButtonUndo );

		// add a button to redo the last step ( viewer and window )
		toolButtonRedo = new JButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/next.png")));
		toolButtonRedo.setToolTipText( "Next view" );
		toolButtonRedo.setEnabled( false );
		toolButtonRedo.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				OrbitBehavior currentBehaviour = Sim3DWindow.lastActiveWindow.getBehavior();
				toolButtonRedo.setEnabled( currentBehaviour.redoPreviousStep() );
				toolButtonUndo.setEnabled( true );
			}
		} );
		mainToolBar.add( toolButtonRedo );
		mainToolBar.addSeparator(separatorDim);

		// add a button that allows mouse nodes to be added to segments
		addNode = new JToggleButton( " Add Node " );
		addNode.setToolTipText( "Add Node" );
		addNode.setEnabled( false );
		addNode.addActionListener( new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				if (addNode.isSelected())
					OrbitBehavior.setCreationBehaviour("add Node");
				else
					OrbitBehavior.setCreationBehaviour(null);
			}
		} );
		mainToolBar.add( addNode );

		// End creation of view control label and buttons

		// Create record button
		controlRecord = new JToggleButton(new ImageIcon(GUIFrame.class.getResource("/resources/images/record.png")));
		controlRecord.setToolTipText( "Record" );
		controlRecord.setMargin( noMargin );
		controlRecord.setEnabled( false );
		controlRecord.addActionListener( new ActionListener() {

			public void actionPerformed( ActionEvent event ) {
				if( controlRecord.isSelected() ) {
					startCapture();
				}
				else {
                    stopCapture();
				}
			}
		} );

		mainToolBar.addSeparator(separatorDim);
		mainToolBar.add( controlRecord );

		// Add toolbar to the window
		getContentPane().add( mainToolBar, BorderLayout.NORTH );
	}

	private static class RegionMenu extends JMenu implements MenuListener {

		RegionMenu(String text) {
			super(text);
			this.addMenuListener(this);
		}

		public void menuCanceled(MenuEvent e) {}

		public void menuDeselected(MenuEvent e) {
			this.removeAll();
		}

		public void menuSelected(MenuEvent e) {
			for (int i = 0; i < Region.getAll().size(); i++) {
				this.add(new WindowSpawner(Region.getAll().get(i)));
			}
		}
	}

	private static class WindowSpawner extends JMenuItem implements ActionListener {
		private final Region region;

		WindowSpawner(Region reg) {
			region = reg;
			this.setText(region.getName());
			this.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e) {
			DisplayEntity.simulation.spawnWindow(region);
		}
	}

	public void initializeStatusBar() {


		// Create the status bar
		JPanel statusBar = new JPanel();
		statusBar.setBorder( BorderFactory.createLineBorder( Color.darkGray ) );
		statusBar.setLayout( new FlowLayout( FlowLayout.LEFT, 10, 5 ) );

		// Create the display clock and label
		JLabel clockLabel = new JLabel( "Simulation Time (hrs):" );

		clockDisplay = new JLabel( "------.--", JLabel.RIGHT );
		clockDisplay.setPreferredSize( new Dimension( 55, 16 ) );
		clockDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );

		statusBar.add( clockLabel );
		statusBar.add( clockDisplay );
		//statusBar.addSeparator();

		// Create the progress bar
		progressBar = new JProgressBar( 0, 100 );
		progressBar.setValue( 0 );
		progressBar.setStringPainted( true );

		// Add the progress bar to the status bar
		statusBar.add( progressBar );

		// Create a speed-up factor display
		JLabel speedUpLabel = new JLabel( "Speed Up:" );
		speedUpDisplay = new JLabel( "------", JLabel.RIGHT );
		speedUpDisplay.setPreferredSize( new Dimension( 60, 16 ) );
		speedUpDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );

		statusBar.add( speedUpLabel );
		statusBar.add( speedUpDisplay );

		// Create a remaining run time display
		JLabel remainingLabel = new JLabel( "Time Remaining (mins):" );
		remainingDisplay = new JLabel( "------", JLabel.RIGHT );
		remainingDisplay.setPreferredSize( new Dimension( 40, 16 ) );
		remainingDisplay.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		statusBar.add( remainingLabel );
		statusBar.add( remainingDisplay );

		locatorPos = new JLabel( "(-, -, -)" );
		locatorPos.setPreferredSize( new Dimension( 140, 16 ) );
		locatorPos.setForeground( new Color( 1.0f, 0.0f, 0.0f ) );
		locatorLabel = new JLabel( "Pos:     " );
		statusBar.add( locatorLabel );
		statusBar.add( locatorPos );

		// Add the status bar to the window
		getContentPane().add( statusBar, BorderLayout.SOUTH );
	}

	public JMenu getWindowList() {

		return windowList;
	}

	public void setClock( double clockContents ) {
		clockDisplay.setText(String.format("%.2f", clockContents));
	}

	public void setProgress( int val ) {
		if (lastValue == val)
			return;

		progressBar.setValue( val );
		progressBar.repaint(25);
		lastValue = val;

		if (DisplayEntity.simulation.getSimulationState() >= Simulation.SIM_STATE_CONFIGURED) {
			String title = String.format("%d%% %s - %s", val, DisplayEntity.simulation.getModelName(), InputAgent.getRunName());
			setTitle(title);
		}
	}

	public void setProgressText( String txt ) {
		progressBar.setString( txt );
	}

	public void setSpeedUp( double val ) {
		speedUpDisplay.setText(String.format("%,.0f", val));
	}

	public void setRemaining( double val ) {
		remainingDisplay.setText(String.format("%.1f", val));
	}

	public void initialize() {
		controlStartResume.setSelected( false );
		controlStartResume.setEnabled( false );
		controlStop.setSelected( false );
		controlStop.setEnabled( false );
		clockDisplay.setText( "------.--" );
	}

	private void startSimulation() {
		OrbitBehavior.setViewerBehaviour( OrbitBehavior.CHANGE_TRANSLATION );
		if( DisplayEntity.simulation.getSimulationState() <= Simulation.SIM_STATE_CONFIGURED ) {
			if (InputAgent.isSessionEdited()) {
				InputAgent.saveAs();
			}
			DisplayEntity.simulation.start();
		}
		else if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_PAUSED ) {
			DisplayEntity.simulation.resume();
		}
		else if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_STOPPED ) {
			DisplayEntity.simulation.restart();
		}
		else
			throw new ErrorException( "Invalid Simulation State for Start/Resume" );
	}

	private void pauseSimulation() {
		if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING )
			DisplayEntity.simulation.pause();
		else
			throw new ErrorException( "Invalid Simulation State for pause" );
	}

	private void stopSimulation() {
		if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING ||
			DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_PAUSED )
			DisplayEntity.simulation.stop();
		else
			throw new ErrorException( "Invalid Simulation State for stop" );
	}

	public void startCapture() {
		if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_PAUSED ) {
			DisplayEntity.simulation.resume();
		}
		DisplayEntity.simulation.startExternalProcess("doCaptureNetwork");
	}

	public void stopCapture() {
		DisplayEntity.simulation.setCaptureFlag( false );
		if( DisplayEntity.simulation.getSimulationState() == Simulation.SIM_STATE_RUNNING ) {
			DisplayEntity.simulation.pause();
		}
	}

	public void updateForSimulationState() {
		switch( DisplayEntity.simulation.getSimulationState() ) {
			case Simulation.SIM_STATE_LOADED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				regionList.setEnabled( true );
				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				toolButtonZoomBox.setEnabled( true );
				toolButtonTranslate.setEnabled( true );
				toolButtonRotate.setEnabled( true );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				addNode.setEnabled( false );
				controlRecord.setEnabled( false );
				progressBar.setEnabled( false );
				if( showEventViewer != null )
					showEventViewer.setEnabled( false );
				if( showEventTracker != null )
					showEventTracker.setEnabled( false );
				break;

			case Simulation.SIM_STATE_UNCONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				regionList.setEnabled( true );
				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( false );
				remainingDisplay.setEnabled( false );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				toolButtonZoomBox.setEnabled( true );
				toolButtonTranslate.setEnabled( true );
				toolButtonRotate.setEnabled( true );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				addNode.setEnabled( false );
				controlRecord.setEnabled( false );
				progressBar.setEnabled( false );
				showPosition.setState( true );
				setShowPositionXY();
				if( showEventViewer != null )
					showEventViewer.setEnabled( false );
				if( showEventTracker != null )
					showEventTracker.setEnabled( false );
				break;

			case Simulation.SIM_STATE_CONFIGURED:
				for( int i = 0; i < fileMenu.getItemCount() - 1; i++ ) {
					fileMenu.getItem(i).setEnabled(true);
				}
				for( int i = 0; i < viewMenu.getItemCount(); i++ ) {
					viewMenu.getItem(i).setEnabled(true);
				}

				regionList.setEnabled( true );
				windowList.setEnabled( true );
				speedUpDisplay.setEnabled( true );
				remainingDisplay.setEnabled( true );
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStop.setSelected( false );
				controlStop.setEnabled( false );
				toolButtonZoomBox.setEnabled( true );
				toolButtonTranslate.setEnabled( true );
				toolButtonRotate.setEnabled( true );
				toolButtonIsometric.setEnabled( true );
				toolButtonXYPlane.setEnabled( true );
				addNode.setEnabled( true );
				controlRecord.setEnabled( true );
				progressBar.setEnabled( true );
				if( showEventViewer != null )
					showEventViewer.setEnabled( true );
				if( showEventTracker != null )
					showEventTracker.setEnabled( true );
				break;

			case Simulation.SIM_STATE_RUNNING:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( true );
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				toolButtonZoomBox.setSelected( false );
				toolButtonTranslate.setSelected( false );
				toolButtonRotate.setSelected( false );

				toolButtonTranslate.setSelected( true );
				OrbitBehavior.setViewerBehaviour(OrbitBehavior.CHANGE_TRANSLATION);

				if( showEventViewer != null )
					showEventViewer.setEnabled( true );
				if( showEventTracker != null )
					showEventTracker.setEnabled( true );
				break;

			case Simulation.SIM_STATE_PAUSED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStop.setEnabled( true );
				controlStop.setSelected( false );
				break;
			case Simulation.SIM_STATE_STOPPED:
				controlStartResume.setEnabled( true );
				controlStartResume.setSelected( false );
				controlStop.setEnabled( false );
				controlStop.setSelected( false );
				break;

			default:
				throw new ErrorException( "Unrecognized Graphics State" );
		}
		fileMenu.setEnabled( true );
	}

	public void updateForRealTime() {
		controlRealTime.setSelected( DisplayEntity.simulation.getRealTimeExecution() );

		//speedFactor.setText("" + simulation.getRealTimeFactor());
	}

	static Image getWindowIcon() {
		return iconImage;
	}

	public void copyLocationToClipBoard(Vector3d pos) {
		String data = String.format("(%.3f, %.3f, %.3f)", pos.x, pos.y, pos.z);
		StringSelection stringSelection = new StringSelection(data);
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		clipboard.setContents( stringSelection, null );
	}

	public void showLocatorPosition(Vector3d pos, Region region) {
		// null indicates nothing to display
		if( pos == null ) {
			locatorPos.setText( "(-, -, -)" );
		}
		else {
			if( showPosition.getState() ) {
				locatorPos.setText(String.format("(%.3f, %.3f, %.3f)",
					pos.x, pos.y, pos.z));
			}
			else if( showLatLong.getState() ){

				// If the background image has been rotated, transform the coordinates to lat and long by rotating clockwise about image center
				double theta = -1.0 * region.getImageAngle();
				if( theta != 0.0 ) {
					Vector3d cent = region.getImagePos();
					Point2d oldPt = new Point2d( pos.x, pos.y );
					Point2d newPt = Util.rotatePoint_degrees_center( oldPt, theta, cent );
					locatorPos.setText(String.format("(%.3f, %.3f)",
						region.getLatForY(newPt.y), region.getLongForX(newPt.x)));
				}
				else {
					locatorPos.setText(String.format("(%.3f, %.3f)",
						region.getLatForY(pos.y), region.getLongForX(pos.x)));
				}
			}
		}
	}

	public void enableShowLatLong() {
		showLatLong.setEnabled( true );
	}

	public void setShowPositionXY() {
		boolean show = showPosition.getState();
		showPosition.setState( show );
		if ( showLatLong.getState() ) {
			showLatLong.setState( ! show );
		}
		locatorLabel.setVisible( show );
		locatorPos.setVisible( show );
		locatorLabel.setText( "Pos: " );
		locatorPos.setText( "(-, -, -)" );
		OrbitBehavior.showPosition(show);
	}

	public void setShowPositionLatLong() {
		boolean show = showLatLong.getState();
		showLatLong.setState( show );
		if( showPosition.getState() ) {
			showPosition.setState( ! show );
		}
		locatorLabel.setVisible( show );
		locatorPos.setVisible( show );
		locatorLabel.setText( "Lat Long: " );
		locatorPos.setText( "(-, -)" );
		OrbitBehavior.showPosition(show);
	}

	public void setToolButtonUndoEnable( boolean bool ) {
		toolButtonUndo.setEnabled( bool );
	}

	public void setToolButtonRedoEnable( boolean bool ) {
		toolButtonRedo.setEnabled( bool );
	}

	public static void displayWindows() {
		EntityPallet.getInstance().setVisible(true);
		DisplayEntity.simulation.spawnWindow( DisplayEntity.simulation.getDefaultRegion() );
		EditBox.getInstance().setVisible(true);
		ObjectSelector.getInstance().setVisible(true);
	}

	// ******************************************************************************************************
	// MAIN
	// ******************************************************************************************************

	public static void main( String args[] ) {
		// Process the input arguments and filter out directives
		ArrayList<String> configFiles = new ArrayList<String>(args.length);
		boolean batch = false;
		boolean minimize = false;
		boolean quiet = false;

		for (String each : args) {
			// Batch mode
			if (each.equalsIgnoreCase("-b") ||
			    each.equalsIgnoreCase("-batch")) {
				batch = true;
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
			// Do not open default windows
			if (each.equalsIgnoreCase("-q") ||
					each.equalsIgnoreCase("-quiet")) {
				quiet = true;
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
			splashScreen.setBounds(splashX, splashY, imageIcon.getIconWidth(), imageIcon.getIconHeight());

			// Build the splash screen
			splashScreen.getContentPane().add(new JLabel(imageIcon));

			// Display it
			splashScreen.setVisible(true);
		}

		FileEntity.setRootDirectory(System.getProperty("user.dir"));

		// create a graphic simulation
		System.out.println( "Loading Simulation Environment ... " );
		System.out.flush();

		GraphicSimulation gsim = new GraphicSimulation();
		gsim.setName("Simulation");
		gsim.setInputName("Simulation");

		System.out.println( "Simulation Environment Loaded" );

		if (batch)
			InputAgent.setBatch(true);

		if (minimize)
			gsim.setMinimized();

		InputAgent.readURL(InputAgent.class.getResource("/resources/inputs/autoload.cfg"));

		if( configFiles.size() == 0 ) {
			InputAgent.loadDefault();

			if(! quiet) {
				displayWindows();
			}
		}

		// Hide the splash screen
		if (splashScreen != null) {
			splashScreen.dispose();
			splashScreen = null;
		}

		// Process any config files passed on command line
		for (int i = 0; i < configFiles.size(); i++) {
			// Consume regular configuration files
			gsim.configure(configFiles.get(i));
			continue;
		}

		// Start the model if in batch mode
		if (batch) {
			if (InputAgent.numErrors() > 0)
				System.exit(0);
			gsim.start();
		}
	}
}
