package com.digicorp.data;

import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Created by kevin.adesara on 01/08/17.
 */

public class BaseMeasurementTracker<K, V> {
    /**
     * Measurement data mapped with particular key and timestamp
     */
    private LinkedHashMap<K, LinkedHashMap<Long, V>> measurementMap;


    public BaseMeasurementTracker() {
        this.measurementMap = new LinkedHashMap<>();
    }

    /**
     * Check weather measurements for particular key are recorded or not
     *
     * @param key - key to check
     * @return true if measurements are recorded, false otherwise
     */
    public boolean containsMeasurements(K key) {
        return measurementMap.containsKey(key);
    }

    /**
     * Add measurement for key with timestamp in millis
     *
     * @param key   - key for which value is recorded
     * @param value - value to add with timestamp
     *              <br />
     *              e.g., Recording the bluetooth RSSI reading <br />
     *              Key - Bluetooth address<br/>
     *              Value - RSSI value -54 in dBm
     */
    public void addMeasurement(K key, V value) {
        LinkedHashMap<Long, V> measurements;
        if (containsMeasurements(key)) {
            measurements = measurementMap.get(key);
        } else {
            measurements = new LinkedHashMap<>();
            measurementMap.put(key, measurements);
        }

        measurements.put(new Date().getTime(), value);
    }

    /**
     * Get measurements, if recorded, with timestamp for particular key.
     *
     * @param key - key to get the measurements
     * @return Measurements mapped with timestamp if recorded, null otherwise.
     */
    public LinkedHashMap<Long, V> getMeasurements(K key) {
        return measurementMap.get(key);
    }
}
