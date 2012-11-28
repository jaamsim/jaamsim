package com.jaamsim.observers;

import com.sandwell.JavaSimulation.Entity;

public class OverlayTextObserverProto extends ObserverProto {
	@Override
	public Observer instantiate() {
		return new OverlayTextObserver(observeeInput.getValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return new OverlayTextObserver(observee);
	}

}
