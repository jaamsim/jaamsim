/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2002-2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2019-2020 JaamSim Software Inc.
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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JTextArea;

/**
 * Class to display information about model objects.
 */
public class AboutBox extends JDialog {

	public static final String softwareName = "JaamSim";
	public static final String version = "2024-03";

	public AboutBox() {
		super((JDialog)null, "About", true);
		setType(Type.UTILITY);

		setResizable(false);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		// setup the layout
		int index = 0;
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);

		// build the default constraints
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridx = index++;
		constraints.gridy = 0;

		constraints.insets = new Insets( 15, 15, 0, 15 );

		// display the model's name
		StringBuilder name = new StringBuilder("<html>");
		name.append(softwareName).append("<br>");
		name.append("Version: ").append(version).append("</html>");
		JLabel lab = new JLabel(name.toString());
		lab.setFont(FrameBox.boldFont);
		layout.setConstraints( lab, constraints );
		getContentPane().add( lab );

		StringBuilder msg = new StringBuilder("NOTICE:\n\n");
		msg.append("Licensed under the Apache License, Version 2.0 (the \"License\");\n");
		msg.append("you may not use this file except in compliance with the License.\n");
		msg.append("You may obtain a copy of the License at\n");
		msg.append("\n");
		msg.append("   http://www.apache.org/licenses/LICENSE-2.0\n");
 		msg.append("\n");
		msg.append("Unless required by applicable law or agreed to in writing, software\n");
		msg.append("distributed under the License is distributed on an \"AS IS\" BASIS,\n");
		msg.append("WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
		msg.append("See the License for the specific language governing permissions and\n");
		msg.append("limitations under the License.");

		JTextArea area = new JTextArea(msg.toString());
		area.setEditable(false);
		area.setFocusable(false);
		area.setBackground(lab.getBackground());
		area.setFont(FrameBox.boldFont);
		constraints.gridy = index++;
		layout.setConstraints( area, constraints );
		getContentPane().add( area );

		JButton closeButton = new JButton("Close");
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});

		constraints.gridy = index++;
		constraints.insets = new Insets( 10, 75, 15, 75 );
		constraints.anchor = GridBagConstraints.CENTER;
		layout.setConstraints( closeButton, constraints );
		getContentPane().add( closeButton );

		setSize( 300, 150 );
		pack();
	}

}
