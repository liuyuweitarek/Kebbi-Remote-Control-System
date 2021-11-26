package ntu.mil.grpckebbi;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.RobotEventListener;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.agent.VoiceResultJsonParser;

import static android.service.controls.ControlsProviderService.TAG;
import static ntu.mil.grpckebbi.MainActivity.getContext;


public class RobotActivity extends AppCompatActivity {
    public NuwaRobotAPI robotAPI;
    public IClientId iClientId;
    public VoiceEventListener voiceEventListener;
    public RobotEventListener robotEventListener;

    public static String nuwa_listenResult;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.nuwa_listenResult = "";
        this.iClientId = new IClientId(this.getPackageName());
        this.robotAPI = new NuwaRobotAPI(this,iClientId);
        this.voiceEventListener = new VoiceEventListener() {
            @Override
            public void onWakeup(boolean b, String s, float v) {

            }

            @Override
            public void onTTSComplete(boolean b) {

            }

            @Override
            public void onSpeechRecognizeComplete(boolean b, ResultType resultType, String s) {

            }

            @Override
            public void onSpeech2TextComplete(boolean b, String s) {
                nuwa_listenResult = VoiceResultJsonParser.parseVoiceResult(s); ;
            }

            @Override
            public void onMixUnderstandComplete(boolean b, ResultType resultType, String s) {

            }

            @Override
            public void onSpeechState(ListenType listenType, SpeechState speechState) {
            }

            @Override
            public void onSpeakState(SpeakType speakType, SpeakState speakState) {

            }

            @Override
            public void onGrammarState(boolean b, String s) {

            }

            @Override
            public void onListenVolumeChanged(ListenType listenType, int i) {

            }

            @Override
            public void onHotwordChange(HotwordState hotwordState, HotwordType hotwordType, String s) {

            }
        };
        this.robotEventListener = new RobotEventListener() {
            @Override
            public void onWikiServiceStart() {
                // Nuwa Robot SDK is ready now, you call call Nuwa SDK API now.
                Log.d(TAG,"onWikiServiceStart, robot ready to be control ") ;

            }

            @Override
            public void onWikiServiceStop() {

            }

            @Override
            public void onWikiServiceCrash() {

            }

            @Override
            public void onWikiServiceRecovery() {

            }

            @Override
            public void onStartOfMotionPlay(String s) {

            }

            @Override
            public void onPauseOfMotionPlay(String s) {

            }

            @Override
            public void onStopOfMotionPlay(String s) {

            }

            @Override
            public void onCompleteOfMotionPlay(String s) {
                Log.d(TAG,"Play Motion Complete " + s);
            }

            @Override
            public void onPlayBackOfMotionPlay(String s) {

            }

            @Override
            public void onErrorOfMotionPlay(int i) {

            }

            @Override
            public void onPrepareMotion(boolean b, String s, float v) {

            }

            @Override
            public void onCameraOfMotionPlay(String s) {

            }

            @Override
            public void onGetCameraPose(float v, float v1, float v2, float v3, float v4, float v5, float v6, float v7, float v8, float v9, float v10, float v11) {

            }

            @Override
            public void onTouchEvent(int i, int i1) {

            }

            @Override
            public void onPIREvent(int i) {

            }

            @Override
            public void onTap(int i) {

            }

            @Override
            public void onLongPress(int i) {

            }

            @Override
            public void onWindowSurfaceReady() {

            }

            @Override
            public void onWindowSurfaceDestroy() {

            }

            @Override
            public void onTouchEyes(int i, int i1) {

            }

            @Override
            public void onRawTouch(int i, int i1, int i2) {

            }

            @Override
            public void onFaceSpeaker(float v) {

            }

            @Override
            public void onActionEvent(int i, int i1) {

            }

            @Override
            public void onDropSensorEvent(int i) {

            }

            @Override
            public void onMotorErrorEvent(int i, int i1) {

            }
        };

        //Step 2 : Register receive Robot Event
        Log.d(TAG,"register EventListener ") ;
        this.robotAPI.registerRobotEventListener(this.robotEventListener);//listen callback of robot service event
        this.robotAPI.registerVoiceEventListener(this.voiceEventListener);//listen callback of robot service event
    }

    public RobotActivity(RobotEventListener robotEventListener, VoiceEventListener voiceEventListener) {
        this.robotEventListener = robotEventListener;
        this.voiceEventListener = voiceEventListener;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if(robotEventListener!= null)
            robotAPI.registerRobotEventListener(this.robotEventListener);
        if(voiceEventListener!= null)
            robotAPI.registerVoiceEventListener(this.voiceEventListener);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        robotAPI.release();
    }
}
