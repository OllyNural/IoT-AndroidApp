package ollynural.iot_androidapp;

/**
 *  @Author Oliver Nural - on36@kent.ac.uk
 */

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

class MqttClient {

    // Setup Broker URI and Topic
    private final String BROKER = "tcp://iot.eclipse.org:1883";
    private final String TOPIC  = "on36/iot-beacon";

    // Quality of Service should be 2 - Should send exactly once
    // Slower but more secure and will always get to the server which is what we want
    private final int qos       = 1;

    // Memory persistence for the MqttAsyncClient
    private MemoryPersistence persistence = new MemoryPersistence();

    // Set up as global variable to allow pinging and sending messages
    private MqttAsyncClient mqttAsyncClient = null;

    /**
     * Sets up the connection to the MQTT server and connects
     * Connection is set to keep alive with a duration of every 60 seconds.
     */
    void setUpConnection() {
        System.out.println("Setting up connection to: " + BROKER);
        try {
            // Initialise the global client with uri, clientId and persistence
            mqttAsyncClient = new MqttAsyncClient(BROKER, "", persistence);
            // Add Options of starting clean session and connection keep alive time
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            connectOptions.setKeepAliveInterval(60);
            // Connect using connectOptions to the broker
            mqttAsyncClient.connect(connectOptions);
            System.out.println("Connected to: " + BROKER);
        } catch (MqttException e) {
            System.out.println("Except: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Send message to the MQTT Server
     * Converts rangeStatus to bytes, and passes as message, setting QoS as 2 - Exactly Once
     * Publishes message on TOPIC  with above message
     *
     * @param ANDROID_ID    - Id of the user's device
     * @param mac_address   - Mac address of the iBeacon
     * @param rangeStatus   - Whether the iBeacon is in range or out of range
     */
    void sendMessage(String ANDROID_ID, String mac_address, boolean rangeStatus) {
        System.out.println("~~~Received request to send to MQTT Server:");
        System.out.println("~~~ANDROID_ID: " + ANDROID_ID);
        System.out.println("~~~Mac_addresss: " + mac_address);
        System.out.println("~~~Range Status: " + rangeStatus);

        // Set up data to send
        // Uses StringBuilder as more efficient than String
        StringBuilder content = new StringBuilder("");
        content.append(rangeStatus);

        // Send message as bytes
        MqttMessage mqttMessage = new MqttMessage(content.toString().getBytes());
        // Add QOS
        mqttMessage.setQos(qos);
        try {
            // Publish message on TOPIC
            mqttAsyncClient.publish(TOPIC, mqttMessage);
            System.out.println("Message published! " + "\n" + "Topic: " + TOPIC + "\n" + "Message: " + mqttMessage);
        } catch (MqttException e) {
            System.out.println("Except: " + e);
            e.printStackTrace();
        }
    }

}
