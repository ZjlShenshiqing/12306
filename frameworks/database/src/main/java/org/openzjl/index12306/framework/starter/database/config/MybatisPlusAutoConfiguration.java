package org.openzjl.index12306.framework.starter.database.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.openzjl.index12306.framework.starter.database.handler.CustomIdGenerator;
import org.openzjl.index12306.framework.starter.database.handler.MyMetaObjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * MyBatis-Plus 自动配置。
 * <p>
 * 提供分页拦截器、元数据自动填充处理器、自定义主键生成器等基础 Bean。
 * 这些 Bean 会被 Spring 容器加载，用于统一数据库访问层的通用能力。
 * </p>
 *
 * <p>注意：</p>
 * <ul>
 *     <li>分页插件需根据真实数据库类型设置，此处为 MySQL。</li>
 *     <li>如项目中存在多个 {@link com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator}，
 *     通过 {@link org.springframework.context.annotation.Primary} 指定优先级。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/9/22 20:33
 */
public class MybatisPlusAutoConfiguration {

    /**
     * 构建并注册 MyBatis-Plus 拦截器。
     * <p>
     * 当前仅启用分页拦截器，数据库类型设置为 MySQL。
     * </p>
     *
     * @return MybatisPlusInterceptor 拦截器实例
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * 注册 MyBatis 元对象处理器。
     * <p>
     * 用于统一处理公共字段的自动填充（如创建时间、更新时间、操作人等）。
     * </p>
     *
     * @return MyMetaObjectHandler 实例
     */
    @Bean
     public MyMetaObjectHandler myMetaObjectHandler() {
        return new MyMetaObjectHandler();
    }

    /**
     * 注册自定义主键生成器。
     * <p>
     * 当系统中存在多个主键生成策略时，标注 {@code @Primary} 以确保该生成器优先被注入使用。
     * </p>
     *
     * @return 自定义的 IdentifierGenerator 实例
     */
    @Bean
    @Primary
     public IdentifierGenerator idGenerator() {
        return new CustomIdGenerator();
    }
}
