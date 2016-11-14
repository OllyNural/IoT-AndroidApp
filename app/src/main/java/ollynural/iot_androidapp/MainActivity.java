package ollynural.iot_androidapp;

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
//
//    // Stops scanning after 10 seconds
//    private static final long SCAN_PERIOD = 10000;
    // Array list so you can add as many iBeacons as you want. This future proofs the app.
    private static final List<String> MAC_ADDRESS = new ArrayList<>(Collections.singletonList("C5:04:72:47:93:60"));
    // To prevent false positives I use a boolean
    private Boolean repeatCheckBoolean = false;

    // MQTT Client for sending MQTT Messages
    MqttClient mqttClient = new MqttClient();

    // Gets the Unique Android ID for the device the user is running the app
    // Due to context not setting early enough this can't be final and I have to change this during onCreate()
    private String android_id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Creating APP");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Set the Unique Android ID
        android_id = Secure.getString(MainActivity.this.getContentResolver(), Secure.ANDROID_ID);
        // Set up Bluetooth Adapter and Scanner
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        // Start Scanning!
        scanLeDevice();
    }

    private void scanLeDevice() {
        bluetoothLeScanner.startScan(scanCallback);
        System.out.println("Started scanning! for id: " + android_id);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            // If the BLE Device's MAC Address is in our list of our iBeacons
            if (MAC_ADDRESS.contains(result.getDevice().getAddress())) {
                System.out.println("Found iBeacon!");
                if (result.getRssi() > -65) {
                    // Has come in range twice now, so should be correct
                    if (repeatCheckBoolean) {
                        System.out.println("IN RANGE RSSI: " + result.getRssi());
                        mqttClient.sendMessage(android_id, result.getDevice().getAddress(), true);
                    }
                    // Check if just a false positive, as was just out of range
                    else {
                        System.out.println("Was just OUT of range, trying again!");
                        repeatCheckBoolean = true;
                    }
                } else {
                    // Has left range twice now, so should be correct
                    if (!repeatCheckBoolean) {
                        System.out.println("OUT OF RANGE RSSI: " + result.getRssi());
                        mqttClient.sendMessage(android_id, result.getDevice().getAddress(), false);
                    }
                    // Check if just a false positive, as was just in range
                    else {
                        System.out.println("Was just IN of range, trying again!");
                        repeatCheckBoolean = false;
                    }
                }
            }
            super.onScanResult(callbackType, result);
        }
    };

}
