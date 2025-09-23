package org.openzjl.index12306.framework.starter.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.openzjl.index12306.framework.starter.common.enums.DelEnum;

import java.util.Date;


/**
 * 元数据处理器
 * <p>
 * 统一处理公共字段的自动填充逻辑，避免在各个持久层重复设置相同字段。
 * </p>
 *
 * <p>填充规范：</p>
 * <ul>
 *     <li>新增场景：自动填充 {@code createTime}、{@code updateTime}、{@code delFlag}。</li>
 *     <li>更新场景：自动填充 {@code updateTime}。</li>
 *     <li>删除标识 {@code delFlag}：使用 {@link org.openzjl.index12306.framework.starter.common.enums.DelEnum}，约定 NORMAL=未删除。</li>
 * </ul>
 *
 * <p>要求：</p>
 * <ul>
 *     <li>实体类需存在对应字段及类型匹配（如 {@code Date createTime}）。</li>
 *     <li>时间语义为应用服务器当前时间。</li>
 * </ul>
 *
 * @author zhangjlk
 * @date 2025/9/22 20:33
 */
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 数据新增时的公共字段自动填充。
     * <p>
     * 规则：
     * </p>
     * <ul>
     *     <li>{@code createTime}：若未显式赋值，则填充为当前时间。</li>
     *     <li>{@code updateTime}：若未显式赋值，则填充为当前时间。</li>
     *     <li>{@code delFlag}：若未显式赋值，则填充为 {@code DelEnum.NORMAL.code()}。</li>
     * </ul>
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "updateTime", Date.class, new Date());
        this.strictInsertFill(metaObject, "delFlag", Integer.class, DelEnum.NORMAL.code());
    }

    /**
     * 数据更新时的公共字段自动填充。
     * <p>
     * 规则：
     * </p>
     * <ul>
     *     <li>{@code updateTime}：更新为当前时间。</li>
     * </ul>
     *
     * @param metaObject 元数据对象
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", Date.class, new Date());
    }
}
