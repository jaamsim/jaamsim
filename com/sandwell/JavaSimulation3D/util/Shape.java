/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2004-2011 Ausenco Engineering Canada Inc.
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

import java.util.HashMap;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.RenderingAttributes;
import javax.media.j3d.Shape3D;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Point3d;

import com.sandwell.JavaSimulation.DoubleVector;

/**
 * Class from which all particular graphic elements can inherit from.
 * DisplayEntity will maintain a list of the current shapes in a 'layered'
 * approach for use with the OrderedGroup type.
 */
public abstract class Shape extends BranchGroup {

	public static final int SHAPE_OUTLINE = 0;
	public static final int SHAPE_FILLED = 1;
	public static final int SHAPE_FILLED_STRIP = 2;


	public static final int COLOR_WHITE = 0;
	public static final int COLOR_BLACK = 1;
	public static final int COLOR_RED = 2;
	public static final int COLOR_GREEN = 3;
	public static final int COLOR_BLUE = 4;
	public static final int COLOR_CYAN = 5;
	public static final int COLOR_YELLOW = 6;
	public static final int COLOR_PURPLE = 7;
	public static final int COLOR_LIGHT_GREY = 8;
	public static final int COLOR_MED_GREY = 9;
	public static final int COLOR_DARK_RED = 10;
	public static final int COLOR_DARK_GREEN = 11;
	public static final int COLOR_DARK_BLUE = 12;
	public static final int COLOR_DARK_CYAN = 13;
	public static final int COLOR_DARK_YELLOW = 14;
	public static final int COLOR_DARK_PURPLE = 15;

	public static final int POLYGON_LINES = 0;
	public static final int POLYGON_FILL_BOTH = 1;
	public static final int POLYGON_FILL_FRONT = 2;

	public static final int LINE_SOLID_1PX = 0;
	public static final int LINE_DASH_1PX = 1;
	public static final int LINE_DOT_1PX = 2;
	public static final int LINE_SOLID_2PX = 3;
	public static final int LINE_DASH_2PX = 4;
	public static final int LINE_DOT_2PX = 5;
	public static final int LINE_SOLID_3PX = 6;
	public static final int LINE_DASH_3PX = 7;
	public static final int LINE_DOT_3PX = 8;


	private static final ColoringAttributes[] colorList;
	private static final HashMap<String, ColoringAttributes> colorMap = new HashMap<String, ColoringAttributes>(238);
	private static final PolygonAttributes[] polygonAttributeList;
	private static final LineAttributes[] lineAttributeList;

	protected String name; // Allow the shapes to carry name information
	protected String nameOfTheCallingVariable; // Name of the variable which contains this shape
	private int layer; // The currently set layer for the shape, used to display

	protected Appearance appearance; // The appearance of the shape primitive
	protected Shape3D shape; // Holds the geometry and Appearance

	protected double centreX;
	protected double centreY;
	protected double centreZ;

	static {
		colorList = new ColoringAttributes[16];
		colorList[Shape.COLOR_WHITE] =
			new ColoringAttributes(1.0f, 1.0f, 1.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_BLACK] =
			new ColoringAttributes(0.0f, 0.0f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_RED] =
			new ColoringAttributes(1.0f, 0.0f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_GREEN] =
			new ColoringAttributes(0.0f, 1.0f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_BLUE] =
			new ColoringAttributes(0.0f, 0.0f, 1.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_CYAN] =
			new ColoringAttributes(0.0f, 1.0f, 1.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_YELLOW] =
			new ColoringAttributes(1.0f, 1.0f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_PURPLE] =
			new ColoringAttributes(1.0f, 0.0f, 1.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_LIGHT_GREY] =
			new ColoringAttributes(0.75f, 0.75f, 0.75f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_MED_GREY] =
			new ColoringAttributes(0.5f, 0.5f, 0.5f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_RED] =
			new ColoringAttributes(0.75f, 0.0f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_GREEN] =
			new ColoringAttributes(0.0f, 0.75f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_BLUE] =
			new ColoringAttributes(0.0f, 0.0f, 0.75f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_CYAN] =
			new ColoringAttributes(0.0f, 0.75f, 0.75f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_YELLOW] =
			new ColoringAttributes(0.75f, 0.75f, 0.0f, ColoringAttributes.FASTEST);
		colorList[Shape.COLOR_DARK_PURPLE] =
			new ColoringAttributes(0.75f, 0.0f, 0.75f, ColoringAttributes.FASTEST);

		// Populate the color map
		addColor("0", colorList[0]);
		addColor("1", colorList[1]);
		addColor("2", colorList[2]);
		addColor("3", colorList[3]);
		addColor("4", colorList[4]);
		addColor("5", colorList[5]);
		addColor("6", colorList[6]);
		addColor("7", colorList[7]);
		addColor("8", colorList[8]);
		addColor("9", colorList[9]);
		addColor("10", colorList[10]);
		addColor("11", colorList[11]);
		addColor("12", colorList[12]);
		addColor("13", colorList[13]);
		addColor("14", colorList[14]);
		addColor("15", colorList[15]);
		addColor( "lavenderblush", 255, 240, 245 );
		addColor( "pink", 255, 192, 203 );
		addColor( "lightpink", 255, 182, 193 );
		addColor( "palevioletred", 219, 112, 147 );
		addColor( "hotpink", 255, 105, 180 );
		addColor( "deeppink", 255, 20, 147 );
		addColor( "violetred", 208, 32, 144 );
		addColor( "mediumvioletred", 199, 21, 133 );
		addColor( "raspberry", 135, 38, 87 );
		addColor( "thistle", 216, 191, 216 );
		addColor( "plum", 221, 160, 221 );
		addColor( "orchid", 218, 112, 214 );
		addColor( "violet", 238, 130, 238 );
		addColor( "magenta", 255, 0, 255 );
		addColor( "purple", 128, 0, 128 );
		addColor( "mediumorchid", 186, 85, 211 );
		addColor( "darkorchid", 153, 50, 204 );
		addColor( "darkviolet", 148, 0, 211 );
		addColor( "blueviolet", 138, 43, 226 );
		addColor( "indigo", 75, 0, 130 );
		addColor( "mediumpurple", 147, 112, 219 );
		addColor( "lightslateblue", 132, 112, 255 );
		addColor( "mediumslateblue", 123, 104, 238 );
		addColor( "slateblue", 106, 90, 205 );
		addColor( "darkslateblue", 72, 61, 139 );
		addColor( "ghostwhite", 248, 248, 255 );
		addColor( "lavender", 230, 230, 250 );
		addColor( "blue", 0, 0, 255 );
		addColor( "darkblue", 0, 0, 139 );
		addColor( "navy", 0, 0, 128 );
		addColor( "midnightblue", 25, 25, 112 );
		addColor( "cobalt", 61, 89, 171 );
		addColor( "royalblue", 65, 105, 225 );
		addColor( "cornflowerblue", 100, 149, 237 );
		addColor( "lightsteelblue", 176, 196, 222 );
		addColor( "lightslategray", 119, 136, 153 );
		addColor( "slategray", 112, 128, 144 );
		addColor( "dodgerblue", 30, 144, 255 );
		addColor( "aliceblue", 240, 248, 255 );
		addColor( "powderblue", 176, 224, 230 );
		addColor( "lightblue", 173, 216, 230 );
		addColor( "lightskyblue", 135, 206, 250 );
		addColor( "skyblue", 135, 206, 235 );
		addColor( "deepskyblue", 0, 191, 255 );
		addColor( "peacock", 51, 161, 201 );
		addColor( "steelblue", 70, 130, 180 );
		addColor( "darkturquoise", 0, 206, 209 );
		addColor( "cadetblue", 95, 158, 160 );
		addColor( "azure", 240, 255, 255 );
		addColor( "lightcyan", 224, 255, 255 );
		addColor( "paleturquoise", 187, 255, 255 );
		addColor( "cyan", 0, 255, 255 );
		addColor( "turquoise", 64, 224, 208 );
		addColor( "mediumturquoise", 72, 209, 204 );
		addColor( "lightseagreen", 32, 178, 170 );
		addColor( "manganeseblue", 3, 168, 158 );
		addColor( "teal", 0, 128, 128 );
		addColor( "darkslategray", 47, 79, 79 );
		addColor( "turquoiseblue", 0, 199, 140 );
		addColor( "aquamarine", 127, 255, 212 );
		addColor( "mintcream", 245, 255, 250 );
		addColor( "mint", 189, 252, 201 );
		addColor( "seagreen", 84, 255, 159 );
		addColor( "mediumspringgreen", 0, 250, 154 );
		addColor( "springgreen", 0, 255, 127 );
		addColor( "emeraldgreen", 0, 201, 87 );
		addColor( "mediumseagreen", 60, 179, 113 );
		addColor( "cobaltgreen", 61, 145, 64 );
		addColor( "darkseagreen", 143, 188, 143 );
		addColor( "honeydew", 240, 255, 240 );
		addColor( "palegreen", 152, 251, 152 );
		addColor( "lawngreen", 124, 252, 0 );
		addColor( "greenyellow", 173, 255, 47 );
		addColor( "limegreen", 50, 205, 50 );
		addColor( "forestgreen", 34, 139, 34 );
		addColor( "sapgreen", 48, 128, 20 );
		addColor( "green", 0, 128, 0 );
		addColor( "darkgreen", 0, 100, 0 );
		addColor( "darkolivegreen", 85, 107, 47 );
		addColor( "olivedrab", 107, 142, 35 );
		addColor( "olive", 128, 128, 0 );
		addColor( "ivory", 255, 255, 240 );
		addColor( "lightyellow", 255, 255, 224 );
		addColor( "lightgoldenrodyellow", 250, 250, 210 );
		addColor( "cornsilk", 255, 248, 220 );
		addColor( "lemonchiffon", 255, 250, 205 );
		addColor( "beige", 245, 245, 220 );
		addColor( "yellow", 255, 255, 0 );
		addColor( "khaki", 240, 230, 140 );
		addColor( "lightgoldenrod", 255, 236, 139 );
		addColor( "palegoldenrod", 238, 232, 170 );
		addColor( "darkkhaki", 189, 183, 107 );
		addColor( "banana", 227, 207, 87 );
		addColor( "gold", 255, 215, 0 );
		addColor( "goldenrod", 218, 165, 32 );
		addColor( "darkgoldenrod", 184, 134, 11 );
		addColor( "brick", 156, 102, 31 );
		addColor( "floralwhite", 255, 250, 240 );
		addColor( "seashell", 255, 245, 238 );
		addColor( "oldlace", 253, 245, 230 );
		addColor( "linen", 250, 240, 230 );
		addColor( "antiquewhite", 250, 235, 215 );
		addColor( "papayawhip", 255, 239, 213 );
		addColor( "blanchedalmond", 255, 235, 205 );
		addColor( "eggshell", 252, 230, 201 );
		addColor( "bisque", 255, 228, 196 );
		addColor( "moccasin", 255, 228, 181 );
		addColor( "navajowhite", 255, 222, 173 );
		addColor( "wheat", 245, 222, 179 );
		addColor( "peachpuff", 255, 218, 185 );
		addColor( "tan", 210, 180, 140 );
		addColor( "burlywood", 222, 184, 135 );
		addColor( "melon", 227, 168, 105 );
		addColor( "sandybrown", 244, 164, 96 );
		addColor( "cadmiumyellow", 255, 153, 18 );
		addColor( "carrot", 237, 145, 33 );
		addColor( "orange", 255, 128, 0 );
		addColor( "flesh", 255, 125, 64 );
		addColor( "cadmiumorange", 255, 97, 3 );
		addColor( "chocolate", 210, 105, 30 );
		addColor( "rawsienna", 199, 97, 20 );
		addColor( "sienna", 160, 82, 45 );
		addColor( "brown", 138, 54, 15 );
		addColor( "lightsalmon", 255, 160, 122 );
		addColor( "darksalmon", 233, 150, 122 );
		addColor( "salmon", 250, 128, 114 );
		addColor( "lightcoral", 240, 128, 128 );
		addColor( "coral", 255, 114, 86 );
		addColor( "tomato", 255, 99, 71 );
		addColor( "orangered", 255, 69, 0 );
		addColor( "red", 255, 0, 0 );
		addColor( "crimson", 220, 20, 60 );
		addColor( "firebrick", 178, 34, 34 );
		addColor( "indianred", 176, 23, 31 );
		addColor( "burntumber", 138, 51, 36 );
		addColor( "maroon", 128, 0, 0 );
		addColor( "sepia", 94, 38, 18 );
		addColor( "white", 255, 255, 255 );
		addColor( "gray99", 252, 252, 252 );
		addColor( "gray98", 250, 250, 250 );
		addColor( "gray97", 247, 247, 247 );
		addColor( "gray96", 245, 245, 245 );
		addColor( "gray95", 242, 242, 242 );
		addColor( "gray94", 240, 240, 240 );
		addColor( "gray93", 237, 237, 237 );
		addColor( "gray92", 235, 235, 235 );
		addColor( "gray91", 232, 232, 232 );
		addColor( "gray90", 229, 229, 229 );
		addColor( "gray89", 227, 227, 227 );
		addColor( "gray88", 224, 224, 224 );
		addColor( "gray87", 222, 222, 222 );
		addColor( "gray86", 219, 219, 219 );
		addColor( "gray85", 217, 217, 217 );
		addColor( "gray84", 214, 214, 214 );
		addColor( "gray83", 212, 212, 212 );
		addColor( "gray82", 209, 209, 209 );
		addColor( "gray81", 207, 207, 207 );
		addColor( "gray80", 204, 204, 204 );
		addColor( "gray79", 201, 201, 201 );
		addColor( "gray78", 199, 199, 199 );
		addColor( "gray77", 196, 196, 196 );
		addColor( "gray76", 194, 194, 194 );
		addColor( "gray75", 191, 191, 191 );
		addColor( "gray74", 189, 189, 189 );
		addColor( "gray73", 186, 186, 186 );
		addColor( "gray72", 184, 184, 184 );
		addColor( "gray71", 181, 181, 181 );
		addColor( "gray70", 179, 179, 179 );
		addColor( "gray69", 176, 176, 176 );
		addColor( "gray68", 173, 173, 173 );
		addColor( "gray67", 171, 171, 171 );
		addColor( "gray66", 168, 168, 168 );
		addColor( "gray65", 166, 166, 166 );
		addColor( "gray64", 163, 163, 163 );
		addColor( "gray63", 161, 161, 161 );
		addColor( "gray62", 158, 158, 158 );
		addColor( "gray61", 156, 156, 156 );
		addColor( "gray60", 153, 153, 153 );
		addColor( "gray59", 150, 150, 150 );
		addColor( "gray58", 148, 148, 148 );
		addColor( "gray57", 145, 145, 145 );
		addColor( "gray56", 143, 143, 143 );
		addColor( "gray55", 140, 140, 140 );
		addColor( "gray54", 138, 138, 138 );
		addColor( "gray53", 135, 135, 135 );
		addColor( "gray52", 133, 133, 133 );
		addColor( "gray51", 130, 130, 130 );
		addColor( "gray50", 127, 127, 127 );
		addColor( "gray49", 125, 125, 125 );
		addColor( "gray48", 122, 122, 122 );
		addColor( "gray47", 120, 120, 120 );
		addColor( "gray46", 117, 117, 117 );
		addColor( "gray45", 115, 115, 115 );
		addColor( "gray44", 112, 112, 112 );
		addColor( "gray43", 110, 110, 110 );
		addColor( "gray42", 107, 107, 107 );
		addColor( "gray41", 105, 105, 105 );
		addColor( "gray40", 102, 102, 102 );
		addColor( "gray39", 99, 99, 99 );
		addColor( "gray38", 97, 97, 97 );
		addColor( "gray37", 94, 94, 94 );
		addColor( "gray36", 92, 92, 92 );
		addColor( "gray35", 89, 89, 89 );
		addColor( "gray34", 87, 87, 87 );
		addColor( "gray33", 84, 84, 84 );
		addColor( "gray32", 82, 82, 82 );
		addColor( "gray31", 79, 79, 79 );
		addColor( "gray30", 77, 77, 77 );
		addColor( "gray29", 74, 74, 74 );
		addColor( "gray28", 71, 71, 71 );
		addColor( "gray27", 69, 69, 69 );
		addColor( "gray26", 66, 66, 66 );
		addColor( "gray25", 64, 64, 64 );
		addColor( "gray24", 61, 61, 61 );
		addColor( "gray23", 59, 59, 59 );
		addColor( "gray22", 56, 56, 56 );
		addColor( "gray21", 54, 54, 54 );
		addColor( "gray20", 51, 51, 51 );
		addColor( "gray19", 48, 48, 48 );
		addColor( "gray18", 46, 46, 46 );
		addColor( "gray17", 43, 43, 43 );
		addColor( "gray16", 41, 41, 41 );
		addColor( "gray15", 38, 38, 38 );
		addColor( "gray14", 36, 36, 36 );
		addColor( "gray13", 33, 33, 33 );
		addColor( "gray12", 31, 31, 31 );
		addColor( "gray11", 28, 28, 28 );
		addColor( "gray10", 26, 26, 26 );
		addColor( "gray9", 23, 23, 23 );
		addColor( "gray8", 20, 20, 20 );
		addColor( "gray7", 18, 18, 18 );
		addColor( "gray6", 15, 15, 15 );
		addColor( "gray5", 13, 13, 13 );
		addColor( "gray4", 10, 10, 10 );
		addColor( "gray3", 8, 8, 8 );
		addColor( "gray2", 5, 5, 5 );
		addColor( "gray1", 3, 3, 3 );
		addColor( "black", 0, 0, 0 );

		polygonAttributeList = new PolygonAttributes[3];
		polygonAttributeList[Shape.POLYGON_LINES] =
			new PolygonAttributes(PolygonAttributes.POLYGON_LINE, PolygonAttributes.CULL_NONE, 0.0f);
		polygonAttributeList[Shape.POLYGON_FILL_BOTH] =
			new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_NONE, 0.0f);
		polygonAttributeList[Shape.POLYGON_FILL_FRONT] =
			new PolygonAttributes(PolygonAttributes.POLYGON_FILL, PolygonAttributes.CULL_BACK, 0.0f);

		lineAttributeList = new LineAttributes[9];
		lineAttributeList[LINE_SOLID_1PX] =
			new LineAttributes(1, LineAttributes.PATTERN_SOLID, false);
		lineAttributeList[LINE_DASH_1PX] =
			new LineAttributes(1, LineAttributes.PATTERN_DASH, false);
		lineAttributeList[LINE_DOT_1PX] =
			new LineAttributes(1, LineAttributes.PATTERN_DOT, false);
		lineAttributeList[LINE_SOLID_2PX] =
			new LineAttributes(2, LineAttributes.PATTERN_SOLID, false);
		lineAttributeList[LINE_DASH_2PX] =
			new LineAttributes(2, LineAttributes.PATTERN_DASH, false);
		lineAttributeList[LINE_DOT_2PX] =
			new LineAttributes(2, LineAttributes.PATTERN_DOT, false);
		lineAttributeList[LINE_SOLID_3PX] =
			new LineAttributes(3, LineAttributes.PATTERN_SOLID, false);
		lineAttributeList[LINE_DASH_3PX] =
			new LineAttributes(3, LineAttributes.PATTERN_DASH, false);
		lineAttributeList[LINE_DOT_3PX] =
			new LineAttributes(3, LineAttributes.PATTERN_DOT, false);
	}

	/**
	 * Default constructor setting all of the BranchGroup properties.
	 */
	public Shape() {

		super();
		setCapability( BranchGroup.ALLOW_DETACH );

		appearance = new Appearance();
		appearance.setCapability( Appearance.ALLOW_COLORING_ATTRIBUTES_WRITE );
		appearance.setCapability( Appearance.ALLOW_LINE_ATTRIBUTES_WRITE );
		appearance.setCapability( Appearance.ALLOW_POLYGON_ATTRIBUTES_WRITE );
		appearance.setCapability( Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_WRITE );
		RenderingAttributes renderingAttributes = new RenderingAttributes();
		renderingAttributes.setCapability(RenderingAttributes.ALLOW_VISIBLE_WRITE);
		appearance.setRenderingAttributes(renderingAttributes);

		shape = new Shape3D();
		shape.setCapability( Shape3D.ALLOW_GEOMETRY_WRITE );
		shape.setAppearance( appearance );

		addChild( shape );

		createInitialGeometry();

		// Careful here, you need to have added something in createInitialGeometry.
		compile();

		layer = 0;

		name = "Shape";
		nameOfTheCallingVariable = "No Name";
	}

	/**
	 * Here you must add a Geometry object to the Shape3D. Called in the
	 * constructor of the Shape class.
	 */
	public abstract void createInitialGeometry();

	/**
	 * Add the given color with RGB values to the color map
	 */
	private static void addColor( String colorName, int r, int g, int b ) {
		colorMap.put( colorName, new ColoringAttributes( r / 255.0f, g / 255.0f, b / 255.0f, ColoringAttributes.FASTEST) );
	}

	private static void addColor(String colorName, ColoringAttributes col) {
		colorMap.put(colorName, col);
	}

	public static ColoringAttributes getColorWithName( String colorName ) {
		return colorMap.get( colorName.toLowerCase() );
	}

	public static ColoringAttributes getPresetColor(int index) {
		if (index > 15)
			return colorList[Shape.COLOR_WHITE];
		else
			return colorList[index];
	}

	public static LineAttributes getPresetLineStyle(int index) {
		if (index > 9)
			return lineAttributeList[Shape.LINE_SOLID_1PX];
		else
			return lineAttributeList[index];
	}

	public static ColoringAttributes defineColor(float r, float g, float b) {
		if (r > 1.0f || g > 1.0f || b > 1.0f)
			return new ColoringAttributes(r / 255.0f, g / 255.0f, b / 255.0f, ColoringAttributes.FASTEST);
		else
			return new ColoringAttributes(r, g, b, ColoringAttributes.FASTEST);
	}

	/**
	 * Set the color of the appeance member. Defaults to Shape.COLOR_WHITE if
	 * index is not correct.
	 *
	 * @param color Index of the color to set, like Shape.COLOR_WHITE.
	 */
	public void setColor( int color ) {
		if( color < colorList.length && color > -1 ) {
			appearance.setColoringAttributes(colorList[color]);
		}
		else {
			appearance.setColoringAttributes(colorList[Shape.COLOR_WHITE]);
		}
	}

	// This method will eventually replace original setColor()
	public void setColor( ColoringAttributes rgb ) {
		appearance.setColoringAttributes( rgb );
	}

	/**
	 * Returns the coloring attributes of the shape.
	 *
	 * @return
	 */
	public ColoringAttributes getColor() {
		return appearance.getColoringAttributes();
	}

	public void setLineAttributes( LineAttributes la ) {
		shape.getAppearance().setLineAttributes( la );
	}

	/**
	 * Set the line style of the shape. Defaults to Shape.LINE_SOLID if index is
	 * not correct.
	 *
	 * @param lStyle Index of the line style to set, like Shape.LINE_SOLID.
	 */
	public void setLineStyle( int lStyle ) {
		if( lStyle < lineAttributeList.length && lStyle > -1 ) {
			appearance.setLineAttributes(lineAttributeList[lStyle]);
		}
		else {
			appearance.setLineAttributes(lineAttributeList[Shape.LINE_SOLID_1PX]);
		}
	}

	/**
	 * Don't do this.  Try and modify the code to not need this method.
	 *
	 * @param la
	 */
	public void setLineStyle( LineAttributes la ) {
		appearance.setLineAttributes( la );
	}

	/**
	 * Used to maintain compatibility with previous implementation.
	 *
	 * @param att
	 */
	public void setTransparency( TransparencyAttributes att ) {
		appearance.setTransparencyAttributes( att );
	}

	/**
	 * Set the polygon style of the shape. Defaults to Shape.POLYGON_FILL_FRONT
	 * if index is not correct.
	 *
	 * @param pStyle Index of the polygon style to set, like Shape.POLYGON_FILL_FRONT.
	 */
	public void setPolygonStyle( int pStyle ) {
		if( pStyle < polygonAttributeList.length && pStyle > -1 ) {
			appearance.setPolygonAttributes(polygonAttributeList[pStyle]);
		}
		else {
			appearance.setPolygonAttributes(polygonAttributeList[Shape.POLYGON_FILL_FRONT]);
		}
	}

	/**
	 * @return Returns the layer.
	 */
	public int getLayer() {
		return layer;
	}

	/**
	 * @param layer The layer to set.
	 */
	public void setLayer( int layer ) {
		this.layer = layer;
	}

	/**
	 * Sets the center of the shape and adjusts for z-order (layer).
	 *
	 * @param x
	 * @param y
	 * @param z
	 */
	public void setCenter( double x, double y, double z ) {
		centreX = x;
		centreY = y;
		//pseudoCenterZ = z;
		//centreZ = pseudoCenterZ + (double)(layer * 0.00001d);
		centreZ = z;
	}

	public void setCenter( double x, double y ) {
		centreX = x;
		centreY = y;
	}

	public Point3d getCenterInDouble( ) {
		return new Point3d (  centreX ,  centreY, centreZ );
	}

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName( String name ) {
		this.name = name;
	}

	public String toString() {
		return  nameOfTheCallingVariable +" (" + name + ")" ;
	}
	/**
	 * Method to return a description of the Shape in treenode form.
	 *
	 * @return A treenode element which can be added to a GUI.
	 */
	public javax.swing.tree.DefaultMutableTreeNode getTreeNode() {

		javax.swing.tree.DefaultMutableTreeNode myRoot = new javax.swing.tree.DefaultMutableTreeNode( this  );

//		// Layer Information
//		myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Layer: " + layer ) );
//
//		// Check to see if a color has been set.
//		if( appearance.getColoringAttributes() != null ) {
//			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Color: " + appearance.getColoringAttributes() ) );
//		}
//		if( color > -1 ) {
//			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Color: " + (String)colorDescList.get( color ) ) );
//		}
//		else {
//			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( "Default Color" ) );
//		}
//
//		// Check to see if a lineStyle has been set, display nothing if no line style.
//		if( lineStyle > -1 ) {
//			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( (String)lineDescList.get( lineStyle ) ) );
//		}
//
//		// Check to see if a polygonStyle has been set, display nothing if no polygon style.
//		if( polygonStyle > -1 ) {
//			myRoot.add( new javax.swing.tree.DefaultMutableTreeNode( (String)polygonDescList.get( polygonStyle ) ) );
//		}

		return myRoot;
	}
	public void setSize(double size){}
	public void setSize(DoubleVector sizes){}
	public void setColor(ColoringAttributes [] colors){
		this.setColor(colors[0]);
	}
}
