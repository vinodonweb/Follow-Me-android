package com.vinodsharma.followme.volleyApi;

import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.vinodsharma.followme.MainActivity;

public class TripExistsCheckForLead {
    private static final String TAG = "TripExistsCheck";
    private static final String dataUrl =
            "http://christopherhield-001-site4.htempurl.com/api/Datapoints/TripExists";
    private final RequestQueue queue;
    private final MainActivity mainActivity;

    public TripExistsCheckForLead(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.queue = Volley.newRequestQueue(mainActivity);
    }

    public void tripExists(String tripId) {

        Uri.Builder buildURL = Uri.parse(dataUrl).buildUpon();
        buildURL.appendPath(tripId);
        String urlToUse = buildURL.build().toString();

        Response.Listener<String> listener = results -> {
            Log.d(TAG, "onResponse: " + results);
            boolean exists = Boolean.parseBoolean(results);
            if (exists) {
                mainActivity.tripExists();
            } else {
                mainActivity.tripNotExist();
            }
        };

        Response.ErrorListener error = volleyError -> {
            String s = volleyError.getMessage();
            if (volleyError.networkResponse != null) {
                s += new String(volleyError.networkResponse.data);
            }
            Log.d(TAG, "tripExists: " + s);
            mainActivity.tripExistsError(s, tripId);
        };

        // Request a string response from the provided URL.
        StringRequest stringRequest =
                new StringRequest(
                        Request.Method.GET,
                        urlToUse,
                        listener,
                        error);

        queue.add(stringRequest);
    }

}