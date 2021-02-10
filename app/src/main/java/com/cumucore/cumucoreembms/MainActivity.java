package com.cumucore.cumucoreembms;

import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    static String EWUSERNAME = "EweMBMSSampleCodeAppMainActivity";
    private File mServerWorkdir;
    AlertDialog alertDialog = null;
    private String mMspPackageName;
    private boolean mBound = false;


    private static final String REGEX_IP = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        // setContentView(R.layout.activity_main);

        setContentView(getResources().getIdentifier("activity_main", "layout", getPackageName()));
        // Preferences Init
        PreferenceManager.setDefaultValues(this, getResources().getIdentifier("preferences", "xml", getPackageName()), false);
        Log.i(EWUSERNAME, "<-- onCreate()");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId()==R.id.setting){
           Intent intent = new Intent(this, ActivitySetting.class);
           startActivity(intent);
        }
        return true;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        Log.i(EWUSERNAME, "--> onPostCreate()");
        super.onPostCreate(savedInstanceState);
        startAppActivity();
        Log.i(EWUSERNAME, "<-- onPostCreate()");
    }

    @Override
    protected void onStop() {
        Log.i(EWUSERNAME, "--> onStop()");
        super.onStop();
        Log.i(EWUSERNAME, "<-- onStop()");
    }

    @Override
    protected void onDestroy() {
        Log.i(EWUSERNAME, "--> onDestroy()");
        super.onDestroy();
        stopServer();
        Log.i(EWUSERNAME, "<-- onDestroy()");
        onBackPressed(); // kill app for good

    }

    @Override
    public void onBackPressed() {

        android.os.Process.killProcess(android.os.Process.myPid());
    }

    protected boolean isValidIP(final String ip) {
        if (!ip.isEmpty()) {
            Pattern pattern = Pattern.compile(REGEX_IP);
            Matcher matcher = pattern.matcher(ip);
            return matcher.matches();
        }
        return false;
    }

    private boolean checkFirstRun() {

        final String PREFS_NAME = "VersionsPrefs";
        final String PREF_VERSION_NAME_KEY = "version_name";
        final String DOESNT_EXIST = "";

        // Get current version name
        String currentVersionName = "SampleCodeApp";

        // Get saved version code
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String savedVersionName = prefs.getString(PREF_VERSION_NAME_KEY, DOESNT_EXIST);

        // Update the shared preferences with the current version code
        prefs.edit().putString(PREF_VERSION_NAME_KEY, currentVersionName).apply();

        Log.w(EWUSERNAME, "Check application version [Version Name :" + currentVersionName + "]");
        // Check for first run or upgrade
        if (currentVersionName.equals(savedVersionName)) {
            // This is just a normal run
            return false;

        }
        // else This is a new install
        //           or an upgrade
        //           or the user cleared the shared preferences
        return true;

    }

    private void startAppActivity() {
        Log.i(EWUSERNAME, "--> startAppActivity");
        final SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        Fragment fragment = null;
        String mspServer_ip = mySharedPreferences.getString("pref_key_ip", null);
        String mspServer_port = mySharedPreferences.getString("pref_key_port", null);

        //Check if MSP Network Interface is a local Network Interface
        boolean bFound = false;
        try {
            InetAddress inetaddress = InetAddress.getByName(mspServer_ip);
            if (inetaddress != null) {
                NetworkInterface netInterface = NetworkInterface.getByInetAddress(inetaddress);
                if (netInterface != null) {
                    bFound = true;
                    Log.i(EWUSERNAME, "MSP Network Interface found on local: " + mspServer_ip + ": " + netInterface.getDisplayName());
                }
            }
        } catch (SocketException e) {
            Log.w(EWUSERNAME, e.toString());
        } catch (UnknownHostException e) {
            Log.w(EWUSERNAME, e.toString());
        }

        // Start MSP engine only if allowed by application configuration
        if (bFound) {
            Log.i(EWUSERNAME, "MSP service will simultaneously launched with the Application");
            startServer();
        } else {
            Log.i(EWUSERNAME, "MSP Network Interface (" + mspServer_ip + ") not found on local.");
            // App launched with a remote Expway Middleware service
            Log.i(EWUSERNAME, "MSP Web server is not started as per application configuration");
        }

        fragment = new SampleCodeFragment();
        getSupportFragmentManager().beginTransaction().replace(getResources().getIdentifier("container", "id", getPackageName()), fragment).commitAllowingStateLoss();

        Log.i(EWUSERNAME, "<-- startAppActivity");
    }

    /**
     * Get explicit Intent
     */
    private Intent getExplicitIntent(String action) throws IOException {
        Log.v(EWUSERNAME, "-->getExplicitIntent()");

        PackageManager pm = this.getPackageManager();
        Intent implicitIntent = new Intent(action);
        List<ResolveInfo> resolveInfos = pm.queryIntentServices(implicitIntent, 0);

        int count = resolveInfos.size();

        if (resolveInfos == null || count != 1) {
            Log.e(EWUSERNAME, ((count < 1) ? "No service found!" : "Multiple services found! [count: " + count + "]"));
            throw new IOException("Impossible to query Intent Services to start MSP server!");
        }

        ResolveInfo serviceInfo = resolveInfos.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;

        Log.v(EWUSERNAME, "[Service PackageName: " + packageName + "]");
        Log.v(EWUSERNAME, "[Service Name: " + className + "]");

        ComponentName component = new ComponentName(packageName, className);

        Intent explicitIntent = new Intent();
        explicitIntent.setComponent(component);

        Log.v(EWUSERNAME, "<--getExplicitIntent()");

        return explicitIntent;
    }

    /*
     * Start MSP server. APK should be installed first !
     */
    private void startServer() {
        Log.i(EWUSERNAME, "startServer() => bind");

        alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(Html.fromHtml("<font color='#000000'>MSP Server Initialization</font>"));
        alertDialog.setMessage("Waiting for connection... is MSP Server APK installed ?");
        alertDialog.setIcon(android.R.drawable.ic_dialog_info);
        alertDialog.setCancelable(true);
        alertDialog.show();

        try {
            mMspPackageName = getString(getResources().getIdentifier("ew_msp_package_name", "string", getPackageName()));
            mServerWorkdir = new File(Environment.getExternalStorageDirectory(),
                    getString(getResources().getIdentifier("ew_msp_workdir_name", "string", getPackageName())));

            // Use explicit instead of implicit Intent for android 5.0 compliance (targetSdkVersion >= Level 21)
            Intent intent = getExplicitIntent(mMspPackageName + ".START");

            if (intent != null) {
                String lWorkdir = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_key_msp_workdir", mServerWorkdir.getPath());

                Log.i(EWUSERNAME, "MSP Server:     [Server package name:     " + mMspPackageName + "]");
                // Log Parts of shared Preferences set
                // MSP server settings part
                String lServerURL = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_key_ip", null);
                if (lServerURL != null && !lServerURL.isEmpty()) {
                    Log.i(EWUSERNAME, "MSP Server:     [Server address:          " + lServerURL + "]");
                } else {
                    Log.i(EWUSERNAME, "MSP Server:     [Server address:          empty]");
                }

                String lServerPort = PreferenceManager.getDefaultSharedPreferences(this).getString("pref_key_port", null);
                if (lServerPort != null && !lServerPort.isEmpty()) {
                    int lIntServerPort = Integer.parseInt(lServerPort);
                    Log.i(EWUSERNAME, "MSP Server:     [Server Port:             " + lIntServerPort + "]");
                } else {
                    Log.i(EWUSERNAME, "MSP Server:     [Server Port:             empty]");
                }

                // LIVE STREAMING settings part
                Log.i(EWUSERNAME, "LIVE STREAMING: [Close streams:           " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_close_stream", false) + "]");
                Log.i(EWUSERNAME, "LIVE STREAMING: [Delay open:              " + Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString("pref_delay_open", "0")) + "]");
                // MISC settings part
                Log.i(EWUSERNAME, "MISC:           [show Expway logo:        " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_with_logo", true) + "]");
                // DEBUG settings part
                Log.i(EWUSERNAME, "DEBUG:          [Live Chronometers:       " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_with_chrono", false) + "]");
                Log.i(EWUSERNAME, "DEBUG:          [AT Commands Test engine: " + PreferenceManager.getDefaultSharedPreferences(this).getBoolean("pref_active_ATCMD_test", false) + "]");

                if (lWorkdir != null && lWorkdir.isEmpty())
                    lWorkdir = mServerWorkdir.getPath();

                intent.putExtra("com.expway.embmsserver.EweMBMSServerWorkdir", lWorkdir);
                intent.putExtra("EweMBMSServerWorkdir", lWorkdir); // For backward compatibility, will be removed.

                ComponentName compname = null;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    compname = this.startForegroundService(intent);
                } else {
                    compname = this.startService(intent);
                }

                if (compname == null) {
                    Log.e(EWUSERNAME, "eMBMS service not found!");
                } else {
                    Log.v(EWUSERNAME, "Bind to eMBMS service");
                    if (!mBound) {
                        mBound = true;

                        if (this.bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
                            Log.i(EWUSERNAME, "eMBMS service started and bound.");
                        } else {
                            Log.e(EWUSERNAME, "Impossible to bind to the eMBMS service!");
                        }
                    } else {
                        Log.i(EWUSERNAME, "Already bound to the eMBMS service.");
                    }

                    if (alertDialog != null) {
                        alertDialog.dismiss();
                    }
                }
            } else {
                Log.e(EWUSERNAME, "Cannot get Explicit Intent to start and bind to the eMBMS service!");

                alertDialog = new AlertDialog.Builder(this).create();
                alertDialog.setTitle(Html.fromHtml("<font color='#000000'>Warning</font>"));
                alertDialog.setMessage("eMBMS server is not available for EweMBMSApps run !");

                alertDialog.setIcon(android.R.drawable.ic_dialog_alert);
                alertDialog.setButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        alertDialog.dismiss();
                    }
                });

                alertDialog.show();
            }
        } catch (IOException e) {
            Log.w(EWUSERNAME, e.getMessage());
            Log.w(EWUSERNAME, e.toString());
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(EWUSERNAME, "--> onServiceConnected");
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.i(EWUSERNAME, "<-- onServiceDisconnected");
            mBound = false;
        }
    };

    private void stopServer() {
        Log.i(EWUSERNAME, "--> stopServer()");

        try {
            // Use explicit instead of implicit Intent for android 5.0 compliance (targetSdkVersion >= Level 21)
            Intent intent = getExplicitIntent(mMspPackageName + ".STOP");

            if (intent != null) {
                if (this.stopService(intent)) {
                    Log.i(EWUSERNAME, "eMBMS stopService called ...");

                    if (mBound) {
                        mBound = false;
                        this.unbindService(mConnection);
                        Log.i(EWUSERNAME, "eMBMS service unbound.");
                    } else {
                        Log.i(EWUSERNAME, "No eMBMS service to unbind!");
                    }
                } else {
                    Log.e(EWUSERNAME, "Impossible to stop to the eMBMS service!");
                }
            } else {
                Log.e(EWUSERNAME, "Cannot get Explicit Intent to stop and unbind to the eMBMS service!");
            }
        } catch (IOException e) {
            Log.w(EWUSERNAME, e.toString());
        }

        Log.i(EWUSERNAME, "<-- stopServer()");
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        Log.v(EWUSERNAME, "onRestoreInstanceState");
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        Log.v(EWUSERNAME, "onSaveInstanceState");

        super.onSaveInstanceState(outState);
    }
}
