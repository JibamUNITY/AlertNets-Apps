package com.example.signin;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.maps.SupportMapFragment;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import com.google.firebase.database.DatabaseReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ReportActivity extends AppCompatActivity {

    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient fusedLocationProviderClient;
    private FloatingActionButton resetLocationButton;

    Button submitBtn;
    EditText location, description;
    TextView name;
    DatabaseReference databaseUsers;
    Spinner title;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        TextView linkEditText = findViewById(R.id.linkEditText);
        TextView googleName = findViewById(R.id.nameEditText);

        // Hide the TextView
        linkEditText.setVisibility(View.GONE);
        googleName.setVisibility(View.GONE);

        name = findViewById(R.id.nameEditText);

        submitBtn = findViewById(R.id.submitButton);
        location = findViewById(R.id.linkEditText);
        name = findViewById(R.id.nameEditText);
        title = findViewById(R.id.titleEditText);
        description = findViewById(R.id.reportEditText);
        databaseUsers = FirebaseDatabase.getInstance().getReference();

        GoogleSignInAccount acct = GoogleSignIn.getLastSignedInAccount(this);
        if (acct != null) {
            String personName = acct.getDisplayName();
            name.setText(personName);
        }

        submitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SubmitData();
            }
        });

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        fusedLocationProviderClient = (FusedLocationProviderClient) LocationServices.getFusedLocationProviderClient(this);

        Dexter.withContext(getApplicationContext()).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        getCurrentLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        // Handle permission denied
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

        // Create a new FloatingActionButton for reset
        FloatingActionButton resetButton = new FloatingActionButton(this);
        resetButton.setImageResource(R.drawable.resetlocation); // Set your icon here
        resetButton.setSize(FloatingActionButton.SIZE_NORMAL);
        resetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.main))); // Set background tint from colors.xml
        resetButton.setRippleColor(getResources().getColor(android.R.color.white));
        resetButton.setElevation(12);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetMapToCurrentLocation();
            }
        });

        // Set layout parameters for the new button
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.fab_margin);
        layoutParams.rightMargin = (int) getResources().getDimension(R.dimen.fab_margin);
        ((FrameLayout) findViewById(R.id.map)).addView(resetButton, layoutParams);
    }

    private void SubmitData() {

        String userlocation = location.getText().toString();
        String username = name.getText().toString();
        String usertitle = title.getSelectedItem().toString();
        String userdescription = description.getText().toString();

        // Check if description is empty
        if (userdescription.isEmpty()) {
            Toast.makeText(this, "Please fill in the description", Toast.LENGTH_SHORT).show();
            return;
        }

        int wordCount = userdescription.split("\\s+").length; // Split by whitespace to count words
        if (wordCount > 30) {
            Toast.makeText(this, "Description is too long (max 30 words)", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseUsers.push().getKey();

        // Get the current date and time
        String time = getCurrentTime();
        String date = getCurrentDate();

        User user = new User(userlocation, username, usertitle, userdescription, time, date);
        databaseUsers.child("users").child(id).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ReportActivity.this, "Report Submitted", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private String getCurrentTime() {
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    private String getCurrentDate() {
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }
    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        onMapReadyCallback(location, googleMap);
                    }
                });
            }
        });
    }

    public void resetMapToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        onMapReadyCallback(location, googleMap);
                    }
                });
            }
        });
    }

    private void onMapReadyCallback(Location location, GoogleMap googleMap) {
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Show a marker at the clicked position
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Pinned Location");
                googleMap.clear(); // Clear existing markers
                googleMap.addMarker(markerOptions);

                // Update EditText with location details
                updateLocationDetails(latLng);
            }
        });

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location !");
            googleMap.clear(); // Clear existing markers
            googleMap.addMarker(markerOptions);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            // Update EditText with current location details
            updateLocationDetails(latLng);
        } else {
            Toast.makeText(ReportActivity.this, "Please turn on your location permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationDetails(LatLng latLng) {

        EditText linkEditText = findViewById(R.id.linkEditText);

        // Set the text in the EditText views with location details
        linkEditText.setText(latLng.latitude + ", " + latLng.longitude);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // Handle toolbar back button click
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
