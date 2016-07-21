package tw.com.chainsea.call_example;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import tw.com.chainsea.call_example.base.Constant;

public class CallActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        String id = getIntent().getStringExtra(Constant.INTENT_ID);
        String name = getIntent().getStringExtra(Constant.INTENT_CALL_NAME);
        String pw = getIntent().getStringExtra(Constant.INTENT_PASSWORD);
        String callId = getIntent().getStringExtra(Constant.INTENT_CALL_TO);

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment,
                        CallingFragment.newInstance(id, pw, callId, name))
                .commit();
    }
}
