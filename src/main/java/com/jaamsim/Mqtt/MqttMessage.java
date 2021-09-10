/*
 * JaamSim Discrete Event Simulation
 * Copyright (C) 2021 Georg Hackenberg
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
package com.jaamsim.Mqtt;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.Output;
import com.jaamsim.units.UserSpecifiedUnit;

public class MqttMessage extends DisplayEntity {
	
	private String topic;
	private Object value;
	
	public void setTopic(String topic) {
		this.topic = topic;
	}
	
	@Output(name = "Topic", description = "The topic of the MQTT message", reportable = true, sequence = 0, unitType = UserSpecifiedUnit.class)
	public Object getTopic(double simTime) {
		return topic;
	}
	
	public void setValue(Object value) {
		this.value = value;
	}
	
	@Output(name = "Value", description = "The payload of the MQTT message", reportable = true, sequence = 1, unitType = UserSpecifiedUnit.class)
	public Object getValue(double simTime) {
		return value;
	}
	
}