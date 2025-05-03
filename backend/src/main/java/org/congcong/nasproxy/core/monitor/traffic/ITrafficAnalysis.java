package org.congcong.nasproxy.core.monitor.traffic;


import org.congcong.nasproxy.common.entity.UserAccessEntity;
import org.congcong.nasproxy.core.monitor.db.PartitionType;

import java.util.List;
import java.util.Map;

public interface ITrafficAnalysis {

    /**
     * 从currentTime倒推一天或者一个月的上行流量
     */
    Long getTotalUpTraffic(long currentTime, PartitionType partitionType);

    /**
     * 从currentTime倒推一天或者一个月的下行流量
     */
    Long getTotalDownTraffic(long currentTime, PartitionType partitionType);

    /**
     * 从currentTime倒推一天或者一个月的用户流量排序
     * key 为用户名
     * value 为流量
     * @param partitionType
     * @return
     */
    Map<String, Long> getTop10Traffic(long currentTime, PartitionType partitionType);

    /**
     * 统计人访问应用次数的明细与流量
     * @return
     */
    List<UserAccessEntity> getUserAccessDetail(long currentTime, String userName);

}
