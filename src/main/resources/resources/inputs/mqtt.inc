Define ObjectType {
	MqttClient
	MqttSend
	MqttReceive
	MqttMessage
}

Define	IconModel  {
	MqttClientIconModel
	MqttSendIconModel
	MqttReceiveIconModel
	MqttMessageIconModel
}

MqttClientIconModel  ImageFile { '<res>/images/MqttClient-256.png'  } Transparent { TRUE }
MqttSendIconModel    ImageFile { '<res>/images/MqttSend-256.png'    } Transparent { TRUE }
MqttReceiveIconModel ImageFile { '<res>/images/MqttReceive-256.png' } Transparent { TRUE }
MqttMessageIconModel ImageFile { '<res>/images/MqttMessage-256.png' } Transparent { TRUE }

MqttClient  JavaClass { com.jaamsim.Mqtt.MqttClient  } Palette { 'MQTT Objects' } DefaultDisplayModel { MqttClientIconModel  } IconFile { '<res>/images/MqttClient-24.png'  } DefaultSize { 0.5 0.5 0.5 m }
MqttSend    JavaClass { com.jaamsim.Mqtt.MqttSend    } Palette { 'MQTT Objects' } DefaultDisplayModel { MqttSendIconModel    } IconFile { '<res>/images/MqttSend-24.png'    } DefaultSize { 0.5 0.5 0.5 m }
MqttReceive JavaClass { com.jaamsim.Mqtt.MqttReceive } Palette { 'MQTT Objects' } DefaultDisplayModel { MqttReceiveIconModel } IconFile { '<res>/images/MqttReceive-24.png' } DefaultSize { 0.5 0.5 0.5 m }
MqttMessage JavaClass { com.jaamsim.Mqtt.MqttMessage } Palette { 'MQTT Objects' } DefaultDisplayModel { MqttMessageIconModel } IconFile { '<res>/images/MqttMessage-24.png' } DefaultSize { 0.5 0.5 0.5 m }

MqttClient  Description { 'MQTT client object.' }
MqttSend    Description { 'MQTT send operation object.' }
MqttReceive Description { 'MQTT receive operation object.' }
MqttMessage Description { 'MQTT message object.' }