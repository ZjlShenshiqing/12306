/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.framework.starter.database.toolkit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.openzjl.index12306.framework.starter.log.toolkit.BeanUtil;
import org.openzjl.index12306.framework.starter.convention.page.PageRequest;
import org.openzjl.index12306.framework.starter.convention.page.PageResponse;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类
 * <p>提供分页请求与响应之间的转换方法，支持泛型数据转换，简化 MyBatis-Plus 分页对象与业务分页响应对象的映射。</p>
 *
 * @author zhangjlk
 * @date 2025/9/22 20:33
 */
public class PageUtil {

    /**
     * 将分页请求对象转换为 MyBatis-Plus 的 Page 分页对象
     *
     * @param pageRequest 分页请求参数（包含当前页码和每页大小）
     * @return MyBatis-Plus 的 Page 对象，用于数据库分页查询
     */
    public static Page convert(PageRequest pageRequest) {
        return convert(pageRequest.getCurrent(), pageRequest.getSize());
    }

    /**
     * 根据当前页码和每页大小构造 MyBatis-Plus 的 Page 分页对象
     *
     * @param current 当前页码（从1开始）
     * @param size    每页记录数
     * @return MyBatis-Plus 的 Page 对象
     */
    public static Page convert(long current, long size) {
        return new Page(current, size);
    }

    /**
     * 将 MyBatis-Plus 的 IPage 查询结果转换为通用分页响应对象（数据类型不变）
     *
     * @param iPage MyBatis-Plus 查询返回的分页结果
     * @return 封装后的通用分页响应对象 PageResponse
     */
    public static PageResponse convert(IPage iPage) {
        return buildConventionPage(iPage);
    }

    /**
     * 将 MyBatis-Plus 的 IPage 查询结果转换为指定目标类型的分页响应对象（使用 BeanUtil 自动转换每条记录）
     *
     * @param <TARGET>   目标数据类型（如 DTO）
     * @param <ORIGINAL> 原始数据类型（如 Entity）
     * @param iPage      MyBatis-Plus 查询返回的分页结果
     * @param targetClass 目标类型 Class 对象
     * @return 转换后的分页响应对象，records 字段为 TARGET 类型列表
     */
    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Class<TARGET> targetClass) {
        iPage.convert(each -> BeanUtil.convert(each, targetClass));
        return buildConventionPage(iPage);
    }

    /**
     * 将 MyBatis-Plus 的 IPage 查询结果转换为指定目标类型的分页响应对象（使用自定义函数映射每条记录）
     *
     * @param <TARGET>   目标数据类型（如 DTO）
     * @param <ORIGINAL> 原始数据类型（如 Entity）
     * @param iPage      MyBatis-Plus 查询返回的分页结果
     * @param mapper     自定义映射函数，用于将 ORIGINAL 类型转换为 TARGET 类型
     * @return 转换后的分页响应对象，records 字段为 TARGET 类型列表
     */
    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Function<? super ORIGINAL, ? extends TARGET> mapper) {
        List<TARGET> targetDataList = iPage.getRecords().stream()
                .map(mapper)
                .collect(Collectors.toList());
        return PageResponse.<TARGET>builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(targetDataList)
                .total(iPage.getTotal())
                .build();
    }

    /**
     * 构建通用分页响应对象（内部私有方法，避免重复代码）
     *
     * @param iPage MyBatis-Plus 分页查询结果
     * @return 封装后的 PageResponse 对象
     */
    private static PageResponse buildConventionPage(IPage iPage) {
        return PageResponse.builder()
                .current(iPage.getCurrent())
                .size(iPage.getSize())
                .records(iPage.getRecords())
                .total(iPage.getTotal())
                .build();
    }
}
