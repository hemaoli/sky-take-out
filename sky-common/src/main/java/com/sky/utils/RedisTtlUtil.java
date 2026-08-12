package com.sky.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis缓存TTL随机工具:防止所有缓存同时过期导致雪崩
 */
public class RedisTtlUtil {

    /**
     * 生成1~24的随机整数
     * @return 1~24
     */
    public static int getRandomHour() {
        return ThreadLocalRandom.current().nextInt(1, 24 + 1);
    }

}
