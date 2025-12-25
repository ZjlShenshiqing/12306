-- ============================================
-- 车票余量令牌桶回滚脚本
-- ============================================
-- 功能：原子性地将已扣减的令牌重新加回到令牌桶中
-- 
-- 使用场景：
--   1. 订单取消：用户主动取消订单，需要释放已扣减的余票
--   2. 订单超时未支付：订单创建后长时间未支付，系统自动取消并释放余票
--   3. 订单支付失败：支付过程中出现异常，需要回滚已扣减的令牌
--
-- 参数说明：
--   KEYS[1]: 令牌桶的Hash Key（如：index12306-ticket-service:ticket_availability_token_bucket:车次ID）
--   KEYS[2]: 路线标识（如：1001_1002，表示出发站_到达站）
--   ARGV[1]: 座位类型和数量的JSON数组（如：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]）
--   ARGV[2]: 需要回滚的路线段JSON数组（如：[{"startStation":"1001","endStation":"1002"},...]）
--
-- 返回值：
--   0: 表示回滚成功
-- ============================================

-- ========== 处理路线标识，提取实际的Key ==========
-- KEYS[2] 可能包含Redis前缀（如：index12306-ticket-service:1001_1002）
-- 需要提取出实际的路线标识（如：1001_1002）
local inputString = KEYS[2]
local actualKey = inputString
local colonIndex = string.find(actualKey, ":")
if colonIndex ~= nil then
    -- 如果包含冒号，提取冒号后面的部分作为实际Key
    actualKey = string.sub(actualKey, colonIndex + 1)
end

-- ========== 解析座位类型和数量的JSON数组 ==========
-- ARGV[1] 是座位类型和回滚数量的JSON字符串
-- 例如：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]
-- 表示：需要回滚2张商务座（编码0）和3张一等座（编码1）
local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)

-- ========== 解析需要回滚的路线段数组 ==========
-- ARGV[2] 是需要回滚余票的所有路线段的JSON字符串
-- 例如：[{"startStation":"1001","endStation":"1002"},{"startStation":"1001","endStation":"1003"},...]
-- 用户取消A->D的订单，需要将A->B、A->C、A->D、B->C、B->D、C->D所有路线段的余票都加回去
local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- ========== 原子性地将令牌加回到所有相关路线段 ==========
-- 遍历用户要回滚的每种座位类型
for index, jsonObj in ipairs(jsonArray) do
    -- 获取座位类型编码（如：0=商务座，1=一等座）
    local seatType = tonumber(jsonObj.seatType)
    -- 获取该座位类型的回滚数量（需要加回的令牌数量）
    local count = tonumber(jsonObj.count)
    
    -- 遍历所有需要回滚的路线段
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        local startStation = tostring(alongJsonObj.startStation)  -- 出发站编码
        local endStation = tostring(alongJsonObj.endStation)      -- 到达站编码
        
        -- 构建Hash字段Key：出发站_到达站_座位类型
        -- 例如：1001_1002_0 表示从1001站到1002站的商务座
        local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
        
        -- 从Redis Hash中获取该路线段该座位类型的当前余票数量
        local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
        
        -- 安全检查：只有当余票数量大于等于0时才执行回滚
        -- 这个判断防止异常情况下的数据不一致：
        --   - 如果余票数量为负数，说明数据异常，不应该继续回滚
        --   - 如果余票数量为null（Redis返回nil，tonumber后为nil），也不应该回滚
        if ticketSeatAvailabilityTokenValue >= 0 then
            -- 原子性地将该路线段该座位类型的余票数量加回去
            -- hincrby: Hash字段值增减操作，count 表示增加count张票
            -- 这个操作是原子性的，确保在高并发场景下不会出现数据不一致
            redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), count)
        end
    end
end

-- ========== 返回成功结果 ==========
-- 返回0表示回滚操作成功完成
-- 调用方可以根据返回值判断回滚是否成功
return 0