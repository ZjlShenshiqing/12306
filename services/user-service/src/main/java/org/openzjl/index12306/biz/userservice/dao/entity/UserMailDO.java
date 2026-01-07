package org.openzjl.index12306.biz.userservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.openzjl.index12306.framework.starter.database.base.BaseDO;

/**
 * 用户邮箱表实体对象
 *
 * @author zhangjlk
 * @date 2026/1/7 16:15
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_user_mail")
public class UserMailDO extends BaseDO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 邮箱号
     */
    private String mail;

    /**
     * 注销时间戳
     */
    private Long deletionTime;
}
