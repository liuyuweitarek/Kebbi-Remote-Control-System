package ntu.mil.grpckebbi.Voice;

import android.util.Log;

import ntu.mil.grpckebbi.GrpcClientActivity;

import static ntu.mil.grpckebbi.GrpcClientActivity.sendReply;
import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;
import static ntu.mil.grpckebbi.GrpcClientActivity.TAG;
import static ntu.mil.grpckebbi.RobotActivity.nuwa_listenResult;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;

public interface Speak {
    static void say(String speech){
        mRobotAPI.startTTS(speech);
        sendReply(COMMAND_SUCCESS, null);
    }

    static void sayThenListen(String speech) {
        nuwa_listenResult = "";
        new Thread(){
            @Override
            public void run(){
                try {
                    if (! speech.equals("")) {
                        mRobotAPI.startTTS(speech);
                    }
                    mRobotAPI.startSpeech2Text(false);
                    Thread.sleep(10000);
                    mRobotAPI.stopListen();
                    sendReply(COMMAND_SUCCESS, nuwa_listenResult);
                } catch(Exception e) {
                    Log.d(TAG, "sendReply()" + e.toString());
                    mRobotAPI.hideFace();
                    sendReply(COMMAND_FAILED, e.toString());
                }
            }
        }.start();
    }
}
