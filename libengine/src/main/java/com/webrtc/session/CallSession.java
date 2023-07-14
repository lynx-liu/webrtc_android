package com.webrtc.session;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.webrtc.engine.AVEngine;
import com.webrtc.engine.EngineCallback;
import com.webrtc.engine.WebRTCEngine;

import org.webrtc.IceCandidate;
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

    public CallSession(Context context, String roomId, ISessionEvent iSessionEvent) {
        executor = Executors.newSingleThreadExecutor();
        this.mRoomId = roomId;

        this.iSessionEvent = iSessionEvent;
        avEngine = AVEngine.createEngine(new WebRTCEngine(context));
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

    // 设置静音
    public boolean toggleMuteAudio(boolean enable) {
        return avEngine.muteAudio(enable);
    }

    // 设置扬声器
    public boolean toggleSpeaker(boolean enable) {
        return avEngine.toggleSpeaker(enable);
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
                    iSessionEvent.sendInvite(mRoomId, inviteList);
                }
            } else {
                avEngine.joinRoom(mUserIDList);
            }
        }));
    }

    // 新成员进入
    public void newPeer(String userId) {
        handler.post(() -> executor.execute(() -> {
            // 其他人加入房间
            avEngine.userIn(userId);

            // 更换界面
            _callState = EnumType.CallState.Connected;

            if (sessionCallback != null && sessionCallback.get() != null) {
                startTime = System.currentTimeMillis();
                sessionCallback.get().didChangeState(_callState);
            }
        }));
    }

    // 对方网络断开
    public void onDisConnect(String userId, EnumType.CallEndReason reason) {
        executor.execute(() -> {
            avEngine.disconnected(userId, reason);
        });
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

    //------------------------------------各种参数----------------------------------------------/

    public void setIsComing(boolean isComing) {
        this.mIsComing = isComing;
    }

    public boolean isComing() {
        return mIsComing;
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
    public void disconnected(EnumType.CallEndReason reason) {
        handler.post(() -> {
            release(reason);
        });
    }

    @Override
    public void onSendIceCandidate(String userId, IceCandidate candidate) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.sendIceCandidate(userId, candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp);
            }
        });
    }

    @Override
    public void onSendOffer(String userId, SessionDescription description) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.sendOffer(userId, description.description);
            }
        });
    }

    @Override
    public void onSendAnswer(String userId, SessionDescription description) {
        executor.execute(() -> {
            if (iSessionEvent != null) {
                iSessionEvent.sendAnswer(userId, description.description);
            }
        });
    }

    @Override
    public void onRemoteStream(String userId) {

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

        void didUserLeave(String userId);

        void didDisconnected(String userId);
    }
}
