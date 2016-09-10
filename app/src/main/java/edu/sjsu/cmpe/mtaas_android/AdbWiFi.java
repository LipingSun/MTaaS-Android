package edu.sjsu.cmpe.mtaas_android;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import org.apache.http.conn.util.InetAddressUtils;

import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class AdbWiFi {
    private final static String TAG = "adbwifi.utility";

    private static int configPort = 5555;

    // adb wifi port, default 5555
    public static int getPort() {
        return configPort;
    }

    // not used right now, 2013-07-16
    public static void setPort(int port) {
        configPort = port;
    }

    // get phone ip
    public static String getIp() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress().toUpperCase();
                        boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // detect if wifi is connected
    public static boolean isWifiConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo.State state = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (state == NetworkInfo.State.CONNECTED) {
            return true;
        } else {
            return false;
        }
    }

    // detect if adbd is running
    public static boolean getAdbdStatus() {
        int lineCount = 0;
        try {
            Process process = Runtime.getRuntime().exec("ps | grep adbd");
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String str = input.readLine();
            while (str != null) {
                lineCount++;
                str = input.readLine();
            }
            if (lineCount >= 2) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // set adb wifi service
    public static boolean setAdbWifiStatus(boolean status) {
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            if (status) {
                os.writeBytes("setprop service.adb.tcp.port " + String.valueOf(getPort()) + "\n");
            } else {
                os.writeBytes("setprop service.adb.tcp.port -1" + "\n");
            }

            os.writeBytes("stop adbd\n");
            os.writeBytes("start adbd\n");

            os.writeBytes("exit\n");
            os.flush();

            p.waitFor();
            if (p.exitValue() != 255) {
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
