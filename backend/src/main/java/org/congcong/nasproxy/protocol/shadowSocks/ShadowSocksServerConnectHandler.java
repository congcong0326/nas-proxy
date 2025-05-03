package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.core.proxy.AbstractTcpTunnelConnectorHandler;


public class ShadowSocksServerConnectHandler extends AbstractTcpTunnelConnectorHandler<Message> {


    private static final ShadowSocksServerConnectHandler INSTANCE = new ShadowSocksServerConnectHandler();

    private ShadowSocksServerConnectHandler() {}

    public static ShadowSocksServerConnectHandler getInstance() {
        return INSTANCE;
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
        return "ShadowSocks";
    }

    @Override
    protected ChannelHandler getRemoveClass() {
        return this;
    }
}
