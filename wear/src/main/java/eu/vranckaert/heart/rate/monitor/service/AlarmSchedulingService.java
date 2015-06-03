package eu.vranckaert.heart.rate.monitor.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import eu.vranckaert.hear.rate.monitor.shared.model.ActivityState;
import eu.vranckaert.hear.rate.monitor.shared.model.Measurement;
import eu.vranckaert.heart.rate.monitor.WearHeartRateApplication;
import eu.vranckaert.heart.rate.monitor.WearUserPreferences;
import eu.vranckaert.heart.rate.monitor.controller.HeartRateMonitorIntentService;
import eu.vranckaert.heart.rate.monitor.util.DateUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * Date: 04/02/15
 * Time: 14:23
 *
 * @author Dirk Vranckaert
 */
public class AlarmSchedulingService {
    private static final int REQUEST_CODE_ONE_TIME_HEART_RATE_MEASUREMENT = 123;
    private static final int REQUEST_CODE_REPEATING_HEART_RATE_MEASUREMENT = 321;

    private static AlarmSchedulingService INSTANCE;

    public static AlarmSchedulingService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new AlarmSchedulingService();
        }

        return INSTANCE;
    }

    private static AlarmManager getAlarmManager() {
        return (AlarmManager) WearHeartRateApplication.getInstance().getApplicationContext().getSystemService(
                Context.ALARM_SERVICE);
    }

    private PendingIntent getHeartRateMonitorIntent(int requestCode) {
        Context context = WearHeartRateApplication.getInstance().getApplicationContext();
        Intent intent = new Intent(context, HeartRateMonitorIntentService.class);
        PendingIntent operation = PendingIntent.getService(context, requestCode, intent, 0);
        return operation;
    }

    public void scheduleHeartRateMonitorInXMillis(int delay) {
        Log.d("dirk", "Executing one time heart rate measurement in " + delay + " millis");
        getAlarmManager().cancel(getHeartRateMonitorIntent(REQUEST_CODE_ONE_TIME_HEART_RATE_MEASUREMENT));

        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MILLISECOND, delay);

        Log.i("dirk", "Heart rate monitoring will be started in " + delay + " milliseconds, at " + calendar.getTime().toString());

        getAlarmManager().setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), getHeartRateMonitorIntent(REQUEST_CODE_ONE_TIME_HEART_RATE_MEASUREMENT));
    }

    public void rescheduleHeartRateMeasuringAlarms() {
        Log.d("dirk", "Canceling all one time and repeating heart measurment alarms");
        getAlarmManager().cancel(getHeartRateMonitorIntent(REQUEST_CODE_ONE_TIME_HEART_RATE_MEASUREMENT));
        getAlarmManager().cancel(getHeartRateMonitorIntent(REQUEST_CODE_REPEATING_HEART_RATE_MEASUREMENT));
        
        int activityState = WearUserPreferences.getInstance().getAcceptedActivity();
        long interval = ActivityState.getMeasuringIntervalForActivity(activityState);
        Log.d("dirk", "interval is " + interval + " millis for activityState " + activityState);
        boolean defaultInterval = interval == ActivityState.DEFAULT_MEASURING_INTERVAL;
        Log.d("dirk", "interval is default interval?" + defaultInterval);
        Measurement latestMeasurement = WearUserPreferences.getInstance().getLatestMeasurment();
        long currentTime = new Date().getTime();
        long nextExecution = -1;
        if (latestMeasurement != null) {
            Log.d("dirk", "Previous measurement found (previous execution: " + new Date(latestMeasurement.getStartMeasurement()).toString() + ")");
            if ((latestMeasurement.getStartMeasurement() + interval) <= currentTime) {
                Log.d("dirk", "Next execution will be in past");
                scheduleHeartRateMonitorInXMillis(0);
                nextExecution = latestMeasurement.getStartMeasurement() + (2 * interval);
                while (nextExecution < currentTime) {
                    nextExecution += interval;
                }
                Log.d("dirk", "Next repeating measurement scheduled at " + new Date(nextExecution).toString());
            } else {
                nextExecution = latestMeasurement.getStartMeasurement() + interval;
                Log.d("dirk", "Next repeating measurement scheduled at " + new Date(nextExecution).toString());
            }
        } else {
            nextExecution = currentTime + interval;
            Log.d("dirk", "First repeating measurement scheduled at " + new Date(nextExecution).toString());
        }
        
        if (defaultInterval) {
            Log.d("dirk", "Optimizing measurement start time at quarter of hour...");
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(nextExecution);
            nextExecution = getNextExecutionAtQuarterOfHours(calendar);
            Log.d("dirk", "Measurement optimized to run at " + new Date(nextExecution).toString());
        }
        
        getAlarmManager().setRepeating(AlarmManager.RTC_WAKEUP, nextExecution, interval, getHeartRateMonitorIntent(REQUEST_CODE_REPEATING_HEART_RATE_MEASUREMENT));
    }

    private long getNextExecutionAtQuarterOfHours(Calendar calendar) {
        DateUtil.resetSecondsAndMillis(calendar);
        long now = new Date().getTime();
        while (calendar.get(Calendar.MINUTE) % 15 != 0 || calendar.getTimeInMillis() < now) {
            calendar.add(Calendar.MINUTE, 1);
        }
        
        return calendar.getTimeInMillis();
    }
}
