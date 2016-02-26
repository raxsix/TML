package eu.raxsix.tml.network;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import eu.raxsix.tml.application.MyApplication;


public class Network {

    private static final String TAG = Network.class.getSimpleName();

    // Volley request tags
    public static final String POST_CROUP_CREATE_DATA = "post_create_data";
    public static final String POST_JOIN_DATA = "post_join_data";
    public static final String GET_CROUP_LIST_DATA = "get_group_list";

    // Request parameter constants
    public static final String PARAMS_ROOM_ID = "room";
    public static final String PARAMS_USER_ID = "id";
    public static final String PARAMS_GROUP_NAME = "name";
    public static final String PARAMS_FIELD_NAME = "username";
    public static final String PARAMS_PASSWORD = "password";

    // Json tags
    public static final String TAG_USER_ID = "id";
    public static final String TAG_NAME = "name";
    public static final String TAG_COUNT = "count";
    public static final String TAG_ROOMS = "rooms";
    public static final String TAG_ERROR = "error";
    public static final String TAG_WEB = "web";
    public static final String TAG_LIST = "list";


    //public static final String TAG_FROM = "from";
    public static final String TAG_LAT = "lat";
    public static final String TAG_LNG = "lng";

    // Socket.io message tags
    public static final String MESSAGE_POSITION = "position";
    public static final String MESSAGE_LEAVE = "leave";
    public static final String MESSAGE_USER_JOINED = "userJoined";
    public static final String MESSAGE_USER_LEFT = "userLeft";

    public static final String SOCKET_USER_ID = "userID=";

    /**
     * Helper method to test the network availability
     *
     * @return boolean
     */
    public static boolean isNetworkAvailable() {
        ConnectivityManager manager = (ConnectivityManager)
                MyApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
        boolean isAvailable = false;
        if (networkInfo != null && networkInfo.isConnected()) {
            isAvailable = true;
        }

        return isAvailable;
    }


    public static void showDialog(ProgressDialog dialog) {
        if (!dialog.isShowing())
            dialog.show();
    }

    public static void hideDialog(ProgressDialog dialog) {
        if (dialog.isShowing())
            dialog.dismiss();
    }

    public static String trimMessage(String json, String key) {
        String trimmedString;

        try {
            JSONObject obj = new JSONObject(json);
            trimmedString = obj.getString(key);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }

        return trimmedString;
    }


    public static void displayMessage(String toastString) {
        Toast.makeText(MyApplication.getAppContext(), toastString, Toast.LENGTH_LONG).show();
    }

}

