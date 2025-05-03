package org.congcong.nasproxy.core.monitor.db;

import java.util.List;
import java.util.Map;

public interface DataPartition<T> {



    void writeByTime(T t, String table, PartitionType partitionType);

    void writeByKey(String key, T value, String table, PartitionType partitionType);

    void writeOrMerge(String table, String key, Long value, PartitionType partitionType);

    List<T> query(long time, String table);


    Map<String, T> queryMap(long time, String table);

}
