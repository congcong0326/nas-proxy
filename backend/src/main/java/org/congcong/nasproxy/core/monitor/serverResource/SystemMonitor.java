package org.congcong.nasproxy.core.monitor.serverResource;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.text.SimpleDateFormat;


public class SystemMonitor implements ISystemMonitor {

    private static final SystemMonitor INSTANCE = new SystemMonitor();

    private SystemMonitor() {}

    public static SystemMonitor getInstance() {
        return INSTANCE;
    }

    private static final long startTime = System.currentTimeMillis();

    private final SystemInfo si = new SystemInfo();


    @Override
    public double getCpuUsage() {
        CentralProcessor cpu = si.getHardware().getProcessor();
        long[] prevTicks = cpu.getSystemCpuLoadTicks();
        // 获取当前CPU负载（百分比）
        return cpu.getSystemCpuLoadBetweenTicks(prevTicks) * 100;
    }

    @Override
    public String getDiskUsage() {
        StringBuilder diskUsageInfo = new StringBuilder();
        FileSystem fileSystem = si.getOperatingSystem().getFileSystem();
        for (OSFileStore fileStore : fileSystem.getFileStores()) {
            // 获取磁盘的总空间和可用空间
            long totalSpace = fileStore.getTotalSpace();
            long usableSpace = fileStore.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            // 输出磁盘使用情况
            diskUsageInfo.append("磁盘").append(fileStore.getName()).append("：").append(percentGBParse(usedSpace, totalSpace));
        }
        return diskUsageInfo.toString();
    }


    @Override
    public String getSystemMemoryUsage() {
        long totalMemory = si.getHardware().getMemory().getTotal();
        long availableMemory = si.getHardware().getMemory().getAvailable();
        long used = totalMemory - availableMemory;
        return percentMBParse(used, totalMemory);
    }

    private String percentMBParse(long usedKb, long totalKb) {
        double usage = (double) usedKb / totalKb * 100;
        String formattedPercent = String.format("%.2f", usage);
        return  formattedPercent + "%，" +"[" + (usedKb / (1024 * 1024)) + "MB/" + (totalKb / (1024 * 1024))+"MB]。";
    }

    private String percentGBParse(long usedKb, long totalKb) {
        double usage = (double) usedKb / totalKb * 100;
        String formattedPercent = String.format("%.2f", usage);
        return  formattedPercent + "%，" +"[" + (usedKb / (1024 * 1024 * 1024)) + "GB/" + (totalKb / (1024 * 1024 * 1024))+"GB]。";
    }

    @Override
    public String getJvmMemoryUsage() {
        // 获取 MemoryMXBean 实例来获取堆内存的信息
        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();

        // 获取堆内存的使用情况
        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        long heapMemoryUsed = heapMemoryUsage.getUsed(); // 堆内存已用
        long heapMemoryMax = heapMemoryUsage.getMax();   // 堆内存最大值

        // 获取最终的内存使用信息字符串
        return percentMBParse(heapMemoryUsed, heapMemoryMax);
    }

    @Override
    public String processRunDetail() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String format = formatter.format(startTime);
        StringBuilder sb = new StringBuilder("启动时间：").append(format).append("，");
        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        // 将已运行时间转换为秒和分钟
        long seconds = elapsedTime / 1000;
        long minutes = seconds / 60;
        long hour = minutes / 60;
        sb.append("运行时间：");
        if (hour > 24) {
            sb.append(hour).append("小时").append("。");
        } else {
            long day = hour / 24;
            hour = hour % 24;
            sb.append(day).append("天").append(hour).append("小时").append("。");
        }
        return sb.toString();
    }
}
