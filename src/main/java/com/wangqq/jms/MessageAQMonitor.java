package com.wangqq.jms;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.wangqq.mapper.MessageAqMapper;
import com.wangqq.util.DateUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @Title: MessageAQMonitor.java
 * @Description: AQ 监控任务, 在AQ断开后重连
 * @author wangqq
 * @date 2020年6月28日 下午4:35:31
 * @version 1.0
 */
@Slf4j
@Component
public class MessageAQMonitor {

    @Autowired
    private MessageAQConfig aqConfig;
    @Autowired
    private MessageAqMapper aqMapper;

    @Scheduled(cron = "${queue.aq.cron}")
    private void monitorJob() {
        // 检查消息队列是否启用
        if (!aqConfig.enable) {
            return;
        }
        // 获取当前时间，并向前推5分钟
        String formatDateTime = DateUtil.formatDate(new Date(System.currentTimeMillis() - 300000));
        // 将该时间转为0时区的时间【数据库中存储的队列时间为0时区的时间】
        String zeroZoneTime = DateUtil.timeConvert(formatDateTime, "+08:00", "+00:00", "yyyy-MM-dd HH:mm:ss");
        // 查询是否存在5分钟以前的队列未被消费
        int selectCount = aqMapper.selectCount(aqConfig.queue, zeroZoneTime);
        if (selectCount != 0) {
            // 若存在，则重新启动监听
            if (MessageAQConnection.closeConnection()) {
                log.info("--> AQ connection has been closed.");
                if (MessageAQConnection.establishConnection(aqConfig)) {
                    log.info("--> AQ connection has been re-established.");
                }
            }
        }
    }

}