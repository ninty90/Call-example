package tw.com.chainsea.call;

import android.os.Bundle;
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
import org.linphone.core.LinphoneCore;
import org.linphone.core.LinphoneCoreException;
import org.linphone.core.LinphoneCoreFactory;
import org.linphone.core.LinphoneProxyConfig;
import org.linphone.core.Reason;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.pengtao.ptlog.PtLog;
import tw.com.chainsea.call.base.LinphoneManager;
import tw.com.chainsea.call.base.OnLinphoneListener;
import tw.com.chainsea.call.base.UIThreadDispatcher;
import tw.com.chainsea.call.entity.DigitModel;
import tw.com.chainsea.call.ui.DialerGridAdapter;
import tw.com.chainsea.call.widget.SquareImage;

/**
 * A placeholder fragment containing a simple view.
 */
public class CallingFragment extends Fragment implements OnLinphoneListener, View.OnClickListener {
    View mView = null;

    LinphoneCore mLc = null;
    SquareImage muteImage = null;
    SquareImage speakerImage = null;
    OnCallingListener mListener = null;
    TextView tvTimer = null;
    TextView tvTitle = null;

    private final static String USER_ID = "user_id";
    private final static String PWD = "pwd";
    private final static String AGENT_ID = "agent_id";
    private final static String HOST = "host";
    private final static String AGENT_NAME = "agent_name";

    public static CallingFragment newInstance(String userId, String pwd, String agentId, String host, String agentName) {
        CallingFragment fragment = new CallingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(USER_ID, userId);
        bundle.putString(PWD, pwd);
        bundle.putString(AGENT_ID, agentId);
        bundle.putString(HOST, host);
        bundle.putString(AGENT_NAME, agentName);
        fragment.setArguments(bundle);

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

        tvTitle = (TextView)mView.findViewById(R.id.calling_title);
        tvTitle.setText(getArguments().getString(AGENT_NAME));
        speakerImage = (SquareImage)mView.findViewById(R.id.calling_speaker);
        speakerImage.setOnClickListener(this);
        muteImage = (SquareImage)mView.findViewById(R.id.calling_mute);
        muteImage.setOnClickListener(this);
        tvTimer = (TextView) mView.findViewById(R.id.calling_time);
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

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        initLinphone();
    }

    private void initLinphone() {
        LinphoneManager.createAndStart(getActivity(), this);
        mLc = LinphoneManager.getLc();
        Bundle bundle = getArguments();
        try {
            registerUserAuth(bundle.getString(USER_ID), bundle.getString(PWD), bundle.getString(HOST));
        } catch (LinphoneCoreException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRegState(LinphoneCore.RegistrationState registrationState, String reason) {
        PtLog.d("registrationState = " + registrationState.toString() + ", reason = " + reason);
        if ( registrationState == LinphoneCore.RegistrationState.RegistrationOk ) {
            setCallingTo(getArguments().getString(AGENT_ID),  getArguments().getString(HOST));
        } else if ( registrationState == LinphoneCore.RegistrationState.RegistrationFailed ) {
            PtLog.e("register failed");
            if ( mListener != null ) mListener.onRegistrationFailed();
        }
    }

    @Override
    public void onCallState(final LinphoneCall linphoneCall, LinphoneCall.State state, String reason) {
        PtLog.d("callState = " + state.toString());

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
        if ( state == LinphoneCall.State.CallEnd ) {
            stopCallingTimer();
            if ( mListener != null ) mListener.onCallEnd();
        }

        if ( state == LinphoneCall.State.Connected ) {
            startCallingTimer();
        }

    }

    public void registerUserAuth(String name, String password, String host) throws LinphoneCoreException {
        PtLog.d("name = " + name + ", pw = " + password + ", host = " + host);

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

        mLc.clearAuthInfos();
        mLc.clearProxyConfigs();

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
        super.onDestroy();
        PtLog.e(" CallingFragment Destroyed ");
        LinphoneManager.destroy();
    }

    class CallingTimer extends TimerTask {

        @Override
        public void run() {
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
                    tvTimer.setText(getString(R.string.timer, minutes, seconds));
                }
            });
        }
    }

    Timer callingTimer;
    int minutes = 0;
    int seconds = 0;
    public void startCallingTimer() {
        callingTimer = new Timer();
        callingTimer.scheduleAtFixedRate(new CallingTimer(), 1000, 1000);
    }

    private void stopCallingTimer() {
        callingTimer.cancel();
        callingTimer.purge();
    }
}
