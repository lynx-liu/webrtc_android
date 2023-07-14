package com.android;

import android.Manifest;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson.JSON;
import com.android.core.ui.room.RoomInfo;
import com.android.permission.Permissions;
import com.webrtc.net.ICallback;
import com.webrtc.net.UrlConnRequest;
import com.webrtc.net.Urls;
import com.webrtc.socket.IUserState;
import com.android.core.voip.SocketManager;
import com.android.webrtc.R;

import java.util.List;

public class LoginActivity extends AppCompatActivity implements IUserState {
    private EditText etRoom;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etRoom = findViewById(R.id.et_room);
        etRoom.setText(App.getInstance().getRoomId());

        String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
        Permissions.request(this, permissions, integer -> {
            if (integer == 0) {
                // 权限同意
                String username = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

                // 设置用户名
                App.getInstance().setUsername(username);

                // 添加登录回调
                SocketManager.getInstance().addUserStateCallback(this);
            } else {
                // 权限拒绝
                finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (SocketManager.getInstance().isLogged()) {
            SocketManager.getInstance().unConnect();
        }
    }

    public void onLogin(View view) {
        String roomid = etRoom.getText().toString().trim();
        if(TextUtils.isEmpty(roomid)) {
            Toast.makeText(getApplicationContext(), "频道号不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        App.getInstance().setRoomId(roomid);

        // 连接socket:登录
        SocketManager.getInstance().connect(Urls.WS, App.getInstance().getUsername(), 0);
    }

    @Override
    public void userLogin() {
        String url = Urls.getRoomList();
        UrlConnRequest.getInstance().get(url, null, iCallback);
    }

    ICallback iCallback = new ICallback() {
        @Override
        public void onSuccess(String result) {
            Log.d("llx", result);
            List<RoomInfo> roomInfos = JSON.parseArray(result, RoomInfo.class);

            boolean isOutgoing = true;
            String roomid = App.getInstance().getRoomId();
            for(RoomInfo roomInfo:roomInfos) {
                if(roomInfo.roomId.equals(roomid)) {
                    isOutgoing = false;
                }
            }
            CallMultiActivity.openActivity(LoginActivity.this, App.getInstance().getRoomId(), isOutgoing);
        }

        @Override
        public void onFailure(int code, Throwable t) {
            Log.d("llx", "code:" + code + ",msg:" + t.toString());
        }
    };

    @Override
    public void userLogout() {

    }
}
