package edu.sjsu.cmpe.mtaas_android;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AuthActivity extends AppCompatActivity {

    private EditText e_email;
    private EditText e_pwd;

    private final static String TAG = "AuthActivity";

    public static final int REQUEST_READ_PHONE_STATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        e_email = (EditText) findViewById(R.id.emailEditTxt);
        e_pwd = (EditText) findViewById(R.id.pwdEditTxt);
    }

    public void login(View v) {
        authenticate("http://mtaas-worker.us-west-2.elasticbeanstalk.com/login");
    }

    public void register(View v) {
        authenticate("http://mtaas-worker.us-west-2.elasticbeanstalk.com/register");
    }

    public void authenticate(String url) {

        RequestQueue queue = Volley.newRequestQueue(this);

        final JSONObject jsonObject = new JSONObject();

        try {
            jsonObject.put("username", e_email.getText().toString());
            jsonObject.put("password", e_pwd.getText().toString());
        } catch (JSONException e) {
            // handle exception
        }

        JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Response: " + response.toString());
                        try {
                            ShareValues.setValue(AuthActivity.this, "owner", response.getString("user_id"));
                            ShareValues.setValue(AuthActivity.this, "token", response.getString("token"));

                            Log.d("owner", ShareValues.getValue(AuthActivity.this, "owner"));
                            Log.d("token", ShareValues.getValue(AuthActivity.this, "token"));

                            Toast.makeText(AuthActivity.this, "Authentication Succeed!", Toast.LENGTH_LONG).show();

                            if (ContextCompat.checkSelfPermission(AuthActivity.this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                                ActivityCompat.requestPermissions(AuthActivity.this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
                            } else {
                                registerDevice();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(AuthActivity.this, "Authentication Failed!", Toast.LENGTH_LONG).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("NOT WORK");
                        Toast.makeText(AuthActivity.this, "Error!", Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                System.out.println("headers");
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };

        queue.add(jsonObjRequest);

    }

    public void registerDevice() {

        new Thread(new Runnable() {
            public void run() {
                RequestQueue queue = Volley.newRequestQueue(AuthActivity.this);

                RequestFuture<JSONObject> future = RequestFuture.newFuture();

                JSONObject device = getDeviceInfo();

                try {

                    // Check if device is already registered
                    String url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/api/v1/device?filter[spec.imei]=" + device.getJSONObject("spec").getString("imei");

                    JsonObjectRequest getDeviceByIMEIRequest = new JsonObjectRequest(Request.Method.GET, url, null, future, future) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            headers.put("Authorization", ShareValues.getValue(AuthActivity.this, "token"));
                            return headers;
                        }
                    };

                    queue.add(getDeviceByIMEIRequest);

                    JSONObject response = future.get(5, TimeUnit.SECONDS);

                    int requestMethod = -1;

                    JSONObject payload = new JSONObject();

                    if (response.getInt("total") == 0) {
                        // register device
                        requestMethod = Request.Method.POST;
                        url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/api/v1/device";
                        payload = device;
                    } else {
                        // update registered device status to online
                        JSONObject registeredDevice = response.getJSONArray("payload").getJSONObject(0);
                        if (registeredDevice.getString("status").equals("offline")) {
                            requestMethod = Request.Method.PUT;
                            url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/api/v1/device/" + registeredDevice.getString("_id");
                            payload.put("status", "online");
                        }
                    }

                    JsonObjectRequest deviceRegisterRequest = new JsonObjectRequest(requestMethod, url, payload, future, future) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            headers.put("Authorization", ShareValues.getValue(AuthActivity.this, "token"));
                            return headers;
                        }
                    };

                    queue.add(deviceRegisterRequest);

                    future.get();

                    Intent intent = new Intent(AuthActivity.this, MainActivity.class);
                    startActivity(intent);

                } catch (InterruptedException | ExecutionException | JSONException | TimeoutException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public JSONObject getDeviceInfo() {

        JSONObject spec = new JSONObject();
        JSONObject device = new JSONObject();

        try {
            TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
            spec.put("imei", telephonyManager.getDeviceId());

            spec.put("brand", Build.BRAND);
            spec.put("model", Build.MODEL);

            spec.put("os_version", Build.VERSION.RELEASE);

            spec.put("cpu", Build.SUPPORTED_ABIS[0]);

            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            spec.put("avail_ram", memInfo.availMem / (1024 * 1024));
            spec.put("total_ram", memInfo.totalMem / (1024 * 1024));

            StatFs InternalStorageStat = new StatFs(Environment.getDataDirectory().getPath());
            spec.put("avail_internal_storage", InternalStorageStat.getFreeBytes() / (1024 * 1024));
            spec.put("total_internal_storage", InternalStorageStat.getTotalBytes() / (1024 * 1024));

            StatFs ExternalStorageStat = new StatFs(Environment.getExternalStorageDirectory().getPath());
            spec.put("avail_disk", ExternalStorageStat.getFreeBytes());
            spec.put("total_disk", ExternalStorageStat.getTotalBytes());

            device.put("spec", spec);
            device.put("status", "online");
            device.put("owner", ShareValues.getValue(AuthActivity.this, "owner"));

        } catch (JSONException e) {
            // handle exception
        }
        return device;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    registerDevice();
                }
                break;
            default:
                break;
        }
    }

    public void printDeviceInfo() {
        String str = "";
        str = "OS Version: " + System.getProperty("os.version"); // OS version
        System.out.println(str);

        str = "Hardware: " + Build.HARDWARE;      // API Level
        System.out.println(str);

        str = "OS Build Version: " + android.os.Build.VERSION.RELEASE + "," + android.os.Build.VERSION.INCREMENTAL + "," + Build.SUPPORTED_ABIS[0]; // OS version
        System.out.println(str);

        str = "Brand: " + Build.BRAND; // OS version
        System.out.println(str);

        str = "Device: " + android.os.Build.DEVICE;           // Device
        System.out.println(str);

        str = "Model: " + android.os.Build.MODEL;            // Model
        System.out.println(str);

        str = "Product: " + android.os.Build.PRODUCT;
        System.out.println(str);


        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        str = "Device Id (IMEI): " + telephonyManager.getDeviceId();
        System.out.println(str);

        ActivityManager actManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
        actManager.getMemoryInfo(memInfo);

        long totalMemory = memInfo.availMem;
        str = "Ram avail Memory: " + memInfo.availMem / (1024 * 1024);
        System.out.println(str);

        str = "Ram total Memory: " + memInfo.totalMem / (1024 * 1024);
        System.out.println(str);

        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long inTotal = stat.getTotalBytes() / (1024 * 1024);
        str = "Total RAM memory:" + inTotal;
        System.out.println(str);

        long inAvail = stat.getFreeBytes() / (1024 * 1024);
        str = "Avail RAM memory:" + inAvail;
        System.out.println(str);


        StatFs stat1 = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesTotal = (long) stat1.getTotalBytes() / (1024 * 1024);
        str = "Total disk memory: " + bytesTotal;
        System.out.println(str);

        long bytesFree = (long) stat1.getFreeBytes() / (1024 * 1024);
        str = "Avail disk memory: " + bytesFree;
        System.out.println(str);

    }
}
