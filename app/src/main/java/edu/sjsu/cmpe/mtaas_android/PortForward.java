package edu.sjsu.cmpe.mtaas_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.*;

public class PortForward {

    private final static String TAG = "PortForward";

    //public static final String PREFS_NAME = "PrefsFile";

   // private static SharedPreferences settings;

    public static String ngrokURL = "null";

    private static Runtime runtime;

    public static boolean init(Context context) {
       // settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        ShareValues.init(context);
        File ngrokFile = new File(context.getFilesDir(), "ngrok");
        if (!ngrokFile.exists()) {
            InputStream ngrokInputStream = null;
            try {
                ngrokInputStream = context.getAssets().open("ngrok");
                OutputStream ngrokOutputStream = context.openFileOutput(ngrokFile.getName(), Context.MODE_PRIVATE);
                byte[] buffer = new byte[4096];
                int length;
                while ((length = ngrokInputStream.read(buffer)) > 0) {
                    ngrokOutputStream.write(buffer, 0, length);
                }
                ngrokInputStream.close();
                ngrokOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static boolean setConnection(Context context, boolean control) {
        if (control) {
            try {
                return connect(context);
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        } else {
            return disconnect();
        }
    }

    public static boolean connect(Context context) throws IOException {
        //if (isConnected()) {
        //    return true;
        //}
        init(context);
        String ngrokPath = context.getFilesDir().getAbsolutePath() + "/ngrok";
        runtime = Runtime.getRuntime();
        Process process = runtime.exec("su");
        InputStream stdout = process.getInputStream();
        DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
        stdin.writeBytes("chmod 777 " + ngrokPath + "\n");
        stdin.writeBytes("HOME=" + context.getFilesDir().getAbsolutePath() + "\n");
        stdin.writeBytes("." + ngrokPath + " authtoken 6KHrji6yCPTL5axiYrDEx_jm3AEJBzvz4sE4uSnFmT" + "\n");
        stdin.writeBytes("." + ngrokPath + " tcp -log-level debug -log stdout 5555" + "\n");
        // TODO: cleanup stream

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            Log.d(TAG, line);
            if (line.contains("URL:tcp://")) {
                ngrokURL = line.substring(line.indexOf("URL:tcp://") + 10, line.indexOf(" Proto:tcp"));
                Log.d(TAG, ngrokURL);
                SharedPreferences.Editor prefEditor = ShareValues.settings.edit();
                prefEditor.putString("ngrok_url", ngrokURL);
                prefEditor.apply();
                return true;
            } else if (line.contains("limited to 1 simultaneous ngrok client session")) {
                ngrokURL = ShareValues.settings.getString("ngrok_url", "null");
                return true;
            }
        }
        return false;
    }

    public static boolean disconnect() {
        //TODO
        //runtime.halt(0);
        //Log.d(TAG, "runtime exit");
        return true;
    }

    private static boolean isConnected() {
        //TODO
        Log.d(TAG, "Enter isConnected()");
        try {
            Process process = Runtime.getRuntime().exec("su");
            InputStream stdout = process.getInputStream();
            DataOutputStream stdin = new DataOutputStream(process.getOutputStream());
            stdin.writeBytes("pgrep ngrok");
            //Process process = Runtime.getRuntime().exec("ps a");
            //Process process = Runtime.getRuntime().exec("whoami");
            //InputStream stdout = process.getInputStream();
            //LineNumberReader lineNumberReader = new LineNumberReader(new InputStreamReader(process.getInputStream()));
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stdout));
            String line;
            //int lineCount = 0;
            //line = bufferedReader.readLine();
            //if (line != null) {
            //    Log.d(TAG + "line", line);
            //} else {
            //    Log.d(TAG + "line", "line is null");
            //}
            //line = bufferedReader.readLine();
            //Log.d(TAG, line);
            //line = lineNumberReader.readLine();
            Log.d(TAG, "Before read");

            while ((line = bufferedReader.readLine()) != null) {
                //lineCount++;
                Log.d(TAG, line);
                //line = lineNumberReader.readLine();
                //if (lineCount >= 2) {
                //    return true;
                //}
            }
            Log.d(TAG, "out " + line);
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG + "line", "err");
        }
        return false;
    }
}
