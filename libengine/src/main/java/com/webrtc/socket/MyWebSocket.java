package com.webrtc.socket;

import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.X509TrustManager;

public class MyWebSocket extends WebSocketClient {
    private final static String TAG = "WebSocket";
    private final ISocketEvent socketEvent;
    private boolean connectFlag = false;

    public MyWebSocket(URI serverUri, ISocketEvent event) {
        super(serverUri);
        this.socketEvent = event;
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        Log.e("llx", "onClose:" + reason + "remote:" + remote);
        if (connectFlag) {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.socketEvent.reConnect();
        } else {
            this.socketEvent.logout("onClose");
        }
    }

    @Override
    public void onError(Exception ex) {
        Log.e("llx", "onError:" + ex.toString());
        this.socketEvent.logout("onError");
        connectFlag = false;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        Log.e("llx", "onOpen");
        this.socketEvent.onOpen();
        connectFlag = true;
    }

    @Override
    public void onMessage(String message) {
        Log.d(TAG, message);

        try {
            handleMessage(message);
        } catch (JSONException e) {
            Log.e("llx",e.toString());
        }
    }

    public void setConnectFlag(boolean flag) {
        connectFlag = flag;
    }

    // ---------------------------------------处理接收消息-------------------------------------

    private void handleMessage(String message) throws JSONException {
        JSONObject map = new JSONObject(message);
        String eventName = (String) map.get("eventName");
        if (eventName == null) return;
        // 登录成功
        if (eventName.equals("__login_success")) {
            handleLogin(map);
            return;
        }
        // 进入房间
        if (eventName.equals("__peers")) {
            handlePeers(map);
            return;
        }
        // 新人入房间
        if (eventName.equals("__new_peer")) {
            handleNewPeer(map);
            return;
        }
        // offer
        if (eventName.equals("__offer")) {
            handleOffer(map);
            return;
        }
        // answer
        if (eventName.equals("__answer")) {
            handleAnswer(map);
            return;
        }
        // ice-candidate
        if (eventName.equals("__ice_candidate")) {
            handleIceCandidate(map);
        }
        // 离开房间
        if (eventName.equals("__leave")) {
            handleLeave(map);
        }
        // 意外断开
        if (eventName.equals("__disconnect")) {
            handleDisConnect(map);
        }
    }

    private void handleDisConnect(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String fromId = (String) data.get("fromID");
            this.socketEvent.onDisConnect(fromId);
        }
    }

    private void handleLogin(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String userID = (String) data.get("userID");
            String avatar = (String) data.get("avatar");
            this.socketEvent.loginSuccess(userID, avatar);
        }
    }

    private void handleIceCandidate(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String userID = (String) data.get("fromID");
            String id = (String) data.get("id");
            int label = (int) data.get("label");
            String candidate = (String) data.get("candidate");
            this.socketEvent.onIceCandidate(userID, id, label, candidate);
        }
    }

    private void handleAnswer(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.socketEvent.onAnswer(userID, sdp);
        }
    }

    private void handleOffer(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String sdp = (String) data.get("sdp");
            String userID = (String) data.get("fromID");
            this.socketEvent.onOffer(userID, sdp);
        }
    }

    private void handlePeers(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String you = (String) data.get("you");
            String connections = (String) data.get("connections");
            int roomSize = (int) data.get("roomSize");
            this.socketEvent.onPeers(you, connections, roomSize);
        }
    }

    private void handleNewPeer(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String userID = (String) data.get("userID");
            this.socketEvent.onNewPeer(userID);
        }
    }

    private void handleLeave(JSONObject map) throws JSONException {
        JSONObject data = new JSONObject(map.getString("data"));
        if (data != null) {
            String fromID = (String) data.get("fromID");
            this.socketEvent.onLeave(fromID);
        }
    }

    /**
     * ------------------------------发送消息----------------------------------------
     */
    public void createRoom(String room, int roomSize, String myId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__create");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("roomSize", roomSize);
        childMap.put("userID", myId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 发送邀请
    public void sendInvite(String room, String myId, List<String> users) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__invite");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("audioOnly", true);
        childMap.put("inviteID", myId);

        String join = listToString(users);
        childMap.put("userList", join);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 取消邀请
    public void sendCancel(String mRoomId, String useId, List<String> users) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__cancel");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("inviteID", useId);
        childMap.put("room", mRoomId);

        String join = listToString(users);
        childMap.put("userList", join);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    public static String listToString(List<String> mList) {
        final String SEPARATOR = ",";
        StringBuilder sb = new StringBuilder();
        String convertedListStr;
        if (null != mList && mList.size() > 0) {
            for (String item : mList) {
                sb.append(item);
                sb.append(SEPARATOR);
            }
            convertedListStr = sb.toString();
            convertedListStr = convertedListStr.substring(0, convertedListStr.length() - SEPARATOR.length());
            return convertedListStr;
        } else return "";
    }

    //加入房间
    public void sendJoin(String room, String myId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__join");

        Map<String, String> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("userID", myId);


        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 离开房间
    public void sendLeave(String myId, String room, String userId) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__leave");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("room", room);
        childMap.put("fromID", myId);
        childMap.put("userID", userId);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        if (isOpen()) {
            send(jsonString);
        }
    }

    // send offer
    public void sendOffer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("sdp", sdp);
        childMap.put("userID", userId);
        childMap.put("fromID", myId);
        map.put("data", childMap);
        map.put("eventName", "__offer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // send answer
    public void sendAnswer(String myId, String userId, String sdp) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("sdp", sdp);
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        map.put("data", childMap);
        map.put("eventName", "__answer");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // send ice-candidate
    public void sendIceCandidate(String myId, String userId, String id, int label, String candidate) {
        Map<String, Object> map = new HashMap<>();
        map.put("eventName", "__ice_candidate");

        Map<String, Object> childMap = new HashMap<>();
        childMap.put("userID", userId);
        childMap.put("fromID", myId);
        childMap.put("id", id);
        childMap.put("label", label);
        childMap.put("candidate", candidate);

        map.put("data", childMap);
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        if (isOpen()) {
            send(jsonString);
        }
    }

    // 断开重连
    public void sendDisconnect(String room, String myId, String userId) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> childMap = new HashMap<>();
        childMap.put("fromID", myId);
        childMap.put("userID", userId);
        childMap.put("room", room);
        map.put("data", childMap);
        map.put("eventName", "__disconnect");
        JSONObject object = new JSONObject(map);
        final String jsonString = object.toString();
        Log.d(TAG, "send-->" + jsonString);
        send(jsonString);
    }

    // 忽略证书
    public static class TrustManagerTest implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
