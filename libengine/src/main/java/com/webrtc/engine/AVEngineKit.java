package com.webrtc.engine;

import android.content.Context;
import android.util.Log;

import com.webrtc.session.CallSession;
import com.webrtc.session.EnumType;
import com.webrtc.session.ISessionEvent;

public class AVEngineKit {
    private final static String TAG = "AVEngineKit";
    private CallSession mCurrentCallSession;
    private ISessionEvent iSessionEvent;

    public AVEngineKit(ISessionEvent iSocketEvent) {
        iSessionEvent = iSocketEvent;
    }

    public void sendDisconnected(String room, String toId, boolean isCrashed) {
        iSessionEvent.sendDisConnect(room, toId, isCrashed);
    }

    // 挂断会话
    public void endCall() {
        Log.d(TAG, "endCall mCurrentCallSession != null is " + (mCurrentCallSession != null));
        if (mCurrentCallSession != null) {
            if (mCurrentCallSession.isComing()) {
                mCurrentCallSession.leave();
            } else {
                if (mCurrentCallSession.getState() == EnumType.CallState.Outgoing) {
                    mCurrentCallSession.sendCancel();
                } else {
                    // 已经接通，挂断电话
                    mCurrentCallSession.leave();
                }
            }
            mCurrentCallSession.setCallState(EnumType.CallState.Idle);
        }
    }

    // 加入房间
    public void joinRoom(Context context, String room) {
        // 忙线中
        if (mCurrentCallSession != null && mCurrentCallSession.getState() != EnumType.CallState.Idle) {
            Log.e(TAG, "joinRoom error,currentCallSession is exist");
            return;
        }
        mCurrentCallSession = new CallSession(context, room, iSessionEvent);
        mCurrentCallSession.setIsComing(true);
        mCurrentCallSession.joinHome(room);
    }

    public void createAndJoinRoom(Context context, String room) {
        // 忙线中
        if (mCurrentCallSession != null && mCurrentCallSession.getState() != EnumType.CallState.Idle) {
            Log.e(TAG, "joinRoom error,currentCallSession is exist");
            return;
        }
        mCurrentCallSession = new CallSession(context, room, iSessionEvent);
        mCurrentCallSession.setIsComing(false);
        mCurrentCallSession.createHome(room, 9);
    }

    // 离开房间
    public void leaveRoom() {
        if (mCurrentCallSession != null) {
            mCurrentCallSession.leave();
            mCurrentCallSession.setCallState(EnumType.CallState.Idle);
        }
    }

    // 获取对话实例
    public CallSession getCurrentSession() {
        return this.mCurrentCallSession;
    }
}
