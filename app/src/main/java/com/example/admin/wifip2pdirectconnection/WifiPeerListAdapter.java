package com.example.admin.wifip2pdirectconnection;

import android.content.Context;
import android.database.DataSetObserver;
import android.net.wifi.p2p.WifiP2pDevice;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import static java.security.AccessController.getContext;

/**
 * Created by admin on 8/7/2017.
 */

public class WifiPeerListAdapter extends ArrayAdapter<WifiP2pDevice> {
    private static final String LOG_TAG = WifiPeerListAdapter.class.getSimpleName();
    private List<WifiP2pDevice> items;
    public WifiPeerListAdapter(@NonNull Context context, @LayoutRes int resource, List<WifiP2pDevice> items) {
        super(context, resource);
        this.items = items;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {

    }

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public WifiP2pDevice getItem(int position) {
        return items.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
            v = layoutInflater.inflate(R.layout.row_items,parent, false);
        }
       // Log.i(LOG_TAG, "device in adapter, " + );
        WifiP2pDevice device = items.get(position);
        Log.i(LOG_TAG, "device in adapter, " + device);

        if (device != null) {
            TextView textView = (TextView)v.findViewById(R.id.device_name);
            textView.setText(device.deviceName);
        }
        return v;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }


    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    @Override
    public boolean isEnabled(int position) {
        return false;
    }
}
