-- ============================================
-- 车票余量令牌桶扣减脚本
-- ============================================
-- 功能：原子性地检查并扣减车票余量令牌，防止超卖
-- 
-- 参数说明：
--   KEYS[1]: 令牌桶的Hash Key（如：index12306-ticket-service:ticket_availability_token_bucket:车次ID）
--   KEYS[2]: 路线标识（如：1001_1002，表示出发站_到达站）
--   ARGV[1]: 座位类型和数量的JSON数组（如：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]）
--   ARGV[2]: 需要扣减的路线段JSON数组（如：[{"startStation":"1001","endStation":"1002"},...]）
--
-- 返回值：
--   成功：{"tokenIsNull":false}
--   失败：{"tokenIsNull":true,"tokenIsNullSeatTypeCounts":["0_2","1_3"]}
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
-- ARGV[1] 是座位类型和购买数量的JSON字符串
-- 例如：[{"seatType":"0","count":"2"},{"seatType":"1","count":"3"}]
-- 表示：购买2张商务座（编码0）和3张一等座（编码1）
local jsonArrayStr = ARGV[1]
local jsonArray = cjson.decode(jsonArrayStr)

-- ========== 初始化结果变量 ==========
local result = {}                    -- 返回结果对象
local tokenIsNull = false            -- 令牌是否为空（余票是否不足）
local tokenIsNullSeatTypeCounts = {} -- 余票不足的座位类型和数量列表

-- ========== 检查所有座位类型的余票是否充足 ==========
-- 遍历用户要购买的每种座位类型，检查余票是否足够
for index, jsonObj in ipairs(jsonArray) do
    -- 获取座位类型编码（如：0=商务座，1=一等座）
    local seatType = tonumber(jsonObj.seatType)
    -- 获取该座位类型的购买数量
    local count = tonumber(jsonObj.count)
    
    -- 构建Redis Hash的字段Key：路线标识_座位类型
    -- 例如：1001_1002_0 表示从1001站到1002站的商务座（编码0）
    local actualInnerHashKey = actualKey .. "_" .. seatType
    
    -- 从Redis Hash中获取该路线该座位类型的余票数量
    -- KEYS[1] 是令牌桶的Hash Key
    -- actualInnerHashKey 是Hash的字段Key
    local ticketSeatAvailabilityTokenValue = tonumber(redis.call('hget', KEYS[1], tostring(actualInnerHashKey)))
    
    -- 如果余票数量小于购买数量，说明余票不足
    if ticketSeatAvailabilityTokenValue < count then
        tokenIsNull = true
        -- 记录余票不足的座位类型和数量（格式：座位类型_数量）
        table.insert(tokenIsNullSeatTypeCounts, seatType .. "_" .. count)
    end
end

-- ========== 如果余票不足，直接返回结果 ==========
-- 构建返回结果
result['tokenIsNull'] = tokenIsNull
if tokenIsNull then
    -- 如果余票不足，返回详细信息（哪些座位类型余票不足）
    result['tokenIsNullSeatTypeCounts'] = tokenIsNullSeatTypeCounts
    -- 返回JSON格式的结果，告知调用方余票不足
    return cjson.encode(result)
end

-- ========== 余票充足，解析路线段数组，准备扣减 ==========
-- ARGV[2] 是需要扣减余票的所有路线段的JSON字符串
-- 例如：[{"startStation":"1001","endStation":"1002"},{"startStation":"1001","endStation":"1003"},...]
-- 用户购买A->D的票，需要扣减A->B、A->C、A->D、B->C、B->D、C->D所有路线段的余票
local alongJsonArrayStr = ARGV[2]
local alongJsonArray = cjson.decode(alongJsonArrayStr)

-- ========== 原子性地扣减所有相关路线段的余票 ==========
-- 遍历用户要购买的每种座位类型
for index, jsonObj in ipairs(jsonArray) do
    local seatType = tonumber(jsonObj.seatType)
    local count = tonumber(jsonObj.count)
    
    -- 遍历所有需要扣减的路线段
    for indexTwo, alongJsonObj in ipairs(alongJsonArray) do
        local startStation = tostring(alongJsonObj.startStation)  -- 出发站编码
        local endStation = tostring(alongJsonObj.endStation)      -- 到达站编码
        
        -- 构建Hash字段Key：出发站_到达站_座位类型
        -- 例如：1001_1002_0 表示从1001站到1002站的商务座
        local actualInnerHashKey = startStation .. "_" .. endStation .. "_" .. seatType
        
        -- 原子性地扣减该路线段该座位类型的余票数量
        -- hincrby: Hash字段值增减操作，-count 表示减少count张票
        -- 这个操作是原子性的，确保在高并发场景下不会出现超卖
        redis.call('hincrby', KEYS[1], tostring(actualInnerHashKey), -count)
    end
end

-- ========== 返回成功结果 ==========
-- 所有检查和扣减操作都成功完成，返回成功结果
-- result['tokenIsNull'] = false（默认值）
return cjson.encode(result)
