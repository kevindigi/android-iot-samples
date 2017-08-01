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
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.digicorp.androidiotsamples.Constant;
import com.digicorp.androidiotsamples.R;
import com.digicorp.androidiotsamples.impact_measurement.data.SensorData;
import com.digicorp.data.BaseMeasurementTracker;
import com.digicorp.utils.BLERecordParser;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.jjoe64.graphview.series.DataPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static com.digicorp.androidiotsamples.impact_measurement.MeasurementChartActivity.EXTRA_MEASUREMENT_DATA;
import static com.digicorp.androidiotsamples.impact_measurement.MeasurementChartActivity.EXTRA_MEASUREMENT_DATA_POINTS;
import static com.digicorp.androidiotsamples.impact_measurement.MeasurementChartActivity.EXTRA_SENSOR_DATA;

public class ImpactMeasurementActivity extends AppCompatActivity implements BaseQuickAdapter.OnItemClickListener {

    private static final int RC_COARSE_LOCATION = 1;
    private static final int RC_ENABLE_BT = 2;

    @BindView(R.id.rclvDevices)
    RecyclerView rclvDevices;

    private DeviceRecyclerViewAdapter deviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private BaseMeasurementTracker<String, Entry> measurementTracker = new BaseMeasurementTracker<>();
    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            ScanRecord scanRecord = result.getScanRecord();
            if (scanRecord == null) {
                return;
            }

            String hexScanRecordString = BLERecordParser.convertScanRecordBytesToHexString(scanRecord.getBytes());
            String[] hexParts = hexScanRecordString.split(":");
            int x = Integer.parseInt(hexParts[Constant.X_POSITION], 16);
            int y = Integer.parseInt(hexParts[Constant.Y_POSITION], 16);
            int z = Integer.parseInt(hexParts[Constant.Z_POSITION], 16);
            SensorData data = new SensorData(device.getAddress(), device.getName(), hexScanRecordString, scanRecord.getBytes());
            data.setX(x);
            data.setY(y);
            data.setZ(z);
            measurementTracker.addMeasurement(device.getAddress(), new Entry(x, y));
            deviceListAdapter.addData(data);
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
        deviceListAdapter.setOnItemClickListener(this);
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
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        SensorData data = ((DeviceRecyclerViewAdapter) adapter).getItem(position);
        LinkedHashMap<Long, Entry> measurementData = measurementTracker.getMeasurements(data.getSensorAddress());

        ArrayList<Entry> entries = new ArrayList<>(measurementData.values());
        ArrayList<DataPoint> dataPoints = new ArrayList<>();
        for (Entry entry : entries) {
            dataPoints.add(new DataPoint(entry.getX(), entry.getY()));
        }
        Collections.sort(entries, new EntryXComparator());
        Intent intent = new Intent(this, MeasurementChartActivity.class);
        intent.putExtra(EXTRA_SENSOR_DATA, data);
        intent.putParcelableArrayListExtra(EXTRA_MEASUREMENT_DATA, entries);
        intent.putExtra(EXTRA_MEASUREMENT_DATA_POINTS, dataPoints);
        startActivity(intent);
    }


    private class DeviceRecyclerViewAdapter extends BaseQuickAdapter<SensorData, BaseViewHolder> {

        DeviceRecyclerViewAdapter() {
            super(R.layout.raw_device_item, new ArrayList<SensorData>());
        }

        @Override
        public void addData(@NonNull SensorData data) {
            int index = -1;
            List<SensorData> lstData = getData();
            for (SensorData result : lstData) {
                if (result.getSensorAddress().equalsIgnoreCase(data.getSensorAddress())) {
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
        protected void convert(BaseViewHolder helper, SensorData item) {
            helper.setText(R.id.lblDeviceAddress, item.getSensorAddress());
            helper.setText(R.id.lblDeviceName, item.getSensorName());

            String sb = "\nReading:\n" +
                    item.getHexScanRecord() + "\n\n" +
                    "X: " + item.getX() + "\n" +
                    "Y: " + item.getY() + "\n" +
                    "Z: " + item.getZ() + "\n";

            helper.setText(R.id.lblReading, sb);
        }
    }
}
