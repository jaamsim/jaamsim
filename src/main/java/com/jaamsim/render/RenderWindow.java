/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2012 Ausenco Engineering Canada Inc.
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
package com.jaamsim.render;

import java.awt.Frame;
import java.awt.Image;
import java.util.ArrayList;

import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;

import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;

public class RenderWindow {

	private Frame _awtFrame;
	private final GLWindow _window;
	private int _windowID;
	private int _viewID;
	private String _name;

	private final WindowInteractionListener _appListener;

	// These two members are almost certainly a bad idea and are just around to debug the picking
	private String _debugString = "";
	private ArrayList<Long> _debugIDs = new ArrayList<Long>();

	private ArrayList<Integer> _VAOs = new ArrayList<Integer>();

	RenderWindow(int x, int y, int width, int height, String title, String name,
	             GLContext sharedContext,
	             GLCapabilitiesImmutable caps,
	             GLEventListener glListener, Image icon, int windowID, int viewID,
	             WindowInteractionListener appListener) {

		_window = GLWindow.create(caps);

		_window.addGLEventListener(glListener);
		_window.setSharedContext(sharedContext);

		_awtFrame = new Frame(title);
		NewtCanvasAWT canvas = new NewtCanvasAWT(_window);
		_awtFrame.add(canvas);
		_awtFrame.setBounds(x, y, width, height);

		if (icon != null) {
			_awtFrame.setIconImage(icon);
		}

		_appListener = appListener;

		_windowID = windowID;
		_viewID = viewID;

		_name = name;

	}

	public GLWindow getGLWindowRef() {
		return _window;
	}

	public Frame getAWTFrameRef() {
		return _awtFrame;
	}

	public String getName() {
		return _name;
	}

	public int getWindowID() {
		return _windowID;
	}

	public int getViewID() {
		return _viewID;
	}

	public synchronized int getViewableWidth() {
		return _window.getWidth();
	}

	public synchronized int getViewableHeight() {
		return _window.getHeight();
	}

	public synchronized int getWindowWidth() {
		return _awtFrame.getBounds().width;
	}

	public synchronized int getWindowHeight() {
		return _awtFrame.getBounds().height;
	}

	public synchronized int getViewableX() {
		return _awtFrame.getBounds().x + _awtFrame.getInsets().left;
	}
	public synchronized int getViewableY() {
		return _awtFrame.getBounds().y + _awtFrame.getInsets().top;
	}

	public synchronized int getWindowX() {
		return _awtFrame.getBounds().x;
	}
	public synchronized int getWindowY() {
		return _awtFrame.getBounds().y;
	}

	// DEBUG (maybe)
	// TODO Review this mouse position caching....

	private boolean _mouseIn;
	private int _mouseX;
	private int _mouseY;

	public synchronized boolean isMouseInWindow() {
		return _mouseIn;
	}
	public synchronized void setMouseIn(boolean isIn) {
		_mouseIn = isIn;
	}

	public synchronized int getMouseX() {
		return _mouseX;
	}
	public synchronized void setMouseX(int x) {
		_mouseX = x;
	}

	public synchronized int getMouseY() {
		return _mouseY;
	}
	public synchronized void setMouseY(int y) {
		_mouseY = y;
	}

	// Hacky
	public synchronized void setDebugString(String s) {
		_debugString = s;
	}
	public synchronized String getDebugString() {
		return _debugString;
	}

	public synchronized void setDebugIDs(ArrayList<Long> ids) {
		_debugIDs = ids;
	}
	public synchronized ArrayList<Long> getDebugIDs() {
		return _debugIDs;
	}

	public WindowInteractionListener getWindowListener() {
		return _appListener;
	}

	public void addVAO(int vao) {
		_VAOs.add(vao);
	}

	public ArrayList<Integer> getVAOs() {
		return _VAOs;
	}
}
