package com.microvideoSecKill.controller;

import com.microvideoSecKill.domain.SecKillGoods;
import com.microvideoSecKill.error.code.BusinessErrCode;
import com.microvideoSecKill.redis.RedisPoolUtil;
import com.microvideoSecKill.result.ReturnResult;
import com.microvideoSecKill.rocketmq.AsyncProducer;
import com.microvideoSecKill.service.SecKillGoodsService;
import io.swagger.annotations.Api;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author John
 * @date 2021/12/30
 */
@Api(tags = "SecKill related API")
@RestController
@CrossOrigin()
@RequiredArgsConstructor
@RequestMapping("api/seckill")
@Slf4j
public class SecKillController implements InitializingBean {

    @Autowired
    SecKillGoodsService secKillGoodsService;

    @Autowired
    RedisPoolUtil redisPoolUtil;

    @Autowired
    AsyncProducer producer;

    // 把MySQL的所有商品goodId 对应的库存StockCount，提前加载到Redis缓存里
    @Override
    public void afterPropertiesSet() throws Exception {
        log.info("Initial the stockCount in Redis");

        List<SecKillGoods> listSecKillGoods = secKillGoodsService.getAllGoodsStockCount();
        for (SecKillGoods secKillItem : listSecKillGoods) {
            String key = secKillItem.getSecKillGoodsId();
            String value = secKillItem.getStockCount().toString();
            RedisPoolUtil.del(key);
            RedisPoolUtil.set(key, value);
            log.info("Loaded into Redis - Key: {}, Value: {}", key, value);
        }
    }

    /**
     * QPS:1306
     * 5000 * 10
     * QPS: 2114
     * */
    @RequestMapping(value="/do_seckill", method=RequestMethod.POST)
    @ResponseBody
    public ReturnResult<String> seckill(@RequestParam("userId") String userId,
                                        @RequestParam("goodsId") String goodsId) {
        // MySQL 减库存
        // 每次秒杀的商品（或红包）的数量为1
//        int buyCount = 1;
//        boolean success = secKillGoodsService.reduceStock(goodsId, buyCount);
//        if (success) {
//            log.info("SecKill Successful !");
//            return ReturnResult.setSuccessResult(goodsId);
//        } else {
//            log.error("SecKill failed !");
//            return ReturnResult.setFailResult(BusinessErrCode.SECKILL_FAILED_ERROR);
//        }


        // Redis 预减库存 (via Redis命令decr)
//        long stock = RedisPoolUtil.decr(goodsId);
//
//        if (stock < 0) {
//            log.error("SecKill failed !");
//            return ReturnResult.setFailResult(BusinessErrCode.SECKILL_FAILED_ERROR);
//        } else {
//            log.info("SecKill Successful !");
//            return ReturnResult.setSuccessResult(goodsId);
//        }


        // Redis 预减库存 （改进版 via Lua脚本）
        // 每次秒杀的商品（或红包）的数量为1
        int buyCount = 1;
        Long result = redisPoolUtil.evalsha(goodsId, Integer.toString(buyCount));
        log.info("Result of evalsha: {}  ( 1 - Successful, 0 - Failed )", result);
        // 如果脚本中返回的结果是0，表示失败，如果返回1，表示成功。
        if (result == 0) {
            log.error("SecKill failed !");
            return ReturnResult.setFailResult(BusinessErrCode.SECKILL_FAILED_ERROR);
        } else {
            log.info("SecKill Successful !");

            // 异步发送秒杀成功的订单到RocketMQ
            try {
                producer.sendMessageAsync(userId, goodsId, buyCount);
            } catch (Exception e) {
                log.error("Hit error in sending Async Message to RocketMQ - goodsId is {}," +
                        "userId is {}, Error: ", goodsId, userId, e);
            }

            return ReturnResult.setSuccessResult(goodsId);
        }
    }




    /**
     * MySQL 创建新的goodsId 和 库存数量stockCount
     * */
    @RequestMapping(value="/create_seckill", method=RequestMethod.POST)
    @ResponseBody
    public ReturnResult<String> createNewSecKillGoodsAndStockCount(
            @RequestParam("goodsId") String goodsId,
            @RequestParam("stockCount") Integer stockCount) {

        boolean success = secKillGoodsService.createNewSecKillGoodsAndStockCount(goodsId, stockCount);
        if (success) {
            log.info("Create New secKill and stockCount Successful !");
            return ReturnResult.setSuccessResult(goodsId);
        } else {
            log.error("Create New secKill and stockCount failed !");
            return ReturnResult.setFailResult(BusinessErrCode.SECKILL_FAILED_ERROR);
        }
    }

    /**
     * MySQL 重置库存数量stockCount
     * */
    @RequestMapping(value="/reset_seckill", method=RequestMethod.POST)
    @ResponseBody
    public ReturnResult<String> resetSecKillStockCount(
            @RequestParam("goodsId") String goodsId,
            @RequestParam("stockCount") Integer stockCount) {

        boolean success = secKillGoodsService.resetStockCount(goodsId, stockCount);
        if (success) {
            log.info("Reset SecKill stockCount Successful !");
            return ReturnResult.setSuccessResult(goodsId);
        } else {
            log.error("Reset SecKill stockCount failed !");
            return ReturnResult.setFailResult(BusinessErrCode.SECKILL_FAILED_ERROR);
        }
    }
}
