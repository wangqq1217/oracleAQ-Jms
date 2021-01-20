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
