package com.example.cozyiot.func;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

public class SynchronizedMqttConnector {
    private static final String TAG = "MqttConnector";
    private static SynchronizedMqttConnector instance;
    private final Map<String, String> messageMap = new HashMap<>();

    private MqttClient mqttClient;
    private String brokerUrl;
    private String username;
    private String password;

    // 콜백 인터페이스
    public interface MqttEventListener {
        void onConnectionLost(Throwable cause);
        void onMessageReceived(String topic, String message);
        void onDeliveryComplete(IMqttDeliveryToken token);
    }

    private MqttEventListener eventListener;

    private SynchronizedMqttConnector() {}

    // 인스턴스 반환
    public static synchronized SynchronizedMqttConnector getInstance() {
        if (instance == null) {
            instance = new SynchronizedMqttConnector();
        }
        return instance;
    }

    // 콜백 리스너 등록
    public void setEventListener(MqttEventListener listener) {
        this.eventListener = listener;
    }

    // 연결 시도
    public synchronized void connect(String broker, String username, String password) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                Log.i(TAG, "Already connected to MQTT broker.");
                return;
            }

            this.brokerUrl = "tcp://" + broker;
            this.username = username;
            this.password = password;

            mqttClient = new MqttClient(brokerUrl, "CozyIoTClient_" + username, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(username);
            options.setPassword(password.toCharArray());
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);

            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    Log.e(TAG, "Connection lost: " + cause);
                    if (eventListener != null) eventListener.onConnectionLost(cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String msg = new String(message.getPayload());
                    Log.i(TAG, "Message arrived: " + topic + " = " + msg);
                    messageMap.put(topic, message.toString());
                    if (eventListener != null) eventListener.onMessageReceived(topic, msg);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.i(TAG, "Message delivered.");
                    if (eventListener != null) eventListener.onDeliveryComplete(token);
                }
            });

            mqttClient.connect(options);
            Log.i(TAG, "Connecting to MQTT broker...");

        } catch (Exception e) {
            Log.e(TAG, "Failed to connect to broker: " + e.getMessage());
        }
    }

    // 구독
    public void subscribe(String topicVar ,String topic) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String finalTopic;
                if(topicVar.equals("pico")){
                    finalTopic = "pico/getData/"+topic;
                } else {
                    finalTopic = "userInfo/getData/"+this.username+"/"+topic;
                }
                mqttClient.subscribe(finalTopic);
                Log.i(TAG, "Subscribed to topic: " + finalTopic);
            } else {
                Log.e(TAG, "Cannot subscribe - not connected");
            }
        } catch (Exception e) {
            Log.e(TAG, "Subscribe failed: " + e.getMessage());
        }
    }

    // 발행
    public void publish(String topicVar, String message) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String finalTopic;
                if(topicVar.equals("pico")){
                    finalTopic = "pico/callData";
                } else if(topicVar.equals("userInfo")){
                    finalTopic = "userInfo/callData/"+this.username;
                } else {
                    finalTopic = topicVar;
                }
                mqttClient.publish(finalTopic, new MqttMessage(message.getBytes()));
                Log.i(TAG, "Message published: " + message);
            } else {
                Log.e(TAG, "MQTT client not connected, publish failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Publish failed: " + e.getMessage());
        }
    }

    // 자동 제어
    public void autoMotorRequestPublish(String message) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String finalTopic = "pico/auto_run";
                String finalMessage = this.username+","+message;
                mqttClient.publish(finalTopic, new MqttMessage(finalMessage.getBytes()));
                Log.i(TAG, "Message published: " + message);
            } else {
                Log.e(TAG, "MQTT client not connected, publish failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Publish failed: " + e.getMessage());
        }
    }

    // 일반 발행
    public void plantPublish(String message) {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                String topic = "userInfo/plant/"+this.username;
                mqttClient.publish(topic, new MqttMessage(message.getBytes()));
                Log.i(TAG, "Message published: " + message);
            } else {
                Log.e(TAG, "MQTT client not connected, publish failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Publish failed: " + e.getMessage());
        }
    }

    // 연결 상태
    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    // 연결 종료
    public void disconnect() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                Log.i(TAG, "Disconnected from broker.");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to disconnect: " + e.getMessage());
        }
    }

    public String getLatestMessage(String topicVar ,String topic) {
        String finalTopic;
        if(topicVar.equals("pico")){
            finalTopic = "pico/getData/"+topic;
        } else {
            finalTopic = "userInfo/getData/"+this.username+"/"+topic;
        }
        return messageMap.get(finalTopic);
    }
}
