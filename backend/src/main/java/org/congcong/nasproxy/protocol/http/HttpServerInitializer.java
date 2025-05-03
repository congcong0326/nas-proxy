package org.congcong.nasproxy.protocol.http;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.core.proxy.AbstractChannelInitializer;


public class HttpServerInitializer extends AbstractChannelInitializer {


    public HttpServerInitializer(Inbound inbound) {
        super(inbound);
    }

    @Override
    protected void init(SocketChannel socketChannel) {
        socketChannel.pipeline().addLast(
                new HttpRequestDecoder(),
                // 需要聚合下，如果一次没有解析出完整的http请求，容易导致后续流程报错
                new HttpObjectAggregator(1048576),
                HttpServerHandler.getInstance()
        );
    }
}
