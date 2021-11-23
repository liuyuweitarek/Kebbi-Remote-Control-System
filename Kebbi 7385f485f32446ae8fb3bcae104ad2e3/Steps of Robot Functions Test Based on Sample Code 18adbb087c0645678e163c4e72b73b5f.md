# Steps of Robot Functions Test Based on Sample Code Project

> Author: Yuwei Liu
> 

### 1. 新增"debug目錄"在官方的"Sample Code目錄"底下，並新增自己要測試用的Activity。

![Untitled](Steps%20of%20Robot%20Functions%20Test%20Based%20on%20Sample%20Code%2018adbb087c0645678e163c4e72b73b5f/Untitled.png)

### 2. 創建一個Debug用的Activity 四步驟

   java/debug/DebugBaseActivity.java

```java
package {Your Package Name};

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

public class DebugBaseActivity extends AppCompatActivity {
    private final String TAG = "DebugBaseActivity";
		
		// NuwaAPI Object
    NuwaRobotAPI mRobotAPI;
    IClientId mClientId;
    VoiceEventListener mVoiceEventListener;
    RobotEventListener mRobotEventListener;
		
		// UI Object
    Button mBtnTestSpeak;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        //將設計的UI載入
        setContentView(R.layout.debug_example);
        mContext = this.getApplicationContext();
				
				// 1.機器人的核心API:
        // 以Activity的名稱生成IClientId，並以此創建NuwaRobotAPI
        mClientId = new IClientId(this.getPackageName());
				mRobotAPI = new NuwaRobotAPI(this, mClientId);

        // 2.機器人的兩大狀態偵測 RobotEventCallback、VoiceEventCallback
        // 所謂的Callback會定義機器人執行A功能的不同階段，這些階段機器人可能會傳出內部處理的資訊，方便我們做進一步以此判斷並執行其他動作；或是我們可以將外部的資訊送入內部操作機器人。
				// 舉例來說：下方我們在VoiceEventCallback定義，當機器人做完文字轉語音時，要把臉秀出來。
				mVoiceEventListener = new VoiceEventCallback() {
            @Override
            public void onTTSComplete(boolean isError) {
                super.onTTSComplete(isError);
                //TODO 在這邊寫任何你希望機器人做完TTS後要做的事情
								// e.g. mRobotAPI.showFace();
            }
						...
        };
				
				//同理，請參考官方文件中對這些Callback的狀態定義
				mRobotEventListener = new RobotEventCallback() {
            @Override
            public void onMotionComplete(boolean isError) {
                super.onMotionComplete(isError);
                
            }
						...
        };

        // 3.上面定義的Callback需要讓機器人的核心API知道，所以需要register
        // 既然有register，那麼自然就有unregister，通常會在這個Activity結束後-> onDestroy()
        mRobotAPI.registerVoiceEventListener(mVoiceEventListener);
				mRobotAPI.registerRobotEventListener(mRobotEventListener);
        
        
        // 4. 呼叫設計的UI
        // 這裡定義我們設計的按鈕，以及按下按鈕後會做的動作
        mBtnTestSpeak = (Button) findViewById(R.id.btn_test_speak);
        mBtnTestSpeak.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: Test if NuwaRobotAPI.startTTS() work.");
                mRobotAPI.startTTS("出生第一次說話");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 5. 使用完後需要釋出資源，避免Memory Leak
        mRobotAPI.release();
    }
}
```

res/layout/debug_example.xml ：定義APP UI介面

```xml
<?xml version="1.0" encoding="utf-8"?>
<android.support.constraint.ConstraintLayout
    xmlns:android="http://schemas.android.com/apk/res/android" android:layout_width="match_parent"
    android:layout_height="match_parent">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        android:orientation="horizontal">

        <Button
            android:id="@+id/btn_test_speak"
            android:layout_gravity="center"
            android:layout_marginLeft="50dp"
            android:layout_marginRight="50dp"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:text="Test Speak"/>
    </LinearLayout>
</android.support.constraint.ConstraintLayout>
```

AndroidManifest.xml：讓新定義的Activity被APP主文件知道(注意下方繁體中文的部分就好，其他都是官方文件寫的)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.nuwarobotics.example">
    <!-- 获取手机录音机使用权限，识别、语义理解需要用到此权限 -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" /> <!-- 保存文件需要用到此权限 -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" /> <!-- 保存文件需要用到此权限 -->
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" /> <!-- 云端功能需要用到此权限 -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />

    <application
        android:name=".NuwaApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/AppTheme">
        <activity
            android:name=".Main"
            android:theme="@style/AppTheme.NoActionBar">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

				<!-- 在這裡新增我們自己的 Activity/>
        <activity
            android:name=".debug.DebugBaseActivity"
            android:theme="@style/AppTheme.NoActionBar" />
        
        <!-- 下面這邊都是官方給的Sample Code 的 Activity/>
        <activity
            android:name=".sample.SampleActivity"
            android:theme="@style/AppTheme.NoActionBar" />
        ...
    </application>

</manifest>
```

assets/cfg_functions.xml ：官方的Sample Code用ListView列表呈現，列表中每個列都是一個獨立的Activity，因此我們要將我們自己新增的Activity加進這個列表裡面。

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<ActivityList>
		<!-- 這邊是我們自己新增的Activity-->
    <Label name="debug.DebugBaseActivity">[Debug Speak] Test Speak </Label>
    
		
		<!-- 下面是官方Sample的Activity-->
    <Label name="motor.MotorControlActivity">[Motor] Motor control Example</Label>
    <Label name="motor.MovementControlActivity">[Motor] Movement control Example</Label>
    <!--<Label name="motion.MotionSDKExampleActivity">Motion SDK Example</Label>-->
    <Label name="motion.demo.QueryMotionActivity">[Motion] Query Motion List Example</Label>
    <Label name="motion.demo.PlayMotionActivity">[Motion] Motion Play Example</Label>
    <Label name="motion.demo.ControlMotionActivity">[Motion] Motion Play/Pause/Resume Control Example</Label>
    <Label name="motion.demo.WindowControlWithMotionActivity">[Motion] Motion Play with window view control Example</Label>
    <Label name="motion.MotionTtsExampleActivity">[Motion] Motion with TTS Example</Label>

    <Label name="led.LEDExampleActivity">[HW] LED Control Example</Label>
    <Label name="sensor.SensorExampleActivity">[HW] Sensor Detect Example</Label>
    <!-- Voice Example-->
    <Label name="voice.WakeupActivity">[Voice] Wakeup Example</Label>
    <Label name="voice.LocalcmdActivity">[Voice] Local command Example</Label>
    <Label name="voice.CloudASRActivity">[Voice] Cloud ASR Example</Label>
    <Label name="voice.LocalcmdAndCloudASRActivity">[Voice] Local command and Cloud ASR Example</Label>
    <Label name="voice.TTSActivity">[Voice] TTS Example</Label>

    <Label name="activity.DisablePowerkeyExampleActivity">[System] Disable Power Key Example</Label>

    <Label name="activity.FaceControlExampleActivity">[Face] Face Control Example</Label>

    <Label name="activity.startNuwaFaceRecognitionActivity">[Contact] Lunch Nuwa Add Contact(Face Recognition) Example</Label>

    <Label name="activity.StartNuwaAuthorizationActivity">[Auth] Launch NUWA Authorization Example</Label>

</ActivityList>
```