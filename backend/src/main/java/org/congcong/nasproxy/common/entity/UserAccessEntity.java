package org.congcong.nasproxy.common.entity;

import lombok.Data;

@Data
public class UserAccessEntity {


    private String remoteUrl;

    private int accessCount;

    private long up;

    private long down;

    private int durationTime;

    private String clientIp;

    private long lastAccessTime;


}
