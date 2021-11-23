package ntu.mil.grpckebbi;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.gson.Gson;
import com.interaction.robot.interaction.InteractGrpc;
import com.interaction.robot.interaction.RobotInput;

import ntu.mil.grpckebbi.Network.Connection;
import ntu.mil.grpckebbi.Utils.RobotCommand;
import ntu.mil.grpckebbi.Utils.WindowUtils;

import ntu.mil.grpckebbi.Network.Connection;
import ntu.mil.grpckebbi.Utils.WindowUtils;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;

import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;

import static ntu.mil.grpckebbi.Network.Connection.mInteracter;

public class GrpcClientActivity extends Activity{
    static final String TAG = GrpcClientActivity.class.getSimpleName();

    public static Connection mConnection;
    public TextView localIp;

    private static final int CODE_REMOTE_AI = 14;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grpc_client);
        mConnection = new Connection(this);
        mInteracter = mConnection.start();
    }



    @Override
    protected void onResume() {
        super.onResume();
        WindowUtils.updateUI(GrpcClientActivity.this);
        if(localIp != null)
            localIp.setText(mConnection.GetLocalIpAddress());
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        mRobotAPI.hideFace();
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




    // Grpc Communicate Protocal Method
    public static void sendReply(String intent, String value){
        Log.d(TAG, "sendReply()" + intent + value);
        new Thread(){
            @Override
            public void run(){
                try {
                    if (mInteracter != null) {
                        //This is the message returning to server
                        RobotInput message = RobotInput.newBuilder().setUtterance(new Gson().toJson(new RobotCommand(intent, value))).build();
                        //This is the new incoming command
                        RobotCommand command = new Gson().fromJson(mInteracter.robotSend(message).getUtterance(), RobotCommand.class);
                        Log.d(TAG, command.toString());
                        executeAction(command);

                    }
                } catch(Exception e) {
                    Log.d(TAG, "sendReply()" + e.toString());
                    mRobotAPI.hideFace();
                }
            }
        }.start();
    }

    private static void executeAction(RobotCommand robotCommand) {
        Log.d(TAG, "executeAction()" + robotCommand.toString());
        switch (robotCommand.getIntent()) {
            case "say":
                mRobotAPI.startTTS(robotCommand.getValue());
                sendReply(COMMAND_SUCCESS, null);
                break;
            default:
                sendReply(COMMAND_FAILED, "command not found");
                break;
        }
    }

}
