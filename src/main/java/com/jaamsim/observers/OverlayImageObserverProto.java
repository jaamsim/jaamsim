package com.jaamsim.observers;

import com.sandwell.JavaSimulation.Entity;

public class OverlayImageObserverProto extends ObserverProto {
	@Override
	public Observer instantiate() {
		return new OverlayImageObserver(observeeInput.getValue());
	}

	@Override
	public Observer instantiate(Entity observee) {
		return new OverlayImageObserver(observee);
	}

}
