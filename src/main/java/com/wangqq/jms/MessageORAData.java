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