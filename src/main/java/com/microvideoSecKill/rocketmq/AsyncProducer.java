package com.microvideoSecKill.rocketmq;

import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
//@Component
public class AsyncProducer {

    @Value("${rocketmq.producer.groupName}")
    private String groupName;

    @Value("${rocketmq.producer.namesrvAddr}")
    private String namesrvAddr;

    @Value("${rocketmq.producer.topic}")
    private String producerTopic;

    private DefaultMQProducer producer;

    @PostConstruct
    public void initMQProducer() throws MQClientException {
        // 指定生产组名为seckill-order-producer
        producer = new DefaultMQProducer(groupName);
        // 配置namesrv地址
        producer.setNamesrvAddr(namesrvAddr);
        // 启动Producer
        producer.start();
        System.out.println("生产者 Producer start");
    }

    public void sendMessageAsync(String userId, String goodsId, Integer buyCount) throws Exception {
        // 创建消息对象，topic 和 消息内容secKillMessage
        SecKillMessage secKillMessage = new SecKillMessage();
        secKillMessage.setUserId(userId);
        secKillMessage.setGoodsId(goodsId);
        secKillMessage.setBuyCount(buyCount);

        Message msg = new Message(producerTopic, JSON.toJSONString(secKillMessage).getBytes());

        // 进行异步发送，通过SendCallback接口来得知发送的结果
        producer.send(msg, new SendCallback() {
            // 发送成功的回调接口
            @Override
            public void onSuccess(SendResult sendResult) {
                Date date = new Date();
                System.out.println(date.toString() + " Producer 发送消息成功！Result is : " + sendResult);
            }

            // 发送失败的回调接口
            @Override
            public void onException(Throwable throwable) {
                throwable.printStackTrace();
                Date date = new Date();
                System.out.println(date.toString() + " Producer 发送消息失败！Result is : " + throwable.getMessage());
            }
        });
    }

    @PreDestroy
    public void shutDownProducer() {
        if (producer != null) {
            producer.shutdown();
            System.out.println("生产者 Producer shutdown");
        }
    }
}
