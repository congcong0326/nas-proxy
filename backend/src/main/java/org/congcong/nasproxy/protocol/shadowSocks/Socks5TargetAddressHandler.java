package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.ProtocolType;
import org.congcong.nasproxy.common.entity.SocketMessage;
import org.congcong.nasproxy.protocol.ProtocolHandlerFactory;

import java.util.List;

@Slf4j
public class Socks5TargetAddressHandler extends ByteToMessageDecoder {

    //[1-byte type][variable-length host][2-byte port]
    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf byteBuf, List<Object> list) throws Exception {
        // 检查可读字节数是否足够至少包含1字节类型和2字节端口
        if (byteBuf.readableBytes() < 3) {
            return;  // 等待更多数据
        }
        byteBuf.markReaderIndex();  // 标记当前位置以便在数据不足时恢复
        byte type = byteBuf.readByte();
        SocketMessage socketMessage = new SocketMessage();
        Socks5AddressType socks5AddressType = Socks5AddressType.valueOf(type);
        socketMessage.setType(socks5AddressType);

        String host;
        if (socks5AddressType == Socks5AddressType.IPv4) {
            if (byteBuf.readableBytes() < 4) {
                byteBuf.resetReaderIndex();  // 恢复读取位置
                return;  // 等待更多数据
            }
            byte[] hostBytes = new byte[4];
            byteBuf.readBytes(hostBytes);
            host = (hostBytes[0] & 0xFF) + "." + (hostBytes[1] & 0xFF) + "." +
                    (hostBytes[2] & 0xFF) + "." + (hostBytes[3] & 0xFF);
        } else if (socks5AddressType == Socks5AddressType.DOMAIN) {
            if (byteBuf.readableBytes() < 1) {
                byteBuf.resetReaderIndex();
                return;  // 等待更多数据
            }
            int domainLength = byteBuf.readByte();
            if (byteBuf.readableBytes() < domainLength) {
                byteBuf.resetReaderIndex();
                return;  // 等待更多数据
            }
            byte[] hostBytes = new byte[domainLength];
            byteBuf.readBytes(hostBytes);
            host = new String(hostBytes);
        } else if (socks5AddressType == Socks5AddressType.IPv6) {
            if (byteBuf.readableBytes() < 16) {
                byteBuf.resetReaderIndex();
                return;  // 等待更多数据
            }
            byte[] hostBytes = new byte[16];
            byteBuf.readBytes(hostBytes);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 16; i += 2) {
                sb.append(String.format("%02x%02x", hostBytes[i], hostBytes[i + 1]));
                if (i < 14) sb.append(":");
            }
            host = sb.toString();
        } else {
            throw new IllegalArgumentException("Unknown address type: " + type);
        }

        if (byteBuf.readableBytes() < 2) {
            byteBuf.resetReaderIndex();  // 等待端口数据
            return;
        }
        // 解析端口
        socketMessage.setHost(host);
        socketMessage.setPort(byteBuf.readUnsignedShort());
        socketMessage.setKeepalive(true);
        // 拷贝剩下的负载数据
        int readableBytes = byteBuf.readableBytes();
        if (readableBytes != 0) {
            byte[] byteArray = new byte[readableBytes];  // 创建字节数组
            byteBuf.readBytes(byteArray);  // 将 byteBuf 数据拷贝到字节数组中
            socketMessage.setFirstRequest(byteArray);
            log.debug("first request ready");
        } else {
            log.debug("wait for first request");
            byteBuf.resetReaderIndex();
            return;
        }
        ctx.pipeline().addLast(ProtocolHandlerFactory.getInstance().getHandler(socketMessage, ProtocolType.shadow_socks));
        ctx.channel().pipeline().remove(this);
        // 代理请求到本地管理端
        ctx.fireChannelRead(socketMessage);
    }

//    private void enrichContext(ChannelHandlerContext ctx, String remoteIp) {
//        Context context = ctx.channel().attr(Const.CONTEXT).get();
//        if (context != null) {
//            InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
//            String clientIp = remoteAddress.getAddress().getHostAddress(); // 获取客户端IP
//            context.setRemoteUrl(remoteIp);
//            context.setClientIp(clientIp);
//        }
//    }


}
