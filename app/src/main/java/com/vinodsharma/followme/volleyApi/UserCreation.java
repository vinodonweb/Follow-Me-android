package com.vinodsharma.followme.volleyApi;

import android.health.connect.datatypes.SleepSessionRecord;
import android.net.Uri;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.vinodsharma.followme.MainActivity;

import org.json.JSONObject;

public class UserCreation {

    private static final String dataUrl =
            "http://christopherhield-001-site4.htempurl.com/api/UserAccounts/CreateUserAccount";
    private static final String TAG = "UserCreation";
    private RequestQueue queue;
    private String urlToUse;
    private final MainActivity mainActivity;

    public UserCreation(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        this.queue = Volley.newRequestQueue(mainActivity);
        Uri.Builder buildURL = Uri.parse(dataUrl).buildUpon();
        urlToUse = buildURL.build().toString();
    }

    public void createUser(String firstName, String lastName, String email, String userName, String password){
        Log.d(TAG, "createUser: " + firstName + " " + lastName + " " + email + " " + userName + " " + password);

        Response.Listener<JSONObject> listener = jsonObject -> {
            Log.d(TAG, "onResponse: " + jsonObject);
            mainActivity.handleCreateUserAccountSuccess(
                    jsonObject.optString("firstName"),
                    jsonObject.optString("lastName"),
                    jsonObject.optString("email"),
                    jsonObject.optString("userName"));
        };

        Response.ErrorListener error = volleyError -> {
            Log.d(TAG, "onErrorResponse1: " + volleyError.getMessage());

            if (volleyError.networkResponse != null) {
                String s = new String(volleyError.networkResponse.data);
                Log.d(TAG, "onErrorResponse2: " + s);
                mainActivity.handleCreateUserAccountFail(s);
            } else {
                mainActivity.handleCreateUserAccountFail(volleyError.getMessage());
            }
        };

        //make JSON object to write to the PUT endpoint
        JSONObject jsonParams;
        try{
            jsonParams = new JSONObject();
            jsonParams.put("firstName", firstName);
            jsonParams.put("lastName", lastName);
            jsonParams.put("email", email);
            jsonParams.put("userName", userName);
            jsonParams.put("password", password);
        } catch (Exception e){
            Log.d(TAG, "createUser: " + e.getMessage());
            throw  new RuntimeException(e);
        }

        //Request a String response from the provided URL
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.POST,
                urlToUse,
                jsonParams,
                listener,
                error);

        queue.add(jsonObjectRequest);
    }
}
