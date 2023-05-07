package com.webrtc.session;

import java.util.List;

public interface ISessionEvent {
    void createRoom(String room, int roomSize);
    void sendInvite(String room, List<String> userIds, boolean audioOnly);// 发送单人邀请
    void sendRefuse(String room, String inviteId, int refuseType);
    void sendTransAudio(String toId);
    void sendDisConnect(String room, String toId, boolean isCrashed);
    void sendCancel(String mRoomId, List<String> toId);
    void sendJoin(String room);
    void sendRingBack(String targetId, String room);
    void sendLeave(String room, String userId);
    void sendOffer(String userId, String sdp);
    void sendAnswer(String userId, String sdp);
    void sendIceCandidate(String userId, String id, int label, String candidate);
    void onRemoteRing();
    void shouldStartRing(boolean isComing);
    void shouldStopRing();
}
