package com.xyrlsz.xcimoc.test;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class WebSocketLogger {
    private WebSocket webSocket;

    public void connect() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("ws://10.0.2.2:8081")
                .build();

        webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket ws, okhttp3.Response response) {
                ws.send("测试启动");
            }

            @Override
            public void onMessage(WebSocket ws, String text) {
                // 可选：接收服务端消息
            }

            @Override
            public void onClosed(WebSocket ws, int code, String reason) {
                // 连接关闭
            }

            @Override
            public void onFailure(WebSocket ws, Throwable t, okhttp3.Response response) {
                // 连接失败
            }
        });
    }

    public void send(String msg) {
        if (webSocket != null) {
            webSocket.send(msg);
        }
    }

    public void close() {
        if (webSocket != null) {
            webSocket.close(1000, "测试完成");
        }
    }
}
