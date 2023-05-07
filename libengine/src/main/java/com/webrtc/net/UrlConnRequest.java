package com.webrtc.net;

import java.io.InputStream;
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

    /**
     * get请求
     *
     * @param url      url
     * @param params   params
     * @param callback callback
     */
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

    /**
     * post请求
     *
     * @param url      url
     * @param params   params
     * @param callback callback
     */
    public void post(String url, Map<String, Object> params, ICallback callback) {
        try {
            String postStr = null;
            if (params != null) {
                postStr = UrlConnUtils.builderUrlParams(params);
            }
            String result = UrlConnUtils.sendPost(url, postStr);
            callback.onSuccess(result);
        } catch (Exception e) {
            callback.onFailure(-1, e);
        }
    }

    /**
     * 设置双向证书
     *
     * @param certificate certificate
     * @param pwd         pwd
     */
    public void setCertificate(InputStream certificate, String pwd) {

    }
}
