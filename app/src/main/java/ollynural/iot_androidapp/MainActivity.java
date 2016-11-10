package ollynural.iot_androidapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.le.BluetoothLeScanner;

public class MainActivity extends AppCompatActivity {

    // Stops scanning after 10 seconds
    private static final long SCAN_PERIOD = 10000;
    private static final String MAC_ADDRESS = "C5:04:72:47:93:60";

    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private BluetoothLeScanner bluetoothLeScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        System.out.println("Creating app!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
        scanLeDevice(true);
    }

    private void scanLeDevice(final boolean enable) {
        bluetoothLeScanner.startScan(scanCallback);
        System.out.println("Scanning!");
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            if (result.getDevice().getAddress().equals(MAC_ADDRESS)) {
                System.out.println("Found iBeacon!");
                if (result.getRssi() > -60) {
                    System.out.println("IN RANGE RSSI: " + result.getRssi());
                } else {
                    System.out.println("OUT OF RANGE RSSI: " + result.getRssi());
                }
            }
            super.onScanResult(callbackType, result);
        }
    };
}
