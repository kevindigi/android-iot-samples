package com.digicorp.androidiotsamples.ble_distance;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.digicorp.androidiotsamples.R;
import com.digicorp.helper.BeaconIdentifier;
import com.digicorp.helper.DistanceManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

import static com.digicorp.androidiotsamples.Constant.TX_POWER;


public class BLEDistanceActivity extends AppCompatActivity {
    private static final String TAG = "tag";
    private static final int RC_PERMISSION_COARSE_LOCATION = 1;
    private static final int RC_ENABLE_BLUETOOTH = 2;
    @BindView(R.id.rclvBeacon)
    RecyclerView rclvBeacon;
    private BeaconRecyclerViewAdapter beaconRecyclerViewAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private DistanceManager distanceManager;

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble_distance);
        ButterKnife.bind(this);

        rclvBeacon.setLayoutManager(new LinearLayoutManager(this));
        distanceManager = new DistanceManager();
        checkAndRequestPermissionsIfRequired();
    }

    private void checkAndRequestPermissionsIfRequired() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Android M Permission check
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    this.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access and bluetooth");
                builder.setMessage("Please grant location access and bluetooth so this app can detect beacons.");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, RC_PERMISSION_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_PERMISSION_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RC_ENABLE_BLUETOOTH);
            }
        }
        // Initializes list view adapter.
        beaconRecyclerViewAdapter = new BeaconRecyclerViewAdapter();
        rclvBeacon.setAdapter(beaconRecyclerViewAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        beaconRecyclerViewAdapter.getData().clear();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == RC_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mScanning = true;

            // Changed by Kevin for the deprecated api call
            ArrayList<ScanFilter> scanFilters = new ArrayList<ScanFilter>() {{
                add(new ScanFilter.Builder().build());
            }};
            /*for (String macAddress : Constants.ALLOWED_MAC_ADDRESSES) {
                ScanFilter filter = new ScanFilter.Builder().setDeviceAddress(macAddress).build();
                scanFilters.add(filter);
            }*/
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    if (result.getScanRecord() == null || result.getScanRecord().getBytes() == null) {
                        return;
                    }

                    if (BeaconIdentifier.isBeacon(result.getScanRecord().getBytes())) {
                        //Log.d("BeaconFound", result.getDevice().getAddress());
                        beaconRecyclerViewAdapter.addData(result);
                        //Log.e("ScanResult", "" + result);
                        distanceManager.addSample(result.getDevice().getAddress(), result.getRssi());
                        beaconRecyclerViewAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    beaconRecyclerViewAdapter.getData().clear();
                    beaconRecyclerViewAdapter.notifyDataSetChanged();
                }
            });
            // **********************************
        } else {
            mScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                }
            });
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
            menu.findItem(R.id.menu_refresh).setVisible(false);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.menu_scan:
                beaconRecyclerViewAdapter.getData().clear();
                scanLeDevice(true);
                supportInvalidateOptionsMenu();
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                supportInvalidateOptionsMenu();
                break;
        }
        return true;
    }

    private class BeaconRecyclerViewAdapter extends BaseQuickAdapter<ScanResult, BaseViewHolder> {
        private StringBuilder builder = new StringBuilder();

        BeaconRecyclerViewAdapter() {
            super(R.layout.raw_ble_distance_item, new ArrayList<ScanResult>());
        }

        @Override
        public void addData(@NonNull ScanResult data) {
            int index = -1;
            List<ScanResult> lstData = getData();
            for (ScanResult result : lstData) {
                if (result.getDevice().getAddress().equalsIgnoreCase(data.getDevice().getAddress())) {
                    index = lstData.indexOf(result);
                    break;
                }
            }
            if (index == -1) {
                super.addData(data);
            } else {
                super.setData(index, data);
            }
        }

        @Override
        protected void convert(BaseViewHolder helper, ScanResult item) {

            BluetoothDevice device = item.getDevice();

            String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {
                deviceName = getString(R.string.unknown_device);
            }
            helper.setText(R.id.lblBLEDeviceName, deviceName);
            helper.setText(R.id.lblBLEDeviceAddress, device.getAddress());
            helper.setText(R.id.lblBLERssi, String.valueOf(item.getRssi()));

            builder.replace(0, builder.length(), "");
            builder.append(String.format(Locale.US, "Method::WeightedAvg: %.2f m\n", distanceManager.getDistance(device.getAddress(), TX_POWER, DistanceManager.METHOD_WEIGHTED_AVG, false)));
            builder.append(String.format(Locale.US, "Method::LastFewSamples: %.2f m\n", distanceManager.getDistance(device.getAddress(), TX_POWER, DistanceManager.METHOD_LAST_FEW_SAMPLES, false)));
            builder.append(String.format(Locale.US, "Method::Avg: %.2f m\n", distanceManager.getDistance(device.getAddress(), TX_POWER, DistanceManager.METHOD_AVG, false)));
            helper.setText(R.id.lblBLEAccuracy, builder.toString());
        }
    }
}