package com.wangqq.jms;

import javax.jms.Message;
import javax.jms.MessageListener;

import org.springframework.stereotype.Component;

import com.wangqq.bean.Test;

import lombok.extern.slf4j.Slf4j;
import oracle.jms.AQjmsAdtMessage;

/**
 * @Title: JMSListener.java
 * @Description: JMS监听ORACLEAQ的队列消息
 * @author wangqq
 * @date 2020年6月28日 上午11:23:42
 * @version 1.0
 */
@Slf4j
@Component
public class MessageAQListener implements MessageListener {

    
    @Override
    public void onMessage(Message message1) {
        AQjmsAdtMessage adtMessage = (AQjmsAdtMessage)message1;
        try {
            MessageORAData payload = (MessageORAData)adtMessage.getAdtPayload();
            // 获取消息内容
            Test test = payload.getContent();
            
            System.out.println(test.toString());
            
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
