package com.jaamsim.MeshFiles;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

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
			MeshData data = ColParser.parse(new URL("file:///" + inputName));
			DataBlock block = data.getDataAsBlock();
			File outFile = new File(outputName);
			FileOutputStream outStream = new FileOutputStream(outFile);
			BlockWriter.writeBlock(outStream, block);
		} catch (Exception ex) {
			LogBox.renderLogException(ex);
		}
	}
}
