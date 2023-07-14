package com.webrtc.engine;

import com.webrtc.session.EnumType;
import java.util.List;

public interface IEngine {
    /**
     * 初始化
     */
    void init(EngineCallback callback);

    void joinRoom(List<String> userIds);

    void userIn(String userId);

    void disconnected(String userId, EnumType.CallEndReason reason);

    /**
     * receive Offer
     */
    void receiveOffer(String userId, String description);

    /**
     * receive Answer
     */
    void receiveAnswer(String userId, String sdp);

    /**
     * receive IceCandidate
     */
    void receiveIceCandidate(String userId, String id, int label, String candidate);

    void leaveRoom(String userId);

    /**
     * 开始远端推流
     */
    void startStream();

    /**
     * 停止远端推流
     */
    void stopStream();

    /**
     * 设置静音
     */
    boolean muteAudio(boolean enable);

    /**
     * 开启扬声器
     */
    boolean toggleSpeaker(boolean enable);

    /**
     * 切换外放和耳机
     */
    boolean toggleHeadset(boolean isHeadset);

    /**
     * 释放所有内容
     */
    void release();
}
