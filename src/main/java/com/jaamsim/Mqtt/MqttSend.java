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

import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.jaamsim.Graphics.DisplayEntity;
import com.jaamsim.ProcessFlow.LinkedComponent;
import com.jaamsim.input.EntityInput;
import com.jaamsim.input.ExpEvaluator;
import com.jaamsim.input.ExpResult;
import com.jaamsim.input.ExpResult.Iterator;
import com.jaamsim.input.ExpressionInput;
import com.jaamsim.input.Keyword;
import com.jaamsim.input.StringInput;
import com.jaamsim.units.DimensionlessUnit;

public class MqttSend extends LinkedComponent {

	@Keyword(description = "The MQTT client", exampleList = "[client]")
	protected final EntityInput<MqttClient> client;
	{
		client = new EntityInput<MqttClient>(MqttClient.class, "Client", KEY_INPUTS, null);
		client.setRequired(true);

		this.addInput(client);
	}

	@Keyword(description = "The MQTT topic", exampleList = "some/mqtt/topic")
	protected final StringInput topic;
	{
		topic = new StringInput("Topic", KEY_INPUTS, null);
		topic.setRequired(true);

		this.addInput(topic);
	}

	@Keyword(description = "The MQTT payload", exampleList = {"1", "\"string\"", "{1, 2, 3}", "{\"a\", \"b\", \"c\"}", "{\"a\"=1, \"b\"=2, \"c\"=3}"})
	private final ExpressionInput expression;
	{
		expression = new ExpressionInput("Expression", KEY_INPUTS, null);
		expression.setUnitType(DimensionlessUnit.class);
		expression.setRequired(true);

		this.addInput(expression);
	}

	protected org.eclipse.paho.client.mqttv3.MqttClient mqtt;

	@Override
	public void lateInit() {
		mqtt = this.client.getValue().getClient();
	}

	@Override
	public void doEnd() {
		mqtt = null;
	}

	@Override
	public void addEntity(DisplayEntity ent) {
		try {
			super.addEntity(ent);
			
			String payload = generatePayload();
			
			MqttMessage message = new MqttMessage(payload.getBytes());
			
			mqtt.publish(topic.getValue(), message);
			
			sendToNextComponent(ent);
		} catch (Exception e) {
			error(e.getLocalizedMessage());
		}
	}
	
	private String generatePayload() throws Exception {
		ExpResult result = ExpEvaluator.evaluateExpression(expression.getValue(), getSimTime());
		
		JSONObject object = new JSONObject();
		
		object.put("value", transformResult(result));
		
		return object.toString();
	}
	
	private Object transformResult(ExpResult result) throws Exception {
		switch (result.type) {
			case NUMBER:
				return result.value;
			case STRING:
				return result.stringVal;
			case COLLECTION:
				if (result.colVal.getSize() == 0) {
					return new JSONArray();
				} else {
					switch (result.colVal.getIter().nextKey().type) {
						case NUMBER: {
							JSONArray array = new JSONArray();
							Iterator iterator = result.colVal.getIter();
							while (iterator.hasNext()) {
								ExpResult key = iterator.nextKey();
								ExpResult value = result.colVal.index(key);
								switch (key.type) {
									case NUMBER:
										array.put(((int) key.value) - 1, transformResult(value));
										break;
									default:
										throw new Exception("Key type not supported: " + key.type);
								}
							}
							return array;
						}
						case STRING: {
							JSONObject object = new JSONObject();
							Iterator iterator = result.colVal.getIter();
							while (iterator.hasNext()) {
								ExpResult key = iterator.nextKey();
								ExpResult value = result.colVal.index(key);
								switch (key.type) {
									case STRING:
										object.put(key.stringVal, transformResult(value));
										break;
									default:
										throw new Exception("Key type not supported: " + key.type);
								}
							}
							return object;
						}
						default:
							throw new Exception("Key type not supported: " + result.colVal.getIter().nextKey().type);
					}
				}
			default:
				throw new Exception("Result type not supported: " + result.type);
		}
	}

}