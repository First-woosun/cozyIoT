package com.example.cozyiot;

import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MqttSub implements MqttCallback {

    private static MqttClient mqttClient;
    private static final String TAG = "MqttConnector";
    private static final String SERVER_URI = "tcp://218.49.196.80:1883"; //공인외부 아이피 설정
    private static final String CLIENT_ID = "CozyDow";

    public static void createMqttClient() {
        try {
            mqttClient = new MqttClient(SERVER_URI, CLIENT_ID, null);
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
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
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    Log.d(TAG, "Delivery complete");
                }
            });
        } catch (MqttException e) {
            Log.e(TAG, "Failed to connect to MQTT broker", e);
        }
    }

    @Override
    public void connectionLost(Throwable arg0) {
        // TODO Auto-generated method stub
        System.out.println(arg0);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // TODO Auto-generated method stub
        System.out.println("topic   : " + topic);
        System.out.println("message : " + message.getPayload());
    }
}