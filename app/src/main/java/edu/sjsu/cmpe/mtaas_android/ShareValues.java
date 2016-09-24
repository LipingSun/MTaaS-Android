package edu.sjsu.cmpe.mtaas_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Created by zheng on 9/23/16.
 */
public class ShareValues {

    private final static String TAG = "ShareValues";

    public static final String PREFS_NAME = "PrefsFile";

    public static void setValue(Context context, String key, String value) {
        SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = settings.edit();
        prefEditor.putString(key, value);
        prefEditor.apply();
        Log.d(TAG, key + ':' + value);
    }

    public static String getValue(Context context, String key) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(key, "null");
    }
}
