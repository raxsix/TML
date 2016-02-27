package eu.raxsix.tml.service;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;

import java.util.HashMap;

import eu.raxsix.tml.application.AppConfig;
import eu.raxsix.tml.database.TmlContract;
import eu.raxsix.tml.pojo.User;


public class TmlIntentService extends IntentService {

    public static final String ACTION_DATA_UPDATED = "eu.raxsix.tml.ACTION_DATA_UPDATED";

    public TmlIntentService() {
        super("IntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        getContentResolver().delete(
                TmlContract.RoomEntry.CONTENT_URI,
                null,
                null
        );

        getContentResolver().delete(
                TmlContract.UserEntry.CONTENT_URI,
                null,
                null
        );

        if (intent.hasExtra(AppConfig.EXTRA_USER_LIST)) {
            HashMap userList = (HashMap) intent.getSerializableExtra(AppConfig.EXTRA_USER_LIST);

            for (Object obj : userList.values()) {

                User user = (User) obj;

                ContentValues UserValues = new ContentValues();
                UserValues.put(TmlContract.UserEntry.COLUMN_USER_NAME, user.getName());
                UserValues.put(TmlContract.UserEntry.COLUMN_DISTANCE, user.getDistance());
                getContentResolver().insert(
                        TmlContract.UserEntry.CONTENT_URI,
                        UserValues
                );
            }
        }


        ContentValues RoomValues = new ContentValues();
        RoomValues.put(TmlContract.RoomEntry.COLUMN_NAME, intent.getExtras().getString(AppConfig.EXTRA_ROOM_NAME));
        RoomValues.put(TmlContract.RoomEntry.COLUMN_COUNT, intent.getExtras().getInt(AppConfig.EXTRA_ROOM_COUNT));
        Uri insertedUri = getContentResolver().insert(
                TmlContract.RoomEntry.CONTENT_URI,
                RoomValues
        );

        Intent dataUpdatedIntent = new Intent(ACTION_DATA_UPDATED);
        sendBroadcast(dataUpdatedIntent);
    }

}
