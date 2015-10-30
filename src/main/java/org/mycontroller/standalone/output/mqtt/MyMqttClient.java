/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mycontroller.standalone.output.mqtt;

import java.util.HashMap;

import org.eclipse.paho.client.mqttv3.IMqttClient;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.mycontroller.standalone.AppProperties;
import org.mycontroller.standalone.ObjectFactory;
import org.mycontroller.standalone.api.jaxrs.mapper.GatewayInfo;
import org.mycontroller.standalone.gateway.IMySensorsGateway;
import org.mycontroller.standalone.gateway.MySensorsGatewayException;
import org.mycontroller.standalone.gateway.mqtt.MqttCallbackListener;
import org.mycontroller.standalone.gateway.mqtt.MqttGatewayCommon;
import static org.mycontroller.standalone.gateway.mqtt.MqttGatewayImpl.CLIENT_ID;
import static org.mycontroller.standalone.gateway.mqtt.MqttGatewayImpl.CONNECTION_TIME_OUT;
import static org.mycontroller.standalone.gateway.mqtt.MqttGatewayImpl.KEEP_ALIVE;
import org.mycontroller.standalone.mysensors.RawMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 *
 * @author loris
 */
public class MyMqttClient {
    private static final Logger _logger = LoggerFactory.getLogger(MyMqttClient.class.getName());

    public static final long TIME_TO_WAIT = 100;
    public static final long DISCONNECT_TIME_OUT = 1000 * 1;
    public static final int CONNECTION_TIME_OUT = 1000 * 5;
    public static final int KEEP_ALIVE = 1000 * 5;
    public static final String CLIENT_ID = "MC";
    public static final int MY_SENSORS_QOS = 0;
    
    private IMqttClient _mqttClient;
    
    public MyMqttClient() {
    try {
    _mqttClient = new MqttClient(
                    "tcp://" + ObjectFactory.getAppProperties().getClientMqttHost() + ":"
                            + ObjectFactory.getAppProperties().getClientMqttPort(), CLIENT_ID);
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setConnectionTimeout(CONNECTION_TIME_OUT);
            connectOptions.setKeepAliveInterval(KEEP_ALIVE);
            _mqttClient.connect(connectOptions);
            _mqttClient.subscribe(ObjectFactory.getAppProperties().getClientMqttRootTopic() + "/#");
            _logger.info("MQTT Client[{}] connected successfully..", _mqttClient.getServerURI());
  
        } catch (MqttException ex) {        
            _logger.error(
                    "Unable to connect with MQTT broker gateway[{}], Reason Code: {}, Reboot '{}' service once MQTT Broker gateway comes UP!",
                    _mqttClient.getServerURI(), ex.getReasonCode(), AppProperties.APPLICATION_NAME, ex);
        }
    }     
    
    public synchronized void write(RawMessage rawMessage) throws MySensorsGatewayException {
        _logger.debug("Message to send, Topic:[{}], PayLoad:[{}]", rawMessage.getMqttTopic(), rawMessage.getPayload());
        try {
            MqttMessage message = new MqttMessage(rawMessage.getPayloadBytes());
            message.setQos(MY_SENSORS_QOS);
            _mqttClient.publish(rawMessage.getMqttTopic(), message);
        } catch (MqttException ex) {
            if (ex.getMessage().contains("Timed out waiting for a response from the server")) {
                _logger.debug(ex.getMessage());
            } else {
                _logger.error("Exception, Reason Code:{}", ex.getReasonCode(), ex);                
            }
        }
    }
    
    public void close() {
        try {            
            _mqttClient.disconnect(DISCONNECT_TIME_OUT);
            _mqttClient.close();
        } catch (Exception ex) {
            _logger.error("Exception,", ex);
        }
    }

     
}
