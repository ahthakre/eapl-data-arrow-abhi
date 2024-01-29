package com.example.ninthj;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap googleMap;
    private Handler handler;
    private List<LatLng> markerPositions = new ArrayList<>();
    private int currentIndex = 0;
    private List<Circle> circles = new ArrayList<>();
    private int totalCoordinates;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapFragment);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        LatLng defaultLocation = new LatLng(19.076090, 72.877426);
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10));
        map.getUiSettings().setZoomControlsEnabled(true);
        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference locationRef = database.getReference();

        locationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                try {
                    totalCoordinates = Math.min(100,(int)dataSnapshot.getChildrenCount());
                    int count=0;
                    for (DataSnapshot locationSnapshot : dataSnapshot.getChildren()) {
                        if(count>=100){
                            break;
                        }
                        Double latitude = locationSnapshot.child("Lat").getValue(Double.class);
                        Double longitude = locationSnapshot.child("Long").getValue(Double.class);

                        if (latitude != null && longitude != null) {
                            LatLng location = new LatLng(latitude, longitude);
                            markerPositions.add(location);
                            count++;
                        }
                    }

                    addDot();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Error fetching data from Firebase: " + databaseError.getMessage());
            }
        });
    }

    private void addDot() {
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (currentIndex < markerPositions.size() - 1) {
                    LatLng currentLocation = markerPositions.get(currentIndex);
                    LatLng nextLocation = markerPositions.get(currentIndex + 1);



                    animateCameraToDot(currentLocation);
                    drawArrow(currentLocation, nextLocation);

                    currentIndex++;
                    addDot();
                } else {
                    showMarkerCount();
                }
            }
        }, 100);
    }

    private void animateCameraToDot(LatLng dotPosition) {
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(dotPosition, 25));
    }

    private void drawArrow(LatLng start, LatLng end) {
        googleMap.addPolyline(new PolylineOptions().add(start, end).color(Color.BLACK));

        // Calculate arrowhead points
        LatLng[] arrowheadPoints = calculateArrowheadPoints(start, end);

        // Draw arrowhead
        googleMap.addPolyline(new PolylineOptions()
                .add(end, arrowheadPoints[0])
                .color(Color.RED));

        googleMap.addPolyline(new PolylineOptions()
                .add(end, arrowheadPoints[1])
                .color(Color.RED));
    }

    private LatLng[] calculateArrowheadPoints(LatLng start, LatLng end) {
        double distance=Math.sqrt(Math.pow(end.longitude-start.longitude,2)+Math.pow(end.latitude-start.latitude,2));
        // Calculate arrowhead size
        double lengthOfArrow= 0.3*distance;

        // Calculate angle between start and end points
        double angle = Math.atan2(end.longitude - start.longitude, end.latitude - start.latitude);

        // Calculate arrowhead points
        double xR = end.latitude - lengthOfArrow * Math.cos(angle + Math.PI / 4);
        double yR = end.longitude - lengthOfArrow * Math.sin(angle + Math.PI / 4);

        double xL = end.latitude - lengthOfArrow * Math.cos(angle - Math.PI / 4);
        double yL = end.longitude - lengthOfArrow * Math.sin(angle - Math.PI / 4);

        return new LatLng[]{new LatLng(xR, yR), new LatLng(xL, yL)};
    }
    private void showMarkerCount() {
        Toast.makeText(MainActivity.this, "All coordinates plotted: " + totalCoordinates, Toast.LENGTH_SHORT).show();
    }
}