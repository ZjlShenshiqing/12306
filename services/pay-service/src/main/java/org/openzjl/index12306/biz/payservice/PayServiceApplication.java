package org.openzjl.index12306.biz.payservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.retry.annotation.EnableRetry;

/**
 * 支付服务应用启动器
 *
 * @author zhangjlk
 * @date 2026/1/22 12:26
 */
@SpringBootApplication
@MapperScan
@EnableFeignClients
@EnableRetry
public class PayServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayServiceApplication.class, args);
    }
}
