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

private static void defColor(String colorName, String family, int r, int g, int b) {
	mapColor(colorName, family, new Color4d(r, g, b, 255));
}

private static void mapColor(String colorName, String family, Color4d col) {
	String name = colorName.toLowerCase();
	if (colorMap.put(name, col) != null)
		System.out.println(String.format("ColorName added twice: %s ", colorName));
	if (colorNameMap.put(col, colorName) != null)
		System.out.println(String.format("Color4d added twice: %s", colorName));
	namedColourList.add(col);
}

private static void initColors() {

	// Pink colours
	defColor("pink",            "Pink", 255, 192, 203);
	defColor("lightpink",       "Pink", 255, 182, 193);
	defColor("hotpink",         "Pink", 255, 105, 180);
	defColor("deeppink",        "Pink", 255,  20, 147);
	defColor("palevioletred",   "Pink", 219, 112, 147);
	defColor("mediumvioletred", "Pink", 199,  21, 133);

	defColor("violetred",       "Pink", 208,  32, 144);

	// Red colours
	defColor("lightsalmon", "Red", 255, 160, 122);
	defColor("salmon",      "Red", 250, 128, 114);
	defColor("darksalmon",  "Red", 233, 150, 122);
	defColor("lightcoral",  "Red", 240, 128, 128);
	defColor("indianred",   "Red", 176,  23,  31);
	defColor("crimson",     "Red", 220,  20,  60);
	defColor("firebrick",   "Red", 178,  34,  34);
	mapColor("darkred",     "Red", DARK_RED);
	mapColor("red",         "Red", RED);

	// Orange colours
	defColor("orangered",     "Orange", 255,  69,   0);
	defColor("tomato",        "Orange", 255,  99,  71);
	defColor("coral",         "Orange", 255, 114,  86);
	defColor("orange",        "Orange", 255, 128,   0);

	defColor("flesh",         "Orange", 255, 125,  64);
	defColor("carrot",        "Orange", 237, 145,  33);
	defColor("cadmiumorange", "Orange", 255,  97,   3);

	// Yellow colours
	mapColor("yellow",               "Yellow", YELLOW);
	defColor("lightyellow",          "Yellow", 255, 255, 224);
	defColor("lemonchiffon",         "Yellow", 255, 250, 205);
	defColor("lightgoldenrodyellow", "Yellow", 250, 250, 210);
	defColor("papayawhip",           "Yellow", 255, 239, 213);
	defColor("moccasin",             "Yellow", 255, 228, 181);
	defColor("peachpuff",            "Yellow", 255, 218, 185);
	defColor("palegoldenrod",        "Yellow", 238, 232, 170);
	defColor("khaki",                "Yellow", 240, 230, 140);
	defColor("darkkhaki",            "Yellow", 189, 183, 107);
	defColor("gold",                 "Yellow", 255, 215,   0);

	defColor("cadmiumyellow",        "Yellow", 255, 153,  18);
	defColor("lightgoldenrod",       "Yellow", 255, 236, 139);
	defColor("banana",               "Yellow", 227, 207,  87);
	mapColor("darkyellow",           "Yellow", DARK_YELLOW);

	// Brown colours
	defColor("cornsilk",       "Brown", 255, 248, 220);
	defColor("blanchedalmond", "Brown", 255, 235, 205);
	defColor("bisque",         "Brown", 255, 228, 196);
	defColor("navajowhite",    "Brown", 255, 222, 173);
	defColor("wheat",          "Brown", 245, 222, 179);
	defColor("burlywood",      "Brown", 222, 184, 135);
	defColor("tan",            "Brown", 210, 180, 140);
	defColor("sandybrown",     "Brown", 244, 164,  96);
	defColor("goldenrod",      "Brown", 218, 165,  32);
	defColor("darkgoldenrod",  "Brown", 184, 134,  11);
	defColor("chocolate",      "Brown", 210, 105,  30);
	defColor("sienna",         "Brown", 160,  82,  45);
	defColor("brown",          "Brown", 138,  54,  15);
	defColor("maroon",         "Brown", 128,   0,   0);

	defColor("rawsienna",      "Brown", 199,  97,  20);
	defColor("burntumber",     "Brown", 138,  51,  36);
	defColor("sepia",          "Brown",  94,  38,  18);
	defColor("brick",          "Brown", 156, 102,  31);
	defColor("melon",          "Brown", 227, 168, 105);

	// Green colours
	defColor("darkolivegreen",    "Green",  85, 107,  47);
	defColor("olive",             "Green", 128, 128,   0);
	defColor("olivedrab",         "Green", 107, 142,  35);
	defColor("limegreen",         "Green",  50, 205,  50);
	defColor("lawngreen",         "Green", 124, 252,   0);
	defColor("greenyellow",       "Green", 173, 255,  47);
	defColor("springgreen",       "Green",   0, 255, 127);
	defColor("mediumspringgreen", "Green",   0, 250, 154);
	defColor("palegreen",         "Green", 152, 251, 152);
	defColor("darkseagreen",      "Green", 143, 188, 143);
	defColor("mediumseagreen",    "Green",  60, 179, 113);
	defColor("seagreen",          "Green",  84, 255, 159);
	defColor("forestgreen",       "Green",  34, 139,  34);
	mapColor("green",             "Green", GREEN);
	mapColor("darkgreen",         "Green", DARK_GREEN);

	defColor("mint",              "Green", 189, 252, 201);
	defColor("emeraldgreen",      "Green",   0, 201,  87);
	defColor("cobaltgreen",       "Green",  61, 145,  64);
	defColor("sapgreen",          "Green",  48, 128,  20);

	// Cyan colours
	mapColor("cyan",            "Cyan", CYAN);
	defColor("lightcyan",       "Cyan", 224, 255, 255);
	defColor("paleturquoise",   "Cyan", 187, 255, 255);
	defColor("aquamarine",      "Cyan", 127, 255, 212);
	defColor("turquoise",       "Cyan",  64, 224, 208);
	defColor("mediumturquoise", "Cyan",  72, 209, 204);
	defColor("darkturquoise",   "Cyan",   0, 206, 209);
	defColor("lightseagreen",   "Cyan",  32, 178, 170);
	defColor("cadetblue",       "Cyan",  95, 158, 160);
	mapColor("darkcyan",        "Cyan", DARK_CYAN);
	defColor("teal",            "Cyan",   0, 128, 128);

	defColor("manganeseblue",   "Cyan",   3, 168, 158);
	defColor("turquoiseblue",   "Cyan",   0, 199, 140);

	// Blue colours
	defColor("lightsteelblue", "Blue", 176, 196, 222);
	defColor("powderblue",     "Blue", 176, 224, 230);
	defColor("lightblue",      "Blue", 173, 216, 230);
	defColor("skyblue",        "Blue", 135, 206, 235);
	defColor("lightskyblue",   "Blue", 135, 206, 250);
	defColor("deepskyblue",    "Blue",   0, 191, 255);
	defColor("dodgerblue",     "Blue",  30, 144, 255);
	defColor("cornflowerblue", "Blue", 100, 149, 237);
	defColor("steelblue",      "Blue",  70, 130, 180);
	defColor("royalblue",      "Blue",  65, 105, 225);
	mapColor("blue",           "Blue", BLUE);
	mapColor("darkblue",       "Blue", DARK_BLUE);
	defColor("navy",           "Blue",   0,   0, 128);
	defColor("midnightblue",   "Blue",  25,  25, 112);

	defColor("cobalt",         "Blue",  61,  89, 171);
	defColor("peacock",        "Blue",  51, 161, 201);

	// Purple, violet, and magenta colours
	defColor("lavender",        "Purple", 230, 230, 250);
	defColor("thistle",         "Purple", 216, 191, 216);
	defColor("plum",            "Purple", 221, 160, 221);
	defColor("violet",          "Purple", 238, 130, 238);
	defColor("orchid",          "Purple", 218, 112, 214);
	defColor("magenta",         "Purple", 255,   0, 255);
	defColor("mediumorchid",    "Purple", 186,  85, 211);
	defColor("mediumpurple",    "Purple", 147, 112, 219);
	defColor("blueviolet",      "Purple", 138,  43, 226);
	defColor("darkviolet",      "Purple", 148,   0, 211);
	defColor("darkorchid",      "Purple", 153,  50, 204);
	defColor("purple",          "Purple", 128,   0, 128);
	defColor("indigo",          "Purple", 75,    0, 130);
	defColor("darkslateblue",   "Purple", 72,   61, 139);
	defColor("slateblue",       "Purple", 106,  90, 205);
	defColor("mediumslateblue", "Purple", 123, 104, 238);

	mapColor("darkpurple",      "Purple", DARK_PURPLE);
	defColor("raspberry",       "Purple", 135,  38,  87);
	defColor("lightslateblue",  "Purple", 132, 112, 255);

	// White colours
	mapColor("white",         "White", WHITE);
	defColor("honeydew",      "White", 240, 255, 240);
	defColor("mintcream",     "White", 245, 255, 250);
	defColor("azure",         "White", 240, 255, 255);
	defColor("aliceblue",     "White", 240, 248, 255);
	defColor("ghostwhite",    "White", 248, 248, 255);
	defColor("seashell",      "White", 255, 245, 238);
	defColor("beige",         "White", 245, 245, 220);
	defColor("oldlace",       "White", 253, 245, 230);
	defColor("floralwhite",   "White", 255, 250, 240);
	defColor("ivory",         "White", 255, 255, 240);
	defColor("antiquewhite",  "White", 250, 235, 215);
	defColor("linen",         "White", 250, 240, 230);
	defColor("lavenderblush", "White", 255, 240, 245);

	defColor("eggshell",      "White", 252, 230, 201);

	// Grey and black colours
	defColor("lightslategray", "Black", 119, 136, 153);
	defColor("slategray",      "Black", 112, 128, 144);
	defColor("darkslategray",  "Black",  47,  79,  79);
	mapColor("black",          "Black", BLACK);

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
	mapColor("gray75",         "Black", LIGHT_GREY);
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
	mapColor("gray50",         "Black", MED_GREY);
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
