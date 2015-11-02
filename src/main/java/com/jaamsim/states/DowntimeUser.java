package com.jaamsim.states;

import java.util.ArrayList;

import com.jaamsim.BasicObjects.DowntimeEntity;

public interface DowntimeUser {
	public ArrayList<DowntimeEntity> getMaintenanceEntities();
	public ArrayList<DowntimeEntity> getBreakdownEntities();
	public boolean canStartDowntime(DowntimeEntity down);
	public void prepareForDowntime(DowntimeEntity down);
	public void startDowntime(DowntimeEntity down, double dur);
	public void endDowntime(DowntimeEntity down);
}
