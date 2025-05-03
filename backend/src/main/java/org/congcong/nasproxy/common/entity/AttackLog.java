package org.congcong.nasproxy.common.entity;

import lombok.Data;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

@Data
public class AttackLog implements Serializable {

    private static final long serialVersionUID = 6695383790847736493L;

    private int port;

    private int receiveBytes;

    private int sendBytes;

    private long startTime;

    private int attackType;

    private long lastTime;

    private String ip;

    public static String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

}
