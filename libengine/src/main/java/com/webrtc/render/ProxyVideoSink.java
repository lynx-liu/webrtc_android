package com.webrtc.render;

import android.util.Log;

import org.webrtc.VideoFrame;
import org.webrtc.VideoSink;

public class ProxyVideoSink implements VideoSink {
    private static final String TAG = "ProxyVideoSink";
    private VideoSink target;
    public ProxyVideoSink() {

    }

    @Override
    synchronized public void onFrame(VideoFrame frame) {
        if (target == null) {
            Log.d(TAG, "Dropping frame in proxy because target is null.");
            return;
        }
        target.onFrame(frame);
    }

    synchronized public void setTarget(VideoSink target) {
        this.target = target;
    }
}