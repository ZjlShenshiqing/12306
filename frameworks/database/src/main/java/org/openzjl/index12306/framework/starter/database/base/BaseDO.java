package org.openzjl.index12306.framework.starter.database.base;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.util.Date;

/**
 * 数据持久层基础属性
 *
 * @author zhangjlk
 * @date 2025/9/22 20:32
 */
@Data
public class BaseDO {

    /**
     * 创建时间
     */
    // 当执行 INSERT（插入）操作时，MyBatis-Plus 会自动为该字段填充一个值，不需要你手动 set
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 删除标志
     */
    @TableField(fill = FieldFill.INSERT)
    private Integer delFlag;


}
