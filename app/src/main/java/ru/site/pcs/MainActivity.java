package ru.site.pcs;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.site.PCS.R;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private Button send_data;
    private TextView error;
    private TextView notation;
    private ProgressBar progressBar;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1001;
    private static final int UPDATE_TIME = 500;
    private final static int REQUEST_LOCATION = 199;

    private CountDownTimer timer = null;
    private final ArrayList<String> coordinates = new ArrayList<>();
    private boolean started = false;
    private boolean isGPS = false;

    @SuppressLint({"ObsoleteSdkInt", "MissingPermission", "SetTextI18n"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        send_data = findViewById(R.id.send_data);
        error = findViewById(R.id.error);
        notation = findViewById(R.id.notation);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.INVISIBLE);

        googleApiClient = new GoogleApiClient.Builder(this).
                addApi(LocationServices.API).
                addConnectionCallbacks(this).
                addOnConnectionFailedListener(this).build();

        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_TIME);
        locationRequest.setFastestInterval(UPDATE_TIME);

        send_data.setOnClickListener(v -> {
            if (checkAndRequestPermissions()) {
                setStart();
                checkGps();
                if (!isGPS) {
                    enableGps();
                } else {
                    check_new_coordinates();
                }
            }
        });
    }

    private void setStart() {
        if (googleApiClient != null) {
            googleApiClient.connect();
        }
    }

    private void setStop() {
        if (googleApiClient != null && googleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(googleApiClient, this);
            googleApiClient.disconnect();
        }
        coordinates.clear();
        started = false;
        progressBar.setVisibility(View.INVISIBLE);
        send_data.setVisibility(View.VISIBLE);
    }

    @SuppressLint("SetTextI18n")
    private void send_to_server() {
        error.setText("Coordinates: " + coordinates.get(coordinates.size() - 1) + "; Device Id: " + getDeviceId());

        // TODO тут отправка данных на сервер

        notification(10, "Координаты отправлены");
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus && !started) {
            setStop();
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        if (!checkPlayServices()) {
            error.setText("Необходимо установить Google Play Services, чтобы использовать приложение");
            send_data.setEnabled(false);
        } else {
            if (!started) {
                send_data.setEnabled(true);
            }
        }
    }

    @SuppressLint({"SetTextI18n", "MissingPermission"})
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (isGPS) {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onLocationChanged(@NonNull Location location) {
        coordinates.add(location.getLatitude() + ", " + location.getLongitude());
    }

    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST);
            } else {
                finish();
            }
            return false;
        }
        return true;
    }

    private boolean checkAndRequestPermissions() {
        int highLocationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        int locationPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        List<String> listPermissionsNeeded = new ArrayList<>();
        if (highLocationPermission != PackageManager.PERMISSION_GRANTED && locationPermission != PackageManager.PERMISSION_GRANTED) {
            listPermissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION);
            listPermissionsNeeded.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_ID_MULTIPLE_PERMISSIONS: {

                Map<String, Integer> perms = new HashMap<>();
                perms.put(Manifest.permission.ACCESS_FINE_LOCATION, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.ACCESS_COARSE_LOCATION, PackageManager.PERMISSION_GRANTED);
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++)
                        perms.put(permissions[i], grantResults[i]);
                    if (perms.get(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            && perms.get(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        setStart();
                        checkGps();
                        if (!isGPS) {
                            enableGps();
                        } else {
                            setStart();
                            check_new_coordinates();
                        }
                    } else {
                        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
                            showDialogOK("Для работы приложения необходимы разрешения Местоположения",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (which) {
                                                case DialogInterface.BUTTON_POSITIVE:
                                                    checkAndRequestPermissions();
                                                    break;
                                                case DialogInterface.BUTTON_NEGATIVE:
                                                    break;
                                            }
                                        }
                                    });
                        } else {
                            notification(30, "Разрешения не выданы\nНастройки -> Приложения -> PCS -> Разрешения\nустановите статус «Местоположение» включено");
                        }
                    }
                }
            }
        }
    }

    private void showDialogOK(String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("ОК", okListener)
                .setNegativeButton("ОТМЕНА", okListener)
                .create()
                .show();
    }

    private void notification(int sec, String s) {
        sec *= 1000;
        notation.setText(s);
        if (timer != null) {
            timer.cancel();
        }
        timer = new CountDownTimer(sec, 1000) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                notation.setText("");
            }
        };
        timer.start();
    }

    private String getDeviceId() {
        @SuppressLint("HardwareIds") String deviceId = Settings.Secure
                .getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        return deviceId;
    }

    private void check_new_coordinates() {
        notification(30, "Обновляется геолокация,\nждите");
        started = true;
        send_data.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        new CountDownTimer(30000, 100) {
            @SuppressLint("SetTextI18n")
            public void onTick(long millisUntilFinished) {
                try {
                    if (coordinates.size() > 1 && !coordinates.get(coordinates.size() - 1)
                            .equals(coordinates.get(coordinates.size() - 2))) {
                        send_to_server();
                        setStop();
                        this.cancel();
                    }
                } catch (Exception ignored) {
                }
            }

            public void onFinish() {
                notification(10, "Невозможно обновить геолокацию\nПопробуйте еще раз");
                setStop();
            }
        }.start();
    }

    private void checkGps() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        isGPS = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }


    private void enableGps() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        builder.setAlwaysShow(true);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(googleApiClient, builder.build());
        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult result) {
                final Status status = result.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_LOCATION);
                        } catch (IntentSender.SendIntentException ignored) {
                        }
                        break;
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_LOCATION) {
                isGPS = true;
                setStart();
                check_new_coordinates();
            }
        } else {
            notification(20, "Геолокация выключена,\nкоординаты не отправлены!");
        }
    }
}