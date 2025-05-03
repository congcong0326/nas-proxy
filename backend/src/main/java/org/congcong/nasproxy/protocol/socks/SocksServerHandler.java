package org.congcong.nasproxy.protocol.socks;

import io.netty.channel.*;
import io.netty.handler.codec.socksx.SocksMessage;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.SocketMessage;
import org.congcong.nasproxy.common.util.SocketUtils;


import java.net.InetSocketAddress;


@ChannelHandler.Sharable
@Slf4j
public class SocksServerHandler extends SimpleChannelInboundHandler<SocksMessage>  {


    private static final SocksServerHandler INSTANCE = new SocksServerHandler();

    private SocksServerHandler() {}

    public static SocksServerHandler getInstance() {
        return INSTANCE;
    }


    @Override
    protected void channelRead0(ChannelHandlerContext ctx, SocksMessage socksMessage) throws Exception {
        // 第一次请求过来，协商认证方式，目前只支持用户名加密码
        if (socksMessage instanceof DefaultSocks5InitialRequest) {
            DefaultSocks5InitialRequest authType = (DefaultSocks5InitialRequest) socksMessage;
            boolean clientSupportPASSWORDAuth = false;
            for (Socks5AuthMethod authMethod : authType.authMethods()) {
                if (authMethod == Socks5AuthMethod.PASSWORD) {
                    clientSupportPASSWORDAuth = true;
                    break;
                }
            }
            //如果客户端不支持，返回一个UNACCEPTED后断开连接
            if (!clientSupportPASSWORDAuth) {
                ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED)).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        SocketUtils.close(channelFuture.channel());
                    }
                });
                return;
            }
            // 这里目前只支持用户命加密码的认证方式 DefaultSocks5PasswordAuthRequest
            ctx.pipeline().addFirst(new Socks5PasswordAuthRequestDecoder());
            ctx.write(new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD));
        }
        // 校验认证请求
        else if (socksMessage instanceof DefaultSocks5PasswordAuthRequest) {
            DefaultSocks5PasswordAuthRequest authRequest = (DefaultSocks5PasswordAuthRequest) socksMessage;
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().localAddress();
            int localPort = socketAddress.getPort();
            boolean b = PASSWORDAuthService.checkUP(authRequest.username(), authRequest.password(), localPort);
            if (!b) {
                ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE)).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture channelFuture) throws Exception {
                        SocketUtils.close(channelFuture.channel());
                    }
                });
            } else {
                ctx.pipeline().addFirst(new Socks5CommandRequestDecoder());
                ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
                ctx.write(new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS));
            }
        }
        // 认证都通过了，这是请求建立隧道的请求
        else if (socksMessage instanceof Socks5CommandRequest connectRequest) {
            if (connectRequest.type() == Socks5CommandType.CONNECT) {
                ctx.pipeline().addLast(SocksServerConnectHandler.getInstance());
                ctx.pipeline().remove(this);
                ctx.pipeline().remove(Socks5CommandRequestDecoder.class);
                //ctx.pipeline().remove(Socks5ServerEncoder.DEFAULT.getClass());

                SocketMessage socketMessage = new SocketMessage();
                socketMessage.setType(connectRequest.dstAddrType());
                socketMessage.setHost(connectRequest.dstAddr());
                socketMessage.setPort(connectRequest.dstPort());
                socketMessage.setKeepalive(true);
                ctx.fireChannelRead(socketMessage);
            } else {
                log.error("socks not support {}", connectRequest);
                SocketUtils.close(ctx.channel());
            }
        } else {
            // 不认识的类型
            SocketUtils.close(ctx.channel());
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable throwable) {
        throwable.printStackTrace();
        SocketUtils.close(ctx.channel());
    }

}
