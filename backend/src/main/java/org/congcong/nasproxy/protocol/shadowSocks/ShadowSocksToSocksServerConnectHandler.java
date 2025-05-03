package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.core.proxy.SocksServerConnectHandler;



public class ShadowSocksToSocksServerConnectHandler extends SocksServerConnectHandler<Message> {

    private static final ShadowSocksToSocksServerConnectHandler INSTANCE = new ShadowSocksToSocksServerConnectHandler();

    private ShadowSocksToSocksServerConnectHandler() {}

    public static ShadowSocksToSocksServerConnectHandler getInstance() {
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
        return "shadowToSocks";
    }

    @Override
    protected ChannelHandler getRemoveClass() {
        return this;
    }

}
