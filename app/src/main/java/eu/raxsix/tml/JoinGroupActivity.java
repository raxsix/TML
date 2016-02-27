
package eu.raxsix.tml;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONException;
import org.json.JSONObject;

import eu.raxsix.tml.application.AppConfig;
import eu.raxsix.tml.helper.Helper;
import eu.raxsix.tml.helper.Validation;
import eu.raxsix.tml.network.Network;
import eu.raxsix.tml.network.VolleySingleton;

import static eu.raxsix.tml.network.Network.PARAMS_FIELD_NAME;
import static eu.raxsix.tml.network.Network.PARAMS_PASSWORD;
import static eu.raxsix.tml.network.Network.PARAMS_ROOM_ID;
import static eu.raxsix.tml.network.Network.PARAMS_USER_ID;
import static eu.raxsix.tml.network.Network.POST_JOIN_DATA;
import static eu.raxsix.tml.network.Network.TAG_ERROR;
import static eu.raxsix.tml.network.Network.TAG_USER_ID;
import static eu.raxsix.tml.network.Network.displayMessage;
import static eu.raxsix.tml.network.Network.hideDialog;
import static eu.raxsix.tml.network.Network.isNetworkAvailable;
import static eu.raxsix.tml.network.Network.trimMessage;

public class JoinGroupActivity extends AppCompatActivity {

    SharedPreferences mSharedPref;

    private String mRoomId;
    private String mPrefFieldName;
    private ProgressDialog mDialog;
    private TextView mPasswordTextView;
    private TextView mFieldNameTextView;
    private String mRoomName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_group);

        if (!Helper.isTablet(this)) {
            // stop screen rotation on phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        Intent intent = getIntent();
        mRoomId = intent.getExtras().getString(AppConfig.EXTRA_ROOM_ID);

        mRoomName = intent.getExtras().getString(AppConfig.EXTRA_ROOM_NAME);

        // Progress dialog
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(true);
        mDialog.setMessage(getString(R.string.dialog_loading_text));

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefFieldName = mSharedPref.getString(getString(R.string.pref_user_field_name_key), "");

        TextView mGroupTextView = (TextView) findViewById(R.id.groupNameTitle);
        mGroupTextView.setText(mRoomName);
        mFieldNameTextView = (TextView) findViewById(R.id.fieldName);
        if (!mPrefFieldName.isEmpty()) {

            mFieldNameTextView.setText(mPrefFieldName);
        }
        mPasswordTextView = (TextView) findViewById(R.id.joinPassword);
        Button joinButton = (Button) findViewById(R.id.buttonJoin);

        joinButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Reset error
                mFieldNameTextView.setError(null);

                String fieldName = mFieldNameTextView.getText().toString();
                String password = mPasswordTextView.getText().toString();

                boolean cancel = false;
                View focusView = null;

                if (!Validation.isFormFieldNameValid(fieldName)) {

                    mFieldNameTextView.setError(getString(R.string.error_invalid_field_name));
                    focusView = mFieldNameTextView;
                    cancel = true;
                }

                if (cancel) {

                    // There was an error; don't attempt login and focus the first
                    // form field with an error.
                    focusView.requestFocus();
                } else {

                    if (mPrefFieldName.isEmpty()) {

                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString(getString(R.string.pref_user_field_name_key), fieldName);
                        editor.apply();
                    }
                    // perform the join attempt.
                    joinGroup(fieldName, password);
                }
            }
        });

    }

    private void joinGroup(final String fieldName, final String password) {

        // Tag used to cancel the request
        String post_data = POST_JOIN_DATA;

        Network.showDialog(mDialog);

        if (!isNetworkAvailable()) {

            Toast.makeText(JoinGroupActivity.this, getString(R.string.network_not_available), Toast.LENGTH_LONG).show();
            hideDialog(mDialog);

        } else {

            JSONObject params = new JSONObject();
            try {
                params.put(PARAMS_ROOM_ID, mRoomId);
                params.put(PARAMS_FIELD_NAME, fieldName);
                params.put(PARAMS_PASSWORD, password);
                params.put(PARAMS_USER_ID, Helper.readUserIdFromPreferences(JoinGroupActivity.this));

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, AppConfig.JOIN_GROUP_URL, params,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {
                                boolean error = response.getBoolean(TAG_ERROR);

                                if (!error) {

                                    // Generated by server
                                    String userID = response.getString(TAG_USER_ID);

                                    // Write userID to shared preferences. It's for reconnection when the task should be killed
                                    Helper.writeUserIdToSharedPreferences(JoinGroupActivity.this, userID);

                                    Intent mapIntent = new Intent(JoinGroupActivity.this, MapsActivity.class);
                                    //mapIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    //mapIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    mapIntent.putExtra(AppConfig.EXTRA_USER_ID, userID);
                                    mapIntent.putExtra(AppConfig.EXTRA_ROOM_NAME, mRoomName);
                                    startActivity(mapIntent);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            hideDialog(mDialog);
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {

                            String json;

                            NetworkResponse response = error.networkResponse;
                            if (response != null && response.data != null) {
                                switch (response.statusCode) {
                                    case 400:
                                        json = new String(response.data);
                                        json = trimMessage(json, "message");
                                        if (json != null) displayMessage(json);
                                        break;

                                    case 401:
                                        json = new String(response.data);
                                        json = trimMessage(json, "message");
                                        if (json != null) displayMessage(json);
                                        break;

                                    case 404:
                                        json = new String(response.data);
                                        json = trimMessage(json, "message");
                                        if (json != null) displayMessage(json);
                                        break;
                                }
                                //Additional cases
                            }
                            hideDialog(mDialog);
                        }
                    });

            // Adding request to request queue
            VolleySingleton.getInstance().addToRequestQueue(request, post_data);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {
            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return super.onOptionsItemSelected(item);
    }
}
