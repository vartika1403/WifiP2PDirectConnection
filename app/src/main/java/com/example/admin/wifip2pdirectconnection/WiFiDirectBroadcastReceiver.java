package com.example.admin.wifip2pdirectconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

class WiFiDirectBroadcastReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = WiFiDirectBroadcastReceiver.class.getSimpleName();
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
                Log.i(LOG_TAG, "the list is, " + peerList.getDeviceList());
            if (!peerList.getDeviceList().isEmpty()) {
                Toast.makeText(activity, "peer device connected"
                        + peerList.getDeviceList().iterator().next(), Toast.LENGTH_LONG).show();
            }
        }
    };

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel, MainActivity activity) {
        super();
        this.manager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
            // Check to see if Wi-Fi is enabled and notify appropriate activity
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                Log.i(LOG_TAG, "the state is enable, " + state);
                // Wifi P2P is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                Log.i(LOG_TAG, "the state is  not enabled, " + state);
                // Wi-Fi P2P is not enabled
                activity.setIsWifiP2pEnabled(false);
            }
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
            // Call WifiP2pManager.requestPeers() to get a list of current peers
            Log.d(LOG_TAG, "P2P peers changed");
            if (manager != null) {
                manager.requestPeers(channel, peerListListener);
            }
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
            // Respond to new connection or disconnections
            if (manager == null) {
                return;
            }

            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);

            if (networkInfo.isConnected()) {

                // we are connected with the other device, request connection
                // info to find group owner IP
                Log.d(LOG_TAG,
                        "Connected to p2p network. Requesting network details");
             /*   manager.requestConnectionInfo(channel,*/
                        //(WifiP2pManager.ConnectionInfoListener) activity);
            } else {
                // It's a disconnect
                Log.i(LOG_TAG, "Disconnected to p2p network");
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
            // Respond to this device's wifi state changing
            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
            Log.d(LOG_TAG, "Device status" + device.status);
          /*  manager.requestConnectionInfo(channel,
                    (WifiP2pManager.ConnectionInfoListener) activity);*/
        }

    }
}
