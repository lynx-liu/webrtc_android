package com.webrtc.net;

public class Urls {

    //public final static String IP = "10.86.21.71:5000";
    public final static String IP = "42.192.40.58:5000";

    private final static String HOST = "http://" + IP + "/";

    // 信令地址
    public final static String WS = "ws://" + IP + "/ws";

    // 获取用户列表
    public static String getUserList() {
        return HOST + "userList";
    }

    // 获取房间列表
    public static String getRoomList() {
        return HOST + "roomList";
    }
}
