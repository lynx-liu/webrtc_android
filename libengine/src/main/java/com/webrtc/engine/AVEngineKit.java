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

    public void sendRefuseOnPermissionDenied(String room, String inviteId) {
        if (mCurrentCallSession != null) {
            endCall();
        } else {
            iSessionEvent.sendRefuse(room, inviteId, EnumType.RefuseType.Hangup.ordinal());
        }
    }

    public void sendDisconnected(String room, String toId, boolean isCrashed) {
        iSessionEvent.sendDisConnect(room, toId, isCrashed);
    }

    // 拨打电话
    public boolean startOutCall(Context context, final String room, final String targetId,
                                final boolean audioOnly) {
        // 忙线中
        if (mCurrentCallSession != null && mCurrentCallSession.getState() != EnumType.CallState.Idle) {
            Log.i(TAG, "startCall error,currentCallSession is exist");
            return false;
        }
        // 初始化会话
        mCurrentCallSession = new CallSession(context, room, audioOnly, iSessionEvent);
        mCurrentCallSession.setTargetId(targetId);
        mCurrentCallSession.setIsComing(false);
        mCurrentCallSession.setCallState(EnumType.CallState.Outgoing);
        // 创建房间
        mCurrentCallSession.createHome(room, 2);

        return true;
    }

    // 接听电话
    public boolean startInCall(Context context, final String room, final String targetId,
                               final boolean audioOnly) {
        // 忙线中
        if (mCurrentCallSession != null && mCurrentCallSession.getState() != EnumType.CallState.Idle) {
            // 发送->忙线中...
            Log.i(TAG, "startInCall busy,currentCallSession is exist,start sendBusyRefuse!");
            mCurrentCallSession.sendBusyRefuse(room, targetId);
            return false;
        }

        // 初始化会话
        mCurrentCallSession = new CallSession(context, room, audioOnly, iSessionEvent);
        mCurrentCallSession.setTargetId(targetId);
        mCurrentCallSession.setIsComing(true);
        mCurrentCallSession.setCallState(EnumType.CallState.Incoming);

        // 开始响铃并回复
        mCurrentCallSession.shouldStartRing();
        mCurrentCallSession.sendRingBack(targetId, room);

        return true;
    }

    // 挂断会话
    public void endCall() {
        Log.d(TAG, "endCall mCurrentCallSession != null is " + (mCurrentCallSession != null));
        if (mCurrentCallSession != null) {
            // 停止响铃
            mCurrentCallSession.shouldStopRing();

            if (mCurrentCallSession.isComing()) {
                if (mCurrentCallSession.getState() == EnumType.CallState.Incoming) {
                    // 接收到邀请，还没同意，发送拒绝
                    mCurrentCallSession.sendRefuse();
                } else {
                    // 已经接通，挂断电话
                    mCurrentCallSession.leave();
                }
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
        mCurrentCallSession = new CallSession(context, room, false, iSessionEvent);
        mCurrentCallSession.setIsComing(true);
        mCurrentCallSession.joinHome(room);
    }

    public boolean sendData(byte[] data) {
        if(mCurrentCallSession==null)
            return false;
        return mCurrentCallSession.sendData(data);
    }

    public void createAndJoinRoom(Context context, String room) {
        // 忙线中
        if (mCurrentCallSession != null && mCurrentCallSession.getState() != EnumType.CallState.Idle) {
            Log.e(TAG, "joinRoom error,currentCallSession is exist");
            return;
        }
        mCurrentCallSession = new CallSession(context, room, false, iSessionEvent);
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
