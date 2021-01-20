package com.wangqq.jms;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * @Title: MessageAQConfig.java
 * @Description: ORACLE 消息队列配置
 * @author wangqq
 * @date 2020年6月28日 下午3:36:08
 * @version 1.0
 */
@Component
public class MessageAQConfig {
    
    /** 是否开启MessageAq功能 */
    @Value("${queue.aq.enable}")
    public Boolean enable;
    
    /** 数据库用户名 */
	@Value("${spring.datasource.username}")
	public String userName;
	
	/** 数据库密码 */
	@Value("${spring.datasource.password}")
	public String password;
	
	/** 数据库地址url */
	@Value("${spring.datasource.url}")
	public String url;
	
	/** 队列名称 */
	@Value("${queue.aq.name}")
	public String queue;
}