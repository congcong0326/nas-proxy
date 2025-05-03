package org.congcong.nasproxy.core.monitor.traffic;


import jakarta.annotation.PostConstruct;
import org.congcong.nasproxy.common.entity.Context;
import org.congcong.nasproxy.common.entity.UserAccessEntity;
import org.congcong.nasproxy.common.util.TimeUtil;
import org.congcong.nasproxy.core.monitor.connect.ConnectionMonitor;
import org.congcong.nasproxy.core.monitor.connect.IConnectionReport;
import org.congcong.nasproxy.core.monitor.db.DataPartition;
import org.congcong.nasproxy.core.monitor.db.PartitionType;
import org.congcong.nasproxy.core.monitor.db.mapDb.DataPartitionImpl;
import org.congcong.nasproxy.core.monitor.db.mapDb.DbMangeService;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


public class TrafficReportService implements ITrafficReport, ITrafficAnalysis {


    private static final TrafficReportService INSTANCE = new TrafficReportService();

    private TrafficReportService() {}

    public static TrafficReportService getInstance() {
        return INSTANCE;
    }

    private final IConnectionReport connectionReport = ConnectionMonitor.getInstance();
    private final DataPartition<Context> dataPartition = new DataPartitionImpl<>();
    private final DataPartition<Long> trafficPartition = new DataPartitionImpl<>();

    /**
     * 用户的总流量
     */
    private final String TRAFFIC_USER_TABLE = "TRAFFIC_USER_TABLE";

    /**
     * 总下行流量
     */
    private final String TRAFFIC_ALL_DOWN_TABLE = "TRAFFIC_ALL_DOWN_TABLE";

    /**
     * 总上行流量
     */
    private final String TRAFFIC_ALL_UP_TABLE = "TRAFFIC_ALL_UP_TABLE";


    private final LinkedBlockingQueue<Context> reportQueue = new LinkedBlockingQueue<>(500);



    public void initConsumer() {
        Thread thread = new Thread(() -> {
            for(;;) {
                try {
                    Context context = reportQueue.poll(1, TimeUnit.SECONDS);
                    if (context != null) {
                        dataPartition.writeByTime(context, context.getUserName(), PartitionType.DAY);
                        long total = context.getUp() + context.getDown();
                        trafficPartition.writeOrMerge(TRAFFIC_USER_TABLE, context.getUserName(), total, PartitionType.DAY);
                        trafficPartition.writeOrMerge(TRAFFIC_ALL_DOWN_TABLE, TRAFFIC_ALL_DOWN_TABLE, context.getDown(), PartitionType.DAY);
                        trafficPartition.writeOrMerge(TRAFFIC_ALL_UP_TABLE, TRAFFIC_ALL_UP_TABLE, context.getUp(), PartitionType.DAY);
                        //trafficPartition.writeOrMerge(TRAFFIC_ALL_30_DAT_TABLE, TRAFFIC_ALL_30_DAT_TABLE, total, PartitionType.MONTH);
                        if (context.getClientIp() != null) {
                            connectionReport.reportAccessIp(context.getClientIp());
                        }
                        DbMangeService.getMangeDb().commit();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    @Override
    public void report(Context context) {
        if (context != null && context.getUserName() != null) {
            //日志可以丢，但是不能影响业务
            reportQueue.offer(context);
        }

    }


    @Override
    public Long getTotalUpTraffic(long currentTime, PartitionType partitionType) {
        if (partitionType == PartitionType.DAY) {
            return getSingleDayUpTraffic(currentTime);
        } else {
            List<Long> timestampsFromTodayToMonthStart = TimeUtil.getTimestampsFromTodayToMonthStart(currentTime);
            Long result = 0L;
            for (Long ago : timestampsFromTodayToMonthStart) {
                result += getSingleDayUpTraffic(ago);
            }
            return result;
        }
    }

    private Long getSingleDayUpTraffic(long time) {
        List<Long> query = trafficPartition.query(time, TRAFFIC_ALL_UP_TABLE);
        if (query == null || query.isEmpty()) {
            return 0L;
        }
        return query.get(0);
    }

    @Override
    public Long getTotalDownTraffic(long currentTime, PartitionType partitionType) {
        if (partitionType == PartitionType.DAY) {
            return getSingleDayDownTraffic(currentTime);
        } else {
            List<Long> timestampsFromTodayToMonthStart = TimeUtil.getTimestampsFromTodayToMonthStart(currentTime);
            Long result = 0L;
            for (Long ago : timestampsFromTodayToMonthStart) {
                result += getSingleDayDownTraffic(ago);
            }
            return result;
        }
    }

    private Long getSingleDayDownTraffic(long time) {
        List<Long> query = trafficPartition.query(time, TRAFFIC_ALL_DOWN_TABLE);
        if (query == null || query.isEmpty()) {
            return 0L;
        }
        return query.get(0);
    }

    @Override
    public Map<String, Long> getTop10Traffic(long currentTime, PartitionType partitionType) {
        if (partitionType == PartitionType.DAY) {
            return getSingleUserTraffic(currentTime);
        } else {
            List<Long> timestampsFromTodayToMonthStart = TimeUtil.getTimestampsFromTodayToMonthStart(currentTime);
            Map<String, Long> result = new HashMap<>();
            for (Long ago : timestampsFromTodayToMonthStart) {
                getSingleUserTraffic(ago).forEach((k,v) -> {
                    if (result.containsKey(k)) {
                        result.put(k, result.getOrDefault(k, 0L) + v);
                    } else {
                        result.put(k, v);
                    }
                });
            }
            return result;
        }
    }

    private Map<String, Long> getSingleUserTraffic(long currentTime) {
        return trafficPartition.queryMap(currentTime, TRAFFIC_USER_TABLE);
    }

    @Override
    public List<UserAccessEntity> getUserAccessDetail(long currentTime, String userName) {
        Map<String, Context> stringContextMap = dataPartition.queryMap(System.currentTimeMillis(), userName);
        Map<String, UserAccessEntity> result = new HashMap<>();

        // 累计访问次数、上行流量和下行流量
        for (Context value : stringContextMap.values()) {
            UserAccessEntity userAccessEntity = result.get(value.getRemoteUrl());
            if (userAccessEntity != null) {
                userAccessEntity.setAccessCount(userAccessEntity.getAccessCount() + 1);
                userAccessEntity.setUp(userAccessEntity.getUp() + value.getUp());
                userAccessEntity.setDown(userAccessEntity.getDown() + value.getDown());
                userAccessEntity.setDurationTime(userAccessEntity.getDurationTime() + value.getDurationTime());
                if (value.getStartTime() > userAccessEntity.getLastAccessTime()) {
                    userAccessEntity.setLastAccessTime(value.getStartTime());
                }
            } else {
                userAccessEntity = new UserAccessEntity();
                userAccessEntity.setAccessCount(1);
                userAccessEntity.setUp(value.getUp());
                userAccessEntity.setDown(value.getDown());
                userAccessEntity.setDurationTime(value.getDurationTime());
                userAccessEntity.setLastAccessTime(value.getStartTime());
                userAccessEntity.setRemoteUrl(value.getRemoteUrl());
                userAccessEntity.setClientIp(value.getClientIp());
                result.put(value.getRemoteUrl(), userAccessEntity);
            }
        }

        // 按访问次数降序排序并转换为 LinkedHashMap
        return result.values().stream().sorted(new Comparator<UserAccessEntity>() {
            @Override
            public int compare(UserAccessEntity o1, UserAccessEntity o2) {
                return (int) (-o1.getDown() + o2.getDown());
            }
        }).collect(Collectors.toList());
    }
}
