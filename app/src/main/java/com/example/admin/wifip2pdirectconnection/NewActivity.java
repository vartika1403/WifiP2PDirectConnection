package com.example.admin.wifip2pdirectconnection;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.IOException;
import java.net.ServerSocket;

import static android.R.attr.port;

public class NewActivity extends AppCompatActivity {
    private static final String LOG_TAG = NewActivity.class.getSimpleName();
    private static final String SERVICE_TYPE = "_localdash._tcp";
    private static final String SERVICE_TYPE_PLUS_DOT = SERVICE_TYPE + ".";
    public static final String BROADCAST_TAG = "NSDBroadcast";
    public static final String KEY_SERVICE_INFO = "serviceinfo";
    private static final String DEFAULT_SERVICE_NAME = "LocalDashKK";

    private Context context;
    private LocalBroadcastManager broadcaster;
    private NsdManager nsdManager;
    private NsdManager.ResolveListener resolveListener;
    private NsdManager.DiscoveryListener discoveryListener;
    private NsdManager.RegistrationListener registrationListener;
    public String serviceName = DEFAULT_SERVICE_NAME;
    NsdServiceInfo service;
    private int serverPort;
//    private int port = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new);
        broadcaster = LocalBroadcastManager.getInstance(this);
        nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        final Button registerService = (Button)findViewById(R.id.register_nsd_service);
        registerService.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                serverPort = getPort();
               registerService(serverPort);
              //  discoverServices();
               
            }
        });

        final Button dicoverService = (Button) findViewById(R.id.discover_nsd_service);
        dicoverService.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onClick(View v) {
                discoverServices();
                initializeResolveListener();
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void discoverServices() {
        stopDiscovery();
        initializeDiscoveryListner();
        nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initializeDiscoveryListner() {
        discoveryListener = new NsdManager.DiscoveryListener() {
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(LOG_TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                Log.d(LOG_TAG, "Service discovery success" + service);
                String serviceType = service.getServiceType();
                Log.d(LOG_TAG, "Service discovery success: " + service.getServiceName());
                Log.d(LOG_TAG, "Service Port:, " + String.valueOf(service.getPort()));
                Log.d(LOG_TAG, "Servier Host:, " + service.getHost());

                // For some reason the service type received has an extra dot with it, hence
                // handling that case
                boolean isService = serviceType.equals(SERVICE_TYPE) || serviceType.equals
                        (SERVICE_TYPE_PLUS_DOT);
                if (!isService) {
                    Log.d(LOG_TAG, "Unknown Service Type: " + service.getServiceType());
                } else if (service.getServiceName().equals(serviceName)) {
                    Log.d(LOG_TAG, "Same machine: " + serviceName);
                } else if (service.getServiceName().contains(serviceName)) {
                    Log.d(LOG_TAG, "different machines. (" + service.getServiceName() + "-" +
                            serviceName + ")");
                    nsdManager.resolveService(service, resolveListener);
                }
            }

            @Override
            public void onServiceLost(NsdServiceInfo serviceInfo) {
                Log.e(LOG_TAG, "service lost" + serviceInfo);
                if (service == serviceInfo) {
                    service = null;
                }
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(LOG_TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(LOG_TAG, "Discovery failed: Error code:" + errorCode);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(LOG_TAG, "Discovery failed: Error code:" + errorCode);
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void stopDiscovery() {
        if (discoveryListener != null) {
            try {
                nsdManager.stopServiceDiscovery(discoveryListener);
            } finally {
            }
            discoveryListener = null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void registerService(int port) {
        initializeRegistrationListner();
        NsdServiceInfo nsdServiceInfo = new NsdServiceInfo();
        nsdServiceInfo.setServiceName(serviceName);
        nsdServiceInfo.setServiceType(SERVICE_TYPE);
        nsdServiceInfo.setPort(port);
        Log.i(LOG_TAG, "port, " + port);
        nsdManager.registerService(nsdServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private void initializeRegistrationListner() {
        registrationListener = new NsdManager.RegistrationListener() {

            @Override
            public void onServiceRegistered(NsdServiceInfo NsdServiceInfo) {
                // Save the service name.  Android may have changed it in order to
                // resolve a conflict, so update the name you initially requested
                // with the name Android actually used.
                serviceName = NsdServiceInfo.getServiceName();
                Log.i(LOG_TAG, "serviceName registered, " + serviceName);
            }

            @Override
            public void onRegistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Registration failed!  Put debugging code here to determine why.
                Log.i(LOG_TAG, "service reg failed, " + errorCode);
            }

            @Override
            public void onServiceUnregistered(NsdServiceInfo arg0) {
                // Service has been unregistered.  This only happens when you call
                // NsdManager.unregisterService() and pass in this listener.
            }

            @Override
            public void onUnregistrationFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Unregistration failed.  Put debugging code here to determine why.
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(LOG_TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.d(LOG_TAG, "Resolve Succeeded. " + serviceInfo);
                if (serviceInfo.getServiceName().equals(serviceName)) {
                    Log.d(LOG_TAG, "Same IP.");
                    return;
                }
                service = serviceInfo;

                Intent intent = new Intent(BROADCAST_TAG);
//                intent.putExtra(KEY_SERVICE_INFO, mService);
                broadcaster.sendBroadcast(intent);
            }
        };
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

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onPause() {
        stopDiscovery();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localDashReceiver);
        super.onPause();
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onResume() {
        super.onResume();
       /* nsdManager = (NsdManager) getSystemService(Context.NSD_SERVICE);
        if (nsdManager != null) {
            registerService(serverPort);
        }*/
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_TAG);
       // filter.addAction(DataHandler.DEVICE_LIST_CHANGED);
        //filter.addAction(DataHandler.CHAT_REQUEST_RECEIVED);
        //filter.addAction(DataHandler.CHAT_RESPONSE_RECEIVED);
        LocalBroadcastManager.getInstance(NewActivity.this).registerReceiver(localDashReceiver,
                filter);
       // LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DataHandler
         //       .DEVICE_LIST_CHANGED));

//        appController.startConnectionListener();
//        mNsdHelper.registerService(ConnectionUtils.getPort(LocalDashNSD.this));
    }

    @Override
    protected void onDestroy() {
   //     //mNsdHelper.tearDown();
     //   Utility.clearPreferences(LocalDashNSD.this);
       // appController.stopConnectionListener();
        //mNsdHelper.tearDown();
        //mNsdHelper = null;
        //DBAdapter.getInstance(LocalDashNSD.this).clearDatabase();
        super.onDestroy();
    }

    private BroadcastReceiver localDashReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case BROADCAST_TAG:
                    NsdServiceInfo serviceInfo = getChosenServiceInfo();
                    String ipAddress = serviceInfo.getHost().getHostAddress();
                    int port = serviceInfo.getPort();
                    // ip and host address of the server
                    Log.i(LOG_TAG, "ipAddress and port, " + port + ipAddress);
                   // DataSender.sendCurrentDeviceData(LocalDashNSD.this, ipAddress, port, true);
                    String ipAddressClient = getLocalIpAddress();
                    Log.d(LOG_TAG, "ipAddressClient, " + ipAddressClient);

                    break;
                default:
                    break;
            }
        }
    };

    private String getLocalIpAddress() {
        @SuppressLint("WifiManagerLeak") WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
        String ip = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());
        return ip;
    }

    public NsdServiceInfo getChosenServiceInfo() {
        return service;
    }
}
