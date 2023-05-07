package com.webrtc.net;

public interface ICallback {
    void onSuccess(String result);
    void onFailure(int code, Throwable t);
}
