package eu.raxsix.tml;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteDatabase;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Property;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import eu.raxsix.tml.application.AppConfig;
import eu.raxsix.tml.database.TmlContract;
import eu.raxsix.tml.database.TmlDbHelper;
import eu.raxsix.tml.helper.LatLngInterpolator;
import eu.raxsix.tml.network.Network;
import eu.raxsix.tml.pojo.User;
import eu.raxsix.tml.service.SocketService;
import eu.raxsix.tml.service.TmlIntentService;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

import static eu.raxsix.tml.application.AppConfig.EXTRA_ROOM_NAME;
import static eu.raxsix.tml.application.AppConfig.EXTRA_USER_ID;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener, GpsStatus.Listener, LoaderManager.LoaderCallbacks {

    private static final String TAG = MapsActivity.class.getSimpleName();

    private static final int ONGOING_SERVICE_NOTIFICATION_ID = 5;
    private static final int LOCATION_UPDATE_INTERVAL_TIME = 5000;

    private String mName;
    private String mUserID;
    private Socket mSocket;
    private String mRoomName;

    // Is this Activity active
    private boolean mIsActive = false;
    // Is this Activity active
    private boolean mIsGpsOn = false;
    // Is the Activity initialized
    private boolean isInit = false;
    // For location point
    private boolean fallowMode = false;

    private boolean isLocationUpdateRunning = false;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastLocation;
    private Location mCurrentLocation = new Location("CurrentLocation");
    private LocationManager mLocationManager;

    private HashSet<String> mLeftUsers = new HashSet<>();
    private HashSet<String> mJoinedUsers = new HashSet<>();
    private Map<String, User> mUsersHasMap = new HashMap<>();

    private ProgressDialog mDialog;
    private FrameLayout mFrameLayout;

    private SocketService mService;
    boolean mBound = false;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {

                ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = cm.getActiveNetworkInfo();

                if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {


                    if (mSocket != null && !mSocket.connected()) {

                        connectSocket();
                    }

                    if (mSocket != null && mSocket.connected() && !mUsersHasMap.isEmpty()) {

                        startLocationUpdates();
                    }

                } else if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {


                    if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {

                        stopLocationUpdates();
                    }


                    turnEventsOff();


                    if (mSocket != null) {
                        mSocket.close();
                    }


                    if (mUsersHasMap != null) {

                        cleanUsersHashMap();
                    }

                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        TmlDbHelper mOpenHelper = new TmlDbHelper(this);
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS " + TmlContract.RoomEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TmlContract.UserEntry.TABLE_NAME);

        db.execSQL(TmlDbHelper.SQL_CREATE_ROOM_TABLE);
        db.execSQL(TmlDbHelper.SQL_CREATE_USER_TABLE);


        // Get the userID, it is from the http request, it is generated by the server

        mUserID = getIntent().getExtras().getString(EXTRA_USER_ID);

        mRoomName = getIntent().getExtras().getString(EXTRA_ROOM_NAME);

        // Progress dialog
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(true);
        mDialog.setMessage(getString(R.string.dialog_loading_text));
        Network.showDialog(mDialog);

        // Create an instance of GoogleAPIClient.
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(MapsActivity.this)
                    .addConnectionCallbacks(MapsActivity.this)
                    .addOnConnectionFailedListener(MapsActivity.this)
                    .addApi(LocationServices.API)
                    .build();
        }

        // Get the last known location
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(MapsActivity.this);

        mGoogleApiClient.connect();

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        // For camera fallow mode
        mFrameLayout = (FrameLayout) findViewById(R.id.map_touch_layer);

    }


    @Override
    protected void onStart() {
        super.onStart();

        mIsActive = true;

        // Register the network state receiver to detect network drop down
        registerReceiver(mBroadcastReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));

        mLocationManager.addGpsStatusListener(this);

        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            mIsGpsOn = true;
        } else {
            mIsGpsOn = false;
            turnOnGPS();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();


        if (isInit) {
            if (mSocket != null) {

                mSocket.on(Network.MESSAGE_POSITION, onPosition);
            }
        }
        if (mLeftUsers != null) {
            mLeftUsers.clear();
        }

        if (mJoinedUsers != null) {
            mJoinedUsers.clear();
        }
        isInit = true;
    }

    @Override
    protected void onRestart() {
        super.onRestart();

        // Bind the service back again
        if (mConnection != null) {
            Intent intent = new Intent(this, SocketService.class);
            bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        }

    }

    protected void onStop() {

        super.onStop();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }

        mIsActive = false;

        if (mSocket != null) {
            mSocket.off(Network.MESSAGE_POSITION, onPosition);
        }

        unregisterReceiver(mBroadcastReceiver);

        mLocationManager.removeGpsStatusListener(this);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        // Send the leave
        mSocket.emit(Network.MESSAGE_LEAVE, new JSONObject());

        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }

        turnEventsOff();

        mGoogleApiClient.disconnect();

        mSocket.disconnect();

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // Unbind from the service
        if (mBound) {
            unbindService(mConnection);
            mBound = false;
        }
        stopService(new Intent(this, SocketService.class));
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        mMap.setMyLocationEnabled(true);

        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);

        mMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {
                return null;
            }

            @Override
            public View getInfoContents(Marker marker) {

                View v = getLayoutInflater().inflate(R.layout.custom_info_window, null);

                TextView name = (TextView) v.findViewById(R.id.nameTextView);
                TextView distance = (TextView) v.findViewById(R.id.distanceTextView);
                TextView time = (TextView) v.findViewById(R.id.timeTextView);

                name.setText(marker.getTitle());
                distance.setText(marker.getSnippet());

                DateFormat df = DateFormat.getTimeInstance();
                String date = df.format(Calendar.getInstance().getTime());
                time.setText(date);

                return v;
            }
        });

        mMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
            @Override
            public boolean onMyLocationButtonClick() {

                fallowMode = true;
                return false;
            }
        });

        mFrameLayout.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                fallowMode = false;
                return false; // Pass on the touch to the map or shadow layer.
            }
        });

    }

    @Override
    public void onConnected(Bundle bundle) {

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        if (mLastLocation != null) {

            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14));
        }

        // When Google Maps are connected start the service and Bind it
        Intent intent = new Intent(this, SocketService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
        startService(new Intent(MapsActivity.this, SocketService.class));
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Toast.makeText(this, R.string.google_maps_connection_failed, Toast.LENGTH_SHORT).show();
    }

    /**
     * Defines callbacks for service binding, passed to bindService()
     */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {


            // We've bound to LocalService, cast the IBinder and get LocalService instance
            SocketService.LocalBinder binder = (SocketService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;

            if (mSocket == null) {

                connectSocket();
            }

            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

            if (mUsersHasMap.isEmpty() || !lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                sendServiceNotification(getString(R.string.location_update_inactive));
            }

            if (mIsGpsOn && isLocationUpdateRunning) {
                sendServiceNotification(getString(R.string.location_update_active));
            }

            addUserListButtonToMap();

        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };


    private void connectSocket() {

        mSocket = mService.connectToServer(mUserID);
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Network.MESSAGE_USER_JOINED, onUserJoined);
        mSocket.on(Network.MESSAGE_USER_LEFT, onUserLeft);
        mSocket.on(Network.MESSAGE_POSITION, onPosition);
        mSocket.connect();

        if (mGoogleApiClient != null) {

            mService.setGoogleApiClient(mGoogleApiClient);
            mService.setContext(this);
        }
    }

    /**
     * Fired upon connecting, no parameters
     */
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... noParameters) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    Network.hideDialog(mDialog);
                }
            });
        }
    };


    /**
     * Fired upon user joined the room, parameters user info
     */
    private Emitter.Listener onUserJoined = new Emitter.Listener() {
        @Override
        public void call(final Object... userInfo) {

            final JSONObject newUser = (JSONObject) userInfo[0];
            User userToList = new User();
            String userID = null;
            String name = "User";
            boolean web;
            boolean list;

            try {
                if (!newUser.getString(Network.TAG_USER_ID).contentEquals(mUserID)) {

                    if (mUsersHasMap.containsKey(newUser.getString(Network.TAG_USER_ID))) {

                        userToList = mUsersHasMap.get(newUser.getString(Network.TAG_USER_ID));
                    }

                    if (newUser.has(Network.TAG_NAME)) {
                        name = newUser.getString(Network.TAG_NAME);
                        userToList.setName(name);
                    }

                    if (newUser.has(Network.TAG_USER_ID)) {
                        userID = newUser.getString(Network.TAG_USER_ID);
                    }

                    if (newUser.has(Network.TAG_WEB)) {
                        web = newUser.getBoolean(Network.TAG_WEB);
                        userToList.setWeb(web);
                    }

                    if (userID != null) {
                        mUsersHasMap.put(userID, userToList);
                    } else {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(MapsActivity.this, R.string.server_error_nr_1, Toast.LENGTH_LONG).show();
                                turnEventsOff();
                                finish();
                            }
                        });
                    }

                    if (newUser.has(Network.TAG_LIST)) {
                        list = newUser.getBoolean(Network.TAG_LIST);

                        if (!list) {
                            final String finalName = name;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    if (mIsActive) {
                                        Toast.makeText(MapsActivity.this, finalName + " has joined ", Toast.LENGTH_LONG).show();
                                    } else {

                                        mJoinedUsers.add(finalName);

                                        Intent notificationIntent = new Intent(MapsActivity.this, MapsActivity.class);
                                        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                                        notificationIntent.setAction(Intent.ACTION_MAIN);
                                        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);


                                        PendingIntent intent = PendingIntent.getActivity(MapsActivity.this, 0, notificationIntent, 0);

                                        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MapsActivity.this)
                                                .setSmallIcon(R.drawable.ic_joined_room)
                                                .setVisibility(Notification.VISIBILITY_PUBLIC)
                                                .setContentIntent(intent)
                                                .setAutoCancel(true)
                                                .setNumber(mJoinedUsers.size())
                                                .setPriority(5) //private static final PRIORITY_HIGH = 5;
                                                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                                        mBuilder.setContentTitle(getString(R.string.app_name));

                                        StringBuilder sb = new StringBuilder();
                                        if (mJoinedUsers.size() > 1) {

                                            sb.append(mJoinedUsers.size()).append(getString(R.string.users_joined_the_room));

                                        } else {

                                            sb.append(getString(R.string.user)).append(finalName).append(getString(R.string.joined_the_room));
                                        }
                                        mBuilder.setContentText(sb);

                                        NotificationManager mNotificationManager = (NotificationManager) MapsActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                        mNotificationManager.notify(0, mBuilder.build());

                                    }
                                }
                            });
                        }
                    }
                }

                if (!mUsersHasMap.isEmpty()) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (!isLocationUpdateRunning) {
                                // You are not alone in the room start location update
                                if (mGoogleApiClient.isConnected()) {
                                    startLocationUpdates();

                                    LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                                    if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                                        sendServiceNotification(getString(R.string.location_update_active));
                                }

                            }
                        }
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            Intent serviceIntent = new Intent(MapsActivity.this, TmlIntentService.class);
            serviceIntent.putExtra(AppConfig.EXTRA_ROOM_NAME, mRoomName);
            serviceIntent.putExtra(AppConfig.EXTRA_ROOM_COUNT, mUsersHasMap.size());
            serviceIntent.putExtra(AppConfig.EXTRA_USER_LIST, (HashMap) mUsersHasMap);
            startService(serviceIntent);
        }
    };


    private Emitter.Listener onUserLeft = new Emitter.Listener() {
        @Override
        public void call(Object... args) {


            try {
                JSONObject leftUser = (JSONObject) args[0];

                final String userID = leftUser.getString(Network.TAG_USER_ID);

                final String name = leftUser.getString(Network.TAG_NAME);

                if (mUsersHasMap.containsKey(userID)) {

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            removeUserFromHashMap(userID);

                            if (mIsActive) {
                                Toast.makeText(MapsActivity.this, name + getString(R.string.has_left), Toast.LENGTH_LONG).show();
                            } else {

                                mLeftUsers.add(name);

                                Intent notificationIntent = new Intent(MapsActivity.this, MapsActivity.class);
                                notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                                notificationIntent.setAction(Intent.ACTION_MAIN);
                                notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);


                                PendingIntent intent = PendingIntent.getActivity(MapsActivity.this, 0, notificationIntent, 0);

                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(MapsActivity.this)
                                        .setSmallIcon(R.drawable.ic_stat_leave)
                                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                                        .setContentIntent(intent)
                                        .setAutoCancel(true)
                                        .setNumber(mLeftUsers.size())
                                        .setPriority(5) //private static final PRIORITY_HIGH = 5;
                                        .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS);

                                mBuilder.setContentTitle(getString(R.string.app_name));
                                StringBuilder sb = new StringBuilder();

                                if (mLeftUsers.size() > 1) {


                                    sb.append(mLeftUsers.size()).append(getString(R.string.users_left_the_room));

                                } else {

                                    sb.append(getString(R.string.user)).append(name).append(getString(R.string.left_the_room));
                                }
                                mBuilder.setContentText(sb);
                                NotificationManager mNotificationManager = (NotificationManager) MapsActivity.this.getSystemService(Context.NOTIFICATION_SERVICE);
                                mNotificationManager.notify(1, mBuilder.build());
                            }
                        }
                    });
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }


        }
    };


    private Emitter.Listener onPosition = new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            JSONObject newPosition = (JSONObject) args[0];

            try {
                // Get the client id out of the jsonObject
                final String userID = newPosition.getString(Network.TAG_USER_ID);

                mName = newPosition.getString(Network.TAG_NAME);

                final double emittedLat = newPosition.getDouble(Network.TAG_LAT);

                double emittedLng = newPosition.getDouble(Network.TAG_LNG);

                // Create LatLng object with the new lat and longitude
                final LatLng emittedLatLng = new LatLng(emittedLat, emittedLng);

                // Create location object for distanceTo() method parameter
                final Location location = new Location("location");
                location.setLatitude(emittedLat);
                location.setLongitude(emittedLng);


                // Make formatted distance
                final String formattedDistanceNumber = formatNumber(mCurrentLocation.distanceTo(location));


                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        if (mUsersHasMap.containsKey(userID)) {

                            User user = mUsersHasMap.get(userID);

                            if (user.getLocationMarker() == null) {

                                if (!user.isWeb()) {
                                    Marker marker = user.setLocationMarker(mMap.addMarker(new MarkerOptions()
                                            .title(mName)
                                            .position(emittedLatLng)));

                                    if (mIsGpsOn) {
                                        marker.setSnippet(formattedDistanceNumber);
                                    } else {
                                        marker.setSnippet("-");
                                    }

                                } else {
                                    Marker marker = user.setLocationMarker(mMap.addMarker(new MarkerOptions()
                                            .title(mName)
                                            .position(emittedLatLng)
                                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))));
                                    if (mIsGpsOn) {
                                        marker.setSnippet(formattedDistanceNumber);
                                    } else {
                                        marker.setSnippet("-");
                                    }

                                }
                            } else {

                                if (user.isWeb()) {
                                    Marker marker = user.getLocationMarker();
                                    marker.setPosition(emittedLatLng);
                                    if (mIsGpsOn) {
                                        marker.setSnippet(formattedDistanceNumber);
                                    } else {
                                        marker.setSnippet("-");
                                    }
                                    marker.setIcon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                                } else {
                                    animateMarker(user.getLocationMarker(), emittedLatLng, new LatLngInterpolator.LinearFixed());
                                    Marker marker = user.getLocationMarker();
                                    marker.setPosition(emittedLatLng);
                                    if (mIsGpsOn) {
                                        marker.setSnippet(formattedDistanceNumber);
                                    } else {
                                        marker.setSnippet("-");
                                    }
                                    if (marker.isInfoWindowShown()) {
                                        marker.hideInfoWindow();
                                        marker.showInfoWindow();
                                    }
                                }
                            }
                        }
                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };


    protected void startLocationUpdates() {

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(LOCATION_UPDATE_INTERVAL_TIME);
        locationRequest.setFastestInterval(LOCATION_UPDATE_INTERVAL_TIME);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, locationRequest, this);

        isLocationUpdateRunning = true;
    }

    protected void stopLocationUpdates() {

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);

        isLocationUpdateRunning = false;
    }


    @Override
    public void onLocationChanged(Location location) {

        if (mSocket != null && !mSocket.connected()) {

            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                stopLocationUpdates();
            }
        }

        // Map camera will follow your current position
        if (fallowMode) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, mMap.getCameraPosition().zoom));
        }
        // Calculate
//        float meters = LocationServices.FusedLocationApi.getLastLocation(
//                mGoogleApiClient).distanceTo(mCurrentLocation);
//
//        if (meters > 2) {

        JSONObject phoneLocation = new JSONObject();
        try {
            phoneLocation.put(Network.TAG_LAT, location.getLatitude());
            phoneLocation.put(Network.TAG_LNG, location.getLongitude());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Send the new location to the server
        mSocket.emit(Network.MESSAGE_POSITION, phoneLocation);
//        }

        mCurrentLocation.setLatitude(location.getLatitude());
        mCurrentLocation.setLongitude(location.getLongitude());

    }


    private void turnEventsOff() {

        if (mSocket != null) {
            mSocket.off(Socket.EVENT_CONNECT, onConnect);
            mSocket.off(Network.MESSAGE_POSITION, onPosition);
            mSocket.off(Network.MESSAGE_USER_JOINED, onUserJoined);
            mSocket.off(Network.MESSAGE_USER_LEFT, onUserLeft);
        }
    }

    private String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
            return String.format("(%.1f%s)", distance, unit);
        }

        return String.format("(%.0f%s)", distance, unit);
    }

    private void removeUserFromHashMap(String userID) {

        User user = mUsersHasMap.get(userID);

        if (user.getLocationMarker() != null) {
            // Delete the last marker of that user
            user.getLocationMarker().remove();
        }

        // Delete the user from the hasMap
        mUsersHasMap.remove(userID);

        if (mUsersHasMap.isEmpty()) {
            // You are now alone in the room stop location update, to save battery and bandwidth
            if (isLocationUpdateRunning) {

                if (mGoogleApiClient.isConnected()) {
                    stopLocationUpdates();
                }
                sendServiceNotification(getString(R.string.location_update_inactive));
            }
        }

        Intent serviceIntent = new Intent(MapsActivity.this, TmlIntentService.class);
        serviceIntent.putExtra(AppConfig.EXTRA_ROOM_NAME, mRoomName);
        serviceIntent.putExtra(AppConfig.EXTRA_ROOM_COUNT, mUsersHasMap.size());
        serviceIntent.putExtra(AppConfig.EXTRA_USER_LIST, (HashMap) mUsersHasMap);
        startService(serviceIntent);

    }

    private void cleanUsersHashMap() {

        for (User user : mUsersHasMap.values()) {

            if (user.getLocationMarker() != null) {
                // Delete the last marker of that user
                user.getLocationMarker().remove();
            }
        }
        mUsersHasMap.clear();
    }


    static void animateMarker(Marker marker, LatLng finalPosition, final LatLngInterpolator latLngInterpolator) {
        TypeEvaluator<LatLng> typeEvaluator = new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                return latLngInterpolator.interpolate(fraction, startValue, endValue);
            }
        };
        Property<Marker, LatLng> property = Property.of(Marker.class, LatLng.class, "position");
        ObjectAnimator animator = ObjectAnimator.ofObject(marker, property, typeEvaluator, finalPosition);

        animator.setDuration(LOCATION_UPDATE_INTERVAL_TIME);
        animator.start();

    }

    private void sendServiceNotification(String status) {

        Intent notificationIntent = new Intent(MapsActivity.this, MapsActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PendingIntent intent = PendingIntent.getActivity(MapsActivity.this, 0, notificationIntent, 0);

        Notification notification = new NotificationCompat.Builder(MapsActivity.this)
                .setSmallIcon(R.drawable.ic_tml_is_running)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(status)
                .setContentIntent(intent).build();
        mService.startForeground(ONGOING_SERVICE_NOTIFICATION_ID, notification);
    }

    private void addUserListButtonToMap() {

        Button button = (Button) findViewById(R.id.btn_group_list);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showUserList();
            }

            private void showUserList() {
                AlertDialog.Builder builderSingle = new AlertDialog.Builder(MapsActivity.this);
                builderSingle.setIcon(R.drawable.ic_social_group_w);
                builderSingle.setTitle(getString(R.string.users_in_this_room) + mUsersHasMap.size() + ")");

                final ArrayAdapter<User> arrayAdapter = new ArrayAdapter<>(
                        MapsActivity.this,
                        android.R.layout.simple_list_item_1);


                for (User user : mUsersHasMap.values()) {

                    if (user.getLocationMarker() != null) {

                        LatLng latLng = user.getLocationMarker().getPosition();
                        Location location = new Location("user current location");
                        location.setLatitude(latLng.latitude);
                        location.setLongitude(latLng.longitude);
                        String formattedDistanceNumber = formatNumber(mCurrentLocation.distanceTo(location));
                        if (mIsGpsOn) {
                            user.setDistance(formattedDistanceNumber);
                        }
                        arrayAdapter.add(user);
                    } else {
                        arrayAdapter.add(user);
                    }

                }

                builderSingle.setNegativeButton(
                        getString(R.string.cancel),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                builderSingle.setNeutralButton(getString(R.string.reload), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        showUserList();
                    }
                });


                builderSingle.setAdapter(
                        arrayAdapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                User user = arrayAdapter.getItem(which);
                                if (fallowMode) {
                                    fallowMode = false;
                                }

                                // This is for when the web user has not been clicked a point on the map
                                if (user.getLocationMarker() != null) {

                                    if (mIsGpsOn) {
                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        LatLng latLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
                                        builder.include(user.getLocationMarker().getPosition());
                                        builder.include(latLng);


                                        LatLngBounds bounds = builder.build();
                                        int padding = 100;
                                        CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                                        try {
                                            mMap.animateCamera(cu);
                                        } catch (IllegalStateException e) {
                                            e.printStackTrace();
                                            Log.e(TAG, "SCREEN ERROR");
                                        }
                                        user.getLocationMarker().showInfoWindow();
                                    } else {
                                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(user.getLocationMarker().getPosition(), 14));
                                    }


                                }

                                if (user.isWeb()) {
                                    Toast.makeText(MapsActivity.this, user.getName() + getString(R.string.is_a_web_user), Toast.LENGTH_LONG).show();
                                }
                            }
                        });
                builderSingle.show();
            }
        });
    }


    private void turnOnGPS() {
        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage(getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {


                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {


                }
            });
            dialog.show();
        }
    }

    @Override
    public void onGpsStatusChanged(int event) {

        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:

                mIsGpsOn = true;

                if (isLocationUpdateRunning) {
                    sendServiceNotification(getString(R.string.location_update_active));
                }


                break;
            case GpsStatus.GPS_EVENT_STOPPED:

                mIsGpsOn = false;

                sendServiceNotification(getString(R.string.location_update_inactive));
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Intent serviceIntent = new Intent(MapsActivity.this, TmlIntentService.class);
        serviceIntent.putExtra(AppConfig.EXTRA_ROOM_NAME, "INACTIVE");
        serviceIntent.putExtra(AppConfig.EXTRA_ROOM_COUNT, "");
        startService(serviceIntent);
    }

    @Override
    public Loader onCreateLoader(int id, Bundle args) {
        CursorLoader loader = null;

        switch (id) {

            case 0:
                loader = new CursorLoader(this,
                        TmlContract.RoomEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);
                break;

            case 1:

                loader = new CursorLoader(this,
                        TmlContract.UserEntry.CONTENT_URI,
                        null,
                        null,
                        null,
                        null);
                break;
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader loader, Object data) {

    }

    @Override
    public void onLoaderReset(Loader loader) {

    }
}
