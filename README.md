- 该文档中，jdk版本1.8，java项目为maven构建的springboot项目，并使用了定时任务来做AQ监听的重连功能，解决由于外部原因导致连接断裂之后，需要手动重启项目才能恢复连接的问题

# 一、创建队列

## 1.1.管理员登录执行

- 管理员登录，执行授权操作，oracle使用队列需要单独的授权，默认未开启，须手动开启，授权命令如下，username使用自己的用户名即可

```sql
GRANT EXECUTE ON SYS.DBMS_AQ to 'username';
GRANT EXECUTE ON SYS.DBMS_AQADM to 'username';
GRANT EXECUTE ON SYS.DBMS_AQ_BQVIEW to 'username';
GRANT EXECUTE ON SYS.DBMS_AQIN to 'username';
GRANT EXECUTE ON SYS.DBMS_JOB to 'username';
```

## 1.2.用户登录执行执行

### 1.2.1. 创建消息负荷payload

- 创建的此type用来封装队列所带的，根据实际需求进行创建

```sql
CREATE OR REPLACE TYPE TYPE_QUEUE_INFO AS OBJECT
(
  param_1             VARCHAR2(100),
  param_2             VARCHAR2(100)
)
```

### 1.2.2. 创建队列表

- 创建对列表，并指定队列数据的类型，队列表名自定义即可，数据类型使用上面刚创建的type

```sql
begin
  sys.dbms_aqadm.create_queue_table(
    queue_table => 'QUEUE_TABLE',
    queue_payload_type => 'TYPE_QUEUE_INFO',
    sort_list => 'ENQ_TIME',
    compatible => '10.0.0',
    primary_instance => 0,
    secondary_instance => 0);
end;
```

### 1.2.3. 创建队列并启动

- 创建名称为QUEUE_TEST的队列，并指定对列表名【同一个oracle用户下，可以有多个对列表，同一个对列表中，可以有多个队列】

```sql
begin
  sys.dbms_aqadm.create_queue(
    queue_name => 'QUEUE_TEST',
    queue_table => 'QUEUE_TABLE',
    queue_type => sys.dbms_aqadm.normal_queue,
    max_retries => 5,
    retry_delay => 0,
    retention_time => 0);
end;
```
- 刚创建的队列的状态默认是未开启的，需要手动开启一下，同理，存在删除、停止等操作

```sql
begin
  -- 启动队列
  sys.dbms_aqadm.start_queue(
      queue_name => 'QUEUE_TEST'
  );
  
  -- 暂停队列
  --sys.dbms_aqadm.STOP_QUEUE(
  --    queue_name => 'QUEUE_TEST'
  --);
  
  -- 删除队列
  --sys.dbms_aqadm.DROP_QUEUE(
  --    queue_name => 'QUEUE_TEST'
  --);
  
  -- 删除对列表
  --sys.dbms_aqadm.DROP_QUEUE_TABLE(
  --    queue_table => 'QUEUE_TABLE'
  --);
end;
```

### 1.2.4. 创建存储过程

- 储存过程的作用为把数据加载到队列中，生成的新的队列会自动添加进绑定的对列表中，等待消费者进行消费

```sql
CREATE OR REPLACE PROCEDURE pro_queue(param_1 VARCHAR2, param_2 VARCHAR2) as
  r_enqueue_options    DBMS_AQ.ENQUEUE_OPTIONS_T;
  r_message_properties DBMS_AQ.MESSAGE_PROPERTIES_T;
  v_message_handle     RAW(16);
  o_payload            TYPE_QUEUE_INFO;
begin
  -- 封装最终消息
  o_payload := TYPE_QUEUE_INFO(param_1, param_2);
  -- 入队操作，指定队列
  dbms_aq.enqueue(queue_name         => 'QUEUE_TEST',
                  enqueue_options    => r_enqueue_options,
                  message_properties => r_message_properties,
                  payload            => o_payload,
                  msgid              => v_message_handle);

  -- 出队操作
  --dbms_aq.enqueue(queue_name => 'QUEUE_TEST',
  --                dequeue_options => r_dequeue_options,
  --                message_properties => r_message_properties,
  --                payload => o_payload,
  --                msgid => v_message_handle);
end pro_queue;
```

# 二、Java中JMS的使用

## 2.1. 项目配置

### 2.1.1. maven

```xml
<dependency>
      <groupId>com.oracle</groupId>
      <artifactId>jmscommon</artifactId>
      <version>1.2</version>
</dependency>
<dependency>
	<groupId>com.oracle</groupId>
	<artifactId>orai18n</artifactId>
	<version>1.2</version>
</dependency>
<dependency>
	<groupId>com.oracle</groupId>
	<artifactId>jta</artifactId>
	<version>1.2</version>
</dependency>
<dependency>
	<groupId>com.oracle</groupId>
	<artifactId>aqapi_g</artifactId>
	<version>1.2</version>
</dependency>
```
### 2.1.2. yml

~~~yaml
spring:
  datasource:
    url: jdbc:oracle:thin:@ip:port/sid
    username: **
    password: **    
queue:
  aq:
    # 该队列是否可用，用来控制队列的加载和重连，不可省略
    enable: true
    # 队列名称，不可省略
    name: QUEUE_TEST
    # 队列重连的定时任务对应的时间表达式，不可省略
    cron: 0 */1 * * * ?
~~~

## 2.2. AQ初始化

- 在项目启动结束后立即运行此类，会根据所配置的队列名称监听对应的队列

```java
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
```

## 2.3. 配置信息类

- 配置类，将yml的配置文件转为java对象【时间表达式在代码中不会以对象属性的方式被使用，因此在该类中没有设置】

~~~java
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
~~~

## 2.4. AQ 连接工厂类

- AQ 链接的核心类，根据配置对象以及注入的监听对象，动态监听AQ队列

~~~java
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
~~~

## 2.5. 创建AQ 数据承载类

- 用来接收oracle队列中所带的参数，基本保证与数据库中的type格式相同即可

~~~java
package com.wangqq.bean;

import lombok.Builder;
import lombok.Data;

/**
 * @Title: Test.java
 * @Description: AQ 数据承载类
 * @author wangqq
 * @date 2021-01-20 16:19:16
 * @version 1.0
 */
@Data
@Builder
public class Test {
    
    private String param_1;
    
    private String param_2;
    
}
~~~

## 2.6. 数据类型转换

- 将oracleAq所承载的数据，转化为我们自己需要的实例对象，及上述中的Test对象

~~~java
package com.wangqq.jms;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Struct;

import com.wangqq.bean.Test;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import oracle.sql.Datum;
import oracle.sql.ORAData;
import oracle.sql.ORADataFactory;

/**
 * @Title: MessageORAData.java
 * @Description: 数据类型转换类
 * @author synjones
 * @date 2018年12月3日 上午11:29:50
 * @version 1.0
 */
@Slf4j
@NoArgsConstructor
public class MessageORAData implements ORAData, ORADataFactory {

    private Object[] rawData = new Object[8];
    
    private static final MessageORAData MESSAGE_FACTORY = new MessageORAData();

    public static ORADataFactory getFactory() {
        return MESSAGE_FACTORY;
    }

    @Override
    public ORAData create(Datum datum, int sqlType) throws SQLException {
        if (datum == null) {
            return null;
        } else {
            try {
                MessageORAData payOraData = new MessageORAData();
                Struct aStruct = (Struct) datum;
                payOraData.rawData = aStruct.getAttributes();
                return payOraData;
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        }
    }

    @Override
    public Datum toDatum(Connection arg0) throws SQLException {
        return null;
    }

    /**
     * 消息内容解析并封装
     * 
     * @return
     * @author wangqq
     * @date 2020年7月6日 上午8:38:01
     */
    public Test getContent() {
        try {
            return Test.builder()
                .param_1(rawData[0] == null ? null : rawData[0].toString())
                .param_2(rawData[0] == null ? null : rawData[0].toString())
                .build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }
}
~~~

## 2.7. AQ 监听

~~~java
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
~~~

## 2.8. AQ 监控任务, 在AQ断开后重连

- 通过定时任务，定时查询是否有入队时间在5分钟之内的队列未被消费【队列入队后，会在对列表中产生一条数据，消费之后该数据会被清除掉】，若存在，则说明监听异常，需要重新创建连接监听队列
- 数据库对列表中的入队时间在本次测试中为0时区的时间，故而在代码中转换了一下时区，否则无法根据入队时间查询数据

~~~java
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
~~~

## 2.9. 队列表中队列数量的查询

- 根据队列名称和入队时间，查询在入队时间之后入对的队列数量

~~~java
package com.wangqq.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * @Title: MessageAqMapper.java
 * @Description: oracleAQ的查询
 * @author wangqq
 * @date 2020年6月28日 下午4:04:50
 * @version 1.0
 */
@Mapper
public interface MessageAqMapper {

    /**
     * 
     * 查询数据库中的队列表中符合条件的队列的条数
     *
     * @param qName         队列名称
     * @param minDatetime   队列入队的最小时间
     * @return
     * @author wangqq
     * @date 2020-07-10 15:44:43
     */
    @Select("select count(msgid) from T_QUEUE_TABLE t where t.q_name = #{qName,jdbcType=VARCHAR} "
            + "and to_char(cast(t.enq_time AS DATE), 'yyyy-MM-dd HH24:mi:ss') < #{minDatetime,jdbcType=VARCHAR}")
    int selectCount(String qName, String minDatetime);

}
~~~

## 2.10. 日期工具类

```java
package com.wangqq.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * @Title: DateUtil.java
 * @Description: 日期工具类
 * @author wangqq
 * @date 2018年10月29日 下午5:27:21
 * @version 1.0
 */
public class DateUtil {

	/**
	 * 字符串转date,默认格式yyyy-MM-dd HH:mm:ss
	 * 
	 * @param source
	 * @return
	 */
	public static Date parseDate(String source) {
		return parseDate(source, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 字符串转date
	 * 
	 * @param source
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static Date parseDate(String source, String pattern) {
		if (source == null || source.equals("")) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		try {
			return sdf.parse(source);
		} catch (ParseException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 格式化日期,默认格式yyyy-MM-dd HH:mm:ss
	 * 
	 * @param date
	 * @return
	 */
	public static String formatDate(Date date) {
		return formatDate(date, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 格式化日期
	 * 
	 * @param date
	 * @param pattern
	 *            格式
	 * @return
	 */
	public static String formatDate(Date date, String pattern) {
		if (date == null) {
			return null;
		}
		SimpleDateFormat sdf = new SimpleDateFormat(pattern);
		return sdf.format(date);
	}

	/**
     * 时区 时间转换方法:将传入的时间（可能为其他时区）转化成目标时区对应的时间
     * @param sourceTime 时间格式必须为：yyyy-MM-dd HH:mm:ss
     * @param sourceId 入参的时间的时区id 比如：+08:00
     * @param targetId 要转换成目标时区id 比如：+09:00
     * @param reFormat 返回格式 默认：yyyy-MM-dd HH:mm:ss
     * @return string 转化时区后的时间
     */
    public static String timeConvert(String sourceTime, String sourceId,
            String targetId,String reFormat){
        //校验入参是否合法
        if (null == sourceId || "".equals(sourceId) || null == targetId
                || "".equals(targetId) || null == sourceTime
                || "".equals(sourceTime)){
            return null;
        }
        
        if(reFormat == null || "".equals(reFormat)){
            reFormat = "yyyy-MM-dd HH:mm:ss";
        }
        
        //校验 时间格式必须为：yyyy-MM-dd HH:mm:ss
        String reg = "^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$";
        if (!sourceTime.matches(reg)){
            return null;
        }
        
        try{
            //时间格式
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            //根据入参原时区id，获取对应的timezone对象
            TimeZone sourceTimeZone = TimeZone.getTimeZone("GMT"+sourceId);
            //设置SimpleDateFormat时区为原时区（否则是本地默认时区），目的:用来将字符串sourceTime转化成原时区对应的date对象
            df.setTimeZone(sourceTimeZone);
            //将字符串sourceTime转化成原时区对应的date对象
            java.util.Date sourceDate = df.parse(sourceTime);
            
            //开始转化时区：根据目标时区id设置目标TimeZone
            TimeZone targetTimeZone = TimeZone.getTimeZone("GMT"+targetId);
            //设置SimpleDateFormat时区为目标时区（否则是本地默认时区），目的:用来将字符串sourceTime转化成目标时区对应的date对象
            df.setTimeZone(targetTimeZone);
            //得到目标时间字符串
            String targetTime = df.format(sourceDate);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            java.util.Date date = sdf.parse(targetTime);
            sdf = new SimpleDateFormat(reFormat);
            
            return sdf.format(date);
        }
        catch (ParseException e){
            e.printStackTrace();
        }
        return null;
    }
}
```

