package com.example.admin.a2018iotkorea;

import android.animation.ObjectAnimator;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.admin.a2018iotkorea.ble.AppController;
import com.example.admin.a2018iotkorea.ble.BluetoothLeService;
import com.example.admin.a2018iotkorea.ble.PolarBleService;
import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

public class MySensorData extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    InputStream myInputStream;
    OutputStream myOutputStream;

    boolean stopWorking;
    byte[] readBuffer;
    Thread workerThread;

    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    //Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;

    //Polar Sensor
    String TAG = "MySensorData";
    int hr=0,prr=0,rr=0;
    AppController app = AppController.getInstance();
    PolarBleService mPolarBleService;
    private boolean bleServiceConnectionBounded=false;
    TextView tv_heart;

    //Max value
    private int o3_max = 604;
    private int pm_max = 501;
    private int co_max = 51;
    private int so2_max = 1024;
    private int no2_max = 2049;
    private int tem_max = 200;
    private int aqi_max = 500;

    //Air Quality Data Textviews
    TextView text_o3;
    TextView text_co;
    TextView text_no2;
    TextView text_so2;
    TextView text_pm2_5;
    TextView text_temp;

    ProgressBar progress_o3;
    ProgressBar progress_co;
    ProgressBar progress_no2;
    ProgressBar progress_so2;
    ProgressBar progress_pm2_5;
    ProgressBar progress_temp;


    GpsInfo gps;
    private LatLng location;

    SharedPreferences sharedPreferences;

    public void getMYSensorData(String jsonObject, int type) {
        //0 if AQI Data
        if (type == 0) {
            getAIRData(jsonObject, 0);
        }
        //1 if Real Time Data
        else if (type == 1) {
            getAIRData(jsonObject, 1);
        }
        else if (type == 2) {
            sendHistoricalData(jsonObject);
        }
    }

    public void getAIRData(String jsonObject, int type ) {
        try {
            JSONObject myData = new JSONObject(jsonObject);
            JSONObject setData = new JSONObject(myData.get("data").toString());
            String bluetoothDeviceAddress = myData.getString("bd-addr");
            int o3 = setData.getInt("o3");
            int co = setData.getInt("co");
            int no2 = setData.getInt("no2");
            int so2 = setData.getInt("so2");
            int pm2_5 = setData.getInt("pm2_5");
            int time = setData.getInt("time");

            text_o3.setText(Integer.toString(o3));
            text_co.setText(Integer.toString(co));
            text_no2.setText(Integer.toString(no2));
            text_so2.setText(Integer.toString(so2));
            text_pm2_5.setText(Integer.toString(pm2_5));

            progressChanger(progress_o3, o3);
            progressChanger(progress_co, co);
            progressChanger(progress_no2, no2);
            progressChanger(progress_so2, so2);
            progressChanger(progress_pm2_5, pm2_5);

            //Send AQI data to the server
            JSONObject aqiJSON = new JSONObject();
            JSONObject locationJSON = new JSONObject();

            //Get user id
            int user_id = sharedPreferences.getInt("global_user_id", 0);

            // Get current location
            gps = new GpsInfo(MySensorData.this);
            if (gps.isGetLocation()) {  // GPS On, NN.XX
                location = new LatLng(gps.getLatitude(), gps.getLongitude());
                Log.d("test", location.toString());
            } else {
                gps.showSettingsAlert();     // GPS setting Alert
            }

            //Timestamp
            Date currTime = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String current_date = formatter.format(currTime);

            try {
                locationJSON.put("lat", location.latitude);
                locationJSON.put("lng", location.longitude);

                aqiJSON.put("user-id", user_id);
                aqiJSON.put("bd-addr", bluetoothDeviceAddress);
                aqiJSON.put("timestamp", current_date);
                aqiJSON.put("data", setData);
                aqiJSON.put("location", locationJSON);

                String str_aqiJson = aqiJSON.toString();

                if (type == 0) {
                    new UserManagementThread(MySensorData.this).execute(getString(R.string.sendAQIData), str_aqiJson);
                }
                else {
                    new UserManagementThread(MySensorData.this).execute(getString(R.string.sendRealTimeData), str_aqiJson);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void sendHistoricalData(String jsonObject){
        try {
            JSONObject myData = new JSONObject(jsonObject);
            JSONArray dataArray = new JSONArray(myData.get("data").toString());
            String bluetoothDeviceAddress = myData.getString("bd-addr");

            JSONArray toSendArray = new JSONArray();
            JSONObject historicalJSON = new JSONObject();

            //User-id
            int user_id = sharedPreferences.getInt("global_user_id", 0);

            //Loop through data JSONArray to create message array to send historical data
            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject eachJsonMessage = new JSONObject();

                //Timestamp
//                Date currTime = new Date();
//                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//                String current_date = formatter.format(currTime);

                //Data
                JSONObject setData = dataArray.getJSONObject(i);
                int eachTime = setData.getInt("time");
                setData.remove("time");

                Date currTime = new Date(eachTime*1000L);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String current_date = formatter.format(currTime);

                // Location
                gps = new GpsInfo(MySensorData.this);
                if (gps.isGetLocation()) {  // GPS On, NN.XX
                    location = new LatLng(gps.getLatitude(), gps.getLongitude());
                    Log.d("test", location.toString());
                } else {
                    gps.showSettingsAlert();     // GPS setting Alert
                }

                JSONObject locationJSON = new JSONObject();

                locationJSON.put("lat",location.latitude);
                locationJSON.put("lng", location.longitude);

                eachJsonMessage.put("timestamp", current_date);
                eachJsonMessage.put("data", setData);
                eachJsonMessage.put("location", locationJSON);

                toSendArray.put(eachJsonMessage);
            }

            historicalJSON.put("user-id", user_id);
            historicalJSON.put("bd-addr", bluetoothDeviceAddress);
            historicalJSON.put("message", toSendArray);

            String str_historicalJSON = historicalJSON.toString();

            new UserManagementThread(MySensorData.this).execute(getString(R.string.sendHistoricalData), str_historicalJSON);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_sensor_data);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Progress Bar
        progress_o3 = (ProgressBar) findViewById(R.id.progress_o3);
        progress_co = (ProgressBar) findViewById(R.id.progress_co);
        progress_no2 = (ProgressBar) findViewById(R.id.progress_no2);
        progress_so2 = (ProgressBar) findViewById(R.id.progress_so2);
        progress_pm2_5 = (ProgressBar) findViewById(R.id.progress_pm25);
//        progress_temp = (ProgressBar) findViewById(R.id.progress_temp);

        initProgress(progress_o3,aqi_max);
        initProgress(progress_co,aqi_max);
        initProgress(progress_no2,aqi_max);
        initProgress(progress_so2,aqi_max);
        initProgress(progress_pm2_5,aqi_max);
//        initProgress(progress_temp,aqi_max);


        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        //Insert bluetooth into activity
        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
        }

        //Polar Sensor
        tv_heart =(TextView)findViewById(R.id.heart);

        //Display AirQuality Data textview
        text_o3 = (TextView) findViewById(R.id.text_o3);
        text_co = (TextView) findViewById(R.id.text_co);
        text_no2 = (TextView) findViewById(R.id.text_no2);
        text_so2 = (TextView) findViewById(R.id.text_so2);
        text_pm2_5 = (TextView) findViewById(R.id.text_pm25);
//        text_temp = (TextView) findViewById(R.id.text_temp);


        locationPermissionCheck();

        gps = new GpsInfo(MySensorData.this);
        if (gps.isGetLocation()) {  // GPS On, NN.XX
            location = new LatLng(gps.getLatitude(), gps.getLongitude());
            Log.d("test", location.toString());
        } else {
            gps.showSettingsAlert();     // GPS setting Alert
        }

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
        getMenuInflater().inflate(R.menu.my_sensor_data, menu);
        return true;
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.Home) {
            // Handle the camera action
            Intent intent = new Intent(MySensorData.this, Home.class);
            startActivity(intent);
            finish();
        } else if (id == R.id.mySensor) {
            Intent intent = new Intent(MySensorData.this, MySensorData.class);
            startActivity(intent);
            finish();

        } else if (id == R.id.signOut) {
            SharedPreferences.Editor sharedPreferencesEditor = sharedPreferences.edit();
            sharedPreferencesEditor.remove("global_user_id");
            sharedPreferencesEditor.commit();

            Intent intent = new Intent(MySensorData.this, MainActivity.class);
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

    /////////////////////   Progressbar   /////////////////////
    private void initProgress(ProgressBar pb, int max) {
        pb.setMax(max);
        progressChanger(pb,0);
        pb.setScaleY(2.5f);
    }

    private void progressChanger(ProgressBar pb, int value){
        int lastProgress = pb.getProgress();
        selectProgressColor(pb,value);
        pb.setProgress(value);
        ObjectAnimator animation = ObjectAnimator.ofInt(pb, "progress",lastProgress, value);
        animation.setDuration(500);
        animation.setInterpolator(new DecelerateInterpolator());
        animation.start();

    }

    /////////////////////   AQI Color   /////////////////////
    private void selectProgressColor(ProgressBar pb,int aqi_value){
        if(aqi_value > 300)          pb.getProgressDrawable().setColorFilter(Color.rgb(139,00,00),PorterDuff.Mode.SRC_IN);
        else if(aqi_value > 200)    pb.getProgressDrawable().setColorFilter(Color.rgb(255,20,93),PorterDuff.Mode.SRC_IN);
        else if(aqi_value > 150)    pb.getProgressDrawable().setColorFilter(Color.rgb(255,0,0),PorterDuff.Mode.SRC_IN);
        else if(aqi_value > 100)    pb.getProgressDrawable().setColorFilter(Color.rgb(255,140,0),PorterDuff.Mode.SRC_IN);
        else if(aqi_value > 50)     pb.getProgressDrawable().setColorFilter(Color.rgb(255,215,0),PorterDuff.Mode.SRC_IN);
        else                        pb.getProgressDrawable().setColorFilter(Color.rgb(50,205,50),PorterDuff.Mode.SRC_IN);
    }

    public void locationPermissionCheck() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    0);
        }
    }
    /////////////////////   Polar Sensor   /////////////////////
    @Override
    protected void onResume() {
        super.onResume();
        if(app.polarBleDeviceServiceConnected && app.polarBleDeviceAddress == null){
            deactivatePolar();
        }else if(app.polarBleDeviceAddress != null && !app.polarBleDeviceServiceConnected){
            activatePolar();
        }
    }

    protected void activatePolar() {
        Log.w(this.getClass().getName(), "activatePolar()");
        if (!app.polarBleDeviceServiceConnected && app.polarBleDeviceAddress!=null && app.polarBleDeviceAddress.length()>1){
            Intent gattactivateClickerServiceIntent = new Intent(this, PolarBleService.class);
            bindService(gattactivateClickerServiceIntent, mPolarBleServiceConnection, BIND_AUTO_CREATE);
        }
        registerReceiver(mPolarBleUpdateReceiver, makePolarGattUpdateIntentFilter());
    }

    protected void deactivatePolar() {
        Log.w(this.getClass().getName(), "deactivatePolar()");
        if(app.polarBleDeviceServiceConnected){
            if(mPolarBleService!=null)
                unbindService(mPolarBleServiceConnection);
        }
        app.bleDeviceServiceConnected=false;

        if(bleServiceConnectionBounded)
            unregisterReceiver(mPolarBleUpdateReceiver);
        bleServiceConnectionBounded=false;
    }

    private final BroadcastReceiver mPolarBleUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context ctx, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
            } else if (PolarBleService.ACTION_HR_DATA_AVAILABLE.equals(action)) {
                String data = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);
                StringTokenizer tokens = new StringTokenizer(data, ";");
                hr = Integer.parseInt(tokens.nextToken());
                prr = Integer.parseInt(tokens.nextToken());
                rr = Integer.parseInt(tokens.nextToken());

                Log.w(TAG, "####Received HR: " +hr+" RR: "+rr+" pRR: "+prr);
                tv_heart.setText("HR:"+hr+" RR:"+rr +" pRR:"+prr);

                //////////////// Send Heart Sensor Data to Server /////////////////////////
                JSONObject heartsession = new JSONObject();
                JSONObject heartData = new JSONObject();
                JSONObject locationData = new JSONObject();

                // Get current location
                gps = new GpsInfo(MySensorData.this);
                if (gps.isGetLocation()) {  // GPS On, NN.XX
                    location = new LatLng(gps.getLatitude(), gps.getLongitude());
                    Log.d("test", location.toString());
                } else {
                    gps.showSettingsAlert();     // GPS setting Alert
                }

                //Get user id
                int user_id = sharedPreferences.getInt("global_user_id", 0);

                Date currTime = new Date();
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String current_date = formatter.format(currTime);

                try {

                    heartData.put("heart-rate", hr);
                    heartData.put("rr-interval", rr);

                    locationData.put("lat", location.latitude);
                    locationData.put("lng", location.longitude);

                    heartsession.put("user-id", user_id);
                    heartsession.put("bd-addr", mPolarBleService.getmBluetoothDeviceAddress());
                    heartsession.put("data", heartData);
                    heartsession.put("location", locationData);
                    heartsession.put("timestamp", current_date);

                    String str_heartsession = heartsession.toString();

                    new UserManagementThread(MySensorData.this).execute(getString(R.string.heartRateData), str_heartsession);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };

    private final ServiceConnection mPolarBleServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mPolarBleService = ((PolarBleService.LocalBinder) service).getService();
            if (!mPolarBleService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mPolarBleService.connect(app.polarBleDeviceAddress);
            app.polarBleDeviceServiceConnected = true;
        }
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mPolarBleService = null;
        }
    };

    private static IntentFilter makePolarGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(PolarBleService.ACTION_HR_DATA_AVAILABLE);
        return intentFilter;
    }


}
