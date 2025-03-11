package com.vinodsharma.followme;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.vinodsharma.followme.databinding.ActivityTripLeadBinding;
import com.vinodsharma.followme.locationService.PointReceiver;
import com.vinodsharma.followme.locationService.LocationService;
import com.vinodsharma.followme.volleyApi.AddTripPoint;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class    TripLeadActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityTripLeadBinding binding;

    private static final int LOCATION_REQUEST = 111;
    private static final int BACKGROUND_LOCATION_REQUEST = 222;
    private static final int NOTIFICATION_REQUEST = 333;
    private PointReceiver pointReceiver;
    private Intent locationServiceIntent;
    private boolean lastCheckHadNetwork = true;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private Polyline llHistoryPolyline;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    private Marker carMarker;
    public static int screenHeight;
    public static int screenWidth;
    private final float zoomDefault = 15.0f;

    private String tripID;
    private String userName;
    private String firstName;
    private String lastName;

    private long tripStartTime = System.currentTimeMillis();
    private double totalDistance = 0.0;

    private boolean isPause = false;

    private ObjectAnimator broadcastAnimator;
    private ObjectAnimator progressAnimator;
    private ObjectAnimator progressTextAnimator;


    private static final String TAG = "TripLeadActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTripLeadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getScreenDimensions();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.followingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }


        //disable the pause text view
        binding.pauseText.setVisibility(View.GONE);

        startLocationService();

        //get the passed intent data from main activity
        Intent intent = getIntent();
        tripID = intent.getStringExtra("tripID");
        userName = intent.getStringExtra("userName");
        firstName = intent.getStringExtra("firstName");
        lastName = intent.getStringExtra("lastName");

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());
        //set the title of the activity
        binding.tripIDLead.setText(String.format("Trip ID: %s", tripID));
        binding.tripStartLead.setText(String.format("Trip started at: %s", sdf.format(new Date(tripStartTime))));

        // Set up share button
        binding.shareIcon.setOnClickListener(v -> openShareTo());

        broadcastAnimator = ObjectAnimator.ofFloat(binding.broadcast, "alpha", 1.0f, 0.25f);
        binding.broadcast.setAlpha(0.25f);
        progressAnimator = ObjectAnimator.ofFloat(binding.awaiting, "alpha", 1.0f, 0.25f);
        progressTextAnimator = ObjectAnimator.ofFloat(binding.awaitingText, "alpha", 1.0f, 0.25f);
        startObjectAnimators();

        setupNetworkCallback();
       if(isGpsNotEnabled()){
           startLocationService();
       }

    }



    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);



    }

    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    private boolean checkPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_REQUEST);
            return false;
        }
        return true;
    }

    private void startObjectAnimators() {
        broadcastAnimator.setDuration(750);
        broadcastAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        broadcastAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        progressAnimator.setDuration(750);
        progressAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        progressAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        progressTextAnimator.setDuration(750);
        progressTextAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        progressTextAnimator.setRepeatMode(ObjectAnimator.REVERSE);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(progressAnimator, progressTextAnimator);
        animatorSet.start();
    }

    private void setupNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();
        connectivityManager.registerNetworkCallback(
                builder.build(),
                new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        super.onAvailable(network);
                        if (!lastCheckHadNetwork) {
                            Toast.makeText(TripLeadActivity.this, "Network Available", Toast.LENGTH_SHORT).show();
                            lastCheckHadNetwork = true;
                            TripLeadActivity.this.runOnUiThread(() ->
                                    binding.networkMessage.setVisibility(View.GONE));
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        if (lastCheckHadNetwork) {
                            Toast.makeText(TripLeadActivity.this, "Network Lost", Toast.LENGTH_SHORT).show();
                            lastCheckHadNetwork = false;
                            TripLeadActivity.this.runOnUiThread(() -> {
                                binding.broadcast.setAlpha(0.25f);
                                broadcastAnimator.cancel();
                                binding.networkMessage.setVisibility(View.VISIBLE);
                                binding.networkMessage.setText("NO NETWORK CONNECTION\nNot all data may be sent");
                            });
                        }

                    }
                }
        );
    }

    private boolean isGpsNotEnabled() {
        android.location.LocationManager locationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager == null || !locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }


    private void startLocationService() {

            // Create a receiver to get the location updates
            pointReceiver = new PointReceiver(this);

            // Register the receiver
            ContextCompat.registerReceiver(this,
                    pointReceiver,
                    new IntentFilter("com.example.broadcast.MY_BROADCAST"),
                    ContextCompat.RECEIVER_EXPORTED);
        //starting service
        locationServiceIntent = new Intent(this, LocationService.class);

        Log.d(TAG, "startService: START");
        ContextCompat.startForegroundService(this, locationServiceIntent);
        Log.d(TAG, "startService: END");

    }

    //initial location marker color
    private float getMarkerColor() {
        return BitmapDescriptorFactory.HUE_GREEN;
    }


    public void updateLocation(LatLng latLng, float bearing) {
        // Update the UI
        Log.d(TAG, "updateLocation: " + latLng + " " + bearing);

        latLonHistory.add(latLng); // Add the LL to our location history

        if (llHistoryPolyline != null) {
            llHistoryPolyline.remove(); // Remove old polyline
        }

        if (latLonHistory.size() == 1) { // First update
//            mMap.addMarker(new MarkerOptions().alpha(0.8f).position(latLng).title("My Origin"));
            mMap.addMarker(new MarkerOptions()
                    .alpha(0.8f)
                    .position(latLng)
                    .title("My Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor())));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoomDefault));
            return;
        }

        if (latLonHistory.size() > 1) { // Second (or more) update
            PolylineOptions polylineOptions = new PolylineOptions();

            for (LatLng ll : latLonHistory) {
                polylineOptions.add(ll);
            }
            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);

            float r = getRadius();
            if (r > 0) {
                Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
                Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
                BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);


                MarkerOptions options = new MarkerOptions();
                options.position(latLng);
                options.icon(iconBitmap);
                options.rotation(bearing);

                if (carMarker != null) {
                    carMarker.remove();
                }

                carMarker = mMap.addMarker(options);
            }
        }
        mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        binding.awaiting.setVisibility(View.GONE);
        binding.awaitingText.setVisibility(View.GONE);

        //calculate the distance
        if(latLonHistory.size() > 1){
           LatLng previousPoint = latLonHistory.get(latLonHistory.size() - 2);
           float [] results = new float[1];
              Location.distanceBetween(previousPoint.latitude, previousPoint.longitude, latLng.latitude, latLng.longitude, results);
                totalDistance += results[0];
                updateTripDistance();

        }


        //add trip point
        if(!isPause){
            broadcastAnimator.start();
            AddTripPoint addTripPoint = new AddTripPoint(this);
            addTripPoint.sendPoint(tripID, latLng.latitude, latLng.longitude, new Date(), userName);
        }


        //calculate elapsed time
        calculateElapsedTime();
    }

    //calculate total distance traveled
    private void updateTripDistance() {
        String distanceStr;
        if (totalDistance < 1000) {
            distanceStr = String.format(Locale.getDefault(), "Distance: %.0f m", totalDistance);
        } else {
            distanceStr = String.format(Locale.getDefault(), "Distance: %.2f km", totalDistance / 1000.0);
        }
       binding.distanceLead.setText(distanceStr);
    }


    //calculate elapsed time
    private void calculateElapsedTime() {
        long elapsedMillis = System.currentTimeMillis() - tripStartTime;
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        String  elapsedTimeFormated = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        binding.elapsedLead.setText(elapsedTimeFormated);
    }


    public void pauseFunction(View view){
        isPause = !isPause;
        if(isPause){
            binding.pauseText.setVisibility(View.VISIBLE);
            binding.awaiting.setAlpha(0.25f);
            broadcastAnimator.cancel();
            binding.pause.setImageResource(R.drawable.play);
            Toast.makeText(this, "Trip paused", Toast.LENGTH_SHORT).show();
        } else {
            broadcastAnimator.start();
            binding.pauseText.setVisibility(View.GONE);
            binding.pause.setImageResource(R.drawable.pause);
            Toast.makeText(this, "Trip resumed", Toast.LENGTH_SHORT).show();
            startForegroundService(locationServiceIntent);
        }
    }

    //end the trip
    public void endTrip(View view){
        // Stop the location service
        if (locationServiceIntent != null) {
            stopService(locationServiceIntent);
        }

        // Unregister the point receiver if it exists
        if (pointReceiver != null) {
            try {
                unregisterReceiver(pointReceiver);
                pointReceiver = null;
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver already unregistered: " + e.getMessage());
            }
        }

        // Send final location point as 0.0, 0.0 to the API
        AddTripPoint addTripPoint = new AddTripPoint(this);
        addTripPoint.sendPoint(tripID,0.0,0.0, new Date(), userName);

        // Show alert dialog to confirm trip end
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip Ended");
        builder.setMessage("Trip has ended successfully.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Finish the activity
            finish();
        });
        builder.setCancelable(false); // Prevent dismissal by tapping outside
        builder.create().show();
    }

    //trip point handle successfully
    public void handleAddTripPointSuccess(String tripId, String latitude, String longitude, String datetime, String userName) {
        // You can add any success handling here if needed
        Log.d(TAG, "Trip point added successfully: " + tripId + " " + latitude + " " + longitude + " " + datetime + " " + userName);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null && locationListener != null)
            locationManager.removeUpdates(locationListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop the location service
        if (locationServiceIntent != null) {
            stopService(locationServiceIntent);
        }

        // Unregister the point receiver if it exists
        if (pointReceiver != null) {
            try {
                unregisterReceiver(pointReceiver);
                pointReceiver = null;
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "Receiver already unregistered: " + e.getMessage());
            }
        }

        // Send final location point as 0.0, 0.0 to the API
        AddTripPoint addTripPoint = new AddTripPoint(this);
        addTripPoint.sendPoint(tripID,0.0,0.0, new Date(), userName);

        Log.d(TAG, "onDestroy: Trip ended and service stopped");
    }


    @Override
    protected void onResume() {
        super.onResume();
        if (checkPermission() && locationManager != null && locationListener != null)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 10, locationListener);
    }

    @Override
    public void onBackPressed(){
        //move the task to the background without destroying it
        moveTaskToBack(true);
    }


    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }

    //open the trip details to shareTo Intent=
    private void openShareTo(){
        String subject = "Follow Me Trip ID" + tripID;
        String shareMessage = String.format(
                "%s %s has shared a \"Follow Me\" Trip ID with you.\nUse Follow Me Trip ID: %s”",
                firstName, lastName, tripID
        );

        // Create an Intent to share the text
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);

        // Start the share activity
        startActivity(Intent.createChooser(shareIntent, "Share Trip ID via"));
    }

    public void handleAddTripPointFail(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
        Log.d(TAG, "handleAddTripPointFail: " + s);
    }
}