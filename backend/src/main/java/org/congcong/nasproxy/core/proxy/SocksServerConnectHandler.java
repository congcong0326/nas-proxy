package org.congcong.nasproxy.core.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.proxy.ProxyConnectException;
import io.netty.util.NetUtil;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.*;
import org.congcong.nasproxy.common.util.SocketUtils;

@Slf4j
public class SocksServerConnectHandler<T extends Message> extends AbstractTcpTunnelConnectorHandler<Message> {

    @Override
    public RouteType getRouteType() {
        return RouteType.socks;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Message message) throws Exception {
        String outboundServerAddress = message.getProxyServerHost();
        int outboundServerPort = message.getProxyServerPort();
        // do connect remote
        Channel inboundChannel = ctx.channel();
        Bootstrap b = new Bootstrap();
        // 直接使用处理client的线程组去连接目标服务器
        b.group(inboundChannel.eventLoop())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, message.keepalive())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                Socks5ClientEncoder.DEFAULT,//编码
                                new Socks5InitialResponseDecoder(),//解码
                                new Socks5ClientHandler(message, getRelayPromise(ctx, message))//socks 客户端
                        );
                    }
                });
        b.connect(outboundServerAddress, outboundServerPort)
                .addListener((ChannelFutureListener) channelFuture -> {
                    if (!channelFuture.isSuccess()) {
                        log.error("{} tunnel connect {} failed", getType(), message.getHost());
                        connectionMonitor.connectRemoteFailedIncrement();
                        ctx.writeAndFlush(getConnectFailedMessage(message));
                        SocketUtils.close(ctx.channel());
                    }
                });
        ctx.channel().closeFuture().addListener((ChannelFutureListener) future -> {
            closeAndFlush(ctx);
        });
    }

    private static class Socks5ClientHandler extends SimpleChannelInboundHandler<Object> {

        private final Message message;

        private final Promise<Channel> promise;

        public Socks5ClientHandler(Message message, Promise<Channel> promise) {
            this.message = message;
            this.promise = promise;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            // 发送初始化握手请求，支持无认证
            ctx.writeAndFlush(new DefaultSocks5InitialRequest(Socks5AuthMethod.NO_AUTH));
        }

        private void sendConnectRequest(ChannelHandlerContext ctx) throws ProxyConnectException {
            // 构建连接请求（目标地址和端口）
            String host = message.getHost();
            Socks5AddressType addrType = null;
            if (NetUtil.isValidIpV4Address(host)) {
                addrType = Socks5AddressType.IPv4;
            } else if (NetUtil.isValidIpV6Address(host)) {
                addrType = Socks5AddressType.IPv6;
            } else {
                addrType = Socks5AddressType.DOMAIN;
            }
            Socks5CommandRequest request = new DefaultSocks5CommandRequest(
                    Socks5CommandType.CONNECT,
                    addrType,
                    message.getHost(),  // 目标主机
                    message.getPort() // 目标端口
            );
            ctx.writeAndFlush(request);
        }



        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 处理握手响应
            if (msg instanceof DefaultSocks5InitialResponse) {
                Socks5InitialResponse response = (Socks5InitialResponse) msg;
                if (response.authMethod() == Socks5AuthMethod.NO_AUTH) {
                    // 无认证，直接发送连接请求
                    sendConnectRequest(ctx);
                    ctx.pipeline().addFirst(new Socks5CommandResponseDecoder());
                    ctx.pipeline().remove(Socks5InitialResponseDecoder.class);
                } else {
                    ctx.close();
                    promise.setFailure(new RuntimeException("not support auth"));
                }
            } else if (msg instanceof Socks5CommandResponse response) {
                if (response.status() == Socks5CommandStatus.SUCCESS) {
                    log.info("connect success");
                    ctx.channel().pipeline().remove(Socks5ClientEncoder.class);
                    ctx.channel().pipeline().remove(Socks5CommandResponseDecoder.class);
                    ctx.channel().pipeline().remove(this);
                    promise.setSuccess(ctx.channel());
                } else {
                    log.warn("connect failed {}", response);
                    ctx.close();
                    promise.setFailure(new RuntimeException("connect failed"));
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
            promise.setFailure(new RuntimeException(cause.getMessage()));
        }
    }


    @Override
    protected Object getConnectSuccessMessage(Message message) {
        return null;
    }

    @Override
    protected Object getConnectFailedMessage(Message message) {
        return Unpooled.EMPTY_BUFFER;
    }

    @Override
    protected String getType() {
        return "socksClient";
    }

    @Override
    protected ChannelHandler getRemoveClass() {
        return this;
    }
}
