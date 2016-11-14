package ollynural.iot_androidapp;

/**
 * Created by Oliver Nural on 11/14/2016.
 */

class MqttClient {

    void sendMessage(String ANDROID_ID, String mac_address, boolean rangeStatus) {
        System.out.println("~~~Received request to send to MQTT Server:");
        System.out.println("~~~ANDROID_ID: " + ANDROID_ID);
        System.out.println("~~~Mac_addresss: " + mac_address);
        System.out.println("~~~Range Status: " + rangeStatus);
    }


}
