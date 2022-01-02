package com.microvideoSecKill.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import javax.annotation.PostConstruct;


@Component
@Slf4j
public class RedisPoolUtil {


    /**
     * 设置key的有效期，单位是秒
     * @param key
     * @param exTime
     * @return
     */
    public static Long expire(String key,int exTime){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.expire(key,exTime);
        } catch (Exception e) {
            log.error("expire key:{} error",key,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    //exTime的单位是秒
    public static String setEx(String key,String value,int exTime){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.setex(key,exTime,value);
        } catch (Exception e) {
            log.error("setex key:{} value:{} error",key,value,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static String set(String key,String value){
        Jedis jedis = null;
        String result = null;

        try {
            jedis = RedisPool.getJedis();
            result = jedis.set(key,value);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static String get(String key){
        Jedis jedis = null;
        String result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.get(key);
        } catch (Exception e) {
            log.error("get key:{} error",key,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static String getSet(String key, String value) {
        Jedis jedis = null;
        String result = null;

        try {
            jedis = RedisPool.getJedis();
            result = jedis.getSet(key, value);
        } catch (Exception e) {
            log.error("set key:{} value:{} error",key,value,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static Long del(String key){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.del(key);
        } catch (Exception e) {
            log.error("del key:{} error",key,e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static Long setnx(String key, String value){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.setnx(key, value);
        } catch (Exception e) {
            log.error("setnx key:{} value:{} error", key, value, e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    public static Long decr(String key){
        Jedis jedis = null;
        Long result = null;
        try {
            jedis = RedisPool.getJedis();
            result = jedis.decr(key);
        } catch (Exception e) {
            log.error("decrease key:{} error", key, e);
            RedisPool.returnBrokenResource(jedis);
            return result;
        }
        RedisPool.returnResource(jedis);
        return result;
    }

    /**
     * Lua逻辑：
     * 首先判断活动的库存是否存在，以及库存的余量是否够本次的购买数量，     *
     * 如果不够，则返回0，
     * 如果够，则完成扣减并返回1。
     *
     * 2个入参：
     * KEYS[1] : 活动库存的Key
     * KEYS[2] : 活动库存的扣减量
     */
    private String STOCK_DEDUCTION_SCRIPT_LUA =
            // 调用Redis的get指令，查询活动库存，其中KEYS[1]为传入的参数1，即库存key
            "local c_s = redis.call('get', KEYS[1])\n" +
            // 判断活动库存是否存在，并是否充足，其中KEYS[2]为传入的参数2，即当前抢购数量
            "if not c_s or tonumber(c_s) < tonumber(KEYS[2]) then\n" +
            "return 0\n" +
            "end\n" +
            // 如果活动库存充足，则进行扣减操作。其中KEYS[2]为传入的参数2，即当前抢购数量
            "redis.call('decrby',KEYS[1], KEYS[2])\n" +
            "return 1";


    private String STOCK_DEDUCTION_SCRIPT_SHA1 = "";

    /**
     * 在系统启动时，将脚本预加载到Redis中，并返回一个加密的字符串，下次只要传该加密的字符串，即可执行对应的脚本，
     * 减少了Redis的预编译。
     */
    @PostConstruct
    public void init() {
        try (Jedis jedis = RedisPool.getJedis()) {
            String sha1 = jedis.scriptLoad(STOCK_DEDUCTION_SCRIPT_LUA);
            log.info("Generated SHA1: {}", sha1);
            STOCK_DEDUCTION_SCRIPT_SHA1 = sha1;
        }
    }

    /**
     * 调用Lua脚本，不需要每次都传入Lua脚本，只需要传入预编译返回的sha1即可。
     * @param key - The SecKill Goods ID
     * @param buyCount - The buy count of SecKill Goods
     * @return 执行脚本之后的结果0或1
     */
    public Long evalsha(String key, String buyCount) {
        try (Jedis jedis = RedisPool.getJedis()) {
            // 2个入参：KEYS[1] : 活动库存的Key, KEYS[2] : 活动库存的扣减量
            // keyCount = 2
            Object obj = jedis.evalsha(STOCK_DEDUCTION_SCRIPT_SHA1, 2, key, buyCount);
            // 如果脚本中返回的结果是0，表示失败，如果返回1，表示成功。
            return (Long) obj;
        }
    }


    // For Test Only
    public static void main(String[] args) {
        log.info("RedisPoolUtil Test Start...");

        RedisPoolUtil redisPoolUtil = new RedisPoolUtil();
        redisPoolUtil.init();
        Long result = redisPoolUtil.evalsha("20211230_RedPaper_Round3", "1");
        // 如果脚本中返回的结果是0，表示失败，如果返回1，表示成功。
        log.info("Result of evalsha: {}", result);

//        RedisPoolUtil.set("keyTest2","valueTest2");
//
//        String value = RedisPoolUtil.get("keyTest2");
//
//        RedisPoolUtil.setEx("keyEx","valueEx",10*1);
//
//        RedisPoolUtil.expire("keyTest2",20*1);
//
//        RedisPoolUtil.del("keyTest2");
//
//        String aaa = RedisPoolUtil.get(null);
//        System.out.println(aaa);

        log.info("RedisPoolUtil Test End...");
    }


}
