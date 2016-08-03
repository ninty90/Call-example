package tw.com.chainsea.call_example;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import cn.hadcn.davinci.log.LogLevel;
import cn.hadcn.davinci.log.VinciLog;
import tw.com.chainsea.call_example.base.Constant;

public class MainActivity extends AppCompatActivity {
    private Button btnCall;
    private EditText etCallNumber;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 201;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        VinciLog.init(LogLevel.DEBUG, "CallExample", this);
        checkAndRequestAudioPermission();

        setContentView(R.layout.activity_main);
        btnCall = (Button)findViewById(R.id.call_button);
        etCallNumber = (EditText)findViewById(R.id.call_to);

        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CallActivity.class);
                intent.putExtra(Constant.INTENT_ID, "1100017");
                intent.putExtra(Constant.INTENT_PASSWORD, "1234");
                intent.putExtra(Constant.INTENT_CALL_TO, etCallNumber.getText().toString());
                intent.putExtra(Constant.INTENT_CALL_NAME, etCallNumber.getText().toString());
                startActivity(intent);
            }
        });
    }

    public void checkAndRequestAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            /*if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            }*/
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        }
    }
}
