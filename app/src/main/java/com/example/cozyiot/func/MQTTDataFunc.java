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
    public String callData(String topic_var, String message){
        String callTopic;
        String getTopic;
        String returnData;

        if(topic_var.equals("pico")){
            callTopic = "pico/callData";
            getTopic = "pico/getData/"+message;
        } else {
            callTopic = "userInfo/callData/"+this.ID;
            getTopic = "userInfo/getData/"+this.ID+"/"+message;
        }

        connector.connect();

        // 연결 완료될 때까지 기다리기 (최대 5초)
        int retryCount = 0;
        while (!connector.isConnected() && retryCount < 50) {  // 50*100ms = 5초
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            retryCount++;
        }
        connector.subscribe(getTopic);

        if (!connector.isConnected()) {
            Log.e("callData", "MQTT 연결 실패로 publish 불가");
            return "fail";
        }

//        connector.subscribe(getTopic);
        connector.publish(callTopic, message);

//        Log.i("callData", message);
//        connector.disconnect();

        retryCount = 0;
        returnData = connector.getLatestMessage(getTopic);
        while(returnData == null && retryCount < 50){
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            returnData = connector.getLatestMessage(getTopic);
            retryCount++;
        }
        System.out.println(getTopic+": "+returnData);

        if (returnData != null) {
            connector.disconnect();
        }
        return returnData;
    }

    //데이터 호출 후 업로드된 데이터를 추출하는 함수
//    public String getData(String topic_var, String topic){
////        boolean isConnect = connector.connect();
//        String finalTopic;
//        if(topic_var.equals("pico")){
//            finalTopic = "pico/getData/"+topic;
//        } else {
//            finalTopic = "userInfo/getData/"+this.ID+"/"+topic;
//        }
//        String returnData;
//
//        if(isConnect){
//            connector.subscribe(finalTopic);
//            returnData = connector.getLatestMessage(finalTopic);
//            System.out.println(returnData);
//        } else {
//            returnData = "fail";
//        }
//
////        connector.disconnect();
//        return  returnData;
//    }

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

    public void quitConnection(){
        try{
            connector.disconnect();
            Log.i("MQTT", "Disconnect");
        } catch (Exception e) {
            Log.e("MQTT", "ERROR OCCURRED : " + e);
        }

    }

    public String getLatestMessage(String topicVar ,String topic){
        String getTopic;

        if(topicVar.equals("pico")){
            getTopic = "pico/getData/"+topic;
        } else {
            getTopic = "userInfo/getData/"+this.ID+"/"+topic;
        }
        return connector.getLatestMessage(getTopic);
    }
}
