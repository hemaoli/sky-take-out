package com.sky.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Redis缓存TTL随机工具:防止所有缓存同时过期导致雪崩
 */
public class RedisTtlUtil {

    /**
     * 生成55~65分钟的随机整数
     * @return 55-65
     */
    public static int getRandomMinute() {
        return ThreadLocalRandom.current().nextInt(55, 66);
    }

}
