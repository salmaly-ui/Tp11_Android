package com.ens.realtimetracker;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap googleMap;
    private LocationManager locationManager;
    private TextView tvCoordinates;
    private Button btnCenter;
    private Marker currentMarker;
    private LatLng lastPosition;

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean granted : permissions.values()) {
                    if (!granted) {
                        allGranted = false;
                        break;
                    }
                }
                if (allGranted) {
                    startLocationUpdates();
                } else {
                    Toast.makeText(this, "Permission necessaire pour la localisation", Toast.LENGTH_LONG).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCoordinates = findViewById(R.id.tvCoordinates);
        btnCenter = findViewById(R.id.btnCenter);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnCenter.setOnClickListener(v -> {
            if (lastPosition != null) {
                centerMapOnPosition(lastPosition);
            } else {
                Toast.makeText(this, "Position non disponible", Toast.LENGTH_SHORT).show();
            }
        });

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        if (googleMap != null) {
            googleMap.getUiSettings().setZoomControlsEnabled(true);
        }
        checkPermissionsAndStart();
    }

    private void checkPermissionsAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndStart();
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void checkGpsAndStart() {
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            startLocationUpdates();
        } else {
            showEnableGpsDialog();
        }
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,
                    10,
                    this
            );

            locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    2000,
                    10,
                    this
            );

            Toast.makeText(this, "Recherche de position...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        LatLng position = new LatLng(latitude, longitude);
        lastPosition = position;

        String coordText = String.format(java.util.Locale.FRANCE, "Lat: %.6f | Lon: %.6f", latitude, longitude);
        tvCoordinates.setText(coordText);

        addOrUpdateMarker(position);
        centerMapOnPosition(position);
    }

    private void addOrUpdateMarker(LatLng position) {
        if (googleMap == null) return;

        if (currentMarker == null) {
            currentMarker = googleMap.addMarker(new MarkerOptions()
                    .position(position)
                    .title("Ma position")
                    .snippet("Derniere mise a jour"));
        } else {
            currentMarker.setPosition(position);
        }
    }

    private void centerMapOnPosition(LatLng position) {
        if (googleMap != null && position != null) {
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 16f));
        }
    }

    private void showEnableGpsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("GPS desactive");
        builder.setMessage("Pour suivre votre position, veuillez activer le GPS.");
        builder.setPositiveButton("Parametres", (dialog, which) -> {
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        });
        builder.setNegativeButton("Annuler", (dialog, which) -> dialog.dismiss());
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // Non utilise
    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS active", Toast.LENGTH_SHORT).show();
            startLocationUpdates();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Toast.makeText(this, "GPS desactive", Toast.LENGTH_SHORT).show();
            showEnableGpsDialog();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            checkGpsAndStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }
}