/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.MeshFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URI;

import com.jaamsim.basicsim.Log;
import com.jaamsim.collada.ColParser;
import com.jaamsim.ui.LogBox;

public class BinaryExporter {

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: BinaryExporter [collada-input-filename] [jsb-binary-output-filename]");
			return;
		}

		String inputName = args[0];
		String outputName = args[1];

		try {
			ColParser.setKeepData(true);
			MeshData data = ColParser.parse(new URI("file:///" + inputName));
			DataBlock block = data.getDataAsBlock();
			File outFile = new File(outputName);
			FileOutputStream outStream = new FileOutputStream(outFile);
			BlockWriter.writeBlock(outStream, block);
		} catch (Exception ex) {
			Log.logException(ex);
		}
	}
}
