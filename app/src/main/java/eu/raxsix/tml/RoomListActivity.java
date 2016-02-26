
package eu.raxsix.tml;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import eu.raxsix.tml.application.AppConfig;
import eu.raxsix.tml.helper.Helper;
import eu.raxsix.tml.network.Network;
import eu.raxsix.tml.network.VolleySingleton;
import eu.raxsix.tml.pojo.Room;

public class RoomListActivity extends ListActivity {

    private static final String TAG = RoomListActivity.class.getSimpleName();
    private List<Room> mRoomList = new ArrayList<>();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room_list);

        requestRoomList();

        if (!Helper.isTablet(this)) {
            // stop screen rotation on phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRoomList.clear();
                requestRoomList();
                Snackbar.make(view, R.string.list_refreshed, Snackbar.LENGTH_LONG)
                        .setAction(R.string.action, null).show();
            }
        });

    }

    private void requestRoomList() {
        // Tag used to cancel the request
        String get_list_data = Network.GET_CROUP_LIST_DATA;

        JSONObject params = new JSONObject();

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST, AppConfig.GROUP_LIST_URL, params,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {

                            boolean error = response.getBoolean(Network.TAG_ERROR);


                            if (!error) {

                                final List<String[]> roomList = new LinkedList<>();

                                JSONArray rooms = response.getJSONArray(Network.TAG_ROOMS);

                                for (int i = 0; i < rooms.length(); i++) {

                                    JSONObject room = rooms.getJSONObject(i);

                                    if (room.has(Network.TAG_NAME) && !room.isNull(Network.TAG_NAME)) {

                                        Room roomObject = new Room();

                                        roomObject.setId(room.getString(Network.TAG_USER_ID));
                                        roomObject.setName(room.getString(Network.TAG_NAME));

                                        mRoomList.add(roomObject);

                                        String count = String.valueOf(room.getInt(Network.TAG_COUNT));
                                        roomList.add(new String[]{room.getString(Network.TAG_NAME), count});
                                    }

                                }

                                setListAdapter(new ArrayAdapter<String[]>(
                                        RoomListActivity.this,
                                        android.R.layout.simple_list_item_2,
                                        android.R.id.text1,
                                        roomList) {


                                    @Override
                                    public View getView(int position, View convertView, ViewGroup parent) {

                                        // Must always return just a View.
                                        View view = super.getView(position, convertView, parent);

                                        // If you look at the android.R.layout.simple_list_item_2 source, you'll see
                                        // it's a TwoLineListItem with 2 TextViews - text1 and text2.
                                        //TwoLineListItem listItem = (TwoLineListItem) view;
                                        String[] entry = roomList.get(position);
                                        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                                        TextView text2 = (TextView) view.findViewById(android.R.id.text2);
                                        text1.setText(entry[0]);
                                        text2.setText(getString(R.string.joined) + " " + entry[1]);
                                        return view;
                                    }


                                });


                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "LoginError:" + error.getMessage());

                    }
                });

        // Adding request to request queue
        VolleySingleton.getInstance().addToRequestQueue(request, get_list_data);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        Intent joinIntent = new Intent(this, JoinGroupActivity.class);

        joinIntent.putExtra(AppConfig.EXTRA_ROOM_ID, mRoomList.get(position).getId());

        joinIntent.putExtra(AppConfig.EXTRA_ROOM_NAME, mRoomList.get(position).getName());
        startActivity(joinIntent);
    }

}

