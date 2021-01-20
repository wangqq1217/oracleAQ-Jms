package com.wangqq.jms;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * @Title: MessageAQInit.java
 * @Description: AQ 初始化
 * @author wangqq
 * @date 2020年6月28日 下午3:45:23
 * @version 1.0
 */
@Component
public class MessageAQInit implements CommandLineRunner {

    @Autowired
    private MessageAQConfig aqConfig;
    @Autowired
    private MessageAQListener listener;

    @Override
    public void run(String... args) throws RuntimeException {
        // 检查消息队列是否启用
        if (aqConfig.enable) {
            // 设置AQ的消息监听器
            MessageAQConnection.setListener(listener);
            // 初始化AQ连接
            if (!MessageAQConnection.initFactory(aqConfig)) {
                throw new RuntimeException("Message Oracle AQ initialization failed!");
            }
            // 建立连接
            if (!MessageAQConnection.establishConnection(aqConfig)) {
                throw new RuntimeException("Message Oracle AQ connection failed!");
            }
        }
    }
}