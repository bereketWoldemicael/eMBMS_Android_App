package com.cumucore.cumucoreembms.interfaces;

import android.content.Context;
import android.content.SharedPreferences;

public class AppSharedPreferenceManager {
    private static Context context;
    private  static  AppSharedPreferenceManager appSharedPreferenceManager;
    public static final String APP_PREFERENCE_NAME = "COMUCORE_EMBMS";
    protected AppSharedPreferenceManager (Context mContext){
        this.context = mContext;

    }

    public synchronized static AppSharedPreferenceManager getAppSharedPreferenceManager(Context context){
        if(appSharedPreferenceManager == null){
            appSharedPreferenceManager = new AppSharedPreferenceManager(context);
            return  appSharedPreferenceManager;
        }
        return  appSharedPreferenceManager;
    }


    public static void    storeOnSharedPreference (String key, String value){
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_PREFERENCE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.commit();
    }

    public  static String getSharedPreferenceForKey(String key){
        SharedPreferences sharedPreferences = context.getSharedPreferences(APP_PREFERENCE_NAME, Context.MODE_PRIVATE);
        return sharedPreferences.getString(key,"");
    }
}
