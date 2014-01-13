#!/bin/sh
#
# Build script for a JaamSim jarfile
# Created Dec 9, 2011 by Harvey Harrison
#
JARNAME="JaamSim$1.jar"
LOGFILE="build.log"

CLASSES=" -C build/ ."
RESFILES="-C src/main/resources/ ."

BUILDDIR="-d build/"
CLASSPATH="-classpath jar/:jar/*"

# Clean out any build products from old builds
# note we don't use git-clean here to avoid removing temp files we have in the
# source tree that people might want to keep.
if [ ! -d build ]
then
	mkdir build
else
	rm -rf build/*
fi

# Move old build files out of the way
if [ -e "$LOGFILE" ]
then
	mv -f "$LOGFILE" "$LOGFILE.old"
fi

# Compile
javac -Xmaxwarns 100000 -Xlint $CLASSPATH $BUILDDIR src/main/java/com/sandwell/*/*.java src/main/java/com/jaamsim/*/*.java src/main/java/com/jaamsim/render/util/*.java src/main/java/com/jaamsim/video/*/*.java > "$LOGFILE" 2>&1

# If there were any errors during compilation, complain
# test for a non-zero error code
if [ ! $? ]
then
	echo "Error during compilation, consult $LOGFILE for details"
	exit
else
	echo "Compilation finished successfully"
fi

# Package
rm -f $JARNAME
jar cfme $JARNAME Manifest.MF com.sandwell.JavaSimulation3D.GUIFrame $CLASSES $RESFILES

# If there were any errors while producing the jar file, complain
# test for a non-zero error code
if [ ! $? ]
then
	echo "Error producing jar file"
	exit
else
	echo "JAR file created:$JARNAME"
fi
