package org.openzjl.index12306.biz.userservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 用户服务主入口
 *
 * @author zhangjlk
 * @date 2026/1/7 15:50
 */
@SpringBootApplication
@MapperScan("org.openzjl.index12306.biz.userservice.dao.mapper")
public class UserServiceApplication {
}
