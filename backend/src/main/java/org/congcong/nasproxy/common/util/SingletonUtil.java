package org.congcong.nasproxy.common.util;

import java.util.HashMap;
import java.util.Map;

public class SingletonUtil {

    private static final Map<Class<?>, Object> instances = new HashMap<>();



    private SingletonUtil() {
    }

    // 获取单例实例的方法
    public static <T> T getInstance(Class<T> clazz) {
        // 首先检查是否已存在实例
        Object o = instances.get(clazz);
        if (o == null) {
            // 使用双重锁定来保证线程安全
            synchronized (instances) {
                o = instances.get(clazz);
                if (o == null) {
                    try {
                        o = clazz.getDeclaredConstructor().newInstance();
                        instances.put(clazz, o);
                    } catch (Exception e) {
                        throw new RuntimeException("Error creating singleton instance", e);
                    }
                }
            }
        }
        return clazz.cast(o);
    }



}