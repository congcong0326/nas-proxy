package org.congcong.nasproxy.protocol.shadowSocks;

import io.netty.channel.socket.SocketChannel;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.common.entity.Const;
import org.congcong.nasproxy.core.proxy.AbstractChannelInitializer;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.CryptoProcessorFactory;
import org.congcong.nasproxy.protocol.shadowSocks.encryption.algorithm.Algorithm;

import java.util.Map;


public class ShadowSocksInitializer extends AbstractChannelInitializer {


    public ShadowSocksInitializer(Inbound inbound) {
        super(inbound);
    }

    @Override
    protected void init(SocketChannel socketChannel) {
        Map<String, Object> settings = inbound.getSettings();
        String method = (String) settings.get(Const.METHOD);
        String password = (String) settings.get(Const.PASSWORD);
        socketChannel.pipeline().addLast(
                // 解密数据
                new DecryptedSocksHandler(CryptoProcessorFactory.createProcessor(Algorithm.valueOf(method), password)),
                new Socks5TargetAddressHandler(),
                // 加密数据
                new EncryptedSocksHandler(CryptoProcessorFactory.createProcessor(Algorithm.valueOf(method), password)));
    }
}
