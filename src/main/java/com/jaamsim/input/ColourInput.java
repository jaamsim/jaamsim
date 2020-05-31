/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2011 Ausenco Engineering Canada Inc.
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
package com.jaamsim.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import com.jaamsim.basicsim.Entity;
import com.jaamsim.math.Color4d;

public class ColourInput extends Input<Color4d> {

public static final Color4d WHITE       = new Color4d(255, 255, 255);
public static final Color4d BLACK       = new Color4d(  0,   0,   0);
public static final Color4d RED         = new Color4d(255,   0,   0);
public static final Color4d GREEN       = new Color4d(  0, 255,   0);
public static final Color4d BLUE        = new Color4d(  0,   0, 255);
public static final Color4d CYAN        = new Color4d(  0, 255, 255);
public static final Color4d YELLOW      = new Color4d(255, 255,   0);
public static final Color4d PURPLE      = new Color4d(255,   0, 255);
public static final Color4d LIGHT_GREY  = new Color4d(191, 191, 191);
public static final Color4d MED_GREY    = new Color4d(128, 128, 128);
public static final Color4d DARK_RED    = new Color4d(191,   0,   0);
public static final Color4d DARK_GREEN  = new Color4d(  0, 191,   0);
public static final Color4d DARK_BLUE   = new Color4d(  0,   0, 191);
public static final Color4d DARK_CYAN   = new Color4d(  0, 191, 191);
public static final Color4d DARK_YELLOW = new Color4d(191, 191,   0);
public static final Color4d DARK_PURPLE = new Color4d(191,   0, 191);

private static final HashMap<String, Color4d> colorMap;
private static final HashMap<Color4d, String> colorNameMap;
public static final ArrayList<Color4d> namedColourList;
private static final HashMap<String, ArrayList<String>> colorFamilyMap;

public static final String[] colorFamilies = new String[]{"Pink", "Red", "Orange", "Yellow",
		"Brown", "Green", "Cyan", "Blue", "Purple", "White", "Black"};

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

	colorFamilyMap = new HashMap<>();
	for (String family : colorFamilies) {
		colorFamilyMap.put(family, new ArrayList<String>());
	}

	initColors();
	Collections.sort(namedColourList, colourComparator);
}
	public ColourInput(String key, String cat, Color4d def) {
		super(key, cat, def);
	}

	@Override
	public void parse(Entity thisEnt, KeywordIndex kw)
	throws InputErrorException {
		value = Input.parseColour(thisEnt.getJaamSimModel(), kw);
	}

	@Override
	public String getValidInputDesc() {
		return Input.VALID_COLOUR;
	}

public static Color4d getColorWithName(String colorName) {
	return colorMap.get(colorName.toLowerCase());
}

public static String getColorName(Color4d col) {
	return colorNameMap.get(col);
}
public static String[] getColorFamilies() {
	return colorFamilies;
}

public static ArrayList<String> getColorListForFamily(String family) {
	return colorFamilyMap.get(family);
}

private static void defColor(String colorName, String family, int r, int g, int b) {
	mapColor(colorName, family, new Color4d(r, g, b, 255));
}

private static void mapColor(String colorName, String family, Color4d col) {
	String name = colorName.toLowerCase();
	if (colorMap.put(name, col) != null)
		System.out.println(String.format("ColorName added twice: %s ", colorName));

	if (!colorNameMap.containsKey(col)) {
		colorNameMap.put(col, colorName);
		namedColourList.add(col);
	}

	ArrayList<String> list = colorFamilyMap.get(family);
	if (list == null) {
		System.out.println(String.format("Invalid color family: %s", family));
		return;
	}
	list.add(colorName);
}

private static void initColors() {

	// Pink colours
	defColor("Pink",            "Pink", 255, 192, 203);
	defColor("LightPink",       "Pink", 255, 182, 193);
	defColor("HotPink",         "Pink", 255, 105, 180);
	defColor("DeepPink",        "Pink", 255,  20, 147);
	defColor("PaleVioletRed",   "Pink", 219, 112, 147);
	defColor("MediumVioletRed", "Pink", 199,  21, 133);

	defColor("VioletRed",       "Pink", 247,  83, 148);

	// Red colours
	defColor("LightSalmon", "Red", 255, 160, 122);
	defColor("Salmon",      "Red", 250, 128, 114);
	defColor("DarkSalmon",  "Red", 233, 150, 122);
	defColor("LightCoral",  "Red", 240, 128, 128);
	defColor("IndianRed",   "Red", 205,  92,  92);
	defColor("Crimson",     "Red", 220,  20,  60);
	defColor("FireBrick",   "Red", 178,  34,  34);
	defColor("DarkRed",     "Red", 139,   0,   0);
	defColor("Red",         "Red", 255,   0,   0);  // RED

	// Orange colours
	defColor("OrangeRed",     "Orange", 255,  69,   0);
	defColor("Tomato",        "Orange", 255,  99,  71);
	defColor("Coral",         "Orange", 255, 127,  80);
	defColor("DarkOrange",    "Orange", 255, 140,   0);
	defColor("Orange",        "Orange", 255, 165,   0);

	defColor("Flesh",         "Orange", 255, 125,  64);
	defColor("Carrot",        "Orange", 237, 145,  33);
	defColor("CadmiumOrange", "Orange", 237, 135,  45);

	// Yellow colours
	defColor("Yellow",               "Yellow", 255, 255,   0);  // YELLOW
	defColor("LightYellow",          "Yellow", 255, 255, 224);
	defColor("LemonChiffon",         "Yellow", 255, 250, 205);
	defColor("LightGoldenrodYellow", "Yellow", 250, 250, 210);
	defColor("PapayaWhip",           "Yellow", 255, 239, 213);
	defColor("Moccasin",             "Yellow", 255, 228, 181);
	defColor("PeachPuff",            "Yellow", 255, 218, 185);
	defColor("PaleGoldenrod",        "Yellow", 238, 232, 170);
	defColor("Khaki",                "Yellow", 240, 230, 140);
	defColor("DarkKhaki",            "Yellow", 189, 183, 107);
	defColor("Gold",                 "Yellow", 255, 215,   0);

	defColor("CadmiumYellow",        "Yellow", 255, 246,   0);
	defColor("LightGoldenrod",       "Yellow", 255, 236, 139);
	defColor("Banana",               "Yellow", 255, 255,  53);
	defColor("DarkYellow",           "Yellow", 155, 135,  12);

	// Brown colours
	defColor("Cornsilk",       "Brown", 255, 248, 220);
	defColor("BlanchedAlmond", "Brown", 255, 235, 205);
	defColor("Bisque",         "Brown", 255, 228, 196);
	defColor("NavajoWhite",    "Brown", 255, 222, 173);
	defColor("Wheat",          "Brown", 245, 222, 179);
	defColor("Burlywood",      "Brown", 222, 184, 135);
	defColor("Tan",            "Brown", 210, 180, 140);
	defColor("RosyBrown",      "Brown", 188, 143, 143);
	defColor("SandyBrown",     "Brown", 244, 164,  96);
	defColor("Goldenrod",      "Brown", 218, 165,  32);
	defColor("DarkGoldenrod",  "Brown", 184, 134,  11);
	defColor("Peru",           "Brown", 205, 133,  63);
	defColor("Chocolate",      "Brown", 210, 105,  30);
	defColor("SaddleBrown",    "Brown", 139,  69,  19);
	defColor("Sienna",         "Brown", 160,  82,  45);
	defColor("Brown",          "Brown", 138,  54,  15);
	defColor("Maroon",         "Brown", 128,   0,   0);

	defColor("RawSienna",      "Brown", 199,  97,  20);
	defColor("BurntUmber",     "Brown", 138,  51,  36);
	defColor("Sepia",          "Brown",  94,  38,  18);
	defColor("Brick",          "Brown", 156, 102,  31);
	defColor("Melon",          "Brown", 227, 168, 105);

	// Green colours
	defColor("DarkOliveGreen",    "Green",  85, 107,  47);
	defColor("Olive",             "Green", 128, 128,   0);
	defColor("OliveDrab",         "Green", 107, 142,  35);
	defColor("YellowGreen",       "Green", 154, 205,  50);
	defColor("LimeGreen",         "Green",  50, 205,  50);
	defColor("Lime",              "Green",   0, 255,   0);  // GREEN
	defColor("LawnGreen",         "Green", 124, 252,   0);
	defColor("Chartreuse",        "Green", 127, 255,   0);
	defColor("GreenYellow",       "Green", 173, 255,  47);
	defColor("SpringGreen",       "Green",   0, 255, 127);
	defColor("MediumSpringGreen", "Green",   0, 250, 154);
	defColor("LightGreen",        "Green", 144, 238, 144);
	defColor("PaleGreen",         "Green", 152, 251, 152);
	defColor("DarkSeaGreen",      "Green", 143, 188, 143);
	defColor("MediumAquamarine",  "Green", 102, 205, 170);
	defColor("MediumSeaGreen",    "Green",  60, 179, 113);
	defColor("SeaGreen",          "Green",  46, 139,  87);
	defColor("ForestGreen",       "Green",  34, 139,  34);
	defColor("Green",             "Green",   0, 128,   0);
	defColor("DarkGreen",         "Green",   0, 100,   0);

	defColor("Mint",              "Green", 189, 252, 201);
	defColor("EmeraldGreen",      "Green",   0, 201,  87);
	defColor("CobaltGreen",       "Green",  61, 145,  64);
	defColor("SapGreen",          "Green",  48, 128,  20);

	// Cyan colours
	defColor("Cyan",            "Cyan",   0, 255, 255);  // CYAN
	defColor("Aqua",            "Cyan",   0, 255, 255);  // same as Cyan
	defColor("LightCyan",       "Cyan", 224, 255, 255);
	defColor("PaleTurquoise",   "Cyan", 175, 238, 238);
	defColor("Aquamarine",      "Cyan", 127, 255, 212);
	defColor("Turquoise",       "Cyan",  64, 224, 208);
	defColor("MediumTurquoise", "Cyan",  72, 209, 204);
	defColor("DarkTurquoise",   "Cyan",   0, 206, 209);
	defColor("LightSeaGreen",   "Cyan",  32, 178, 170);
	defColor("CadetBlue",       "Cyan",  95, 158, 160);
	defColor("DarkCyan",        "Cyan",   0, 139, 139);
	defColor("Teal",            "Cyan",   0, 128, 128);

	defColor("ManganeseBlue",   "Cyan",   3, 168, 158);
	defColor("TurquoiseBlue",   "Cyan",   0, 199, 140);

	// Blue colours
	defColor("LightSteelBlue", "Blue", 176, 196, 222);
	defColor("PowderBlue",     "Blue", 176, 224, 230);
	defColor("LightBlue",      "Blue", 173, 216, 230);
	defColor("SkyBlue",        "Blue", 135, 206, 235);
	defColor("LightSkyBlue",   "Blue", 135, 206, 250);
	defColor("DeepSkyBlue",    "Blue",   0, 191, 255);
	defColor("DodgerBlue",     "Blue",  30, 144, 255);
	defColor("CornflowerBlue", "Blue", 100, 149, 237);
	defColor("SteelBlue",      "Blue",  70, 130, 180);
	defColor("RoyalBlue",      "Blue",  65, 105, 225);
	defColor("Blue",           "Blue",   0,   0, 255);  // BLUE
	defColor("MediumBlue",     "Blue",   0,   0, 205);
	defColor("DarkBlue",       "Blue",   0,   0, 139);
	defColor("Navy",           "Blue",   0,   0, 128);
	defColor("MidnightBlue",   "Blue",  25,  25, 112);

	defColor("Cobalt",         "Blue",  61,  89, 171);
	defColor("Peacock",        "Blue",  51, 161, 201);

	// Purple, violet, and magenta colours
	defColor("Lavender",        "Purple", 230, 230, 250);
	defColor("Thistle",         "Purple", 216, 191, 216);
	defColor("Plum",            "Purple", 221, 160, 221);
	defColor("Violet",          "Purple", 238, 130, 238);
	defColor("Orchid",          "Purple", 218, 112, 214);
	defColor("Magenta",         "Purple", 255,   0, 255);
	defColor("Fuchsia",         "Purple", 255,   0, 255);  // same as Magenta
	defColor("MediumOrchid",    "Purple", 186,  85, 211);
	defColor("MediumPurple",    "Purple", 147, 112, 219);
	defColor("BlueViolet",      "Purple", 138,  43, 226);
	defColor("DarkViolet",      "Purple", 148,   0, 211);
	defColor("DarkOrchid",      "Purple", 153,  50, 204);
	defColor("DarkMagenta",     "Purple", 139,   0, 139);
	defColor("Purple",          "Purple", 128,   0, 128);
	defColor("Indigo",          "Purple",  75,   0, 130);
	defColor("DarkSlateBlue",   "Purple",  72,  61, 139);
	defColor("SlateBlue",       "Purple", 106,  90, 205);
	defColor("MediumSlateBlue", "Purple", 123, 104, 238);

	defColor("DarkPurple",      "Purple", 191,   0, 191);  // DARK_PURPLE
	defColor("Raspberry",       "Purple", 135,  38,  87);
	defColor("LightSlateBlue",  "Purple", 132, 112, 255);

	// White colours
	defColor("White",         "White", 255, 255, 255);  // WHITE
	defColor("Snow",          "White", 255, 250, 250);
	defColor("Honeydew",      "White", 240, 255, 240);
	defColor("MintCream",     "White", 245, 255, 250);
	defColor("Azure",         "White", 240, 255, 255);
	defColor("AliceBlue",     "White", 240, 248, 255);
	defColor("GhostWhite",    "White", 248, 248, 255);
	defColor("WhiteSmoke",    "White", 245, 245, 245);
	defColor("Seashell",      "White", 255, 245, 238);
	defColor("Beige",         "White", 245, 245, 220);
	defColor("OldLace",       "White", 253, 245, 230);
	defColor("FloralWhite",   "White", 255, 250, 240);
	defColor("Ivory",         "White", 255, 255, 240);
	defColor("AntiqueWhite",  "White", 250, 235, 215);
	defColor("Linen",         "White", 250, 240, 230);
	defColor("LavenderBlush", "White", 255, 240, 245);
	defColor("MistyRose",     "White", 255, 228, 225);

	defColor("EggShell",      "White", 240, 234, 214);

	// Grey and black colours
	defColor("Gainsboro",      "Black", 220, 220, 220);
	defColor("LightGray",      "Black", 211, 211, 211);
	defColor("Silver",         "Black", 192, 192, 192);
	defColor("DarkGray",       "Black", 169, 169, 169);
	defColor("Gray",           "Black", 128, 128, 128);  // MED_GREY
	defColor("DimGray",        "Black", 105, 105, 105);
	defColor("LightSlateGray", "Black", 119, 136, 153);
	defColor("SlateGray",      "Black", 112, 128, 144);
	defColor("DarkSlateGray",  "Black",  47,  79,  79);
	defColor("Black",          "Black",   0,   0,   0);  // BLACK

	defColor("gray99",         "Black", 252, 252, 252);
	defColor("gray98",         "Black", 250, 250, 250);
	defColor("gray97",         "Black", 247, 247, 247);
	defColor("gray96",         "Black", 245, 245, 245);
	defColor("gray95",         "Black", 242, 242, 242);
	defColor("gray94",         "Black", 240, 240, 240);
	defColor("gray93",         "Black", 237, 237, 237);
	defColor("gray92",         "Black", 235, 235, 235);
	defColor("gray91",         "Black", 232, 232, 232);
	defColor("gray90",         "Black", 229, 229, 229);
	defColor("gray89",         "Black", 227, 227, 227);
	defColor("gray88",         "Black", 224, 224, 224);
	defColor("gray87",         "Black", 222, 222, 222);
	defColor("gray86",         "Black", 219, 219, 219);
	defColor("gray85",         "Black", 217, 217, 217);
	defColor("gray84",         "Black", 214, 214, 214);
	defColor("gray83",         "Black", 212, 212, 212);
	defColor("gray82",         "Black", 209, 209, 209);
	defColor("gray81",         "Black", 207, 207, 207);
	defColor("gray80",         "Black", 204, 204, 204);
	defColor("gray79",         "Black", 201, 201, 201);
	defColor("gray78",         "Black", 199, 199, 199);
	defColor("gray77",         "Black", 196, 196, 196);
	defColor("gray76",         "Black", 194, 194, 194);
	defColor("gray75",         "Black", 191, 191, 191);  // LIGHT_GREY
	defColor("gray74",         "Black", 189, 189, 189);
	defColor("gray73",         "Black", 186, 186, 186);
	defColor("gray72",         "Black", 184, 184, 184);
	defColor("gray71",         "Black", 181, 181, 181);
	defColor("gray70",         "Black", 179, 179, 179);
	defColor("gray69",         "Black", 176, 176, 176);
	defColor("gray68",         "Black", 173, 173, 173);
	defColor("gray67",         "Black", 171, 171, 171);
	defColor("gray66",         "Black", 168, 168, 168);
	defColor("gray65",         "Black", 166, 166, 166);
	defColor("gray64",         "Black", 163, 163, 163);
	defColor("gray63",         "Black", 161, 161, 161);
	defColor("gray62",         "Black", 158, 158, 158);
	defColor("gray61",         "Black", 156, 156, 156);
	defColor("gray60",         "Black", 153, 153, 153);
	defColor("gray59",         "Black", 150, 150, 150);
	defColor("gray58",         "Black", 148, 148, 148);
	defColor("gray57",         "Black", 145, 145, 145);
	defColor("gray56",         "Black", 143, 143, 143);
	defColor("gray55",         "Black", 140, 140, 140);
	defColor("gray54",         "Black", 138, 138, 138);
	defColor("gray53",         "Black", 135, 135, 135);
	defColor("gray52",         "Black", 133, 133, 133);
	defColor("gray51",         "Black", 130, 130, 130);
	defColor("gray50",         "Black", 128, 128, 128);  // MED_GREY
	defColor("gray49",         "Black", 125, 125, 125);
	defColor("gray48",         "Black", 122, 122, 122);
	defColor("gray47",         "Black", 120, 120, 120);
	defColor("gray46",         "Black", 117, 117, 117);
	defColor("gray45",         "Black", 115, 115, 115);
	defColor("gray44",         "Black", 112, 112, 112);
	defColor("gray43",         "Black", 110, 110, 110);
	defColor("gray42",         "Black", 107, 107, 107);
	defColor("gray41",         "Black", 105, 105, 105);
	defColor("gray40",         "Black", 102, 102, 102);
	defColor("gray39",         "Black", 99, 99, 99);
	defColor("gray38",         "Black", 97, 97, 97);
	defColor("gray37",         "Black", 94, 94, 94);
	defColor("gray36",         "Black", 92, 92, 92);
	defColor("gray35",         "Black", 89, 89, 89);
	defColor("gray34",         "Black", 87, 87, 87);
	defColor("gray33",         "Black", 84, 84, 84);
	defColor("gray32",         "Black", 82, 82, 82);
	defColor("gray31",         "Black", 79, 79, 79);
	defColor("gray30",         "Black", 77, 77, 77);
	defColor("gray29",         "Black", 74, 74, 74);
	defColor("gray28",         "Black", 71, 71, 71);
	defColor("gray27",         "Black", 69, 69, 69);
	defColor("gray26",         "Black", 66, 66, 66);
	defColor("gray25",         "Black", 64, 64, 64);
	defColor("gray24",         "Black", 61, 61, 61);
	defColor("gray23",         "Black", 59, 59, 59);
	defColor("gray22",         "Black", 56, 56, 56);
	defColor("gray21",         "Black", 54, 54, 54);
	defColor("gray20",         "Black", 51, 51, 51);
	defColor("gray19",         "Black", 48, 48, 48);
	defColor("gray18",         "Black", 46, 46, 46);
	defColor("gray17",         "Black", 43, 43, 43);
	defColor("gray16",         "Black", 41, 41, 41);
	defColor("gray15",         "Black", 38, 38, 38);
	defColor("gray14",         "Black", 36, 36, 36);
	defColor("gray13",         "Black", 33, 33, 33);
	defColor("gray12",         "Black", 31, 31, 31);
	defColor("gray11",         "Black", 28, 28, 28);
	defColor("gray10",         "Black", 26, 26, 26);
	defColor("gray9",          "Black", 23, 23, 23);
	defColor("gray8",          "Black", 20, 20, 20);
	defColor("gray7",          "Black", 18, 18, 18);
	defColor("gray6",          "Black", 15, 15, 15);
	defColor("gray5",          "Black", 13, 13, 13);
	defColor("gray4",          "Black", 10, 10, 10);
	defColor("gray3",          "Black", 8, 8, 8);
	defColor("gray2",          "Black", 5, 5, 5);
	defColor("gray1",          "Black", 3, 3, 3);
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
	public ArrayList<String> getValidOptions(Entity ent) {
		ArrayList<String> list = new ArrayList<>(colorMap.keySet());
		Collections.sort(list, Input.uiSortOrder);
		return list;
	}

	@Override
	public String toString() {
		return toString(value);
	}
}
