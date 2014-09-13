/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2013 Ausenco Engineering Canada Inc.
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
package com.jaamsim.BasicObjects;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import com.jaamsim.Samples.SampleExpInput;
import com.jaamsim.input.ColourInput;
import com.jaamsim.input.Input;
import com.jaamsim.input.InputAgent;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.ValueInput;
import com.jaamsim.input.Vec3dListInput;
import com.jaamsim.math.Vec3d;
import com.jaamsim.render.HasScreenPoints;
import com.jaamsim.units.DimensionlessUnit;
import com.jaamsim.units.DistanceUnit;
import com.jaamsim.units.TimeUnit;
import com.sandwell.JavaSimulation.EntityTarget;
import com.sandwell.JavaSimulation3D.DisplayEntity;

/**
 * Moves one or more Entities along a path with a specified travel time. Entities can have different travel times, which
 * are represented as varying speeds.
 */
public class EntityDelay extends LinkedComponent implements HasScreenPoints {

	@Keyword(description = "The delay time for the path.\n" +
			"The input can be a constant value, a time series of values, or a probability distribution to be sampled.",
	         example = "Delay1 Duration { 10.0 s }")
	private final SampleExpInput duration;

    @Keyword(description = "A list of { x, y, z } coordinates defining the line segments that" +
            "make up the path.  When two coordinates are given it is assumed that z = 0." ,
             example = "Delay1  Points { { 6.7 2.2 m } { 4.9 2.2 m } { 4.9 3.4 m } }")
	private final Vec3dListInput pointsInput;

	@Keyword(description = "The width of the path in pixels.",
	         example = "Delay1 Width { 1 }")
	private final ValueInput widthInput;

	@Keyword(description = "The colour of the path.\n" +
			"The input can be a colour keyword or RGB value.",
	         example = "Delay1 Color { red }")
	private final ColourInput colorInput;

	private final HashMap<Long, EntityDelayEntry> entityMap = new HashMap<Long, EntityDelayEntry>();  // List of the entities being handled

	private double totalLength;  // Graphical length of the path
	private final ArrayList<Double> lengthList;  // Length of each segment of the path
	private final ArrayList<Double> cumLengthList;  // Total length to the end of each segment

	private Object screenPointLock = new Object();
	private HasScreenPoints.PointsInfo[] cachedPointInfo;

	{
		duration = new SampleExpInput( "Duration", "Key Inputs", null);
		duration.setUnitType(TimeUnit.class);
		duration.setEntity(this);
		this.addInput( duration);

		ArrayList<Vec3d> defPoints =  new ArrayList<Vec3d>();
		defPoints.add(new Vec3d(0.0d, 0.0d, 0.0d));
		defPoints.add(new Vec3d(1.0d, 0.0d, 0.0d));
		pointsInput = new Vec3dListInput("Points", "Key Inputs", defPoints);
		pointsInput.setValidCountRange( 2, Integer.MAX_VALUE );
		pointsInput.setUnitType(DistanceUnit.class);
		this.addInput(pointsInput);

		widthInput = new ValueInput("Width", "Key Inputs", 1.0d);
		widthInput.setUnitType(DimensionlessUnit.class);
		widthInput.setValidRange(1.0d, Double.POSITIVE_INFINITY);
		this.addInput(widthInput);

		colorInput = new ColourInput("Color", "Key Inputs", ColourInput.BLACK);
		this.addInput(colorInput);
		this.addSynonym(colorInput, "Colour");
	}

	public EntityDelay() {
		lengthList = new ArrayList<Double>();
		cumLengthList = new ArrayList<Double>();
	}

	@Override
	public void earlyInit() {
		super.earlyInit();
		this.setPresentState("Idle");

		entityMap.clear();

		// Initialize the segment length data
		lengthList.clear();
		cumLengthList.clear();
		totalLength = 0.0;
		for( int i = 1; i < pointsInput.getValue().size(); i++ ) {
			// Get length between points
			Vec3d vec = new Vec3d();
			vec.sub3( pointsInput.getValue().get(i), pointsInput.getValue().get(i-1));
			double length = vec.mag3();

			lengthList.add( length);
			totalLength += length;
			cumLengthList.add( totalLength);
		}
	}

	private static class EntityDelayEntry {
		DisplayEntity ent;
		double startTime;
		double duration;
	}

	@Override
	public void addDisplayEntity( DisplayEntity ent ) {
		super.addDisplayEntity(ent);

		// Add the entity to the list of entities being delayed
		double simTime = this.getSimTime();
		double dur = duration.getValue().getNextSample(simTime);

		EntityDelayEntry entry = new EntityDelayEntry();
		entry.ent = ent;
		entry.startTime = simTime;
		entry.duration = dur;
		entityMap.put(ent.getEntityNumber(), entry);

		this.scheduleProcess(dur, 5, new RemoveDisplayEntityTarget(this, ent));
	}

	private static class RemoveDisplayEntityTarget extends EntityTarget<EntityDelay> {
		private final DisplayEntity delayedEnt;

		RemoveDisplayEntityTarget(EntityDelay d, DisplayEntity e) {
			super(d, "removeDisplayEntity");
			delayedEnt = e;
		}

		@Override
		public void process() {
			ent.removeDisplayEntity(delayedEnt);
		}
	}

	public void removeDisplayEntity(DisplayEntity ent) {
		// Remove the entity from the lists
		entityMap.remove(ent.getEntityNumber());

		// Send the entity to the next component
		this.sendToNextComponent(ent);
	}

	/**
	 * Return the position coordinates for a given distance along the path.
	 * @param dist = distance along the path.
	 * @return position coordinates
	 */
	private Vec3d getPositionForDistance( double dist) {

		// Find the present segment
		int seg = 0;
		for( int i = 0; i < cumLengthList.size(); i++) {
			if( dist <= cumLengthList.get(i)) {
				seg = i;
				break;
			}
		}

		// Interpolate between the start and end of the segment
		double frac = 0.0;
		if( seg == 0 ) {
			frac = dist / lengthList.get(0);
		}
		else {
			frac = ( dist - cumLengthList.get(seg-1) ) / lengthList.get(seg);
		}
		if( frac < 0.0 )  frac = 0.0;
		else if( frac > 1.0 )  frac = 1.0;

		Vec3d vec = new Vec3d();
		vec.interpolate3(pointsInput.getValue().get(seg), pointsInput.getValue().get(seg+1), frac);
		return vec;
	}

	@Override
	public void updateForInput( Input<?> in ) {
		super.updateForInput(in);

		// If Points were input, then use them to set the start and end coordinates
		if( in == pointsInput || in == colorInput || in == widthInput ) {
			synchronized(screenPointLock) {
				cachedPointInfo = null;
			}
			return;
		}
	}

	@Override
	public void updateGraphics( double simTime ) {

		// Loop through the entities on the path
		for (EntityDelayEntry entry : entityMap.values()) {
			// Calculate the distance travelled by this entity
			double dist = ( simTime - entry.startTime ) / entry.duration * totalLength;

			// Set the position for the entity
			entry.ent.setPosition( this.getPositionForDistance( dist) );
		}
	}

	@Override
	public HasScreenPoints.PointsInfo[] getScreenPoints() {
		synchronized(screenPointLock) {
			if (cachedPointInfo == null) {
				cachedPointInfo = new HasScreenPoints.PointsInfo[1];
				HasScreenPoints.PointsInfo pi = new HasScreenPoints.PointsInfo();
				cachedPointInfo[0] = pi;

				pi.points = pointsInput.getValue();
				pi.color = colorInput.getValue();
				pi.width = widthInput.getValue().intValue();
				if (pi.width < 1) pi.width = 1;
			}
			return cachedPointInfo;
		}
	}

	@Override
	public boolean selectable() {
		return true;
	}

	/**
	 *  Inform simulation and editBox of new positions.
	 */
	@Override
	public void dragged(Vec3d dist) {
		ArrayList<Vec3d> vec = new ArrayList<Vec3d>(pointsInput.getValue().size());
		for (Vec3d v : pointsInput.getValue()) {
			vec.add(new Vec3d(v.x + dist.x, v.y + dist.y, v.z + dist.z));
		}

		StringBuilder tmp = new StringBuilder();
		for (Vec3d v : vec) {
			tmp.append(String.format((Locale)null, " { %.3f %.3f %.3f m }", v.x, v.y, v.z));
		}
		InputAgent.processEntity_Keyword_Value(this, pointsInput, tmp.toString());

		super.dragged(dist);
	}

}
