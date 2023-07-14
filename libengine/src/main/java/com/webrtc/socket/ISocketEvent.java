package com.webrtc.socket;

public interface ISocketEvent {
    void onOpen();
    void loginSuccess(String userId, String avatar);
    void onPeers(String myId, String userList, int roomSize);
    void onNewPeer(String myId);
    void onOffer(String userId, String sdp);
    void onAnswer(String userId, String sdp);
    void onIceCandidate(String userId, String id, int label, String candidate);
    void onLeave(String userId);
    void logout(String str);
    void onDisConnect(String userId);
    void reConnect();
}
