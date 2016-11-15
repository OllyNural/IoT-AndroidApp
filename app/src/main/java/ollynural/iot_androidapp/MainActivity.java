package ollynural.iot_androidapp;

/**
 *  @Author Oliver Nural - on36@kent.ac.uk
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothLeScanner bluetoothLeScanner;

    // Array list so you can add as many iBeacons as you want, future proofing the app.
    private static final List<String> MAC_ADDRESS = new ArrayList<>(Collections.singletonList("C5:04:72:47:93:60"));
    // To prevent false positives I use a boolean set each time the status changes
    private boolean repeatCheckBoolean = false;
    // To check if a message has already been sent whilst in range/out of range
    private boolean hasSentInRangeMessage = false;
    private boolean hasSentOutRangeMessage = false;

    // MQTT Client for sending MQTT Messages
    MqttClient mqttClient = new MqttClient();

    // Gets the Unique Android ID for the device the user is running the app
    // Due to application context not setting early enough this can't be final and I have to change this during onCreate()
    private String android_id = "";

    /**
     * On create of app this function will initiate our Mqtt Connection,
     * retrieve android_id from device and start scanning for Bluetooth LE devices
     *
     * This also pings the mqtt server every few seconds to ensure the connection
     * is kept alive as I have told it to
     *
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Setup of App
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set up the connection to the server
        mqttClient.setUpConnection();
        // Set the Unique Android ID
        android_id = Secure.getString(MainActivity.this.getContentResolver(), Secure.ANDROID_ID);
        // Set up Bluetooth Adapter and Scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Start Scanning!WW
        bluetoothLeScanner.startScan(scanCallback);
        System.out.println("Started scanning! for id: " + android_id);
    }


    /**
     * Callback for the Bluetooth LE Scanner
     * Checks if beacon is in list of our iBeacons, in or out of range, in the correct state and
     * if we have already sent a message for this state. If all criteria met, this will call
     * MqttClient.sendMessage to send a message to the MQTT server.
     *
     * repeatCheckBoolean states are as follows:
     * when equal to false this means the last reading was 'out of range'
     * when equal to true, this means the last reading was 'in range'
     */
    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // If the BLE Device's MAC Address is in our list of our iBeacons
            if (MAC_ADDRESS.contains(result.getDevice().getAddress())) {
                System.out.println("Found iBeacon!");
                if (result.getRssi() > -60) {
                    // Check if false positive or if it has been  'in range' twice now
                    if (repeatCheckBoolean) {
                        // Has come in range twice now, so should be correct
                        System.out.println("IN RANGE RSSI: " + result.getRssi());
                        // Check if a message has already been sent for in range since being in range
                        if (!hasSentInRangeMessage) {
                            mqttClient.sendMessage(android_id, result.getDevice().getAddress(), true);
                            // Set sent in range to true and reset out of range to false
                            hasSentInRangeMessage = true;
                            hasSentOutRangeMessage = false;
                        }
                    }
                    // Check if it is a false 'in range' positive, as was just out of range
                    else {
                        repeatCheckBoolean = true;
                    }
                } else {
                    // Check if false positive or if it has been  'out of range' twice now
                    if (!repeatCheckBoolean) {
                        // Has been out of range twice now, so should be correct
                        System.out.println("OUT OF RANGE RSSI: " + result.getRssi());
                        // Check if a message has already been sent for out of range since being out of range
                        if (!hasSentOutRangeMessage) {
                            mqttClient.sendMessage(android_id, result.getDevice().getAddress(), false);
                            // Set out of range to true and reset in range to false;
                            hasSentOutRangeMessage = true;
                            hasSentInRangeMessage = false;
                        }
                    }
                    // Check if it is a false 'out of range' positive, as was just in range
                    else {
                        repeatCheckBoolean = false;
                    }
                }
            }
            super.onScanResult(callbackType, result);
        }
    };

}
