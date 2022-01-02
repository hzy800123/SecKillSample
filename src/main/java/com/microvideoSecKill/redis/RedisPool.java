package com.microvideoSecKill.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


@Slf4j
public class RedisPool {
    private static JedisPool pool;  // Jedis 连接池
    private static Integer maxTotal = 20;   // 最大连接数
    private static Integer maxIdle = 10;    // 在JedisPool中最大的idle状态（空闲的）的Jedis实例的个数
    private static Integer minIdle = 2;     // 在JedisPool中最小的idle状态（空闲的）的Jedis实例的个数

    private static Boolean testOnBorrow = true;
    private static Boolean testOnReturn = true;

    private static String redisIp = "localhost";
    private static Integer redisPort = 6379;

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();;

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);
        config.setBlockWhenExhausted(true);

        pool = new JedisPool(config, redisIp, redisPort, 1000*2);
    }

    static {
        initPool();
    }

    public static Jedis getJedis() {
        return pool.getResource();
    }

    public static void returnBrokenResource(Jedis jedis) {
        pool.returnBrokenResource(jedis);
    }

    public static void returnResource(Jedis jedis) {
        pool.returnResource(jedis);
    }

    // For Test Only
    public static void main(String[] args) {
        Jedis jedis = pool.getResource();
        jedis.set("testKey", "testValue");
        returnResource(jedis);
        pool.destroy();

        log.info("Set testKey and testValue done !");
    }
}
