/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.ticketservice.toolkit;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * 日期工具类
 *
 * @author zhangjlk
 * @date 2025/12/25 下午6:16
 */
@Slf4j
public class DateUtil {

    /**
     * 计算两个时间之间的差值，返回格式为 "HH:mm" 的时间差字符串
     * <p>
     * 该方法用于计算两个 {@link Date} 对象之间的时间差，常用于：
     * 1. 计算列车的运行时长（出发时间到到达时间的差值）
     * 2. 计算订单的处理时长
     * 3. 计算其他需要显示时间差的场景
     * <p>
     * 计算流程：
     * 1. 将两个 Date 对象转换为 LocalDateTime
     * 2. 使用 Duration 计算时间差
     * 3. 提取小时数和分钟数
     * 4. 格式化为 "HH:mm" 格式的字符串
     * <p>
     *
     * <p>
     * 注意事项：
     * - 返回格式固定为 "HH:mm"，小时和分钟都是两位数（不足两位前面补0）
     *
     * @param startTime 开始时间，不能为 null
     * @param endTime   结束时间，不能为 null
     * @return 时间差字符串，格式为 "HH:mm"（如："06:30" 表示6小时30分钟）
     */
    public static String calculateHourDifference(Date startTime, Date endTime) {
        // 将开始时间转换为 LocalDateTime
        // date.toInstant(): 将 Date 转换为 Instant（时间戳，不包含时区信息）
        // atZone(ZoneId.systemDefault()): 将 Instant 转换为系统默认时区的 ZonedDateTime
        // toLocalDateTime(): 从 ZonedDateTime 中提取 LocalDateTime（不包含时区信息）
        LocalDateTime startDateTime = startTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        
        // 将结束时间转换为 LocalDateTime
        // 使用相同的转换方式，确保两个时间在同一时区下进行比较
        LocalDateTime endDateTime = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        
        // 计算时间差
        // Duration.between() 计算两个 LocalDateTime 之间的时间差
        // 返回的 Duration 对象包含了完整的时间差信息（包括小时、分钟、秒等）
        Duration duration = Duration.between(startDateTime, endDateTime);
        
        // 提取小时数
        // duration.toHours() 返回总的小时数（向下取整）
        // 例如：6小时30分钟会返回 6
        long hours = duration.toHours();
        
        // 提取分钟数（去除小时部分）
        // duration.toMinutes() 返回总的分钟数
        // % 60 取模运算，得到不足1小时的分钟数
        // 例如：6小时30分钟，toMinutes() 返回 390，390 % 60 = 30
        long minutes = duration.toMinutes() % 60;
        
        // 格式化为 "HH:mm" 格式的字符串
        // String.format("%02d:%02d", hours, minutes) 将小时和分钟格式化为两位数
        // %02d 表示整数格式，宽度为2，不足两位前面补0
        // 例如：6小时30分钟会格式化为 "06:30"
        return String.format("%02d:%02d", hours, minutes);
    }

    /**
     * 将 Date 对象转换为指定格式的时间字符串
     * <p>
     * 该方法用于将传统的 {@link Date} 对象转换为指定格式的字符串，
     * <p>
     * 转换流程：
     * 1. 将 Date 对象转换为 Instant（时间戳）
     * 2. 将 Instant 转换为系统默认时区的 LocalDateTime
     * 3. 使用指定的格式模式创建 DateTimeFormatter
     * 4. 将 LocalDateTime 格式化为字符串并返回
     * <p>
     *
     * @param date    要转换的 Date 对象，不能为 null
     * @param pattern 日期时间格式模式，如 "HH:mm"、"yyyy-MM-dd HH:mm:ss" 等
     *                常用格式：
     *                - "HH:mm" - 仅显示小时和分钟（如：14:30）
     *                - "yyyy-MM-dd HH:mm:ss" - 完整日期时间（如：2025-12-25 14:30:00）
     *                - "yyyy-MM-dd" - 仅显示日期（如：2025-12-25）
     * @return 格式化后的时间字符串，格式由 pattern 参数决定
     * @throws NullPointerException 如果 date 或 pattern 为 null
     * @throws IllegalArgumentException 如果 pattern 格式不正确
     */
    public static String convertDateToLocalTime(Date date, String pattern) {
        // 将 Date 对象转换为 LocalDateTime
        // date.toInstant(): 将 Date 转换为 Instant（时间戳，不包含时区信息）
        // atZone(ZoneId.systemDefault()): 将 Instant 转换为系统默认时区的 ZonedDateTime
        // toLocalDateTime(): 从 ZonedDateTime 中提取 LocalDateTime（不包含时区信息）
        // 这样可以将传统的 Date 对象转换为 Java 8+ 的 LocalDateTime 对象
        LocalDateTime localDateTime = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();

        // 根据指定的格式模式创建日期时间格式化器
        // DateTimeFormatter 用于定义日期时间的显示格式
        // pattern 参数指定格式，如 "HH:mm" 表示只显示小时和分钟
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);

        // 使用格式化器将 LocalDateTime 格式化为字符串
        // 返回格式化后的时间字符串，格式由 pattern 参数决定
        return localDateTime.format(dateTimeFormatter);
    }

    @SneakyThrows
    public static void main(String[] args) {
        String startTimeStr = "2022-10-01 01:00:00";
        String endTimeStr = "2022-10-01 12:23:00";
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date startTime = formatter.parse(startTimeStr);
        Date endTime = formatter.parse(endTimeStr);
        String calculateHourDifference = calculateHourDifference(startTime, endTime);
        log.info("开始时间：{}，结束时间：{}，两个时间相差时分：{}", startTimeStr, endTimeStr, calculateHourDifference);
    }
}
