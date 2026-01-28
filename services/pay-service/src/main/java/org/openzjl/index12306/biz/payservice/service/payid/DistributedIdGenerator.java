package org.openzjl.index12306.biz.payservice.service.payid;

/**
 * 分布式全局唯一订单号生成器
 * 基于类似雪花算法的设计，生成全局唯一的订单ID
 * ID结构：时间戳 + 节点ID + 序列号
 *
 * @author zhangjlk
 * @date 2026/1/16 22:28
 */
public class DistributedIdGenerator {

    /**
     * 起始时间戳（2021-01-01 00:00:00）
     * 用于计算相对时间，减少时间戳占用的位数
     */
    private static final long EPOCH = 1609459200000L;

    /**
     * 节点ID占用的位数
     * 5位可以表示 0-31 共32个节点
     */
    private static final int NODE_BITS = 5;

    /**
     * 序列号占用的位数
     * 7位可以表示 0-127 共128个序列号
     */
    private static final long SEQUENCE_BITS = 7;

    /**
     * 节点ID，用于区分不同的服务器节点
     * 每个节点应该有唯一的ID（0-31之间）
     */
    private final long nodeID;

    /**
     * 上次生成ID时的时间戳
     * 用于检测时钟回拨问题
     */
    private long lastTimestamp = -1L;

    /**
     * 序列号，同一毫秒内的递增序号
     * 用于保证同一毫秒内生成的ID唯一性
     * 范围：0-127（根据SEQUENCE_BITS=7计算）
     */
    private long sequence = 0L;

    /**
     * 构造函数
     *
     * @param nodeID 节点ID，范围应该在 0-31 之间（根据NODE_BITS计算）
     */
    public DistributedIdGenerator(long nodeID) {
        this.nodeID = nodeID;
    }

    /**
     * 生成全局唯一的订单ID
     * 使用同步方法保证线程安全
     *
     * ID生成算法（类似雪花算法）：
     * - 时间戳部分：当前时间相对起始时间的毫秒数
     * - 节点ID部分：用于区分不同服务器节点
     * - 序列号部分：同一毫秒内的递增序号
     *
     * @return 全局唯一的订单ID
     */
    public synchronized long generateId() {
        // 计算当前时间相对于起始时间戳的毫秒数
        long timestamp = System.currentTimeMillis() - EPOCH;

        // 检测时钟回拨：如果当前时间小于上次时间，说明系统时钟被回拨了
        // 这种情况会导致ID重复，因此抛出异常拒绝生成ID
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("Clock moved backwards. Refusing to generate ID.");
        }

        // 当前时间戳等于上次时间戳（同一毫秒内）
        if (timestamp == lastTimestamp) {
            // 序列号递增，并使用位掩码确保不超过最大值
            // (1 << SEQUENCE_BITS) - 1 计算得到序列号的最大值（127）
            // 例如：SEQUENCE_BITS=7，则 (1 << 7) - 1 = 128 - 1 = 127
            // & 操作确保序列号在 0-127 范围内循环
            sequence = (sequence + 1) & ((1 << SEQUENCE_BITS) - 1);

            // 如果序列号溢出（从127回到0），说明同一毫秒内已生成128个ID
            // 需要等待到下一毫秒才能继续生成
            if (sequence == 0) {
                timestamp = tillNextMills(lastTimestamp);
            }
        } else {
            // 情况2：当前时间戳大于上次时间戳（进入新的毫秒）
            // 序列号重置为0，新毫秒从0开始计数
            sequence = 0L;
        }

        // 更新上次时间戳
        lastTimestamp = timestamp;

        // 组合生成最终的ID
        // ID结构（64位）：时间戳 | 节点ID | 序列号
        // 位运算说明：
        // 1. timestamp << (NODE_BITS + SEQUENCE_BITS)：时间戳左移12位（5+7），为节点ID和序列号留出空间
        // 2. nodeID << SEQUENCE_BITS：节点ID左移7位，为序列号留出空间
        // 3. sequence：序列号放在最低位
        // 4. | 操作：将三部分按位或运算组合成最终的ID
        // 例如：假设 timestamp=1000, nodeID=5, sequence=10
        // 结果 = (1000 << 12) | (5 << 7) | 10 = 4096000 | 640 | 10 = 4096650
        return (timestamp << (NODE_BITS + SEQUENCE_BITS)) | (nodeID << SEQUENCE_BITS) | sequence;
    }

    /**
     * 等待直到下一毫秒
     * 当同一毫秒内序列号溢出（超过最大值）时，需要等待到下一毫秒才能继续生成ID
     * 通过自旋等待的方式，确保获取到大于上次时间戳的新时间戳
     *
     * @param lastTimestamp 上次生成ID时的时间戳
     * @return 新的时间戳（大于lastTimestamp的下一毫秒时间戳）
     */
    private long tillNextMills(long lastTimestamp) {
        // 获取当前时间相对于起始时间戳的毫秒数
        long timestamp = System.currentTimeMillis() - EPOCH;
        // 自旋等待：如果当前时间戳小于等于上次时间戳，说明还在同一毫秒内
        // 需要等待直到进入下一毫秒，才能继续生成新的ID
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis() - EPOCH;
        }
        // 返回新的时间戳（已经进入下一毫秒）
        return timestamp;
    }
}

