/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2008-2011 Ausenco Engineering Canada Inc.
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.JTextArea;
import javax.swing.border.LineBorder;

public class EntityToolTip extends Thread {
	/** The ToolTip window box */
	private JWindow window;

	/** True => this tread is dead */
	protected boolean abort;

	/** After passing this time in milliseconds, the tooltip will be shown */
	protected int delayToDisplay = 200;

	/** Label inside the tooltip */
	JTextArea textArea;

	private static final Comparator<DisplayEntity > tooltipSorter;

	static {
		// Sort by classname and then by entity name in alphabetic order
		tooltipSorter = new Comparator<DisplayEntity>() {
			public int compare(DisplayEntity a, DisplayEntity b) {
				if (a.getClass() != b.getClass()) {
					// Different classes, return the ordering by classname
					return a.getClass().getName().compareTo(b.getClass().getName());
				} else {
					// Same class, return the ordering by case-insensitive name compare
					return a.getName().compareToIgnoreCase(b.getName());
				}
			}};
	}

	public EntityToolTip() {
		window = new JWindow();
		window.setAlwaysOnTop( true );
		textArea = new JTextArea();
		textArea.setEditable(false);
		textArea.setFont(  new Font("Verdana", Font.PLAIN, 11 ) );
		JPanel contentPane = (JPanel)window.getContentPane();
		contentPane.setBorder( new LineBorder(Color.black) );
		window.getContentPane().add(textArea, BorderLayout.CENTER);
		window.getContentPane().setBackground( new Color( 255, 255, 198 ) );
		abort = false;
	}

	// Abort (kill) the thread
	public void abort() {
		abort = true;
	}

	// Thread starts here
	public void run() {
		// The text which will be shown in the tooltip box
		final StringBuilder area = new StringBuilder();

		// While it has not been killed
		while( true ) {
			try {
				Thread.sleep(delayToDisplay);
				if (abort) {
					window.setVisible( false );
					window.dispose();
					window = null;
					return;
				}

				MouseEvent mouseEvent = OrbitBehavior.tootipEvent;
				OrbitBehavior orbitBehavior = OrbitBehavior.tooltipBehavior;
				if (mouseEvent == null || orbitBehavior == null) {
					window.setVisible(false);
					continue;
				}

				Sim3DWindow currentWindow = orbitBehavior.getWindow();
				if (currentWindow == null)
					continue;

				if(currentWindow.getPicker().getMenu().isVisible()) {
					continue;
				}

				ArrayList<DisplayEntity> entityList = currentWindow.getPicker().getEntityListWithToolTip(mouseEvent);
				if (entityList == null) {
					window.setVisible(false);
					continue;
				}

				Collections.sort(entityList, tooltipSorter);

				area.setLength(0);
				for (DisplayEntity each : entityList) {
					area.append(each.toString()).append("\n");
				}
				// Remove the last line break
				if (area.length() > 0) {
					area.setLength(area.length() - 1);
				}

				textArea.setText(area.toString());
				window.pack();
				int locX = currentWindow.getX() + mouseEvent.getX() + 10;
				int locY = currentWindow.getY() + mouseEvent.getY() + 30;
				window.setLocation(locX, locY);
				window.setVisible(true);
			}
			catch ( InterruptedException e ) {}
			catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
}
