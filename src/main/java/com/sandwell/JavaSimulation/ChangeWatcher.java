package com.sandwell.JavaSimulation;

import java.util.ArrayList;

/**
 * Change watcher is a useful one writer, many reader alternative to dirty flags
 * The basic pattern is the object with state (eg: DisplayEntity) creates a ChangeWatcher member
 * and anything interested in the state requests a ChangeWatcher.Tracker.
 * On a change in important data, call ChangeWatcher.changed() and the reader calls
 * ChangeWatcher.Tracker.hasChanged() to get that info.
 *
 * This also allows chaining of ChangeWatchers by setting a master Tracker for the watcher. This
 * is used for dependent changes (eg: DisplayEntities are dependent on Regions)
 * @author matt.chudleigh
 *
 */
public class ChangeWatcher {

	private int dataVersion = 0;
	private ArrayList<ChangeWatcher.Tracker> dependentTrackers = new ArrayList<ChangeWatcher.Tracker>();

	public void changed() {
		dataVersion++;
	}

	public class Tracker {
		private int seenVersion = -1;
		/**
		 * Check returns if a change has been detected since the last clear (but does not clear/update the
		 * seen state)
		 * @return
		 */
		public boolean check() {
			// Make sure the master watcher has not been changed
			checkDependents();

			return (seenVersion != dataVersion);
		}

		/**
		 * Returns if the state has changed, and records this as the last seen state (ie: equivalent to
		 * clearing the flag)
		 * @return
		 */
		public boolean checkAndClear() {
			// Make sure the master watcher has not been changed
			checkDependents();

			boolean ret = (seenVersion != dataVersion);
			seenVersion = dataVersion;
			return ret;
		}
	}

	public Tracker getTracker() {
		return new Tracker();
	}

	public void clearDependents() {
		dependentTrackers.clear();
		changed();
	}

	public void addDependent(ChangeWatcher master) {
		changed();
		if (master == null) {
			return;
		}
		dependentTrackers.add(master.getTracker());
	}

	private void checkDependents() {
		for (Tracker t : dependentTrackers) {
			if (t.checkAndClear()) {
				changed();
			}
		}
	}

}
