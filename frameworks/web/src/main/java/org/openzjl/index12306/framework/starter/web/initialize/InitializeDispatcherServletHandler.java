package org.openzjl.index12306.framework.starter.web.initialize;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.DispatcherServlet;

import static org.openzjl.index12306.framework.starter.web.config.WebAutoConfiguration.INITIALIZE_PATH;

/**
 * 通过 {@link InitializeDispatcherServletController} 初始化 {@link DispatcherServlet}
 *
 * @author zhangjlk
 * @date 2025/10/4 14:46
 */
@RequiredArgsConstructor
// CommandLineRunner 是 Spring Boot 提供的一个接口，用于在项目启动完成后、容器初始化完毕后，自动执行一段“自定义逻辑”（比如预热、初始化数据、调用自己等）
public final class InitializeDispatcherServletHandler implements CommandLineRunner {

    /**
     * Spring 提供的“HTTP 客户端工具”，用来发送 HTTP 请求（GET/POST 等）
     */
    private final RestTemplate restTemplate;

    /**
     * Spring 的“环境配置管理器”，可以读取配置文件
     */
    private final ConfigurableEnvironment environment;

    @Override
    public void run(String... args) throws Exception {
        String url = String.format("http://127.0.0.1:%s%s",
                environment.getProperty("server.port", "8080") + environment.getProperty("server.servlet.context-path", ""),
                INITIALIZE_PATH);
        try {
            restTemplate.execute(url, HttpMethod.GET, null, null);
        } catch (Throwable ignored) {
        }
    }
}
