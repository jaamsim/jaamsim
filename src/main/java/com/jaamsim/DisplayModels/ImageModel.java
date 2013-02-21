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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec4d;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.ImageProxy;
import com.jaamsim.render.OverlayTextureProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.TexCache;
import com.sandwell.JavaSimulation.BooleanInput;
import com.sandwell.JavaSimulation.ChangeWatcher;
import com.sandwell.JavaSimulation.Entity;
import com.sandwell.JavaSimulation.ErrorException;
import com.sandwell.JavaSimulation.InputErrorException;
import com.sandwell.JavaSimulation.IntegerVector;
import com.sandwell.JavaSimulation.Keyword;
import com.sandwell.JavaSimulation.StringInput;
import com.sandwell.JavaSimulation.Util;
import com.sandwell.JavaSimulation3D.DisplayEntity;
import com.sandwell.JavaSimulation3D.OverlayImage;

public class ImageModel extends DisplayModel {

	@Keyword(desc = "The file containing the image to show, valid formats are: BMP, JPG, PNG, PCX, GIF.",
	         example = "Ship3DModel ImageFile { ..\\images\\CompanyIcon.png }")
	private final StringInput imageFile;

	@Keyword(desc = "Indicates the loaded image has an alpha channel (transparency information) that should be used",
	         example = "CompanyLogo Transparent { TRUE }")
	private final BooleanInput transparent;

	@Keyword(desc = "Indicates the loaded image should use texture compression in video memory",
	         example = "WorldMap CompressedTexture { TRUE }")
	private final BooleanInput compressedTexture;

	private static ArrayList<String> validFileExtentions;

	{
		imageFile = new StringInput( "ImageFile", "DisplayModel", null );
		this.addInput( imageFile, true);

		transparent = new BooleanInput("Transparent", "DisplayModel", false);
		this.addInput(transparent, true);

		compressedTexture = new BooleanInput("CompressedTexture", "DisplayModel", false);
		this.addInput(compressedTexture, true);

	}
	static {
		validFileExtentions = new ArrayList<String>();
		validFileExtentions.add("BMP");
		validFileExtentions.add("JPG");
		validFileExtentions.add("PNG");
		validFileExtentions.add("PCX");
		validFileExtentions.add("GIF");
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

	public String getImageFile() {
		return imageFile.getValue();
	}

	@Override
	public void validate() {
		String ext = Util.getFileExtention(imageFile.getValue());
		if(! validFileExtentions.contains(ext)){
			throw new InputErrorException("Invalid file format \"%s\"", imageFile.getValue());
		}
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker modelTracker;

		private DisplayEntity dispEnt;

		public Binding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			dispEnt = (DisplayEntity)observee;

			if (dispEnt != null) {
				observeeTracker = dispEnt.getGraphicsChangeTracker();
			}
			modelTracker = dm.getGraphicsChangeTracker();
		}

		private void updateCache(double simTime) {
			if (cachedProxies != null && observeeTracker != null
			    && !observeeTracker.checkAndClear()
			    && !modelTracker.checkAndClear()) {
				// Nothing changed
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			// Gather some inputs
			Transform trans;
			Vec4d scale;
			long pickingID;
			if (dispEnt == null) {
				trans = Transform.ident;
				scale = Vec4d.ONES;
				pickingID = 0;
			} else {
				trans = dispEnt.getGlobalTrans(simTime);
				scale = dispEnt.getJaamMathSize(getModelScale());
				pickingID = dispEnt.getEntityNumber();
			}

			cachedProxies = new ArrayList<RenderProxy>();
			try {
				cachedProxies.add(new ImageProxy(new URL(Util.getAbsoluteFilePath(imageFile.getValue())), trans,
				                       scale, transparent.getValue(), compressedTexture.getValue(), getVisibilityInfo(), pickingID));
			} catch (MalformedURLException e) {
				cachedProxies.add(new ImageProxy(TexCache.BAD_TEXTURE, trans, scale,
				                                 transparent.getValue(), compressedTexture.getValue(), getVisibilityInfo(), pickingID));
			}

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
		private ChangeWatcher.Tracker observeeTracker;
		private ChangeWatcher.Tracker modelTracker;

		private OverlayTextureProxy cachedProxy = null;

		public OverlayBinding(Entity ent, DisplayModel dm) {
			super(ent, dm);
			try {
				imageObservee = (OverlayImage)observee;
				if (imageObservee != null) {
					observeeTracker = imageObservee.getGraphicsChangeTracker();
				}
			} catch (ClassCastException e) {
				// The observee is not a display entity
				imageObservee = null;
			}
			modelTracker = dm.getGraphicsChangeTracker();

		}

		@Override
		public void collectProxies(double simTime, ArrayList<RenderProxy> out) {
			if (imageObservee == null || !imageObservee.getShow()) {
				return;
			}

			if (!modelTracker.checkAndClear() &&
			    !observeeTracker.checkAndClear() &&
			    cachedProxy != null) {
				// Nothing changed

				out.add(cachedProxy);
				++_cacheHits;
				return;
			}

			++_cacheMisses;
			registerCacheMiss("OverlayImage");

			String filename = imageFile.getValue();
			IntegerVector pos = imageObservee.getScreenPos();
			IntegerVector size = imageObservee.getImageSize();

			boolean alignRight = imageObservee.getAlignRight();
			boolean alignBottom = imageObservee.getAlignBottom();

			try {
				cachedProxy = new OverlayTextureProxy(pos.get(0), pos.get(1), size.get(0), size.get(1),
				                                      new URL(Util.getAbsoluteFilePath(filename)),
				                                      transparent.getValue(), false,
				                                      alignRight, alignBottom, getVisibilityInfo());

				out.add(cachedProxy);
			} catch (MalformedURLException ex) {
				cachedProxy = null;
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
