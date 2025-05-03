package org.congcong.nasproxy.core.monitor.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.congcong.nasproxy.core.monitor.UserTrafficType;

@ChannelHandler.Sharable
public class DownTrafficHandler extends ChannelInboundHandlerAdapter {


    private static final DownTrafficHandler INSTANCE = new DownTrafficHandler();

    private DownTrafficHandler() {}

    public static DownTrafficHandler getInstance() {
        return INSTANCE;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            // 如果接收到的数据是 ByteBuf 类型，则累加它的可读字节长度
            ByteBuf byteBuf = (ByteBuf) msg;
            int readableBytes = byteBuf.readableBytes();
            TrafficHandlerUtil.handleTraffic(ctx, readableBytes, UserTrafficType.UP);
        }
        ctx.fireChannelRead(msg);
    }

}
