package org.congcong.nasproxy.common.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Context implements Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    private String userName;

    private String remoteUrl;

    private long up;

    private long down;

    private long startTime = System.currentTimeMillis();

    private int durationTime;

    private String clientIp;

//    private ProtocolType outboundServerType;

//    private String outboundServerAddress;
//
//    private int outboundServerPort;
//
//    private String notOutboundGeoIp;


}
