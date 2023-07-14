package com.android.core.voip;

import com.webrtc.session.ISessionEvent;
import java.util.List;

public class VoipEvent implements ISessionEvent {
    @Override
    public void createRoom(String room, int roomSize) {
        SocketManager.getInstance().createRoom(room, roomSize);
    }

    @Override
    public void sendInvite(String room, List<String> userIds) {
        SocketManager.getInstance().sendInvite(room, userIds);
    }

    @Override
    public void sendDisConnect(String room, String toId, boolean isCrashed) {
        SocketManager.getInstance().sendDisconnect(room, toId);
    }

    @Override
    public void sendCancel(String mRoomId, List<String> toIds) {
        SocketManager.getInstance().sendCancel(mRoomId, toIds);
    }

    @Override
    public void sendJoin(String room) {
        SocketManager.getInstance().sendJoin(room);
    }

    @Override
    public void sendLeave(String room, String userId) {
        SocketManager.getInstance().sendLeave(room, userId);
    }

    @Override
    public void sendOffer(String userId, String sdp) {
        SocketManager.getInstance().sendOffer(userId, sdp);
    }

    @Override
    public void sendAnswer(String userId, String sdp) {
        SocketManager.getInstance().sendAnswer(userId, sdp);

    }

    @Override
    public void sendIceCandidate(String userId, String id, int label, String candidate) {
        SocketManager.getInstance().sendIceCandidate(userId, id, label, candidate);
    }
}
