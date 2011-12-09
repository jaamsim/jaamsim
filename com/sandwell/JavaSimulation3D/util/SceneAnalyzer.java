/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2003-2011 Ausenco Engineering Canada Inc.
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
package com.sandwell.JavaSimulation3D.util;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Leaf;
import javax.media.j3d.Node;
import javax.media.j3d.TransformGroup;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Utility class provides the ability to traverse a scene graph and display to
 * the user.
 */
public class SceneAnalyzer {

	private static void printScene( Group root, int indent ) {
		// stores where we changed the capability
		boolean setRead = false;

		// force the display of children for traversing the tree
		if( !root.getCapability( Group.ALLOW_CHILDREN_READ ) ) {
			if( !root.isCompiled() ) {
				root.setCapability( Group.ALLOW_CHILDREN_READ );
				setRead = true;
			}
		}

		if( root.getCapability( Group.ALLOW_CHILDREN_READ ) ) {
			// display this node with indent
			describe( root, indent );

			// loop through all the children
			int num = root.numChildren();
			for( int i = 0; i < num; i++ ) {
				Node n = root.getChild( i );

				// if this child is a group, pass off the work
				if( n instanceof Shape2D )
					describe( n, indent + 1 );

				else if( n instanceof Group )
					printScene( (Group)n, indent + 1 );

				// if this child is a leaf, display it
				else
					describe( n, indent + 1 );
			}
		}
		else if( root instanceof Shape ) {

			System.out.print( getIndent( indent ) );
			System.out.println( "Compiled Group" );
		}

		// if we set capabilities for traversal, clear them
		if( setRead )
			root.clearCapability( Group.ALLOW_CHILDREN_READ );
	}

	public static void printScene( BranchGroup root ) {
		printScene( root, 0 );
	}

	private static void describe( Node n, int indent ) {
		System.out.print( getIndent( indent ) );
		if( n instanceof Shape2D )
			System.out.println( getDisplay( (Shape2D)n ) );

		else if( n instanceof BranchGroup )
			System.out.println( getDisplay( (BranchGroup)n ) );

		else if( n instanceof TransformGroup )
			System.out.println( getDisplay( (TransformGroup)n ) );

		else if( n instanceof Leaf )
			System.out.println( getDisplay( (Leaf)n ) );
	}

	private static String getDisplay( BranchGroup bg ) {
		return "[BG " + Integer.toHexString( bg.hashCode() ) + "] (" + bg.numChildren() + " children)";
	}

	private static String getDisplay( TransformGroup tg ) {
		return "[TG " + Integer.toHexString( tg.hashCode() ) + "] (" + tg.numChildren() + " children)";
	}

	private static String getDisplay( Shape2D s ) {
		return s.toString();
	}

	private static String getDisplay( Leaf l ) {
		return "[" + l + "]";
	}

	private static String getIndent( int indent ) {
		String ret = "";
		for( int i = 0; i < indent; i++ )
			ret = ret + "| ";
		return ret;
	}

	public static DefaultMutableTreeNode getTree( BranchGroup root ) {
		DefaultMutableTreeNode top = new DefaultMutableTreeNode( "Display Model" );
		buildSceneTree( root, top );
		return top;
	}

	private static void buildSceneTree( Group root, DefaultMutableTreeNode parent ) {
		// stores where we changed the capability
		boolean setRead = false;

		// force the display of children for traversing the tree
		if( !root.getCapability( Group.ALLOW_CHILDREN_READ ) ) {
			if( !root.isCompiled() ) {
				root.setCapability( Group.ALLOW_CHILDREN_READ );
				setRead = true;
			}
		}

		if( root.getCapability( Group.ALLOW_CHILDREN_READ ) || root instanceof Shape ) {
			// display this node with indent
			DefaultMutableTreeNode thisNode = buildTreeNode( root );
			parent.add( thisNode );

			// loop through all the children
			int num = root.numChildren();
			for( int i = 0; i < num; i++ ) {
				Node n = root.getChild( i );

				// if this child is a group, pass off the work
				if( n instanceof Shape2D || n instanceof Shape )
					thisNode.add( buildTreeNode( n ) );

				else if( n instanceof Group )
					buildSceneTree( (Group)n, thisNode );

				// if this child is a leaf, display it
				else
					thisNode.add( buildTreeNode( n ) );
			}
		}
		else {
			parent.add( new DefaultMutableTreeNode( "Compiled Group" ) );
		}

		// if we set capabilities for traversal, clear them
		if( setRead )
			root.clearCapability( Group.ALLOW_CHILDREN_READ );
	}

	private static DefaultMutableTreeNode buildTreeNode( Node n ) {
		String disp = null;
		if( n instanceof Shape2D )
			return ((Shape2D)n).getTreeNode();

		else if( n instanceof Shape )
			return ((Shape)n).getTreeNode();

		else if( n instanceof BranchGroup )
			disp = getDisplay( (BranchGroup)n );

		else if( n instanceof TransformGroup )
			disp = getDisplay( (TransformGroup)n );

		else if( n instanceof Leaf )
			disp = getDisplay( (Leaf)n );

		return new DefaultMutableTreeNode( disp );
	}
}
