package com.vinodsharma.followme.volleyApi;

import androidx.annotation.NonNull;

import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class LatLngTime {

    private static final SimpleDateFormat sdf =
            new SimpleDateFormat("MM/dd HH:mm:ss", Locale.US);
    private final LatLng latLng;
    private final Date dateTime;

    public LatLngTime(double lat, double lon, Date dateTime) {
        this.latLng = new LatLng(lat, lon);
        this.dateTime = dateTime;
    }

    public LatLng getLatLng() {
        return latLng;
    }

    public Date getDateTime() {
        return dateTime;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format(Locale.getDefault(),
                "%.6f, %.6f at %s",
                latLng.latitude, latLng.longitude, sdf.format(dateTime));
    }
}
