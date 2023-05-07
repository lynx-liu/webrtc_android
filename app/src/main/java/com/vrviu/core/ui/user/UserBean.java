package com.vrviu.core.ui.user;

import android.text.TextUtils;

public class UserBean {
    private String userId;
    private String avatar;
    private String nickName;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getNickName() {
        if (TextUtils.isEmpty(nickName)) {
            return userId;
        }
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }
}
