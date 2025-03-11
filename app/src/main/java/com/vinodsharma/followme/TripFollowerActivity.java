package com.vinodsharma.followme;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.vinodsharma.followme.databinding.ActivityTripFollowingBinding;
import com.vinodsharma.followme.volleyApi.GetLastLocationAPIVolley;
import com.vinodsharma.followme.volleyApi.GetTripPoints;
import com.vinodsharma.followme.volleyApi.LatLngTime;
import com.vinodsharma.followme.volleyApi.TripExistCheckForFollower;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class TripFollowerActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityTripFollowingBinding binding;
    private String tripId;
    private long tripStartTime = 0;
    private final ArrayList<LatLng> latLonHistory = new ArrayList<>();
    public static int screenHeight;
    public static int screenWidth;
    private final float zoomDefault = 15.0f;
    private Marker carMarker;
    private Polyline llHistoryPolyline;
    private double totalDistance = 0.0;
    private boolean isRecenterEnabled = true;

    private static final String TAG = "TripFollowerActivity";

    private ObjectAnimator receiverAnimator;
    private ObjectAnimator progressAnimator;

    private boolean lastCheckHadNetwork = true;
    private Handler handler;
    private Runnable fetchRunnable;
    private boolean isTripEnded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityTripFollowingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        getScreenDimensions();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.followingMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        if(NetworkCheck.DoesNotHaveNetworkConnection(this)){
            showNoNetworkForEarlyPoint();
            return;
        }

        Intent intent = getIntent();
        tripId = intent.getStringExtra("tripID");
        binding.TripIDFollewr.setText(String.format("Trip ID: %s", tripId));
        Log.d(TAG, "Received trip ID: " + tripId);

        handler = new Handler();
        fetchRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isTripEnded) {
                    getLastPoint();
                    getTripPoints();
                    handler.postDelayed(this, 1000);
                }
            }
        };

        TripExistCheckForFollower tripFollowerActivity = new TripExistCheckForFollower(this);
        tripFollowerActivity.tripExists(tripId);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.moveCamera(CameraUpdateFactory.zoomTo(zoomDefault));
        mMap.setBuildingsEnabled(true);
        mMap.getUiSettings().setZoomControlsEnabled(true);

        //hide map loader initially
        binding.followerProgress.setVisibility(View.GONE);
    }

    private void getScreenDimensions() {
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenHeight = displayMetrics.heightPixels;
        screenWidth = displayMetrics.widthPixels;
    }

    private float getMarkerColor() {
        return BitmapDescriptorFactory.HUE_GREEN;
    }


    private float getRadius() {
        float z = mMap.getCameraPosition().zoom;
        return 15f * z - 130f;
    }

    private void calculateTotalDistance() {
        String distanceStr;
        if (totalDistance < 1000) {
            distanceStr = String.format(Locale.getDefault(), "Distance: %.0f m", totalDistance);
        } else {
            distanceStr = String.format(Locale.getDefault(), "Distance: %.2f km", totalDistance / 1000.0);
        }
        binding.distanceFollwer.setText(distanceStr);
    }

    private float calculateBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lon2 = Math.toRadians(to.longitude);

        double dLon = lon2 - lon1;
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2)
                - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return (float) ((bearing + 360) % 360);
    }

    private void updateElapsedTime() {
        long elapsedMillis = System.currentTimeMillis() - tripStartTime;
        long seconds = (elapsedMillis / 1000) % 60;
        long minutes = (elapsedMillis / (1000 * 60)) % 60;
        long hours = (elapsedMillis / (1000 * 60 * 60));
        String  elapsedTimeFormated = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
        binding.elapsedFollower.setText(String.format("Elapsed : %s", elapsedTimeFormated));
    }

    public void tripExists(String tripId) {
        //set up receiver animator
        receiverAnimator = ObjectAnimator.ofFloat(binding.broadcastFollwer, "alpha", 1.0f, 0.25f);
        receiverAnimator.setDuration(750);
        receiverAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        receiverAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        receiverAnimator.start();

        //set the progress animation
        progressAnimator = ObjectAnimator.ofFloat(binding.followerProgress, "alpha", 1.0f, 0.25f);
        progressAnimator.setDuration(750);
        progressAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        progressAnimator.setRepeatMode(ObjectAnimator.REVERSE);
        progressAnimator.start();

        setupNetworkCallback();
        if(NetworkCheck.DoesNotHaveNetworkConnection(this)){
            showNoNetworkForEarlyPoint();
            return;
        }

        handler.post(fetchRunnable);

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
                            Toast.makeText(TripFollowerActivity.this, "Network Available", Toast.LENGTH_SHORT).show();
                            TripFollowerActivity.this.runOnUiThread(() ->
                                    binding.networkMsg.setVisibility(View.GONE));
                        }
                    }

                    @Override
                    public void onLost(@NonNull Network network) {
                        super.onLost(network);
                        if (lastCheckHadNetwork) {
                            Toast.makeText(TripFollowerActivity.this, "Network Lost", Toast.LENGTH_SHORT).show();
                            lastCheckHadNetwork = false;
                            TripFollowerActivity.this.runOnUiThread(() -> {
                                binding.networkMsg.setVisibility(View.VISIBLE);
                                binding.networkMsg.setText("NO NETWORK CONNECTION\nNot all data may be sent");
                            });
                        }

                    }
                }
        );
    }

    public void tripNotExist(String tripIdForFollower) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip not Exists");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage("The Trip ID: " + tripId + " was not found.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(TripFollowerActivity.this, MainActivity.class);
            startActivity(intent);
        });
        builder.show();
    }

    private void getLastPoint() {
        GetLastLocationAPIVolley lastLocationAPIVolley = new GetLastLocationAPIVolley(this);
        lastLocationAPIVolley.getLastLocation(tripId);
    }

    private void getTripPoints() {
        GetTripPoints getTripPoints = new GetTripPoints(this);
        getTripPoints.getPoints(tripId);
    }

    //recenter the map
    public void reCenterFunction(View view){
        isRecenterEnabled = !isRecenterEnabled;
        if (isRecenterEnabled) {
            Toast.makeText(this, "Recenter enabled", Toast.LENGTH_SHORT).show();
            binding.reCenter.setAlpha(1.0f);
            if (!latLonHistory.isEmpty()) {
                LatLng latest = latLonHistory.get(latLonHistory.size() - 1);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latest));
            }
        } else {
            Toast.makeText(this, "Recenter disabled", Toast.LENGTH_SHORT).show();
            binding.reCenter.setAlpha(0.5f);
        }
    }

    public void handleLastLocationSuccess(LatLngTime llt) {
        if (llt == null || (llt.getLatLng().latitude == 0.0 && llt.getLatLng().longitude == 0.0)) {
            // Trip has ended
            if (receiverAnimator != null) {
                binding.broadcastFollwer.setAlpha(0.25f);
                receiverAnimator.cancel();
            }
            isTripEnded = true;
            handler.removeCallbacks(fetchRunnable);
            Toast.makeText(this, "Trip has ended", Toast.LENGTH_SHORT).show();
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Trip Ended");
            builder.setIcon(R.drawable.ic_launcher);
            builder.setMessage("The Trip has ended.");
            builder.setPositiveButton("OK", null);
            builder.show();
        } else {
            // Update car marker with the latest point
            LatLng latest = new LatLng(llt.getLatLng().latitude, llt.getLatLng().longitude);
            float bearing = 0;
            if (!latLonHistory.isEmpty()) {
                LatLng lastInHistory = latLonHistory.get(latLonHistory.size() - 1);
                bearing = calculateBearing(lastInHistory, latest);
            }
            updateCarMarker(latest, bearing);
        }
    }

    private void updateCarMarker(LatLng position, float bearing) {
        if (mMap == null) return;
        float r = getRadius();
        if (r > 0) {
            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.drawable.car);
            Bitmap resized = Bitmap.createScaledBitmap(icon, (int) r, (int) r, false);
            BitmapDescriptor iconBitmap = BitmapDescriptorFactory.fromBitmap(resized);
            MarkerOptions options = new MarkerOptions()
                    .position(position)
                    .icon(iconBitmap)
                    .rotation(bearing);
            if (carMarker != null) {
                carMarker.remove();
            }
            carMarker = mMap.addMarker(options);
        }
    }

    public void handleLastLocationFail(String error, String tripId) {
        Log.e(TAG, "Failed to get last location: " + error);
    }

    private void updateTripStartTime(){
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM d, hh:mm a", Locale.getDefault());
        String formatedTime = sdf.format(new Date(tripStartTime));
        binding.follwerStartTime.setText(String.format("Start Time: %s", formatedTime));
    }

    public void acceptInitialPathPoints(ArrayList<LatLngTime> points) {
        if (mMap == null || points.isEmpty()) return;

        Log.d(TAG, "acceptInitialPathPoints: Received " + points.size() + " points");

        if (tripStartTime == 0 && !points.isEmpty()) {
            tripStartTime = points.get(0).getDateTime().getTime();
            updateTripStartTime();
        }

        if (latLonHistory.isEmpty() && !points.isEmpty()) {
            LatLng origin = new LatLng(points.get(0).getLatLng().latitude, points.get(0).getLatLng().longitude);
            mMap.addMarker(new MarkerOptions()
                    .alpha(0.8f)
                    .position(origin)
                    .title("Leader Origin")
                    .icon(BitmapDescriptorFactory.defaultMarker(getMarkerColor())));
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(origin, zoomDefault));
        }

        // Update latLonHistory
        latLonHistory.clear();
        for (LatLngTime point : points) {
            if (point.getLatLng().latitude != 0.0 || point.getLatLng().longitude != 0.0) {
                latLonHistory.add(new LatLng(point.getLatLng().latitude, point.getLatLng().longitude));
            }
        }
        Log.d(TAG, "latLonHistory size: " + latLonHistory.size());

        if (llHistoryPolyline != null) {
            llHistoryPolyline.remove();
        }

        if (latLonHistory.size() >= 1) {
            PolylineOptions polylineOptions = new PolylineOptions();
            for (LatLng ll : latLonHistory) {
                polylineOptions.add(ll);
            }
            llHistoryPolyline = mMap.addPolyline(polylineOptions);
            llHistoryPolyline.setEndCap(new RoundCap());
            llHistoryPolyline.setWidth(12);
            llHistoryPolyline.setColor(Color.BLUE);

            // Calculate bearing and update car marker
            float bearing = 0;
            LatLng latest = latLonHistory.get(latLonHistory.size() - 1);
            if (latLonHistory.size() >= 2) {
                LatLng secondLast = latLonHistory.get(latLonHistory.size() - 2);
                bearing = calculateBearing(secondLast, latest);
            }
            updateCarMarker(latest, bearing);

            // Move camera to the latest point if recenter is enabled
            if (isRecenterEnabled) {
                mMap.animateCamera(CameraUpdateFactory.newLatLng(latest));
            }

            // Calculate total distance
            totalDistance = 0;
            for (int i = 1; i < latLonHistory.size(); i++) {
                float[] results = new float[1];
                Location.distanceBetween(
                        latLonHistory.get(i - 1).latitude, latLonHistory.get(i - 1).longitude,
                        latLonHistory.get(i).latitude, latLonHistory.get(i).longitude,
                        results
                );
                totalDistance += results[0];
            }
        }

        calculateTotalDistance();
        updateElapsedTime();
    }

    public void getTripPointsError(String error, String tripId) {
        Log.e(TAG, "Failed to get trip points: " + error);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && fetchRunnable != null) {
            handler.removeCallbacks(fetchRunnable);
        }
    }

    @Override
    protected void onPause(){
        super.onPause();
        //stop polling data
            handler.removeCallbacks(fetchRunnable);
    }

    @Override
    protected void onResume(){
        super.onResume();
        //start polling data
        handler.post(fetchRunnable);
    }

    public void tripNotFound(String tripId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Trip not Found");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage("The Trip ID: " + tripId + " was not found.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Intent intent = new Intent(TripFollowerActivity.this, MainActivity.class);
            startActivity(intent);
        });
        builder.show();
    }

    public void tripExistsError(String s, String tripId) {
        Log.d(TAG, "tripExistsError: " + s);
    }

    private void showNoNetworkForEarlyPoint(){
        binding.networkMsg.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle("Follow Me - No Network")
                .setIcon(R.drawable.ic_launcher)
                .setMessage("No network connection - cannot access trip data now\n\nCannot follow the trip now.")
                .setPositiveButton("OK", (dialog, which) -> {
                    finish();
                })
                .show();
    }
}