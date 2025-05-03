package org.congcong.nasproxy.protocol.socks;

import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.core.proxy.AbstractChannelInitializer;


public class SocksServerInitializer extends AbstractChannelInitializer {


    public SocksServerInitializer(Inbound inbound) {
        super(inbound);
    }

    @Override
    protected void init(SocketChannel socketChannel) {
        socketChannel.pipeline().addLast(
                Socks5ServerEncoder.DEFAULT,
                new Socks5InitialRequestDecoder(),
                SocksServerHandler.getInstance());
    }
}
