package ollynural.iot_androidapp;

/**
 * Created by Oliver Nural on 11/14/2016.
 */

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

class MqttClient {

    // Uses StringBuffer over StringBuilder and String as threadsafe and more efficient than String
    private final StringBuffer topic        = new StringBuffer("unikent/users/on36/beaconTopic");
    // Quality of Service should be 2 - Exactly Once
    // Slower but more secure and will always get to the server which is what we want
    private final int qos                   = 1;
//    private final String broker             = "tcp://co657-mqtt.kent.ac.uk:1883";
//    private final String broker             = "tcp://129.12.44.120:1883";
    private final String broker               = "tcp://iot.eclipse.org:1883";

    private MemoryPersistence persistence = new MemoryPersistence();
    // Set up as global variable to allow pinging and sending messages
    private MqttAsyncClient mqttAsyncClient = null;

    void setUpConnection() {
        System.out.println("Setting up connection!");
        try {
            mqttAsyncClient = new MqttAsyncClient(broker, "", persistence);
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            System.out.println("Connecting to broker: " + broker);
            IMqttToken iMqttToken = mqttAsyncClient.connect(connectOptions);
            System.out.println(iMqttToken);
            System.out.println("Connected!");
        } catch (MqttException e) {
            System.out.println("Except: " + e);
            e.printStackTrace();
        }
    }

    void disconnect() {
        try {
            mqttAsyncClient.disconnect();
        } catch (MqttException e) {
            System.out.println("Except: " + e);
            e.printStackTrace();
        }
        System.out.println("Disconnected!");
    }

    void sendMessage(String ANDROID_ID, String mac_address, boolean rangeStatus) {
        System.out.println("~~~Received request to send to MQTT Server:");
        System.out.println("~~~ANDROID_ID: " + ANDROID_ID);
        System.out.println("~~~Mac_addresss: " + mac_address);
        System.out.println("~~~Range Status: " + rangeStatus);

        // Set up sending data
//        topic.append(mac_address);
        StringBuffer content = new StringBuffer("");
        content.append(rangeStatus);
        // Send as bytes
        MqttMessage mqttMessage = new MqttMessage(content.toString().getBytes());
        mqttMessage.setQos(qos);
        try {
            mqttAsyncClient.publish(topic.toString(), mqttMessage);
            System.out.println("Message published! " + topic.toString() + " " + mqttMessage);
        } catch (MqttException e) {
            System.out.println("Except: " + e);
            e.printStackTrace();
        }
    }

}
