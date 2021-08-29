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
			error(e.getLocalizedMessage());
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
			error(e.getLocalizedMessage());
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
		error(exception.getLocalizedMessage());
	}

}