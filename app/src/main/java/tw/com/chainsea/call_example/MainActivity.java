package tw.com.chainsea.call_example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import me.pengtao.ptlog.*;
import tw.com.chainsea.call.CallingFragment;

public class MainActivity extends AppCompatActivity {
    private final static String mUserId = "0000021231232002";
    private final static String mUserPwd = "00000000";
    private final static String mHost = "211.150.71.180:5066";
    private final static String mAgentId = "2001";
    private final static String mAgentName = "程曦客服";
    /*private final static String mUserId = "0000021231239001";
    private final static String mUserPwd = "00000000";
    private final static String mHost = "211.150.71.180:5066";
    private final static String mAgentId = "12345678";*/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment,
                        CallingFragment.newInstance(mUserId, mUserPwd, mAgentId, mHost, mAgentName))
                .commit();
        PtLog.init(BuildConfig.DEBUG, "Call-example", this);
    }
}
