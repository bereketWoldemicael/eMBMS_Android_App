package com.cumucore.cumucoreembms;

import android.content.Intent;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.EditTextPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.cumucore.cumucoreembms.interfaces.AppSharedPreferenceManager;

public class ActivitySetting extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_setting);

        Toolbar toolbar = findViewById(R.id.toolbar_setting);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle("Setting");

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.setting_holder, new MySettingFragment())
                .commit();

    }

    public static class MySettingFragment extends PreferenceFragmentCompat {
        AppSharedPreferenceManager appSharedPreferenceManager;
        EditTextPreference editTextPreferenceIpForward;
        EditTextPreference editTextPreferencePortForward;

        EditTextPreference editTextPreferenceMulticastIpOne;
        EditTextPreference editTextPreferenceMulticastIpTwo;

        @Override
        public void onCreatePreferences(Bundle bundle, String s) {
            appSharedPreferenceManager = AppSharedPreferenceManager
                    .getAppSharedPreferenceManager(getContext());
            addPreferencesFromResource(R.xml.preferences);
            // ip forwrad port number
            final String ip_forward_key = getResources().getString(R.string.ip_forward_key);

            String ip_forward = appSharedPreferenceManager.getSharedPreferenceForKey(ip_forward_key);

            editTextPreferenceIpForward = (EditTextPreference)findPreference(ip_forward_key);
            editTextPreferenceIpForward.setText(ip_forward);
            editTextPreferenceIpForward.setSummary(ip_forward);

            editTextPreferenceIpForward.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object object) {
                    appSharedPreferenceManager.storeOnSharedPreference(
                            ip_forward_key, (String)object
                    );
                    editTextPreferenceIpForward.setText(appSharedPreferenceManager
                            .getSharedPreferenceForKey(ip_forward_key));

                    editTextPreferenceIpForward.setSummary(
                            appSharedPreferenceManager.getSharedPreferenceForKey(ip_forward_key)
                    );

                    return true;
                }
            });

            editTextPreferencePortForward = (EditTextPreference)findPreference(getResources()
                    .getString(R.string.port_foward_key));

            editTextPreferencePortForward.setText(appSharedPreferenceManager.getSharedPreferenceForKey(getResources()
            .getString(R.string.port_foward_key)));

            editTextPreferencePortForward.setSummary(appSharedPreferenceManager.getSharedPreferenceForKey(getResources()
                    .getString(R.string.port_foward_key)));

            editTextPreferencePortForward.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object object) {
                    appSharedPreferenceManager.storeOnSharedPreference(getResources().getString(R.string.port_foward_key), (String)object);

                    editTextPreferencePortForward.setText(appSharedPreferenceManager.getSharedPreferenceForKey(getResources()
                            .getString(R.string.port_foward_key)));

                    editTextPreferencePortForward.setSummary(appSharedPreferenceManager.getSharedPreferenceForKey(getResources()
                            .getString(R.string.port_foward_key)));
                    return true;
                }
            });

            editTextPreferenceMulticastIpOne =(EditTextPreference) findPreference(getResources().getString(R.string.multi_cast_socket_one_key));
            editTextPreferenceMulticastIpOne.setText(appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_one_key)));
            editTextPreferenceMulticastIpOne.setSummary(appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_one_key)));
            editTextPreferenceMulticastIpOne.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object object) {
                    appSharedPreferenceManager.storeOnSharedPreference( getResources().getString(R.string.multi_cast_socket_one_key), (String)object);
                    editTextPreferenceMulticastIpOne.setText(appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_one_key)));
                    editTextPreferenceMulticastIpOne.setSummary(appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_one_key)));
                    return true;
                }
            });

            editTextPreferenceMulticastIpTwo = (EditTextPreference) findPreference(getResources().getString(R.string.multi_cast_socket_two_key));
            editTextPreferenceMulticastIpTwo.setText( appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_two_key)));
            editTextPreferenceMulticastIpTwo.setSummary( appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_two_key)));

            editTextPreferenceMulticastIpTwo.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object o) {
                    appSharedPreferenceManager.storeOnSharedPreference(getResources().getString(R.string.multi_cast_socket_two_key), (String)o);
                    editTextPreferenceMulticastIpTwo.setText( appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_two_key)));
                    editTextPreferenceMulticastIpTwo.setSummary( appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_two_key)));
                    return true;
                }
            });
        }
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==android.R.id.home){
            Intent homeIntent = new Intent(this, MainActivity.class);
            startActivity(homeIntent);
            return  true;
        }
        return true;
    }
}
