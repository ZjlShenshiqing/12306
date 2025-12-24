package org.openzjl.index12306.biz.ticketservice.toolkit;

import org.openzjl.index12306.biz.ticketservice.dto.domain.TicketListDTO;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * 自定义时间比较器
 * <p>
 * 用于按照出发时间对车票列表进行排序的比较器。
 * 实现 {@link Comparator} 接口，比较两个 {@link TicketListDTO} 对象的出发时间。
 * <p>
 * 使用场景：
 * 1. 车票列表查询结果按出发时间升序排序
 * 2. 帮助用户快速找到最早或最晚出发的车次
 * 3. 在车票列表展示时提供时间排序功能
 * <p>
 * 比较逻辑：
 * - 将字符串格式的出发时间（如 "08:30"）解析为 {@link LocalTime} 对象
 * - 使用 {@link LocalTime#compareTo(LocalTime)} 进行时间比较
 * - 返回负数表示第一个时间早于第二个时间
 * - 返回0表示两个时间相同
 * - 返回正数表示第一个时间晚于第二个时间
 * <p>
 *
 * @author zhangjlk
 * @date 2025/12/24 下午8:23
 */
public class TimeStringComparator implements Comparator<TicketListDTO> {

    /**
     * 时间格式化器
     * <p>
     * 用于将字符串格式的时间（如 "08:30"）解析为 {@link LocalTime} 对象
     * 格式：HH:mm（24小时制，小时:分钟）
     * 例如："08:30" 表示早上8点30分，"20:15" 表示晚上8点15分
     */
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /**
     * 比较两个车票的出发时间
     * <p>
     * 将字符串格式的出发时间解析为 {@link LocalTime} 对象，然后进行比较。
     * 比较结果用于排序：时间早的排在前面，时间晚的排在后面。
     *
     * @param ticketList1 第一个车票对象，包含出发时间字符串（格式：HH:mm）
     * @param ticketList2 第二个车票对象，包含出发时间字符串（格式：HH:mm）
     * @return 比较结果：
     *         - 负数：ticketList1 的出发时间早于 ticketList2（ticketList1 排在前面）
     *         - 0：两个出发时间相同
     *         - 正数：ticketList1 的出发时间晚于 ticketList2（ticketList1 排在后面）
     * @throws java.time.format.DateTimeParseException 如果出发时间字符串格式不正确（不是 HH:mm 格式）
     */
    @Override
    public int compare(TicketListDTO ticketList1, TicketListDTO ticketList2) {
        // 将第一个车票的出发时间字符串解析为 LocalTime 对象
        // 例如："08:30" -> LocalTime(8, 30)
        LocalTime localTime1 = LocalTime.parse(ticketList1.getDepartureTime(), FORMATTER);
        
        // 将第二个车票的出发时间字符串解析为 LocalTime 对象
        // 例如："20:15" -> LocalTime(20, 15)
        LocalTime localTime2 = LocalTime.parse(ticketList2.getDepartureTime(), FORMATTER);
        
        // 比较两个时间对象，返回比较结果
        // 如果 localTime1 < localTime2，返回负数（升序排序）
        // 如果 localTime1 = localTime2，返回0
        // 如果 localTime1 > localTime2，返回正数
        return localTime1.compareTo(localTime2);
    }
}
