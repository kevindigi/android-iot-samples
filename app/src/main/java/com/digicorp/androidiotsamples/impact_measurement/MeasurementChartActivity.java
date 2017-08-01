package com.digicorp.androidiotsamples.impact_measurement;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.digicorp.androidiotsamples.R;
import com.digicorp.androidiotsamples.impact_measurement.data.SensorData;
import com.digicorp.widgets.TextView;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MeasurementChartActivity extends AppCompatActivity {

    public static final String EXTRA_SENSOR_DATA = "sensor_data";
    public static final String EXTRA_MEASUREMENT_DATA = "measurement_data";
    public static final String EXTRA_MEASUREMENT_DATA_POINTS = "measurement_data_points";

    @BindView(R.id.lblDeviceName)
    TextView lblDeviceName;

    @BindView(R.id.lblDeviceAddress)
    TextView lblDeviceAddress;

    @BindView(R.id.lcMeasurementChart)
    LineChart lcMeasurementChart;

    @BindView(R.id.graph)
    GraphView graph;

    private SensorData sensorData;
    private ArrayList<Entry> measurementData = new ArrayList<>();
    private ArrayList<DataPoint> measurementDataPoints = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measurement_chart);
        ButterKnife.bind(this);

        lcMeasurementChart.enableScroll();
        lcMeasurementChart.setPinchZoom(true);

        // activate horizontal zooming and scrolling
        graph.getViewport().setScalable(true);

        // activate horizontal scrolling
        graph.getViewport().setScrollable(true);

        // activate horizontal and vertical zooming and scrolling
        graph.getViewport().setScalableY(true);

        // activate vertical scrolling
        graph.getViewport().setScrollableY(true);

        graph.getLegendRenderer().setVisible(true);
        graph.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);

        Bundle bundle = getIntent().getExtras();
        if (bundle == null || !bundle.containsKey(EXTRA_SENSOR_DATA) || !bundle.containsKey(EXTRA_MEASUREMENT_DATA)) {
            Toast.makeText(this, "Sensor and/or measurement data not found.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        extractSensorAndMeasurementData(bundle);
        drawChart();
    }

    private void extractSensorAndMeasurementData(Bundle bundle) {
        sensorData = bundle.getParcelable(EXTRA_SENSOR_DATA);
        if (sensorData != null) {
            lblDeviceAddress.setText(sensorData.getSensorAddress());
            lblDeviceName.setText(sensorData.getSensorName());
        }

        if (bundle.get(EXTRA_MEASUREMENT_DATA) instanceof ArrayList)
            measurementData = bundle.getParcelableArrayList(EXTRA_MEASUREMENT_DATA);

        if (measurementData == null) {
            measurementData = new ArrayList<>();
        }

        if (bundle.get(EXTRA_MEASUREMENT_DATA_POINTS) instanceof ArrayList) {
            measurementDataPoints = (ArrayList<DataPoint>) bundle.get(EXTRA_MEASUREMENT_DATA_POINTS);
        }
        if (measurementDataPoints == null) {
            measurementDataPoints = new ArrayList<>();
        }
    }

    private void drawChart() {
        // MP Line Chart
        lcMeasurementChart.clear();
        LineDataSet dataSet = new LineDataSet(measurementData, sensorData.getSensorName()); // add entries to dataset
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.RED); // styling, ...

        LineData lineData = new LineData(dataSet);
        lcMeasurementChart.setData(lineData);
        lcMeasurementChart.invalidate(); // refresh


        // GraphView chart
        LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(measurementDataPoints.toArray(new DataPoint[]{}));
        series.setTitle(sensorData.getSensorName());
        graph.addSeries(series);

    }
}
