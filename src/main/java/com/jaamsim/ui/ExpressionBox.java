/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2018 JaamSim Software Inc.
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.jaamsim.input.Input;
import com.jaamsim.input.Parser;

public class ExpressionBox extends JDialog {

	private final JTextArea editArea;
	private final JButton acceptButton;
	private final JButton cancelButton;
	private int result;

	public static final int CANCEL_OPTION = 1;  // Cancel button is clicked
	public static final int APPROVE_OPTION = 0; // Accept button is clicked
	public static final int ERROR_OPTION = -1;  // Error occurs or the dialog is dismissed

	public ExpressionBox(String str) {
		super((JDialog)null, "Expression Builder", true);

		getContentPane().setLayout( new BorderLayout() );
		setPreferredSize(new Dimension(800, 300));
		setIconImage(GUIFrame.getWindowIcon());
		setAlwaysOnTop(true);

		// Initial text
		ArrayList<String> tokens = new ArrayList<>();
		Parser.tokenize(tokens, str, true);


		// Input text
		editArea = new JTextArea();
		editArea.setText(Input.getValueString(tokens, true));
		JScrollPane scrollPane = new JScrollPane(editArea);
		scrollPane.setBorder(new EmptyBorder(5, 10, 0, 10));
		getContentPane().add(scrollPane, BorderLayout.CENTER);

		// Buttons
		acceptButton = new JButton("Accept");
		cancelButton = new JButton("Cancel");

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout( new FlowLayout(FlowLayout.CENTER) );
		buttonPanel.add(acceptButton);
		buttonPanel.add(cancelButton);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		pack();
		editArea.requestFocusInWindow();

		// Window closed event
		this.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				result = ERROR_OPTION;
				setVisible(false);
				dispose();
			}
		} );

		// Accept button
		acceptButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = APPROVE_OPTION;
				setVisible(false);
				dispose();
			}
		} );

		// Cancel button
		cancelButton.addActionListener( new ActionListener() {
			@Override
			public void actionPerformed( ActionEvent e ) {
				result = CANCEL_OPTION;
				setVisible(false);
				dispose();
			}
		} );
	}

	public int showDialog() {

		// Position the editor at the centre of the screen
		Rectangle winSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		setLocation((winSize.width - getWidth())/2, (winSize.height - getHeight())/2);

		// Show the dialog box and wait for editing to finish
		this.setVisible(true);

		// Return how the editing was completed
		return result;
	}

	public String getInputString() {
		return editArea.getText();
	}

}
