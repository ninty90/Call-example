package tw.com.chainsea.call;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;

import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreFactoryImpl;
import org.linphone.core.LinphoneCoreListener;
import org.linphone.core.LinphoneProxyConfig;

/**
 *
 * Created by 90Chris on 2015/7/1.
 */
public class LinphoneService extends Service implements LinphoneCoreListener.LinphoneCallStateListener,
        LinphoneCoreListener.LinphoneGlobalStateListener, LinphoneCoreListener.LinphoneRegistrationStateListener {

    final String TAG = "pengtao" + getClass().getSimpleName();
    private PendingIntent mkeepAlivePendingIntent;
    private static LinphoneService instance;

    @Override
    public void onCreate() {
        super.onCreate();
        LinphoneCoreFactoryImpl.instance();

        LinphoneManager.createAndStart(LinphoneService.this);
        instance = this; // instance is ready once linphone manager has been created

        //make sure the application will at least wakes up every 10 mn
        Intent intent = new Intent(this, KeepAliveHandler.class);
        mkeepAlivePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_ONE_SHOT);
        ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP
                , SystemClock.elapsedRealtime()+600000
                , 600000
                , mkeepAlivePendingIntent);
    }

    public static boolean isReady() {
        return instance != null;
    }

    /**
     * @throws RuntimeException service not instantiated
     */
    public static LinphoneService instance()  {
        if (isReady()) return instance;

        throw new RuntimeException("LinphoneService not instantiated yet");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LinphoneManager.destroy();
        ((AlarmManager) this.getSystemService(Context.ALARM_SERVICE)).cancel(mkeepAlivePendingIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void callState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        Log.e(TAG, "callState = " + state.toString());
    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
        Log.e(TAG, "globalState = " + globalState.toString());
    }

    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
        Log.e(TAG, "registrationState = " + registrationState.toString());
    }
}
