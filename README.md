# 程曦Call SDK 文档说明

[Demo APK Download here](http://7xq276.com2.z0.glb.qiniucdn.com/call_example.apk)

###準備

一，下載[chainsea_call_sdk_1.0.3.aar](https://raw.githubusercontent.com/ninty90/Call-example/master/app/libs/chainsea_call_sdk_1.0.3.aar)

二，把`chainsea_call_sdk_1.0.3.aar`放入到app的libs目錄下，并在`build.gradle`添加abiFilters和依賴
```
defaultConfig {
        ...
        ndk {
            abiFilters "armeabi", "armeabi-v7a", "x86"
        }
    }
repositories{
	//添加libs目錄為依賴來源之一
    flatDir{
        dirs 'libs'
    }
	//添加該maven倉庫，用以導入日誌庫地址
	maven { url "https://jitpack.io" }
}

//在dependencies下添加依賴
compile(name:'chainsea_call_sdk_1.0.3', ext:'aar')
compile 'com.github.CPPAlien:VinciLog:2.0.1'
```

###使用說明

功能總入口`SipCallManager`，首先需要初始化
```
SipCallManager(Context context, String ip, int port)
```

**ip** : ip地址

**port** : 端口號

例如：
```
SipCallManager callManager = new SipCallManager(getContext(), "127.0.0.1", 5035);
```

初始化成功后，設置用戶信息

```
setUserInfo(String userId, String pw)
```

例如：

```
callManager.setUserInfo("1100017", "1234");
```

然後進行call動作

```
startCall(String to, Listener listener)

//回調接口
public interface Listener {
    void onCallEnd(); //通話結束
    void onCalling(); //通話成功
}
```


**to** : 需要撥打的電話號碼

**listener** : 通話回調接口

例如：
```
callManager.startCall("95555", this);
```

**注意：通話結束后需要調用release()操作來釋放資源**

###其他功能

`void hangup()` 掛斷處理

`void sendDtmf(char c)` 發送dtmf

`void setSpeaker(boolean isSpeakerEnabled)` 是否開啟外放，傳入true為開啟，false為關閉

`void setMute(boolean isMicMuted)`   是否禁言，true為禁言，false為不禁言