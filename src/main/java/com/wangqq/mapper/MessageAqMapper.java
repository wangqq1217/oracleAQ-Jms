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
     * @param minDatetime   队列发布的最小时间
     * @return
     * @author wangqq
     * @date 2020-07-10 15:44:43
     */
    @Select("select count(msgid) from T_QUEUE_TABLE t where t.q_name = #{qName,jdbcType=VARCHAR} "
            + "and to_char(cast(t.enq_time AS DATE), 'yyyy-MM-dd HH24:mi:ss') < #{minDatetime,jdbcType=VARCHAR}")
    int selectCount(String qName, String minDatetime);

}
