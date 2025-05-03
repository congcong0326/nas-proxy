package org.congcong.nasproxy.protocol.http;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpRequestEncoder;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.HttpMessage;
import org.congcong.nasproxy.common.util.HttpUtils;
import org.congcong.nasproxy.common.util.SocketUtils;
import org.congcong.nasproxy.core.proxy.RelayHandler;

@Slf4j
@ChannelHandler.Sharable
public class HttpProxyConnectorHandler extends SimpleChannelInboundHandler<HttpMessage> {


    public static final HttpProxyConnectorHandler INSTANCE = new HttpProxyConnectorHandler();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpMessage httpMessage) throws Exception {
        // do connect remote
        Channel inboundChannel = ctx.channel();
        Bootstrap b = new Bootstrap();
        // 直接使用处理client的线程组去连接目标服务器
        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, httpMessage.isKeepalive())
                .handler(new HttpRequestEncoder());
        b.connect(httpMessage.getHost(), httpMessage.getPort())
                .addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        if (!channelFuture.isSuccess()) {
                            log.error("http proxy connect {} failed, send bad gate way to client then close channel, reason {}", httpMessage.getHost(), channelFuture.cause().getMessage());
                            ctx.writeAndFlush(HttpUtils.BAD_GATEWAY(httpMessage.getHost()));
                            SocketUtils.close(ctx.channel());
                        } else {
                            log.info("http proxy connect {} success", httpMessage.getHost());
                            Channel channel = channelFuture.channel(); // 获取连接的 Channel
                            channel.writeAndFlush(httpMessage.getFirstRequest());
                            // 继续做一个中继
                            ctx.pipeline().addLast(new RelayHandler(channel));
                            channel.pipeline().addLast(new RelayHandler(ctx.channel()));
                            // 发送完第一次请求后就可以不关心数据了
                            channel.pipeline().remove(HttpRequestEncoder.class);
                        }
                    }
                });
    }
}
