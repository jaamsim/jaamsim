/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
 * Copyright (C) 2018 JaamSim Software Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.math.Color4d;

public class ColourInput extends Input<Color4d> {

public static final Color4d WHITE       = new Color4d(1.0d, 1.0d, 1.0d);
public static final Color4d BLACK       = new Color4d(0.0d, 0.0d, 0.0d);
public static final Color4d RED         = new Color4d(1.0d, 0.0d, 0.0d);
public static final Color4d GREEN       = new Color4d(0.0d, 1.0d, 0.0d);
public static final Color4d BLUE        = new Color4d(0.0d, 0.0d, 1.0d);
public static final Color4d CYAN        = new Color4d(0.0d, 1.0d, 1.0d);
public static final Color4d YELLOW      = new Color4d(1.0d, 1.0d, 0.0d);
public static final Color4d PURPLE      = new Color4d(1.0d, 0.0d, 1.0d);
public static final Color4d LIGHT_GREY  = new Color4d(0.75d, 0.75d, 0.75d);
public static final Color4d MED_GREY    = new Color4d(0.5d, 0.5d, 0.5d);
public static final Color4d DARK_RED    = new Color4d(0.75d, 0.0d, 0.0d);
public static final Color4d DARK_GREEN  = new Color4d(0.0d, 0.75d, 0.0d);
public static final Color4d DARK_BLUE   = new Color4d(0.0d, 0.0d, 0.75d);
public static final Color4d DARK_CYAN   = new Color4d(0.0d, 0.75d, 0.75d);
public static final Color4d DARK_YELLOW = new Color4d(0.75d, 0.75d, 0.0d);
public static final Color4d DARK_PURPLE = new Color4d(0.75d, 0.0d, 0.75d);

private static final HashMap<String, Color4d> colorMap;
private static final HashMap<Color4d, String> colorNameMap;
public static final ArrayList<Color4d> namedColourList;

public static Comparator<Color4d> colourComparator = new Comparator<Color4d>() {
	@Override
	public int compare(Color4d col1, Color4d col2) {
		String str1 = ColourInput.toString(col1);
		String str2 = ColourInput.toString(col2);
		return Input.uiSortOrder.compare(str1, str2);
	}
};

static {
	colorMap = new HashMap<>();
	colorNameMap = new HashMap<>();
	namedColourList = new ArrayList<>();
	initColors();
	Collections.sort(namedColourList, colourComparator);
}
	public ColourInput(String key, String cat, Color4d def) {
		super(key, cat, def);
	}

	@Override
	public void parse(KeywordIndex kw)
	throws InputErrorException {
		value = Input.parseColour(kw);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_COLOUR;
	}

public static Color4d getColorWithName(String colorName) {
	return colorMap.get(colorName);
}

public static String getColorName(Color4d col) {
	return colorNameMap.get(col);
}

private static void defColor(String colorName, int r, int g, int b) {
	mapColor(colorName, new Color4d(r / 255.0d, g / 255.0d, b / 255.0d));
}

private static void mapColor(String colorName, Color4d col) {
	if (colorMap.put(colorName, col) != null)
		System.out.println(String.format("ColorName added twice: %s ", colorName));
	if (colorNameMap.put(col, colorName) != null)
		System.out.println(String.format("Color4d added twice: %s", colorName));
	namedColourList.add(col);
}

private static void initColors() {
	mapColor("white", WHITE);
	mapColor("black", BLACK);

	mapColor("red", RED);
	mapColor("green", GREEN);
	mapColor("blue", BLUE);
	mapColor("cyan", CYAN);
	mapColor("yellow", YELLOW);
	//mapColor("purple", PURPLE); This RGB value is actually "magenta"

	mapColor("darkred", DARK_RED);
	mapColor("darkgreen", DARK_GREEN);
	mapColor("darkblue", DARK_BLUE);
	mapColor("darkcyan", DARK_CYAN);
	mapColor("darkyellow", DARK_YELLOW);
	mapColor("darkpurple", DARK_PURPLE);

	defColor("lavenderblush", 255, 240, 245);
	defColor("pink", 255, 192, 203);
	defColor("lightpink", 255, 182, 193);
	defColor("palevioletred", 219, 112, 147);
	defColor("hotpink", 255, 105, 180);
	defColor("deeppink", 255, 20, 147);
	defColor("violetred", 208, 32, 144);
	defColor("mediumvioletred", 199, 21, 133);
	defColor("raspberry", 135, 38, 87);
	defColor("thistle", 216, 191, 216);
	defColor("plum", 221, 160, 221);
	defColor("orchid", 218, 112, 214);
	defColor("violet", 238, 130, 238);
	defColor("magenta", 255, 0, 255);
	defColor("purple", 128, 0, 128);
	defColor("mediumorchid", 186, 85, 211);
	defColor("darkorchid", 153, 50, 204);
	defColor("darkviolet", 148, 0, 211);
	defColor("blueviolet", 138, 43, 226);
	defColor("indigo", 75, 0, 130);
	defColor("mediumpurple", 147, 112, 219);
	defColor("lightslateblue", 132, 112, 255);
	defColor("mediumslateblue", 123, 104, 238);
	defColor("slateblue", 106, 90, 205);
	defColor("darkslateblue", 72, 61, 139);
	defColor("ghostwhite", 248, 248, 255);
	defColor("lavender", 230, 230, 250);
	defColor("navy", 0, 0, 128);
	defColor("midnightblue", 25, 25, 112);
	defColor("cobalt", 61, 89, 171);
	defColor("royalblue", 65, 105, 225);
	defColor("cornflowerblue", 100, 149, 237);
	defColor("lightsteelblue", 176, 196, 222);
	defColor("lightslategray", 119, 136, 153);
	defColor("slategray", 112, 128, 144);
	defColor("dodgerblue", 30, 144, 255);
	defColor("aliceblue", 240, 248, 255);
	defColor("powderblue", 176, 224, 230);
	defColor("lightblue", 173, 216, 230);
	defColor("lightskyblue", 135, 206, 250);
	defColor("skyblue", 135, 206, 235);
	defColor("deepskyblue", 0, 191, 255);
	defColor("peacock", 51, 161, 201);
	defColor("steelblue", 70, 130, 180);
	defColor("darkturquoise", 0, 206, 209);
	defColor("cadetblue", 95, 158, 160);
	defColor("azure", 240, 255, 255);
	defColor("lightcyan", 224, 255, 255);
	defColor("paleturquoise", 187, 255, 255);
	defColor("turquoise", 64, 224, 208);
	defColor("mediumturquoise", 72, 209, 204);
	defColor("lightseagreen", 32, 178, 170);
	defColor("manganeseblue", 3, 168, 158);
	defColor("teal", 0, 128, 128);
	defColor("darkslategray", 47, 79, 79);
	defColor("turquoiseblue", 0, 199, 140);
	defColor("aquamarine", 127, 255, 212);
	defColor("mintcream", 245, 255, 250);
	defColor("mint", 189, 252, 201);
	defColor("seagreen", 84, 255, 159);
	defColor("mediumspringgreen", 0, 250, 154);
	defColor("springgreen", 0, 255, 127);
	defColor("emeraldgreen", 0, 201, 87);
	defColor("mediumseagreen", 60, 179, 113);
	defColor("cobaltgreen", 61, 145, 64);
	defColor("darkseagreen", 143, 188, 143);
	defColor("honeydew", 240, 255, 240);
	defColor("palegreen", 152, 251, 152);
	defColor("lawngreen", 124, 252, 0);
	defColor("greenyellow", 173, 255, 47);
	defColor("limegreen", 50, 205, 50);
	defColor("forestgreen", 34, 139, 34);
	defColor("sapgreen", 48, 128, 20);
	defColor("darkolivegreen", 85, 107, 47);
	defColor("olivedrab", 107, 142, 35);
	defColor("olive", 128, 128, 0);
	defColor("ivory", 255, 255, 240);
	defColor("lightyellow", 255, 255, 224);
	defColor("lightgoldenrodyellow", 250, 250, 210);
	defColor("cornsilk", 255, 248, 220);
	defColor("lemonchiffon", 255, 250, 205);
	defColor("beige", 245, 245, 220);
	defColor("khaki", 240, 230, 140);
	defColor("lightgoldenrod", 255, 236, 139);
	defColor("palegoldenrod", 238, 232, 170);
	defColor("darkkhaki", 189, 183, 107);
	defColor("banana", 227, 207, 87);
	defColor("gold", 255, 215, 0);
	defColor("goldenrod", 218, 165, 32);
	defColor("darkgoldenrod", 184, 134, 11);
	defColor("brick", 156, 102, 31);
	defColor("floralwhite", 255, 250, 240);
	defColor("seashell", 255, 245, 238);
	defColor("oldlace", 253, 245, 230);
	defColor("linen", 250, 240, 230);
	defColor("antiquewhite", 250, 235, 215);
	defColor("papayawhip", 255, 239, 213);
	defColor("blanchedalmond", 255, 235, 205);
	defColor("eggshell", 252, 230, 201);
	defColor("bisque", 255, 228, 196);
	defColor("moccasin", 255, 228, 181);
	defColor("navajowhite", 255, 222, 173);
	defColor("wheat", 245, 222, 179);
	defColor("peachpuff", 255, 218, 185);
	defColor("tan", 210, 180, 140);
	defColor("burlywood", 222, 184, 135);
	defColor("melon", 227, 168, 105);
	defColor("sandybrown", 244, 164, 96);
	defColor("cadmiumyellow", 255, 153, 18);
	defColor("carrot", 237, 145, 33);
	defColor("orange", 255, 128, 0);
	defColor("flesh", 255, 125, 64);
	defColor("cadmiumorange", 255, 97, 3);
	defColor("chocolate", 210, 105, 30);
	defColor("rawsienna", 199, 97, 20);
	defColor("sienna", 160, 82, 45);
	defColor("brown", 138, 54, 15);
	defColor("lightsalmon", 255, 160, 122);
	defColor("darksalmon", 233, 150, 122);
	defColor("salmon", 250, 128, 114);
	defColor("lightcoral", 240, 128, 128);
	defColor("coral", 255, 114, 86);
	defColor("tomato", 255, 99, 71);
	defColor("orangered", 255, 69, 0);
	defColor("crimson", 220, 20, 60);
	defColor("firebrick", 178, 34, 34);
	defColor("indianred", 176, 23, 31);
	defColor("burntumber", 138, 51, 36);
	defColor("maroon", 128, 0, 0);
	defColor("sepia", 94, 38, 18);
	defColor("gray99", 252, 252, 252);
	defColor("gray98", 250, 250, 250);
	defColor("gray97", 247, 247, 247);
	defColor("gray96", 245, 245, 245);
	defColor("gray95", 242, 242, 242);
	defColor("gray94", 240, 240, 240);
	defColor("gray93", 237, 237, 237);
	defColor("gray92", 235, 235, 235);
	defColor("gray91", 232, 232, 232);
	defColor("gray90", 229, 229, 229);
	defColor("gray89", 227, 227, 227);
	defColor("gray88", 224, 224, 224);
	defColor("gray87", 222, 222, 222);
	defColor("gray86", 219, 219, 219);
	defColor("gray85", 217, 217, 217);
	defColor("gray84", 214, 214, 214);
	defColor("gray83", 212, 212, 212);
	defColor("gray82", 209, 209, 209);
	defColor("gray81", 207, 207, 207);
	defColor("gray80", 204, 204, 204);
	defColor("gray79", 201, 201, 201);
	defColor("gray78", 199, 199, 199);
	defColor("gray77", 196, 196, 196);
	defColor("gray76", 194, 194, 194);
	mapColor("gray75", LIGHT_GREY);
	defColor("gray74", 189, 189, 189);
	defColor("gray73", 186, 186, 186);
	defColor("gray72", 184, 184, 184);
	defColor("gray71", 181, 181, 181);
	defColor("gray70", 179, 179, 179);
	defColor("gray69", 176, 176, 176);
	defColor("gray68", 173, 173, 173);
	defColor("gray67", 171, 171, 171);
	defColor("gray66", 168, 168, 168);
	defColor("gray65", 166, 166, 166);
	defColor("gray64", 163, 163, 163);
	defColor("gray63", 161, 161, 161);
	defColor("gray62", 158, 158, 158);
	defColor("gray61", 156, 156, 156);
	defColor("gray60", 153, 153, 153);
	defColor("gray59", 150, 150, 150);
	defColor("gray58", 148, 148, 148);
	defColor("gray57", 145, 145, 145);
	defColor("gray56", 143, 143, 143);
	defColor("gray55", 140, 140, 140);
	defColor("gray54", 138, 138, 138);
	defColor("gray53", 135, 135, 135);
	defColor("gray52", 133, 133, 133);
	defColor("gray51", 130, 130, 130);
	mapColor("gray50", MED_GREY);
	defColor("gray49", 125, 125, 125);
	defColor("gray48", 122, 122, 122);
	defColor("gray47", 120, 120, 120);
	defColor("gray46", 117, 117, 117);
	defColor("gray45", 115, 115, 115);
	defColor("gray44", 112, 112, 112);
	defColor("gray43", 110, 110, 110);
	defColor("gray42", 107, 107, 107);
	defColor("gray41", 105, 105, 105);
	defColor("gray40", 102, 102, 102);
	defColor("gray39", 99, 99, 99);
	defColor("gray38", 97, 97, 97);
	defColor("gray37", 94, 94, 94);
	defColor("gray36", 92, 92, 92);
	defColor("gray35", 89, 89, 89);
	defColor("gray34", 87, 87, 87);
	defColor("gray33", 84, 84, 84);
	defColor("gray32", 82, 82, 82);
	defColor("gray31", 79, 79, 79);
	defColor("gray30", 77, 77, 77);
	defColor("gray29", 74, 74, 74);
	defColor("gray28", 71, 71, 71);
	defColor("gray27", 69, 69, 69);
	defColor("gray26", 66, 66, 66);
	defColor("gray25", 64, 64, 64);
	defColor("gray24", 61, 61, 61);
	defColor("gray23", 59, 59, 59);
	defColor("gray22", 56, 56, 56);
	defColor("gray21", 54, 54, 54);
	defColor("gray20", 51, 51, 51);
	defColor("gray19", 48, 48, 48);
	defColor("gray18", 46, 46, 46);
	defColor("gray17", 43, 43, 43);
	defColor("gray16", 41, 41, 41);
	defColor("gray15", 38, 38, 38);
	defColor("gray14", 36, 36, 36);
	defColor("gray13", 33, 33, 33);
	defColor("gray12", 31, 31, 31);
	defColor("gray11", 28, 28, 28);
	defColor("gray10", 26, 26, 26);
	defColor("gray9", 23, 23, 23);
	defColor("gray8", 20, 20, 20);
	defColor("gray7", 18, 18, 18);
	defColor("gray6", 15, 15, 15);
	defColor("gray5", 13, 13, 13);
	defColor("gray4", 10, 10, 10);
	defColor("gray3", 8, 8, 8);
	defColor("gray2", 5, 5, 5);
	defColor("gray1", 3, 3, 3);
}

	public static String toString(Color4d col) {
		if (col == null)
			return "";

		int red = (int) Math.round(col.r * 255);
		int green = (int) Math.round(col.g * 255);
		int blue = (int) Math.round(col.b * 255);
		int alpha = (int) Math.round(col.a * 255);

		StringBuilder sb = new StringBuilder();
		String colorName = getColorName(new Color4d(col.r, col.g, col.b));
		if (colorName != null) {
			sb.append(colorName);
		}
		else {
			sb.append(red).append(SEPARATOR);
			sb.append(green).append(SEPARATOR);
			sb.append(blue);
		}

		if (alpha == 255) {
			return sb.toString();
		}
		else {
			sb.append(SEPARATOR).append(alpha);
			return sb.toString();
		}
	}

	@Override
	public String getDefaultString() {
		return toString(defValue);
	}

	@Override
	public ArrayList<String> getValidOptions() {
		ArrayList<String> list = new ArrayList<>(colorMap.keySet());
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public String toString() {
		return toString(value);
	}

	public static ArrayList<Color4d> getColoursInUse() {
		ArrayList<Color4d> ret = new ArrayList<>();
		for (Entity ent : Entity.getClonesOfIterator(Entity.class)) {
			for (Input<?> in : ent.getEditableInputs()) {
				if (!(in instanceof ColourInput))
					continue;
				Color4d col = (Color4d) in.getValue();
				if (ret.contains(col))
					continue;
				ret.add(col);
			}
		}
		Collections.sort(ret, ColourInput.colourComparator);
		return ret;
	}

}
