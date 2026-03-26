/*
 * Copyright (c) 2025-2026 zhangjlk
 * All rights reserved.
 */
package org.openzjl.index12306.biz.orderservice.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openzjl.index12306.biz.orderservice.config.OrderQueryFallbackProperties;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.dto.resp.TicketOrderPassengerDetailRespDTO;
import org.openzjl.index12306.biz.orderservice.service.OrderQueryFallbackService;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 订单详情按物理分表兜底查询实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderQueryFallbackServiceImpl implements OrderQueryFallbackService {

    private static final int TABLE_COUNT = 32;

    private final OrderQueryFallbackProperties properties;

    @Override
    public TicketOrderDetailRespDTO queryByOrderSnFallback(String orderSn) {
        if (!properties.isEnabled() || orderSn == null || orderSn.isBlank()) {
            return null;
        }
        for (OrderQueryFallbackProperties.DataSourceConfig datasource : properties.getDatasources()) {
            for (int tableIndex = 0; tableIndex < TABLE_COUNT; tableIndex++) {
                TicketOrderDetailRespDTO detail = querySingleTable(datasource, tableIndex, orderSn);
                if (detail != null) {
                    return detail;
                }
            }
        }
        return null;
    }

    private TicketOrderDetailRespDTO querySingleTable(OrderQueryFallbackProperties.DataSourceConfig datasource, int tableIndex, String orderSn) {
        String orderTable = "t_order_" + tableIndex;
        String orderItemTable = "t_order_item_" + tableIndex;
        String orderSql = "SELECT * FROM " + orderTable + " WHERE order_sn = ? AND (del_flag = 0 OR del_flag IS NULL) LIMIT 1";
        String orderItemSql = "SELECT * FROM " + orderItemTable + " WHERE order_sn = ? AND (del_flag = 0 OR del_flag IS NULL)";
        try (Connection connection = DriverManager.getConnection(datasource.getUrl(), datasource.getUsername(), datasource.getPassword());
             PreparedStatement orderStatement = connection.prepareStatement(orderSql)) {
            orderStatement.setString(1, orderSn);
            try (ResultSet orderResultSet = orderStatement.executeQuery()) {
                if (!orderResultSet.next()) {
                    return null;
                }
                TicketOrderDetailRespDTO detail = new TicketOrderDetailRespDTO();
                detail.setOrderSn(orderResultSet.getString("order_sn"));
                detail.setTrainId(getLong(orderResultSet, "train_id"));
                detail.setDeparture(orderResultSet.getString("departure"));
                detail.setArrival(orderResultSet.getString("arrival"));
                detail.setRidingDate(formatDate(orderResultSet.getTimestamp("riding_date")));
                detail.setOrderTime(toDate(orderResultSet.getTimestamp("order_time")));
                detail.setTrainNumber(orderResultSet.getString("train_number"));
                detail.setDepartureTime(toDate(orderResultSet.getTimestamp("departure_time")));
                detail.setArrivalTime(toDate(orderResultSet.getTimestamp("arrival_time")));
                detail.setPassengerDetails(queryPassengerDetails(connection, orderItemSql, orderSn));
                return detail;
            }
        } catch (SQLException ex) {
            log.debug("订单物理表兜底查询失败，table={}，orderSn={}", orderTable, orderSn, ex);
            return null;
        }
    }

    private List<TicketOrderPassengerDetailRespDTO> queryPassengerDetails(Connection connection, String sql, String orderSn) throws SQLException {
        List<TicketOrderPassengerDetailRespDTO> result = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, orderSn);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    TicketOrderPassengerDetailRespDTO detail = new TicketOrderPassengerDetailRespDTO();
                    detail.setId(String.valueOf(rs.getLong("id")));
                    detail.setUserId(rs.getString("user_id"));
                    detail.setUsername(rs.getString("username"));
                    detail.setSeatType(getInteger(rs, "seat_type"));
                    detail.setCarriageNumber(rs.getString("carriage_number"));
                    detail.setSeatNumber(rs.getString("seat_number"));
                    detail.setRealName(rs.getString("real_name"));
                    detail.setIdType(String.valueOf(getInteger(rs, "id_type")));
                    detail.setIdCard(rs.getString("id_card"));
                    detail.setTicketType(getInteger(rs, "ticket_type"));
                    detail.setAmount(getInteger(rs, "amount"));
                    detail.setStatus(getInteger(rs, "status"));
                    result.add(detail);
                }
            }
        }
        return result;
    }

    private Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    private Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    private Date toDate(Timestamp timestamp) {
        return timestamp == null ? null : new Date(timestamp.getTime());
    }

    private String formatDate(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return timestamp.toLocalDateTime().toLocalDate().toString();
    }
}
