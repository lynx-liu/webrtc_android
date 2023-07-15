package com.webrtc.net;

public class Urls {
    public final static String IP = "39.109.123.162:5000";
    private final static String HOST = "http://" + IP + "/";
    public final static String WS = "ws://" + IP + "/ws";
    public static String getRoomList() {
        return HOST + "roomList";
    }
}
