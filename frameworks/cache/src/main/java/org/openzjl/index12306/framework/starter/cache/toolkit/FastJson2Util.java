package org.openzjl.index12306.framework.starter.cache.toolkit;

import com.alibaba.fastjson2.util.ParameterizedTypeImpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * FastJson2 工具类。
 * <p>
 * 提供基于 {@link com.alibaba.fastjson2.util.ParameterizedTypeImpl} 的泛型类型构造能力，
 * 便于在使用 FastJson2 进行反序列化时，动态构建嵌套泛型的 {@link java.lang.reflect.Type} 描述。
 * </p>
 *
 * <p>使用场景：</p>
 * <ul>
 *     <li>需要反序列化为嵌套泛型类型时，例如：List<Map<String, User>>。</li>
 *     <li>在运行时根据不同的类型参数动态拼装泛型 Type。</li>
 * </ul>
 *
 * <p>示例：</p>
 * <pre>
 * // 构造 List<Map<String, User>> 的 Type（从外到内依次传入）
 * Type type = FastJson2Util.buildType(List.class, Map.class, String.class, User.class);
 * </pre>
 *
 * @author zhangjlk
 * @date 2025/10/2 17:03
 */
public final class FastJson2Util {

    /**
     * 由外到内依次传入类型，构造嵌套泛型的 {@link Type} 描述。
     * <p>
     * 传参规则（从外层到内层）：外层原始类型在前，随后依次传入其类型参数；
     * 当类型参数自身仍为泛型时，继续追加其原始类型及类型参数，直至最内层的具体类型。
     * </p>
     *
     * <p>注意：</p>
     * <ul>
     *     <li>当仅传入 1 个元素时，直接返回该类型的 {@code ParameterizedTypeImpl} 包装。</li>
     *     <li>当传入多个元素时，从参数数组尾部开始向前构建嵌套结构。</li>
     *     <li>入参为 {@code null} 或长度为 0 时，返回 {@code null}。</li>
     * </ul>
     *
     * @param types 从外到内依次传入的类型序列
     * @return 组合后的嵌套泛型 {@link Type}，若入参为空则返回 {@code null}
     */
    public static Type buildType(Type... types) {
        ParameterizedTypeImpl beforeType = null;
        if (types != null && types.length > 0) {
            if (types.length == 1) {
                return new ParameterizedTypeImpl(new Type[]{null}, null, types[0]);
            }

            for (int i = types.length - 1; i > 0; i--) {
                beforeType = new ParameterizedTypeImpl(new Type[]{beforeType == null ? types[i] : beforeType}, null, types[i - 1]);
            }
        }
        return beforeType;
    }
}
