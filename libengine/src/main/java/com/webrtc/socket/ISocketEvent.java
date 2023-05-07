package com.webrtc.socket;

public interface ISocketEvent {
    void onOpen();
    void loginSuccess(String userId, String avatar);
    void onInvite(String room, boolean audioOnly, String inviteId, String userList);
    void onCancel(String inviteId);
    void onRing(String userId);
    void onPeers(String myId, String userList, int roomSize);
    void onNewPeer(String myId);
    void onReject(String userId, int type);
    void onOffer(String userId, String sdp);
    void onAnswer(String userId, String sdp);
    void onIceCandidate(String userId, String id, int label, String candidate);
    void onLeave(String userId);
    void logout(String str);
    void onTransAudio(String userId);
    void onDisConnect(String userId);
    void reConnect();
}
