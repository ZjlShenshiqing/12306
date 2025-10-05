package org.openzjl.index12306.framework.starter.web.config;

import org.openzjl.index12306.framework.starter.web.GlobalExceptionHandler;
import org.openzjl.index12306.framework.starter.web.initialize.InitializeDispatcherServletController;
import org.openzjl.index12306.framework.starter.web.initialize.InitializeDispatcherServletHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * Web 组件自动装配
 * @author zhangjlk
 * @date 2025/10/4 14:47
 */
public class WebAutoConfiguration {

    public final static String INITIALIZE_PATH = "/initialize/dispatcher-servlet";

    @Bean
    @ConditionalOnMissingBean // 如果容器里还没有这个类型的 Bean，才创建
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    public InitializeDispatcherServletController initializeDispatcherServletController() {
        return new InitializeDispatcherServletController();
    }

    /**
     * 创建一个定制化的 RestTemplate 实例
     *
     * 说明：
     * - 使用 @Bean 注解，表示这个方法返回的对象会被 Spring 容器管理
     * - 接收一个 ClientHttpRequestFactory 参数，由 Spring 自动注入（来自下面的 simpleClientHttpRequestFactory）
     * - 这样可以灵活控制底层 HTTP 请求行为（如超时、连接池等）
     */
    @Bean
    public RestTemplate simpleRestTemplate(ClientHttpRequestFactory factory) {
        return new RestTemplate(factory);
    }

    /**
     * 创建一个简单的 HTTP 请求工厂（基于 JDK 原生 HttpURLConnection）
     *
     * 说明：
     * - 设置连接超时和读取超时为 5 秒，避免请求卡死
     * - 此 Bean 会被上面的 simpleRestTemplate 自动注入使用
     */
    @Bean
    public ClientHttpRequestFactory simpleClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(5000);
        factory.setConnectTimeout(5000);
        return factory;
    }

    /**
     * 创建 InitializeDispatcherServletHandler 实例，用于项目启动后自动调用 /init 接口触发 DispatcherServlet 初始化
     *
     * 说明：
     * - 使用 @Bean 注解，表示这个对象由 Spring 管理
     * - 构造器注入 RestTemplate 和 ConfigurableEnvironment，用于后续自调用接口
     * - 它会在项目启动完成后自动执行（因为实现了 CommandLineRunner）
     */
    @Bean
    public InitializeDispatcherServletHandler initializeDispatcherServletHandler(RestTemplate restTemplate, ConfigurableEnvironment configurableEnvironment) {
        return new InitializeDispatcherServletHandler(restTemplate, configurableEnvironment);
    }
}
