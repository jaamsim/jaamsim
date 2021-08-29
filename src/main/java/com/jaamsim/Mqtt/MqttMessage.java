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