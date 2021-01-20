package com.wangqq.jms;

import javax.jms.Queue;
import javax.jms.Session;

import lombok.extern.slf4j.Slf4j;
import oracle.jms.AQjmsConnection;
import oracle.jms.AQjmsConnectionFactory;
import oracle.jms.AQjmsConsumer;
import oracle.jms.AQjmsSession;

/**
 * @Title: MessageAQConnection.java
 * @Description: AQ 连接
 * @author wangqq
 * @date 2020年6月28日 下午3:50:32
 * @version 1.0
 */
@Slf4j
public class MessageAQConnection {

    private static AQjmsConnectionFactory aQjmsConnectionFactory;

    private static AQjmsConsumer aQjmsConsumer;

    private static AQjmsSession aQjmsSession;

    private static AQjmsConnection aQjmsConnection;

    private static MessageAQListener listener;

    /**
     * 设置JMS监听器
     * 
     * @param messageAqJmsListener
     * @author wangqq
     * @date 2020年7月6日 上午8:33:57
     */
    public static void setListener(MessageAQListener messageAqJmsListener) {
        listener = messageAqJmsListener;
    }

    /**
     * 初始化 AQ 连接 Factory
     *
     * @param aqConfig 消息队列配置
     * @return 是否成功
     */
    public static boolean initFactory(MessageAQConfig aqConfig) {
        try {
            aQjmsConnectionFactory = new AQjmsConnectionFactory();
            aQjmsConnectionFactory.setJdbcURL(aqConfig.url);
            aQjmsConnectionFactory.setUsername(aqConfig.userName);
            aQjmsConnectionFactory.setPassword(aqConfig.password);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 连接消息队列
     *
     * @param aqConfig 消息队列配置
     * @return 是否成功
     */
    public static boolean establishConnection(MessageAQConfig aqConfig) {
        try {
            aQjmsConnection = (AQjmsConnection) aQjmsConnectionFactory.createConnection();
            aQjmsSession = (AQjmsSession) aQjmsConnection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            aQjmsConnection.start();
            Queue queue = aQjmsSession.getQueue(aqConfig.userName, aqConfig.queue);
            aQjmsConsumer = (AQjmsConsumer) aQjmsSession.createConsumer(queue, null, MessageORAData.getFactory(), null,
                    false);
            aQjmsConsumer.setMessageListener(listener);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * 关闭消息队列连接
     *
     * @return 是否成功
     */
    public static boolean closeConnection() {
        try {
            aQjmsConsumer.close();
            aQjmsSession.close();
            aQjmsConnection.close();
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }
}