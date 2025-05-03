package org.congcong.nasproxy.common.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserAccessIpEntity implements Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    private String userName;

    private String accessIp;

    private long accessTime;

}
