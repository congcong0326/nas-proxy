package org.congcong.nasproxy.core.monitor.db.mapDb;


import org.congcong.nasproxy.core.monitor.db.DataPartition;
import org.congcong.nasproxy.core.monitor.db.PartitionType;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

public class DataPartitionImpl<T> implements DataPartition<T> {

    private static final AtomicLong counter = new AtomicLong();


    private String buildTableName(String table, long partition) {
        return table + partition;
    }


    private String getUniqueKey(long currentTimeMillis) {
        long sequence = counter.getAndIncrement() % 1000;
        return currentTimeMillis + "_" + sequence;
    }

    @Override
    public void writeByTime(T t, String table, PartitionType partitionType) {
        long currentTimeMillis = System.currentTimeMillis();
        long partition = mod(currentTimeMillis, partitionType);
        Map<String, Object> tableObject = DbMangeService.createOrOpenTable(table, buildTableName(table, partition), partitionType);
        tableObject.put(getUniqueKey(currentTimeMillis), t);
    }

    @Override
    public void writeByKey(String key, T value, String table, PartitionType partitionType) {
        long currentTimeMillis = System.currentTimeMillis();
        long partition = mod(currentTimeMillis, partitionType);
        Map<String, Object> tableObject = DbMangeService.createOrOpenTable(table, buildTableName(table, partition), partitionType);
        tableObject.put(key, value);
    }

    @Override
    public void writeOrMerge(String table, String key, Long value, PartitionType partitionType) {
        long partition = mod(System.currentTimeMillis(), partitionType);
        Map<String, Object> data = DbMangeService.createOrOpenTable(table, buildTableName(table, partition), partitionType);
        data.put(key, (Long) data.getOrDefault(key, 0L) + value);
    }


    @Override
    public List<T> query(long time, String table) {
        PartitionType partitionType = DbMangeService.getPartitionType(table);
        if (partitionType == PartitionType.UNKNOWN) {
            return Collections.emptyList();
        }
        long partition = mod(time, partitionType);
        Map<String, Object> data = DbMangeService.createOrOpenTable(table, buildTableName(table, partition), DbMangeService.getPartitionType(table));
        List<T> result = new ArrayList<>(data.size());
        for (Object value : data.values()) {
            result.add((T) value);
        }
        return result;
    }


    @Override
    public Map<String, T> queryMap(long time, String table) {
        PartitionType partitionType = DbMangeService.getPartitionType(table);
        if (partitionType == PartitionType.UNKNOWN) {
            return Collections.emptyMap();
        }
        long partition = mod(time, partitionType);
        Map<String, Object> data = DbMangeService.createOrOpenTable(table, buildTableName(table, partition), DbMangeService.getPartitionType(table));
        return (Map<String, T>) data;
    }


    // 计算按不同时间单位取模
    private long mod(long timestamp, PartitionType partitionType) {
        switch (partitionType) {
//            case HOUR:
//                return timestamp / (1000 * 60 * 60); // 按小时取模
            case DAY:
                return convertToDateOnlyTimestamp(timestamp);
                //return timestamp  / (1000 * 60 * 60 * 24); // 按天取模
            case MONTH:
                return getMonthMod(timestamp); // 按月取模
            default:
                throw new UnsupportedOperationException("Unsupported time unit");
        }
    }

    private long convertToDateOnlyTimestamp(long timestamp) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = new Date(timestamp);
            String formattedDate = sdf.format(date);  // 格式化为 yyyy-MM-dd

            // 将格式化后的日期字符串转换为时间戳
            Date parsedDate = sdf.parse(formattedDate);
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(parsedDate);
            calendar.set(Calendar.HOUR_OF_DAY, 0);  // 清除时分秒
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);

            return calendar.getTimeInMillis();  // 返回清除时分秒后的时间戳
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date format", e);
        }
    }

    // 按月取模
    private long getMonthMod(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH); // MONTH: 0-11, so we need to adjust
        return (year * 12 + month); // 使用年份和月份来唯一标识一个月
    }
}
