package org.congcong.nasproxy.core.monitor.db.mapDb;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import org.congcong.nasproxy.core.monitor.db.PartitionType;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DbMangeService {

    @Getter
    private static final DB mangeDb;

    private static final Map<String, String> tables;

    private static final Map<String, Long> tablesNames;

    //private static final Map<String, Map<String, Object>> tableHolder;

    private static final Cache<String, Map<String, Object>> tableHolder;

    private static final ScheduledExecutorService scheduler;

     static {
        mangeDb = DBMaker.fileDB("manage-service.db").checksumHeaderBypass().transactionEnable()
                .make();
        tables = mangeDb.treeMap("tablesMange")
                .keySerializer(Serializer.STRING).valueSerializer(Serializer.STRING).createOrOpen();

        tablesNames = mangeDb.treeMap("tablesNameMange")
                 .keySerializer(Serializer.STRING).valueSerializer(Serializer.LONG).createOrOpen();

        tableHolder = CacheBuilder.newBuilder()
                .maximumSize(200)
                .expireAfterAccess(1, TimeUnit.DAYS)
                .build();

         // 定时任务，每天执行一次，删除 30 天前的数据
         scheduler = Executors.newSingleThreadScheduledExecutor();
         // 每天晚上12点执行任务
         long initialDelay = computeInitialDelay();
         long period = TimeUnit.DAYS.toMillis(1); // 每天执行一次
         scheduler.scheduleAtFixedRate(() -> {
             long currentTime = System.currentTimeMillis();
             long thirtyDaysAgo = currentTime - TimeUnit.DAYS.toMillis(30);
             // 遍历 tablesNames Map，删除创建时间超过30天的表
             for (Map.Entry<String, Long> entry : tablesNames.entrySet()) {
                 String tableName = entry.getKey();
                 Long creationTime = entry.getValue();
                 SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                 log.warn("table {} create time is {}",tableName, formatter.format(creationTime));
                 if (creationTime < thirtyDaysAgo) {
                     log.warn("Deleted table {} begin", tableName);
                     // 删除该表的数据
                     BTreeMap needDeleteMap = mangeDb.treeMap(tableName).keySerializer(Serializer.STRING).valueSerializer(Serializer.JAVA).createOrOpen();
                     needDeleteMap.clear();
                     tablesNames.remove(tableName);
                     log.warn("Deleted table {} success", tableName);
                 }
             }
             mangeDb.commit(); // 提交更改
         }, initialDelay, period, TimeUnit.MILLISECONDS);
    }


    public static PartitionType getPartitionType(String table) {
        String type = tables.get(table);
        if (type == null) {
            return PartitionType.UNKNOWN;
        }
        return PartitionType.valueOf(type);
    }

    public static Map<String, Object> createOrOpenTable(String table, String tableName, PartitionType partitionType) {
        Map<String, Object> result = tableHolder.getIfPresent(tableName);
        if (result != null) {
            return result;
        } else {
            synchronized (tableHolder) {
                if ((result = tableHolder.getIfPresent(tableName)) != null) {
                    return result;
                }
                tablesNames.put(tableName, System.currentTimeMillis());
                tables.put(table, partitionType.name());
                tableHolder.put(tableName, mangeDb.treeMap(tableName).keySerializer(Serializer.STRING).valueSerializer(Serializer.JAVA).createOrOpen());
            }
        }
        return tableHolder.getIfPresent(tableName);
    }


    // 计算距离今晚12点的初始延迟时间（单位：毫秒）
    private static long computeInitialDelay() {
        LocalTime midnight = LocalTime.MIDNIGHT;
        LocalTime now = LocalTime.now();
        long delay = Duration.between(now, midnight).toMillis();
        if (delay < 0) {
            // 如果当前时间已经过了午夜，设置延迟为第二天的午夜
            delay += TimeUnit.DAYS.toMillis(1);
        }
        return delay;
    }


  
}
