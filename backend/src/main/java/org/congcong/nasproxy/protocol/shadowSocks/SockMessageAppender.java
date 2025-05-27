package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.SocketMessage;

@Slf4j
public class SockMessageAppender extends ChannelInboundHandlerAdapter {

    private SocketMessage message;


    public void channelRead(ChannelHandlerContext ctx, Object in) throws Exception {
        // 暂存握手请求
        if (in instanceof SocketMessage inMessage) {
            message = inMessage;
            ctx.fireChannelRead(message);
        } else if (in instanceof ByteBuf appendData) {
            // isConsume 代表连接已经建立好，该handler的职责完成，可以移除
            if (message != null && message.isConsume()) {
                ctx.channel().pipeline().remove(this);
                message = null;
                ctx.fireChannelRead(in);
                return;
            }
            // 此时与目标服务器地址的连接未建立成功，需要将收到的数据追加到message的firstRequest中
            // 只有第一次请求在一次没接收完成时才会出现该情况
            assert message != null;
            byte[] firstRequest = (byte[]) message.getFirstRequest();
            byte[] tmp = new byte[firstRequest.length + appendData.readableBytes()];
            System.arraycopy(firstRequest, 0, tmp, 0, firstRequest.length);
            appendData.readBytes(tmp, firstRequest.length, appendData.readableBytes());
            message.setFirstRequest(tmp);
        } else {
            ctx.fireChannelRead(in);
        }
    }
}
