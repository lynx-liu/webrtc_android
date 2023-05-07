package com.vrviu.core.voip;

import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;


import com.vrviu.App;
import com.webrtc.session.CallSession;
import com.webrtc.session.EnumType.CallState;
import com.vrviu.core.util.RomUtil;
import com.vrviu.core.util.Utils;
import com.vrviu.webrtc.R;


/**
 * 语音通话控制界面
 */
public class FragmentAudio extends SingleCallFragment implements View.OnClickListener {
    private static final String TAG = "FragmentAudio";
    private ImageView muteImageView;
    private ImageView speakerImageView;
    private boolean micEnabled = false; // 静音
    private boolean isSpeakerOn = false; // 扬声器

    @Override
    int getLayout() {
        return R.layout.fragment_audio;
    }

    @Override
    public void initView(View view) {
        super.initView(view);
        muteImageView = view.findViewById(R.id.muteImageView);
        speakerImageView = view.findViewById(R.id.speakerImageView);
        minimizeImageView.setVisibility(View.GONE);
        outgoingHangupImageView.setOnClickListener(this);
        incomingHangupImageView.setOnClickListener(this);
        minimizeImageView.setOnClickListener(this);
        muteImageView.setOnClickListener(this);
        acceptImageView.setOnClickListener(this);
        speakerImageView.setOnClickListener(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M || RomUtil.isMiui() || RomUtil.isFlyme()) {
            lytParent.post(() -> {
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) minimizeImageView.getLayoutParams();
                params.topMargin = Utils.getStatusBarHeight(getContext());
                minimizeImageView.setLayoutParams(params);
            });
        }
    }

    @Override
    public void init() {
        super.init();
        CallSession currentSession = gEngineKit.getCurrentSession();
        currentState = currentSession.getState();
        // 如果已经接通
        if (currentState == CallState.Connected) {
            descTextView.setVisibility(View.GONE); // 提示语
            outgoingActionContainer.setVisibility(View.VISIBLE);
            durationTextView.setVisibility(View.VISIBLE);
            minimizeImageView.setVisibility(View.VISIBLE);
            startRefreshTime();
        } else {
            // 如果未接通
            if (isOutgoing) {
                descTextView.setText(R.string.av_waiting);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                incomingActionContainer.setVisibility(View.GONE);
            } else {
                descTextView.setText(R.string.av_audio_invite);
                outgoingActionContainer.setVisibility(View.GONE);
                incomingActionContainer.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void didChangeState(CallState state) {
        currentState = state;
        runOnUiThread(() -> {
            if (state == CallState.Connected) {
                incomingActionContainer.setVisibility(View.GONE);
                outgoingActionContainer.setVisibility(View.VISIBLE);
                minimizeImageView.setVisibility(View.VISIBLE);
                descTextView.setVisibility(View.GONE);

                startRefreshTime();
            } else {
                // do nothing now
            }
        });
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        // 接听
        if (id == R.id.acceptImageView) {
            CallSession session = gEngineKit.getCurrentSession();
            if (session != null)
                Log.d(TAG, "session = " + session + "; session.getState() = " + session.getState());
            if (session != null && session.getState() == CallState.Incoming) {
                session.joinHome(session.getRoomId());
            } else if (session != null) {
                session.sendRefuse();
            }
        }
        // 挂断电话
        if (id == R.id.incomingHangupImageView || id == R.id.outgoingHangupImageView) {
            App.getInstance().setOtherUserId("0");
            CallSession session = gEngineKit.getCurrentSession();
            if (session != null) {
                gEngineKit.endCall();
            }
            //            activity.finish();
            //再onEvent中结束，防止ChatActivity结束了，消息发送不了
        }
        // 静音
        if (id == R.id.muteImageView) {
            CallSession session = gEngineKit.getCurrentSession();
            if (session != null && session.getState() != CallState.Idle) {
                if (session.toggleMuteAudio(!micEnabled)) {
                    micEnabled = !micEnabled;
                }
                muteImageView.setSelected(micEnabled);
            }
        }
        // 扬声器
        if (id == R.id.speakerImageView) {
            CallSession session = gEngineKit.getCurrentSession();
            if (session != null && session.getState() != CallState.Idle) {
                if (session.toggleSpeaker(!isSpeakerOn)) {
                    isSpeakerOn = !isSpeakerOn;
                }
                speakerImageView.setSelected(isSpeakerOn);
            }
        }
        // 小窗
        if (id == R.id.minimizeImageView) {
            if (callSingleActivity != null) {
                callSingleActivity.showFloatingView();
            }
        }
    }
}