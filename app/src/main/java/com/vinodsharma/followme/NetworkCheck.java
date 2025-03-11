package com.vinodsharma.followme;

import static androidx.core.content.ContextCompat.getSystemService;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;

public class NetworkCheck {

    public static  boolean DoesNotHaveNetworkConnection(Context context) {
        ConnectivityManager connectivityManager = getSystemService(context, ConnectivityManager.class);
        if (connectivityManager == null) {
            return true;
        }
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities(network);
        if (networkCapabilities != null) {
            return !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                    !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) &&
                    !networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
        }
        return true;
    }
}
