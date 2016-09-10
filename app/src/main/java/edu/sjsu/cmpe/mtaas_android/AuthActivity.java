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
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class AuthActivity extends AppCompatActivity {

    private TextView txtView;
    private EditText e_email;
    private EditText e_pwd;
    private String email;
    private String pwd;

    public static final int REQUEST_READ_PHONE_STATE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        System.out.println("start");
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_PHONE_STATE}, REQUEST_READ_PHONE_STATE);
        } else {
            getInfos();
        }
    }

    public void login(View view) {

        Intent intent = new Intent(this, MainActivity.class);
        e_email = (EditText) findViewById(R.id.emailEditTxt);
        final String email = e_email.getText().toString();
        e_pwd = (EditText) findViewById(R.id.pwdEditTxt);
        final String pwd = e_pwd.getText().toString();
        // intent.putExtra(EXTRA_MESSAGE, message);
        if (validate(email, pwd)) {
            startActivity(intent);
        }
    }

    public boolean validate(final String email, final String pwd) {
        // Instantiate the RequestQueue.
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "mtaas-worker.us-west-2.elasticbeanstalk.com/login";


        JsonObjectRequest jsObjRequest = new JsonObjectRequest(
                Request.Method.POST, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println("Response: " + response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        System.out.println("NOT WORK");
                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<String, String>();
                //add params <key,value>
                System.out.println("params");
                params.put("username", email);
                params.put("password", pwd);

                return params;
            }

            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<String, String>();
                System.out.println("headers");

                // add headers <key,value>
                //String credentials = USERNAME+":"+PASSWORD;
                        /*String auth = "Basic "
                                + Base64.encodeToString(credentials.getBytes(),
                                Base64.NO_WRAP);*/
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


        stat = new StatFs(Environment.getExternalStorageDirectory().getPath());
        long bytesTotal = (long) stat.getTotalBytes() / (1024 * 1024);
        str = "Total disk memory: " + bytesTotal;
        System.out.println(str);

        long bytesFree = (long) stat.getFreeBytes() / (1024 * 1024);
        str = "Avail disk memory: " + bytesFree;
        System.out.println(str);
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
