package com.jaamsim.render;

import java.net.URI;
import java.util.ArrayList;

public class OverlayTextureProxy implements RenderProxy {

	private int _x, _y;
	private int _width, _height;

	private URI _imageURI;

	private boolean _isTransparent;
	private boolean _isCompressed;
	private boolean _alignRight, _alignBottom;
	private VisibilityInfo _visInfo;

	private OverlayTexture cached;

	public OverlayTextureProxy(int x, int y, int width, int height, URI imageURI, boolean transparent, boolean compressed,
	                           boolean alignRight, boolean alignBottom, VisibilityInfo visInfo) {
		_x = x; _y = y;
		_width = width; _height = height;
		_imageURI = imageURI;
		_isTransparent = transparent; _isCompressed = compressed;
		_alignRight = alignRight; _alignBottom = alignBottom;
		_visInfo = visInfo;
	}

	@Override
	public void collectRenderables(Renderer r, ArrayList<Renderable> outList) {
		return;
	}

	@Override
	public void collectOverlayRenderables(Renderer r,
			ArrayList<OverlayRenderable> outList) {
		if (cached == null) {
			cached = new OverlayTexture(_x, _y, _width, _height, _imageURI, _isTransparent, _isCompressed,
			                            _alignRight, _alignBottom, _visInfo);
		}
		outList.add(cached);
	}

}
