package ntu.mil.grpckebbi.Voice;

import android.util.Log;


import ntu.mil.grpckebbi.GrpcClientActivity;


import static ntu.mil.grpckebbi.GrpcClientActivity.getInstance;
import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;
import static ntu.mil.grpckebbi.GrpcClientActivity.TAG;

import static ntu.mil.grpckebbi.RobotActivity.nuwa_listenResult;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;



public class Speak {
    public static void say(String speech){
        mRobotAPI.startTTS(speech);
        ((GrpcClientActivity) getInstance()).sendReply(COMMAND_SUCCESS, null);
    }

    public static void sayThenListen(String speech) {
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
                    ((GrpcClientActivity) getInstance()).sendReply(COMMAND_SUCCESS, nuwa_listenResult);


                } catch(Exception e) {
                    Log.d(TAG, "sendReply()" + e.toString());
                    mRobotAPI.hideFace();
                    ((GrpcClientActivity) getInstance()).sendReply(COMMAND_FAILED, e.toString());
                }
            }
        }.start();
    }
}
