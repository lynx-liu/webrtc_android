package com.webrtc.net;

public class Urls {
    public final static String IP = "154.221.16.209:5000";
    private final static String HOST = "http://" + IP + "/";
    public final static String WS = "ws://" + IP + "/ws";
    public static String getRoomList() {
        return HOST + "roomList";
    }
}
