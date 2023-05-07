package com.vrviu;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.vrviu.core.base.BaseActivity;
import com.webrtc.net.Urls;
import com.webrtc.socket.IUserState;
import com.vrviu.core.voip.SocketManager;
import com.vrviu.webrtc.R;

public class LoginActivity extends BaseActivity implements IUserState {
    private EditText etUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        setSupportActionBar(findViewById(R.id.toolbar));
        etUser = findViewById(R.id.et_user);
        etUser.setText(App.getInstance().getUsername());

        if (SocketManager.getInstance().isLogged()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    public void onLogin(View view) {
        String username = etUser.getText().toString().trim();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "please input your name", Toast.LENGTH_LONG).show();
            return;
        }

        // 设置用户名
        App.getInstance().setUsername(username);
        // 添加登录回调
        SocketManager.getInstance().addUserStateCallback(this);
        // 连接socket:登录
        SocketManager.getInstance().connect(Urls.WS, username, 0);
    }

    @Override
    public void userLogin() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public void userLogout() {

    }
}
