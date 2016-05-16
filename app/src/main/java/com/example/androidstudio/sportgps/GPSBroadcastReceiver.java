package com.example.androidstudio.sportgps;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Config;
import android.util.Log;
import android.widget.RemoteViews;

import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * Created by AndroidStudio on 15.05.2016.
 */
public class GPSBroadcastReceiver extends BroadcastReceiver implements LocationListener {
    private static LocationManager locationManager;
    private static String provider;
    private Context context;
    private static List<Location> points;
    private static List<List<LatLng>> wayPointList;
    private static StopWatch stopWatch;
    private static StopWatch gpsWorkingTime;
    private static boolean workingGPS = false;
    private static float totalDistance;
    private static float currentWayDistance;
    private static float cResetDistance;
    private static float currentSpeed;
    private static List<LatLng> cResetPoints;
    private static Boolean activityActivated;
    private static NotificationManager mNotificationManager;
    private static Uri gpxFile;
    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;
        Bundle bundle1 = intent.getExtras();
        if (bundle1 != null) {
            String cmd = bundle1.getString("GPS_COMMAND");
            if(cmd != null) command(cmd);
        }
    }

    public void command(String cmd) {
        String cmdLowerCase = cmd.toLowerCase();
        Intent intent = new Intent("com.ee.GPSBroadcastRequest");
        if(cmdLowerCase.equals("start")) {
            if (!workingGPS) {
                reset();
                startGPS();
                if(workingGPS){
                    intent.putExtra("RESULT_TYPE", "GPS_STARTED");
                    context.sendBroadcast(intent);
                }
            }
            else{
                intent.putExtra("RESULT_TYPE", "GPS_STARTED_ALREADY");
                context.sendBroadcast(intent);
            }
        }
        else if(cmdLowerCase.equals("stop")){
            stopGPS();
        }
        else if(cmdLowerCase.equals("reset")){
            reset();
        }
        else if(cmdLowerCase.equals("add_way")){
            if(points.size() != 0){
                currentWayDistance = 0;
                List<LatLng> list = new ArrayList<LatLng>();
                Location location = points.get(points.size() - 1);
                list.add(new LatLng(location.getLatitude(),location.getLongitude()));
                wayPointList.add(list);
                sendStatisticResult(false);
            }
        }
        else if (cmdLowerCase.equals("c_reset")){
            cReset();
            sendStatisticResult(false);
        }
        else if(cmdLowerCase.equals("give_points")){
            PointsParcelable pointsParcelable = new PointsParcelable();
            pointsParcelable.totalPointList = new ArrayList<LatLng>();
            for (Location location:points) {
                pointsParcelable.totalPointList.add(new LatLng(location.getLatitude(),location.getLongitude()));
            }
            pointsParcelable.wayPointList = new ArrayList<LatLng>();
            for (List<LatLng> wayPoints: wayPointList) {
                pointsParcelable.wayPointList.add(wayPoints.get(0));
            }
            pointsParcelable.cResetPointList = cResetPoints;
            intent.putExtra("RESULT_TYPE", "GPS_GET_POINTS");
            intent.putExtra("DATA", pointsParcelable);
            context.sendBroadcast(intent);
        } else if(cmdLowerCase.equals("give_statistic")){
            sendStatisticResult(false);
        } else if(cmdLowerCase.equals("activity_destroyed")){
            activityDestroyed();
        }
        else if(cmdLowerCase.equals("activity_created")){
            activityCreated();
        }else if(cmdLowerCase.equals("gpx_share")){
            if(gpxFile == null) saveGPX();
            if(gpxFile != null){
                intent.putExtra("FILE_PATH", gpxFile.getPath());
                intent.putExtra("RESULT_TYPE", "SHARE_GPX");
                context.sendBroadcast(intent);
            }
        }
    }

    private void saveGPX(){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?><gpx xmlns=\"http://www.topografix.com/GPX/1/1\" creator=\"MapSource 6.15.5\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"  xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\"><trk>\n";
        String name = "<name>" + df.format(new Date()) + "</name><trkseg>\n";

        String segments = "";
        DateFormat dfFile = new SimpleDateFormat("dd_MM_yyyy_HH_mm_ss");
        for (Location l : points) {
            segments += "<trkpt lat=\"" + l.getLatitude() + "\" lon=\"" + l.getLongitude() + "\"><time>" + df.format(new Date(l.getTime())) + "</time></trkpt>\n";
        }

        String footer = "</trkseg></trk></gpx>";

        File outputDir = context.getCacheDir();
        try {
            File outputFile = File.createTempFile(df.format(new Date()), "gpx", outputDir);
            FileWriter writer = new FileWriter(outputFile, false);
            writer.append(header);
            writer.append(name);
            writer.append(segments);
            writer.append(footer);
            writer.flush();
            writer.close();
            gpxFile = Uri.fromFile(outputFile);
            Log.i(this.getClass().getName(), "Saved " + points.size() + " points.");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(this.getClass().getName(), "Error Writting Path",e);
        }
    }

    private void activityDestroyed(){
        activityActivated = false;
        if(workingGPS) showNotification();
    }

    private void activityCreated(){
        activityActivated = true;
        if(mNotificationManager != null){
            mNotificationManager.cancel(0);
            mNotificationManager = null;
        }
        if(workingGPS){
            Intent intent = new Intent("com.ee.GPSBroadcastRequest");
            intent.putExtra("RESULT_TYPE", "GPS_WORKING");
            context.sendBroadcast(intent);
        }
    }

    private void updateNotification(){
        showNotification();

    }

    private void showNotification(){
        mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // get the view layout
        RemoteViews remoteView = new RemoteViews(
                context.getPackageName(), R.layout.notification);

        // define intents
        Intent intent = new Intent("com.ee.GPSBroadcastRequest");
        intent.putExtra("GPS_COMMAND", "ADD_WAY");
        PendingIntent pIntentAddWaypoint = PendingIntent.getBroadcast(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        Intent intent1 = new Intent("com.ee.GPSBroadcastRequest");
        intent1.putExtra("GPS_COMMAND", "C_RESET");
        PendingIntent pIntentResetTripmeter = PendingIntent.getBroadcast(
                context,
                2,
                intent1,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
        PendingIntent pIntentOpenActivity = PendingIntent.getActivity(
                context,
                0,
                new Intent(context, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        // attach events
        remoteView.setOnClickPendingIntent(R.id.buttonAddWayPoint, pIntentAddWaypoint);
        remoteView.setOnClickPendingIntent(R.id.buttonResetTripmeter, pIntentResetTripmeter);
        remoteView.setOnClickPendingIntent(R.id.buttonOpenActivity, pIntentOpenActivity);

        remoteView.setTextViewText(R.id.textViewTripmeter, String.valueOf((int)cResetDistance));
        remoteView.setTextViewText(R.id.textViewWayPointMeter, String.valueOf((int)currentWayDistance));
        // build notification
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setContent(remoteView)
                        .setSmallIcon(R.drawable.ic_my_location_white_48dp);

        mBuilder.setOngoing(true);
        // notify

        mNotificationManager.notify(0, mBuilder.build());
    }

    private void sendStatisticResult(boolean changedLocation){
        Intent i = new Intent("com.ee.GPSBroadcastRequest");
        i.putExtra("STATISTIC_DATA", getStatisticResult());
        if(changedLocation){
            i.putExtra("RESULT_TYPE", "LOCATION_CHANGED");
        }
        else{
            i.putExtra("RESULT_TYPE", "STATISTIC");
        }
        context.sendBroadcast(i);
    }



    public void cReset(){
        cResetPoints = new ArrayList<LatLng>();
        cResetDistance = 0;
    }

    public void reset(){
        gpsWorkingTime = new StopWatch();
        gpsWorkingTime.start();
        totalDistance = 0;
        points = new ArrayList<Location>();
        wayPointList = new ArrayList<List<LatLng>>();
        cReset();
    }

    public void startGPS() {
        stopWatch = new StopWatch();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getName(), "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getName(), "No FINE location permissions!");
        }
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager != null){
            Criteria criteria = new Criteria();
            provider = locationManager.getBestProvider(criteria, false);
            if(provider != null){
                locationManager.requestLocationUpdates(provider, 500, 1, this);
                workingGPS = true;
                gpsWorkingTime = new StopWatch();
                gpsWorkingTime.start();
                Log.d(this.getClass().getName(), "GPS STARTED!");
            }
        }
    }

    public void stopGPS() {
        stopWatch.stop();
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getName(), "No COARSE location permissions!");
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(this.getClass().getName(), "No FINE location permissions!");
        }

        if (locationManager!=null){
            locationManager.removeUpdates(this);
        }
        if(gpsWorkingTime != null){
            gpsWorkingTime.stop();
        }
        workingGPS = false;
        if(activityActivated){
            Intent intent = new Intent("com.ee.GPSBroadcastRequest");
            intent.putExtra("RESULT_TYPE", "GPS_STOPPED");
            context.sendBroadcast(intent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if(!workingGPS) return;
        LatLng newPoint = new LatLng(location.getLatitude(), location.getLongitude());
        if(points.size() != 0){
            Location l1 = points.get(points.size()-1);
            LatLng point = new LatLng(l1.getLatitude(),l1.getLongitude());
            float distance = getDistance(point, newPoint);
            totalDistance += distance;
            if(stopWatch != null){
                stopWatch.stop();
                float speed = (distance/((float)stopWatch.getTotalTimeMillis()/1000));
                location.setSpeed(speed);
                if(cResetPoints.size() != 0){
                    cResetDistance += distance;
                }
            }
        }
        currentSpeed = location.getSpeed();
        int currentWayPointSize = 0;
        if(wayPointList.size() != 0){
            List<LatLng> wayPoint = wayPointList.get(wayPointList.size() - 1);
            currentWayPointSize = wayPoint.size();
            float distance = getDistance(wayPoint.get(wayPoint.size()-1), newPoint);
            currentWayDistance += distance;
            wayPoint.add(newPoint);
        }
        points.add(location);
        cResetPoints.add(newPoint);
        if(activityActivated){
            sendStatisticResult(true);
        }
        else{
            updateNotification();
        }
        stopWatch.start();
    }

    private StatisticParcelable getStatisticResult(){
        StatisticParcelable statistic = new StatisticParcelable();
        statistic.cResetDistance = (int)cResetDistance;
        if(cResetPoints.size() < 2){
            statistic.cResetLines = 0;
        }
        else {
            statistic.cResetLines = cResetPoints.size() - 1;
        }
        statistic.wayPointDistance = (int)currentWayDistance;
        statistic.wayPointLines = 0;
        if(wayPointList.size() != 0){
            List<LatLng> wayPoint = wayPointList.get(wayPointList.size() - 1);
            if(wayPoint.size() < 2){
                statistic.wayPointLines = 0;
            }
            else{
                statistic.wayPointLines = wayPoint.size() - 1;
            }
        }
        statistic.totalDistance = (int)totalDistance;
        if(points.size() < 2){
            statistic.totalLines = 0;
        }
        else{
            statistic.totalLines = points.size() - 1;
        }
        if(points.size() != 0){
            Location location = points.get(points.size() - 1);
            statistic.currentPoint = new LatLng(location.getLatitude(),location.getLongitude());
        }
        statistic.currentSpeed = (int)(currentSpeed / 1000 * 3600);
        statistic.wayPointCount = wayPointList.size();
        statistic.timeSeconds = 0;
        if(gpsWorkingTime != null)statistic.timeSeconds = gpsWorkingTime.getTotalTimeSeconds();
        return statistic;
    }

    public float getDistance(LatLng latLngA, LatLng latLngB){
        double earthRadius = 3958.75;
        double latDiff = Math.toRadians(latLngB.latitude-latLngA.latitude);
        double lngDiff = Math.toRadians(latLngB.longitude-latLngA.longitude);
        double a = Math.sin(latDiff /2) * Math.sin(latDiff /2) +
                Math.cos(Math.toRadians(latLngA.latitude)) * Math.cos(Math.toRadians(latLngB.latitude)) *
                        Math.sin(lngDiff /2) * Math.sin(lngDiff /2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double distance = earthRadius * c;
        int meterConversion = 1609;
        return new Float(distance * meterConversion).floatValue();
    }


    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }
}
