package org.congcong.nasproxy.core.monitor.serverResource;

public interface ISystemMonitor {

    double getCpuUsage();


    // 获取磁盘使用情况（磁盘总量和已用量）
    String getDiskUsage();

    String getSystemMemoryUsage();


    String getJvmMemoryUsage();


    String processRunDetail();

}
