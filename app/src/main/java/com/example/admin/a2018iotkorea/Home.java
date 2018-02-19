package com.example.admin.a2018iotkorea;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

public class Home extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        OnMapReadyCallback, GoogleMap.OnCameraIdleListener{

    // Intent request codes
//    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
//    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
//    private static final int REQUEST_ENABLE_BT = 3;


    //Local Bluetooth adapter
//    private BluetoothAdapter mBluetoothAdapter = null;

    private GoogleMap mMap;
    private String locationName;
    private LatLng location;
    GpsInfo gps;

    SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        //Get location permission
        locationPermissionCheck();
//        LatLngBounds bounds= mMap.getProjection().getVisibleRegion().latLngBounds;
//        LatLng north = bounds.northeast;
//        LatLng south = bounds.southwest;

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.mMap);
        mapFragment.getMapAsync(this);


        //Google Search Bar
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);
        ((EditText)findViewById(R.id.place_autocomplete_search_input)).setTextSize(14.0f);
        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place
                locationName = String.valueOf(place.getName());
                location = place.getLatLng();

                locationChange();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error
            }
        });

//        locationChange();

        sharedPreferences = getSharedPreferences("global_user_id", 0);

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);

        return true;
    }
/*
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.secure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(Home.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            }
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(Home.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                return true;
            }
        }


        return super.onOptionsItemSelected(item);
    }

    private void ensureDiscoverable() {
        if (mBluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }
*/
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here
        int id = item.getItemId();
//TODO WORK HERE
        if (id == R.id.Home) {
            // Handle the camera action
            Intent intent = new Intent(Home.this, Home.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.mySensor) {
            Intent intent = new Intent(Home.this, MySensorData.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.signOut) {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.remove("global_user_id");
            sharedPreferencesEditor.commit();

            Intent intent = new Intent(Home.this, MainActivity.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);

        return true;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
 //       markerInitialize();
        mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle_retro));
        gps = new GpsInfo(Home.this);
        if (gps.isGetLocation()) {  // GPS On, NN.XX
            location = new LatLng(gps.getLatitude(), gps.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder().target(location).zoom(17).build();
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        } else {
            gps.showSettingsAlert();     // GPS setting Alert
        }
        mMap.setOnCameraIdleListener(this);
    }

    public void markerInitialize(){
        mMap.clear();
        mMap.addMarker(new MarkerOptions().position(location).title(locationName));
        CameraPosition cameraPosition = new CameraPosition.Builder().target(location).zoom(14).build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

        //UserManageMentThread - send northeast and southwest
        //REsponse should be a jsonObject air quality data
        //show aqi data of the area clicked
        //
        //get northeast and southwest points based on destination -can determine zoom level

////        or
//        if(mapMarker != null)        mapMarker.remove();
//        mapMarker = mMap.addMarker(new MarkerOptions().position(location).title(locationName));
    }
    public void locationChange(){
        markerInitialize();
    }

    public void locationPermissionCheck() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }

    @Override
    public void onCameraIdle() {
        LatLngBounds bounds = mMap.getProjection().getVisibleRegion().latLngBounds;
        LatLng southwest = bounds.southwest;
        LatLng northeast = bounds.northeast;

        JSONObject body = new JSONObject();

        try {
            body.put("south", Double.toString(southwest.latitude));
            body.put("west", Double.toString(southwest.longitude));
            body.put("north", Double.toString(northeast.latitude));
            body.put("east", Double.toString(northeast.longitude));
            String str_body = body.toString();

            //Get the result from sending user email&password to the server
            String result = null;
            try {
                result = new UserManagementThread(Home.this).execute(getString(R.string.getMapAirData), str_body).get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }


            JSONObject jResult = new JSONObject(result);
            if (jResult.getBoolean("status")) {
                JSONArray airDataSet = jResult.getJSONArray("message");

                for( int i =0; i<airDataSet.length(); i++){
                    JSONObject airData = airDataSet.getJSONObject(i);
                    String infoString =
                            "O3: " + airData.getDouble("O3") +"\n"+
                            "CO: " + airData.getDouble("CO") +"\n"+
                            "NO2: " + airData.getDouble("NO2") +"\n"+
                            "SO2: " + airData.getDouble("SO2") +"\n"+
                            "PM 2.5: " + airData.getDouble("PM") +"\n";
                    mMap.addMarker(new MarkerOptions()
                            .position(new LatLng(airData.getDouble("lat"), airData.getDouble("lng")))
                            .title("Air Quality Index")
                            .snippet(infoString));

                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }
}
