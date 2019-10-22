package com.example.sample2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Calendar;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends AppCompatActivity {

    public class TimerBackground extends TimerTask {

        @Override
        public void run() {
            new GPSAsync().execute();
        }
    }

    public class GPSAsync extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {
            if (mCurrentLocation != null)
                appendToFile(mCurrentLocation.getLatitude()+"", mCurrentLocation.getLongitude()+"", ""+mCurrentLocation.getSpeed());
            return null;
        }
    }

    Button startLoggingBtn;
    Button stopLoggingBtn;
    TextView latitudeText;
    TextView longitudeText;
    TextView speedText;

    private FusedLocationProviderClient mFusedLocationClient;
    private SettingsClient mSettingsClient;
    private LocationRequest mLocationRequest;
    private LocationSettingsRequest mLocationSettingsRequest;
    private LocationCallback mLocationCallback;
    private Location mCurrentLocation;
    private String fileName;
    private Timer timer;
    private int MY_PERMISSIONS_REQUEST;

    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    File directory;
    File file;
    File sdCard;
    FileOutputStream fOut;
    OutputStreamWriter osw;

    private Boolean mRequestingLocationUpdates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startLoggingBtn = findViewById(R.id.start);
        stopLoggingBtn = findViewById(R.id.stop);
        latitudeText = findViewById(R.id.latValue);
        longitudeText = findViewById(R.id.longValue);
        speedText = findViewById(R.id.speedValue);
        if (checkPermissions()) {
            ActivityCompat.requestPermissions(MainActivity.this, PERMISSIONS, MY_PERMISSIONS_REQUEST);
        } else {
            // Permission has already been granted
        }

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mSettingsClient = LocationServices.getSettingsClient(this);

        mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                mCurrentLocation = locationResult.getLastLocation();
                update();
            }
        };

        mRequestingLocationUpdates = false;
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        mLocationSettingsRequest = builder.build();
        update();
    }

    private boolean checkPermissions(){
        return ContextCompat.checkSelfPermission(MainActivity.this, READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, INTERNET) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(MainActivity.this, ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED;
    }

    private void update() {
        if (mCurrentLocation != null) {
            String latitude = String.format(Locale.ENGLISH, "%f", mCurrentLocation.getLatitude());
            String longitude = String.format(Locale.ENGLISH,"%f", mCurrentLocation.getLongitude());
            String speed = String.format(Locale.ENGLISH,"%f", mCurrentLocation.getSpeed());
            latitudeText.setText(latitude);
            longitudeText.setText(longitude);
            speedText.setText(speed);
        }
        toggle();

    }

    private void toggle() {
        if (mRequestingLocationUpdates) {
            startLoggingBtn.setEnabled(false);
            stopLoggingBtn.setEnabled(true);
        } else {
            startLoggingBtn.setEnabled(true);
            stopLoggingBtn.setEnabled(false);
        }
    }

    private void appendToFile(String latitude, String longitude, String speed) {
        try
        {
            String myStr = latitude + "," + longitude + "," + speed + "\n";
            osw.write(myStr);
            osw.flush();
        }
        catch (IOException ioe)
        {
            ioe.printStackTrace();
        }

    }

    private void startLocationUpdates() {
        mSettingsClient
                .checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener(this, new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                        Toast.makeText(getApplicationContext(), "Logging Location", Toast.LENGTH_SHORT).show();

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        timer = new Timer();
                        TimerTask timerTask = new TimerBackground();
                        timer.schedule(timerTask, 0, 1000);
                        update();
                    }
                });
    }

    public void startLogging(View view) {
        mRequestingLocationUpdates = true;
        fileName = "gps_logs_" + Calendar.getInstance().getTime().toString() + ".csv";
        sdCard = Environment.getExternalStorageDirectory();
        directory = new File (sdCard.getAbsolutePath() + "/GPS Logger");
        if (!directory.exists())
        {
            directory.mkdirs();
        }
        try {
            file = new File(directory, fileName);
            if (!file.exists())
            {
                file.createNewFile();
            }
            fOut = new FileOutputStream(file, true);
            osw = new OutputStreamWriter(fOut);
        } catch (Exception e) {

        }
        appendToFile("LATITUDE", "LONGITUDE", "SPEED");
        startLocationUpdates();
    }

    public void stopLogging(View view) {
        mRequestingLocationUpdates = false;
        timer.cancel();
        try {
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stopLocationUpdates();
    }

    public void stopLocationUpdates() {
        mFusedLocationClient
                .removeLocationUpdates(mLocationCallback)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(getApplicationContext(), "Stopped Logging!!", Toast.LENGTH_SHORT).show();
                    }
                });
        toggle();
    }

    public void onViewMap(View view) {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        Bundle bundle = new Bundle();
        if(mCurrentLocation == null){
            bundle.putDouble("Lat", 0.0);
            bundle.putDouble("Long", 0.0);
        } else {
            bundle.putDouble("Lat", mCurrentLocation.getLatitude());
            bundle.putDouble("Long", mCurrentLocation.getLongitude());
        }intent.putExtra("latLong", bundle);
        startActivity(intent);
    }

    public void onResume() {
        super.onResume();
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mRequestingLocationUpdates) {
            stopLocationUpdates();
        }
    }
}
