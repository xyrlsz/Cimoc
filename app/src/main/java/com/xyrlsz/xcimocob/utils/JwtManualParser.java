package com.xyrlsz.xcimocob.utils;

import org.json.JSONException;
import org.json.JSONObject;

public  class JwtManualParser {
    private String jwt;
    public JwtManualParser(String jwt){
        this.jwt = jwt;
    }

    public String getJwt() {
        return jwt;
    }
    private String parserPayload(){
        // 分割JWT的三部分
        String[] parts = jwt.split("\\.");
        if (parts.length != 3) {
            System.out.println("无效的JWT格式");
            return "";
        }
        // 解码Payload
        String payloadJson = new String(Base64Utils.decodeUrlSafe(parts[1]));
        System.out.println("Payload: " + payloadJson);
        return payloadJson;
    }
    public JSONObject getPayload() throws JSONException {
        return new JSONObject(parserPayload());
    }
}
