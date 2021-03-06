package com.voipgrid.vialer.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.telephony.TelephonyManager;

import com.voipgrid.vialer.Preferences;
import com.voipgrid.vialer.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to check connectivity of the device.
 */
public class ConnectivityHelper {
    public enum Connection {
        NO_CONNECTION("Unknown", -1),
        SLOW("Slow", 0),
        WIFI("Wifi", 1),
        LTE("4G", 2),
        HSDPA("HSDPA", 3),     // ~ 2-14 Mbps
        HSPAP("HSPAP", 4),     // ~ 10-20 Mbps
        HSUPA("HSUPA", 5),     // ~ 1-23 Mbps
        EVDO_B("EVDO_B", 6);   // ~ 5 Mbps

        String stringValue;
        int intValue;

        Connection(String string, int intValue) {
            stringValue = string;
            this.intValue = intValue;
        }

        public int toInt() {
            return intValue;
        }

        @Override
        public String toString() {
            return stringValue;
        }
    }

    private final ConnectivityManager mConnectivityManager;
    private final TelephonyManager mTelephonyManager;
    public static boolean mWifiKilled = false;

    private static final List<Connection> sFastDataTypes = new ArrayList<>();
    private static final List<Connection> sFast3GDataTypes = new ArrayList<>();

    private static Context mContext;

    static {
        sFastDataTypes.add(Connection.WIFI);
        sFastDataTypes.add(Connection.LTE);
    }

    static {
        sFast3GDataTypes.add(Connection.HSDPA);
        sFast3GDataTypes.add(Connection.HSPAP);
        sFast3GDataTypes.add(Connection.HSUPA);
        sFast3GDataTypes.add(Connection.EVDO_B);
    }

    /**
     * Constructor.
     * @param connectivityManager
     * @param telephonyManager
     */
    public ConnectivityHelper(ConnectivityManager connectivityManager, TelephonyManager telephonyManager) {
        mConnectivityManager = connectivityManager;
        mTelephonyManager = telephonyManager;
    }

    /**
     * Check the device current connectivity state based on the active network.
     * @return
     */
    public boolean hasNetworkConnection() {
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    /**
     * Get the current connection type.
     * @return Long representation of the connection type.
     */
    public Connection getConnectionType() {
        NetworkInfo info = mConnectivityManager.getActiveNetworkInfo();
        if (info == null || !info.isConnected()) {
            return Connection.NO_CONNECTION;
        }

        // We need to check 2 methods for the type because they both can give a different
        // value for the same information we are checking.
        // Get network type from ConnectivityManager.
        int networkTypeConnection = info.getSubtype();
        // Get network type from TelephonyManager.
        int networkTypeTelephony = mTelephonyManager.getNetworkType();

        if (info.getType() == ConnectivityManager.TYPE_WIFI) {
            return Connection.WIFI;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_LTE || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_LTE) {
            return Connection.LTE;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSDPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSDPA) {
            return Connection.HSDPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSPAP || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSPAP) {
            return Connection.HSPAP;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_HSUPA || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_HSUPA) {
            return Connection.HSUPA;
        } else if (networkTypeConnection == TelephonyManager.NETWORK_TYPE_EVDO_B || networkTypeTelephony == TelephonyManager.NETWORK_TYPE_EVDO_B) {
            return Connection.EVDO_B;
        }
        return Connection.SLOW;
    }

    /**
     * Get the connection type as string. This is mainly used for the GA tracking.
     *
     * @return String representation of the connection type.
     */
    public String getConnectionTypeString() {
        String connectionString;

        switch (getConnectionType()) {
            case WIFI:
                connectionString = Connection.WIFI.toString();
                break;
            case LTE:
                connectionString = Connection.LTE.toString();
                break;
            case HSDPA:
                connectionString = Connection.HSDPA.toString();
                break;
            case HSPAP:
                connectionString = Connection.HSPAP.toString();
                break;
            case HSUPA:
                connectionString = Connection.HSUPA.toString();
                break;
            case EVDO_B:
                connectionString = Connection.EVDO_B.toString();
                break;
            default:
                connectionString = Connection.NO_CONNECTION.toString();
                break;
        }

        return connectionString;
    }

    public String getAnalyticsLabel() {
        String analyticsLabel;
        switch (getConnectionType()) {
            case WIFI:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_wifi);
                break;
            case LTE:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_4g);
                break;
            case HSDPA:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hsdpa);
                break;
            case HSPAP:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hspap);
                break;
            case HSUPA:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_hsupa);
                break;
            case EVDO_B:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_evdo_b);
                break;
            default:
                analyticsLabel = mContext.getString(R.string.analytics_event_label_unknown);
                break;
        }
        return analyticsLabel;
    }

    /**
     * Check if the device is connected via wifi or LTE connection.
     * @return
     */
    public boolean hasFastData() {
        Preferences pref = new Preferences(mContext);
        Connection connectionType = getConnectionType();
        return sFastDataTypes.contains(connectionType) || (sFast3GDataTypes.contains(connectionType) && pref.has3GEnabled());
    }

    public static ConnectivityHelper get(Context context) {
        ConnectivityManager c = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        TelephonyManager t = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        mContext = context;
        return new ConnectivityHelper(c, t);
    }

    public void useWifi(Context context, boolean useWifi) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(useWifi);
    }

    /**
     * Sets a timer that runs until the timeout limit is reached. At each interval it
     * will check whether or not we succeeded in enabling LTE. If it did not succeed then
     * we will try to get Wifi on again.
     */
    public void waitForLTE(final Context context, int timeout, final int interval) {
        final int remainingTime = timeout - interval;
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Keep waiting until the remaining time is less then the interval.
                if(remainingTime > interval) {
                    waitForLTE(context, remainingTime, interval);
                } else if(getConnectionType() != Connection.LTE) {
                    // Turn wifi back on if we don't succeed in connecting with LTE before the timeout.
                    useWifi(context, true);
                }
            }
        }, interval);
    }

    public void attemptUsingLTE(final Context context, int timeout) {
        if (getConnectionType() == Connection.WIFI) {
            useWifi(context, false);
            mWifiKilled = true;
            waitForLTE(context, timeout+(timeout/10), timeout/10);
        }
    }

    /**
     * One way conversion to charsequence from preference (long) because bidirectional maps
     * are not nativly supported in java.
     */
    public static long converseToPreference(CharSequence connectionPreference, Context context) {
        if (connectionPreference.equals(context.getString(R.string.call_connection_only_cellular))) {
            return Preferences.CONNECTION_PREFERENCE_LTE;
        } else if (connectionPreference.equals(context.getString(R.string.call_connection_use_wifi_cellular))) {
            return Preferences.CONNECTION_PREFERENCE_WIFI;
        }
        return Preferences.CONNECTION_PREFERENCE_NONE;
    }

    /**
     * One way conversion to preference (long) from charsequence because bidirectional maps
     * are not nativly supported in java.
     */
    public static CharSequence converseFromPreference(long preference, Context context) {
        if (preference == Preferences.CONNECTION_PREFERENCE_LTE) {
            return context.getString(R.string.call_connection_only_cellular);
        } else if (preference == Preferences.CONNECTION_PREFERENCE_WIFI) {
            return context.getString(R.string.call_connection_use_wifi_cellular);
        }
        return context.getString(R.string.call_connection_optional);
    }
}
