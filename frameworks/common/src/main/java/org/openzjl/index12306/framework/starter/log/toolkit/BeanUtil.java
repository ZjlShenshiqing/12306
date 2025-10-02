package org.openzjl.index12306.framework.starter.log.toolkit;

import com.github.dozermapper.core.DozerBeanMapperBuilder;
import com.github.dozermapper.core.Mapper;
import com.github.dozermapper.core.loader.api.BeanMappingBuilder;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.lang.reflect.Array;
import java.util.*;

import static com.github.dozermapper.core.loader.api.TypeMappingOptions.mapEmptyString;
import static com.github.dozermapper.core.loader.api.TypeMappingOptions.mapNull;

/**
 * 对象属性复制工具类
 *
 * @author zhangjlk
 * @date 2025/9/18 10:37
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeanUtil {

    /**
     * 全局唯一的对象映射工具（俗称“Bean拷贝器”）
     * 用于在不同对象之间自动复制属性值，比如：
     *   UserEntity  →  UserDTO
     *   Order       →  OrderVO
     *
     * 全局唯一的原因是创建一次后可以反复使用，性能高、节省资源。
     */
    protected static Mapper BEAN_MAPPER_BUILDER;

    static {
        /**
         * 创建一个默认配置的“对象映射器”
         * 它能自动把 A 对象 的字段值复制到 B 对象
         * 支持字段名不同、类型转换、嵌套对象等复杂情况
         */
        BEAN_MAPPER_BUILDER = DozerBeanMapperBuilder.buildDefault();
    }

    /**
     * 对象属性复制
     *
     * @param source  数据对象
     * @param target  目标对象
     * @param <T>    目标对象的类型
     * @param <S>    源对象的类型
     * @return 转换后对象
     */
    public static <T, S> T convert(S source, T target) {
        Optional.ofNullable(source)
                // 使用 Dozer 映射器，把源对象 each 的数据复制到 target 中
                // 注意：这里是“复制到已有对象”，不是创建新对象
                .ifPresent(each -> BEAN_MAPPER_BUILDER.map(each, target));
        // 返回已经填充好的对象
        return target;
    }

    /**
     * 将源对象转换为指定类型的新对象
     *
     * @param source 源对象（例如 UserEntity）
     * @param clazz  目标类的 Class 对象（例如 UserDTO.class）
     * @param <T>    目标对象的类型
     * @param <S>    源对象的类型
     * @return 返回一个新创建的目标类型对象，填充了 source 的数据；如果 source 为 null，则返回 null
     */
    public static <T, S> T convert(S source, Class<T> clazz) {

        // 如果 source 不是 null，就执行 map 转换
        // 使用 Dozer 映射器：把 source 对象转成 clazz 类型的新对象
        return Optional.ofNullable(source)
                .map(each -> BEAN_MAPPER_BUILDER.map(each, clazz))
                // 如果source是null就返回null
                .orElse(null);
    }

    /**
     * 复制多个对象（List版本）
     *
     * @param sources 数据对象
     * @param clazz   复制目标类型
     * @param <T>     目标对象的类型
     * @param <S>     源对象的类型
     * @return        转换后对象集合
     */
    public static <T, S> List<T> convert(List<S>sources, Class<T> clazz) {
        return Optional.ofNullable(sources)
                .map(each -> {
                    List<T> targetList = new ArrayList<>(each.size());
                    each.stream()
                            .forEach(item -> targetList.add(BEAN_MAPPER_BUILDER.map(item, clazz)));
                    return targetList;
                })
                .orElse(null);
    }

    /**
     * 复制多个对象（Set版本）
     *
     * @param sources 数据对象
     * @param clazz   复制目标类型
     * @param <T>     目标对象的类型
     * @param <S>     源对象的类型
     * @return        转换后对象集合
     */
    public static <T, S> Set<T> convert(Set<S> sources, Class<T> clazz) {
        return Optional.ofNullable(sources)
                .map(each -> {
                    Set<T> targetSet = new HashSet<T>(each.size());
                    each.stream()
                            .forEach(item -> targetSet.add(BEAN_MAPPER_BUILDER.map(sources, clazz)));
                    return targetSet;
                })
                .orElse(null);
    }

    /**
     * 复制多个对象（数组版本）
     *
     * @param sources 数据对象
     * @param clazz   复制目标类型
     * @param <T>     目标对象的类型
     * @param <S>     源对象的类型
     * @return        转换后对象集合
     */
    public static <T, S> T[] convert(S[] sources, Class<T> clazz) {
        return Optional.ofNullable(sources)
                .map(each -> {
                    T[] targetArray = (T[]) Array.newInstance(clazz, sources.length);
                    for (int i = 0; i < targetArray.length; i++) {
                        targetArray[i] = BEAN_MAPPER_BUILDER.map(sources[i], clazz);
                    }
                    return targetArray;
                })
                .orElse(null);
    }

    /**
     * 拷贝非空且非空串属性
     * 功能：将 source 对象中不为 null 且不是空字符串的属性，复制到 target 对象中
     *
     * @param source 数据源
     * @param target 指向源
     */
    public static void convertIgnoreNullAndBlank(Object source, Object target) {
        DozerBeanMapperBuilder dozerBeanMapperBuilder = DozerBeanMapperBuilder.create();
        Mapper mapper = dozerBeanMapperBuilder.withMappingBuilders(new BeanMappingBuilder() {

            /**
             * 只有满足以下条件的属性才会被拷贝：
             *
             * 属性值 不是 null
             * 也不是 ""（空字符串）
             */
            @Override
            protected void configure() {
                mapping(source.getClass(), target.getClass(), mapNull(false), mapEmptyString(false));
            }
        }).build();
        // 开始执行真正的属性拷贝操作
        mapper.map(source, target);
    }

    /**
     * 拷贝非空属性
     * @param source 数据源
     * @param target 目标源
     */
    public static void convertIgnoreNull(Object source, Object target) {
        DozerBeanMapperBuilder dozerBeanMapperBuilder = DozerBeanMapperBuilder.create();
        Mapper mapper = dozerBeanMapperBuilder.withMappingBuilders(new BeanMappingBuilder() {
            @Override
            protected void configure() {
                mapping(source.getClass(), target.getClass(), mapNull(false));
            }
        }
        ).build();
        mapper.map(source, target);
    }

}
