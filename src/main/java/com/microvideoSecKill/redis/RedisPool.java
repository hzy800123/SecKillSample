package com.microvideoSecKill.redis;

import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.*;
import redis.clients.util.Hashing;
import redis.clients.util.Pool;
import redis.clients.util.Sharded;

import java.util.*;


@Slf4j
public class RedisPool {
    /**
     * If use Redis Sentinel (JedisSentinelPool), set it to 'true'.
     * If use single Redis, set it to 'false'.
     */
    private static boolean jedisSentinelMode = false;

    private static JedisPool jedisPool;  // Jedis 连接池
    private static JedisSentinelPool jedisSentinelPool;  // Jedis Sentinel 连接池
    private static Pool<Jedis> pool;

    private static Integer maxTotal = 10;   // 最大连接数
    private static Integer maxIdle = 10;    // 在JedisPool中最大的idle状态（空闲的）的Jedis实例的个数
    private static Integer minIdle = 2;     // 在JedisPool中最小的idle状态（空闲的）的Jedis实例的个数

    private static Boolean testOnBorrow = true;
    private static Boolean testOnReturn = true;

    private static String redisIp = "localhost";
    private static Integer redisPort = 6379;

    private static void initPool() {
        JedisPoolConfig config = new JedisPoolConfig();

        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        config.setMinIdle(minIdle);

        config.setTestOnBorrow(testOnBorrow);
        config.setTestOnReturn(testOnReturn);
        config.setBlockWhenExhausted(true);

//        JedisShardInfo jedisShardInfo1 = new JedisShardInfo(redisIp, redisPort, 1000*2);
//        JedisShardInfo jedisShardInfo2 = new JedisShardInfo(redisIp, redisPort, 1000*2);
//        List<JedisShardInfo> jedisShardInfoList = new ArrayList<JedisShardInfo>();
//        jedisShardInfoList.add(jedisShardInfo1);
//        jedisShardInfoList.add(jedisShardInfo2);
//
//        ShardedJedisPool sharedJedispool = new ShardedJedisPool(
//                config, jedisShardInfoList, Hashing.MURMUR_HASH, Sharded.DEFAULT_KEY_TAG_PATTERN);
//        ShardedJedis

        if(jedisSentinelMode) {
            //哨兵的配置信息(IP and Port)
            Set<String> sentinels = new HashSet<String>(Arrays.asList(
                    "127.0.0.1:26379",
                    "127.0.0.1:26380",
                    "127.0.0.1:26381"
            ));
            //创建连接池
            //mymaster是我们配置给哨兵的服务名称
            //sentinels是哨兵配置信息
            //config
            //客户端通过哨兵的配置信息，去连接哨兵，然后获取信息。
            jedisSentinelPool = new JedisSentinelPool("mymaster",
                    sentinels,
                    config,
                    1000*5);
            pool = jedisSentinelPool;
        } else {
            log.info("Connected to Redis Server with port :{}", redisPort);
            jedisPool = new JedisPool(config, redisIp, redisPort, 1000*5);
            pool = jedisPool;
        }
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
