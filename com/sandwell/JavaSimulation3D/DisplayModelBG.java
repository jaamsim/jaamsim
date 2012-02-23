/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
/*
 * The root branch which will be hooked up to DisplayEntity
 */
import java.util.ArrayList;
import java.util.Enumeration;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Group;
import javax.media.j3d.Link;
import javax.media.j3d.Material;
import javax.media.j3d.Node;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Transform3D;
import javax.media.j3d.TransformGroup;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3d;

import com.sandwell.JavaSimulation.DoubleVector;
import com.sandwell.JavaSimulation3D.util.Shape;

public class DisplayModelBG extends BranchGroup {

	protected ArrayList<Node> namedNodeList;

	private final TransformGroup rootTransform;	// Transformation (size and rotation will be controlled by DisplayEntity and DisplayModel respectively)
	private final Vector3d modelSize;

	public DisplayModelBG(BranchGroup bg, Vector3d size){
		this.setCapability(ALLOW_DETACH);
		namedNodeList = new ArrayList<Node>();
		this.populateNamedNodeListFor(bg);
		rootTransform = new TransformGroup();
		rootTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		rootTransform.addChild( bg );
		this.addChild(rootTransform);
		modelSize = size;
	}
	private Node getNode(String name){
		for(Node each: namedNodeList)
			if(each.getName().equalsIgnoreCase(name) )
				return each;
		return null;
	}

	public void setNode_Color(String name, ColoringAttributes color){
		ColoringAttributes[] colors = new ColoringAttributes[1];
		colors[0] = color;
		this.setNode_Color(name, colors);
	}

	public void setNode_Color(String name, ColoringAttributes[] colors){
		Node node = getNode(name);
		if(node == null)
			return;

		// 1)  Only one Shape (2D Graphics)
		if(node instanceof Shape){
			((Shape)node).setColor(colors);
			return;
		}

		// 2) A bunch of Shapes (2D Graphics)
		if(node instanceof BranchGroup && ((BranchGroup)node).getAllChildren().hasMoreElements() && ((BranchGroup)node).getChild(0) instanceof Shape){
			Enumeration<?> children = ((BranchGroup)node).getAllChildren();
			while(children.hasMoreElements()) {
				Shape each = (Shape)children.nextElement();
				each.setColor(colors);
			}
			return;
		}

		// 3) A 3D Model
		// one color for all the sub shape3d
		Color3f color3f = new Color3f();
		colors[0].getColor(color3f);
		ArrayList<Shape3D> shape3DList = DisplayModel.getShape3DsBelow(node);
		for(Shape3D each: shape3DList) {

			Appearance appearance = each.getAppearance();
			if(appearance != null) {
				appearance.setColoringAttributes(colors[0]);
				Material material = appearance.getMaterial();
				if(material != null) {
					material.setAmbientColor(color3f);
					material.setDiffuseColor(color3f);
				}
			}
		}
	}

	public void setNode_Visible(String name, boolean bool){
		Node node = getNode(name);
		if(node == null)
			return;

		ArrayList<Shape3D> shape3DList = DisplayModel.getShape3DsBelow(node);
		for(Shape3D each: shape3DList) {
			Appearance appearance = each.getAppearance();
			if(appearance != null) {
				RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
				if(renderingAttributes != null && renderingAttributes.getCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE) ) {
					renderingAttributes.setVisible(bool);
				}
			}
		}
	}

	public void setNode_Size(String name, double size){
		DoubleVector sizes = new DoubleVector(1);
		sizes.add(size);
		this.setNode_Size(name, sizes);
	}
	public void setNode_Size(String name, DoubleVector sizes){
		Node node = getNode(name);
		if(node == null)
			return;

		if(node instanceof Shape) {
			((Shape)node).setSize(sizes);
		}
	}

	public void setNode_Points(String name, ArrayList<Vector3d> points){
		Node node = getNode(name);
		if(node == null)
			return;

		if(node instanceof Shape) {
			((Shape)node).assignPointsPairs(points);
		}
	}

	public void setNode_LineStyle(String name, int style){
		Node node = getNode(name);
		if(node == null)
			return;

		if(node instanceof Shape) {
			((Shape)node).setLineStyle(style);
		}
	}

	private void populateNamedNodeListFor(BranchGroup node){
		populateNamedNodeList(node);
		for(Node each: namedNodeList){
			setNodeAllowChangeColorAndVisibleWrite(each);
		}
	}

	private void populateNamedNodeList(Node node) {
		if(node == null)
			return;

		if( DisplayModel.nodeHasValidTag(node) ){
			namedNodeList.add(node);
		}

		if (node instanceof Group) {
			Enumeration<?> enumeration = ((Group)node).getAllChildren();
			while (enumeration.hasMoreElements ()) {
				populateNamedNodeList((Node)enumeration.nextElement());
			}
		} else if (node instanceof Link) {
			populateNamedNodeList(((Link)node).getSharedGroup());
		}
	}

	private void setNodeAllowChangeColorAndVisibleWrite(Node node){
		ArrayList<Shape3D> shape3DList = DisplayModel.getShape3DsBelow(node);
		for(Shape3D each: shape3DList) {
			Appearance appearance = each.getAppearance();

			// Can not modify appearance if there is no appearance or the object is compiled
			if (appearance == null || each.isCompiled()) {
				continue;
			}

			appearance.setCapability(Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE);
			Material material = appearance.getMaterial();

			if(material == null) {
				material = new Material();
				appearance.setMaterial(material);
			}

			// Allow changing color
			material.setCapability(Material.ALLOW_COMPONENT_WRITE);
			RenderingAttributes renderingAttributes = appearance.getRenderingAttributes();
			if (renderingAttributes == null) {
				renderingAttributes = new RenderingAttributes();
				appearance.setCapability(Appearance.ALLOW_RENDERING_ATTRIBUTES_WRITE);
				appearance.setRenderingAttributes(renderingAttributes);
			}
			renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
		}
	}

	public void setModelSize(Vector3d size){
		// If the model has a non-zero size, set a scale factor that will scale it
		// to the desired size
		Vector3d scaleFactor = new Vector3d(modelSize);
		if (scaleFactor.x > 0.0d)
			scaleFactor.x = size.x / scaleFactor.x;

		if (scaleFactor.y > 0.0d)
			scaleFactor.y = size.y / scaleFactor.y;

		if (scaleFactor.z > 0.0d)
			scaleFactor.z = size.z / scaleFactor.z;

		Transform3D temp = new Transform3D();
		temp.setScale(scaleFactor);
		rootTransform.setTransform(temp);
	}
}
