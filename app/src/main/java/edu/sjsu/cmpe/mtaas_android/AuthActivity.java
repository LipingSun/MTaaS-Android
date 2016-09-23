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
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private TextView txtView;
    private EditText e_email;
    private EditText e_pwd;
    private String email="";
    private String pwd="";

    private final static String TAG = "AuthActivity";
    private final static JSONObject device=new JSONObject();



    // public static final String PREFS_NAME = "TokenPrefsFile";
    public static String token = "null";
    public static String owner = "null";
   // private static SharedPreferences settings;

    public static final int REQUEST_READ_PHONE_STATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        e_email = (EditText) findViewById(R.id.emailEditTxt);
        e_pwd = (EditText) findViewById(R.id.pwdEditTxt);

        System.out.println("start");

        ShareValues.init(AuthActivity.this);
    }

    /*public void init(Context context) {
        settings = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }*/

    public void connect() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            getInfos();
        }

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/api/v1/device";

        Log.d("owner", ShareValues.getValue("owner","null"));
        Log.d("token", ShareValues.getValue("token","null"));
        Log.d("owner", owner);
        Log.d("token", token);

        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST, url, device,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Response: " + response.toString());

                        Toast.makeText(AuthActivity.this, "connected to server", Toast.LENGTH_LONG).show();

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

                headers.put("Authorization", token);

                return headers;
            }
        };
        queue.add(jsObjRequest);



    }

    public void login(View view) {


        Intent intent = new Intent(this, MainActivity.class);

        Log.d(TAG, "Login");

        if (validate(e_email.getText().toString(), e_pwd.getText().toString())) {
            startActivity(intent);
        }
    }

    public void register(View view) {

       // callAPI()

        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/register";

        boolean flag=false;

        final JSONObject jsonObject = new JSONObject();
        try {
            Log.d(TAG, "Register");
            jsonObject.put("username", e_email.getText().toString());
            jsonObject.put("password", e_pwd.getText().toString());
        } catch (JSONException e) {
            // handle exception
        }


        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Response: " + response.toString());
                        Toast.makeText(AuthActivity.this, "Register successfully, Please use this account to login!", Toast.LENGTH_LONG).show();

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("NOT WORK");
                        Toast.makeText(AuthActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                System.out.println("headers");
                headers.put("Content-Type", "application/json");
                return headers;
            }
        };
        queue.add(jsObjRequest);
    }



    public boolean validate( String email, String pwd) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);

        String url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/login";

        boolean flag=false;

        final JSONObject jsonObject = new JSONObject();
        try {
            Log.d(TAG, "Auth");
            jsonObject.put("username", email);
            jsonObject.put("password", pwd);
        } catch (JSONException e) {
            // handle exception
        }


        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST, url, jsonObject,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Response: " + response.toString());
                        try {
                                token=response.getString("token");
                                Log.d(TAG, token);
                                owner=response.getString("user_id");
                                ShareValues.setValue("owner", owner);
                                ShareValues.setValue("token", token);

                                Log.d("owner", ShareValues.getValue("owner","null"));
                                Log.d("token", ShareValues.getValue("token","null"));


                            connect();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(AuthActivity.this, "There is some error here", Toast.LENGTH_LONG).show();
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
        queue.add(jsObjRequest);
        return true;
    }


    public void getInfos() {
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

        final JSONObject spec=new JSONObject();
        //final JSONObject device=new JSONObject();

        try {
            Log.d(TAG, "Specs");
            spec.put("imei", telephonyManager.getDeviceId());
            spec.put("brand", Build.BRAND);
            spec.put("model", Build.MODEL);
            spec.put("os_version",android.os.Build.VERSION.RELEASE);
            spec.put("cpu", Build.SUPPORTED_ABIS[0]);
            spec.put("avail_ram", memInfo.availMem / (1024 * 1024));
            spec.put("total_ram", memInfo.totalMem / (1024 * 1024));
            spec.put("avail_internal_storage", stat.getFreeBytes() / (1024 * 1024));
            spec.put("total_internal_storage", stat.getTotalBytes() / (1024 * 1024));
            spec.put("avail_disk", stat1.getFreeBytes());
            spec.put("total_disk", stat1.getTotalBytes() );

            device.put("spec",spec);
           // device.put("owner",ShareValues.getValue("owner","null"));
            device.put("owner",owner);
            device.put("status","online");

        } catch (JSONException e) {
            // handle exception
        }
        //return device;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_READ_PHONE_STATE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    getInfos();
                }
                break;

            default:
                break;
        }
    }
}
