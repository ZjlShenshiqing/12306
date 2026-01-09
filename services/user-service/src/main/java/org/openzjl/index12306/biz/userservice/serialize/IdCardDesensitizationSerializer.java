package org.openzjl.index12306.biz.userservice.serialize;

import cn.hutool.core.util.DesensitizedUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/**
 * 身份证号脱敏序列化器。
 * <p>
 * 用于在 JSON 序列化过程中对身份证号进行脱敏处理，保护用户隐私信息。
 * 继承自 {@link JsonSerializer}，实现自定义序列化逻辑。
 * </p>
 *
 * <p>脱敏规则：</p>
 * <ul>
 *     <li>保留前 4 位：显示身份证号的前 4 位数字（通常是地区代码）。</li>
 *     <li>保留后 4 位：显示身份证号的后 4 位数字（通常是校验位）。</li>
 *     <li>中间部分脱敏：将中间部分替换为星号（*）或其他脱敏字符。</li>
 * </ul>
 *
 * <p>脱敏示例：</p>
 * <pre>
 * 原始身份证号：110101199001011234
 * 脱敏后结果：1101**********1234
 * </pre>
 *
 * @author zhangjlk
 * @date 2026/1/9 12:23
 */
public class IdCardDesensitizationSerializer extends JsonSerializer<String> {
    /**
     * 序列化身份证号，进行脱敏处理。
     * <p>
     * 将完整的身份证号转换为脱敏后的字符串，然后写入 JSON 输出流。
     * </p>
     *
     * <p>脱敏参数说明：</p>
     * <ul>
     *     <li>第一个参数 {@code 4}：保留前 4 位数字。</li>
     *     <li>第二个参数 {@code 4}：保留后 4 位数字。</li>
     *     <li>中间部分：自动替换为脱敏字符（通常是星号 *）。</li>
     * </ul>
     *
     * <p>处理流程：</p>
     * <ol>
     *     <li>接收原始身份证号字符串。</li>
     *     <li>调用 {@link DesensitizedUtil#idCardNum(String, int, int)} 进行脱敏处理。</li>
     *     <li>将脱敏后的字符串写入 JSON 输出流。</li>
     * </ol>
     *
     * @param idCard              原始身份证号字符串
     * @param jsonGenerator       JSON 生成器，用于写入序列化后的数据
     * @param serializerProvider  序列化提供者，提供序列化上下文信息
     * @throws IOException 当 JSON 写入失败时抛出
     */
    @Override
    public void serialize(String idCard, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        // 参数说明：
        // - idCard: 原始身份证号
        // - 4: 保留前 4 位
        // - 4: 保留后 4 位
        // 例如：110101199001011234 -> 1101**********1234
        String idCardDesensitization = DesensitizedUtil.idCardNum(idCard, 4, 4);
        
        // 将脱敏后的身份证号写入 JSON 输出流
        // 这样在序列化对象为 JSON 时，身份证号字段会自动应用脱敏规则
        jsonGenerator.writeString(idCardDesensitization);
    }
}
