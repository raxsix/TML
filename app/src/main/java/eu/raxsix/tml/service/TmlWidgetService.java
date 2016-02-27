package eu.raxsix.tml.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.RemoteViews;

import eu.raxsix.tml.MainActivity;
import eu.raxsix.tml.R;
import eu.raxsix.tml.database.TmlContract;
import eu.raxsix.tml.widget.TmlWidgetProvider;

/**
 * Created by Ragnar on 2/27/2016.
 */
public class TmlWidgetService extends IntentService {

    public static final int COL_ROOM_NAME = 1;
    public static final int COL_COUNT = 2;


    public TmlWidgetService() {
        super("TmlWidgetService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {


        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
        int[] appWidgetIds = appWidgetManager
                .getAppWidgetIds(new ComponentName(this, TmlWidgetProvider.class));

        Uri dateUri = TmlContract.RoomEntry.CONTENT_URI;

        Cursor data = getContentResolver().query(dateUri, null, null, null, null);

        if (data == null) {
            return;
        }

        if (!data.moveToFirst()) {

            data.close();
            return;
        }
        String roomName = data.getString(COL_ROOM_NAME);
        String count = data.getString(COL_COUNT);

        data.close();

        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {

            // Construct the RemoteViews object
            RemoteViews views = new RemoteViews(getPackageName(), R.layout.widget_detail_layout);

            views.setTextViewText(R.id.widgetRoomName, roomName);
            views.setTextViewText(R.id.widgetCount, count);

            // Led to the MainActivity
            Intent launchIntent = new Intent(this, MainActivity.class);
            launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            launchIntent.setAction(Intent.ACTION_MAIN);
            launchIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, 0);
            views.setOnClickPendingIntent(R.id.widget, pendingIntent);

            // Instruct the widget manager to update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views);
        }
    }

}
