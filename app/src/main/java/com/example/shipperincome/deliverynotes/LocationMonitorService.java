package com.example.shipperincome.deliverynotes;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import java.util.List;

public class LocationMonitorService extends Service implements LocationListener {
    public static final String ACTION_START = "com.example.shipperincome.deliverynotes.START";
    public static final String ACTION_STOP = "com.example.shipperincome.deliverynotes.STOP";

    private static final String TAG = "LocationMonitor";
    private static final long MIN_TIME_MS = 30_000;
    private static final float MIN_DISTANCE_M = 20f;

    private LocationManager locationManager;
    private NotesDb db;
    // Callback vi tri va truy van DB chay tren luong rieng de khong chan luong UI.
    private HandlerThread workerThread;
    private Handler workerHandler;
    private boolean updatesRequested = false;

    @Override
    public void onCreate() {
        super.onCreate();
        Notifier.ensureChannels(this);
        db = NotesDb.get(this);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        workerThread = new HandlerThread("LocationMonitorWorker");
        workerThread.start();
        workerHandler = new Handler(workerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!hasLocationPermission()) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            startForeground(
                    Notifier.FOREGROUND_ID,
                    Notifier.monitorNotification(this),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION);
        } catch (RuntimeException e) {
            // Android 14+ tu choi khoi dong FGS vi tri khi app dang o nen (vd. he thong tu khoi dong lai).
            Log.w(TAG, "Cannot start foreground location service", e);
            stopSelf();
            return START_NOT_STICKY;
        }
        MonitorStatus.setRunning(true);
        startLocationUpdates();
        workerHandler.post(this::checkLastKnownLocation);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        MonitorStatus.setRunning(false);
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if (workerThread != null) {
            workerThread.quitSafely();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        checkNotes(location);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void startLocationUpdates() {
        if (updatesRequested || !hasLocationPermission()) {
            return;
        }
        updatesRequested = true;
        for (String provider : new String[]{LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER}) {
            try {
                locationManager.requestLocationUpdates(
                        provider, MIN_TIME_MS, MIN_DISTANCE_M, this, workerThread.getLooper());
            } catch (IllegalArgumentException | SecurityException ignored) {
            }
        }
    }

    private void checkLastKnownLocation() {
        if (!hasLocationPermission()) {
            return;
        }
        Location best = null;
        try {
            for (String provider : locationManager.getProviders(true)) {
                Location location = locationManager.getLastKnownLocation(provider);
                if (location != null && (best == null || location.getAccuracy() < best.getAccuracy())) {
                    best = location;
                }
            }
        } catch (SecurityException ignored) {
            return;
        }
        if (best != null) {
            checkNotes(best);
        }
    }

    private void checkNotes(Location current) {
        List<LocationNote> notes = db.getAllNotes();
        long now = System.currentTimeMillis();
        SharedPreferences prefs = getSharedPreferences(NotesPrefs.NAME, Context.MODE_PRIVATE);
        long cooldownMs = prefs.getInt(NotesPrefs.KEY_TIMEOUT_MINS, NotesPrefs.DEFAULT_TIMEOUT_MINS) * 60_000L;

        for (LocationNote note : notes) {
            float[] result = new float[1];
            Location.distanceBetween(
                    current.getLatitude(),
                    current.getLongitude(),
                    note.latitude,
                    note.longitude,
                    result);
            boolean closeEnough = result[0] <= note.radiusMeters;
            boolean cooledDown = now - note.lastNotifiedAt > cooldownMs;
            if (closeEnough && cooledDown) {
                Notifier.showNote(this, note, result[0]);
                db.markNotified(note.id, now);
            }
        }
    }
}
