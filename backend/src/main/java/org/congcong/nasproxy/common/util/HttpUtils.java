package org.congcong.nasproxy.common.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.*;
import org.congcong.nasproxy.common.entity.Pair;
import org.congcong.nasproxy.common.exception.HostAbsentException;

import java.net.URI;
import java.net.URISyntaxException;

public class HttpUtils {

    public static Pair<String, Integer> getHostIpAndPort(HttpRequest httpRequest) throws HostAbsentException {
        String host = httpRequest.headers().get("Host");
        if (host != null && !host.isEmpty()) {
            String[] parts = host.split(":");
            String hostname = parts[0]; // 主机名
            int port = (parts.length > 1) ? Integer.parseInt(parts[1]) : 80; // 默认端口为80
            return new Pair<>(hostname, port);
        }
        throw new HostAbsentException();
    }

    public static Pair<String, Integer> getUriAndPort(HttpRequest httpRequest) throws URISyntaxException {
        String uri = httpRequest.uri();
        URI targetUri = new URI(uri);
        String targetHost = targetUri.getHost();
        if (targetHost == null) {
            throw new RuntimeException("缺少host");
        }
        int targetPort = targetUri.getPort() == -1 ? 80 : targetUri.getPort();
        return new Pair<>(targetHost, targetPort);
    }

    public static boolean isConnectMethod(HttpRequest httpRequest) {
        return httpRequest.method() == HttpMethod.CONNECT;
    }

    public static boolean isKeepalive(HttpRequest httpRequest) {
        boolean isPersistent;
        if (httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_1)) {
            // HTTP/1.1 默认是长连接，除非有 Connection: close
            isPersistent = !httpRequest.headers().contains("Connection", "close", true);
        } else if (httpRequest.protocolVersion().equals(HttpVersion.HTTP_1_0)) {
            // HTTP/1.0 需要 Connection: keep-alive 才是长连接
            isPersistent = httpRequest.headers().contains("Connection", "keep-alive", true);
        } else {
            // 对于其他协议版本，默认处理为非持久连接
            isPersistent = false;
        }
        return isPersistent;
    }

    public static FullHttpResponse BAD_GATEWAY(String hostname) {
        // 创建一个502 Bad Gateway的响应
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_GATEWAY);

        // 设置响应头
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // 如果需要可以添加响应体
        String responseBody = "502 Bad Gateway: Unable to connect to the target server: " + hostname;
        response.content().writeBytes(Unpooled.copiedBuffer(responseBody, java.nio.charset.StandardCharsets.UTF_8));
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes()); // 更新内容长度

        return response;
    }

    public static ByteBuf BAD_GATEWAY_BUF() {
        String badGatewayResponse = "HTTP/1.1 502 Bad Gateway\r\n" +
                "Content-Type: text/plain\r\n" +
                "Content-Length: %d\r\n" +
                "\r\n" +
                "502 Bad Gateway";

        // 计算正文长度
        String responseBody = "502 Bad Gateway";
        int contentLength = responseBody.length();
        String response = String.format(badGatewayResponse, contentLength);

        // 创建 ByteBuf 并发送响应
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(response.getBytes());
        return buf;
    }

    public static ByteBuf CONNECT_ESTABLISHED_BUF() {
        String connect = "HTTP/1.1 200 Connection Established\r\n" +
                "Proxy-agent: https://github.com/cong/cong\r\n" +
                "\r\n";
        ByteBuf buf = PooledByteBufAllocator.DEFAULT.buffer();
        buf.writeBytes(connect.getBytes());
        return buf;
    }
}
