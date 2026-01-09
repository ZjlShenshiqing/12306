package org.openzjl.index12306.biz.userservice.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 手机号脱敏序列化器。
 * <p>
 * 用于在 JSON 序列化过程中对手机号进行脱敏处理，保护用户隐私信息。
 * 继承自 {@link JsonSerializer}，实现自定义序列化逻辑。
 * </p>
 *
 * <p>脱敏规则：</p>
 * <ul>
 *     <li>保留前 3 位：显示手机号的前 3 位数字（通常是运营商号段，如 138、159 等）。</li>
 *     <li>保留后 4 位：显示手机号的后 4 位数字（便于用户识别自己的手机号）。</li>
 *     <li>中间部分脱敏：将中间 4 位数字替换为星号（*）。</li>
 * </ul>
 *
 * <p>脱敏示例：</p>
 * <pre>
 * 原始手机号：13812345678
 * 脱敏后结果：138****5678
 * </pre>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>API 响应中返回用户信息时，对手机号进行脱敏。</li>
 *     <li>日志记录时，避免记录完整的手机号。</li>
 *     <li>前端展示时，保护用户隐私信息。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2026/1/9 12:24
 */
public class PhoneDesensitizationSerializer extends JsonSerializer<String> {

    /**
     * 序列化手机号，进行脱敏处理。
     * <p>
     * 将完整的手机号转换为脱敏后的字符串，然后写入 JSON 输出流。
     * </p>
     *
     * <p>脱敏规则：</p>
     * <ul>
     *     <li>使用 {@link DesensitizedUtil#mobilePhone(String)} 方法进行脱敏。</li>
     *     <li>默认规则：保留前 3 位和后 4 位，中间 4 位脱敏。</li>
     *     <li>脱敏字符：使用星号（*）替换中间部分。</li>
     * </ul>
     *
     * @param phone               原始手机号字符串（11 位数字）
     * @param jsonGenerator       JSON 生成器，用于写入序列化后的数据
     * @param serializerProvider  序列化提供者，提供序列化上下文信息
     * @throws IOException 当 JSON 写入失败时抛出
     */
    @Override
    public void serialize(String phone, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // DesensitizedUtil.mobilePhone() 方法会自动处理手机号脱敏
        // 规则：保留前 3 位和后 4 位，中间 4 位替换为星号
        // 例如：13812345678 -> 138****5678
        String phoneDesensitization = DesensitizedUtil.mobilePhone(phone);
        
        // 将脱敏后的手机号写入 JSON 输出流
        // 这样在序列化对象为 JSON 时，手机号字段会自动应用脱敏规则
        jsonGenerator.writeString(phoneDesensitization);
    }
}
