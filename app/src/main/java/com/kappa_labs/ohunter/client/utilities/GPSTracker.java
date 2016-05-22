package com.kappa_labs.ohunter.client.utilities;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;

/**
 * Service for tracking GPS position.
 */
public class GPSTracker extends Service implements LocationListener {

    public static final String GPS_TRACKER_INTENT = "location_changed_key";
    public static final String BROADCAST_TYPE_KEY = "broadcast_type_bundle";
    public static final String LATITUDE_KEY = "latitude_bundle";
    public static final String LONGITUDE_KEY = "longitude_bundle";

    /**
     * Type of the broadcasts this service sends.
     */
    public enum BroadcastType {
        PERMISSION_REQUEST, LOCATION_UPDATE
    }

    private static final int MINIMUM_UPDATE_DISTANCE_IN_METERS = 2;
    private static final int FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 5000;

    private LocationManager locationManager;
    private Location location;
    private Context mContext;


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.mContext = this;

        getLocation();
        return super.onStartCommand(intent, flags, startId);
    }

    private Location getLocation() {
        try {
            locationManager = (LocationManager) mContext.getSystemService(LOCATION_SERVICE);

            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Intent intent = new Intent(GPS_TRACKER_INTENT);
                intent.putExtra(BROADCAST_TYPE_KEY, BroadcastType.PERMISSION_REQUEST.ordinal());
                sendBroadcast(intent);
            } else {
                /* Use location from GPS provider */
                if (location == null) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        Intent intent = new Intent(GPS_TRACKER_INTENT);
                        intent.putExtra(BROADCAST_TYPE_KEY, BroadcastType.PERMISSION_REQUEST.ordinal());
                        sendBroadcast(intent);
                        return null;
                    }
                    locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS,
                            MINIMUM_UPDATE_DISTANCE_IN_METERS,
                            this
                    );
                    if (locationManager != null) {
                        location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        if (location != null) {
                            onLocationChanged(location);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return location;
    }

    private void stopUsingGPS() {
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    @Override
    public void onDestroy() {
        stopUsingGPS();
        super.onDestroy();
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        Intent intent = new Intent(GPS_TRACKER_INTENT);
        intent.putExtra(BROADCAST_TYPE_KEY, BroadcastType.LOCATION_UPDATE.ordinal());
        intent.putExtra(LATITUDE_KEY, newLocation.getLatitude());
        intent.putExtra(LONGITUDE_KEY, newLocation.getLongitude());
        sendBroadcast(intent);
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
