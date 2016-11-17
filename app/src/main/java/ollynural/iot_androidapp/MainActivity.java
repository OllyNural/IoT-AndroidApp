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
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private BluetoothLeScanner bluetoothLeScanner;

    // Array list so you can add as many iBeacons as you want, future proofing the app.
    private static final List<String> MAC_ADDRESS = new ArrayList<>(Collections.singletonList("C5:04:72:47:93:60"));
    // To prevent false positives I use a boolean set each time the status changes ignore single anomalous result
    private boolean repeatCheckBoolean = false;
    // To check if a message has already been sent whilst in range/out of range
    private boolean hasSentInRangeMessage = false;
    private boolean hasSentOutRangeMessage = false;

    // Dirty way of stopping the user from pressing the startScan button more than once during a scan
    // True if the user presses startScan and False if they press stopScan
    private boolean isCurrentlyScanning;

    // MQTT Client for sending MQTT Messages
    private MqttClient mqttClient = new MqttClient();

    // Gets the Unique Android ID for the device the user is running the app
    // Due to application context not setting early enough this can't be final and I have to change this during onCreate()
    private String android_id = "";

    // Get the TextView for the text on the screen
    private TextView textView;
    private TextView textConnectionView;

    /**
     * On create of app this function will initiate our Mqtt Connection,
     * retrieve android_id from device and start scanning for Bluetooth LE devices
     *
     * If connection cannot be made, shows message to user, and continues.
     *
     * @param savedInstanceState - Saved Instance State
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set up our content view and textViews
        setContentView(R.layout.activity_main);
        textView = (TextView)findViewById(R.id.textViewMain);
        textConnectionView = (TextView)findViewById(R.id.textConnectionView);
        // Set up the connection to the server and check if working
        boolean conn = mqttClient.setUpConnection();
        if (!conn) {
            changeConnectionText("Cannot connect to server.");
        }
        // Get and set the Unique Android ID
        android_id = Secure.getString(MainActivity.this.getContentResolver(), Secure.ANDROID_ID);
        // Set up Bluetooth Adapter and Scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Setup listeners for the start and stop scanning buttons
        setUpListeners();
    }

    /**
     * Sets up Listeners for the two buttons on the UI - buttonStart and buttonStop
     * These call BluetoothLeScanner#startScan and #stopScan respectively
     * Callbacks are below
     */
    public void setUpListeners() {
        // Grab both buttons and set up listeners
        Button startButton = (Button) findViewById(R.id.startButton);
        Button stopButton = (Button) findViewById(R.id.stopButton);

        startButton.setOnClickListener(v -> {
            //Start Scanning if we're not already doing so, and set bool to true!
            if(!isCurrentlyScanning) {
                isCurrentlyScanning = true;
                textView.setText("Scanning...");
                bluetoothLeScanner.startScan(scanCallback);
            }
        });

        stopButton.setOnClickListener(v -> {
            // Stop Scanning - Set boolean checker to false to allow pressing button again
            if (isCurrentlyScanning) {
                // Reset the in and out range boolean checkers
                hasSentInRangeMessage = false;
                hasSentOutRangeMessage = false;
                bluetoothLeScanner.stopScan(scanCallback);
                isCurrentlyScanning = false;
                textView.setText("Not Scanning");
            }
        });
    }

    /**
     * Behemoth Callback for the Bluetooth LE Scanner
     * Checks if beacon is in list of our iBeacons, in or out of range, in the correct state and
     * if we have already sent a message for this state. If all criteria met, this will call
     * MqttClient.sendMessage to send a message to the MQTT server.
     *
     * Also chances the text on the screen to show whether a beacon is in range or out of range
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

                // -60 is boundary cut-off for in and out of range
                if (result.getRssi() > -60) {
                    // ~~~ IN RANGE ~~~

                    // Check if false positive or if it has been  'in range' twice now
                    if (repeatCheckBoolean) {
                        // Has come in range twice now, so should be correct
                        System.out.println("IN RANGE RSSI: " + result.getRssi());

                        // Check if a message has already been sent for in range since being in range
                        if (!hasSentInRangeMessage) {
                            changeText("In Range of an iBeacon!");
                            boolean err = mqttClient.sendMessage(android_id, result.getDevice().getAddress(), true);
                            // If the message doesn't send then don't set the sentInRangeMessage variable to true
                            // This allows it to send the message next time it gets in r    ange
                            if (err) {
                                changeConnectionText("Cannot connect to server.");
                                hasSentInRangeMessage = false;
                            } else {
                                changeConnectionText(" ");
                                hasSentInRangeMessage = true;
                            }
                            hasSentOutRangeMessage = false;
                        }

                    }
                    // Check if it is a false 'in range' positive, as was just out of range
                    else {
                        repeatCheckBoolean = true;
                    }
                } else {
                    // ~~~ OUT OF RANGE ~~~

                    // Check if false positive or if it has been  'out of range' twice now
                    if (!repeatCheckBoolean) {
                        // Has been out of range twice now, so should be correct
                        System.out.println("OUT OF RANGE RSSI: " + result.getRssi());

                        // Check if a message has already been sent for out of range since being out of range
                        if (!hasSentOutRangeMessage) {
                            changeText("Continue shopping!");
                            boolean err = mqttClient.sendMessage(android_id, result.getDevice().getAddress(), false);
                            // If the message doesn't send then don't set the sentInRangeMessage variable to true
                            // This allows it to send the message next time it gets in range
                            if (err) {
                                changeConnectionText("Cannot connect to server.");
                                hasSentOutRangeMessage = false;
                            } else {
                                changeConnectionText(" ");
                                hasSentOutRangeMessage = true;
                            }
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

    private void changeText(String text) {
        textView.setText(text);
    }

    private void changeConnectionText(String text) {
        textConnectionView.setText(text);
    }
}
