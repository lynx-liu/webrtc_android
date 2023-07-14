package com.webrtc.engine;

import com.webrtc.session.EnumType;

import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public interface EngineCallback {
    void joinRoomSucc();

    void exitRoom();

    void disconnected(EnumType.CallEndReason reason);

    void onSendIceCandidate(String userId, IceCandidate candidate);

    void onSendOffer(String userId, SessionDescription description);

    void onSendAnswer(String userId, SessionDescription description);

    void onRemoteStream(String userId);

    void onDisconnected(String userId);
}
