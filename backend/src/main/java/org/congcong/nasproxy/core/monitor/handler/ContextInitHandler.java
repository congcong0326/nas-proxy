package org.congcong.nasproxy.core.monitor.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.common.entity.Const;
import org.congcong.nasproxy.common.entity.Context;

import java.net.InetSocketAddress;


@Slf4j
public class ContextInitHandler extends ChannelInboundHandlerAdapter  {

    private final Inbound inbound;


    public ContextInitHandler(Inbound inbound) {
        this.inbound = inbound;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Context context = new Context();
        context.setUserName(inbound.getUser());
        // 获取客户端IP
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        context.setClientIp(clientIp);

        ctx.channel().attr(Const.CONTEXT).set(context);
    }

}
