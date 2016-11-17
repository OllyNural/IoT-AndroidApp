package ollynural.iot_androidapp;

/**
 *  @Author Oliver Nural - on36@kent.ac.uk
 */

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

class MqttClient {

    // Setup Broker URI
    // NOTE -----
    // This will change to my own node server running on raptor. This will allow
    // me to add fake user metrics and also add timeouts for edge cases mentioned
    // in the video
    private final String BROKER = "tcp://iot.eclipse.org:1883";

    // Set Quality of Service to 2 - Exactly Once
    private final int qos       = 2;

    // Memory persistence for the MqttAsyncClient
    private MemoryPersistence persistence   = new MemoryPersistence();

    // Set up as global variable to allow pinging and sending messages
    private MqttAsyncClient mqttAsyncClient = null;

    /**
     * Sets up the connection to the MQTT server and connects
     * Connection is default set to keep alive with 60 second intervals - good for this.
     */
    boolean setUpConnection() {
        System.out.println("Setting up connection to: " + BROKER);
        try {
            // Initialise the global client with uri, clientId and persistence
            mqttAsyncClient = new MqttAsyncClient(BROKER, "", persistence);
            // Add Options of starting clean session
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            // Connect using connectOptions to the broker
            mqttAsyncClient.connect(connectOptions);
            System.out.println("Connected to: " + BROKER);
        } catch (MqttException e) {
            e.printStackTrace();
            return false;
        }
        return true;
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
    boolean sendMessage(String ANDROID_ID, String mac_address, boolean rangeStatus) {
        System.out.println("~~~Received request to send to MQTT Server:");
        System.out.println("~~~ANDROID_ID: " + ANDROID_ID);
        System.out.println("~~~Mac_addresss: " + mac_address);
        System.out.println("~~~Range Status: " + rangeStatus);

        // Set up data to put in our message
        StringBuilder topic  = new StringBuilder("on36/iot-beacon/");
        topic.append(mac_address);

        StringBuilder content = new StringBuilder("");
        content.append(ANDROID_ID).append(":").append(rangeStatus);

        // Send message as bytes
        MqttMessage mqttMessage = new MqttMessage(content.toString().getBytes());
        // Add QOS
        mqttMessage.setQos(qos);
        try {
            // Publish message on TOPIC
            mqttAsyncClient.publish(topic.toString(), mqttMessage);
            System.out.println("Message published! " + "\n" + "Topic: " + topic + "\n" + "Message: " + mqttMessage);
        } catch (MqttException e) {
            e.printStackTrace();
            // Attempt to reconnect on a new Thread
            // Horribly horribly bad however it's running on a phone so there is a lot of battery life
            // to play with and we want the transition between connectivity to be seem less.
            // Even so I would rather do it better but I couldn't find a way...
            runInNewThread(this::setUpConnection);
            return true;
        }
        return false;
    }

    /**
     * Starts a new thread and runs a function
     *
     * @param runnable - function to run
     */
    private static void runInNewThread(Runnable runnable){
        new Thread(() -> {
            try {
                runnable.run();
            } catch (Exception e){
                System.err.println(e);
            }
        }).start();
    }

}
