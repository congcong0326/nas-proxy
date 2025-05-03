package org.congcong.nasproxy.core;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.congcong.nasproxy.common.config.Inbound;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.common.config.WolConfig;
import org.congcong.nasproxy.common.util.ConfigReader;
import org.congcong.nasproxy.core.monitor.disk.DiskService;
import org.congcong.nasproxy.core.monitor.traffic.TrafficReportService;
import org.congcong.nasproxy.core.monitor.wol.IpMonitor;
import org.congcong.nasproxy.core.proxy.OutboundDecideService;
import org.congcong.nasproxy.core.proxy.ProxyServer;
import org.congcong.nasproxy.protocol.http.HttpServerInitializer;
import org.congcong.nasproxy.protocol.shadowSocks.ShadowSocksInitializer;
import org.congcong.nasproxy.protocol.socks.SocksServerInitializer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class BootStrap implements ApplicationRunner {


    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 开启代理
        ProxyConfig proxyConfig = ConfigReader.loadProxyConfig("config.json");
        OutboundDecideService.getInstance().init(proxyConfig);
        initInbounds(proxyConfig.getInbounds(), proxyConfig);

        // 开启wol
        WolConfig wolConfig = ConfigReader.loadWolConfig("wol.json");
        IpMonitor.getInstance().start(wolConfig);

        // 开启日志消费者
        TrafficReportService.getInstance().initConsumer();

        // 统计磁盘温度
        DiskService.getInstance().start();
    }
    
    private void initInbounds(List<Inbound> inbounds, ProxyConfig proxyConfig) {
        for (Inbound inbound : inbounds) {
            if (!inbound.isEnable()) {
                log.info("not load service {}", inbound.getTag());
                continue;
            }
            log.info("load service {}", inbound.getTag());
            new Thread(() -> {
                ProxyServer proxyServer = new ProxyServer() {
                    @Override
                    public int getPort() {
                        return inbound.getPort();
                    }

                    @Override
                    public String getIp() {
                        return "0.0.0.0";
                    }

                    @Override
                    public String getServerName() {
                        return inbound.getTag();
                    }

                    @Override
                    public ChannelInitializer<SocketChannel> getChildHandler() {
                        switch (inbound.getProtocol()) {
                            case http:
                                return new HttpServerInitializer(inbound);
                            case socks:
                                return new SocksServerInitializer(inbound);
                            case shadow_socks:
                                return new ShadowSocksInitializer(inbound);
                            default:
                                log.error("not support server type {}", inbound.getProtocol());
                                System.exit(0);
                        }
                        return null;
                    }
                };
                try {
                    proxyServer.start();
                } catch (InterruptedException ignored) {

                }
            }).start();
        }
    }
}
