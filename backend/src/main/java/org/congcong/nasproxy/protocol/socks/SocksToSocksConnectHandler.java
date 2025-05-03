package org.congcong.nasproxy.protocol.socks;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.common.entity.SocketMessage;
import org.congcong.nasproxy.core.proxy.SocksServerConnectHandler;
import org.springframework.stereotype.Component;

@Component
public class SocksToSocksConnectHandler extends SocksServerConnectHandler<Message> {


    private static final SocksToSocksConnectHandler INSTANCE = new SocksToSocksConnectHandler();

    private SocksToSocksConnectHandler() {}

    public static SocksToSocksConnectHandler getInstance() {
        return INSTANCE;
    }


    @Override
    protected Object getConnectSuccessMessage(Message message) {
        SocketMessage socketMessage = (SocketMessage) message;
        return new DefaultSocks5CommandResponse(
                Socks5CommandStatus.SUCCESS,
                socketMessage.getType(),
                socketMessage.getHost(),
                ((SocketMessage) message).getPort());
    }

    @Override
    protected Object getConnectFailedMessage(Message message) {
        return new DefaultSocks5CommandResponse(
                Socks5CommandStatus.FAILURE, ((SocketMessage) message).getType());
    }

    @Override
    protected String getType() {
        return "socks tunnel to socks";
    }

    @Override
    protected ChannelHandler getRemoveClass() {
        return this;
    }

}
