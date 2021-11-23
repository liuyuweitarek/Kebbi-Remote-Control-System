# com.nuwarobotics.service.facecontrol()使用方式

> Author: Yuwei Liu
> 

# 系統設定(這個最白爛，沒開試到死都沒有)

## 1. 首先確保 Kebbi系統的FacePreseenter被設為「主畫面應用程式」。

---

![258E2572-AD8D-43D3-8196-9EAA2FD2976B.jpeg](com%20nuwarobotics%20service%20facecontrol()%E4%BD%BF%E7%94%A8%E6%96%B9%E5%BC%8F%20550ca637b60049dcb302ab87dc841851/258E2572-AD8D-43D3-8196-9EAA2FD2976B.jpeg)

![000F7E74-5F90-4341-9C26-8DE7FA8B16C7.jpeg](com%20nuwarobotics%20service%20facecontrol()%E4%BD%BF%E7%94%A8%E6%96%B9%E5%BC%8F%20550ca637b60049dcb302ab87dc841851/000F7E74-5F90-4341-9C26-8DE7FA8B16C7.jpeg)

![4710771D-FF32-4C49-A765-8D3FAB6DA593.jpeg](com%20nuwarobotics%20service%20facecontrol()%E4%BD%BF%E7%94%A8%E6%96%B9%E5%BC%8F%20550ca637b60049dcb302ab87dc841851/4710771D-FF32-4C49-A765-8D3FAB6DA593.jpeg)

---

## 2. 「設定」→「關於凱比」→「系統設定」→「應用程式和通知」→「FacePreseenter」→「進階」→「顯示在其他應用程式上層」→「勾選允許」

---

## 3. 程式中也可以先做檢查，但要怎麼自動開要再研究。

```java
@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(mFaceManager.getDeviceSupport(context) != 0){
       //support Face Control
    }else{
       //not support Face control
    }
}
```

# Android程式調用

下面範例會:

1.  機器人切換APP介面到機器人面部表情介面
2.  機器人面部表情介面「動嘴巴」，「同時」說一句話
3.  說完話後，嘴巴閉起來
4.  點左眼，則由機器人面部表情切換為APP介面

ShowFaceActivity.java

```java
package com.nuwarobotics.example.debug;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

//import nuwa sdk
import com.nuwarobotics.example.R;
import com.nuwarobotics.service.IClientId;
import com.nuwarobotics.service.agent.NuwaRobotAPI;
import com.nuwarobotics.service.agent.VoiceEventCallback;
import com.nuwarobotics.service.agent.VoiceEventListener;
import com.nuwarobotics.service.facecontrol.FaceControlManager;
import com.nuwarobotics.service.facecontrol.IonCompleteListener;
import com.nuwarobotics.service.facecontrol.UnityFaceCallback;
import com.nuwarobotics.service.facecontrol.UnityFaceManager;
import com.nuwarobotics.service.facecontrol.utils.ServiceConnectListener;

public class ShowFaceActivity extends AppCompatActivity {
    private final String TAG = "DebugShowFace";

    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;
    VoiceEventListener mVoiceEventListener;

    UnityFaceManager mFaceManager;
    UnityFaceCallback mUnityCallback;
    ServiceConnectListener mFaceControlConnect;
    IonCompleteListener.Stub mUnityStubListener;

    Context mContext;
    Button mBtnHide;
    String json = "666_RE_Bye";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.debug_show_face);
        mContext = this.getApplicationContext();
        mClientId = new IClientId(this.getPackageName());
        mVoiceEventListener = new VoiceEventCallback() {
            @Override
            public void onTTSComplete(boolean isError) {
                super.onTTSComplete(isError);
                mFaceManager.mouthOff();
            }
        };

        mRobotAPI = new NuwaRobotAPI(this, mClientId);
        mRobotAPI.registerVoiceEventListener(mVoiceEventListener);

        mFaceControlConnect = new ServiceConnectListener() {
            @Override
            public void onConnectChanged(ComponentName componentName, boolean b) {
                Log.d(TAG, "faceService onbind : " + b);
            }
        };
        mUnityStubListener = new IonCompleteListener.Stub() {
            @Override
            public void onComplete(String s) throws RemoteException {
                Log.d("FaceControl", "onMotionComplete:" + s);
            }
        };
        mUnityCallback = new UnityFaceCallback() {
            @Override
            public void on_touch_left_eye() {
                Log.d("FaceControl", "on_touch_left_eye()");
                mFaceManager.hideFace();
            }
        };

        mRobotAPI.initFaceControl(this, this.getClass().getName(), mFaceControlConnect);
        
				mFaceManager = mRobotAPI.UnityFaceManager().getInstance();
        mFaceManager.registerCallback(mUnityCallback);

        mBtnHide = (Button) findViewById(R.id.btn_showface);
        mBtnHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Test ShowFace and Different stage of ");
                mFaceManager.showFace();
                mFaceManager.mouthOn(200);
                mRobotAPI.startTTS("終於可以把臉露出來了");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRobotAPI.release();

        mFaceManager.unregisterCallback(mUnityCallback);
        mFaceManager.release();
    }
}
```

如果只是要把APP介面收起來，露出機器人的臉的話，NuwaRobotAPI.showFace() 其實就很夠用；但是更生動的互動應該是隨著情緒模組運算，以及語句本身想表達的意思變更面部表情會更好，因此有必要將UnityFaceManager整合到新系統的API中。

值得一提的是這四個物件之間的關係：

UnityFaceManager mFaceManager;
UnityFaceCallback mUnityCallback;
ServiceConnectListener mFaceControlConnect;
IonCompleteListener.Stub mUnityStubListener;