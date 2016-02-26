package eu.raxsix.tml.helper;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

import eu.raxsix.tml.application.AppConfig;

public class Helper {

    private static final String DEFAULT = "N/A";

    public static void writeUserIdToSharedPreferences(Context context, String userID) {

        // Set the shared preferences mode
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE);

        // Set key-value pares
        // Initialize editor
        SharedPreferences.Editor editor = sharedPreferences.edit();
        // Entering the values
        editor.putString(AppConfig.PREF_KEY, userID);
        // Commit the changes
        editor.apply();
    }

    public static String readUserIdFromPreferences(Context context) {

        // Set the shared preferences mode
        SharedPreferences sharedPreferences = context.getSharedPreferences(AppConfig.PREF_FILE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(AppConfig.PREF_KEY, DEFAULT);
    }


    public static boolean isTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK)
                >= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }
}
