package com.vinodsharma.followme.locationService;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.vinodsharma.followme.R;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    private Notification notification;

    private final String channelId = "LOCATION_CHANNEL";
    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // Since we need to post a notification, we first create a channel
        createNotificationChannel();

        // Create a notification required when running a foreground service.
        notification = new NotificationCompat.Builder(this, channelId)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: " + intent);
        // This Toast is just to let you know that the service has started.
        Toast.makeText(this, "SERVICE STARTED", Toast.LENGTH_LONG).show();

        // Start the service in the foreground
        startForeground(1, notification);
        Log.d(TAG, "onStartCommand: Service started in foreground");
        setupLocationListener();
        // If the service is killed, restart it
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        Uri soundUri = Uri.parse("android.resource://" +
                this.getPackageName() + "/" +
                R.raw.notif_sound);
        AudioAttributes att = new AudioAttributes.Builder().
                setUsage(AudioAttributes.USAGE_NOTIFICATION).build();


        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel mChannel = new NotificationChannel(channelId, channelId, importance);
        mChannel.setSound(soundUri, att);
        mChannel.setLightColor(Color.YELLOW);
        mChannel.setVibrationPattern(new long[]{0, 300, 100, 300});

        NotificationManager mNotificationManager = getSystemService(NotificationManager.class);
        mNotificationManager.createNotificationChannel(mChannel);
    }

    private void setupLocationListener() {

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new SvcLocListener(this);

        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
        }

    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy: ");
        locationManager.removeUpdates(locationListener);
        Log.d(TAG, "SERVICE DESTROYED");
        Toast.makeText(this, "SERVICE DESTROYED", Toast.LENGTH_LONG).show();

        super.onDestroy();
    }
}