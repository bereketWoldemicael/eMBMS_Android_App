package com.cumucore.cumucoreembms;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ScrollView;
import android.widget.TextView;
import android.support.v4.app.*;
import android.widget.VideoView;

import com.cumucore.cumucoreembms.interfaces.AppSharedPreferenceManager;
import com.cumucore.cumucoreembms.interfaces.AsyncTaskCompleteListener;
import  com.cumucore.cumucoreembms.interfaces.IPacketListener;
import com.cumucore.cumucoreembms.tools.MulticastReceiver;
import com.expway.msp.EServiceType;
import com.expway.msp.IMsp3gppStreamingManager;
import com.expway.msp.IMspAcquisition;
import com.expway.msp.IMspStreamingManager;
import com.expway.msp.IMspEngine;
import com.expway.msp.IMspEsgManager;
import com.expway.msp.IMspFileCastingManager;
import com.expway.msp.IMspLiveManager;
import com.expway.msp.MspControl;
import com.expway.msp.MspException;
import com.expway.msp.MspRegistrationParameters;
import com.expway.msp.Schedule;
import com.expway.msp.ScheduleFile;
import com.expway.msp.Service;
import com.expway.msp.event.acquisition.AcquisitionCompletedEvent;
import com.expway.msp.event.acquisition.AcquisitionErrorEvent;
import com.expway.msp.event.acquisition.AcquisitionEvent;
import com.expway.msp.event.acquisition.AcquisitionProgressEvent;
import com.expway.msp.event.acquisition.IAcquisitionListener;
import com.expway.msp.event.acquisition.IMetadataListener;
import com.expway.msp.event.acquisition.MetadataEvent;
import com.expway.msp.event.connection.ConnectionEvent;
import com.expway.msp.event.connection.DisconnectionEvent;
import com.expway.msp.event.connection.IMspConnectionListener;
import com.expway.msp.event.connection.ProtocolErrorEvent;
import com.expway.msp.event.modem.ModemEvent;
import com.expway.msp.event.modem.ModemTypeEvent;
import com.expway.msp.event.registration.IRegistrationListener;
import com.expway.msp.event.registration.RegistrationErrorEvent;
import com.expway.msp.event.registration.RegistrationEvent;
import com.expway.msp.event.service.AppServiceDescription;
import com.expway.msp.event.service.IService3gppStreamingListener;
import com.expway.msp.event.service.IServiceFileCastingListener;
import com.expway.msp.event.service.IServiceLiveListener;
import com.expway.msp.event.service.IServiceStreamingListener;
import com.expway.msp.event.service.Service3gppStreamingEvent;
import com.expway.msp.event.service.Service3gppStreamingOpenedEvent;
import com.expway.msp.event.service.ServiceStreamingEvent;
import com.expway.msp.event.service.ServiceStreamingOpenedEvent;
import com.expway.msp.event.service.ServiceAvailabilityChangeEvent;
import com.expway.msp.event.service.ServiceFileCastEvent;
import com.expway.msp.event.service.ServiceFileDownloadFailEvent;
import com.expway.msp.event.service.ServiceFileDownloadProgressEvent;
import com.expway.msp.event.service.ServiceFileDownloadStopEvent;
import com.expway.msp.event.service.ServiceLiveBadPresentationEvent;
import com.expway.msp.event.service.ServiceLiveCloseEvent;
import com.expway.msp.event.service.ServiceLiveEvent;
import com.expway.msp.event.service.ServiceLiveMpdReadyEvent;
import com.expway.msp.event.service.ServiceLiveReadyEvent;
import com.expway.msp.event.service.ServiceLiveServiceQualityIndicationEvent;
import com.expway.msp.event.service.ServiceLiveTransmissionModeEvent;

import com.expway.msp.event.modem.IModemListener;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Sample code fragment.
 *
 * @author Expway
 */
public class SampleCodeFragment extends Fragment implements IAcquisitionListener, IRegistrationListener,
        AsyncTaskCompleteListener<Service[]>, IMetadataListener, IServiceLiveListener, IModemListener,
        IServiceFileCastingListener, IServiceStreamingListener, IPacketListener, IMspConnectionListener, SurfaceHolder.Callback {

    private static String EWUSERNAME = "SampleCodeFragment";
    // No service class is used: to be changed if needed
    protected static final String[] DEFAULT_SERVICE_CLASS_TYPES = {};

    private static TextView tLogText;
    private static ScrollView svLog;
    private static TextView mReceivedPacketCounter;

    private boolean isMspClientRegistered = false;
    private boolean isModemEnabled = false;
    private boolean hasMetadataChanged = false;
    private boolean closePslteFired = false;

    private IMspEngine mIEngine = null;
    private MspControl mMspControl = null;
    private IMspAcquisition mAcq = null;
    private IMspEsgManager mIESG = null;
    private IMspLiveManager mILive = null;
    private IMspFileCastingManager mIFC = null;
    private IMspStreamingManager mIRTP = null;


    private GetServicesTask taskServices = null;
    private StartEmbmsTask taskEmbms = null;
    private ConnectTask taskConnect = null;

    private static MulticastReceiver[] receivers;
    private static ReceiveTask[] recv_tasks;

    private Service services[] = null;
    private Service serviceDASH = null;
    private Service serviceHLS = null;
    private Service serviceFC = null;
    //todo added by bereket
    DatagramSocket socketSender;
    // todo
    /****
     *  change the IP Address that you want to forward and port number
     */
    private static  String ip_forward = "192.168.9.193";
    private static  int port_forward = 8888;

    private InetAddress group_forward;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder holder;
    private static final String TAG = "MBMS_PLAYER";

    // todo end added by bereket


    // Hardcoded parameters for MCPTT: to be changed according to the actual RTP stream
    private static String group1 = "224.0.1.190:5001"; // ip:port
    private static String group2 = "225.1.0.1:5004";

    private static int tmgi = 21165205; // the hexa formated tmgi

    private static String pslteServiceId = "pslteId"; // just an id2
    private static int maxRTPCounter = 100;

    private boolean isVLCStarted = false;

    static Handler serviceMessageHandler = new Handler() {
        public void handleMessage(Message msg) {
            String textMessage = (String) msg.obj;

            tLogText.append(textMessage);
            svLog.post(new Runnable() {
                public void run() {
                    svLog.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(EWUSERNAME, "==> onCreateView");
        AppSharedPreferenceManager appSharedPreferenceManager = AppSharedPreferenceManager
                .getAppSharedPreferenceManager(getContext());

         ip_forward= appSharedPreferenceManager
                .getSharedPreferenceForKey(getResources().getString(R.string.ip_forward_key));

         try {
             port_forward = Integer.parseInt( appSharedPreferenceManager
                     .getSharedPreferenceForKey(getResources().getString(R.string.ip_forward_key)));
         }catch (Exception ex){
             Log.i(TAG, "onCreateView: ");
         }
         String g1 = appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_one_key));
         if(!g1.isEmpty()){
             group1 =g1;
         }
         String g2 = appSharedPreferenceManager.getSharedPreferenceForKey(getResources().getString(R.string.multi_cast_socket_two_key));

         if(!g2.isEmpty()){
             group2=g2;
         }

        isVLCStarted=false;
        inflater.inflate(R.layout.samplecode_fragment, container);
        SurfaceView surfaceView = container.findViewById(R.id.surfaceview_video);


        // mediaPlayer = MediaPlayer.create(getContext(), R.raw.source);
        mediaPlayer = new MediaPlayer();

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // start wrting in the cache
                Log.d(TAG, "MP Finished playing");
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Log.d(TAG, what+ " error");
                Log.d(TAG, extra+ " error");
                return false;
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        holder = surfaceView.getHolder();
        holder.addCallback(this);


        try {
            socketSender = new DatagramSocket();
            group_forward = InetAddress.getByName(ip_forward);
        } catch (SocketException e) {
            // todo show the error in ui
            e.printStackTrace();
        } catch (UnknownHostException e) {
            // todo show the error on UI
            e.printStackTrace();
        }


        View result;
        // Inflate the layout for this fragment
        result = inflater.inflate(getResources().getIdentifier("samplecode_fragment", "layout",
                getActivity().getPackageName()), container, false); //R.layout.samplecode_fragment

        buttonStartInit(result);
        buttonExitInit(result);
        scrollViewInit(result);
        textViewInit(result);

        return result;
    }

    @Override
    public void onDestroyView() {
        Log.i(EWUSERNAME, "==> onDestroyView");
        super.onDestroyView();

        socketSender.close();

        removeListeners();
        Log.i(EWUSERNAME, "<== onDestroyView");
    }

    /**
     * Called when the activity is about to be destroyed.
     */
    @Override
    public void onDestroy() {
        Log.i(EWUSERNAME, "==> onDestroy");
        super.onDestroy();
        disconnect();
        Log.i(EWUSERNAME, "<== onDestroy");
    }

    private void textViewInit(View v) {
        mReceivedPacketCounter = (TextView) v.findViewById(getResources().getIdentifier("textPacketCount", "id", this.getActivity().getPackageName()));
    }


    /*
     * #############################################################################################
     * Helper methods, including asynchronous tasks
     * #############################################################################################
     */

    private void scrollViewInit(View v) {
        tLogText = (TextView) v.findViewById(getResources().getIdentifier("tLogText", "id", getActivity().getPackageName()));
        svLog = (ScrollView) v.findViewById(getResources().getIdentifier("scrollView1", "id", getActivity().getPackageName()));
        tLogText.setMovementMethod(new ScrollingMovementMethod());
    }

    /*
     * Add MW listeners
     */
    private void addListeners() {
        Log.i(EWUSERNAME, "==> addListeners");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> addListeners \n"));
        mIEngine.addModemListener(this);
        mIEngine.addRegistrationListener(this);
        mIEngine.addConnectionListener(this);
        mAcq.addAcquisitionListener(this);
        mAcq.addMetadataListener(this);
        mILive.addServiceLiveListener(this);
        mIFC.addServiceFileCastingListener(this);
        mIRTP.addServiceStreamingListener(this);

        Log.i(EWUSERNAME, "<== addListeners");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "<== addListeners \n"));
    }

    /*
     * Remove MW listeners
     */
    private void removeListeners() {
        Log.i(EWUSERNAME, "==> removeListeners");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> removeListeners \n"));
        if (mIEngine != null) {
            mIEngine.removeModemListener(this);
            mIEngine.removeRegistrationListener(this);
            mIEngine.removeConnectionListener(this);
        }
        if (mAcq != null) {
            mAcq.removeAcquisitionListener(this);
            mAcq.removeMetadataListener(this);
        }
        if (mILive != null) {
            mILive.removeServiceLiveListener(this);
        }
        if (mIFC != null) {
            mIFC.removeServiceFileCastingListener(this);
        }
        if (mIRTP != null) {
            mIRTP.removeServiceStreamingListener(this);
        }

        Log.i(EWUSERNAME, "<== removeListeners");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "<== removeListeners \n"));
    }

    private void buttonStartInit(View v) {
        ((Button) v.findViewById(getResources().getIdentifier("buttonStart", "id",
                getActivity().getPackageName()))).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.i(EWUSERNAME, "Start sample code");

                // start the whole sample code scenario
                startScenario();
            }
        });
    }

    private void buttonExitInit(View v) {
        ((Button) v.findViewById(getResources().getIdentifier("buttonExit", "id",
                getActivity().getPackageName()))).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                closePslteFired=true;
                getActivity().finish();
            }
        });
    }

    /*
     * Start the sample code scenario
     */
    private void startScenario() {

        // get MW handlers
        mMspControl = MspControl.getInstance();
        mIEngine = mMspControl.getEngineInterface();
        mAcq = mMspControl.getAcquisitionInterface();
        mIESG = mMspControl.getEsgManagerInterface();
        mILive = mMspControl.getInstance().getLiveManagerInterface();
        mIFC = mMspControl.getInstance().getFileCastingManagerInterface();
        mIRTP = mMspControl.getInstance().getStreamingManagerInterface();

        // get the ip/port of the MSP server
        final SharedPreferences mySharedPreferences = PreferenceManager.getDefaultSharedPreferences(this.getContext());
        String mspServer_ip = mySharedPreferences.getString("pref_key_ip", null);
        String mspServer_port = mySharedPreferences.getString("pref_key_port", null);

        Log.i(EWUSERNAME, "==> Start scenario ");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> Start scenario \n"));

        // MSP URL is given by ip:port/ewmsp
        String mspUrlStr = "http://" + mspServer_ip + ":" + mspServer_port + "/ewmsp";
        URL mspServerURL = null;
        try {
            mspServerURL = new URL(mspUrlStr);
        } catch (MalformedURLException e) {
            Log.w(EWUSERNAME, e.toString());
        }

        // build app_id (arbitrary parameters here)
        MspRegistrationParameters app_id = new MspRegistrationParameters("Expway", "sampleCodeApp",
                null, null, DEFAULT_SERVICE_CLASS_TYPES);

        // add listeners for MW events
        addListeners();

        // connect/register MSP Client to MSP Server
        connect(mspServerURL, app_id);

        // start eMBMS service (along with acquisition)
        startEmbms();

        // open RTP service for given tmgi: does not require acquisition
        // should wait for embms-capable modem to be enabled
        openPSLTEService(tmgi);

        // get Live/filecasting services from announcement and open 1st service / download 1st file of each type
        // wait for acquisition to be finished
        getAndOpenServices(this, EServiceType.HLS);
        getAndOpenServices(this, EServiceType.DASH);
        getAndOpenServices(this, EServiceType.FILE);
        //  getAndOpenServices(this, EServiceType.STREAMING_RTP);
    }

    /*
     * Connect MSP client to MSP server and register
     */
    public void connect(URL mspServerURL, MspRegistrationParameters app_id) {
        taskConnect = new ConnectTask(mspServerURL, app_id);
        taskConnect.execute();
    }


    private class ConnectTask extends AsyncTask<Void, Void, Void> {
        private URL mspServerURL;
        private MspRegistrationParameters app_id;

        public ConnectTask(URL mspServerURL, MspRegistrationParameters app_id) {
            this.mspServerURL = mspServerURL;
            this.app_id = app_id;
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.i(EWUSERNAME, "==> Connect MSP Client to " + mspServerURL.toString());
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "==> Connect MSP Client to " + mspServerURL.toString() + "\n"));
                mIEngine.connect(mspServerURL, app_id);
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    public void disconnect() {
        DisconnectTask task = new DisconnectTask();
        task.execute();
    }

    private class DisconnectTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                Log.i(EWUSERNAME, "==> Disconnect MSP Client");
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "==> Disconnect MSP Client" + "\n"));
                if (mIEngine != null) {
                    mIEngine.disconnect();
                }
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    /*
     * Start eMBMS service (enable modem) and acquisition
     */
    public void startEmbms() {
        taskEmbms = new StartEmbmsTask();
        taskEmbms.execute();
    }

    private class StartEmbmsTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                // wait for registration success
                while (!isMspClientRegistered) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Log.w(EWUSERNAME, e.toString());
                    }
                }
                Log.i(EWUSERNAME, "<== MSP Client is registered");
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "<== MSP Client is registered" + "\n"));

                Log.i(EWUSERNAME, "==> Start eMBMS service");
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "==> Start eMBMS service" + "\n"));

                mIEngine.start();
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    /*
     * Get and open services following acquisition (by service type here)
     * Wait for metadata change
     */
    public void getAndOpenServices(AsyncTaskCompleteListener<Service[]> cb, EServiceType type) {
        taskServices = new GetServicesTask(cb, type);
        taskServices.execute();
    }

    private class GetServicesTask extends AsyncTask<Void, Void, Void> {
        private AsyncTaskCompleteListener<Service[]> callback;
        private EServiceType typeService;

        public GetServicesTask(AsyncTaskCompleteListener<Service[]> cb, EServiceType type) {
            this.callback = cb;
            this.typeService = type;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // wait for metadata changes
            while (!hasMetadataChanged) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.w(EWUSERNAME, e.toString());
                }
            }

            Log.i(EWUSERNAME, "==> Get the list of " + this.typeService + " services");
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "==> Get the list of " + this.typeService + " services" + "\n"));

            getServiceList(this.callback, this.typeService);

            return null;
        }
    }

    /*
     * Get the list of services (by service type here) following metadata change
     * On task complete store 1st service (given the service type)
     * Then open this service
     */
    public void getServiceList(AsyncTaskCompleteListener<Service[]> cb, EServiceType eServiceType) {
        ServiceListTask task = new ServiceListTask(cb, eServiceType);
        task.execute();
    }

    private class ServiceListTask extends AsyncTask<Void, Void, Service[]> {
        private AsyncTaskCompleteListener<Service[]> callback;
        private EServiceType type;

        public ServiceListTask(AsyncTaskCompleteListener<Service[]> cb, EServiceType eServiceType) {
            this.callback = cb;
            this.type = eServiceType;
        }

        @Override
        protected Service[] doInBackground(Void... params) {
            try {
                return mIESG.getServices(type);
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
                return null;
            }
        }

        @Override
        protected void onPostExecute(Service[] result) {
            callback.onTaskComplete(result, this.type);

        }
    }

    @Override
    public void onTaskComplete(Service[] result, EServiceType eServiceType) {

        this.services = result;

        // store 1st service for
        storeAndPrintServices(eServiceType);
        // open  service
        switch (eServiceType) {
            case HLS:
                if (serviceHLS != null)
                    openHLSService();
                break;
            case DASH:
                if (serviceDASH != null)
                    openDASHService();
                break;
            case FILE:
                if (serviceFC != null)
                    openFCService();
                break;
            default:
                break;
        }
    }

    /*
     * Open 1st DASH service
     */
    public void openDASHService() {
        new OpenDASHTask().execute();
    }

    public class OpenDASHTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.i(EWUSERNAME, "<== Got DASH service");
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "<== Got DASH service" + "\n"));

            Log.i(EWUSERNAME, "==> Open DASH service " + serviceDASH.getIdentifier());
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "==> Open DASH service " + serviceDASH.getIdentifier() + "\n"));
            try {
                // just pass the service Id got from acquisition
                mILive.openLiveService(serviceDASH.getIdentifier());
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    /*
     * Open 1st HLS service
     */
    public void openHLSService() {
        new OpenHLSTask().execute();
    }

    public class OpenHLSTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.i(EWUSERNAME, "<== Got HLS service");
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "<== Got HLS service" + "\n"));

            Log.i(EWUSERNAME, "==> Open HLS service " + serviceHLS.getIdentifier());
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "==> Open HLS service " + serviceHLS.getIdentifier() + "\n"));
            try {
                // just pass the service Id got from acquisition
                mILive.openLiveService(serviceHLS.getIdentifier());
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    /*
     * Download 1st FC service / file
     */
    public void openFCService() {
        new openFCTask().execute();
    }

    public class openFCTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            Log.i(EWUSERNAME, "<== Got FC service");
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "<== Got FC service" + "\n"));

            // here we start the download for the 1st file, assuming we are within schedule
            for (Schedule schedule : serviceFC.getSchedules()) {
                // only consider 1st schedule
                if (schedule instanceof ScheduleFile) {
                    ScheduleFile schedule_file = (ScheduleFile) schedule;
                    Log.i(EWUSERNAME, "==> Download file " + schedule_file.getFileUri() + " from service " + serviceFC.getIdentifier());
                    serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                            "==> Download file " + schedule_file.getFileUri() + " from service " + serviceFC.getIdentifier() + "\n"));

                    try {
                        // pass the service Id and file URI got from acquisition
                        mIFC.startDownload(serviceFC.getIdentifier(), schedule_file.getFileUri());
                    } catch (MspException e) {
                        Log.w(EWUSERNAME, e.toString());
                    }
                    break;
                }
            }
            return null;
        }
    }

    /*
     * Open PSLTE service given its tmgi
     */
    public void openPSLTEService(long tmgi) {
        new openRTPTask(tmgi).execute();
    }

    public class openRTPTask extends AsyncTask<String, Void, Void> {

        private long mTmgi;

        public openRTPTask(long tmgi) {
            this.mTmgi = tmgi;
        }

        @Override
        protected Void doInBackground(String... params) {

            // wait for modem activation
            while (!isModemEnabled) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Log.w(EWUSERNAME, e.toString());
                }
            }

            Log.i(EWUSERNAME, "==> Open RTP service tmgi " + mTmgi);
            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                    "==> Open RTP service tmgi " + mTmgi + "\n"));
            try {
                // pass the service Id (arbitrary here), and tmgi
                mIRTP.openStreamingService(pslteServiceId, mTmgi);
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
            return null;
        }
    }

    /*
     * Close a service given its id
     */
    public void closeService(String serviceID) {
        new CloseServiceTask().execute(serviceID);
    }

    private class CloseServiceTask extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            for (String param : params) {
                try {
                    // just pass the service id
                    mILive.closeLiveService(param);
                } catch (MspException e) {
                    Log.w(EWUSERNAME, e.toString());
                }
            }
            return null;
        }
    }

    /*
     * Store 1st service for a given type and print its attributes
     */
    private void storeAndPrintServices(EServiceType eType) {
        if (services != null) {
            for (Service service : services) {

                switch (eType) {
                    case HLS:
                        Log.i(EWUSERNAME, "TYPE " + service.getType());
                        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                "TYPE " + service.getType() + "\n"));
                        for (String langCode : service.getNameLanguageCodes()) {
                            Log.i(EWUSERNAME, "LangCode " + langCode + " - name " + service.getName(langCode));
                            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                    "    LangCode " + langCode + " - name " + service.getName(langCode) + "\n"));
                        }
                        Log.i(EWUSERNAME, service.getName("EN") + " " + service.getIdentifier());
                        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                "    " + service.getIdentifier() + "\n"));

                        if (serviceHLS == null)
                            serviceHLS = service;
                        break;

                    case DASH:
                        Log.i(EWUSERNAME, "TYPE " + service.getType());
                        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                "TYPE " + service.getType() + "\n"));
                        if (service.getMpdUri() != null) {
                            for (String langCode : service.getNameLanguageCodes()) {
                                Log.i(EWUSERNAME, "LangCode " + langCode + " - name " + service.getName(langCode));
                                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                        "    LangCode " + langCode + " - name " + service.getName(langCode) + "\n"));
                            }
                            Log.i(EWUSERNAME, "MPD URI " + service.getMpdUri().toString());
                            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                    "    " + "MPD URI " + service.getMpdUri().toString() + "\n"));
                            Log.i(EWUSERNAME, service.getName("EN") + " " + service.getIdentifier());
                            serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                    "    " + service.getIdentifier() + "\n"));

                            if (serviceDASH == null)
                                serviceDASH = service;
                        }
                        break;

                    case FILE:
                        Log.i(EWUSERNAME, "TYPE " + service.getType());
                        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                "TYPE " + service.getType() + "\n"));
                        if (service.getMpdUri() == null) {
                            for (Schedule schedule : service.getSchedules()) {
                                if (schedule instanceof ScheduleFile) {
                                    ScheduleFile schedule_file = (ScheduleFile) schedule;

                                    Log.i(EWUSERNAME, "fileURI " + schedule_file.getFileUri().toString());
                                    serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                            "    fileURI " + schedule_file.getFileUri().toString() + "\n"));

                                    Log.i(EWUSERNAME, " start " + schedule_file.getGlobalTimeRange().getTimeStart());
                                    serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                            "    start " + schedule_file.getGlobalTimeRange().getTimeStart() + "\n"));

                                    Log.i(EWUSERNAME, " end " + schedule_file.getGlobalTimeRange().getTimeEnd());
                                    serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                            "    end " + schedule_file.getGlobalTimeRange().getTimeEnd() + "\n"));

                                }
                                if (serviceFC == null)
                                    serviceFC = service;
                            }
                        }
                        break;

                    default:
                        Log.i(EWUSERNAME, "Unknown type" + service.getType());
                        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                                "Unknown type" + service.getType() + "\n"));
                        break;

                }
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "---\n"));
            }
        }
    }

    static void ui_updatePacketReceivedText() {
        if (receivers != null && receivers[0] != null && receivers[1] != null)
            mReceivedPacketCounter.setText("RTP packets received: \n" + group1 + ": " + receivers[0].getCountPacket() + "\n"
                    + group2 + ": " + receivers[1].getCountPacket());
    }

    final static Runnable run_updatePacketReceivedText = new Runnable() {
        @Override
        public void run() {
            ui_updatePacketReceivedText();
        }
    };

    void setPacketReceivedText() {
        this.getActivity().runOnUiThread(run_updatePacketReceivedText);
    }


    private synchronized void displayInfos() {
        setPacketReceivedText();
    }

    static class ReceiverInfo {
        volatile String info = "";
    }

    private Map<MulticastReceiver, ReceiverInfo> hm_receiver_info = new HashMap<MulticastReceiver, ReceiverInfo>(); //MAP used to display message

    private class ReceiveTask implements Runnable {
        MulticastReceiver receiver;
        Thread th;

        ReceiveTask(MulticastReceiver receiver) {
            this.receiver = receiver;
        }

        void stop() {
            receiver.stopReceiveLoop();
            th.interrupt();
        }

        void waitStopped() {
            try {
                th.join();
            } catch (InterruptedException e) {
            }
        }

        void start() {
            th = new Thread(this, "Recv-" + receiver.getInfoMulticastAddress());
            th.start();
        }

        private ReceiveTask[] recv_tasks;
        private MulticastReceiver[] receivers;

        private boolean isReceiverStarted() {
            return receivers != null && receivers.length > 0;
        }

        private void stopReceive() {
            if (!isReceiverStarted()) return;
            for (ReceiveTask rt : recv_tasks) rt.stop();
            receivers = null;
            for (ReceiveTask rt : recv_tasks) rt.waitStopped();
            recv_tasks = null;
        }

        @Override
        public void run() {
            //Log.i(EWUSERNAME, "MCR Running task");
            try {
                receiver.receiveLoop();
            } catch (IOException e) {
                e.printStackTrace();
                Log.i(EWUSERNAME, "Thread : IOException " + e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                Log.i(EWUSERNAME, "Thread : Exception " + e.getMessage());
            }

            Log.i(EWUSERNAME, "end of the task");
        }
    }

    /*
     * Open multicast socket on given interface and multicast group (address_and_port)
     */
    private MulticastReceiver startReceiveFrom(int i, NetworkInterface ni, String address_and_port, int buf_len) {
        try {
            MulticastReceiver receiver = new MulticastReceiver(ni, address_and_port, buf_len, this);
            ReceiveTask task = new ReceiveTask(receiver);
            recv_tasks[i] = task;
            task.start();
            return receiver;
        } catch (Exception e) {
            Log.d(EWUSERNAME, "" + e.getMessage());
        }
        return null;
    }

    private boolean isReceiverStarted() {
        return receivers != null && receivers.length > 0;
    }

    private void stopReceive() {
        if (!isReceiverStarted()) return;
        for (ReceiveTask rt : recv_tasks) rt.stop();
        receivers = null;
        for (ReceiveTask rt : recv_tasks) rt.waitStopped();
        recv_tasks = null;
        getActivity().finish();
    }

    /*
     * #############################################################################################
     * Here below we implement callbacks to manage events from the MW
     * #############################################################################################
     */
    @Override
    public void acquisitionEnded(AcquisitionEvent event) {
        Log.i(EWUSERNAME, "Acquisition ended");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0, "EVT : Acquisition ended\n"));
    }

    @Override
    public void acquisitionError(AcquisitionErrorEvent event) {
        Log.i(EWUSERNAME, "Acquisition error");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0, "EVT : Acquisition error\n"));
    }

    @Override
    public void acquisitionProgress(AcquisitionProgressEvent event) {
        Log.i(EWUSERNAME, "Acquisition progress " + event.getProgress());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Acquisition progress " + event.getProgress() + " % \n"));
    }

    @Override
    public void acquisitionSessionCompleted(AcquisitionCompletedEvent event) {
        Log.i(EWUSERNAME, "Acquisition session completed TSI " + event.getTsi());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Acquisition session completed TSI " + event.getTsi() + "\n"));
    }

    @Override
    public void acquisitionStarted(AcquisitionEvent event) {
        Log.i(EWUSERNAME, "Acquisition started");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0, "EVT : Acquisition started\n"));
    }

    @Override
    public void registrationError(RegistrationErrorEvent event) {
        Log.i(EWUSERNAME, "Registration error " + event.getApplicationIdentity() + " " + event.getType());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Registration error " + event.getApplicationIdentity() + " " + event.getType() + " cause " + event.getCause() + "\n"));
    }

    @Override
    public void registrationNotAllowed(RegistrationEvent event) {
        Log.i(EWUSERNAME,
                "Registration not allowed " + event.getApplicationIdentity() + " " + event.getType());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Registration not allowed " + event.getApplicationIdentity() + " " + event.getType() + "\n"));
    }

    @Override
    public void registrationStarted(RegistrationEvent event) {
        Log.i(EWUSERNAME, "Registration started " + event.getApplicationIdentity());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Registration started " + event.getApplicationIdentity() + "\n"));
    }

    @Override
    public void registrationSucceed(RegistrationEvent event) {

        Log.i(EWUSERNAME, "Registration success " + event.getApplicationIdentity());
        isMspClientRegistered = true;
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Registration succeed " + event.getApplicationIdentity() + "\n"));
    }

    @Override
    public void metadataChanged(MetadataEvent e) {
        hasMetadataChanged = true;
        Log.i(EWUSERNAME, "Metadata " + e.getType());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Metadata " + e.getType() + "\n"));
    }

    @Override
    public void serviceAvailabilityChanged(ServiceAvailabilityChangeEvent event) {
        Log.i(EWUSERNAME, "EVT : Service availability of " + event.getServiceId() + " changed to Available: " + event.isAvailable());
    }

    /**
     * @deprecated Use instead liveReady.
     */
    @Override
    @Deprecated
    public void liveMpdReady(ServiceLiveMpdReadyEvent event) {
    }

    @Override
    public void liveReady(ServiceLiveReadyEvent event) {
        AppServiceDescription[] serviceDescription = event.getAppServiceDescription();
        for (AppServiceDescription desc : serviceDescription) {
            if (desc instanceof AppServiceDescription) {
                Log.i(EWUSERNAME, "liveReady() - Live service " + event.getServiceId() + " ready to play " + desc.getUrl());
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "EVT : liveReady() - Live service " + event.getServiceId() + " ready to play " + desc.getUrl() + "\n"));

                // This URL can now be passed to a player
                // here we close the service right away instead
                Log.i(EWUSERNAME, "==> Close Live service " + event.getServiceId());
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "==> Close Live service " + event.getServiceId() + "\n"));
                closeService(event.getServiceId());
            }
        }
    }

    @Override
    public void liveOpened(ServiceLiveEvent e) {
        Log.i(EWUSERNAME, "liveOpened(): Live service " + e.getServiceId() + " opened");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : liveOpened(): Live service " + e.getServiceId() + " opened" + "\n"));
    }

    @Override
    public void liveServiceBadPresentation(ServiceLiveBadPresentationEvent event) {
        Log.i(EWUSERNAME, "EVT : Bad service presentation for " + event.getServiceId());
    }

    @Override
    public void liveServiceQualityIndication(ServiceLiveServiceQualityIndicationEvent event) {
        Log.i(EWUSERNAME, "EVT : Service quality indicator is " + (event.isSuccess() ? "SUCCESS" : "FAILURE") + " for " + event.getServiceId());
    }

    @Override
    public void liveTransmissionMode(ServiceLiveTransmissionModeEvent event) {
        Log.i(EWUSERNAME, "EVT : liveTransmissionMode - Live service: " + event.getServiceId() + ", Transmission mode: " + event.getMode().getMessage());
    }

    @Override
    public void liveClosed(ServiceLiveCloseEvent event) {
        Log.i(EWUSERNAME, "liveClosed(): Live service " + event.getServiceId() + " closed [cause: " + event.getCause() + "]");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : liveClosed(): Live service " + event.getServiceId() + " closed [cause: " + event.getCause() + "]" + "\n"));
    }

    @Override
    public void fileDownloadCancelled(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Download cancelled " + event.getServiceId() + "-" + event.getFileUri());
    }

    @Override
    public void fileDownloadCompleted(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Download Completed " + event.getServiceId() + "-" + event.getFileUri());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Download Completed " + event.getServiceId() + "-" + event.getFileUri() + "\n"));
    }

    @Override
    public void fileDownloadFailed(ServiceFileDownloadFailEvent event) {
        Log.i(EWUSERNAME, "Download Failed " + event.getServiceId() + "-" + event.getFileUri() + ". Cause: " + event.getCause().getMessage() + ".");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Download Failed " + event.getServiceId() + "-" + event.getFileUri() + ". Cause: " + event.getCause().getMessage() + ".\n"));
    }

    @Override
    public void fileDownloadProgress(ServiceFileDownloadProgressEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + "-" + event.getFileUri() + " progress : " + event.getProgress());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Service " + event.getServiceId() + "-" + event.getFileUri() + " progress : " + event.getProgress() + ".\n"));
    }

    @Override
    public void fileDownloadStart(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + "-" + event.getFileUri() + " started");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Service " + event.getServiceId() + "-" + event.getFileUri() + " started\n"));
    }

    @Override
    public void fileDownloadStopped(ServiceFileDownloadStopEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + "-" + event.getFileUri() + " stopped");
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : Service " + event.getServiceId() + "-" + event.getFileUri() + " stopped\n"));
    }

    @Override
    public void fileRepairExpected(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + "-" + event.getFileUri() + " file repair expected");
    }

    @Override
    public void fileRepairCancelled(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + " - file " + event.getFileUri() + " file repair cancelled");
    }

    @Override
    public void fileRepairStarted(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "Service " + event.getServiceId() + "-" + event.getFileUri() + " file repair started");
    }

    @Override
    public void fileRepairByApplication(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "File repair by application for  " + event.getServiceId());
    }

    @Override
    public void fecDecodingEnded(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "FEC decoding ended " + event.getServiceId() + "-" + event.getFileUri());
    }

    @Override
    public void fecDecodingStarted(ServiceFileCastEvent event) {
        Log.i(EWUSERNAME, "FEC decoding started " + event.getServiceId() + "-" + event.getFileUri());
    }

    @Override
    public void streamingOpened(ServiceStreamingOpenedEvent event) {
        Log.i(EWUSERNAME, "serviceStreamingOpened " + event.getServiceId());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : service Streaming opened " + event.getServiceId() + " output on interface " + event.getNetworkInterface().getDisplayName() + "\n"));

        // IP and port are known beforehand
        // Open multicast socket on all relevant IP:port multicast groups
        receivers = new MulticastReceiver[2];
        recv_tasks = new ReceiveTask[2];
        NetworkInterface ni = event.getNetworkInterface();

        Log.i(EWUSERNAME, "==> Open multicast socket on " + group1);
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> Open multicast socket on " + group1 + "\n"));
        MulticastReceiver r1 = startReceiveFrom(0, ni, group1, 1052);

        Log.i(EWUSERNAME, "==> Open multicast socket on " + group2);
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> Open multicast socket on " + group2 + "\n"));
        MulticastReceiver r2 = startReceiveFrom(1, ni, group2, 1052);

        receivers[0] = r1;
        receivers[1] = r2;
    }

    @Override
    public void streamingClosed(ServiceStreamingEvent event) {
        Log.i(EWUSERNAME, "serviceStreamingClosed " + event.getServiceId());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : service Streaming closed " + event.getServiceId() + "\n"));
    }

    @Override
    public void streamingAlreadyOpened(ServiceStreamingOpenedEvent event) {
        Log.i(EWUSERNAME, "serviceStreamingAlreadyOpened " + event.getServiceId());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : service Streaming already opened " + event.getServiceId() + " output on interface " + event.getNetworkInterface().getDisplayName() + "\n"));

        // IP and port are known beforehand
        // Open multicast socket on all relevant IP:port multicast groups
        receivers = new MulticastReceiver[2];
        recv_tasks = new ReceiveTask[2];
        NetworkInterface ni = event.getNetworkInterface();

        Log.i(EWUSERNAME, "==> Open multicast socket on " + group1);
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> Open multicast socket on " + group1 + "\n"));
        MulticastReceiver r1 = startReceiveFrom(0, ni, group1, 1052);

        Log.i(EWUSERNAME, "==> Open multicast socket on " + group2);
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "==> Open multicast socket on " + group2 + "\n"));
        MulticastReceiver r2 = startReceiveFrom(1, ni, group2, 1052);

        receivers[0] = r1;
        receivers[1] = r2;
    }

    @Override
    public void streamingAlreadyClosed(ServiceStreamingEvent event) {
        Log.i(EWUSERNAME, "serviceStreamingAlreadyClosed " + event.getServiceId());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : serviceStreamingAlreadyClosed \n"));
    }

    @Override
    public void packetReceived(MulticastReceiver source, byte[] data, int length) {
        //Log.i(EWUSERNAME, "packetReceived " + receivers[0].getCountPacket());
        displayInfos();

        if (source.equals(receivers[1])) {
            if(!isVLCStarted){
                startVLC();
            }
            // The socketSender forwards the packet to certain Ip address and port number  the packets can be forwarded
            try {
                socketSender.send(new DatagramPacket(data, data.length, group_forward, port_forward));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        //  if (receivers[0].getCountPacket() > maxRTPCounter && !closePslteFired)
        if (closePslteFired) {

            stopReceive();
            try {
                Log.i(EWUSERNAME, "==> Close RTP service " + pslteServiceId);
                serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                        "==> Close RTP service " + pslteServiceId + "\n"));
                mIRTP.closeStreamingService(pslteServiceId);
            } catch (MspException e) {
                Log.w(EWUSERNAME, e.toString());
            }
        }
        if (receivers[1].getCountPacket() > 0) {
            // forwarding it hear
        }

    }


    @Override
    public void connected(ConnectionEvent connectionEvent) {
        Log.i(EWUSERNAME, "connected " + connectionEvent.toString());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : connected " + connectionEvent.toString() + "\n"));
    }

    @Override
    public void disconnected(DisconnectionEvent disconnectionEvent) {
        Log.i(EWUSERNAME, "disconnected " + disconnectionEvent.toString());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : disconnected " + disconnectionEvent.toString() + "\n"));
    }

    @Override
    public void protocolError(ProtocolErrorEvent protocolErrorEvent) {
        Log.i(EWUSERNAME, "protocolError " + protocolErrorEvent.toString());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : protocolError " + protocolErrorEvent.toString() + "\n"));
    }

    @Override
    public void modemDisabled(ModemEvent modemEvent) {
        Log.i(EWUSERNAME, "modemDisabled " + modemEvent.toString());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : modemDisabled " + modemEvent.toString() + "\n"));
    }

    @Override
    public void modemEnabled(ModemEvent modemEvent) {
        Log.i(EWUSERNAME, "modemEnabled " + modemEvent.toString());
        serviceMessageHandler.sendMessage(serviceMessageHandler.obtainMessage(0,
                "EVT : modemEnabled " + modemEvent.toString() + "\n"));
        isModemEnabled = true;
    }

    @Override
    public void modemDeviceOff(ModemEvent modemEvent) {
        Log.i(EWUSERNAME, "modemDeviceOff " + modemEvent.toString());
    }

    @Override
    public void modemNoDevice(ModemEvent modemEvent) {
        Log.i(EWUSERNAME, "modemNoDevice " + modemEvent.toString());
    }

    @Override
    public void modemMaxUEReached(ModemEvent modemEvent) {
        Log.i(EWUSERNAME, "modemMaxUEReached " + modemEvent.toString());
    }

    @Override
    public void modemType(ModemTypeEvent modemTypeEvent) {
        Log.i(EWUSERNAME, "modemType " + modemTypeEvent.toString());
    }


    /**
     * This is called immediately after the surface is first created.
     * Implementations of this should start up whatever rendering code
     * they desire.  Note that only one thread can ever draw into
     * a , so you should not draw into the Surface here
     * if your normal rendering will be in another thread.
     *
     * @param holder The SurfaceHolder whose surface is being created.
     */
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mediaPlayer.setDisplay(holder);



    }

    /**
     * This is called immediately after any structural changes (format or
     * size) have been made to the surface.  You should at this point update
     * the imagery in the surface.  This method is always called at least
     * once, after {@link #surfaceCreated}.
     *
     * @param holder The SurfaceHolder whose surface has changed.
     * @param format The new PixelFormat of the surface.
     * @param width  The new width of the surface.
     * @param height The new height of the surface.
     */
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    /**
     * This is called immediately before a surface is being destroyed. After
     * returning from this call, you should no longer try to access this
     * surface.  If you have a rendering thread that directly accesses
     * the surface, you must ensure that thread is no longer touching the
     * Surface before returning from this function.
     *
     * @param holder The SurfaceHolder whose surface is being destroyed.
     */
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

//    @TargetApi(Build.VERSION_CODES.M)
//    private class ByteArrayMediaSource extends MediaDataSource{
//        byte data[];
//        public ByteArrayMediaSource(){
//
//        }
//        public  ByteArrayMediaSource(byte[] data){
//            this.data = data;
//        }
//
//
//
//        @Override
//        public synchronized int readAt(long position, byte[] buffer, int offset, int size) {
//           System.arraycopy(data, (int)position, buffer, offset, size);
//           return  size;
//        }
//
//
//        @Override
//        public synchronized long getSize() {
//            return data.length;
//        }
//
//        @Override
//        public void close() {
//
//        }
//    }

    public void startVLC(){
        // com.sancel.vlmediaplayer
        Intent intent = getActivity().getPackageManager().getLaunchIntentForPackage("com.sancel.vlmediaplayer");
        if(intent!=null){
            intent.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
            intent.putExtra("title", "IRT GmbH");
            intent.setData(Uri.parse("rtp:////@225.1.0.1:5004"));
            startActivity(intent);
        }
        isVLCStarted=true;
    }
}
