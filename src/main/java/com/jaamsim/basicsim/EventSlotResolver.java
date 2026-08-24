/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2026 JaamSim Software Inc.
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
package com.jaamsim.basicsim;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;

import com.jaamsim.events.EventHandle;
import com.jaamsim.events.ProcessTarget;

/**
 * Maps each ProcessTarget and EventHandle in a model to a stable textual name, so that a
 * pending event can be recorded in a checkpoint file and re-created on restore.
 * <p>
 * Nearly every live target in JaamSim is held in a final field of the entity that
 * schedules it, for example Device.endStepTarget with its endStepHandle. This class finds
 * those targets by reflecting over the fields of every entity, which avoids having to
 * modify each of the scheduling call sites.
 * <p>
 * A target that cannot be resolved is not a silent failure. It means the target is
 * constructed with arguments, or held in a collection, and therefore needs hand-written
 * support before a model using it can be checkpointed. CheckpointProbe reports those.
 * <p>
 * Slot names have the form {@code <owner>#<DeclaringClass>.<field>}, where owner is the
 * entity number, or "model" for the targets owned by the JaamSimModel itself. The
 * declaring class is included so that identically named fields at different levels of an
 * inheritance chain do not collide.
 */
public class EventSlotResolver {

	/** Owner name used for targets held by the JaamSimModel rather than by an entity. */
	public static final String MODEL_OWNER = "model";

	private final IdentityHashMap<ProcessTarget, String> targetSlots = new IdentityHashMap<>();
	private final IdentityHashMap<EventHandle, String> handleSlots = new IdentityHashMap<>();

	private int numOwnersScanned;
	private int numFieldsRejected;

	public EventSlotResolver(JaamSimModel simModel) {

		// The JaamSimModel owns a few targets of its own, such as the threshold-changed
		// target and the pause-condition target
		scanOwner(simModel, MODEL_OWNER);

		for (Entity ent : simModel.getClonesOfIterator(Entity.class)) {
			scanOwner(ent, Long.toString(ent.getEntityNumber()));
		}
	}

	/**
	 * Records every ProcessTarget and EventHandle held in an instance field of the given
	 * object, walking up the class hierarchy so that inherited fields are included.
	 */
	private void scanOwner(Object owner, String ownerName) {
		numOwnersScanned++;

		Class<?> klass = owner.getClass();
		while (klass != null && klass != Object.class) {
			for (Field fld : klass.getDeclaredFields()) {

				// A static field cannot belong to a particular entity
				if (Modifier.isStatic(fld.getModifiers()))
					continue;

				Class<?> type = fld.getType();
				boolean isTarget = ProcessTarget.class.isAssignableFrom(type);
				boolean isHandle = EventHandle.class.isAssignableFrom(type);
				boolean isArray = type.isArray()
						&& (ProcessTarget.class.isAssignableFrom(type.getComponentType())
						 || EventHandle.class.isAssignableFrom(type.getComponentType()));

				if (!isTarget && !isHandle && !isArray)
					continue;

				Object val = null;
				try {
					fld.setAccessible(true);
					val = fld.get(owner);
				}
				catch (Throwable t) {
					// Field is not reachable by reflection, so anything it holds will be
					// reported as unresolved rather than being silently mapped
					numFieldsRejected++;
					continue;
				}

				if (val == null)
					continue;

				String slot = String.format("%s#%s.%s",
						ownerName, klass.getSimpleName(), fld.getName());

				if (isArray) {
					int len = Array.getLength(val);
					for (int i = 0; i < len; i++) {
						record(Array.get(val, i), String.format("%s[%s]", slot, i));
					}
					continue;
				}

				record(val, slot);
			}
			klass = klass.getSuperclass();
		}
	}

	private void record(Object val, String slot) {
		if (val instanceof ProcessTarget)
			targetSlots.put((ProcessTarget) val, slot);
		else if (val instanceof EventHandle)
			handleSlots.put((EventHandle) val, slot);
	}

	/**
	 * Returns the slot name for the given target, or null if it could not be resolved.
	 */
	public String getSlot(ProcessTarget target) {
		if (target == null)
			return null;
		return targetSlots.get(target);
	}

	/**
	 * Returns the slot name for the given handle, or null if it could not be resolved.
	 */
	public String getSlot(EventHandle handle) {
		if (handle == null)
			return null;
		return handleSlots.get(handle);
	}

	public int getNumTargetSlots() {
		return targetSlots.size();
	}

	public int getNumHandleSlots() {
		return handleSlots.size();
	}

	public int getNumOwnersScanned() {
		return numOwnersScanned;
	}

	public int getNumFieldsRejected() {
		return numFieldsRejected;
	}

}
