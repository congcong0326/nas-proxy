package org.congcong.nasproxy.common.util;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;

public class ThreadSafeBloomFilter {


    private static final BloomFilter<byte[]> filter = BloomFilter.create(
            Funnels.byteArrayFunnel(),
            10000000,
            0.01
    );

    public static synchronized boolean addElement(byte[] element) {
        // 先判断元素是否可能存在
        boolean mightContain = filter.mightContain(element);

        // 如果该元素不存在，则加入布隆过滤器
        if (!mightContain) {
            filter.put(element);
        }

        return mightContain;
    }

}
