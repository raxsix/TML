package eu.raxsix.tml.service;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;

import org.json.JSONObject;

import java.net.URISyntaxException;

import eu.raxsix.tml.application.AppConfig;
import eu.raxsix.tml.network.Network;
import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketService extends Service {

    private static final String TAG = SocketService.class.getSimpleName();

    private Socket mSocket;
    private GoogleApiClient mGoogleApiClient;
    private Context mContext;

    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();


    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SocketService getService() {
            // Return this instance of LocalService so clients can call public methods
            return SocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "Service onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        Log.d(TAG, "Service onRebind");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "Service onCreate");
        // Get a handler that can be used to post to the main thread
        Handler mainHandler = new Handler(getMainLooper());

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service onDestroy");

    }


    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.d(TAG, "onTaskRemoved");

        NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();

        // When the user should remove the app from task, we need to close location update
        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, (LocationListener) mContext);


        // Send the leave
        mSocket.emit(Network.MESSAGE_LEAVE, new JSONObject());

        stopSelf();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "onUnbind");
        return super.onUnbind(intent);

    }

    public Socket connectToServer(String userID) {

        Log.d(TAG, "connectToServer");
        {
            try {
                IO.Options opts = new IO.Options();
                opts.query = Network.SOCKET_USER_ID + userID;
                opts.forceNew = true;
                mSocket = IO.socket(AppConfig.SOCKET_URL, opts);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }
        return mSocket;
    }


    public void setGoogleApiClient(GoogleApiClient mGoogleApiClient) {
        this.mGoogleApiClient = mGoogleApiClient;
    }

    public void setContext(Context mContext) {
        this.mContext = mContext;
    }

}
