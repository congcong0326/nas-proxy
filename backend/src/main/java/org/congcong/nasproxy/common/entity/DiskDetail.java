package org.congcong.nasproxy.common.entity;

import java.util.List;

public record DiskDetail(
    String device,
    String model,
    String serial,
    String size,
    int temperature,
    String health,
    boolean smartSupported,
    boolean smartEnabled,
    long powerOnHours,
    int powerCycleCount,
    int reallocatedSectorCount,
    int seekErrorRate,
    int spinRetryCount,
    int udmaCrcErrorCount,
    List<Integer> historyTemperature
) {}