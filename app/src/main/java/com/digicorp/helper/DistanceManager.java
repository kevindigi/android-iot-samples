package com.digicorp.helper;

import android.support.annotation.NonNull;
import android.util.Log;

import com.digicorp.androidiotsamples.Constant;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * @author kevin.adesara on 06/07/17.
 */

public class DistanceManager {

    public static final int METHOD_AVG = 1;
    public static final int METHOD_WEIGHTED_AVG = 2;
    public static final int METHOD_LAST_FEW_SAMPLES = 3;
    private final HashMap<String, LinkedHashMap<Long, Integer>> beaconRssiSampleMap = new HashMap<>();

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    public DistanceManager() {
    }

    public void addSample(String macAddress, int rssi) {
        synchronized (beaconRssiSampleMap) {
            LinkedHashMap<Long, Integer> samples = beaconRssiSampleMap.get(macAddress);
            if (samples == null) {
                samples = new LinkedHashMap<>();
            }
            samples.put(System.currentTimeMillis(), rssi);
            beaconRssiSampleMap.put(macAddress, samples);
        }
    }

    public double getDistance(String macAddress, int txPower, int method, boolean debugLog) {
        synchronized (beaconRssiSampleMap) {
            if (beaconRssiSampleMap.get(macAddress) == null) {
                return 0;
            }
            LinkedHashMap<Long, Integer> samples = new LinkedHashMap<>(beaconRssiSampleMap.get(macAddress));
            long fromTimestamp = System.currentTimeMillis() - (method == METHOD_LAST_FEW_SAMPLES ?
                    Constant.DISTANCE_FIND_LAST_FEW_SAMPLE_TIME_FRAME_MILLIS : Constant.DISTANCE_FIND_TIME_FRAME_MILLIS);
            long toTimestamp = System.currentTimeMillis();
            long diff = toTimestamp - fromTimestamp;
            //Log.d("Time diff", "" + (diff / 1000));
            LinkedHashMap<Long, Integer> filteredRssi = filterRssiSamplesWithinTimeFrame(samples, fromTimestamp, toTimestamp);
            //Log.d("No. of Samples", ""+filteredRssi.size());

            // Way 1. (most accurate till now)
            LinkedHashMap<Long, Integer> smoothRssi = reduceNoiseFromRSSI(filteredRssi);

            // Way 2. (not accurate at ~10m)
            //double avgOutlierRssi = calculateWeightedAverageOfRssiSamples(filteredRssi);
            //Log.d("OUTLIER", String.format(Locale.US, "Before: %s", filteredRssi.values()));
            //LinkedHashMap<Long, Integer> smoothRssi = removeOutliers(filteredRssi, avgOutlierRssi, BLEDistanceConstants.OUTLIER_CONSTANT);
            //Log.d("OUTLIER", String.format(Locale.US, "After: %s", smoothRssi.values()));

            // Way 3. (not accurate at ~10m)
            // find the rssi having the max. count among other
            //double outlierFinderRssi = calculateOutlierFinderRssi(filteredRssi);
            //Log.d("OUTLIER", String.format(Locale.US, "Before: %s", filteredRssi.values()));
            //Log.d("OUTLIER", "Finder rssi: " + outlierFinderRssi);
            //LinkedHashMap<Long, Integer> smoothRssi = removeOutliers(filteredRssi, outlierFinderRssi, BLEDistanceConstants.OUTLIER_CONSTANT);
            //Log.d("OUTLIER", String.format(Locale.US, "After: %s", smoothRssi.values()));


            double rssi = 0;
            double distance;

            switch (method) {
                case METHOD_AVG:
                    rssi = calculateAverage(smoothRssi);
                    break;
                case METHOD_WEIGHTED_AVG:
                    rssi = calculateWeightedAverageOfRssiSamples(smoothRssi);
                    break;
                case METHOD_LAST_FEW_SAMPLES:
                    if (samples.size() >= Constant.LAST_FEW_SAMPLE_COUNT) {
                        rssi = calculateAverage(smoothRssi);
                    } else {
                        return -1;
                    }
                    break;
            }
            distance = calculateAccuracy(txPower, rssi);
            if (debugLog) {
                logSamples(smoothRssi, fromTimestamp, toTimestamp, rssi, distance);
            }
            return distance;
        }
    }

    private LinkedHashMap<Long, Integer> removeOutliers(LinkedHashMap<Long, Integer> filteredRssi, double avgOutlierRssi, int outlierConstant) {
        LinkedHashMap<Long, Integer> outlierRemoveRssi = new LinkedHashMap<>();

        int minRssi = (int) avgOutlierRssi - outlierConstant;
        int maxRssi = (int) avgOutlierRssi + outlierConstant;

        for (Map.Entry<Long, Integer> entry : filteredRssi.entrySet()) {
            if (entry.getValue() >= minRssi && entry.getValue() <= maxRssi) {
                outlierRemoveRssi.put(entry.getKey(), entry.getValue());
            }
        }

        return outlierRemoveRssi;
    }

    private LinkedHashMap<Long, Integer> reduceNoiseFromRSSI(LinkedHashMap<Long, Integer> filteredRssi) {
        LinkedHashMap<Long, Integer> smoothRssi = new LinkedHashMap<>();

        ArrayList<Long> rssiTimestamps = new ArrayList<>(filteredRssi.keySet());
        ArrayList<Integer> rssiSet = new ArrayList<>(filteredRssi.values());
        int totalRssi = rssiSet.size();
        for (int i = 0; i < totalRssi; i++) {
            int avgRssi;
            Integer nextRssi = rssiSet.get(i + 1 >= totalRssi ? i : i + 1);
            Integer currentRssi = rssiSet.get(i);
            avgRssi = (currentRssi + nextRssi) / 2;
            smoothRssi.put(rssiTimestamps.get(i), avgRssi);
            //Log.e("SMOOTH_RSSI", String.format(Locale.US, "(%d + %d) / 2 = %d", currentRssi, nextRssi, avgRssi));
        }

        return smoothRssi;
    }

    private LinkedHashMap<Long, Integer> filterRssiSamplesWithinTimeFrame(@NonNull LinkedHashMap<Long, Integer> rssiSamples, long fromTimestamp, long toTimestamp) {
        LinkedHashMap<Long, Integer> filteredSamples = new LinkedHashMap<>();
        Set<Long> timestampSet = rssiSamples.keySet();
        for (Long timestamp : timestampSet) {

            if (fromTimestamp == 0) {
                if (timestamp <= toTimestamp) {
                    filteredSamples.put(timestamp, rssiSamples.get(timestamp));
                }
            } else if (timestamp > fromTimestamp && timestamp <= toTimestamp) {
                filteredSamples.put(timestamp, rssiSamples.get(timestamp));
            }
        }
        return filteredSamples;
    }

    private double calculateAverage(@NonNull LinkedHashMap<Long, Integer> filteredRssi) {
        //Simple avg.
        int sum = 0;
        Set<Map.Entry<Long, Integer>> entries = filteredRssi.entrySet();
        for (Map.Entry<Long, Integer> entry : entries) {
            sum += entry.getValue();
        }
        return sum / (entries.size() == 0 ? 1 : entries.size());

    }

    private double calculateWeightedAverageOfRssiSamples(@NonNull LinkedHashMap<Long, Integer> filteredRssi) {
        //1. find count
        HashMap<Integer, Integer> uniqueRssiCountMap = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : filteredRssi.entrySet()) {
            if (uniqueRssiCountMap.containsKey(entry.getValue())) {
                // update count by 1
                uniqueRssiCountMap.put(entry.getValue(), uniqueRssiCountMap.get(entry.getValue()) + 1);
            } else {
                // add count
                uniqueRssiCountMap.put(entry.getValue(), 1);
            }
        }
        //Log.d("RSSI COUNT", "" + uniqueRssiCountMap);

        //2. find weight of each rssi
        HashMap<Integer, Double> uniqueRssiWeightMap = new HashMap<>();
        int totalSamples = filteredRssi.size();
        for (Map.Entry<Integer, Integer> entry : uniqueRssiCountMap.entrySet()) {
            int count = entry.getValue();
            double weight = ((double) count / (double) totalSamples);
            uniqueRssiWeightMap.put(entry.getKey(), weight);
        }
        //Log.d("RSSI WEIGHT", "" + uniqueRssiWeightMap);

        //3. calculate weighted average
        double sum = 0;
        for (Map.Entry<Integer, Double> entry : uniqueRssiWeightMap.entrySet()) {
            sum += entry.getKey() * entry.getValue();
        }

        return sum;
    }

    private double calculateAccuracy(int txPower, double rssi) {
        if (rssi == 0) {
            return -1.0; // if we cannot determine accuracy, return -1.
        }

        double ratio = rssi * 1.0 / txPower;
        if (ratio < 1.0) {
            return Math.pow(ratio, 10);
        } else {
//            return ((0.89976) * Math.pow(ratio, 7.7095)) + 0.111; // iPhone 5
            return ((0.42093) * Math.pow(ratio, 6.9476)) + 0.54992; // Nexus 5
//            return ((0.62093) * Math.pow(ratio, 6.9476)) + 0.54992; // Test 1
//            return ((0.9401940951) * Math.pow(ratio, 6.170094565)) + 0; // Moto X Pro
        }
    }

    private void logSamples(LinkedHashMap<Long, Integer> samples, long fromTimestamp, long toTimestamp, double rssiWeightedAvg, double distance) {
        JSONObject object = new JSONObject();
        JSONArray array = new JSONArray();
        try {
            for (Map.Entry<Long, Integer> entry : samples.entrySet()) {
                JSONObject jsonSample = new JSONObject();
                jsonSample.put("rssi", entry.getValue());
                jsonSample.put("timestamp", getTimeString(entry.getKey()));
                array.put(jsonSample);
            }
            object.put("calc_rssi", rssiWeightedAvg);
            object.put("distance", distance);
            object.put("start_time", getTimeString(fromTimestamp));
            object.put("end_time", getTimeString(toTimestamp));
            object.put("samples", array);
            Log.d("SampleData", object.toString());
        } catch (JSONException ignored) {
        }
    }

    private String getTimeString(long millis) {
        Date d = new Date(millis);
        return simpleDateFormat.format(d);
    }
}
