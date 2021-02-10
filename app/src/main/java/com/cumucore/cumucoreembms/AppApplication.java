package com.cumucore.cumucoreembms;

import android.app.Application;

import com.cumucore.cumucoreembms.interfaces.AppSharedPreferenceManager;

public class AppApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        AppSharedPreferenceManager appSharedPreferenceManager = AppSharedPreferenceManager.getAppSharedPreferenceManager(getApplicationContext());
        appSharedPreferenceManager.storeOnSharedPreference(getResources().getString(R.string.multi_cast_socket_one_key), "224.0.1.190:5001");
        appSharedPreferenceManager.storeOnSharedPreference(getResources().getString(R.string.multi_cast_socket_two_key), "225.1.0.1:5004");
    }
}
