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
        // 通用指标
        long powerOnHours,
        int powerCycleCount,
        // HDD特有指标
        int reallocatedSectorCount,
        int seekErrorRate,
        int spinRetryCount,
        int udmaCrcErrorCount,
        // SSD通用指标
        int percentageUsed,         // SSD剩余寿命指标（百分比）
        long dataUnitsRead,         // 读取的数据量
        long dataUnitsWritten,      // 写入的数据量
        int unsafeShutdowns,        // 非正常断电次数
        int mediaErrors,            // 媒体和数据完整性错误
        // SATA SSD特有指标
        int ssdLifeLeft,            // SSD剩余寿命（百分比）
        long flashWritesGiB,        // Flash写入总量（GiB）
        long lifetimeWritesGiB,     // 寿命写入总量（GiB）
        long lifetimeReadsGiB,      // 寿命读取总量（GiB）
        int averageEraseCount,      // 平均擦除次数
        int maxEraseCount,          // 最大擦除次数
        int totalEraseCount,        // 总擦除次数
        String diskType,            // 磁盘类型：HDD、SATA_SSD或NVME_SSD
        List<Integer> historyTemperature
) {}