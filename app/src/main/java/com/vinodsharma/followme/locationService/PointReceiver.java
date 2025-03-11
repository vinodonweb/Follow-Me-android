package com.vinodsharma.followme.locationService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.vinodsharma.followme.TripLeadActivity;

public class PointReceiver extends BroadcastReceiver {

    private static final String TAG = "PointReceiver";

    private final TripLeadActivity tripLeadActivity;

    public PointReceiver(TripLeadActivity tripLeadActivity) {
        this.tripLeadActivity = tripLeadActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive: " + intent);

        if (intent == null || intent.getAction() == null)
            return;

        if (!intent.getAction().equals("com.example.broadcast.MY_BROADCAST"))
            return;


        double lat = intent.getDoubleExtra("LATITUDE", 0);
        double lon = intent.getDoubleExtra("LONGITUDE", 0);
        float bearing = intent.getFloatExtra("BEARING", 0);

        tripLeadActivity.updateLocation(new LatLng(lat, lon), bearing);

    }
}