package com.android.core.voip;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;

import com.webrtc.engine.WebRTCEngine;
import com.webrtc.session.CallSession;
import com.webrtc.session.EnumType.CallState;
import com.android.core.util.RomUtil;
import com.android.core.util.Utils;
import com.android.webrtc.R;

import org.webrtc.RendererCommon;
import org.webrtc.SurfaceViewRenderer;

/**
 * 视频通话控制界面
 */
public class FragmentVideo extends SingleCallFragment implements View.OnClickListener {
    private static final String TAG = "FragmentVideo";
    private ImageView outgoingAudioOnlyImageView;
    private ImageView incomingAudioOnlyImageView;
    private ImageView connectedAudioOnlyImageView;
    private ImageView connectedHangupImageView;
    private ImageView switchCameraImageView;
    private FrameLayout fullscreenRenderer;
    private FrameLayout pipRenderer;
    private LinearLayout inviteeInfoContainer;
    private boolean isFromFloatingView = false;
    private int mVideoWidth = 1280;
    private int mVideoHeight = 720;
    private SurfaceViewRenderer localSurfaceView;
    private SurfaceViewRenderer remoteSurfaceView;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (callSingleActivity != null) {
            isFromFloatingView = callSingleActivity.isFromFloatingView();
        }
    }

    @Override
    int getLayout() {
        return R.layout.fragment_video;
    }

    @Override
    public void initView(View view) {
        super.initView(view);
        fullscreenRenderer = view.findViewById(R.id.fullscreen_video_view);
        pipRenderer = view.findViewById(R.id.pip_video_view);
        inviteeInfoContainer = view.findViewById(R.id.inviteeInfoContainer);
        outgoingAudioOnlyImageView = view.findViewById(R.id.outgoingAudioOnlyImageView);
        incomingAudioOnlyImageView = view.findViewById(R.id.incomingAudioOnlyImageView);
        connectedAudioOnlyImageView = view.findViewById(R.id.connectedAudioOnlyImageView);
        connectedHangupImageView = view.findViewById(R.id.connectedHangupImageView);
        switchCameraImageView = view.findViewById(R.id.switchCameraImageView);
        outgoingHangupImageView.setOnClickListener(this);
        incomingHangupImageView.setOnClickListener(this);
        minimizeImageView.setOnClickListener(this);
        connectedHangupImageView.setOnClickListener(this);
        acceptImageView.setOnClickListener(this);
        switchCameraImageView.setOnClickListener(this);
        pipRenderer.setOnClickListener(this);
        outgoingAudioOnlyImageView.setOnClickListener(this);
        incomingAudioOnlyImageView.setOnClickListener(this);
        connectedAudioOnlyImageView.setOnClickListener(this);

        int statusBarHeight = Utils.getStatusBarHeight(getContext());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || RomUtil.isMiui() || RomUtil.isFlyme()) {
            lytParent.post(() -> {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) inviteeInfoContainer.getLayoutParams();
                params.topMargin = (int) (statusBarHeight * 1.2);
                inviteeInfoContainer.setLayoutParams(params);
                RelativeLayout.LayoutParams params1 = (RelativeLayout.LayoutParams) minimizeImageView.getLayoutParams();
                params1.topMargin = statusBarHeight;
                minimizeImageView.setLayoutParams(params1);
            });

            pipRenderer.post(() -> {
                FrameLayout.LayoutParams params2 = (FrameLayout.LayoutParams) pipRenderer.getLayoutParams();
                params2.topMargin = (int) (statusBarHeight * 1.2);
                pipRenderer.setLayoutParams(params2);
            });
        }

        if(!WebRTCEngine.isScreencaptureEnabled()) {
            fullscreenRenderer.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    Log.d("llx", event.toString());
                    int action = event.getAction();
                    int x = (int) (event.getX() * 65536 / fullscreenRenderer.getWidth());
                    int y = (int) (event.getY() * 65536 / fullscreenRenderer.getHeight());
                    gEngineKit.sendData(new byte[]{(byte) action, (byte) ((x >> 24) & 0xFF), (byte) ((x >> 16) & 0xFF), (byte) ((x >> 8) & 0xFF), (byte) (x & 0xFF), (byte) ((y >> 24) & 0xFF), (byte) ((y >> 16) & 0xFF), (byte) ((y >> 8) & 0xFF), (byte) (y & 0xFF)});
                    return true;
                }
            });
        }

        pipRenderer.setVisibility(View.GONE);

//        if(isOutgoing){ //测试崩溃对方是否会停止
//            lytParent.postDelayed(() -> {
//                int i = 1 / 0;
//            }, 10000);
//        }

    }

    @Override
    public void init() {
        super.init();
        CallSession session = gEngineKit.getCurrentSession();
        if (session != null) {
            currentState = session.getState();
        }
        if (session == null || CallState.Idle == session.getState()) {
            if (callSingleActivity != null) {
                callSingleActivity.finish();
            }
        } else if (CallState.Connected == session.getState()) {
            incomingActionContainer.setVisibility(View.GONE);
            outgoingActionContainer.setVisibility(View.GONE);
            connectedActionContainer.setVisibility(View.VISIBLE);
            inviteeInfoContainer.setVisibility(View.GONE);
            minimizeImageView.setVisibility(View.VISIBLE);
            startRefreshTime();
        } else {
            if (isOutgoing) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_waiting);
            } else {
                incomingActionContainer.setVisibility(View.VISIBLE);
                outgoingActionContainer.setVisibility(View.GONE);
                connectedActionContainer.setVisibility(View.GONE);
                descTextView.setText(R.string.av_video_invite);
                if (currentState == CallState.Incoming && !WebRTCEngine.isScreencaptureEnabled()) {
                    View surfaceView = gEngineKit.getCurrentSession().setupLocalVideo(false);
                    Log.d(TAG, "init surfaceView != null is " + (surfaceView != null) + "; isOutgoing = " + isOutgoing + "; currentState = " + currentState);
                    if (surfaceView != null) {
                        localSurfaceView = (SurfaceViewRenderer) surfaceView;
                        localSurfaceView.setZOrderMediaOverlay(false);
                        fullscreenRenderer.addView(localSurfaceView);
                    }
                }
            }
        }
        if (isFromFloatingView) {
            didCreateLocalVideoTrack();
            if (session != null) {
                didReceiveRemoteVideoTrack(session.mTargetId);
                fullscreenRenderer.post(new Runnable() {
                    @Override
                    public void run() {
                        int frameLayoutWidth = fullscreenRenderer.getWidth(); // 获取 FrameLayout 的宽度
                        int frameLayoutHeight = frameLayoutWidth * mVideoWidth / mVideoHeight; // 计算 FrameLayout 的高度
                        fullscreenRenderer.setLayoutParams(new FrameLayout.LayoutParams(frameLayoutWidth, frameLayoutHeight));
                    }
                });
            }
        }
    }

    @Override
    public void didChangeState(CallState state) {
        currentState = state;
        Log.d(TAG, "didChangeState, state = " + state);
        runOnUiThread(() -> {
            if (state == CallState.Connected) {

                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.GONE);
                connectedActionContainer.setVisibility(View.VISIBLE);
                inviteeInfoContainer.setVisibility(View.GONE);
                descTextView.setVisibility(View.GONE);
                minimizeImageView.setVisibility(View.VISIBLE);
                // 开启计时器
                startRefreshTime();
            } else {
                // do nothing now
            }
        });
    }

    @Override
    public void didChangeMode(Boolean isAudio) {
        runOnUiThread(() -> callSingleActivity.switchAudio());
    }

    @Override
    public void didCreateLocalVideoTrack() {
        if (localSurfaceView == null) {
            View surfaceView = gEngineKit.getCurrentSession().setupLocalVideo(true);
            if (surfaceView != null) {
                localSurfaceView = (SurfaceViewRenderer) surfaceView;
            } else {
                if (callSingleActivity != null) callSingleActivity.finish();
                return;
            }
        } else {
            localSurfaceView.setZOrderMediaOverlay(true);
        }
        Log.d(TAG,
                "didCreateLocalVideoTrack localSurfaceView != null is " + (localSurfaceView != null) + "; remoteSurfaceView == null = " + (remoteSurfaceView == null)
        );

        if (localSurfaceView.getParent() != null) {
            ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
        }
        if (isOutgoing && remoteSurfaceView == null  && !WebRTCEngine.isScreencaptureEnabled()) {
            if (fullscreenRenderer != null && fullscreenRenderer.getChildCount() != 0)
                fullscreenRenderer.removeAllViews();
            fullscreenRenderer.addView(localSurfaceView);
        } else if(pipRenderer.getVisibility()==View.VISIBLE){
            if (pipRenderer.getChildCount() != 0) pipRenderer.removeAllViews();
            pipRenderer.addView(localSurfaceView);
        }
    }

    @Override
    public void didReceiveRemoteVideoTrack(String userId) {
        if (localSurfaceView != null && pipRenderer.getVisibility()==View.VISIBLE) {
            localSurfaceView.setZOrderMediaOverlay(true);
            if (isOutgoing) {
                if (localSurfaceView.getParent() != null) {
                    ((ViewGroup) localSurfaceView.getParent()).removeView(localSurfaceView);
                }
                pipRenderer.addView(localSurfaceView);
            }
        }

        View surfaceView = gEngineKit.getCurrentSession().setupRemoteVideo(userId, false, new RendererCommon.RendererEvents() {
            @Override
            public void onFirstFrameRendered() {
                Log.d(TAG, "createRender onFirstFrameRendered");
            }

            @Override
            public void onFrameResolutionChanged(int videoWidth, int videoHeight, int rotation) {
                Log.d(TAG, "videoWidth:"+videoWidth+", videoHeight:"+videoHeight+", rotation:"+rotation);
                mVideoWidth = videoWidth;
                mVideoHeight = videoHeight;

                runOnUiThread(() -> {
                    int frameLayoutWidth = fullscreenRenderer.getWidth(); // 获取 FrameLayout 的宽度
                    int frameLayoutHeight = frameLayoutWidth * videoWidth / videoHeight; // 计算 FrameLayout 的高度
                    fullscreenRenderer.setLayoutParams(new FrameLayout.LayoutParams(frameLayoutWidth, frameLayoutHeight));
                });
            }
        });

        if (surfaceView != null) {
            fullscreenRenderer.setVisibility(View.VISIBLE);
            remoteSurfaceView = (SurfaceViewRenderer) surfaceView;
            fullscreenRenderer.removeAllViews();
            if (remoteSurfaceView.getParent() != null) {
                ((ViewGroup) remoteSurfaceView.getParent()).removeView(remoteSurfaceView);
            }
            fullscreenRenderer.addView(remoteSurfaceView);
        }
    }

    @Override
    public void didUserLeave(String userId) {

    }

    @Override
    public void didError(String error) {

    }


    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 接听
        CallSession session = gEngineKit.getCurrentSession();
        if (id == R.id.acceptImageView) {
            if (session != null && session.getState() == CallState.Incoming) {
                session.joinHome(session.getRoomId());
            } else if (session != null) {
                if (callSingleActivity != null) {
                    session.sendRefuse();
                    callSingleActivity.finish();
                }
            }
        }
        // 挂断电话
        if (id == R.id.incomingHangupImageView || id == R.id.outgoingHangupImageView || id == R.id.connectedHangupImageView) {
            if (session != null) {
                Log.d(TAG, "endCall");
                gEngineKit.endCall();
            }
            if (callSingleActivity != null) callSingleActivity.finish();
        }

        // 切换摄像头
        if (id == R.id.switchCameraImageView) {
            session.switchCamera();
        }
        if (id == R.id.pip_video_view && remoteSurfaceView!=null && pipRenderer.getVisibility()==View.VISIBLE) {
            boolean isFullScreenRemote = fullscreenRenderer.getChildAt(0) == remoteSurfaceView;
            fullscreenRenderer.removeAllViews();
            pipRenderer.removeAllViews();
            if (isFullScreenRemote) {
                remoteSurfaceView.setZOrderMediaOverlay(true);
                pipRenderer.addView(remoteSurfaceView);
                localSurfaceView.setZOrderMediaOverlay(false);
                fullscreenRenderer.addView(localSurfaceView);
            } else {
                localSurfaceView.setZOrderMediaOverlay(true);
                pipRenderer.addView(localSurfaceView);
                remoteSurfaceView.setZOrderMediaOverlay(false);
                fullscreenRenderer.addView(remoteSurfaceView);
            }
        }

        // 切换到语音拨打
        if (id == R.id.outgoingAudioOnlyImageView || id == R.id.incomingAudioOnlyImageView || id == R.id.connectedAudioOnlyImageView) {
            if(WebRTCEngine.isScreencaptureEnabled()) {//投屏端切换语音时,对方也切换语音
                if (session != null) {
                    if (callSingleActivity != null) callSingleActivity.isAudioOnly = true;
                    session.switchToAudio();
                }
            } else {//摄像头端切换语音时, 只关闭自己的摄像头和推流
                gEngineKit.getCurrentSession().switchLocalVideoEnable();
            }
        }

        // 小窗
        if (id == R.id.minimizeImageView) {
            if (callSingleActivity != null) callSingleActivity.showFloatingView();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        fullscreenRenderer.removeAllViews();
        pipRenderer.removeAllViews();
    }
}