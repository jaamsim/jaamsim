/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.DisplayModels;

import java.net.URI;
import java.util.ArrayList;

import javax.swing.filechooser.FileNameExtensionFilter;

import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.ImageProxy;
import com.jaamsim.render.OverlayTextureProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.VisibilityInfo;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.OverlayImage;

public class ImageModel extends DisplayModel {

	@Keyword(description = "The file containing the image to show, valid formats are: BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel ImageFile { ..\\images\\CompanyIcon.png }")
	private final FileInput imageFile;

	@Keyword(description = "Indicates the loaded image has an alpha channel (transparency information) that should be used",
	         example = "CompanyLogo Transparent { TRUE }")
	private final BooleanInput transparent;

	@Keyword(description = "Indicates the loaded image should use texture compression in video memory",
	         example = "WorldMap CompressedTexture { TRUE }")
	private final BooleanInput compressedTexture;

	private static final String[] validFileExtensions;
	private static final String[] validFileDescriptions;
	static {
		validFileExtensions = new String[5];
		validFileDescriptions = new String[5];

		validFileExtensions[0] = "JPG";
		validFileExtensions[1] = "PNG";
		validFileExtensions[2] = "GIF";
		validFileExtensions[3] = "BMP";
		validFileExtensions[4] = "PCX";

		validFileDescriptions[0] = "JPEG Image (*.jpg)";
		validFileDescriptions[1] = "Portable Network Graphics (*.png)";
		validFileDescriptions[2] = "Graphics Interchange Format (*.gif)";
		validFileDescriptions[3] = "Windows Bitmap (*.bmp)";
		validFileDescriptions[4] = "Personal Computer Exchange (*.pcx)";
	}

	{
		imageFile = new FileInput( "ImageFile", "DisplayModel", null );
		imageFile.setFileType("Image");
		imageFile.setValidFileExtensions(validFileExtensions);
		imageFile.setValidFileDescriptions(validFileDescriptions);
		this.addInput( imageFile);

		transparent = new BooleanInput("Transparent", "DisplayModel", false);
		this.addInput(transparent);

		compressedTexture = new BooleanInput("CompressedTexture", "DisplayModel", false);
		this.addInput(compressedTexture);

	}

	public ImageModel() {}

	@Override
	public DisplayModelBinding getBinding(Entity ent) {
		if (ent instanceof OverlayImage) {
			return new OverlayBinding(ent, this);
		}
		return new Binding(ent, this);
	}

	@Override
	public boolean canDisplayEntity(Entity ent) {
		return ent instanceof DisplayEntity;
	}

	public URI getImageFile() {
		return imageFile.getValue();
	}

	/**
	 * Compares the specified file extension to the list of valid extensions.
	 *
	 * @param str - the file extension to be tested.
	 * @return TRUE if the extension is valid.
	 */
	public static boolean isValidExtension(String str) {

		for (String ext : validFileExtensions) {
			if (str.equalsIgnoreCase(ext))
				return true;
		}
		return false;
	}

	/**
	 * Returns a file name extension filter for each of the supported file types.
	 *
	 * @return an array of file name extension filters.
	 */
	public static FileNameExtensionFilter[] getFileNameExtensionFilters() {
		return FileInput.getFileNameExtensionFilters("Image", validFileExtensions, validFileDescriptions);
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;

		private DisplayEntity dispEnt;

		private Transform transCache;
		private Vec3d scaleCache;
		private URI imageCache;
		private boolean compressedCache;
		private boolean transparentCache;
		private VisibilityInfo viCache;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)observee;
		}

		private void updateCache(double simTime) {
			Transform trans;
			Vec3d scale;
			long pickingID;
			if (dispEnt == null) {
				trans = Transform.ident;
				scale = DisplayModel.ONES;
				pickingID = 0;
			} else {
				trans = dispEnt.getGlobalTrans(simTime);
				scale = dispEnt.getSize();
				scale.mul3(getModelScale());
				pickingID = dispEnt.getEntityNumber();
			}

			URI imageName = imageFile.getValue();
			Boolean transp = transparent.getValue();
			Boolean compressed = compressedTexture.getValue();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(transCache, trans);
			dirty = dirty || dirty_vec3d(scaleCache, scale);
			dirty = dirty || !compare(imageCache, imageName);
			dirty = dirty || transparentCache != transp;
			dirty = dirty || compressedCache != compressed;
			dirty = dirty || !compare(viCache, vi);

			transCache = trans;
			scaleCache = scale;
			imageCache = imageName;
			transparentCache = transp;
			compressedCache = compressed;
			viCache = vi;

			if (cachedProxies != null && !dirty) {
				// Nothing changed
				registerCacheHit("ImageModel");
				return;
			}

			registerCacheMiss("ImageModel");
			// Gather some inputs

			cachedProxies = new ArrayList<RenderProxy>();
			cachedProxies.add(new ImageProxy(imageName, trans,
			                       scale, transp, compressed, vi, pickingID));

		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			// This is slightly quirky behaviour, as a null entity will be shown because we use that for previews
			if (dispEnt == null || !dispEnt.getShow()) {
				return;
			}

			updateCache(simTime);

			out.addAll(cachedProxies);
		}
	}

	private class OverlayBinding extends DisplayModelBinding {

		private OverlayImage imageObservee;

		private OverlayTextureProxy cachedProxy = null;

		private URI filenameCache;
		private IntegerVector posCache;
		private IntegerVector sizeCache;
		private boolean alignBottomCache;
		private boolean alignRightCache;
		private boolean transpCache;
		private VisibilityInfo viCache;

		public OverlayBinding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				imageObservee = (OverlayImage)observee;
			} catch (ClassCastException e) {
				// The observee is not a display entity
				imageObservee = null;
			}
		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (imageObservee == null || !imageObservee.getShow()) {
				return;
			}

			URI filename = imageFile.getValue();
			IntegerVector pos = imageObservee.getScreenPosition();
			IntegerVector size = imageObservee.getImageSize();

			boolean alignRight = imageObservee.getAlignRight();
			boolean alignBottom = imageObservee.getAlignBottom();
			boolean transp = transparent.getValue();

			VisibilityInfo vi = getVisibilityInfo();

			boolean dirty = false;

			dirty = dirty || !compare(filenameCache, filename);
			dirty = dirty || !compare(posCache, pos);
			dirty = dirty || !compare(sizeCache, size);
			dirty = dirty || alignRightCache != alignRight;
			dirty = dirty || alignBottomCache != alignBottom;
			dirty = dirty || transpCache != transp;
			dirty = dirty || !compare(viCache, vi);

			filenameCache = filename;
			posCache = pos;
			sizeCache = size;
			alignRightCache = alignRight;
			alignBottomCache = alignBottom;
			transpCache = transp;
			viCache = vi;

			if (cachedProxy != null && !dirty) {
				// Nothing changed

				out.add(cachedProxy);
				registerCacheHit("OverlayImage");
				return;
			}

			registerCacheMiss("OverlayImage");

			try {
				cachedProxy = new OverlayTextureProxy(pos.get(0), pos.get(1), size.get(0), size.get(1),
				                                      filename,
				                                      transp, false,
				                                      alignRight, alignBottom, vi);

				out.add(cachedProxy);
			} catch (ErrorException ex) {
				cachedProxy = null;
			}
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {
			// No selection widgets for now
		}
	}

}
