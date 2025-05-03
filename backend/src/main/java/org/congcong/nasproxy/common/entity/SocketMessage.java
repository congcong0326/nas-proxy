package org.congcong.nasproxy.common.entity;

import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import lombok.Data;

@Data
public class SocketMessage extends AbstractMessageEntity {

    private Socks5AddressType type;

}
