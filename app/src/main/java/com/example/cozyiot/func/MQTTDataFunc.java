package com.example.cozyiot.func;

import android.util.Log;

public class MQTTDataFunc {

    private String IPAddress;
    private String ID;
    private String Password;
    private static MqttConnector connector;

    public MQTTDataFunc(String IPAddress, String ID, String Password){
        this.IPAddress = IPAddress;
        this.ID = ID;
        this.Password = Password;
        connector = new MqttConnector(this.IPAddress, this.ID, this.Password);
    }

    // 서버에 데이터를 호출하는 함수
    public String callData(String message){
        boolean isconnect = connector.connect();
        String returnValue;
        String topic = "pico/callData";

        if(isconnect){
            connector.publish(topic, message);
            returnValue = "success";
        } else {
            returnValue = "fail";
        }

        Log.i("callData", message);

        connector.disconnect();
        return returnValue;
    }

    //데이터 호출 후 업로드된 데이터를 추출하는 함수
    public String getData(String topic){
        boolean isConnect = connector.connect();
        String finalTopic = "pico/getData" + topic;
        String returnData;

        if(isConnect){
            connector.subscribe(topic);
            returnData = connector.getLatestMessage(finalTopic);
            System.out.println(returnData);
        } else {
            returnData = "fail";
        }

        connector.disconnect();
        return  returnData;
    }

    public String pushData(String message, String topic){
        boolean isConnect = connector.connect();
        String returnString;

        if(isConnect){
            connector.publish(topic, message);
            returnString = "success";
        } else {
            returnString = "fail";
        }

        return returnString;
    }

    private void quitConnection(){
        try{
            connector.disconnect();
            Log.i("MQTT", "Disconnect");
        } catch (Exception e) {
            Log.e("MQTT", "ERROR OCCURRED : " + e);
        }

    }
}
