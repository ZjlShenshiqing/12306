package org.openzjl.index12306.biz.ticketservice.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.openzjl.index12306.framework.starter.database.base.BaseDO;

/**
 * 车站实体
 *
 * @author zhangjlk
 * @date 2025/11/25 15:31
 */
@Data
@TableName("t_station")
public class StationDO extends BaseDO {

    /**
     * id
     */
    private long id;

    /**
     * 车站编码
     */
    private String code;

    /**
     * 车站名称
     */
    private String name;

    /**
     * 拼音
     */
    private String spell;

    /**
     * 地区
     */
    private String region;

    /**
     * 地区编码
     */
    private String regionName;
}
