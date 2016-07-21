package tw.com.chainsea.call_example;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import cn.hadcn.davinci.log.VinciLog;
import tw.com.chainsea.call.SipCallManager;
import tw.com.chainsea.call_example.entity.DigitModel;
import tw.com.chainsea.call_example.ui.DialerGridAdapter;
import tw.com.chainsea.call_example.widget.SquareImage;

/**
 * A placeholder fragment containing a simple view.
 */
public class CallingFragment extends Fragment implements View.OnClickListener, SipCallManager.Listener {
    View mView = null;

    SquareImage muteImage = null;
    SquareImage speakerImage = null;
    TextView tvTimer = null;
    TextView tvTitle = null;
    SipCallManager callManager;

    private final static String USER_ID = "user_id";
    private final static String PWD = "pwd";
    private final static String AGENT_ID = "agent_id";
    private final static String AGENT_NAME = "agent_name";

    public static CallingFragment newInstance(String userId, String pwd, String agentId, String agentName) {
        CallingFragment fragment = new CallingFragment();
        Bundle bundle = new Bundle();
        bundle.putString(USER_ID, userId);
        bundle.putString(PWD, pwd);
        bundle.putString(AGENT_ID, agentId);
        bundle.putString(AGENT_NAME, agentName);
        fragment.setArguments(bundle);

        return fragment;
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
                callManager.sendDtmf(digitList.get(i).getNum().charAt(0));
            }
        });
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        String id = getArguments().getString(USER_ID);
        String pw = getArguments().getString(PWD);
        String callTo = getArguments().getString(AGENT_ID);

        VinciLog.e("id = " + id + ", pw = " + pw + ", callTo = " + callTo);
        callManager = new SipCallManager(getContext(), id, pw);
        callManager.startCall(callTo, this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if ( id == R.id.calling_mute ) {
            callManager.setMute(true);
        } else if ( id == R.id.calling_speaker ) {
            callManager.setSpeaker(true);
        } else if ( id == R.id.calling_hang_up ) {
            callManager.hangup();
        }
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        callManager.release();
    }

    @Override
    public void onCallEnd() {
        getActivity().finish();
    }

    @Override
    public void onCalling() {
        VinciLog.e("start calling");
    }
}
