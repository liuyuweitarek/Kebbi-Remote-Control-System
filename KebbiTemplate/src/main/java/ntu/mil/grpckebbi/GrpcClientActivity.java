package ntu.mil.grpckebbi;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaActionSound;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
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

import butterknife.BindView;
import butterknife.ButterKnife;

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
import ntu.mil.grpckebbi.Voice.GoogleSpeechService;
import ntu.mil.grpckebbi.Voice.SpeakUtil;
import ntu.mil.grpckebbi.Voice.VoiceRecorder;

public class GrpcClientActivity extends Activity{
    public static final String TAG = GrpcClientActivity.class.getSimpleName();

    /** Grpc Connection UI Params */
    private EditText ip_1, ip_2, ip_3, ip_4, host_port;
    private String ip;
    private TextView localIp;
    private int port;
    private static GrpcClientActivity mGrpcContext;
    public static InteractGrpc.InteractBlockingStub mInteracter;

    /** This params are for Demo: VoiceRecorder+GoogleS2T. */
    public TextView txtDbValue, txtRecordFileName;
    private LineChart mLineChart;
    @BindView(R.id.status)
    TextView status;
    @BindView(R.id.textMessage)
    TextView textMessage;
    @BindView(R.id.listview)
    ListView listView;
    private List<String> stringList;

    /** Services State Params */
    private static boolean serviceActive = false;
    private static boolean serviceBound;
    private static int cameraMode;
    private static boolean messageConfirmed;

    //Google speech api
    private GoogleSpeechService googleSpeechService;
    private String googleTranscript = "";
    private boolean apiReady = true;
    private boolean isSentenceCompleted = false;

    /** Make other Classes could use sendReply() / excecuteAction() */
    public static GrpcClientActivity getInstance() {
        return mGrpcContext;
    }

    /** VoiceRecorder*/
    public static SpeakUtil mSpeakTool;
    private MediaActionSound mMediaActionSound;
    private VoiceRecorder mVoiceRecorder;

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

        if(googleSpeechService == null)
            startGoogleSpeechService();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(googleSpeechService != null ){
            stopGoogleSpeechService();
        }
//        mRobotAPI.hideFace(); DEMO_COMMENT
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
            setContentView(R.layout.voice_s2t); // DEMO_ADD
            SetupVoiceRecorderGoogleS2TUI();
//            mRobotAPI.showFace(); DEMO_COMMENT
        });

        localIp = findViewById(R.id.ipAdress);
        localIp.setText(GetLocalIpAddress());
    }

    /** These  functions are for Demo: VoiceRecorder+GoogleS2T. */
    private void SetupVoiceRecorderGoogleS2TUI(){
        txtDbValue = (TextView) findViewById(R.id.txtDbValue);
        txtRecordFileName = findViewById(R.id.txtRecordFileName);

        mLineChart = findViewById(R.id.liveChart);

        mVoiceRecorder = new VoiceRecorder(GrpcClientActivity.this, voiceCallback);
        mMediaActionSound = new MediaActionSound();

        ButterKnife.bind(this);

        // This line is needed, not only justfor Demo.
        googleSpeechService = new GoogleSpeechService(GrpcClientActivity.this);

        stringList = new ArrayList<>();
        adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, stringList);
        listView.setAdapter(adapter);
        initChart();
    }

    private void initChart(){
        mLineChart.getDescription().setEnabled(false);// Tag
        mLineChart.setTouchEnabled(true);// Touchable
        mLineChart.setDragEnabled(true);// Interactive

        // Set a basic line
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        mLineChart.setData(data);

        // Bottom left tags
        Legend l =  mLineChart.getLegend();
        l.setForm(Legend.LegendForm.LINE);
        l.setTextColor(Color.BLACK);

        // X Axis
        XAxis x =  mLineChart.getXAxis();
        x.setTextColor(Color.BLACK);
        x.setDrawGridLines(true);//畫X軸線
        x.setPosition(XAxis.XAxisPosition.BOTTOM);//把標籤放底部
        x.setLabelCount(5,true);//設置顯示5個標籤

        // Content of X axis
        x.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return "No. "+Math.round(value);
            }
        });

        YAxis y = mLineChart.getAxisLeft();
        y.setTextColor(Color.BLACK);
        y.setDrawGridLines(true);
        y.setAxisMaximum(16000);
        y.setAxisMinimum(0);
        mLineChart.getAxisRight().setEnabled(false);// Right YAxis invisible
        mLineChart.setVisibleXRange(0,100);// Set visible range
    }

    private void addData(int inputData){
        LineData data =  mLineChart.getData();

        // DB data only, set to 0. If other type of data be added, set to other int.
        ILineDataSet set = data.getDataSetByIndex(0);
        if (set == null){
            set = createSet("DB_DATA");
            data.addDataSet(set);
        }
        data.addEntry(new Entry(set.getEntryCount(),inputData),0);

        // Renew plot
        data.notifyDataChanged();
        mLineChart.notifyDataSetChanged();
        mLineChart.setVisibleXRange(0,100); // Visible range
        mLineChart.moveViewToX(data.getEntryCount());// Whether track on the newest data point
    }

    private LineDataSet createSet(String SetName) {
        LineDataSet set = new LineDataSet(null, SetName);
        set.setAxisDependency(YAxis.AxisDependency.LEFT);
        set.setColor(Color.GRAY);
        set.setLineWidth(2);
        set.setDrawCircles(false);
        set.setFillColor(Color.RED);
        set.setFillAlpha(50);
        set.setDrawFilled(true);
        set.setValueTextColor(Color.BLACK);
        set.setDrawValues(false);
        return set;
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
                        .usePlaintext()
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
//                    mRobotAPI.hideFace(); DEMO_COMMENT
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
                startVoiceRecorder();
//                mSpeakTool.sayThenListen(robotCommand.getValue());
                /**
                                     Here is going to change listen source from Kebbi origin listen method into `Voice Recorder` + `Google S2T API ` to implement continue listening method.
                                **/
//                sendReply(COMMAND_SUCCESS,"I got it!");
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
                    if(serviceActive) stopVideoStream(); else sendReply(COMMAND_FAILED, "No services are currently running.");
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
    private final VoiceRecorder.Callback voiceCallback = new VoiceRecorder.Callback() {

        private String byteToDbValue(byte[] buffer, int size){
            for (int i = 0; i < size - 1; i += 2) {
                // The buffer has LINEAR16 in little endian.
                int s = buffer[i + 1];
                if (s < 0) s *= -1;
                s <<= 8;
                s += Math.abs(buffer[i]);
                return Integer.toString(s);
            }
            return "";
        }

        @Override
        public void onVoiceStart(final String recordFilename) {
            Log.d(TAG, "onVoiceStart()");
            startGoogleSpeechService();
            if(googleSpeechService == null) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txtRecordFileName.setText(recordFilename);
                }
            });
            isSentenceCompleted = false;
            googleTranscript = "";
            googleSpeechService.startRecognizing(mVoiceRecorder.getSampleRate());

        }

        @Override
        public void onVoice(final byte[] data, final int size, boolean sentenceCompleted) {
            Log.d(TAG, "onVoice()");
            if(googleSpeechService == null) {
                return;
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String db_value = byteToDbValue(data,size);
                    addData(Integer.valueOf(db_value));
                    txtDbValue.setText(db_value);
                }
            });
            isSentenceCompleted = sentenceCompleted;
            googleSpeechService.recognize(data, size);
        }

        @Override
        public void onVoiceEnd() {
            Log.d(TAG, "onVoiceEnd()");
            if(googleSpeechService == null) {
                return;
            }
            googleSpeechService.finishRecognizing();
            if(TextUtils.isEmpty(googleTranscript))
                googleTranscript = "TIMEOUT";

            sendReply(COMMAND_SUCCESS, googleTranscript);
            stopVoiceRecorder();
        }
    };

    private void startVoiceRecorder(){
        if(mVoiceRecorder != null){
            mMediaActionSound.play(MediaActionSound.START_VIDEO_RECORDING);
            apiReady = true;
            mVoiceRecorder.start();
        }
    }

    private void stopVoiceRecorder(){
        if(mVoiceRecorder != null){
            mMediaActionSound.play(MediaActionSound.STOP_VIDEO_RECORDING);
            mVoiceRecorder.stop();
        }
    }

    private ArrayAdapter adapter;


    private void startGoogleSpeechService(){
        Intent googleSpeechAPIService = new Intent(this, GoogleSpeechService.class);
        bindService(googleSpeechAPIService, speechConnection, BIND_AUTO_CREATE);
        this.startService(googleSpeechAPIService);
    }

    private void stopGoogleSpeechService(){

        stopVoiceRecorder();
        googleSpeechService.removeListener(mSpeechServiceListener);
        unbindService(speechConnection);
        googleSpeechService = null;
    }

    private final ServiceConnection speechConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            googleSpeechService = GoogleSpeechService.from(binder);
            googleSpeechService.addListener(mSpeechServiceListener);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            googleSpeechService = null;
        }
    };

    private final GoogleSpeechService.Listener mSpeechServiceListener = new GoogleSpeechService.Listener() {
        @Override
        public void onSpeechRecognized(String text, boolean isFinal) {
            if (textMessage != null && !TextUtils.isEmpty(text)) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinal) {
                            textMessage.setText(null);
                            stringList.add(0,text);
                            adapter.notifyDataSetChanged();
                        } else {
                            textMessage.setText(text);
                        }
                    }
                });
            }
            if(isFinal){
                googleTranscript += text;
                if(mVoiceRecorder != null && isSentenceCompleted) {
                    mVoiceRecorder.dismiss();
                    isSentenceCompleted = false;
                }
            }
        }

        @Override
        public void onApiMessage(String msg) {
            if(msg.equals("completed")) {
                apiReady = true;
            }
            if(msg.equals("api_error")) {
                sendReply(COMMAND_SUCCESS, "API_ERROR");
                stopVoiceRecorder();
                apiReady = false;
            }
        }
    };
}
