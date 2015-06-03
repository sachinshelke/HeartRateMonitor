package eu.vranckaert.hear.rate.monitor.shared.model;

import eu.vranckaert.hear.rate.monitor.shared.DateUtil;

/**
 * Date: 01/06/15
 * Time: 17:18
 *
 * @author Dirk Vranckaert
 */
public class ActivityState {
    public static final long DEFAULT_MEASURING_INTERVAL = DateUtil.convertMinutesToMillis(15);
    public static final long DETECTION_INTERVAL = 10000; // 10 seconds
    public static final int TRUSTED_COUNT = 6; // TRUSTED_COUNT * DETECTION_INTERVAL = time before an activity is accepted. So 6 * 10000 = 60000 millis or 60 seconds or 1 minute before we accept that a user is running/bicycling/driving/still/...

    public static final int IN_VEHICLE = 0;
    public static final int ON_BICYCLE = 1;
    public static final int ON_FOOT = 2;
    public static final int STILL = 3;
    public static final int UNKNOWN = 4;
    public static final int TILTING = 5;
    public static final int WALKING = 7;
    public static final int RUNNING = 8;

    public static long getMeasuringIntervalForActivity(int activity) {
        long interval = DEFAULT_MEASURING_INTERVAL;

        switch (activity) {
            case ActivityState.IN_VEHICLE:
                interval = DateUtil.convertMinutesToMillis(5);
                break;
            case ActivityState.ON_BICYCLE:
                interval = DateUtil.convertMinutesToMillis(1);
                break;
            case ActivityState.RUNNING:
                interval = DateUtil.convertMinutesToMillis(1);
                break;
        }

        return interval;
    }
}
