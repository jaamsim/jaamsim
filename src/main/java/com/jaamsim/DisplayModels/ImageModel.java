/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018-2019 JaamSim Software Inc.
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
package com.jaamsim.DisplayModels;

import java.net.URI;
import java.util.ArrayList;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.Graphics.OverlayImage;
import com.jaamsim.basicsim.Entity;
import com.jaamsim.basicsim.ErrorException;
import com.jaamsim.datatypes.IntegerVector;
import com.jaamsim.input.BooleanInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.FileInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.math.Transform;
import com.jaamsim.math.Vec2d;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.CachedTexLoader;
import com.jaamsim.render.DisplayModelBinding;
import com.jaamsim.render.ImageProxy;
import com.jaamsim.render.OverlayLineProxy;
import com.jaamsim.render.OverlayTextureProxy;
import com.jaamsim.render.RenderProxy;
import com.jaamsim.render.VisibilityInfo;

public class ImageModel extends DisplayModel {

	@Keyword(description = "The file containing the image to show, valid formats are: BMP, JPG, PNG, PCX, GIF.",
	         exampleList = {"../images/CompanyIcon.png"})
	private final FileInput imageFile;

	@Keyword(description = "Indicates the loaded image has an alpha channel (transparency information) that should be used",
	         exampleList = {"TRUE"})
	private final BooleanInput transparent;

	@Keyword(description = "Indicates the loaded image should use texture compression in video memory",
	         exampleList = {"TRUE"})
	private final BooleanInput compressedTexture;

	public static final String[] VALID_FILE_EXTENSIONS = {"JPG", "PNG", "GIF", "BMP", "PCX"};
	public static final String[] VALID_FILE_DESCRIPTIONS = {
			"JPEG Image (*.jpg)",
			"Portable Network Graphics (*.png)",
			"Graphics Interchange Format (*.gif)",
			"Windows Bitmap (*.bmp)",
			"Personal Computer Exchange (*.pcx)"};

	{
		imageFile = new FileInput( "ImageFile", GRAPHICS, null );
		imageFile.setFileType("Image");
		imageFile.setValidFileExtensions(VALID_FILE_EXTENSIONS);
		imageFile.setValidFileDescriptions(VALID_FILE_DESCRIPTIONS);
		this.addInput( imageFile);

		transparent = new BooleanInput("Transparent", GRAPHICS, false);
		this.addInput(transparent);

		compressedTexture = new BooleanInput("CompressedTexture", GRAPHICS, false);
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

		for (String ext : VALID_FILE_EXTENSIONS) {
			if (str.equalsIgnoreCase(ext))
				return true;
		}
		return false;
	}

	private class Binding extends DisplayModelBinding {

		private ArrayList<RenderProxy> cachedProxies;

		private final DisplayEntity dispEnt;

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
				trans = dispEnt.getGlobalTrans();
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

			CachedTexLoader loader = new CachedTexLoader(imageName, transp, compressed);
			cachedProxies = new ArrayList<>();
			cachedProxies.add(new ImageProxy(loader, trans, scale, vi, pickingID));

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
				                                      alignRight, alignBottom, vi, imageObservee.getEntityNumber());

				out.add(cachedProxy);
			} catch (ErrorException ex) {
				cachedProxy = null;
			}
		}

		@Override
		protected void collectSelectionBox(double simTime, ArrayList<RenderProxy> out) {

			double start = alignRightCache ? posCache.get(0) + sizeCache.get(0) : posCache.get(0);
			double end = alignRightCache ? posCache.get(0) : posCache.get(0) + sizeCache.get(0);
			double top = posCache.get(1);
			double bottom = posCache.get(1) + sizeCache.get(1);

			ArrayList<Vec2d> rect = new ArrayList<>(8);
			rect.add(new Vec2d( start, bottom ));
			rect.add(new Vec2d(   end, bottom ));
			rect.add(new Vec2d(   end, bottom ));
			rect.add(new Vec2d(   end, top    ));
			rect.add(new Vec2d(   end, top    ));
			rect.add(new Vec2d( start, top    ));
			rect.add(new Vec2d( start, top    ));
			rect.add(new Vec2d( start, bottom ));

			OverlayLineProxy outline = new OverlayLineProxy(rect, ColourInput.GREEN,
					!alignBottomCache, alignRightCache, 1, viCache, imageObservee.getEntityNumber());
			out.add(outline);
		}
	}

}
