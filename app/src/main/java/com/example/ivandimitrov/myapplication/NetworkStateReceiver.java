package com.example.ivandimitrov.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

/**
 * Created by Ivan Dimitrov on 1/6/2017.
 */

public class NetworkStateReceiver extends BroadcastReceiver {
    NetworkListener listener;

    public NetworkStateReceiver(NetworkListener listener) {
        this.listener = listener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final ConnectivityManager connectivityManager = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                Toast.makeText(context, "WIFI Detected", Toast.LENGTH_LONG).show();
                listener.onInternetChanged(true);
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                Toast.makeText(context, "Mobile Detected", Toast.LENGTH_LONG).show();
                listener.onInternetChanged(true);
            }
        } else {
            Toast.makeText(context, "No internet", Toast.LENGTH_LONG).show();
            listener.onInternetChanged(false);

        }
    }

    interface NetworkListener {
        void onInternetChanged(boolean internetWorking);
    }
}
