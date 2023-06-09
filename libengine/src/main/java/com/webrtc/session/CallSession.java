package com.webrtc.session;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.webrtc.engine.AVEngine;
import com.webrtc.engine.EngineCallback;
import com.webrtc.engine.WebRTCEngine;

import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 会话层
 */
public class CallSession implements EngineCallback {
    private static final String TAG = "CallSession";
    private WeakReference<CallSessionCallback> sessionCallback;
    private final ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());
    // session参数
    private boolean mIsAudioOnly;
    // 房间人列表
    private List<String> mUserIDList;
    // 单聊对方Id/群聊邀请人
    public String mTargetId;
    // 房间Id
    private String mRoomId;
    // myId
    public String mMyId;
    // 房间大小
    private int mRoomSize;

    private boolean mIsComing;
    private EnumType.CallState _callState = EnumType.CallState.Idle;
    private long startTime;

    private final AVEngine avEngine;
    private final ISessionEvent iSessionEvent;

    public CallSession(Context context, String roomId, boolean audioOnly, ISessionEvent iSessionEvent) {
        executor = Executors.newSingleThreadExecutor();
        this.mIsAudioOnly = audioOnly;
        this.mRoomId = roomId;

        this.iSessionEvent = iSessionEvent;
        avEngine = AVEngine.createEngine(new WebRTCEngine(audioOnly, context));
        avEngine.init(this);
    }

    // ----------------------------------------各种控制--------------------------------------------

    // 创建房间
    public void createHome(String room, int roomSize) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.createRoom(room, roomSize);
            }
        });
    }

    // 加入房间
    public void joinHome(String roomId) {
        executor.execute(() -> {
            _callState = EnumType.CallState.Connecting;
             Log.d(TAG, "joinHome mEvent = " + iSessionEvent);
            setIsComing(true);
            if (iSessionEvent != null) {
                iSessionEvent.sendJoin(roomId);
            }
        });
    }

    //开始响铃
    public void shouldStartRing() {
        if (iSessionEvent != null) {
            iSessionEvent.shouldStartRing(true);
        }
    }

    // 关闭响铃
    public void shouldStopRing() {
        Log.d(TAG, "shouldStopRing mEvent != null is " + (iSessionEvent != null));
        if (iSessionEvent != null) {
            iSessionEvent.shouldStopRing();
        }
    }

    // 发送响铃回复
    public void sendRingBack(String targetId, String room) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.sendRingBack(targetId, room);
            }
        });
    }

    // 发送拒绝信令
    public void sendRefuse() {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                // 取消拨出
                iSessionEvent.sendRefuse(mRoomId, mTargetId, EnumType.RefuseType.Hangup.ordinal());
            }
        });
		release(EnumType.CallEndReason.Hangup);
    }

    // 发送忙时拒绝
    public void sendBusyRefuse(String room, String targetId) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                // 取消拨出
                iSessionEvent.sendRefuse(room, targetId, EnumType.RefuseType.Busy.ordinal());
            }
        });
		release(EnumType.CallEndReason.Hangup);
    }

    // 发送取消信令
    public void sendCancel() {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                // 取消拨出
                List<String> list = new ArrayList<>();
                list.add(mTargetId);
                iSessionEvent.sendCancel(mRoomId, list);
            }
        });
		release(EnumType.CallEndReason.Hangup);
    }

    public boolean sendData(byte[] data) {
        return avEngine.sendData(data);
    }

    // 离开房间
    public void leave() {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.sendLeave(mRoomId, mMyId);
            }
        });
        // 释放变量
        release(EnumType.CallEndReason.Hangup);
    }

    // 切换到语音接听
    public void sendTransAudio() {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                // 发送到对面，切换到语音
                iSessionEvent.sendTransAudio(mTargetId);
            }
        });
    }

    // 设置静音
    public boolean toggleMuteAudio(boolean enable) {
        return avEngine.muteAudio(enable);
    }

    // 设置扬声器
    public boolean toggleSpeaker(boolean enable) {
        return avEngine.toggleSpeaker(enable);
    }

    // 设置扬声器
    public boolean toggleHeadset(boolean isHeadset) {
        return avEngine.toggleHeadset(isHeadset);
    }

    // 切换到语音通话
    public void switchToAudio() {
        mIsAudioOnly = true;
        // 告诉远端
        sendTransAudio();
        // 本地切换
        if (sessionCallback != null && sessionCallback.get() != null) {
            sessionCallback.get().didChangeMode(true);
        }
    }

    // 调整摄像头前置后置
    public void switchCamera() {
        avEngine.switchCamera();
    }

    public void switchLocalVideoEnable() {
        avEngine.switchLocalVideoEnable();
    }

    // 释放资源
    private void release(EnumType.CallEndReason reason) {
        executor.execute(() -> {
            // 释放内容
            avEngine.release();
            // 状态设置为Idle
            _callState = EnumType.CallState.Idle;

            //界面回调
            if (sessionCallback != null && sessionCallback.get() != null) {
                sessionCallback.get().didCallEndWithReason(reason);
            } else {
				//TODO 结束会话
			}
        });
    }

    //------------------------------------receive---------------------------------------------------

    // 加入房间成功
    public void onJoinHome(String myId, String users, int roomSize) {
        // 开始计时
        mRoomSize = roomSize;
        startTime = 0;
        handler.post(() -> executor.execute(() -> {
            mMyId = myId;
            List<String> strings;
            if (!TextUtils.isEmpty(users)) {
                String[] split = users.split(",");
                strings = Arrays.asList(split);
                mUserIDList = strings;
            }

            // 发送邀请
            if (!mIsComing) {
                if (roomSize == 2) {
                    List<String> inviteList = new ArrayList<>();
                    inviteList.add(mTargetId);
                    iSessionEvent.sendInvite(mRoomId, inviteList, mIsAudioOnly);
                }
            } else {
                avEngine.joinRoom(mUserIDList);
            }

            if (!isAudioOnly()) {
                // 画面预览
                if (sessionCallback != null && sessionCallback.get() != null) {
                    sessionCallback.get().didCreateLocalVideoTrack();
                }
            }
        }));
    }

    // 新成员进入
    public void newPeer(String userId) {
        handler.post(() -> executor.execute(() -> {
            // 其他人加入房间
            avEngine.userIn(userId);

            // 关闭响铃
            if (iSessionEvent != null) {
                iSessionEvent.shouldStopRing();
            }
            // 更换界面
            _callState = EnumType.CallState.Connected;

            if (sessionCallback != null && sessionCallback.get() != null) {
                startTime = System.currentTimeMillis();
                sessionCallback.get().didChangeState(_callState);

            }
        }));
    }

    // 对方已拒绝
    public void onRefuse(String userId, int type) {
        avEngine.userReject(userId, type);
    }

    // 对方已响铃
    public void onRingBack(String userId) {
        if (iSessionEvent != null) {
            iSessionEvent.onRemoteRing();
            //mEvent.shouldStartRing(false);
        }
    }

    // 切换到语音
    public void onTransAudio(String userId) {
        mIsAudioOnly = true;
        // 本地切换
        if (sessionCallback != null && sessionCallback.get() != null) {
            sessionCallback.get().didChangeMode(true);
        }
    }

    // 对方网络断开
    public void onDisConnect(String userId, EnumType.CallEndReason reason) {
        executor.execute(() -> {
            avEngine.disconnected(userId, reason);
        });
    }

    // 对方取消拨出
    public void onCancel(String userId) {
        Log.d(TAG, "onCancel userId = " + userId);
        shouldStopRing();
        release(EnumType.CallEndReason.RemoteHangup);
    }

    public void onReceiveOffer(String userId, String description) {
        executor.execute(() -> {
            avEngine.receiveOffer(userId, description);
        });
    }

    public void onReceiverAnswer(String userId, String sdp) {
        executor.execute(() -> {
            avEngine.receiveAnswer(userId, sdp);
        });
    }

    public void onRemoteIceCandidate(String userId, String id, int label, String candidate) {
        executor.execute(() -> {
            avEngine.receiveIceCandidate(userId, id, label, candidate);
        });
    }

    // 对方离开房间
    public void onLeave(String userId) {
        if (mRoomSize > 2) {
            // 返回到界面上
            if (sessionCallback != null && sessionCallback.get() != null) {
                sessionCallback.get().didUserLeave(userId);
            }
        }
        executor.execute(() -> avEngine.leaveRoom(userId));


    }

    // --------------------------------界面显示相关--------------------------------------------/

    public long getStartTime() {
        return startTime;
    }

    public View setupLocalVideo(boolean isOverlay) {
        return avEngine.setupLocalPreview(isOverlay);
    }

    public View setupRemoteVideo(String userId, boolean isOverlay, RendererCommon.RendererEvents rendererEvents) {
        return avEngine.setupRemoteVideo(userId, isOverlay, rendererEvents);
    }

    //------------------------------------各种参数----------------------------------------------/

    public void setIsAudioOnly(boolean _isAudioOnly) {
        this.mIsAudioOnly = _isAudioOnly;
    }

    public boolean isAudioOnly() {
        return mIsAudioOnly;
    }

    public void setTargetId(String targetIds) {
        this.mTargetId = targetIds;
    }

    public void setIsComing(boolean isComing) {
        this.mIsComing = isComing;
    }

    public boolean isComing() {
        return mIsComing;
    }

    public void setRoom(String _room) {
        this.mRoomId = _room;
    }

    public String getRoomId() {
        return mRoomId;
    }

    public EnumType.CallState getState() {
        return _callState;
    }

    public void setCallState(EnumType.CallState callState) {
        this._callState = callState;
    }

    public void setSessionCallback(CallSessionCallback sessionCallback) {
        this.sessionCallback = new WeakReference<>(sessionCallback);
    }

    //-----------------------------Engine回调-----------------------------------------

    @Override
    public void joinRoomSucc() {
        // 关闭响铃
        if (iSessionEvent != null) {
            iSessionEvent.shouldStopRing();
        }
        // 更换界面
        _callState = EnumType.CallState.Connected;
        //Log.d(TAG, "joinRoomSucc, sessionCallback.get() = " + sessionCallback.get());
        if (sessionCallback != null && sessionCallback.get() != null) {
            startTime = System.currentTimeMillis();
            sessionCallback.get().didChangeState(_callState);
        }
    }

    @Override
    public void exitRoom() {
        // 状态设置为Idle
        if (mRoomSize == 2) {
            handler.post(() -> {
                release(EnumType.CallEndReason.RemoteHangup);
            });
        }
    }

    @Override
    public void reject(int type) {
        shouldStopRing();
        Log.d(TAG, "reject type = " + type);
        switch (type) {
            case 0:
                release(EnumType.CallEndReason.Busy);
                break;
            case 1:
                release(EnumType.CallEndReason.RemoteHangup);
                break;
        }
    }

    @Override
    public void disconnected(EnumType.CallEndReason reason) {
        handler.post(() -> {
            shouldStopRing();
            release(reason);
        });
    }

    @Override
    public void onSendIceCandidate(String userId, IceCandidate candidate) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                Log.d("llx", "onSendIceCandidate");
                iSessionEvent.sendIceCandidate(userId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
            }
        });
    }

    @Override
    public void onSendOffer(String userId, SessionDescription description) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                Log.d("llx", "onSendOffer");
                iSessionEvent.sendOffer(userId, description.description);
            }
        });
    }

    @Override
    public void onSendAnswer(String userId, SessionDescription description) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                Log.d("llx", "onSendAnswer");
                iSessionEvent.sendAnswer(userId, description.description);
            }
        });
    }

    @Override
    public void onRemoteStream(String userId) {
        // 画面预览
        if (sessionCallback != null && sessionCallback.get() != null) {
           Log.d(TAG, "onRemoteStream sessionCallback.get() != null ");
            sessionCallback.get().didReceiveRemoteVideoTrack(userId);
        } else {
            Log.d(TAG, "onRemoteStream sessionCallback.get() == null ");
        }
    }

    @Override
    public void onDisconnected(String userId) {
        //断线了，需要关闭通话界面
        if (sessionCallback != null && sessionCallback.get() != null) {
           Log.d(TAG, "onDisconnected sessionCallback.get() != null ");
            sessionCallback.get().didDisconnected(userId);
        } else {
            Log.d(TAG, "onDisconnected sessionCallback.get() == null ");
        }
    }

    public interface CallSessionCallback {
        void didCallEndWithReason(EnumType.CallEndReason var1);

        void didChangeState(EnumType.CallState var1);

        void didChangeMode(boolean isAudioOnly);

        void didCreateLocalVideoTrack();

        void didReceiveRemoteVideoTrack(String userId);

        void didUserLeave(String userId);

        void didError(String error);

        void didDisconnected(String userId);
    }
}
