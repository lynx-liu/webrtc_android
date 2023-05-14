package com.android;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;

import com.webrtc.session.CallSession;
import com.android.core.voip.VoipEvent;
import com.webrtc.engine.AVEngineKit;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

public class App extends Application {
    private static App app;
    private static AVEngineKit avEngineKit;
    private String username = "";
    private String roomId = "";
    private String otherUserId = "";

    @Override
    public void onCreate() {
        super.onCreate();
        app = this;
        avEngineKit = new AVEngineKit(new VoipEvent());// 初始化信令

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
                CallSession session = avEngineKit.getCurrentSession();
                Log.d("llx", "uncaughtException session = " + session);
                if (session != null) {
                    avEngineKit.endCall();
                } else {
                    avEngineKit.sendDisconnected(getRoomId(), getOtherUserId(),true);
                }

                Writer result = new StringWriter();
                PrintWriter printWriter = new PrintWriter(result);
                Throwable cause = throwable;
                while (null != cause) {
                    cause.printStackTrace(printWriter);
                    cause = cause.getCause();
                }
                Log.e("llx", result.toString());
                printWriter.close();
            }
        });
    }

    public static App getInstance() {
        return app;
    }

    public AVEngineKit getAvEngineKit() {
        return avEngineKit;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRoomId() {
        return roomId;
    }

    public void setRoomId(String roomId) {
        this.roomId = roomId;
    }

    public String getOtherUserId() {
        return otherUserId;
    }

    public void setOtherUserId(String otherUserId) {
        this.otherUserId = otherUserId;
    }
}
