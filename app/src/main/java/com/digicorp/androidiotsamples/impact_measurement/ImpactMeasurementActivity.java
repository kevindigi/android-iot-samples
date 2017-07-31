package com.digicorp.androidiotsamples.impact_measurement;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.digicorp.androidiotsamples.Constant;
import com.digicorp.androidiotsamples.R;
import com.digicorp.utils.BLERecordParser;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class ImpactMeasurementActivity extends AppCompatActivity {

    private static final int RC_COARSE_LOCATION = 1;
    private static final int RC_ENABLE_BT = 2;

    @BindView(R.id.rclvDevices)
    RecyclerView rclvDevices;

    private DeviceRecyclerViewAdapter deviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler handler = new Handler();
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            //if (BeaconIdentifier.isBeacon(result.getScanRecord().getBytes())) {
            //Log.d("BeaconFound", result.getDevice().getAddress());
            deviceListAdapter.addData(result);
            //}
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            deviceListAdapter.getData().clear();
            deviceListAdapter.notifyDataSetChanged();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_impact_measurment);
        ButterKnife.bind(this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rclvDevices.setLayoutManager(layoutManager);

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

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
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, RC_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    protected void onResume() {
        super.onResume();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RC_ENABLE_BT);
                return;
            }
        }
        // Initializes list view adapter.
        deviceListAdapter = new DeviceRecyclerViewAdapter();
        rclvDevices.setAdapter(deviceListAdapter);
        scanLeDevice(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        deviceListAdapter.getData().clear();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case RC_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("coarse location permission granted");
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
            case R.id.menu_scan:
                deviceListAdapter.getData().clear();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == RC_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
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
                add(new ScanFilter.Builder().setDeviceAddress("00:A0:50:0E:1A:1C").build());
            }};
            ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
            mBluetoothAdapter.getBluetoothLeScanner().startScan(scanFilters, settings, scanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
            supportInvalidateOptionsMenu();
        }
    }

    private class DeviceRecyclerViewAdapter extends BaseQuickAdapter<ScanResult, BaseViewHolder> {

        DeviceRecyclerViewAdapter() {
            super(R.layout.raw_device_item, new ArrayList<ScanResult>());
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
            helper.setText(R.id.lblDeviceAddress, device.getAddress());
            helper.setText(R.id.lblDeviceName, device.getName() == null
                    || device.getName().length() == 0 ? "Unknown Device" : device.getName());

            ScanRecord scanRecord = item.getScanRecord();
            if (scanRecord == null) {
                return;
            }

            String hexScanRecordString = BLERecordParser.convertScanRecordBytesToHexString(scanRecord.getBytes());
            StringBuilder sb = new StringBuilder("Reading:\n").append(hexScanRecordString)
                    .append("\n\n");
            String[] hexParts = hexScanRecordString.split(":");
            sb.append("X: ").append(Integer.parseInt(hexParts[Constant.X_POSITION], 16)).append("\n");
            sb.append("Y: ").append(Integer.parseInt(hexParts[Constant.Y_POSITION], 16)).append("\n");
            sb.append("Z: ").append(Integer.parseInt(hexParts[Constant.Z_POSITION], 16)).append("\n");

            helper.setText(R.id.lblReading, sb.toString());
        }
    }
}
