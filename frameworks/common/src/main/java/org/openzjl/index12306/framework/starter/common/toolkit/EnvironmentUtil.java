package org.openzjl.index12306.framework.starter.common.toolkit;

import org.openzjl.index12306.framework.starter.bases.ApplicationContextHolder;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.ArrayList;
import java.util.List;

/**
 * 环境工具类
 *
 * @author zhangjlk
 * @date 2025/9/18 10:38
 */
public class EnvironmentUtil {

    private static List<String> ENVIRONMENT_LIST = new ArrayList<>();

    static {
        ENVIRONMENT_LIST.add("dev"); // 开发环境
        ENVIRONMENT_LIST.add("test"); // 测试环境
    }

    /**
     * 判断当前是否为开发环境
     *
     * @return 判断结果
     */
    public static boolean isDevEnvironment() {
        // ConfigurableEnvironment 是配置文件的信息
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        /**
         * 尝试获取配置项：spring.profiles.active
         * 如果没有设置这个属性，默认返回 "dev"
         */
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active", "dev");

        return ENVIRONMENT_LIST.stream()
                .filter(each -> propertyActive.contains(each))
                .findFirst()
                .isPresent();
    }

    /**
     * 判断是否为正式环境
     *
     * @return 判断结果
     */
    public static boolean isProdEnvironment() {
        ConfigurableEnvironment configurableEnvironment = ApplicationContextHolder.getBean(ConfigurableEnvironment.class);
        String propertyActive = configurableEnvironment.getProperty("spring.profiles.active", "dev");
        return ENVIRONMENT_LIST.stream().filter(each -> propertyActive.contains(each)).findFirst().isPresent();
    }
}
