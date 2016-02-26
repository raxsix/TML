
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
import static eu.raxsix.tml.network.Network.PARAMS_GROUP_NAME;
import static eu.raxsix.tml.network.Network.PARAMS_PASSWORD;
import static eu.raxsix.tml.network.Network.POST_CROUP_CREATE_DATA;
import static eu.raxsix.tml.network.Network.TAG_USER_ID;
import static eu.raxsix.tml.network.Network.displayMessage;
import static eu.raxsix.tml.network.Network.trimMessage;

public class CreateGroupActivity extends AppCompatActivity {


    private ProgressDialog mDialog;
    private TextView mFieldNameTextView;
    private TextView mGroupTextView;
    private TextView mPasswordTextView;
    private String mPrefFieldName;
    private String mPrefGroupName;
    SharedPreferences mSharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        if (!Helper.isTablet(this)) {
            // stop screen rotation on phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }

        // Progress dialog
        mDialog = new ProgressDialog(this);
        mDialog.setCancelable(true);
        mDialog.setMessage(getString(R.string.dialog_loading_text));


        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefFieldName = mSharedPref.getString(getString(R.string.pref_user_field_name_key), "");
        mPrefGroupName = mSharedPref.getString(getString(R.string.pref_group_name_key), "");

        // Init views
        mGroupTextView = (TextView) findViewById(R.id.groupName);
        if (!mPrefGroupName.isEmpty()) {

            mGroupTextView.setText(mPrefGroupName);
        }

        mPasswordTextView = (TextView) findViewById(R.id.password);


        mFieldNameTextView = (TextView) findViewById(R.id.fieldName);
        if (!mPrefFieldName.isEmpty()) {

            mFieldNameTextView.setText(mPrefFieldName);
        }

        Button mCreateButton = (Button) findViewById(R.id.buttonCreateGroup);


        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Reset errors.
                mFieldNameTextView.setError(null);
                mPasswordTextView.setError(null);
                mGroupTextView.setError(null);

                String fieldName = mFieldNameTextView.getText().toString();
                String groupName = mGroupTextView.getText().toString();
                String password = mPasswordTextView.getText().toString();

                boolean cancel = false;
                View focusView = null;

                if (!Validation.isFormFieldNameValid(fieldName)) {

                    mFieldNameTextView.setError(getString(R.string.error_invalid_field_name));
                    focusView = mFieldNameTextView;
                    cancel = true;
                }

                if (!Validation.isFormGroupNameValid(groupName)) {

                    mGroupTextView.setError(getString(R.string.error_invalid_group_name));
                    focusView = mGroupTextView;
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

                    if (mPrefGroupName.isEmpty()) {

                        SharedPreferences.Editor editor = mSharedPref.edit();
                        editor.putString(getString(R.string.pref_group_name_key), groupName);
                        editor.apply();
                    }

                    // perform the group creation attempt.
                    createGroup(fieldName, groupName, password);
                }
            }
        });

    }

    private void createGroup(final String fieldName, final String groupName, final String password) {

        Network.showDialog(mDialog);

        // Tag used to cancel the request
        final String post_data = POST_CROUP_CREATE_DATA;

        if (!Network.isNetworkAvailable()) {

            Toast.makeText(CreateGroupActivity.this, getString(R.string.network_not_available), Toast.LENGTH_LONG).show();

        } else {

            JSONObject params = new JSONObject();
            try {

                params.put(PARAMS_GROUP_NAME, groupName);
                params.put(PARAMS_FIELD_NAME, fieldName);
                params.put(PARAMS_PASSWORD, password);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest request = new JsonObjectRequest(
                    Request.Method.POST, AppConfig.CREATE_GROUP_URL, params,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {

                            try {

                                boolean error = response.getBoolean(Network.TAG_ERROR);

                                if (!error) {

                                    // Generated by server
                                    String userID = response.getString(TAG_USER_ID);

                                    Helper.writeUserIdToSharedPreferences(CreateGroupActivity.this, userID);

                                    Intent mapIntent = new Intent(CreateGroupActivity.this, MapsActivity.class);
                                    mapIntent.putExtra(AppConfig.EXTRA_USER_ID, userID);
                                    startActivity(mapIntent);
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                            Network.hideDialog(mDialog);
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
                                }
                                //Additional cases
                            }

                            Network.hideDialog(mDialog);
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