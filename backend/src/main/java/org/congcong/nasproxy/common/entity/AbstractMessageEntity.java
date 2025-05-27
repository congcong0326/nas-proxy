package org.congcong.nasproxy.common.entity;

import lombok.Data;

@Data
public class AbstractMessageEntity implements Message {

    private String host;

    private int port;

    private String ip;

    private Object firstRequest;

    private boolean keepalive;

    private String proxyServerHost;

    private int proxyServerPort;

    boolean consume;

    public void setConsume() {
        consume = true;
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getIp() {
        return ip;
    }

    @Override
    public String getProxyServerHost() {
        return proxyServerHost;
    }

    @Override
    public int getProxyServerPort() {
        return proxyServerPort;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public Object firstRequest() {
        return firstRequest;
    }

    @Override
    public boolean keepalive() {
        return keepalive;
    }

}
