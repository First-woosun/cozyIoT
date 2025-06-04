package com.example.cozyiot.func;

import android.util.Log;
import org.eclipse.paho.client.mqttv3.*;

public class MqttConnector {
    private MqttClient mqttClient;
    private String latestMessage = null;
    private static final String TAG = "MqttConnector";
    private final String serverUri;
    private final String clientId;
    private final String clientPassword;

    // 생성자에서 정보만 설정하고 연결은 나중에
    public MqttConnector(String ipAddress, String clientId, String clientPassword) {
        this.serverUri = "tcp://" + ipAddress;
        this.clientId = clientId;
        this.clientPassword = clientPassword;
    }

    // 연결 메서드 (성공 시 true 반환)
    public boolean connect() {
        try {
            mqttClient = new MqttClient(serverUri, clientId, null);

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(clientId);
            options.setPassword(clientPassword.toCharArray());

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    Log.d(TAG, "Message arrived: " + message.toString());
                    latestMessage = message.toString();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Delivery complete");
                }
            });

            mqttClient.connect(options);
            Log.d(TAG, "Connected to MQTT broker");
            return true;
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker", e);
            return false;
        }
    }

    public void publish(String topic, String message) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttClient.publish(topic, mqttMessage);
                Log.d(TAG, "Message published to topic: " + topic);
            } else {
                Log.e(TAG, "MQTT client not connected");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Failed to publish message", e);
        }
    }

    public void subscribe(String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic);
                Log.d(TAG, "Subscribed to topic: " + topic);
            } else {
                Log.e(TAG, "MQTT client not connected");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Failed to subscribe", e);
        }
    }

    public boolean disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            }
        } catch (MqttException e) {
            Log.e(TAG, "Failed to disconnect", e);
            return true;
        }
        return false;
    }

    public String getLatestMessage() {
        return latestMessage;
    }
}
