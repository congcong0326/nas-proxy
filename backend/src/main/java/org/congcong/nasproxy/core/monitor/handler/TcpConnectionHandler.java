package org.congcong.nasproxy.core.monitor.handler;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.util.SocketUtils;
import org.congcong.nasproxy.core.monitor.connect.ConnectionMonitor;
import org.congcong.nasproxy.core.monitor.connect.IConnectionMonitor;

@Slf4j
@ChannelHandler.Sharable
public class TcpConnectionHandler extends ChannelInboundHandlerAdapter {


    private static final TcpConnectionHandler INSTANCE = new TcpConnectionHandler();

    private TcpConnectionHandler() {}

    public static TcpConnectionHandler getInstance() {
        return INSTANCE;
    }

    private final IConnectionMonitor connectionMonitor = ConnectionMonitor.getInstance();


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // 当连接建立时增加计数
        connectionMonitor.tcpConnectIncrement();
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        // 当连接断开时减少计数
        connectionMonitor.tcpConnectDecrement();
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        SocketUtils.close(ctx.channel());
        ctx.fireExceptionCaught(cause);
    }

}
