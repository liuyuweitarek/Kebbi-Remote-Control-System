package ntu.mil.grpckebbi;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.MenuItem;

import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.VoiceEventListener;


import ntu.mil.grpckebbi.Utils.PrefsHelper;

import static ntu.mil.grpckebbi.Utils.Constants.LOCALE;

public class MainActivity extends RobotActivity{
//public class MainActivity extends AppCompatActivity{
    public final static String TAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_PERMISSIONS = 786;
    private static final int REQUEST_OVERLAY = 787;
    private static final int ACTIVITY = 123;

    public static NuwaRobotAPI mRobotAPI;
    private static IClientId mClientId;
    private static Context mContext;

    public static String locale;


    public MainActivity(){ super(null, null);}

    public MainActivity(RobotEventListener robotEventListener, VoiceEventListener voiceEventListener) {super(robotEventListener, voiceEventListener);}

    public static Context getContext(){
        return mContext;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        requestPermissions();
        mContext = this.getApplicationContext();
        locale = getResources().getConfiguration().locale.getLanguage();
        Log.d(TAG, "Here is:" + locale);
        if(getIntent().getStringExtra(LOCALE) != null)
            locale = getIntent().getStringExtra(LOCALE);

        PrefsHelper.put(this, "sttSource", "nuwa");
        PrefsHelper.put(this, "locale", locale);
    }

    @Override
    protected void onResume() {
        Log.i(TAG, "onResume()");
        super.onResume();
//        initRobotApi(); DEMO_COMMENT
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mRobotAPI.release(); DEMO_COMMENT
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void initRobotApi(){
        //Step 1 : Initial Nuwa API Object
        mClientId = new IClientId(this.getPackageName());
        mRobotAPI = new NuwaRobotAPI(this,mClientId);

        //Step 2 : Register receive Robot Event
        Log.d(TAG,"register EventListener ") ;
        mRobotAPI.registerRobotEventListener(robotEventListener);//listen callback of robot service event
        mRobotAPI.registerVoiceEventListener(voiceEventListener);//listen callback of robot service event

    }

    private void requestPermissions() {
        Log.i(TAG, "requestPermissions()");
        String[] permissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WAKE_LOCK};
        requestPermissions(permissions, REQUEST_PERMISSIONS);
    }

    private boolean verifyPermissions() {
        int writePermission = checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = checkCallingOrSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = checkCallingOrSelfPermission(Manifest.permission.CAMERA);
        int recordPermission = checkCallingOrSelfPermission(Manifest.permission.RECORD_AUDIO);
        int wakeupPermission = checkCallingOrSelfPermission(Manifest.permission.WAKE_LOCK);
        return (writePermission != PackageManager.PERMISSION_GRANTED) ||
                (readPermission != PackageManager.PERMISSION_GRANTED) ||
                (cameraPermission != PackageManager.PERMISSION_GRANTED) ||
                (recordPermission != PackageManager.PERMISSION_GRANTED) ||
                (wakeupPermission != PackageManager.PERMISSION_GRANTED);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (verifyPermissions()) {
            Log.e(TAG, "some permissions not granted");
            requestPermissions();
            return;
        }

        if (!Settings.canDrawOverlays(MainActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_OVERLAY);
            return;
        }

        startActivityForResult(new Intent(this, GrpcClientActivity.class), ACTIVITY);


    }

}
