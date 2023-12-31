package com.webrtc.engine;

import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.view.MotionEvent;

import com.webrtc.render.ProxyVideoSink;

import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceViewRenderer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class Peer implements SdpObserver, PeerConnection.Observer {
    private final static String TAG = "Peer";
    private final PeerConnection pc;
    private final String mUserId;
    private List<IceCandidate> queuedRemoteCandidates;
    private SessionDescription localSdp;
    private final PeerConnectionFactory mFactory;
    private final List<PeerConnection.IceServer> mIceLis;
    private final IPeerEvent mEvent;
    private boolean isOffer;
    private final DataChannel controlChannel;
    private ControlUtil controlUtil;
    private Point screenSize;

    public MediaStream _remoteStream;
    public SurfaceViewRenderer renderer;
    public ProxyVideoSink sink;

    public Peer(PeerConnectionFactory factory, List<PeerConnection.IceServer> list, String userId, IPeerEvent event) {
        mFactory = factory;
        mIceLis = list;
        mEvent = event;
        mUserId = userId;
        queuedRemoteCandidates = new ArrayList<>();
        this.pc = createPeerConnection();

        controlChannel = pc.createDataChannel("ControlChannel", new DataChannel.Init());
        Log.d("llx", "create Peer:" + mUserId);
    }

    public PeerConnection createPeerConnection() {
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(mIceLis);
        if (mFactory != null) {
            return mFactory.createPeerConnection(rtcConfig, this);
        } else {
            return null;
        }
    }

    public boolean sendData(byte[] data) {
        ByteBuffer byteBuffer = ByteBuffer.wrap(data);
        DataChannel.Buffer buffer = new DataChannel.Buffer(byteBuffer, true);
        return controlChannel.send(buffer);
    }

    public void setOffer(boolean isOffer) {
        this.isOffer = isOffer;
    }

    // 创建offer
    public void createOffer() {
        if (pc == null) return;
        pc.createOffer(this, offerOrAnswerConstraint());
    }

    // 创建answer
    public void createAnswer() {
        if (pc == null) return;
        pc.createAnswer(this, offerOrAnswerConstraint());
    }

    // 设置LocalDescription
    public void setLocalDescription(SessionDescription sdp) {
        if (pc == null) return;
        pc.setLocalDescription(this, sdp);
    }

    // 设置RemoteDescription
    public void setRemoteDescription(SessionDescription sdp) {
        if (pc == null) return;
        pc.setRemoteDescription(this, sdp);
    }

    //添加本地流
    public void addLocalStream(MediaStream stream) {
        if (pc == null) return;
        pc.addStream(stream);
    }

    // 添加RemoteIceCandidate
    public synchronized void addRemoteIceCandidate(final IceCandidate candidate) {
        if (pc != null) {
            if (queuedRemoteCandidates != null) {
                synchronized (Peer.class) {
                    if (queuedRemoteCandidates != null) {
                        queuedRemoteCandidates.add(candidate);
                    }
                }
            } else {
                pc.addIceCandidate(candidate);
            }
        }
    }

    public void createRender(EglBase mRootEglBase, Context context, boolean isOverlay, RendererCommon.RendererEvents rendererEvents) {
        controlUtil = new ControlUtil(context);
        screenSize = controlUtil.getScreenSize();
        renderer = new SurfaceViewRenderer(context);
        renderer.init(mRootEglBase.getEglBaseContext(), rendererEvents);
        renderer.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
        renderer.setMirror(false);
        renderer.setZOrderMediaOverlay(isOverlay);
        sink = new ProxyVideoSink();
        sink.setTarget(renderer);
        if (_remoteStream != null && _remoteStream.videoTracks.size() > 0) {
            _remoteStream.videoTracks.get(0).addSink(sink);
        }
    }

    // 关闭Peer
    public void close() {
        if (renderer != null) {
            renderer.release();
        }
        if (sink != null) {
            sink.setTarget(null);
        }
        if (pc != null) {
            try {
                pc.close();
            } catch (Exception e) {
                Log.e(TAG, "close: " + e);
            }
        }
    }

    //------------------------------Observer-------------------------------------
    @Override
    public void onSignalingChange(PeerConnection.SignalingState signalingState) {
        Log.i(TAG, "onSignalingChange: " + signalingState);
    }

    @Override
    public void onIceConnectionChange(PeerConnection.IceConnectionState newState) {
        Log.i(TAG, "onIceConnectionChange: " + newState);
        if (newState == PeerConnection.IceConnectionState.DISCONNECTED || newState == PeerConnection.IceConnectionState.FAILED) {
            mEvent.onDisconnected(mUserId);
        }
    }

    @Override
    public void onIceConnectionReceivingChange(boolean receiving) {
        Log.i(TAG, "onIceConnectionReceivingChange:" + receiving);
    }

    @Override
    public void onIceGatheringChange(PeerConnection.IceGatheringState newState) {
        // 检测本地ice状态
        Log.i(TAG, "onIceGatheringChange:" + newState.toString());
    }

    @Override
    public void onIceCandidate(IceCandidate candidate) {
        // 发送IceCandidate
        mEvent.onSendIceCandidate(mUserId, candidate);
        Log.d("llx",candidate.sdp);
    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {
        Log.i(TAG, "onIceCandidatesRemoved:");
    }

    @Override
    public void onAddStream(MediaStream stream) {
        Log.i(TAG, "onAddStream:");
        stream.audioTracks.get(0).setEnabled(true);
        _remoteStream = stream;
        if (mEvent != null) {
            mEvent.onRemoteStream(mUserId, stream);
        }
    }

    @Override
    public void onRemoveStream(MediaStream stream) {
        Log.i(TAG, "onRemoveStream:");
        if (mEvent != null) {
            mEvent.onRemoveStream(mUserId, stream);
        }
    }

    @Override
    public void onDataChannel(DataChannel dataChannel) {
        dataChannel.registerObserver(new DataChannel.Observer() {
            @Override
            public void onBufferedAmountChange(long l) {

            }

            @Override
            public void onStateChange() {
                DataChannel.State state = controlChannel.state();
            }

            @Override
            public void onMessage(DataChannel.Buffer buffer) {
                ByteBuffer data = buffer.data;
                byte[] byteArray = new byte[data.remaining()];
                data.get(byteArray);
                // 处理接收到的数据
                String receivedData = bytesToHexString(byteArray);
                Log.d("llx", "rev: " + receivedData);
                if(controlUtil==null) return;

                int action = byteArray[0];
                int x = ((((byteArray[1]&0xFF)<<24)|((byteArray[2]&0xFF)<<16)|((byteArray[3]&0xFF)<<8)|(byteArray[4]&0xFF))*screenSize.x)>>16;
                int y = ((((byteArray[5]&0xFF)<<24)|((byteArray[6]&0xFF)<<16)|((byteArray[7]&0xFF)<<8)|(byteArray[8]&0xFF))*screenSize.y)>>16;
                switch (action) {
                    case MotionEvent.ACTION_CANCEL:
                        action = MotionEvent.ACTION_UP;
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_UP:
                        controlUtil.injectTouch(action,0, new Point(x,y),1.0f,0);
                        break;
                }
            }
        });
    }

    private String bytesToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    public void onRenegotiationNeeded() {
        Log.i(TAG, "onRenegotiationNeeded:");
    }

    @Override
    public void onAddTrack(RtpReceiver receiver, MediaStream[] mediaStreams) {
        Log.i(TAG, "onAddTrack:" + mediaStreams.length);
    }

    //-------------SdpObserver--------------------
    @Override
    public void onCreateSuccess(SessionDescription origSdp) {
        Log.d(TAG, "sdp创建成功       " + origSdp.type);
        String sdpString = origSdp.description;
        final SessionDescription sdp = new SessionDescription(origSdp.type, sdpString);
        localSdp = sdp;
        setLocalDescription(sdp);
    }

    @Override
    public void onSetSuccess() {
        Log.d(TAG, "sdp连接成功   " + pc.signalingState().toString());
        if (pc == null) return;
        // 发送者
        if (isOffer) {
            if (pc.getRemoteDescription() == null) {
                Log.d(TAG, "Local SDP set succesfully");
                if (!isOffer) {
                    //接收者，发送Answer
                    mEvent.onSendAnswer(mUserId, localSdp);
                } else {
                    //发送者,发送自己的offer
                    mEvent.onSendOffer(mUserId, localSdp);
                }
            } else {
                Log.d(TAG, "Remote SDP set succesfully");
                drainCandidates();
            }
        } else {
            if (pc.getLocalDescription() != null) {
                Log.d(TAG, "Local SDP set succesfully");
                if (!isOffer) {
                    //接收者，发送Answer
                    mEvent.onSendAnswer(mUserId, localSdp);
                } else {
                    //发送者,发送自己的offer
                    mEvent.onSendOffer(mUserId, localSdp);
                }
                drainCandidates();
            } else {
                Log.d(TAG, "Remote SDP set succesfully");
            }
        }
    }

    @Override
    public void onCreateFailure(String error) {
        Log.i(TAG, " SdpObserver onCreateFailure:" + error);
    }

    @Override
    public void onSetFailure(String error) {
        Log.i(TAG, "SdpObserver onSetFailure:" + error);
    }

    private void drainCandidates() {
        synchronized (Peer.class) {
            if (queuedRemoteCandidates != null) {
                Log.d(TAG, "Add " + queuedRemoteCandidates.size() + " remote candidates");
                for (IceCandidate candidate : queuedRemoteCandidates) {
                    pc.addIceCandidate(candidate);
                }
                queuedRemoteCandidates = null;
            }
        }
    }

    private MediaConstraints offerOrAnswerConstraint() {
        MediaConstraints mediaConstraints = new MediaConstraints();
        ArrayList<MediaConstraints.KeyValuePair> keyValuePairs = new ArrayList<>();
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        keyValuePairs.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        mediaConstraints.mandatory.addAll(keyValuePairs);
        return mediaConstraints;
    }

    // ----------------------------回调-----------------------------------

    public interface IPeerEvent {

        void onSendIceCandidate(String userId, IceCandidate candidate);

        void onSendOffer(String userId, SessionDescription description);

        void onSendAnswer(String userId, SessionDescription description);

        void onRemoteStream(String userId, MediaStream stream);

        void onRemoveStream(String userId, MediaStream stream);

        void onDisconnected(String userId);
    }
}
