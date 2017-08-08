package com.example.admin.wifip2pdirectconnection;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceInfo;
import android.net.wifi.p2p.nsd.WifiP2pDnsSdServiceRequest;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements WifiP2pManager.ChannelListener, WifiP2pManager.PeerListListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private BroadcastReceiver receiver;
    private IntentFilter intentFilter;
    private boolean isWifiP2pEnabled = false;
    private boolean retryChannel = false;
    private WifiPeerListAdapter wifiPeerListAdapter;
    private WifiP2pDnsSdServiceRequest serviceRequest = null;

    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        Button discoverPeer = (Button)findViewById(R.id.discover_peer);

        discoverPeer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.i(LOG_TAG, "peer discovered");
                        peersUpdated();
                        // Code for when the discovery initiation is successful goes here.
                        // No services have actually been discovered yet, so this method
                        // can often be left blank.  Code for peer discovery goes in the
                        // onReceive method, detailed below.
                    }

                    @Override
                    public void onFailure(int reasonCode) {
                        Log.i(LOG_TAG, "peer discovered failed, " + reasonCode);
                        // Code for when the discovery initiation fails goes here.
                        // Alert the user that something went wrong.
                    }
                });
            }
        });

        Button registerService = (Button)findViewById(R.id.register_local_service);
        registerService.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                Map<String, String> record = new HashMap<String, String>();
                int serverPort = getPort();
                record.put("listnerport", String.valueOf(serverPort));
               // record.put("buddyname", "John Doe" + (int) (Math.random() * 1000));
                record.put("available", "visible");

                WifiP2pDnsSdServiceInfo serviceInfo =
                        WifiP2pDnsSdServiceInfo.newInstance("_test", "_presence._tcp", record);

                manager.addLocalService(channel, serviceInfo, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Log.i(LOG_TAG, "service is added ");
                    }

                    @Override
                    public void onFailure(int reason) {
                       Log.i(LOG_TAG, "failure in adding local service, " + reason);
                    }
                });
            }
        });

        Button discoverPeerUsingLocalService = (Button)findViewById(R.id.discover_peer_local_Service);
        discoverPeerUsingLocalService.setOnClickListener(new View.OnClickListener() {
            final HashMap<String, String> buddies = new HashMap<String, String>();
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                WifiP2pManager.DnsSdTxtRecordListener txtListener = new WifiP2pManager.DnsSdTxtRecordListener() {
                    @Override
                    public void onDnsSdTxtRecordAvailable(
                            String fullDomain, Map record, WifiP2pDevice device) {
                        Log.d(LOG_TAG, "DnsSdTxtRecord available -" + record.toString());
                        buddies.put(device.deviceAddress, (String) record.get("buddyname"));
                    }
                };
                WifiP2pManager.DnsSdServiceResponseListener servListener = new WifiP2pManager.DnsSdServiceResponseListener() {
                    @Override
                    public void onDnsSdServiceAvailable(String instanceName, String registrationType,
                                                        WifiP2pDevice resourceType) {

                        // Update the device name with the human-friendly version from
                        // the DnsTxtRecord, assuming one arrived.
                        resourceType.deviceName = buddies
                                .containsKey(resourceType.deviceAddress) ? buddies
                                .get(resourceType.deviceAddress) : resourceType.deviceName;
                    }
                };
          manager.setDnsSdResponseListeners(channel, servListener, txtListener);
                serviceRequest = WifiP2pDnsSdServiceRequest.newInstance();
                manager.addServiceRequest(channel, serviceRequest,
                        new WifiP2pManager.ActionListener() {

                            @Override
                            public void onSuccess() {
                                Log.d(LOG_TAG, "Added service discovery request");
                            }

                            @Override
                            public void onFailure(int arg0) {
                                Log.d(LOG_TAG, "ERRORCEPTION: Failed adding service discovery request");
                            }
                        });
                manager.discoverServices(channel, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        Log.d(LOG_TAG, "Service discovery initiated");
                    }

                    @Override
                    public void onFailure(int arg0) {
                        Log.d(LOG_TAG, "Service discovery failed: " + arg0);
                    }
                });
            }
        });
    }


    private int getPort() {
        int localPort = -1;
        try {
            ServerSocket s = new ServerSocket(0);
            localPort = s.getLocalPort();

            //closing the port
            if (s != null && !s.isClosed()) {
                s.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i(LOG_TAG, ":free port requested:" + localPort);
    return localPort;
    }

    private void peersUpdated() {
        Log.i(LOG_TAG, "peersUpdated");

        manager.requestPeers(channel, new WifiP2pManager.PeerListListener() {
            @Override
            public void onPeersAvailable(WifiP2pDeviceList peersList) {
                // print all peers
                Log.i(LOG_TAG, "peers," + peersList.getDeviceList());
                for (WifiP2pDevice device : peersList.getDeviceList()) {
                    Log.i(LOG_TAG, "peersUpdated: " + device.deviceName + " "
                            + device.primaryDeviceType + " " + device.deviceAddress);
                    peers.add(device);
                    TextView firstDeviceName = (TextView)findViewById(R.id.first_device_name_text);
                    firstDeviceName.setText(device.deviceName);
                    firstDeviceName.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            WifiP2pDevice device = peers.get(0);

                            WifiP2pConfig config = new WifiP2pConfig();
                            config.deviceAddress = device.deviceAddress;
                            config.wps.setup = WpsInfo.PBC;

                            manager.connect(channel, config, new WifiP2pManager.ActionListener() {

                                @Override
                                public void onSuccess() {
                                    // WiFiDirectBroadcastReceiver will notify us. Ignore for now.
                                }

                                @Override
                                public void onFailure(int reason) {
                                    Toast.makeText(MainActivity.this, "Connect failed. Retry.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    });
                    //  wifiPeerListAdapter.notifyDataSetChanged();
                }

                Log.i(LOG_TAG, "the peers, " + peers.size());
               /* wifiPeerListAdapter = new WifiPeerListAdapter(MainActivity.this, R.layout.row_items, peers);
                ListView listView = (ListView) findViewById(R.id.devices_list);
                listView.setAdapter(wifiPeerListAdapter);
                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                    }
                });*/
            }
        });
    }

    /* register the broadcast receiver with the intent values to be matched */
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);
    }
    /* unregister the broadcast receiver */
    @Override
    protected void onPause() {
        //super.onPause();
        unregisterReceiver(receiver);
        super.onPause();
    }

    @Override
    public void onChannelDisconnected() {
        Log.i(LOG_TAG, "channel disconnected");
        // we will try once more
           /* if (manager != null && !retryChannel) {
                Toast.makeText(this, "Channel lost. Trying again", Toast.LENGTH_LONG).show();
                resetData();
                retryChannel = true;*/
        manager.initialize(this, getMainLooper(), MainActivity.this);
           /* } else {
                Toast.makeText(this,
                        "Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.",
                        Toast.LENGTH_LONG).show();
            }*/
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peers) {
        Log.i(LOG_TAG, "the list of peers, " + peers.getDeviceList());
    }
}
