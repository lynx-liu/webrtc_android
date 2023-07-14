package com.webrtc.net;

import java.util.Map;

public class UrlConnRequest {
    private static UrlConnRequest urlConnRequest = null;
    private UrlConnRequest() {
    }

    public static UrlConnRequest getInstance() {
        if(null==urlConnRequest) {
            urlConnRequest = new UrlConnRequest();
        }
        return urlConnRequest;
    }

    public void get(String url, Map<String, Object> params, ICallback callback) {
        try {
            String param = null;
            if (params != null) {
                param = UrlConnUtils.builderUrlParams(params);

            }
            String s = UrlConnUtils.sendGet(url, param);
            callback.onSuccess(s);

        } catch (Exception e) {
            callback.onFailure(-1, e);
        }
    }
}
