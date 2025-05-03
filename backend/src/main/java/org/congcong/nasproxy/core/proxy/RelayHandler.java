package org.congcong.nasproxy.core.proxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.Const;
import org.congcong.nasproxy.common.entity.Context;
import org.congcong.nasproxy.common.util.SocketUtils;

/**
 * 中继处理器
 * 将这个处理器注册到某个channel的pipeline，当io事件发生时，传递给其他channel
 */
@Slf4j
public class RelayHandler extends ChannelInboundHandlerAdapter {

    private final Channel relayChannel;



    public RelayHandler(Channel relayChannel) {
        this.relayChannel = relayChannel;
    }

    /**
     * client --> (auto read)channel ---> channel (write) ---> server
     * client <-- (write)channel <--- channel (auto read)<---server
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 当对端不可写时，别读了
        if (!relayChannel.isWritable()) {
            // 暂停读取，但不丢弃消息
            Context context = ctx.channel().attr(Const.CONTEXT).get();
            if (context != null) {
                log.error("relay channel is not writable, user {}, remote url {}", context.getUserName(), context.getRemoteUrl());
            }
            ctx.channel().config().setAutoRead(false);
            // 添加写入完成监听器，以便在可写时恢复
            relayChannel.writeAndFlush(msg).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("relay channel can  write");
                    ctx.channel().config().setAutoRead(true);
                }
            });
        } else {
            // 正常转发
            relayChannel.writeAndFlush(msg);
        }
    }

    //对端关闭，则一起关闭
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        //有些协议不是长连接，请求一次就关闭了，这个打印并不重要
        log.debug("{} is inactive, close relay {}", ctx.channel().localAddress(), relayChannel.localAddress());
        SocketUtils.close(relayChannel);
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        // 当本channel不可写时，暂停 relayChannel 的读取
        relayChannel.config().setAutoRead(ctx.channel().isWritable());  // 停止读取 relayChannel
        ctx.fireChannelWritabilityChanged();
    }

}
