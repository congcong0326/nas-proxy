package org.congcong.nasproxy.common.entity;

public record DiskInfo(
    String device,
    String model,
    String serial,
    String size,
    String status,
    int temperature
) {}