# NuwaRobotAPI.setSpeakParameter()

> Author: Yuwei Liu
> 

![Untitled](NuwaRobotAPI%20setSpeakParameter()%202b42ebcde281437aa76a446474d2aaea/Untitled.png)

---

修正:

1.  實際用法需要3個參數，上圖的原文件範例中只有2個。
2.  已確定可用參數 <str>volume, <str>speed, <str>pitch, 範圍皆介於 <str>0.01-9.99，超過範圍TTS會壞，機器人無法發聲。  → [Better]
3.  未知用法參數 isForced , gain → [ASK]
4.  用途未知，可能是某種Config VoiceEventListener.SpeakType.NONE VoiceEventListener.SpeakType.NORMAL → [ASK]