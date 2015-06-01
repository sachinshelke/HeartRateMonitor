package eu.vranckaert.heart.rate.monitor.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import eu.vranckaert.heart.rate.monitor.Measurement;
import eu.vranckaert.heart.rate.monitor.view.AbstractViewHolder;
import eu.vranckaert.heart.rate.monitor.view.HearRateHistoryView;
import eu.vranckaert.heart.rate.monitor.view.HeartRateMonitorView;
import eu.vranckaert.heart.rate.monitor.view.HeartRateUnavailableView;
import eu.vranckaert.heart.rate.monitor.view.HeartRateView;
import eu.vranckaert.heart.rate.monitor.view.HeartRateView.HeartRateListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Date: 28/05/15
 * Time: 08:03
 *
 * @author Dirk Vranckaert
 */
public class HearRateActivity extends WearableActivity implements SensorEventListener, HeartRateListener {
    private HeartRateView mView;

    private SensorManager mSensorManager;
    private Sensor mHeartRateSensor;
    private boolean mMeasuring;
    private long mStartTimeMeasurement;
    private boolean mFirstValueFound;
    private List<Float> mMeasuredValues = new ArrayList<>();

    private HeartRateMonitorView mMonitorView;
    private HearRateHistoryView mHistoryView;
    private boolean mInputLocked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        if (getSystemService(Context.SENSOR_SERVICE) != null) {
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//            if (mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE) != null) {
                mHeartRateSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);

                setAmbientEnabled();
                if (mView == null) {
                    mView = new HeartRateView(this, this);
                }
                setContentView(mView.getView());
//            } else {
//                heartRateSensorNotSupported();
//            }
//        } else {
//            heartRateSensorNotSupported();
//        }
    }

    private void heartRateSensorNotSupported() {
        setContentView(new HeartRateUnavailableView(this).getView());
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        Log.d("dirk", "onEnterAmbient");
        Log.d("dirk", "isAmbient=" + isAmbient());
        if (mMeasuring) {
            // TODO notify views to enter ambient mode
        } else {
            finish();
        }

        super.onEnterAmbient(ambientDetails);
    }

    @Override
    public void onExitAmbient() {
        Log.d("dirk", "onExitAmbient");
        Log.d("dirk", "isAmbient=" + isAmbient());
        // TODO notify views to exit ambient mode

        super.onExitAmbient();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d("dirk", "onSensorChanged");
        Log.d("dirk", "event.sensor.getType() = " + event.sensor.getType());
        Log.d("dirk", "event.sensor.getStringType() = " + event.sensor.getStringType());
        Log.d("dirk", "event.sensor.getName() = " + event.sensor.getName());
        Log.d("dirk", "event.sensor.getVendor() = " + event.sensor.getVendor());
        Log.d("dirk", "event.values.length = " + event.values.length);
        for (int i = 0; i < event.values.length; i++) {
            float value = event.values[i];
            Log.d("dirk", "event.values[i] = " + value);
        }

        if (event.values.length > 0) {
            float value = event.values[event.values.length - 1];
            if (mFirstValueFound || value > 0f) {
                mFirstValueFound = true;
                mMeasuredValues.add(value);
                mMonitorView.setMeasuringHeartBeat((int) value);
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d("dirk", "onAccuracyChanged:" + accuracy);
    }

    @Override
    protected void onDestroy() {
        stopHearRateMonitor();
        super.onDestroy();
    }

    private void startHearRateMonitor() {
        mFirstValueFound = false;
        mMeasuredValues.clear();
        if (mSensorManager != null && mHeartRateSensor != null) {
            mSensorManager.registerListener(this, mHeartRateSensor, SensorManager.SENSOR_DELAY_FASTEST);
            mMeasuring = true;
            mStartTimeMeasurement = new Date().getTime();
        }
    }

    private void stopHearRateMonitor() {
        mMeasuring = false;
        if (mSensorManager != null && mHeartRateSensor != null) {
            mSensorManager.unregisterListener(this, mHeartRateSensor);
        }

        if (!mMeasuredValues.isEmpty()) {
            final float averageHeartBeat = calculateAverageHeartBeat();
            Measurement measurement = new Measurement();
            measurement.setAverageHeartBeat(averageHeartBeat);
            measurement.setStartMeasurement(mStartTimeMeasurement);
            measurement.setEndMeasurement(new Date().getTime());
            // TODO store measured data...
        }
    }

    private float calculateAverageHeartBeat() {
        Log.d("dirk", "calculateAverageHeartBeat");
        Log.d("dirk", "mMeasuredValues.size=" + mMeasuredValues.size());

        float sum = 0f;
        for (int i=0; i<mMeasuredValues.size(); i++) {
            float measuredValue = mMeasuredValues.get(i);
            sum += measuredValue;
        }
        float averageHearBeat = sum / mMeasuredValues.size();
        Log.d("dirk", "averageHearBeat=" + averageHearBeat);
        return averageHearBeat;
    }

    @Override
    public void onHearRateViewCreated(AbstractViewHolder view) {
        if (view instanceof HeartRateMonitorView) {
            mMonitorView = (HeartRateMonitorView) view;
        } else if (view instanceof HearRateHistoryView) {
            mHistoryView = (HearRateHistoryView) view;
        }

        if (mMonitorView != null && mHistoryView != null) {
            loadHistoricalData();
        }
    }

    private void loadHistoricalData() {
        // TODO TEMP
        if (!mMeasuredValues.isEmpty()) {
            final float averageHeartBeat = calculateAverageHeartBeat();
            Measurement measurement = new Measurement();
            measurement.setAverageHeartBeat(averageHeartBeat);
            measurement.setStartMeasurement(mStartTimeMeasurement);
            measurement.setEndMeasurement(new Date().getTime());
            mMonitorView.setLatestMeasurement(measurement);
        } else {
            mMonitorView.setLatestMeasurement(null);
        }

        mHistoryView.setMeasurements(null);
    }

    @Override
    public View getBoxInsetReferenceView() {
        return mView.getBoxInsetReferenceView();
    }

    @Override
    public boolean toggleHeartRateMonitor() {
        if (!mInputLocked) {
            mInputLocked = true;
            if (!mMeasuring) {
                startHearRateMonitor();
            } else {
                stopHearRateMonitor();
                loadHistoricalData();
            }
            mInputLocked = false;
        }
        return mMeasuring;
    }
}