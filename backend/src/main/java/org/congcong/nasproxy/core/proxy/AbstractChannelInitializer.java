package org.congcong.nasproxy.core.proxy;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.core.monitor.handler.ContextInitHandler;
import org.congcong.nasproxy.core.monitor.handler.DownTrafficHandler;
import org.congcong.nasproxy.core.monitor.handler.TcpConnectionHandler;
import org.congcong.nasproxy.core.monitor.handler.UpTrafficHandler;

public abstract class AbstractChannelInitializer extends ChannelInitializer<SocketChannel> {

    protected final Inbound inbound;


    public AbstractChannelInitializer(Inbound inbound) {
        this.inbound = inbound;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        socketChannel.pipeline().addLast(
                TcpConnectionHandler.getInstance(),
                DownTrafficHandler.getInstance(),
                new ContextInitHandler(inbound),
                //new LoggingHandler(LogLevel.INFO),
                UpTrafficHandler.getInstance());
        init(socketChannel);
    }

    protected abstract void init(SocketChannel socketChannel);
}
