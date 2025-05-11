package com.example.cozyiot.func;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttConnectorTest {
    private static MqttClient mqttClient;
    private static final String TAG = "MqttConnector";
    private static final String SERVER_URI = "tcp://218.49.196.80:1883"; //공인외부 아이피 설정
    private static final String CLIENT_ID = "cozydow";
    private static final String CLIENT_PASSWORD = "1234";
    private static String latestMassage = null;

    // MQTT 클라이언트 연결
    public static boolean createMqttClient() {
        try {
            mqttClient = new MqttClient(SERVER_URI, CLIENT_ID, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(CLIENT_ID);
            options.setPassword(CLIENT_PASSWORD.toCharArray());
            mqttClient.connect(options);
            Log.d(TAG, "Connected to MQTT broker");

            // 콜백 설정
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    Log.d(TAG, "Message arrived: " + message.toString());
                    latestMassage = message.toString();
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Delivery complete");
                }
            });
            return true;
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker", e);
            return false;
        }
    }

    // MQTT 서버로 메시지 발행
    public static void publish(String message, String topic) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage mqttMessage = new MqttMessage(message.getBytes());
                mqttClient.publish(topic, mqttMessage);
                Log.d(TAG, "Message published to topic: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to publish message", e);
            }
        } else {
            Log.e(TAG, "MQTT client not connected");
        }
    }

    // MQTT 서버 구독
    public static void subscribe(String topic) {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.subscribe(topic);
                Log.d(TAG, "Subscribed to topic: " + topic);
            } catch (MqttException e) {
                Log.e(TAG, "Failed to subscribe", e);
            }
        } else {
            Log.e(TAG, "MQTT client not connected");
        }
    }

    // MQTT 연결 해제
    public static void disconnect() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                Log.d(TAG, "Disconnected from MQTT broker");
            } catch (MqttException e) {
                Log.e(TAG, "Failed to disconnect", e);
            }
        }
    }

    public static String getLatestMassage(){
        return latestMassage;
    }
}
