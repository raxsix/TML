package eu.raxsix.tml.widget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;

import eu.raxsix.tml.service.TmlIntentService;
import eu.raxsix.tml.service.TmlWidgetService;


public class TmlWidgetProvider extends AppWidgetProvider {


    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (TmlIntentService.ACTION_DATA_UPDATED.equals(intent.getAction())) {

            context.startService(new Intent(context, TmlWidgetService.class));
        }
    }
}
