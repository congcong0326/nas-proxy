package org.congcong.nasproxy.core.monitor.connect;



import org.congcong.nasproxy.common.entity.AttackLog;
import org.congcong.nasproxy.core.monitor.db.PartitionType;

import java.util.List;
import java.util.Map;

public interface IConnectionMonitor {

    /**
     * 当前的tcp连接数
     * @return
     */
    int currentTcpConnections();

    void tcpConnectIncrement();

    void tcpConnectDecrement();



    /**
     * 连接成功率
     * @return
     */
    double connectionSuccessRate();

    int connectionCount();

    int connectionFailed();

    void connectRemoteFailedIncrement();

    /**
     * 连接的地理位置分析
     * key user
     * value ip location
     * @return
     */
    Map<String, Long> ipAddressAnalysis(long currentTime, PartitionType partitionType, int count);


    /**
     * 可疑的ip地址
     * @param currentTime
     * @param partitionType
     * @return
     */
    Map<String, Long> suspiciousIPAddress(long currentTime, PartitionType partitionType);

    List<AttackLog> getAttackLog(long currentTime, PartitionType partitionType);

}
