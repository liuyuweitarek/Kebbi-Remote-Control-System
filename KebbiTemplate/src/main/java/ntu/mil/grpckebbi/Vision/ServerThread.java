package ntu.mil.grpckebbi.Vision;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import static ntu.mil.grpckebbi.Vision.Constants.STREAM_CANCELED;
import static ntu.mil.grpckebbi.Vision.Constants.STREAM_CONNECTED;

public class ServerThread implements Runnable{
    private static final String TAG = ServerThread.class.getSimpleName();

    private int mServerPort;
    private String mServerIP;
    private Handler mHandler;
    private VideoStreamService streamService;
    private ServerSocket serverSocket;

    ServerThread(Context context, String ip, int port, Handler handler){
        super();
        mServerIP = ip;
        mServerPort = port;
        mHandler = handler;
        streamService = (VideoStreamService) context;
    }

    void stop(){
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(mServerPort);
            serverSocket.setReuseAddress(true);
            mHandler.post(() -> Log.d(TAG, "Listening on IP: " + mServerIP));
            Socket socket = serverSocket.accept();
            new Thread(new OutputThread(socket)).start();
        }catch(Exception e){
            Log.e(TAG, e.getMessage());
        }
    }

    public class OutputThread implements Runnable {
        Socket socket;
        OutputStream os = null;

        OutputThread(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            if(socket != null){
                streamService.setStreamState(STREAM_CONNECTED);
                try {
                    os = socket.getOutputStream();
                    while(socket != null){
                        DataOutputStream dos = new DataOutputStream(os);
                        dos.writeInt(4);
                        dos.writeUTF("H");
                        dos.writeInt(streamService.getFrame().size());
                        dos.flush();
                        dos.write(streamService.getFrame().toByteArray());
                        dos.flush();
                        Thread.sleep(1000/15);

                    }
                } catch (Exception e) {
                    //e.printStackTrace();
                    try {
                        if (os != null){
                            Log.d(TAG, "os closed");
                            os.close();
                        }
                        if(socket != null){
                            Log.d(TAG, "s closed");
                            socket.close();
                            socket = null;
                            serverSocket.close();
                            streamService.setStreamState(STREAM_CANCELED);
                        }
                    } catch (Exception e2) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
