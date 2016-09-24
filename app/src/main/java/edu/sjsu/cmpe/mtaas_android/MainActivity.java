package edu.sjsu.cmpe.mtaas_android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.android.volley.*;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = "MainActivity";

    private LinearLayout toggleButton;
    private TextView hint, toggleLeft, toggleRight;
    private boolean toggleStatus = false;
    private WifiStateReceiver wifiStateReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initVal();
    }

    @Override
    protected void onStart() {
        super.onStart();
        init();
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onStop() {
        //PortForward.disconnect();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        unregisterReceiver(wifiStateReceiver);
        super.onDestroy();
    }

    private void initVal() {
        this.toggleButton = (LinearLayout) findViewById(R.id.toggleLayout);
        this.hint = (TextView) findViewById(R.id.hint);
        this.toggleLeft = (TextView) findViewById(R.id.toggleLeft);
        this.toggleRight = (TextView) findViewById(R.id.toggleRight);

        this.wifiStateReceiver = new WifiStateReceiver();
        registerReceiver(wifiStateReceiver, new IntentFilter(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION));
    }

    private void init() {
        if (AdbWiFi.isWifiConnected(this)) {
            resetToggleStatus();
        } else {
            lockToggle();
        }
    }

    private void resetToggleStatus() {
        toggleStatus = AdbWiFi.getAdbdStatus();
        setToggleStatus(toggleStatus);
        toggleButton.setOnClickListener(null);
        toggleButton.setOnClickListener(new ToggleClickListener());
    }

    private void lockToggle() {
        AdbWiFi.setAdbWifiStatus(false);             // try stop
        toggleStatus = false;
        setToggleStatus(toggleStatus);
        hint.setText("wifi is not connected");
        toggleButton.setOnClickListener(null);
    }

    private class WifiStateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)) {
                if (intent.getBooleanExtra(WifiManager.EXTRA_SUPPLICANT_CONNECTED, false)) {
                    // wifi is connected, waiting for ip
                    try {
                        Thread.sleep(10000);
                        int tryTimes = 0;
                        while (AdbWiFi.getIp() == null && tryTimes < 0) {
                            Thread.sleep(1000);
                        }
                        resetToggleStatus();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    // wifi connection lost
                    lockToggle();
                }
            }
        }
    }

    private class ToggleClickListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (AdbWiFi.isWifiConnected(MainActivity.this)) {
                // try switch
                boolean ret = AdbWiFi.setAdbWifiStatus(!toggleStatus) && PortForward.setConnection(MainActivity.this, !toggleStatus);
                if (ret) {
                    // switch successfully
                    toggleStatus = !toggleStatus;
                    setToggleStatus(toggleStatus);

                    String url = "http://mtaas-worker.us-west-2.elasticbeanstalk.com/api/v1/device/" + ShareValues.getValue(MainActivity.this, "device_id");
                    JSONObject payload = new JSONObject();
                    try {
                        if (toggleStatus) {
                            payload.put("status", "available");
                            Toast.makeText(MainActivity.this, "service started", Toast.LENGTH_SHORT).show();
                        } else {
                            payload.put("status", "online");
                            Toast.makeText(MainActivity.this, "service stopped", Toast.LENGTH_SHORT).show();
                        }
                        payload.put("adb_uri", ShareValues.getValue(MainActivity.this, "ngrok_url"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    JsonObjectRequest jsonObjRequest = new JsonObjectRequest(Request.Method.PUT, url, payload,
                            new Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    Log.d(TAG, response.toString());
                                }
                            },
                            new Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.e(TAG, error.toString());
                                }
                            }) {
                        @Override
                        public Map<String, String> getHeaders() throws AuthFailureError {
                            Map<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json");
                            headers.put("Authorization", ShareValues.getValue(MainActivity.this, "token"));
                            return headers;
                        }
                    };

                    RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
                    requestQueue.add(jsonObjRequest);
                } else {
                    // failed
                    Toast.makeText(MainActivity.this, "something wrong", Toast.LENGTH_SHORT).show();
                }
            } else {
                lockToggle();
            }
        }
    }

    private void setToggleStatus(boolean status) {
        if (!status) {
            toggleLeft.setText("OFF");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_dark));
            toggleRight.setText("");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.gray_light));
            hint.setText("");
        } else {
            toggleLeft.setText("");
            toggleLeft.setBackgroundColor(getResources().getColor(R.color.gray_light));
            toggleRight.setText("ON");
            toggleRight.setBackgroundColor(getResources().getColor(R.color.blue_holo));
            hint.setText("adb connect " + PortForward.ngrokURL);
        }
    }
}
