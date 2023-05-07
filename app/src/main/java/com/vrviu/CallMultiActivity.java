package com.vrviu;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.vrviu.core.base.BaseActivity;
import com.vrviu.core.voip.FragmentMeeting;
import com.vrviu.permission.Permissions;
import com.webrtc.session.CallSession;
import com.webrtc.session.EnumType;
import com.webrtc.engine.AVEngineKit;
import com.vrviu.webrtc.R;

import java.util.UUID;

/**
 * 多人通话界面
 */
public class CallMultiActivity extends BaseActivity implements CallSession.CallSessionCallback, View.OnClickListener {
    private AVEngineKit gEngineKit;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ImageView meetingHangupImageView;
    private CallSession.CallSessionCallback currentFragment;
    public static final String EXTRA_MO = "isOutGoing";

    public static void openActivity(Activity activity, String room, boolean isOutgoing) {
        Intent intent = new Intent(activity, CallMultiActivity.class);
        intent.putExtra("room", room);
        intent.putExtra(EXTRA_MO, isOutgoing);
        activity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
        setContentView(R.layout.activity_multi_call);

        meetingHangupImageView = findViewById(R.id.meetingHangupImageView);
        meetingHangupImageView.setOnClickListener(this);

        Fragment fragment = new FragmentMeeting();
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction().add(R.id.meeting_container, fragment).commit();
        currentFragment = (CallSession.CallSessionCallback) fragment;

        gEngineKit = App.getInstance().getAvEngineKit();

        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA};
        Permissions.request(this, permissions, integer -> {
            if (integer == 0) {
                // 权限同意
                Intent intent = getIntent();
                String room = intent.getStringExtra("room");
                boolean isOutgoing = intent.getBooleanExtra(EXTRA_MO, false);

                init(room, isOutgoing);
            } else {
                // 权限拒绝
                CallMultiActivity.this.finish();
            }
        });
    }

    @Override
    public void onBackPressed() {

    }

    private void initView() {

    }

    private void init(String room, boolean isOutgoing) {
        if (isOutgoing) {
            // 创建一个房间并进入
            gEngineKit.createAndJoinRoom(this,
                    "room-" + UUID.randomUUID().toString().substring(0, 16));
        } else {
            // 加入房间
            gEngineKit.joinRoom(getApplicationContext(), room);
        }

        CallSession session = gEngineKit.getCurrentSession();
        if (session == null) {
            this.finish();
        } else {
            session.setSessionCallback(this);
        }
    }

    @TargetApi(19)
    private static int getSystemUiVisibility() {
        int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        return flags;
    }

    //-------------------------------------------------回调相关------------------------------------
    @Override
    public void didCallEndWithReason(EnumType.CallEndReason var1) {
        finish();
    }

    @Override
    public void didChangeState(EnumType.CallState callState) {
        handler.post(() -> currentFragment.didChangeState(callState));
    }

    @Override
    public void didChangeMode(boolean var1) {
        handler.post(() -> currentFragment.didChangeMode(var1));
    }

    @Override
    public void didCreateLocalVideoTrack() {
        handler.post(() -> currentFragment.didCreateLocalVideoTrack());
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        handler.post(() -> currentFragment.didReceiveRemoteVideoTrack(userId));
    }

    @Override
    public void didUserLeave(String userId) {
        handler.post(() -> currentFragment.didUserLeave(userId));
    }

    @Override
    public void didError(String var1) {
        finish();
    }

    @Override
    public void didDisconnected(String userId) {

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.meetingHangupImageView:
                handleHangup();
                break;
        }
    }

    // 处理挂断事件
    private void handleHangup() {
        gEngineKit.leaveRoom();
        this.finish();
    }
}
