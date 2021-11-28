package ntu.mil.grpckebbi.Voice;

import android.content.Context;
import android.media.MediaActionSound;
import android.util.Log;


import ntu.mil.grpckebbi.GrpcClientActivity;

import static ntu.mil.grpckebbi.GrpcClientActivity.getInstance;
import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;

import static ntu.mil.grpckebbi.RobotActivity.nuwa_listenResult;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;

public class SpeakUtil {
    private static final String TAG = SpeakUtil.class.getSimpleName();
    private static  Context mContext;

    // Voice Recorder Params
    private static String voiceRecordFilename;
    private static boolean isSentenceCompleted = false;
    private static final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {
        @Override
        public void onVoiceStart(String recordFilename) {
            Log.d(TAG, "onVoiceStart()");
            isSentenceCompleted = false;
            voiceRecordFilename = recordFilename;
        }

        @Override
        public void onVoice(byte[] data, int size, boolean sentenceCompleted) {
            Log.d(TAG, "onVoice()");
            isSentenceCompleted = sentenceCompleted;
        }

        @Override
        public void onVoiceEnd() {
            Log.d(TAG, "onVoiceEnd()");
            stopVoiceRecorder();
        }
    };

    private static VoiceRecorder voiceRecorder;
    private static MediaActionSound mediaActionSound;

    private static void startVoiceRecorder(){
        Log.d(TAG, "startVoiceRecorder()");
        if(voiceRecorder != null){
            mediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
            voiceRecorder.start();
        }
    }

    private static void stopVoiceRecorder(){
        Log.d(TAG, "stopVoiceRecorder()");
        if(voiceRecorder != null){
            mediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
            voiceRecorder.stop();
        }
    }

    public SpeakUtil() {
        this.mContext = getInstance();
        this.voiceRecorder = new VoiceRecorder(mContext, voiceCallback);
        this.mediaActionSound = new MediaActionSound();
    }


    public static void say(String speech){
        mRobotAPI.startTTS(speech);
        GrpcClientActivity.getInstance().sendReply(COMMAND_SUCCESS, null);
    }

    public static void sayThenListen(String speech) {
        nuwa_listenResult = "";
        new Thread(){
            @Override
            public void run(){
                try {
                    startVoiceRecorder();
                    if (! speech.equals("")) {
                        mRobotAPI.startTTS(speech);
                    }
                    mRobotAPI.startSpeech2Text(false);
                    Thread.sleep(10000);
                    mRobotAPI.stopListen();
                    stopVoiceRecorder();
                    GrpcClientActivity.getInstance().sendReply(COMMAND_SUCCESS, nuwa_listenResult + '#' + voiceRecordFilename);

                } catch(Exception e) {
                    Log.d(TAG, "sendReply()" + e.toString());
                    mRobotAPI.hideFace();
                    GrpcClientActivity.getInstance().sendReply(COMMAND_FAILED, e.toString());
                }
            }
        }.start();
    }
}
