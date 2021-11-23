package ntu.mil.grpckebbi.Network;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.interaction.robot.interaction.InteractGrpc;
import com.interaction.robot.interaction.RobotConnectReply;
import com.interaction.robot.interaction.RobotConnectRequest;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import ntu.mil.grpckebbi.GrpcClientActivity;
import ntu.mil.grpckebbi.MainActivity;
import ntu.mil.grpckebbi.R;
import ntu.mil.grpckebbi.Utils.RobotCommand;
import ntu.mil.grpckebbi.Utils.WindowUtils;
import com.google.gson.Gson;

import static ntu.mil.grpckebbi.GrpcClientActivity.sendReply;
import static ntu.mil.grpckebbi.MainActivity.TAG;
import static ntu.mil.grpckebbi.MainActivity.mRobotAPI;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_FAILED;
import static ntu.mil.grpckebbi.Utils.Constants.COMMAND_SUCCESS;

public class Connection {
    private EditText ip_1, ip_2, ip_3, ip_4, host_port;
    private String ip;
    private TextView localIp;
    private int port;

    private Activity mActivity;
    public static InteractGrpc.InteractBlockingStub mInteracter;

    public Connection(Context context){
        this.mActivity = (Activity) context;
    }

    public InteractGrpc.InteractBlockingStub start(){
        SetupUI();
        LoadData();
        AssignIps();
        return mInteracter;
    }

    private void SetupUI(){
        ip_1 = this.mActivity.findViewById(R.id.ip1);
        ip_2 = this.mActivity.findViewById(R.id.ip2);
        ip_3 = this.mActivity.findViewById(R.id.ip3);
        ip_4 = this.mActivity.findViewById(R.id.ip4);
        host_port = this.mActivity.findViewById(R.id.port);

        ImageView connectBtn = this.mActivity.findViewById(R.id.connectBtn);
        connectBtn.setOnClickListener(v -> {
            mInteracter = Connect(ip);
            mRobotAPI.showFace();
        });

        localIp = this.mActivity.findViewById(R.id.ipAdress);
        localIp.setText(GetLocalIpAddress());
    }

    private InteractGrpc.InteractBlockingStub Connect(String saved_ip) {
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
                    sendReply(COMMAND_SUCCESS, "Locale set to " + locale);
                }
                else
                    sendReply(COMMAND_FAILED, "Locale not supported! Please use 'zh' (Chinese) or 'en' (English)");
//
            } catch (Exception e) {
                Log.e(TAG, "connect() Fail: " + e);
            }
        }
        else{
            Toast.makeText(this.mActivity,"Select an IP first!", Toast.LENGTH_LONG).show();
        }
        return mInteracter;
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
        SharedPreferences prefs = this.mActivity.getPreferences(Context.MODE_PRIVATE);
        ip = prefs.getString("ip", this.mActivity.getString(R.string.def_ip));
        port = prefs.getInt("port", Integer.parseInt(this.mActivity.getString(R.string.def_port)));
        host_port.setText(String.valueOf(port));
    }

    private void SaveData() {
        SharedPreferences sharedPref = this.mActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString("ip", ip);
        editor.putInt("port", port);
        editor.apply();
    }
}
