package com.example.androidstudio.sportgps;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private IntentFilter filter1;

    private GoogleMap mGoogleMap;
    private static int mGoogleMapType = -1;
    private TextView tvCResetDistance;
    private TextView tvCResetLine;
    private TextView tvWayPointDistance;
    private TextView tvWayPointLine;
    private TextView tvTotalDistance;
    private TextView tvTotalLine;
    private TextView tvWayPointCount;
    private TextView tvSpeed;
    private TextView tvTime;
    private Timer timer;
    private long currentTimeSeconds;
    private Button btnAddWayPoint;
    private Button btnCounterReset;
    private List<LatLng> points;
    private int markerCount = 0;
    private Polyline mPolyline;
    private PolylineOptions mPolylineOptions;
    private boolean locationFocused = true;
    private boolean locationChanging = true;
    private MenuItem mItemStart;
    private MenuItem mItemReset;
    private MenuItem mItemStop;
    private MenuItem mItemMapFitTrack;
    private MenuItem mItemMapTypeNormal;
    private MenuItem mItemMapTypeHybrid;
    private MenuItem mItemMapTypeNone;
    private MenuItem mItemMapTypeSatelLite;
    private MenuItem mItemMapTypeTerrain;
    private MenuItem mItemShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        tvCResetDistance = (TextView) findViewById(R.id.textview_creset_distance);
        tvCResetLine = (TextView) findViewById(R.id.textview_creset_line);
        tvWayPointDistance = (TextView) findViewById(R.id.textview_wp_distance);
        tvWayPointLine = (TextView) findViewById(R.id.textview_wp_line);
        tvTotalDistance = (TextView) findViewById(R.id.textview_total_distance);
        tvTotalLine = (TextView) findViewById(R.id.textview_total_line);
        tvWayPointCount = (TextView) findViewById(R.id.textview_wpcount);
        tvSpeed = (TextView) findViewById(R.id.textview_speed);
        tvTime = (TextView) findViewById(R.id.textview_time);
        btnAddWayPoint = (Button) findViewById(R.id.buttonAddWayPoint);
        btnCounterReset = (Button) findViewById(R.id.buttonCounterReset);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        filter1 = new IntentFilter("com.ee.GPSBroadcastRequest");
        registerReceiver(myReceiver, filter1);
        timer = new Timer();
        points = new ArrayList<LatLng>();
        sendGPSBroadcast("activity_created");

    }

    private void startGPS() {
        sendGPSBroadcastOrder("start");
        currentTimeSeconds = 0;
    }

    private void reset(){
        currentTimeSeconds = 0;
        points = new ArrayList<LatLng>();
        sendGPSBroadcastOrder("reset");
        mapInit();
    }

    private void stopGPS() {
        timer.cancel();
        sendGPSBroadcastOrder("stop");
    }

    private void startTime() {
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                currentTimeSeconds++;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        tvTime.setText(getTimeString(currentTimeSeconds));
                    }
                });
            }
        }, 0, 1000);
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("com.ee.GPSBroadcastRequest")) {
                String resultType = intent.getStringExtra("RESULT_TYPE");
                if (resultType != null) {
                    if (resultType.equals("STATISTIC")) {
                        StatisticParcelable statisticParcelable = (StatisticParcelable) intent.getParcelableExtra("STATISTIC_DATA");
                        if (statisticParcelable != null) {
                            updateStatistic(false,statisticParcelable);
                        }
                        onUpdateLocation();
                    } else if (resultType.equals("GPS_STARTED")) {
                        setButtonEnabled(true);
                        sendGPSBroadcast("give_statistic");
                        startTime();
                    } else if (resultType.equals("GPS_GET_POINTS")) {
                        PointsParcelable pointsParcelable = (PointsParcelable) intent.getParcelableExtra("DATA");
                        if (pointsParcelable != null) {
                            restoreMapData(pointsParcelable);
                        }
                    } else if (resultType.equals("GPS_STARTED_ALREADY")) {
                        setButtonEnabled(true);
                        sendGPSBroadcast("give_points");
                        sendGPSBroadcast("give_statistic");
                    } else if (resultType.equals("LOCATION_CHANGED")) {
                        StatisticParcelable statisticParcelable = (StatisticParcelable) intent.getParcelableExtra("STATISTIC_DATA");
                        if (statisticParcelable != null) {
                            updateStatistic(true,statisticParcelable);
                        }
                    } else if (resultType.equals("GPS_STOPPED")) {
                        StatisticParcelable statisticParcelable = (StatisticParcelable) intent.getParcelableExtra("STATISTIC_DATA");
                        setButtonEnabled(false);
                    } else if (resultType.equals("SHARE_GPX")) {
                        Intent sendIntent = new Intent();
                        sendIntent.setAction(Intent.ACTION_SEND);

                        sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(intent.getStringExtra("FILE_PATH"))));
                        sendIntent.setType("text/plain");
                        try {
                            startActivity(Intent.createChooser(sendIntent, "Send gpx"));
                        } catch (android.content.ActivityNotFoundException ex) {
                            Toast.makeText(MainActivity.this, "Share is not supported.", Toast.LENGTH_SHORT).show();
                        }
                    }else if (resultType.equals("GPS_WORKING")) {
                        setButtonEnabled(true);
                        sendGPSBroadcast("give_points");
                        sendGPSBroadcast("give_statistic");
                    }
                }
            }
        }
    };

    private void setButtonEnabled(boolean startedGPS){
        mItemStart.setVisible(!startedGPS);
        mItemStop.setVisible(startedGPS);
        mItemReset.setVisible(startedGPS);
        btnAddWayPoint.setEnabled(startedGPS);
        btnCounterReset.setEnabled(startedGPS);
        mItemShare.setEnabled(!startedGPS && points.size() > 1);
    }

    private void restoreMapData(PointsParcelable pointsParcelable){
        points = pointsParcelable.totalPointList;
        if (!checkReady()) return;
        mapInit();
        markerCount = 0;
        for (LatLng point:pointsParcelable.wayPointList) {
            markerCount++;
            mGoogleMap.addMarker(new MarkerOptions().position(point).title(Integer.toString(markerCount)));
        }
        onUpdateLocation();
    }

    private void updateStatistic(Boolean locationChanged, StatisticParcelable data){
        tvCResetDistance.setText(String.valueOf(data.cResetDistance));
        tvCResetLine.setText(String.valueOf(data.cResetLines));
        tvWayPointDistance.setText(String.valueOf(data.wayPointDistance));
        tvWayPointLine.setText(String.valueOf(data.wayPointLines));
        tvTotalDistance.setText(String.valueOf(data.totalDistance));
        tvTotalLine.setText(String.valueOf(data.totalLines));
        tvWayPointCount.setText(String.valueOf(data.wayPointCount));
        tvSpeed.setText(String.format("%s km/h", data.currentSpeed));
        currentTimeSeconds = data.timeSeconds;
        tvTime.setText(getTimeString(currentTimeSeconds));
        if(locationChanged){
            if (points.size() != 0) {
                if (!data.currentPoint.equals(points.get(points.size() - 1))) {
                    points.add(data.currentPoint);
                }
            }
            else{
                points.add(data.currentPoint);
            }
        }
        if(mItemMapFitTrack != null) mItemMapFitTrack.setEnabled(points.size() != 0);
        onUpdateLocation();
    }

    private void onUpdateLocation() {
        if (!checkReady()) return;
        if (points.size() != 0) {
            LatLng currentPoint = points.get(points.size() - 1);
            if(locationFocused){
                locationChanging = true;
                mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(currentPoint));
            }
            mPolyline.setPoints(points);
        }
    }


    private String getTimeString(long seconds) {
        long min = seconds / 60;
        long sec = seconds - (min * 60);
        String minStr = String.valueOf(min);
        if (min < 10) minStr = String.format("0%s", minStr);
        String secStr = String.valueOf(sec);
        if (sec < 10) secStr = String.format("0%s", secStr);
        return String.format("%s:%s", minStr, secStr);

    }

    @Override
    protected void onDestroy() {
        sendGPSBroadcast("activity_destroyed");
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        mItemStart = (MenuItem)menu.findItem(R.id.action_start);
        mItemStop = (MenuItem)menu.findItem(R.id.action_stop);
        mItemReset = (MenuItem)menu.findItem(R.id.action_reset);
        mItemMapFitTrack = (MenuItem) menu.findItem(R.id.menu_map_zoom_fittrack);


        mItemMapTypeNormal = (MenuItem) menu.findItem(R.id.menu_map_type_normal);
        mItemMapTypeHybrid = (MenuItem) menu.findItem(R.id.menu_map_type_hybrid);
        mItemMapTypeNone = (MenuItem) menu.findItem(R.id.menu_map_type_none);
        mItemMapTypeSatelLite = (MenuItem) menu.findItem(R.id.menu_map_type_satellite);
        mItemMapTypeTerrain = (MenuItem) menu.findItem(R.id.menu_map_type_terrain);
        mItemShare = (MenuItem)menu.findItem(R.id.action_share);
        mItemShare.setEnabled(false);
        mItemStop.setVisible(false);
        mItemReset.setVisible(false);
        mItemMapFitTrack.setEnabled(false);
        checkMapType();
        updateMapType();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (item.getItemId()) {
            case R.id.action_start:
                startGPS();
                return true;
            case R.id.action_stop:
                stopGPS();
                return true;
            case R.id.action_reset:
                reset();
                return true;
            case R.id.menu_map_zoom_fittrack:
                locationFocused = false;
                updateMapZoomFitTrack();
                return true;
            case R.id.action_share:
                sendGPSBroadcast("gpx_share");
                return true;
            case R.id.menu_map_type_hybrid:
            case R.id.menu_map_type_none:
            case R.id.menu_map_type_normal:
            case R.id.menu_map_type_satellite:
            case R.id.menu_map_type_terrain:
                item.setChecked(true);
                changeMapTypeId();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void changeMapTypeId(){
        if (mItemMapTypeNormal.isChecked()) {
            mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL;
        } else if (mItemMapTypeHybrid.isChecked()) {
            mGoogleMapType = GoogleMap.MAP_TYPE_HYBRID;
        } else if (mItemMapTypeNone.isChecked()) {
            mGoogleMapType = GoogleMap.MAP_TYPE_NONE;
        } else if (mItemMapTypeSatelLite.isChecked()) {
            mGoogleMapType = GoogleMap.MAP_TYPE_SATELLITE;
        } else if (mItemMapTypeTerrain.isChecked()) {
            mGoogleMapType = GoogleMap.MAP_TYPE_TERRAIN;
        }
        updateMapType();
    }

    private void updateMapType() {
        if (!checkReady()) {
            return;
        }
        mGoogleMap.setMapType(mGoogleMapType);
    }

    private void checkMapType(){
        if(mGoogleMapType == GoogleMap.MAP_TYPE_NORMAL){
            mItemMapTypeNormal.setChecked(true);
        }
        else if(mGoogleMapType == GoogleMap.MAP_TYPE_HYBRID){
            mItemMapTypeHybrid.setChecked(true);
        }
        else if(mGoogleMapType == GoogleMap.MAP_TYPE_NONE){
            mItemMapTypeNone.setChecked(true);
        }
        else if(mGoogleMapType == GoogleMap.MAP_TYPE_SATELLITE){
            mItemMapTypeSatelLite.setChecked(true);
        }
        else if(mGoogleMapType == GoogleMap.MAP_TYPE_TERRAIN){
            mItemMapTypeTerrain.setChecked(true);
        }
        else{
            mGoogleMapType = GoogleMap.MAP_TYPE_NORMAL;
            mItemMapTypeNormal.setChecked(true);
        }
    }

    public void buttonAddWayPointClicked(View view) {
        sendGPSBroadcast("ADD_WAY");
        if(points.size() != 0){
            markerCount++;
            mGoogleMap.addMarker(new MarkerOptions().position(points.get(points.size()-1)).title(Integer.toString(markerCount)));
        }
    }

    public void buttonCResetClicked(View view) {
        sendGPSBroadcast("C_RESET");
    }


    private void sendGPSBroadcast(String command) {
        Intent intent = new Intent();
        intent.setAction("com.ee.GPSBroadcastRequest");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        if (command != null) {
            intent.putExtra("GPS_COMMAND", command);
        }
        sendBroadcast(intent);
    }

    private void sendGPSBroadcastOrder(String command) {
        Intent intent = new Intent();
        intent.setAction("com.ee.GPSBroadcastRequest");
        intent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        if (command != null) {
            intent.putExtra("GPS_COMMAND", command);
        }
        sendOrderedBroadcast(intent, null, myReceiver, null, Activity.RESULT_OK, null, null);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {
                locationFocused = true;
                onUpdateLocation();
                return true;
            }
        });
        mGoogleMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition cameraPosition) {
                if (locationChanging) {
                    locationChanging = false;
                } else {
                    locationFocused = false;
                }
            }
        });
        mapInit();
    }

    public void mapInit(){
        if(!checkReady()) return;
        updateMapType();
        mGoogleMap.clear();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        mGoogleMap.setMyLocationEnabled(true);
        mGoogleMap.moveCamera(CameraUpdateFactory.zoomTo(17));
        if(points.size() != 0){
            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(points.get(points.size()-1)));
        }
        updateTrackPosition();
    }

    private boolean checkReady() {
        if (mGoogleMap == null) {
            //Toast.makeText(this, R.string.map_not_ready, Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void updateMapZoomFitTrack(){
        if (mPolyline==null){
            return;
        }

        List<LatLng> points = mPolyline.getPoints();

        if (points.size()<=1){
            return;
        }
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : points) {
            builder.include(point);
        }
        LatLngBounds bounds = builder.build();
        int padding = 0; // offset from edges of the map in pixels
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding));

    }

    private void updateTrackPosition(){
        if (!checkReady()) {
            return;
        }
        mPolylineOptions = new PolylineOptions().width(10).color(Color.BLUE);
        mPolyline = mGoogleMap.addPolyline(mPolylineOptions);


    }
}
