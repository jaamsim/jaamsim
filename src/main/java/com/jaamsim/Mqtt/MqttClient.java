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

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;

public class MqttClient extends DisplayEntity implements MqttCallback {
	
	@Keyword(description = "The server URI", exampleList = "tcp://localhost:1883")
	private final StringInput serverUri;
	{
		serverUri = new StringInput("ServerUri", KEY_INPUTS, "tcp://localhost:1883");
		serverUri.setRequired(true);
		
		this.addInput(serverUri);
	}

	@Keyword(description = "The client ID", exampleList = "JaamSim")
	private final StringInput clientId;
	{
		clientId = new StringInput("ClientId", KEY_INPUTS, "JaamSim");
		clientId.setRequired(true);
		
		this.addInput(clientId);
	}
	private MqttConnectOptions options;
	{
		options = new MqttConnectOptions();
		options.setCleanSession(true);
		options.setAutomaticReconnect(true);
	}
	 
	private org.eclipse.paho.client.mqttv3.MqttClient client;
	
	public org.eclipse.paho.client.mqttv3.MqttClient getClient() {
		return client;
	}
	 
	@Override
	public void earlyInit() {
		try {
			super.earlyInit();
			
			client = new org.eclipse.paho.client.mqttv3.MqttClient(serverUri.getValue(), clientId.getValue());
			client.connect(options);
			client.setCallback(this);
		} catch (MqttException e) {
			error("The MQTT client could not connect (reason code = " + e.getReasonCode() + ")");
		} catch (Exception e) {
			error("The MQTT client could not connect (exception = " + e.getLocalizedMessage() + ")");
		}
	}
	
	@Override
	public void doEnd() {
		try {
			super.doEnd();
			
			if (client.isConnected()) {
				client.disconnect();
			}
		} catch (MqttException e) {
			error("The MQTT client could not disconnect (reason code = " + e.getReasonCode() + ")");
		} catch (Exception e) {
			error("The MQTT client could not disconnect (exception = " + e.getLocalizedMessage() + ")");
		}
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		// Ignore
	}
	
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// Ignore
	}
	
	@Override
	public void connectionLost(Throwable exception) {
		error("The MQTT client lost its connection (exception = " + exception.getLocalizedMessage() + ")");
	}

}