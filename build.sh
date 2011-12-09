#!/bin/sh
#
# Build script for a JaamSim jarfile including the Java3D classes
# Created Dec 9, 2011 by Harvey Harrison
#

JARNAME="JaamSim$1.jar"
LOGFILE="build.log"
SIMSOURCE="com/sandwell/JavaSimulation/*.java com/sandwell/JavaSimulation3D/*.java com/sandwell/JavaSimulation3D/util/*.java"
EXTSOURCE="com/eteks/sweethome3d/*/*.java"

BUILDDIR="build/"
RESFILES="resources/"
BLDFILES="-C $BUILDDIR ."

# Clean out any build products from old builds
# note we don't use git-clean here to avoid removing temp files we have in the
# source tree that people might want to keep.
rm -rf $BUILDDIR
mkdir $BUILDDIR

# Move old build files out of the way
if [ -e "$LOGFILE" ]
then
	mv -f "$LOGFILE" "$LOGFILE.old"
fi

# Compile
javac -d $BUILDDIR -Xmaxwarns 100000 -Xlint -classpath lib/ $SIMSOURCE $EXTSOURCE > "$LOGFILE" 2>&1
cp -r lib/* $BUILDDIR

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
jar cfe $JARNAME com.sandwell.JavaSimulation3D.GUIFrame $RESFILES $BLDFILES

# If there were any errors while producing the jar file, complain
# test for a non-zero error code
if [ ! $? ]
then
	echo "Error producing jar file"
	exit
else
	echo "JAR file created:$JARNAME"
fi
