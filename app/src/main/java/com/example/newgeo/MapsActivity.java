package com.example.newgeo;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryDataEventListener;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;

import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, DialogFrag.DialogFragListener {


    private static final String CHANNEL_ID = "channel_01";

    private FusedLocationProviderClient mFusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private Marker currentUser;
    HashMap<String, String> locMap
            = new HashMap<>();

    double currentLatitude, currentLongitude, currentLatitudeN, currentLongitudeN;
    private GoogleMap mMap;
    DatabaseReference refLoc, refInfo;
    GeoFire geoFire;
    private String setInfo;
    private int setRadius;
    private List<LatLng> remindLocations;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse response) {

                        buildLocationRequest();
                        buildLocationCallBack();
                        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(MapsActivity.this);
                        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                                .findFragmentById(R.id.map);
                        mapFragment.getMapAsync(MapsActivity.this);


                        refInfo = FirebaseDatabase.getInstance().getReference("UserReminderInfo");

                        refLoc = FirebaseDatabase.getInstance().getReference("UserLocation");
                        geoFire = new GeoFire(refLoc);
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse response) {
                        Toast.makeText(MapsActivity.this, "You must grant permission", Toast.LENGTH_SHORT);
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permission, PermissionToken token) {

                    }
                }).check();


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.


    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(final LocationResult locationResult) {
                if (mMap != null) {

                    currentLatitude = locationResult.getLastLocation().getLatitude();
                    currentLongitude = locationResult.getLastLocation().getLongitude();
                    geoFire.setLocation("You", new GeoLocation(locationResult.getLastLocation().getLatitude(),
                            locationResult.getLastLocation().getLongitude()), new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            if (currentUser != null) currentUser.remove();
                            System.out.println(locationResult.getLastLocation().getLatitude()+" location "+locationResult.getLastLocation().getLongitude());
                            currentUser = mMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(locationResult.getLastLocation().getLatitude(),
                                            locationResult.getLastLocation().getLongitude()))
                                    .title("You"));
                            mMap.animateCamera(CameraUpdateFactory
                                    .newLatLngZoom(currentUser.getPosition(), 13.0f));
                        }
                    });
                }
            }
        };
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setSmallestDisplacement(10f);

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);

        if (mFusedLocationClient != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
            }
            mFusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
        }

        populate();


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {

                currentLatitudeN = point.latitude;
                currentLongitudeN = point.longitude;
                LatLng latLng = new LatLng(currentLatitudeN, currentLongitudeN);
               // String key = currentLatitudeN+","+currentLongitudeN;

                    //locMap.put(key, true);
                    mMap.addMarker(new MarkerOptions().position(latLng).title("New Reminder"));
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));



            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                //String key = marker.getPosition().latitude+","+marker.getPosition().longitude;
                //System.out.println(key);
                //if(locMap.containsKey(key)==false) {
                    openDialog();
                    //System.out.println(setInfo + " done " + setRadius);
                    System.out.println(" done ");
               // }
                /*else{
                    marker.remove();
                    String id = locMap.get(key);
                    refInfo.child(id).setValue(null);
                    refLoc.child(id).setValue(null);
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                }*/

                return false;
            }
        });
    }

    @Override
    protected void onStop() {
        mFusedLocationClient.removeLocationUpdates(locationCallback);
        super.onStop();
    }

    private void openDialog() {
        DialogFrag df = new DialogFrag();
        df.show(getSupportFragmentManager(), "Add Reminder");
    }

    @Override
    public void applyText(String info, String radius) {
        setInfo = info;
        this.setRadius = Integer.parseInt(radius);
        //LatLng latLng = new LatLng(currentLatitudeN, currentLongitudeN);
       // remindLocations.add(latLng);
        addGeoReminder(false,"0");
    }





    private void addGeoReminder(boolean chk,String dm-/) {
        final String ID;
        if(chk==false) {
            System.out.println("Inside AddGeo!");
            GeoInfo newG = new GeoInfo(setInfo, setRadius, currentLatitudeN, currentLongitudeN);
            ID = refInfo.child("UserReminderInfo").push().getKey();






            refInfo.child(ID).setValue(newG);
            geoFire.setLocation(ID, new GeoLocation(currentLatitudeN, currentLongitudeN),
                    new GeoFire.CompletionListener() {
                        @Override
                        public void onComplete(String key, DatabaseError error) {
                            LatLng newL = new LatLng(currentLatitudeN, currentLongitudeN);
                            mMap.addCircle(new CircleOptions()
                                    .center(newL)
                                    .radius(setRadius)
                                    .strokeColor(Color.GREEN)
                                    .fillColor(0x220000FF)
                                    .strokeWidth(5.0f));


                        }
                    });
        }

        else{
            ID = id;
        }
        String key = currentLatitudeN+","+currentLongitudeN;
        System.out.println(key+"  "+ID);
        locMap.put(key,ID);
        System.out.println(locMap.get(key));
       // GeoFire newG = new GeoFire( refLoc.child(id));
        GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(currentLatitudeN, currentLongitudeN), setRadius/1000);



        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
                /*System.out.println(key+" entered");
                System.out.println(location.latitude+" "+location.longitude);
                sendNotification("Entered "+ID+" "+location.latitude+" "+location.longitude);*/

                refInfo.child(ID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        GeoInfo g = dataSnapshot.getValue(GeoInfo.class);
                        sendNotification(g.info);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyExited(String key) {
                refInfo.child(ID).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        GeoInfo g = dataSnapshot.getValue(GeoInfo.class);
                        sendNotification("Did You Miss ("+g.info+")?");
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {
                sendNotification("Moving "+key);
            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });



    }

    private void addMarkers(String info,int radius,double lat, double longt, String id){
        LatLng newL = new LatLng(lat, longt);
        String key = lat+","+longt;
       // locMap.put(key,true);
        mMap.addMarker(new MarkerOptions().position(newL).title(info));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(newL));
        mMap.addCircle(new CircleOptions()
                .center(newL)
                .radius(radius)
                .strokeColor(Color.GREEN)
                .fillColor(0x220000FF)
                .strokeWidth(5.0f));

        setInfo = info;
        setRadius = radius;
        currentLatitudeN = lat;
        currentLongitudeN = longt;
        addGeoReminder(true,id);
    }



    private void populate(){
        refInfo.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        for(DataSnapshot d:dataSnapshot.getChildren()){
                            GeoInfo g = d.getValue(GeoInfo.class);
                            System.out.println(d.getKey());
                            addMarkers(g.info,g.radius,g.lat,g.longt,d.getKey());
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void sendNotification(String notificationDetails) {
        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MapsActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MapsActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.drawable.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.drawable.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText(getString(R.string.geofence_transition_notification_text))
                .setContentIntent(notificationPendingIntent);

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }


}
