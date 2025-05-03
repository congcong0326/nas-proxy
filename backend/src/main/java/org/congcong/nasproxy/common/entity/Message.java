package org.congcong.nasproxy.common.entity;

public interface Message {

    String getHost();

    String getIp();

    String getProxyServerHost();

    int getProxyServerPort();

    int getPort();

    Object firstRequest();

    boolean keepalive();
}
