package com.webrtc.engine;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.util.Log;

import com.webrtc.session.EnumType;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.SessionDescription;
import org.webrtc.audio.AudioDeviceModule;
import org.webrtc.audio.JavaAudioDeviceModule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WebRTCEngine implements IEngine, Peer.IPeerEvent {
    private static final String TAG = "WebRTCEngine";
    private PeerConnectionFactory _factory;
    private MediaStream _localStream;
    private AudioSource audioSource;
    private AudioTrack _localAudioTrack;
    private static final String AUDIO_TRACK_ID = "StreamA0";

    // 对话实例列表
    private ConcurrentHashMap<String, Peer> peers = new ConcurrentHashMap<>();
    // 服务器实例列表
    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();

    private EngineCallback mCallback;

    private Context mContext;
    private AudioManager audioManager;
    private boolean isSpeakerOn = true;

    public WebRTCEngine(Context mContext) {
        this.mContext = mContext;
        audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        // 初始化ice地址
        initIceServer();
    }


    // -----------------------------------对外方法------------------------------------------
    @Override
    public void init(EngineCallback callback) {
        mCallback = callback;

        if (_factory == null) {
            _factory = createConnectionFactory();
        }
        if (_localStream == null) {
            createLocalStream();
        }
    }

    @Override
    public void joinRoom(List<String> userIds) {
        for (String id : userIds) {
            // create Peer
            Peer peer = new Peer(_factory, iceServers, id, this);
            peer.setOffer(false);
            // add localStream
            peer.addLocalStream(_localStream);
            // 添加列表
            peers.put(id, peer);
        }
        if (mCallback != null) {
            mCallback.joinRoomSucc();
        }

        if (isHeadphonesPlugged()) {
            toggleHeadset(true);
        } else {
            toggleSpeaker(false);
        }
    }

    @Override
    public void userIn(String userId) {
        // create Peer
        Peer peer = new Peer(_factory, iceServers, userId, this);
        peer.setOffer(true);
        // add localStream
        peer.addLocalStream(_localStream);
        // 添加列表
        peers.put(userId, peer);
        // createOffer
        peer.createOffer();
    }

    @Override
    public void disconnected(String userId, EnumType.CallEndReason reason) {
        if (mCallback != null) {
            mCallback.disconnected(reason);
        }
    }

    @Override
    public void receiveOffer(String userId, String description) {
        Peer peer = peers.get(userId);
        if (peer != null) {
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, description);
            peer.setOffer(false);
            peer.setRemoteDescription(sdp);
            peer.createAnswer();
        }
    }

    @Override
    public void receiveAnswer(String userId, String sdp) {
        Peer peer = peers.get(userId);
        if (peer != null) {
            SessionDescription sessionDescription = new SessionDescription(SessionDescription.Type.ANSWER, sdp);
            peer.setRemoteDescription(sessionDescription);
        }
    }

    @Override
    public void receiveIceCandidate(String userId, String id, int label, String candidate) {
        Peer peer = peers.get(userId);
        if (peer != null) {
            IceCandidate iceCandidate = new IceCandidate(id, label, candidate);
            peer.addRemoteIceCandidate(iceCandidate);
        }
    }

    @Override
    public void leaveRoom(String userId) {
        Peer peer = peers.get(userId);
        if (peer != null) {
            peer.close();
            peers.remove(userId);
        }
       Log.d(TAG, "leaveRoom peers.size() = " + peers.size() + "; mCallback = " + mCallback);
        if (peers.size() <= 1) {
            if (mCallback != null) {
                mCallback.exitRoom();
            }
            if (peers.size() == 1) {
                for (Map.Entry<String, Peer> set : peers.entrySet()) {
                    set.getValue().close();
                }
                peers.clear();
            }
        }
    }

    @Override
    public void startStream() {

    }

    @Override
    public void stopStream() {

    }

    @Override
    public boolean muteAudio(boolean enable) {
        if (_localAudioTrack != null) {
            _localAudioTrack.setEnabled(!enable);
            return true;
        }
        return false;
    }

    @Override
    public boolean toggleSpeaker(boolean enable) {
        if (audioManager != null) {
            isSpeakerOn = enable;
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            if (enable) {
                audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.FX_KEY_CLICK);
                audioManager.setSpeakerphoneOn(true);
            } else {
                //5.0以上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //设置mode
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                } else {
                    //设置mode
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
                //设置音量，解决有些机型切换后没声音或者声音突然变大的问题
                audioManager.setStreamVolume(
                        AudioManager.STREAM_VOICE_CALL,
                        audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL),
                        AudioManager.FX_KEY_CLICK
                );
                audioManager.setSpeakerphoneOn(false);
            }
            return true;
        }
        return false;

    }

    @Override
    public boolean toggleHeadset(boolean isHeadset) {
        if (audioManager != null) {
            if (isHeadset) {
                //5.0以上
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    //设置mode
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                } else {
                    //设置mode
                    audioManager.setMode(AudioManager.MODE_IN_CALL);
                }
                audioManager.setSpeakerphoneOn(false);
            } else {
                toggleSpeaker(isSpeakerOn);
            }
        }
        return false;
    }

    private boolean isHeadphonesPlugged() {
        if (audioManager == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioDeviceInfo[] audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS);
            for (AudioDeviceInfo deviceInfo : audioDevices) {
                if (deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                        || deviceInfo.getType() == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                    return true;
                }
            }
            return false;
        } else {
            return audioManager.isWiredHeadsetOn();
        }
    }

    @Override
    public void release() {
        if (audioManager != null) {
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }
        // 清空peer
        if (peers != null) {
            for (Peer peer : peers.values()) {
                peer.close();
            }
            peers.clear();
        }

        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        if (_localStream != null) {
            _localStream.dispose();
            _localStream = null;
        }

        if (_factory != null) {
            _factory.dispose();
            _factory = null;
        }
    }

    // -----------------------------其他方法--------------------------------

    private void initIceServer() {
        // 初始化一些stun和turn的地址
        PeerConnection.IceServer iceServer0 = PeerConnection.IceServer.builder("stun:stun.l.google.com:19302")
                .createIceServer();
        iceServers.add(iceServer0);

        PeerConnection.IceServer iceServer1 = PeerConnection.IceServer.builder("stun:154.221.16.209:3478?transport=udp")
                .createIceServer();
        PeerConnection.IceServer iceServer2 = PeerConnection.IceServer.builder("turn:154.221.16.209:3478?transport=udp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();
        PeerConnection.IceServer iceServer3 = PeerConnection.IceServer.builder("turn:154.221.16.209:3478?transport=tcp")
                .setUsername("test")
                .setPassword("123456")
                .createIceServer();

        iceServers.add(iceServer1);
        iceServers.add(iceServer2);
        iceServers.add(iceServer3);
    }

    public PeerConnectionFactory createConnectionFactory() {
        // 1. 初始化的方法，必须在开始之前调用
        PeerConnectionFactory.initialize(PeerConnectionFactory
                .InitializationOptions
                .builder(mContext)
                .createInitializationOptions());

        // 构造Factory
        AudioDeviceModule audioDeviceModule = JavaAudioDeviceModule.builder(mContext).createAudioDeviceModule();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        return PeerConnectionFactory.builder()
                .setOptions(options)
                .setAudioDeviceModule(audioDeviceModule)
                .createPeerConnectionFactory();
    }

    /**
     * 创建本地流
     */
    public void createLocalStream() {
        _localStream = _factory.createLocalMediaStream("Stream");
        // 音频
        audioSource = _factory.createAudioSource(createAudioConstraints());
        _localAudioTrack = _factory.createAudioTrack(AUDIO_TRACK_ID, audioSource);
        _localStream.addTrack(_localAudioTrack);
    }

    //**************************************各种约束******************************************/
    private static final String AUDIO_ECHO_CANCELLATION_CONSTRAINT = "googEchoCancellation";
    private static final String AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT = "googAutoGainControl";
    private static final String AUDIO_HIGH_PASS_FILTER_CONSTRAINT = "googHighpassFilter";
    private static final String AUDIO_NOISE_SUPPRESSION_CONSTRAINT = "googNoiseSuppression";

    // 配置音频参数
    private MediaConstraints createAudioConstraints() {
        MediaConstraints audioConstraints = new MediaConstraints();
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_ECHO_CANCELLATION_CONSTRAINT, "true"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_AUTO_GAIN_CONTROL_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_HIGH_PASS_FILTER_CONSTRAINT, "false"));
        audioConstraints.mandatory.add(
                new MediaConstraints.KeyValuePair(AUDIO_NOISE_SUPPRESSION_CONSTRAINT, "true"));
        return audioConstraints;
    }

    //------------------------------------回调---------------------------------------------
    @Override
    public void onSendIceCandidate(String userId, IceCandidate candidate) {
        if (mCallback != null) {
            mCallback.onSendIceCandidate(userId, candidate);
        }

    }

    @Override
    public void onSendOffer(String userId, SessionDescription description) {
        if (mCallback != null) {
            mCallback.onSendOffer(userId, description);
        }
    }

    @Override
    public void onSendAnswer(String userId, SessionDescription description) {
        if (mCallback != null) {
            mCallback.onSendAnswer(userId, description);
        }
    }

    @Override
    public void onRemoteStream(String userId, MediaStream stream) {
        if (mCallback != null) {
            mCallback.onRemoteStream(userId);
        }
    }

    @Override
    public void onRemoveStream(String userId, MediaStream stream) {
        leaveRoom(userId);
    }

    @Override
    public void onDisconnected(String userId) {
        if (mCallback != null) {
           Log.d(TAG, "onDisconnected mCallback != null");
            mCallback.onDisconnected(userId);
        } else {
           Log.d(TAG, "onDisconnected mCallback == null");
        }
    }
}
