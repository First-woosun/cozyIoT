package com.example.cozyiot.func;

public class topicCompare {

    private String IPAddress;
    private String ID;
    private String Password;
    private static MqttConnector connector;

    public topicCompare(String IPAddress, String ID, String Password){
        this.IPAddress = IPAddress;
        this.ID = ID;
        this.Password = Password;
        connector = new MqttConnector(this.IPAddress, this.ID, this.Password);
    }

    // 서버에 데이터를 호출하는 함수
    private static String callData(String message){
        boolean isconnect = connector.connect();
        String returnValue;
        String topic = "pico/callData";

        if(isconnect){
            connector.publish(topic, message);
            returnValue = "success";
        } else {
            returnValue = "fail";
        }

        return returnValue;
    }

    //데이터 호출 후 업로드된 데이터를 추출하는 함수
    private static String getData(String Message){
        boolean isConnect = connector.connect();
        String topic = "pico/getData";
        String returnData;

        if(isConnect){
            connector.subscribe(topic);
            returnData = connector.getLatestMessage(topic);
        } else {
            returnData = "fail";
        }

        return  returnData;
    }
}
