package com.example.sample2;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;

import androidx.fragment.app.FragmentActivity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback
{

    private GoogleMap mMap;
    private double latitude;
    private double longitude;
    ArrayList<String> latitudeList = new ArrayList<String>();
    ArrayList<String> longitudeList = new ArrayList<String>();

    LatLng initialLocation;
    private static final int READ_REQUEST_CODE = 42;

    @Override
    public void onActivityResult(int requestCode, int resultCode,
                                 Intent resultData) {

        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            uri = resultData.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                try {
                    String csvLine;
                    while ((csvLine = reader.readLine()) != null) {
                        String[] row = csvLine.split(",");
                        latitudeList.add(row[0]);
                        longitudeList.add(row[1]);
                    }
                    plotMap();
                }
                catch (IOException ex) {
                    throw new RuntimeException("Error in reading CSV file: "+ex);
                }
                finally {
                    try {
                        inputStream.close();
                    }
                    catch (IOException e) {
                        throw new RuntimeException("Error while closing input stream: "+e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "The specified file was not correct", Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras().getBundle("latLong");
        latitude = bundle.getDouble("Lat");
        longitude = bundle.getDouble("Long");

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    private void plotMap() {
        for(int i = 1; i < latitudeList.size() ; i++) {
            mMap.addMarker(new MarkerOptions().position(new LatLng(Double.valueOf(latitudeList.get(i)), Double.valueOf(longitudeList.get(i)))));
        }
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        for (int i = 1; i < latitudeList.size(); ++i)
            boundsBuilder.include(new LatLng(Double.valueOf(latitudeList.get(i)), Double.valueOf(longitudeList.get(i))));
        LatLngBounds latLngBounds = boundsBuilder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 100));
    }

}