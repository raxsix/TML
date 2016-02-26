package eu.raxsix.tml.application;

import android.app.Application;
import android.content.Context;


public class MyApplication extends Application {

    private static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
    }

    public static Context getAppContext() {
        return sInstance.getApplicationContext();
    }
}

