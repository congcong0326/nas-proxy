package org.congcong.nasproxy.core.monitor.connect;


import org.congcong.nasproxy.common.entity.AttackLog;
import org.congcong.nasproxy.core.monitor.db.DataPartition;
import org.congcong.nasproxy.core.monitor.db.PartitionType;
import org.congcong.nasproxy.core.monitor.db.mapDb.DataPartitionImpl;
import org.congcong.nasproxy.core.monitor.db.mapDb.DbMangeService;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;


public class ConnectionMonitor implements IConnectionMonitor, IConnectionReport {

    private static final ConnectionMonitor INSTANCE = new ConnectionMonitor();

    private ConnectionMonitor() {}

    public static ConnectionMonitor getInstance() {
        return INSTANCE;
    }

    private final AtomicInteger currentConnectionCount = new AtomicInteger(0);

    private final AtomicInteger failedConnectionCount = new AtomicInteger(0);
    private final AtomicInteger totalConnectionCount = new AtomicInteger(0);

    private final DataPartition<Long> userAccessIp = new DataPartitionImpl<>();

    private final DataPartition<Long> suspiciousIp = new DataPartitionImpl<>();

    private final DataPartition<AttackLog> attackLogWrite = new DataPartitionImpl<>();

    @Override
    public int currentTcpConnections() {
        return currentConnectionCount.get();
    }

    @Override
    public void tcpConnectIncrement() {
        totalConnectionCount.incrementAndGet();
        currentConnectionCount.incrementAndGet();
    }

    @Override
    public void tcpConnectDecrement() {
        currentConnectionCount.decrementAndGet();
    }

    @Override
    public double connectionSuccessRate() {
        int total = totalConnectionCount.get();
        int failed = failedConnectionCount.get();
        return (double) (total - failed) / (double) total;
    }

    @Override
    public int connectionCount() {
        return totalConnectionCount.get();
    }

    @Override
    public int connectionFailed() {
        return failedConnectionCount.get();
    }

    @Override
    public void connectRemoteFailedIncrement() {
         failedConnectionCount.getAndIncrement();
    }

    @Override
    public Map<String, Long> ipAddressAnalysis(long currentTime, PartitionType partitionType, int count) {
        Map<String, Long> successIp = userAccessIp.queryMap(System.currentTimeMillis(), buildIpTable("successIp"));
        Map<String, Long> result = new LinkedHashMap<>();
        IPGeoUtil ipUtil = IPGeoUtil.getInstance();
        int maxCount = 0;
        for (Map.Entry<String, Long> entry : successIp.entrySet()) {
            String ip = entry.getKey();
            Long accessCount = entry.getValue();
            if (accessCount != 1) {
                Optional<IPGeoUtil.LocationInfo> location = ipUtil.getLocation(ip);
                if (location.isPresent()) {
                    result.put(ip + " / " + location.get(), accessCount);
                } else {
                    result.put(ip, accessCount);
                }
                if (maxCount ++ > count) {
                    break;
                }
            }
        }
        return result;
    }


    @Override
    public  Map<String, Long> suspiciousIPAddress(long currentTime, PartitionType partitionType) {
        IPGeoUtil ipUtil = IPGeoUtil.getInstance();
        Map<String, Long> failedIp = suspiciousIp.queryMap(System.currentTimeMillis(), buildIpTable("failedIp"));
        if (failedIp.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map.Entry<String, Long> entry : failedIp.entrySet()) {
            String ip = entry.getKey();
            Long accessCount = entry.getValue();
            if (accessCount == 1) {
                Optional<IPGeoUtil.LocationInfo> location = ipUtil.getLocation(ip);
                if (location.isPresent()) {
                    result.put(ip + " / " + location.get(), accessCount);
                } else {
                    result.put(ip, accessCount);
                }
            }
        }
        return result;
    }

    @Override
    public List<AttackLog> getAttackLog(long currentTime, PartitionType partitionType) {
         return attackLogWrite.query(currentTime, "attack_log");
    }


    @Override
    public void reportAccessIp(String ip) {
        userAccessIp.writeOrMerge(buildIpTable("successIp"), ip, 1L, PartitionType.MONTH);
    }

    @Override
    public void reportSuspiciousIp(String ip) {
        suspiciousIp.writeOrMerge(buildIpTable("failedIp"), ip, 1L, PartitionType.MONTH);
        DbMangeService.getMangeDb().commit();
    }

    @Override
    public void reportAttackLog(AttackLog attackLog) {
        attackLogWrite.writeByTime(attackLog, "attack_log", PartitionType.MONTH);
        suspiciousIp.writeOrMerge(buildIpTable("failedIp"), attackLog.getIp(), 1L, PartitionType.MONTH);
        DbMangeService.getMangeDb().commit();
    }

    private String buildIpTable(String userName) {
        return userName + "_ip_table";
    }
}
