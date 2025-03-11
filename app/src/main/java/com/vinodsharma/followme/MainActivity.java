package com.vinodsharma.followme;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.vinodsharma.followme.databinding.ActivityMainBinding;
import com.vinodsharma.followme.databinding.FollowerInputBinding;
import com.vinodsharma.followme.databinding.LogininputBinding;
import com.vinodsharma.followme.databinding.RegisterBinding;
import com.vinodsharma.followme.databinding.TripIdBinding;
import com.vinodsharma.followme.volleyApi.TripExistCheckForFollower;
import com.vinodsharma.followme.volleyApi.TripExistsCheckForLead;
import com.vinodsharma.followme.volleyApi.UserCreation;
import com.vinodsharma.followme.volleyApi.VerifyUserCredentials;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private static final long minSplashTime = 2000;
    private long startTime;
    //login input values
    private  String loginInputUserName;
    private  String loginInputPassword;
    private boolean isLogIn = false;

    private String tripIDInput;

    //register input values
    private String registerFirstName;
    private String registerLastName;
    private String registerEmail;
    private String registerUserName;
    private String registerPassword;

    //location
    private static final int LOCATION_REQUEST = 111;
    private static final int BACKGROUND_LOCATION_REQUEST = 222;
    private static final int NOTIFICATION_REQUEST = 333;

    //shared preference
    private SharedPreferences sharedPref;
    private static final String PREF_NAME = "LoginPrefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER = "remember";

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        startTime = System.currentTimeMillis();

        SplashScreen.installSplashScreen(this)
                .setKeepOnScreenCondition(() -> System.currentTimeMillis() - startTime < minSplashTime);

        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //initialize the shared preference
        sharedPref = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        ViewCompat.setOnApplyWindowInsetsListener(binding.main, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST) {
            if (permissions[0].equals(android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    Toast.makeText(this, "Location Permission not Granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
        if (requestCode == NOTIFICATION_REQUEST) {
            if (permissions[0].equals(android.Manifest.permission.POST_NOTIFICATIONS)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkAppPermission();
                    return;
                } else {
                    Toast.makeText(this, "Notification Permission not Granted", Toast.LENGTH_SHORT).show();
                }

            }
        }
        if (requestCode == BACKGROUND_LOCATION_REQUEST) {
            if (permissions[0].equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    generateTripId(null);

                } else {
                    Toast.makeText(this, "Background Location Permission not Granted", Toast.LENGTH_SHORT).show();
                }

            }
        }

    }

    private boolean checkAppPermission() {

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    }, LOCATION_REQUEST);
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.POST_NOTIFICATIONS
                        }, NOTIFICATION_REQUEST);
                return false;
            }
        }
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    }, BACKGROUND_LOCATION_REQUEST);
            return false;
        }

        return true;
    }

    //check if the GPS is enabled
    private boolean isGpsNotEnabled() {
        android.location.LocationManager locationManager =
                (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager == null || !locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER);
    }

        //show login alert
    public void showLoginAlert(View view) {

        isLogIn = sharedPref.getBoolean("isLoggedIn", false);

        if(isLogIn){
            generateTripId(view);
        } else {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Follow Me");
            alert.setIcon(R.drawable.ic_launcher);
            alert.setMessage("Please login to continue");

            //inflate the login dialog layout
            LogininputBinding loginBinding = LogininputBinding.inflate(getLayoutInflater());
            alert.setView(loginBinding.getRoot());

            // Pre-fill credentials if they exist in SharedPreferences
            boolean rememberMe = sharedPref.getBoolean(KEY_REMEMBER, false);
            if (rememberMe) {
                loginBinding.userNameLogin.setText(sharedPref.getString(KEY_USERNAME, ""));
                loginBinding.loginPassword.setText(sharedPref.getString(KEY_PASSWORD, ""));
                loginBinding.checkBox.setChecked(true);
            }


            alert.setPositiveButton("Login", (dialog, which) -> {


                //get login input values
                //check if input field is empty
                if (loginBinding.userNameLogin.getText().toString().isEmpty() || loginBinding.loginPassword.getText().toString().isEmpty()) {
                    Toast.makeText(this, "UserName and password required to login", Toast.LENGTH_LONG).show();
                    return;
                }

                //show notNetworkLogin
                if(NetworkCheck.DoesNotHaveNetworkConnection(this)){
                    showNoNetworkLogin();
                    return;
                }

                loginInputUserName = loginBinding.userNameLogin.getText().toString();
                loginInputPassword = loginBinding.loginPassword.getText().toString();

                //save the login credentials values
                SharedPreferences.Editor editor = sharedPref.edit();
                if (loginBinding.checkBox.isChecked()) {
                    //save login input values
                    editor.putString(KEY_USERNAME, loginInputUserName);
                    editor.putString(KEY_PASSWORD, loginInputPassword);
                    editor.putBoolean(KEY_REMEMBER, true);

                } else {
                    // Clear saved credentials if checkbox is unchecked
                    editor.clear();
                }
                editor.apply();
                //verify the user credentials
                VerifyUserCredentials verifyUserCredentials = new VerifyUserCredentials(this);
                verifyUserCredentials.checkCredentials(loginInputUserName, loginInputPassword);
            });


            alert.setNegativeButton("Cancel", (dialog, which) -> {
                // Do something when user press the cancel button
            });
            alert.setNeutralButton("Register", (dialog, which) -> {
                showRegisterAlert(view);
            });
            alert.show();
        }


    }

    //show register form
    public void showRegisterAlert(View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("Please register to continue");

        //inflate the register dialog layout
        RegisterBinding registerBinding = RegisterBinding.inflate(getLayoutInflater());
        alert.setView(registerBinding.getRoot());


        alert.setPositiveButton("Register", (dialogInterface, which) -> {
            //get register input values
            registerFirstName = registerBinding.firstName.getText().toString().trim();
            registerLastName = registerBinding.lastName.getText().toString().trim();
            registerEmail = registerBinding.email.getText().toString().trim();
            registerUserName = registerBinding.userName.getText().toString().trim();
            registerPassword = registerBinding.passwordRegisterInput.getText().toString().trim();



            //validate the input fields
            if(registerFirstName.length() < 1 || registerFirstName.length() > 100){
                Toast.makeText(this, "First name must be between 1 and 100 characters", Toast.LENGTH_LONG).show();
            }

            if(registerFirstName.length() < 1 || registerFirstName.length() > 100){
                Toast.makeText(this, "Last name must be between 1 and 100 characters", Toast.LENGTH_LONG).show();
            }

            // Validate Email: Must be a valid email format
            if (!Patterns.EMAIL_ADDRESS.matcher(registerEmail).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            }

            //check usesr name is empty
            if(registerUserName.isEmpty()){
                Toast.makeText(this, "Username is required", Toast.LENGTH_LONG).show();
            }

            //validate uesr name: 8 - 12 characters
            if(registerUserName.length() < 8 || registerUserName.length() > 12){
                Toast.makeText(this, "Username must be between 8 and 12 characters", Toast.LENGTH_LONG).show();
            }

            //validate the password: 8 - 12 characters
            if(registerPassword.length() < 8 || registerPassword.length() > 12){
                Toast.makeText(this, "Password must be between 8 and 12 characters", Toast.LENGTH_LONG).show();
            }

            //show RegisterNoNetwork
            if(NetworkCheck.DoesNotHaveNetworkConnection(this)){
                showNoNetworkRegister();
                return;
            }

                //register the user
                UserCreation userCreation = new UserCreation(this);
                userCreation.createUser(registerFirstName, registerLastName, registerEmail, registerUserName, registerPassword);

        });
        alert.setNegativeButton("Cancel", (dialogInterface, which) -> {
            // Do something when user press the cancel button
        });

        alert.show();
    }

    //show Alert if the user account failed to create form volley
    public void handleCreateUserAccountFail(Object o){
        String errorMessage = "";
        if(o != null){
            errorMessage = o.toString();
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Follow Me - Registration Failed");
        builder.setIcon(R.drawable.ic_launcher);
        builder.setMessage(errorMessage);
        builder.setPositiveButton("OK", (dialog, which) -> {
            showRegisterAlert(null);
        });
        builder.create().show();
    }

    //handle user creation success
    public void handleCreateUserAccountSuccess(String firstName, String lastName, String email, String userName){

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Follow Me - Registration Successful");
        builder.setIcon(R.drawable.ic_launcher);
        String message = "Welcome " + firstName + " " + lastName + " " +  "\n"
                + "\n" +
                "Your userName is: " + userName + "\n" +
                "Your email is: " + email;

        builder.setMessage(message);
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Do something when user press the OK button
            showLoginAlert(null);
        });
        builder.create().show();
    }

    //show Follower alert
    public void showFollowerAlert(View view){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("Enter the Trip ID to follow.");

        //inflate the Follower trip layout
        FollowerInputBinding followerBinding = FollowerInputBinding.inflate(getLayoutInflater());
        alert.setView(followerBinding.getRoot());


        alert.setPositiveButton("Ok", (dialog, which) -> {

            //Get ID input text
            String followerTripIDInput = followerBinding.FollowTripID.getText().toString().trim();

            //check if the trip id is empty
            if(followerTripIDInput.isEmpty()){
                Toast.makeText(this, "Trip ID cannot be empty", Toast.LENGTH_LONG).show();
                return;
            }

            Log.d(TAG, "sending trip id to TripFollower: " + followerTripIDInput);
            Intent intent = new Intent(this, TripFollowerActivity.class);
            intent.putExtra("tripID", followerTripIDInput);
            startActivity(intent);

        });

        alert.setNegativeButton("Cancel", (dialog, which) -> {
            // Do something when user press the cancel button
        });
        alert.show();
    }

    //handle verify user credentials success
    public void handleVerifyUserCredentialsSuccess(String userName, String firstName, String lastName){
//        loginFirstName = firstName;
//        loginLastName = lastName;

        Toast.makeText(this, "Welcome " + firstName + " " + lastName, Toast.LENGTH_LONG).show();

        isLogIn = true;
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putBoolean("isLoggedIn", true);
        editor.putString("firstName", firstName);
        editor.putString("lastName", lastName);
        editor.apply();

        boolean hasPerm = checkAppPermission();
        if (hasPerm) {
            generateTripId(null);
        }


    }

    //alert if GPS is not enable
    private void gpsNotEnabledAlert(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("GPS is not enabled. Please enable GPS by turning on \"Use location\" in the Location settings to continue.");
        alert.setPositiveButton("Go to Location Settings", (dialog, which) -> {
            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        });

        alert.setPositiveButton("Cancel", (dialog, which) -> {
            // Do something when user press the OK button
        });
        alert.create().show();
    }

    //make a alert to generate the trip id Alert
    public void generateTripId(View view){
        if(isGpsNotEnabled()){
            gpsNotEnabledAlert();
            return;
        }



        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("Please provide a Trip ID for this Journey. share this ID with your friends");

        //inflate the trip ID  layout
        TripIdBinding tripIdBinding = TripIdBinding.inflate(getLayoutInflater());
        alert.setView(tripIdBinding.getRoot());

        AlertDialog dialog = alert.create();


        dialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK", (dialog1, which) -> {
            //Get ID input text
            tripIDInput = tripIdBinding.tripIDLogin.getText().toString().trim();

            if (tripIDInput.isEmpty()) {
                Toast.makeText(this, "Trip ID cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            TripExistsCheckForLead tripExistsCheckForLead = new TripExistsCheckForLead(this);
            tripExistsCheckForLead.tripExists(tripIDInput);

        });

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", (dialog12, which) -> {
            // Do something when user press the cancel button
        });

        dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Generate", (dialog13, which) -> {
            //override the onClick to prevent the dialog from closing
        });

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
             tripIDInput = generateRandomTripId();
            tripIdBinding.tripIDLogin.setText(tripIDInput);
        });

    }

    //generate the trip id.
    public String generateRandomTripId(){
        String tripId = "";
        String alphaNumeric = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        for (int i = 0; i < 5; i++) {
            tripId += alphaNumeric.charAt((int) (Math.random() * alphaNumeric.length()));
        }
        tripId += "-";
        for (int i = 0; i < 5; i++) {
            tripId += alphaNumeric.charAt((int) (Math.random() * alphaNumeric.length()));
        }
        return tripId;
    }

    //handle verify user credentials success
    public void handleVerifyUserCredentialsFail(){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setTitle("Follow Me - Login Failed");
            alert.setIcon(R.drawable.ic_launcher);
            alert.setMessage("Invalid username or password");
            alert.setPositiveButton("OK", (which, dialog) -> {
                showLoginAlert(null);
            });
            alert.create().show();
    }

    //handle trip exits or not

    //trip does not exits
    public void tripExistsError(String s, String tripId) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me - Trip ID Error");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("Trip ID: " + tripId + " error: " + s);
        alert.setPositiveButton("OK", null);
        alert.create().show();
    }

    //trip not exits
    public void tripNotExist() {
        Log.d(TAG, "tripNotExist: trip does not exist");

        //retrieve the value from shared preference
        String firstName = sharedPref.getString("firstName", "");
        String lastName = sharedPref.getString("lastName", "");


        Intent intent = new Intent(this, TripLeadActivity.class);
        intent.putExtra("tripID", tripIDInput);
        intent.putExtra("userName", KEY_USERNAME);
        intent.putExtra("firstName", firstName);
        intent.putExtra("lastName", lastName);
        startActivity(intent);
    }

    //trip exits
    public void tripExists() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Follow Me - Trip ID Exists");
        alert.setIcon(R.drawable.ic_launcher);
        alert.setMessage("Trip ID " + tripIDInput +  " exists.");
        alert.setPositiveButton("OK", null);
        alert.create().show();

    }

     //show No Internet when registering
    private void showNoNetworkRegister(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_launcher);
        alert.setTitle("Follow Me - No Network");
        alert.setMessage("No network connection - cannot create user account now");
        alert.setPositiveButton("OK", null);
        alert.create().show();
    }

    //alert not network while login
    private void showNoNetworkLogin(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_launcher);
        alert.setTitle("Follow Me - No network");
        alert.setMessage("No network connection - cannot login now");
        alert.setPositiveButton("OK", null);
        alert.create().show();
    }


}