package edu.sjsu.cmpe.mtaas_android;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by zheng on 9/23/16.
 */
public class ShareValues {
    public static final String PREFS_NAME = "PrefsFile";

    public static SharedPreferences settings;

    public static void init(Context context) {
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static void setValue(String key, String value) {
        SharedPreferences.Editor prefEditor = ShareValues.settings.edit();
        prefEditor.putString(key, value);
        //prefEditor.apply();
        prefEditor.commit();
    }

    public static String getValue(String key, String value) {
        return ShareValues.settings.getString("ngrok_url", "null");
    }
}
