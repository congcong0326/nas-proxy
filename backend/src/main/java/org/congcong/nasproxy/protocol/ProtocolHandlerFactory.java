package org.congcong.nasproxy.protocol;

import io.netty.channel.SimpleChannelInboundHandler;
import org.congcong.nasproxy.common.config.Outbound;
import org.congcong.nasproxy.common.entity.AbstractMessageEntity;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.common.entity.ProtocolType;
import org.congcong.nasproxy.common.exception.UnsupportedProxyException;
import org.congcong.nasproxy.core.proxy.OutboundDecideService;
import org.congcong.nasproxy.protocol.http.WebTunnelConnectorHandler;
import org.congcong.nasproxy.protocol.http.WebTunnelToSocksConnectHandler;
import org.congcong.nasproxy.protocol.shadowSocks.ShadowSocksServerConnectHandler;
import org.congcong.nasproxy.protocol.shadowSocks.ShadowSocksToSocksServerConnectHandler;
import org.congcong.nasproxy.protocol.socks.SocksServerConnectHandler;
import org.congcong.nasproxy.protocol.socks.SocksToSocksConnectHandler;

public class ProtocolHandlerFactory {

    private static final ProtocolHandlerFactory INSTANCE = new ProtocolHandlerFactory();

    private ProtocolHandlerFactory() {}

    public static ProtocolHandlerFactory getInstance() {
        return INSTANCE;
    }

    private final OutboundDecideService outboundDecideService = OutboundDecideService.getInstance();

    public SimpleChannelInboundHandler<Message> getHandler(Message message, ProtocolType protocolType) {
        Outbound decide = outboundDecideService.decide(message);
        ProtocolType protocol = decide.getProtocol();
        if (decide.getSettings() != null) {
            String targetAddress = (String) decide.getSettings().get("targetAddress");
            if (targetAddress != null) {
                if (message instanceof AbstractMessageEntity abstractMessageEntity) {
                    if (protocol == ProtocolType.socks) {
                        abstractMessageEntity.setProxyServerHost(targetAddress);
                        abstractMessageEntity.setProxyServerPort(decide.getPort());
                    } else if (protocol == ProtocolType.direct) {
                        abstractMessageEntity.setHost(targetAddress);
                        abstractMessageEntity.setIp(targetAddress);
                        abstractMessageEntity.setPort(decide.getPort());
                    }
                }
            }
        }
        if (protocolType == ProtocolType.http) {
            if (protocol == ProtocolType.direct) {
                return WebTunnelConnectorHandler.getInstance();
            } else if (protocol == ProtocolType.socks) {
                return WebTunnelToSocksConnectHandler.getInstance();
            }
        } else if (protocolType == ProtocolType.socks) {
            if (protocol == ProtocolType.direct) {
                return SocksServerConnectHandler.getInstance();
            } else if (protocol == ProtocolType.socks) {
                return SocksToSocksConnectHandler.getInstance();
            }
        } else if (protocolType == ProtocolType.shadow_socks) {
            if (protocol == ProtocolType.direct) {
                return ShadowSocksServerConnectHandler.getInstance();
            } else if (protocol == ProtocolType.socks) {
                return ShadowSocksToSocksServerConnectHandler.getInstance();
            }
        }
        throw new UnsupportedProxyException();
    }


}
