package ntu.mil.grpckebbi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.interaction.robot.interaction.InteractGrpc;
import com.interaction.robot.interaction.RobotConnectReply;
import com.interaction.robot.interaction.RobotConnectRequest;
import com.interaction.robot.interaction.RobotInput;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ntu.mil.grpckebbi.Utils.RobotCommand;
import ntu.mil.grpckebbi.Utils.WindowUtils;

import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;

import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;
import static ntu.mil.grpckebbi.Vision.Constants.MODE_RECORD;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_STARTED;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_CONNECTED;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_CANCELED;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_OFF;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_PORT;

import ntu.mil.grpckebbi.Vision.DetectService;
import ntu.mil.grpckebbi.Vision.VideoRecordListener;
import ntu.mil.grpckebbi.Vision.VideoStreamService;
import ntu.mil.grpckebbi.Voice.SpeakUtil;

public class GrpcClientActivity extends Activity{
    public static final String TAG = GrpcClientActivity.class.getSimpleName();

    /** Grpc Connection UI Params */
    private EditText ip_1, ip_2, ip_3, ip_4, host_port;
    private String ip;
    private TextView localIp;
    private int port;
    private static GrpcClientActivity mGrpcContext;
    public static InteractGrpc.InteractBlockingStub mInteracter;


    /** Services State Params */
    private static boolean serviceActive = false;
    private static boolean serviceBound;
    private static int cameraMode;
    private static boolean messageConfirmed;

    /** Make other Classes could use sendReply() / excecuteAction() */
    public static GrpcClientActivity getInstance() {
        return mGrpcContext;
    }



    /** VoiceRecorder*/
    public static SpeakUtil mSpeakTool;
    /** Grpc Activity Life Cycle */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_grpc_client);
        mGrpcContext = this;
        mSpeakTool = new SpeakUtil();
        SetupUI();
        LoadData();
        AssignIps();
    }

    @Override
    protected void onResume() {
        super.onResume();
        WindowUtils.updateUI(GrpcClientActivity.this);
        if(localIp != null)
            localIp.setText(GetLocalIpAddress());
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

    /** gRPC Connection Methods */
    private void SetupUI(){
        ip_1 = findViewById(R.id.ip1);
        ip_2 = findViewById(R.id.ip2);
        ip_3 = findViewById(R.id.ip3);
        ip_4 = findViewById(R.id.ip4);
        host_port = findViewById(R.id.port);

        ImageView connectBtn = findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(v -> {
            Connect(ip);
            mRobotAPI.showFace();
        });

        localIp = findViewById(R.id.ipAdress);
        localIp.setText(GetLocalIpAddress());
    }

    private void Connect(String saved_ip) {
        ip = ip_1.getText().toString() + "."
                + ip_2.getText().toString() + "."
                + ip_3.getText().toString() + "."
                + ip_4.getText().toString();

        if(ip_1.getText().toString().equals("") && saved_ip != null)
            ip = saved_ip;

        port = Integer.parseInt(host_port.getText().toString());

        SaveData();

        Log.i(TAG,"Attempting to connect to server with ip " + ip);

        if(!ip.isEmpty()) {
            try {
                ManagedChannel managedChannel = ManagedChannelBuilder.forAddress(ip, port)
                        .usePlaintext(true)
                        //.keepAliveWithoutCalls(true)
                        .build();
                mInteracter = InteractGrpc.newBlockingStub(managedChannel);

                String jsonHandshake = new Gson().toJson(new RobotCommand("Kebbi", localIp.getText().toString()));
                RobotConnectRequest robotConnectRequest = RobotConnectRequest.newBuilder().setStatus(jsonHandshake).build();
                RobotConnectReply robotConnectReply = mInteracter.robotConnect(robotConnectRequest);

                String locale = robotConnectReply.getStatus();
                List<String> locales = new ArrayList<>();
                locales.add("en");
                locales.add("zh");
                if(locales.contains(locale)){
                    sendReply(COMMAND_SUCCESS,"Locale set to " + locale);
                }
                else{
                    sendReply(COMMAND_FAILED,"Locale not supported! Please use 'zh' (Chinese) or 'en' (English)");
                }
            } catch (Exception e) {
                Log.e(TAG, "connect() Fail: " + e);
                sendReply(COMMAND_FAILED,e.toString());
            }
        }
        else{
            Toast.makeText(this,"Select an IP first!", Toast.LENGTH_LONG).show();
        }
    }

    public String GetLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()&& inetAddress instanceof Inet4Address) { return inetAddress.getHostAddress(); }
                }
            }
        } catch (SocketException ex) {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    private void AssignIps() {
        if(ip != null && !ip.isEmpty()){
            Log.i(TAG, ip);
            String[] ipArray = ip.split("\\.");
            ip_1.setText(ipArray[0]);
            ip_2.setText(ipArray[1]);
            ip_3.setText(ipArray[2]);
            ip_4.setText(ipArray[3]);
        }
    }

    private void LoadData() {
        SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
        ip = prefs.getString("ip", getString(R.string.def_ip));
        port = prefs.getInt("port", Integer.parseInt(getString(R.string.def_port)));
        host_port.setText(String.valueOf(port));
    }

    private void SaveData() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ip", ip);
        editor.putInt("port", port);
        editor.apply();
    }

    /** gRPC Communicate Protocal Method */
    public void sendReply(String intent, String value){
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

    public void executeAction(RobotCommand robotCommand) {
        Log.d(TAG, "executeAction()" + robotCommand.toString());
        switch (robotCommand.getIntent()) {
            case "say":
                mSpeakTool.say(robotCommand.getValue());
                break;
            case "listen":
                mSpeakTool.sayThenListen(robotCommand.getValue());
                break;
            case "video_record":
                if(robotCommand.getValue().equals("start")){
                    if(!serviceActive) startRecording(); else sendReply(COMMAND_FAILED, "A camera service is already running, stop that service first");
                } else if(robotCommand.getValue().equals("stop"))
                    if(serviceActive) stopDetectService(); else sendReply(COMMAND_FAILED, "No services are currently running");
                break;
            case "video_stream":
                if (robotCommand.getValue().equals("start")){
                    if(!serviceActive) startVideoStream(); else sendReply(COMMAND_FAILED, "A camera service is already running, stop that service first");
                } else if (robotCommand.getValue().equals("stop")){
                    if(serviceActive) stopVideoStream(); else sendReply(COMMAND_FAILED, "No services are currently runnung.");
                }
                break;
            default:
                sendReply(COMMAND_FAILED, "command not found");
                break;
        }
    }

    /** Detection Service
          * TODO: find_face -> move_to_face -> follow_people
          * DONE: video_record
       */
    protected ServiceConnection detectConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "onServiceConnected()" + name.toString());
            DetectService.LocalBinder localBinder = (DetectService.LocalBinder) service;
            DetectService detectService = localBinder.getService();
            serviceBound = true;
            switch (cameraMode){
                case MODE_RECORD:
                    Log.d(TAG, "here is record");
                    detectService.addRecordListener(videoRecordListener);
                    break;
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            serviceBound = false;
        }
    };

    private void startRecording(){
        Log.d(TAG, "startRecording()");
        cameraMode = MODE_RECORD;
        serviceActive = true;
        messageConfirmed = false;
        Intent intent = new Intent(this, DetectService.class);
        intent.putExtra("cameraMode", MODE_RECORD);
        intent.putExtra("showPreview", false);
        bindService(intent, detectConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
        Log.d(TAG, "successfully start detectConnection");
    }

    private final VideoRecordListener videoRecordListener = recordFile ->  {
        Log.d(TAG, "onStateChange()");
        if(!messageConfirmed){
            messageConfirmed = true;
            sendReply(COMMAND_SUCCESS, recordFile);
        }
    };

    private void stopDetectService(){
        serviceActive = false;
        messageConfirmed = false;
        if(serviceBound)
            mGrpcContext.unbindService(detectConnection);
        serviceBound = false;
        stopService(new Intent(this, DetectService.class));
    }

    /** Video Stream Service */
    protected ServiceConnection streamConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.d(TAG, "onServiceConnected()");
            VideoStreamService.LocalBinder localBinder = (VideoStreamService.LocalBinder) service;
            VideoStreamService videoStreamService = localBinder.getService();
            videoStreamService.registerVideoStreamListener(videoStreamState -> {
                switch (videoStreamState) {
                    case STREAM_STARTED:
                        if(!messageConfirmed) {
                            messageConfirmed = true;
                            sendReply(COMMAND_SUCCESS, String.valueOf(STREAM_PORT));
                        }
                        break;
                    case STREAM_CONNECTED:
                        Log.d(TAG, "a client is listening to the stream!");
                        break;
                    case STREAM_CANCELED:
                        Log.d(TAG, "stream off");
                        stopVideoStream();
                        break;
                    case STREAM_OFF:
                        if(!messageConfirmed) {
                            messageConfirmed = true;
                            sendReply(COMMAND_SUCCESS, null);
                        }
                        break;
                }
            });
            serviceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            serviceBound = false;
        }
    };

    private void startVideoStream() {
        serviceActive = true;
        messageConfirmed = false;
        Intent intent = new Intent(this, VideoStreamService.class);
        bindService(intent, streamConnection, Context.BIND_AUTO_CREATE);
        startService(intent);
    }

    private void stopVideoStream() {
        serviceActive = false;
        messageConfirmed = false;
        if(serviceBound) unbindService(streamConnection);
        stopService(new Intent(this, VideoStreamService.class));
    }

    /** Voice Service */
}
