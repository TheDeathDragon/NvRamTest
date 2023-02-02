package la.shiro.nvramtest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.HexDump;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Random;
import java.util.StringTokenizer;

import vendor.mediatek.hardware.nvram.V1_0.INvram;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final String TAG = "Rin";
    private TextView tv_title;
    private TextView tv_wifi_mac;

    private Button btn_read_wifi_mac;
    private Button btn_write_wifi_mac;

    private static final int MAC_ADDRESS_OFFSET = 4;
    private static final int MAC_ADDRESS_DIGITS = 6;

    private static final String MAC_ADDRESS_FILENAME = "/mnt/vendor/nvdata/APCFG/APRDEB/WIFI";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_title = findViewById(R.id.tv_title);
        tv_wifi_mac = findViewById(R.id.tv_wifi_mac);
        btn_read_wifi_mac = findViewById(R.id.btn_read_wifi_mac);
        btn_write_wifi_mac = findViewById(R.id.btn_write_wifi_mac);
        btn_read_wifi_mac.setOnClickListener(this);
        btn_write_wifi_mac.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        String wifiMac = getWifiMacFromNvRam();
        if (v.getId() == R.id.btn_read_wifi_mac) {
            tv_wifi_mac.setText("Get WiFi MAC : " + getWifiMacFromNvRam());
        } else if (v.getId() == R.id.btn_write_wifi_mac) {
            if (wifiMac.startsWith("00")) {
                String mRandomWifiMac = getRandomWifiMac();
                Log.d(TAG, "onClick() : " + mRandomWifiMac);
                if (updateWifiMac(mRandomWifiMac) && getWifiMacFromNvRam().equalsIgnoreCase(mRandomWifiMac)) {
                    tv_wifi_mac.setText("Set WiFi MAC : " + mRandomWifiMac);
                } else {
                    tv_wifi_mac.setText("Set WiFi MAC : Failed\ngetWifiMacFromNvRam() : " + getWifiMacFromNvRam() + " mRandomWifiMac : " + mRandomWifiMac);
                }
            }
        }
    }

    private String getWifiMacFromNvRam() {

        String wifiMac;
        try {
            INvram agent = INvram.getService();
            if (agent == null) {
                Toast.makeText(this, "No support MAC address writing due to NvRam", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "getWifiMacFromNvRam() NvRAMAgent is null");
                return "Failed to get NvRAMAgent";
            }
            try {
                wifiMac = agent.readFileByName(MAC_ADDRESS_FILENAME, MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS);
            } catch (Exception e) {
                e.printStackTrace();
                return "Failed to read MAC address from NvRAM";
            }
            Log.d(TAG, "getWifiMacFromNvRam() Raw data:" + wifiMac);
        } catch (Exception e) {
            e.printStackTrace();
            return "Failed to get NvRAMAgent";
        }
        return wifiMac.substring(8, 10) + ":" + wifiMac.substring(10, 12) + ":" + wifiMac.substring(12, 14) + ":" + wifiMac.substring(14, 16) + ":" + wifiMac.substring(16, 18) + ":" + wifiMac.substring(18, 20);

    }

    private String getRandomWifiMac() {
        // 00:08:22:xx:xx:xx
        String mRandomWifiMac;
        int nextInt;
        Random mRandom = new Random();
        StringBuilder sb = new StringBuilder("00:08:22:");
        NumberFormat formatter = new DecimalFormat("00");
        nextInt = mRandom.nextInt(90) + 10;// add 10 to avoid :0X: hex string
        String hexString1 = formatter.format(nextInt);
        nextInt = mRandom.nextInt(245) + 10;
        String hexString2 = Integer.toHexString(nextInt);
        nextInt = mRandom.nextInt(245) + 10;
        String hexString3 = Integer.toHexString(nextInt);
        sb.append(hexString1).append(":").append(hexString2).append(":").append(hexString3);
        mRandomWifiMac = sb.toString();
        Log.d(TAG, "getRandomWifiMac() : " + mRandomWifiMac);
        return mRandomWifiMac;
    }

    private boolean updateWifiMac(String wifiMac) {
        try {
            int i = 0;
            INvram agent = INvram.getService();
            byte[] macBytes = new byte[MAC_ADDRESS_DIGITS];
            if (agent == null) {
                Toast.makeText(this, "No support MAC address writing due to NvRam", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "NvRAMAgent is null");
                return false;
            }

            //parse mac address firstly
            StringTokenizer txtBuffer = new StringTokenizer(wifiMac, ":");
            while (txtBuffer.hasMoreTokens()) {
                macBytes[i] = (byte) Integer.parseInt(txtBuffer.nextToken(), 16);
                i++;
            }
            if (i != MAC_ADDRESS_DIGITS) {
                Log.d(TAG, "Wrong length of wifi mac address:" + i);
                Toast.makeText(this, "The format of mac address is not correct", Toast.LENGTH_SHORT).show();
                return false;
            }

            String buff;
            try {
                buff = agent.readFileByName(MAC_ADDRESS_FILENAME, MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }

            // Remove \0 in the end
            byte[] buffArr = HexDump.hexStringToByteArray(buff.substring(0, buff.length() - 1));

            for (i = 0; i < MAC_ADDRESS_DIGITS; i++) {
                buffArr[i + 4] = macBytes[i];
            }

            ArrayList<Byte> dataArray = new ArrayList<>(MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS);

            for (i = 0; i < MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS; i++) {
                dataArray.add(i, buffArr[i]);
            }
            Log.d(TAG, "updateWifiMac: dataArray = " + dataArray);

            try {
                agent.writeFileByNamevec(MAC_ADDRESS_FILENAME, MAC_ADDRESS_OFFSET + MAC_ADDRESS_DIGITS, dataArray);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to write MAC address to NvRAM", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Failed to write MAC address to NvRAM");
                return false;
            }
            Toast.makeText(this, "Update successfully.\r\nPlease reboot this device", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, "Failed to write MAC address to NvRAM");
        }
        return true;
    }
}