package org.congcong.nasproxy.core.proxy;

import org.congcong.nasproxy.common.config.Outbound;
import org.congcong.nasproxy.common.config.ProxyConfig;
import org.congcong.nasproxy.common.entity.AbstractMessageEntity;
import org.congcong.nasproxy.common.entity.Message;
import org.congcong.nasproxy.common.entity.Pair;
import org.congcong.nasproxy.common.entity.ProtocolType;
import org.congcong.nasproxy.common.util.GeoCacheUtil;
import org.congcong.nasproxy.protocol.ProtocolHandlerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class OutboundDecideService {

    private static final OutboundDecideService INSTANCE = new OutboundDecideService();

    private OutboundDecideService() {}

    public static OutboundDecideService getInstance() {
        return INSTANCE;
    }

    private Map<String, Outbound> domainRoutingCache = new HashMap<>();

    private List<Outbound> geositeRoutingCache = new ArrayList<>();

    private List<Outbound> geositeNotRoutingCache = new ArrayList<>();

    private final Outbound defaultOutbound = new Outbound();


    public void init(ProxyConfig proxyConfig) {
        defaultOutbound.setProtocol(ProtocolType.direct);
        List<Outbound> outbounds = proxyConfig.getOutbounds();
        if (outbounds != null) {
            for (Outbound outbound : outbounds) {
                Map<String, Object> settings = outbound.getSettings();
                if (settings.containsKey("targetAddress") && settings.containsKey("domain")) {
                    String domain = (String) settings.get("domain");
                    if (domain.startsWith("geosite")) {
                        String[] split = domain.split(":");
                        if (split.length == 2) {
                            if (split[1].startsWith("!")) {
                                geositeNotRoutingCache.add(outbound);
                            } else {
                                geositeRoutingCache.add(outbound);
                            }
                        }
                    } else {
                        domainRoutingCache.put(domain, outbound);
                    }
                }
            }
        }

    }

    /**
     * 优先级：
     * 自定义域名与ip
     * 地理位置不满足
     * 地理位置满足
     * 默认直连
     * @param message
     * @return
     */
    public Outbound decide(Message message) {
        String host = message.getHost();
        Outbound outbound = domainRoutingCache.get(host);
        if (outbound != null) {
            return outbound;
        }
        Pair<String, String> countryAndIpAddress = GeoCacheUtil.getCountryAndIpAddress(host);
        String country = countryAndIpAddress.getFst();
        String ip = countryAndIpAddress.getSnd();
        if (message instanceof AbstractMessageEntity messageEntity) {
            messageEntity.setIp(ip);
        }
        for (Outbound outbound1 : geositeNotRoutingCache) {
            Map<String, Object> settings = outbound1.getSettings();
            String domain = (String) settings.get("domain");
            if (!domain.contains(country)) {
                return outbound1;
            }
        }
        for (Outbound outbound1 : geositeRoutingCache) {
            Map<String, Object> settings = outbound1.getSettings();
            String domain = (String) settings.get("domain");
            if (domain.contains(country)) {
                return outbound1;
            }
        }
        return defaultOutbound;
    }

}
