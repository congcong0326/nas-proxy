package org.congcong.nasproxy.protocol.http;

import io.netty.channel.ChannelHandler;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.common.util.HttpUtils;
import org.congcong.nasproxy.core.proxy.AbstractTcpTunnelConnectorHandler;

@Slf4j
@ChannelHandler.Sharable
public class WebTunnelConnectorHandler extends AbstractTcpTunnelConnectorHandler<Message> {


    private static final WebTunnelConnectorHandler INSTANCE = new WebTunnelConnectorHandler();

    private WebTunnelConnectorHandler() {}

    public static WebTunnelConnectorHandler getInstance() {
        return INSTANCE;
    }

    @Override
    protected Object getConnectSuccessMessage(Message message) {
        return HttpUtils.CONNECT_ESTABLISHED_BUF();
    }

    @Override
    protected Object getConnectFailedMessage(Message message) {
        return HttpUtils.BAD_GATEWAY_BUF();
    }

    @Override
    protected String getType() {
        return "web tunnel";
    }

    @Override
    protected ChannelHandler getRemoveClass() {
        return this;
    }


}
