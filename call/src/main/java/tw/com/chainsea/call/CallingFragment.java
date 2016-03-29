package tw.com.chainsea.call;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import org.linphone.core.LinphoneAddress;
import org.linphone.core.LinphoneAuthInfo;
import org.linphone.core.LinphoneCall;
import org.linphone.core.LinphoneCallParams;
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
import org.linphone.core.Reason;
import org.linphone.core.SubscriptionState;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import tw.com.chainsea.call.base.CallLog;
import tw.com.chainsea.call.base.UIThreadDispatcher;
import tw.com.chainsea.call.base.Utility;
import tw.com.chainsea.call.entity.DigitModel;
import tw.com.chainsea.call.ui.DialerGridAdapter;
import tw.com.chainsea.call.widget.SquareImage;

import static android.content.Intent.ACTION_MAIN;

/**
 * A placeholder fragment containing a simple view.
 */
public class CallingFragment extends Fragment implements LinphoneCoreListener.LinphoneListener, View.OnClickListener {
    View mView = null;

    LinphoneCore mLc = null;
    SquareImage muteImage = null;
    SquareImage speakerImage = null;
    OnCallingListener mListener = null;

    public static CallingFragment newInstance() {
        CallingFragment fragment = new CallingFragment();
        return fragment;
    }

    public void setCallingListener( OnCallingListener listener ) {
        mListener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_calling_two, container, false);
        mView.findViewById(R.id.calling_hang_up).setOnClickListener(this);
        speakerImage = (SquareImage)mView.findViewById(R.id.calling_speaker);
        speakerImage.setOnClickListener(this);
        muteImage = (SquareImage)mView.findViewById(R.id.calling_mute);
        muteImage.setOnClickListener(this);
        displayDialer();
        return mView;
    }

    public void displayDialer() {
        final List<DigitModel> digitList = new ArrayList<>();
        digitList.add(new DigitModel("1", null));digitList.add(new DigitModel("2", "ABC"));digitList.add(new DigitModel("3", "DEF"));
        digitList.add(new DigitModel("4", "GHI"));digitList.add(new DigitModel("5", "JKL"));digitList.add(new DigitModel("6", "MNO"));
        digitList.add(new DigitModel("7", "PQRS"));digitList.add(new DigitModel("8", "TUV"));digitList.add(new DigitModel("9", "WXYZ"));
        digitList.add(new DigitModel("*", null));digitList.add(new DigitModel("0", "+"));digitList.add(new DigitModel("#", null));
        DialerGridAdapter dialerGridAdapter = new DialerGridAdapter(getActivity(), digitList);
        GridView dialerGrid = (GridView)mView.findViewById(R.id.calling_dialer);
        dialerGrid.setAdapter(dialerGridAdapter);
        dialerGrid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                TextView textView = (TextView) mView.findViewById(R.id.calling_input);
                textView.append(digitList.get(i).getNum());
                mLc.sendDtmf(digitList.get(i).getNum().charAt(0));
            }
        });
    }

    Handler mHandler = new Handler();
    private ServiceWaitThread mThread;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (LinphoneService.isReady()) {
            onServiceReady();
        } else {
            // start linphone as background
            getActivity().startService(new Intent(ACTION_MAIN).setClass(getActivity(), LinphoneService.class));
            mThread = new ServiceWaitThread();
            mThread.start();
        }
    }

    private void initLinphone() {
        mLc = LinphoneManager.getLc();
        mLc.addListener(this);
        try {
            registerUserAuth(Utility.getUsername(getActivity()), Utility.getPassword(getActivity()), Utility.getHost(getActivity()));
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    protected void onServiceReady() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                initLinphone();
            }
        }, 1000);
    }

    private class ServiceWaitThread extends Thread {
        public void run() {
            while (!LinphoneService.isReady()) {
                try {
                    sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException("waiting thread sleep() has been interrupted");
                }
            }

            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    onServiceReady();
                }
            });
            mThread = null;
        }
    }

    @Override
    public void callState(LinphoneCore linphoneCore, final LinphoneCall linphoneCall, LinphoneCall.State state, String s) {
        CallLog.e( "callState = " + state.toString() );

        if ( state == LinphoneCall.State.IncomingReceived ) {
            mView.findViewById(R.id.receive_call).setVisibility(View.VISIBLE);
            mView.findViewById(R.id.accept).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        mLc.acceptCall(linphoneCall);
                    } catch (LinphoneCoreException e) {
                        e.printStackTrace();
                    }
                    mView.findViewById(R.id.receive_call).setVisibility(View.GONE);
                }
            });
            mView.findViewById(R.id.decline).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mLc.declineCall(linphoneCall, Reason.None);
                    mView.findViewById(R.id.receive_call).setVisibility(View.GONE);
                }
            });
        }
        if ( state == LinphoneCall.State.Connected ) {
            startTimeCount();
        }
        if ( state == LinphoneCall.State.CallEnd ) {
            if ( mListener != null ) mListener.onCallEnd();
        }
    }

    public void registerUserAuth(String name, String password, String host) throws LinphoneCoreException {
        CallLog.e("registerUserAuth name = " + name + ", registerUserAuth pw = " + password + ", registerUserAuth host = " + host);

        String identity = "sip:" + name + "@" + host;
        String proxy = "sip:" + host;

        LinphoneAddress proxyAddr = LinphoneCoreFactory.instance().createLinphoneAddress(proxy);
        LinphoneAddress identityAddr = LinphoneCoreFactory.instance().createLinphoneAddress(identity);

        LinphoneAuthInfo authInfo = LinphoneCoreFactory.instance().createAuthInfo(name, null, password, null, null, host);

        LinphoneProxyConfig prxCfg = mLc.createProxyConfig(identityAddr.asString(), proxyAddr.asStringUriOnly(), proxyAddr.asStringUriOnly(), true);

        prxCfg.enableAvpf(false);
        prxCfg.setAvpfRRInterval(0);
        prxCfg.enableQualityReporting(false);
        prxCfg.setQualityReportingCollector(null);
        prxCfg.setQualityReportingInterval(0);

        prxCfg.enableRegister(true);

        mLc.addProxyConfig(prxCfg);
        mLc.addAuthInfo(authInfo);

        mLc.setDefaultProxyConfig(prxCfg);
    }

    public void setCallingTo(String callto, String host) {
        LinphoneAddress lAddress;
        try {
            lAddress = mLc.interpretUrl(callto + "@" + host);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
            return;
        }
        lAddress.setDisplayName("Chris");

        LinphoneCallParams params = mLc.createDefaultCallParameters();
        params.setVideoEnabled(false);
        try {
            mLc.inviteAddressWithParams(lAddress, params);
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if ( id == R.id.calling_mute ) {
            toggleMicro();
        } else if ( id == R.id.calling_speaker ) {
            toggleSpeaker();
        } else if ( id == R.id.calling_hang_up ) {
            hangUp();
        }
    }

    public void hangUp() {
        LinphoneCall currentCall = mLc.getCurrentCall();

        if (currentCall != null) {
            mLc.terminateCall(currentCall);
        } else if (mLc.isInConference()) {
            mLc.terminateConference();
        } else {
            mLc.terminateAllCalls();
        }
    }

    private boolean isMicMuted = false;
    private void toggleMicro() {
        isMicMuted = !isMicMuted;
        mLc.muteMic(isMicMuted);
        if (isMicMuted) {
            muteImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.mute_p));
        } else {
            muteImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.mute_n));
        }
    }

    private boolean isSpeakerEnabled = false;
    private void toggleSpeaker() {
        isSpeakerEnabled = !isSpeakerEnabled;
        mLc.enableSpeaker(isSpeakerEnabled);
        if (isSpeakerEnabled) {
            speakerImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.speaker_p));
        } else {
            speakerImage.setImageDrawable(ContextCompat.getDrawable(getActivity(), R.drawable.speaker_n));
        }
    }

    @Override
    public void onDestroy() {
        LinphoneManager.destroy();
    }


    @Override
    public void registrationState(LinphoneCore linphoneCore, LinphoneProxyConfig linphoneProxyConfig, LinphoneCore.RegistrationState registrationState, String s) {
        CallLog.d("registrationState = " + registrationState.toString() + ", s = " + s);
        if ( registrationState == LinphoneCore.RegistrationState.RegistrationOk ) {
            setCallingTo(Utility.getAgent(getActivity()),  Utility.getHost(getActivity()));
        } else if ( registrationState == LinphoneCore.RegistrationState.RegistrationFailed ) {
            CallLog.e("register failed close the window");
            if ( mListener != null ) mListener.onRegistrationFailed();
        }
    }

    int minutes = 0;
    int seconds = 0;
    boolean continueTimeCount = true;
    public void startTimeCount() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    ++seconds;
                    if (seconds > 60) {
                        ++minutes;
                        seconds = 0;
                        if (minutes > 60) {
                            minutes = 0;
                        }
                    }
                    UIThreadDispatcher.dispatch(new Runnable() {
                        @Override
                        public void run() {
                            TextView timeText = (TextView) mView.findViewById(R.id.calling_time);
                            String minDis = "" + minutes;
                            if (minutes < 10) {
                                minDis = "0" + minDis;
                            }
                            String secDis = "" + seconds;
                            if (seconds < 10) {
                                secDis = "0" + secDis;
                            }
                            timeText.setText(minDis + ":" + secDis);
                        }
                    });
                    if (!continueTimeCount) {
                        break;
                    }
                }
            }
        }).start();
    }

    @Override
    public void globalState(LinphoneCore linphoneCore, LinphoneCore.GlobalState globalState, String s) {
        CallLog.e( "globalState = " + globalState.toString() );
    }

    @Override
    public void authInfoRequested(LinphoneCore linphoneCore, String s, String s1, String s2) {

    }

    @Override
    public void callStatsUpdated(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCallStats linphoneCallStats) {

    }

    @Override
    public void newSubscriptionRequest(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend, String s) {

    }

    @Override
    public void notifyPresenceReceived(LinphoneCore linphoneCore, LinphoneFriend linphoneFriend) {

    }

    @Override
    public void textReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneAddress linphoneAddress, String s) {

    }

    @Override
    public void dtmfReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, int i) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneAddress linphoneAddress, byte[] bytes) {

    }

    @Override
    public void transferState(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneCall.State state) {

    }

    @Override
    public void infoReceived(LinphoneCore linphoneCore, LinphoneCall linphoneCall, LinphoneInfoMessage linphoneInfoMessage) {

    }

    @Override
    public void subscriptionStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, SubscriptionState subscriptionState) {

    }

    @Override
    public void publishStateChanged(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, PublishState publishState) {

    }

    @Override
    public void show(LinphoneCore linphoneCore) {

    }

    @Override
    public void displayStatus(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayMessage(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void displayWarning(LinphoneCore linphoneCore, String s) {

    }

    @Override
    public void fileTransferProgressIndication(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, int i) {

    }

    @Override
    public void fileTransferRecv(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, byte[] bytes, int i) {

    }

    @Override
    public int fileTransferSend(LinphoneCore linphoneCore, LinphoneChatMessage linphoneChatMessage, LinphoneContent linphoneContent, ByteBuffer byteBuffer, int i) {
        return 0;
    }

    @Override
    public void callEncryptionChanged(LinphoneCore linphoneCore, LinphoneCall linphoneCall, boolean b, String s) {

    }

    @Override
    public void isComposingReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom) {

    }

    @Override
    public void uploadProgressIndication(LinphoneCore linphoneCore, int i, int i1) {

    }

    @Override
    public void uploadStateChanged(LinphoneCore linphoneCore, LinphoneCore.LogCollectionUploadState logCollectionUploadState, String s) {

    }

    @Override
    public void messageReceived(LinphoneCore linphoneCore, LinphoneChatRoom linphoneChatRoom, LinphoneChatMessage linphoneChatMessage) {

    }

    @Override
    public void notifyReceived(LinphoneCore linphoneCore, LinphoneEvent linphoneEvent, String s, LinphoneContent linphoneContent) {

    }

    @Override
    public void configuringStatus(LinphoneCore linphoneCore, LinphoneCore.RemoteProvisioningState remoteProvisioningState, String s) {

    }
}
