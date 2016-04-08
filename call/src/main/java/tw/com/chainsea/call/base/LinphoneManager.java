package tw.com.chainsea.call.base;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallStats;
import org.linphone.core.LinphoneChatMessage;
import org.linphone.core.LinphoneChatRoom;
import org.linphone.core.LinphoneContent;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneEvent;
import org.linphone.core.LinphoneFriend;
import org.linphone.core.LinphoneInfoMessage;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.PublishState;
import org.linphone.core.SubscriptionState;
import org.linphone.mediastream.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import me.pengtao.ptlog.PtLog;
import tw.com.chainsea.call.R;

/**
 * LinphoneManager
 * Created by 90Chris on 2015/7/1.
 */
public class LinphoneManager implements LinphoneCoreListener.LinphoneListener {

    private static LinphoneManager instance;
    private LinphoneCore mLc;

    private  String mLPConfigXsd = null;
    private  String mLinphoneFactoryConfigFile =null;
    private  String mLinphoneConfigFile = null;
    private  String mLinphoneRootCaFile = null;
    private  String mRingSoundFile = null;
    private  String mRingbackSoundFile = null;
    private  String mPauseSoundFile = null;
    private  String mChatDatabaseFile = null;
    private  String mErrorToneFile = null;
    private static boolean sExited;

    private Context mServiceContext;
    private Resources mR;
    private Timer mTimer;

    private OnLinphoneListener mLinphoneListener = null;

    protected LinphoneManager(final Context c, OnLinphoneListener listener) {
        LinphoneCoreFactory.instance().setDebugMode(true, "LinphoneCall");
        sExited = false;
        mServiceContext = c;

        String basePath = c.getFilesDir().getAbsolutePath();
        mLPConfigXsd = basePath + "/lpconfig.xsd";
        mLinphoneFactoryConfigFile = basePath + "/linphonerc";
        mLinphoneConfigFile = basePath + "/.linphonerc";
        mLinphoneRootCaFile = basePath + "/rootca.pem";
        mRingSoundFile = basePath + "/oldphone_mono.wav";
        mRingbackSoundFile = basePath + "/ringback.wav";
        mPauseSoundFile = basePath + "/toy_mono.wav";
        mChatDatabaseFile = basePath + "/linphone-history.db";
        mErrorToneFile = basePath + "/error.wav";

        mR = c.getResources();

        mLinphoneListener = listener;
    }

    public synchronized static LinphoneManager createAndStart(Context c, OnLinphoneListener listener) {
        if (instance != null)
            throw new RuntimeException("Linphone Manager is already initialized");

        instance = new LinphoneManager(c, listener);
        instance.startLibLinphone(c);

        /*TelephonyManager tm = (TelephonyManager) c.getSystemService(Context.TELEPHONY_SERVICE);
        boolean gsmIdle = tm.getCallState() == TelephonyManager.CALL_STATE_IDLE;
        setGsmIdle(gsmIdle);*/

        return instance;
    }

    public static synchronized LinphoneCore getLcIfManagerNotDestroyedOrNull() {
        if (sExited || instance == null) {
            // Can occur if the UI thread play a posted event but in the meantime the LinphoneManager was destroyed
            // Ex: stop call and quickly terminate application.
            Log.w("Trying to get linphone core while LinphoneManager already destroyed or not created");
            return null;
        }
        return getLc();
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public static synchronized LinphoneCore getLc() {
        return getInstance().mLc;
    }

    public static synchronized LinphoneManager getInstance() {
        if (instance != null) return instance;

        if (sExited) {
            throw new RuntimeException("Linphone Manager was already destroyed. "
                    + "Better use getLcIfManagerNotDestroyed and check returned value");
        }

        throw new RuntimeException("Linphone Manager should be created before accessed");
    }

    private synchronized void startLibLinphone(Context c) {
        try {
            copyAssetsFromPackage();

            mLc = LinphoneCoreFactory.instance().createLinphoneCore(this, mLinphoneConfigFile, mLinphoneFactoryConfigFile, null, c);
            //mLc.addListener((LinphoneCoreListener) c);

            try {
                initLiblinphone();
            } catch (LinphoneCoreException e) {
                Log.e(e);
            }

            TimerTask lTask = new TimerTask() {
                @Override
                public void run() {
                    UIThreadDispatcher.dispatch(new Runnable() {
                        @Override
                        public void run() {
                            if (mLc != null) {
                                mLc.iterate();
                            }
                        }
                    });
                }
            };
			/*use schedule instead of scheduleAtFixedRate to avoid iterate from being call in burst after cpu wake up*/
            mTimer = new Timer("Linphone scheduler");
            mTimer.schedule(lTask, 0, 20);
        }
        catch (Exception e) {
            e.printStackTrace();
            PtLog.e("Cannot start linphone");
        }
    }

    private synchronized void initLiblinphone() throws LinphoneCoreException {
        mLc.setContext(mServiceContext);
        try {
            String versionName = mServiceContext.getPackageManager().getPackageInfo(mServiceContext.getPackageName(), 0).versionName;
            if (versionName == null) {
                versionName = String.valueOf(mServiceContext.getPackageManager().getPackageInfo(mServiceContext.getPackageName(), 0).versionCode);
            }
            mLc.setUserAgent("LinphoneAndroid", versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(e, "cannot get version name");
        }

        mLc.setRing(null);
        mLc.setRootCA(mLinphoneRootCaFile);
        mLc.setPlayFile(mPauseSoundFile);
        mLc.setChatDatabasePath(mChatDatabaseFile);
        //mLc.setCallErrorTone(Reason.NotFound, mErrorToneFile);

        int availableCores = Runtime.getRuntime().availableProcessors();
        PtLog.w("MediaStreamer : " + availableCores + " cores detected and configured");
        mLc.setCpuCount(availableCores);

        int migrationResult = getLc().migrateToMultiTransport();
        PtLog.d("Migration to multi transport result = " + migrationResult);

        mLc.setNetworkReachable(true);

        /*IntentFilter lFilter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        lFilter.addAction(Intent.ACTION_SCREEN_OFF);
        mServiceContext.registerReceiver(mKeepAliveReceiver, lFilter);*/
    }

    private void copyAssetsFromPackage() throws IOException {
        copyIfNotExist(R.raw.oldphone_mono, mRingSoundFile);
        copyIfNotExist(R.raw.ringback, mRingbackSoundFile);
        copyIfNotExist(R.raw.toy_mono, mPauseSoundFile);
        copyIfNotExist(R.raw.incoming_chat, mErrorToneFile);
        copyIfNotExist(R.raw.linphonerc_default, mLinphoneConfigFile);
        copyFromPackage(R.raw.linphonerc_factory, new File(mLinphoneFactoryConfigFile).getName());
        copyIfNotExist(R.raw.lpconfig, mLPConfigXsd);
        copyIfNotExist(R.raw.rootca, mLinphoneRootCaFile);
    }

    public void copyIfNotExist(int ressourceId, String target) throws IOException {
        File lFileToCopy = new File(target);
        if (!lFileToCopy.exists()) {
            copyFromPackage(ressourceId,lFileToCopy.getName());
        }
    }

    public void copyFromPackage(int ressourceId, String target) throws IOException{
        FileOutputStream lOutputStream = mServiceContext.openFileOutput (target, 0);
        InputStream lInputStream = mR.openRawResource(ressourceId);
        int readByte;
        byte[] buff = new byte[8048];
        while (( readByte = lInputStream.read(buff)) != -1) {
            lOutputStream.write(buff,0, readByte);
        }
        lOutputStream.flush();
        lOutputStream.close();
        lInputStream.close();
    }

    public static synchronized void destroy() {
        if (instance == null) return;
        //getInstance().changeStatusToOffline();
        sExited = true;
        instance.doDestroy();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void doDestroy() {
        try {
            mTimer.cancel();
            mLc.destroy();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
        }
        finally {
            mLc = null;
            instance = null;
        }
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {
        PtLog.e("authInfoRequested" + ", " + s + ", " + s1 + ", " + s2 );
    }

    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {
        PtLog.e("callStatsUpdated = " + linphoneCall.getState().toString());
    }

    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {
        PtLog.e("newSubscriptionRequest");
    }

    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {
        PtLog.e("notifyPresenceReceived");
    }

    @Override
    public void textReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneAddress linphoneAddress, String s) {
        PtLog.e("textReceived");
    }

    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {
        PtLog.e("dtmfReceived");
    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {
        PtLog.e("notifyReceived");
    }

    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {
        PtLog.e("transferState");
    }

    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {
        PtLog.e("infoReceived");
    }

    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {
        PtLog.e("subscriptionStateChanged");
    }

    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {
        PtLog.e("publishStateChanged");
    }

    @Override
    public void show(LinphoneCore linphoneCore) {
        PtLog.e("show");
    }

    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {
        PtLog.e("displayStatus = " + s);
    }

    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {
        PtLog.e("displayMessage");
    }

    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {
        PtLog.e("displayWarning");
    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {
        PtLog.e("fileTransferProgressIndication");
    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {
        PtLog.e("fileTransferRecv");
    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        PtLog.e("fileTransferSend");
        return 0;
    }

    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {
        PtLog.e("callEncryptionChanged");
    }



    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {
        PtLog.e("isComposingReceived");
    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
        PtLog.e("globalState = " + globalState.toString() + ", " + s);
    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {
        PtLog.e("uploadProgressIndication");
    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {
        PtLog.e("uploadStateChanged");
    }

    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {
        PtLog.e("messageReceived");
    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {
        PtLog.e("notifyReceived");
    }

    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {
        PtLog.e("configuringStatus");
    }

    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
        mLinphoneListener.onRegState(registrationState, s);
    }

    @Override
    public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        mLinphoneListener.onCallState(linphoneCall, state, s);
    }
}
