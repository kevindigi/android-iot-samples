package com.digicorp.androidiotsamples;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.BaseViewHolder;
import com.digicorp.androidiotsamples.ble_distance.BLEDistanceActivity;
import com.digicorp.androidiotsamples.impact_measurement.ImpactMeasurementActivity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements BaseQuickAdapter.OnItemClickListener {

    @BindView(R.id.rclvSample)
    RecyclerView rclvSample;

    private Map<String, Class> activitySampleMapping;
    private ArrayList<String> mSamples;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        prepareActivitySampleMapping();

        mSamples = new ArrayList<>(activitySampleMapping.keySet());
        SampleAdapter adapter = new SampleAdapter(mSamples);
        rclvSample.setAdapter(adapter);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        rclvSample.setLayoutManager(layoutManager);
        adapter.setOnItemClickListener(this);

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rclvSample.getContext(),
                layoutManager.getOrientation());
        rclvSample.addItemDecoration(dividerItemDecoration);
    }

    private void prepareActivitySampleMapping() {
        activitySampleMapping = new LinkedHashMap<String, Class>() {{
            put("BLE Distance", BLEDistanceActivity.class);
            put("Impact Measurement", ImpactMeasurementActivity.class);
        }};
    }

    @Override
    public void onItemClick(BaseQuickAdapter adapter, View view, int position) {
        Class activityClass = activitySampleMapping.get(mSamples.get(position));
        if (activityClass != null) {
            startActivity(new Intent(this, activityClass));
        }
    }

    private class SampleAdapter extends BaseQuickAdapter<String, BaseViewHolder> {

        SampleAdapter(@Nullable List<String> data) {
            super(R.layout.raw_sample_item, data);
        }

        @Override
        protected void convert(BaseViewHolder helper, String item) {
            helper.setText(R.id.lblSample, item);
        }
    }
}
