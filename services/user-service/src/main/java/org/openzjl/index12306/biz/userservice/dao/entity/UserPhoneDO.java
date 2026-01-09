package org.openzjl.index12306.biz.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.framework.starter.database.base.BaseDO;

import java.time.LocalDateTime;

/**
 * 用户手机号实体对象
 *
 * @author zhangjlk
 * @date 2026/1/8 16:23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_phone")
public class UserPhoneDO extends BaseDO {

    /**
     * 用户id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 注销时间戳
     */
    private Long deletionTime;
}
