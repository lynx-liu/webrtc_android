package com.webrtc.engine;


import android.view.View;

import com.webrtc.session.EnumType;

import org.webrtc.RendererCommon;

import java.util.List;

/**
 * rtc基类
 */
public interface IEngine {

    /**
     * 初始化
     */
    void init(EngineCallback callback);

    /**
     * 加入房間
     */
    void joinRoom(List<String> userIds);

    boolean sendData(byte[] data);

    /**
     * 有人进入房间
     */
    void userIn(String userId);

    /**
     * 用户拒绝
     * @param userId userId
     * @param type type
     */
    void userReject(String userId,int type);

    /**
     * 用户网络断开
     * @param userId userId
     * @param reason
     */
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

    /**
     * 离开房间
     *
     * @param userId userId
     */
    void leaveRoom(String userId);

    /**
     * 开启本地预览
     */
    View setupLocalPreview(boolean isOverlay);

    /**
     * 关闭本地预览
     */
    void stopPreview();

    /**
     * 开始远端推流
     */
    void startStream();

    /**
     * 停止远端推流
     */
    void stopStream();

    /**
     * 开始远端预览
     */
    View setupRemoteVideo(String userId, boolean isO, RendererCommon.RendererEvents rendererEvents);

    /**
     * 关闭远端预览
     */
    void stopRemoteVideo();

    /**
     * 开关本地视频
     */
    void switchLocalVideoEnable();

    /**
     * 切换摄像头
     */
    void switchCamera();

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
